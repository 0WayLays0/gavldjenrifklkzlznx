package com.waylays.arcanes.music;

import com.waylays.arcanes.ArcanesOublies;
import com.waylays.arcanes.network.MusicPackets;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * The server side of the music player: who plays what, to whom it carries. A player's music is heard by the players
 * within its range (in the same world); those who have not got its file ask for it, the server asks the player playing
 * it to send it up (once), keeps it a while (a few files at most, the oldest let go first) and passes it on to them a
 * piece at a time.
 */
@EventBusSubscriber(modid = ArcanesOublies.MODID)
public final class MusicRelay {
    private static final Pattern HASH = Pattern.compile("[0-9a-f]{40}");
    /** How much the server keeps of the files it was sent. */
    private static final long CACHE_BYTES = 96L * 1024 * 1024;
    /** A listener hears it from this far past its range, and stops hearing it a little farther. */
    private static final double HEAR_MARGIN = 4, LOSE_MARGIN = 12;
    /** Pieces of files sent down each tick, to all. */
    private static final int SEND_PER_TICK = 3;

    /** A player's music: what, how loud, how far, since when (server time, ms), and who hears it now. */
    private static final class Playing {
        String hash;
        String title;
        float volume;
        int range;
        boolean loop;
        long startedAt;
        int lengthMs;
        int size;
        final Set<UUID> listeners = new HashSet<>();
    }

    /** A file being sent up, or kept. */
    private static final class Stored {
        final byte[][] pieces;
        int received;
        long lastUsed = System.currentTimeMillis();
        final Set<UUID> waiting = new HashSet<>();

        Stored(int total) {
            this.pieces = new byte[total][];
        }

        boolean complete() {
            return received == pieces.length;
        }

        long bytes() {
            long n = 0;
            for (byte[] p : pieces) {
                n += p == null ? 0 : p.length;
            }
            return n;
        }
    }

    private record Send(UUID to, String hash, int index) {}

    private static final Map<UUID, Playing> PLAYING = new HashMap<>();
    private static final Map<String, Stored> STORED = new LinkedHashMap<>(16, .75f, true);
    private static final Map<String, UUID> UPLOADER = new HashMap<>();
    private static final ArrayDeque<Send> OUTBOX = new ArrayDeque<>();

    private MusicRelay() {}

    private static long now() {
        return System.currentTimeMillis();
    }

    /** Where in its track a player's music is now, ms. */
    private static int offset(Playing p) {
        long t = now() - p.startedAt;
        if (p.loop && p.lengthMs > 0) {
            t %= p.lengthMs;
        }
        return (int) Math.max(0, Math.min(Integer.MAX_VALUE, t));
    }

    public static void start(ServerPlayer player, MusicPackets.Start s) {
        if (!HASH.matcher(s.hash()).matches()) {
            return;
        }
        Playing p = PLAYING.computeIfAbsent(player.getUUID(), id -> new Playing());
        boolean changed = !s.hash().equals(p.hash);
        p.hash = s.hash();
        p.title = s.title();
        p.volume = Mth.clamp(s.volume(), 0, 1);
        p.range = Mth.clamp(s.range(), 0, 64);
        p.loop = s.loop();
        p.startedAt = now() - Math.max(0, s.offsetMs());
        p.lengthMs = Math.max(0, s.lengthMs());
        p.size = s.size();
        if (changed) {
            // A new track: everyone hearing the old one is told again
            p.listeners.clear();
        }
        // Whoever hears it already is told how it plays now (louder, farther, from another moment)
        MinecraftServer server = player.getServer();
        if (server != null) {
            for (UUID id : p.listeners) {
                ServerPlayer l = server.getPlayerList().getPlayer(id);
                if (l != null) {
                    PacketDistributor.sendToPlayer(l, play(player, p));
                }
            }
        }
    }

    public static void stop(ServerPlayer player) {
        Playing p = PLAYING.remove(player.getUUID());
        if (p != null) {
            halt(player.getServer(), player.getId(), p.listeners);
        }
    }

