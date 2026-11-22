package com.waylays.cinematic.studio;

import com.mojang.logging.LogUtils;
import com.waylays.cinematic.net.ActionMsg;
import com.waylays.cinematic.net.DownloadMsg;
import com.waylays.cinematic.net.Net;
import com.waylays.cinematic.net.PlayStateMsg;
import com.waylays.cinematic.net.PoseKeysMsg;
import com.waylays.cinematic.net.SyncMsg;
import com.waylays.cinematic.net.UploadMsg;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.BlockSnapshot;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The studio on the server: actors, recording (with the blocks the performer changes), playback of scenes (the
 * blocks put back as they were first), skins and takes received from and sent to the clients.
 */
public final class StudioServer implements FrameSource {
    private static final Logger LOG = LogUtils.getLogger();
    public static final StudioServer INSTANCE = new StudioServer();
    private static final int COUNTDOWN = 60;
    private static final int BLOCK_FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE | Block.UPDATE_SUPPRESS_DROPS;

    private MinecraftServer server;

    // What is playing (one scene at a time)
    private String scene;
    private boolean playing;
    private int tick, startTick, countdown;
    private UUID recActor, recorder;
    private final Map<BlockPos, BlockState> pending = new LinkedHashMap<>();
    private final List<BlockChange> recBlocks = new ArrayList<>();

    // A take finished on the server, waiting for the performer's client to send its frames
    private UUID uploadActor, uploader;
    private List<BlockChange> uploadBlocks = List.of();
    private final Map<String, Net.Assembler> uploads = new HashMap<>();

    private StudioServer() {}

    public static void init() {
        ActorEntity.serverSource = INSTANCE;
        MinecraftForge.EVENT_BUS.addListener((ServerStartedEvent e) -> INSTANCE.start(e.getServer()));
        MinecraftForge.EVENT_BUS.addListener((ServerStoppedEvent e) -> INSTANCE.stopServer());
        MinecraftForge.EVENT_BUS.addListener(INSTANCE::onServerTick);
        MinecraftForge.EVENT_BUS.addListener(INSTANCE::onLogin);
        MinecraftForge.EVENT_BUS.addListener(INSTANCE::onLogout);
        MinecraftForge.EVENT_BUS.addListener(INSTANCE::onBreak);
        MinecraftForge.EVENT_BUS.addListener(INSTANCE::onPlace);
    }

    private void start(MinecraftServer server) {
        this.server = server;
        resetPlayback();
        uploadActor = uploader = null;
        uploads.clear();
    }

    private void stopServer() {
        server = null;
        resetPlayback();
    }

    private void resetPlayback() {
        scene = null;
        playing = false;
        tick = startTick = countdown = 0;
        recActor = recorder = null;
        pending.clear();
        recBlocks.clear();
    }

    private StudioData data() {
        return StudioData.get(server);
    }

    // ------------------------------------------------------------------ FrameSource

    @Override
    public Track track(UUID actor) {
        if (server == null) return null;
        StudioData.Actor a = data().actor(actor);
        return a == null ? null : a.track;
    }

    @Override
    public int tick(UUID actor) {
        if (scene == null || server == null || actor.equals(recActor)) return 0;
        StudioData.Actor a = data().actor(actor);
        if (a == null || !scene.equals(a.scene)) return 0;
        return playing ? tick : startTick;
    }

    // ------------------------------------------------------------------ permissions and messages

    public boolean canDirect(ServerPlayer p) {
        return p.hasPermissions(StudioConfig.PERMISSION.get()) || server.isSingleplayerOwner(p.getGameProfile());
    }

    private static void tell(ServerPlayer p, String key, Object... args) {
        p.displayClientMessage(Component.translatable(key, args), true);
    }

    // ------------------------------------------------------------------ orders

