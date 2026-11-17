package com.waylays.arcanes.entity.snow;

import com.waylays.arcanes.entity.SpellEffectEntity;
import com.waylays.arcanes.registry.ModEntities;
import com.waylays.arcanes.spells.SpellUtil;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.damage.DamageSources;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
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
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A spell of Snow Magic at work, drawn by {@code SnowSceneRenderer}:
 * <ul>
 * <li>{@link Kind#ROLL}, Avalanche Ball: a snowball rolls ahead along the ground, growing as it goes, bowling over and
 * chilling those it runs into, and bursts into powder at the end of its run (or against a wall).</li>
 * <li>{@link Kind#GLOBE}, Snow Globe: a dome of glass on a brass base closes over an enemy; inside, snow whirls and
 * freezes; no one inside can leave it; then the glass shatters on them.</li>
 * <li>{@link Kind#TRAIL}, Frost Glide: the caster glides ahead on a ribbon of ice; the ribbon stays, chilling and
 * slowing whoever crosses it.</li>
 * <li>{@link Kind#CROWN}, Hoarfrost Crown: a crown of ice crystals turns over the caster's head and looses an icicle
 * at the nearest enemy every second and a half.</li>
 * <li>{@link Kind#WHITEOUT}, Whiteout: a wall of blizzard whirls round the caster; inside, enemies go blind and freeze;
 * at its end, the frozen shatter.</li>
 * </ul>
 * No snow spell summons anything.
 */
public class SnowScene extends SpellEffectEntity {
    public enum Kind { ROLL, GLOBE, TRAIL, CROWN, WHITEOUT }

    /** Avalanche Ball: its run (ticks), its speed (blocks a tick), its first and last radius. */
    public static final int ROLL_TIME = 32;
    public static final float ROLL_SPEED = .55f, ROLL_R0 = .45f, ROLL_R1 = 1.35f;
    /** Snow Globe: its radius, how long it holds. */
    public static final float GLOBE_R = 2.6f;
    public static final int GLOBE_TIME = 90;
    /** Frost Glide: the glide (ticks), the ribbon's length and how long it stays. */
    public static final int GLIDE_TIME = 10, TRAIL_TIME = 140;
    public static final float TRAIL_LENGTH = 10;
    /** Hoarfrost Crown: how long it lasts, how often it shoots, how far. */
    public static final int CROWN_TIME = 300, CROWN_EVERY = 30;
    public static final double CROWN_REACH = 12;
    /** Whiteout: its radius, how long it lasts, when it shatters the frozen. */
    public static final float WHITEOUT_R = 10;
    public static final int WHITEOUT_TIME = 170, SHATTER_AT = 150;

    private static final EntityDataAccessor<Integer> DATA_KIND = SynchedEntityData.defineId(SnowScene.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_LIFE = SynchedEntityData.defineId(SnowScene.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DATA_SIZE = SynchedEntityData.defineId(SnowScene.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_DIR = SynchedEntityData.defineId(SnowScene.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> DATA_STRUCK = SynchedEntityData.defineId(SnowScene.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_VICTIM = SynchedEntityData.defineId(SnowScene.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_CASTER = SynchedEntityData.defineId(SnowScene.class, EntityDataSerializers.INT);
    /** When it burst (the snowball), shattered (the globe, the blizzard's frozen): the game tick, or -1000. */
    private static final EntityDataAccessor<Integer> DATA_BURST = SynchedEntityData.defineId(SnowScene.class, EntityDataSerializers.INT);

    @Nullable
    private AbstractSpell spell;
    private float power = 1;
    private final Set<Integer> hit = new HashSet<>();
    private final Map<Integer, Integer> lastHurt = new HashMap<>();
    private final Set<Integer> captured = new HashSet<>();
    /** How many enemies the scene has struck (read by the tests). */
    private int strikes;

    public SnowScene(EntityType<? extends SnowScene> entityType, Level level) {
        super(entityType, level);
    }

    /**
     * A scene of {@code kind} at {@code at}, facing {@code dir} (degrees, as an entity's yaw), lasting {@code life}
     * ticks; {@code power} is the spell's power (its damage).
     */
    public static SnowScene spawn(Level level, @Nullable LivingEntity caster, @Nullable AbstractSpell spell, Kind kind, Vec3 at, float dir, int life,
                                  float power) {
        SnowScene s = new SnowScene(ModEntities.SNOW_SCENE.get(), level);
        s.setOwner(caster);
        s.spell = spell;
        s.power = power;
        s.entityData.set(DATA_KIND, kind.ordinal());
        s.entityData.set(DATA_LIFE, life);
        s.entityData.set(DATA_DIR, dir);
        s.entityData.set(DATA_CASTER, caster == null ? -1 : caster.getId());
        s.entityData.set(DATA_SIZE, kind == Kind.ROLL ? ROLL_R0 : kind == Kind.GLOBE ? GLOBE_R : kind == Kind.TRAIL ? TRAIL_LENGTH : WHITEOUT_R);
        s.moveTo(at.x, at.y, at.z, dir, 0);
        level.addFreshEntity(s);
        return s;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_KIND, 0);
        builder.define(DATA_LIFE, 20);
        builder.define(DATA_SIZE, 1f);
        builder.define(DATA_DIR, 0f);
        builder.define(DATA_STRUCK, -1000);
        builder.define(DATA_VICTIM, -1);
        builder.define(DATA_CASTER, -1);
        builder.define(DATA_BURST, -1000);
    }

    /** Ticks since it burst or shattered (negative or huge before), with the partial tick. */
    public float sinceBurst(float partialTicks) {
        return (int) level().getGameTime() + partialTicks - this.entityData.get(DATA_BURST);
    }

    public boolean hasBurst() {
        return this.entityData.get(DATA_BURST) > -1000;
    }

    private void burstNow() {
        this.entityData.set(DATA_BURST, (int) level().getGameTime());
    }

    public Kind getKind() {
        Kind[] all = Kind.values();
        return all[Mth.clamp(this.entityData.get(DATA_KIND), 0, all.length - 1)];
    }

    public int getLife() {
        return this.entityData.get(DATA_LIFE);
    }

    /** The snowball's radius, the globe's, the ribbon's length, the blizzard's radius. */
    public float getSize() {
        return this.entityData.get(DATA_SIZE);
    }

    /** The way it faces (degrees, as a yaw). */
    public float getDir() {
        return this.entityData.get(DATA_DIR);
    }

    /** Ticks since it last struck (or burst), with the partial tick. */
    public float sinceStruck(float partialTicks) {
        return (int) level().getGameTime() + partialTicks - this.entityData.get(DATA_STRUCK);
    }

    @Nullable
    public Entity victim() {
        int id = this.entityData.get(DATA_VICTIM);
        return id >= 0 ? level().getEntity(id) : null;
    }

    @Nullable
    public Entity caster() {
        int id = this.entityData.get(DATA_CASTER);
        return id >= 0 ? level().getEntity(id) : null;
    }

    public int strikes() {
        return strikes;
    }

    /** The unit vector it faces, flat. */
    public Vec3 forward() {
        float yaw = getDir() * Mth.DEG_TO_RAD;
        return new Vec3(-Mth.sin(yaw), 0, Mth.cos(yaw));
    }

    private void struck(@Nullable Entity victim) {
        this.entityData.set(DATA_STRUCK, (int) level().getGameTime());
        if (victim != null) {
            this.entityData.set(DATA_VICTIM, victim.getId());
        }
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
                : e.hurt(level().damageSources().freeze(), amount);
        if (done) {
            strikes++;
        }
        return done;
    }

    /** Chills {@code e}: its frost rises by {@code ticks}, up to a little past frozen through. */
    public static void chill(LivingEntity e, int ticks) {
        if (e.canFreeze()) {
            e.setTicksFrozen(Math.min(e.getTicksRequiredToFreeze() + 120, e.getTicksFrozen() + ticks));
        }
    }

    private static void slow(LivingEntity e, int ticks, int amplifier) {
        MobEffectInstance had = e.getEffect(MobEffects.MOVEMENT_SLOWDOWN);
        if (had == null || had.getDuration() < ticks || had.getAmplifier() < amplifier) {
            e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, ticks, amplifier, false, true, true));
        }
    }

    private void snow(double x, double y, double z, int count, double spread) {
        SpellUtil.particles(level(), ParticleTypes.SNOWFLAKE, x, y, z, count, spread, spread * .6, spread, .05);
        SpellUtil.particles(level(), new BlockParticleOption(ParticleTypes.BLOCK, Blocks.SNOW_BLOCK.defaultBlockState()), x, y, z, count / 2, spread, spread * .5,
                spread, .1);
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
            case ROLL -> roll();
            case GLOBE -> globe();
            case TRAIL -> trail();
            case CROWN -> crown();
            case WHITEOUT -> whiteout();
        }
    }

    // ---------------------------------------------------------------- Avalanche Ball

    private void roll() {
        if (hasBurst()) {
            return;
        }
        Vec3 d = forward();
        float t = Math.min(1, tickCount / (float) ROLL_TIME);
        float r = Mth.lerp(t, ROLL_R0, ROLL_R1);
        this.entityData.set(DATA_SIZE, r);
        Vec3 next = position().add(d.scale(ROLL_SPEED));
        // Against a wall it bursts; up and down slopes it follows the ground
        HitResult wall = level().clip(new ClipContext(position().add(0, r, 0), next.add(d.scale(r)).add(0, r, 0), ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, this));
        if (wall.getType() != HitResult.Type.MISS || tickCount >= ROLL_TIME) {
            burst(r);
            return;
        }
        Vec3 ground = SpellUtil.groundAt(level(), next, 3, 1);
        setPos(ground.x, ground.y, ground.z);
        if (tickCount % 2 == 0) {
            snow(getX(), getY() + .2, getZ(), 6, r * .6);
        }
        if (tickCount % 6 == 0) {
            sound(SoundEvents.SNOW_STEP, 1, .6f + r * .2f);
        }
        for (LivingEntity e : enemies(r + .6, position().add(0, r, 0))) {
            if (hit.add(e.getId())) {
                hurt(e, power * (.8f + .6f * t));
                e.knockback(.6 + r * .5, -d.x, -d.z);
                e.hurtMarked = true;
                slow(e, 60, 1);
                chill(e, 80);
                struck(e);
                sound(SoundEvents.SNOW_BREAK, 1.2f, .7f);
            }
        }
    }

    private void burst(float r) {
        burstNow();
        this.entityData.set(DATA_LIFE, tickCount + 20);
        for (LivingEntity e : enemies(3 + r, position().add(0, .5, 0))) {
            hurt(e, power * .5f);
            slow(e, 80, 1);
            chill(e, 140);
        }
        snow(getX(), getY() + r, getZ(), 60, 1.6);
        SpellUtil.particles(level(), ParticleTypes.CLOUD, getX(), getY() + r, getZ(), 20, 1.2, .6, 1.2, .05);
        sound(SoundEvents.SNOW_BREAK, 2, .5f);
        sound(SoundEvents.POWDER_SNOW_BREAK, 2, .7f);
    }

    // ---------------------------------------------------------------- Snow Globe

    private void globe() {
        float r = getSize();
        Vec3 c = position();
        if (tickCount == 1) {
            for (LivingEntity e : enemies(r, c.add(0, .8, 0))) {
                captured.add(e.getId());
            }
            sound(SoundEvents.AMETHYST_BLOCK_PLACE, 1.4f, .7f);
            sound(SoundEvents.GLASS_PLACE, 1.4f, 1.2f);
        }
        int shatter = getLife() - 12;
        for (int id : captured) {
            if (!(level().getEntity(id) instanceof LivingEntity e) || !e.isAlive()) {
                continue;
            }
            if (tickCount < shatter) {
                // No way out: drawn back from the glass
                Vec3 off = e.position().subtract(c).multiply(1, 0, 1);
                if (off.length() > r - .6) {
                    Vec3 back = off.normalize().scale(-.35);
                    e.setDeltaMovement(back.x, Math.min(e.getDeltaMovement().y, .1), back.z);
                    e.hurtMarked = true;
                }
                if (e.getY() > c.y + r * .9) {
                    e.setDeltaMovement(e.getDeltaMovement().x, -.3, e.getDeltaMovement().z);
                    e.hurtMarked = true;
                }
                slow(e, 20, 1);
                chill(e, 4);
            } else if (tickCount == shatter) {
                hurt(e, power * 1.4f);
                chill(e, 100);
                struck(e);
            }
        }
        if (tickCount % 3 == 0 && tickCount < shatter) {
            SpellUtil.particles(level(), ParticleTypes.SNOWFLAKE, c.x, c.y + r * .5, c.z, 4, r * .5, r * .4, r * .5, .02);
        }
        if (tickCount == shatter) {
            burstNow();
            sound(SoundEvents.GLASS_BREAK, 2, .8f);
            sound(SoundEvents.AMETHYST_CLUSTER_BREAK, 1.6f, .9f);
            snow(c.x, c.y + r * .5, c.z, 40, r * .7);
        }
    }

    // ---------------------------------------------------------------- Frost Glide

    private void trail() {
        Vec3 d = forward();
        Entity caster = getOwner();
        if (tickCount <= GLIDE_TIME && caster instanceof LivingEntity c && c.isAlive()) {
            c.setDeltaMovement(d.x * 1.05, Math.min(c.getDeltaMovement().y, .05), d.z * 1.05);
            c.hurtMarked = true;
            c.resetFallDistance();
            if (tickCount % 2 == 0) {
                SpellUtil.particles(level(), ParticleTypes.SNOWFLAKE, c.getX(), c.getY() + .2, c.getZ(), 6, .3, .1, .3, .05);
            }
            if (tickCount == 1) {
                sound(SoundEvents.POWDER_SNOW_PLACE, 1.4f, 1.6f);
                sound(SoundEvents.AMETHYST_BLOCK_CHIME, 1.4f, 1.8f);
            }
        }
        if (tickCount % 5 != 0) {
            return;
        }
        Vec3 a = position(), b = a.add(d.scale(getSize()));
        for (LivingEntity e : enemies(getSize() * .5 + 1.5, a.add(b).scale(.5))) {
            if (Math.abs(e.getY() - getY()) > 2 || segmentDistance(e.position(), a, b) > 1.1) {
                continue;
            }
            slow(e, 40, 2);
            chill(e, 30);
            Integer last = lastHurt.get(e.getId());
            if (last == null || tickCount - last >= 20) {
                lastHurt.put(e.getId(), tickCount);
                hurt(e, power * .3f);
                struck(e);
                SpellUtil.particles(level(), ParticleTypes.SNOWFLAKE, e.getX(), e.getY() + .3, e.getZ(), 8, .3, .2, .3, .03);
            }
        }
    }

    private static double segmentDistance(Vec3 p, Vec3 a, Vec3 b) {
        Vec3 ab = b.subtract(a);
        double t = Mth.clamp(p.subtract(a).dot(ab) / Math.max(1.0E-6, ab.lengthSqr()), 0, 1);
        return a.add(ab.scale(t)).multiply(1, 0, 1).distanceTo(p.multiply(1, 0, 1));
    }

    // ---------------------------------------------------------------- Hoarfrost Crown

    private void crown() {
        Entity caster = getOwner();
        if (!(caster instanceof LivingEntity c) || !c.isAlive()) {
            discard();
            return;
        }
        setPos(c.getX(), c.getY() + c.getBbHeight() + .35, c.getZ());
        if (tickCount == 1) {
            sound(SoundEvents.AMETHYST_BLOCK_RESONATE, 1.4f, 1.4f);
        }
        if (tickCount % CROWN_EVERY != 0) {
            return;
        }
        LivingEntity target = null;
        double best = CROWN_REACH * CROWN_REACH;
        for (LivingEntity e : enemies(CROWN_REACH, c.position())) {
            double d = e.distanceToSqr(c);
            if (d < best && c.hasLineOfSight(e)) {
                best = d;
                target = e;
            }
        }
        if (target == null) {
            return;
        }
        hurt(target, power * .8f);
        slow(target, 40, 1);
        chill(target, 60);
        struck(target);
        Vec3 from = position(), to = target.position().add(0, target.getBbHeight() * .6, 0);
        for (int i = 0; i <= 12; i++) {
            Vec3 p = from.lerp(to, i / 12.0);
            SpellUtil.particles(level(), ParticleTypes.SNOWFLAKE, p.x, p.y, p.z, 1, .02, .02, .02, 0);
        }
        sound(SoundEvents.AMETHYST_BLOCK_HIT, 1.4f, 1.6f);
        level().playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, .6f, 1.8f);
    }

    // ---------------------------------------------------------------- Whiteout

    private void whiteout() {
        float r = getSize();
        if (tickCount == 1) {
            sound(SoundEvents.ELYTRA_FLYING, 1.2f, .5f);
            sound(SoundEvents.POWDER_SNOW_STEP, 2, .5f);
        }
        if (tickCount % 4 == 0 && tickCount < SHATTER_AT) {
            for (int k = 0; k < 6; k++) {
                float a = level().random.nextFloat() * Mth.TWO_PI, rr = r * (.2f + .8f * level().random.nextFloat());
                SpellUtil.particles(level(), ParticleTypes.SNOWFLAKE, getX() + Mth.cos(a) * rr, getY() + 1 + level().random.nextFloat() * 3,
                        getZ() + Mth.sin(a) * rr, 2, .3, .3, .3, .08);
            }
        }
        if (tickCount % 20 == 0 && tickCount < SHATTER_AT) {
            for (LivingEntity e : enemies(r, position().add(0, 1, 0))) {
                e.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 50, 0, false, true, true));
                slow(e, 40, 1);
                chill(e, 50);
                float frozen = e.canFreeze() ? Math.min(1, e.getTicksFrozen() / (float) e.getTicksRequiredToFreeze()) : 0;
                hurt(e, power * .2f * (1 + frozen));
            }
        }
        if (tickCount == SHATTER_AT) {
            burstNow();
            for (LivingEntity e : enemies(r, position().add(0, 1, 0))) {
                boolean frozen = e.isFullyFrozen();
                hurt(e, power * (frozen ? 2.2f : .6f));
                if (frozen) {
                    SpellUtil.particles(level(), new BlockParticleOption(ParticleTypes.BLOCK, Blocks.PACKED_ICE.defaultBlockState()), e.getX(),
                            e.getY() + e.getBbHeight() * .5, e.getZ(), 40, .4, .6, .4, .2);
                    level().playSound(null, e.getX(), e.getY(), e.getZ(), SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 1.2f, .7f);
                }
            }
            SpellUtil.cameraShake(level(), position(), 20, r * 2);
            sound(SoundEvents.AMETHYST_CLUSTER_BREAK, 3, .5f);
            snow(getX(), getY() + 1.5, getZ(), 80, r * .5);
        }
    }

    @Override
    public boolean isOnFire() {
        return false;
    }
}