    private static void halt(MinecraftServer server, int entityId, Set<UUID> listeners) {
        if (server == null) {
            return;
        }
        for (UUID id : listeners) {
            ServerPlayer l = server.getPlayerList().getPlayer(id);
            if (l != null) {
                PacketDistributor.sendToPlayer(l, new MusicPackets.Halt(entityId));
            }
        }
    }

    private static MusicPackets.Play play(ServerPlayer source, Playing p) {
        return new MusicPackets.Play(source.getId(), p.hash, p.title, p.volume, p.range, p.loop, offset(p), p.lengthMs);
    }

    /** A listener asks for a file: sent from the store, or asked of the player playing it. */
    public static void request(ServerPlayer player, String hash) {
        if (!HASH.matcher(hash).matches()) {
            return;
        }
        Stored stored = STORED.get(hash);
        if (stored != null && stored.complete()) {
            stored.lastUsed = now();
            queue(player.getUUID(), hash, stored.pieces.length);
            return;
        }
        // Who plays it now (and so has it)?
        ServerPlayer source = null;
        MinecraftServer server = player.getServer();
        if (server == null) {
            return;
        }
        for (Map.Entry<UUID, Playing> e : PLAYING.entrySet()) {
            if (hash.equals(e.getValue().hash)) {
                source = server.getPlayerList().getPlayer(e.getKey());
                if (source != null && e.getValue().size > MusicPackets.MAX_FILE) {
                    return;
                }
                break;
            }
        }
        if (source == null) {
            return;
        }
        if (stored == null) {
            Playing p = PLAYING.get(source.getUUID());
            int total = Math.max(1, (p.size + MusicPackets.UP_CHUNK - 1) / MusicPackets.UP_CHUNK);
            stored = new Stored(total);
            STORED.put(hash, stored);
            UPLOADER.put(hash, source.getUUID());
            PacketDistributor.sendToPlayer(source, new MusicPackets.SendUp(hash));
        }
        stored.waiting.add(player.getUUID());
    }

    /** A piece of a file sent up by the player playing it (and only them). */
    public static void upload(ServerPlayer player, MusicPackets.Upload u) {
        Stored stored = STORED.get(u.hash());
        if (stored == null || !player.getUUID().equals(UPLOADER.get(u.hash())) || u.total() != stored.pieces.length || u.index() < 0
                || u.index() >= stored.pieces.length || stored.pieces[u.index()] != null) {
            return;
        }
        stored.pieces[u.index()] = u.data();
        stored.received++;
        if (stored.complete()) {
            UPLOADER.remove(u.hash());
            stored.lastUsed = now();
            for (UUID id : stored.waiting) {
                queue(id, u.hash(), stored.pieces.length);
            }
            stored.waiting.clear();
            trim();
        }
    }

    /** Lets the files used longest ago go, past what the server keeps. */
    private static void trim() {
        long total = 0;
        for (Stored s : STORED.values()) {
            total += s.bytes();
        }
        Iterator<Map.Entry<String, Stored>> it = STORED.entrySet().iterator();
        while (total > CACHE_BYTES && it.hasNext()) {
            Map.Entry<String, Stored> e = it.next();
            if (e.getValue().complete()) {
                total -= e.getValue().bytes();
                it.remove();
            }
        }
    }

    /** Queues a file to send down, re-cut into the larger pieces sent to clients. */
    private static void queue(UUID to, String hash, int upPieces) {
        Stored stored = STORED.get(hash);
        if (stored == null) {
            return;
        }
        int size = (int) stored.bytes();
        int pieces = Math.max(1, (size + MusicPackets.DOWN_CHUNK - 1) / MusicPackets.DOWN_CHUNK);
        for (int i = 0; i < pieces; i++) {
            OUTBOX.add(new Send(to, hash, i));
        }
    }

