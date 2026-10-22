package com.waylays.arcanes.entity.ritual;

import com.waylays.arcanes.registry.ModEntities;
import com.waylays.arcanes.spells.SpellUtil;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.Set;
import java.util.UUID;

/**
 * The Dome of Protection: a great dome of light over the place. Hostile creatures cannot enter it (they are pushed
 * back out), their shots break on it, and players under it resist damage.
 */
public class RitualDome extends RitualEntity {
    public static final float RADIUS = 16;
    public static final int LIFE = 20 * 60 * 5;
    public static final int RISE = 40;

    public RitualDome(EntityType<? extends RitualDome> entityType, Level level) {
        super(entityType, level);
    }

    public static RitualDome raise(ServerLevel level, Vec3 at, float radius, int life, Set<UUID> allies, float power) {
        RitualDome dome = new RitualDome(ModEntities.RITUAL_DOME.get(), level);
        dome.moveTo(at.x, at.y, at.z, 0, 0);
        dome.setup(life, radius, allies, power);
        level.addFreshEntity(dome);
        return dome;
    }

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel level) || tickCount < RISE / 2) {
            return;
        }
        float r = getSize();
        Vec3 c = position();
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(r + 2, r + 2, r + 2))) {
            double d = e.position().distanceTo(c);
            if (foe(e) && d < r + .5) {
                Vec3 out = e.position().subtract(c);
                out = out.lengthSqr() < 1.0E-4 ? new Vec3(1, 0, 0) : out.normalize();
                e.setDeltaMovement(out.scale(.7).add(0, .2, 0));
                e.hurtMarked = true;
                if (tickCount % 10 == 0) {
                    SpellUtil.particles(level, ParticleTypes.END_ROD, e.getX(), e.getY() + 1, e.getZ(), 6, .3, .5, .3, .05);
                }
            } else if (e instanceof Player && d < r && tickCount % 40 == 0) {
                e.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 60, 0, true, true));
            }
        }
        for (Projectile shot : level.getEntitiesOfClass(Projectile.class, getBoundingBox().inflate(r + 4))) {
            if (!(shot.getOwner() instanceof Enemy)) {
                continue;
            }
            double now = shot.position().distanceTo(c);
            double next = shot.position().add(shot.getDeltaMovement()).distanceTo(c);
            if (now >= r - 1 && next < r + 1) {
                SpellUtil.particles(level, ParticleTypes.END_ROD, shot.getX(), shot.getY(), shot.getZ(), 8, .2, .2, .2, .1);
                shot.discard();
            }
        }
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 256 * 256;
    }
}
