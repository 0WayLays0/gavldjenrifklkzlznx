package com.waylays.arcanes.entity.fire;

import com.waylays.arcanes.entity.SpellEffectEntity;
import com.waylays.arcanes.registry.ModEntities;
import com.waylays.arcanes.spells.fire.FireFx;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** A burst of Stellar Fire, only to be seen (and heard): a flash, a ball of flame blooming out, sparks, smoke. */
public class FireBurst extends SpellEffectEntity {
    public static final int LIFE = 20;

    private static final EntityDataAccessor<Float> DATA_SIZE = SynchedEntityData.defineId(FireBurst.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> DATA_HEAT = SynchedEntityData.defineId(FireBurst.class, EntityDataSerializers.INT);

    public FireBurst(EntityType<? extends FireBurst> entityType, Level level) {
        super(entityType, level);
    }

    public static void at(Level level, Vec3 at, float size, int tier) {
        FireBurst burst = new FireBurst(ModEntities.FIRE_BURST.get(), level);
        burst.moveTo(at.x, at.y, at.z, 0, 0);
        burst.entityData.set(DATA_SIZE, size);
        burst.entityData.set(DATA_HEAT, tier);
        level.addFreshEntity(burst);
        FireFx.burst(level, at.add(0, size * .4, 0), size, tier);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_SIZE, 1f);
        builder.define(DATA_HEAT, 0);
    }

    public float getSize() {
        return this.entityData.get(DATA_SIZE);
    }

    public int getHeat() {
        return this.entityData.get(DATA_HEAT);
    }

    @Override
    public void tick() {
        super.tick();
        if (level() instanceof ServerLevel && tickCount > LIFE) {
            discard();
        }
    }
}
