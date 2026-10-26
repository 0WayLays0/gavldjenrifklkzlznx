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
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * A black hole hanging in the air: a sphere of nothing ringed with light, its accretion disk whirling white-hot round
 * it and bent over and under it by its pull. Everything that is not the caster's drifts in toward it, faster and
 * faster as it nears; what comes close to the horizon is torn at, and shots are swallowed. When the
 * caster casts again, or its time runs out, it collapses: it shrinks to a point and bursts, throwing everything back
 * out with two jets of light shooting up and down from it.
 */
public class BlackHole extends SpellEffectEntity {
    public static final int GROW_TICKS = 30;
    public static final int COLLAPSE_TICKS = 8;
    public static final int JET_TICKS = 34;
    public static final float BURST_RADIUS = 9;

    private static final EntityDataAccessor<Float> DATA_RANGE = SynchedEntityData.defineId(BlackHole.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> DATA_LIFE = SynchedEntityData.defineId(BlackHole.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_COLLAPSE = SynchedEntityData.defineId(BlackHole.class, EntityDataSerializers.INT);

    @Nullable
    private String spellId;
    private final Map<Integer, Integer> torn = new HashMap<>();

    public BlackHole(EntityType<? extends BlackHole> entityType, Level level) {
        super(entityType, level);
    }

    public static BlackHole open(Level level, LivingEntity caster, AbstractSpell spell, Vec3 at, float range, int life, float damage) {
        BlackHole old = of(level, caster);
        if (old != null) {
            old.collapse();
        }
        BlackHole hole = new BlackHole(ModEntities.BLACK_HOLE.get(), level);
        hole.setOwner(caster);
        hole.spellId = spell.getSpellId();
        hole.damage = damage;
        hole.moveTo(at.x, at.y, at.z, 0, 0);
        hole.entityData.set(DATA_RANGE, range);
        hole.entityData.set(DATA_LIFE, life);
        level.addFreshEntity(hole);
        CosmosFx.sound(level, at, SoundEvents.END_PORTAL_SPAWN, 1.5f, .5f);
        CosmosFx.sound(level, at, SoundEvents.WARDEN_SONIC_BOOM, .8f, .4f);
        SpellUtil.cameraShake(level, at, 15, range + 8);
        return hole;
    }

    /** The caster's black hole that has not collapsed yet. */
    @Nullable
    public static BlackHole of(Level level, LivingEntity caster) {
        for (BlackHole hole : level.getEntitiesOfClass(BlackHole.class, caster.getBoundingBox().inflate(80), h -> caster.getUUID().equals(h.getOwnerUUID()))) {
            if (!hole.isCollapsing()) {
                return hole;
            }
        }
        return null;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_RANGE, 14f);
        builder.define(DATA_LIFE, 160);
        builder.define(DATA_COLLAPSE, -1);
    }

    public float getRange() {
        return this.entityData.get(DATA_RANGE);
    }

    public int getLife() {
        return this.entityData.get(DATA_LIFE);
    }

    /** The tick it began to collapse, or -1. */
    public int getCollapseStart() {
        return this.entityData.get(DATA_COLLAPSE);
    }

    public boolean isCollapsing() {
        return getCollapseStart() >= 0;
    }

    /** The radius of its horizon at {@code age}: it swells as it opens and shrinks to nothing as it collapses. */
    public float horizon(float age) {
        float grow = Mth.clamp(age / GROW_TICKS, 0, 1);
        float r = .3f + 1.3f * (1 - (1 - grow) * (1 - grow));
        int start = getCollapseStart();
        if (start >= 0) {
            r *= 1 - Mth.clamp((age - start) / COLLAPSE_TICKS, 0, 1);
        }
        return r;
    }

    public void collapse() {
        if (!isCollapsing()) {
            this.entityData.set(DATA_COLLAPSE, tickCount);
            CosmosFx.sound(level(), position(), SoundEvents.BEACON_DEACTIVATE, 2, .4f);
            CosmosFx.sound(level(), position(), SoundEvents.PORTAL_TRIGGER, 1, 1.8f);
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            if (!isCollapsing()) {
                // Light and dust falling in
                float range = getRange();
                for (int k = 0; k < 3; k++) {
                    Vec3 dir = new Vec3(random.nextGaussian(), random.nextGaussian() * .5, random.nextGaussian()).normalize();
                    Vec3 from = position().add(dir.scale(range * (.4 + random.nextDouble() * .5)));
                    Vec3 v = position().subtract(from).scale(.05).add(dir.cross(new Vec3(0, 1, 0)).scale(.15));
                    level().addParticle(k == 0 ? ParticleTypes.END_ROD : ParticleTypes.SMOKE, from.x, from.y, from.z, v.x, v.y, v.z);
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
        if (!isCollapsing() && (tickCount >= getLife() || caster == null || !caster.isAlive())) {
            collapse();
        }
        int start = getCollapseStart();
        if (start < 0) {
            pull(level, caster, spell);
            if (tickCount % 25 == 0) {
                CosmosFx.sound(level, position(), SoundEvents.BEACON_AMBIENT, 1.5f, .35f);
                CosmosFx.sound(level, position(), SoundEvents.PORTAL_AMBIENT, .8f, .5f);
            }
            return;
        }
        int t = tickCount - start;
        if (t == COLLAPSE_TICKS) {
            burst(level, caster, spell);
        } else if (t > COLLAPSE_TICKS + JET_TICKS) {
            discard();
        }
    }

    /** Everything not the caster's is drawn in (loose things too); the horizon tears at what comes close and swallows shots. */
    private void pull(ServerLevel level, @Nullable LivingEntity caster, AbstractSpell spell) {
        float range = getRange();
        float r = horizon(tickCount);
        float grow = Mth.clamp(tickCount / (float) GROW_TICKS, 0, 1);
        for (Entity e : level.getEntities(this, getBoundingBox().inflate(range))) {
            if (e instanceof SpellEffectEntity || e instanceof ArmorStand || e.isSpectator() || Cosmos.isAlly(caster, e) || e == caster) {
                continue;
            }
            if (e instanceof Projectile shot && shot.getOwner() != null && (shot.getOwner() == caster || Cosmos.isAlly(caster, shot.getOwner()))) {
                continue;
            }
            Vec3 centre = e.getBoundingBox().getCenter();
            Vec3 to = position().subtract(centre);
            double d = to.length();
            if (d > range || d < 1.0E-3) {
                continue;
            }
            if (e instanceof Projectile && d < r + .8) {
                SpellUtil.particles(level, ParticleTypes.SMOKE, centre.x, centre.y, centre.z, 6, .1, .1, .1, .02);
                e.discard();
                continue;
            }
            if (!(e instanceof LivingEntity || e instanceof Projectile || e instanceof ItemEntity)) {
                continue;
            }
            double near = 1 - d / range;
            double strength = (.03 + .3 * near * near) * grow;
            if (e instanceof LivingEntity living && Cosmos.isBoss(living)) {
                strength *= .35;
            }
            // Drawn in, and a little round, as things circle a drain
            Vec3 swirl = to.cross(new Vec3(0, 1, 0)).normalize().scale(strength * .35);
            Vec3 v = e.getDeltaMovement().add(to.scale(strength / d)).add(swirl);
            double max = .9 + near;
            if (v.lengthSqr() > max * max) {
                v = v.normalize().scale(max);
            }
            e.setDeltaMovement(v);
            e.hurtMarked = true;
            e.resetFallDistance();
            if (e instanceof LivingEntity living && d < r + 1.6 && living.isAlive()) {
                Integer last = torn.get(e.getId());
                if (last == null || tickCount - last >= 10) {
                    torn.put(e.getId(), tickCount);
                    DamageSources.applyDamage(living, damage * .6f, SpellUtil.damageSource(spell, this, caster));
                    SpellUtil.particles(level, CosmosFx.NEBULA, centre.x, centre.y, centre.z, 8, .3, .3, .3, .05);
                }
            }
        }
    }

    /** Collapsed to a point, it bursts: everything is thrown back out and two jets of light shoot up and down. */
    private void burst(ServerLevel level, @Nullable LivingEntity caster, AbstractSpell spell) {
        for (LivingEntity e : SpellUtil.enemiesAround(level, position(), BURST_RADIUS, caster, e -> true)) {
            Vec3 out = e.getBoundingBox().getCenter().subtract(position());
            double d = Math.max(.5, out.length());
            DamageSources.applyDamage(e, damage * 3 * (float) Math.max(.4, 1 - d / BURST_RADIUS * .6), SpellUtil.damageSource(spell, this, caster));
            Vec3 push = out.normalize().scale(1.4 + 1.2 * (1 - d / BURST_RADIUS));
            e.setDeltaMovement(push.x, Math.max(.4, push.y + .4), push.z);
            e.hurtMarked = true;
        }
        SpellUtil.particles(level, new BlastwaveParticleOptions(1f, .6f, .2f, BURST_RADIUS * 2), getX(), getY(), getZ(), 1, 0, 0, 0, 0);
        SpellUtil.particles(level, new BlastwaveParticleOptions(.7f, .8f, 1f, BURST_RADIUS * 1.3f), getX(), getY(), getZ(), 1, 0, 0, 0, 0);
        SpellUtil.particles(level, ParticleTypes.EXPLOSION_EMITTER, getX(), getY(), getZ(), 1, 0, 0, 0, 0);
        SpellUtil.particles(level, ParticleTypes.END_ROD, getX(), getY(), getZ(), 80, .4, 3, .4, .5);
        CosmosFx.stars(level, position(), 60, 2.5);
        CosmosFx.sound(level, position(), SoundEvents.GENERIC_EXPLODE.value(), 2.5f, .5f);
        CosmosFx.sound(level, position(), SoundEvents.WARDEN_SONIC_BOOM, 1.5f, .7f);
        CosmosFx.sound(level, position(), SoundEvents.BEACON_ACTIVATE, 2, 2);
        SpellUtil.cameraShake(level, position(), 25, BURST_RADIUS * 3);
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 192 * 192;
    }
}
