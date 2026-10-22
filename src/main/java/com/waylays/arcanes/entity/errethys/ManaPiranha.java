package com.waylays.arcanes.entity.errethys;

import com.waylays.arcanes.ArcanesOublies;
import com.waylays.arcanes.registry.ModSpells;
import com.waylays.arcanes.spells.SpellUtil;
import com.waylays.arcanes.spells.errethys.ErrethysFx;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
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
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * A mana piranha: a fish of mana that lives only in water, deep-bodied, its jutting lower jaw lined with teeth of light.
 * It darts through the water, follows its summoner when they are in or beside it (never onto land), and bites their
 * enemies in the water or at its edge, fast, in a frenzy: each bite on the same prey within three seconds bites harder
 * (+25 % per bite, up to +100 %). On dry land it flops about like a stranded fish and comes apart after a few seconds.
 */
public class ManaPiranha extends ManaSummon {
    /** How long it lasts out of water. */
    public static final int DRY_TICKS = 60;
    /** Ticks between bites, and how long a frenzy lasts without a bite. */
    public static final int BITE_TICKS = 10;
    public static final int FRENZY_TICKS = 60;
    public static final int MAX_FRENZY = 4;
    private static final ResourceLocation FRENZY = ArcanesOublies.id("piranha_frenzy");

    private int dryTicks;
    private int frenzy;
    private int frenzyTarget = -1;
    private int lastBite = -FRENZY_TICKS;

    public ManaPiranha(EntityType<? extends ManaPiranha> entityType, Level level) {
        super(entityType, level);
        this.moveControl = new SmoothSwimmingMoveControl(this, 85, 10, .02f, .01f, false);
        this.lookControl = new SmoothSwimmingLookControl(this, 10);
        setPathfindingMalus(PathType.WATER, 0);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 8)
                .add(Attributes.MOVEMENT_SPEED, 1.5)
                .add(Attributes.ATTACK_DAMAGE, 2)
                .add(Attributes.FOLLOW_RANGE, 24);
    }

    @Override
    public AbstractSpell getSummoningSpell() {
        return ModSpells.MANA_PIRANHA.get();
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        return new WaterBoundPathNavigation(this, level);
    }

    @Override
    protected void registerMoveGoals() {
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.6, true) {
            @Override
            protected boolean canPerformAttack(LivingEntity target) {
                return isTimeToAttack() && mob.distanceToSqr(target) <= 1.8 * 1.8 + target.getBbWidth() && mob.getSensing().hasLineOfSight(target);
            }

            @Override
            protected int getAttackInterval() {
                return adjustedTickDelay(BITE_TICKS);
            }
        });
        this.goalSelector.addGoal(2, new FollowOwnerInWaterGoal(this));
        this.goalSelector.addGoal(3, new RandomSwimmingGoal(this, 1, 40));
    }

    /** The frenzy of its bites: how many bites in a row on the same prey (0 to {@link #MAX_FRENZY}). */
    public int getFrenzy() {
        return tickCount - lastBite <= FRENZY_TICKS ? frenzy : 0;
    }

    /** A bite: harder for each bite in a row on the same prey; it darts at it, teeth flashing, in a swirl of bubbles. */
    @Override
    public boolean doHurtTarget(Entity target) {
        if (target.getId() == frenzyTarget && tickCount - lastBite <= FRENZY_TICKS) {
            frenzy = Math.min(MAX_FRENZY, frenzy + 1);
        } else {
            frenzy = 0;
            frenzyTarget = target.getId();
        }
        lastBite = tickCount;
        AttributeInstance attack = getAttribute(Attributes.ATTACK_DAMAGE);
        if (attack != null) {
            attack.removeModifier(FRENZY);
            if (frenzy > 0) {
                attack.addTransientModifier(new AttributeModifier(FRENZY, .25 * frenzy, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
            }
        }
        boolean bit = super.doHurtTarget(target);
        if (attack != null) {
            attack.removeModifier(FRENZY);
        }
        Vec3 to = target.getBoundingBox().getCenter().subtract(position());
        if (to.lengthSqr() > 1.0E-4) {
            setDeltaMovement(getDeltaMovement().add(to.normalize().scale(.25)));
        }
        Vec3 jaws = position().add(to.scale(.6)).add(0, getBbHeight() * .5, 0);
        SpellUtil.particles(level(), ParticleTypes.BUBBLE, jaws.x, jaws.y, jaws.z, 6 + 2 * frenzy, .2, .2, .2, .08);
        SpellUtil.particles(level(), ErrethysFx.PALE_DUST, jaws.x, jaws.y, jaws.z, 3 + frenzy, .15, .15, .15, .02);
        playSound(SoundEvents.FOX_BITE, .8f, 1.3f + .1f * frenzy);
        return bit;
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
            playSound(SoundEvents.TROPICAL_FISH_FLOP, getSoundVolume(), getVoicePitch());
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
        return SoundEvents.TROPICAL_FISH_AMBIENT;
    }

    @Override
    public int getAmbientSoundInterval() {
        return 160;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.TROPICAL_FISH_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.TROPICAL_FISH_DEATH;
    }

    @Override
    protected float getSoundVolume() {
        return .5f;
    }

    /** Swims after its summoner while they are in or right beside the water; it never leaves the water for them. */
    static class FollowOwnerInWaterGoal extends Goal {
        private final ManaPiranha piranha;
        private int recalc;

        FollowOwnerInWaterGoal(ManaPiranha piranha) {
            this.piranha = piranha;
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            LivingEntity owner = piranha.getSummoner();
            return piranha.isInWater() && owner != null && byTheWater(owner) && piranha.distanceToSqr(owner) > 4 * 4 && piranha.distanceToSqr(owner) < 32 * 32;
        }

        @Override
        public boolean canContinueToUse() {
            LivingEntity owner = piranha.getSummoner();
            return piranha.isInWater() && owner != null && byTheWater(owner) && piranha.distanceToSqr(owner) > 2.5 * 2.5 && !piranha.getNavigation().isDone();
        }

        @Override
        public void start() {
            recalc = 10;
            LivingEntity owner = piranha.getSummoner();
            if (owner != null) {
                piranha.getNavigation().moveTo(owner.getX(), owner.getY(), owner.getZ(), 1.2);
            }
        }

        @Override
        public void stop() {
            piranha.getNavigation().stop();
        }

        @Override
        public void tick() {
            LivingEntity owner = piranha.getSummoner();
            if (owner != null && --recalc <= 0) {
                recalc = 10;
                piranha.getNavigation().moveTo(owner.getX(), owner.getY(), owner.getZ(), 1.2);
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
