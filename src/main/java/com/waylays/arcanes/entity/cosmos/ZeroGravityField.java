package com.waylays.arcanes.entity.cosmos;

import com.waylays.arcanes.entity.SpellEffectEntity;
import com.waylays.arcanes.registry.ModEffects;
import com.waylays.arcanes.registry.ModEntities;
import com.waylays.arcanes.spells.SpellUtil;
import com.waylays.arcanes.spells.cosmos.Cosmos;
import com.waylays.arcanes.spells.cosmos.CosmosFx;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.damage.DamageSources;
import io.redspace.ironsspellbooks.particle.BlastwaveParticleOptions;
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
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * A field of zero gravity round where its caster stood: a bubble of faint starlight. Foes inside lose their weight and
 * drift helplessly up off the ground; their shots slow and hang in the air. The caster and their allies keep a moon's
 * gravity: great bounding leaps and soft landings. When the caster casts again, or when the field fails, gravity
 * comes back all at once: everything that floated is slammed down to the ground, the higher the harder.
 */
public class ZeroGravityField extends SpellEffectEntity {
    public static final int SLAM_TICKS = 40;
    public static final int OPEN_TICKS = 12;

    private static final EntityDataAccessor<Float> DATA_RADIUS = SynchedEntityData.defineId(ZeroGravityField.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> DATA_LIFE = SynchedEntityData.defineId(ZeroGravityField.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_SLAM = SynchedEntityData.defineId(ZeroGravityField.class, EntityDataSerializers.INT);

    @Nullable
    private String spellId;
    /** Foes that have floated, and the shots held in the air. */
    private final Set<Integer> floated = new HashSet<>();
    private final Set<Integer> held = new HashSet<>();
    /** Slammed foes, and the height they were slammed from. */
    private final Map<Integer, Double> slammed = new HashMap<>();

    public ZeroGravityField(EntityType<? extends ZeroGravityField> entityType, Level level) {
        super(entityType, level);
    }

    public static ZeroGravityField open(Level level, LivingEntity caster, AbstractSpell spell, float radius, int life, float damage) {
        ZeroGravityField old = of(level, caster);
        if (old != null) {
            old.slam();
        }
        ZeroGravityField field = new ZeroGravityField(ModEntities.ZERO_GRAVITY_FIELD.get(), level);
        field.setOwner(caster);
        field.spellId = spell.getSpellId();
        field.damage = damage;
        field.moveTo(caster.getX(), caster.getY(), caster.getZ(), 0, 0);
        field.entityData.set(DATA_RADIUS, radius);
        field.entityData.set(DATA_LIFE, life);
        level.addFreshEntity(field);
        CosmosFx.hum(level, caster.position(), 1.5f, .6f);
        CosmosFx.sound(level, caster.position(), SoundEvents.BEACON_POWER_SELECT, 1.2f, .5f);
        return field;
    }

    /** The caster's field that still holds. */
    @Nullable
    public static ZeroGravityField of(Level level, LivingEntity caster) {
        for (ZeroGravityField field : level.getEntitiesOfClass(ZeroGravityField.class, caster.getBoundingBox().inflate(64), f -> caster.getUUID().equals(f.getOwnerUUID()))) {
            if (!field.isSlamming()) {
                return field;
            }
        }
        return null;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_RADIUS, 8f);
        builder.define(DATA_LIFE, 200);
        builder.define(DATA_SLAM, -1);
    }

    public float getRadius() {
        return this.entityData.get(DATA_RADIUS);
    }

    public int getLife() {
        return this.entityData.get(DATA_LIFE);
    }

    /** The tick gravity came back, or -1. */
    public int getSlamStart() {
        return this.entityData.get(DATA_SLAM);
    }

    public boolean isSlamming() {
        return getSlamStart() >= 0;
    }

    /** How far the bubble is open (0 to 1) at {@code age}. */
    public float openness(float age) {
        float open = Mth.clamp(age / OPEN_TICKS, 0, 1);
        int slam = getSlamStart();
        if (slam >= 0) {
            open *= 1 - Mth.clamp((age - slam) / 6, 0, 1);
        }
        return open;
    }

    /** Inside the field: a dome as tall as it is wide, from a little under its foot. */
    public boolean contains(Entity e) {
        double r = getRadius();
        double dx = e.getX() - getX(), dz = e.getZ() - getZ();
        double y = e.getY() - getY();
        return dx * dx + dz * dz <= r * r && y > -1.5 && y < r;
    }

    /** Gravity comes back. */
    public void slam() {
        if (!isSlamming()) {
            this.entityData.set(DATA_SLAM, tickCount);
            if (level() instanceof ServerLevel level) {
                bringDown(level);
            }
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            if (!isSlamming() && tickCount > OPEN_TICKS) {
                float r = getRadius();
                for (int k = 0; k < 2; k++) {
                    double a = random.nextDouble() * Mth.TWO_PI, d = Math.sqrt(random.nextDouble()) * r;
                    level().addParticle(k == 0 ? ParticleTypes.END_ROD : ParticleTypes.WHITE_ASH, getX() + Math.cos(a) * d, getY() + random.nextDouble() * 1.5,
                            getZ() + Math.sin(a) * d, 0, .02 + random.nextDouble() * .03, 0);
                }
            }
            return;
        }
        if (!(level() instanceof ServerLevel level)) {
            return;
        }
        LivingEntity caster = getLivingOwner();
        AbstractSpell spell = spellId == null ? null : SpellRegistry.getSpell(spellId);
        if (spell == null) {
            discard();
            return;
        }
        if (!isSlamming() && (tickCount >= getLife() || caster == null || !caster.isAlive())) {
            slam();
        }
        if (isSlamming()) {
            land(level, caster, spell);
            if (tickCount - getSlamStart() > SLAM_TICKS || slammed.isEmpty() && tickCount - getSlamStart() > 8) {
                discard();
            }
            return;
        }
        float r = getRadius();
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(r + 1, r + 2, r + 1))) {
            if (!e.isAlive() || !contains(e) || e.isSpectator()) {
                continue;
            }
            if (Cosmos.isAlly(caster, e)) {
                e.addEffect(new MobEffectInstance(ModEffects.LOW_GRAVITY, 8, 0, true, false, true));
                e.resetFallDistance();
            } else if (!Cosmos.isBoss(e)) {
                e.addEffect(new MobEffectInstance(ModEffects.WEIGHTLESS, 8, 0, true, false, true));
                if (floated.add(e.getId())) {
                    // Lifted off its feet
                    e.setDeltaMovement(e.getDeltaMovement().multiply(.3, 0, .3).add(0, .3, 0));
                    CosmosFx.stars(level, e.position().add(0, .2, 0), 8, .3);
                } else {
                    // Drifting slowly up, with no footing to walk on
                    Vec3 v = e.getDeltaMovement();
                    e.setDeltaMovement(v.x * .9, Math.min(v.y, .06), v.z * .9);
                }
                e.hurtMarked = true;
                e.resetFallDistance();
            }
        }
        AABB box = getBoundingBox().inflate(r, r, r);
        for (Projectile shot : level.getEntitiesOfClass(Projectile.class, box)) {
            Entity shooter = shot.getOwner();
            if (!contains(shot) || shooter != null && (shooter == caster || Cosmos.isAlly(caster, shooter))) {
                continue;
            }
            if (held.add(shot.getId())) {
                shot.setNoGravity(true);
                CosmosFx.stars(level, shot.position(), 4, .1);
            }
            shot.setDeltaMovement(shot.getDeltaMovement().scale(.82));
            shot.hurtMarked = true;
        }
        if (tickCount % 40 == 0) {
            CosmosFx.sound(level, position(), SoundEvents.BEACON_AMBIENT, .7f, .5f);
        }
    }

    /** Gravity comes back at once: the floating are flung to the ground, held shots fall. */
    private void bringDown(ServerLevel level) {
        float r = getRadius();
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(r + 4, r + 8, r + 4))) {
            if (e.hasEffect(ModEffects.WEIGHTLESS)) {
                e.removeEffect(ModEffects.WEIGHTLESS);
            }
            if (floated.contains(e.getId()) && e.isAlive()) {
                slammed.put(e.getId(), e.getY());
                e.setDeltaMovement(0, -2.4, 0);
                e.hurtMarked = true;
                SpellUtil.particles(level, CosmosFx.ICE, e.getX(), e.getY() + e.getBbHeight() * .5, e.getZ(), 10, .3, .5, .3, 0);
            } else if (e.hasEffect(ModEffects.LOW_GRAVITY)) {
                e.removeEffect(ModEffects.LOW_GRAVITY);
                if (!e.onGround()) {
                    e.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 40, 0, false, false, true));
                }
            }
        }
        for (Projectile shot : level.getEntitiesOfClass(Projectile.class, getBoundingBox().inflate(r + 4, r + 8, r + 4))) {
            if (held.contains(shot.getId())) {
                shot.setNoGravity(false);
                shot.setDeltaMovement(0, -1, 0);
                shot.hurtMarked = true;
            }
        }
        CosmosFx.sound(level, position(), SoundEvents.BEACON_DEACTIVATE, 1.5f, .6f);
        CosmosFx.sound(level, position(), SoundEvents.WARDEN_SONIC_BOOM, .8f, 1.6f);
        SpellUtil.cameraShake(level, position(), 8, r + 6);
    }

    /** The slammed hit the ground. */
    private void land(ServerLevel level, @Nullable LivingEntity caster, AbstractSpell spell) {
        var it = slammed.entrySet().iterator();
        while (it.hasNext()) {
            var entry = it.next();
            Entity entity = level.getEntity(entry.getKey());
            if (!(entity instanceof LivingEntity e) || !e.isAlive()) {
                it.remove();
                continue;
            }
            e.resetFallDistance();
            if (!e.onGround() && !e.isInWater() && tickCount - getSlamStart() < SLAM_TICKS) {
                if (e.getDeltaMovement().y > -1.5) {
                    e.setDeltaMovement(e.getDeltaMovement().x * .5, -2.4, e.getDeltaMovement().z * .5);
                    e.hurtMarked = true;
                }
                continue;
            }
            it.remove();
            double drop = Math.max(0, entry.getValue() - e.getY());
            float hit = damage * (1 + (float) Math.min(drop, 14) * .2f);
            DamageSources.applyDamage(e, hit, SpellUtil.damageSource(spell, this, caster));
            e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 2));
            SpellUtil.particles(level, new BlastwaveParticleOptions(.62f, .78f, 1f, 2.5f + (float) Math.min(drop, 10) * .2f), e.getX(), e.getY() + .1, e.getZ(), 1, 0, 0, 0, 0);
            SpellUtil.particles(level, new BlockParticleOption(ParticleTypes.BLOCK, e.getBlockStateOn()), e.getX(), e.getY() + .1, e.getZ(), 30, .6, .1, .6, .2);
            SpellUtil.particles(level, ParticleTypes.CLOUD, e.getX(), e.getY() + .1, e.getZ(), 8, .5, .05, .5, .05);
            CosmosFx.sound(level, e.position(), SoundEvents.GENERIC_EXPLODE.value(), .6f, 1.3f);
            CosmosFx.sound(level, e.position(), SoundEvents.ANVIL_LAND, .7f, .6f);
        }
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 128 * 128;
    }
}
