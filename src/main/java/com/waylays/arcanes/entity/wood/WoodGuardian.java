package com.waylays.arcanes.entity.wood;

import com.waylays.arcanes.entity.SummonFollowOwnerGoal;
import com.waylays.arcanes.entity.errethys.ManaSummon;
import com.waylays.arcanes.registry.ModSpells;
import com.waylays.arcanes.spells.SpellUtil;
import com.waylays.arcanes.spells.wood.WoodFx;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.damage.DamageSources;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * The Sylvan Guardian, a treant: it pulls itself out of the ground in front of its summoner, an old oak walking on two
 * trunks, a crown of azalea on its head and shoulders, its eyes glowing green. It fights what its summoner fights:
 * both its arms come down together on its foe, the blow shaking the ground round it; now and then it plunges its hands
 * into the earth and roots burst up round a foe further off and hold it fast. When its time is up it sinks back into
 * the ground.
 */
public class WoodGuardian extends ManaSummon {
    public static final int FORM_TICKS = 24;
    public static final int SLAM_TICKS = 12;
    public static final int ROOTS_COOLDOWN = 140;

    private static final EntityDataAccessor<Integer> DATA_SLAM = SynchedEntityData.defineId(WoodGuardian.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_ROOTS = SynchedEntityData.defineId(WoodGuardian.class, EntityDataSerializers.INT);

    private int rootsCooldown = 60;

    public WoodGuardian(EntityType<? extends WoodGuardian> entityType, Level level) {
        super(entityType, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 60)
                .add(Attributes.ARMOR, 8)
                .add(Attributes.MOVEMENT_SPEED, .25)
                .add(Attributes.ATTACK_DAMAGE, 8)
                .add(Attributes.ATTACK_KNOCKBACK, 1.2)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1)
                .add(Attributes.STEP_HEIGHT, 1.1)
                .add(Attributes.FOLLOW_RANGE, 32);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_SLAM, -100);
        builder.define(DATA_ROOTS, -100);
    }

    /** The tick both its arms last came down on a foe. */
    public int getSlamTick() {
        return this.entityData.get(DATA_SLAM);
    }

    /** The tick it last plunged its hands into the ground. */
    public int getRootsTick() {
        return this.entityData.get(DATA_ROOTS);
    }

    @Override
    public AbstractSpell getSummoningSpell() {
        return ModSpells.WOOD_GUARDIAN.get();
    }

    @Override
    protected void registerMoveGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.2, true));
        this.goalSelector.addGoal(2, new SummonFollowOwnerGoal(this, 1.15, 8, 3, -1));
        this.goalSelector.addGoal(3, new WaterAvoidingRandomStrollGoal(this, .7));
        this.goalSelector.addGoal(4, new LookAtPlayerGoal(this, Player.class, 8));
        this.goalSelector.addGoal(5, new RandomLookAroundGoal(this));
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide || !(level() instanceof ServerLevel level)) {
            return;
        }
        if (tickCount < FORM_TICKS) {
            // Still pulling itself out of the ground
            getNavigation().stop();
            setDeltaMovement(getDeltaMovement().multiply(0, 1, 0));
            if (tickCount % 4 == 0) {
                WoodFx.chips(level, position().add(0, .3, 0), 10, .8);
                WoodFx.sprout(level, position(), .8f);
            }
            return;
        }
        int sinceRoots = tickCount - getRootsTick();
        if (sinceRoots < 14) {
            getNavigation().stop();
            if (sinceRoots == 8) {
                plunge(level);
            }
            return;
        }
        LivingEntity target = getTarget();
        if (--rootsCooldown <= 0 && target != null && target.isAlive() && onGround() && hasLineOfSight(target)) {
            double d = distanceTo(target);
            if (d > 3.5 && d < 16) {
                this.entityData.set(DATA_ROOTS, tickCount);
                rootsCooldown = ROOTS_COOLDOWN;
                getLookControl().setLookAt(target);
                WoodFx.creak(level, position().add(0, 2, 0), 1.4f);
            }
        }
    }

    /** Its hands in the ground: roots burst up round its foe and hold it fast. */
    private void plunge(ServerLevel level) {
        Vec3 front = position().add(Vec3.directionFromRotation(0, yBodyRot).scale(1.2));
        WoodFx.sprout(level, front, 1.6f);
        WoodFx.chips(level, front.add(0, .2, 0), 20, .6);
        SpellUtil.cameraShake(level, position(), 6, 10);
        LivingEntity target = getTarget();
        if (target != null && target.isAlive()) {
            RootSnare.seize(level, this, getSummoningSpell(), target, 50, (float) getAttributeValue(Attributes.ATTACK_DAMAGE) * .35f);
        }
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (hit && level() instanceof ServerLevel level) {
            this.entityData.set(DATA_SLAM, tickCount);
            // The blow shakes the ground round its foe
            float damage = (float) getAttributeValue(Attributes.ATTACK_DAMAGE) * .5f;
            LivingEntity owner = getSummoner();
            for (LivingEntity e : SpellUtil.enemiesAround(level, target.position(), 2.6, this, e -> e != target && !SpellUtil.isAlly(owner, e))) {
                DamageSources.applyDamage(e, damage, level.damageSources().mobAttack(this));
                Vec3 out = e.position().subtract(target.position()).multiply(1, 0, 1);
                out = out.lengthSqr() < 1.0E-4 ? Vec3.ZERO : out.normalize().scale(.6);
                e.push(out.x, .3, out.z);
                e.hurtMarked = true;
            }
            target.setDeltaMovement(target.getDeltaMovement().add(0, .25, 0));
            Vec3 c = target.position();
            WoodFx.chips(level, c.add(0, .2, 0), 16, .7);
            WoodFx.crash(level, c, 1.2f);
            SpellUtil.cameraShake(level, c, 4, 8);
        }
        return hit;
    }

    @Override
    protected void sparkle() {
        if (random.nextInt(5) == 0) {
            level().addParticle(WoodFx.LIFE_DUST, getRandomX(.8), getY() + 2 + random.nextDouble() * 1.6, getRandomZ(.8), 0, .01, 0);
        }
    }

    @Override
    protected void dissolveFx() {
        Vec3 c = position();
        WoodFx.chips(level(), c.add(0, 1.5, 0), 40, .8);
        WoodFx.leaves(level(), c.add(0, 3, 0), 40, 1.2);
        WoodFx.life(level(), c.add(0, 1.5, 0), 20, .8);
        WoodFx.creak(level(), c, 1.4f);
        WoodFx.sprout(level(), c, 1.2f);
    }

    @Override
    public boolean causeFallDamage(float fallDistance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return getTarget() != null ? SoundEvents.WOODEN_DOOR_CLOSE : null;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.WOOD_HIT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.WOOD_BREAK;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        playSound(SoundEvents.WOOD_STEP, .9f, .5f);
        playSound(SoundEvents.ROOTED_DIRT_STEP, .6f, .6f);
    }
}
