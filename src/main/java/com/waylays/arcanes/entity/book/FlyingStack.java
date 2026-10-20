package com.waylays.arcanes.entity.book;

import com.waylays.arcanes.ArcanesOublies;
import com.waylays.arcanes.entity.OwnedSummon;
import com.waylays.arcanes.registry.ModSpells;
import com.waylays.arcanes.spells.SpellUtil;
import com.waylays.arcanes.spells.book.BookFx;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.entity.mobs.AntiMagicSusceptible;
import net.minecraft.core.particles.ParticleTypes;
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

import java.util.Optional;
import java.util.UUID;
import javax.annotation.Nullable;

/**
 * A flying stack of paper (Flying Stack): its summoner sits on it and flies where they look. Forward follows the eyes
 * (look up to climb, down to dive), sideways keys strafe, no key hovers, sneak gets off (with a slow fall). It stays
 * a few seconds without a rider for its summoner to climb back on (right click), then scatters; it scatters too at
 * the end of its time, when shot down or when the spell is cast again.
 */
@EventBusSubscriber(modid = ArcanesOublies.MODID)
public class FlyingStack extends Mob implements OwnedSummon, AntiMagicSusceptible {
    /** How long it waits for its rider before scattering. */
    public static final int RIDERLESS_TICKS = 100;

    private static final EntityDataAccessor<Optional<UUID>> DATA_OWNER = SynchedEntityData.defineId(FlyingStack.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<Float> DATA_SPEED = SynchedEntityData.defineId(FlyingStack.class, EntityDataSerializers.FLOAT);
    private int lifeTicks = 1200;
    private int riderless;
    @Nullable
    private Vec3 lastSoundPos;

    public FlyingStack(EntityType<? extends FlyingStack> entityType, Level level) {
        super(entityType, level);
        this.xpReward = 0;
        setNoGravity(true);
        setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 30)
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
        builder.define(DATA_SPEED, .6f);
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
        return ModSpells.FLYING_STACK.get();
    }

    @Override
    public void dismissSummon() {
        scatter();
    }

    @Override
    public void onAntiMagic(MagicData playerMagicData) {
        scatter();
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
        setRot(player.getYRot(), 0);
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
        if (isAlive() && getControllingPassenger() instanceof Player rider) {
            // Forward along the rider's eyes, pitch included; strafing stays level
            float yaw = rider.getYRot() * Mth.DEG_TO_RAD;
            float pitch = rider.getXRot() * Mth.DEG_TO_RAD;
            Vec3 forward = new Vec3(-Mth.sin(yaw) * Mth.cos(pitch), -Mth.sin(pitch), Mth.cos(yaw) * Mth.cos(pitch));
            Vec3 left = new Vec3(Mth.cos(yaw), 0, Mth.sin(yaw));
            Vec3 wish = forward.scale(input.z < 0 ? input.z * .5 : input.z).add(left.scale(input.x));
            if (wish.lengthSqr() > 1) {
                wish = wish.normalize();
            }
            float speed = getFlySpeed();
            v = v.scale(.9).add(wish.scale(speed * .12));
            if (v.lengthSqr() > speed * speed) {
                v = v.normalize().scale(speed);
            }
            if (wish.lengthSqr() < 1.0E-4) {
                // Hovering: a gentle bob
                v = v.add(0, Mth.sin(tickCount * .12f) * .004, 0);
            }
        } else {
            v = v.scale(.8).add(0, Mth.sin(tickCount * .12f) * .004, 0);
        }
        setDeltaMovement(v);
        move(MoverType.SELF, v);
        calculateEntityAnimation(false);
    }

    /** The rider sits on top of the stack (a player hangs .6 below their attachment point). */
    @Override
    protected Vec3 getPassengerAttachmentPoint(Entity passenger, EntityDimensions dimensions, float scale) {
        return new Vec3(0, .55, 0);
    }

    /** A rider getting off in mid-air floats down like a loose page. */
    @Override
    protected void removePassenger(Entity passenger) {
        super.removePassenger(passenger);
        if (!level().isClientSide && passenger instanceof LivingEntity living && !onGround()) {
            living.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 120, 0, false, false, true));
        }
    }

    // ---------------------------------------------------------------- life

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            clientParticles();
            return;
        }
        if (--lifeTicks <= 0) {
            scatter();
            return;
        }
        riderless = isVehicle() ? 0 : riderless + 1;
        if (riderless > RIDERLESS_TICKS) {
            scatter();
            return;
        }
        // Its rider's client moves it, so tell how fast it goes from where it was
        if (tickCount % 8 == 0) {
            if (lastSoundPos != null && lastSoundPos.distanceToSqr(position()) > 2) {
                level().playSound(null, getX(), getY(), getZ(), SoundEvents.BOOK_PAGE_TURN, SoundSource.NEUTRAL, .6f, .8f + random.nextFloat() * .5f);
            }
            lastSoundPos = position();
        }
    }

    private void clientParticles() {
        // Loose sheets peel off the top and flutter away behind it, more when it goes fast
        Vec3 motion = position().subtract(xo, yo, zo);
        if (random.nextFloat() < .08 + motion.length() * .5) {
            Vec3 back = motion.scale(-1.5);
            level().addParticle(BookFx.paper(), getRandomX(.6), getY() + .45, getRandomZ(.6), back.x, .05 + back.y, back.z);
        }
        if (random.nextInt(3) == 0) {
            level().addParticle(ParticleTypes.ENCHANT, getRandomX(.8), getY() - .1, getRandomZ(.8), 0, -.3, 0);
        }
        if (random.nextInt(4) == 0) {
            level().addParticle(BookFx.VIOLET_DUST, getRandomX(.7), getY() - .05, getRandomZ(.7), 0, 0, 0);
        }
    }

    /** Sets its rider down and bursts into loose pages (no drops). */
    public void scatter() {
        if (!level().isClientSide) {
            ejectPassengers();
            Vec3 c = position().add(0, .3, 0);
            SpellUtil.particles(level(), BookFx.paper(), c.x, c.y, c.z, 30, .5, .3, .5, .25);
            SpellUtil.particles(level(), ParticleTypes.ENCHANT, c.x, c.y, c.z, 30, .5, .3, .5, 1);
            level().playSound(null, c.x, c.y, c.z, SoundEvents.BOOK_PUT, SoundSource.NEUTRAL, 1.2f, .8f);
        }
        discard();
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        if (!level().isClientSide) {
            scatter();
        }
    }

    /** Its summoner and its rider never tear it by mistake; walls, falls and crowds do not hurt it either. */
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
        if (event.getSource().is(DamageTypes.IN_WALL) && event.getEntity().getVehicle() instanceof FlyingStack) {
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
        return SoundEvents.BOOK_PAGE_TURN;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.BOOK_PUT;
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
