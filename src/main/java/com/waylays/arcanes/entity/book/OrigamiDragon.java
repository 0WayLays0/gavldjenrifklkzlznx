package com.waylays.arcanes.entity.book;

import com.waylays.arcanes.entity.OwnedSummon;
import com.waylays.arcanes.entity.SpellEffectEntity;
import com.waylays.arcanes.registry.ModEntities;
import com.waylays.arcanes.registry.ModSpells;
import com.waylays.arcanes.spells.SpellUtil;
import com.waylays.arcanes.spells.book.BookFx;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.damage.DamageSources;
import io.redspace.ironsspellbooks.particle.BlastwaveParticleOptions;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * The Origami Dragon: a single sheet of paper above the caster folds itself, crease after crease, into a dragon
 * about eight blocks long. It hunts what its summoner fights (or the monsters around them): it circles its prey,
 * breathes a cone of razor-sharp paper at it, then dives through it and climbs back up. At the end it folds back
 * into a sheet and bursts into confetti. It cannot be hurt; anti-magic or casting the spell again sends it away.
 * <p>
 * The server flies it; the client keeps the path it flew so that the body and tail follow it like a kite's tail.
 */
public class OrigamiDragon extends SpellEffectEntity implements OwnedSummon {
    public static final int UNFOLD_TICKS = 40;
    public static final int FOLD_TICKS = 25;
    public static final int TRAIL = 64;
    public static final double BREATH_RANGE = 10;
    /** Distance from its chest (the entity's position) to the tip of its snout. */
    public static final double HEAD_REACH = 2.3;

