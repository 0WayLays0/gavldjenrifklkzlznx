package com.waylays.arcanes.entity.errethys;

import com.waylays.arcanes.entity.SummonFollowOwnerGoal;
import com.waylays.arcanes.registry.ModSpells;
import com.waylays.arcanes.spells.SpellUtil;
import com.waylays.arcanes.spells.errethys.ErrethysFx;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
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
 * A mana scarab: a beetle no longer than a hand, but bright, its wing-cases of hard mana shining so it is never lost
 * from sight. Its carapace makes it hard to hurt (armour). While it stays near its summoner it wards them: they take
 * less damage (Resistance, stronger from level 5 of the spell). It bites what its summoner fights, and when its prey is
 * out of reach it opens its wing-cases and flies a short hop to it.
 */
public class ManaScarab extends ManaSummon {
    /** How close to its summoner it must be to ward them. */
    public static final double WARD_RANGE = 8;
    /** The level of the spell from which its ward is stronger. */
    public static final int STRONG_WARD_LEVEL = 5;
    private static final int HOP_EVERY = 50;

    /** The Resistance amplifier it gives its summoner (0: Resistance I). */
    private int ward;
    private int hopCooldown = 20;

    public ManaScarab(EntityType<? extends ManaScarab> entityType, Level level) {
        super(entityType, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 14)
                .add(Attributes.ARMOR, 12)
                .add(Attributes.ARMOR_TOUGHNESS, 4)
                .add(Attributes.KNOCKBACK_RESISTANCE, .5)
                .add(Attributes.MOVEMENT_SPEED, .3)
                .add(Attributes.ATTACK_DAMAGE, 2)
                .add(Attributes.FOLLOW_RANGE, 20);
    }

    /** Set by the spell: the Resistance amplifier its ward gives. */
    public void setWard(int amplifier) {
        this.ward = amplifier;
    }

    public int getWard() {
        return ward;
    }

    @Override
    public AbstractSpell getSummoningSpell() {
        return ModSpells.MANA_SCARAB.get();
    }

    @Override
    protected void registerMoveGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.5, true));
        this.goalSelector.addGoal(2, new SummonFollowOwnerGoal(this, 1.45, 4, 1.5, -1));
        this.goalSelector.addGoal(3, new WaterAvoidingRandomStrollGoal(this, .9));
        this.goalSelector.addGoal(4, new LookAtPlayerGoal(this, Player.class, 6));
        this.goalSelector.addGoal(5, new RandomLookAroundGoal(this));
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide || isRemoved()) {
            return;
        }
        if (hopCooldown > 0) {
            hopCooldown--;
        }
        if (tickCount % 20 == 0) {
            wardSummoner();
        }
        LivingEntity target = getTarget();
        if (target != null && target.isAlive() && onGround() && hopCooldown <= 0) {
            double d = distanceTo(target);
            if (d > 3 && d < 9 && hasLineOfSight(target)) {
                hop(target.position());
            }
        }
    }

    /** Its summoner, close by, takes less damage. */
    private void wardSummoner() {
        LivingEntity master = getSummoner();
        if (master == null || !master.isAlive() || master.level() != level() || distanceToSqr(master) > WARD_RANGE * WARD_RANGE) {
            return;
        }
        master.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 50, ward, true, true), this);
        // A thread of light from the beetle to the one it guards
        Vec3 from = getBoundingBox().getCenter();
        Vec3 to = master.getBoundingBox().getCenter();
        for (int i = 1; i < 8; i++) {
            Vec3 p = from.lerp(to, i / 8.0);
            SpellUtil.particles(level(), ErrethysFx.PALE_DUST, p.x, p.y, p.z, 1, .03, .03, .03, 0);
        }
    }

    /** It opens its wing-cases and flies a short hop toward {@code to}. */
    private void hop(Vec3 to) {
        Vec3 d = to.subtract(position());
        double flat = Math.sqrt(d.x * d.x + d.z * d.z);
        double speed = Math.min(flat * .14, .9);
        setDeltaMovement(d.x / flat * speed, .45, d.z / flat * speed);
        hasImpulse = true;
        hopCooldown = HOP_EVERY;
        level().playSound(null, getX(), getY(), getZ(), SoundEvents.BEE_LOOP_AGGRESSIVE, SoundSource.NEUTRAL, .35f, 1.6f);
    }

    @Override
    public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
        return false;
    }

    /** Client: it is small, so it shines: motes all the time and, now and then, a spark rising off its back. */
    @Override
    protected void sparkle() {
        if (random.nextInt(2) == 0) {
            level().addParticle(random.nextInt(3) == 0 ? ErrethysFx.PALE_DUST : ErrethysFx.SKY_DUST,
                    getRandomX(1.2), getY() + random.nextDouble() * .5, getRandomZ(1.2), 0, .015, 0);
        }
        if (tickCount % 16 == 0) {
            level().addParticle(ParticleTypes.END_ROD, getX(), getY() + getBbHeight() + .1, getZ(), 0, .03, 0);
        }
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return null;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.SHULKER_HURT_CLOSED;
    }

    @Override
    public float getVoicePitch() {
        return 1.6f + random.nextFloat() * .2f;
    }

    @Override
    protected float getSoundVolume() {
        return .5f;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        playSound(SoundEvents.SILVERFISH_STEP, .12f, 1.5f);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Ward", ward);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        ward = tag.getInt("Ward");
    }
}