    public void onAction(ServerPlayer p, ActionMsg msg) {
        if (server == null) return;
        if (!canDirect(p)) {
            tell(p, "cinematic.studio.no_permission");
            return;
        }
        StudioData d = data();
        StudioData.Actor actor = d.actor(msg.actor());
        switch (msg.action()) {
            case CREATE_SCENE -> {
                String name = cleanName(msg.text());
                if (name.isEmpty() || d.scenes.containsKey(name)) {
                    tell(p, "cinematic.studio.bad_name");
                    return;
                }
                d.addScene(name);
            }
            case DELETE_SCENE -> {
                StudioData.Scene s = d.scenes.get(msg.scene());
                if (s == null || busyWith(s.name, p)) return;
                for (StudioData.Actor a : s.actors) removeEntity(a);
                d.scenes.remove(s.name);
                d.setDirty();
            }
            case CREATE_ACTOR -> {
                StudioData.Scene s = d.scenes.get(msg.scene());
                if (s == null) return;
                StudioData.Actor a = new StudioData.Actor();
                a.name = cleanName(msg.text());
                if (a.name.isEmpty()) a.name = "Acteur " + (s.actors.size() + 1);
                a.scene = s.name;
                a.dim = p.serverLevel().dimension();
                s.actors.add(a);
                // Two blocks in front of the director, facing them
                float yr = p.getYRot() * Mth.DEG_TO_RAD;
                net.minecraft.world.phys.Vec3 front = p.position().add(-Mth.sin(yr) * 2.0, 0, Mth.cos(yr) * 2.0);
                spawn(a, p.serverLevel(), front.x, p.getY(), front.z, Mth.wrapDegrees(p.getYRot() + 180f));
                d.setDirty();
            }
            case DUPLICATE_ACTOR -> {
                if (actor == null) return;
                StudioData.Scene s = d.scenes.get(actor.scene);
                StudioData.Actor a = new StudioData.Actor();
                a.name = cleanName(actor.name + " 2");
                a.skin = actor.skin;
                a.slim = actor.slim;
                a.scene = actor.scene;
                a.dim = actor.dim;
                a.track = actor.track.copy();
                a.track.blocks.clear();
                s.actors.add(a);
                Entity e = entity(actor);
                ServerLevel level = server.getLevel(a.dim);
                if (level != null) {
                    if (e != null) spawn(a, level, e.getX(), e.getY(), e.getZ(), e.getYRot());
                    else spawn(a, level, p.getX(), p.getY(), p.getZ(), p.getYRot());
                }
                d.setDirty();
                sendTrack(a, null);
            }
            case DELETE_ACTOR -> {
                if (actor == null || busyWith(actor.scene, p)) return;
                removeEntity(actor);
                StudioData.Scene s = d.scenes.get(actor.scene);
                if (s != null) s.actors.remove(actor);
                d.setDirty();
            }
            case RENAME_ACTOR -> {
                if (actor == null) return;
                String name = cleanName(msg.text());
                if (!name.isEmpty()) actor.name = name;
                d.setDirty();
            }
            case SET_SLIM -> {
                if (actor == null) return;
                actor.slim = msg.number() != 0;
                if (entity(actor) instanceof ActorEntity e) e.setSlim(actor.slim);
                d.setDirty();
            }
            case SET_SKIN -> {
                if (actor == null) return;
                String hash = msg.text();
                if (!hash.isEmpty() && (!Skins.validHash(hash) || !Files.isRegularFile(skinFile(hash)))) {
                    tell(p, "cinematic.skin.missing");
                    return;
                }
                actor.skin = hash;
                if (entity(actor) instanceof ActorEntity e) e.setSkin(hash);
                d.setDirty();
                tell(p, "cinematic.skin.applied", actor.name);
            }
            case MOVE_HERE -> {
                if (actor == null || busyWith(actor.scene, p)) return;
                moveHere(actor, p);
                d.setDirty();
            }
            case CLEAR_TAKE -> {
                if (actor == null || busyWith(actor.scene, p)) return;
                Entity e = entity(actor);
                actor.track = new Track();
                if (e instanceof ActorEntity ae) {
                    // Stays where its take started
                    ae.setPos(ae.getX(), ae.getY(), ae.getZ());
                }
                d.setDirty();
                sendTrack(actor, null);
            }
            case RECORD -> {
                if (actor == null) return;
                if (busy()) {
                    tell(p, "cinematic.studio.busy");
                    return;
                }
                StudioData.Scene s = d.scenes.get(actor.scene);
                if (s == null) return;
                actor.dim = p.serverLevel().dimension();
                for (StudioData.Actor a : s.actors) ensureEntity(a, p);
                restoreBlocks(s, null);
                scene = s.name;
                recActor = actor.id;
                recorder = p.getUUID();
                tick = startTick = 0;
                playing = false;
                countdown = COUNTDOWN;
                pending.clear();
                recBlocks.clear();
                broadcastState();
            }
            case PLAY -> {
                StudioData.Scene s = d.scenes.get(msg.scene());
                if (s == null) return;
                if (recActor != null) {
                    tell(p, "cinematic.studio.busy");
                    return;
                }
                for (StudioData.Actor a : s.actors) ensureEntity(a, p);
                int from = Mth.clamp(msg.number(), 0, Math.max(0, s.length() - 1));
                restoreBlocks(s, null);
                applyBlocksUpTo(s, from);
                scene = s.name;
                tick = startTick = from;
                countdown = 0;
                playing = true;
                broadcastState();
                return;
            }
            case STOP -> {
                if (recActor != null) {
                    if (p.getUUID().equals(recorder) || canDirect(p)) finishRecording();
                } else {
                    resetPlayback();
                    broadcastState();
                }
                return;
            }
        }
        sync();
    }

