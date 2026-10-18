package com.waylays.arcanes.entity.book;

import com.waylays.arcanes.entity.SpellEffectEntity;
import com.waylays.arcanes.registry.ModEntities;
import com.waylays.arcanes.spells.book.BookFx;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import javax.annotation.Nullable;

/**
 * The look of Rewrite: bands of glowing script wind around the caster and rise as their story is written anew.
 * Visual only.
 */
public class RewriteAura extends SpellEffectEntity {
    public static final int LIFETIME = 36;

    private static final EntityDataAccessor<Integer> DATA_TARGET = SynchedEntityData.defineId(RewriteAura.class, EntityDataSerializers.INT);

    public RewriteAura(EntityType<? extends RewriteAura> entityType, Level level) {
        super(entityType, level);
    }

    public RewriteAura(Level level, LivingEntity target) {
        this(ModEntities.REWRITE_AURA.get(), level);
        setOwner(target);
        this.entityData.set(DATA_TARGET, target.getId());
        moveTo(target.getX(), target.getY(), target.getZ());
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(DATA_TARGET, -1);
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
        if (level().isClientSide) {
            if (target != null && tickCount < LIFETIME - 8) {
                float a = tickCount * .5f;
                float r = target.getBbWidth() * .7f + .4f;
                double y = target.getY() + (tickCount / (float) LIFETIME) * target.getBbHeight() * 1.2;
                level().addParticle(BookFx.GILT_DUST, target.getX() + Mth.cos(a) * r, y, target.getZ() + Mth.sin(a) * r, 0, .02, 0);
                level().addParticle(ParticleTypes.ENCHANT, target.getX() - Mth.cos(a) * r, y + .3, target.getZ() - Mth.sin(a) * r, 0, .3, 0);
            }
            return;
        }
        if (target == null || tickCount >= LIFETIME) {
            discard();
        }
    }

    @Override
    public AABB getBoundingBoxForCulling() {
        return getBoundingBox().inflate(2, 4, 2);
    }
}
