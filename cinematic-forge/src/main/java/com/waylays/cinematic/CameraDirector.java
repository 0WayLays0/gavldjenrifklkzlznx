package com.waylays.cinematic;

import com.mojang.logging.LogUtils;
import net.minecraft.client.Camera;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.ViewportEvent;
import org.slf4j.Logger;

import java.lang.reflect.Method;

/**
 * Two cameras.
 * Normal play: the view is smoothed, rolls into turns and strafes, breathes a little, and the FOV eases.
 * Cinematic camera (F8): a director films the player through a sequence of shots and cuts between them.
 */
public final class CameraDirector {
    private static final Logger LOG = LogUtils.getLogger();

    enum Shot {
        ORBIT(62), LOW_HERO(48), CRANE(66), TRIPOD(42), SHOULDER(72), FRONT_TRACK(55);
        final float fov;
        Shot(float fov) { this.fov = fov; }
    }

    private static boolean active;
    private static int shotIndex;
    private static int shotTicks;
    private static float yaw0;
    private static Vec3 tripodPos;
    private static Vec3 camPos;
    private static boolean snap;
    private static float focus = -1f;
    private static CameraType previousType;
    private static boolean previousHideGui;

    // Normal play smoothing
    private static long lastNanos;
    private static float clock;
    private static boolean fpInit;
    private static float smoothYaw, smoothPitch, roll, lastTargetYaw, yawSpeed;
    private static double smoothFov = -1;
    private static long lastFovNanos;

    private static Method setPosition;
    private static boolean setPositionMissing;

    private CameraDirector() {}

    public static boolean active() { return active; }

    /** Linear distance (blocks) the lens focuses on, or -1 for autofocus on the screen centre. */
    public static float focusDistance() { return active ? focus : -1f; }

    static void toggle(Minecraft mc) {
        if (active) stop(mc);
        else start(mc);
    }

    static void start(Minecraft mc) {
        if (mc.player == null || active) return;
        active = true;
        previousType = mc.options.getCameraType();
        previousHideGui = mc.options.hideGui;
        shotIndex = -1;
        nextShot(mc, false);
        FilmLook.fadeFromBlack(0.9f);
    }

    static void stop(Minecraft mc) {
        if (!active) return;
        active = false;
        if (previousType != null) mc.options.setCameraType(previousType);
        mc.options.hideGui = previousHideGui;
        fpInit = false;
    }

    static void nextShot(Minecraft mc, boolean manual) {
        if (mc.player == null) return;
        Shot[] shots = Shot.values();
        shotIndex = (shotIndex + 1) % shots.length;
        shotTicks = 0;
        snap = true;
        yaw0 = mc.player.getYRot();
        if (shots[shotIndex] == Shot.TRIPOD) {
            Vec3 eye = mc.player.getEyePosition();
            tripodPos = clip(mc.player, eye, mc.player.position().add(dir(yaw0 + 140f).scale(9.0)).add(0, 2.6, 0));
        }
        if (manual) FilmLook.dip();
    }

    static void tick(Minecraft mc) {
        if (!active) return;
        if (mc.player == null || mc.level == null) {
            stop(mc);
            return;
        }
        shotTicks++;
        if (mc.options.getCameraType() != CameraType.THIRD_PERSON_BACK) mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
        if (CinematicConfig.SHOT_HIDES_HUD.get()) mc.options.hideGui = true;
        double seconds = CinematicConfig.SHOT_SECONDS.get();
        boolean tripodLost = Shot.values()[shotIndex] == Shot.TRIPOD && tripodPos != null
                && tripodPos.distanceTo(mc.player.position()) > 28.0;
        if ((seconds > 0 && shotTicks >= seconds * 20.0) || tripodLost) nextShot(mc, false);
    }

