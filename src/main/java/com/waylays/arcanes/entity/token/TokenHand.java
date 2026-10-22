package com.waylays.arcanes.entity.token;

import com.waylays.arcanes.entity.SpellEffectEntity;
import com.waylays.arcanes.registry.ModEntities;
import com.waylays.arcanes.spells.SpellUtil;
import com.waylays.arcanes.spells.token.TokenFx;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.damage.DamageSources;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

/**
 * The Hand of Souls: dozens of soul tokens fly in from every side and come together into a giant hand over the
 * target. The hand reaches down and closes on it, lifts it, crushing it, then slams it into the ground, which hurts
 * and throws everyone around; then the tokens scatter. With no one to seize, the open hand slams the spot.
 */
public class TokenHand extends SpellEffectEntity {
    public static final int ASSEMBLE = 30;
    public static final int REACH = 12;
    public static final int HOLD = 40;
    public static final int SLAM = 8;
    public static final int DISPERSE = 24;
    public static final int REACH_END = ASSEMBLE + REACH;
    public static final int HOLD_END = REACH_END + HOLD;
    public static final int SLAM_END = HOLD_END + SLAM;
    public static final int END = SLAM_END + DISPERSE;
    /** Size of the hand (its palm is about two blocks), how high it waits, how high it lifts its catch. */
    public static final float SCALE = 1.8f;
    public static final float HOVER = 6;
    public static final float LIFT = 3;
    public static final float IMPACT_RADIUS = 4;
    public static final int CRUSH_TICKS = 10;

    private static final EntityDataAccessor<Integer> DATA_TARGET = SynchedEntityData.defineId(TokenHand.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_GRABBED = SynchedEntityData.defineId(TokenHand.class, EntityDataSerializers.BOOLEAN);

    @Nullable
    private String spellId;
    private Vec3 hover = Vec3.ZERO;
    private Vec3 grip = Vec3.ZERO;
    private Vec3 lifted = Vec3.ZERO;
    private Vec3 ground = Vec3.ZERO;
    private Vec3 spot = Vec3.ZERO;

    public TokenHand(EntityType<? extends TokenHand> entityType, Level level) {
        super(entityType, level);
    }

    /** The hand gathers over {@code target} (or over {@code spot} if there is none). */
    public static TokenHand summon(Level level, LivingEntity caster, AbstractSpell spell, @Nullable LivingEntity target, Vec3 spot, float damage) {
        TokenHand hand = new TokenHand(ModEntities.TOKEN_HAND.get(), level);
        hand.setOwner(caster);
        hand.spellId = spell.getSpellId();
        hand.damage = damage;
        hand.spot = target != null ? target.position() : spot;
        hand.hover = hand.spot.add(0, HOVER, 0);
        Vec3 d = hand.spot.subtract(caster.position());
        float yaw = (float) (Mth.atan2(-d.x, d.z) * Mth.RAD_TO_DEG);
        hand.moveTo(hand.hover.x, hand.hover.y, hand.hover.z, yaw, 0);
        hand.entityData.set(DATA_TARGET, target != null ? target.getId() : -1);
        level.addFreshEntity(hand);
        return hand;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_TARGET, -1);
        builder.define(DATA_GRABBED, false);
    }

    @Nullable
    public LivingEntity getTarget() {
        return level().getEntity(this.entityData.get(DATA_TARGET)) instanceof LivingEntity living ? living : null;
    }

    public boolean isGrabbing() {
        return this.entityData.get(DATA_GRABBED);
    }

    /** How closed the hand is: 0 open, 1 a fist. */
    public float curl(float partialTicks) {
        float age = tickCount + partialTicks;
        float closing = Mth.clamp((age - (ASSEMBLE + REACH * .5f)) / (REACH * .5f), 0, 1);
        float opening = Mth.clamp((age - SLAM_END) / 6f, 0, 1);
        float closed = isGrabbing() || age < REACH_END ? closing : closing * .35f;
        return closed * (1 - opening);
    }

    private static float ease(float t) {
        t = Mth.clamp(t, 0, 1);
        return t * t * (3 - 2 * t);
    }

