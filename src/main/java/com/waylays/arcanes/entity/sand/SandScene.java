package com.waylays.arcanes.entity.sand;

import com.waylays.arcanes.entity.SpellEffectEntity;
import com.waylays.arcanes.registry.ModEffects;
import com.waylays.arcanes.registry.ModEntities;
import com.waylays.arcanes.spells.SpellUtil;
import com.waylays.arcanes.spells.sand.SandFx;
import com.waylays.arcanes.spells.wind.WindFx;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.damage.DamageSources;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
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
import org.joml.Vector3f;

import javax.annotation.Nullable;

/**
 * What Sand Magic leaves in the world after its cast: a gout of sand, quicksand, a sandstone pillar, a sandstorm round
 * its caster, a dune wave rolling on, a sandworm's leap, a sarcophagus of linen round its victim, an obelisk, the
 * pyramid. Each is a kind of this one entity: its renderer draws the kind's model, and it does the kind's work as it
 * ticks.
 */
public class SandScene extends SpellEffectEntity {
    public enum Kind { BLAST, QUICKSAND, PILLAR, STORM, WAVE, WORM, SARCOPHAGUS, OBELISK, PYRAMID }

    /** A dune wave rolls on this fast (blocks a tick), its wall this tall. */
    public static final double WAVE_SPEED = .45;
    public static final float WAVE_HEIGHT = 2.8f;
    /** The sandworm bursts out this long after the ground starts trembling, and its leap takes this long (see its model). */
    public static final int WORM_BURST = 20, WORM_LEAP = 30;
    /** An obelisk looses a beam at the nearest foe this near, every {@link #OBELISK_EVERY} ticks. */
    public static final double OBELISK_RANGE = 14;
    public static final int OBELISK_EVERY = 30;
    /** The pyramid rises for this long, holds for {@link #PYRAMID_HOLD}, then falls in. */
    public static final int PYRAMID_RISE = 30, PYRAMID_HOLD = 60;
    public static final float PYRAMID_HEIGHT = 8;

