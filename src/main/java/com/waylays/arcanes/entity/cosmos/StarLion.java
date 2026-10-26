package com.waylays.arcanes.entity.cosmos;

import com.waylays.arcanes.entity.SummonFollowOwnerGoal;
import com.waylays.arcanes.entity.errethys.ManaSummon;
import com.waylays.arcanes.registry.ModSpells;
import com.waylays.arcanes.spells.SpellUtil;
import com.waylays.arcanes.spells.cosmos.CosmosFx;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.damage.DamageSources;
import io.redspace.ironsspellbooks.particle.BlastwaveParticleOptions;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
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
 * The Lion, a living constellation: the stars of Leo come down from the sky and join into a great lion of starlight.
 * It runs down what its summoner fights, swipes with its claws, and leaps on prey further off, landing in a burst of
 * stars that strikes all round. When its time is up its stars go back up into the sky.
 */
public class StarLion extends ManaSummon {
    /** Ticks its stars take to come down and join. */
    public static final int FORM_TICKS = 30;
    public static final int POUNCE_COOLDOWN = 70;

    private static final EntityDataAccessor<Integer> DATA_POUNCE = SynchedEntityData.defineId(StarLion.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_SWIPE = SynchedEntityData.defineId(StarLion.class, EntityDataSerializers.INT);

    private int pounceCooldown = 30;
    private boolean pouncing;

    public StarLion(EntityType<? extends StarLion> entityType, Level level) {
        super(entityType, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 30)
                .add(Attributes.MOVEMENT_SPEED, .33)
                .add(Attributes.ATTACK_DAMAGE, 6)
                .add(Attributes.ATTACK_KNOCKBACK, .7)
                .add(Attributes.KNOCKBACK_RESISTANCE, .6)
                .add(Attributes.STEP_HEIGHT, 1.1)
                .add(Attributes.FOLLOW_RANGE, 32);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_POUNCE, -100);
        builder.define(DATA_SWIPE, -100);
    }

    /** The tick it last leapt. */
    public int getPounceTick() {
        return this.entityData.get(DATA_POUNCE);
    }

    /** The tick it last swiped. */
    public int getSwipeTick() {
        return this.entityData.get(DATA_SWIPE);
    }

    @Override
    public AbstractSpell getSummoningSpell() {
        return ModSpells.LIVING_CONSTELLATION.get();
    }

    @Override
    protected void registerMoveGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.35, true));
        this.goalSelector.addGoal(2, new SummonFollowOwnerGoal(this, 1.25, 7, 3, -1));
        this.goalSelector.addGoal(3, new WaterAvoidingRandomStrollGoal(this, .8));
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
            // Still taking shape: it waits where it came down
            getNavigation().stop();
            setDeltaMovement(getDeltaMovement().multiply(0, 1, 0));
            return;
        }
        if (pouncing) {
            if (tickCount - getPounceTick() > 4 && (onGround() || horizontalCollision || isInWater()) || tickCount - getPounceTick() > 40) {
                land(level);
            }
            return;
        }
        LivingEntity target = getTarget();
        if (--pounceCooldown <= 0 && target != null && target.isAlive() && onGround() && hasLineOfSight(target)) {
            double d = distanceTo(target);
            if (d > 3.5 && d < 13) {
                pounce(target, d);
            }
        }
    }

    /** It leaps at {@code target} with a roar. */
    private void pounce(LivingEntity target, double distance) {
        Vec3 to = target.position().subtract(position()).multiply(1, 0, 1).normalize();
        double speed = Math.min(1.7, .45 + distance * .13);
        setDeltaMovement(to.x * speed, .5 + distance * .025, to.z * speed);
        hurtMarked = true;
        setYRot((float) (Math.atan2(to.z, to.x) * 180 / Math.PI) - 90);
        yBodyRot = getYRot();
        pouncing = true;
        pounceCooldown = POUNCE_COOLDOWN;
        this.entityData.set(DATA_POUNCE, tickCount);
        CosmosFx.sound(level(), position(), SoundEvents.RAVAGER_ROAR, .9f, 1.5f);
        CosmosFx.sound(level(), position(), SoundEvents.AMETHYST_BLOCK_CHIME, 1.2f, .7f);
    }

    /** It lands in a burst of stars that strikes all round. */
    private void land(ServerLevel level) {
        pouncing = false;
        float damage = (float) getAttributeValue(Attributes.ATTACK_DAMAGE) * 1.6f;
        LivingEntity owner = getSummoner();
        for (LivingEntity e : SpellUtil.enemiesAround(level, position().add(0, .5, 0), 3, this, e -> !SpellUtil.isAlly(owner, e))) {
            DamageSources.applyDamage(e, damage, level.damageSources().mobAttack(this));
            Vec3 out = e.position().subtract(position()).multiply(1, 0, 1);
            out = out.lengthSqr() < 1.0E-4 ? Vec3.ZERO : out.normalize().scale(.7);
            e.push(out.x, .35, out.z);
            e.hurtMarked = true;
        }
        SpellUtil.particles(level, new BlastwaveParticleOptions(1f, .86f, .5f, 3.5f), getX(), getY() + .1, getZ(), 1, 0, 0, 0, 0);
        CosmosFx.stars(level, position().add(0, .4, 0), 40, 1.2);
        SpellUtil.particles(level, ParticleTypes.END_ROD, getX(), getY() + .3, getZ(), 16, .8, .2, .8, .15);
        CosmosFx.sound(level, position(), SoundEvents.AMETHYST_BLOCK_BREAK, 1.4f, .6f);
        CosmosFx.sound(level, position(), SoundEvents.GENERIC_EXPLODE.value(), .5f, 1.6f);
        SpellUtil.cameraShake(level, position(), 5, 6);
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (hit) {
            this.entityData.set(DATA_SWIPE, tickCount);
            Vec3 c = target.getBoundingBox().getCenter();
            CosmosFx.stars(level(), c, 10, .35);
            CosmosFx.sound(level(), c, SoundEvents.AMETHYST_CLUSTER_BREAK, 1, 1.3f);
        }
        return hit;
    }

    @Override
    protected void sparkle() {
        if (random.nextInt(3) == 0) {
            level().addParticle(random.nextInt(3) == 0 ? ParticleTypes.END_ROD : CosmosFx.STAR,
                    getRandomX(1), getY() + random.nextDouble() * getBbHeight(), getRandomZ(1), 0, .01, 0);
        }
    }

    @Override
    protected void dissolveFx() {
        Vec3 c = getBoundingBox().getCenter();
        CosmosFx.stars(level(), c, 50, 1);
        for (int k = 0; k < 14; k++) {
            CosmosFx.streak(level(), c.add(random.nextGaussian() * .6, random.nextGaussian() * .4, random.nextGaussian() * .6),
                    new Vec3(random.nextGaussian() * .05, .4 + random.nextDouble() * .3, random.nextGaussian() * .05), true);
        }
        CosmosFx.sound(level(), c, SoundEvents.AMETHYST_BLOCK_RESONATE, 1.2f, 1.4f);
    }

    @Override
    public boolean causeFallDamage(float fallDistance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return getTarget() != null ? SoundEvents.AMETHYST_BLOCK_RESONATE : null;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.AMETHYST_BLOCK_HIT;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        playSound(SoundEvents.AMETHYST_BLOCK_STEP, .15f, 1.2f);
    }
}
