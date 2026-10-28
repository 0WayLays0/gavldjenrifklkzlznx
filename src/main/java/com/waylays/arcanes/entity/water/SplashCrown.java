package com.waylays.arcanes.entity.water;

import com.waylays.arcanes.entity.SpellEffectEntity;
import com.waylays.arcanes.registry.ModEntities;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * A splash of Water Magic, only to be seen: a crown of water leaping up in a ring and falling back, foam spreading out
 * over the ground, drops flying. Left where a lance strikes, a bubble bursts, a geyser falls.
 */
public class SplashCrown extends SpellEffectEntity {
    public static final int LIFE = 16;

    private static final EntityDataAccessor<Float> DATA_SIZE = SynchedEntityData.defineId(SplashCrown.class, EntityDataSerializers.FLOAT);

    public SplashCrown(EntityType<? extends SplashCrown> entityType, Level level) {
        super(entityType, level);
    }

    public static void at(Level level, Vec3 at, float size) {
        SplashCrown crown = new SplashCrown(ModEntities.SPLASH_CROWN.get(), level);
        crown.moveTo(at.x, at.y, at.z, level.random.nextFloat() * 360, 0);
        crown.entityData.set(DATA_SIZE, size);
        level.addFreshEntity(crown);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_SIZE, 1f);
    }

    public float getSize() {
        return this.entityData.get(DATA_SIZE);
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide && tickCount > LIFE) {
            discard();
        }
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 96 * 96;
    }
}
