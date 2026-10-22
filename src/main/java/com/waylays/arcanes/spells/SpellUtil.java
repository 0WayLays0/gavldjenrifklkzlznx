package com.waylays.arcanes.spells;

import com.waylays.arcanes.entity.OwnedSummon;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.util.CameraShakeData;
import io.redspace.ironsspellbooks.api.util.CameraShakeManager;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.damage.DamageSources;
import io.redspace.ironsspellbooks.damage.SpellDamageSource;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * Rules shared by every school of the addon: particles sent to clients, ground search, camera shake, spell damage
 * sources and who counts as an ally of the caster.
 */
public final class SpellUtil {
    private SpellUtil() {}

    /** Server side: like ServerLevel#sendParticles with long-range visibility; does nothing on the client. */
    public static void particles(Level level, ParticleOptions particle, double x, double y, double z, int count,
                                 double dx, double dy, double dz, double speed) {
        if (level instanceof ServerLevel serverLevel) {
            for (var player : serverLevel.players()) {
                serverLevel.sendParticles(player, particle, true, x, y, z, count, dx, dy, dz, speed);
            }
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

    /** The ground the caster is aiming at, at most {@code range} away (straight down from the end of the aim if it is in the air). */
    public static Vec3 aimedGround(Level level, LivingEntity caster, double range) {
        Vec3 eye = caster.getEyePosition();
        Vec3 end = eye.add(caster.getLookAngle().scale(range));
        HitResult hit = level.clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, caster));
        Vec3 at = hit.getType() == HitResult.Type.MISS ? end : hit.getLocation();
        HitResult down = level.clip(new ClipContext(at.add(0, .5, 0), at.add(0, -40, 0), ClipContext.Block.COLLIDER, ClipContext.Fluid.ANY, caster));
        if (down.getType() != HitResult.Type.MISS) {
            return down.getLocation();
        }
        return groundAt(level, at, 8, 1);
    }

    /** The enemy the caster looks at within {@code range}, if any. */
    @Nullable
    public static LivingEntity aimedEnemy(Level level, LivingEntity caster, double range) {
        HitResult hit = Utils.raycastForEntity(level, caster, (float) range, true, .6f);
        if (hit instanceof EntityHitResult entityHit && entityHit.getEntity() instanceof LivingEntity living && living.isAlive()
                && !(living instanceof ArmorStand) && !isAlly(caster, living)) {
            return living;
        }
        return null;
    }

    /** Where the caster's aim meets the ground: under the enemy they look at, else the ground they point at. */
    public static Vec3 aimedSpot(Level level, LivingEntity caster, double range) {
        LivingEntity target = aimedEnemy(level, caster, range);
        return target != null ? groundAt(level, target.position(), 3, 1) : aimedGround(level, caster, range);
    }

    public static void cameraShake(Level level, Vec3 origin, int ticks, float radius) {
        if (!level.isClientSide) {
            CameraShakeManager.addCameraShake(new CameraShakeData(level, ticks, origin, radius));
        }
    }

    /** A spell damage source that still works when the caster is gone (credited to the projectile then). */
    public static SpellDamageSource damageSource(AbstractSpell spell, Entity direct, @Nullable Entity caster) {
        return spell.getDamageSource(direct, caster != null ? caster : direct);
    }

    /** The caster, its summons and anything Iron's considers friendly fire are never hurt by the addon's spells. */
    public static boolean isAlly(@Nullable Entity caster, Entity other) {
        if (caster == null) {
            return false;
        }
        if (other == caster) {
            return true;
        }
        if (other instanceof OwnedSummon summon && caster.getUUID().equals(summon.getSummonerUUID())) {
            return true;
        }
        // A summon's own spells spare its master and the master's other summons
        if (caster instanceof OwnedSummon summon && summon.getSummonerUUID() != null
                && (summon.getSummonerUUID().equals(other.getUUID()) || other instanceof OwnedSummon sibling && summon.getSummonerUUID().equals(sibling.getSummonerUUID()))) {
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
