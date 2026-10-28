package com.waylays.arcanes.entity.water;

import com.waylays.arcanes.entity.SpellEffectEntity;
import com.waylays.arcanes.registry.ModEntities;
import com.waylays.arcanes.spells.SpellUtil;
import com.waylays.arcanes.spells.water.WaterFx;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;

/**
 * Cleansing Rain: a small dark cloud gathers over the caster and follows them, raining. In its rain the caster and their
 * allies are mended now and then and washed clean of one harm each time (poison, slowness, weakness...), and fire goes
 * out; enemies under it are soaked.
 */
public class RainCloud extends SpellEffectEntity {
    public static final float RADIUS = 5;
    public static final float HEIGHT = 4.6f;
    public static final int GATHER = 16;
    public static final int PULSE = 30;

    private static final EntityDataAccessor<Integer> DATA_OWNER = SynchedEntityData.defineId(RainCloud.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_LIFE = SynchedEntityData.defineId(RainCloud.class, EntityDataSerializers.INT);

    private float mend;

    public RainCloud(EntityType<? extends RainCloud> entityType, Level level) {
        super(entityType, level);
    }

    /** Gathers the caster's cloud (a cloud already over them is replaced). */
    public static RainCloud gather(Level level, LivingEntity caster, int life, float mend) {
        for (RainCloud old : level.getEntitiesOfClass(RainCloud.class, caster.getBoundingBox().inflate(6), c -> caster.getUUID().equals(c.getOwnerUUID()))) {
            old.discard();
        }
        RainCloud cloud = new RainCloud(ModEntities.RAIN_CLOUD.get(), level);
        cloud.setOwner(caster);
        cloud.mend = mend;
        cloud.moveTo(caster.getX(), caster.getY(), caster.getZ(), 0, 0);
        cloud.entityData.set(DATA_OWNER, caster.getId());
        cloud.entityData.set(DATA_LIFE, life);
        level.addFreshEntity(cloud);
        WaterFx.sound(level, caster.position().add(0, HEIGHT, 0), SoundEvents.WEATHER_RAIN_ABOVE, 1.5f, 1);
        return cloud;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_OWNER, -1);
        builder.define(DATA_LIFE, 200);
    }

    public int getLife() {
        return this.entityData.get(DATA_LIFE);
    }

    @Nullable
    public Entity follows() {
        return level().getEntity(this.entityData.get(DATA_OWNER));
    }

    /** How thick the cloud is (0 to 1): gathering, then thinning away at the end. */
    public float thickness(float age) {
        return Math.min(Math.min(1, age / GATHER), Math.max(0, (getLife() - age) / GATHER));
    }

    @Override
    public void tick() {
        super.tick();
        Entity owner = follows();
        if (owner != null) {
            setPos(owner.getX(), owner.getY(), owner.getZ());
        }
        if (!(level() instanceof ServerLevel level)) {
            return;
        }
        LivingEntity caster = getLivingOwner();
        if (caster == null || !caster.isAlive() || tickCount > getLife()) {
            discard();
            return;
        }
        if (tickCount % 40 == 0) {
            WaterFx.sound(level, position().add(0, HEIGHT, 0), SoundEvents.WEATHER_RAIN, 1.2f, 1.1f);
        }
        if (tickCount % 4 == 0) {
            SpellUtil.particles(level, ParticleTypes.SPLASH, getX(), getY() + .1, getZ(), 6, RADIUS * .6, 0, RADIUS * .6, .1);
        }
        if (tickCount < GATHER || tickCount % PULSE != 0) {
            return;
        }
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(RADIUS, 3, RADIUS))) {
            if (!e.isAlive() || e.distanceToSqr(this) > (RADIUS + .5) * (RADIUS + .5)) {
                continue;
            }
            e.clearFire();
            if (e == caster || SpellUtil.isAlly(caster, e)) {
                if (e.getHealth() < e.getMaxHealth()) {
                    e.heal(mend);
                }
                // Washed clean of one harm
                for (MobEffectInstance effect : e.getActiveEffects()) {
                    if (effect.getEffect().value().getCategory() == MobEffectCategory.HARMFUL) {
                        e.removeEffect(effect.getEffect());
                        break;
                    }
                }
                WaterFx.mist(level, e.position().add(0, 1, 0), 6, .4);
            } else {
                WaterFx.soak(e, PULSE + 10);
            }
        }
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 128 * 128;
    }
}
