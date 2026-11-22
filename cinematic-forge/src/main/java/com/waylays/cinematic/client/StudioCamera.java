package com.waylays.cinematic.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.waylays.cinematic.CameraDirector;
import com.waylays.cinematic.studio.ActorEntity;
import net.minecraft.client.Camera;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.Input;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.MovementInputUpdateEvent;
import net.minecraftforge.client.event.ViewportEvent;
import org.lwjgl.glfw.GLFW;

/**
 * The studio's cameras, in priority order: the pose editor's orbit around the actor, the free camera (fly anywhere,
 * the player stays put), and the film's camera track while a scene plays or is previewed.
 */
public final class StudioCamera {
    // Free camera
    private static boolean free;
    private static Vec3 freePos = Vec3.ZERO, vel = Vec3.ZERO;
    private static float freeYaw, freePitch, freeRoll, freeFov = 70f, speed = 4f;
    private static float lockYaw, lockPitch;
    private static float inForward, inStrafe;
    private static boolean inUp, inDown;

    // Pose editor orbit
    private static boolean orbit;
    private static float orbitYaw = 200f, orbitPitch = 10f, orbitDist = 3.2f;

    // State forced on the game while a studio camera is on, restored after
    private static boolean forced;
    private static CameraType savedType;
    private static boolean savedHideGui;

    // The last view drawn, used for new camera points
    private static Vec3 lastPos = Vec3.ZERO;
    private static float lastYaw, lastPitch, lastRoll, lastFov = 70f;
    private static float focus = -1f;
    private static boolean filmActive;
    private static long lastNanos;

    public static boolean hideSelf = true;
    public static boolean filmCamera = true;

    private StudioCamera() {}

    public static boolean free() {
        return free;
    }

    public static float speed() {
        return speed;
    }

    public static float freeFov() {
        return freeFov;
    }

    public static float freeRoll() {
        return freeRoll;
    }

    public static float focus() {
        return focus;
    }

    /** The film's camera is showing the scene (letterbox, no HUD). */
    public static boolean filmActive() {
        return filmActive;
    }

    public static boolean anyActive() {
        return free || orbit || filmActive;
    }

    // ------------------------------------------------------------------ switching

    public static void toggleFree(Minecraft mc) {
        if (mc.player == null) return;
        free = !free;
        if (free) {
            Camera cam = mc.gameRenderer.getMainCamera();
            freePos = cam.getPosition();
            freeYaw = cam.getYRot();
            freePitch = cam.getXRot();
            freeRoll = 0f;
            freeFov = mc.options.fov().get();
            vel = Vec3.ZERO;
            // The player looks straight ahead while the mouse steers the camera
            lockYaw = mc.player.getYRot();
            lockPitch = 0f;
            mc.player.setXRot(0f);
        }
        mc.player.displayClientMessage(Component.translatable("cinematic.freecam." + (free ? "on" : "off")), true);
    }

    public static void setOrbit(boolean on) {
        orbit = on;
    }

    public static void orbitDrag(double dx, double dy) {
        orbitYaw += (float) dx * 0.6f;
        orbitPitch = Mth.clamp(orbitPitch + (float) dy * 0.5f, -80f, 80f);
    }

    public static void orbitZoom(double amount) {
        orbitDist = Mth.clamp(orbitDist - (float) amount * 0.3f, 1.2f, 12f);
    }

    public static void stopAll(Minecraft mc) {
        free = false;
        orbit = false;
        filmActive = false;
        tick(mc);
    }

    /** Each client tick: keeps the game in third person (no hand, the actors drawn) while a studio camera is on. */
    public static void tick(Minecraft mc) {
        filmActive = filmShouldPlay();
        boolean want = mc.player != null && (free || orbit || filmActive);
        if (want && !forced) {
            forced = true;
            savedType = mc.options.getCameraType();
            savedHideGui = mc.options.hideGui;
        }
        if (want) {
            if (mc.options.getCameraType() != CameraType.THIRD_PERSON_BACK) mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
            if (filmActive && !free && !orbit) mc.options.hideGui = true;
            else mc.options.hideGui = savedHideGui;
        } else if (forced) {
            forced = false;
            if (savedType != null) mc.options.setCameraType(savedType);
            mc.options.hideGui = savedHideGui;
        }
    }

    private static boolean filmShouldPlay() {
        if (!filmCamera || ClientStudio.isRecording()) return false;
        if (!ClientStudio.serverPlaying() && !ClientStudio.previewing) return false;
        return !CameraTrack.forScene(ClientStudio.activeScene()).shots.isEmpty();
    }

    // ------------------------------------------------------------------ input

    public static void onMovementInput(MovementInputUpdateEvent e) {
        if (!free) return;
        Input in = e.getInput();
        inForward = in.forwardImpulse;
        inStrafe = in.leftImpulse;
        inUp = in.jumping;
        inDown = in.shiftKeyDown;
        in.forwardImpulse = 0f;
        in.leftImpulse = 0f;
        in.up = in.down = in.left = in.right = false;
        in.jumping = false;
        in.shiftKeyDown = false;
    }

