package com.waylays.arcanes.entity.lotus;

import com.waylays.arcanes.entity.SpellEffectEntity;
import com.waylays.arcanes.registry.ModEffects;
import com.waylays.arcanes.registry.ModEntities;
import com.waylays.arcanes.spells.SpellUtil;
import com.waylays.arcanes.spells.lotus.LotusFx;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.damage.DamageSources;
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
import java.util.List;

/**
 * What Lotus Magic leaves in the world: a razor petal in flight, a bud closing round its prey, the sacred pond, the
 * throne of the lotus, the roots in the mire, the seed pod, the mandala, a lantern, the storm of petals, the
 * Thousand-Petal Lotus. Each is a kind of this one entity: its renderer draws the kind's model, and it does the kind's
 * work as it ticks.
 */
public class LotusScene extends SpellEffectEntity {
    public enum Kind { PETAL, BUD, POND, THRONE, ROOTS, POD, MANDALA, LANTERN, STORM, THOUSAND }

    /** A razor petal flies this fast (blocks a tick) for this long before it falls apart; it bursts this long after it stops. */
    public static final double PETAL_SPEED = 1.1;
    public static final int PETAL_FLIGHT = 18, PETAL_BURST = 10;
    /** The bud has risen round its prey after this long, and bursts open at this tick. */
    public static final int BUD_RISE = 8, BUD_BURST = 18;
    /** The pond heals and slows every {@link #POND_EVERY} ticks. */
    public static final int POND_EVERY = 20;
    /** The throne looses a petal at the nearest enemy this far off, every {@link #THRONE_EVERY} ticks. */
    public static final double THRONE_RANGE = 10;
    public static final int THRONE_EVERY = 30;
    /** The roots seize first at this tick, then every {@link #ROOTS_EVERY} ticks. */
    public static final int ROOTS_FIRST = 12, ROOTS_EVERY = 30;
    /** The pod shoots a seed at the nearest enemy this far off, every {@link #POD_EVERY} ticks; it floats this high. */
    public static final double POD_RANGE = 16;
    public static final int POD_EVERY = 20;
    public static final float POD_HEIGHT = 2.2f;
    /** The mandala is drawn by this tick; its three rings flare one after another, this many ticks apart. */
    public static final int MANDALA_DRAWN = 22, MANDALA_STEP = 10;
    /** Where each ring of the mandala begins and ends (fractions of its radius). */
    public static final float[] MANDALA_BANDS = {0, .36f, .68f, 1};
    /** A lantern rises for this long, then hunts an enemy this far off at this speed. */
    public static final int LANTERN_RISE = 20, LANTERN_BURST = 12;
    public static final double LANTERN_RANGE = 18, LANTERN_SPEED = .32;
    /** The storm of petals cuts every {@link #STORM_EVERY} ticks. */
    public static final int STORM_EVERY = 10;
    /** The great lotus opens fully at this tick, then lets its petals fall on its foes every few ticks until {@link #THOUSAND_RAIN_END}. */
    public static final int THOUSAND_BLOOM = 50, THOUSAND_RAIN_EVERY = 5, THOUSAND_RAIN_END = 110;
    /** The great lotus floats this high over its pond. */
    public static final float THOUSAND_HEIGHT = 3.5f;

