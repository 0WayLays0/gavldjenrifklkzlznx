package com.waylays.arcanes.spells.bone;

import com.waylays.arcanes.spells.SpellUtil;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/**
 * Shared look and sound of Bone Magic: ivory bone, its marrow glowing crimson, splinters and bone dust, the dry crack
 * and rattle of bone.
 */
public final class BoneFx {
    /** Ivory bone, and the marrow's glow: a deep crimson with a hot core. */
    public static final int IVORY = 0xE8DCC0;
    public static final int MARROW = 0xC2182B;
    public static final int MARROW_CORE = 0xFF6A5A;
    public static final DustParticleOptions MARROW_DUST = new DustParticleOptions(new Vector3f(.82f, .1f, .16f), 1.1f);
    public static final DustParticleOptions BONE_DUST = new DustParticleOptions(new Vector3f(.92f, .88f, .78f), 1.3f);
    private static final BlockParticleOption SPLINTERS = new BlockParticleOption(ParticleTypes.BLOCK, Blocks.BONE_BLOCK.defaultBlockState());

    private BoneFx() {}

    /** Server side: splinters of bone and bone dust bursting out at {@code at}. */
    public static void splinters(Level level, Vec3 at, int count, double spread) {
        SpellUtil.particles(level, SPLINTERS, at.x, at.y, at.z, count, spread, spread * .6, spread, .15);
        SpellUtil.particles(level, BONE_DUST, at.x, at.y, at.z, Math.max(1, count / 2), spread, spread * .6, spread, .02);
    }

    /** Server side: the marrow's crimson glow drifting off at {@code at}. */
    public static void marrow(Level level, Vec3 at, int count, double spread) {
        SpellUtil.particles(level, MARROW_DUST, at.x, at.y, at.z, count, spread, spread * .6, spread, .02);
    }

    /** Bone bursting out of the ground. */
    public static void erupt(Level level, Vec3 at, float volume) {
        sound(level, at, SoundEvents.BONE_BLOCK_BREAK, volume, .6f + level.random.nextFloat() * .3f);
        sound(level, at, SoundEvents.SKELETON_HURT, volume * .5f, .5f + level.random.nextFloat() * .2f);
    }

    /** Bone striking flesh. */
    public static void crack(Level level, Vec3 at, float volume) {
        sound(level, at, SoundEvents.SKELETON_HURT, volume, .7f + level.random.nextFloat() * .3f);
        sound(level, at, SoundEvents.ZOMBIE_BREAK_WOODEN_DOOR, volume * .4f, 1.6f);
    }

    /** The rattle of many bones moving. */
    public static void rattle(Level level, Vec3 at, float volume) {
        sound(level, at, SoundEvents.SKELETON_STEP, volume, .5f + level.random.nextFloat() * .3f);
        sound(level, at, SoundEvents.SKELETON_AMBIENT, volume * .6f, .6f);
    }

    /** A steady pseudo-random number in [0, 1) for a seed, the same on both sides. */
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