    private boolean busy() {
        return recActor != null || countdown > 0 || uploadActor != null;
    }

    private boolean busyWith(String sceneName, ServerPlayer p) {
        if (sceneName.equals(scene) && (recActor != null || countdown > 0)) {
            tell(p, "cinematic.studio.busy");
            return true;
        }
        if (sceneName.equals(scene)) {
            resetPlayback();
            broadcastState();
        }
        return false;
    }

    private static String cleanName(String s) {
        String out = s == null ? "" : s.replaceAll("[\\p{Cntrl}]", "").trim();
        return out.length() > 32 ? out.substring(0, 32) : out;
    }

    /** Moves (and turns) a whole take so it starts where the director stands, facing where they face. */
    private void moveHere(StudioData.Actor a, ServerPlayer p) {
        Track t = a.track;
        if (t.isEmpty()) {
            a.dim = p.serverLevel().dimension();
            Entity e = entity(a);
            if (e != null && e.level() == p.level()) {
                e.moveTo(p.getX(), p.getY(), p.getZ(), p.getYRot(), 0);
                if (e instanceof ActorEntity ae) {
                    ae.yHeadRot = ae.yBodyRot = p.getYRot();
                }
            } else {
                removeEntity(a);
                spawn(a, p.serverLevel(), p.getX(), p.getY(), p.getZ(), p.getYRot());
            }
            return;
        }
        if (!t.blocks.isEmpty()) {
            tell(p, "cinematic.studio.cannot_move_blocks");
            return;
        }
        Frame f0 = t.frames.get(0);
        double x0 = f0.x, y0 = f0.y, z0 = f0.z;
        float d = Mth.wrapDegrees(p.getYRot() - f0.yaw);
        double cos = Math.cos(Math.toRadians(d)), sin = Math.sin(Math.toRadians(d));
        for (Frame f : t.frames) {
            double rx = f.x - x0, rz = f.z - z0;
            double nx = rx * cos - rz * sin, nz = rx * sin + rz * cos;
            double ny = f.y - y0;
            f.x = p.getX() + nx;
            f.y = p.getY() + ny;
            f.z = p.getZ() + nz;
            f.yaw += d;
            f.headYaw += d;
            f.bodyYaw += d;
        }
        if (a.dim != p.serverLevel().dimension()) {
            removeEntity(a);
            a.dim = p.serverLevel().dimension();
            spawn(a, p.serverLevel(), p.getX(), p.getY(), p.getZ(), p.getYRot());
        } else if (entity(a) == null) {
            spawn(a, p.serverLevel(), p.getX(), p.getY(), p.getZ(), p.getYRot());
        }
        sendTrack(a, null);
    }

    // ------------------------------------------------------------------ entities

    private Entity entity(StudioData.Actor a) {
        if (a.entity == null) return null;
        ServerLevel level = server.getLevel(a.dim);
        return level == null ? null : level.getEntity(a.entity);
    }