    private static byte[] piece(Stored stored, int index) {
        int size = (int) stored.bytes();
        int from = index * MusicPackets.DOWN_CHUNK, to = Math.min(size, from + MusicPackets.DOWN_CHUNK);
        byte[] out = new byte[Math.max(0, to - from)];
        int pos = 0, off = 0;
        for (byte[] p : stored.pieces) {
            int a = Math.max(from, pos), b = Math.min(to, pos + p.length);
            if (a < b) {
                System.arraycopy(p, a - pos, out, off, b - a);
                off += b - a;
            }
            pos += p.length;
            if (pos >= to) {
                break;
            }
        }
        return out;
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        // Pieces of files on their way down
        for (int k = 0; k < SEND_PER_TICK && !OUTBOX.isEmpty(); k++) {
            Send s = OUTBOX.poll();
            ServerPlayer to = server.getPlayerList().getPlayer(s.to());
            Stored stored = STORED.get(s.hash());
            if (to == null || stored == null || !stored.complete()) {
                continue;
            }
            int size = (int) stored.bytes();
            int total = Math.max(1, (size + MusicPackets.DOWN_CHUNK - 1) / MusicPackets.DOWN_CHUNK);
            PacketDistributor.sendToPlayer(to, new MusicPackets.Data(s.hash(), s.index(), total, piece(stored, s.index())));
        }
        if (server.getTickCount() % 10 != 0) {
            return;
        }
        // Who is in range of whose music
        List<UUID> gone = new ArrayList<>();
        for (Map.Entry<UUID, Playing> e : PLAYING.entrySet()) {
            ServerPlayer source = server.getPlayerList().getPlayer(e.getKey());
            Playing p = e.getValue();
            if (source == null) {
                gone.add(e.getKey());
                continue;
            }
            if (!p.loop && p.lengthMs > 0 && now() - p.startedAt > p.lengthMs + 3000L) {
                // Over, and the player has not started anything else
                halt(server, source.getId(), p.listeners);
                gone.add(e.getKey());
                continue;
            }
            for (ServerPlayer l : server.getPlayerList().getPlayers()) {
                if (l == source) {
                    continue;
                }
                boolean sameWorld = l.level() == source.level();
                double d = sameWorld ? l.distanceTo(source) : Double.MAX_VALUE;
                boolean hears = p.listeners.contains(l.getUUID());
                if (!hears && p.range > 0 && d <= p.range + HEAR_MARGIN) {
                    p.listeners.add(l.getUUID());
                    PacketDistributor.sendToPlayer(l, play(source, p));
                } else if (hears && (p.range == 0 || d > p.range + LOSE_MARGIN)) {
                    p.listeners.remove(l.getUUID());
                    PacketDistributor.sendToPlayer(l, new MusicPackets.Halt(source.getId()));
                }
            }
        }
        gone.forEach(PLAYING::remove);
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            stop(player);
            for (Playing p : PLAYING.values()) {
                p.listeners.remove(player.getUUID());
            }
            // A file they were sending up will not come now
            UPLOADER.entrySet().removeIf(e -> {
                if (e.getValue().equals(player.getUUID())) {
                    Stored s = STORED.get(e.getKey());
                    if (s != null && !s.complete()) {
                        STORED.remove(e.getKey());
                    }
                    return true;
                }
                return false;
            });
        }
    }

    /** A player gone to another world (or back from the dead) is someone new to those around: told again of the music. */
    @SubscribeEvent
    public static void onChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        forget(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        forget(event.getEntity().getUUID());
    }

    private static void forget(UUID player) {
        for (Playing p : PLAYING.values()) {
            p.listeners.remove(player);
        }
        Playing own = PLAYING.get(player);
        if (own != null) {
            // Their own music's listeners have to be told of the new body it now plays on
            own.listeners.clear();
        }
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        PLAYING.clear();
        STORED.clear();
        UPLOADER.clear();
        OUTBOX.clear();
    }
}
