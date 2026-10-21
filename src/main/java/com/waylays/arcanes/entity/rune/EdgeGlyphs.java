package com.waylays.arcanes.entity.rune;

import com.waylays.arcanes.entity.SpellEffectEntity;
import com.waylays.arcanes.registry.ModEffects;
import com.waylays.arcanes.registry.ModEntities;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;

/** The look of the Edge Rune on its bearer: runes circling the weapon hand for as long as the rune holds. */
public class EdgeGlyphs extends SpellEffectEntity {
    private static final EntityDataAccessor<Integer> DATA_TARGET = SynchedEntityData.defineId(EdgeGlyphs.class, EntityDataSerializers.INT);

    public EdgeGlyphs(EntityType<? extends EdgeGlyphs> entityType, Level level) {
        super(entityType, level);
    }

    /** Puts the runes on {@code bearer} (unless it already wears some). */
    public static void attach(Level level, LivingEntity bearer) {
        if (!level.getEntitiesOfClass(EdgeGlyphs.class, bearer.getBoundingBox().inflate(2), g -> g.getTarget() == bearer).isEmpty()) {
            return;
        }
        EdgeGlyphs glyphs = new EdgeGlyphs(ModEntities.EDGE_GLYPHS.get(), level);
        glyphs.entityData.set(DATA_TARGET, bearer.getId());
        glyphs.moveTo(bearer.getX(), bearer.getY(), bearer.getZ());
        level.addFreshEntity(glyphs);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_TARGET, -1);
    }

    @Nullable
    public LivingEntity getTarget() {
        return level().getEntity(this.entityData.get(DATA_TARGET)) instanceof LivingEntity living ? living : null;
    }

    @Override
    public void tick() {
        super.tick();
        LivingEntity target = getTarget();
        if (target != null) {
            setPos(target.getX(), target.getY(), target.getZ());
        }
        if (!level().isClientSide && tickCount % 10 == 0 && (target == null || !target.isAlive() || !target.hasEffect(ModEffects.RUNE_EDGE))) {
            discard();
        }
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 64 * 64;
    }
}
