package com.waylays.arcanes.entity.cosmos;

import com.waylays.arcanes.entity.SpellEffectEntity;
import com.waylays.arcanes.registry.ModEntities;
import com.waylays.arcanes.spells.SpellUtil;
import com.waylays.arcanes.spells.cosmos.Cosmos;
import com.waylays.arcanes.spells.cosmos.CosmosFx;
import com.waylays.arcanes.spells.cosmos.PruningBatonSpell;
import com.waylays.arcanes.spells.cosmos.TimeTrail;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.damage.DamageSources;
import io.redspace.ironsspellbooks.particle.BlastwaveParticleOptions;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * A TVA reset charge set on the ground: a squat bronze device with orange bands and a clock on its top, counting
 * down five seconds with a beep each second, its light quickening, rings of orange running out over the ground. At
 * zero it resets the timeline all round: the caster's side come back as they were at their best these last five
 * seconds (health, and rid of what ails them and of fire); foes are struck and sent back to where they stood five
 * seconds before, dazed, and those left weak enough are pruned from time altogether.
 */
public class ResetCharge extends SpellEffectEntity {
    public static final int COUNTDOWN = 100;
    public static final int AFTER_TICKS = 24;
    /** How far back the timeline is reset. */
    public static final int BACK = 100;

    private static final EntityDataAccessor<Float> DATA_RADIUS = SynchedEntityData.defineId(ResetCharge.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> DATA_FIRE = SynchedEntityData.defineId(ResetCharge.class, EntityDataSerializers.INT);

    @Nullable
    private String spellId;
    private int spellLevel = 1;

    public ResetCharge(EntityType<? extends ResetCharge> entityType, Level level) {
        super(entityType, level);
    }

    public static ResetCharge set(Level level, LivingEntity caster, AbstractSpell spell, int spellLevel, Vec3 at, float radius, float damage) {
        ResetCharge charge = new ResetCharge(ModEntities.RESET_CHARGE.get(), level);
        charge.setOwner(caster);
        charge.spellId = spell.getSpellId();
        charge.spellLevel = spellLevel;
        charge.damage = damage;
        charge.moveTo(at.x, at.y, at.z, caster.getYRot(), 0);
        charge.entityData.set(DATA_RADIUS, radius);
        level.addFreshEntity(charge);
        CosmosFx.sound(level, at, SoundEvents.ANVIL_PLACE, .8f, 1.4f);
        CosmosFx.sound(level, at, SoundEvents.BEACON_ACTIVATE, 1, 1.6f);
        CosmosFx.time(level, at.add(0, .5, 0), 20, .4);
        return charge;
    }

    /** The caster's charge still counting down. */
    @Nullable
    public static ResetCharge of(Level level, LivingEntity caster) {
        for (ResetCharge charge : level.getEntitiesOfClass(ResetCharge.class, caster.getBoundingBox().inflate(64), c -> caster.getUUID().equals(c.getOwnerUUID()))) {
            if (charge.getFireTick() < 0) {
                return charge;
            }
        }
        return null;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_RADIUS, 10f);
        builder.define(DATA_FIRE, -1);
    }

    public float getRadius() {
        return this.entityData.get(DATA_RADIUS);
    }

    /** The tick it went off, or -1. */
    public int getFireTick() {
        return this.entityData.get(DATA_FIRE);
    }

    /** Seconds left (counting down from 5) at {@code age}. */
    public float remaining(float age) {
        int fire = getFireTick();
        return fire >= 0 ? 0 : Math.max(0, (COUNTDOWN - age) / 20f);
    }

