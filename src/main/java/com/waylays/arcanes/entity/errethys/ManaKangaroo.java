package com.waylays.arcanes.entity.errethys;

import com.waylays.arcanes.entity.SummonFollowOwnerGoal;
import com.waylays.arcanes.registry.ModSpells;
import com.waylays.arcanes.spells.SpellUtil;
import com.waylays.arcanes.spells.errethys.ErrethysFx;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import net.minecraft.core.BlockPos;
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

import javax.annotation.Nullable;

/**
 * A mana kangaroo, as tall as a player: it bounds after what its summoner fights and boxes it. Most blows are a
 * single heavy punch that throws the prey back; every third one is a fury of punches, left and right, too fast to
 * dodge.
 */
public class ManaKangaroo extends ManaSummon {
    /** Ticks a fury of punches lasts, and between two of its blows; the share of a punch each blow deals. */
    public static final int FURY_TICKS = 14;
    public static final int FURY_EVERY = 3;
    public static final float FURY_SHARE = .45f;
    /** Every how many blows comes a fury. */
    public static final int FURY_CYCLE = 3;

    private static final byte PUNCH_EVENT = 70;
    private static final byte FURY_EVENT = 71;

    private int blows;
    private int furyTicks;
    @Nullable
    private LivingEntity furyTarget;
    /** Client: ticks left of the punch and fury animations. */
    private int punchAnim;
    private int furyAnim;

    public ManaKangaroo(EntityType<? extends ManaKangaroo> entityType, Level level) {
        super(entityType, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20)
                .add(Attributes.MOVEMENT_SPEED, .33)
                .add(Attributes.ATTACK_DAMAGE, 3)
                .add(Attributes.FOLLOW_RANGE, 24)
                .add(Attributes.KNOCKBACK_RESISTANCE, .3);
    }

    @Override
    public AbstractSpell getSummoningSpell() {
        return ModSpells.MANA_KANGAROO.get();
    }

    @Override
    protected void registerMoveGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.4, true));
        this.goalSelector.addGoal(2, new SummonFollowOwnerGoal(this, 1.3, 5, 2.5, -1));
        this.goalSelector.addGoal(3, new WaterAvoidingRandomStrollGoal(this, .9));
        this.goalSelector.addGoal(4, new LookAtPlayerGoal(this, Player.class, 8));
        this.goalSelector.addGoal(5, new RandomLookAroundGoal(this));
    }

    /** A heavy punch that throws the prey back, or (every third blow) the start of a fury of punches. */
    @Override
    public boolean doHurtTarget(Entity target) {
        blows++;
        if (blows % FURY_CYCLE == 0 && target instanceof LivingEntity prey) {
            furyTarget = prey;
            furyTicks = FURY_TICKS;
            level().broadcastEntityEvent(this, FURY_EVENT);
            return true;
        }
        level().broadcastEntityEvent(this, PUNCH_EVENT);
        boolean hit = super.doHurtTarget(target);
        if (hit && target instanceof LivingEntity prey) {
            Vec3 away = prey.position().subtract(position()).multiply(1, 0, 1);
            away = away.lengthSqr() < 1.0E-4 ? Vec3.directionFromRotation(0, getYRot()) : away.normalize();
            prey.push(away.x * .9, .35, away.z * .9);
            prey.hurtMarked = true;
            Vec3 c = prey.getBoundingBox().getCenter();
            SpellUtil.particles(level(), ParticleTypes.CRIT, c.x, c.y, c.z, 10, .2, .2, .2, .3);
            level().playSound(null, c.x, c.y, c.z, SoundEvents.PLAYER_ATTACK_KNOCKBACK, SoundSource.NEUTRAL, 1f, .9f);
        }
        return hit;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            punchAnim = Math.max(0, punchAnim - 1);
            furyAnim = Math.max(0, furyAnim - 1);
            return;
        }
        if (furyTicks > 0) {
            furyTicks--;
            LivingEntity prey = furyTarget;
            if (prey != null && prey.isAlive() && furyTicks % FURY_EVERY == 0 && distanceToSqr(prey) < 3.5 * 3.5) {
                lookAt(prey, 60, 60);
                prey.invulnerableTime = 0;
                float damage = (float) getAttributeValue(Attributes.ATTACK_DAMAGE) * FURY_SHARE;
                if (prey.hurt(damageSources().mobAttack(this), damage)) {
                    Vec3 c = prey.getBoundingBox().getCenter();
                    SpellUtil.particles(level(), ParticleTypes.CRIT, c.x, c.y, c.z, 5, .25, .25, .25, .3);
                    SpellUtil.particles(level(), ErrethysFx.PALE_DUST, c.x, c.y, c.z, 3, .2, .2, .2, .02);
                    level().playSound(null, c.x, c.y, c.z, SoundEvents.PLAYER_ATTACK_WEAK, SoundSource.NEUTRAL, 1f, 1.2f + random.nextFloat() * .3f);
                }
            }
            if (furyTicks == 0) {
                furyTarget = null;
            }
        }
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id == PUNCH_EVENT) {
            punchAnim = 8;
        } else if (id == FURY_EVENT) {
            furyAnim = FURY_TICKS;
        } else {
            super.handleEntityEvent(id);
        }
    }

    /** 0 to 1 through a punch (client), or 0 when not punching. */
    public float punchProgress(float partialTicks) {
        return punchAnim <= 0 ? 0 : 1 - (punchAnim - partialTicks) / 8f;
    }

    /** Ticks into the fury of punches (client), or -1 when not in one. */
    public float furyTime(float partialTicks) {
        return furyAnim <= 0 ? -1 : FURY_TICKS - furyAnim + partialTicks;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.GOAT_HURT;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.GOAT_AMBIENT;
    }

    @Override
    public float getVoicePitch() {
        return 1.3f + random.nextFloat() * .2f;
    }

    @Override
    protected float getSoundVolume() {
        return .5f;
    }

    /** Each bound thumps the ground. */
    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        playSound(SoundEvents.GOAT_STEP, .3f, .8f);
    }

    @Override
    public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
        return super.causeFallDamage(Math.max(0, distance - 4), multiplier, source);
    }

    /** For the model: the hop of its bounding gait (0 to 1), from how far its legs have swung. */
    public static float hop(float limbSwing) {
        return Mth.abs(Mth.sin(limbSwing * .6f));
    }
}
