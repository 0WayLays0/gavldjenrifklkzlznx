package com.waylays.arcanes.entity.clockwork;

import com.waylays.arcanes.entity.SpellEffectEntity;
import com.waylays.arcanes.registry.ModEffects;
import com.waylays.arcanes.registry.ModEntities;
import com.waylays.arcanes.spells.SpellUtil;
import com.waylays.arcanes.spells.clockwork.ClockworkFx;
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * What Clockwork Magic leaves in the world: a saw-gear in flight, a hot rivet, the mainspring under its caster, a gear
 * trap, a turret, a gyroscopic cage, the brass automaton, the locomotive on its rails, the airship overhead, the Great
 * Mechanism. Each is a kind of this one entity: its renderer draws the kind's model, and it does the kind's work as it
 * ticks.
 */
public class ClockworkScene extends SpellEffectEntity {
    public enum Kind { SAW, RIVET, SPRING, TRAP, TURRET, GYRO, AUTOMATON, LOCOMOTIVE, AIRSHIP, MECHANISM }

    /** Saw-gear: its speed (blocks a tick), how long it flies, how many it cuts through; a broken machine's burst. */
    public static final double SAW_SPEED = 1.1;
    public static final int SAW_FLIGHT = 22, SAW_PIERCE = 4, BURST = 10;
    /** Rivet: its speed and how long it flies. */
    public static final double RIVET_SPEED = 1.8;
    public static final int RIVET_FLIGHT = 14;
    /** Mainspring: when it lets go; the stomp's reach where the caster lands. */
    public static final int SPRING_LAUNCH = 6;
    public static final double SPRING_STOMP = 3.2;
    /** Gear trap: how long it takes to set, to snap shut, how long it holds. */
    public static final int TRAP_ARM = 10, TRAP_SNAP = 3, TRAP_HOLD = 60;
    /** Turret: how long it takes to unfold, how often it fires, how far. */
    public static final int TURRET_RISE = 12, TURRET_EVERY = 8;
    public static final double TURRET_RANGE = 14;
    /** Gyroscopic cage: how long its rings take to close, how often they strike. */
    public static final int GYRO_CLOSE = 12, GYRO_EVERY = 10;
    /** Automaton: how long it takes to stand, its stride, its reach, a punch's wind-up and how often it punches. */
    public static final int AUTO_RISE = 20, AUTO_WIND = 8, AUTO_PUNCH = 22;
    public static final double AUTO_SPEED = .17, AUTO_REACH = 2.4, AUTO_RANGE = 16;
    /** Locomotive: how long its rails take to lay, how long it runs, how long the track is. */
    public static final int LOCO_RAILS = 14, LOCO_RUN = 26;
    public static final float LOCO_LENGTH = 24;
    /** Airship: how high it flies, how fast, where it comes from; when it drops its bombs, how often, how long they fall. */
    public static final float SHIP_HEIGHT = 9, SHIP_SPEED = .3f, SHIP_FROM = -14;
    public static final int SHIP_FIRST = 20, SHIP_EVERY = 5, SHIP_BOMBS = 10, SHIP_FALL = 10;
    public static final double SHIP_BLAST = 2.8;
    /** Great Mechanism: how long it takes to rise, how often its core strikes, when it overloads. */
    public static final int MECH_RISE = 30, MECH_EVERY = 10, MECH_OVERLOAD = 200;