    private static final EntityDataAccessor<Integer> DATA_KIND = SynchedEntityData.defineId(SandScene.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DATA_SIZE = SynchedEntityData.defineId(SandScene.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> DATA_LIFE = SynchedEntityData.defineId(SandScene.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Vector3f> DATA_DIR = SynchedEntityData.defineId(SandScene.class, EntityDataSerializers.VECTOR3);
    /** Where an obelisk last struck (relative to it) and when (game tick). */
    private static final EntityDataAccessor<Vector3f> DATA_TARGET = SynchedEntityData.defineId(SandScene.class, EntityDataSerializers.VECTOR3);
    private static final EntityDataAccessor<Integer> DATA_STRUCK = SynchedEntityData.defineId(SandScene.class, EntityDataSerializers.INT);

    @Nullable
    private String spellId;
    /** What a sarcophagus binds, or what follows (a sandstorm follows its caster). */
    private int bound = -1;
    private final IntOpenHashSet hit = new IntOpenHashSet();

    public SandScene(EntityType<? extends SandScene> entityType, Level level) {
        super(entityType, level);
    }

    /** A scene of {@code kind} at {@code at}, facing {@code dir}, {@code size} (its length, radius or girth), lasting {@code life} ticks, striking for {@code damage}. */
    public static SandScene spawn(Level level, @Nullable LivingEntity caster, AbstractSpell spell, Kind kind, Vec3 at, Vector3f dir, float size, int life,
                                  float damage) {
        SandScene s = new SandScene(ModEntities.SAND_SCENE.get(), level);
        s.setOwner(caster);
        s.spellId = spell.getSpellId();
        s.damage = damage;
        s.entityData.set(DATA_KIND, kind.ordinal());
        s.entityData.set(DATA_SIZE, size);
        s.entityData.set(DATA_LIFE, life);
        s.entityData.set(DATA_DIR, dir);
        s.entityData.set(DATA_STRUCK, -1000);
        s.moveTo(at.x, at.y, at.z, 0, 0);
        level.addFreshEntity(s);
        return s;
    }

    /** Makes it follow {@code e} (a sarcophagus round its victim, a sandstorm round its caster). */
    public SandScene follow(Entity e) {
        this.bound = e.getId();
        return this;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_KIND, 0);
        builder.define(DATA_SIZE, 1f);
        builder.define(DATA_LIFE, 20);
        builder.define(DATA_DIR, new Vector3f(0, 0, 1));
        builder.define(DATA_TARGET, new Vector3f());
        builder.define(DATA_STRUCK, -1000);
    }

    public Kind getKind() {
        Kind[] all = Kind.values();
        return all[Mth.clamp(this.entityData.get(DATA_KIND), 0, all.length - 1)];
    }

    public float getSize() {
        return this.entityData.get(DATA_SIZE);
    }

    public int getLife() {
        return this.entityData.get(DATA_LIFE);
    }

    public Vector3f getDir() {
        return new Vector3f(this.entityData.get(DATA_DIR));
    }

    public Vector3f getTarget() {
        return new Vector3f(this.entityData.get(DATA_TARGET));
    }

    public float sinceStruck(float partialTicks) {
        return (int) level().getGameTime() + partialTicks - this.entityData.get(DATA_STRUCK);
    }

    @Nullable
    private AbstractSpell spell() {
        return spellId == null ? null : SpellRegistry.getSpell(spellId);
    }

    private boolean strike(AbstractSpell spell, @Nullable LivingEntity caster, LivingEntity e, float amount) {
        e.invulnerableTime = 0;
        return DamageSources.applyDamage(e, amount, SpellUtil.damageSource(spell, this, caster));
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            return;
        }
        ServerLevel level = (ServerLevel) level();
        if (tickCount > getLife()) {
            discard();
            return;
        }
        AbstractSpell spell = spell();
        LivingEntity caster = getLivingOwner();
        if (spell == null) {
            return;
        }
        float size = getSize();
        switch (getKind()) {
            case QUICKSAND -> {
                if (tickCount % 5 == 0 && tickCount < getLife() - 10) {
                    for (LivingEntity e : SpellUtil.enemiesAround(level, position(), size, caster, x -> Math.abs(x.getY() - getY()) < 1.5)) {
                        e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 12, 2, false, false, true));
                        Vec3 in = position().subtract(e.position()).multiply(1, 0, 1);
                        Vec3 v = e.getDeltaMovement();
                        // Drawn in, and held down: no jumping out of it
                        e.setDeltaMovement(in.lengthSqr() > .04 ? v.add(in.normalize().scale(.06)) : v);
                        if (e.getDeltaMovement().y > 0) {
                            e.setDeltaMovement(e.getDeltaMovement().multiply(1, 0, 1));
                        }
                        e.hurtMarked = true;
                        if (tickCount % 20 == 0) {
                            strike(spell, caster, e, damage);
                        }
                    }
                    if (tickCount % 20 == 0) {
                        level.playSound(null, getX(), getY(), getZ(), SoundEvents.SAND_BREAK, SoundSource.PLAYERS, 1.2f, .6f);
                    }
                }
            }
            case PILLAR -> {
                if (tickCount == 2) {
                    for (LivingEntity e : SpellUtil.enemiesAround(level, position().add(0, .5, 0), 1.6, caster, x -> true)) {
                        if (strike(spell, caster, e, damage)) {
                            e.setDeltaMovement(e.getDeltaMovement().multiply(.3, 0, .3).add(0, 1.1, 0));
                            e.hurtMarked = true;
                        }
                    }
                    SandFx.sand(level, position().add(0, .5, 0), 30, .8);
                    level.playSound(null, getX(), getY(), getZ(), SoundEvents.STONE_BREAK, SoundSource.PLAYERS, 1.6f, .6f);
                }
            }
            case STORM -> {
                Entity follow = bound >= 0 ? level.getEntity(bound) : null;
                if (follow != null && follow.isAlive()) {
                    setPos(follow.getX(), follow.getY(), follow.getZ());
                }
                if (tickCount % 10 == 0 && tickCount < getLife() - 12) {
                    for (LivingEntity e : SpellUtil.enemiesAround(level, position().add(0, 1, 0), size, caster, x -> true)) {
                        if (strike(spell, caster, e, damage)) {
                            e.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 30, 0, false, false, true));
                        }
                    }
                    SandFx.sand(level, position().add(0, 1.5, 0), 12, size * .5);
                }
                if (tickCount % 20 == 0) {
                    level.playSound(null, getX(), getY(), getZ(), SoundEvents.SAND_STEP, SoundSource.PLAYERS, 2f, .5f);
                }
                // Shots are blown off course in it
                WindFx.deflect(level, getBoundingBox().inflate(size, 2.5, size), position(), caster, .4);
            }
            case WAVE -> wave(level, spell, caster, size);
            case WORM -> {
                Vector3f d = getDir();
                Vec3 fwd = new Vec3(d.x, 0, d.z).normalize();
                if (tickCount == WORM_BURST) {
                    for (LivingEntity e : SpellUtil.enemiesAround(level, position().add(0, 1, 0), size * 2.6, caster, x -> true)) {
                        if (strike(spell, caster, e, damage)) {
                            e.setDeltaMovement(e.getDeltaMovement().add(fwd.scale(.6)).add(0, 1.2, 0));
                            e.hurtMarked = true;
                        }
                    }
                    SandFx.sand(level, position().add(0, 1, 0), 60, size * 1.2);
                    level.playSound(null, getX(), getY(), getZ(), SoundEvents.RAVAGER_ROAR, SoundSource.PLAYERS, 2.5f, .5f);
                    level.playSound(null, getX(), getY(), getZ(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 1.2f, .6f);
                    SpellUtil.cameraShake(level, position(), 16, size * 8);
                } else if (tickCount == WORM_BURST + (int) (WORM_LEAP * .9f)) {
                    Vec3 dive = position().add(fwd.scale(size * 7));
                    for (LivingEntity e : SpellUtil.enemiesAround(level, dive.add(0, 1, 0), size * 2.2, caster, x -> true)) {
                        if (strike(spell, caster, e, damage * .6f)) {
                            e.setDeltaMovement(e.getDeltaMovement().add(0, .7, 0));
                            e.hurtMarked = true;
                        }
                    }
                    SandFx.sand(level, dive.add(0, 1, 0), 40, size);
                    level.playSound(null, dive.x, dive.y, dive.z, SoundEvents.SAND_BREAK, SoundSource.PLAYERS, 2.5f, .5f);
                } else if (tickCount < WORM_BURST && tickCount % 5 == 0) {
                    SandFx.sand(level, position().add(0, .2, 0), 8, size * 1.2);
                    level.playSound(null, getX(), getY(), getZ(), SoundEvents.SAND_HIT, SoundSource.PLAYERS, 1.5f, .5f);
                }
            }
            case SARCOPHAGUS -> {
                Entity follow = bound >= 0 ? level.getEntity(bound) : null;
                if (!(follow instanceof LivingEntity victim) || !victim.isAlive()) {
                    discard();
                    return;
                }
                setPos(victim.getX(), victim.getY(), victim.getZ());
                victim.addEffect(new MobEffectInstance(ModEffects.ENTANGLED, 5, 0, false, false, true));
                if (tickCount % 20 == 0 && tickCount < getLife() - 10) {
                    strike(spell, caster, victim, damage);
                }
                if (tickCount == getLife() - 10) {
                    // The linen tears loose in a burst of sand
                    strike(spell, caster, victim, damage * 2);
                    SandFx.sand(level, victim.position().add(0, 1, 0), 30, .6);
                    level.playSound(null, getX(), getY(), getZ(), SoundEvents.WOOL_BREAK, SoundSource.PLAYERS, 1.6f, .6f);
                }
            }
            case OBELISK -> {
                if (tickCount > 14 && tickCount % OBELISK_EVERY == 0 && tickCount < getLife() - 14) {
                    LivingEntity foe = null;
                    double best = Double.MAX_VALUE;
                    for (LivingEntity e : SpellUtil.enemiesAround(level, position(), OBELISK_RANGE, caster, x -> true)) {
                        double dd = e.distanceToSqr(this);
                        if (dd < best) {
                            best = dd;
                            foe = e;
                        }
                    }
                    if (foe != null && strike(spell, caster, foe, damage)) {
                        foe.igniteForSeconds(3);
                        Vec3 rel = foe.position().add(0, foe.getBbHeight() * .5, 0).subtract(position());
                        this.entityData.set(DATA_TARGET, rel.toVector3f());
                        this.entityData.set(DATA_STRUCK, (int) level.getGameTime());
                        level.playSound(null, getX(), getY() + size, getZ(), SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 1.4f, 1.6f);
                    }
                }
            }
            case PYRAMID -> pyramid(level, spell, caster, size);
            default -> {
            }
        }
    }

    private void wave(ServerLevel level, AbstractSpell spell, @Nullable LivingEntity caster, float width) {
        Vector3f d = getDir();
        Vec3 fwd = new Vec3(d.x, 0, d.z).normalize();
        Vec3 side = new Vec3(-fwd.z, 0, fwd.x);
        Vec3 next = position().add(fwd.scale(WAVE_SPEED));
        // It keeps to the ground, climbing a block or dropping as the ground does
        Vec3 ground = SpellUtil.groundAt(level, next, 2, 1);
        setPos(next.x, ground.y, next.z);
        for (LivingEntity e : SpellUtil.enemiesAround(level, position().add(0, 1, 0), width * .5 + 1.5, caster, x -> true)) {
            Vec3 rel = e.position().subtract(position());
            double along = rel.dot(fwd), across = Math.abs(rel.dot(side));
            if (along < -1.8 || along > .8 || across > width * .5 || rel.y < -1 || rel.y > WAVE_HEIGHT) {
                continue;
            }
            if (hit.add(e.getId())) {
                strike(spell, caster, e, damage);
            }
            // Carried along with it
            Vec3 v = e.getDeltaMovement();
            e.setDeltaMovement(fwd.scale(WAVE_SPEED * 1.6).add(0, Math.max(v.y, .05), 0));
            e.hurtMarked = true;
        }
        if (tickCount % 4 == 0) {
            SandFx.sand(level, position().add(0, 1, 0), 10, width * .3);
        }
        if (tickCount % 8 == 0) {
            level.playSound(null, getX(), getY(), getZ(), SoundEvents.SAND_BREAK, SoundSource.PLAYERS, 1.6f, .5f);
        }
    }

    private void pyramid(ServerLevel level, AbstractSpell spell, @Nullable LivingEntity caster, float half) {
        int fall = PYRAMID_RISE + PYRAMID_HOLD;
        if (tickCount < fall) {
            for (LivingEntity e : SpellUtil.enemiesAround(level, position().add(0, 1, 0), half * 1.5, caster, x -> Math.abs(x.getY() - getY()) < PYRAMID_HEIGHT)) {
                Vec3 rel = e.position().subtract(position());
                boolean inside = Math.abs(rel.x) < half && Math.abs(rel.z) < half;
                if (!inside) {
                    continue;
                }
                if (tickCount > PYRAMID_RISE * .5f) {
                    e.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 25, 0, false, false, true));
                    e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 25, 1, false, false, true));
                    // Walled in: whoever reaches its sides is turned back in
                    if (Math.abs(rel.x) > half * .8 || Math.abs(rel.z) > half * .8) {
                        Vec3 in = rel.multiply(-1, 0, -1).normalize().scale(.25);
                        e.setDeltaMovement(in.add(0, e.getDeltaMovement().y, 0));
                        e.hurtMarked = true;
                    }
                }
                if (tickCount % 20 == 0 && tickCount > PYRAMID_RISE) {
                    strike(spell, caster, e, damage * .15f);
                }
            }
            if (tickCount < PYRAMID_RISE && tickCount % 4 == 0) {
                SandFx.sand(level, position().add(0, 1, 0), 20, half);
                SpellUtil.cameraShake(level, position(), 4, half * 3);
            }
            if (tickCount == 1) {
                level.playSound(null, getX(), getY(), getZ(), SoundEvents.STONE_PLACE, SoundSource.PLAYERS, 3f, .4f);
            }
        } else if (tickCount == fall) {
            for (LivingEntity e : SpellUtil.enemiesAround(level, position().add(0, 1, 0), half * 1.5, caster, x -> Math.abs(x.getY() - getY()) < PYRAMID_HEIGHT)) {
                Vec3 rel = e.position().subtract(position());
                if (Math.abs(rel.x) < half + .5 && Math.abs(rel.z) < half + .5 && strike(spell, caster, e, damage)) {
                    e.setDeltaMovement(e.getDeltaMovement().add(0, .6, 0));
                    e.hurtMarked = true;
                }
            }
            SandFx.sand(level, position().add(0, 2, 0), 120, half);
            level.playSound(null, getX(), getY(), getZ(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 2.5f, .5f);
            level.playSound(null, getX(), getY(), getZ(), SoundEvents.STONE_BREAK, SoundSource.PLAYERS, 3f, .4f);
            SpellUtil.cameraShake(level, position(), 30, half * 5);
        }
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 128 * 128;
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (spellId != null) {
            tag.putString("Spell", spellId);
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        spellId = tag.contains("Spell") ? tag.getString("Spell") : null;
    }
}
