package com.waylays.arcanes.entity.lava;

import com.waylays.arcanes.entity.SpellEffectEntity;
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
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * A spell of Lava Magic at work, drawn by {@code LavaSceneRenderer}:
 * <ul>
 * <li>{@link Kind#SPIT}, Magma Spit: a gob of magma lobbed in an arc; where it lands it splashes into a bubbling pool
 * that burns whoever stands in it.</li>
 * <li>{@link Kind#GEYSER}, Lava Geyser: the ground cracks and glows, then a column of lava bursts up, flinging and
 * burning those over it, raining drops of fire round about.</li>
 * <li>{@link Kind#CARAPACE}, Obsidian Carapace: plates of obsidian veined with lava float round the caster, who takes
 * less harm and no fire; whoever strikes them in close catches fire.</li>
 * <li>{@link Kind#RIVER}, Lava River: a river of lava flows out ahead, crust drifting on it, slowing and burning those in
 * it, then cools into black crust.</li>
 * <li>{@link Kind#CALDERA}, Caldera: the ground all round the caster cracks and shakes, then caves into a lake of
 * magma that flings enemies up and keeps burning.</li>
 * </ul>
 * No lava spell summons anything, and none sets the world on fire: the fire is only on those it burns.
 */
public class LavaScene extends SpellEffectEntity {
    public enum Kind { SPIT, GEYSER, CARAPACE, RIVER, CALDERA }

    /** Magma Spit: its flight (ticks), how high it arcs, its pool's radius and how long the pool stays. */
    public static final int SPIT_FLIGHT = 14, POOL_TIME = 90;
    public static final float SPIT_ARC = 3.5f, POOL_R = 2.2f;
    /** Lava Geyser: the warning, the eruption, its radius and height. */
    public static final int GEYSER_WARN = 20, GEYSER_BURST = 16;
    public static final float GEYSER_R = 1.7f, GEYSER_HEIGHT = 6;
    /** Obsidian Carapace: how long it lasts. */
    public static final int CARAPACE_TIME = 240;
    /** Lava River: how long it takes to flow out, how long it stays, its length and half width. */
    public static final int RIVER_FLOW = 20, RIVER_TIME = 120;
    public static final float RIVER_LENGTH = 12, RIVER_HALF = 1;
    /** Caldera: the cracking, when it caves in, how long the lake stays, its radius. */
    public static final int CALDERA_COLLAPSE = 40, CALDERA_TIME = 140;
    public static final float CALDERA_R = 8;

    /** Each caster's carapace, while it lasts. */
    private static final Map<UUID, LavaScene> CARAPACES = new HashMap<>();

    private static final EntityDataAccessor<Integer> DATA_KIND = SynchedEntityData.defineId(LavaScene.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_LIFE = SynchedEntityData.defineId(LavaScene.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DATA_SIZE = SynchedEntityData.defineId(LavaScene.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_DIR = SynchedEntityData.defineId(LavaScene.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> DATA_LANDED = SynchedEntityData.defineId(LavaScene.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_STRUCK = SynchedEntityData.defineId(LavaScene.class, EntityDataSerializers.INT);

    @Nullable
    private AbstractSpell spell;
    private float power = 1;
    private Vec3 from = Vec3.ZERO, to = Vec3.ZERO;
    private final Map<Integer, Integer> lastHurt = new HashMap<>();
    private int strikes;

    public LavaScene(EntityType<? extends LavaScene> entityType, Level level) {
        super(entityType, level);
    }

    /**
     * A scene of {@code kind} at {@code at} (the spit: thrown from {@code at} to {@code target}), facing {@code dir}
     * (degrees, as a yaw), lasting {@code life} ticks; {@code power} is the spell's power.
     */
    public static LavaScene spawn(Level level, @Nullable LivingEntity caster, @Nullable AbstractSpell spell, Kind kind, Vec3 at, @Nullable Vec3 target,
                                  float dir, int life, float power) {
        LavaScene s = new LavaScene(ModEntities.LAVA_SCENE.get(), level);
        s.setOwner(caster);
        s.spell = spell;
        s.power = power;
        s.from = at;
        s.to = target == null ? at : target;
        s.entityData.set(DATA_KIND, kind.ordinal());
        s.entityData.set(DATA_LIFE, life);
        s.entityData.set(DATA_DIR, dir);
        s.entityData.set(DATA_SIZE, switch (kind) {
            case SPIT -> POOL_R;
            case GEYSER -> GEYSER_R;
            case CARAPACE -> 1;
            case RIVER -> RIVER_LENGTH;
            case CALDERA -> CALDERA_R;
        });
        s.moveTo(at.x, at.y, at.z, dir, 0);
        level.addFreshEntity(s);
        if (kind == Kind.CARAPACE && caster != null && !level.isClientSide) {
            LavaScene old = CARAPACES.put(caster.getUUID(), s);
            if (old != null && old != s) {
                old.discard();
            }
        }
        return s;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_KIND, 0);
        builder.define(DATA_LIFE, 20);
        builder.define(DATA_SIZE, 1f);
        builder.define(DATA_DIR, 0f);
        builder.define(DATA_LANDED, -1000);
        builder.define(DATA_STRUCK, -1000);
    }

    public Kind getKind() {
        Kind[] all = Kind.values();
        return all[Mth.clamp(this.entityData.get(DATA_KIND), 0, all.length - 1)];
    }

    public int getLife() {
        return this.entityData.get(DATA_LIFE);
    }

    public float getSize() {
        return this.entityData.get(DATA_SIZE);
    }

    public float getDir() {
        return this.entityData.get(DATA_DIR);
    }

    public Vec3 forward() {
        float yaw = getDir() * Mth.DEG_TO_RAD;
        return new Vec3(-Mth.sin(yaw), 0, Mth.cos(yaw));
    }

    /** The spit: whether it has landed, and ticks since (with the partial tick). */
    public boolean landed() {
        return this.entityData.get(DATA_LANDED) > -1000;
    }

    public float sinceLanded(float partialTicks) {
        return (int) level().getGameTime() + partialTicks - this.entityData.get(DATA_LANDED);
    }

    /** Ticks since it last struck someone (the carapace's burn), with the partial tick. */
    public float sinceStruck(float partialTicks) {
        return (int) level().getGameTime() + partialTicks - this.entityData.get(DATA_STRUCK);
    }

    public int strikes() {
        return strikes;
    }

    /** The carapace of {@code caster}, if one is on them. */
    @Nullable
    public static LavaScene carapaceOf(LivingEntity caster) {
        LavaScene s = CARAPACES.get(caster.getUUID());
        return s != null && !s.isRemoved() ? s : null;
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
                : e.hurt(level().damageSources().lava(), amount);
        if (done) {
            strikes++;
        }
        return done;
    }

    /** Sets {@code e} burning for {@code seconds} at least. */
    public static void burn(LivingEntity e, int seconds) {
        if (!e.fireImmune() && e.getRemainingFireTicks() < seconds * 20) {
            e.igniteForSeconds(seconds);
        }
    }

    /** Burns those in an area once a second each: {@code amount} each time. */
    private void scorch(List<LivingEntity> in, float amount, int seconds) {
        for (LivingEntity e : in) {
            burn(e, seconds);
            Integer last = lastHurt.get(e.getId());
            if (last == null || tickCount - last >= 20) {
                lastHurt.put(e.getId(), tickCount);
                hurt(e, amount);
            }
        }
    }

    private void embers(double x, double y, double z, int count, double spread) {
        SpellUtil.particles(level(), ParticleTypes.LAVA, x, y, z, count, spread, spread * .3, spread, 0);
        SpellUtil.particles(level(), ParticleTypes.FLAME, x, y + .3, z, count, spread, spread * .5, spread, .03);
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
            case SPIT -> spit();
            case GEYSER -> geyser();
            case CARAPACE -> carapace();
            case RIVER -> river();
            case CALDERA -> caldera();
        }
    }

    @Override
    public void remove(RemovalReason reason) {
        if (getKind() == Kind.CARAPACE && ownerUUID != null && CARAPACES.get(ownerUUID) == this) {
            CARAPACES.remove(ownerUUID);
        }
        super.remove(reason);
    }

    // ---------------------------------------------------------------- Magma Spit

    private void spit() {
        if (!landed()) {
            float s = Math.min(1, tickCount / (float) SPIT_FLIGHT);
            Vec3 p = from.lerp(to, s).add(0, SPIT_ARC * 4 * s * (1 - s), 0);
            setPos(p.x, p.y, p.z);
            if (tickCount % 2 == 0) {
                SpellUtil.particles(level(), ParticleTypes.FLAME, p.x, p.y, p.z, 3, .1, .1, .1, .01);
            }
            if (s >= 1) {
                this.entityData.set(DATA_LANDED, (int) level().getGameTime());
                this.entityData.set(DATA_LIFE, tickCount + POOL_TIME);
                for (LivingEntity e : enemies(POOL_R, position().add(0, .5, 0))) {
                    hurt(e, power * .9f);
                    burn(e, 4);
                }
                embers(getX(), getY() + .2, getZ(), 18, 1);
                sound(SoundEvents.LAVA_POP, 1.6f, .7f);
                sound(SoundEvents.FIRECHARGE_USE, 1.2f, .6f);
            }
            return;
        }
        if (tickCount % 10 == 0) {
            scorch(enemies(POOL_R, position().add(0, .5, 0)), power * .3f, 3);
        }
        if (tickCount % 7 == 0) {
            SpellUtil.particles(level(), ParticleTypes.LAVA, getX(), getY() + .1, getZ(), 2, POOL_R * .5, 0, POOL_R * .5, 0);
            sound(SoundEvents.LAVA_AMBIENT, .4f, 1.2f);
        }
    }

    // ---------------------------------------------------------------- Lava Geyser

    private void geyser() {
        if (tickCount == 1) {
            sound(SoundEvents.BASALT_BREAK, 1.6f, .6f);
        }
        if (tickCount < GEYSER_WARN) {
            if (tickCount % 4 == 0) {
                SpellUtil.particles(level(), ParticleTypes.SMOKE, getX(), getY() + .1, getZ(), 6, GEYSER_R * .6, .05, GEYSER_R * .6, .02);
            }
            return;
        }
        if (tickCount == GEYSER_WARN) {
            this.entityData.set(DATA_STRUCK, (int) level().getGameTime());
            sound(SoundEvents.GENERIC_EXPLODE.value(), 1.2f, .7f);
            sound(SoundEvents.LAVA_POP, 2, .5f);
            SpellUtil.cameraShake(level(), position(), 10, 10);
            for (LivingEntity e : enemies(GEYSER_R + .4, position().add(0, 1, 0))) {
                hurt(e, power * 1.4f);
                burn(e, 5);
                e.setDeltaMovement(e.getDeltaMovement().x * .3, 1.15, e.getDeltaMovement().z * .3);
                e.hurtMarked = true;
            }
        }
        if (tickCount < GEYSER_WARN + GEYSER_BURST) {
            for (LivingEntity e : enemies(GEYSER_R + .4, position().add(0, GEYSER_HEIGHT * .5, 0))) {
                if (Math.abs(e.getX() - getX()) < GEYSER_R + .4 && Math.abs(e.getZ() - getZ()) < GEYSER_R + .4 && e.getY() < getY() + GEYSER_HEIGHT) {
                    burn(e, 5);
                }
            }
            embers(getX(), getY() + GEYSER_HEIGHT * level().random.nextFloat(), getZ(), 4, GEYSER_R * .5);
        }
    }

    // ---------------------------------------------------------------- Obsidian Carapace

    private void carapace() {
        Entity caster = getOwner();
        if (!(caster instanceof LivingEntity c) || !c.isAlive()) {
            discard();
            return;
        }
        setPos(c.getX(), c.getY(), c.getZ());
        if (tickCount == 1) {
            sound(SoundEvents.BASALT_PLACE, 1.4f, .5f);
            sound(SoundEvents.LAVA_EXTINGUISH, 1, .8f);
        }
        if (tickCount % 20 == 1) {
            c.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 30, 1, false, false, true));
            c.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 30, 0, false, false, true));
        }
    }

    /** Someone struck the carapace's wearer in close: they catch fire and are scorched. */
    public void strikeBack(LivingEntity attacker) {
        Integer last = lastHurt.get(attacker.getId());
        if (last != null && tickCount - last < 10) {
            return;
        }
        lastHurt.put(attacker.getId(), tickCount);
        burn(attacker, 4);
        hurt(attacker, power * .5f);
        this.entityData.set(DATA_STRUCK, (int) level().getGameTime());
        embers(attacker.getX(), attacker.getY() + 1, attacker.getZ(), 8, .4);
        sound(SoundEvents.LAVA_POP, 1.2f, 1.2f);
    }

    // ---------------------------------------------------------------- Lava River

    private void river() {
        if (tickCount == 1) {
            sound(SoundEvents.LAVA_AMBIENT, 2, .6f);
            sound(SoundEvents.BUCKET_EMPTY_LAVA, 2, .7f);
        }
        float flowed = getSize() * Math.min(1, tickCount / (float) RIVER_FLOW);
        if (tickCount % 5 != 0 || tickCount > getLife() - 20) {
            return;
        }
        Vec3 d = forward(), a = position(), b = a.add(d.scale(flowed));
        for (LivingEntity e : enemies(flowed * .5 + 2, a.add(b).scale(.5))) {
            if (Math.abs(e.getY() - getY()) > 1.5 || segmentDistance(e.position(), a, b) > RIVER_HALF + .3) {
                continue;
            }
            e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20, 1, false, true, true));
            scorch(List.of(e), power * .35f, 3);
        }
        Vec3 p = a.add(d.scale(level().random.nextFloat() * flowed));
        SpellUtil.particles(level(), ParticleTypes.LAVA, p.x, p.y + .1, p.z, 1, .4, 0, .4, 0);
    }

    private static double segmentDistance(Vec3 p, Vec3 a, Vec3 b) {
        Vec3 ab = b.subtract(a);
        double t = Mth.clamp(p.subtract(a).dot(ab) / Math.max(1.0E-6, ab.lengthSqr()), 0, 1);
        return a.add(ab.scale(t)).multiply(1, 0, 1).distanceTo(p.multiply(1, 0, 1));
    }

    // ---------------------------------------------------------------- Caldera

    private void caldera() {
        float r = getSize();
        if (tickCount < CALDERA_COLLAPSE) {
            if (tickCount % 10 == 0) {
                SpellUtil.cameraShake(level(), position(), 10, r * 2);
                sound(SoundEvents.BASALT_BREAK, 2, .4f + tickCount * .01f);
                for (LivingEntity e : enemies(r, position().add(0, 1, 0))) {
                    e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20, 1, false, true, true));
                }
            }
            if (tickCount % 3 == 0) {
                float a = level().random.nextFloat() * Mth.TWO_PI, rr = r * level().random.nextFloat();
                SpellUtil.particles(level(), ParticleTypes.SMOKE, getX() + Mth.cos(a) * rr, getY() + .1, getZ() + Mth.sin(a) * rr, 4, .3, .05, .3, .02);
            }
            return;
        }
        if (tickCount == CALDERA_COLLAPSE) {
            this.entityData.set(DATA_LANDED, (int) level().getGameTime());
            SpellUtil.cameraShake(level(), position(), 30, r * 3);
            sound(SoundEvents.GENERIC_EXPLODE.value(), 3, .5f);
            sound(SoundEvents.LAVA_POP, 3, .4f);
            for (LivingEntity e : enemies(r, position().add(0, 1, 0))) {
                hurt(e, power * 2.4f);
                burn(e, 8);
                e.setDeltaMovement(e.getDeltaMovement().x, .9, e.getDeltaMovement().z);
                e.hurtMarked = true;
            }
            for (int k = 0; k < 12; k++) {
                float a = k * Mth.TWO_PI / 12;
                embers(getX() + Mth.cos(a) * r * .6, getY() + .3, getZ() + Mth.sin(a) * r * .6, 6, .6);
            }
            return;
        }
        if (tickCount % 10 == 0 && tickCount < getLife() - 15) {
            scorch(enemies(r, position().add(0, .5, 0)), power * .4f, 4);
        }
        if (tickCount % 4 == 0) {
            float a = level().random.nextFloat() * Mth.TWO_PI, rr = r * Mth.sqrt(level().random.nextFloat());
            SpellUtil.particles(level(), ParticleTypes.LAVA, getX() + Mth.cos(a) * rr, getY() + .2, getZ() + Mth.sin(a) * rr, 2, .2, 0, .2, 0);
        }
        if (tickCount % 15 == 0) {
            sound(SoundEvents.LAVA_AMBIENT, 1.5f, .7f);
        }
    }

    /** The caldera: whether it has caved in, and ticks since. */
    public boolean collapsed() {
        return getKind() == Kind.CALDERA && landed();
    }

    @Override
    public boolean isOnFire() {
        return false;
    }
}