    private static final EntityDataAccessor<Integer> DATA_KIND = SynchedEntityData.defineId(ClockworkScene.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DATA_SIZE = SynchedEntityData.defineId(ClockworkScene.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> DATA_LIFE = SynchedEntityData.defineId(ClockworkScene.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Vector3f> DATA_DIR = SynchedEntityData.defineId(ClockworkScene.class, EntityDataSerializers.VECTOR3);
    private static final EntityDataAccessor<Vector3f> DATA_TARGET = SynchedEntityData.defineId(ClockworkScene.class, EntityDataSerializers.VECTOR3);
    private static final EntityDataAccessor<Integer> DATA_STRUCK = SynchedEntityData.defineId(ClockworkScene.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_DONE = SynchedEntityData.defineId(ClockworkScene.class, EntityDataSerializers.BOOLEAN);

    @Nullable
    private String spellId;
    private int bound = -1;
    private final Set<Integer> hit = new HashSet<>();
    private final List<Integer> held = new ArrayList<>();
    private int strikes;
    private int prey = -1;
    private int nextPunch;
    private boolean landed;

    public ClockworkScene(EntityType<? extends ClockworkScene> entityType, Level level) {
        super(entityType, level);
    }

    /** A scene of {@code kind} at {@code at}, facing {@code dir}, {@code size} (its radius or reach), lasting {@code life} ticks, striking for {@code damage}. */
    public static ClockworkScene spawn(Level level, @Nullable LivingEntity caster, AbstractSpell spell, Kind kind, Vec3 at, Vector3f dir, float size, int life,
                                       float damage) {
        ClockworkScene s = new ClockworkScene(ModEntities.CLOCKWORK_SCENE.get(), level);
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

    public ClockworkScene follow(@Nullable Entity e) {
        this.bound = e == null ? -1 : e.getId();
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

    public boolean isDone() {
        return this.entityData.get(DATA_DONE);
    }

    public float sinceStruck(float partialTicks) {
        return (int) level().getGameTime() + partialTicks - this.entityData.get(DATA_STRUCK);
    }

    public int strikes() {
        return strikes;
    }

    /** Where the airship is along its way (from the middle of the bombed ground) {@code t} ticks after it was called. */
    public static float shipAlong(float t) {
        return SHIP_FROM + SHIP_SPEED * t;
    }

    /** When the airship's bomb {@code i} falls from it. */
    public static int bombDrop(int i) {
        return SHIP_FIRST + i * SHIP_EVERY;
    }

    /** Where bomb {@code i} lands, from the middle of the bombed ground: along the ship's way, and to one side or the other. */
    public static Vector3f bombLands(int i, Vector3f dir) {
        Vector3f d = new Vector3f(dir.x, 0, dir.z);
        if (d.lengthSquared() < 1.0E-6f) {
            d.set(0, 0, 1);
        }
        d.normalize();
        float along = shipAlong(bombDrop(i)) + SHIP_SPEED * SHIP_FALL;
        float side = (i % 2 == 0 ? 1 : -1) * (1 + .6f * ((i / 2) % 2));
        return new Vector3f(d).mul(along).add(-d.z * side, 0, d.x * side);
    }

    /** Where the locomotive's front is along its track {@code t} ticks after it was called (it gathers speed as it runs). */
    public static float trainAt(float t) {
        float u = Mth.clamp((t - LOCO_RAILS) / LOCO_RUN, 0, 1);
        return LOCO_LENGTH * (.35f * u + .65f * u * u);
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

    private static void sound(Level level, Vec3 at, SoundEvent sound, float volume, float pitch) {
        level.playSound(null, at.x, at.y, at.z, sound, SoundSource.PLAYERS, volume, pitch);
    }

    private List<LivingEntity> enemies(ServerLevel level, Vec3 c, double radius, @Nullable LivingEntity caster, double height) {
        double y = c.y;
        return SpellUtil.enemiesAround(level, c, radius, caster, x -> Math.abs(x.getY() - y) < height);
    }

    private static List<LivingEntity> allies(ServerLevel level, Vec3 c, double radius, @Nullable LivingEntity caster) {
        List<LivingEntity> out = new ArrayList<>();
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(c, c).inflate(radius, 3, radius))) {
            if (e.isAlive() && e.position().subtract(c).horizontalDistanceSqr() <= radius * radius
                    && (caster == null ? e instanceof Player : SpellUtil.isAlly(caster, e) || e.isAlliedTo(caster))) {
                out.add(e);
            }
        }
        return out;
    }

    private static Vec3 centre(LivingEntity e) {
        return e.position().add(0, e.getBbHeight() * .55, 0);
    }

    @Nullable
    private Entity bound(ServerLevel level) {
        return bound >= 0 ? level.getEntity(bound) : null;
    }

    /** How far {@code p} is from the segment {@code a}..{@code b}, flat on the ground. */
    private static double flatDistance(Vec3 p, Vec3 a, Vec3 b) {
        double dx = b.x - a.x, dz = b.z - a.z;
        double l2 = dx * dx + dz * dz;
        double t = l2 < 1.0E-6 ? 0 : Mth.clamp(((p.x - a.x) * dx + (p.z - a.z) * dz) / l2, 0, 1);
        double x = a.x + dx * t - p.x, z = a.z + dz * t - p.z;
        return Math.sqrt(x * x + z * z);
    }

    private Vec3 flatDir() {
        Vector3f d = getDir();
        Vec3 v = new Vec3(d.x, 0, d.z);
        return v.lengthSqr() < 1.0E-6 ? new Vec3(0, 0, 1) : v.normalize();
    }

    /** Knocks {@code e} away from {@code from}, {@code power} along the ground and {@code up} up. */
    private static void knock(LivingEntity e, Vec3 from, double power, double up) {
        Vec3 away = new Vec3(e.getX() - from.x, 0, e.getZ() - from.z);
        away = away.lengthSqr() < 1.0E-4 ? new Vec3(1, 0, 0) : away.normalize();
        double k = power * Math.max(0, 1 - e.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.KNOCKBACK_RESISTANCE));
        e.setDeltaMovement(e.getDeltaMovement().add(away.scale(k)).add(0, up, 0));
        e.hurtMarked = true;
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
            case SAW -> saw(level, spell, caster);
            case RIVET -> rivet(level, spell, caster);
            case SPRING -> spring(level, spell, caster);
            case TRAP -> trap(level, spell, caster);
            case TURRET -> turret(level, spell, caster);
            case GYRO -> gyro(level, spell, caster, size);
            case AUTOMATON -> automaton(level, spell, caster);
            case LOCOMOTIVE -> locomotive(level, spell, caster);
            case AIRSHIP -> airship(level, spell, caster);
            case MECHANISM -> mechanism(level, spell, caster, size);
        }
    }

    // ---------------------------------------------------------------- the kinds

    /** Flies on, cutting through what it meets (a few, each once), and breaks into cogs where it stops. */
    private void projectile(ServerLevel level, AbstractSpell spell, @Nullable LivingEntity caster, double speed, int flight, int pierce, double wide,
                            SoundEvent hitSound, float pitch) {
        if (isDone()) {
            return;
        }
        Vector3f d = getDir();
        Vec3 from = position(), to = from.add(new Vec3(d.x, d.y, d.z).normalize().scale(speed));
        BlockHitResult wall = level.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        Vec3 end = wall.getType() == HitResult.Type.MISS ? to : wall.getLocation();
        List<LivingEntity> line = new ArrayList<>();
        for (LivingEntity e : SpellUtil.enemiesAround(level, from.add(end).scale(.5), speed + 2, caster, x -> !hit.contains(x.getId()))) {
            AABB box = e.getBoundingBox().inflate(wide);
            if (box.contains(from) || box.clip(from, end).isPresent()) {
                line.add(e);
            }
        }
        line.sort(Comparator.comparingDouble(e -> e.distanceToSqr(from)));
        for (LivingEntity e : line) {
            hit.add(e.getId());
            strike(spell, caster, e, damage);
            ClockworkFx.sparks(level, centre(e), 8, .3);
            sound(level, centre(e), hitSound, 1, pitch);
            if (hit.size() >= pierce) {
                finish(centre(e), BURST);
                return;
            }
        }
        if (wall.getType() != HitResult.Type.MISS || tickCount >= flight) {
            ClockworkFx.sparks(level, end, 10, .3);
            sound(level, end, SoundEvents.CHAIN_BREAK, 1, 1.2f);
            finish(end, BURST);
            return;
        }
        setPos(to);
    }

    private void saw(ServerLevel level, AbstractSpell spell, @Nullable LivingEntity caster) {
        projectile(level, spell, caster, SAW_SPEED, SAW_FLIGHT, SAW_PIERCE, .45, SoundEvents.GRINDSTONE_USE, 1.4f);
        if (!isDone() && tickCount % 4 == 0) {
            sound(SoundEvents.CHAIN_STEP, .8f, 1.8f);
        }
    }

    private void rivet(ServerLevel level, AbstractSpell spell, @Nullable LivingEntity caster) {
        projectile(level, spell, caster, RIVET_SPEED, RIVET_FLIGHT, 1, .25, SoundEvents.ANVIL_PLACE, 1.8f);
    }

    private void spring(ServerLevel level, AbstractSpell spell, @Nullable LivingEntity caster) {
        Entity rider = bound(level);
        if (rider == null || !rider.isAlive()) {
            return;
        }
        if (tickCount == SPRING_LAUNCH) {
            Vec3 ahead = flatDir().scale(1.15);
            rider.setDeltaMovement(ahead.x, 1.15, ahead.z);
            rider.hurtMarked = true;
            rider.fallDistance = 0;
            for (LivingEntity e : enemies(level, position().add(0, .5, 0), 2.6, caster, 2)) {
                if (strike(spell, caster, e, damage * .5f)) {
                    knock(e, position(), .9, .35);
                }
            }
            ClockworkFx.steam(level, position().add(0, .3, 0), 16, .8);
            sound(SoundEvents.PISTON_EXTEND, 1.4f, .7f);
            sound(SoundEvents.IRON_DOOR_OPEN, 1f, 1.5f);
        }
        if (tickCount > SPRING_LAUNCH && !landed) {
            rider.fallDistance = 0;
            if (tickCount > SPRING_LAUNCH + 5 && rider.onGround()) {
                landed = true;
                Vec3 at = rider.position();
                for (LivingEntity e : enemies(level, at.add(0, .5, 0), SPRING_STOMP, caster, 2.5)) {
                    if (strike(spell, caster, e, damage)) {
                        knock(e, at, .7, .5);
                    }
                }
                ClockworkFx.sparks(level, at.add(0, .3, 0), 20, 1.2);
                ClockworkFx.steam(level, at.add(0, .3, 0), 20, 1.5);
                SpellUtil.cameraShake(level, at, 8, 10);
                sound(level, at, SoundEvents.ANVIL_LAND, 1.2f, .8f);
                sound(level, at, SoundEvents.GENERIC_EXPLODE.value(), .6f, 1.5f);
                finish(at, 24);
            }
        }
    }

    private void trap(ServerLevel level, AbstractSpell spell, @Nullable LivingEntity caster) {
        if (tickCount == 1) {
            sound(SoundEvents.CHAIN_PLACE, 1, 1.2f);
        }
        if (tickCount < TRAP_ARM) {
            return;
        }
        float since = sinceStruck(0);
        boolean sprung = this.entityData.get(DATA_STRUCK) > -1000;
        if (!sprung) {
            if (tickCount % 20 == 0) {
                sound(SoundEvents.COMPARATOR_CLICK, .5f, 1.6f);
            }
            List<LivingEntity> near = enemies(level, position(), 1.4, caster, 1.6);
            if (near.isEmpty()) {
                return;
            }
            for (LivingEntity e : near) {
                held.add(e.getId());
                strike(spell, caster, e, damage);
                e.addEffect(new MobEffectInstance(ModEffects.ENTANGLED, TRAP_HOLD, 0, false, false, true));
                e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, TRAP_HOLD, 4, false, false, true));
            }
            struck(near.get(0).position());
            this.entityData.set(DATA_LIFE, tickCount + TRAP_SNAP + TRAP_HOLD + 20);
            ClockworkFx.sparks(level, position().add(0, .5, 0), 16, .6);
            sound(SoundEvents.ANVIL_LAND, 1, 1.4f);
            sound(SoundEvents.IRON_DOOR_CLOSE, 1.2f, .7f);
            return;
        }
        if (since < TRAP_SNAP + TRAP_HOLD) {
            // The jaws hold their prey where it stands, and bite it again now and then
            for (int id : held) {
                if (level.getEntity(id) instanceof LivingEntity e && e.isAlive()) {
                    e.setDeltaMovement(0, Math.min(0, e.getDeltaMovement().y), 0);
                    e.hurtMarked = true;
                    if ((int) since % 20 == 19) {
                        strike(spell, caster, e, damage * .3f);
                    }
                }
            }
        } else if (!isDone()) {
            sound(SoundEvents.IRON_DOOR_OPEN, 1, 1.2f);
            this.entityData.set(DATA_DONE, true);
        }
    }

    private void turret(ServerLevel level, AbstractSpell spell, @Nullable LivingEntity caster) {
        if (tickCount == 1) {
            sound(SoundEvents.PISTON_EXTEND, 1, 1.2f);
        }
        if (tickCount < TURRET_RISE || tickCount >= getLife() - 15 || (tickCount - TURRET_RISE) % TURRET_EVERY != 0) {
            return;
        }
        Vec3 muzzle = position().add(0, 1.4, 0);
        List<LivingEntity> foes = SpellUtil.enemiesAround(level, muzzle, TURRET_RANGE, caster, x -> true);
        foes.sort(Comparator.comparingDouble(e -> e.distanceToSqr(muzzle)));
        for (LivingEntity e : foes) {
            Vec3 at = centre(e);
            if (level.clip(new ClipContext(muzzle, at, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this)).getType() != HitResult.Type.MISS) {
                continue;
            }
            strike(spell, caster, e, damage);
            struck(at);
            ClockworkFx.sparks(level, at, 6, .2);
            sound(SoundEvents.CROSSBOW_SHOOT, 1, 1.6f);
            sound(level, at, SoundEvents.ANVIL_PLACE, .4f, 2f);
            return;
        }
    }

    private void gyro(ServerLevel level, AbstractSpell spell, @Nullable LivingEntity caster, float radius) {
        if (tickCount == 1) {
            sound(SoundEvents.BEACON_ACTIVATE, 1, 1.8f);
        }
        Vec3 c = position().add(0, 1, 0);
        boolean ending = tickCount >= getLife() - BURST;
        if (tickCount == getLife() - BURST) {
            for (LivingEntity e : enemies(level, c, radius + .5, caster, 2.5)) {
                if (strike(spell, caster, e, damage * 1.5f)) {
                    knock(e, position(), .8, .4);
                }
            }
            ClockworkFx.sparks(level, c, 24, .8);
            sound(SoundEvents.GENERIC_EXPLODE.value(), .7f, 1.6f);
            this.entityData.set(DATA_DONE, true);
            return;
        }
        if (ending || tickCount < GYRO_CLOSE) {
            return;
        }
        List<LivingEntity> inside = enemies(level, c, radius + .5, caster, 2.5);
        for (LivingEntity e : inside) {
            // Drawn to the middle of the rings, and kept there
            Vec3 in = new Vec3(getX() - e.getX(), 0, getZ() - e.getZ());
            e.setDeltaMovement(in.scale(.25).add(0, Math.min(0, e.getDeltaMovement().y), 0));
            e.hurtMarked = true;
            e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 15, 3, false, false, true));
        }
        if ((tickCount - GYRO_CLOSE) % GYRO_EVERY == 0 && !inside.isEmpty()) {
            for (LivingEntity e : inside) {
                strike(spell, caster, e, damage * .5f);
            }
            struck(centre(inside.get(0)));
            ClockworkFx.arcs(level, c, 6, .6);
            sound(SoundEvents.BEACON_POWER_SELECT, .8f, 2f);
        }
    }

    private void automaton(ServerLevel level, AbstractSpell spell, @Nullable LivingEntity caster) {
        if (tickCount == 1) {
            sound(SoundEvents.IRON_GOLEM_REPAIR, 1.2f, .8f);
        }
        if (tickCount < AUTO_RISE || tickCount >= getLife() - 20) {
            return;
        }
        if (tickCount % 30 == 0) {
            sound(SoundEvents.PISTON_CONTRACT, .5f, 1.5f);
        }
        // A punch winding up lands now
        float since = sinceStruck(0);
        if ((int) since == AUTO_WIND) {
            Vec3 f = flatDir();
            Vec3 at = position().add(f.scale(1.4)).add(0, 1, 0);
            for (LivingEntity e : enemies(level, at, 1.8, caster, 2.5)) {
                if (strike(spell, caster, e, damage)) {
                    e.setDeltaMovement(e.getDeltaMovement().add(f.scale(1.3)).add(0, .45, 0));
                    e.hurtMarked = true;
                }
            }
            ClockworkFx.steam(level, at, 10, .4);
            ClockworkFx.sparks(level, at, 10, .4);
            SpellUtil.cameraShake(level, at, 5, 8);
            sound(level, at, SoundEvents.IRON_GOLEM_ATTACK, 1.4f, .8f);
            sound(level, at, SoundEvents.ANVIL_LAND, .7f, 1.2f);
        }
        if (since < AUTO_WIND + 4) {
            return;
        }
        LivingEntity target = prey >= 0 && level.getEntity(prey) instanceof LivingEntity p && p.isAlive() && p.distanceToSqr(this) < AUTO_RANGE * AUTO_RANGE ? p : null;
        if (target == null || tickCount % 10 == 0) {
            List<LivingEntity> foes = SpellUtil.enemiesAround(level, position(), AUTO_RANGE, caster, x -> true);
            foes.sort(Comparator.comparingDouble(e -> e.distanceToSqr(this)));
            target = foes.isEmpty() ? null : foes.get(0);
            prey = target == null ? -1 : target.getId();
        }
        Vec3 goal;
        double stop;
        if (target != null) {
            goal = target.position();
            stop = AUTO_REACH - .4;
        } else if (caster != null) {
            goal = caster.position();
            stop = 3.5;
        } else {
            return;
        }
        Vec3 to = new Vec3(goal.x - getX(), 0, goal.z - getZ());
        double dist = to.length();
        if (dist > 1.0E-3) {
            this.entityData.set(DATA_DIR, to.normalize().toVector3f());
        }
        if (target != null && dist < AUTO_REACH && tickCount >= nextPunch) {
            nextPunch = tickCount + AUTO_PUNCH;
            struck(target.position());
            sound(SoundEvents.PISTON_EXTEND, 1, .8f);
            return;
        }
        if (dist > stop) {
            Vec3 step = to.normalize().scale(Math.min(AUTO_SPEED, dist - stop));
            Vec3 next = position().add(step);
            // Over the ground: up a step, or down to the ground below
            BlockHitResult ground = level.clip(new ClipContext(next.add(0, 1.2, 0), next.add(0, -3, 0), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
            if (ground.getType() != HitResult.Type.MISS) {
                double y = ground.getLocation().y;
                if (y - getY() <= 1.1) {
                    setPos(next.x, y, next.z);
                }
            } else {
                setPos(next.x, getY(), next.z);
            }
            if (tickCount % 8 == 0) {
                sound(SoundEvents.IRON_GOLEM_STEP, .8f, .9f);
            }
        }
    }

    private void locomotive(ServerLevel level, AbstractSpell spell, @Nullable LivingEntity caster) {
        if (tickCount == 1) {
            sound(SoundEvents.ANVIL_USE, .8f, 1.4f);
        }
        if (tickCount == LOCO_RAILS) {
            sound(SoundEvents.FIREWORK_ROCKET_LAUNCH, 1.5f, .5f);
            sound(SoundEvents.BELL_BLOCK, 1.2f, 1.6f);
        }
        if (tickCount < LOCO_RAILS || tickCount > LOCO_RAILS + LOCO_RUN) {
            return;
        }
        Vec3 f = flatDir();
        float s0 = trainAt(tickCount - 1), s1 = trainAt(tickCount);
        Vec3 a = position().add(f.scale(Math.max(0, s0 - 1.5))), b = position().add(f.scale(s1 + 1.2));
        for (LivingEntity e : SpellUtil.enemiesAround(level, a.add(b).scale(.5), a.distanceTo(b) * .5 + 2.5, caster, x -> !hit.contains(x.getId()))) {
            if (flatDistance(e.position(), a, b) < 1.6 && Math.abs(e.getY() - getY()) < 2.5) {
                hit.add(e.getId());
                if (strike(spell, caster, e, damage)) {
                    // Flung to the side of the track it stood on, and up
                    Vec3 side = new Vec3(-f.z, 0, f.x);
                    double which = (e.getX() - getX()) * side.x + (e.getZ() - getZ()) * side.z >= 0 ? 1 : -1;
                    e.setDeltaMovement(e.getDeltaMovement().add(side.scale(1.1 * which)).add(f.scale(.6)).add(0, .6, 0));
                    e.hurtMarked = true;
                }
                ClockworkFx.sparks(level, centre(e), 10, .4);
                sound(level, centre(e), SoundEvents.ANVIL_LAND, .8f, .7f);
            }
        }
        if (tickCount % 4 == 0) {
            sound(level, position().add(f.scale(s1)), SoundEvents.PISTON_EXTEND, 1.2f, .6f + tickCount * .01f);
        }
        if (tickCount == LOCO_RAILS + LOCO_RUN) {
            Vec3 end = position().add(f.scale(s1)).add(0, 1, 0);
            ClockworkFx.steam(level, end, 40, 1.8);
            sound(level, end, SoundEvents.GENERIC_EXPLODE.value(), 1f, 1.3f);
            finish(end, 20);
        }
    }

    private void airship(ServerLevel level, AbstractSpell spell, @Nullable LivingEntity caster) {
        if (tickCount == 1) {
            sound(SoundEvents.ELYTRA_FLYING, 1.5f, .6f);
        }
        if (tickCount % 20 == 0) {
            Vec3 f = flatDir();
            sound(level, position().add(f.scale(shipAlong(tickCount))).add(0, SHIP_HEIGHT, 0), SoundEvents.BEACON_AMBIENT, 1.5f, .5f);
        }
        Vector3f dir = getDir();
        for (int i = 0; i < SHIP_BOMBS; i++) {
            int drop = bombDrop(i);
            if (tickCount == drop) {
                Vec3 f = flatDir();
                Vec3 from = position().add(f.scale(shipAlong(drop))).add(0, SHIP_HEIGHT - 2.4, 0);
                sound(level, from, SoundEvents.DISPENSER_LAUNCH, 1, .8f);
            }
            if (tickCount == drop + SHIP_FALL) {
                Vector3f l = bombLands(i, dir);
                Vec3 at = position().add(l.x, l.y, l.z);
                for (LivingEntity e : enemies(level, at.add(0, .5, 0), SHIP_BLAST, caster, 3)) {
                    if (strike(spell, caster, e, damage)) {
                        knock(e, at, .6, .45);
                    }
                }
                ClockworkFx.blast(level, at.add(0, .5, 0));
                sound(level, at, SoundEvents.GENERIC_EXPLODE.value(), 1.2f, 1.1f + (i % 3) * .1f);
                struck(at);
            }
        }
    }

    private void mechanism(ServerLevel level, AbstractSpell spell, @Nullable LivingEntity caster, float radius) {
        Vec3 c = position();
        if (tickCount == 1) {
            sound(SoundEvents.ANVIL_USE, 2f, .5f);
            sound(SoundEvents.BEACON_ACTIVATE, 2f, .6f);
        }
        if (tickCount < MECH_RISE) {
            if (tickCount % 5 == 0) {
                sound(SoundEvents.PISTON_EXTEND, 1.5f, .5f + tickCount * .02f);
            }
            return;
        }
        if (tickCount < MECH_OVERLOAD) {
            if (tickCount % 20 == 0) {
                sound(SoundEvents.BEACON_AMBIENT, 2.5f, .7f);
                for (LivingEntity a : allies(level, c, radius, caster)) {
                    a.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 30, 0, false, false, true));
                }
                for (LivingEntity e : enemies(level, c.add(0, 1, 0), radius, caster, 8)) {
                    e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 1, false, false, true));
                }
            }
            if ((tickCount - MECH_RISE) % MECH_EVERY == 0) {
                List<LivingEntity> foes = enemies(level, c.add(0, 1, 0), radius, caster, 8);
                if (!foes.isEmpty()) {
                    LivingEntity e = foes.get(level.random.nextInt(foes.size()));
                    if (strike(spell, caster, e, damage * .3f)) {
                        e.addEffect(new MobEffectInstance(MobEffects.GLOWING, 40, 0, false, false, true));
                    }
                    struck(centre(e));
                    ClockworkFx.arcs(level, centre(e), 8, .4);
                    sound(level, centre(e), SoundEvents.LIGHTNING_BOLT_IMPACT, .6f, 1.8f);
                }
            }
            if (tickCount % 8 == 0) {
                sound(SoundEvents.PISTON_CONTRACT, 1.2f, .6f);
            }
            return;
        }
        if (tickCount == MECH_OVERLOAD) {
            for (LivingEntity e : enemies(level, c.add(0, 1, 0), radius, caster, 8)) {
                if (strike(spell, caster, e, damage)) {
                    knock(e, c, 1.4, .7);
                }
            }
            for (LivingEntity a : allies(level, c, radius, caster)) {
                a.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 300, 1, false, true, true));
            }
            ClockworkFx.arcs(level, c.add(0, 6, 0), 60, 3);
            ClockworkFx.steam(level, c.add(0, 1, 0), 80, radius * .5);
            SpellUtil.cameraShake(level, c, 25, radius * 2);
            sound(SoundEvents.GENERIC_EXPLODE.value(), 2.5f, .6f);
            sound(SoundEvents.LIGHTNING_BOLT_THUNDER, 2f, 1.2f);
            sound(SoundEvents.BELL_RESONATE, 2.5f, .8f);
            this.entityData.set(DATA_DONE, true);
        }
    }
}
