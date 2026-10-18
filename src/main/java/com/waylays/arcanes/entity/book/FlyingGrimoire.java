package com.waylays.arcanes.entity.book;

import com.waylays.arcanes.entity.OwnedSummon;
import com.waylays.arcanes.entity.OwnerTargetGoal;
import com.waylays.arcanes.spells.SpellUtil;
import com.waylays.arcanes.spells.book.BookFx;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomFlyingGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.animal.FlyingAnimal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.EnumSet;
import java.util.UUID;

/**
 * A flying grimoire from the Forbidden Library: a little book with glowing eyes and fangs along its pages. It flies
 * along with its summoner, bites what the summoner fights and spits rune bolts at it from afar; it bursts into
 * pages when its time is up or when it is destroyed.
 */
public class FlyingGrimoire extends PathfinderMob implements FlyingAnimal, OwnedSummon {
    public static final int BITE_TICKS = 6;
    public static final int SPIT_TICKS = 8;
    public static final double BOLT_RANGE = 14;

    /** Ticks the last bite and the last spat bolt started (for the jaw animation), -100 when none. */
    private static final EntityDataAccessor<Integer> DATA_BITE = SynchedEntityData.defineId(FlyingGrimoire.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_SPIT = SynchedEntityData.defineId(FlyingGrimoire.class, EntityDataSerializers.INT);

    @Nullable
    private UUID ownerUUID;
    private int lifeTicks = 400;
    private float boltDamage = 2;
    private int boltCooldown = 30;

    public FlyingGrimoire(EntityType<? extends FlyingGrimoire> entityType, Level level) {
        super(entityType, level);
        this.moveControl = new FlyingMoveControl(this, 20, true);
        setNoGravity(true);
        this.xpReward = 0;
        setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 12)
                .add(Attributes.FLYING_SPEED, .7)
                .add(Attributes.MOVEMENT_SPEED, .3)
                .add(Attributes.ATTACK_DAMAGE, 3)
                .add(Attributes.FOLLOW_RANGE, 32);
    }

