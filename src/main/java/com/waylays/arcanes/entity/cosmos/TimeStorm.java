package com.waylays.arcanes.entity.cosmos;

import com.waylays.arcanes.entity.SpellEffectEntity;
import com.waylays.arcanes.registry.ModEntities;
import com.waylays.arcanes.spells.SpellUtil;
import com.waylays.arcanes.spells.cosmos.Cosmos;
import com.waylays.arcanes.spells.cosmos.CosmosFx;
import com.waylays.arcanes.spells.cosmos.PruningBatonSpell;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.damage.DamageSources;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.List;

/**
 * The End of Time: a living storm of violet cloud lit orange from within, the thing at the end of all timelines.
 * It rolls slowly after the caster's foes, swallowing what it covers in darkness and wearing it away; every so often
 * it lashes out with tendrils of orange lightning at those round it, striking them and dragging them in. What it
 * leaves weak enough is devoured: pruned from time. At its end it draws itself in and is gone.
 */
public class TimeStorm extends SpellEffectEntity {
    public static final float RADIUS = 5.5f;
    public static final float HEIGHT = 6;
    public static final float REACH = 12;
    public static final int GROW_TICKS = 30;
    public static final int FADE_TICKS = 24;
    public static final int LASH_EVERY = 12;
    public static final int TENDRILS = 3;

    private static final EntityDataAccessor<Integer> DATA_LIFE = SynchedEntityData.defineId(TimeStorm.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_LASH = SynchedEntityData.defineId(TimeStorm.class, EntityDataSerializers.INT);
    @SuppressWarnings("unchecked")
    private static final EntityDataAccessor<Integer>[] DATA_TARGETS = new EntityDataAccessor[]{
            SynchedEntityData.defineId(TimeStorm.class, EntityDataSerializers.INT),
            SynchedEntityData.defineId(TimeStorm.class, EntityDataSerializers.INT),
            SynchedEntityData.defineId(TimeStorm.class, EntityDataSerializers.INT)};

    @Nullable
    private String spellId;
    private int spellLevel = 1;

    public TimeStorm(EntityType<? extends TimeStorm> entityType, Level level) {
        super(entityType, level);
    }

    public static TimeStorm unleash(Level level, LivingEntity caster, AbstractSpell spell, int spellLevel, Vec3 at, int life, float damage) {
        TimeStorm storm = new TimeStorm(ModEntities.TIME_STORM.get(), level);
        storm.setOwner(caster);
        storm.spellId = spell.getSpellId();
        storm.spellLevel = spellLevel;
        storm.damage = damage;
        storm.moveTo(at.x, at.y, at.z, 0, 0);
        storm.entityData.set(DATA_LIFE, life);
        level.addFreshEntity(storm);
        CosmosFx.sound(level, at, SoundEvents.WITHER_SPAWN, 1.2f, .5f);
        CosmosFx.sound(level, at, SoundEvents.LIGHTNING_BOLT_THUNDER, 1.5f, .6f);
        SpellUtil.cameraShake(level, at, 20, 24);
        return storm;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_LIFE, 200);
        builder.define(DATA_LASH, -100);
        for (EntityDataAccessor<Integer> target : DATA_TARGETS) {
            builder.define(target, -1);
        }
    }

    public int getLife() {
        return this.entityData.get(DATA_LIFE);
    }

    /** The tick it last lashed out. */
    public int getLashTick() {
        return this.entityData.get(DATA_LASH);
    }

    /** The id of what tendril {@code k} last struck, or -1. */
    public int getTendrilTarget(int k) {
        return this.entityData.get(DATA_TARGETS[k]);
    }

