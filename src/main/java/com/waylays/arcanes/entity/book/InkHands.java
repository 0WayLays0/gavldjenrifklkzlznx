package com.waylays.arcanes.entity.book;

import com.waylays.arcanes.registry.ModEntities;
import com.waylays.arcanes.registry.ModSpells;
import com.waylays.arcanes.spells.SpellUtil;
import com.waylays.arcanes.spells.book.BookFx;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.damage.DamageSources;
import io.redspace.ironsspellbooks.entity.mobs.AntiMagicSusceptible;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import java.util.UUID;
import javax.annotation.Nullable;

/**
 * Black hands of living ink rising out of the ground around a victim: they seize it (it cannot move), blind it and
 * squeeze it every second, then sink back into the ink.
 */
public class InkHands extends Entity implements AntiMagicSusceptible {
    public static final int RISE_TICKS = 6;
    public static final int SINK_TICKS = 8;
    public static final int SQUEEZE_INTERVAL = 20;

    private static final EntityDataAccessor<Integer> DATA_TARGET = SynchedEntityData.defineId(InkHands.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_HOLD = SynchedEntityData.defineId(InkHands.class, EntityDataSerializers.INT);

    @Nullable
    private UUID ownerUUID;
    private float damage;

    public InkHands(EntityType<? extends InkHands> entityType, Level level) {
        super(entityType, level);
        noPhysics = true;
    }

    public InkHands(Level level, @Nullable LivingEntity owner, LivingEntity target, float damage, int holdTicks) {
        this(ModEntities.INK_HANDS.get(), level);
        this.ownerUUID = owner == null ? null : owner.getUUID();
        this.damage = damage;
        this.entityData.set(DATA_TARGET, target.getId());
        this.entityData.set(DATA_HOLD, holdTicks);
        moveTo(target.getX(), target.getY(), target.getZ(), random.nextFloat() * 360f, 0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_TARGET, -1);
        builder.define(DATA_HOLD, 40);
    }

    @Nullable
    public LivingEntity getTarget() {
        return level().getEntity(this.entityData.get(DATA_TARGET)) instanceof LivingEntity living ? living : null;
    }

    /** Ticks the victim is held, from the moment the hands close on it. */
    public int getHoldTicks() {
        return this.entityData.get(DATA_HOLD);
    }

    public boolean isHolding() {
        return tickCount >= RISE_TICKS && tickCount < RISE_TICKS + getHoldTicks();
    }

    @Override
    public void tick() {
        super.tick();
        LivingEntity target = getTarget();
        if (level().isClientSide) {
            if (target != null && isHolding() && random.nextInt(3) == 0) {
                level().addParticle(ParticleTypes.SQUID_INK, target.getX() + (random.nextFloat() - .5f) * target.getBbWidth(),
                        target.getY() + random.nextFloat() * target.getBbHeight() * .6, target.getZ() + (random.nextFloat() - .5f) * target.getBbWidth(), 0, -.02, 0);
            }
            return;
        }
        if (target == null || !target.isAlive() || tickCount > RISE_TICKS + getHoldTicks() + SINK_TICKS) {
            discard();
            return;
        }
        if (tickCount < RISE_TICKS + getHoldTicks()) {
            // The hands sink with the victim if it falls, but never let it walk away
            setPos(getX(), target.getY(), getZ());
        }
        Entity owner = ownerUUID != null && level() instanceof ServerLevel serverLevel ? serverLevel.getEntity(ownerUUID) : null;
        if (tickCount == RISE_TICKS) {
            grab(target, owner);
        }
        if (!isHolding()) {
            return;
        }
        hold(target);
        int held = tickCount - RISE_TICKS;
        if (held > 0 && held % SQUEEZE_INTERVAL == 0) {
            target.invulnerableTime = 0;
            DamageSources.applyDamage(target, damage * .25f, SpellUtil.damageSource(ModSpells.LIVING_INK.get(), this, owner));
            level().playSound(null, getX(), getY(), getZ(), SoundEvents.SLIME_SQUISH, SoundSource.PLAYERS, 1f, .5f);
        }
        if (tickCount == RISE_TICKS + getHoldTicks() - 1) {
            level().playSound(null, getX(), getY(), getZ(), SoundEvents.SQUID_SQUIRT, SoundSource.PLAYERS, 1f, .4f);
        }
    }

    private void grab(LivingEntity target, @Nullable Entity owner) {
        DamageSources.applyDamage(target, damage, SpellUtil.damageSource(ModSpells.LIVING_INK.get(), this, owner));
        target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, getHoldTicks() + 30, 0, false, false, true));
        double y = target.getY() + target.getBbHeight() * .4;
        SpellUtil.particles(level(), ParticleTypes.SQUID_INK, target.getX(), y, target.getZ(), 20, .3, .4, .3, .05);
        SpellUtil.particles(level(), BookFx.INK_SHEEN_DUST, target.getX(), y, target.getZ(), 12, .4, .5, .4, 0);
        level().playSound(null, getX(), getY(), getZ(), SoundEvents.SLIME_BLOCK_FALL, SoundSource.PLAYERS, 1.4f, .5f);
        level().playSound(null, getX(), getY(), getZ(), SoundEvents.SOUL_ESCAPE, SoundSource.PLAYERS, 1.2f, .6f);
    }

    /** Pins the victim where the hands caught it. */
    private void hold(LivingEntity target) {
        double dx = getX() - target.getX();
        double dz = getZ() - target.getZ();
        if (dx * dx + dz * dz > .04) {
            target.teleportTo(getX(), target.getY(), getZ());
        }
        target.setDeltaMovement(0, Math.min(0, target.getDeltaMovement().y), 0);
        if (target instanceof Player || tickCount % 10 == 0) {
            target.hurtMarked = true;
        }
        if (tickCount % 10 == RISE_TICKS % 10) {
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 15, 6, false, false, false));
        }
    }

    @Override
    public void onAntiMagic(MagicData playerMagicData) {
        discard();
    }

    @Override
    public AABB getBoundingBoxForCulling() {
        return getBoundingBox().inflate(2);
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
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
    }
}
