package com.waylays.arcanes.entity.gem;

import com.waylays.arcanes.entity.SpellEffectEntity;
import com.waylays.arcanes.registry.ModEffects;
import com.waylays.arcanes.registry.ModEntities;
import com.waylays.arcanes.spells.SpellUtil;
import com.waylays.arcanes.spells.gem.Gem;
import com.waylays.arcanes.spells.gem.GemFx;
import com.waylays.arcanes.spells.wind.WindFx;
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
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
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
 * What Gem Magic's newer spells leave in the world, one kind each: ruby shards in flight, the ruby nova, the ruby meteor
 * and its molten pool; the lapis shooting star, the celestial vault, the lapis mirror; the diamond lance, diamond dust,
 * the diamond bastion; amethyst spikes, the budding amethyst, the amethyst chimes; the emerald lifeline, the emerald
 * aegis, the emerald tree, emerald thorns. Its renderer draws the kind's model, and it does the kind's work as it
 * ticks.
 */
public class GemScene extends SpellEffectEntity {
    public enum Kind { SHARD, NOVA, METEOR, STAR, VAULT, MIRROR, LANCE, DUST, BASTION, SPIKES, BUDDING, CHIMES, LIFELINE, AEGIS, TREE, THORNS }

    /** Ruby shards fly this fast for this long, each through this many enemies, and shatter this long after they stop. */
    public static final double SHARD_SPEED = 1.3;
    public static final int SHARD_FLIGHT = 18, SHARD_PIERCE = 2, SHARD_BURST = 10;
    /** The nova's ruby rises and gathers light this long, then looses its rays; they reach this far. */
    public static final int NOVA_CHARGE = 22;
    /** The meteor falls for this long from this high; its molten pool burns every {@link #POOL_EVERY} ticks. */
    public static final int METEOR_FALL = 30, POOL_EVERY = 10;
    public static final float METEOR_HEIGHT = 30;
    /** The shooting star flies this fast, turning toward its prey, for this long. */
    public static final double STAR_SPEED = 1.0;
    public static final int STAR_FLIGHT = 30, STAR_BURST = 12;
    /** The vault hangs this high and lets a star fall every {@link #VAULT_EVERY} ticks. */
    public static final float VAULT_HEIGHT = 8;
    public static final int VAULT_OPEN = 12, VAULT_EVERY = 8;
    /** The mirror seals the nearest enemy this far off every {@link #MIRROR_EVERY} ticks, and turns shots this close to its master. */
    public static final double MIRROR_RANGE = 8, MIRROR_GUARD = 3;
    public static final int MIRROR_EVERY = 40;
    /** The lance reaches this far, in this many ticks. */
    public static final float LANCE_RANGE = 22;
    public static final int LANCE_FLIGHT = 4;
    /** The diamond dust cuts every {@link #DUST_EVERY} ticks. */
    public static final int DUST_EVERY = 10;
    /** The bastion guards its side this far round it, and shatters when its life runs out. */
    public static final int BASTION_SHATTER = 14;
    /** Amethyst clusters burst out one a tick, a block apart. */
    public static final int SPIKES = 12;
    /** The budding amethyst grows a stage every {@link #BUD_STAGE} ticks, three stages, then shatters. */
    public static final int BUD_STAGE = 20;
    /** The chimes ring every {@link #CHIME_EVERY} ticks, at up to {@link #CHIME_TARGETS} enemies this far off. */
    public static final int CHIME_EVERY = 20, CHIME_TARGETS = 3;
    public static final double CHIME_RANGE = 10;
    /** The lifeline heals every {@link #LIFELINE_EVERY} ticks. */
    public static final int LIFELINE_EVERY = 10;
    /** The emerald tree grows this long, then heals its side round it every {@link #TREE_EVERY} ticks. */
    public static final int TREE_GROW = 20, TREE_EVERY = 40;
    /** Emerald thorns burst out of the ground this long after they are called. */
    public static final int THORNS_RISE = 6;