    public static void onScroll(InputEvent.MouseScrollingEvent e) {
        if (!free || Minecraft.getInstance().screen != null) return;
        long win = Minecraft.getInstance().getWindow().getWindow();
        double d = e.getScrollDelta();
        if (InputConstants.isKeyDown(win, GLFW.GLFW_KEY_LEFT_CONTROL) || InputConstants.isKeyDown(win, GLFW.GLFW_KEY_RIGHT_CONTROL)) {
            freeFov = Mth.clamp(freeFov - (float) d * 2f, 5f, 130f);
        } else if (InputConstants.isKeyDown(win, GLFW.GLFW_KEY_LEFT_ALT) || InputConstants.isKeyDown(win, GLFW.GLFW_KEY_RIGHT_ALT)) {
            freeRoll = Mth.clamp(freeRoll + (float) d * 2f, -90f, 90f);
        } else {
            speed = Mth.clamp(speed * (d > 0 ? 1.25f : 0.8f), 0.25f, 80f);
        }
        e.setCanceled(true);
    }

    public static void onInteract(InputEvent.InteractionKeyMappingTriggered e) {
        if (free) {
            e.setSwingHand(false);
            e.setCanceled(true);
        }
    }

    // ------------------------------------------------------------------ camera

    /** Returns true when a studio camera placed the view this frame. */
    public static boolean onCameraAngles(ViewportEvent.ComputeCameraAngles e) {
        long now = System.nanoTime();
        float dt = lastNanos == 0 ? 0f : Math.min((now - lastNanos) / 1.0e9f, 0.1f);
        lastNanos = now;
        focus = -1f;
        Minecraft mc = Minecraft.getInstance();
        Camera camera = e.getCamera();
        float pt = (float) e.getPartialTick();
        if (orbit) {
            ActorEntity a = ClientStudio.findEntity(ClientStudio.editActor());
            Entity target = a != null ? a : camera.getEntity();
            if (target == null) return false;
            Vec3 c = target.getPosition(pt).add(0, 1.0, 0);
            float yr = orbitYaw * Mth.DEG_TO_RAD, pr = orbitPitch * Mth.DEG_TO_RAD;
            Vec3 pos = c.add(-Mth.sin(yr) * Mth.cos(pr) * orbitDist, Mth.sin(pr) * orbitDist, Mth.cos(yr) * Mth.cos(pr) * orbitDist);
            Vec3 d = c.subtract(pos);
            place(e, camera, pos, (float) Math.toDegrees(Math.atan2(-d.x, d.z)),
                    (float) -Math.toDegrees(Math.atan2(d.y, Math.sqrt(d.x * d.x + d.z * d.z))), 0f, 70f);
            focus = orbitDist;
            return true;
        }
        if (free && mc.player != null) {
            LocalPlayer p = mc.player;
            freeYaw += p.getYRot() - lockYaw;
            freePitch = Mth.clamp(freePitch + p.getXRot() - lockPitch, -90f, 90f);
            p.setYRot(lockYaw);
            p.setXRot(lockPitch);
            p.yRotO = lockYaw;
            p.xRotO = lockPitch;
            float yr = freeYaw * Mth.DEG_TO_RAD, pr = freePitch * Mth.DEG_TO_RAD;
            Vec3 fwd = new Vec3(-Mth.sin(yr) * Mth.cos(pr), -Mth.sin(pr), Mth.cos(yr) * Mth.cos(pr));
            Vec3 left = new Vec3(Mth.cos(yr), 0, Mth.sin(yr));
            Vec3 want = fwd.scale(inForward).add(left.scale(inStrafe)).add(0, (inUp ? 1 : 0) - (inDown ? 1 : 0), 0);
            if (want.lengthSqr() > 1) want = want.normalize();
            vel = vel.lerp(want.scale(speed), 1f - Math.exp(-8f * dt));
            freePos = freePos.add(vel.scale(dt));
            place(e, camera, freePos, freeYaw, freePitch, freeRoll, freeFov);
            return true;
        }
        if (filmActive) {
            CameraTrack.View v = CameraTrack.forScene(ClientStudio.activeScene()).evaluate(ClientStudio.sceneTime(pt));
            if (v != null) {
                place(e, camera, v.pos, v.yaw, v.pitch, v.roll, v.fov);
                focus = v.focus;
                return true;
            }
        }
        return false;
    }

    private static void place(ViewportEvent.ComputeCameraAngles e, Camera camera, Vec3 pos, float yaw, float pitch, float roll, float fov) {
        CameraDirector.moveCamera(camera, pos);
        e.setYaw(yaw);
        e.setPitch(pitch);
        e.setRoll(roll);
        lastPos = pos;
        lastYaw = yaw;
        lastPitch = pitch;
        lastRoll = roll;
        lastFov = fov;
    }

    public static boolean onFov(ViewportEvent.ComputeFov e) {
        if (orbit || free || filmActive) {
            e.setFOV(lastFov);
            return true;
        }
        return false;
    }

    /** Remembers an ordinary view too, so a camera point can be taken from the player's own eyes. */
    public static void noteView(Camera camera, float fov) {
        lastPos = camera.getPosition();
        lastYaw = camera.getYRot();
        lastPitch = camera.getXRot();
        lastRoll = 0f;
        lastFov = fov;
    }

    public static CameraTrack.Key currentKey(int tick) {
        return CameraTrack.key(tick, lastPos, lastYaw, lastPitch, lastRoll, lastFov);
    }

    /** Puts the free camera where a camera point is (to adjust it). */
    public static void goTo(CameraTrack.Key k) {
        Minecraft mc = Minecraft.getInstance();
        if (!free) toggleFree(mc);
        freePos = new Vec3(k.x, k.y, k.z);
        freeYaw = k.yaw;
        freePitch = k.pitch;
        freeRoll = k.roll;
        freeFov = k.fov;
        vel = Vec3.ZERO;
    }
}
