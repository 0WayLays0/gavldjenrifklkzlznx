package com.waylays.cinematic.client;

import com.waylays.cinematic.net.ActionMsg;
import com.waylays.cinematic.net.DownloadMsg;
import com.waylays.cinematic.net.Net;
import com.waylays.cinematic.net.PlayStateMsg;
import com.waylays.cinematic.net.PoseKeysMsg;
import com.waylays.cinematic.net.SyncMsg;
import com.waylays.cinematic.net.UploadMsg;
import com.waylays.cinematic.studio.ActorEntity;
import com.waylays.cinematic.studio.BodyPart;
import com.waylays.cinematic.studio.Emote;
import com.waylays.cinematic.studio.Frame;
import com.waylays.cinematic.studio.FrameSource;
import com.waylays.cinematic.studio.PoseKey;
import com.waylays.cinematic.studio.Track;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The client's view of the studio: the scenes and takes the server sent, what is playing, the local timeline cursor
 * (to preview and edit without the server), the performer's recorder and the pose editor's working copy.
 */
public final class ClientStudio implements FrameSource {
    public static final ClientStudio INSTANCE = new ClientStudio();

    private static boolean canDirect;
    private static List<SyncMsg.SceneInfo> scenes = List.of();
    private static final Map<UUID, SyncMsg.ActorInfo> actors = new HashMap<>();
    private static final Map<UUID, String> actorScene = new HashMap<>();
    private static final Map<UUID, Track> tracks = new HashMap<>();
    private static final Map<String, Net.Assembler> downloads = new HashMap<>();
    private static PlayStateMsg play = PlayStateMsg.idle();

    public static String selectedScene = "";
    public static float cursor;
    public static boolean previewing;
    /** The actor F8's automatic camera films instead of the player (null = the player). */
    public static UUID autoTarget;
    /** Gesture recorded into the take from now on. */
    public static int emote;

    // Pose editor
    private static UUID editActor;
    private static List<PoseKey> editKeys;
    private static BodyPart livePart;
    private static float[] liveValue;

    // Recording
    private static UUID recActor;
    private static Track recTrack;

    private ClientStudio() {}

    public static void init() {
        ActorEntity.clientSource = INSTANCE;
    }

    public static void reset() {
        canDirect = false;
        scenes = List.of();
        actors.clear();
        actorScene.clear();
        tracks.clear();
        downloads.clear();
        play = PlayStateMsg.idle();
        previewing = false;
        cursor = 0;
        recActor = null;
        recTrack = null;
        editActor = null;
        editKeys = null;
        liveValue = null;
        autoTarget = null;
        emote = 0;
    }

    // ------------------------------------------------------------------ queries

    public static boolean serverHasMod() {
        Minecraft mc = Minecraft.getInstance();
        try {
            return mc.getConnection() != null && Net.CHANNEL.isRemotePresent(mc.getConnection().getConnection());
        } catch (RuntimeException e) {
            return false;
        }
    }

    public static boolean canDirect() {
        return canDirect;
    }

    public static List<SyncMsg.SceneInfo> scenes() {
        return scenes;
    }

    public static SyncMsg.SceneInfo scene(String name) {
        for (SyncMsg.SceneInfo s : scenes) if (s.name().equals(name)) return s;
        return null;
    }

    public static SyncMsg.ActorInfo actor(UUID id) {
        return id == null ? null : actors.get(id);
    }

    public static Track trackOf(UUID id) {
        return id == null ? null : tracks.get(id);
    }

    public static PlayStateMsg play() {
        return play;
    }

    public static boolean isRecording() {
        return play.recorder() != null;
    }

    public static boolean amRecording() {
        Minecraft mc = Minecraft.getInstance();
        return mc.player != null && mc.player.getUUID().equals(play.recorder());
    }

    public static boolean serverPlaying() {
        return play.playing() && play.recorder() == null && !play.scene().isEmpty();
    }

    /** Length in ticks of a scene: its longest take, or its last camera point. */
    public static int sceneLength(String name) {
        int len = 0;
        SyncMsg.SceneInfo s = scene(name);
        if (s != null) {
            for (SyncMsg.ActorInfo a : s.actors()) {
                Track t = tracks.get(a.id());
                len = Math.max(len, t != null ? t.length() : a.length());
            }
        }
        return Math.max(len, CameraTrack.forScene(name).lastTick());
    }

