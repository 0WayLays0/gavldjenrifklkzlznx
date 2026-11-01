package com.waylays.arcanes.spells.sand;

import com.waylays.arcanes.spells.SpellUtil;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/** Shared feel of Sand Magic: puffs and showers of sand. */
public final class SandFx {
    private SandFx() {}

    /** Server side: sand thrown up at {@code at}. */
    public static void sand(Level level, Vec3 at, int count, double spread) {
        SpellUtil.particles(level, new BlockParticleOption(ParticleTypes.BLOCK, Blocks.SAND.defaultBlockState()), at.x, at.y, at.z, count, spread, spread * .5,
                spread, .1);
        SpellUtil.particles(level, new BlockParticleOption(ParticleTypes.FALLING_DUST, Blocks.SAND.defaultBlockState()), at.x, at.y + .5, at.z,
                Math.max(1, count / 3), spread, spread * .5, spread, 0);
    }

    /** Server side: glass bursting at {@code at}. */
    public static void glass(Level level, Vec3 at, int count, double spread) {
        SpellUtil.particles(level, new BlockParticleOption(ParticleTypes.BLOCK, Blocks.GLASS.defaultBlockState()), at.x, at.y, at.z, count, spread, spread,
                spread, .15);
    }
}
