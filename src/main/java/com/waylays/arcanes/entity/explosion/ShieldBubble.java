package com.waylays.arcanes.entity.explosion;

import com.waylays.arcanes.effect.ReactiveShieldEffect;
import com.waylays.arcanes.registry.ModEntities;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import java.util.List;
import javax.annotation.Nullable;

/**
 * The visible shell of Reactive Shield, following its bearer so every player sees it (mob effects are only sent to
 * the bearer). Purely visual: the effect does the work. Gone when the shield is.
 */
public class ShieldBubble extends Entity {
    private static final EntityDataAccessor<Integer> DATA_OWNER = SynchedEntityData.defineId(ShieldBubble.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_CHARGES = SynchedEntityData.defineId(ShieldBubble.class, EntityDataSerializers.INT);
    /** Tick of the last explosion, for the flash. */
    private static final EntityDataAccessor<Integer> DATA_FLASH = SynchedEntityData.defineId(ShieldBubble.class, EntityDataSerializers.INT);

    public ShieldBubble(EntityType<? extends ShieldBubble> entityType, Level level) {
        super(entityType, level);
        noPhysics = true;
    }

    /** Wraps {@code owner} in a fresh bubble, replacing any it already had. */
    public static void attach(LivingEntity owner) {
        Level level = owner.level();
        if (level.isClientSide) {
            return;
        }
        for (ShieldBubble old : bubblesOf(owner)) {
            old.discard();
        }
        ShieldBubble bubble = new ShieldBubble(ModEntities.SHIELD_BUBBLE.get(), level);
        bubble.entityData.set(DATA_OWNER, owner.getId());
        bubble.entityData.set(DATA_CHARGES, Math.max(1, ReactiveShieldEffect.charges(owner)));
        bubble.moveTo(owner.getX(), owner.getY(), owner.getZ());
        level.addFreshEntity(bubble);
    }

    public static List<ShieldBubble> bubblesOf(LivingEntity owner) {
        return owner.level().getEntitiesOfClass(ShieldBubble.class, owner.getBoundingBox().inflate(3), b -> b.getOwnerId() == owner.getId());
    }

    public int getOwnerId() {
        return this.entityData.get(DATA_OWNER);
    }

    @Nullable
    public LivingEntity getOwner() {
        return level().getEntity(getOwnerId()) instanceof LivingEntity living ? living : null;
    }

    public int getCharges() {
        return this.entityData.get(DATA_CHARGES);
    }

    public int getFlashTick() {
        return this.entityData.get(DATA_FLASH);
    }

    @Override
    public void tick() {
        super.tick();
        LivingEntity owner = getOwner();
        if (level().isClientSide) {
            if (owner != null) {
                setPos(owner.getX(), owner.getY(), owner.getZ());
            }
            return;
        }
        int charges = owner == null || !owner.isAlive() ? 0 : ReactiveShieldEffect.charges(owner);
        if (charges <= 0) {
            discard();
            return;
        }
        setPos(owner.getX(), owner.getY(), owner.getZ());
        if (charges < getCharges()) {
            this.entityData.set(DATA_FLASH, tickCount);
        }
        this.entityData.set(DATA_CHARGES, charges);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_OWNER, -1);
        builder.define(DATA_CHARGES, ReactiveShieldEffect.CHARGES);
        builder.define(DATA_FLASH, -100);
    }

    @Override
    public AABB getBoundingBoxForCulling() {
        return getBoundingBox().inflate(2);
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
    }
}
