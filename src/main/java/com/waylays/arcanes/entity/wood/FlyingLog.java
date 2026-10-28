package com.waylays.arcanes.entity.wood;

import com.waylays.arcanes.entity.SpellEffectEntity;
import com.waylays.arcanes.registry.ModEntities;
import com.waylays.arcanes.spells.SpellUtil;
import com.waylays.arcanes.spells.wood.WoodFx;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.damage.DamageSources;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

/**
 * Log Hurl: a great oak log (its ends showing their rings, stubs of branches, ivy round it) is torn up out of the
 * ground in front of the caster and hurled, turning over as it flies, lying across its path. Where it comes down it
 * rolls on along the ground, bowling over every enemy in its way (hurt, thrown up and back), until it has rolled its
 * distance or runs into a wall: then it bursts apart in a spray of chunks and splinters that strikes all round.
 * The server moves it; both sides draw it from its phase.
 */
public class FlyingLog extends SpellEffectEntity {
    public enum Phase { RISE, FLY, ROLL, SHATTER }

    public static final int RISE_TICKS = 6;
    public static final int SHATTER_TICKS = 16;
    public static final float LENGTH = 3.2f;
    public static final float RADIUS = .55f;
    public static final float BURST = 2.6f;
    private static final double GRAVITY = .06;

    private static final EntityDataAccessor<Integer> DATA_PHASE = SynchedEntityData.defineId(FlyingLog.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_PHASE_TICK = SynchedEntityData.defineId(FlyingLog.class, EntityDataSerializers.INT);

    private final IntOpenHashSet struck = new IntOpenHashSet();
    @Nullable
    private String spellId;
    private Vec3 motion = Vec3.ZERO;
    private Vec3 launch = Vec3.ZERO;
    private float rollLeft;
    private float speed;
    /** Client: how far it has turned over, for drawing. */
    public float spin;
    public float spinO;

    public FlyingLog(EntityType<? extends FlyingLog> entityType, Level level) {
        super(entityType, level);
    }

    /** Tears the log up in front of {@code caster}; it is hurled the way they look, then rolls up to {@code roll} blocks. */
    public static FlyingLog hurl(Level level, LivingEntity caster, AbstractSpell spell, float damage, float roll) {
        FlyingLog log = new FlyingLog(ModEntities.FLYING_LOG.get(), level);
        log.setOwner(caster);
        log.spellId = spell.getSpellId();
        log.damage = damage;
        log.rollLeft = roll;
        float yaw = caster.getYRot();
        float r = yaw * Mth.DEG_TO_RAD;
        Vec3 at = SpellUtil.groundAt(level, caster.position().add(-Mth.sin(r) * 2.2, 0, Mth.cos(r) * 2.2), 2, 1);
        log.moveTo(at.x, at.y - RADIUS * 2, at.z, yaw, 0);
        Vec3 look = caster.getLookAngle();
        Vec3 flat = new Vec3(look.x, 0, look.z);
        flat = flat.lengthSqr() < 1.0E-4 ? new Vec3(-Mth.sin(r), 0, Mth.cos(r)) : flat.normalize();
        log.launch = flat.scale(1.15).add(0, Mth.clamp(look.y, -.3, .6) * .9 + .32, 0);
        level.addFreshEntity(log);
        WoodFx.sprout(level, at, 1.4f);
        WoodFx.creak(level, at, 1);
        return log;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_PHASE, 0);
        builder.define(DATA_PHASE_TICK, 0);
    }

    public Phase getPhase() {
        Phase[] all = Phase.values();
        return all[Mth.clamp(this.entityData.get(DATA_PHASE), 0, all.length - 1)];
    }

    /** The tick its current phase began. */
    public int getPhaseTick() {
        return this.entityData.get(DATA_PHASE_TICK);
    }

    private void setPhase(Phase phase) {
        this.entityData.set(DATA_PHASE, phase.ordinal());
        this.entityData.set(DATA_PHASE_TICK, tickCount);
    }