    /** How big it is (0 to 1) at {@code age}: it swells up, and draws itself in at the end. */
    public float size(float age) {
        float grow = Mth.clamp(age / GROW_TICKS, 0, 1);
        float fade = Mth.clamp((getLife() - age) / FADE_TICKS, 0, 1);
        return (1 - (1 - grow) * (1 - grow)) * fade;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            float s = size(tickCount);
            if (s > .2f) {
                for (int k = 0; k < 2; k++) {
                    double a = random.nextDouble() * Mth.TWO_PI, d = random.nextDouble() * RADIUS * s;
                    level().addParticle(k == 0 ? ParticleTypes.REVERSE_PORTAL : ParticleTypes.LARGE_SMOKE, getX() + Math.cos(a) * d, getY() + random.nextDouble() * HEIGHT * s,
                            getZ() + Math.sin(a) * d, 0, .02, 0);
                }
            }
            return;
        }
        if (!(level() instanceof ServerLevel level)) {
            return;
        }
        LivingEntity caster = getLivingOwner();
        AbstractSpell spell = spellId == null ? null : SpellRegistry.getSpell(spellId);
        if (spell == null || tickCount > getLife()) {
            if (spell != null) {
                CosmosFx.sound(level, position(), SoundEvents.BEACON_DEACTIVATE, 1.5f, .5f);
            }
            discard();
            return;
        }
        float s = size(tickCount);
        DamageSource source = SpellUtil.damageSource(spell, this, caster);
        List<LivingEntity> near = SpellUtil.enemiesAround(level, position().add(0, 1, 0), REACH * s + 1, caster, e -> e != caster);
        // It rolls after the nearest foe
        LivingEntity prey = near.stream().filter(e -> caster == null || Cosmos.isFoe(caster, e) || e.getLastHurtByMob() == caster)
                .min(Comparator.comparingDouble(e -> e.distanceToSqr(this))).orElse(null);
        if (prey != null && tickCount > GROW_TICKS / 2) {
            Vec3 to = prey.position().subtract(position()).multiply(1, 0, 1);
            if (to.lengthSqr() > 1) {
                Vec3 step = to.normalize().scale(.13);
                Vec3 next = SpellUtil.groundAt(level, position().add(step), 3, 1);
                setPos(next.x, next.y, next.z);
            }
        }
        // What it covers is swallowed in darkness and worn away
        for (LivingEntity e : near) {
            double dx = e.getX() - getX(), dz = e.getZ() - getZ();
            if (dx * dx + dz * dz > RADIUS * s * RADIUS * s || e.getY() - getY() > HEIGHT * s) {
                continue;
            }
            e.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 40, 0, false, false));
            e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20, 1, false, false));
            if (tickCount % 10 == 0) {
                strike(level, e, damage * .45f, source);
            }
        }
        // Tendrils of lightning lash out
        if (tickCount % LASH_EVERY == 0 && s > .5f) {
            int k = 0;
            for (LivingEntity e : near) {
                if (k >= TENDRILS) {
                    break;
                }
                if (!level.getWorldBorder().isWithinBounds(e.blockPosition())) {
                    continue;
                }
                this.entityData.set(DATA_TARGETS[k++], e.getId());
                strike(level, e, damage * .9f, source);
                Vec3 pull = position().add(0, 1, 0).subtract(e.position());
                pull = pull.lengthSqr() < 1.0E-4 ? Vec3.ZERO : pull.normalize().scale(.6);
                e.push(pull.x, .2, pull.z);
                e.hurtMarked = true;
                Vec3 c = e.getBoundingBox().getCenter();
                SpellUtil.particles(level, ParticleTypes.ELECTRIC_SPARK, c.x, c.y, c.z, 12, .3, .4, .3, .2);
                CosmosFx.time(level, c, 8, .3);
            }
            for (; k < TENDRILS; k++) {
                this.entityData.set(DATA_TARGETS[k], -1);
            }
            this.entityData.set(DATA_LASH, tickCount);
            if (!near.isEmpty()) {
                CosmosFx.sound(level, position(), SoundEvents.LIGHTNING_BOLT_IMPACT, 1.2f, .7f + random.nextFloat() * .3f);
            }
        }
        if (tickCount % 30 == 0) {
            CosmosFx.sound(level, position(), SoundEvents.LIGHTNING_BOLT_THUNDER, .8f, .4f);
            CosmosFx.sound(level, position(), SoundEvents.WITHER_AMBIENT, .5f, .4f);
        }
    }

    /** Strikes {@code e}; what is left weak enough is devoured. */
    private void strike(ServerLevel level, LivingEntity e, float amount, DamageSource source) {
        DamageSources.applyDamage(e, amount, source);
        if (e.isAlive() && !Cosmos.isBoss(e) && e.getHealth() <= PruningBatonSpell.pruneLine(spellLevel + 3, e.getMaxHealth())) {
            CosmosFx.prune(level, e, source);
        }
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 160 * 160;
    }
}
