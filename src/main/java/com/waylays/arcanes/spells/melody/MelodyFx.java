package com.waylays.arcanes.spells.melody;

import com.waylays.arcanes.spells.SpellUtil;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/** Shared feel of Music Magic: notes of every colour, gold dust, rose and violet sparkles. */
public final class MelodyFx {
    public static final DustParticleOptions GOLD = new DustParticleOptions(new Vector3f(1f, .83f, .42f), 1f);
    public static final DustParticleOptions ROSE = new DustParticleOptions(new Vector3f(1f, .44f, .85f), 1f);
    public static final DustParticleOptions VIOLET = new DustParticleOptions(new Vector3f(.65f, .48f, 1f), 1.2f);

    private MelodyFx() {}

    /** Server side: notes of all colours and gold dust round {@code at}. */
    public static void notes(Level level, Vec3 at, int count, double spread) {
        // A note particle takes its colour from its speed: a spread of speeds gives a spread of colours
        SpellUtil.particles(level, ParticleTypes.NOTE, at.x, at.y, at.z, count, spread, spread, spread, 1);
        SpellUtil.particles(level, GOLD, at.x, at.y, at.z, Math.max(1, count / 2), spread, spread, spread, .05);
        SpellUtil.particles(level, ParticleTypes.END_ROD, at.x, at.y, at.z, Math.max(1, count / 4), spread, spread, spread, .02);
    }

    /** Server side: hearts and rose sparkles round {@code at} (a healing note). */
    public static void hearts(Level level, Vec3 at, int count, double spread) {
        SpellUtil.particles(level, ParticleTypes.HEART, at.x, at.y + .4, at.z, count, spread, spread * .5, spread, 0);
        SpellUtil.particles(level, ROSE, at.x, at.y, at.z, count * 2, spread, spread, spread, .05);
    }

    /** Server side: a sleeper's breath: slow violet motes and a note above them. */
    public static void sleep(Level level, Vec3 at, int count) {
        SpellUtil.particles(level, VIOLET, at.x, at.y, at.z, count * 2, .25, .15, .25, .01);
        SpellUtil.particles(level, ParticleTypes.NOTE, at.x, at.y + .2, at.z, count, .2, .1, .2, 1);
    }

    /** Server side: a ring of gold and notes running out along the ground, {@code radius} round. */
    public static void shock(Level level, Vec3 at, float radius) {
        int n = Mth.clamp((int) (radius * 6), 12, 72);
        for (int i = 0; i < n; i++) {
            float a = i * Mth.TWO_PI / n;
            double x = at.x + Mth.cos(a) * radius, z = at.z + Mth.sin(a) * radius;
            SpellUtil.particles(level, i % 3 == 0 ? ParticleTypes.NOTE : GOLD, x, at.y, z, 1, .1, .05, .1, i % 3 == 0 ? 1 : 0);
        }
        SpellUtil.particles(level, ParticleTypes.POOF, at.x, at.y, at.z, 8, radius * .3, .1, radius * .3, .05);
    }

    /** Server side: a trumpets' blast: notes and gold flung along {@code dir}. */
    public static void blast(Level level, Vec3 at, Vec3 dir, int count) {
        for (int i = 0; i < count; i++) {
            Vec3 p = at.add(dir.scale(i * .8));
            double s = .3 + i * .15;
            SpellUtil.particles(level, i % 2 == 0 ? ParticleTypes.NOTE : GOLD, p.x, p.y, p.z, 2, s, s * .6, s, i % 2 == 0 ? 1 : .05);
        }
        SpellUtil.particles(level, ParticleTypes.CLOUD, at.x, at.y, at.z, 6, .5, .3, .5, .08);
    }

    /** Server side: a pillar of sound striking the ground at {@code at}. */
    public static void pillar(Level level, Vec3 at) {
        for (int i = 0; i < 8; i++) {
            SpellUtil.particles(level, ParticleTypes.END_ROD, at.x, at.y + i * .8, at.z, 1, .15, .3, .15, .01);
        }
        SpellUtil.particles(level, ParticleTypes.NOTE, at.x, at.y + .4, at.z, 8, .8, .3, .8, 1);
        SpellUtil.particles(level, VIOLET, at.x, at.y + .2, at.z, 10, .8, .1, .8, .05);
    }
}
