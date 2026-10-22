package com.waylays.arcanes.entity.wall;

import com.waylays.arcanes.entity.SpellEffectEntity;
import com.waylays.arcanes.registry.ModEntities;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** Where Wall Magic strikes: a ring of embers racing over the ground, cracks glowing, bricks flying out of it. */
public class WallImpact extends SpellEffectEntity {
    public static final int LIFE = 28;
    private static final EntityDataAccessor<Float> DATA_SIZE = SynchedEntityData.defineId(WallImpact.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> DATA_BRICKS = SynchedEntityData.defineId(WallImpact.class, EntityDataSerializers.INT);

    public WallImpact(EntityType<? extends WallImpact> entityType, Level level) {
        super(entityType, level);
    }

    public static WallImpact strike(Level level, Vec3 at, float size, int bricks) {
        WallImpact impact = new WallImpact(ModEntities.WALL_IMPACT.get(), level);
        impact.moveTo(at.x, at.y, at.z, level.random.nextFloat() * 360, 0);
        impact.entityData.set(DATA_SIZE, size);
        impact.entityData.set(DATA_BRICKS, bricks);
        level.addFreshEntity(impact);
        return impact;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_SIZE, 2f);
        builder.define(DATA_BRICKS, 6);
    }

    public float getSize() {
        return this.entityData.get(DATA_SIZE);
    }

    public int getBricks() {
        return this.entityData.get(DATA_BRICKS);
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
