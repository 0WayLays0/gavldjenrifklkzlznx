package com.waylays.arcanes.spells.lotus;

import com.waylays.arcanes.spells.SpellUtil;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/** Shared feel of Lotus Magic: drifts of pink petals, golden pollen, rings of light. */
public final class LotusFx {
    /** The pink of the lotus and the gold of its heart, as dust. */
    public static final DustParticleOptions PINK = new DustParticleOptions(new Vector3f(.96f, .54f, .75f), 1.1f);
    public static final DustParticleOptions GOLD = new DustParticleOptions(new Vector3f(1f, .83f, .43f), .8f);

    private LotusFx() {}

    /** Server side: petals scattered round {@code at}. */
    public static void petals(Level level, Vec3 at, int count, double spread) {
        SpellUtil.particles(level, ParticleTypes.CHERRY_LEAVES, at.x, at.y, at.z, count, spread, spread * .6, spread, .02);
        SpellUtil.particles(level, PINK, at.x, at.y, at.z, Math.max(1, count / 3), spread, spread * .6, spread, .02);
    }

    /** Server side: golden pollen and glints of light. */
    public static void pollen(Level level, Vec3 at, int count, double spread) {
        SpellUtil.particles(level, GOLD, at.x, at.y, at.z, count, spread, spread, spread, .03);
        SpellUtil.particles(level, ParticleTypes.END_ROD, at.x, at.y, at.z, Math.max(1, count / 4), spread, spread, spread, .02);
    }

    /** Server side: a ring of pink light round {@code c}, {@code r} across, {@code count} motes. */
    public static void ring(Level level, Vec3 c, double r, int count) {
        for (int k = 0; k < count; k++) {
            double a = k * Math.PI * 2 / count;
            SpellUtil.particles(level, PINK, c.x + Math.cos(a) * r, c.y + .2, c.z + Math.sin(a) * r, 1, .05, .1, .05, 0);
            if (k % 3 == 0) {
                SpellUtil.particles(level, ParticleTypes.END_ROD, c.x + Math.cos(a) * r, c.y + .2, c.z + Math.sin(a) * r, 1, .05, .2, .05, .03);
            }
        }
    }
}
