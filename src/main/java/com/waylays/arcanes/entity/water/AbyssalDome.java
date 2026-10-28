package com.waylays.arcanes.entity.water;

import com.waylays.arcanes.entity.SpellEffectEntity;
import com.waylays.arcanes.registry.ModEntities;
import com.waylays.arcanes.spells.SpellUtil;
import com.waylays.arcanes.spells.water.WaterFx;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.damage.DamageSources;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

/**
 * The Abyssal Dome (Water Magic's ultimate): the sea itself rises round the caster in a great dome of deep water, the
 * light from above rippling over the ground in it, shoals of fish and a manta ray gliding through it, kelp swaying at
 * its rim. Under it the caster's side breathe as fish do and move through it with ease, mending; their enemies are
 * slowed and crushed by the weight of the deep, those that breathe air run out of it and drown (harder), and their
 * shots crawl through the water. At its end the dome collapses, the sea crashing down on all the enemies under it.
 */
public class AbyssalDome extends SpellEffectEntity {
    public static final int FORM = 30;
    public static final int COLLAPSE = 24;
    public static final int DROWN_TICKS = 10;
    /** The share of the drowning damage dealt to enemies not (or not yet) drowning: the weight of the deep. */
    public static final float CRUSH = .4f;

    private static final EntityDataAccessor<Integer> DATA_LIFE = SynchedEntityData.defineId(AbyssalDome.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DATA_RADIUS = SynchedEntityData.defineId(AbyssalDome.class, EntityDataSerializers.FLOAT);

    @Nullable
    private String spellId;

    public AbyssalDome(EntityType<? extends AbyssalDome> entityType, Level level) {
        super(entityType, level);
    }

    public static AbyssalDome raise(Level level, LivingEntity caster, AbstractSpell spell, Vec3 at, float radius, int life, float damage) {
        AbyssalDome dome = new AbyssalDome(ModEntities.ABYSSAL_DOME.get(), level);
        dome.setOwner(caster);
        dome.spellId = spell.getSpellId();
        dome.damage = damage;
        dome.moveTo(at.x, at.y, at.z, 0, 0);
        dome.entityData.set(DATA_LIFE, life);
        dome.entityData.set(DATA_RADIUS, radius);
        level.addFreshEntity(dome);
        WaterFx.sound(level, at, SoundEvents.AMBIENT_UNDERWATER_ENTER, 3, .5f);
        WaterFx.sound(level, at, SoundEvents.CONDUIT_ACTIVATE, 2.5f, .7f);
        WaterFx.crash(level, at, 2.5f);
        return dome;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_LIFE, 300);
        builder.define(DATA_RADIUS, 12f);
    }

    public int getLife() {
        return this.entityData.get(DATA_LIFE);
    }

    public float getRadius() {
        return this.entityData.get(DATA_RADIUS);
    }

    /** How far it has grown (0 to 1), easing out as the water swells to its full size. */
    public float formed(float age) {
        float t = Mth.clamp(age / FORM, 0, 1);
        return 1 - (1 - t) * (1 - t) * (1 - t);
    }

    /** How far it has collapsed (0 to 1). */
    public float collapsed(float age) {
        return Mth.clamp((age - getLife()) / COLLAPSE, 0, 1);
    }

