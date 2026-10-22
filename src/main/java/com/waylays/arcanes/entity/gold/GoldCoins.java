package com.waylays.arcanes.entity.gold;

import com.waylays.arcanes.entity.SpellEffectEntity;
import com.waylays.arcanes.registry.ModEntities;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * A spray of gold coins (drawn only: they are not items and cannot be picked up): thrown out of a point, they spin,
 * fall, bounce once on the ground with a glint, and fade.
 */
public class GoldCoins extends SpellEffectEntity {
    public static final int LIFE = 40;
    private static final EntityDataAccessor<Integer> DATA_COUNT = SynchedEntityData.defineId(GoldCoins.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DATA_FORCE = SynchedEntityData.defineId(GoldCoins.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_FLOOR = SynchedEntityData.defineId(GoldCoins.class, EntityDataSerializers.FLOAT);

    public GoldCoins(EntityType<? extends GoldCoins> entityType, Level level) {
        super(entityType, level);
    }

    public static GoldCoins spray(Level level, Vec3 at, int count, float force) {
        GoldCoins coins = new GoldCoins(ModEntities.GOLD_COINS.get(), level);
        coins.moveTo(at.x, at.y, at.z, level.random.nextFloat() * 360, 0);
        coins.entityData.set(DATA_COUNT, count);
        coins.entityData.set(DATA_FORCE, force);
        // Where the ground is under it, for the coins to bounce on
        float floor = 0;
        for (int k = 0; k < 8; k++) {
            if (!level.getBlockState(coins.blockPosition().below(k)).getCollisionShape(level, coins.blockPosition().below(k)).isEmpty()) {
                floor = (float) (coins.blockPosition().below(k).getY() + 1 - at.y);
                break;
            }
            floor = -8;
        }
        coins.entityData.set(DATA_FLOOR, floor);
        level.addFreshEntity(coins);
        return coins;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_COUNT, 10);
        builder.define(DATA_FORCE, .4f);
        builder.define(DATA_FLOOR, -1f);
    }

    public int getCount() {
        return this.entityData.get(DATA_COUNT);
    }

    public float getForce() {
        return this.entityData.get(DATA_FORCE);
    }

    /** The ground's height from the spray's middle. */
    public float getFloor() {
        return this.entityData.get(DATA_FLOOR);
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