    static void onCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        long now = System.nanoTime();
        float dt = lastNanos == 0 ? 0f : Math.min((now - lastNanos) / 1.0e9f, 0.1f);
        lastNanos = now;
        clock = (clock + dt) % 3600f;
        Camera camera = event.getCamera();
        Entity entity = camera.getEntity();
        float pt = (float) event.getPartialTick();
        if (entity == null) return;
        if (active) {
            directShot(event, camera, entity, pt, dt);
        } else {
            smoothPlay(event, entity, dt);
        }
    }

    // ---------------------------------------------------------------- normal play

    private static void smoothPlay(ViewportEvent.ComputeCameraAngles event, Entity entity, float dt) {
        float targetYaw = event.getYaw();
        float targetPitch = event.getPitch();
        boolean firstPerson = Minecraft.getInstance().options.getCameraType().isFirstPerson();
        if (!fpInit || dt <= 0f) {
            smoothYaw = targetYaw;
            smoothPitch = targetPitch;
            lastTargetYaw = targetYaw;
            fpInit = true;
        }
        if (dt > 0f) {
            yawSpeed = Mth.lerp(1f - (float) Math.exp(-10f * dt), yawSpeed, Mth.wrapDegrees(targetYaw - lastTargetYaw) / dt);
        }
        lastTargetYaw = targetYaw;

        if (CinematicConfig.SMOOTH_CAMERA.get() && firstPerson) {
            float a = 1f - (float) Math.exp(-CinematicConfig.CAMERA_SMOOTHING.get().floatValue() * dt);
            float dy = Mth.wrapDegrees(targetYaw - smoothYaw);
            if (Math.abs(dy) > 120f) a = 1f;
            smoothYaw = targetYaw - dy * (1f - a);
            smoothPitch = Mth.lerp(a, smoothPitch, targetPitch);
            event.setYaw(smoothYaw);
            event.setPitch(smoothPitch);
        } else {
            smoothYaw = targetYaw;
            smoothPitch = targetPitch;
        }

        float rollTarget = 0f;
        if (CinematicConfig.TILT.get()) {
            rollTarget = Mth.clamp(-yawSpeed * 0.012f, -5f, 5f);
            if (entity instanceof LivingEntity living) {
                rollTarget += -living.xxa * 1.6f;
                if (living.hurtTime > 0) rollTarget += Mth.sin(living.hurtTime * 1.3f) * living.hurtTime * 0.35f;
            }
        }
        roll = Mth.lerp(1f - (float) Math.exp(-6f * dt), roll, rollTarget);
        float r = event.getRoll() + roll;
        if (CinematicConfig.HANDHELD.get()) {
            event.setYaw(event.getYaw() + 0.16f * Mth.sin(clock * 0.9f) + 0.07f * Mth.sin(clock * 2.3f + 1f));
            event.setPitch(event.getPitch() + 0.12f * Mth.sin(clock * 1.1f + 2f) + 0.05f * Mth.sin(clock * 2.9f));
            r += 0.15f * Mth.sin(clock * 0.7f + 4f);
        }
        event.setRoll(r);
    }

    static void onFov(ViewportEvent.ComputeFov event) {
        long now = System.nanoTime();
        float dt = lastFovNanos == 0 ? 0f : Math.min((now - lastFovNanos) / 1.0e9f, 0.1f);
        lastFovNanos = now;
        double target = event.getFOV();
        if (active) {
            // Lens of the current shot, scaled with the player's own FOV setting
            double base = Minecraft.getInstance().options.fov().get();
            target = Shot.values()[shotIndex].fov * (base / 70.0);
        } else if (!CinematicConfig.SMOOTH_FOV.get()) {
            smoothFov = target;
            return;
        }
        if (smoothFov < 0 || (active && snap)) smoothFov = target;
        else smoothFov = Mth.lerp(1f - Math.exp(-(active ? 2.5 : 7.0) * dt), smoothFov, target);
        event.setFOV(smoothFov);
    }

    // ---------------------------------------------------------------- cinematic camera

    private static void directShot(ViewportEvent.ComputeCameraAngles event, Camera camera, Entity e, float pt, float dt) {
        Shot shot = Shot.values()[shotIndex];
        float t = (shotTicks + pt) / 20f;
        Vec3 base = e.getPosition(pt);
        Vec3 eye = e.getEyePosition(pt);
        Vec3 lookAt = eye.add(0, -0.15, 0);
        Vec3 pos;
        Float fixedYaw = null, fixedPitch = null;
        switch (shot) {
            case ORBIT -> pos = base.add(dir(yaw0 + 180f + t * 14f).scale(6.0)).add(0, 2.2, 0);
            case LOW_HERO -> pos = base.add(dir(yaw0).scale(3.2 + t * 0.18)).add(dir(yaw0 + 90f).scale(1.2)).add(0, 0.25, 0);
            case CRANE -> pos = base.add(dir(yaw0 + 180f + t * 4f).scale(7.0 + t * 0.3)).add(0, 1.5 + Math.min(t, 10f) * 0.7, 0);
            case TRIPOD -> pos = tripodPos != null ? tripodPos : eye.add(0, 2, 0);
            case SHOULDER -> {
                float vy = e.getViewYRot(pt);
                pos = eye.add(dir(vy).scale(-2.6)).add(dir(vy + 90f).scale(-0.9)).add(0, 0.25, 0);
                fixedYaw = vy;
                fixedPitch = e.getViewXRot(pt) * 0.8f;
            }
            default -> {
                float by = e instanceof LivingEntity l ? Mth.rotLerp(pt, l.yBodyRotO, l.yBodyRot) : e.getViewYRot(pt);
                pos = eye.add(dir(by).scale(3.6)).add(0, 0.1, 0);
            }
        }
        if (shot != Shot.TRIPOD) pos = clip(e, eye, pos);

        if (snap || camPos == null) {
            camPos = pos;
            snap = false;
        } else {
            float speed = shot == Shot.SHOULDER ? 12f : 5f;
            camPos = camPos.lerp(pos, 1f - Math.exp(-speed * dt));
        }

        if (!moveCamera(camera, camPos)) {
            // Without the position hook the shots cannot work: fall back to the third-person view
            return;
        }
        Vec3 d = lookAt.subtract(camPos);
        float yaw = fixedYaw != null ? fixedYaw : (float) Math.toDegrees(Math.atan2(-d.x, d.z));
        float pitch = fixedPitch != null ? fixedPitch : (float) -Math.toDegrees(Math.atan2(d.y, Math.sqrt(d.x * d.x + d.z * d.z)));
        // Handheld drift and a slight dutch angle on the low shot
        yaw += 0.25f * Mth.sin(clock * 0.6f) + 0.1f * Mth.sin(clock * 1.7f);
        pitch += 0.2f * Mth.sin(clock * 0.8f + 1f);
        event.setYaw(yaw);
        event.setPitch(pitch);
        event.setRoll(shot == Shot.LOW_HERO ? 4f : 0.2f * Mth.sin(clock * 0.5f));
        focus = (float) camPos.distanceTo(eye);
    }

    private static boolean moveCamera(Camera camera, Vec3 pos) {
        if (setPositionMissing) return false;
        try {
            if (setPosition == null) {
                // Camera.setPosition(Vec3) is protected: find it by signature, independent of the obfuscated name
                for (Method m : Camera.class.getDeclaredMethods()) {
                    Class<?>[] p = m.getParameterTypes();
                    if (p.length == 1 && p[0] == Vec3.class && m.getReturnType() == void.class) {
                        m.setAccessible(true);
                        setPosition = m;
                        break;
                    }
                }
                if (setPosition == null) throw new NoSuchMethodException("Camera.setPosition(Vec3)");
            }
            setPosition.invoke(camera, pos);
            return true;
        } catch (ReflectiveOperationException | RuntimeException ex) {
            LOG.error("Cinematic: cannot move the camera, cinematic shots are disabled", ex);
            setPositionMissing = true;
            return false;
        }
    }

    private static Vec3 clip(Entity e, Vec3 from, Vec3 to) {
        HitResult hit = e.level().clip(new ClipContext(from, to, ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, e));
        if (hit.getType() == HitResult.Type.MISS) return to;
        Vec3 back = from.subtract(hit.getLocation());
        double len = back.length();
        return len < 1.0e-4 ? hit.getLocation() : hit.getLocation().add(back.scale(Math.min(0.3, len) / len));
    }

    /** Horizontal unit vector of a Minecraft yaw (0 = south, 90 = west). */
    private static Vec3 dir(float yawDeg) {
        float r = yawDeg * Mth.DEG_TO_RAD;
        return new Vec3(-Mth.sin(r), 0, Mth.cos(r));
    }
}