    private static final EntityDataAccessor<Integer> DATA_KIND = SynchedEntityData.defineId(LotusScene.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DATA_SIZE = SynchedEntityData.defineId(LotusScene.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> DATA_LIFE = SynchedEntityData.defineId(LotusScene.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Vector3f> DATA_DIR = SynchedEntityData.defineId(LotusScene.class, EntityDataSerializers.VECTOR3);
    /** Where it last struck (relative to it) and when (game tick). */
    private static final EntityDataAccessor<Vector3f> DATA_TARGET = SynchedEntityData.defineId(LotusScene.class, EntityDataSerializers.VECTOR3);
    private static final EntityDataAccessor<Integer> DATA_STRUCK = SynchedEntityData.defineId(LotusScene.class, EntityDataSerializers.INT);
    /** A petal or a lantern that has struck stops there and bursts. */
    private static final EntityDataAccessor<Boolean> DATA_DONE = SynchedEntityData.defineId(LotusScene.class, EntityDataSerializers.BOOLEAN);

    @Nullable
    private String spellId;
    /** What it follows or holds (a throne, a pod or a storm its caster, a bud its prey). */
    private int bound = -1;
    /** Which of its fellows it is (each lantern hunts its own enemy). */
    private int index;
    private int strikes;

    public LotusScene(EntityType<? extends LotusScene> entityType, Level level) {
        super(entityType, level);
    }

    /** A scene of {@code kind} at {@code at}, facing {@code dir}, {@code size} (its radius or reach), lasting {@code life} ticks, striking for {@code damage}. */
    public static LotusScene spawn(Level level, @Nullable LivingEntity caster, AbstractSpell spell, Kind kind, Vec3 at, Vector3f dir, float size, int life,
                                   float damage) {
        LotusScene s = new LotusScene(ModEntities.LOTUS_SCENE.get(), level);
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

    /** Makes it follow or hold {@code e}. */
    public LotusScene follow(Entity e) {
        this.bound = e.getId();
        return this;
    }

    /** Which of its fellows it is. */
    public LotusScene index(int index) {
        this.index = index;
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

    /** Whether a petal or a lantern has struck (and stopped to burst). */
    public boolean isDone() {
        return this.entityData.get(DATA_DONE);
    }

    public float sinceStruck(float partialTicks) {
        return (int) level().getGameTime() + partialTicks - this.entityData.get(DATA_STRUCK);
    }

    /** Server: how many times it has struck. */
    public int strikes() {
        return strikes;
    }

    @Nullable
    private AbstractSpell spell() {
        return spellId == null ? null : SpellRegistry.getSpell(spellId);
    }

    private boolean strike(AbstractSpell spell, @Nullable LivingEntity caster, LivingEntity e, float amount) {
        e.invulnerableTime = 0;
        boolean hit = DamageSources.applyDamage(e, amount, SpellUtil.damageSource(spell, this, caster));
        if (hit) {
            strikes++;
        }
        return hit;
    }

    /** Shows the clients where it struck, and when. */
    private void struck(Vec3 at) {
        this.entityData.set(DATA_TARGET, at.subtract(position()).toVector3f());
        this.entityData.set(DATA_STRUCK, (int) level().getGameTime());
    }

    private void sound(SoundEvent sound, float volume, float pitch) {
        level().playSound(null, getX(), getY(), getZ(), sound, SoundSource.PLAYERS, volume, pitch);
    }

    /** Stops it where it is: it bursts there, then is gone after {@code linger} ticks. */
    private void finish(Vec3 at, int linger) {
        struck(at);
        this.entityData.set(DATA_DONE, true);
        this.entityData.set(DATA_LIFE, tickCount + linger);
    }

    private List<LivingEntity> enemies(ServerLevel level, Vec3 c, double radius, @Nullable LivingEntity caster, double height) {
        return SpellUtil.enemiesAround(level, c, radius, caster, x -> Math.abs(x.getY() - getY()) < height);
    }

    /** The caster's side within {@code radius} of {@code c}: the caster, its summons, its friends (players, if it has none). */
    private List<LivingEntity> allies(ServerLevel level, Vec3 c, double radius, @Nullable LivingEntity caster) {
        List<LivingEntity> out = new ArrayList<>();
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(c, c).inflate(radius, 3, radius))) {
            if (e.isAlive() && e.position().subtract(c).horizontalDistanceSqr() <= radius * radius
                    && (caster == null ? e instanceof Player : SpellUtil.isAlly(caster, e) || e.isAlliedTo(caster))) {
                out.add(e);
            }
        }
        return out;
    }

    @Nullable
    private LivingEntity nearestSeen(ServerLevel level, Vec3 from, double range, @Nullable LivingEntity caster) {
        List<LivingEntity> foes = SpellUtil.enemiesAround(level, from, range, caster, x -> true);
        foes.sort(Comparator.comparingDouble(e -> e.distanceToSqr(from)));
        for (LivingEntity e : foes) {
            if (level.clip(new ClipContext(from, e.getEyePosition(), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this)).getType() == HitResult.Type.MISS) {
                return e;
            }
        }
        return null;
    }

    private static Vec3 centre(LivingEntity e) {
        return e.position().add(0, e.getBbHeight() * .55, 0);
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
            case PETAL -> petal(level, spell, caster);
            case BUD -> bud(level, spell, caster, size);
            case POND -> pond(level, caster, size);
            case THRONE -> throne(level, spell, caster);
            case ROOTS -> roots(level, spell, caster, size);
            case POD -> pod(level, spell, caster);
            case MANDALA -> mandala(level, spell, caster, size);
            case LANTERN -> lantern(level, spell, caster, size);
            case STORM -> storm(level, spell, caster, size);
            case THOUSAND -> thousand(level, spell, caster, size);
        }
    }

    // ---------------------------------------------------------------- the kinds

    private void petal(ServerLevel level, AbstractSpell spell, @Nullable LivingEntity caster) {
        if (isDone()) {
            return;
        }
        Vector3f d = getDir();
        Vec3 from = position(), to = from.add(new Vec3(d.x, d.y, d.z).normalize().scale(PETAL_SPEED));
        BlockHitResult wall = level.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        Vec3 end = wall.getType() == HitResult.Type.MISS ? to : wall.getLocation();
        LivingEntity first = null;
        double best = Double.MAX_VALUE;
        for (LivingEntity e : SpellUtil.enemiesAround(level, from.add(end).scale(.5), PETAL_SPEED + 2, caster, x -> true)) {
            AABB box = e.getBoundingBox().inflate(.35);
            if (box.contains(from) || box.clip(from, end).isPresent()) {
                double dist = e.distanceToSqr(from);
                if (dist < best) {
                    best = dist;
                    first = e;
                }
            }
        }
        if (first != null) {
            if (strike(spell, caster, first, damage)) {
                first.setDeltaMovement(first.getDeltaMovement().add(new Vec3(d.x, 0, d.z).normalize().scale(.15)));
                first.hurtMarked = true;
            }
            Vec3 at = centre(first);
            LotusFx.petals(level, at, 10, .3);
            sound(SoundEvents.PLAYER_ATTACK_SWEEP, .8f, 1.6f);
            finish(at, PETAL_BURST);
            return;
        }
        if (wall.getType() != HitResult.Type.MISS || tickCount >= PETAL_FLIGHT) {
            LotusFx.petals(level, end, 6, .2);
            finish(end, PETAL_BURST);
            return;
        }
        setPos(to);
    }

    private void bud(ServerLevel level, AbstractSpell spell, @Nullable LivingEntity caster, float size) {
        Vec3 c = position();
        if (tickCount < BUD_BURST) {
            // Closing round its prey: whatever is inside is held in the middle
            for (LivingEntity e : enemies(level, c.add(0, 1, 0), size + .4, caster, 2.5)) {
                if (tickCount >= BUD_RISE - 2) {
                    e.addEffect(new MobEffectInstance(ModEffects.ENTANGLED, 6, 0, false, false, true));
                    Vec3 in = new Vec3(c.x - e.getX(), 0, c.z - e.getZ());
                    e.setDeltaMovement(in.scale(.3).add(0, Math.min(0, e.getDeltaMovement().y), 0));
                    e.hurtMarked = true;
                }
            }
        }
        if (tickCount == 1) {
            sound(SoundEvents.ROOTED_DIRT_BREAK, 1.5f, .7f);
        }
        if (tickCount == BUD_RISE) {
            sound(SoundEvents.AZALEA_LEAVES_BREAK, 1.5f, .6f);
        }
        if (tickCount == BUD_BURST) {
            for (LivingEntity e : enemies(level, c.add(0, 1, 0), size + .8, caster, 3)) {
                if (strike(spell, caster, e, damage)) {
                    e.setDeltaMovement(e.getDeltaMovement().multiply(.3, 0, .3).add(0, .85, 0));
                    e.hurtMarked = true;
                }
            }
            LotusFx.petals(level, c.add(0, 1.4, 0), 40, .9);
            LotusFx.pollen(level, c.add(0, 1.2, 0), 20, .6);
            sound(SoundEvents.AZALEA_LEAVES_BREAK, 2f, 1.2f);
            sound(SoundEvents.FIRECHARGE_USE, .6f, 1.6f);
            sound(SoundEvents.AMETHYST_BLOCK_CHIME, 1.5f, 1.4f);
        }
    }

    private void pond(ServerLevel level, @Nullable LivingEntity caster, float radius) {
        Vec3 c = position();
        if (tickCount > 10 && tickCount < getLife() - 20 && tickCount % POND_EVERY == 0) {
            for (LivingEntity a : allies(level, c, radius, caster)) {
                a.heal(damage);
                a.clearFire();
                LotusFx.pollen(level, a.position().add(0, 1, 0), 4, .3);
            }
            for (LivingEntity e : enemies(level, c.add(0, .5, 0), radius, caster, 2.5)) {
                e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, POND_EVERY + 10, 1, false, false, true));
                e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, POND_EVERY + 10, 0, false, false, true));
            }
            SpellUtil.particles(level, net.minecraft.core.particles.ParticleTypes.SPLASH, c.x, c.y + .2, c.z, 12, radius * .5, .05, radius * .5, .1);
        }
        if (tickCount % 30 == 5) {
            sound(SoundEvents.WATER_AMBIENT, .7f, 1.2f);
        }
    }

    private void throne(ServerLevel level, AbstractSpell spell, @Nullable LivingEntity caster) {
        Entity follow = bound >= 0 ? level.getEntity(bound) : null;
        if (follow != null && follow.isAlive()) {
            setPos(follow.getX(), follow.getY(), follow.getZ());
            Vec3 look = follow.getLookAngle().multiply(1, 0, 1);
            if (look.lengthSqr() > 1.0E-4) {
                this.entityData.set(DATA_DIR, look.normalize().toVector3f());
            }
        } else if (bound >= 0 && tickCount < getLife() - 15) {
            this.entityData.set(DATA_LIFE, tickCount + 15);
        }
        if (follow instanceof LivingEntity wearer && tickCount < getLife() - 15) {
            if (tickCount % 10 == 0) {
                wearer.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 15, 0, false, false, true));
                wearer.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 15, 0, false, false, true));
            }
            if (tickCount % 20 == 0) {
                wearer.heal(1);
            }
            if (tickCount >= 15 && tickCount % THRONE_EVERY == 0) {
                Vec3 from = position().add(0, .6, 0);
                LivingEntity prey = nearestSeen(level, from, THRONE_RANGE, caster);
                if (prey != null && strike(spell, caster, prey, damage)) {
                    struck(centre(prey));
                    LotusFx.petals(level, centre(prey), 8, .3);
                    sound(SoundEvents.PLAYER_ATTACK_SWEEP, .9f, 1.7f);
                }
            }
        }
        if (tickCount % 24 == 0) {
            LotusFx.petals(level, position().add(0, .4, 0), 2, .8);
        }
    }

    private void roots(ServerLevel level, AbstractSpell spell, @Nullable LivingEntity caster, float radius) {
        Vec3 c = position();
        if (tickCount >= getLife() - 10) {
            return;
        }
        List<LivingEntity> inside = enemies(level, c.add(0, .5, 0), radius, caster, 2.5);
        if (tickCount % 5 == 0) {
            for (LivingEntity e : inside) {
                e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 12, 2, false, false, true));
                Vec3 in = new Vec3(c.x - e.getX(), 0, c.z - e.getZ());
                if (in.lengthSqr() > .25) {
                    e.setDeltaMovement(e.getDeltaMovement().add(in.normalize().scale(.06)));
                    e.hurtMarked = true;
                }
            }
            SpellUtil.particles(level, net.minecraft.core.particles.ParticleTypes.BUBBLE_POP, c.x, c.y + .1, c.z, 3, radius * .5, 0, radius * .5, 0);
        }
        if (tickCount >= ROOTS_FIRST && (tickCount - ROOTS_FIRST) % ROOTS_EVERY == 0) {
            for (LivingEntity e : inside) {
                e.addEffect(new MobEffectInstance(ModEffects.ENTANGLED, 30, 0, false, false, true));
                if (strike(spell, caster, e, damage)) {
                    struck(e.position());
                }
            }
            sound(SoundEvents.ROOTED_DIRT_BREAK, 2f, .5f);
            sound(SoundEvents.MUD_STEP, 2f, .6f);
        }
        if (tickCount % 20 == 3) {
            sound(SoundEvents.BUBBLE_COLUMN_UPWARDS_AMBIENT, .6f, .6f);
        }
    }

    private void pod(ServerLevel level, AbstractSpell spell, @Nullable LivingEntity caster) {
        Entity follow = bound >= 0 ? level.getEntity(bound) : null;
        if (follow != null && follow.isAlive()) {
            // Floating at its master's left shoulder, a little behind
            float yaw = follow.getYRot() * Mth.DEG_TO_RAD;
            Vec3 back = new Vec3(Mth.sin(yaw), 0, -Mth.cos(yaw)).scale(.6);
            Vec3 side = new Vec3(Mth.cos(yaw), 0, Mth.sin(yaw)).scale(1.0);
            setPos(follow.position().add(back).add(side));
            Vec3 look = follow.getLookAngle().multiply(1, 0, 1);
            if (look.lengthSqr() > 1.0E-4) {
                this.entityData.set(DATA_DIR, look.normalize().toVector3f());
            }
        } else if (bound >= 0 && tickCount < getLife() - 12) {
            this.entityData.set(DATA_LIFE, tickCount + 12);
        }
        if (tickCount >= 12 && tickCount < getLife() - 12 && tickCount % POD_EVERY == 0) {
            Vec3 from = position().add(0, POD_HEIGHT + .3, 0);
            LivingEntity prey = nearestSeen(level, from, POD_RANGE, caster);
            if (prey != null) {
                Vec3 to = centre(prey);
                Vector3f dir = to.subtract(from).toVector3f();
                if (dir.lengthSquared() > 1.0E-4) {
                    this.entityData.set(DATA_DIR, dir.normalize());
                }
                strike(spell, caster, prey, damage);
                struck(to);
                LotusFx.petals(level, to, 6, .25);
                sound(SoundEvents.SWEET_BERRY_BUSH_BREAK, 1.2f, 1.5f);
                sound(SoundEvents.CROSSBOW_SHOOT, .5f, 1.8f);
            }
        }
    }

    private void mandala(ServerLevel level, AbstractSpell spell, @Nullable LivingEntity caster, float radius) {
        Vec3 c = position();
        if (tickCount == 2) {
            sound(SoundEvents.ENCHANTMENT_TABLE_USE, 2f, .6f);
        }
        for (int k = 0; k < 3; k++) {
            if (tickCount == MANDALA_DRAWN + k * MANDALA_STEP) {
                double r0 = MANDALA_BANDS[k] * radius, r1 = MANDALA_BANDS[k + 1] * radius;
                for (LivingEntity e : enemies(level, c.add(0, .5, 0), r1 + .6, caster, 3)) {
                    double d = Math.sqrt(e.position().subtract(c).horizontalDistanceSqr());
                    if (d >= r0 - .6 && strike(spell, caster, e, damage)) {
                        e.setDeltaMovement(e.getDeltaMovement().add(0, .45, 0));
                        e.hurtMarked = true;
                        e.addEffect(new MobEffectInstance(MobEffects.GLOWING, 60, 0, false, false, true));
                    }
                }
                LotusFx.ring(level, c, (r0 + r1) * .5, 24 + k * 12);
                sound(SoundEvents.AMETHYST_BLOCK_CHIME, 2f, .8f + k * .3f);
                sound(SoundEvents.BELL_RESONATE, .7f, 1.2f + k * .2f);
            }
        }
    }

    private void lantern(ServerLevel level, AbstractSpell spell, @Nullable LivingEntity caster, float blast) {
        if (isDone()) {
            return;
        }
        Vec3 pos = position();
        if (tickCount < LANTERN_RISE) {
            Vector3f d = getDir();
            setPos(pos.add(d.x * .07, .07, d.z * .07));
        } else {
            List<LivingEntity> foes = SpellUtil.enemiesAround(level, pos, LANTERN_RANGE, caster, x -> true);
            foes.sort(Comparator.comparingDouble(e -> e.distanceToSqr(pos)));
            if (!foes.isEmpty()) {
                LivingEntity prey = foes.get(index % foes.size());
                Vec3 to = centre(prey).subtract(pos);
                if (to.length() < 1.2) {
                    burst(level, spell, caster, blast);
                    return;
                }
                setPos(pos.add(to.normalize().scale(Math.min(LANTERN_SPEED, to.length()))));
            } else {
                setPos(pos.add(0, .01 * Mth.sin(tickCount * .1f), 0));
            }
        }
        if (tickCount >= getLife() - LANTERN_BURST) {
            burst(level, spell, caster, blast);
        }
    }

    private void burst(ServerLevel level, AbstractSpell spell, @Nullable LivingEntity caster, float blast) {
        Vec3 at = position();
        for (LivingEntity e : SpellUtil.enemiesAround(level, at, blast, caster, x -> true)) {
            if (strike(spell, caster, e, damage)) {
                e.igniteForSeconds(3);
            }
        }
        LotusFx.petals(level, at, 24, .6);
        LotusFx.pollen(level, at, 16, .5);
        SpellUtil.particles(level, net.minecraft.core.particles.ParticleTypes.FLAME, at.x, at.y, at.z, 10, .3, .3, .3, .05);
        sound(SoundEvents.FIREWORK_ROCKET_TWINKLE, 1.5f, 1.2f);
        sound(SoundEvents.FIRECHARGE_USE, .8f, 1.4f);
        finish(at, LANTERN_BURST);
    }

    private void storm(ServerLevel level, AbstractSpell spell, @Nullable LivingEntity caster, float radius) {
        Entity follow = bound >= 0 ? level.getEntity(bound) : null;
        if (follow != null && follow.isAlive()) {
            setPos(follow.getX(), follow.getY(), follow.getZ());
        } else if (bound >= 0 && tickCount < getLife() - 15) {
            this.entityData.set(DATA_LIFE, tickCount + 15);
        }
        if (tickCount >= getLife() - 15) {
            return;
        }
        Vec3 c = position();
        if (tickCount % STORM_EVERY == 0) {
            for (LivingEntity e : enemies(level, c.add(0, 1, 0), radius, caster, 3)) {
                Vec3 in = new Vec3(c.x - e.getX(), 0, c.z - e.getZ());
                double dist = in.length();
                if (dist > .3) {
                    Vec3 round = new Vec3(-in.z, 0, in.x).scale(1 / dist);
                    e.setDeltaMovement(e.getDeltaMovement().scale(.5).add(round.scale(.35)).add(in.scale(.04 / dist)));
                    e.hurtMarked = true;
                }
                if (strike(spell, caster, e, damage)) {
                    e.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 25, 0, false, false, true));
                }
            }
            LotusFx.petals(level, c.add(0, 1.2, 0), 10, radius * .6);
        }
        if (tickCount % 14 == 0) {
            sound(SoundEvents.AZALEA_LEAVES_FALL, 1.5f, .8f);
            sound(SoundEvents.BREEZE_WHIRL, .6f, 1.3f);
        }
    }

    private void thousand(ServerLevel level, AbstractSpell spell, @Nullable LivingEntity caster, float radius) {
        Vec3 c = position();
        if (tickCount == 2) {
            sound(SoundEvents.BEACON_ACTIVATE, 2.5f, .7f);
            sound(SoundEvents.GENERIC_SPLASH, 2f, .5f);
        }
        if (tickCount == THOUSAND_BLOOM - 10) {
            sound(SoundEvents.AMETHYST_BLOCK_RESONATE, 2.5f, .6f);
        }
        if (tickCount == THOUSAND_BLOOM) {
            for (LivingEntity e : enemies(level, c.add(0, 1, 0), radius, caster, 6)) {
                if (strike(spell, caster, e, damage)) {
                    e.setDeltaMovement(e.getDeltaMovement().multiply(.2, 0, .2).add(0, .9, 0));
                    e.hurtMarked = true;
                    e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 2, false, false, true));
                    e.addEffect(new MobEffectInstance(MobEffects.GLOWING, 100, 0, false, false, true));
                }
            }
            for (LivingEntity a : allies(level, c, radius, caster)) {
                a.heal(damage * .5f);
                a.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 1, false, true, true));
            }
            SpellUtil.cameraShake(level, c, 20, radius * 2.5f);
            LotusFx.petals(level, c.add(0, THOUSAND_HEIGHT + 1, 0), 120, radius * .5);
            LotusFx.pollen(level, c.add(0, THOUSAND_HEIGHT + 1, 0), 60, 2);
            sound(SoundEvents.TOTEM_USE, 1.5f, .8f);
            sound(SoundEvents.BELL_RESONATE, 2.5f, .5f);
            sound(SoundEvents.LIGHTNING_BOLT_THUNDER, .8f, 1.6f);
        }
        if (tickCount > THOUSAND_BLOOM && tickCount <= THOUSAND_RAIN_END && tickCount % THOUSAND_RAIN_EVERY == 0) {
            List<LivingEntity> foes = enemies(level, c.add(0, 1, 0), radius, caster, 6);
            if (!foes.isEmpty()) {
                LivingEntity e = foes.get(level.random.nextInt(foes.size()));
                if (strike(spell, caster, e, damage * .15f)) {
                    struck(centre(e));
                    LotusFx.petals(level, centre(e), 6, .3);
                }
                sound(SoundEvents.PLAYER_ATTACK_SWEEP, .8f, 1.5f + level.random.nextFloat() * .3f);
            }
        }
        if (tickCount % 8 == 0 && tickCount < getLife() - 30) {
            LotusFx.petals(level, c.add(0, THOUSAND_HEIGHT + 2, 0), 6, radius * .5);
        }
    }
}
