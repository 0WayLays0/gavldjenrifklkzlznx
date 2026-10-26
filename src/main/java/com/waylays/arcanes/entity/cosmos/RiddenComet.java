package com.waylays.arcanes.entity.cosmos;

import com.waylays.arcanes.ArcanesOublies;
import com.waylays.arcanes.entity.OwnedSummon;
import com.waylays.arcanes.registry.ModEntities;
import com.waylays.arcanes.registry.ModSpells;
import com.waylays.arcanes.spells.SpellUtil;
import com.waylays.arcanes.spells.cosmos.Cosmos;
import com.waylays.arcanes.spells.cosmos.CosmosFx;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.damage.DamageSources;
import io.redspace.ironsspellbooks.entity.mobs.AntiMagicSusceptible;
import io.redspace.ironsspellbooks.particle.BlastwaveParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * A comet its caster rides: a nucleus of ice wrapped in a glowing coma, a long blue ion tail and a pale curved dust
 * tail streaming behind. It never stops: it goes where its rider looks, faster with the spell's level, and rams
 * whatever is in its way. Cast again (or jump off) and it dives: it crashes where it is headed, a blast all round, a
 * crater of burning ground left behind, its rider thrown clear and floating down.
 */
@EventBusSubscriber(modid = ArcanesOublies.MODID)
public class RiddenComet extends Mob implements OwnedSummon, AntiMagicSusceptible {
    public static final float DIVE_SPEED = 1.6f;
    public static final int DIVE_TICKS = 40;
    /** Ticks after it is called before it can crash (it starts on the ground). */
    private static final int GRACE = 12;