    private void spawn(StudioData.Actor a, ServerLevel level, double x, double y, double z, float yaw) {
        ActorEntity e = StudioRegistry.ACTOR.get().create(level);
        if (e == null) return;
        e.setActorId(a.id);
        e.setSkin(a.skin);
        e.setSlim(a.slim);
        e.moveTo(x, y, z, yaw, 0);
        e.yHeadRot = e.yBodyRot = yaw;
        level.addFreshEntity(e);
        a.entity = e.getUUID();
    }

    /** Respawns an actor whose entity was lost (killed by a command, never saved...), where its take starts. */
    private void ensureEntity(StudioData.Actor a, ServerPlayer p) {
        Entity e = entity(a);
        if (e instanceof ActorEntity ae) {
            ae.setSkin(a.skin);
            ae.setSlim(a.slim);
            return;
        }
        ServerLevel level = server.getLevel(a.dim);
        if (level == null) return;
        if (a.track.isEmpty()) {
            spawn(a, p.serverLevel(), p.getX(), p.getY(), p.getZ(), p.getYRot());
            a.dim = p.serverLevel().dimension();
        } else {
            Frame f = a.track.frame(0);
            if (level.isLoaded(BlockPos.containing(f.x, f.y, f.z))) spawn(a, level, f.x, f.y, f.z, f.yaw);
        }
        data().setDirty();
    }

    private void removeEntity(StudioData.Actor a) {
        Entity e = entity(a);
        if (e != null) e.discard();
        a.entity = null;
    }

    // ------------------------------------------------------------------ blocks

    /** Puts every block the scene's actors changed back as it was before the scene. */
    private void restoreBlocks(StudioData.Scene s, UUID skip) {
        for (StudioData.Actor a : s.actors) {
            ServerLevel level = server.getLevel(a.dim);
            if (level == null) continue;
            List<BlockChange> list = new ArrayList<>(a.track.blocks);
            list.sort(Comparator.comparingInt(BlockChange::tick).reversed());
            for (BlockChange c : list) level.setBlock(c.pos(), c.before(), BLOCK_FLAGS);
        }
    }

    private void applyBlocksUpTo(StudioData.Scene s, int upTo) {
        for (StudioData.Actor a : s.actors) {
            ServerLevel level = server.getLevel(a.dim);
            if (level == null) continue;
            for (BlockChange c : a.track.blocks) {
                if (c.tick() <= upTo) level.setBlock(c.pos(), c.after(), BLOCK_FLAGS);
            }
        }
    }

    private void applyBlocksAt(StudioData.Scene s, int at) {
        for (StudioData.Actor a : s.actors) {
            if (a.id.equals(recActor)) continue;
            ServerLevel level = server.getLevel(a.dim);
            if (level == null) continue;
            for (BlockChange c : a.track.blocks) {
                if (c.tick() != at) continue;
                level.setBlock(c.pos(), c.after(), BLOCK_FLAGS);
                if (c.after().isAir()) {
                    level.levelEvent(2001, c.pos(), Block.getId(c.before()));
                } else {
                    level.playSound(null, c.pos(), c.after().getSoundType().getPlaceSound(), SoundSource.BLOCKS, 1f, 0.8f);
                }
            }
        }
    }

    private boolean recordingBy(Entity e) {
        return recActor != null && playing && e != null && e.getUUID().equals(recorder);
    }

    private void onBreak(BlockEvent.BreakEvent e) {
        if (recordingBy(e.getPlayer())) pending.putIfAbsent(e.getPos().immutable(), e.getState());
    }

    private void onPlace(BlockEvent.EntityPlaceEvent e) {
        if (!recordingBy(e.getEntity())) return;
        if (e instanceof BlockEvent.EntityMultiPlaceEvent multi) {
            for (BlockSnapshot snap : multi.getReplacedBlockSnapshots()) pending.putIfAbsent(snap.getPos().immutable(), snap.getReplacedBlock());
        } else {
            pending.putIfAbsent(e.getPos().immutable(), e.getBlockSnapshot().getReplacedBlock());
        }
    }

