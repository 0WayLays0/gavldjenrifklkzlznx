package com.waylays.arcanes.spells.gold;

import io.redspace.ironsspellbooks.particle.BlastwaveParticleOptions;
import io.redspace.ironsspellbooks.particle.FogParticleOptions;
import io.redspace.ironsspellbooks.particle.SparkParticleOptions;
import com.waylays.arcanes.spells.SpellUtil;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/**
 * Look of the Gold school: palette and particle bursts (server side, sent to every nearby client).
 */
public final class GoldFx {
    public static final Vector3f GOLD = new Vector3f(1.0f, 0.79f, 0.24f);
    public static final Vector3f PALE_GOLD = new Vector3f(1.0f, 0.95f, 0.62f);
    public static final Vector3f DEEP_GOLD = new Vector3f(0.85f, 0.52f, 0.08f);
    public static final Vector3f MOLTEN = new Vector3f(1.0f, 0.45f, 0.05f);

    public static final DustParticleOptions GOLD_DUST = new DustParticleOptions(GOLD, 1.1f);
    public static final DustParticleOptions PALE_DUST = new DustParticleOptions(PALE_GOLD, 0.7f);
    public static final DustParticleOptions DEEP_DUST = new DustParticleOptions(DEEP_GOLD, 1.4f);
    public static final DustParticleOptions MOLTEN_DUST = new DustParticleOptions(MOLTEN, 1.2f);

    private GoldFx() {}

    public static ParticleOptions sparks() {
        return new SparkParticleOptions(GOLD);
    }

    public static ParticleOptions nuggetShards() {
        return new ItemParticleOption(ParticleTypes.ITEM, new ItemStack(Items.GOLD_NUGGET));
    }

    public static ParticleOptions chainShards() {
        return new ItemParticleOption(ParticleTypes.ITEM, new ItemStack(Items.CHAIN));
    }

    /** Shatter burst: flash, golden shockwave ring, flying nugget shards, sparks and glitter. Server side. */
    public static void shatterBurst(Level level, double x, double y, double z, float size) {
        SpellUtil.particles(level, ParticleTypes.FLASH, x, y, z, 1, 0, 0, 0, 0);
        SpellUtil.particles(level, new BlastwaveParticleOptions(GOLD, size * 1.6f), x, y, z, 1, 0, 0, 0, 0);
        SpellUtil.particles(level, nuggetShards(), x, y, z, (int) (18 * size), .15, .15, .15, .28);
        SpellUtil.particles(level, sparks(), x, y, z, (int) (14 * size), .1, .1, .1, .45);
        SpellUtil.particles(level, GOLD_DUST, x, y, z, (int) (20 * size), .5 * size, .5 * size, .5 * size, .05);
        SpellUtil.particles(level, new FogParticleOptions(GOLD, .5f * size), x, y, z, 3, .2, .2, .2, .02);
    }

    /** Line of gold dust and sparks between two points (beams, trails). Server side. */
    public static void particleLine(Level level, Vec3 from, Vec3 to, double step) {
        Vec3 delta = to.subtract(from);
        int steps = Math.max(1, (int) (delta.length() / step));
        for (int i = 0; i <= steps; i++) {
            Vec3 p = from.add(delta.scale(i / (double) steps));
            SpellUtil.particles(level, i % 2 == 0 ? GOLD_DUST : PALE_DUST, p.x, p.y, p.z, 1, .03, .03, .03, 0);
        }
    }
}
