package com.waylays.arcanes.spells.clockwork;

import com.waylays.arcanes.spells.SpellUtil;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/** Shared feel of Clockwork Magic: sparks off hot metal, puffs of steam, flecks of brass, blue sparks of the cores' energy. */
public final class ClockworkFx {
    public static final DustParticleOptions BRASS = new DustParticleOptions(new Vector3f(.87f, .67f, .31f), 1f);
    public static final DustParticleOptions BLUE = new DustParticleOptions(new Vector3f(.35f, .8f, 1f), 1f);

    private ClockworkFx() {}

    /** Server side: sparks and flecks of brass flung round {@code at}. */
    public static void sparks(Level level, Vec3 at, int count, double spread) {
        SpellUtil.particles(level, ParticleTypes.LAVA, at.x, at.y, at.z, Math.max(1, count / 5), spread, spread, spread, 0);
        SpellUtil.particles(level, ParticleTypes.CRIT, at.x, at.y, at.z, Math.max(1, count / 2), spread, spread, spread, .3);
        SpellUtil.particles(level, BRASS, at.x, at.y, at.z, Math.max(1, count / 3), spread, spread, spread, .05);
    }

    /** Server side: puffs of steam round {@code at}. */
    public static void steam(Level level, Vec3 at, int count, double spread) {
        SpellUtil.particles(level, ParticleTypes.CLOUD, at.x, at.y, at.z, count, spread, spread * .5, spread, .03);
        SpellUtil.particles(level, ParticleTypes.POOF, at.x, at.y, at.z, Math.max(1, count / 2), spread, spread * .5, spread, .02);
    }

    /** Server side: blue sparks of the cores' energy round {@code at}. */
    public static void arcs(Level level, Vec3 at, int count, double spread) {
        SpellUtil.particles(level, ParticleTypes.ELECTRIC_SPARK, at.x, at.y, at.z, count, spread, spread, spread, .2);
        SpellUtil.particles(level, BLUE, at.x, at.y, at.z, Math.max(1, count / 2), spread, spread, spread, .05);
    }

    /** Server side: a bomb going off at {@code at}. */
    public static void blast(Level level, Vec3 at) {
        SpellUtil.particles(level, ParticleTypes.EXPLOSION, at.x, at.y, at.z, 2, .6, .3, .6, 0);
        SpellUtil.particles(level, ParticleTypes.LARGE_SMOKE, at.x, at.y, at.z, 10, .8, .4, .8, .04);
        sparks(level, at, 14, .8);
    }
}
