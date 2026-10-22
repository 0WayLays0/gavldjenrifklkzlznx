package com.waylays.arcanes.spells.verdigris;

import com.waylays.arcanes.spells.SpellUtil;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/** Shared look and sound of Moss, Copper and Rust Magic: verdigris, rust flakes, moss, the clang of old copper. */
public final class VerdigrisFx {
    /** Verdigris (the green of old copper), bright copper, rust and moss. */
    public static final int VERDIGRIS = 0x4FBF96;
    public static final int COPPER = 0xD8814A;
    public static final int RUST = 0xA0451E;
    public static final int MOSS = 0x6B8E23;
    public static final DustParticleOptions RUST_DUST = new DustParticleOptions(new Vector3f(.63f, .27f, .12f), 1.2f);
    public static final DustParticleOptions VERDIGRIS_DUST = new DustParticleOptions(new Vector3f(.31f, .75f, .59f), 1.1f);

    private VerdigrisFx() {}

    /** Server side: chips of old copper and moss flying off. */
    public static void debris(Level level, Vec3 at, int count, double spread) {
        SpellUtil.particles(level, new BlockParticleOption(ParticleTypes.BLOCK, Blocks.OXIDIZED_COPPER.defaultBlockState()), at.x, at.y, at.z, count, spread, spread * .5, spread, .15);
        SpellUtil.particles(level, new BlockParticleOption(ParticleTypes.BLOCK, Blocks.MOSS_BLOCK.defaultBlockState()), at.x, at.y, at.z, count / 2 + 1, spread, spread * .5, spread, .1);
        SpellUtil.particles(level, ParticleTypes.SCRAPE, at.x, at.y, at.z, count / 2 + 1, spread, spread * .5, spread, .3);
    }

    /** Server side: rust flaking off in a puff. */
    public static void rust(Level level, Vec3 at, int count, double spread) {
        SpellUtil.particles(level, RUST_DUST, at.x, at.y, at.z, count, spread, spread, spread, 0);
        SpellUtil.particles(level, ParticleTypes.WAX_ON, at.x, at.y, at.z, count / 2 + 1, spread, spread, spread, .2);
    }

    /** Server side: chips of a block breaking off. */
    public static void chips(Level level, Vec3 at, BlockState state, int count, double spread) {
        SpellUtil.particles(level, new BlockParticleOption(ParticleTypes.BLOCK, state), at.x, at.y, at.z, count, spread, spread * .5, spread, .15);
    }

    /** The grinding clang of old copper bursting out of the ground. */
    public static void rise(Level level, Vec3 at, float volume) {
        sound(level, at, SoundEvents.COPPER_BREAK, volume, .6f + level.random.nextFloat() * .2f);
        sound(level, at, SoundEvents.MOSS_BREAK, volume * .8f, .8f);
        sound(level, at, SoundEvents.GRINDSTONE_USE, volume * .5f, .6f);
    }

    /** A steady pseudo-random number in [0, 1) for a seed, the same on both sides (shapes of spikes and stones). */
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