    /** Sets it off now (the caster presses the button). */
    public void detonate() {
        if (getFireTick() < 0 && level() instanceof ServerLevel level) {
            fire(level);
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel level)) {
            return;
        }
        int fire = getFireTick();
        if (fire < 0) {
            int left = COUNTDOWN - tickCount;
            if (left % 20 == 0 && left > 0) {
                float pitch = 1 + (COUNTDOWN - left) / (float) COUNTDOWN;
                CosmosFx.sound(level, position(), SoundEvents.NOTE_BLOCK_BELL.value(), 1.4f, pitch);
                CosmosFx.tick(level, position(), 1, pitch);
                SpellUtil.particles(level, new BlastwaveParticleOptions(1f, .54f, .12f, getRadius() * 2 * (1 - left / (float) COUNTDOWN) + 1), getX(), getY() + .05, getZ(), 1, 0, 0, 0, 0);
            } else if (left < 30 && left % 5 == 0) {
                CosmosFx.sound(level, position(), SoundEvents.NOTE_BLOCK_HAT.value(), 1, 2);
            }
            if (left <= 0) {
                fire(level);
            }
        } else if (tickCount - fire > AFTER_TICKS) {
            discard();
        }
    }

    /** The reset. */
    private void fire(ServerLevel level) {
        this.entityData.set(DATA_FIRE, tickCount);
        LivingEntity caster = getLivingOwner();
        AbstractSpell spell = spellId == null ? null : SpellRegistry.getSpell(spellId);
        float r = getRadius();
        List<LivingEntity> inside = new ArrayList<>();
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(r, r, r))) {
            if (e.isAlive() && e.distanceToSqr(this) <= r * r && !e.isSpectator()) {
                inside.add(e);
            }
        }
        for (LivingEntity e : inside) {
            if (Cosmos.isAlly(caster, e)) {
                restore(level, e);
            } else if (spell != null) {
                reset(level, caster, spell, e);
            }
        }
        SpellUtil.particles(level, new BlastwaveParticleOptions(1f, .54f, .12f, r * 2), getX(), getY() + .1, getZ(), 1, 0, 0, 0, 0);
        SpellUtil.particles(level, new BlastwaveParticleOptions(1f, .9f, .75f, r * 1.4f), getX(), getY() + 1, getZ(), 1, 0, 0, 0, 0);
        CosmosFx.time(level, position().add(0, 1, 0), 80, r * .4);
        CosmosFx.fold(level, position(), 2, .6f);
        CosmosFx.sound(level, position(), SoundEvents.WARDEN_SONIC_BOOM, 1.4f, 1.2f);
        CosmosFx.sound(level, position(), SoundEvents.BEACON_DEACTIVATE, 2, .8f);
        SpellUtil.cameraShake(level, position(), 18, r + 10);
    }

    /** The caster's side come back as they were at their best these last seconds. */
    private void restore(ServerLevel level, LivingEntity e) {
        float best = TimeTrail.bestHealth(e, BACK);
        if (best > e.getHealth()) {
            e.setHealth(Math.min(e.getMaxHealth(), best));
        }
        e.getActiveEffects().stream().filter(effect -> !effect.getEffect().value().isBeneficial()).map(MobEffectInstance::getEffect).toList()
                .forEach(e::removeEffect);
        e.clearFire();
        Vec3 c = e.getBoundingBox().getCenter();
        CosmosFx.time(level, c, 20, .5);
        SpellUtil.particles(level, net.minecraft.core.particles.ParticleTypes.HAPPY_VILLAGER, c.x, c.y, c.z, 8, .4, .6, .4, 0);
    }

    /** Foes are struck, sent back where they were and dazed; the weak are pruned. */
    private void reset(ServerLevel level, @Nullable LivingEntity caster, AbstractSpell spell, LivingEntity e) {
        DamageSource source = SpellUtil.damageSource(spell, this, caster);
        DamageSources.applyDamage(e, damage * 3, source);
        if (!e.isAlive()) {
            return;
        }
        if (!Cosmos.isBoss(e) && e.getHealth() <= PruningBatonSpell.pruneLine(spellLevel + 4, e.getMaxHealth())) {
            CosmosFx.prune(level, e, source);
            return;
        }
        Vec3 back = TimeTrail.positionAgo(e, BACK);
        if (back.distanceToSqr(e.position()) > .25 && back.distanceToSqr(e.position()) < 40 * 40 && !Cosmos.isBoss(e)) {
            TemporalEcho.leave(level, e, e.position(), e.getYRot(), 30, Cosmos.ORANGE);
            if (e instanceof Player) {
                e.teleportTo(back.x, back.y, back.z);
            } else {
                e.moveTo(back.x, back.y, back.z, e.getYRot(), e.getXRot());
            }
            e.setDeltaMovement(Vec3.ZERO);
            e.hurtMarked = true;
        }
        e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1));
        e.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 80, 0));
        CosmosFx.time(level, e.getBoundingBox().getCenter(), 16, .4);
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 128 * 128;
    }
}