    /** Called once by the spell: owner, strength and lifetime. */
    public void setup(LivingEntity owner, int lifeTicks, double health, double biteDamage, float boltDamage) {
        this.ownerUUID = owner.getUUID();
        this.lifeTicks = lifeTicks;
        this.boltDamage = boltDamage;
        var maxHealth = getAttribute(Attributes.MAX_HEALTH);
        if (maxHealth != null) {
            maxHealth.setBaseValue(health);
        }
        var attack = getAttribute(Attributes.ATTACK_DAMAGE);
        if (attack != null) {
            attack.setBaseValue(biteDamage);
        }
        setHealth(getMaxHealth());
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_BITE, -100);
        this.entityData.define(DATA_SPIT, -100);
    }

    /** Ticks since the last bite / bolt began. */
    public int sinceBite() {
        return tickCount - this.entityData.get(DATA_BITE);
    }

    public int sinceSpit() {
        return tickCount - this.entityData.get(DATA_SPIT);
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        FlyingPathNavigation navigation = new FlyingPathNavigation(this, level);
        navigation.setCanOpenDoors(false);
        navigation.setCanFloat(true);
        navigation.setCanPassDoors(true);
        return navigation;
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
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.3, true) {
            @Override
            protected double getAttackReachSqr(LivingEntity target) {
                return 2.2 * 2.2 + target.getBbWidth();
            }
        });
        this.goalSelector.addGoal(2, new FollowOwnerGoal(this));
        this.goalSelector.addGoal(3, new WaterAvoidingRandomFlyingGoal(this, .8));
        this.goalSelector.addGoal(4, new LookAtPlayerGoal(this, Player.class, 8));

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
            if (random.nextInt(5) == 0) {
                level().addParticle(ParticleTypes.ENCHANT, getRandomX(.6), getY() + random.nextDouble() * .5, getRandomZ(.6), 0, .1, 0);
            }
            if (random.nextInt(12) == 0) {
                level().addParticle(BookFx.VIOLET_DUST, getRandomX(.4), getY() + .2, getRandomZ(.4), 0, 0, 0);
            }
            return;
        }
        if (--lifeTicks <= 0) {
            burst();
            return;
        }
        LivingEntity target = getTarget();
        if (target != null && SpellUtil.isAlly(getOwner(), target)) {
            setTarget(null);
            target = null;
        }
        if (tickCount % 30 == 0 && random.nextInt(3) == 0) {
            level().playSound(null, getX(), getY(), getZ(), SoundEvents.BOOK_PAGE_TURN, SoundSource.NEUTRAL, .6f, 1.2f + random.nextFloat() * .4f);
        }
        if (target != null && --boltCooldown <= 0 && distanceToSqr(target) < BOLT_RANGE * BOLT_RANGE && distanceToSqr(target) > 3 * 3 && hasLineOfSight(target)) {
            spit(target);
            boltCooldown = 40 + random.nextInt(25);
        }
    }

    /** Spits a rune bolt at the target. */
    private void spit(LivingEntity target) {
        LivingEntity owner = getOwner();
        GrimoireBolt bolt = new GrimoireBolt(level(), owner != null ? owner : this, boltDamage);
        Vec3 from = position().add(0, getBbHeight() * .5, 0).add(getLookAngle().scale(.4));
        Vec3 to = target.getBoundingBox().getCenter();
        bolt.setPos(from.x, from.y, from.z);
        bolt.shoot(to.subtract(from), 1.1f);
        level().addFreshEntity(bolt);
        this.entityData.set(DATA_SPIT, tickCount);
        level().playSound(null, getX(), getY(), getZ(), SoundEvents.EVOKER_CAST_SPELL, SoundSource.NEUTRAL, .7f, 1.6f);
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hit = super.doHurtTarget(target);
        this.entityData.set(DATA_BITE, tickCount);
        if (hit) {
            level().playSound(null, getX(), getY(), getZ(), SoundEvents.EVOKER_FANGS_ATTACK, SoundSource.NEUTRAL, .8f, 1.5f);
            SpellUtil.particles(level(), BookFx.paper(), target.getX(), target.getY() + target.getBbHeight() * .6, target.getZ(), 3, .2, .2, .2, .1);
        }
        return hit;
    }

    /** Bursts into pages and glyphs and disappears (no drops). */
    public void burst() {
        if (!level().isClientSide) {
            Vec3 c = getBoundingBox().getCenter();
            SpellUtil.particles(level(), BookFx.paper(), c.x, c.y, c.z, 14, .2, .2, .2, .2);
            SpellUtil.particles(level(), ParticleTypes.ENCHANT, c.x, c.y, c.z, 30, .2, .2, .2, 1.2);
            SpellUtil.particles(level(), BookFx.VIOLET_DUST, c.x, c.y, c.z, 10, .25, .25, .25, 0);
            level().playSound(null, c.x, c.y, c.z, SoundEvents.BOOK_PUT, SoundSource.NEUTRAL, 1.2f, 1.1f);
        }
        discard();
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        if (!level().isClientSide) {
            burst();
        }
    }

    @Override
    public boolean isFlying() {
        return !onGround();
    }

    @Override
    public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    protected void checkFallDamage(double y, boolean onGround, BlockState state, BlockPos pos) {
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
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.BOOK_PAGE_TURN;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.BOOK_PUT;
    }

    @Override
    protected float getSoundVolume() {
        return .8f;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("LifeTicks", lifeTicks);
        tag.putFloat("BoltDamage", boltDamage);
        if (ownerUUID != null) {
            tag.putUUID("Owner", ownerUUID);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        lifeTicks = tag.getInt("LifeTicks");
        boltDamage = tag.getFloat("BoltDamage");
        if (tag.hasUUID("Owner")) {
            ownerUUID = tag.getUUID("Owner");
        }
    }

    /** Flies near the owner's shoulders when there is nothing to fight; teleports back when left far behind. */
    static class FollowOwnerGoal extends Goal {
        private final FlyingGrimoire grimoire;
        private int recalc;

        FollowOwnerGoal(FlyingGrimoire grimoire) {
            this.grimoire = grimoire;
            setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity owner = grimoire.getOwner();
            return owner != null && !owner.isSpectator() && grimoire.getTarget() == null && grimoire.distanceToSqr(owner) > 4 * 4;
        }

        @Override
        public boolean canContinueToUse() {
            LivingEntity owner = grimoire.getOwner();
            return owner != null && grimoire.getTarget() == null && grimoire.distanceToSqr(owner) > 2.5 * 2.5;
        }

        @Override
        public void start() {
            recalc = 0;
        }

        @Override
        public void stop() {
            grimoire.getNavigation().stop();
        }

        @Override
        public void tick() {
            LivingEntity owner = grimoire.getOwner();
            if (owner == null) {
                return;
            }
            grimoire.getLookControl().setLookAt(owner, 10, grimoire.getMaxHeadXRot());
            if (--recalc > 0) {
                return;
            }
            recalc = 10;
            if (grimoire.distanceToSqr(owner) > 24 * 24) {
                Vec3 spot = owner.position().add(0, owner.getBbHeight() + .5, 0);
                if (grimoire.level().noCollision(grimoire, grimoire.getBoundingBox().move(spot.subtract(grimoire.position())))) {
                    grimoire.moveTo(spot.x, spot.y, spot.z, grimoire.getYRot(), grimoire.getXRot());
                    grimoire.getNavigation().stop();
                }
            } else {
                grimoire.getNavigation().moveTo(owner.getX(), owner.getY() + owner.getBbHeight() + .3, owner.getZ(), 1.2);
            }
        }
    }
}