    /** The scene the cameras follow: the one playing on the server, else the selected one. */
    public static String activeScene() {
        return serverPlaying() ? play.scene() : selectedScene;
    }

    /** Fractional time (ticks) of the active scene. */
    public static float sceneTime(float partialTick) {
        Minecraft mc = Minecraft.getInstance();
        if (serverPlaying() && mc.level != null) return play.baseTick() + (mc.level.getGameTime() - play.baseGameTime()) + partialTick;
        return cursor + (previewing ? partialTick : 0f);
    }

    /** Fractional time (ticks) an actor is at. */
    public static float actorTime(UUID id, float partialTick) {
        String sc = actorScene.get(id);
        if (sc == null) return 0;
        Minecraft mc = Minecraft.getInstance();
        if (sc.equals(play.scene())) {
            if (play.playing() && mc.level != null) return play.baseTick() + (mc.level.getGameTime() - play.baseGameTime()) + partialTick;
            if (play.countdown() > 0 || play.recorder() != null) return play.baseTick();
        }
        if (sc.equals(selectedScene)) return cursor + (previewing ? partialTick : 0f);
        return 0;
    }

    @Override
    public Track track(UUID actor) {
        return tracks.get(actor);
    }

    @Override
    public int tick(UUID actor) {
        return (int) Math.floor(actorTime(actor, 0f));
    }

    /** The actor being recorded is replaced by its performer: hide it. */
    public static boolean hidden(ActorEntity e) {
        UUID id = e.actorId();
        return id != null && id.equals(play.recordingActor());
    }

