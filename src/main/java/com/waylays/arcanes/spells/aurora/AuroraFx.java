package com.waylays.arcanes.spells.aurora;

import com.waylays.arcanes.spells.SpellUtil;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/** Shared feel of Aurora Magic: motes of green, turquoise and violet light, and glints of starlight. */
public final class AuroraFx {
    public static final DustParticleOptions GREEN = new DustParticleOptions(new Vector3f(.27f, 1f, .63f), 1f);
    public static final DustParticleOptions TEAL = new DustParticleOptions(new Vector3f(.25f, .91f, .88f), 1f);
    public static final DustParticleOptions VIOLET = new DustParticleOptions(new Vector3f(.64f, .42f, 1f), 1f);

    private AuroraFx() {}

    /** Server side: motes of the aurora's colours and glints of starlight round {@code at}. */
    public static void lights(Level level, Vec3 at, int count, double spread) {
        SpellUtil.particles(level, GREEN, at.x, at.y, at.z, Math.max(1, count / 3), spread, spread, spread, .02);
        SpellUtil.particles(level, TEAL, at.x, at.y, at.z, Math.max(1, count / 3), spread, spread, spread, .02);
        SpellUtil.particles(level, VIOLET, at.x, at.y, at.z, Math.max(1, count / 3), spread, spread, spread, .02);
        SpellUtil.particles(level, ParticleTypes.END_ROD, at.x, at.y, at.z, Math.max(1, count / 4), spread, spread, spread, .03);
    }
}
