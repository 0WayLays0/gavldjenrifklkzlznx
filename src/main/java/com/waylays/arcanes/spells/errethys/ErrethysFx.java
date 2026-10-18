package com.waylays.arcanes.spells.errethys;

import com.waylays.arcanes.spells.SpellUtil;
import io.redspace.ironsspellbooks.particle.BlastwaveParticleOptions;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/**
 * Look of the Errethys school: creatures of translucent sky-blue mana, sparkling as they move, bursting into motes
 * of light when they appear and when they fade.
 */
public final class ErrethysFx {
    public static final Vector3f SKY = new Vector3f(0.53f, 0.81f, 0.92f);
    public static final Vector3f PALE = new Vector3f(0.85f, 0.96f, 1.0f);

    public static final DustParticleOptions SKY_DUST = new DustParticleOptions(SKY, 1.0f);
    public static final DustParticleOptions PALE_DUST = new DustParticleOptions(PALE, .7f);

    private ErrethysFx() {}

    /** A creature takes shape: a ring of light on the ground, motes rising and gathering. Server side. */
    public static void materialize(Level level, Vec3 at, float size) {
        SpellUtil.particles(level, new BlastwaveParticleOptions(SKY, 1.5f * size), at.x, at.y + .1, at.z, 1, 0, 0, 0, 0);
        SpellUtil.particles(level, SKY_DUST, at.x, at.y + .5 * size, at.z, (int) (18 * size), .35 * size, .4 * size, .35 * size, 0);
        SpellUtil.particles(level, PALE_DUST, at.x, at.y + .5 * size, at.z, (int) (10 * size), .3 * size, .4 * size, .3 * size, 0);
        SpellUtil.particles(level, ParticleTypes.END_ROD, at.x, at.y + .4 * size, at.z, (int) (6 * size), .25 * size, .3 * size, .25 * size, .03);
        level.playSound(null, at.x, at.y, at.z, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.NEUTRAL, 1.2f, 1.4f + level.random.nextFloat() * .3f);
        level.playSound(null, at.x, at.y, at.z, SoundEvents.ALLAY_AMBIENT_WITH_ITEM, SoundSource.NEUTRAL, .6f, .8f);
    }

    /** A creature comes apart into drifting motes of mana. Server side. */
    public static void dissolve(Level level, Vec3 at, float size) {
        SpellUtil.particles(level, SKY_DUST, at.x, at.y, at.z, (int) (14 * size), .3 * size, .3 * size, .3 * size, 0);
        SpellUtil.particles(level, PALE_DUST, at.x, at.y, at.z, (int) (8 * size), .25 * size, .25 * size, .25 * size, 0);
        SpellUtil.particles(level, ParticleTypes.END_ROD, at.x, at.y, at.z, (int) (5 * size), .2 * size, .2 * size, .2 * size, .05);
        level.playSound(null, at.x, at.y, at.z, SoundEvents.AMETHYST_CLUSTER_BREAK, SoundSource.NEUTRAL, .7f, 1.6f);
    }
}