    private static final EntityDataAccessor<Integer> DATA_DURATION = SynchedEntityData.defineId(OrigamiDragon.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_BREATHING = SynchedEntityData.defineId(OrigamiDragon.class, EntityDataSerializers.BOOLEAN);

    private enum Phase { CIRCLE, BREATHE, DIVE, CLIMB }

    private Phase phase = Phase.CIRCLE;
    private int phaseTicks;
    @Nullable
    private LivingEntity target;
    private final Set<Integer> hitThisDive = new HashSet<>();
    private Vec3 velocity = Vec3.ZERO;

    /** Client: where it was on each of the last ticks, newest first. */
    private final Vec3[] trail = new Vec3[TRAIL];
    private int trailHead;
    private int trailSize;

    public OrigamiDragon(EntityType<? extends OrigamiDragon> entityType, Level level) {
        super(entityType, level);
    }

    public OrigamiDragon(Level level, LivingEntity owner, Vec3 at, int duration, float damage) {
        this(ModEntities.ORIGAMI_DRAGON.get(), level);
        setOwner(owner);
        this.damage = damage;
        this.entityData.set(DATA_DURATION, duration);
        moveTo(at.x, at.y, at.z, owner.getYRot(), 0);
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(DATA_DURATION, 400);
        this.entityData.define(DATA_BREATHING, false);
    }

    public int getDuration() {
        return this.entityData.get(DATA_DURATION);
    }

    public boolean isBreathing() {
        return this.entityData.get(DATA_BREATHING);
    }

    /** Facing: where its head points. */
    public Vec3 getHeading(float partialTicks) {
        float yaw = Mth.rotLerp(partialTicks, yRotO, getYRot()) * Mth.DEG_TO_RAD;
        float pitch = Mth.lerp(partialTicks, xRotO, getXRot()) * Mth.DEG_TO_RAD;
        return new Vec3(-Mth.sin(yaw) * Mth.cos(pitch), -Mth.sin(pitch), Mth.cos(yaw) * Mth.cos(pitch));
    }

    /** Client: the {@code index}-th point of its path, newest first (null past the end). */
    @Nullable
    public Vec3 getTrailPoint(int index) {
        if (index >= trailSize) {
            return null;
        }
        return trail[Math.floorMod(trailHead - 1 - index, TRAIL)];
    }

    // ---------------------------------------------------------------- summon

    @Override
    @Nullable
    public UUID getSummonerUUID() {
        return ownerUUID;
    }

    @Override
    @Nullable
    public LivingEntity getSummoner() {
        return getLivingOwner();
    }

    @Override
    public AbstractSpell getSummoningSpell() {
        return ModSpells.ORIGAMI_DRAGON.get();
    }

    @Override
    public void dismissSummon() {
        burst();
    }

    // ---------------------------------------------------------------- flight

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            trail[trailHead] = position();
            trailHead = (trailHead + 1) % TRAIL;
            trailSize = Math.min(TRAIL, trailSize + 1);
            clientEffects();
            return;
        }
        LivingEntity owner = getLivingOwner();
        int duration = getDuration();
        if ((owner == null || !owner.isAlive()) && tickCount < duration - FOLD_TICKS) {
            // Its summoner is gone: it folds up early
            duration = tickCount + FOLD_TICKS;
            this.entityData.set(DATA_DURATION, duration);
        }
        if (tickCount >= duration) {
            burst();
            return;
        }
        sounds(duration);
        Vec3 goal;
        double speed;
        if (tickCount < UNFOLD_TICKS) {
            // Unfolding: hangs in the air, rising a little
            goal = position().add(0, .03, 0);
            speed = .05;
        } else if (tickCount >= duration - FOLD_TICKS || owner == null) {
            // Folding back up: slows down above its summoner
            goal = owner != null ? owner.position().add(0, 6, 0) : position();
            speed = .15;
            setBreathing(false);
        } else {
            if (target == null || !target.isAlive() || tickCount % 20 == 0 && !isHuntable(target, owner)) {
                target = findTarget(owner);
            }
            goal = hunt(owner);
            speed = phase == Phase.DIVE ? 1.25 : phase == Phase.BREATHE ? .35 : .8;
        }
        fly(goal, speed);
    }

    /** Steers toward {@code goal}: it turns smoothly, never on the spot. */
    private void fly(Vec3 goal, double speed) {
        Vec3 toGoal = goal.subtract(position());
        double distance = toGoal.length();
        Vec3 wanted = distance < 1.0E-3 ? Vec3.ZERO : toGoal.scale(Math.min(speed, distance * .2) / distance);
        double turn = phase == Phase.DIVE ? .16 : .08;
        velocity = velocity.add(wanted.subtract(velocity).scale(turn));
        Vec3 next = position().add(velocity);
        // Skims over the ground instead of plunging into it
        BlockPos below = BlockPos.containing(next.x, next.y - 1, next.z);
        if (!level().getBlockState(below).getCollisionShape(level(), below).isEmpty()) {
            velocity = new Vec3(velocity.x, Math.max(velocity.y, .25), velocity.z);
            next = position().add(velocity);
        }
        setPos(next.x, next.y, next.z);
        // Faces where it flies, or its prey while it breathes
        Vec3 facing = velocity;
        if (phase == Phase.BREATHE && target != null) {
            facing = target.getBoundingBox().getCenter().subtract(headPosition());
        }
        if (facing.lengthSqr() > 1.0E-4) {
            float yaw = (float) (Mth.atan2(-facing.x, facing.z) * Mth.RAD_TO_DEG);
            float pitch = (float) (-Mth.atan2(facing.y, facing.horizontalDistance()) * Mth.RAD_TO_DEG);
            setYRot(Mth.approachDegrees(getYRot(), yaw, 12));
            setXRot(Mth.approach(getXRot(), Mth.clamp(pitch, -60, 60), 6));
        }
    }

    /** Circle the prey, breathe paper at it, dive through it, climb back up; with no prey, circle the summoner. */
    private Vec3 hunt(LivingEntity owner) {
        phaseTicks++;
        if (target == null) {
            setPhase(Phase.CIRCLE);
            float a = tickCount * .045f;
            return owner.position().add(Mth.cos(a) * 7, 6, Mth.sin(a) * 7);
        }
        Vec3 prey = target.getBoundingBox().getCenter();
        switch (phase) {
            case CIRCLE -> {
                if (phaseTicks > 45) {
                    setPhase(Phase.BREATHE);
                }
                float a = tickCount * .07f;
                return prey.add(Mth.cos(a) * 8, 6, Mth.sin(a) * 8);
            }
            case BREATHE -> {
                setBreathing(phaseTicks > 8);
                if (phaseTicks > 8 && phaseTicks % 5 == 0) {
                    breathe(owner, prey);
                }
                if (phaseTicks > 40) {
                    setBreathing(false);
                    hitThisDive.clear();
                    setPhase(Phase.DIVE);
                    level().playSound(null, getX(), getY(), getZ(), SoundEvents.PHANTOM_SWOOP, SoundSource.PLAYERS, 2f, 1.3f);
                }
                // Hangs off to one side, a little above, facing the prey
                Vec3 away = position().subtract(prey).multiply(1, 0, 1);
                away = away.lengthSqr() < 1.0E-4 ? new Vec3(1, 0, 0) : away.normalize();
                return prey.add(away.scale(6.5)).add(0, 3.5, 0);
            }
            case DIVE -> {
                swoop(owner);
                if (phaseTicks > 28 || position().distanceToSqr(prey) < 2.5 * 2.5) {
                    setPhase(Phase.CLIMB);
                }
                return prey;
            }
            default -> {
                swoop(owner);
                if (phaseTicks > 22) {
                    setPhase(Phase.CIRCLE);
                }
                Vec3 ahead = velocity.multiply(1, 0, 1);
                ahead = ahead.lengthSqr() < 1.0E-4 ? new Vec3(1, 0, 0) : ahead.normalize();
                return position().add(ahead.scale(6)).add(0, 7, 0);
            }
        }
    }

    private void setPhase(Phase next) {
        if (phase != next) {
            phase = next;
            phaseTicks = 0;
        }
    }

    private void setBreathing(boolean breathing) {
        if (isBreathing() != breathing) {
            this.entityData.set(DATA_BREATHING, breathing);
        }
    }

    public Vec3 headPosition() {
        return position().add(getHeading(1).scale(HEAD_REACH));
    }

    /** A cone of paper shards: cuts and slows everything in front of its jaws. */
    private void breathe(LivingEntity owner, Vec3 prey) {
        Vec3 head = headPosition();
        Vec3 aim = prey.subtract(head).normalize();
        for (LivingEntity e : SpellUtil.enemiesAround(level(), head, BREATH_RANGE, owner, e -> !(e instanceof ArmorStand))) {
            Vec3 to = e.getBoundingBox().getCenter().subtract(head);
            if (to.normalize().dot(aim) < .82) {
                continue;
            }
            e.invulnerableTime = 0;
            if (DamageSources.applyDamage(e, damage * .3f, SpellUtil.damageSource(ModSpells.ORIGAMI_DRAGON.get(), this, owner))) {
                e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 1, false, false, true));
                Vec3 c = e.getBoundingBox().getCenter();
                SpellUtil.particles(level(), ParticleTypes.SWEEP_ATTACK, c.x, c.y, c.z, 1, .2, .2, .2, 0);
            }
        }
        SpellUtil.particles(level(), BookFx.paper(), head.x, head.y, head.z, 6, .1, .1, .1, 0);
        level().playSound(null, head.x, head.y, head.z, SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 1.6f, 1.3f + random.nextFloat() * .4f);
        level().playSound(null, head.x, head.y, head.z, SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1f, 1.6f);
    }

    /** Diving or climbing out of a dive, its body hits everything it passes through (once per dive). */
    private void swoop(LivingEntity owner) {
        AABB body = getBoundingBox().inflate(1.8);
        for (LivingEntity e : level().getEntitiesOfClass(LivingEntity.class, body, e -> e.isAlive() && !SpellUtil.isAlly(owner, e) && !(e instanceof ArmorStand))) {
            if (!hitThisDive.add(e.getId())) {
                continue;
            }
            e.invulnerableTime = 0;
            if (DamageSources.applyDamage(e, damage * 1.5f, SpellUtil.damageSource(ModSpells.ORIGAMI_DRAGON.get(), this, owner))) {
                Vec3 push = velocity.multiply(1, 0, 1);
                push = push.lengthSqr() < 1.0E-4 ? Vec3.ZERO : push.normalize().scale(.9);
                e.setDeltaMovement(e.getDeltaMovement().add(push).add(0, .55, 0));
                e.hurtMarked = true;
                Vec3 c = e.getBoundingBox().getCenter();
                SpellUtil.particles(level(), BookFx.paper(), c.x, c.y, c.z, 14, .3, .4, .3, .3);
                SpellUtil.particles(level(), ParticleTypes.CRIT, c.x, c.y, c.z, 12, .3, .4, .3, .4);
                level().playSound(null, c.x, c.y, c.z, SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.PLAYERS, 1.4f, .8f);
            }
        }
    }

    private boolean isHuntable(LivingEntity e, LivingEntity owner) {
        return e.isAlive() && !SpellUtil.isAlly(owner, e) && e.distanceToSqr(owner) < 40 * 40;
    }

    /** What its summoner fights, else whoever hurt them, else the nearest monster around them. */
    @Nullable
    private LivingEntity findTarget(LivingEntity owner) {
        List<LivingEntity> candidates = new ArrayList<>();
        if (owner instanceof Mob mob) {
            candidates.add(mob.getTarget());
        }
        if (owner.tickCount - owner.getLastHurtMobTimestamp() < 200) {
            candidates.add(owner.getLastHurtMob());
        }
        candidates.add(owner.getLastHurtByMob());
        for (LivingEntity candidate : candidates) {
            if (candidate != null && isHuntable(candidate, owner)) {
                return candidate;
            }
        }
        LivingEntity nearest = null;
        double best = Double.MAX_VALUE;
        for (LivingEntity e : SpellUtil.enemiesAround(level(), owner.position(), 24, owner, e -> e instanceof Enemy)) {
            double d = e.distanceToSqr(owner);
            if (d < best) {
                best = d;
                nearest = e;
            }
        }
        return nearest;
    }

    private void sounds(int duration) {
        Vec3 c = position();
        if (tickCount < UNFOLD_TICKS && tickCount % 3 == 0) {
            level().playSound(null, c.x, c.y, c.z, SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 1.8f, .6f + tickCount / (float) UNFOLD_TICKS);
        }
        if (tickCount == UNFOLD_TICKS) {
            // Fully folded: it roars, a rustling, papery roar
            level().playSound(null, c.x, c.y, c.z, SoundEvents.ENDER_DRAGON_GROWL, SoundSource.PLAYERS, 2.5f, 1.6f);
            SpellUtil.particles(level(), new BlastwaveParticleOptions(BookFx.VIOLET, 6), c.x, c.y, c.z, 1, 0, 0, 0, 0);
            SpellUtil.cameraShake(level(), c, 15, 18);
        }
        if (tickCount > UNFOLD_TICKS && tickCount < duration - FOLD_TICKS && tickCount % 16 == 0) {
            level().playSound(null, c.x, c.y, c.z, SoundEvents.ENDER_DRAGON_FLAP, SoundSource.PLAYERS, 1.4f, 1.5f);
        }
        if (tickCount == duration - FOLD_TICKS) {
            level().playSound(null, c.x, c.y, c.z, SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 2f, .5f);
        }
    }

    /** Folded back into a sheet, it bursts into a cloud of confetti and glyphs. */
    private void burst() {
        Vec3 c = position();
        SpellUtil.particles(level(), BookFx.paper(), c.x, c.y, c.z, 80, 1.2, .8, 1.2, .4);
        SpellUtil.particles(level(), ParticleTypes.ENCHANT, c.x, c.y, c.z, 120, 1, .8, 1, 2);
        SpellUtil.particles(level(), BookFx.VIOLET_DUST, c.x, c.y, c.z, 40, 1.5, 1, 1.5, 0);
        SpellUtil.particles(level(), BookFx.GILT_DUST, c.x, c.y, c.z, 30, 1.5, 1, 1.5, 0);
        SpellUtil.particles(level(), new BlastwaveParticleOptions(BookFx.GILT, 4), c.x, c.y, c.z, 1, 0, 0, 0, 0);
        level().playSound(null, c.x, c.y, c.z, SoundEvents.BOOK_PUT, SoundSource.PLAYERS, 2.5f, .7f);
        level().playSound(null, c.x, c.y, c.z, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 2.5f, 1.2f);
        discard();
    }

    private void clientEffects() {
        Vec3 heading = getHeading(1);
        if (isBreathing()) {
            // The breath: a stream of paper shards and glyphs out of its jaws
            Vec3 head = position().add(heading.scale(HEAD_REACH));
            for (int i = 0; i < 4; i++) {
                Vec3 v = heading.scale(.7 + random.nextDouble() * .5).add(random.nextGaussian() * .1, random.nextGaussian() * .1, random.nextGaussian() * .1);
                level().addParticle(i == 0 ? ParticleTypes.ENCHANT : BookFx.paper(), head.x, head.y, head.z, v.x, v.y, v.z);
            }
            level().addParticle(BookFx.VIOLET_DUST, head.x + heading.x * 2, head.y + heading.y * 2, head.z + heading.z * 2, 0, 0, 0);
        }
        if (tickCount > UNFOLD_TICKS && random.nextInt(2) == 0) {
            // Glyphs trailing off its wings and tail
            Vec3 back = position().subtract(heading.scale(2 + random.nextDouble() * 3));
            level().addParticle(random.nextBoolean() ? BookFx.VIOLET_DUST : BookFx.GILT_DUST, back.x + random.nextGaussian() * .8, back.y + random.nextGaussian() * .4, back.z + random.nextGaussian() * .8, 0, 0, 0);
        }
    }

    @Override
    public AABB getBoundingBoxForCulling() {
        return getBoundingBox().inflate(9);
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 160 * 160;
    }
}
