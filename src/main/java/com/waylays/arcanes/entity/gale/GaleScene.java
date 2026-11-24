package com.waylays.arcanes.entity.gale;

import com.waylays.arcanes.entity.SpellEffectEntity;
import com.waylays.arcanes.registry.ModEffects;
import com.waylays.arcanes.registry.ModEntities;
import com.waylays.arcanes.spells.SpellUtil;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.damage.DamageSources;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A spell of the Gale at work, drawn by {@code GaleSceneRenderer} (the air bent and shimmering, faintly violet):
 * <ul>
 * <li>{@link Kind#SLASH}, Air Slash: a blade of air flies ahead, cutting all it passes through.</li>
 * <li>{@link Kind#SPHERE}, Air Sphere: a ball of crushed air flies ahead and bursts on the first thing it meets,
 * throwing everything round it back.</li>
 * <li>{@link Kind#SWEEP}, Sweeping Gale: a kick on the ground sends a wave of wind along it, at the legs: those it
 * catches are cut down and slowed.</li>
 * <li>{@link Kind#DASH}, Cutting Dash: the caster darts ahead, cutting whoever is beside the way.</li>
 * <li>{@link Kind#GRAVITY}, Crushing Gravity: the air over a spot weighs down; those under it are bowed, crouched
 * under the weight, slowed, unable to jump.</li>
 * <li>{@link Kind#STORM}, Cor Leonis: for a while blades of air cut endlessly all round the caster, who runs faster, a
 * storm of blades in human form.</li>
 * </ul>
 * None of them summons anything.
 */
public class GaleScene extends SpellEffectEntity {
    public enum Kind { SLASH, SPHERE, SWEEP, DASH, GRAVITY, STORM }

    /** Air Slash: its speed (blocks a tick), how long it flies, how wide it cuts. */
    public static final float SLASH_SPEED = 1.3f, SLASH_WIDTH = 3.2f;
    public static final int SLASH_TIME = 16;
    /** Air Sphere: its speed, how long it flies, its burst's radius, how long the burst is seen. */
    public static final float SPHERE_SPEED = .8f, SPHERE_BURST_R = 4f;
    public static final int SPHERE_TIME = 30, SPHERE_AFTER = 18;
    /** Sweeping Gale: how far the wave runs and in how long, how wide it grows. */
    public static final float SWEEP_REACH = 11, SWEEP_WIDTH = 6;
    public static final int SWEEP_TIME = 14;
    /** Cutting Dash: how far, in how long, how near the way one is cut. */
    public static final float DASH_REACH = 9, DASH_SIDE = 1.8f;
    public static final int DASH_TIME = 7, DASH_AFTER = 20;
    /** Crushing Gravity: its radius and how long it lasts. */
    public static final float GRAVITY_R = 4.5f;
    public static final int GRAVITY_TIME = 120;
    /** Cor Leonis: how long, how far the blades reach, how often they cut. */
    public static final float STORM_R = 4.5f;
    public static final int STORM_TIME = 200, STORM_EVERY = 5;

    private static final EntityDataAccessor<Integer> DATA_KIND = SynchedEntityData.defineId(GaleScene.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_LIFE = SynchedEntityData.defineId(GaleScene.class, EntityDataSerializers.INT);
    /** The way it goes: a unit vector (the caster's look, pitch included). */
    private static final EntityDataAccessor<Vector3f> DATA_AIM = SynchedEntityData.defineId(GaleScene.class, EntityDataSerializers.VECTOR3);
    /** The slash's tilt (radians): flat, or slanted. */
    private static final EntityDataAccessor<Float> DATA_TILT = SynchedEntityData.defineId(GaleScene.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> DATA_CASTER = SynchedEntityData.defineId(GaleScene.class, EntityDataSerializers.INT);
    /** When it burst (the sphere) or stopped (the slash, the dash): the game tick, or -1000. */
    private static final EntityDataAccessor<Integer> DATA_BURST = SynchedEntityData.defineId(GaleScene.class, EntityDataSerializers.INT);
    /** Where the dash ended. */
    private static final EntityDataAccessor<Vector3f> DATA_END = SynchedEntityData.defineId(GaleScene.class, EntityDataSerializers.VECTOR3);

    @Nullable
    private AbstractSpell spell;
    private float power = 1;
    private Vec3 from = Vec3.ZERO;
    private final Set<Integer> hit = new HashSet<>();
    private final Map<Integer, Integer> lastHurt = new HashMap<>();
    /** How many it struck (read by the tests). */
    private int strikes;

    public GaleScene(EntityType<? extends GaleScene> type, Level level) {
        super(type, level);
    }

    /** A scene of {@code kind} at {@code at}, going {@code aim}, slashing at {@code tilt}, lasting {@code life} ticks. */
    public static GaleScene spawn(Level level, @Nullable LivingEntity caster, @Nullable AbstractSpell spell, Kind kind, Vec3 at, Vec3 aim, float tilt,
                                  int life, float power) {
        GaleScene s = new GaleScene(ModEntities.GALE_SCENE.get(), level);
        s.setOwner(caster);
        s.spell = spell;
        s.power = power;
        s.from = at;
        Vec3 d = aim.lengthSqr() < 1.0E-6 ? new Vec3(0, 0, 1) : aim.normalize();
        s.entityData.set(DATA_KIND, kind.ordinal());
        s.entityData.set(DATA_LIFE, life);
        s.entityData.set(DATA_AIM, new Vector3f((float) d.x, (float) d.y, (float) d.z));
        s.entityData.set(DATA_TILT, tilt);
        s.entityData.set(DATA_CASTER, caster == null ? -1 : caster.getId());
        s.moveTo(at.x, at.y, at.z, (float) (Mth.atan2(-d.x, d.z) * Mth.RAD_TO_DEG), 0);
        level.addFreshEntity(s);
        return s;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_KIND, 0);
        builder.define(DATA_LIFE, 20);
        builder.define(DATA_AIM, new Vector3f(0, 0, 1));
        builder.define(DATA_TILT, 0f);
        builder.define(DATA_CASTER, -1);
        builder.define(DATA_BURST, -1000);
        builder.define(DATA_END, new Vector3f());
    }

    public Kind getKind() {
        Kind[] all = Kind.values();
        return all[Mth.clamp(this.entityData.get(DATA_KIND), 0, all.length - 1)];
    }

    public int getLife() {
        return this.entityData.get(DATA_LIFE);
    }

    public Vec3 aim() {
        Vector3f a = this.entityData.get(DATA_AIM);
        return new Vec3(a.x, a.y, a.z);
    }

    public float tilt() {
        return this.entityData.get(DATA_TILT);
    }

    /** Ticks since it burst or stopped (with the partial tick), huge before. */
    public float sinceBurst(float partialTicks) {
        return (int) level().getGameTime() + partialTicks - this.entityData.get(DATA_BURST);
    }

    public boolean burst() {
        return this.entityData.get(DATA_BURST) > -1000;
    }

    /** Where the dash ended (relative to where it began). */
    public Vec3 end() {
        Vector3f e = this.entityData.get(DATA_END);
        return new Vec3(e.x, e.y, e.z);
    }

    @Nullable
    public Entity caster() {
        int id = this.entityData.get(DATA_CASTER);
        return id >= 0 ? level().getEntity(id) : null;
    }

    public int strikes() {
        return strikes;
    }

    private void setBurst() {
        this.entityData.set(DATA_BURST, (int) level().getGameTime());
    }

    private void sound(SoundEvent sound, float volume, float pitch) {
        level().playSound(null, getX(), getY(), getZ(), sound, SoundSource.PLAYERS, volume, pitch);
    }

    private List<LivingEntity> enemies(double radius, Vec3 at) {
        return SpellUtil.enemiesAround(level(), at, radius, getLivingOwner(), x -> true);
    }

    private boolean hurt(LivingEntity e, float amount) {
        LivingEntity caster = getLivingOwner();
        boolean done = spell != null ? DamageSources.applyDamage(e, amount, SpellUtil.damageSource(spell, this, caster))
                : e.hurt(level().damageSources().magic(), amount);
        if (done) {
            strikes++;
        }
        return done;
    }

    /** Hurts {@code e} once every {@code every} ticks at most. */
    private boolean hurtEvery(LivingEntity e, float amount, int every) {
        Integer last = lastHurt.get(e.getId());
        if (last != null && tickCount - last < every) {
            return false;
        }
        lastHurt.put(e.getId(), tickCount);
        return hurt(e, amount);
    }

    private void cutParticles(Entity e) {
        SpellUtil.particles(level(), ParticleTypes.SWEEP_ATTACK, e.getX(), e.getY() + e.getBbHeight() * .5, e.getZ(), 1, .1, .1, .1, 0);
        SpellUtil.particles(level(), ParticleTypes.CRIT, e.getX(), e.getY() + e.getBbHeight() * .5, e.getZ(), 6, .3, .3, .3, .2);
    }

    /** Whether the way from here to {@code to} is blocked. */
    private boolean blocked(Vec3 to) {
        return level().clip(new ClipContext(position(), to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this)).getType() == HitResult.Type.BLOCK;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            return;
        }
        if (tickCount > getLife()) {
            discard();
            return;
        }
        switch (getKind()) {
            case SLASH -> slash();
            case SPHERE -> sphere();
            case SWEEP -> sweep();
            case DASH -> dash();
            case GRAVITY -> gravity();
            case STORM -> storm();
        }
    }

    // ---------------------------------------------------------------- Air Slash

    private void slash() {
        if (tickCount == 1) {
            sound(SoundEvents.PLAYER_ATTACK_SWEEP, 1.4f, .6f);
            sound(SoundEvents.BREEZE_WIND_CHARGE_BURST.value(), .8f, 1.6f);
        }
        if (burst()) {
            return;
        }
        Vec3 next = position().add(aim().scale(SLASH_SPEED));
        if (blocked(next) || tickCount >= SLASH_TIME) {
            setBurst();
            this.entityData.set(DATA_LIFE, tickCount + 8);
            sound(SoundEvents.WIND_CHARGE_BURST.value(), .7f, 1.4f);
            return;
        }
        setPos(next);
        for (LivingEntity e : enemies(SLASH_WIDTH * .55, position())) {
            if (hit.add(e.getId())) {
                hurt(e, power);
                cutParticles(e);
            }
        }
    }

    // ---------------------------------------------------------------- Air Sphere

    private void sphere() {
        if (tickCount == 1) {
            sound(SoundEvents.BREEZE_CHARGE, 1.2f, .7f);
        }
        if (burst()) {
            return;
        }
        Vec3 next = position().add(aim().scale(SPHERE_SPEED));
        boolean touched = !enemies(1.2, position()).isEmpty();
        if (touched || blocked(next) || tickCount >= SPHERE_TIME) {
            setBurst();
            this.entityData.set(DATA_LIFE, tickCount + SPHERE_AFTER);
            sound(SoundEvents.WIND_CHARGE_BURST.value(), 2f, .6f);
            sound(SoundEvents.GENERIC_EXPLODE.value(), .6f, 1.6f);
            SpellUtil.cameraShake(level(), position(), 8, 10);
            for (LivingEntity e : enemies(SPHERE_BURST_R, position())) {
                double d = e.position().distanceTo(position());
                hurt(e, power * (float) (1 - .5 * d / SPHERE_BURST_R));
                Vec3 push = e.position().subtract(position()).normalize();
                e.push(push.x * 1.4, .45, push.z * 1.4);
                e.hurtMarked = true;
            }
            SpellUtil.particles(level(), ParticleTypes.GUST_EMITTER_SMALL, getX(), getY(), getZ(), 1, 0, 0, 0, 0);
            return;
        }
        setPos(next);
    }

    // ---------------------------------------------------------------- Sweeping Gale

    private void sweep() {
        if (tickCount == 1) {
            sound(SoundEvents.BREEZE_JUMP, 1.4f, .6f);
            sound(SoundEvents.PLAYER_ATTACK_SWEEP, 1.2f, .5f);
            SpellUtil.cameraShake(level(), position(), 4, 6);
        }
        if (tickCount > SWEEP_TIME) {
            return;
        }
        float t = tickCount / (float) SWEEP_TIME;
        Vec3 flat = new Vec3(aim().x, 0, aim().z).normalize();
        Vec3 front = position().add(flat.scale(SWEEP_REACH * t));
        float half = (1.5f + (SWEEP_WIDTH - 1.5f) * t) / 2;
        Vec3 side = new Vec3(flat.z, 0, -flat.x);
        for (LivingEntity e : enemies(SWEEP_REACH + 2, position())) {
            Vec3 rel = e.position().subtract(position());
            double along = rel.dot(flat), across = Math.abs(rel.dot(side));
            if (along < 0 || along > SWEEP_REACH * t + .8 || along < SWEEP_REACH * t - 1.6 || across > half + .6 || Math.abs(rel.y) > 1.5) {
                continue;
            }
            if (hit.add(e.getId())) {
                hurt(e, power);
                // Cut at the legs: thrown up off them, slowed
                e.push(flat.x * .3, .35, flat.z * .3);
                e.hurtMarked = true;
                e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 70, 2, false, true, true));
                cutParticles(e);
            }
        }
        // A light touch: big vanilla gusts drowned out the violet wave drawn by the renderer
        if (tickCount % 2 == 0) {
            SpellUtil.particles(level(), ParticleTypes.SMALL_GUST, front.x, front.y + .3, front.z, 1, half, .1, half, 0);
        }
    }

    // ---------------------------------------------------------------- Cutting Dash

    private void dash() {
        Entity caster = getOwner();
        if (!(caster instanceof LivingEntity dasher)) {
            return;
        }
        if (tickCount == 1) {
            sound(SoundEvents.BREEZE_JUMP, 1.4f, 1.2f);
            sound(SoundEvents.PLAYER_ATTACK_SWEEP, 1.2f, 1.4f);
        }
        if (tickCount <= DASH_TIME) {
            Vec3 d = aim();
            dasher.setDeltaMovement(d.x * DASH_REACH / DASH_TIME, Math.max(-.1, d.y * DASH_REACH / DASH_TIME * .4), d.z * DASH_REACH / DASH_TIME);
            dasher.hurtMarked = true;
            dasher.fallDistance = 0;
            for (LivingEntity e : enemies(DASH_SIDE + 1, dasher.position())) {
                if (e != dasher && e.position().distanceTo(dasher.position()) < DASH_SIDE + e.getBbWidth() && hit.add(e.getId())) {
                    hurt(e, power);
                    cutParticles(e);
                }
            }
        } else if (!burst()) {
            Vec3 rel = dasher.position().subtract(from);
            this.entityData.set(DATA_END, new Vector3f((float) rel.x, (float) rel.y, (float) rel.z));
            setBurst();
            dasher.setDeltaMovement(dasher.getDeltaMovement().scale(.3));
            dasher.hurtMarked = true;
        }
    }

    // ---------------------------------------------------------------- Crushing Gravity

    private void gravity() {
        if (tickCount == 1) {
            sound(SoundEvents.ANVIL_LAND, .8f, .5f);
            sound(SoundEvents.WARDEN_SONIC_CHARGE, .6f, 1.6f);
            SpellUtil.cameraShake(level(), position(), 10, GRAVITY_R + 2);
        }
        int left = getLife() - tickCount;
        for (LivingEntity e : enemies(GRAVITY_R, position())) {
            if (Math.abs(e.getY() - getY()) > 3) {
                continue;
            }
            e.addEffect(new MobEffectInstance(ModEffects.CRUSHING_GRAVITY, Math.min(left, 30) + 10, 0, false, true, true));
            // Pressed down: falls fast, cannot rise
            if (e.getDeltaMovement().y > 0) {
                e.setDeltaMovement(e.getDeltaMovement().multiply(1, 0, 1));
            }
            e.setDeltaMovement(e.getDeltaMovement().add(0, -.08, 0));
            e.hurtMarked = true;
            if (tickCount % 20 == 1) {
                hurtEvery(e, power * .25f, 20);
            }
        }
        if (tickCount % 4 == 0) {
            SpellUtil.particles(level(), ParticleTypes.REVERSE_PORTAL, getX(), getY() + 2.5, getZ(), 8, GRAVITY_R * .6, .5, GRAVITY_R * .6, .05);
        }
    }

    // ---------------------------------------------------------------- Cor Leonis

    private void storm() {
        Entity caster = getOwner();
        if (!(caster instanceof LivingEntity bearer) || !bearer.isAlive()) {
            discard();
            return;
        }
        setPos(bearer.position());
        if (tickCount == 1) {
            sound(SoundEvents.BREEZE_WIND_CHARGE_BURST.value(), 2f, .5f);
            sound(SoundEvents.WARDEN_SONIC_BOOM, .5f, 1.8f);
            SpellUtil.cameraShake(level(), position(), 12, 10);
        }
        if (tickCount % 20 == 1) {
            bearer.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 30, 2, false, false, true));
        }
        if (tickCount % STORM_EVERY == 0) {
            for (LivingEntity e : enemies(STORM_R, position().add(0, 1, 0))) {
                if (e != bearer && hurtEvery(e, power, 10)) {
                    Vec3 push = e.position().subtract(position()).normalize();
                    e.push(push.x * .5, .15, push.z * .5);
                    e.hurtMarked = true;
                    cutParticles(e);
                }
            }
            sound(SoundEvents.PLAYER_ATTACK_SWEEP, .7f, 1.2f + random.nextFloat() * .6f);
        }
    }

    @Override
    public boolean isOnFire() {
        return false;
    }
}
