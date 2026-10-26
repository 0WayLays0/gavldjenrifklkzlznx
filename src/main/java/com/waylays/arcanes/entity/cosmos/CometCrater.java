package com.waylays.arcanes.entity.cosmos;

import com.waylays.arcanes.entity.SpellEffectEntity;
import com.waylays.arcanes.registry.ModEntities;
import com.waylays.arcanes.spells.SpellUtil;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * The crater a comet leaves: a scorched round of ground glowing molten orange at its rim and ice-blue at its heart,
 * smoking, cooling over a few seconds. Only seen; the ground itself is not broken.
 */
public class CometCrater extends SpellEffectEntity {
    public static final int LIFE = 160;
    private static final EntityDataAccessor<Float> DATA_RADIUS = SynchedEntityData.defineId(CometCrater.class, EntityDataSerializers.FLOAT);

    public CometCrater(EntityType<? extends CometCrater> entityType, Level level) {
        super(entityType, level);
    }

    public static void mark(Level level, Vec3 at, float radius) {
        CometCrater crater = new CometCrater(ModEntities.COMET_CRATER.get(), level);
        Vec3 ground = SpellUtil.groundAt(level, at, 3, 1);
        crater.moveTo(ground.x, ground.y, ground.z, level.random.nextFloat() * 360, 0);
        crater.entityData.set(DATA_RADIUS, radius);
        level.addFreshEntity(crater);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_RADIUS, 4f);
    }

    public float getRadius() {
        return this.entityData.get(DATA_RADIUS);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            if (random.nextInt(3) == 0 && tickCount < LIFE - 20) {
                double a = random.nextDouble() * Math.PI * 2, r = random.nextDouble() * getRadius();
                level().addParticle(ParticleTypes.SMOKE, getX() + Math.cos(a) * r, getY() + .1, getZ() + Math.sin(a) * r, 0, .03, 0);
            }
            return;
        }
        if (tickCount >= LIFE) {
            discard();
        }
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 128 * 128;
    }
}
