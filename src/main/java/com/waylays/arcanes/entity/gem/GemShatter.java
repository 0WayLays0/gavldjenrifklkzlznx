package com.waylays.arcanes.entity.gem;

import com.waylays.arcanes.entity.SpellEffectEntity;
import com.waylays.arcanes.registry.ModEntities;
import com.waylays.arcanes.spells.gem.Gem;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** A gem breaking: a flash, then shards of it flying out in every direction, tumbling and glinting, then gone. */
public class GemShatter extends SpellEffectEntity {
    public static final int LIFE = 24;
    private static final EntityDataAccessor<Integer> DATA_GEM = SynchedEntityData.defineId(GemShatter.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DATA_SIZE = SynchedEntityData.defineId(GemShatter.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> DATA_SHARDS = SynchedEntityData.defineId(GemShatter.class, EntityDataSerializers.INT);

    public GemShatter(EntityType<? extends GemShatter> entityType, Level level) {
        super(entityType, level);
    }

    public static GemShatter burst(Level level, Vec3 at, Gem gem, float size, int shards) {
        GemShatter shatter = new GemShatter(ModEntities.GEM_SHATTER.get(), level);
        shatter.moveTo(at.x, at.y, at.z, level.random.nextFloat() * 360, 0);
        shatter.entityData.set(DATA_GEM, gem.ordinal());
        shatter.entityData.set(DATA_SIZE, size);
        shatter.entityData.set(DATA_SHARDS, shards);
        level.addFreshEntity(shatter);
        return shatter;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_GEM, 0);
        builder.define(DATA_SIZE, 1f);
        builder.define(DATA_SHARDS, 8);
    }

    public Gem getGem() {
        return Gem.of(this.entityData.get(DATA_GEM));
    }

    public float getSize() {
        return this.entityData.get(DATA_SIZE);
    }

    public int getShards() {
        return this.entityData.get(DATA_SHARDS);
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
        return distance < 128 * 128;
    }
}
