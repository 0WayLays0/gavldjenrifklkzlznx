package com.waylays.arcanes.entity.errethys;

import com.waylays.arcanes.entity.SummonFollowOwnerGoal;
import com.waylays.arcanes.registry.ModSpells;
import com.waylays.arcanes.spells.SpellUtil;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
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
import org.joml.Vector3f;

/**
 * A mana salamander: a long low lizard of red mana, embers glowing in its veins, a crest of little flames down its back.
 * Its bite sets its prey on fire, and when its prey is close it breathes a gout of flame that sets alight every enemy in
 * front of it that is fighting its summoner. Fire cannot hurt it. It never sets blocks alight.
 */
public class ManaSalamander extends ManaSummon {
    /** How long its bite and its breath set the prey burning, in seconds. */
    public static final float BURN_SECONDS = 4;
    /** How far its breath reaches, and how often it breathes. */
    public static final double BREATH_REACH = 4.5;
    public static final int BREATH_EVERY = 60;

    public static final DustParticleOptions RED_DUST = new DustParticleOptions(new Vector3f(1f, .32f, .16f), 1f);
    public static final DustParticleOptions EMBER_DUST = new DustParticleOptions(new Vector3f(1f, .72f, .3f), .7f);

    private int breathCooldown = 30;

    public ManaSalamander(EntityType<? extends ManaSalamander> entityType, Level level) {
        super(entityType, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 12)
                .add(Attributes.MOVEMENT_SPEED, .3)
                .add(Attributes.ATTACK_DAMAGE, 2)
                .add(Attributes.FOLLOW_RANGE, 24);
    }

    @Override
    public AbstractSpell getSummoningSpell() {
        return ModSpells.MANA_SALAMANDER.get();
    }

    @Override
    protected void registerMoveGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.4, true));
        this.goalSelector.addGoal(2, new SummonFollowOwnerGoal(this, 1.3, 5, 2, -1));
        this.goalSelector.addGoal(3, new WaterAvoidingRandomStrollGoal(this, 1));
        this.goalSelector.addGoal(4, new LookAtPlayerGoal(this, Player.class, 6));
        this.goalSelector.addGoal(5, new RandomLookAroundGoal(this));
    }

    /** Its bite sets the prey on fire. */
    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (hit && target instanceof LivingEntity prey) {
            prey.igniteForSeconds(BURN_SECONDS);
            SpellUtil.particles(level(), ParticleTypes.FLAME, prey.getX(), prey.getY() + prey.getBbHeight() * .5, prey.getZ(), 10, .25, .3, .25, .02);
        }
        return hit;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide || isRemoved()) {
            return;
        }
        if (breathCooldown > 0) {
            breathCooldown--;
        }
        LivingEntity target = getTarget();
        if (target != null && target.isAlive() && breathCooldown <= 0 && distanceToSqr(target) < BREATH_REACH * BREATH_REACH && hasLineOfSight(target)) {
            breathe(target);
            breathCooldown = BREATH_EVERY;
        }
    }

    /** A gout of flame toward {@code target}: every enemy of its summoner in the cone in front of it catches fire. */
    private void breathe(LivingEntity target) {
        Vec3 mouth = position().add(0, getBbHeight() * .6, 0);
        Vec3 dir = target.getEyePosition().subtract(mouth).normalize();
        setYRot((float) (Mth.atan2(dir.z, dir.x) * Mth.RAD_TO_DEG) - 90);
        setYHeadRot(getYRot());
        float damage = (float) getAttributeValue(Attributes.ATTACK_DAMAGE) * .5f;
        LivingEntity master = getSummoner();
        // Only what it fights: its prey, and what is going after its summoner or after itself
        for (LivingEntity e : SpellUtil.enemiesAround(level(), mouth.add(dir.scale(BREATH_REACH * .5)), BREATH_REACH * .6 + 1, this,
                x -> x == target || x instanceof Mob m && m.getTarget() != null && (m.getTarget() == this || m.getTarget() == master))) {
            Vec3 to = e.getBoundingBox().getCenter().subtract(mouth);
            if (to.length() <= BREATH_REACH + .5 && to.normalize().dot(dir) > .7) {
                e.hurt(damageSources().mobAttack(this), damage);
                e.igniteForSeconds(BURN_SECONDS);
            }
        }
        for (int i = 1; i <= 9; i++) {
            double t = i / 9.0 * BREATH_REACH;
            Vec3 p = mouth.add(dir.scale(t));
            double spread = .08 + t * .12;
            SpellUtil.particles(level(), ParticleTypes.FLAME, p.x, p.y, p.z, 4, spread, spread, spread, .02);
            if (i % 3 == 0) {
                SpellUtil.particles(level(), RED_DUST, p.x, p.y, p.z, 3, spread, spread, spread, 0);
            }
        }
        SpellUtil.particles(level(), ParticleTypes.LAVA, mouth.x, mouth.y, mouth.z, 2, .1, .1, .1, 0);
        level().playSound(null, getX(), getY(), getZ(), SoundEvents.BLAZE_SHOOT, SoundSource.NEUTRAL, .8f, 1.3f);
        level().playSound(null, getX(), getY(), getZ(), SoundEvents.FIRECHARGE_USE, SoundSource.NEUTRAL, .6f, 1.1f);
    }

    /** Client: embers and red motes rising off it, now and then a little flame on its crest. */
    @Override
    protected void sparkle() {
        if (random.nextInt(3) == 0) {
            level().addParticle(random.nextInt(3) == 0 ? EMBER_DUST : RED_DUST, getRandomX(.7), getY() + random.nextDouble() * getBbHeight(), getRandomZ(.7), 0, .02, 0);
        }
        if (random.nextInt(12) == 0) {
            level().addParticle(ParticleTypes.SMALL_FLAME, getRandomX(.4), getY() + getBbHeight(), getRandomZ(.4), 0, .015, 0);
        }
    }

    /** Server: it comes apart in embers and red motes. */
    @Override
    protected void dissolveFx() {
        Vec3 c = getBoundingBox().getCenter();
        SpellUtil.particles(level(), RED_DUST, c.x, c.y, c.z, 16, .35, .25, .35, 0);
        SpellUtil.particles(level(), EMBER_DUST, c.x, c.y, c.z, 10, .3, .2, .3, 0);
        SpellUtil.particles(level(), ParticleTypes.FLAME, c.x, c.y, c.z, 8, .25, .2, .25, .03);
        level().playSound(null, c.x, c.y, c.z, SoundEvents.FIRE_EXTINGUISH, SoundSource.NEUTRAL, .5f, 1.6f);
    }

    @Override
    public boolean fireImmune() {
        return true;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.FIRE_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.FROG_HURT;
    }

    @Override
    public float getVoicePitch() {
        return 1.3f + random.nextFloat() * .2f;
    }

    @Override
    protected float getSoundVolume() {
        return .5f;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        playSound(SoundEvents.FROG_STEP, .15f, 1.4f);
    }
}
