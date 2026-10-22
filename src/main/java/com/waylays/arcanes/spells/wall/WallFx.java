package com.waylays.arcanes.spells.wall;

import com.waylays.arcanes.spells.SpellUtil;
import com.waylays.arcanes.entity.wall.WallImpact;
import com.waylays.arcanes.entity.wall.WallSigil;
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

/**
 * Shared look and sound of Wall Magic, the tyrant's masonry. It is built of three blocks only: stone bricks, mossy
 * stone bricks and cracked stone bricks. What makes it magic is the mortar: it glows like embers between the bricks,
 * and every spell is called with a sigil burning on the ground.
 */
public final class WallFx {
    /** The school's colour: the grey of stone bricks. */
    public static final int STONE = 0x9A9A92;
    /** The glowing mortar: a deep ember red, a hot orange core. */
    public static final int EMBER = 0xFF4A1C;
    public static final int EMBER_CORE = 0xFFB070;
    public static final DustParticleOptions EMBER_DUST = new DustParticleOptions(new org.joml.Vector3f(1f, .35f, .12f), 1.1f);
    public static final BlockState BRICKS = Blocks.STONE_BRICKS.defaultBlockState();
    public static final BlockState MOSSY = Blocks.MOSSY_STONE_BRICKS.defaultBlockState();
    public static final BlockState CRACKED = Blocks.CRACKED_STONE_BRICKS.defaultBlockState();

    private WallFx() {}

    /** One of the three bricks for a seed: mostly plain, some mossy, some cracked. */
    public static BlockState brick(int seed) {
        float h = hash(seed);
        return h < .5f ? BRICKS : h < .75f ? MOSSY : CRACKED;
    }

    /** Server side: chips of the three bricks flying off. */
    public static void dust(Level level, Vec3 at, int count, double spread) {
        for (BlockState state : new BlockState[]{BRICKS, MOSSY, CRACKED}) {
            SpellUtil.particles(level, new BlockParticleOption(ParticleTypes.BLOCK, state), at.x, at.y, at.z, Math.max(1, count / 3), spread, spread * .5, spread, .15);
        }
    }

    /** The grinding of stone bursting out of the ground. */
    public static void rise(Level level, Vec3 at, float volume) {
        sound(level, at, SoundEvents.STONE_BREAK, volume, .6f + level.random.nextFloat() * .2f);
        sound(level, at, SoundEvents.GRINDSTONE_USE, volume * .5f, .5f);
    }

    /** Stone slamming into something. */
    public static void slam(Level level, Vec3 at, float volume) {
        sound(level, at, SoundEvents.STONE_BREAK, volume, .5f);
        sound(level, at, SoundEvents.ANVIL_LAND, volume * .5f, .5f);
    }

    /** Embers and brick chips bursting out at {@code at}. */
    public static void embers(Level level, Vec3 at, int count, double spread) {
        SpellUtil.particles(level, EMBER_DUST, at.x, at.y, at.z, count, spread, spread * .6, spread, .02);
        SpellUtil.particles(level, ParticleTypes.SMALL_FLAME, at.x, at.y, at.z, Math.max(1, count / 4), spread * .6, spread * .4, spread * .6, .02);
    }

    /** A sigil burning on the ground at {@code at}, {@code radius} wide, for {@code life} ticks: the incantation. */
    public static void sigil(Level level, Vec3 at, float radius, int life) {
        if (!level.isClientSide) {
            WallSigil.burn(level, at, radius, life);
        }
    }

    /** An impact: a ring of embers racing out over the ground and bricks flying from it. */
    public static void impact(Level level, Vec3 at, float size, int bricks) {
        if (!level.isClientSide) {
            WallImpact.strike(level, at, size, bricks);
            dust(level, at.add(0, .3, 0), (int) (12 * size), size * .5);
            embers(level, at.add(0, .3, 0), (int) (8 * size), size * .4);
        }
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
