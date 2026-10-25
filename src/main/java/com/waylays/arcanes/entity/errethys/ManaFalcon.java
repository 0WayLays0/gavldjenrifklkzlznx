package com.waylays.arcanes.entity.errethys;

import com.waylays.arcanes.ArcanesOublies;
import com.waylays.arcanes.entity.OwnedSummon;
import com.waylays.arcanes.registry.ModSpells;
import com.waylays.arcanes.spells.errethys.ErrethysFx;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.entity.mobs.AntiMagicSusceptible;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.UUID;

/**
 * A mana falcon, big enough to carry its summoner: they ride it (right click) and it flies where they look, fast.
 * Forward follows the eyes (look up to climb, down to dive), sideways keys strafe, no key glides on, sneak gets off
 * (with a slow fall). Without its rider it circles above its summoner, waiting to be ridden again, until its time is
 * up; it fades if its summoner is gone.
 */
@EventBusSubscriber(modid = ArcanesOublies.MODID)
public class ManaFalcon extends Mob implements OwnedSummon, AntiMagicSusceptible {
    /** How long it waits for a summoner who is gone (out of the world) before fading. */
    public static final int OWNERLESS_TICKS = 200;

    private static final EntityDataAccessor<Optional<UUID>> DATA_OWNER = SynchedEntityData.defineId(ManaFalcon.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<Float> DATA_SPEED = SynchedEntityData.defineId(ManaFalcon.class, EntityDataSerializers.FLOAT);
    private int lifeTicks = 1200;
    private int ownerless;
    private float circle;

    public ManaFalcon(EntityType<? extends ManaFalcon> entityType, Level level) {
        super(entityType, level);
        this.xpReward = 0;
        setNoGravity(true);
        setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 15)
                .add(Attributes.MOVEMENT_SPEED, .5);
    }

