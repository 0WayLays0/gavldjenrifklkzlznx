package com.waylays.arcanes.client.music;

import com.waylays.arcanes.ArcanesOublies;
import com.waylays.arcanes.network.MusicPackets;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.sounds.SoundManager;
import net.neoforged.neoforge.network.PacketDistributor;

import javax.annotation.Nullable;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CompletableFuture;

/**
 * The player's own music: the track playing (on the player, heard as far as its range), the queue it came from, where it
 * is in it, paused or not; next, previous, repeat and shuffle. The server is told what plays, so that the players near
 * hear it too.
 */
public final class MusicPlayer {
    private static final Random RANDOM = new Random();

    @Nullable
    private static MusicLibrary.Track current;
    private static List<MusicLibrary.Track> queue = new ArrayList<>();
    private static final List<Integer> order = new ArrayList<>();
    private static int index;
    @Nullable
    private static byte[] bytes;
    @Nullable
    private static String hash;
    private static int lengthMs;
    private static boolean paused;
    private static boolean loading;
    /** Where it is in the track, ms, and the moment that was last brought up to date. */
    private static double positionMs;
    private static long lastNanos;
    @Nullable
    private static MusicSoundInstance instance;
    @Nullable
    private static MusicStream.Levels levels;
    /** Restarts of a stream that stopped by itself (the player died, changed worlds) without getting anywhere. */
    private static int stalls;
    private static double stallFrom;
    /**
     * Whether the sound engine has played the current sound at all, and ticks since it was last tried: with no sound
     * device (or the engine off) the music goes on silently, its clock running, and is tried again now and then.
     */
    private static boolean heardOnce;
    private static int silentTicks;
    /** Pieces of the file still to send up to the server, and the next one. */
    private static int uploadNext = -1;
    private static int sentRange = -1;
    /** When the playing track last changed (for the note shown on the screen's corner). */
    private static long changedAt;
    private static int generation;

    private MusicPlayer() {}

    @Nullable
    public static MusicLibrary.Track current() {
        return current;
    }

    public static boolean playing() {
        return current != null && !paused;
    }

    public static boolean paused() {
        return paused;
    }

    public static boolean loading() {
        return loading;
    }

    public static long positionMs() {
        return (long) positionMs;
    }

    public static int lengthMs() {
        return lengthMs > 0 ? lengthMs : current != null ? current.lengthMs : 0;
    }

    public static long changedAt() {
        return changedAt;
    }

    @Nullable
    public static String hash() {
        return hash;
    }

    @Nullable
    public static byte[] bytes() {
        return bytes;
    }

    /** How loud the music is now, 0 to 1 (for the dancing bars), {@code aheadMs} later. */
    public static float level(long aheadMs) {
        MusicStream.Levels l = levels;
        return l == null || !playing() ? 0 : l.at((long) positionMs + aheadMs);
    }

    /** Whether the sound engine is playing it now. */
    public static boolean soundActive() {
        return instance != null && Minecraft.getInstance().getSoundManager().isActive(instance);
    }

    public static List<MusicLibrary.Track> queue() {
        return queue;
    }

    // ---------------------------------------------------------------- control

    /** Plays {@code track}, the queue then being {@code from} (the list it was chosen in). */
    public static void play(MusicLibrary.Track track, List<MusicLibrary.Track> from) {
        queue = new ArrayList<>(from);
        if (!queue.contains(track)) {
            queue.add(0, track);
        }
        reorder(queue.indexOf(track));
        load(track);
    }

    /** The order the queue plays in: in order, or shuffled with {@code first} first. */
    private static void reorder(int first) {
        order.clear();
        for (int i = 0; i < queue.size(); i++) {
            order.add(i);
        }
        if (MusicLibrary.get().shuffle) {
            java.util.Collections.shuffle(order, RANDOM);
            order.remove(Integer.valueOf(first));
            order.add(0, first);
            index = 0;
        } else {
            index = Math.max(0, first);
        }
    }

    public static void setShuffle(boolean shuffle) {
        MusicLibrary lib = MusicLibrary.get();
        lib.shuffle = shuffle;
        lib.save();
        if (current != null && queue.contains(current)) {
            reorder(queue.indexOf(current));
        }
    }

    public static void setRepeat(MusicLibrary.Repeat repeat) {
        MusicLibrary lib = MusicLibrary.get();
        lib.repeat = repeat;
        lib.save();
        if (instance != null) {
            instance.stream().setLoop(repeat == MusicLibrary.Repeat.ONE);
        }
        tellServer();
    }