    /** Whether {@code e} stands under the dome. */
    public boolean holds(Entity e) {
        Vec3 d = e.position().subtract(position());
        float r = getRadius() * formed(tickCount);
        return d.y > -2 && d.lengthSqr() < r * r;
    }

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel level)) {
            return;
        }
        if (tickCount > getLife() + COLLAPSE) {
            discard();
            return;
        }
        LivingEntity caster = getLivingOwner();
        AbstractSpell spell = spellId == null ? null : SpellRegistry.getSpell(spellId);
        float r = getRadius();
        if (tickCount == getLife()) {
            collapse(level, caster, spell, r);
        }
        if (tickCount >= getLife()) {
            return;
        }
        if (tickCount < FORM && tickCount % 3 == 0) {
            float grown = r * formed(tickCount);
            for (int k = 0; k < 8; k++) {
                float a = k * Mth.TWO_PI / 8 + tickCount * .2f;
                WaterFx.spray(level, position().add(Mth.cos(a) * grown, .2, Mth.sin(a) * grown), 3, .5);
            }
        }
        if (tickCount % 40 == 0) {
            WaterFx.sound(level, position(), SoundEvents.AMBIENT_UNDERWATER_LOOP_ADDITIONS_RARE, 2, .7f);
            WaterFx.sound(level, position(), SoundEvents.CONDUIT_AMBIENT, 2, .8f);
        }
        if (tickCount % 4 == 0) {
            for (int k = 0; k < 3; k++) {
                double a = random.nextDouble() * Mth.TWO_PI, d = Math.sqrt(random.nextDouble()) * r * .9;
                WaterFx.bubbles(level, position().add(Math.cos(a) * d, .3 + random.nextDouble() * 3, Math.sin(a) * d), 2, .2);
            }
        }
        if (tickCount < FORM / 2) {
            return;
        }
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(position(), position()).inflate(r + 1))) {
            if (!e.isAlive() || !holds(e)) {
                continue;
            }
            if (e == caster || SpellUtil.isAlly(caster, e)) {
                // The caster's side breathe and move through the deep as fish do, and mend in it
                if (tickCount % 20 == 0) {
                    e.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING, 50, 0, true, true));
                    e.addEffect(new MobEffectInstance(MobEffects.DOLPHINS_GRACE, 50, 0, true, true));
                    e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 50, 0, true, true));
                    e.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 50, 0, true, true));
                    e.setAirSupply(e.getMaxAirSupply());
                }
                continue;
            }
            // Their enemies wade, crushed by the weight of the deep; those that breathe air run out of it and drown
            if (tickCount % 20 == 0) {
                e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 1, false, true));
                e.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 40, 1, false, true));
                e.clearFire();
            }
            boolean drowning = false;
            if (!e.canBreatheUnderwater()) {
                e.setAirSupply(Math.max(-20, e.getAirSupply() - 8));
                drowning = e.getAirSupply() <= 0;
            }
            if (tickCount % DROWN_TICKS == 0 && spell != null) {
                WaterFx.hurtInPlace(e, drowning ? damage : damage * CRUSH, SpellUtil.damageSource(spell, this, caster));
                if (drowning) {
                    WaterFx.bubbles(level, e.getEyePosition(), 10, .2);
                }
            }
        }
        // Enemy shots crawl through the water
        for (Projectile shot : level.getEntitiesOfClass(Projectile.class, new AABB(position(), position()).inflate(r + 2))) {
            Entity from = shot.getOwner();
            if (!holds(shot) || from == caster || from != null && SpellUtil.isAlly(caster, from)) {
                continue;
            }
            Vec3 v = shot.getDeltaMovement();
            if (v.lengthSqr() > .3 * .3) {
                shot.setDeltaMovement(v.scale(.8));
                shot.hurtMarked = true;
                if (tickCount % 3 == 0) {
                    WaterFx.bubbles(level, shot.position(), 3, .1);
                }
            }
        }
    }

    /** The dome collapses: the sea crashes down on every enemy under it. */
    private void collapse(ServerLevel level, @Nullable LivingEntity caster, @Nullable AbstractSpell spell, float r) {
        for (LivingEntity e : SpellUtil.enemiesAround(level, position(), r + 1, caster, x -> x.getY() > getY() - 2)) {
            if (spell != null) {
                e.invulnerableTime = 0;
                DamageSources.applyDamage(e, damage * 2.5f, SpellUtil.damageSource(spell, this, caster));
            }
            e.setDeltaMovement(e.getDeltaMovement().multiply(.3, 0, .3).add(0, -1.2, 0));
            e.hurtMarked = true;
            WaterFx.soak(e, 80);
        }
        for (int k = 0; k < 10; k++) {
            float a = k * Mth.TWO_PI / 10;
            SplashCrown.at(level, position().add(Mth.cos(a) * r * .7, 0, Mth.sin(a) * r * .7), 2.4f);
        }
        SplashCrown.at(level, position(), 3.4f);
        WaterFx.splash(level, position().add(0, 2, 0), 160, r * .6);
        WaterFx.crash(level, position(), 4);
        WaterFx.sound(level, position(), SoundEvents.CONDUIT_DEACTIVATE, 2.5f, .6f);
        SpellUtil.cameraShake(level, position(), 24, r + 20);
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 192 * 192;
    }
}