    private void resolvePending() {
        if (pending.isEmpty()) return;
        ServerPlayer p = server.getPlayerList().getPlayer(recorder);
        if (p != null) {
            for (Map.Entry<BlockPos, BlockState> en : pending.entrySet()) {
                BlockState now = p.serverLevel().getBlockState(en.getKey());
                if (!now.equals(en.getValue())) recBlocks.add(new BlockChange(tick, en.getKey(), en.getValue(), now));
            }
        }
        pending.clear();
    }

    // ------------------------------------------------------------------ ticking

    private void onServerTick(TickEvent.ServerTickEvent e) {
        if (e.phase != TickEvent.Phase.END || server == null || scene == null) return;
        StudioData.Scene s = data().scenes.get(scene);
        if (s == null) {
            resetPlayback();
            broadcastState();
            return;
        }
        if (countdown > 0) {
            countdown--;
            if (countdown == 0) {
                playing = true;
                tick = startTick;
                broadcastState();
            }
            return;
        }
        if (!playing) return;
        tick++;
        if (recActor != null) {
            resolvePending();
            applyBlocksAt(s, tick);
            if (tick >= StudioConfig.MAX_TAKE_SECONDS.get() * 20) finishRecording();
        } else {
            applyBlocksAt(s, tick);
            if (tick > s.length() + 20) {
                resetPlayback();
                broadcastState();
            }
        }
    }

    private void finishRecording() {
        resolvePending();
        uploadActor = recActor;
        uploader = recorder;
        uploadBlocks = new ArrayList<>(recBlocks);
        resetPlayback();
        broadcastState();
    }

    // ------------------------------------------------------------------ uploads (takes, skins)

    public void onUpload(ServerPlayer p, UploadMsg msg) {
        if (server == null) return;
        if (msg.kind() == UploadMsg.SKIN_REQUEST) {
            sendSkin(p, msg.key());
            return;
        }
        if (!canDirect(p)) return;
        int max = msg.kind() == UploadMsg.SKIN ? Skins.MAX_BYTES : 16 * 1024 * 1024;
        if (msg.total() < 0 || msg.total() > max) return;
        String key = p.getUUID() + "/" + msg.kind() + "/" + msg.key();
        Net.Assembler as = uploads.get(key);
        if (msg.offset() == 0 || as == null || as.total() != msg.total()) {
            as = new Net.Assembler(msg.total());
            uploads.put(key, as);
        }
        byte[] all;
        try {
            all = as.add(msg.offset(), msg.data());
        } catch (IllegalArgumentException ex) {
            uploads.remove(key);
            return;
        }
        if (all == null) return;
        uploads.remove(key);
        if (msg.kind() == UploadMsg.SKIN) receiveSkin(p, msg.key(), all);
        else if (msg.kind() == UploadMsg.TAKE) receiveTake(p, msg.key(), all);
    }

    private void receiveTake(ServerPlayer p, String key, byte[] bytes) {
        UUID id;
        try {
            id = UUID.fromString(key);
        } catch (IllegalArgumentException e) {
            return;
        }
        if (!id.equals(uploadActor) || !p.getUUID().equals(uploader)) return;
        StudioData.Actor a = data().actor(id);
        uploadActor = uploader = null;
        if (a == null) return;
        Track t;
        try {
            t = Track.fromBytes(bytes);
        } catch (RuntimeException e) {
            LOG.warn("Cinematic: unreadable take from {}", p.getGameProfile().getName(), e);
            tell(p, "cinematic.studio.take_failed");
            return;
        }
        int max = StudioConfig.MAX_TAKE_SECONDS.get() * 20;
        while (t.frames.size() > max) t.frames.remove(t.frames.size() - 1);
        t.blocks.clear();
        t.blocks.addAll(uploadBlocks);
        t.poseKeys.addAll(a.track.poseKeys);
        a.track = t;
        a.dim = p.serverLevel().dimension();
        data().setDirty();
        sendTrack(a, null);
        sync();
        tell(p, "cinematic.studio.take_saved", a.name, String.format(java.util.Locale.ROOT, "%.1f", t.length() / 20f), t.blocks.size());
    }

    private Path skinDir() {
        return server.getWorldPath(LevelResource.ROOT).resolve("cinematic_skins");
    }