    public static void setVolume(float volume) {
        MusicLibrary lib = MusicLibrary.get();
        lib.volume = Math.max(0, Math.min(1, volume));
        if (instance != null) {
            instance.setVolume(lib.volume);
        }
    }

    /** Saves the volume once the slider is let go, and tells those who hear it. */
    public static void volumeSettled() {
        MusicLibrary.get().save();
        tellServer();
    }

    public static void setRange(int range) {
        MusicLibrary lib = MusicLibrary.get();
        int r = MusicSoundInstance.snap(range);
        if (r == lib.range) {
            return;
        }
        lib.range = r;
        lib.save();
        // How far it carries is part of how the sound is made: it starts again from where it is
        if (playing()) {
            startAt(positionMs);
        }
    }

    private static void load(MusicLibrary.Track track) {
        stopSound();
        current = track;
        paused = false;
        loading = true;
        positionMs = 0;
        lengthMs = track.lengthMs;
        bytes = null;
        hash = null;
        uploadNext = -1;
        changedAt = System.currentTimeMillis();
        int gen = ++generation;
        CompletableFuture.supplyAsync(() -> {
            try {
                byte[] data = track.read();
                String h = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-1").digest(data));
                int len = MusicDecoders.lengthMs(data, track.ext);
                return new Object[] {data, h, len};
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }, Util.backgroundExecutor()).whenComplete((r, err) -> Minecraft.getInstance().execute(() -> {
            if (gen != generation) {
                return;
            }
            loading = false;
            if (err != null || r == null) {
                ArcanesOublies.LOGGER.warn("Could not read the music file {}", track.id, err);
                current = null;
                return;
            }
            bytes = (byte[]) r[0];
            hash = (String) r[1];
            int len = (Integer) r[2];
            if (len > 0) {
                lengthMs = len;
                track.lengthMs = len;
            }
            levels = new MusicStream.Levels();
            startAt(0);
        }));
    }

    /** Starts the sound again {@code ms} into the track (to seek, to resume, after a change of range). */
    private static void startAt(double ms) {
        Minecraft mc = Minecraft.getInstance();
        stopSound();
        if (current == null || bytes == null || mc.player == null) {
            return;
        }
        MusicLibrary lib = MusicLibrary.get();
        try {
            MusicStream stream = new MusicStream(bytes, current.ext, (long) ms, lib.repeat == MusicLibrary.Repeat.ONE, levels);
            instance = new MusicSoundInstance(mc.player, stream, lib.volume, lib.range);
            mc.getSoundManager().play(instance);
            mc.getMusicManager().stopPlaying();
            heardOnce = false;
            silentTicks = 0;
        } catch (Exception | LinkageError e) {
            ArcanesOublies.LOGGER.warn("Could not play the music file {}", current.id, e);
            instance = null;
            return;
        }
        positionMs = ms;
        lastNanos = System.nanoTime();
        paused = false;
        tellServer();
    }

    private static void stopSound() {
        if (instance != null) {
            Minecraft.getInstance().getSoundManager().stop(instance);
            instance = null;
        }
    }

    public static void togglePause() {
        if (current == null) {
            return;
        }
        if (paused) {
            paused = false;
            startAt(positionMs);
        } else {
            paused = true;
            stopSound();
            PacketDistributor.sendToServer(new MusicPackets.Stop());
            sentRange = -1;
        }
    }

    public static void seek(double ms) {
        if (current == null || bytes == null) {
            return;
        }
        double to = Math.max(0, Math.min(ms, Math.max(0, lengthMs() - 500)));
        if (paused) {
            positionMs = to;
        } else {
            startAt(to);
        }
    }

    public static void stop() {
        generation++;
        stopSound();
        if (current != null && Minecraft.getInstance().getConnection() != null) {
            PacketDistributor.sendToServer(new MusicPackets.Stop());
        }
        current = null;
        bytes = null;
        hash = null;
        paused = false;
        loading = false;
        positionMs = 0;
        sentRange = -1;
        uploadNext = -1;
    }

    public static void next() {
        step(1, true);
    }

    /** Back to the start of the track, or (within its first seconds) to the one before. */
    public static void previous() {
        if (positionMs > 3000) {
            seek(0);
        } else {
            step(-1, true);
        }
    }

    /** Moves {@code by} places in the queue; at its ends, around if {@code wrap} or repeating it all, else stops. */
    private static void step(int by, boolean wrap) {
        if (queue.isEmpty() || order.isEmpty()) {
            stop();
            return;
        }
        int next = index + by;
        if (next < 0 || next >= order.size()) {
            if (!wrap && MusicLibrary.get().repeat != MusicLibrary.Repeat.ALL) {
                stop();
                return;
            }
            next = Math.floorMod(next, order.size());
        }
        index = next;
        int at = order.get(index);
        if (at >= 0 && at < queue.size()) {
            load(queue.get(at));
        }
    }

    /** Tells the server what plays (or that nothing does, for the player's ears only). */
    private static void tellServer() {
        if (Minecraft.getInstance().getConnection() == null || current == null || hash == null || paused) {
            return;
        }
        MusicLibrary lib = MusicLibrary.get();
        if (lib.range <= 0) {
            if (sentRange != 0) {
                PacketDistributor.sendToServer(new MusicPackets.Stop());
                sentRange = 0;
            }
            return;
        }
        int size = bytes == null ? 0 : bytes.length;
        PacketDistributor.sendToServer(new MusicPackets.Start(hash, current.artist != null ? current.artist + " - " + current.title : current.title,
                lib.volume, lib.range, lib.repeat == MusicLibrary.Repeat.ONE, (int) positionMs, lengthMs(), size));
        sentRange = lib.range;
    }

    /** The server wants the file: someone near hears it and has not got it. */
    static void sendUp(String wanted) {
        if (wanted.equals(hash) && bytes != null && bytes.length <= MusicPackets.MAX_FILE) {
            uploadNext = 0;
        }
    }

    // ---------------------------------------------------------------- each tick

    public static void tick(Minecraft mc) {
        long now = System.nanoTime();
        if (current != null && !paused && !loading && !mc.isPaused()) {
            positionMs += (now - lastNanos) / 1e6;
        }
        lastNanos = now;
        // Sending a file up, a few pieces a tick
        if (uploadNext >= 0 && bytes != null && hash != null && mc.getConnection() != null) {
            int total = Math.max(1, (bytes.length + MusicPackets.UP_CHUNK - 1) / MusicPackets.UP_CHUNK);
            for (int k = 0; k < 6 && uploadNext < total; k++, uploadNext++) {
                int from = uploadNext * MusicPackets.UP_CHUNK;
                int len = Math.min(MusicPackets.UP_CHUNK, bytes.length - from);
                byte[] piece = new byte[len];
                System.arraycopy(bytes, from, piece, 0, len);
                PacketDistributor.sendToServer(new MusicPackets.Upload(hash, uploadNext, total, piece));
            }
            if (uploadNext >= total) {
                uploadNext = -1;
            }
        }
        if (current == null || paused || loading) {
            return;
        }
        if (mc.player == null) {
            return;
        }
        SoundManager sounds = mc.getSoundManager();
        boolean active = instance != null && sounds.isActive(instance);
        if (active) {
            heardOnce = true;
        }
        if (!active) {
            boolean ended = instance != null && instance.stream().ended();
            int len = lengthMs();
            if (ended || len > 0 && positionMs >= len - 250) {
                stalls = 0;
                if (MusicLibrary.get().repeat == MusicLibrary.Repeat.ONE) {
                    startAt(0);
                } else {
                    step(1, false);
                }
            } else if (instance != null && !heardOnce) {
                // It never started: no sound device, or the sound engine is off. The music goes on silently (its clock,
                // the bars, the other players' music all go on), and is tried again every five seconds
                if (++silentTicks >= 100) {
                    startAt(positionMs);
                }
            } else {
                // Stopped by itself (the player died, changed worlds, the sounds were reloaded): on again from where it was
                if (Math.abs(positionMs - stallFrom) < 500) {
                    stalls++;
                } else {
                    stalls = 0;
                }
                stallFrom = positionMs;
                if (stalls < 5) {
                    startAt(positionMs);
                } else {
                    ArcanesOublies.LOGGER.warn("The music keeps stopping: {}", current.id);
                    stop();
                }
            }
        } else if (mc.level != null && mc.level.getGameTime() % 100 == 0) {
            // The game's own music would play over it
            mc.getMusicManager().stopPlaying();
        }
    }

    /** Leaving the world: the music stops (the queue is kept). */
    public static void onLeave() {
        stopSound();
        current = null;
        bytes = null;
        hash = null;
        paused = false;
        loading = false;
        sentRange = -1;
        uploadNext = -1;
        generation++;
    }
}
