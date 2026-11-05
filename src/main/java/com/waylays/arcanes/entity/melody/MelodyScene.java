package com.waylays.arcanes.entity.melody;

import com.waylays.arcanes.entity.SpellEffectEntity;
import com.waylays.arcanes.registry.ModEffects;
import com.waylays.arcanes.registry.ModEntities;
import com.waylays.arcanes.spells.SpellUtil;
import com.waylays.arcanes.spells.melody.MelodyFx;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.damage.DamageSources;
import net.minecraft.core.Holder;
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
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * What Music Magic leaves in the world: a note of gold in flight, a staff of light swept like a blade, a soothing melody
 * round its caster, a crescendo's rings of sound, a lullaby's clef over sleeping enemies, a war drum, a harp of light, a
 * row of phantom trumpets, the storm organ and the phantom orchestra. Each is a kind of this one entity: its renderer
 * draws the kind's model, and it does the kind's work as it ticks.
 */
public class MelodyScene extends SpellEffectEntity {
    public enum Kind { NOTE, STAFF, SOOTHE, CRESCENDO, LULLABY, DRUM, HARP, TRUMPETS, ORGAN, ORCHESTRA }

    /** Note: its speed, how long it flies, how far it turns toward an enemy each tick; a note's burst where it stops. */
    public static final double NOTE_SPEED = .9;
    public static final int NOTE_FLIGHT = 24, BURST = 10;
    public static final float NOTE_TURN = .16f;
    /** Staff slash: its reach, how wide its arc (radians), how long its sweep, when it strikes. */
    public static final float SLASH_REACH = 4.5f, SLASH_ARC = 2.6f;
    public static final int SLASH_SWEEP = 6, SLASH_HIT = 3;
    /** Soothing melody: how far it reaches, how often it heals. */
    public static final double SOOTHE_RADIUS = 6;
    public static final int SOOTHE_EVERY = 20;
    /** Crescendo: its speed, how long it travels, how wide its rings start and end. */
    public static final double CRESC_SPEED = .8;
    public static final int CRESC_FLIGHT = 18;
    public static final float CRESC_FROM = .7f, CRESC_TO = 3.2f;
    /** Lullaby: how long the clef takes to rise, how often it lulls. */
    public static final int LULL_RISE = 15, LULL_EVERY = 10;
    /** War drum: how long it takes to appear, its first beat, how often it beats, how far a beat carries. */
    public static final int DRUM_RISE = 14, DRUM_FIRST = 20, DRUM_EVERY = 16;
    public static final double DRUM_REACH = 6, DRUM_RALLY = 8;
    /** Harp: how long it takes to appear, how often a string is plucked, how far its light flies, how long a ray shows. */
    public static final int HARP_RISE = 15, HARP_EVERY = 8, HARP_RAY = 6;
    public static final double HARP_RANGE = 18;
    /** Trumpets: how many, when they first blow, how often, how many blasts, how far a blast carries and how wide. */
    public static final int TRUMPETS = 5, TRUMP_RISE = 16, TRUMP_FIRST = 20, TRUMP_EVERY = 20, TRUMP_BLASTS = 3;
    public static final double TRUMP_REACH = 14, TRUMP_COS = .7;
    /** Storm organ: how long it takes to rise, how often a pipe calls down sound, when the last chord sounds, a pillar's reach. */
    public static final int ORGAN_RISE = 30, ORGAN_EVERY = 8, ORGAN_FINALE = 160, ORGAN_FALL = 6;
    public static final double ORGAN_PILLAR = 2;
    /** Phantom orchestra: how long it takes to rise, how many violins, how often a note falls, when the finale sounds. */
    public static final int ORCH_RISE = 40, ORCH_VIOLINS = 8, ORCH_EVERY = 10, ORCH_FINALE = 220, ORCH_FALL = 8;
    public static final float ORCH_RING = 6;