    private Path skinFile(String hash) {
        return skinDir().resolve(hash + ".png");
    }

    private void receiveSkin(ServerPlayer p, String hash, byte[] bytes) {
        String problem = Skins.problem(bytes, StudioConfig.MAX_SKIN_PIXELS.get());
        if (problem != null) {
            tell(p, problem);
            return;
        }
        if (!Skins.sha1(bytes).equals(hash)) return;
        try {
            Files.createDirectories(skinDir());
            Files.write(skinFile(hash), bytes);
        } catch (IOException e) {
            LOG.error("Cinematic: could not store skin {}", hash, e);
        }
    }

    private void sendSkin(ServerPlayer p, String hash) {
        if (!Skins.validHash(hash)) return;
        Path f = skinFile(hash);
        if (!Files.isRegularFile(f)) return;
        try {
            byte[] bytes = Files.readAllBytes(f);
            Net.chunks(bytes, Net.DOWN_CHUNK, (off, piece) -> Net.toPlayer(p, new DownloadMsg(DownloadMsg.SKIN, hash, bytes.length, off, piece)));
        } catch (IOException e) {
            LOG.error("Cinematic: could not read skin {}", hash, e);
        }
    }

    public void onPoseKeys(ServerPlayer p, PoseKeysMsg msg) {
        if (server == null || !canDirect(p)) return;
        StudioData.Actor a = data().actor(msg.actor());
        if (a == null) return;
        a.track.poseKeys.clear();
        a.track.poseKeys.addAll(msg.keys());
        a.track.sortKeys();
        data().setDirty();
        sendTrack(a, null);
        sync();
        tell(p, "cinematic.pose.saved", a.name);
    }

    // ------------------------------------------------------------------ keeping the clients up to date

    private void onLogin(PlayerEvent.PlayerLoggedInEvent e) {
        if (server == null || !(e.getEntity() instanceof ServerPlayer p)) return;
        Net.toPlayer(p, syncFor(p));
        for (StudioData.Scene s : data().scenes.values()) {
            for (StudioData.Actor a : s.actors) sendTrack(a, p);
        }
        Net.toPlayer(p, state());
    }

    private void onLogout(PlayerEvent.PlayerLoggedOutEvent e) {
        if (server == null) return;
        UUID id = e.getEntity().getUUID();
        uploads.keySet().removeIf(k -> k.startsWith(id + "/"));
        if (id.equals(recorder)) {
            resetPlayback();
            broadcastState();
        }
        if (id.equals(uploader)) uploadActor = uploader = null;
    }

    private SyncMsg syncFor(ServerPlayer p) {
        List<SyncMsg.SceneInfo> scenes = new ArrayList<>();
        for (StudioData.Scene s : data().scenes.values()) {
            List<SyncMsg.ActorInfo> actors = new ArrayList<>();
            for (StudioData.Actor a : s.actors) {
                actors.add(new SyncMsg.ActorInfo(a.id, a.name, a.skin, a.slim, a.track.length(), a.track.blocks.size(), a.track.poseKeys.size()));
            }
            scenes.add(new SyncMsg.SceneInfo(s.name, actors));
        }
        return new SyncMsg(canDirect(p), scenes);
    }

    private void sync() {
        for (ServerPlayer p : server.getPlayerList().getPlayers()) Net.toPlayer(p, syncFor(p));
    }

    /** Sends an actor's take to one player, or to everyone when {@code to} is null. */
    private void sendTrack(StudioData.Actor a, ServerPlayer to) {
        byte[] bytes = a.track.toBytes();
        String key = a.id.toString();
        Net.chunks(bytes, Net.DOWN_CHUNK, (off, piece) -> {
            DownloadMsg m = new DownloadMsg(DownloadMsg.TAKE, key, bytes.length, off, piece);
            if (to != null) Net.toPlayer(to, m);
            else Net.toAll(server, m);
        });
    }

    private PlayStateMsg state() {
        if (scene == null) return PlayStateMsg.idle();
        return new PlayStateMsg(scene, playing, playing ? tick : startTick, server.overworld().getGameTime(), countdown, recActor, recorder);
    }

    private void broadcastState() {
        if (server != null) Net.toAll(server, state());
    }
}
