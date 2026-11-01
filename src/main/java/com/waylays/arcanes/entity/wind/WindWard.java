package com.waylays.arcanes.entity.wind;

import com.waylays.arcanes.entity.SpellEffectEntity;
import com.waylays.arcanes.registry.ModEntities;
import com.waylays.arcanes.spells.wind.WindFx;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

/**
 * Tailwind's ward: rings of wind circling someone for a while; shots that come at them are turned aside. It moves with
 * them and ends with them.
 */
public class WindWard extends SpellEffectEntity {
    public static final double REACH = 2.6;

    private static final EntityDataAccessor<Integer> DATA_HOLDER = SynchedEntityData.defineId(WindWard.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_LIFE = SynchedEntityData.defineId(WindWard.class, EntityDataSerializers.INT);

    public WindWard(EntityType<? extends WindWard> entityType, Level level) {
        super(entityType, level);
    }

    /** Wards {@code holder} for {@code life} ticks (its ward renewed if it has one). */
    public static WindWard on(Level level, LivingEntity holder, @Nullable Entity caster, int life) {
        for (WindWard old : level.getEntitiesOfClass(WindWard.class, holder.getBoundingBox().inflate(1))) {
            if (old.getHolder() == holder) {
                old.discard();
            }
        }
        WindWard ward = new WindWard(ModEntities.WIND_WARD.get(), level);
        ward.setOwner(caster);
        ward.moveTo(holder.getX(), holder.getY(), holder.getZ(), 0, 0);
        ward.entityData.set(DATA_HOLDER, holder.getId());
        ward.entityData.set(DATA_LIFE, life);
        level.addFreshEntity(ward);
        return ward;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_HOLDER, -1);
        builder.define(DATA_LIFE, 200);
    }

    @Nullable
    public Entity getHolder() {
        return level().getEntity(this.entityData.get(DATA_HOLDER));
    }

    public int getLife() {
        return this.entityData.get(DATA_LIFE);
    }

    @Override
    public void tick() {
        super.tick();
        Entity holder = getHolder();
        if (holder != null) {
            setPos(holder.getX(), holder.getY(), holder.getZ());
        }
        if (!(level() instanceof ServerLevel level)) {
            return;
        }
        if (holder == null || !holder.isAlive() || tickCount > getLife()) {
            discard();
            return;
        }
        Vec3 heart = holder.position().add(0, holder.getBbHeight() * .5, 0);
        if (WindFx.deflect(level, holder.getBoundingBox().inflate(REACH), heart, holder, .8) > 0) {
            level.playSound(null, heart.x, heart.y, heart.z, SoundEvents.BREEZE_DEFLECT, SoundSource.PLAYERS, 1, 1.2f);
            level.sendParticles(ParticleTypes.GUST, heart.x, heart.y, heart.z, 1, .3, .3, .3, 0);
        }
    }
}
