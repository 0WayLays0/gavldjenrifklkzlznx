package com.waylays.arcanes.entity.gold;

import com.waylays.arcanes.spells.SpellUtil;
import com.waylays.arcanes.spells.gold.GoldFx;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.util.EnumSet;
import java.util.UUID;

/**
 * A golden knight summoned by Golden Sentinels. Guards its summoner: attacks what the summoner attacks, what attacks
 * the summoner and nearby monsters. Shatters into gold when its time runs out or when it dies.
 */
public class GoldenSentinel extends PathfinderMob {
    @Nullable
    private UUID ownerUUID;
    private int lifeTicks = 600;

    public GoldenSentinel(EntityType<? extends GoldenSentinel> entityType, Level level) {
        super(entityType, level);
        this.xpReward = 0;
        setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 24)
                .add(Attributes.MOVEMENT_SPEED, .3)
                .add(Attributes.ATTACK_DAMAGE, 5)
                .add(Attributes.FOLLOW_RANGE, 32)
                .add(Attributes.KNOCKBACK_RESISTANCE, .4);
    }

    /** Called once by the spell: owner, strength and golden gear. */
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
        setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.GOLDEN_HELMET));
        setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.GOLDEN_CHESTPLATE));
        setItemSlot(EquipmentSlot.LEGS, new ItemStack(Items.GOLDEN_LEGGINGS));
        setItemSlot(EquipmentSlot.FEET, new ItemStack(Items.GOLDEN_BOOTS));
        setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.GOLDEN_SWORD));
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            setDropChance(slot, 0);
        }
    }

    @Nullable
    public UUID getOwnerUUID() {
        return ownerUUID;
    }

    @Nullable
    public LivingEntity getOwner() {
        if (ownerUUID != null && level() instanceof ServerLevel serverLevel && serverLevel.getEntity(ownerUUID) instanceof LivingEntity living) {
            return living;
        }
        return null;
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.25, true));
        this.goalSelector.addGoal(2, new FollowOwnerGoal(this, 1.15));
        this.goalSelector.addGoal(3, new WaterAvoidingRandomStrollGoal(this, .8));
        this.goalSelector.addGoal(4, new LookAtPlayerGoal(this, Player.class, 8));
        this.goalSelector.addGoal(5, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new OwnerTargetGoal(this, true));
        this.targetSelector.addGoal(2, new OwnerTargetGoal(this, false));
        this.targetSelector.addGoal(3, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(4, new NearestAttackableTargetGoal<>(this, Mob.class, 5, true, false,
                e -> e instanceof Enemy && !SpellUtil.isAlly(getOwner(), e)));
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            if (random.nextInt(8) == 0) {
                level().addParticle(GoldFx.PALE_DUST, getRandomX(.6), getY() + random.nextDouble() * getBbHeight(), getRandomZ(.6), 0, .02, 0);
            }
            return;
        }
        if (--lifeTicks <= 0) {
            shatter();
        }
        LivingEntity target = getTarget();
        if (target != null && SpellUtil.isAlly(getOwner(), target)) {
            setTarget(null);
        }
    }

    /** Bursts into gold and disappears (no drops). */
    public void shatter() {
        if (!level().isClientSide) {
            GoldFx.shatterBurst(level(), getX(), getY() + 1, getZ(), 1.2f);
            level().playSound(null, getX(), getY(), getZ(), SoundEvents.AMETHYST_CLUSTER_BREAK, SoundSource.NEUTRAL, 1.5f, .8f);
        }
        discard();
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        if (!level().isClientSide) {
            shatter();
        }
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (hit) {
            SpellUtil.particles(level(), GoldFx.sparks(), target.getX(), target.getY() + target.getBbHeight() * .6, target.getZ(), 5, .2, .2, .2, .25);
        }
        return hit;
    }

    @Override
    public boolean isAlliedTo(Entity other) {
        if (ownerUUID != null && (ownerUUID.equals(other.getUUID())
                || other instanceof GoldenSentinel sentinel && ownerUUID.equals(sentinel.ownerUUID))) {
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
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.ANVIL_HIT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.AMETHYST_CLUSTER_BREAK;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        playSound(SoundEvents.CHAIN_STEP, .4f, .8f);
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

    /** Targets what attacked the owner ({@code defend}) or what the owner attacked. */
    static class OwnerTargetGoal extends TargetGoal {
        private final GoldenSentinel sentinel;
        private final boolean defend;
        @Nullable
        private LivingEntity candidate;
        private int lastTimestamp;

        OwnerTargetGoal(GoldenSentinel sentinel, boolean defend) {
            super(sentinel, false);
            this.sentinel = sentinel;
            this.defend = defend;
            setFlags(EnumSet.of(Goal.Flag.TARGET));
        }

        @Override
        public boolean canUse() {
            LivingEntity owner = sentinel.getOwner();
            if (owner == null) {
                return false;
            }
            candidate = defend ? owner.getLastHurtByMob() : owner.getLastHurtMob();
            int timestamp = defend ? owner.getLastHurtByMobTimestamp() : owner.getLastHurtMobTimestamp();
            return candidate != null && timestamp != lastTimestamp && !SpellUtil.isAlly(owner, candidate)
                    && canAttack(candidate, TargetingConditions.DEFAULT);
        }

        @Override
        public void start() {
            LivingEntity owner = sentinel.getOwner();
            if (owner != null) {
                lastTimestamp = defend ? owner.getLastHurtByMobTimestamp() : owner.getLastHurtMobTimestamp();
            }
            mob.setTarget(candidate);
            super.start();
        }
    }

    /** Stays near the owner when there is nothing to fight; teleports back when left far behind. */
    static class FollowOwnerGoal extends Goal {
        private final GoldenSentinel sentinel;
        private final double speed;
        private int recalc;

        FollowOwnerGoal(GoldenSentinel sentinel, double speed) {
            this.sentinel = sentinel;
            this.speed = speed;
            setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity owner = sentinel.getOwner();
            return owner != null && !owner.isSpectator() && sentinel.getTarget() == null && sentinel.distanceToSqr(owner) > 6 * 6;
        }

        @Override
        public boolean canContinueToUse() {
            LivingEntity owner = sentinel.getOwner();
            return owner != null && sentinel.getTarget() == null && !sentinel.getNavigation().isDone() && sentinel.distanceToSqr(owner) > 3 * 3;
        }

        @Override
        public void start() {
            recalc = 0;
        }

        @Override
        public void stop() {
            sentinel.getNavigation().stop();
        }

        @Override
        public void tick() {
            LivingEntity owner = sentinel.getOwner();
            if (owner == null) {
                return;
            }
            sentinel.getLookControl().setLookAt(owner, 10, sentinel.getMaxHeadXRot());
            if (--recalc > 0) {
                return;
            }
            recalc = 10;
            if (sentinel.distanceToSqr(owner) > 20 * 20) {
                BlockPos base = owner.blockPosition();
                for (int i = 0; i < 10; i++) {
                    BlockPos pos = base.offset(sentinel.getRandom().nextInt(5) - 2, 0, sentinel.getRandom().nextInt(5) - 2);
                    if (sentinel.level().getBlockState(pos.below()).isSolidRender(sentinel.level(), pos.below())
                            && sentinel.level().noCollision(sentinel, sentinel.getBoundingBox().move(pos.getCenter().subtract(sentinel.position()).add(0, -.5, 0)))) {
                        sentinel.moveTo(pos.getX() + .5, pos.getY(), pos.getZ() + .5, sentinel.getYRot(), sentinel.getXRot());
                        sentinel.getNavigation().stop();
                        return;
                    }
                }
            } else {
                sentinel.getNavigation().moveTo(owner, speed);
            }
        }
    }
}
