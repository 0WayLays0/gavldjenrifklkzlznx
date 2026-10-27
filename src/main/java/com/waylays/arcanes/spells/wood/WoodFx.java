package com.waylays.arcanes.spells.wood;

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
 * Shared look and sound of Wood Magic: bark and wood chips, leaves, the spring-green glow of the life in the wood, the
 * creak and crack of timber.
 */
public final class WoodFx {
    /** The school's wood, and the life in it: a bright spring green. */
    public static final int WOOD = 0xB07A3E;
    public static final int LIFE = 0x8FE04A;
    public static final int LIFE_CORE = 0xE4FFB0;
    public static final DustParticleOptions LIFE_DUST = new DustParticleOptions(new Vector3f(.56f, .88f, .29f), 1.1f);
    private static final BlockParticleOption CHIPS = new BlockParticleOption(ParticleTypes.BLOCK, Blocks.OAK_LOG.defaultBlockState());
    private static final BlockParticleOption LEAVES = new BlockParticleOption(ParticleTypes.BLOCK, Blocks.AZALEA_LEAVES.defaultBlockState());

    private WoodFx() {}

    /** Server side: chips of wood and bark bursting out at {@code at}. */
    public static void chips(Level level, Vec3 at, int count, double spread) {
        SpellUtil.particles(level, CHIPS, at.x, at.y, at.z, count, spread, spread * .6, spread, .15);
    }

    /** Server side: leaves flying at {@code at}. */
    public static void leaves(Level level, Vec3 at, int count, double spread) {
        SpellUtil.particles(level, LEAVES, at.x, at.y, at.z, count, spread, spread * .6, spread, .1);
    }

    /** Server side: the green glow of life drifting off at {@code at}. */
    public static void life(Level level, Vec3 at, int count, double spread) {
        SpellUtil.particles(level, LIFE_DUST, at.x, at.y, at.z, count, spread, spread * .6, spread, .02);
    }

    /** Wood bursting out of the ground. */
    public static void sprout(Level level, Vec3 at, float volume) {
        sound(level, at, SoundEvents.WOOD_PLACE, volume, .6f + level.random.nextFloat() * .2f);
        sound(level, at, SoundEvents.ROOTED_DIRT_BREAK, volume, .6f);
    }

    /** Timber creaking, about to give. */
    public static void creak(Level level, Vec3 at, float volume) {
        sound(level, at, SoundEvents.BAMBOO_WOOD_BREAK, volume, .4f);
        sound(level, at, SoundEvents.WOODEN_DOOR_OPEN, volume * .7f, .5f);
    }

    /** Wood crashing into something. */
    public static void crash(Level level, Vec3 at, float volume) {
        sound(level, at, SoundEvents.WOOD_BREAK, volume, .5f);
        sound(level, at, SoundEvents.ZOMBIE_BREAK_WOODEN_DOOR, volume * .6f, .6f);
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
