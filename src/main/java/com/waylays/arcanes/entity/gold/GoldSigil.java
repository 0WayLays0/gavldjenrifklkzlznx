package com.waylays.arcanes.entity.gold;

import com.waylays.arcanes.entity.SpellEffectEntity;
import com.waylays.arcanes.registry.ModEntities;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** The incantation of Gold magic: the Sun Stone of Eldorado burning on the ground for a moment, its rings turning. */
public class GoldSigil extends SpellEffectEntity {
    private static final EntityDataAccessor<Float> DATA_RADIUS = SynchedEntityData.defineId(GoldSigil.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> DATA_LIFE = SynchedEntityData.defineId(GoldSigil.class, EntityDataSerializers.INT);

    public GoldSigil(EntityType<? extends GoldSigil> entityType, Level level) {
        super(entityType, level);
    }

    public static GoldSigil burn(Level level, Vec3 at, float radius, int life) {
        GoldSigil sigil = new GoldSigil(ModEntities.GOLD_SIGIL.get(), level);
        sigil.moveTo(at.x, at.y, at.z, level.random.nextFloat() * 360, 0);
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

    public float strength(float partialTicks) {
        float age = tickCount + partialTicks;
        return Mth.clamp(Math.min(age / 6, (this.entityData.get(DATA_LIFE) - age) / 10), 0, 1);
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