    private static final EntityDataAccessor<Integer> DATA_KIND = SynchedEntityData.defineId(GemScene.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DATA_SIZE = SynchedEntityData.defineId(GemScene.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> DATA_LIFE = SynchedEntityData.defineId(GemScene.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Vector3f> DATA_DIR = SynchedEntityData.defineId(GemScene.class, EntityDataSerializers.VECTOR3);
    /** Where it last struck (relative to it) and when (game tick). */
    private static final EntityDataAccessor<Vector3f> DATA_TARGET = SynchedEntityData.defineId(GemScene.class, EntityDataSerializers.VECTOR3);
    private static final EntityDataAccessor<Integer> DATA_STRUCK = SynchedEntityData.defineId(GemScene.class, EntityDataSerializers.INT);
    /** A shard or a star that has stopped (and bursts there). */
    private static final EntityDataAccessor<Boolean> DATA_DONE = SynchedEntityData.defineId(GemScene.class, EntityDataSerializers.BOOLEAN);

    @Nullable
    private String spellId;
    /** What it follows (a mirror, chimes, a lifeline or an aegis its caster), or hunts (a star its prey), or heals (a lifeline its patient). */
    private int bound = -1;
    private int patient = -1;
    private final Set<Integer> hit = new HashSet<>();
    private int strikes;
    private float dealt;

    public GemScene(EntityType<? extends GemScene> entityType, Level level) {
        super(entityType, level);
    }

    /** A scene of {@code kind} at {@code at}, facing {@code dir}, {@code size} (its radius or reach), lasting {@code life} ticks, striking for {@code damage}. */
    public static GemScene spawn(Level level, @Nullable LivingEntity caster, AbstractSpell spell, Kind kind, Vec3 at, Vector3f dir, float size, int life,
                                 float damage) {
        GemScene s = new GemScene(ModEntities.GEM_SCENE.get(), level);
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

    /** Makes it follow (or, for a star, hunt) {@code e}. */
    public GemScene follow(@Nullable Entity e) {
        this.bound = e == null ? -1 : e.getId();
        return this;
    }

    /** The one a lifeline heals. */
    public GemScene heal(LivingEntity e) {
        this.patient = e.getId();
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
        float before = e.getHealth();
        boolean ok = DamageSources.applyDamage(e, amount, SpellUtil.damageSource(spell, this, caster));
        if (ok) {
            strikes++;
            dealt += Math.max(0, before - Math.max(0, e.getHealth()));
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

    /** The caster's side within {@code radius} of {@code c}: the caster, its summons, its friends (players, if it has none). */
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

    @Nullable
    private LivingEntity nearestSeen(ServerLevel level, Vec3 from, double range, @Nullable LivingEntity caster) {
        List<LivingEntity> foes = SpellUtil.enemiesAround(level, from, range, caster, x -> true);
        foes.sort(Comparator.comparingDouble(e -> e.distanceToSqr(from)));
        for (LivingEntity e : foes) {
            if (seen(level, from, e)) {
                return e;
            }
        }
        return null;
    }

    private boolean seen(ServerLevel level, Vec3 from, LivingEntity e) {
        return level.clip(new ClipContext(from, e.getEyePosition(), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this)).getType() == HitResult.Type.MISS;
    }

    private static Vec3 centre(LivingEntity e) {
        return e.position().add(0, e.getBbHeight() * .55, 0);
    }

    @Nullable
    private Entity bound(ServerLevel level) {
        return bound >= 0 ? level.getEntity(bound) : null;
    }

    /** Keeps it on what it follows; if that is gone, it ends soon. */
    private void keepUp(ServerLevel level, int linger) {
        Entity follow = bound(level);
        if (follow != null && follow.isAlive()) {
            setPos(follow.getX(), follow.getY(), follow.getZ());
            Vec3 look = Vec3.directionFromRotation(0, follow.getYRot());
            this.entityData.set(DATA_DIR, look.toVector3f());
        } else if (bound >= 0 && tickCount < getLife() - linger) {
            this.entityData.set(DATA_LIFE, tickCount + linger);
        }
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
            case SHARD -> shard(level, spell, caster);
            case NOVA -> nova(level, spell, caster, size);
            case METEOR -> meteor(level, spell, caster, size);
            case STAR -> star(level, spell, caster, size);
            case VAULT -> vault(level, spell, caster, size);
            case MIRROR -> mirror(level, spell, caster);
            case LANCE -> lance(level, spell, caster);
            case DUST -> dust(level, spell, caster, size);
            case BASTION -> bastion(level, spell, caster, size);
            case SPIKES -> spikes(level, spell, caster);
            case BUDDING -> budding(level, spell, caster, size);
            case CHIMES -> chimes(level, spell, caster);
            case LIFELINE -> lifeline(level, caster);
            case AEGIS -> keepUp(level, 10);
            case TREE -> tree(level, caster, size);
            case THORNS -> thorns(level, spell, caster, size);
        }
    }

    // ---------------------------------------------------------------- ruby

    private void shard(ServerLevel level, AbstractSpell spell, @Nullable LivingEntity caster) {
        if (isDone()) {
            return;
        }
        Vector3f d = getDir();
        Vec3 from = position(), to = from.add(new Vec3(d.x, d.y, d.z).normalize().scale(SHARD_SPEED));
        BlockHitResult wall = level.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        Vec3 end = wall.getType() == HitResult.Type.MISS ? to : wall.getLocation();
        List<LivingEntity> line = new ArrayList<>();
        for (LivingEntity e : SpellUtil.enemiesAround(level, from.add(end).scale(.5), SHARD_SPEED + 2, caster, x -> !hit.contains(x.getId()))) {
            AABB box = e.getBoundingBox().inflate(.3);
            if (box.contains(from) || box.clip(from, end).isPresent()) {
                line.add(e);
            }
        }
        line.sort(Comparator.comparingDouble(e -> e.distanceToSqr(from)));
        for (LivingEntity e : line) {
            hit.add(e.getId());
            if (strike(spell, caster, e, damage)) {
                e.igniteForSeconds(2);
            }
            GemFx.glitter(level, centre(e), Gem.RUBY, 8, .3);
            sound(SoundEvents.AMETHYST_BLOCK_HIT, 1f, .7f);
            if (hit.size() >= SHARD_PIERCE) {
                GemFx.shatter(level, centre(e), Gem.RUBY, .5f, 6);
                finish(centre(e), SHARD_BURST);
                return;
            }
        }
        if (wall.getType() != HitResult.Type.MISS || tickCount >= SHARD_FLIGHT) {
            GemFx.shatter(level, end, Gem.RUBY, .4f, 5);
            finish(end, SHARD_BURST);
            return;
        }
        setPos(to);
    }

    private void nova(ServerLevel level, AbstractSpell spell, @Nullable LivingEntity caster, float radius) {
        if (tickCount == 1) {
            sound(SoundEvents.BEACON_POWER_SELECT, 1.5f, 1.4f);
        }
        if (tickCount % 6 == 0 && tickCount < NOVA_CHARGE) {
            GemFx.chime(level, position().add(0, 3, 0), .8f, .6f + tickCount * .05f);
        }
        if (tickCount == NOVA_CHARGE) {
            Vec3 c = position();
            for (LivingEntity e : enemies(level, c.add(0, 1, 0), radius, caster, 4)) {
                if (strike(spell, caster, e, damage)) {
                    e.igniteForSeconds(4);
                    Vec3 out = new Vec3(e.getX() - c.x, 0, e.getZ() - c.z);
                    if (out.lengthSqr() > 1.0E-4) {
                        e.setDeltaMovement(e.getDeltaMovement().add(out.normalize().scale(.6)).add(0, .3, 0));
                        e.hurtMarked = true;
                    }
                }
            }
            GemFx.shatter(level, c.add(0, 3, 0), Gem.RUBY, 1.4f, 14);
            SpellUtil.particles(level, net.minecraft.core.particles.ParticleTypes.FLAME, c.x, c.y + .3, c.z, 40, radius * .5, .1, radius * .5, .05);
            SpellUtil.cameraShake(level, c, 10, radius * 2);
            sound(SoundEvents.BLAZE_SHOOT, 2f, .6f);
            sound(SoundEvents.FIRECHARGE_USE, 2f, .8f);
        }
    }

    private void meteor(ServerLevel level, AbstractSpell spell, @Nullable LivingEntity caster, float radius) {
        Vec3 c = position();
        if (tickCount == 1) {
            sound(SoundEvents.FIRECHARGE_USE, 2f, .5f);
        }
        if (tickCount == METEOR_FALL - 12) {
            sound(SoundEvents.GHAST_WARN, 1.2f, 1.6f);
        }
        if (tickCount == METEOR_FALL) {
            for (LivingEntity e : enemies(level, c.add(0, 1, 0), radius, caster, 4)) {
                if (strike(spell, caster, e, damage)) {
                    e.igniteForSeconds(5);
                    Vec3 out = new Vec3(e.getX() - c.x, 0, e.getZ() - c.z);
                    e.setDeltaMovement(e.getDeltaMovement().add(out.lengthSqr() > 1.0E-4 ? out.normalize().scale(.9) : Vec3.ZERO).add(0, .55, 0));
                    e.hurtMarked = true;
                }
            }
            GemFx.shatter(level, c.add(0, 1, 0), Gem.RUBY, 2.5f, 24);
            SpellUtil.particles(level, net.minecraft.core.particles.ParticleTypes.EXPLOSION, c.x, c.y + .5, c.z, 3, 1, .3, 1, 0);
            SpellUtil.particles(level, net.minecraft.core.particles.ParticleTypes.LAVA, c.x, c.y + .5, c.z, 20, radius * .4, .2, radius * .4, .1);
            SpellUtil.cameraShake(level, c, 20, radius * 4);
            level.playSound(null, c.x, c.y, c.z, SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 2f, .7f);
            sound(SoundEvents.GLASS_BREAK, 2f, .5f);
        }
        if (tickCount > METEOR_FALL && tickCount < getLife() - 15 && tickCount % POOL_EVERY == 0) {
            for (LivingEntity e : enemies(level, c.add(0, .5, 0), radius * .8, caster, 2)) {
                if (strike(spell, caster, e, damage * .12f)) {
                    e.igniteForSeconds(3);
                }
            }
            SpellUtil.particles(level, net.minecraft.core.particles.ParticleTypes.LAVA, c.x, c.y + .2, c.z, 3, radius * .4, 0, radius * .4, 0);
            if (tickCount % 30 == 0) {
                sound(SoundEvents.LAVA_AMBIENT, 1.2f, 1.3f);
            }
        }
    }

    // ---------------------------------------------------------------- lapis

    private void star(ServerLevel level, AbstractSpell spell, @Nullable LivingEntity caster, float blast) {
        if (isDone()) {
            return;
        }
        Vector3f d = getDir();
        Vec3 heading = new Vec3(d.x, d.y, d.z).normalize();
        Entity prey = bound(level);
        if (prey instanceof LivingEntity target && target.isAlive()) {
            Vec3 want = centre(target).subtract(position()).normalize();
            heading = heading.scale(.82).add(want.scale(.18)).normalize();
            this.entityData.set(DATA_DIR, heading.toVector3f());
        }
        Vec3 from = position(), to = from.add(heading.scale(STAR_SPEED));
        BlockHitResult wall = level.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        Vec3 end = wall.getType() == HitResult.Type.MISS ? to : wall.getLocation();
        boolean touched = false;
        for (LivingEntity e : SpellUtil.enemiesAround(level, from.add(end).scale(.5), STAR_SPEED + 2, caster, x -> true)) {
            AABB box = e.getBoundingBox().inflate(.4);
            if (box.contains(from) || box.clip(from, end).isPresent()) {
                touched = true;
                break;
            }
        }
        if (touched || wall.getType() != HitResult.Type.MISS || tickCount >= STAR_FLIGHT) {
            for (LivingEntity e : SpellUtil.enemiesAround(level, end, blast, caster, x -> true)) {
                if (strike(spell, caster, e, damage)) {
                    e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1, false, false, true));
                }
            }
            GemFx.shatter(level, end, Gem.LAPIS, .8f, 8);
            GemFx.chime(level, end, 1.2f, 1.6f);
            finish(end, STAR_BURST);
            return;
        }
        setPos(to);
        if (tickCount % 2 == 0) {
            GemFx.glitter(level, position(), Gem.LAPIS, 2, .1);
        }
    }

    private void vault(ServerLevel level, AbstractSpell spell, @Nullable LivingEntity caster, float radius) {
        Vec3 c = position();
        if (tickCount == 1) {
            sound(SoundEvents.BEACON_ACTIVATE, 1.5f, 1.5f);
        }
        if (tickCount >= VAULT_OPEN && tickCount < getLife() - 12 && (tickCount - VAULT_OPEN) % VAULT_EVERY == 0) {
            List<LivingEntity> foes = enemies(level, c.add(0, 1, 0), radius, caster, 6);
            Vec3 at;
            if (!foes.isEmpty()) {
                LivingEntity e = foes.get(level.random.nextInt(foes.size()));
                at = centre(e);
                if (strike(spell, caster, e, damage)) {
                    e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 0, false, false, true));
                }
            } else {
                double a = level.random.nextDouble() * Math.PI * 2, r = Math.sqrt(level.random.nextDouble()) * radius;
                at = c.add(Math.cos(a) * r, .2, Math.sin(a) * r);
            }
            struck(at);
            GemFx.glitter(level, at, Gem.LAPIS, 8, .4);
            GemFx.chime(level, at, .9f, 1.4f + level.random.nextFloat() * .5f);
        }
    }

    private void mirror(ServerLevel level, AbstractSpell spell, @Nullable LivingEntity caster) {
        keepUp(level, 12);
        if (tickCount >= getLife() - 12) {
            return;
        }
        Vec3 c = position().add(0, 1.2, 0);
        if (WindFx.deflect(level, new AABB(c, c).inflate(MIRROR_GUARD), c, caster, .2) > 0) {
            sound(SoundEvents.AMETHYST_BLOCK_HIT, 1.5f, 1.4f);
            GemFx.glitter(level, c, Gem.LAPIS, 8, 1);
        }
        if (tickCount >= 10 && tickCount % MIRROR_EVERY == 10) {
            LivingEntity prey = nearestSeen(level, c, MIRROR_RANGE, caster);
            if (prey != null) {
                if (strike(spell, caster, prey, damage)) {
                    prey.addEffect(new MobEffectInstance(ModEffects.SILENCE, 40, 0, false, true, true));
                    prey.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 2, false, false, true));
                }
                struck(centre(prey));
                GemFx.glitter(level, centre(prey), Gem.LAPIS, 10, .4);
                sound(SoundEvents.CHAIN_PLACE, 1.4f, 1.2f);
                GemFx.chime(level, c, 1f, .9f);
            }
        }
    }

    // ---------------------------------------------------------------- diamond

    private void lance(ServerLevel level, AbstractSpell spell, @Nullable LivingEntity caster) {
        if (tickCount != 1) {
            return;
        }
        Vector3f d = getDir();
        Vec3 from = position(), dir = new Vec3(d.x, d.y, d.z).normalize();
        BlockHitResult wall = level.clip(new ClipContext(from, from.add(dir.scale(LANCE_RANGE)), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        Vec3 end = wall.getType() == HitResult.Type.MISS ? from.add(dir.scale(LANCE_RANGE)) : wall.getLocation();
        this.entityData.set(DATA_SIZE, (float) from.distanceTo(end));
        for (LivingEntity e : SpellUtil.enemiesAround(level, from.add(end).scale(.5), from.distanceTo(end) * .5 + 2, caster, x -> true)) {
            AABB box = e.getBoundingBox().inflate(.4);
            if (box.contains(from) || box.clip(from, end).isPresent()) {
                if (strike(spell, caster, e, damage)) {
                    e.setDeltaMovement(e.getDeltaMovement().add(dir.multiply(.4, 0, .4)));
                    e.hurtMarked = true;
                }
                GemFx.glitter(level, centre(e), Gem.DIAMOND, 10, .3);
            }
        }
        struck(end);
        GemFx.shatter(level, end, Gem.DIAMOND, .6f, 6);
        level.playSound(null, from.x, from.y, from.z, SoundEvents.TRIDENT_THROW, SoundSource.PLAYERS, 1.5f, 1.4f);
        GemFx.chime(level, from, 1.2f, 1.8f);
    }

    private void dust(ServerLevel level, AbstractSpell spell, @Nullable LivingEntity caster, float radius) {
        Vec3 c = position();
        if (tickCount > 6 && tickCount < getLife() - 12 && tickCount % DUST_EVERY == 0) {
            for (LivingEntity e : enemies(level, c.add(0, 1, 0), radius, caster, 3)) {
                if (strike(spell, caster, e, damage)) {
                    e.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 30, 0, false, false, true));
                    e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20, 0, false, false, true));
                }
            }
            GemFx.glitter(level, c.add(0, 1, 0), Gem.DIAMOND, 12, radius * .5);
            if (tickCount % 20 == 0) {
                GemFx.chime(level, c, .6f, 1.8f + level.random.nextFloat() * .3f);
            }
        }
    }

    private void bastion(ServerLevel level, AbstractSpell spell, @Nullable LivingEntity caster, float radius) {
        Vec3 c = position();
        int shatter = getLife() - BASTION_SHATTER;
        if (tickCount == 1) {
            sound(SoundEvents.AMETHYST_CLUSTER_PLACE, 2f, .5f);
            SpellUtil.cameraShake(level, c, 8, 12);
        }
        if (tickCount < shatter) {
            if (tickCount % 10 == 0) {
                for (LivingEntity a : allies(level, c, radius, caster)) {
                    a.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 15, 1, false, false, true));
                }
            }
            if (WindFx.deflect(level, new AABB(c, c).inflate(radius, 3, radius).move(0, 1.5, 0), c.add(0, 1.5, 0), caster, .2) > 0) {
                sound(SoundEvents.AMETHYST_BLOCK_HIT, 1.5f, 1.6f);
            }
            if (tickCount % 40 == 0) {
                GemFx.chime(level, c.add(0, 2, 0), .8f, 1.2f);
            }
        }
        if (tickCount == shatter) {
            for (LivingEntity e : enemies(level, c.add(0, 1, 0), radius, caster, 4)) {
                strike(spell, caster, e, damage);
            }
            GemFx.shatter(level, c.add(0, 1.5, 0), Gem.DIAMOND, 2.5f, 24);
            SpellUtil.cameraShake(level, c, 10, 16);
        }
    }

    // ---------------------------------------------------------------- amethyst

    private void spikes(ServerLevel level, AbstractSpell spell, @Nullable LivingEntity caster) {
        int k = tickCount - 1;
        if (k < 0 || k >= SPIKES) {
            return;
        }
        Vector3f d = getDir();
        Vec3 at = position().add(d.x * (k + 1.2), 0, d.z * (k + 1.2));
        Vec3 ground = SpellUtil.groundAt(level, at, 3, 1);
        for (LivingEntity e : SpellUtil.enemiesAround(level, ground.add(0, .8, 0), 1.4, caster, x -> !hit.contains(x.getId()))) {
            hit.add(e.getId());
            if (strike(spell, caster, e, damage)) {
                e.setDeltaMovement(e.getDeltaMovement().multiply(.4, 0, .4).add(0, .55, 0));
                e.hurtMarked = true;
            }
        }
        GemFx.glitter(level, ground.add(0, .6, 0), Gem.AMETHYST, 5, .3);
        if (k % 2 == 0) {
            level.playSound(null, ground.x, ground.y, ground.z, SoundEvents.AMETHYST_CLUSTER_PLACE, SoundSource.PLAYERS, 1.2f, .8f + k * .05f);
        }
    }

    private void budding(ServerLevel level, AbstractSpell spell, @Nullable LivingEntity caster, float radius) {
        Vec3 c = position();
        for (int stage = 1; stage <= 3; stage++) {
            if (tickCount == stage * BUD_STAGE) {
                for (LivingEntity e : enemies(level, c.add(0, 1, 0), radius * .7, caster, 3)) {
                    if (strike(spell, caster, e, damage * .25f)) {
                        e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 1, false, false, true));
                    }
                }
                GemFx.chime(level, c.add(0, 1, 0), 1.5f, .6f + stage * .25f);
                level.playSound(null, c.x, c.y, c.z, SoundEvents.AMETHYST_CLUSTER_PLACE, SoundSource.PLAYERS, 1.5f, .6f + stage * .2f);
                GemFx.glitter(level, c.add(0, 1, 0), Gem.AMETHYST, 10 + 6 * stage, .4 * stage);
            }
        }
        if (tickCount == 4 * BUD_STAGE) {
            for (LivingEntity e : enemies(level, c.add(0, 1, 0), radius, caster, 4)) {
                if (strike(spell, caster, e, damage)) {
                    Vec3 out = new Vec3(e.getX() - c.x, 0, e.getZ() - c.z);
                    e.setDeltaMovement(e.getDeltaMovement().add(out.lengthSqr() > 1.0E-4 ? out.normalize().scale(.7) : Vec3.ZERO).add(0, .4, 0));
                    e.hurtMarked = true;
                }
            }
            GemFx.shatter(level, c.add(0, 1.2, 0), Gem.AMETHYST, 2.2f, 22);
            SpellUtil.cameraShake(level, c, 10, 14);
            sound(SoundEvents.AMETHYST_CLUSTER_BREAK, 2.5f, .5f);
        }
    }

    private void chimes(ServerLevel level, AbstractSpell spell, @Nullable LivingEntity caster) {
        keepUp(level, 12);
        if (tickCount < 10 || tickCount >= getLife() - 12 || tickCount % CHIME_EVERY != 0) {
            return;
        }
        Vec3 c = position().add(0, 2.6, 0);
        List<LivingEntity> foes = SpellUtil.enemiesAround(level, c, CHIME_RANGE, caster, x -> true);
        foes.sort(Comparator.comparingDouble(e -> e.distanceToSqr(c)));
        int rung = 0;
        for (LivingEntity e : foes) {
            if (rung >= CHIME_TARGETS) {
                break;
            }
            if (!seen(level, c, e)) {
                continue;
            }
            if (strike(spell, caster, e, damage)) {
                e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20, 1, false, false, true));
            }
            if (rung == 0) {
                struck(centre(e));
            }
            GemFx.glitter(level, centre(e), Gem.AMETHYST, 6, .3);
            rung++;
        }
        // Enemy shots near its master shatter on the ringing
        for (Projectile shot : level.getEntitiesOfClass(Projectile.class, new AABB(position(), position()).inflate(4, 3, 4).move(0, 1, 0))) {
            Entity by = shot.getOwner();
            if (by != null && !SpellUtil.isAlly(caster, by)) {
                GemFx.glitter(level, shot.position(), Gem.AMETHYST, 6, .2);
                shot.discard();
            }
        }
        GemFx.chime(level, c, 1.4f, .8f + (tickCount / CHIME_EVERY % 4) * .15f);
    }

    // ---------------------------------------------------------------- emerald

    private void lifeline(ServerLevel level, @Nullable LivingEntity caster) {
        keepUp(level, 8);
        Entity p = patient >= 0 ? level.getEntity(patient) : null;
        if (!(p instanceof LivingEntity patientEntity) || !patientEntity.isAlive()) {
            if (tickCount < getLife() - 8) {
                this.entityData.set(DATA_LIFE, tickCount + 8);
            }
            return;
        }
        // The beam runs to the patient: where it is, every tick
        this.entityData.set(DATA_TARGET, centre(patientEntity).subtract(position()).toVector3f());
        if (tickCount == 1) {
            cleanse(patientEntity);
        }
        if (tickCount < getLife() - 8 && tickCount % LIFELINE_EVERY == 0) {
            patientEntity.heal(damage);
            this.entityData.set(DATA_STRUCK, (int) level.getGameTime());
            GemFx.glitter(level, centre(patientEntity), Gem.EMERALD, 6, .4);
            if (tickCount % 20 == 0) {
                GemFx.chime(level, centre(patientEntity), .7f, 1.6f);
            }
        }
    }

    /** Takes one harmful effect off {@code e}. */
    public static void cleanse(LivingEntity e) {
        for (MobEffectInstance effect : new ArrayList<>(e.getActiveEffects())) {
            Holder<MobEffect> holder = effect.getEffect();
            if (holder.value().getCategory() == MobEffectCategory.HARMFUL) {
                e.removeEffect(holder);
                return;
            }
        }
    }

    private void tree(ServerLevel level, @Nullable LivingEntity caster, float radius) {
        Vec3 c = position();
        if (tickCount == 1) {
            sound(SoundEvents.AMETHYST_CLUSTER_PLACE, 1.5f, 1.2f);
        }
        if (tickCount >= TREE_GROW && tickCount < getLife() - 15 && (tickCount - TREE_GROW) % TREE_EVERY == 0) {
            for (LivingEntity a : allies(level, c, radius, caster)) {
                a.heal(damage);
                GemFx.glitter(level, centre(a), Gem.EMERALD, 6, .3);
            }
            struck(c.add(0, 2.2, 0));
            GemFx.chime(level, c.add(0, 2, 0), 1.2f, 1.2f);
            GemFx.glitter(level, c.add(0, 2.4, 0), Gem.EMERALD, 14, radius * .4);
        }
    }

    private void thorns(ServerLevel level, AbstractSpell spell, @Nullable LivingEntity caster, float radius) {
        if (tickCount != THORNS_RISE) {
            return;
        }
        Vec3 c = position();
        for (LivingEntity e : SpellUtil.enemiesAround(level, c.add(0, 1, 0), radius, caster, x -> true)) {
            if (strike(spell, caster, e, damage)) {
                e.setDeltaMovement(e.getDeltaMovement().multiply(.3, 0, .3).add(0, .45, 0));
                e.hurtMarked = true;
                e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 1, false, false, true));
            }
        }
        if (caster != null && dealt > 0) {
            caster.heal(dealt * .4f);
            GemFx.glitter(level, centre(caster), Gem.EMERALD, 10, .4);
        }
        GemFx.shatter(level, c.add(0, .8, 0), Gem.EMERALD, .9f, 8);
        level.playSound(null, c.x, c.y, c.z, SoundEvents.AMETHYST_CLUSTER_PLACE, SoundSource.PLAYERS, 1.5f, 1.3f);
        level.playSound(null, c.x, c.y, c.z, SoundEvents.SWEET_BERRY_BUSH_BREAK, SoundSource.PLAYERS, 1.5f, .8f);
    }
}
