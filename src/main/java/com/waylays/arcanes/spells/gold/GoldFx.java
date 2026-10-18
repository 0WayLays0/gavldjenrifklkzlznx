package com.waylays.arcanes.spells.gold;

import com.waylays.arcanes.entity.gold.GoldenSentinel;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.util.CameraShakeData;
import io.redspace.ironsspellbooks.api.util.CameraShakeManager;
import io.redspace.ironsspellbooks.damage.DamageSources;
import io.redspace.ironsspellbooks.damage.SpellDamageSource;
import io.redspace.ironsspellbooks.particle.BlastwaveParticleOptions;
import io.redspace.ironsspellbooks.particle.FogParticleOptions;
import io.redspace.ironsspellbooks.particle.SparkParticleOptions;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * Shared look and rules of the Gold school: palette, particle bursts (server side, sent to every nearby client),
 * damage sources and who counts as an ally of the caster.
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

    /** Server side: like ServerLevel#sendParticles with long-range visibility; does nothing on the client. */
    public static void particles(Level level, ParticleOptions particle, double x, double y, double z, int count,
                                 double dx, double dy, double dz, double speed) {
        if (level instanceof ServerLevel serverLevel) {
            for (var player : serverLevel.players()) {
                serverLevel.sendParticles(player, particle, true, x, y, z, count, dx, dy, dz, speed);
            }
        }
    }

    /** Shatter burst: flash, golden shockwave ring, flying nugget shards, sparks and glitter. Server side. */
    public static void shatterBurst(Level level, double x, double y, double z, float size) {
        particles(level, ParticleTypes.FLASH, x, y, z, 1, 0, 0, 0, 0);
        particles(level, new BlastwaveParticleOptions(GOLD, size * 1.6f), x, y, z, 1, 0, 0, 0, 0);
        particles(level, nuggetShards(), x, y, z, (int) (18 * size), .15, .15, .15, .28);
        particles(level, sparks(), x, y, z, (int) (14 * size), .1, .1, .1, .45);
        particles(level, GOLD_DUST, x, y, z, (int) (20 * size), .5 * size, .5 * size, .5 * size, .05);
        particles(level, new FogParticleOptions(GOLD, .5f * size), x, y, z, 3, .2, .2, .2, .02);
    }

    /** Line of gold dust and sparks between two points (beams, trails). Server side. */
    public static void particleLine(Level level, Vec3 from, Vec3 to, double step) {
        Vec3 delta = to.subtract(from);
        int steps = Math.max(1, (int) (delta.length() / step));
        for (int i = 0; i <= steps; i++) {
            Vec3 p = from.add(delta.scale(i / (double) steps));
            particles(level, i % 2 == 0 ? GOLD_DUST : PALE_DUST, p.x, p.y, p.z, 1, .03, .03, .03, 0);
        }
    }

    /**
     * The walkable surface nearest to {@code pos} (searching {@code range} blocks up and down, nearest first): a block
     * with a collision shape and {@code clearance} free blocks above it. Falls back to {@code pos}.
     */
    public static Vec3 groundAt(Level level, Vec3 pos, int range, int clearance) {
        BlockPos base = BlockPos.containing(pos.x, pos.y + 1.0E-3, pos.z);
        for (int k = 0; k <= range; k++) {
            for (int sign : k == 0 ? new int[]{0} : new int[]{-1, 1}) {
                BlockPos feet = base.above(k * sign);
                BlockPos below = feet.below();
                VoxelShape floor = level.getBlockState(below).getCollisionShape(level, below);
                if (floor.isEmpty()) {
                    continue;
                }
                boolean free = true;
                for (int h = 0; h < clearance && free; h++) {
                    BlockPos p = feet.above(h);
                    free = level.getBlockState(p).getCollisionShape(level, p).isEmpty();
                }
                if (free) {
                    return new Vec3(pos.x, below.getY() + floor.max(Direction.Axis.Y), pos.z);
                }
            }
        }
        return pos;
    }

    public static void cameraShake(Level level, Vec3 origin, int ticks, float radius) {
        if (!level.isClientSide) {
            CameraShakeManager.addCameraShake(new CameraShakeData(ticks, origin, radius));
        }
    }

    /** A spell damage source that still works when the caster is gone (credited to the projectile then). */
    public static SpellDamageSource damageSource(AbstractSpell spell, Entity direct, @Nullable Entity caster) {
        return spell.getDamageSource(direct, caster != null ? caster : direct);
    }

    /** The caster, its golden sentinels and anything Iron's considers friendly fire are never hurt by Gold spells. */
    public static boolean isAlly(@Nullable Entity caster, Entity other) {
        if (caster == null) {
            return false;
        }
        if (other == caster) {
            return true;
        }
        if (other instanceof GoldenSentinel sentinel && caster.getUUID().equals(sentinel.getOwnerUUID())) {
            return true;
        }
        return DamageSources.isFriendlyFireBetween(caster, other);
    }

    /** Living, non-allied entities in a sphere. */
    public static List<LivingEntity> enemiesAround(Level level, Vec3 center, double radius, @Nullable Entity caster, Predicate<LivingEntity> extra) {
        List<LivingEntity> out = new ArrayList<>();
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(center, center).inflate(radius))) {
            if (e.isAlive() && !isAlly(caster, e) && e.getBoundingBox().getCenter().distanceToSqr(center) <= radius * radius && extra.test(e)) {
                out.add(e);
            }
        }
        return out;
    }

    /** Hurts every enemy in a sphere. Returns how many were hit. */
    public static int areaDamage(Level level, Vec3 center, double radius, float damage, AbstractSpell spell, Entity direct, @Nullable Entity caster) {
        int hit = 0;
        for (LivingEntity e : enemiesAround(level, center, radius, caster, x -> true)) {
            if (DamageSources.applyDamage(e, damage, damageSource(spell, direct, caster))) {
                hit++;
            }
        }
        return hit;
    }
}
