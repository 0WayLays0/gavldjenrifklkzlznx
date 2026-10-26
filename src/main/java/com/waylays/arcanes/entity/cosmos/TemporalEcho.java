package com.waylays.arcanes.entity.cosmos;

import com.waylays.arcanes.entity.SpellEffectEntity;
import com.waylays.arcanes.registry.ModEntities;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * An echo of a creature in time: its exact shape, made of scanlines of light, standing where it was a moment ago (a
 * past that was undone, a future that was erased) and fading. Only drawn; it does nothing.
 */
public class TemporalEcho extends SpellEffectEntity {
    private static final EntityDataAccessor<Integer> DATA_TARGET = SynchedEntityData.defineId(TemporalEcho.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_LIFE = SynchedEntityData.defineId(TemporalEcho.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_COLOR = SynchedEntityData.defineId(TemporalEcho.class, EntityDataSerializers.INT);

    public TemporalEcho(EntityType<? extends TemporalEcho> entityType, Level level) {
        super(entityType, level);
    }

    /** An echo of {@code target} at {@code at}, facing {@code yRot}, in {@code rgb} light, fading over {@code life} ticks. */
    public static void leave(Level level, Entity target, Vec3 at, float yRot, int life, int rgb) {
        if (level.isClientSide) {
            return;
        }
        TemporalEcho echo = new TemporalEcho(ModEntities.TEMPORAL_ECHO.get(), level);
        echo.moveTo(at.x, at.y, at.z, yRot, 0);
        echo.entityData.set(DATA_TARGET, target.getId());
        echo.entityData.set(DATA_LIFE, life);
        echo.entityData.set(DATA_COLOR, rgb);
        level.addFreshEntity(echo);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_TARGET, -1);
        builder.define(DATA_LIFE, 20);
        builder.define(DATA_COLOR, 0xFF8A1E);
    }

    @Nullable
    public Entity getTarget() {
        return level().getEntity(this.entityData.get(DATA_TARGET));
    }

    public int getLife() {
        return this.entityData.get(DATA_LIFE);
    }

    public int getColor() {
        return this.entityData.get(DATA_COLOR);
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide && tickCount >= getLife()) {
            discard();
        }
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 96 * 96;
    }
}
