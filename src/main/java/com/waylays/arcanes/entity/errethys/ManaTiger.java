package com.waylays.arcanes.entity.errethys;

import com.waylays.arcanes.entity.SummonFollowOwnerGoal;
import com.waylays.arcanes.registry.ModSpells;
import com.waylays.arcanes.spells.SpellUtil;
import com.waylays.arcanes.spells.errethys.ErrethysFx;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
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
 * A mana tiger: a great cat of sky-blue mana, its stripes glowing from within. It stalks what its summoner fights and,
 * from a few blocks away, pounces on it with a roar: a long leap that ends in a blow half again as heavy as its claws,
 * leaving the prey slowed. Up close it rakes with its claws. It takes no fall damage.
 */
public class ManaTiger extends ManaSummon {
    /** The pounce: from how far, how often, how hard, and how long the prey stays slowed. */
    public static final double POUNCE_MIN = 3.5, POUNCE_MAX = 10;
    public static final int POUNCE_EVERY = 100;
    public static final float POUNCE_DAMAGE = 1.5f;
    public static final int POUNCE_SLOW_TICKS = 50;
    private static final int LEAP_TICKS = 30;

    private int pounceCooldown = 30;
    /** Ticks left of a leap in progress (server). */
    private int leapTicks;
    private boolean struck;

    public ManaTiger(EntityType<? extends ManaTiger> entityType, Level level) {
        super(entityType, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 26)
                .add(Attributes.MOVEMENT_SPEED, .33)
                .add(Attributes.ATTACK_DAMAGE, 6)
                .add(Attributes.ATTACK_KNOCKBACK, .6)
                .add(Attributes.KNOCKBACK_RESISTANCE, .4)
                .add(Attributes.STEP_HEIGHT, 1.1)
                .add(Attributes.FOLLOW_RANGE, 32);
    }

    @Override
    public AbstractSpell getSummoningSpell() {
        return ModSpells.MANA_TIGER.get();
    }

    @Override
    protected void registerMoveGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.35, true));
        this.goalSelector.addGoal(2, new SummonFollowOwnerGoal(this, 1.25, 7, 3, -1));
        this.goalSelector.addGoal(3, new WaterAvoidingRandomStrollGoal(this, .8));
        this.goalSelector.addGoal(4, new LookAtPlayerGoal(this, Player.class, 10));
        this.goalSelector.addGoal(5, new RandomLookAroundGoal(this));
    }

    /** Whether it is in the middle of a pounce (server side). */
    public boolean isLeaping() {
        return leapTicks > 0;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide || isRemoved()) {
            return;
        }
        if (pounceCooldown > 0) {
            pounceCooldown--;
        }
        LivingEntity target = getTarget();
        if (leapTicks > 0) {
            leapTicks--;
            Vec3 c = getBoundingBox().getCenter();
            SpellUtil.particles(level(), ErrethysFx.SKY_DUST, c.x, c.y, c.z, 3, .3, .2, .3, 0);
            if (target != null && !struck && getBoundingBox().inflate(.7).intersects(target.getBoundingBox())) {
                strike(target);
            }
            if (leapTicks < LEAP_TICKS - 4 && onGround()) {
                land();
            }
            return;
        }
        if (target != null && target.isAlive() && onGround() && pounceCooldown <= 0 && !isInWater()) {
            double d = distanceTo(target);
            if (d > POUNCE_MIN && d < POUNCE_MAX && hasLineOfSight(target)) {
                leap(target);
            }
        }
    }

    /** It gathers itself and springs at {@code target} with a roar. */
    private void leap(LivingEntity target) {
        Vec3 to = target.position().subtract(position());
        double flat = Math.sqrt(to.x * to.x + to.z * to.z);
        double speed = Math.min(flat * .16, 1.35);
        Vec3 dir = new Vec3(to.x / flat, 0, to.z / flat);
        setDeltaMovement(dir.x * speed, .42 + flat * .03 + Math.max(0, to.y) * .09, dir.z * speed);
        hasImpulse = true;
        float yaw = (float) (Mth.atan2(to.z, to.x) * Mth.RAD_TO_DEG) - 90;
        setYRot(yaw);
        setYBodyRot(yaw);
        setYHeadRot(yaw);
        leapTicks = LEAP_TICKS;
        pounceCooldown = POUNCE_EVERY;
        struck = false;
        getNavigation().stop();
        level().playSound(null, getX(), getY(), getZ(), SoundEvents.RAVAGER_ROAR, SoundSource.NEUTRAL, .7f, 1.5f);
        SpellUtil.particles(level(), ErrethysFx.PALE_DUST, getX(), getY() + .2, getZ(), 14, .4, .1, .4, .02);
    }

    /** The pounce lands on its prey: a heavy blow and the prey slowed. */
    private void strike(LivingEntity target) {
        struck = true;
        float damage = (float) getAttributeValue(Attributes.ATTACK_DAMAGE) * POUNCE_DAMAGE;
        if (target.hurt(damageSources().mobAttack(this), damage)) {
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, POUNCE_SLOW_TICKS, 1), this);
            Vec3 push = getDeltaMovement().multiply(1, 0, 1).normalize().scale(.6);
            target.push(push.x, .25, push.z);
        }
        swing(net.minecraft.world.InteractionHand.MAIN_HAND);
        Vec3 c = target.getBoundingBox().getCenter();
        SpellUtil.particles(level(), ErrethysFx.SKY_DUST, c.x, c.y, c.z, 18, .4, .4, .4, .05);
        SpellUtil.particles(level(), ErrethysFx.PALE_DUST, c.x, c.y, c.z, 8, .3, .3, .3, .02);
        level().playSound(null, c.x, c.y, c.z, SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.NEUTRAL, .9f, .8f);
        level().playSound(null, c.x, c.y, c.z, SoundEvents.AMETHYST_BLOCK_BREAK, SoundSource.NEUTRAL, .6f, .7f);
    }

    private void land() {
        leapTicks = 0;
        SpellUtil.particles(level(), ErrethysFx.SKY_DUST, getX(), getY() + .1, getZ(), 10, .5, .05, .5, .02);
        level().playSound(null, getX(), getY(), getZ(), SoundEvents.CAMEL_STEP_SAND, SoundSource.NEUTRAL, .8f, .7f);
    }

    @Override
    public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
        return false;
    }

    /** Client: motes off its stripes, more of them when it is airborne. */
    @Override
    protected void sparkle() {
        int odds = onGround() ? 3 : 1;
        if (random.nextInt(odds) == 0) {
            level().addParticle(random.nextInt(3) == 0 ? ErrethysFx.PALE_DUST : ErrethysFx.SKY_DUST,
                    getRandomX(.8), getY() + random.nextDouble() * getBbHeight(), getRandomZ(.8), 0, .01, 0);
        }
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return getTarget() != null ? SoundEvents.POLAR_BEAR_WARNING : SoundEvents.CAT_PURR;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.POLAR_BEAR_HURT;
    }

    @Override
    public float getVoicePitch() {
        return 1.25f + random.nextFloat() * .15f;
    }

    @Override
    protected float getSoundVolume() {
        return .6f;
    }

    @Override
    public int getAmbientSoundInterval() {
        return 200;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        playSound(SoundEvents.WOLF_STEP, .2f, .7f);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("PounceCooldown", pounceCooldown);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        pounceCooldown = tag.getInt("PounceCooldown");
    }
}
