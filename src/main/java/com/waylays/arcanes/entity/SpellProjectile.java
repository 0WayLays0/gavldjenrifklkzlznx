package com.waylays.arcanes.entity;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.entity.mobs.AntiMagicSusceptible;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.ForgeEventFactory;

/**
 * Base of the addon's spell projectiles. Built on vanilla {@link Projectile} only (no Iron's internal base class), so a
 * different Iron's Spells build cannot break it. Hits are resolved on the server; the client only moves and draws.
 */
public abstract class SpellProjectile extends Projectile implements AntiMagicSusceptible {
    protected float damage;

    protected SpellProjectile(EntityType<? extends SpellProjectile> entityType, Level level) {
        super(entityType, level);
    }

    public void setDamage(float damage) {
        this.damage = damage;
    }

    public float getDamage() {
        return damage;
    }

    /** Sets the velocity: {@code direction} is normalized and scaled by {@code speed}. */
    public void shoot(Vec3 direction, float speed) {
        setDeltaMovement(direction.normalize().scale(speed));
    }

    /** Downward acceleration per tick (0 = straight flight). */
    protected double gravity() {
        return 0;
    }

    /** Ticks before the projectile disappears on its own. */
    protected int lifetime() {
        return 15 * 20;
    }

    /** A piercing projectile keeps flying after an entity hit unless {@link #onImpact} discards it. */
    protected boolean piercesEntities() {
        return false;
    }

    /** Server side: what the projectile does when it hits something. Usually ends with {@link #discard()}. */
    protected abstract void onImpact(HitResult hit);

    /** Client side, every tick: trail particles. */
    protected void clientTick() {
    }

    @Override
    public void tick() {
        super.tick();
        if (tickCount > lifetime()) {
            if (!level().isClientSide) {
                onExpire();
            }
            discard();
            return;
        }
        if (level().isClientSide) {
            clientTick();
        }
        HitResult hit = ProjectileUtil.getHitResultOnMoveVector(this, this::canHitEntity);
        if (hit.getType() != HitResult.Type.MISS && !ForgeEventFactory.onProjectileImpact(this, hit)) {
            if (!level().isClientSide) {
                onImpact(hit);
            }
            // The client waits for the server to remove the projectile instead of flying through the target,
            // unless it pierces: then the server decides in onImpact whether it goes on
            if (isRemoved() || hit.getType() != HitResult.Type.ENTITY || !piercesEntities()) {
                return;
            }
        }
        Vec3 motion = getDeltaMovement();
        setPos(getX() + motion.x, getY() + motion.y, getZ() + motion.z);
        ProjectileUtil.rotateTowardsMovement(this, 1);
        if (gravity() != 0) {
            setDeltaMovement(motion.x, motion.y - gravity(), motion.z);
        }
    }

    /** Server side: called when the lifetime runs out without a hit. */
    protected void onExpire() {
    }

    @Override
    protected boolean canHitEntity(Entity target) {
        return super.canHitEntity(target) && target != getOwner() && !(target instanceof SpellProjectile);
    }

    @Override
    public void checkDespawn() {
        if (level() instanceof ServerLevel serverLevel && !serverLevel.isPositionEntityTicking(blockPosition())) {
            discard();
        }
    }

    @Override
    public void onAntiMagic(MagicData playerMagicData) {
        discard();
    }

    @Override
    public boolean isOnFire() {
        return false;
    }

    @Override
    protected void defineSynchedData() {
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putFloat("Damage", damage);
        tag.putInt("Age", tickCount);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        damage = tag.getFloat("Damage");
        tickCount = tag.getInt("Age");
    }
}
