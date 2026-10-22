package com.waylays.arcanes.entity.gold;

import com.waylays.arcanes.entity.OwnedSummon;
import com.waylays.arcanes.entity.SpellEffectEntity;
import com.waylays.arcanes.registry.ModEntities;
import com.waylays.arcanes.registry.ModSpells;
import com.waylays.arcanes.spells.SpellUtil;
import com.waylays.arcanes.spells.gold.GoldFx;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.damage.DamageSources;
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
 * The Golden Dragon of Eldorado: a long dragon of gold, horned and maned, that pours out of a giant sun above its
 * summoner (see {@link DragonSun}) and hunts what they fight (or the monsters around them). It circles its prey, then either breathes a
 * stream of molten gold at it or coils round it, climbing in a tight spiral that holds it fast and gilds it before a
 * roar throws everything back; then it dives through its prey and climbs again. Everything it hurts it gilds. At the
 * end it melts away into a cascade of coins, tail first. It cannot be hurt; anti-magic or casting the spell again
 * sends it away.
 * <p>
 * The server flies it; the client keeps the path it flew so that its long body follows it like a ribbon.
 */
public class GoldenDragon extends SpellEffectEntity implements OwnedSummon {
    public static final int EMERGE_TICKS = 40;
    public static final int MELT_TICKS = 30;
    public static final int TRAIL = 160;
    public static final double BREATH_RANGE = 11;
    /** Distance from its chest (the entity's position) to its jaws. */
    public static final double HEAD_REACH = 2.1;
    public static final double COIL_RADIUS = 2.3;
    public static final int COIL_TICKS = 50;
    public static final double ROAR_RADIUS = 5;
    /** How long its summoner can be out of sight before it counts as gone. */
    public static final int OWNER_GRACE_TICKS = 20;

    private static final EntityDataAccessor<Integer> DATA_DURATION = SynchedEntityData.defineId(GoldenDragon.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_BREATHING = SynchedEntityData.defineId(GoldenDragon.class, EntityDataSerializers.BOOLEAN);
    /** The entity it is coiled round (-1: none), so the client can light the coil. */
    private static final EntityDataAccessor<Integer> DATA_COILED = SynchedEntityData.defineId(GoldenDragon.class, EntityDataSerializers.INT);

    private enum Phase { CIRCLE, BREATHE, COIL, DIVE, CLIMB }

    private Phase phase = Phase.CIRCLE;
    private int phaseTicks;
    private int hunts;
    private int ownerMissingTicks;
    @Nullable
    private LivingEntity target;
    private final Set<Integer> hitThisDive = new HashSet<>();
    private Vec3 velocity = Vec3.ZERO;
    private float coilAngle;
    /** Where round its summoner it first heads, coming out of the sun. */
    private float startAngle;

    /** Client: where it was on each of the last ticks, newest first. */
    private final Vec3[] trail = new Vec3[TRAIL];
    private int trailHead;
    private int trailSize;

    public GoldenDragon(EntityType<? extends GoldenDragon> entityType, Level level) {
        super(entityType, level);
    }

    public GoldenDragon(Level level, LivingEntity owner, Vec3 at, int duration, float damage) {
        this(ModEntities.GOLDEN_DRAGON.get(), level);
        setOwner(owner);
        this.damage = damage;
        this.entityData.set(DATA_DURATION, duration);
        moveTo(at.x, at.y, at.z, owner.getYRot(), 0);
        this.startAngle = (owner.getYRot() + 90) * Mth.DEG_TO_RAD;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_DURATION, 400);
        builder.define(DATA_BREATHING, false);
        builder.define(DATA_COILED, -1);
    }

    public int getDuration() {
        return this.entityData.get(DATA_DURATION);
    }

    public boolean isBreathing() {
        return this.entityData.get(DATA_BREATHING);
    }

    public int getCoiled() {
        return this.entityData.get(DATA_COILED);
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
        return ModSpells.GOLDEN_DRAGON.get();
    }