    /** Called once by the spell: owner, lifetime, toughness and top speed (blocks per tick). */
    public void setup(LivingEntity owner, int lifeTicks, double health, float speed) {
        this.entityData.set(DATA_OWNER, Optional.of(owner.getUUID()));
        this.entityData.set(DATA_SPEED, speed);
        this.lifeTicks = lifeTicks;
        var maxHealth = getAttribute(Attributes.MAX_HEALTH);
        if (maxHealth != null) {
            maxHealth.setBaseValue(health);
        }
        setHealth(getMaxHealth());
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_OWNER, Optional.empty());
        builder.define(DATA_SPEED, .9f);
    }

    public float getFlySpeed() {
        return this.entityData.get(DATA_SPEED);
    }

    public int getLifeTicks() {
        return lifeTicks;
    }

    @Override
    @Nullable
    public UUID getSummonerUUID() {
        return this.entityData.get(DATA_OWNER).orElse(null);
    }

    @Override
    @Nullable
    public LivingEntity getSummoner() {
        UUID uuid = getSummonerUUID();
        if (uuid != null && level() instanceof ServerLevel serverLevel && serverLevel.getEntity(uuid) instanceof LivingEntity living) {
            return living;
        }
        return null;
    }

    @Override
    public AbstractSpell getSummoningSpell() {
        return ModSpells.MANA_FALCON.get();
    }

    @Override
    public void dismissSummon() {
        dissolve();
    }

    @Override
    public void onAntiMagic(MagicData playerMagicData) {
        dissolve();
    }

    // ---------------------------------------------------------------- riding

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (!isVehicle() && !player.isPassenger() && player.getUUID().equals(getSummonerUUID())) {
            if (!level().isClientSide) {
                player.startRiding(this);
            }
            return InteractionResult.sidedSuccess(level().isClientSide);
        }
        return InteractionResult.PASS;
    }

    @Override
    @Nullable
    public LivingEntity getControllingPassenger() {
        return getFirstPassenger() instanceof Player player ? player : null;
    }

    @Override
    protected void tickRidden(Player player, Vec3 input) {
        super.tickRidden(player, input);
        // It banks its head with its rider's gaze
        setRot(player.getYRot(), Mth.clamp(player.getXRot() * .6f, -35, 35));
        this.yRotO = this.yBodyRot = this.yHeadRot = getYRot();
        player.fallDistance = 0;
    }

    @Override
    protected Vec3 getRiddenInput(Player player, Vec3 input) {
        return new Vec3(player.xxa * .8, 0, player.zza);
    }

    @Override
    protected float getRiddenSpeed(Player player) {
        return getFlySpeed();
    }

    @Override
    public void travel(Vec3 input) {
        // Moved by whoever simulates it: the rider's client, or the server when nobody rides it
        if (!isControlledByLocalInstance()) {
            return;
        }
        Vec3 v = getDeltaMovement();
        float speed = getFlySpeed();
        if (isAlive() && getControllingPassenger() instanceof Player rider) {
            float yaw = rider.getYRot() * Mth.DEG_TO_RAD;
            float pitch = rider.getXRot() * Mth.DEG_TO_RAD;
            Vec3 forward = new Vec3(-Mth.sin(yaw) * Mth.cos(pitch), -Mth.sin(pitch), Mth.cos(yaw) * Mth.cos(pitch));
            Vec3 left = new Vec3(Mth.cos(yaw), 0, Mth.sin(yaw));
            Vec3 wish = forward.scale(input.z < 0 ? input.z * .5 : input.z).add(left.scale(input.x));
            if (wish.lengthSqr() > 1) {
                wish = wish.normalize();
            }
            v = v.scale(.92).add(wish.scale(speed * .1));
            if (v.lengthSqr() > speed * speed) {
                v = v.normalize().scale(speed);
            }
            if (wish.lengthSqr() < 1.0E-4) {
                // Gliding: it keeps a little of its way and hangs on the air
                v = v.scale(.97).add(0, Mth.sin(tickCount * .1f) * .004, 0);
            }
        } else {
            v = riderless(v, speed);
        }
        setDeltaMovement(v);
        move(MoverType.SELF, v);
        calculateEntityAnimation(false);
    }

    /** Without its rider: wide circles above its summoner, facing the way it flies. */
    private Vec3 riderless(Vec3 v, float speed) {
        LivingEntity owner = getSummoner();
        if (owner == null || level().isClientSide) {
            return v.scale(.9).add(0, Mth.sin(tickCount * .1f) * .004, 0);
        }
        circle += .04f;
        Vec3 goal = owner.position().add(Mth.cos(circle) * 5, owner.getBbHeight() + 3.5 + Mth.sin(circle * .5f), Mth.sin(circle) * 5);
        Vec3 to = goal.subtract(position());
        double distance = to.length();
        Vec3 wish = distance < 1.0E-3 ? Vec3.ZERO : to.scale(Math.min(1, distance / 4) / distance);
        v = v.scale(.9).add(wish.scale(speed * .06));
        if (v.horizontalDistanceSqr() > 1.0E-4) {
            float yaw = (float) (Mth.atan2(-v.x, v.z) * Mth.RAD_TO_DEG);
            setRot(Mth.approachDegrees(getYRot(), yaw, 10), (float) Mth.clamp(-Mth.atan2(v.y, v.horizontalDistance()) * Mth.RAD_TO_DEG, -30, 30));
            this.yBodyRot = this.yHeadRot = getYRot();
        }
        return v;
    }

    /** The rider sits on its back, between its wings. */
    @Override
    protected Vec3 getPassengerAttachmentPoint(Entity passenger, EntityDimensions dimensions, float scale) {
        return new Vec3(0, 1.15, .1);
    }

    /** A rider getting off in mid-air floats down. */
    @Override
    protected void removePassenger(Entity passenger) {
        super.removePassenger(passenger);
        if (!level().isClientSide && passenger instanceof LivingEntity living && !onGround()) {
            living.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 160, 0, false, false, true));
        }
    }

    // ---------------------------------------------------------------- life

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            Vec3 motion = position().subtract(xo, yo, zo);
            if (random.nextFloat() < .15 + motion.length() * .8) {
                // Motes of mana stream off its wingtips
                float yaw = getYRot() * Mth.DEG_TO_RAD;
                Vec3 side = new Vec3(Mth.cos(yaw), 0, Mth.sin(yaw)).scale(random.nextBoolean() ? 1.8 : -1.8);
                level().addParticle(random.nextBoolean() ? ErrethysFx.SKY_DUST : ErrethysFx.PALE_DUST,
                        getX() + side.x, getY() + .7, getZ() + side.z, -motion.x * .3, 0, -motion.z * .3);
            }
            return;
        }
        if (--lifeTicks <= 0) {
            dissolve();
            return;
        }
        ownerless = getSummoner() == null ? ownerless + 1 : 0;
        if (ownerless > OWNERLESS_TICKS) {
            dissolve();
            return;
        }
        if (tickCount % 14 == 0 && getDeltaMovement().y > -.1) {
            level().playSound(null, getX(), getY(), getZ(), SoundEvents.ENDER_DRAGON_FLAP, SoundSource.NEUTRAL, .35f, 1.6f + random.nextFloat() * .2f);
        }
    }

    /** Sets its rider down and comes apart into motes of mana. */
    public void dissolve() {
        if (!level().isClientSide) {
            ejectPassengers();
            ErrethysFx.dissolve(level(), getBoundingBox().getCenter(), 2.5f);
        }
        discard();
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        if (!level().isClientSide) {
            dissolve();
        }
    }

    /** Its summoner and its rider never hurt it by mistake; walls, falls and crowds do not hurt it either. */
    @Override
    public boolean hurt(DamageSource source, float amount) {
        Entity attacker = source.getEntity();
        if (attacker != null && (attacker.getUUID().equals(getSummonerUUID()) || hasPassenger(attacker))) {
            return false;
        }
        if (source.is(DamageTypeTags.IS_FALL) || source.is(DamageTypes.IN_WALL) || source.is(DamageTypes.CRAMMING)) {
            return false;
        }
        return super.hurt(source, amount);
    }

    /** Its rider's head may go through a low ceiling: that never suffocates them. */
    @SubscribeEvent
    public static void onRiderHurt(LivingIncomingDamageEvent event) {
        if (event.getSource().is(DamageTypes.IN_WALL) && event.getEntity().getVehicle() instanceof ManaFalcon) {
            event.setCanceled(true);
        }
    }

    @Override
    public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    public boolean isAlliedTo(Entity other) {
        UUID owner = getSummonerUUID();
        if (owner != null && (owner.equals(other.getUUID()) || other instanceof OwnedSummon summon && owner.equals(summon.getSummonerUUID()))) {
            return true;
        }
        return super.isAlliedTo(other);
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected void doPush(Entity entity) {
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.PARROT_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.AMETHYST_CLUSTER_BREAK;
    }

    @Override
    public float getVoicePitch() {
        return .6f;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("LifeTicks", lifeTicks);
        tag.putFloat("FlySpeed", getFlySpeed());
        UUID owner = getSummonerUUID();
        if (owner != null) {
            tag.putUUID("Owner", owner);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        lifeTicks = tag.getInt("LifeTicks");
        this.entityData.set(DATA_SPEED, tag.getFloat("FlySpeed"));
        if (tag.hasUUID("Owner")) {
            this.entityData.set(DATA_OWNER, Optional.of(tag.getUUID("Owner")));
        }
    }
}
