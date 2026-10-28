package com.waylays.arcanes.spells.water;

import com.waylays.arcanes.spells.SpellUtil;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/**
 * Shared look and sound of Water Magic: spray, splashes, bubbles, mist, the deep blue of the sea and the pale glow of
 * its foam; and soaking (slowed, put out).
 */
public final class WaterFx {
    /** Deep water, bright shallow water, foam, and the glow of living things in the deep. */
    public static final int DEEP = 0x1F6FB8;
    public static final int SHALLOW = 0x4FC3F0;
    public static final int FOAM = 0xEAF8FF;
    public static final int GLOW = 0x7CF2FF;
    public static final DustParticleOptions MIST = new DustParticleOptions(new Vector3f(.75f, .9f, 1f), 1.4f);

    private WaterFx() {}

    /** Server side: water splashing out at {@code at}. */
    public static void splash(Level level, Vec3 at, int count, double spread) {
        SpellUtil.particles(level, ParticleTypes.SPLASH, at.x, at.y, at.z, count, spread, spread * .5, spread, .2);
        SpellUtil.particles(level, ParticleTypes.FALLING_WATER, at.x, at.y, at.z, count / 2, spread, spread * .5, spread, 0);
    }

    /** Server side: bubbles rising at {@code at}. */
    public static void bubbles(Level level, Vec3 at, int count, double spread) {
        SpellUtil.particles(level, ParticleTypes.BUBBLE_POP, at.x, at.y, at.z, count, spread, spread * .6, spread, .05);
    }

    /** Server side: a fine mist hanging at {@code at}. */
    public static void mist(Level level, Vec3 at, int count, double spread) {
        SpellUtil.particles(level, MIST, at.x, at.y, at.z, count, spread, spread * .5, spread, .01);
        SpellUtil.particles(level, ParticleTypes.CLOUD, at.x, at.y, at.z, count / 3, spread, spread * .4, spread, .02);
    }

    /** Server side: spray thrown up from the surface of water at {@code at}. */
    public static void spray(Level level, Vec3 at, int count, double spread) {
        SpellUtil.particles(level, ParticleTypes.FISHING, at.x, at.y, at.z, count, spread, .1, spread, .08);
        SpellUtil.particles(level, ParticleTypes.SPLASH, at.x, at.y + .2, at.z, count, spread, .2, spread, .3);
    }

    /** Soaks {@code e}: a short slow, and any fire on it put out. */
    public static void soak(LivingEntity e, int ticks) {
        e.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN, ticks, 0, false, true));
        e.clearFire();
    }

    /** A wave of water crashing. */
    public static void crash(Level level, Vec3 at, float volume) {
        sound(level, at, SoundEvents.GENERIC_SPLASH, volume, .6f);
        sound(level, at, SoundEvents.PLAYER_SPLASH_HIGH_SPEED, volume * .8f, .7f);
    }

    public static float hash(int seed) {
        int h = seed * 0x9E3779B1;
        h ^= h >>> 15;
        h *= 0x85EBCA77;
        h ^= h >>> 13;
        return (h & 0xFFFFFF) / (float) 0x1000000;
    }

    public static void sound(Level level, Vec3 at, SoundEvent sound, float volume, float pitch) {
        level.playSound(null, at.x, at.y, at.z, sound, SoundSource.PLAYERS, volume, pitch);
    }
}