    private static final EntityDataAccessor<Integer> DATA_KIND = SynchedEntityData.defineId(MelodyScene.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DATA_SIZE = SynchedEntityData.defineId(MelodyScene.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> DATA_LIFE = SynchedEntityData.defineId(MelodyScene.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Vector3f> DATA_DIR = SynchedEntityData.defineId(MelodyScene.class, EntityDataSerializers.VECTOR3);
    private static final EntityDataAccessor<Vector3f> DATA_TARGET = SynchedEntityData.defineId(MelodyScene.class, EntityDataSerializers.VECTOR3);
    private static final EntityDataAccessor<Integer> DATA_STRUCK = SynchedEntityData.defineId(MelodyScene.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_DONE = SynchedEntityData.defineId(MelodyScene.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> DATA_BOUND = SynchedEntityData.defineId(MelodyScene.class, EntityDataSerializers.INT);

    @Nullable
    private String spellId;
    private final Set<Integer> hit = new HashSet<>();
    /** Lullaby: who sleeps, and their health when they fell asleep (a blow wakes them). */
    private final Map<Integer, Float> asleep = new HashMap<>();
    private final Set<Integer> woken = new HashSet<>();
    private int strikes;
    /** Storm organ, orchestra: where each falling pillar or note will land, by the tick it lands on. */
    private final Map<Integer, Vec3> falling = new HashMap<>();

    public MelodyScene(EntityType<? extends MelodyScene> entityType, Level level) {
        super(entityType, level);
    }

    /** A scene of {@code kind} at {@code at}, facing {@code dir}, {@code size} (its radius or reach), lasting {@code life} ticks, striking for {@code damage}. */
    public static MelodyScene spawn(Level level, @Nullable LivingEntity caster, AbstractSpell spell, Kind kind, Vec3 at, Vector3f dir, float size, int life,
                                    float damage) {
        MelodyScene s = new MelodyScene(ModEntities.MELODY_SCENE.get(), level);
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

    /** Follows {@code e} (its feet), as a soothing melody or a harp follows its caster. */
    public MelodyScene follow(@Nullable Entity e) {
        this.entityData.set(DATA_BOUND, e == null ? -1 : e.getId());
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
        builder.define(DATA_DONE, false);
        builder.define(DATA_BOUND, -1);
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

    /** Where it last struck, from where it is. */
    public Vector3f getTarget() {
        return new Vector3f(this.entityData.get(DATA_TARGET));
    }

    public boolean isDone() {
        return this.entityData.get(DATA_DONE);
    }

    public float sinceStruck(float partialTicks) {
        return (int) level().getGameTime() + partialTicks - this.entityData.get(DATA_STRUCK);
    }

    public int strikes() {
        return strikes;
    }

    /** How many sleep under a lullaby now. */
    public int sleepers() {
        return asleep.size();
    }

    @Nullable
    public Entity bound() {
        int id = this.entityData.get(DATA_BOUND);
        return id >= 0 ? level().getEntity(id) : null;
    }

    /**
     * Where the {@code k}th of {@code n} trumpets stands, from the caster's feet: in an arc before them and above their
     * eyes, the middle one furthest ahead, so they are seen in first person as in third.
     */
    public static Vector3f trumpetAt(int k, Vector3f dir) {
        Vector3f d = flat(dir);
        Vector3f side = new Vector3f(d.z, 0, -d.x);
        float t = (k - (TRUMPETS - 1) * .5f) / ((TRUMPETS - 1) * .5f);
        float across = t * 2.6f;
        float up = 2.55f + .4f * (1 - t * t);
        float ahead = 1f + .4f * (1 - t * t);
        return new Vector3f(side).mul(across).add(new Vector3f(d).mul(ahead)).add(0, up, 0);
    }

    /** Where the {@code k}th violin of the orchestra stands, from its middle, {@code age} ticks after it was called. */
    public static Vector3f violinAt(int k, float age) {
        float a = k * Mth.TWO_PI / ORCH_VIOLINS + age * .012f;
        return new Vector3f(Mth.cos(a) * ORCH_RING, 1.6f + .25f * Mth.sin(age * .08f + k), Mth.sin(a) * ORCH_RING);
    }

    /** The harp's place beside its caster: at their right hand, a little ahead, seen at the edge of a first-person view. */
    public static Vector3f harpAt(Vector3f dir) {
        Vector3f d = flat(dir);
        return new Vector3f(d.z, 0, -d.x).mul(-1.6f).add(new Vector3f(d).mul(1.1f)).add(0, .3f, 0);
    }

    public static Vector3f flat(Vector3f dir) {
        Vector3f d = new Vector3f(dir.x, 0, dir.z);
        if (d.lengthSquared() < 1.0E-6f) {
            d.set(0, 0, 1);
        }
        return d.normalize();
    }

    @Nullable
    private AbstractSpell spell() {
        return spellId == null ? null : SpellRegistry.getSpell(spellId);
    }

    private boolean strike(AbstractSpell spell, @Nullable LivingEntity caster, LivingEntity e, float amount) {
        e.invulnerableTime = 0;
        boolean ok = DamageSources.applyDamage(e, amount, SpellUtil.damageSource(spell, this, caster));
        if (ok) {
            strikes++;
        }
        return ok;
    }

    private void struck(Vec3 at) {
        this.entityData.set(DATA_TARGET, at.subtract(position()).toVector3f());
        this.entityData.set(DATA_STRUCK, (int) level().getGameTime());
    }

    private void finish(Vec3 at, int linger) {
        struck(at);
        this.entityData.set(DATA_DONE, true);
        this.entityData.set(DATA_LIFE, tickCount + linger);
    }

    private void sound(SoundEvent sound, float volume, float pitch) {
        level().playSound(null, getX(), getY(), getZ(), sound, SoundSource.PLAYERS, volume, pitch);
    }

    private void sound(Holder<SoundEvent> sound, float volume, float pitch) {
        sound(sound.value(), volume, pitch);
    }

    private static void sound(Level level, Vec3 at, SoundEvent sound, float volume, float pitch) {
        level.playSound(null, at.x, at.y, at.z, sound, SoundSource.PLAYERS, volume, pitch);
    }

    /** A note's pitch: the {@code k}th of a major scale from {@code base}. */
    private static float scale(int k, float base) {
        int[] steps = {0, 2, 4, 5, 7, 9, 11, 12};
        int s = steps[Math.floorMod(k, steps.length)];
        return Mth.clamp(base * (float) Math.pow(2, s / 12.0), .5f, 2f);
    }

    private List<LivingEntity> enemies(ServerLevel level, Vec3 c, double radius, @Nullable LivingEntity caster, double height) {
        double y = c.y;
        return SpellUtil.enemiesAround(level, c, radius, caster, x -> Math.abs(x.getY() - y) < height);
    }

    private static List<LivingEntity> allies(ServerLevel level, Vec3 c, double radius, @Nullable LivingEntity caster) {
        List<LivingEntity> out = new ArrayList<>();
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(c, c).inflate(radius, 3, radius))) {
            if (e.isAlive() && e.position().subtract(c).horizontalDistanceSqr() <= radius * radius
                    && (caster == null ? e instanceof Player : e == caster || SpellUtil.isAlly(caster, e) || e.isAlliedTo(caster))) {
                out.add(e);
            }
        }
        return out;
    }

    private static Vec3 centre(LivingEntity e) {
        return e.position().add(0, e.getBbHeight() * .55, 0);
    }

    private Vec3 flatDir() {
        Vector3f d = flat(getDir());
        return new Vec3(d.x, 0, d.z);
    }

    /** Knocks {@code e} away from {@code from}, {@code power} along the ground and {@code up} up. */
    private static void knock(LivingEntity e, Vec3 from, double power, double up) {
        Vec3 away = new Vec3(e.getX() - from.x, 0, e.getZ() - from.z);
        away = away.lengthSqr() < 1.0E-4 ? new Vec3(1, 0, 0) : away.normalize();
        push(e, away.scale(power), up);
    }

    private static void push(LivingEntity e, Vec3 along, double up) {
        double k = Math.max(0, 1 - e.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.KNOCKBACK_RESISTANCE));
        e.setDeltaMovement(e.getDeltaMovement().add(along.scale(k)).add(0, up * k, 0));
        e.hurtMarked = true;
    }

    private boolean seen(ServerLevel level, Vec3 from, Vec3 to) {
        return level.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this)).getType() == HitResult.Type.MISS;
    }

    @Override
    public void tick() {
        Entity b = bound();
        if (b != null && b.isAlive()) {
            setPos(b.getX(), b.getY(), b.getZ());
        }
        super.tick();
        if (level().isClientSide) {
            return;
        }
        if (b instanceof LivingEntity follower && (getKind() == Kind.HARP || getKind() == Kind.TRUMPETS)) {
            // The harp stays over its caster's shoulder, the trumpets blow where the caster looks
            float yaw = follower.getYRot() * Mth.DEG_TO_RAD;
            this.entityData.set(DATA_DIR, new Vector3f(-Mth.sin(yaw), 0, Mth.cos(yaw)));
        }
        ServerLevel level = (ServerLevel) level();
        if (tickCount > getLife()) {
            wakeAll(level);
            discard();
            return;
        }
        if (this.entityData.get(DATA_BOUND) >= 0 && (b == null || !b.isAlive())) {
            wakeAll(level);
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
            case NOTE -> note(level, spell, caster);
            case STAFF -> staff(level, spell, caster);
            case SOOTHE -> soothe(level, spell, caster);
            case CRESCENDO -> crescendo(level, spell, caster);
            case LULLABY -> lullaby(level, spell, caster, size);
            case DRUM -> drum(level, spell, caster);
            case HARP -> harp(level, spell, caster);
            case TRUMPETS -> trumpets(level, spell, caster);
            case ORGAN -> organ(level, spell, caster, size);
            case ORCHESTRA -> orchestra(level, spell, caster, size);
        }
    }

    // ---------------------------------------------------------------- the kinds

    private void note(ServerLevel level, AbstractSpell spell, @Nullable LivingEntity caster) {
        if (isDone()) {
            return;
        }
        Vector3f d = getDir();
        Vec3 dir = new Vec3(d.x, d.y, d.z).normalize();
        // It bends toward the enemy most ahead of it
        LivingEntity best = null;
        double bestDot = .8;
        for (LivingEntity e : SpellUtil.enemiesAround(level, position(), 9, caster, x -> !hit.contains(x.getId()))) {
            Vec3 to = centre(e).subtract(position());
            double dot = to.normalize().dot(dir);
            if (dot > bestDot && seen(level, position(), centre(e))) {
                bestDot = dot;
                best = e;
            }
        }
        if (best != null) {
            Vec3 to = centre(best).subtract(position()).normalize();
            dir = dir.lerp(to, NOTE_TURN).normalize();
            this.entityData.set(DATA_DIR, dir.toVector3f());
        }
        Vec3 from = position(), to = from.add(dir.scale(NOTE_SPEED));
        BlockHitResult wall = level.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        Vec3 end = wall.getType() == HitResult.Type.MISS ? to : wall.getLocation();
        List<LivingEntity> line = new ArrayList<>();
        for (LivingEntity e : SpellUtil.enemiesAround(level, from.add(end).scale(.5), NOTE_SPEED + 2, caster, x -> !hit.contains(x.getId()))) {
            AABB box = e.getBoundingBox().inflate(.35);
            if (box.contains(from) || box.clip(from, end).isPresent()) {
                line.add(e);
            }
        }
        if (!line.isEmpty()) {
            line.sort(Comparator.comparingDouble(e -> e.distanceToSqr(from)));
            LivingEntity e = line.get(0);
            hit.add(e.getId());
            strike(spell, caster, e, damage);
            push(e, dir.scale(.25), .1);
            MelodyFx.notes(level, centre(e), 6, .4);
            sound(level, centre(e), SoundEvents.NOTE_BLOCK_BELL.value(), 1, scale(getId(), 1f));
            finish(centre(e), BURST);
            return;
        }
        if (wall.getType() != HitResult.Type.MISS || tickCount >= NOTE_FLIGHT) {
            MelodyFx.notes(level, end, 5, .3);
            sound(level, end, SoundEvents.NOTE_BLOCK_CHIME.value(), .7f, scale(getId(), 1f));
            finish(end, BURST);
            return;
        }
        setPos(to);
    }

    private void staff(ServerLevel level, AbstractSpell spell, @Nullable LivingEntity caster) {
        if (tickCount == 1) {
            sound(SoundEvents.PLAYER_ATTACK_SWEEP, 1, 1.4f);
            sound(SoundEvents.NOTE_BLOCK_HARP, 1, 1.5f);
        }
        if (tickCount != SLASH_HIT) {
            return;
        }
        Vec3 f = flatDir();
        Vec3 c = position();
        double half = SLASH_ARC * .5;
        for (LivingEntity e : enemies(level, c, SLASH_REACH + .5, caster, 2.5)) {
            Vec3 to = new Vec3(e.getX() - c.x, 0, e.getZ() - c.z);
            double dist = to.length();
            double angle = dist < 1.0E-3 ? 0 : Math.acos(Mth.clamp(to.scale(1 / dist).dot(f), -1, 1));
            if (angle <= half + .2 && dist <= SLASH_REACH + e.getBbWidth() * .5) {
                if (strike(spell, caster, e, damage)) {
                    knock(e, c, .55, .25);
                }
                MelodyFx.notes(level, centre(e), 4, .3);
                struck(centre(e));
            }
        }
        sound(SoundEvents.NOTE_BLOCK_BELL, .8f, 1.2f);
    }

    private void soothe(ServerLevel level, AbstractSpell spell, @Nullable LivingEntity caster) {
        if (tickCount == 1) {
            sound(SoundEvents.AMETHYST_BLOCK_CHIME, 1.2f, 1.1f);
        }
        if (tickCount % 5 == 0) {
            // A gentle melody: a note of the scale every few ticks
            sound(SoundEvents.NOTE_BLOCK_FLUTE, .5f, scale(tickCount / 5 * 3 % 8, .9f));
        }
        if (tickCount % SOOTHE_EVERY != 0) {
            return;
        }
        for (LivingEntity a : allies(level, position(), SOOTHE_RADIUS, caster)) {
            a.heal(damage);
            a.removeEffect(MobEffects.POISON);
            a.removeEffect(MobEffects.WITHER);
            MelodyFx.hearts(level, centre(a), 3, .4);
        }
        MelodyFx.notes(level, position().add(0, 1.2, 0), 6, 1.2);
    }

    private void crescendo(ServerLevel level, AbstractSpell spell, @Nullable LivingEntity caster) {
        if (isDone()) {
            return;
        }
        float t = Mth.clamp(tickCount / (float) CRESC_FLIGHT, 0, 1);
        float radius = Mth.lerp(t, CRESC_FROM, CRESC_TO);
        if (tickCount % 3 == 1) {
            sound(SoundEvents.NOTE_BLOCK_BASS, .6f + t * 1.4f, .6f + t * .8f);
        }
        Vector3f d = getDir();
        Vec3 dir = new Vec3(d.x, d.y, d.z).normalize();
        Vec3 from = position(), to = from.add(dir.scale(CRESC_SPEED));
        BlockHitResult wall = level.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        Vec3 end = wall.getType() == HitResult.Type.MISS ? to : wall.getLocation();
        for (LivingEntity e : SpellUtil.enemiesAround(level, end, radius + 1.5, caster, x -> !hit.contains(x.getId()))) {
            // Within the ring's disc, as it sweeps by
            Vec3 rel = centre(e).subtract(end);
            double along = rel.dot(dir);
            double across = rel.subtract(dir.scale(along)).length();
            if (Math.abs(along) < CRESC_SPEED + .6 && across < radius + e.getBbWidth() * .5) {
                hit.add(e.getId());
                if (strike(spell, caster, e, damage * (.6f + .8f * t))) {
                    push(e, dir.scale(.4 + .9 * t), .2 + .2 * t);
                }
                MelodyFx.notes(level, centre(e), 5, .4);
                struck(centre(e));
            }
        }
        if (wall.getType() != HitResult.Type.MISS || tickCount >= CRESC_FLIGHT) {
            MelodyFx.notes(level, end, 14, radius * .5);
            sound(level, end, SoundEvents.NOTE_BLOCK_BELL.value(), 1.6f, 1.6f);
            finish(end, BURST);
            return;
        }
        setPos(to);
    }

    private void lullaby(ServerLevel level, AbstractSpell spell, @Nullable LivingEntity caster, float radius) {
        if (tickCount == 1) {
            sound(SoundEvents.AMETHYST_BLOCK_RESONATE, 1.4f, .7f);
        }
        if (tickCount % 6 == 0) {
            sound(SoundEvents.NOTE_BLOCK_CHIME, .6f, scale(7 - tickCount / 6 % 8, .7f));
        }
        if (tickCount < LULL_RISE) {
            return;
        }
        // The sleepers stay asleep, unless a blow wakes them or they leave the song's reach
        for (var it = asleep.entrySet().iterator(); it.hasNext(); ) {
            var entry = it.next();
            Entity x = level.getEntity(entry.getKey());
            if (!(x instanceof LivingEntity e) || !e.isAlive() || e.getHealth() < entry.getValue() - .01f
                    || e.position().subtract(position()).horizontalDistanceSqr() > (radius + 2) * (radius + 2)) {
                if (x instanceof LivingEntity e) {
                    wake(e);
                }
                woken.add(entry.getKey());
                it.remove();
                continue;
            }
            hush(e);
        }
        if ((tickCount - LULL_RISE) % LULL_EVERY == 0) {
            for (LivingEntity e : enemies(level, position().add(0, 1, 0), radius, caster, 3)) {
                if (asleep.containsKey(e.getId()) || woken.contains(e.getId())) {
                    continue;
                }
                asleep.put(e.getId(), e.getHealth());
                hush(e);
                MelodyFx.sleep(level, centre(e).add(0, e.getBbHeight() * .5, 0), 4);
                struck(centre(e));
            }
        }
    }

    /** Keeps a sleeper asleep: still, its weapon lowered, its eyes closed (for a player, darkness). */
    private void hush(LivingEntity e) {
        e.addEffect(new MobEffectInstance(ModEffects.ENTANGLED, 15, 0, false, false, true));
        e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 15, 4, false, false, true));
        if (e instanceof Player) {
            e.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 30, 0, false, false, true));
        } else if (e instanceof Mob m) {
            m.setTarget(null);
            m.getNavigation().stop();
            m.setDeltaMovement(0, Math.min(0, m.getDeltaMovement().y), 0);
        }
        if (tickCount % 20 == 0) {
            MelodyFx.sleep((ServerLevel) level(), e.position().add(0, e.getBbHeight() + .3, 0), 1);
        }
    }

    private static void wake(LivingEntity e) {
        e.removeEffect(ModEffects.ENTANGLED);
        e.removeEffect(MobEffects.WEAKNESS);
        e.removeEffect(MobEffects.DARKNESS);
    }

    private void wakeAll(ServerLevel level) {
        for (int id : asleep.keySet()) {
            if (level.getEntity(id) instanceof LivingEntity e) {
                wake(e);
            }
        }
        asleep.clear();
    }

    private void drum(ServerLevel level, AbstractSpell spell, @Nullable LivingEntity caster) {
        if (tickCount == 1) {
            sound(SoundEvents.NOTE_BLOCK_BASEDRUM, 1.4f, .5f);
        }
        if (tickCount < DRUM_FIRST || tickCount >= getLife() - 10 || (tickCount - DRUM_FIRST) % DRUM_EVERY != 0) {
            return;
        }
        Vec3 c = position();
        for (LivingEntity e : enemies(level, c.add(0, .5, 0), DRUM_REACH, caster, 3)) {
            if (strike(spell, caster, e, damage)) {
                knock(e, c, .7, .3);
            }
        }
        for (LivingEntity a : allies(level, c, DRUM_RALLY, caster)) {
            a.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, DRUM_EVERY + 25, 0, false, true, true));
            a.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, DRUM_EVERY + 25, 0, false, true, true));
        }
        struck(c);
        SpellUtil.cameraShake(level, c, 4, 10);
        MelodyFx.shock(level, c.add(0, .2, 0), (float) DRUM_REACH);
        sound(SoundEvents.NOTE_BLOCK_BASEDRUM, 2.5f, .5f);
        sound(SoundEvents.GENERIC_EXPLODE, .4f, 1.8f);
        sound(SoundEvents.WARDEN_HEARTBEAT, 1.5f, .8f);
    }

    private void harp(ServerLevel level, AbstractSpell spell, @Nullable LivingEntity caster) {
        if (tickCount == 1) {
            sound(SoundEvents.AMETHYST_BLOCK_RESONATE, 1, 1.4f);
        }
        if (tickCount < HARP_RISE || tickCount >= getLife() - 10 || (tickCount - HARP_RISE) % HARP_EVERY != 0) {
            return;
        }
        Vector3f h = harpAt(getDir());
        Vec3 from = position().add(h.x, h.y + 1.6, h.z);
        List<LivingEntity> foes = SpellUtil.enemiesAround(level, from, HARP_RANGE, caster, x -> true);
        foes.sort(Comparator.comparingDouble(e -> e.distanceToSqr(from)));
        for (LivingEntity e : foes) {
            Vec3 at = centre(e);
            if (!seen(level, from, at)) {
                continue;
            }
            strike(spell, caster, e, damage);
            struck(at);
            MelodyFx.notes(level, at, 5, .3);
            int k = (tickCount - HARP_RISE) / HARP_EVERY;
            sound(SoundEvents.NOTE_BLOCK_HARP, 1.2f, scale(k, .8f));
            sound(level, at, SoundEvents.AMETHYST_CLUSTER_HIT, .8f, 1.6f);
            return;
        }
    }

    private void trumpets(ServerLevel level, AbstractSpell spell, @Nullable LivingEntity caster) {
        if (tickCount == 1) {
            sound(SoundEvents.NOTE_BLOCK_DIDGERIDOO, 1.2f, 1.5f);
        }
        int since = tickCount - TRUMP_FIRST;
        if (since < 0 || since % TRUMP_EVERY != 0 || since / TRUMP_EVERY >= TRUMP_BLASTS) {
            return;
        }
        Vec3 f = flatDir();
        Vec3 c = position().add(0, 1, 0);
        for (LivingEntity e : enemies(level, c, TRUMP_REACH, caster, 5)) {
            Vec3 to = centre(e).subtract(c);
            Vec3 flatTo = new Vec3(to.x, 0, to.z);
            double dist = flatTo.length();
            if (dist < 1.0E-3 || flatTo.scale(1 / dist).dot(f) < TRUMP_COS) {
                continue;
            }
            if (strike(spell, caster, e, damage)) {
                push(e, f.scale(1.1 * (1 - dist / TRUMP_REACH * .5)), .35);
            }
            MelodyFx.notes(level, centre(e), 4, .3);
            struck(centre(e));
        }
        MelodyFx.blast(level, c.add(f.scale(3)), f, 12);
        SpellUtil.cameraShake(level, c, 6, 12);
        // A fanfare: each blast a note higher
        int blast = since / TRUMP_EVERY;
        sound(SoundEvents.RAID_HORN, 1.6f, 1.4f + blast * .15f);
        sound(SoundEvents.NOTE_BLOCK_DIDGERIDOO, 2f, 1.2f + blast * .2f);
        sound(SoundEvents.NOTE_BLOCK_BIT, 1.5f, scale(blast * 2, 1f));
    }

    private void organ(ServerLevel level, AbstractSpell spell, @Nullable LivingEntity caster, float radius) {
        Vec3 c = position();
        if (tickCount == 1) {
            sound(SoundEvents.BEACON_ACTIVATE, 2f, .5f);
            sound(SoundEvents.NOTE_BLOCK_BASS, 2f, .5f);
        }
        if (tickCount < ORGAN_RISE) {
            if (tickCount % 6 == 0) {
                sound(SoundEvents.NOTE_BLOCK_BASS, 1.5f, .5f + tickCount * .02f);
            }
            return;
        }
        // Pillars of sound fall where the pipes called them
        Vec3 landing = falling.remove(tickCount);
        if (landing != null) {
            for (LivingEntity e : enemies(level, landing.add(0, 1, 0), ORGAN_PILLAR, caster, 3)) {
                if (strike(spell, caster, e, damage)) {
                    knock(e, landing, .3, .5);
                }
            }
            MelodyFx.pillar(level, landing);
            sound(level, landing, SoundEvents.NOTE_BLOCK_BELL.value(), 1.6f, .6f);
            sound(level, landing, SoundEvents.LIGHTNING_BOLT_IMPACT, .6f, 1.6f);
        }
        if (tickCount < ORGAN_FINALE) {
            if ((tickCount - ORGAN_RISE) % ORGAN_EVERY == 0) {
                List<LivingEntity> foes = enemies(level, c.add(0, 1, 0), radius, caster, 8);
                Vec3 at;
                if (!foes.isEmpty()) {
                    at = foes.get(level.random.nextInt(foes.size())).position();
                } else {
                    float a = level.random.nextFloat() * Mth.TWO_PI, r = radius * (.3f + .6f * level.random.nextFloat());
                    at = c.add(Mth.cos(a) * r, 0, Mth.sin(a) * r);
                }
                falling.put(tickCount + ORGAN_FALL, at);
                struck(at);
                int k = (tickCount - ORGAN_RISE) / ORGAN_EVERY;
                sound(SoundEvents.NOTE_BLOCK_BASS, 2.2f, scale(k * 5, .5f));
                sound(SoundEvents.NOTE_BLOCK_FLUTE, 1.6f, scale(k * 5 + 2, .5f));
            }
            return;
        }
        if (tickCount == ORGAN_FINALE) {
            for (LivingEntity e : enemies(level, c.add(0, 1, 0), radius, caster, 8)) {
                if (strike(spell, caster, e, damage * 1.5f)) {
                    knock(e, c, 1.2, .7);
                }
            }
            MelodyFx.notes(level, c.add(0, 6, 0), 60, radius * .5);
            MelodyFx.shock(level, c.add(0, .2, 0), radius);
            SpellUtil.cameraShake(level, c, 20, radius * 2);
            sound(SoundEvents.NOTE_BLOCK_BASS, 3f, .5f);
            sound(SoundEvents.NOTE_BLOCK_FLUTE, 3f, .6f);
            sound(SoundEvents.NOTE_BLOCK_BELL, 3f, .75f);
            sound(SoundEvents.BELL_RESONATE, 2.5f, .7f);
            sound(SoundEvents.LIGHTNING_BOLT_THUNDER, 1.5f, 1.2f);
            this.entityData.set(DATA_DONE, true);
        }
    }

    private void orchestra(ServerLevel level, AbstractSpell spell, @Nullable LivingEntity caster, float radius) {
        Vec3 c = position();
        if (tickCount == 1) {
            sound(SoundEvents.BEACON_ACTIVATE, 2f, .8f);
            sound(SoundEvents.AMETHYST_BLOCK_RESONATE, 2f, .5f);
        }
        if (tickCount < ORCH_RISE) {
            if (tickCount % 5 == 0) {
                sound(SoundEvents.NOTE_BLOCK_HARP, 1.5f, scale(tickCount / 5, .5f));
            }
            return;
        }
        Vec3 landing = falling.remove(tickCount);
        if (landing != null) {
            for (LivingEntity e : enemies(level, landing.add(0, 1, 0), 1.6, caster, 3)) {
                strike(spell, caster, e, damage * .4f);
            }
            MelodyFx.notes(level, landing.add(0, .5, 0), 6, .5);
            sound(level, landing, SoundEvents.NOTE_BLOCK_BELL.value(), 1.4f, scale(tickCount, 1f));
        }
        if (tickCount < ORCH_FINALE) {
            if (tickCount % 4 == 0) {
                // The violins play their tune, the cellos under them
                int k = (tickCount - ORCH_RISE) / 4;
                int[] tune = {0, 2, 4, 7, 4, 2, 0, 4, 5, 7, 9, 7, 5, 4, 2, 0};
                sound(SoundEvents.NOTE_BLOCK_FLUTE, 1.4f, scale(tune[k % tune.length], 1f));
                if (k % 4 == 0) {
                    sound(SoundEvents.NOTE_BLOCK_BASS, 1.6f, scale(tune[k % tune.length], .5f));
                }
            }
            if (tickCount % 20 == 0) {
                for (LivingEntity a : allies(level, c, radius, caster)) {
                    a.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 45, 1, false, true, true));
                    a.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 45, 1, false, true, true));
                    a.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 45, 0, false, true, true));
                }
                for (LivingEntity e : enemies(level, c.add(0, 1, 0), radius, caster, 8)) {
                    e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 45, 1, false, false, true));
                }
            }
            if ((tickCount - ORCH_RISE) % ORCH_EVERY == 0) {
                List<LivingEntity> foes = enemies(level, c.add(0, 1, 0), radius, caster, 8);
                if (!foes.isEmpty()) {
                    Vec3 at = foes.get(level.random.nextInt(foes.size())).position();
                    falling.put(tickCount + ORCH_FALL, at);
                    struck(at);
                }
            }
            return;
        }
        if (tickCount == ORCH_FINALE) {
            for (LivingEntity e : enemies(level, c.add(0, 1, 0), radius, caster, 8)) {
                if (strike(spell, caster, e, damage)) {
                    knock(e, c, 1.4, .8);
                }
            }
            for (LivingEntity a : allies(level, c, radius, caster)) {
                a.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 300, 2, false, true, true));
            }
            MelodyFx.notes(level, c.add(0, 4, 0), 80, radius * .5);
            MelodyFx.shock(level, c.add(0, .2, 0), radius);
            SpellUtil.cameraShake(level, c, 25, radius * 2);
            sound(SoundEvents.NOTE_BLOCK_BELL, 3f, .5f);
            sound(SoundEvents.NOTE_BLOCK_BELL, 3f, .63f);
            sound(SoundEvents.NOTE_BLOCK_BELL, 3f, .75f);
            sound(SoundEvents.NOTE_BLOCK_BASS, 3f, .5f);
            sound(SoundEvents.RAID_HORN, 2f, 1.2f);
            sound(SoundEvents.BELL_RESONATE, 2.5f, 1f);
            this.entityData.set(DATA_DONE, true);
        }
    }

    @Override
    public void remove(RemovalReason reason) {
        if (!level().isClientSide) {
            wakeAll((ServerLevel) level());
        }
        super.remove(reason);
    }
}
