package com.waylays.arcanes.entity.errethys;

import com.waylays.arcanes.entity.OwnedSummon;
import com.waylays.arcanes.entity.OwnerTargetGoal;
import com.waylays.arcanes.spells.SpellUtil;
import com.waylays.arcanes.spells.errethys.ErrethysFx;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.UUID;

/**
 * A creature of Errethys mana: it fights for its summoner (what the summoner attacks, what attacks the summoner, and
 * monsters nearby), sparkles as it moves, and comes apart into motes of light when its time is up or when it dies.
 * No drops, no experience. Subclasses add their own way of moving in {@link #registerMoveGoals()}.
 */
public abstract class ManaSummon extends PathfinderMob implements OwnedSummon {
    @Nullable
    private UUID ownerUUID;
    private int lifeTicks = 600;

    protected ManaSummon(EntityType<? extends ManaSummon> entityType, Level level) {
        super(entityType, level);
        this.xpReward = 0;
        setPersistenceRequired();
    }

    /** Called once by the spell: owner, lifetime and strength. */
    public void setup(LivingEntity owner, int lifeTicks, double health, double damage) {
        this.ownerUUID = owner.getUUID();
        this.lifeTicks = lifeTicks;
        var maxHealth = getAttribute(Attributes.MAX_HEALTH);
        if (maxHealth != null) {
            maxHealth.setBaseValue(health);
        }
        var attack = getAttribute(Attributes.ATTACK_DAMAGE);
        if (attack != null) {
            attack.setBaseValue(damage);
        }
        setHealth(getMaxHealth());
    }

    public int getLifeTicks() {
        return lifeTicks;
    }

    @Override
    @Nullable
    public UUID getOwnerUUID() {
        return ownerUUID;
    }

    @Override
    @Nullable
    public LivingEntity getOwner() {
        if (ownerUUID != null && level() instanceof ServerLevel serverLevel && serverLevel.getEntity(ownerUUID) instanceof LivingEntity living) {
            return living;
        }
        return null;
    }

    @Override
    protected final void registerGoals() {
        registerMoveGoals();
        this.targetSelector.addGoal(1, new OwnerTargetGoal(this, true));
        this.targetSelector.addGoal(2, new OwnerTargetGoal(this, false));
        this.targetSelector.addGoal(3, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(4, new NearestAttackableTargetGoal<>(this, Mob.class, 5, true, false,
                e -> e instanceof Enemy && !SpellUtil.isAlly(getOwner(), e)));
    }

    /** Movement and attack goals (called from the constructor, before subclass fields are set). */
    protected abstract void registerMoveGoals();

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            if (random.nextInt(4) == 0) {
                level().addParticle(random.nextInt(3) == 0 ? ErrethysFx.PALE_DUST : ErrethysFx.SKY_DUST,
                        getRandomX(.7), getY() + random.nextDouble() * getBbHeight(), getRandomZ(.7), 0, .01, 0);
            }
            return;
        }
        if (--lifeTicks <= 0) {
            dissolve();
            return;
        }
        LivingEntity target = getTarget();
        if (target != null && SpellUtil.isAlly(getOwner(), target)) {
            setTarget(null);
        }
    }

    /** Comes apart into motes of mana and disappears. */
    public void dissolve() {
        if (!level().isClientSide) {
            ErrethysFx.dissolve(level(), getBoundingBox().getCenter(), Math.max(.6f, getBbWidth() * 1.5f));
        }
        discard();
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        if (!level().isClientSide) {
            dissolve();
        }
    }

    @Override
    public boolean isAlliedTo(Entity other) {
        if (ownerUUID != null && (ownerUUID.equals(other.getUUID())
                || other instanceof OwnedSummon summon && ownerUUID.equals(summon.getOwnerUUID()))) {
            return true;
        }
        return super.isAlliedTo(other);
    }

    @Override
    public boolean canAttack(LivingEntity target) {
        return !isAlliedTo(target) && super.canAttack(target);
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.AMETHYST_CLUSTER_BREAK;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("LifeTicks", lifeTicks);
        if (ownerUUID != null) {
            tag.putUUID("Owner", ownerUUID);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        lifeTicks = tag.getInt("LifeTicks");
        if (tag.hasUUID("Owner")) {
            ownerUUID = tag.getUUID("Owner");
        }
    }
}
