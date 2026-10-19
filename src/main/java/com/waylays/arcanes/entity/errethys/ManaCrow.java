package com.waylays.arcanes.entity.errethys;

import com.waylays.arcanes.registry.ModSpells;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomFlyingGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.animal.FlyingAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * A mana crow: it flies like a bird, circling above its summoner in wide loops, swoops down on their enemies to peck
 * and climbs away again. It beats its wings to climb and glides when it comes down.
 */
public class ManaCrow extends ManaSummon implements FlyingAnimal {
    public static final float CIRCLE_RADIUS = 3.5f;
    public static final float CIRCLE_HEIGHT = 2.5f;

    public ManaCrow(EntityType<? extends ManaCrow> entityType, Level level) {
        super(entityType, level);
        this.moveControl = new FlyingMoveControl(this, 20, true);
        setNoGravity(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 6)
                .add(Attributes.FLYING_SPEED, .75)
                .add(Attributes.MOVEMENT_SPEED, .3)
                .add(Attributes.ATTACK_DAMAGE, 2)
                .add(Attributes.FOLLOW_RANGE, 32);
    }

    @Override
    public AbstractSpell getSummoningSpell() {
        return ModSpells.MANA_CROWS.get();
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
    protected void registerMoveGoals() {
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.5, true) {
            @Override
            protected double getAttackReachSqr(LivingEntity target) {
                return 2 * 2 + target.getBbWidth();
            }
        });
        this.goalSelector.addGoal(2, new CircleOwnerGoal(this));
        this.goalSelector.addGoal(3, new WaterAvoidingRandomFlyingGoal(this, .8));
        this.goalSelector.addGoal(4, new LookAtPlayerGoal(this, Player.class, 8));
    }

    @Override
    public void tick() {
        super.tick();
        // Wing beats while it climbs or cruises
        if (!level().isClientSide && isFlying() && getDeltaMovement().y > -.05 && tickCount % 9 == 0) {
            playSound(SoundEvents.PARROT_FLY, .25f, .8f + random.nextFloat() * .2f);
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
    protected SoundEvent getAmbientSound() {
        return SoundEvents.PARROT_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.PARROT_HURT;
    }

    @Override
    public float getVoicePitch() {
        return .45f + random.nextFloat() * .1f;
    }

    @Override
    protected float getSoundVolume() {
        return .6f;
    }

    /** With nothing to fight, loops around above the summoner's head; comes back when left far behind. */
    static class CircleOwnerGoal extends Goal {
        private final ManaCrow crow;
        private float angle;

        CircleOwnerGoal(ManaCrow crow) {
            this.crow = crow;
            setFlags(EnumSet.of(Goal.Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            return crow.getSummoner() != null && crow.getTarget() == null;
        }

        @Override
        public void start() {
            LivingEntity owner = crow.getSummoner();
            if (owner != null) {
                angle = (float) Mth.atan2(crow.getZ() - owner.getZ(), crow.getX() - owner.getX());
            }
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            LivingEntity owner = crow.getSummoner();
            if (owner == null) {
                return;
            }
            if (crow.distanceToSqr(owner) > 24 * 24) {
                Vec3 spot = owner.position().add(0, owner.getBbHeight() + CIRCLE_HEIGHT, 0);
                if (crow.level().noCollision(crow, crow.getBoundingBox().move(spot.subtract(crow.position())))) {
                    crow.moveTo(spot.x, spot.y, spot.z, crow.getYRot(), crow.getXRot());
                }
                return;
            }
            angle += .07f;
            // A slow rise and fall on each loop, like a bird riding the air
            double y = owner.getY() + owner.getBbHeight() + CIRCLE_HEIGHT + Mth.sin(angle * .5f) * .8f;
            crow.getMoveControl().setWantedPosition(owner.getX() + Mth.cos(angle) * CIRCLE_RADIUS, y, owner.getZ() + Mth.sin(angle) * CIRCLE_RADIUS, 1);
        }
    }
}