    @Override
    public void dismissSummon() {
        if (!level().isClientSide && tickCount < getDuration() - MELT_TICKS) {
            this.entityData.set(DATA_DURATION, tickCount + MELT_TICKS);
        }
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
        // A summoner out of sight for a moment (a chunk still loading) is not gone
        ownerMissingTicks = owner == null ? ownerMissingTicks + 1 : 0;
        boolean ownerGone = owner != null ? !owner.isAlive() : ownerMissingTicks > OWNER_GRACE_TICKS;
        if (ownerGone && tickCount < duration - MELT_TICKS) {
            duration = tickCount + MELT_TICKS;
            this.entityData.set(DATA_DURATION, duration);
        }
        if (tickCount >= duration) {
            discard();
            return;
        }
        sounds(duration);
        Vec3 goal;
        double speed;
        if (tickCount < EMERGE_TICKS) {
            // Pouring out of the giant sun: it sweeps down round its summoner, its tail still in the sun
            float a = tickCount * .09f + startAngle;
            goal = owner != null ? owner.position().add(Mth.cos(a) * 6, 4.5, Mth.sin(a) * 6) : position();
            speed = .3 + .5 * tickCount / EMERGE_TICKS;
        } else if (tickCount >= duration - MELT_TICKS || owner == null) {
            // Melting: it slows and coils up above its summoner, shedding coins
            goal = owner != null ? owner.position().add(Mth.cos(tickCount * .2f) * 3, 6, Mth.sin(tickCount * .2f) * 3) : position();
            speed = .35;
            setBreathing(false);
            setCoiled(null);
            if ((duration - tickCount) % 5 == 0) {
                GoldFx.coins(level(), position().subtract(velocity.scale(4 + random.nextInt(6))), 6, .25f);
            }
        } else {
            if (target == null || !target.isAlive() || tickCount % 20 == 0 && !isHuntable(target, owner)) {
                target = findTarget(owner);
                if (phase == Phase.COIL) {
                    setPhase(Phase.CLIMB);
                }
            }
            goal = hunt(owner);
            speed = switch (phase) {
                case DIVE -> 1.3;
                case BREATHE -> .35;
                case COIL -> .9;
                default -> .8;
            };
        }
        fly(goal, speed);
    }

    /** Steers toward {@code goal}: it turns smoothly, never on the spot. */
    private void fly(Vec3 goal, double speed) {
        Vec3 toGoal = goal.subtract(position());
        double distance = toGoal.length();
        Vec3 wanted = distance < 1.0E-3 ? Vec3.ZERO : toGoal.scale(Math.min(speed, distance * .25) / distance);
        double turn = phase == Phase.DIVE || phase == Phase.COIL ? .2 : .08;
        velocity = velocity.add(wanted.subtract(velocity).scale(turn));
        Vec3 next = position().add(velocity);
        // Skims over the ground instead of plunging into it
        BlockPos below = BlockPos.containing(next.x, next.y - 1, next.z);
        if (!level().getBlockState(below).getCollisionShape(level(), below).isEmpty()) {
            velocity = new Vec3(velocity.x, Math.max(velocity.y, .25), velocity.z);
            next = position().add(velocity);
        }
        setPos(next.x, next.y, next.z);
        Vec3 facing = velocity;
        if (phase == Phase.BREATHE && target != null) {
            facing = target.getBoundingBox().getCenter().subtract(headPosition());
        }
        if (facing.lengthSqr() > 1.0E-4) {
            float yaw = (float) (Mth.atan2(-facing.x, facing.z) * Mth.RAD_TO_DEG);
            float pitch = (float) (-Mth.atan2(facing.y, facing.horizontalDistance()) * Mth.RAD_TO_DEG);
            setYRot(Mth.approachDegrees(getYRot(), yaw, phase == Phase.COIL ? 20 : 12));
            setXRot(Mth.approach(getXRot(), Mth.clamp(pitch, -60, 60), 6));
        }
    }

