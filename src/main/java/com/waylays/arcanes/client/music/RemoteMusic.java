package com.waylays.arcanes.client.music;

import com.waylays.arcanes.ArcanesOublies;
import com.waylays.arcanes.network.MusicPackets;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.network.PacketDistributor;

import javax.annotation.Nullable;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Other players' music, heard here: each played on its player, from where it is in its track. Files not yet had are
 * asked of the server and come in pieces; the ones had are kept a while (the last few, up to a size).
 */
public final class RemoteMusic {
    private static final long CACHE_BYTES = 96L * 1024 * 1024;

    /** A player's music heard here: what, how, and the moment ({@link System#nanoTime}) it was {@code offsetMs} in. */
    private static final class Heard {
        MusicPackets.Play play;
        long at;
        @Nullable
        MusicSoundInstance instance;
        int restarts;
    }

    private static final Map<Integer, Heard> HEARD = new HashMap<>();
    private static final LinkedHashMap<String, byte[]> FILES = new LinkedHashMap<>(16, .75f, true);
    private static final Map<String, byte[][]> INCOMING = new HashMap<>();
    private static final Set<String> ASKED = new HashSet<>();

    private RemoteMusic() {}

    /** Called by the packet: someone near plays this. */
    public static void play(MusicPackets.Play p) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && p.entityId() == mc.player.getId()) {
            return;
        }
        Heard h = HEARD.computeIfAbsent(p.entityId(), id -> new Heard());
        boolean sameTrack = h.play != null && h.play.hash().equals(p.hash());
        boolean sameRange = h.play != null && MusicSoundInstance.snap(h.play.range()) == MusicSoundInstance.snap(p.range());
        h.play = p;
        h.at = System.nanoTime();
        h.restarts = 0;
        if (sameTrack && sameRange && h.instance != null && mc.getSoundManager().isActive(h.instance)) {
            // Only louder or softer: no need to start it again
            h.instance.setVolume(p.volume());
            h.instance.stream().setLoop(p.loop());
            return;
        }
        stop(h);
        if (FILES.containsKey(p.hash())) {
            start(p.entityId(), h);
        } else if (ASKED.add(p.hash())) {
            PacketDistributor.sendToServer(new MusicPackets.Request(p.hash()));
        }
    }

    public static void halt(int entityId) {
        Heard h = HEARD.remove(entityId);
        if (h != null) {
            stop(h);
        }
    }

    /** The server wants a file this player plays. */
    public static void sendUp(String hash) {
        MusicPlayer.sendUp(hash);
    }

    /** A piece of a file: once all are in (and they make the file asked for), the music waiting on it starts. */
    public static void data(MusicPackets.Data d) {
        if (d.total() <= 0 || d.total() > MusicPackets.MAX_FILE / MusicPackets.DOWN_CHUNK + 2 || d.index() < 0 || d.index() >= d.total()) {
            return;
        }
        byte[][] pieces = INCOMING.computeIfAbsent(d.hash(), k -> new byte[d.total()][]);
        if (pieces.length != d.total()) {
            return;
        }
        pieces[d.index()] = d.data();
        for (byte[] p : pieces) {
            if (p == null) {
                return;
            }
        }
        INCOMING.remove(d.hash());
        int size = 0;
        for (byte[] p : pieces) {
            size += p.length;
        }
        byte[] file = new byte[size];
        int at = 0;
        for (byte[] p : pieces) {
            System.arraycopy(p, 0, file, at, p.length);
            at += p.length;
        }
        try {
            String h = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-1").digest(file));
            if (!h.equals(d.hash())) {
                ArcanesOublies.LOGGER.warn("A music file came broken ({} instead of {})", h, d.hash());
                ASKED.remove(d.hash());
                return;
            }
        } catch (Exception e) {
            return;
        }
        FILES.put(d.hash(), file);
        trim();
        for (Map.Entry<Integer, Heard> e : HEARD.entrySet()) {
            if (e.getValue().play.hash().equals(d.hash()) && e.getValue().instance == null) {
                start(e.getKey(), e.getValue());
            }
        }
    }

    private static void trim() {
        long total = 0;
        for (byte[] f : FILES.values()) {
            total += f.length;
        }
        Iterator<Map.Entry<String, byte[]>> it = FILES.entrySet().iterator();
        while (total > CACHE_BYTES && it.hasNext()) {
            Map.Entry<String, byte[]> e = it.next();
            total -= e.getValue().length;
            ASKED.remove(e.getKey());
            it.remove();
        }
    }

    /** Where in its track a heard music is now, ms. */
    private static long offset(Heard h) {
        long ms = h.play.offsetMs() + (System.nanoTime() - h.at) / 1_000_000L;
        if (h.play.loop() && h.play.lengthMs() > 0) {
            ms %= h.play.lengthMs();
        }
        return ms;
    }

    private static void start(int entityId, Heard h) {
        Minecraft mc = Minecraft.getInstance();
        byte[] file = FILES.get(h.play.hash());
        if (mc.level == null || file == null) {
            return;
        }
        Entity source = mc.level.getEntity(entityId);
        if (source == null) {
            return;
        }
        long ms = offset(h);
        if (!h.play.loop() && h.play.lengthMs() > 0 && ms >= h.play.lengthMs()) {
            return;
        }
        try {
            String ext = ext(file);
            MusicStream stream = new MusicStream(file, ext, ms, h.play.loop(), null);
            h.instance = new MusicSoundInstance(source, stream, h.play.volume(), h.play.range());
            mc.getSoundManager().play(h.instance);
        } catch (Exception | LinkageError e) {
            ArcanesOublies.LOGGER.warn("Could not play the music of {}", source.getName().getString(), e);
            h.instance = null;
        }
    }

    /** What kind of file it is, by its first bytes. */
    static String ext(byte[] d) {
        if (d.length > 4 && d[0] == 'O' && d[1] == 'g' && d[2] == 'g' && d[3] == 'S') {
            return "ogg";
        }
        if (d.length > 12 && d[0] == 'R' && d[1] == 'I' && d[2] == 'F' && d[3] == 'F' && d[8] == 'W' && d[9] == 'A' && d[10] == 'V' && d[11] == 'E') {
            return "wav";
        }
        return "mp3";
    }

    private static void stop(Heard h) {
        if (h.instance != null) {
            Minecraft.getInstance().getSoundManager().stop(h.instance);
            h.instance = null;
        }
    }

    /** Whoever plays music near: the title and who (for the screen's list of what is heard). */
    public static Map<Integer, String> heard() {
        Map<Integer, String> out = new LinkedHashMap<>();
        HEARD.forEach((id, h) -> out.put(id, h.play.title()));
        return out;
    }

    public static void tick(Minecraft mc) {
        if (mc.level == null) {
            return;
        }
        for (Map.Entry<Integer, Heard> e : HEARD.entrySet()) {
            Heard h = e.getValue();
            if (!FILES.containsKey(h.play.hash())) {
                continue;
            }
            boolean active = h.instance != null && mc.getSoundManager().isActive(h.instance);
            if (!active && h.restarts < 8 && mc.level.getGameTime() % 10 == 0) {
                // Not started yet (its player not yet seen here) or stopped by itself: on again
                if (h.instance == null || !h.instance.stream().ended()) {
                    h.restarts++;
                    h.instance = null;
                    start(e.getKey(), h);
                }
            }
        }
    }

    /** Leaving the world: everything heard stops (the files are kept for a while). */
    public static void onLeave() {
        for (Heard h : HEARD.values()) {
            stop(h);
        }
        HEARD.clear();
        INCOMING.clear();
        ASKED.clear();
        ASKED.addAll(FILES.keySet());
    }
}
