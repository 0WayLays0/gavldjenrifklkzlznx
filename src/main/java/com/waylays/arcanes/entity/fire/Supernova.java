package com.waylays.arcanes.entity.fire;

import com.waylays.arcanes.entity.SpellEffectEntity;
import com.waylays.arcanes.registry.ModEntities;
import com.waylays.arcanes.spells.SpellUtil;
import com.waylays.arcanes.spells.fire.FireFx;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** A supernova of Stellar Fire, to be seen: a sphere of flame swelling out, rings of light, sparks (the spell does the harm). */
public class Supernova extends SpellEffectEntity {
    public static final int LIFE = 24;

    private static final EntityDataAccessor<Float> DATA_RADIUS = SynchedEntityData.defineId(Supernova.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> DATA_HEAT = SynchedEntityData.defineId(Supernova.class, EntityDataSerializers.INT);

    public Supernova(EntityType<? extends Supernova> entityType, Level level) {
        super(entityType, level);
    }

    public static void at(Level level, Vec3 at, float radius, int tier) {
        Supernova nova = new Supernova(ModEntities.SUPERNOVA.get(), level);
        nova.moveTo(at.x, at.y, at.z, 0, 0);
        nova.entityData.set(DATA_RADIUS, radius);
        nova.entityData.set(DATA_HEAT, tier);
        level.addFreshEntity(nova);
        FireFx.burst(level, at.add(0, 1, 0), 1 + radius * .3f, tier);
        FireFx.sound(level, at, SoundEvents.GENERIC_EXPLODE.value(), 2 + radius * .2f, .6f + .08f * tier);
        SpellUtil.cameraShake(level, at, 15 + (int) radius * 2, radius * 3);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_RADIUS, 4f);
        builder.define(DATA_HEAT, 0);
    }

    public float getRadius() {
        return this.entityData.get(DATA_RADIUS);
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

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 192 * 192;
    }
}
