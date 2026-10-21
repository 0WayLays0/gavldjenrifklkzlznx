package com.waylays.arcanes.entity.rune;

import com.waylays.arcanes.entity.SpellEffectEntity;
import com.waylays.arcanes.registry.ModEntities;
import com.waylays.arcanes.spells.rune.RuneFx;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/**
 * A slab of the Runic Bulwark: a carved stone wall segment that rises out of the ground. It is solid (nothing walks
 * through it) and cannot be hurt, so arrows and spells thrown at it stop or glance off; then it crumbles.
 */
public class RuneBulwark extends SpellEffectEntity {
    public static final int RISE_TICKS = 8;
    public static final int CRUMBLE_TICKS = 12;

    private static final EntityDataAccessor<Integer> DATA_LIFE = SynchedEntityData.defineId(RuneBulwark.class, EntityDataSerializers.INT);

    public RuneBulwark(EntityType<? extends RuneBulwark> entityType, Level level) {
        super(entityType, level);
    }

    /** A segment facing {@code yaw}, standing {@code life} ticks. */
    public static RuneBulwark raise(Level level, LivingEntity caster, Vec3 at, float yaw, int life) {
        RuneBulwark wall = new RuneBulwark(ModEntities.RUNE_BULWARK.get(), level);
        wall.setOwner(caster);
        wall.moveTo(at.x, at.y, at.z, yaw, 0);
        wall.entityData.set(DATA_LIFE, life);
        level.addFreshEntity(wall);
        return wall;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_LIFE, 100);
    }

    public int getLife() {
        return this.entityData.get(DATA_LIFE);
    }

    /** 0 buried, 1 standing, back to 0 as it crumbles. */
    public float standing(float partialTicks) {
        float age = tickCount + partialTicks;
        float up = Mth.clamp(age / RISE_TICKS, 0, 1);
        float down = Mth.clamp((age - getLife()) / CRUMBLE_TICKS, 0, 1);
        return (1 - (1 - up) * (1 - up)) * (1 - down);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            return;
        }
        if (tickCount == getLife()) {
            RuneFx.stone(level(), position().add(0, 1.2, 0), Blocks.DEEPSLATE_TILES.defaultBlockState(), 30, .6);
            RuneFx.sound(level(), position(), SoundEvents.DEEPSLATE_TILES_BREAK, 1.2f, .7f);
        }
        if (tickCount >= getLife() + CRUMBLE_TICKS) {
            discard();
        }
    }

    /** Solid: walls stop whoever walks into them, the caster too. */
    @Override
    public boolean canBeCollidedWith() {
        return tickCount < getLife();
    }

    /** Projectiles strike it (and are stopped: it cannot be hurt). */
    @Override
    public boolean isPickable() {
        return tickCount < getLife();
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (!level().isClientSide && !isRemoved()) {
            Vec3 at = source.getSourcePosition() != null ? source.getSourcePosition() : position().add(0, 1.2, 0);
            RuneFx.glyphs(level(), at, 6, .3);
            RuneFx.sound(level(), at, SoundEvents.DEEPSLATE_TILES_HIT, 1f, .8f);
        }
        return false;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 96 * 96;
    }
}
