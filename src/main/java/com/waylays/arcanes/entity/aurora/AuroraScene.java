package com.waylays.arcanes.entity.aurora;

import com.waylays.arcanes.entity.SpellEffectEntity;
import com.waylays.arcanes.registry.ModEntities;
import com.waylays.arcanes.spells.SpellUtil;
import com.waylays.arcanes.spells.aurora.AuroraFx;
import com.waylays.arcanes.spells.wind.WindFx;
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
 * What Aurora Magic leaves in the world: a lance of light in flight, a wisp, a curtain, the trail of a glide, the mantle
 * round its caster, a blessing descending, a spiral, the serpent of light, the halo, the Boreal Night. Each is a kind of
 * this one entity: its renderer draws the kind's model, and it does the kind's work as it ticks.
 */
public class AuroraScene extends SpellEffectEntity {
    public enum Kind { LANCE, WISP, CURTAIN, GLIDE, MANTLE, BLESSING, SPIRAL, SERPENT, HALO, NIGHT }

    /** The lance flies this fast for this long, through this many enemies. */
    public static final double LANCE_SPEED = 1.5;
    public static final int LANCE_FLIGHT = 18, LANCE_PIERCE = 3, BURST = 10;
    /** A wisp waits this long by its caster, then flies at its enemy, this far off at most. */
    public static final int WISP_WAIT = 10;
    public static final double WISP_RANGE = 20;
    /** The curtain stands this high, this long from end to end, and strikes what crosses it every {@link #CURTAIN_EVERY} ticks. */
    public static final float CURTAIN_HEIGHT = 5.5f, CURTAIN_LENGTH = 12;
    public static final int CURTAIN_EVERY = 10;
    /** The glide lasts this long and sweeps this close to its path. */
    public static final int GLIDE_TICKS = 12;
    /** The mantle strikes what is close every {@link #MANTLE_EVERY} ticks. */
    public static final int MANTLE_EVERY = 10;
    /** The blessing's curtains come down at this tick. */
    public static final int BLESSING_LAND = 16;
    /** The spiral strikes every {@link #SPIRAL_EVERY} ticks. */
    public static final int SPIRAL_EVERY = 10;
    /** The serpent circles this far round and this high, diving every {@link #SERPENT_EVERY} ticks; a dive takes {@link #SERPENT_DIVE}. */
    public static final float SERPENT_RADIUS = 6, SERPENT_HEIGHT = 6;
    public static final float SERPENT_SPIN = .06f;
    public static final int SERPENT_FIRST = 20, SERPENT_EVERY = 40, SERPENT_DIVE = 16;
    public static final double SERPENT_RANGE = 16;
    /** The halo hangs this high and strikes every {@link #HALO_EVERY} ticks. */
    public static final float HALO_HEIGHT = 9;
    public static final int HALO_OPEN = 15, HALO_EVERY = 8;
    /** The night falls over this long; then waves sweep it every {@link #NIGHT_WAVE} ticks, four times; then the lights gather. */
    public static final int NIGHT_FALL = 30, NIGHT_WAVE = 40, NIGHT_SWEEP = 28, NIGHT_GATHER = NIGHT_FALL + 4 * NIGHT_WAVE + 10;

