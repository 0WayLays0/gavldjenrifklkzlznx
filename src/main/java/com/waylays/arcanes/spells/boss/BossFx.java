package com.waylays.arcanes.spells.boss;

import com.waylays.arcanes.entity.boss.BossTelegraph;
import com.waylays.arcanes.spells.SpellUtil;
import io.redspace.ironsspellbooks.particle.BlastwaveParticleOptions;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/** Shared effects of Boss Magic: colours, the outline of a warning zone, impacts, shockwaves. */
public final class BossFx {
    private BossFx() {}

    public static Vector3f color(int rgb) {
        return new Vector3f(((rgb >> 16) & 0xFF) / 255f, ((rgb >> 8) & 0xFF) / 255f, (rgb & 0xFF) / 255f);
    }

    public static DustParticleOptions dust(int rgb, float size) {
        return new DustParticleOptions(color(rgb), size);
    }

    /** Sparks of the zone's colour along its edge, on the ground: the warning still reads where grass hides the drawing. */
    public static void outline(ServerLevel level, BossTelegraph t) {
        ParticleOptions dust = dust(t.getStyle().rune, 1.4f);
        float size = t.getSize();
        switch (t.getShape()) {
            case CIRCLE -> circle(level, t.position(), size, dust);
            case RING -> {
                circle(level, t.position(), size, dust);
                circle(level, t.position(), t.getSize2(), dust);
            }
            case LINE -> {
                Vec3 f = t.forward();
                Vec3 side = new Vec3(f.z, 0, -f.x).scale(t.getSize2() * .5);
                Vec3 a = t.position().add(side);
                Vec3 b = t.position().subtract(side);
                Vec3 end = f.scale(size);
                segment(level, a, a.add(end), dust);
                segment(level, b, b.add(end), dust);
                segment(level, a, b, dust);
                segment(level, a.add(end), b.add(end), dust);
            }
            case CONE -> {
                float half = t.getSize2() * Mth.DEG_TO_RAD;
                float yaw = t.getYRot() * Mth.DEG_TO_RAD;
                Vec3 left = new Vec3(-Mth.sin(yaw - half), 0, Mth.cos(yaw - half)).scale(size);
                Vec3 right = new Vec3(-Mth.sin(yaw + half), 0, Mth.cos(yaw + half)).scale(size);
                segment(level, t.position(), t.position().add(left), dust);
                segment(level, t.position(), t.position().add(right), dust);
                int n = Math.max(6, (int) (2 * half * size / 1.2f));
                for (int i = 0; i <= n; i++) {
                    float a = yaw - half + 2 * half * i / n;
                    spark(level, t.position().add(-Mth.sin(a) * size, 0, Mth.cos(a) * size), dust);
                }
            }
        }
    }

    private static void circle(ServerLevel level, Vec3 c, float r, ParticleOptions dust) {
        int n = Mth.clamp((int) (Mth.TWO_PI * r / 1.2f), 8, 96);
        for (int i = 0; i < n; i++) {
            float a = i * Mth.TWO_PI / n;
            spark(level, c.add(Mth.cos(a) * r, 0, Mth.sin(a) * r), dust);
        }
    }

    private static void segment(ServerLevel level, Vec3 a, Vec3 b, ParticleOptions dust) {
        int n = Math.max(1, (int) (a.distanceTo(b) / 1.2));
        for (int i = 0; i <= n; i++) {
            spark(level, a.add(b.subtract(a).scale(i / (double) n)), dust);
        }
    }

    private static void spark(ServerLevel level, Vec3 at, ParticleOptions dust) {
        Vec3 ground = SpellUtil.groundAt(level, at, 4, 1);
        SpellUtil.particles(level, dust, ground.x, ground.y + .15, ground.z, 1, .05, .1, .05, 0);
    }

    /** A ring of light racing out along the ground. */
    public static void shockwave(Level level, Vec3 at, float size, int rgb) {
        SpellUtil.particles(level, new BlastwaveParticleOptions(color(rgb), size), at.x, at.y + .1, at.z, 1, 0, 0, 0, 0);
    }

    /** A heavy blow on the ground: dust, debris, a shockwave, a deep boom and the screen shaking. */
    public static void impact(ServerLevel level, Vec3 at, float radius, int rgb, SoundEvent sound) {
        shockwave(level, at, radius * 2.2f, rgb);
        SpellUtil.particles(level, ParticleTypes.EXPLOSION_EMITTER, at.x, at.y + .5, at.z, 1, 0, 0, 0, 0);
        SpellUtil.particles(level, ParticleTypes.CAMPFIRE_COSY_SMOKE, at.x, at.y + .3, at.z, (int) (radius * 6), radius * .4, .2, radius * .4, .03);
        SpellUtil.particles(level, dust(rgb, 2.5f), at.x, at.y + .5, at.z, (int) (radius * 10), radius * .45, .4, radius * .45, .1);
        level.playSound(null, at.x, at.y, at.z, sound, SoundSource.HOSTILE, 3f, .6f + level.random.nextFloat() * .15f);
        level.playSound(null, at.x, at.y, at.z, SoundEvents.GENERIC_EXPLODE, SoundSource.HOSTILE, 2.5f, .5f);
        SpellUtil.cameraShake(level, at, 20, radius * 3);
    }
}