    /** Where the hand holds what it caught: under the palm. */
    private Vec3 heldAt(LivingEntity target) {
        return position().subtract(0, target.getBbHeight() * .55 + .3, 0);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            return;
        }
        ServerLevel level = (ServerLevel) level();
        LivingEntity target = getTarget();
        boolean alive = target != null && target.isAlive() && target.distanceToSqr(this) < 40 * 40;
        int age = tickCount;
        if (age == 1) {
            TokenFx.sound(level, hover, SoundEvents.BEACON_POWER_SELECT, 2f, 1.4f);
        }
        if (age < ASSEMBLE) {
            if (age % 4 == 0) {
                TokenFx.clink(level, hover, 1.2f);
            }
            return;
        }
        if (age == ASSEMBLE) {
            TokenFx.sound(level, hover, SoundEvents.BEACON_ACTIVATE, 2f, 1.3f);
        }
        if (age < REACH_END) {
            // Reaches down onto the target (following it), or onto the spot
            Vec3 aim = alive ? target.position().add(0, target.getBbHeight() * .55 + .3, 0) : spot.add(0, 1.4, 0);
            Vec3 p = hover.lerp(aim, ease((age - ASSEMBLE) / (float) REACH));
            setPos(p.x, p.y, p.z);
            if (age == REACH_END - 1) {
                grip = p;
                boolean caught = alive && target.position().distanceToSqr(heldAt(target)) < 3 * 3;
                this.entityData.set(DATA_GRABBED, caught);
                TokenFx.sound(level, p, SoundEvents.CHAIN_PLACE, 2f, .6f);
            }
            return;
        }
        boolean grabbing = isGrabbing() && alive;
        if (age < HOLD_END) {
            Vec3 p = grip.add(0, LIFT * ease((age - REACH_END) / (float) HOLD), 0);
            setPos(p.x, p.y, p.z);
            lifted = p;
            if (grabbing) {
                hold(target);
                if ((age - REACH_END) % CRUSH_TICKS == 0) {
                    crush(level, target, damage * .35f);
                }
            }
            return;
        }
        if (age == HOLD_END) {
            ground = SpellUtil.groundAt(level, position(), 12, 1);
            TokenFx.sound(level, position(), SoundEvents.PHANTOM_SWOOP, 2f, .5f);
        }
        if (age < SLAM_END) {
            float t = (age - HOLD_END) / (float) SLAM;
            Vec3 p = lifted.lerp(ground.add(0, grabbing ? target.getBbHeight() * .55 + .3 + .2 : .8, 0), t * t);
            setPos(p.x, p.y, p.z);
            if (grabbing) {
                hold(target);
            }
            return;
        }
        if (age == SLAM_END) {
            slam(level, grabbing ? target : null);
            return;
        }
        if (age > END) {
            discard();
        }
    }

    /** Keeps the catch in the hand: pulled to the palm, never falling. */
    private void hold(LivingEntity target) {
        Vec3 to = heldAt(target).subtract(target.position());
        target.setDeltaMovement(to.scale(.6));
        target.hurtMarked = true;
        target.fallDistance = 0;
    }

    private void crush(ServerLevel level, LivingEntity target, float amount) {
        AbstractSpell spell = spellId == null ? null : SpellRegistry.getSpell(spellId);
        if (spell != null) {
            target.invulnerableTime = 0;
            DamageSources.applyDamage(target, amount, SpellUtil.damageSource(spell, this, getOwner()));
        }
        TokenFx.burst(level, TokenFx.chest(target), 6, .4);
        TokenFx.sound(level, TokenFx.chest(target), SoundEvents.CHAIN_BREAK, 1f, .7f);
    }

    private void slam(ServerLevel level, @Nullable LivingEntity caught) {
        AbstractSpell spell = spellId == null ? null : SpellRegistry.getSpell(spellId);
        Entity owner = getOwner();
        Vec3 at = ground;
        if (spell != null) {
            if (caught != null) {
                caught.invulnerableTime = 0;
                DamageSources.applyDamage(caught, damage * 2, SpellUtil.damageSource(spell, this, owner));
                caught.setDeltaMovement(0, .2, 0);
                caught.hurtMarked = true;
            }
            for (LivingEntity e : SpellUtil.enemiesAround(level, at.add(0, .5, 0), IMPACT_RADIUS, owner, x -> x != caught)) {
                e.invulnerableTime = 0;
                DamageSources.applyDamage(e, damage * .8f, SpellUtil.damageSource(spell, this, owner));
                Vec3 away = e.position().subtract(at).multiply(1, 0, 1);
                away = away.lengthSqr() < 1.0E-4 ? Vec3.ZERO : away.normalize();
                double resist = 1 - Mth.clamp(e.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE), 0, 1);
                e.setDeltaMovement(e.getDeltaMovement().add(away.scale(1.1 * resist)).add(0, .55 * resist, 0));
                e.hurtMarked = true;
            }
        }
        TokenFx.impact(level, at, IMPACT_RADIUS);
        SpellUtil.particles(level, ParticleTypes.EXPLOSION, at.x, at.y + .3, at.z, 4, 1.2, .2, 1.2, 0);
        SpellUtil.cameraShake(level, at, 20, IMPACT_RADIUS + 12);
        level.playSound(null, at.x, at.y, at.z, SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 1.6f, .7f);
        TokenFx.sound(level, at, SoundEvents.ANVIL_LAND, 1.2f, .5f);
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 128 * 128;
    }
}
