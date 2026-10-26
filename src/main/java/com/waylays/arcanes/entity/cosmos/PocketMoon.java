package com.waylays.arcanes.entity.cosmos;

import com.waylays.arcanes.entity.SpellEffectEntity;
import com.waylays.arcanes.registry.ModEntities;
import com.waylays.arcanes.spells.SpellUtil;
import com.waylays.arcanes.spells.cosmos.Cosmos;
import com.waylays.arcanes.spells.cosmos.CosmosFx;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.damage.DamageSources;
import io.redspace.ironsspellbooks.particle.BlastwaveParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A little moon in orbit round its caster, showing its phases as it goes round (lit by a sun that does not move). It
 * strikes what it passes through and throws it aside, and its tide draws foes in toward its caster, into its path.
 * Cast again, the caster throws it where they look like a boomerang: it strikes all along the way, there and back,
 * and goes back into orbit.
 */
public class PocketMoon extends SpellEffectEntity {
    public static final float ORBIT = 2.3f;
    public static final float SPIN = .11f;
    public static final float TIDE_RANGE = 8;
    public static final float THROW_RANGE = 18;
    public static final float SIZE = .55f;

    public enum State { ORBIT, OUT, BACK }

    private static final EntityDataAccessor<Integer> DATA_CASTER = SynchedEntityData.defineId(PocketMoon.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_STATE = SynchedEntityData.defineId(PocketMoon.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_LIFE = SynchedEntityData.defineId(PocketMoon.class, EntityDataSerializers.INT);

    @Nullable
    private String spellId;
    private float angle;
    private Vec3 goal = Vec3.ZERO;
    private int autoThrow = 60;
    private final Map<Integer, Integer> struck = new HashMap<>();
    /** Client: where it has been lately, for its streak when thrown. */
    public final Deque<Vec3> trail = new ArrayDeque<>();

    public PocketMoon(EntityType<? extends PocketMoon> entityType, Level level) {
        super(entityType, level);
    }

    public static PocketMoon raise(Level level, LivingEntity caster, AbstractSpell spell, int life, float damage) {
        PocketMoon old = of(level, caster);
        if (old != null) {
            old.discard();
        }
        PocketMoon moon = new PocketMoon(ModEntities.POCKET_MOON.get(), level);
        moon.setOwner(caster);
        moon.spellId = spell.getSpellId();
        moon.damage = damage;
        moon.angle = caster.getYRot() * Mth.DEG_TO_RAD;
        Vec3 at = moon.orbitPoint(caster);
        moon.moveTo(at.x, at.y, at.z, 0, 0);
        moon.entityData.set(DATA_CASTER, caster.getId());
        moon.entityData.set(DATA_LIFE, life);
        level.addFreshEntity(moon);
        CosmosFx.hum(level, caster.position(), 1, 1.2f);
        CosmosFx.stars(level, at, 20, .5);
        return moon;
    }

    @Nullable
    public static PocketMoon of(Level level, LivingEntity caster) {
        List<PocketMoon> moons = level.getEntitiesOfClass(PocketMoon.class, caster.getBoundingBox().inflate(48), m -> caster.getUUID().equals(m.getOwnerUUID()));
        return moons.isEmpty() ? null : moons.get(0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_CASTER, -1);
        builder.define(DATA_STATE, 0);
        builder.define(DATA_LIFE, 400);
    }

    @Nullable
    public Entity getCaster() {
        return level().getEntity(this.entityData.get(DATA_CASTER));
    }

    public State getState() {
        return State.values()[Mth.clamp(this.entityData.get(DATA_STATE), 0, 2)];
    }

    public int getLife() {
        return this.entityData.get(DATA_LIFE);
    }

    private Vec3 orbitPoint(Entity caster) {
        return caster.position().add(Mth.cos(angle) * ORBIT, caster.getBbHeight() * .6 + .25 * Mth.sin(angle * 2), Mth.sin(angle) * ORBIT);
    }

    /** Throws it where {@code caster} looks, like a boomerang. */
    public void fling(LivingEntity caster) {
        if (getState() != State.ORBIT) {
            return;
        }
        Vec3 eye = caster.getEyePosition();
        HitResult hit = level().clip(new ClipContext(eye, eye.add(caster.getLookAngle().scale(THROW_RANGE)), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, caster));
        goal = hit.getLocation().subtract(caster.getLookAngle().scale(.5));
        this.entityData.set(DATA_STATE, State.OUT.ordinal());
        struck.clear();
        CosmosFx.sound(level(), position(), SoundEvents.ENDER_PEARL_THROW, 1.2f, .6f);
    }

    /** Throws it at {@code target} (a creature casting it has no gaze to follow). */
    private void flingAt(LivingEntity target) {
        goal = target.getBoundingBox().getCenter();
        this.entityData.set(DATA_STATE, State.OUT.ordinal());
        struck.clear();
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            trail.addFirst(position());
            while (trail.size() > 10) {
                trail.removeLast();
            }
            return;
        }
        if (!(level() instanceof ServerLevel level)) {
            return;
        }
        Entity owner = getOwner();
        if (!(owner instanceof LivingEntity caster) || !caster.isAlive() || caster.level() != level || tickCount > getLife()) {
            CosmosFx.stars(level, position(), 20, .4);
            CosmosFx.sound(level, position(), SoundEvents.AMETHYST_BLOCK_BREAK, 1, 1.4f);
            discard();
            return;
        }
        switch (getState()) {
            case ORBIT -> {
                angle += SPIN;
                Vec3 at = orbitPoint(caster);
                setPos(at.x, at.y, at.z);
                strike(level, caster, .6f, 12);
                if (tickCount % 10 == 0) {
                    tide(level, caster);
                }
                if (!(caster instanceof net.minecraft.world.entity.player.Player) && --autoThrow <= 0) {
                    autoThrow = 60;
                    List<LivingEntity> foes = SpellUtil.enemiesAround(level, caster.position(), THROW_RANGE * .8, caster, e -> Cosmos.isFoe(caster, e));
                    if (!foes.isEmpty()) {
                        flingAt(foes.get(0));
                    }
                }
            }
            case OUT -> {
                Vec3 to = goal.subtract(position());
                double d = to.length();
                Vec3 step = d < 1.3 ? to : to.scale(1.3 / d);
                Vec3 next = position().add(step);
                HitResult wall = level.clip(new ClipContext(position(), next, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
                setPos(wall.getType() == HitResult.Type.MISS ? next.x : wall.getLocation().x, wall.getType() == HitResult.Type.MISS ? next.y : wall.getLocation().y,
                        wall.getType() == HitResult.Type.MISS ? next.z : wall.getLocation().z);
                strike(level, caster, 1.4f, 8);
                if (d < 1.3 || wall.getType() != HitResult.Type.MISS) {
                    this.entityData.set(DATA_STATE, State.BACK.ordinal());
                    struck.clear();
                    SpellUtil.particles(level, new BlastwaveParticleOptions(.85f, .88f, 1f, 2.5f), getX(), getY(), getZ(), 1, 0, 0, 0, 0);
                    CosmosFx.sound(level, position(), SoundEvents.AMETHYST_BLOCK_RESONATE, 1.2f, .8f);
                }
            }
            case BACK -> {
                Vec3 home = orbitPoint(caster);
                Vec3 to = home.subtract(position());
                double d = to.length();
                Vec3 step = d < 1.2 ? to : to.scale(1.2 / d);
                setPos(getX() + step.x, getY() + step.y, getZ() + step.z);
                strike(level, caster, 1.4f, 8);
                if (d < 1.5) {
                    this.entityData.set(DATA_STATE, State.ORBIT.ordinal());
                    angle = (float) Math.atan2(getZ() - caster.getZ(), getX() - caster.getX());
                }
            }
        }
    }

    /** What it passes through is struck and thrown aside (each thing once every {@code every} ticks). */
    private void strike(ServerLevel level, LivingEntity caster, float share, int every) {
        AbstractSpell spell = spellId == null ? null : SpellRegistry.getSpell(spellId);
        if (spell == null) {
            return;
        }
        for (LivingEntity e : SpellUtil.enemiesAround(level, position(), SIZE + 1, caster, e -> true)) {
            if (!e.getBoundingBox().inflate(SIZE).contains(position())) {
                continue;
            }
            Integer last = struck.get(e.getId());
            if (last != null && tickCount - last < every) {
                continue;
            }
            struck.put(e.getId(), tickCount);
            DamageSources.applyDamage(e, damage * share, SpellUtil.damageSource(spell, this, caster));
            Vec3 push = e.position().subtract(caster.position()).multiply(1, 0, 1);
            push = push.lengthSqr() < 1.0E-4 ? new Vec3(1, 0, 0) : push.normalize();
            e.push(push.x * .8, .35, push.z * .8);
            e.hurtMarked = true;
            Vec3 c = e.getBoundingBox().getCenter();
            SpellUtil.particles(level, ParticleTypes.CRIT, c.x, c.y, c.z, 8, .25, .25, .25, .3);
            CosmosFx.stars(level, c, 8, .3);
            CosmosFx.sound(level, c, SoundEvents.AMETHYST_CLUSTER_BREAK, 1, 1.2f);
        }
    }

    /** The moon's tide: foes round about drift toward its caster. */
    private void tide(ServerLevel level, LivingEntity caster) {
        for (LivingEntity e : SpellUtil.enemiesAround(level, caster.position(), TIDE_RANGE, caster, e -> Cosmos.isFoe(caster, e))) {
            Vec3 pull = caster.position().subtract(e.position()).multiply(1, 0, 1);
            if (pull.lengthSqr() < 4) {
                continue;
            }
            pull = pull.normalize().scale(.18);
            e.push(pull.x, 0, pull.z);
            e.hurtMarked = true;
            SpellUtil.particles(level, CosmosFx.STAR, e.getX(), e.getY() + .1, e.getZ(), 3, .3, 0, .3, 0);
        }
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 128 * 128;
    }
}