    private static final EntityDataAccessor<Integer> DATA_KIND = SynchedEntityData.defineId(AuroraScene.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DATA_SIZE = SynchedEntityData.defineId(AuroraScene.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> DATA_LIFE = SynchedEntityData.defineId(AuroraScene.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Vector3f> DATA_DIR = SynchedEntityData.defineId(AuroraScene.class, EntityDataSerializers.VECTOR3);
    private static final EntityDataAccessor<Vector3f> DATA_TARGET = SynchedEntityData.defineId(AuroraScene.class, EntityDataSerializers.VECTOR3);
    private static final EntityDataAccessor<Integer> DATA_STRUCK = SynchedEntityData.defineId(AuroraScene.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_DONE = SynchedEntityData.defineId(AuroraScene.class, EntityDataSerializers.BOOLEAN);

    @Nullable
    private String spellId;
    private int bound = -1;
    private int index;
    private final Set<Integer> hit = new HashSet<>();
    private int strikes;

    public AuroraScene(EntityType<? extends AuroraScene> entityType, Level level) {
        super(entityType, level);
    }

    /** A scene of {@code kind} at {@code at}, facing {@code dir}, {@code size} (its radius or reach), lasting {@code life} ticks, striking for {@code damage}. */
    public static AuroraScene spawn(Level level, @Nullable LivingEntity caster, AbstractSpell spell, Kind kind, Vec3 at, Vector3f dir, float size, int life,
                                    float damage) {
        AuroraScene s = new AuroraScene(ModEntities.AURORA_SCENE.get(), level);
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

    public AuroraScene follow(@Nullable Entity e) {
        this.bound = e == null ? -1 : e.getId();
        return this;
    }

    public AuroraScene index(int index) {
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

    public boolean isDone() {
        return this.entityData.get(DATA_DONE);
    }

    public float sinceStruck(float partialTicks) {
        return (int) level().getGameTime() + partialTicks - this.entityData.get(DATA_STRUCK);
    }

    public int strikes() {
        return strikes;
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

    private void keepUp(ServerLevel level, int linger) {
        Entity follow = bound(level);
        if (follow != null && follow.isAlive()) {
            setPos(follow.getX(), follow.getY(), follow.getZ());
        } else if (bound >= 0 && tickCount < getLife() - linger) {
            this.entityData.set(DATA_LIFE, tickCount + linger);
        }
    }

    /** How far {@code p} is from the segment {@code a}..{@code b}, flat on the ground. */
    private static double flatDistance(Vec3 p, Vec3 a, Vec3 b) {
        double dx = b.x - a.x, dz = b.z - a.z;
        double l2 = dx * dx + dz * dz;
        double t = l2 < 1.0E-6 ? 0 : Mth.clamp(((p.x - a.x) * dx + (p.z - a.z) * dz) / l2, 0, 1);
        double x = a.x + dx * t - p.x, z = a.z + dz * t - p.z;
        return Math.sqrt(x * x + z * z);
    }

    /** Where the serpent's head is {@code t} ticks after it was called, round its middle, while it circles. */
    public static Vector3f circling(float t) {
        float a = t * SERPENT_SPIN;
        return new Vector3f(Mth.cos(a) * SERPENT_RADIUS, SERPENT_HEIGHT + .8f * Mth.sin(t * SERPENT_SPIN * 2), Mth.sin(a) * SERPENT_RADIUS);
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
            case LANCE -> lance(level, spell, caster);
            case WISP -> wisp(level, spell, caster);
            case CURTAIN -> curtain(level, spell, caster);
            case GLIDE -> glide(level, spell, caster);
            case MANTLE -> mantle(level, spell, caster, size);
            case BLESSING -> blessing(level, caster, size);
            case SPIRAL -> spiral(level, spell, caster, size);
            case SERPENT -> serpent(level, spell, caster);
            case HALO -> halo(level, spell, caster, size);
            case NIGHT -> night(level, spell, caster, size);
        }
    }

    // ---------------------------------------------------------------- the kinds

    private void lance(ServerLevel level, AbstractSpell spell, @Nullable LivingEntity caster) {
        if (isDone()) {
            return;
        }
        Vector3f d = getDir();
        Vec3 from = position(), to = from.add(new Vec3(d.x, d.y, d.z).normalize().scale(LANCE_SPEED));
        BlockHitResult wall = level.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        Vec3 end = wall.getType() == HitResult.Type.MISS ? to : wall.getLocation();
        List<LivingEntity> line = new ArrayList<>();
        for (LivingEntity e : SpellUtil.enemiesAround(level, from.add(end).scale(.5), LANCE_SPEED + 2, caster, x -> !hit.contains(x.getId()))) {
            AABB box = e.getBoundingBox().inflate(.35);
            if (box.contains(from) || box.clip(from, end).isPresent()) {
                line.add(e);
            }
        }
        line.sort(Comparator.comparingDouble(e -> e.distanceToSqr(from)));
        for (LivingEntity e : line) {
            hit.add(e.getId());
            if (strike(spell, caster, e, damage)) {
                e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 1, false, false, true));
            }
            AuroraFx.lights(level, centre(e), 8, .3);
            sound(SoundEvents.AMETHYST_BLOCK_HIT, 1, 1.6f);
            if (hit.size() >= LANCE_PIERCE) {
                finish(centre(e), BURST);
                return;
            }
        }
        if (wall.getType() != HitResult.Type.MISS || tickCount >= LANCE_FLIGHT) {
            AuroraFx.lights(level, end, 10, .4);
            sound(SoundEvents.GLASS_BREAK, .8f, 1.6f);
            finish(end, BURST);
            return;
        }
        setPos(to);
    }

    private void wisp(ServerLevel level, AbstractSpell spell, @Nullable LivingEntity caster) {
        if (isDone()) {
            return;
        }
        Vec3 pos = position();
        if (tickCount < WISP_WAIT) {
            Vector3f d = getDir();
            setPos(pos.add(d.x * .12, .06, d.z * .12));
            return;
        }
        List<LivingEntity> foes = SpellUtil.enemiesAround(level, pos, WISP_RANGE, caster, x -> true);
        // Ranked from the caster, so that each wisp of a volley (by its index) picks its own enemy
        Vec3 from = caster != null ? caster.position() : pos;
        foes.sort(Comparator.comparingDouble(e -> e.distanceToSqr(from)));
        if (foes.isEmpty()) {
            if (tickCount >= getLife() - BURST) {
                finish(pos, BURST);
            }
            setPos(pos.add(0, .02 * Mth.sin(tickCount * .2f), 0));
            return;
        }
        LivingEntity prey = foes.get(index % foes.size());
        Vec3 to = centre(prey).subtract(pos);
        double speed = Math.min(1.2, .35 + (tickCount - WISP_WAIT) * .05);
        if (to.length() < 1.1) {
            if (strike(spell, caster, prey, damage)) {
                prey.addEffect(new MobEffectInstance(MobEffects.GLOWING, 100, 0, false, false, true));
            }
            AuroraFx.lights(level, centre(prey), 12, .4);
            sound(SoundEvents.AMETHYST_BLOCK_CHIME, 1.2f, 1.8f);
            finish(centre(prey), BURST);
            return;
        }
        setPos(pos.add(to.normalize().scale(Math.min(speed, to.length()))));
        if (tickCount >= getLife() - BURST) {
            finish(position(), BURST);
        }
    }

    private void curtain(ServerLevel level, AbstractSpell spell, @Nullable LivingEntity caster) {
        if (tickCount >= getLife() - 15) {
            return;
        }
        Vector3f d = getDir();
        Vec3 along = new Vec3(d.x, 0, d.z).normalize().scale(CURTAIN_LENGTH * .5);
        Vec3 a = position().subtract(along), b = position().add(along);
        if (tickCount % CURTAIN_EVERY == 0) {
            for (LivingEntity e : SpellUtil.enemiesAround(level, position().add(0, 1, 0), CURTAIN_LENGTH * .5 + 2, caster, x -> true)) {
                if (flatDistance(e.position(), a, b) < 1.1 && e.getY() < getY() + CURTAIN_HEIGHT && e.getY() > getY() - 2) {
                    if (strike(spell, caster, e, damage)) {
                        e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 25, 2, false, false, true));
                        e.addEffect(new MobEffectInstance(MobEffects.GLOWING, 60, 0, false, false, true));
                    }
                    AuroraFx.lights(level, centre(e), 6, .3);
                }
            }
        }
        AABB box = new AABB(a, b).inflate(1.2, 0, 1.2).expandTowards(0, CURTAIN_HEIGHT, 0);
        if (WindFx.deflect(level, box, position().add(0, 2, 0), caster, .2) > 0) {
            sound(SoundEvents.AMETHYST_BLOCK_HIT, 1.2f, 1.4f);
        }
        if (tickCount % 30 == 1) {
            sound(SoundEvents.BEACON_AMBIENT, 1.2f, 1.6f);
        }
    }

    private void glide(ServerLevel level, AbstractSpell spell, @Nullable LivingEntity caster) {
        Entity rider = bound(level);
        if (rider == null || tickCount > GLIDE_TICKS) {
            return;
        }
        Vec3 start = position(), now = rider.position();
        this.entityData.set(DATA_TARGET, now.subtract(start).toVector3f());
        rider.fallDistance = 0;
        for (LivingEntity e : SpellUtil.enemiesAround(level, start.add(now).scale(.5), start.distanceTo(now) * .5 + 2, caster, x -> !hit.contains(x.getId()))) {
            if (flatDistance(e.position(), start, now) < 1.6) {
                hit.add(e.getId());
                if (strike(spell, caster, e, damage)) {
                    e.setDeltaMovement(e.getDeltaMovement().add(0, .5, 0));
                    e.hurtMarked = true;
                    e.addEffect(new MobEffectInstance(MobEffects.GLOWING, 60, 0, false, false, true));
                }
                AuroraFx.lights(level, centre(e), 8, .3);
            }
        }
    }

    private void mantle(ServerLevel level, AbstractSpell spell, @Nullable LivingEntity caster, float radius) {
        keepUp(level, 15);
        Entity wearer = bound(level);
        if (tickCount >= getLife() - 15) {
            return;
        }
        if (wearer instanceof LivingEntity w && tickCount % 10 == 0) {
            w.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 15, 0, false, false, true));
            w.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 15, 0, false, false, true));
        }
        Vec3 c = position().add(0, 1, 0);
        if (WindFx.deflect(level, new AABB(c, c).inflate(radius + .5, 1.5, radius + .5), c, caster, .2) > 0) {
            sound(SoundEvents.AMETHYST_BLOCK_HIT, 1.2f, 1.6f);
        }
        if (tickCount % MANTLE_EVERY == 0) {
            for (LivingEntity e : enemies(level, c, radius + .6, caster, 2.5)) {
                if (strike(spell, caster, e, damage)) {
                    e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20, 1, false, false, true));
                }
            }
        }
    }

    private void blessing(ServerLevel level, @Nullable LivingEntity caster, float radius) {
        if (tickCount == 1) {
            sound(SoundEvents.BEACON_ACTIVATE, 1.5f, 1.4f);
        }
        if (tickCount == BLESSING_LAND) {
            for (LivingEntity a : allies(level, position(), radius, caster)) {
                a.heal(damage);
                a.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 1, false, true, true));
                a.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 300, 0, false, true, true));
                com.waylays.arcanes.entity.gem.GemScene.cleanse(a);
                AuroraFx.lights(level, centre(a), 12, .5);
            }
            sound(SoundEvents.AMETHYST_BLOCK_RESONATE, 2f, 1.3f);
            sound(SoundEvents.PLAYER_LEVELUP, .6f, 1.8f);
        }
    }

    private void spiral(ServerLevel level, AbstractSpell spell, @Nullable LivingEntity caster, float radius) {
        if (tickCount >= getLife() - 15) {
            return;
        }
        Vec3 c = position();
        for (LivingEntity e : enemies(level, c.add(0, 1, 0), radius + 1.5, caster, 6)) {
            Vec3 in = new Vec3(c.x - e.getX(), 0, c.z - e.getZ());
            double dist = in.length();
            if (dist > .4) {
                Vec3 round = new Vec3(-in.z, 0, in.x).scale(1 / dist);
                e.setDeltaMovement(e.getDeltaMovement().scale(.6).add(in.scale(.08 / dist)).add(round.scale(.22)).add(0, dist < radius * .6 ? .07 : 0, 0));
                e.hurtMarked = true;
            }
            if (tickCount % SPIRAL_EVERY == 0 && strike(spell, caster, e, damage)) {
                e.addEffect(new MobEffectInstance(MobEffects.GLOWING, 30, 0, false, false, true));
            }
        }
        if (tickCount % 20 == 0) {
            sound(SoundEvents.BEACON_AMBIENT, 1.2f, 1.9f);
            AuroraFx.lights(level, c.add(0, 2, 0), 10, radius * .4);
        }
    }

    private void serpent(ServerLevel level, AbstractSpell spell, @Nullable LivingEntity caster) {
        if (tickCount >= getLife() - 20) {
            return;
        }
        int since = tickCount - SERPENT_FIRST;
        if (since >= 0 && since % SERPENT_EVERY == 0) {
            Vec3 c = position();
            List<LivingEntity> foes = SpellUtil.enemiesAround(level, c, SERPENT_RANGE, caster, x -> true);
            foes.sort(Comparator.comparingDouble(e -> e.distanceToSqr(c)));
            if (!foes.isEmpty()) {
                struck(foes.get(0).position());
                sound(SoundEvents.PHANTOM_SWOOP, 1.5f, 1.4f);
            }
        }
        // The head reaches its prey half way through the dive
        if (since >= 0 && since % SERPENT_EVERY == SERPENT_DIVE / 2 && this.entityData.get(DATA_STRUCK) > (int) level.getGameTime() - SERPENT_DIVE) {
            Vec3 at = position().add(new Vec3(getTarget()));
            for (LivingEntity e : SpellUtil.enemiesAround(level, at.add(0, 1, 0), 2.8, caster, x -> true)) {
                if (strike(spell, caster, e, damage)) {
                    e.setDeltaMovement(e.getDeltaMovement().add(0, .7, 0));
                    e.hurtMarked = true;
                    e.addEffect(new MobEffectInstance(MobEffects.GLOWING, 80, 0, false, false, true));
                }
            }
            AuroraFx.lights(level, at.add(0, 1, 0), 30, 1.2);
            level.playSound(null, at.x, at.y, at.z, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 2f, .8f);
            level.playSound(null, at.x, at.y, at.z, SoundEvents.FIREWORK_ROCKET_LARGE_BLAST, SoundSource.PLAYERS, 1f, 1.4f);
        }
    }

    private void halo(ServerLevel level, AbstractSpell spell, @Nullable LivingEntity caster, float radius) {
        if (tickCount == 1) {
            sound(SoundEvents.BEACON_ACTIVATE, 2f, 1.2f);
        }
        if (tickCount < HALO_OPEN || tickCount >= getLife() - 15 || (tickCount - HALO_OPEN) % HALO_EVERY != 0) {
            return;
        }
        List<LivingEntity> foes = enemies(level, position().add(0, 1, 0), radius, caster, 6);
        Vec3 at;
        if (!foes.isEmpty()) {
            LivingEntity e = foes.get(level.random.nextInt(foes.size()));
            at = e.position();
            if (strike(spell, caster, e, damage)) {
                e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20, 1, false, false, true));
            }
        } else {
            double a = level.random.nextDouble() * Math.PI * 2, r = Math.sqrt(level.random.nextDouble()) * radius;
            at = position().add(Math.cos(a) * r, 0, Math.sin(a) * r);
        }
        struck(at);
        AuroraFx.lights(level, at.add(0, .5, 0), 8, .4);
        level.playSound(null, at.x, at.y, at.z, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.2f, 1.2f + level.random.nextFloat() * .6f);
    }

    private void night(ServerLevel level, AbstractSpell spell, @Nullable LivingEntity caster, float radius) {
        Vec3 c = position();
        if (tickCount == 1) {
            sound(SoundEvents.AMBIENT_CAVE.value(), 2f, .5f);
            sound(SoundEvents.BEACON_ACTIVATE, 2f, .6f);
        }
        for (int w = 0; w < 4; w++) {
            int start = NIGHT_FALL + w * NIGHT_WAVE;
            int local = tickCount - start;
            if (local == 0) {
                sound(SoundEvents.BEACON_AMBIENT, 2.5f, 1.2f + w * .15f);
            }
            if (local >= 0 && local < NIGHT_SWEEP && local % 4 == 0) {
                // The wave sweeps across the dome: from one side to the other, turned a quarter each time
                float a = w * Mth.HALF_PI + .4f;
                Vec3 across = new Vec3(Mth.cos(a), 0, Mth.sin(a));
                double x = -radius + 2 * radius * local / (double) NIGHT_SWEEP;
                for (LivingEntity e : enemies(level, c.add(0, 1, 0), radius, caster, 8)) {
                    double along = (e.getX() - c.x) * across.x + (e.getZ() - c.z) * across.z;
                    if (Math.abs(along - x) < 2.5 && !hit.contains(e.getId() * 8 + w)) {
                        hit.add(e.getId() * 8 + w);
                        if (strike(spell, caster, e, damage * .3f)) {
                            e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 1, false, false, true));
                            e.addEffect(new MobEffectInstance(MobEffects.GLOWING, 80, 0, false, false, true));
                        }
                        AuroraFx.lights(level, centre(e), 10, .4);
                    }
                }
            }
        }
        if (tickCount % 20 == 0 && tickCount < NIGHT_GATHER) {
            for (LivingEntity a : allies(level, c, radius, caster)) {
                a.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 30, 0, false, false, true));
            }
        }
        if (tickCount == NIGHT_GATHER) {
            for (LivingEntity e : enemies(level, c.add(0, 1, 0), radius, caster, 8)) {
                if (strike(spell, caster, e, damage)) {
                    e.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 25, 0, false, false, true));
                    e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 2, false, false, true));
                }
            }
            for (LivingEntity a : allies(level, c, radius, caster)) {
                a.heal(damage * .4f);
            }
            AuroraFx.lights(level, c.add(0, 3, 0), 80, 3);
            SpellUtil.cameraShake(level, c, 20, radius * 2);
            sound(SoundEvents.TOTEM_USE, 1.6f, 1.1f);
            sound(SoundEvents.BELL_RESONATE, 2.5f, .7f);
        }
    }
}
