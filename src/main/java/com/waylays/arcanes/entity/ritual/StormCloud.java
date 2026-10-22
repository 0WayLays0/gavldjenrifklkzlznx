package com.waylays.arcanes.entity.ritual;

import com.waylays.arcanes.registry.ModEntities;
import com.waylays.arcanes.spells.SpellUtil;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * The Call of the Storm: a dark storm cloud gathers over the place, rain falls under it, and lightning strikes a
 * hostile creature under it every second and a half.
 */
public class StormCloud extends RitualEntity {
    public static final float RADIUS = 24;
    public static final int LIFE = 20 * 60;
    public static final float HEIGHT = 18;
    public static final int STRIKE_TICKS = 30;
    public static final float DAMAGE = 10;

    public StormCloud(EntityType<? extends StormCloud> entityType, Level level) {
        super(entityType, level);
    }

    public static StormCloud gather(ServerLevel level, Vec3 at, float radius, int life, Set<UUID> allies, float power) {
        StormCloud storm = new StormCloud(ModEntities.STORM_CLOUD.get(), level);
        storm.moveTo(at.x, at.y, at.z, 0, 0);
        storm.setup(life, radius, allies, power);
        level.addFreshEntity(storm);
        level.playSound(null, at.x, at.y, at.z, SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.WEATHER, 4f, .6f);
        return storm;
    }

    @Override
    public void tick() {
        super.tick();
        float r = getSize();
        if (level().isClientSide) {
            // The cloud and its rain, thicker as it gathers
            float strength = fade(0, 60, 60);
            for (int i = 0; i < 6 * strength; i++) {
                double a = random.nextDouble() * Math.PI * 2;
                double d = Math.sqrt(random.nextDouble()) * r;
                double x = getX() + Math.cos(a) * d;
                double z = getZ() + Math.sin(a) * d;
                level().addParticle(ParticleTypes.LARGE_SMOKE, x, getY() + HEIGHT + random.nextDouble() * 2, z, 0, 0, 0);
                if (i % 2 == 0) {
                    level().addParticle(ParticleTypes.FALLING_WATER, x, getY() + HEIGHT - 1, z, 0, 0, 0);
                }
            }
            return;
        }
        ServerLevel level = (ServerLevel) level();
        if (tickCount < 60 || tickCount % STRIKE_TICKS != 0 || tickCount > getLife() - 40) {
            return;
        }
        List<LivingEntity> foes = level.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(r, HEIGHT, r),
                e -> foe(e) && e.distanceToSqr(getX(), e.getY(), getZ()) < r * r);
        if (foes.isEmpty()) {
            if (random.nextInt(3) == 0) {
                level.playSound(null, getX(), getY() + HEIGHT, getZ(), SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.WEATHER, 2f, .8f);
            }
            return;
        }
        LivingEntity target = foes.get(random.nextInt(foes.size()));
        LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
        if (bolt != null) {
            bolt.moveTo(target.getX(), target.getY(), target.getZ());
            bolt.setVisualOnly(true);
            level.addFreshEntity(bolt);
        }
        target.invulnerableTime = 0;
        target.hurt(level.damageSources().lightningBolt(), DAMAGE * power);
        target.igniteForSeconds(3);
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 2));
        SpellUtil.particles(level, ParticleTypes.ELECTRIC_SPARK, target.getX(), target.getY() + 1, target.getZ(), 20, .4, .6, .4, .3);
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 256 * 256;
    }
}
