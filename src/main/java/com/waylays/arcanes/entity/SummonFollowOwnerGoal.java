package com.waylays.arcanes.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * A summon with nothing to fight goes back to its owner: walkers to the owner's feet, flyers to a spot above the
 * owner's head. Left more than {@link #TELEPORT_DISTANCE} blocks behind, it teleports back next to the owner.
 */
public class SummonFollowOwnerGoal extends Goal {
    public static final double TELEPORT_DISTANCE = 20;

    private final PathfinderMob mob;
    private final OwnedSummon summon;
    private final double speed;
    private final double startDistance;
    private final double stopDistance;
    /** Height above the owner's head a flyer aims for; negative for walkers. */
    private final float flyHeight;
    private int recalc;

    public <T extends PathfinderMob & OwnedSummon> SummonFollowOwnerGoal(T mob, double speed, double startDistance, double stopDistance, float flyHeight) {
        this.mob = mob;
        this.summon = mob;
        this.speed = speed;
        this.startDistance = startDistance;
        this.stopDistance = stopDistance;
        this.flyHeight = flyHeight;
        setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        LivingEntity owner = summon.getSummoner();
        return owner != null && !owner.isSpectator() && mob.getTarget() == null && mob.distanceToSqr(owner) > startDistance * startDistance;
    }

    @Override
    public boolean canContinueToUse() {
        LivingEntity owner = summon.getSummoner();
        return owner != null && mob.getTarget() == null && !mob.getNavigation().isDone() && mob.distanceToSqr(owner) > stopDistance * stopDistance;
    }

    @Override
    public void start() {
        recalc = 0;
    }

    @Override
    public void stop() {
        mob.getNavigation().stop();
    }

    @Override
    public void tick() {
        LivingEntity owner = summon.getSummoner();
        if (owner == null) {
            return;
        }
        mob.getLookControl().setLookAt(owner, 10, mob.getMaxHeadXRot());
        if (--recalc > 0) {
            return;
        }
        recalc = 10;
        if (mob.distanceToSqr(owner) > TELEPORT_DISTANCE * TELEPORT_DISTANCE) {
            teleportBack(owner);
        } else if (flyHeight >= 0) {
            mob.getNavigation().moveTo(owner.getX(), owner.getY() + owner.getBbHeight() + flyHeight, owner.getZ(), speed);
        } else {
            mob.getNavigation().moveTo(owner, speed);
        }
    }

    private void teleportBack(LivingEntity owner) {
        if (flyHeight >= 0) {
            Vec3 spot = owner.position().add(0, owner.getBbHeight() + flyHeight, 0);
            if (mob.level().noCollision(mob, mob.getBoundingBox().move(spot.subtract(mob.position())))) {
                mob.moveTo(spot.x, spot.y, spot.z, mob.getYRot(), mob.getXRot());
                mob.getNavigation().stop();
            }
            return;
        }
        BlockPos base = owner.blockPosition();
        for (int i = 0; i < 10; i++) {
            BlockPos pos = base.offset(mob.getRandom().nextInt(5) - 2, 0, mob.getRandom().nextInt(5) - 2);
            if (mob.level().getBlockState(pos.below()).isSolidRender(mob.level(), pos.below())
                    && mob.level().noCollision(mob, mob.getBoundingBox().move(pos.getCenter().subtract(mob.position()).add(0, -.5, 0)))) {
                mob.moveTo(pos.getX() + .5, pos.getY(), pos.getZ() + .5, mob.getYRot(), mob.getXRot());
                mob.getNavigation().stop();
                return;
            }
        }
    }
}
