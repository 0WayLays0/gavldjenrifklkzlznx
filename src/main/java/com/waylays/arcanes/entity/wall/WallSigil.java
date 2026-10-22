package com.waylays.arcanes.entity.wall;

import com.waylays.arcanes.entity.SpellEffectEntity;
import com.waylays.arcanes.registry.ModEntities;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** The incantation of Wall Magic: the mason's sigil burning on the ground for a moment, where a spell is called. */
public class WallSigil extends SpellEffectEntity {
    private static final EntityDataAccessor<Float> DATA_RADIUS = SynchedEntityData.defineId(WallSigil.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> DATA_LIFE = SynchedEntityData.defineId(WallSigil.class, EntityDataSerializers.INT);

    public WallSigil(EntityType<? extends WallSigil> entityType, Level level) {
        super(entityType, level);
    }

    public static WallSigil burn(Level level, Vec3 at, float radius, int life) {
        WallSigil sigil = new WallSigil(ModEntities.WALL_SIGIL.get(), level);
        sigil.moveTo(at.x, at.y, at.z, level.random.nextFloat() * 90, 0);
        sigil.entityData.set(DATA_RADIUS, radius);
        sigil.entityData.set(DATA_LIFE, life);
        level.addFreshEntity(sigil);
        return sigil;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_RADIUS, 2f);
        builder.define(DATA_LIFE, 30);
    }

    public float getRadius() {
        return this.entityData.get(DATA_RADIUS);
    }

    /** How strongly it burns: lighting up, then dying out. */
    public float strength(float partialTicks) {
        float age = tickCount + partialTicks;
        int life = this.entityData.get(DATA_LIFE);
        return Mth.clamp(Math.min(age / 6, (life - age) / 10), 0, 1);
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide && tickCount > this.entityData.get(DATA_LIFE)) {
            discard();
        }
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 128 * 128;
    }
}