    /** Unit vector of the way it travels (the log lies across it). */
    public Vec3 forward() {
        float r = getYRot() * Mth.DEG_TO_RAD;
        return new Vec3(-Mth.sin(r), 0, Mth.cos(r));
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            spinO = spin;
            Phase phase = getPhase();
            if (phase == Phase.FLY) {
                spin += .45f;
            } else if (phase == Phase.ROLL) {
                spin += (float) (Math.hypot(getX() - xo, getZ() - zo) / RADIUS);
            }
            return;
        }
        ServerLevel level = (ServerLevel) level();
        switch (getPhase()) {
            case RISE -> rise(level);
            case FLY -> fly(level);
            case ROLL -> roll(level);
            case SHATTER -> {
                if (tickCount - getPhaseTick() > SHATTER_TICKS) {
                    discard();
                }
            }
        }
        if (tickCount > 200 && getPhase() != Phase.SHATTER) {
            shatter(level);
        }
    }

    /** Torn up out of the ground, shaking, then hurled. */
    private void rise(ServerLevel level) {
        int t = tickCount - getPhaseTick();
        setPos(getX(), getY() + (RADIUS * 2 + 1.1) / RISE_TICKS, getZ());
        if (t % 2 == 0) {
            WoodFx.chips(level, position().add(0, RADIUS, 0), 8, .8);
            ground(level, position(), 10);
        }
        if (t >= RISE_TICKS) {
            motion = launch;
            setPhase(Phase.FLY);
            WoodFx.sound(level, position(), SoundEvents.PLAYER_ATTACK_SWEEP, 1.2f, .5f);
            WoodFx.sound(level, position(), SoundEvents.WOOD_BREAK, 1.2f, .6f);
        }
    }

    /** Flying: it strikes what it passes through, and rolls on where it comes down (or bursts against a wall). */
    private void fly(ServerLevel level) {
        Vec3 centre = position().add(0, RADIUS, 0);
        Vec3 next = centre.add(motion);
        BlockHitResult hit = level.clip(new ClipContext(centre, next.add(0, -RADIUS * .9, 0), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        strike(level, false);
        if (hit.getType() == HitResult.Type.BLOCK) {
            if (hit.getDirection() == Direction.UP) {
                setPos(hit.getLocation().x, hit.getLocation().y, hit.getLocation().z);
                speed = (float) Math.max(.55, motion.horizontalDistance() * .85);
                setPhase(Phase.ROLL);
                WoodFx.crash(level, position(), 1.6f);
                WoodFx.chips(level, position().add(0, .3, 0), 24, 1.2);
                ground(level, position(), 24);
                SpellUtil.cameraShake(level, position(), 6, 10);
            } else {
                setPos(hit.getLocation().x, hit.getLocation().y - RADIUS, hit.getLocation().z);
                shatter(level);
            }
            return;
        }
        setPos(next.x, next.y - RADIUS, next.z);
        motion = motion.add(0, -GRAVITY, 0);
    }

    /** Rolling along the ground, bowling over what is in its way. */
    private void roll(ServerLevel level) {
        Vec3 f = forward();
        Vec3 next = position().add(f.scale(speed));
        Double floor = floor(level, next);
        if (floor == null) {
            // Over an edge: it falls on, still moving
            motion = f.scale(speed);
            setPhase(Phase.FLY);
            return;
        }
        if (floor - getY() > 1.1) {
            shatter(level);
            return;
        }
        setPos(next.x, floor, next.z);
        rollLeft -= speed;
        speed = Math.max(.4f, speed * .985f);
        strike(level, true);
        if ((tickCount - getPhaseTick()) % 3 == 0) {
            WoodFx.chips(level, position().add(0, .1, 0), 4, .6);
            ground(level, position(), 6);
            WoodFx.sound(level, position(), SoundEvents.WOOD_STEP, 1.2f, .5f);
        }
        if (rollLeft <= 0) {
            shatter(level);
        }
    }

    /** The surface under {@code at} within a block and a half up or down, if any. */
    @Nullable
    private Double floor(ServerLevel level, Vec3 at) {
        for (int dy = 2; dy >= -2; dy--) {
            BlockPos pos = BlockPos.containing(at.x, at.y + dy * .75, at.z);
            BlockState state = level.getBlockState(pos);
            var shape = state.getCollisionShape(level, pos);
            if (!shape.isEmpty()) {
                BlockPos above = pos.above();
                if (level.getBlockState(above).getCollisionShape(level, above).isEmpty()) {
                    return pos.getY() + shape.max(Direction.Axis.Y);
                }
                return at.y + 5;
            }
        }
        return null;
    }

    /** Every enemy the log is on hits once: hurt, thrown up and on. */
    private void strike(ServerLevel level, boolean rolling) {
        AbstractSpell spell = spellId == null ? null : SpellRegistry.getSpell(spellId);
        LivingEntity caster = getLivingOwner();
        if (spell == null) {
            return;
        }
        Vec3 f = forward();
        Vec3 side = new Vec3(-f.z, 0, f.x);
        Vec3 centre = position().add(0, RADIUS, 0);
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(centre, centre).inflate(LENGTH * .5 + 1, 1.6, LENGTH * .5 + 1))) {
            if (!e.isAlive() || e == caster || SpellUtil.isAlly(caster, e) || struck.contains(e.getId())) {
                continue;
            }
            Vec3 d = e.position().add(0, e.getBbHeight() * .5, 0).subtract(centre);
            if (Math.abs(d.dot(side)) > LENGTH * .5 + e.getBbWidth() * .5 || Math.abs(d.dot(f)) > RADIUS + e.getBbWidth() * .5 + .3
                    || d.y > e.getBbHeight() * .5 + RADIUS + .2 || d.y < -e.getBbHeight() * .5 - RADIUS - .2) {
                continue;
            }
            struck.add(e.getId());
            e.invulnerableTime = 0;
            DamageSources.applyDamage(e, damage, SpellUtil.damageSource(spell, this, caster));
            double resist = 1 - Mth.clamp(e.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE), 0, 1);
            Vec3 push = rolling ? f.scale(speed * 1.1 + .3) : motion.multiply(1, 0, 1).scale(.9);
            e.setDeltaMovement(e.getDeltaMovement().add(push.scale(resist)).add(0, .55 * Math.max(.4, resist), 0));
            e.hurtMarked = true;
            e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 2));
            WoodFx.chips(level, e.position().add(0, e.getBbHeight() * .5, 0), 12, .4);
            WoodFx.sound(level, e.position(), SoundEvents.ZOMBIE_ATTACK_WOODEN_DOOR, 1, .7f);
        }
    }

    /** It bursts apart: chunks and splinters fly out and strike all round. */
    private void shatter(ServerLevel level) {
        setPhase(Phase.SHATTER);
        Vec3 centre = position().add(0, RADIUS, 0);
        AbstractSpell spell = spellId == null ? null : SpellRegistry.getSpell(spellId);
        LivingEntity caster = getLivingOwner();
        if (spell != null) {
            for (LivingEntity e : SpellUtil.enemiesAround(level, centre, BURST, caster, x -> true)) {
                e.invulnerableTime = 0;
                DamageSources.applyDamage(e, damage * .5f, SpellUtil.damageSource(spell, this, caster));
                Vec3 out = e.position().subtract(centre).multiply(1, 0, 1);
                out = out.lengthSqr() < 1.0E-4 ? Vec3.ZERO : out.normalize();
                double resist = 1 - Mth.clamp(e.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE), 0, 1);
                e.setDeltaMovement(e.getDeltaMovement().add(out.scale(.5 * resist)).add(0, .25 * resist, 0));
                e.hurtMarked = true;
            }
        }
        WoodFx.chips(level, centre, 50, 1.2);
        WoodFx.leaves(level, centre, 14, 1);
        WoodFx.crash(level, centre, 2);
        WoodFx.sound(level, centre, SoundEvents.ZOMBIE_BREAK_WOODEN_DOOR, 1.4f, .7f);
        SpellUtil.cameraShake(level, centre, 6, 12);
    }

    /** Clods of the ground under it thrown up. */
    private void ground(ServerLevel level, Vec3 at, int count) {
        BlockPos below = BlockPos.containing(at.x, at.y - .2, at.z);
        BlockState state = level.getBlockState(below);
        if (!state.isAir()) {
            SpellUtil.particles(level, new BlockParticleOption(ParticleTypes.BLOCK, state), at.x, at.y + .1, at.z, count, .8, .1, .8, .15);
        }
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 128 * 128;
    }
}