    private static final EntityDataAccessor<Optional<UUID>> DATA_OWNER = SynchedEntityData.defineId(RiddenComet.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<Float> DATA_SPEED = SynchedEntityData.defineId(RiddenComet.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> DATA_DIVE = SynchedEntityData.defineId(RiddenComet.class, EntityDataSerializers.INT);

    private int life = 160;
    private float damage = 6;
    private final Map<UUID, Integer> rammed = new HashMap<>();

    public RiddenComet(EntityType<? extends RiddenComet> entityType, Level level) {
        super(entityType, level);
        this.xpReward = 0;
        setNoGravity(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 40).add(Attributes.MOVEMENT_SPEED, .5);
    }

    /** Calls a comet under {@code rider}, who rides it at once. */
    public static RiddenComet call(Level level, LivingEntity rider, int life, float speed, float damage) {
        RiddenComet comet = new RiddenComet(ModEntities.RIDDEN_COMET.get(), level);
        comet.entityData.set(DATA_OWNER, Optional.of(rider.getUUID()));
        comet.entityData.set(DATA_SPEED, speed);
        comet.life = life;
        comet.damage = damage;
        comet.moveTo(rider.getX(), rider.getY() + .4, rider.getZ(), rider.getYRot(), 0);
        comet.setDeltaMovement(rider.getLookAngle().scale(.3).add(0, .55, 0));
        level.addFreshEntity(comet);
        rider.startRiding(comet, true);
        CosmosFx.sound(level, rider.position(), SoundEvents.FIREWORK_ROCKET_LAUNCH, 1.5f, .6f);
        CosmosFx.sound(level, rider.position(), SoundEvents.BEACON_ACTIVATE, 1, 1.6f);
        SpellUtil.particles(level, new BlastwaveParticleOptions(.62f, .9f, 1f, 4), rider.getX(), rider.getY() + .1, rider.getZ(), 1, 0, 0, 0, 0);
        CosmosFx.stars(level, rider.position().add(0, .5, 0), 30, 1);
        return comet;
    }

    @Nullable
    public static RiddenComet of(Level level, LivingEntity rider) {
        return rider.getVehicle() instanceof RiddenComet comet ? comet : level.getEntitiesOfClass(RiddenComet.class, rider.getBoundingBox().inflate(64),
                c -> rider.getUUID().equals(c.getSummonerUUID())).stream().findFirst().orElse(null);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_OWNER, Optional.empty());
        builder.define(DATA_SPEED, .8f);
        builder.define(DATA_DIVE, -1);
    }

    public float getFlySpeed() {
        return this.entityData.get(DATA_SPEED);
    }

    /** The tick it began its dive, -1 while it flies. */
    public int getDive() {
        return this.entityData.get(DATA_DIVE);
    }

    /** Dives to crash where it is headed. */
    public void dive() {
        if (getDive() < 0) {
            this.entityData.set(DATA_DIVE, tickCount);
            CosmosFx.sound(level(), position(), SoundEvents.FIREWORK_ROCKET_LARGE_BLAST_FAR, 1.5f, .6f);
        }
    }

    // ---------------------------------------------------------------- flight

    @Override
    @Nullable
    public LivingEntity getControllingPassenger() {
        return getFirstPassenger() instanceof LivingEntity living ? living : null;
    }

    @Override
    protected void tickRidden(net.minecraft.world.entity.player.Player player, Vec3 input) {
        super.tickRidden(player, input);
        player.fallDistance = 0;
    }

    @Override
    protected Vec3 getRiddenInput(net.minecraft.world.entity.player.Player player, Vec3 input) {
        return Vec3.ZERO;
    }

    @Override
    public void travel(Vec3 input) {
        // Moved by whoever simulates it: the rider's client, or the server when nobody rides it
        if (!isControlledByLocalInstance()) {
            return;
        }
        Vec3 v = getDeltaMovement();
        double speed = v.length();
        Vec3 dir = speed < 1.0E-3 ? Vec3.directionFromRotation(getXRot(), getYRot()) : v.scale(1 / speed);
        LivingEntity rider = getControllingPassenger();
        Vec3 look = rider != null ? rider.getLookAngle() : dir.add(0, -.15, 0).normalize();
        float wanted = getFlySpeed();
        if (getDive() >= 0) {
            look = look.add(0, -.6, 0).normalize();
            wanted = DIVE_SPEED;
        }
        dir = dir.lerp(look, getDive() >= 0 ? .3 : .16).normalize();
        speed = Mth.lerp(.1, speed, wanted);
        v = dir.scale(speed);
        setDeltaMovement(v);
        move(MoverType.SELF, v);
        float yaw = (float) (Mth.atan2(-v.x, v.z) * Mth.RAD_TO_DEG);
        float pitch = (float) (-Mth.atan2(v.y, v.horizontalDistance()) * Mth.RAD_TO_DEG);
        setRot(yaw, pitch);
        this.yBodyRot = this.yHeadRot = yaw;
    }

    @Override
    protected Vec3 getPassengerAttachmentPoint(Entity passenger, EntityDimensions dimensions, float scale) {
        return new Vec3(0, 1.25, 0);
    }

    @Override
    protected void removePassenger(Entity passenger) {
        super.removePassenger(passenger);
        if (!level().isClientSide && passenger instanceof LivingEntity living) {
            living.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 80, 0, false, false, true));
            living.resetFallDistance();
            // Without its rider it dives where it was going
            dive();
        }
    }

    // ---------------------------------------------------------------- life

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            Vec3 back = position().subtract(xo, yo, zo);
            for (int k = 0; k < 3; k++) {
                level().addParticle(k == 0 ? ParticleTypes.END_ROD : CosmosFx.ICE, getRandomX(1), getY() + .6 + random.nextDouble() * .6, getRandomZ(1),
                        -back.x * .2, -back.y * .2, -back.z * .2);
            }
            return;
        }
        if (!(level() instanceof ServerLevel level)) {
            return;
        }
        if (tickCount > life) {
            dive();
        }
        int dive = getDive();
        boolean hit = tickCount > GRACE && (horizontalCollision || verticalCollision || onGround());
        if (hit || dive >= 0 && tickCount - dive > DIVE_TICKS) {
            crash(level);
            return;
        }
        ram(level);
        if (tickCount % 10 == 0) {
            CosmosFx.sound(level, position(), SoundEvents.ELYTRA_FLYING, .6f, 1.4f);
        }
    }

    /** Whatever is in its way is struck and thrown aside. */
    private void ram(ServerLevel level) {
        LivingEntity owner = getSummoner();
        Vec3 v = getDeltaMovement();
        AbstractSpell spell = ModSpells.COMET_RIDE.get();
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(1.2),
                e -> e != this && !hasPassenger(e) && e.isAlive() && !Cosmos.isAlly(owner != null ? owner : this, e))) {
            Integer last = rammed.get(e.getUUID());
            if (last != null && tickCount - last < 10) {
                continue;
            }
            rammed.put(e.getUUID(), tickCount);
            DamageSources.applyDamage(e, damage * (1 + (float) v.length() * .5f), SpellUtil.damageSource(spell, this, owner));
            Vec3 push = v.lengthSqr() < 1.0E-4 ? e.position().subtract(position()).normalize() : v.normalize();
            e.push(push.x * 1.3, .5, push.z * 1.3);
            e.hurtMarked = true;
            Vec3 c = e.getBoundingBox().getCenter();
            SpellUtil.particles(level, ParticleTypes.CRIT, c.x, c.y, c.z, 14, .3, .3, .3, .4);
            CosmosFx.stars(level, c, 10, .4);
            CosmosFx.sound(level, c, SoundEvents.ANVIL_LAND, .5f, 1.8f);
        }
    }

    /** It crashes: a blast all round, a crater of burning ground, its rider thrown clear. */
    private void crash(ServerLevel level) {
        LivingEntity owner = getSummoner();
        Vec3 at = position();
        float radius = 4.5f;
        for (Entity passenger : getPassengers()) {
            passenger.stopRiding();
            if (passenger instanceof LivingEntity living) {
                living.setDeltaMovement(getDeltaMovement().scale(-.2).add(0, .9, 0));
                living.hurtMarked = true;
                living.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 80, 0, false, false, true));
                living.resetFallDistance();
            }
        }
        AbstractSpell spell = ModSpells.COMET_RIDE.get();
        for (LivingEntity e : SpellUtil.enemiesAround(level, at, radius, owner != null ? owner : this, e -> e != this)) {
            double d = e.position().distanceTo(at);
            DamageSources.applyDamage(e, damage * 2.5f * (float) (1 - d / (radius * 1.5)), SpellUtil.damageSource(spell, this, owner));
            Vec3 push = e.position().subtract(at).multiply(1, 0, 1);
            push = push.lengthSqr() < 1.0E-4 ? new Vec3(1, 0, 0) : push.normalize();
            e.push(push.x * 1.2, .7, push.z * 1.2);
            e.hurtMarked = true;
        }
        CometCrater.mark(level, at, radius);
        SpellUtil.particles(level, new BlastwaveParticleOptions(.62f, .9f, 1f, radius * 2.4f), at.x, at.y + .1, at.z, 1, 0, 0, 0, 0);
        SpellUtil.particles(level, new BlastwaveParticleOptions(1f, .55f, .15f, radius * 1.6f), at.x, at.y + .15, at.z, 1, 0, 0, 0, 0);
        SpellUtil.particles(level, ParticleTypes.EXPLOSION_EMITTER, at.x, at.y + .5, at.z, 1, 0, 0, 0, 0);
        SpellUtil.particles(level, ParticleTypes.CAMPFIRE_COSY_SMOKE, at.x, at.y + .5, at.z, 20, radius * .4, .3, radius * .4, .02);
        CosmosFx.stars(level, at.add(0, 1, 0), 60, radius * .5);
        SpellUtil.cameraShake(level, at, 18, 20);
        CosmosFx.sound(level, at, SoundEvents.GENERIC_EXPLODE.value(), 2, .8f);
        CosmosFx.sound(level, at, SoundEvents.FIREWORK_ROCKET_LARGE_BLAST, 1.5f, .7f);
        discard();
    }

    // ---------------------------------------------------------------- summon

    @Override
    @Nullable
    public UUID getSummonerUUID() {
        return this.entityData.get(DATA_OWNER).orElse(null);
    }

    @Override
    @Nullable
    public LivingEntity getSummoner() {
        UUID uuid = getSummonerUUID();
        return uuid != null && level() instanceof ServerLevel level && level.getEntity(uuid) instanceof LivingEntity living ? living : null;
    }

    @Override
    public AbstractSpell getSummoningSpell() {
        return ModSpells.COMET_RIDE.get();
    }

    @Override
    public void dismissSummon() {
        dive();
    }

    @Override
    public void onAntiMagic(MagicData playerMagicData) {
        ejectPassengers();
        discard();
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return false;
    }

    @Override
    public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
        return false;
    }

    /** Its rider's head may go through a low ceiling: that never suffocates them. */
    @SubscribeEvent
    public static void onRiderHurt(LivingIncomingDamageEvent event) {
        if ((event.getSource().is(DamageTypes.IN_WALL) || event.getSource().is(DamageTypes.FLY_INTO_WALL)) && event.getEntity().getVehicle() instanceof RiddenComet) {
            event.setCanceled(true);
        }
    }

    @Override
    public boolean isAlliedTo(Entity other) {
        UUID owner = getSummonerUUID();
        return owner != null && (owner.equals(other.getUUID()) || other instanceof OwnedSummon summon && owner.equals(summon.getSummonerUUID())) || super.isAlliedTo(other);
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected void doPush(Entity entity) {
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 192 * 192;
    }
}