    public static ActorEntity findEntity(UUID id) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || id == null) return null;
        for (Entity e : mc.level.entitiesForRendering()) {
            if (e instanceof ActorEntity a && id.equals(a.actorId())) return a;
        }
        return null;
    }

    // ------------------------------------------------------------------ pose editing

    public static void beginEdit(UUID actor) {
        editActor = actor;
        Track t = tracks.get(actor);
        editKeys = t == null ? new ArrayList<>() : new ArrayList<>(t.poseKeys);
        liveValue = null;
    }

    public static void endEdit() {
        editActor = null;
        editKeys = null;
        liveValue = null;
    }

    public static List<PoseKey> editKeys() {
        return editKeys;
    }

    public static UUID editActor() {
        return editActor;
    }

    public static void setLive(BodyPart part, float[] value) {
        livePart = part;
        liveValue = value;
    }

    public static void saveEdit() {
        if (editActor != null && editKeys != null) Net.CHANNEL.sendToServer(new PoseKeysMsg(editActor, new ArrayList<>(editKeys)));
    }

    /** Pose offset (degrees) of a limb, from the take's keys or the editor's working copy. */
    public static boolean poseOffset(UUID actor, BodyPart part, float time, float[] out) {
        if (actor == null) return false;
        if (actor.equals(editActor)) {
            if (liveValue != null && part == livePart) {
                System.arraycopy(liveValue, 0, out, 0, 3);
                return true;
            }
            return Track.poseOffset(editKeys, part, time, out);
        }
        Track t = tracks.get(actor);
        return t != null && t.poseOffset(part, time, out);
    }

    // ------------------------------------------------------------------ orders

    public static void send(ActionMsg.Action action, UUID actor, String text, int number) {
        Net.CHANNEL.sendToServer(new ActionMsg(action, selectedScene, actor, text, number));
    }

    public static void playOnServer() {
        previewing = false;
        send(ActionMsg.Action.PLAY, null, "", Math.round(cursor));
    }

    public static void stopOnServer() {
        send(ActionMsg.Action.STOP, null, "", 0);
    }

    // ------------------------------------------------------------------ ticking

    public static void tick(Minecraft mc) {
        if (previewing) {
            int len = sceneLength(selectedScene);
            cursor += 1f;
            if (cursor >= len) {
                cursor = len;
                previewing = false;
            }
        }
        if (recTrack != null && mc.player != null) capture(mc.player);
    }

    private static void capture(LocalPlayer p) {
        Frame f = new Frame();
        f.x = p.getX();
        f.y = p.getY();
        f.z = p.getZ();
        f.yaw = p.getYRot();
        f.pitch = p.getXRot();
        f.headYaw = p.getYHeadRot();
        f.bodyYaw = p.yBodyRot;
        f.pose = Frame.poseId(p.getPose());
        if (p.onGround()) f.flags |= Frame.GROUND;
        if (p.isSprinting()) f.flags |= Frame.SPRINT;
        if (p.swingingArm == InteractionHand.OFF_HAND) f.flags |= Frame.SWING_OFFHAND;
        if (p.isUsingItem()) {
            f.flags |= Frame.USING;
            if (p.getUsedItemHand() == InteractionHand.OFF_HAND) f.flags |= Frame.USE_OFFHAND;
            f.useTicks = p.getTicksUsingItem();
        }
        if (p.isFallFlying()) f.flags |= Frame.FLYING;
        f.attack = Math.round(Math.max(0f, Math.min(1f, p.attackAnim)) * 255f);
        f.emote = emote;
        EquipmentSlot[] slots = EquipmentSlot.values();
        for (int s = 0; s < Frame.SLOTS && s < slots.length; s++) f.equip[s] = recTrack.itemIndex(p.getItemBySlot(slots[s]));
        recTrack.frames.add(f);
    }

    public static int recordedTicks() {
        return recTrack == null ? 0 : recTrack.length();
    }

    private static void finishRecording() {
        if (recTrack == null) return;
        Track t = recTrack;
        UUID actor = recActor;
        recTrack = null;
        recActor = null;
        emote = 0;
        byte[] bytes = t.toBytes();
        Net.chunks(bytes, Net.UP_CHUNK, (off, piece) ->
                Net.CHANNEL.sendToServer(new UploadMsg(UploadMsg.TAKE, actor.toString(), bytes.length, off, piece)));
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) mc.player.displayClientMessage(Component.translatable("cinematic.studio.take_sending"), true);
    }

    // ------------------------------------------------------------------ packets

    public static void onSync(SyncMsg msg) {
        canDirect = msg.canDirect();
        scenes = List.copyOf(msg.scenes());
        actors.clear();
        actorScene.clear();
        for (SyncMsg.SceneInfo s : scenes) {
            for (SyncMsg.ActorInfo a : s.actors()) {
                actors.put(a.id(), a);
                actorScene.put(a.id(), s.name());
            }
        }
        tracks.keySet().removeIf(id -> !actors.containsKey(id));
        if (scene(selectedScene) == null) {
            selectedScene = scenes.isEmpty() ? "" : scenes.get(0).name();
            cursor = 0;
        }
        if (Minecraft.getInstance().screen instanceof StudioScreen s) s.refresh();
        if (Minecraft.getInstance().screen instanceof ActorScreen s) s.refresh();
    }

    public static void onDownload(DownloadMsg msg) {
        if (msg.kind() == DownloadMsg.SKIN) {
            SkinCache.receive(msg);
            return;
        }
        String key = msg.kind() + "/" + msg.key();
        Net.Assembler as = downloads.get(key);
        if (msg.offset() == 0 || as == null || as.total() != msg.total()) {
            as = new Net.Assembler(msg.total());
            downloads.put(key, as);
        }
        byte[] all;
        try {
            all = as.add(msg.offset(), msg.data());
        } catch (IllegalArgumentException e) {
            downloads.remove(key);
            return;
        }
        if (all == null) return;
        downloads.remove(key);
        try {
            UUID id = UUID.fromString(msg.key());
            tracks.put(id, Track.fromBytes(all));
            if (id.equals(editActor)) beginEdit(id);
        } catch (RuntimeException ignored) {
            // A take this version cannot read: the actor stays where the server puts it
        }
    }

    public static void onPlayState(PlayStateMsg msg) {
        play = msg;
        Minecraft mc = Minecraft.getInstance();
        boolean me = mc.player != null && mc.player.getUUID().equals(msg.recorder());
        if (me && msg.playing() && recTrack == null) {
            recActor = msg.recordingActor();
            recTrack = new Track();
        } else if (recTrack != null && !(me && msg.playing())) {
            finishRecording();
        }
        if (!msg.scene().isEmpty()) {
            previewing = false;
            if (scene(msg.scene()) != null) selectedScene = msg.scene();
        }
    }

    public static String emoteName() {
        return Emote.byId(emote).key();
    }
}