    /**
     * Circle the prey; then breathe molten gold at it, or (every other time) coil round it; dive through it and climb
     * back up. With no prey, circle the summoner.
     */
    private Vec3 hunt(LivingEntity owner) {
        phaseTicks++;
        if (target == null) {
            setPhase(Phase.CIRCLE);
            float a = tickCount * .04f;
            return owner.position().add(Mth.cos(a) * 8, 6.5, Mth.sin(a) * 8);
        }
        Vec3 prey = target.getBoundingBox().getCenter();
        switch (phase) {
            case CIRCLE -> {
                if (phaseTicks > 45) {
                    hunts++;
                    setPhase(hunts % 2 == 0 ? Phase.COIL : Phase.BREATHE);
                    coilAngle = (float) Math.atan2(getZ() - prey.z, getX() - prey.x);
                }
                float a = tickCount * .065f;
                return prey.add(Mth.cos(a) * 9, 6, Mth.sin(a) * 9);
            }
            case BREATHE -> {
                setBreathing(phaseTicks > 8);
                if (phaseTicks > 8 && phaseTicks % 5 == 0) {
                    breathe(owner, prey);
                }
                if (phaseTicks > 45) {
                    setBreathing(false);
                    beginDive();
                }
                Vec3 away = position().subtract(prey).multiply(1, 0, 1);
                away = away.lengthSqr() < 1.0E-4 ? new Vec3(1, 0, 0) : away.normalize();
                return prey.add(away.scale(7)).add(0, 3.5, 0);
            }
            case COIL -> {
                setCoiled(target);
                coil(owner);
                if (phaseTicks >= COIL_TICKS) {
                    roar(owner);
                    setCoiled(null);
                    setPhase(Phase.CLIMB);
                }
                // A tight spiral climbing round the prey: its body wraps round it
                coilAngle += .42f;
                double rise = -target.getBbHeight() * .4 + phaseTicks / (double) COIL_TICKS * (target.getBbHeight() + 2.5);
                return prey.add(Mth.cos(coilAngle) * COIL_RADIUS, rise, Mth.sin(coilAngle) * COIL_RADIUS);
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

    private void beginDive() {
        hitThisDive.clear();
        setPhase(Phase.DIVE);
        level().playSound(null, getX(), getY(), getZ(), SoundEvents.PHANTOM_SWOOP, SoundSource.PLAYERS, 2f, .7f);
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

    private void setCoiled(@Nullable LivingEntity coiled) {
        int id = coiled == null ? -1 : coiled.getId();
        if (getCoiled() != id) {
            this.entityData.set(DATA_COILED, id);
        }
    }

    public Vec3 headPosition() {
        return position().add(getHeading(1).scale(HEAD_REACH));
    }

    /** A stream of molten gold: burns and gilds everything in front of its jaws. */
    private void breathe(LivingEntity owner, Vec3 prey) {
        Vec3 head = headPosition();
        Vec3 aim = prey.subtract(head).normalize();
        for (LivingEntity e : SpellUtil.enemiesAround(level(), head, BREATH_RANGE, owner, e -> !(e instanceof ArmorStand))) {
            Vec3 to = e.getBoundingBox().getCenter().subtract(head);
            if (to.normalize().dot(aim) < .85) {
                continue;
            }
            e.invulnerableTime = 0;
            if (DamageSources.applyDamage(e, damage * .3f, SpellUtil.damageSource(ModSpells.GOLDEN_DRAGON.get(), this, owner))) {
                e.igniteForSeconds(2);
                GoldFx.gildEvery(e, 10);
            }
        }
        level().playSound(null, head.x, head.y, head.z, SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 1.2f, .5f + random.nextFloat() * .2f);
        level().playSound(null, head.x, head.y, head.z, SoundEvents.LAVA_POP, SoundSource.PLAYERS, 1.5f, .7f);
    }

    /** Coiled round its prey: it holds it fast, crushes it and gilds it. */
    private void coil(LivingEntity owner) {
        if (target == null) {
            return;
        }
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 15, 4, false, false, true));
        if (phaseTicks % 10 == 0) {
            target.invulnerableTime = 0;
            if (DamageSources.applyDamage(target, damage * .35f, SpellUtil.damageSource(ModSpells.GOLDEN_DRAGON.get(), this, owner))) {
                GoldFx.gildEvery(target, 20);
            }
            level().playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.CHAIN_HIT, SoundSource.PLAYERS, 1.5f, .6f);
        }
    }

    /** Uncoiling, it roars: a burst of gold throws everything around back and gilds it. */
    private void roar(LivingEntity owner) {
        Vec3 c = target != null ? target.getBoundingBox().getCenter() : position();
        for (LivingEntity e : SpellUtil.enemiesAround(level(), c, ROAR_RADIUS, owner, e -> !(e instanceof ArmorStand))) {
            e.invulnerableTime = 0;
            if (DamageSources.applyDamage(e, damage * .8f, SpellUtil.damageSource(ModSpells.GOLDEN_DRAGON.get(), this, owner))) {
                GoldFx.gild(e, 1);
                Vec3 push = e.position().subtract(c).multiply(1, 0, 1);
                push = push.lengthSqr() < 1.0E-4 ? new Vec3(1, 0, 0) : push.normalize();
                e.push(push.x * 1.1, .6, push.z * 1.1);
                e.hurtMarked = true;
            }
        }
        GoldFx.coins(level(), c, 20, .6f);
        GoldFx.shatterBurst(level(), c.x, c.y, c.z, 2f);
        SpellUtil.cameraShake(level(), c, 12, 16);
        level().playSound(null, c.x, c.y, c.z, SoundEvents.ENDER_DRAGON_GROWL, SoundSource.PLAYERS, 2.5f, 1.1f);
        level().playSound(null, c.x, c.y, c.z, SoundEvents.BELL_BLOCK, SoundSource.PLAYERS, 2f, .6f);
    }

    /** Diving or climbing out of a dive, its body hits everything it passes through (once per dive). */
    private void swoop(LivingEntity owner) {
        AABB body = getBoundingBox().inflate(1.8);
        for (LivingEntity e : level().getEntitiesOfClass(LivingEntity.class, body, e -> e.isAlive() && !SpellUtil.isAlly(owner, e) && !(e instanceof ArmorStand))) {
            if (!hitThisDive.add(e.getId())) {
                continue;
            }
            e.invulnerableTime = 0;
            if (DamageSources.applyDamage(e, damage * 1.4f, SpellUtil.damageSource(ModSpells.GOLDEN_DRAGON.get(), this, owner))) {
                GoldFx.gild(e, 1);
                Vec3 push = velocity.multiply(1, 0, 1);
                push = push.lengthSqr() < 1.0E-4 ? Vec3.ZERO : push.normalize().scale(.9);
                e.setDeltaMovement(e.getDeltaMovement().add(push).add(0, .55, 0));
                e.hurtMarked = true;
                Vec3 c = e.getBoundingBox().getCenter();
                SpellUtil.particles(level(), GoldFx.sparks(), c.x, c.y, c.z, 14, .3, .4, .3, .4);
                level().playSound(null, c.x, c.y, c.z, SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, .8f, 1.2f);
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
        if (tickCount < EMERGE_TICKS && tickCount % 4 == 0) {
            level().playSound(null, c.x, c.y, c.z, SoundEvents.LAVA_POP, SoundSource.PLAYERS, 1.5f, .5f + tickCount / (float) EMERGE_TICKS);
        }
        if (tickCount == EMERGE_TICKS) {
            level().playSound(null, c.x, c.y, c.z, SoundEvents.ENDER_DRAGON_GROWL, SoundSource.PLAYERS, 3f, .9f);
            level().playSound(null, c.x, c.y, c.z, SoundEvents.BELL_RESONATE, SoundSource.PLAYERS, 2f, .8f);
            GoldFx.coins(level(), c, 16, .5f);
            SpellUtil.cameraShake(level(), c, 15, 20);
        }
        if (tickCount > EMERGE_TICKS && tickCount < duration - MELT_TICKS && tickCount % 18 == 0) {
            level().playSound(null, c.x, c.y, c.z, SoundEvents.ARMOR_EQUIP_GOLD.value(), SoundSource.PLAYERS, 1.5f, .6f);
        }
        if (tickCount == duration - MELT_TICKS) {
            level().playSound(null, c.x, c.y, c.z, SoundEvents.AMETHYST_CLUSTER_BREAK, SoundSource.PLAYERS, 2f, .6f);
        }
    }

    private void clientEffects() {
        Vec3 heading = getHeading(1);
        if (isBreathing()) {
            // The breath: molten gold pouring out of its jaws
            Vec3 head = position().add(heading.scale(HEAD_REACH));
            for (int i = 0; i < 4; i++) {
                Vec3 v = heading.scale(.6 + random.nextDouble() * .5).add(random.nextGaussian() * .08, random.nextGaussian() * .08, random.nextGaussian() * .08);
                level().addParticle(i == 0 ? GoldFx.sparks() : GoldFx.MOLTEN_DUST, head.x, head.y, head.z, v.x, v.y, v.z);
            }
            if (random.nextInt(3) == 0) {
                level().addParticle(ParticleTypes.LAVA, head.x, head.y, head.z, 0, 0, 0);
            }
        }
        if (tickCount > EMERGE_TICKS && random.nextInt(2) == 0) {
            // Glitter shed along its body
            Vec3 back = position().subtract(heading.scale(2 + random.nextDouble() * 6));
            level().addParticle(random.nextBoolean() ? GoldFx.PALE_DUST : GoldFx.GOLD_DUST, back.x + random.nextGaussian() * .6, back.y + random.nextGaussian() * .4,
                    back.z + random.nextGaussian() * .6, 0, -.02, 0);
        }
    }

    @Override
    public AABB getBoundingBoxForCulling() {
        return getBoundingBox().inflate(14);
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 160 * 160;
    }
}
