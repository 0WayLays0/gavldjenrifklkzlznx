package com.waylays.arcanes.entity.cosmos;

import com.waylays.arcanes.entity.SpellEffectEntity;
import com.waylays.arcanes.registry.ModEntities;
import com.waylays.arcanes.spells.SpellUtil;
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
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * A small planet called down over an enemy, which is torn off the ground and swept into orbit round it, faster and
 * faster, struck at each turn. Cast again, the caster slings it out of orbit where they look: it flies off like a
 * meteor and whatever it crashes into, it crashes into hard. Left alone, the planet lets it go flying at the end.
 */
@net.neoforged.fml.common.EventBusSubscriber(modid = com.waylays.arcanes.ArcanesOublies.MODID)
public class CaptivePlanet extends SpellEffectEntity {
    private static final String TAG = "arcanes_oublies_orbit";
    public static final float ORBIT = 3.2f;
    public static final float START_SPIN = .07f;
    public static final float SPIN_UP = .0035f;
    public static final float MAX_SPIN = .36f;
    /** Ticks a slung target is watched for its crash. */
    public static final int FLIGHT_TICKS = 60;

    private static final EntityDataAccessor<Integer> DATA_TARGET = SynchedEntityData.defineId(CaptivePlanet.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_KIND = SynchedEntityData.defineId(CaptivePlanet.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DATA_TILT = SynchedEntityData.defineId(CaptivePlanet.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_SPIN = SynchedEntityData.defineId(CaptivePlanet.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> DATA_SLUNG = SynchedEntityData.defineId(CaptivePlanet.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_LIFE = SynchedEntityData.defineId(CaptivePlanet.class, EntityDataSerializers.INT);

    @Nullable
    private String spellId;
    private float angle;
    private float startDistance = ORBIT;
    private float lastSpeed;
    private boolean hadGravity = true;
    /** Client: where the target has been lately, for its trail. */
    public final Deque<Vec3> trail = new ArrayDeque<>();

    public CaptivePlanet(EntityType<? extends CaptivePlanet> entityType, Level level) {
        super(entityType, level);
    }

    public static CaptivePlanet capture(Level level, LivingEntity caster, LivingEntity target, AbstractSpell spell, int life, float damage) {
        for (CaptivePlanet old : level.getEntitiesOfClass(CaptivePlanet.class, caster.getBoundingBox().inflate(64), p -> caster.getUUID().equals(p.getOwnerUUID()))) {
            old.release(null);
        }
        CaptivePlanet planet = new CaptivePlanet(ModEntities.CAPTIVE_PLANET.get(), level);
        planet.setOwner(caster);
        planet.spellId = spell.getSpellId();
        planet.damage = damage;
        Vec3 at = target.position().add(0, target.getBbHeight() + 2.6, 0);
        planet.moveTo(at.x, at.y, at.z, caster.getYRot(), 0);
        planet.entityData.set(DATA_TARGET, target.getId());
        planet.entityData.set(DATA_KIND, level.random.nextInt(4));
        planet.entityData.set(DATA_TILT, 12 + level.random.nextFloat() * 18);
        planet.entityData.set(DATA_SPIN, START_SPIN);
        planet.entityData.set(DATA_LIFE, life);
        Vec3 d = target.getBoundingBox().getCenter().subtract(at);
        planet.angle = (float) Math.atan2(d.z, d.x);
        planet.startDistance = (float) Math.max(1, d.horizontalDistance());
        level.addFreshEntity(planet);
        CosmosFx.hum(level, at, 1.4f, .6f);
        CosmosFx.stars(level, at, 30, 1.2);
        CosmosFx.sound(level, at, SoundEvents.ILLUSIONER_PREPARE_MIRROR, 1, .6f);
        return planet;
    }

    /** The caster's planet holding an enemy, if any. */
    @Nullable
    public static CaptivePlanet of(Level level, LivingEntity caster) {
        for (CaptivePlanet p : level.getEntitiesOfClass(CaptivePlanet.class, caster.getBoundingBox().inflate(64),
                p -> caster.getUUID().equals(p.getOwnerUUID()) && p.getSlungAt() < 0)) {
            return p;
        }
        return null;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_TARGET, -1);
        builder.define(DATA_KIND, 0);
        builder.define(DATA_TILT, 20f);
        builder.define(DATA_SPIN, START_SPIN);
        builder.define(DATA_SLUNG, -1);
        builder.define(DATA_LIFE, 200);
    }

    @Nullable
    public LivingEntity getTarget() {
        return level().getEntity(this.entityData.get(DATA_TARGET)) instanceof LivingEntity living ? living : null;
    }

    /** Which planet it is (0 the ringed gas giant, 1 the blue world, 2 the ice giant, 3 the molten world). */
    public int getKind() {
        return this.entityData.get(DATA_KIND);
    }

    /** How far its orbit is tilted, in degrees. */
    public float getTilt() {
        return this.entityData.get(DATA_TILT);
    }

    /** Radians per tick the target goes round. */
    public float getSpin() {
        return this.entityData.get(DATA_SPIN);
    }

    /** The tick its target was slung out of orbit, -1 if it is still held. */
    public int getSlungAt() {
        return this.entityData.get(DATA_SLUNG);
    }

    public int getLife() {
        return this.entityData.get(DATA_LIFE);
    }

    /** A point of the orbit, from the planet's centre, at {@code a} radians round and {@code r} out. */
    public Vec3 orbitPoint(float a, float r) {
        float tilt = getTilt() * Mth.DEG_TO_RAD;
        float x = Mth.cos(a) * r, z = Mth.sin(a) * r;
        // Tilted about the planet's own X axis, turned by its yaw
        float y = -z * Mth.sin(tilt);
        z = z * Mth.cos(tilt);
        float yaw = getYRot() * Mth.DEG_TO_RAD;
        return new Vec3(x * Mth.cos(yaw) - z * Mth.sin(yaw), y - 1.1, x * Mth.sin(yaw) + z * Mth.cos(yaw));
    }

    @Override
    public void tick() {
        super.tick();
        LivingEntity target = getTarget();
        if (level().isClientSide) {
            if (target != null) {
                trail.addFirst(target.getBoundingBox().getCenter());
                while (trail.size() > (getSlungAt() >= 0 ? 24 : 14)) {
                    trail.removeLast();
                }
            }
            return;
        }
        if (!(level() instanceof ServerLevel level)) {
            return;
        }
        int slung = getSlungAt();
        if (slung >= 0) {
            flight(level, target, tickCount - slung);
            return;
        }
        if (target == null || !target.isAlive()) {
            discard();
            return;
        }
        if (tickCount >= getLife()) {
            // Let go along the way it was going
            Vec3 tangent = orbitPoint(angle + .2f, ORBIT).subtract(orbitPoint(angle, ORBIT)).normalize();
            release(tangent);
            return;
        }
        float spin = Math.min(MAX_SPIN, START_SPIN + SPIN_UP * tickCount);
        this.entityData.set(DATA_SPIN, spin);
        float before = angle;
        angle += spin;
        float r = Mth.lerp(Math.min(1, tickCount / 12f), startDistance, ORBIT);
        Vec3 goal = position().add(orbitPoint(angle, r)).subtract(0, target.getBbHeight() * .5, 0);
        Vec3 step = goal.subtract(target.position());
        if (target instanceof ServerPlayer) {
            target.setDeltaMovement(step);
        } else {
            if (tickCount == 1) {
                hadGravity = !target.isNoGravity();
                if (hadGravity) {
                    target.getPersistentData().putBoolean(TAG, true);
                }
            }
            target.setNoGravity(true);
            target.setPos(goal.x, goal.y, goal.z);
            target.setDeltaMovement(step);
        }
        target.hurtMarked = true;
        target.resetFallDistance();
        // A blow at each turn
        if (Math.floor(before / Mth.TWO_PI) != Math.floor(angle / Mth.TWO_PI)) {
            AbstractSpell spell = spellId == null ? null : SpellRegistry.getSpell(spellId);
            if (spell != null) {
                target.invulnerableTime = 0;
                DamageSources.applyDamage(target, damage * .4f, SpellUtil.damageSource(spell, this, getOwner()));
            }
            CosmosFx.sound(level, target.position(), SoundEvents.ELYTRA_FLYING, .5f, 1.6f + spin);
        }
        if (tickCount % 3 == 0) {
            Vec3 c = target.getBoundingBox().getCenter();
            SpellUtil.particles(level, CosmosFx.STAR, c.x, c.y, c.z, 2, .2, .2, .2, 0);
        }
    }

    /** Lets the target go: flung along {@code direction} (at the speed of its orbit), or just dropped. */
    public void release(@Nullable Vec3 direction) {
        LivingEntity target = getTarget();
        if (target != null && !(target instanceof ServerPlayer)) {
            target.setNoGravity(!hadGravity);
            target.getPersistentData().remove(TAG);
        }
        if (direction == null || target == null || !(level() instanceof ServerLevel level)) {
            discard();
            return;
        }
        float speed = 1.1f + getSpin() * 4.5f;
        Vec3 v = direction.normalize().scale(speed).add(0, .15, 0);
        target.setDeltaMovement(v);
        target.hurtMarked = true;
        lastSpeed = speed;
        this.entityData.set(DATA_SLUNG, tickCount);
        CosmosFx.sound(level, target.position(), SoundEvents.TRIDENT_RIPTIDE_3.value(), 1.2f, 1.2f);
        CosmosFx.sound(level, target.position(), SoundEvents.FIREWORK_ROCKET_LAUNCH, 1, .8f);
    }

    /** Watching the slung target for its crash. */
    private void flight(ServerLevel level, @Nullable LivingEntity target, int since) {
        if (target == null || !target.isAlive() || since > FLIGHT_TICKS) {
            discard();
            return;
        }
        Vec3 c = target.getBoundingBox().getCenter();
        SpellUtil.particles(level, ParticleTypes.END_ROD, c.x, c.y, c.z, 2, .15, .15, .15, .01);
        float speed = (float) target.getDeltaMovement().length();
        boolean crashed = since > 1 && (target.horizontalCollision || target.verticalCollision && lastSpeed > .5f || target.onGround() && since > 4);
        if (crashed) {
            crash(level, target, Math.max(speed, lastSpeed));
            discard();
            return;
        }
        lastSpeed = speed;
        target.resetFallDistance();
    }

    private void crash(ServerLevel level, LivingEntity target, float speed) {
        AbstractSpell spell = spellId == null ? null : SpellRegistry.getSpell(spellId);
        Entity owner = getOwner();
        Vec3 c = target.position();
        if (spell != null) {
            target.invulnerableTime = 0;
            DamageSources.applyDamage(target, damage * (1 + speed * 1.6f), SpellUtil.damageSource(spell, this, owner));
            for (LivingEntity near : SpellUtil.enemiesAround(level, c.add(0, 1, 0), 3, owner, e -> e != target)) {
                DamageSources.applyDamage(near, damage * .6f * (1 + speed), SpellUtil.damageSource(spell, this, owner));
                Vec3 push = near.position().subtract(c).multiply(1, 0, 1).normalize().scale(.7);
                near.push(push.x, .4, push.z);
                near.hurtMarked = true;
            }
        }
        SpellUtil.particles(level, new BlastwaveParticleOptions(.62f, .7f, 1f, 4 + speed * 2), c.x, c.y + .1, c.z, 1, 0, 0, 0, 0);
        SpellUtil.particles(level, ParticleTypes.EXPLOSION, c.x, c.y + .5, c.z, 2, .5, .3, .5, 0);
        CosmosFx.stars(level, c.add(0, 1, 0), 40, 1.2);
        SpellUtil.cameraShake(level, c, 10, 14);
        CosmosFx.sound(level, c, SoundEvents.GENERIC_EXPLODE.value(), 1, 1.2f);
        CosmosFx.sound(level, c, SoundEvents.ANVIL_LAND, .7f, .8f);
    }

    @Override
    public void remove(RemovalReason reason) {
        LivingEntity target = level().isClientSide ? null : getTarget();
        if (target != null && !(target instanceof ServerPlayer) && getSlungAt() < 0 && target.isNoGravity() && hadGravity) {
            target.setNoGravity(false);
            target.getPersistentData().remove(TAG);
        }
        super.remove(reason);
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 128 * 128;
    }

    /** A creature saved while held in orbit (the world closed mid-spell) comes back with its own weight. */
    @net.neoforged.bus.api.SubscribeEvent
    public static void onJoin(net.neoforged.neoforge.event.entity.EntityJoinLevelEvent event) {
        Entity e = event.getEntity();
        if (!e.level().isClientSide && e.getPersistentData().getBoolean(TAG)) {
            e.getPersistentData().remove(TAG);
            e.setNoGravity(false);
        }
    }
}
