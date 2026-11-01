package com.waylays.arcanes.entity.wind;

import com.waylays.arcanes.entity.SpellEffectEntity;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import org.joml.Vector3f;

/**
 * A short-lived sight of Wind Magic whose work was done when it was cast (a Gale's blast, an Updraft's lift): it only
 * has a way it faces, a size and a life, for its renderer.
 */
public abstract class WindVisual extends SpellEffectEntity {
    private static final EntityDataAccessor<Vector3f> DATA_DIR = SynchedEntityData.defineId(WindVisual.class, EntityDataSerializers.VECTOR3);
    private static final EntityDataAccessor<Float> DATA_SIZE = SynchedEntityData.defineId(WindVisual.class, EntityDataSerializers.FLOAT);

    protected WindVisual(EntityType<?> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_DIR, new Vector3f(0, 0, 1));
        builder.define(DATA_SIZE, 1f);
    }

    protected void setup(Vector3f dir, float size) {
        this.entityData.set(DATA_DIR, dir);
        this.entityData.set(DATA_SIZE, size);
    }

    public Vector3f getDir() {
        return new Vector3f(this.entityData.get(DATA_DIR));
    }

    public float getSize() {
        return this.entityData.get(DATA_SIZE);
    }

    /** Ticks it lasts. */
    public abstract int life();

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide && tickCount > life()) {
            discard();
        }
    }
}
