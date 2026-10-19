package com.waylays.arcanes.entity.errethys;

import com.waylays.arcanes.registry.ModSpells;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.SmoothSwimmingLookControl;
import net.minecraft.world.entity.ai.control.SmoothSwimmingMoveControl;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomSwimmingGoal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.navigation.WaterBoundPathNavigation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * A mana salmon: a fish of mana that lives only in water. It swims with a salmon's wriggle, follows its summoner
 * through the water when they are in or beside it (never onto land), and rams their enemies in the water or at its
 * edge. On dry land it flops about like a stranded fish and comes apart after a few seconds.
 */
public class ManaSalmon extends ManaSummon {
    /** How long it lasts out of water. */
    public static final int DRY_TICKS = 60;

    private int dryTicks;

    public ManaSalmon(EntityType<? extends ManaSalmon> entityType, Level level) {
        super(entityType, level);
        this.moveControl = new SmoothSwimmingMoveControl(this, 85, 10, .02f, .01f, false);
        this.lookControl = new SmoothSwimmingLookControl(this, 10);
        setPathfindingMalus(BlockPathTypes.WATER, 0);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 8)
                .add(Attributes.MOVEMENT_SPEED, 1.3)
                .add(Attributes.ATTACK_DAMAGE, 2)
                .add(Attributes.FOLLOW_RANGE, 24);
    }

    @Override
    public AbstractSpell getSummoningSpell() {
        return ModSpells.MANA_SALMON.get();
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        return new WaterBoundPathNavigation(this, level);
    }

    @Override
    protected void registerMoveGoals() {
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.4, true) {
            @Override
            protected double getAttackReachSqr(LivingEntity target) {
                return 1.8 * 1.8 + target.getBbWidth();
            }
        });
        this.goalSelector.addGoal(2, new FollowOwnerInWaterGoal(this));
        this.goalSelector.addGoal(3, new RandomSwimmingGoal(this, 1, 40));
    }

    /** Swims like a fish in water; out of it, gravity takes over. */
    @Override
    public void travel(Vec3 input) {
        if (isControlledByLocalInstance() && isInWater()) {
            moveRelative(getSpeed(), input);
            move(MoverType.SELF, getDeltaMovement());
            setDeltaMovement(getDeltaMovement().scale(.9));
            if (getTarget() == null) {
                setDeltaMovement(getDeltaMovement().add(0, -.005, 0));
            }
        } else {
            super.travel(input);
        }
    }

    /** Stranded, it flops about like any fish. */
    @Override
    public void aiStep() {
        if (!isInWater() && onGround() && verticalCollision) {
            setDeltaMovement(getDeltaMovement().add((random.nextFloat() * 2 - 1) * .05f, .4f, (random.nextFloat() * 2 - 1) * .05f));
            setOnGround(false);
            this.hasImpulse = true;
            playSound(SoundEvents.SALMON_FLOP, getSoundVolume(), getVoicePitch());
        }
        super.aiStep();
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide || isRemoved()) {
            return;
        }
        // A fish of mana does not last out of water
        dryTicks = isInWaterOrBubble() ? 0 : dryTicks + 1;
        if (dryTicks > DRY_TICKS) {
            dissolve();
        }
    }

    @Override
    public boolean canBreatheUnderwater() {
        return true;
    }

    @Override
    public boolean isPushedByFluid() {
        return false;
    }

    @Override
    public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    protected void checkFallDamage(double y, boolean onGround, BlockState state, BlockPos pos) {
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.SALMON_AMBIENT;
    }

    @Override
    public int getAmbientSoundInterval() {
        return 160;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.SALMON_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.SALMON_DEATH;
    }

    @Override
    protected float getSoundVolume() {
        return .5f;
    }

    /** Swims after its summoner while they are in or right beside the water; it never leaves the water for them. */
    static class FollowOwnerInWaterGoal extends Goal {
        private final ManaSalmon salmon;
        private int recalc;

        FollowOwnerInWaterGoal(ManaSalmon salmon) {
            this.salmon = salmon;
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            LivingEntity owner = salmon.getSummoner();
            return salmon.isInWater() && owner != null && byTheWater(owner) && salmon.distanceToSqr(owner) > 4 * 4 && salmon.distanceToSqr(owner) < 32 * 32;
        }

        @Override
        public boolean canContinueToUse() {
            LivingEntity owner = salmon.getSummoner();
            return salmon.isInWater() && owner != null && byTheWater(owner) && salmon.distanceToSqr(owner) > 2.5 * 2.5 && !salmon.getNavigation().isDone();
        }

        @Override
        public void start() {
            recalc = 10;
            LivingEntity owner = salmon.getSummoner();
            if (owner != null) {
                salmon.getNavigation().moveTo(owner.getX(), owner.getY(), owner.getZ(), 1.2);
            }
        }

        @Override
        public void stop() {
            salmon.getNavigation().stop();
        }

        @Override
        public void tick() {
            LivingEntity owner = salmon.getSummoner();
            if (owner != null && --recalc <= 0) {
                recalc = 10;
                salmon.getNavigation().moveTo(owner.getX(), owner.getY(), owner.getZ(), 1.2);
            }
        }

        /** In the water, or standing within a couple of blocks of it. */
        private static boolean byTheWater(LivingEntity owner) {
            if (owner.isInWater()) {
                return true;
            }
            BlockPos feet = owner.blockPosition();
            for (BlockPos pos : BlockPos.betweenClosed(feet.offset(-2, -1, -2), feet.offset(2, 0, 2))) {
                if (owner.level().getFluidState(pos).is(FluidTags.WATER)) {
                    return true;
                }
            }
            return false;
        }
    }
}
