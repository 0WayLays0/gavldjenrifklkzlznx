package com.waylays.arcanes.spells.wind;

import com.waylays.arcanes.spells.SpellUtil;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

/** Shared feel of Wind Magic: gusts of air, blown dust; throwing creatures about and turning shots aside. */
public final class WindFx {
    private WindFx() {}

    /** Server side: a gust of air bursting at {@code at}. */
    public static void gust(Level level, Vec3 at, int count, double spread) {
        SpellUtil.particles(level, ParticleTypes.GUST, at.x, at.y, at.z, Math.max(1, count / 4), spread, spread * .5, spread, 0);
        SpellUtil.particles(level, ParticleTypes.CLOUD, at.x, at.y, at.z, count, spread, spread * .5, spread, .08);
    }

    /** Server side: dust and bits blown up off the ground at {@code at}. */
    public static void dust(Level level, Vec3 at, int count, double spread) {
        SpellUtil.particles(level, ParticleTypes.POOF, at.x, at.y + .2, at.z, count, spread, .1, spread, .05);
    }

    /** How much of a push {@code e} takes (1 none resisted, 0 all). */
    public static double give(LivingEntity e) {
        return 1 - Mth.clamp(e.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE), 0, 1);
    }

    /** Throws {@code e} along {@code push} (its knockback resistance resisting it). */
    public static void throwBy(LivingEntity e, Vec3 push) {
        double k = give(e);
        e.setDeltaMovement(e.getDeltaMovement().add(push.scale(k)));
        e.hurtMarked = true;
    }

    /**
     * Turns aside every shot in {@code box} not loosed by one of {@code owner}'s side: sent flying away from
     * {@code from} at the speed it came (at least {@code minSpeed}). Returns how many were turned.
     */
    public static int deflect(Level level, AABB box, Vec3 from, @Nullable Entity owner, double minSpeed) {
        int turned = 0;
        for (Projectile shot : level.getEntitiesOfClass(Projectile.class, box)) {
            Entity by = shot.getOwner();
            if (by != null && (by == owner || owner != null && SpellUtil.isAlly(owner, by))) {
                continue;
            }
            Vec3 away = shot.position().subtract(from);
            away = away.lengthSqr() < 1.0E-4 ? new Vec3(0, 1, 0) : away.normalize();
            Vec3 v = shot.getDeltaMovement();
            // Already flying away: leave it be
            if (v.dot(away) > 0 && v.lengthSqr() > minSpeed * minSpeed * .25) {
                continue;
            }
            double speed = Math.max(minSpeed, v.length());
            shot.setDeltaMovement(away.add(0, .25, 0).normalize().scale(speed));
            shot.hurtMarked = true;
            turned++;
        }
        return turned;
    }
}
