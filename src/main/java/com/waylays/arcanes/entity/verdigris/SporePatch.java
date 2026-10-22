package com.waylays.arcanes.entity.verdigris;

import com.waylays.arcanes.entity.SpellEffectEntity;
import com.waylays.arcanes.registry.ModEntities;
import com.waylays.arcanes.spells.SpellUtil;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.damage.DamageSources;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

/**
 * Moss Spores: a carpet of moss spreads over the ground, tufts of flowering moss rising out of it, and breathes out
 * spores; every half second the enemies on it choke on them (hurt, poisoned, slowed).
 */
public class SporePatch extends SpellEffectEntity {
    public static final float RADIUS = 3.5f;
    public static final int SPREAD = 12;
    public static final int FADE = 14;
    public static final int PULSE_TICKS = 10;

    private static final EntityDataAccessor<Integer> DATA_LIFE = SynchedEntityData.defineId(SporePatch.class, EntityDataSerializers.INT);

    @Nullable
    private String spellId;

    public SporePatch(EntityType<? extends SporePatch> entityType, Level level) {
        super(entityType, level);
    }

    public static SporePatch spread(Level level, LivingEntity caster, AbstractSpell spell, Vec3 at, int life, float damage) {
        SporePatch patch = new SporePatch(ModEntities.SPORE_PATCH.get(), level);
        patch.setOwner(caster);
        patch.spellId = spell.getSpellId();
        patch.damage = damage;
        patch.moveTo(at.x, at.y, at.z, level.random.nextFloat() * 360, 0);
        patch.entityData.set(DATA_LIFE, life);
        level.addFreshEntity(patch);
        return patch;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_LIFE, 120);
    }

    public int getLife() {
        return this.entityData.get(DATA_LIFE);
    }

    /** How far the moss has spread (0 to 1 of the radius), shrinking away at the end. */
    public float spread(float partialTicks) {
        float age = tickCount + partialTicks;
        float grow = Mth.clamp(age / SPREAD, 0, 1);
        return (1 - (1 - grow) * (1 - grow)) * Mth.clamp(1 - (age - getLife()) / FADE, 0, 1);
    }

    private boolean on(Entity e) {
        return e.distanceToSqr(getX(), e.getY(), getZ()) <= RADIUS * RADIUS && e.getY() > getY() - 1 && e.getY() < getY() + 2.5;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            if (tickCount < getLife()) {
                for (int i = 0; i < 3; i++) {
                    double a = random.nextDouble() * Mth.TWO_PI;
                    double r = Math.sqrt(random.nextDouble()) * RADIUS * spread(0);
                    level().addParticle(i == 0 ? ParticleTypes.SPORE_BLOSSOM_AIR : ParticleTypes.FALLING_SPORE_BLOSSOM,
                            getX() + Math.cos(a) * r, getY() + .2 + random.nextDouble() * 1.5, getZ() + Math.sin(a) * r, 0, .02, 0);
                }
            }
            return;
        }
        if (tickCount > getLife() + FADE) {
            discard();
            return;
        }
        if (tickCount < SPREAD || tickCount >= getLife() || tickCount % PULSE_TICKS != 0) {
            return;
        }
        AbstractSpell spell = spellId == null ? null : SpellRegistry.getSpell(spellId);
        Entity owner = getOwner();
        for (LivingEntity e : SpellUtil.enemiesAround(level(), position().add(0, 1, 0), RADIUS + 2, owner, this::on)) {
            if (spell != null) {
                e.invulnerableTime = 0;
                DamageSources.applyDamage(e, damage, SpellUtil.damageSource(spell, this, owner));
            }
            e.addEffect(new MobEffectInstance(MobEffects.POISON, 40, 0));
            e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20, 0));
        }
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 96 * 96;
    }
}
