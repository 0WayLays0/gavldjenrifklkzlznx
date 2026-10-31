package com.waylays.arcanes.entity.fire;

import com.waylays.arcanes.entity.SpellEffectEntity;
import com.waylays.arcanes.registry.ModEntities;
import com.waylays.arcanes.spells.SpellUtil;
import com.waylays.arcanes.spells.fire.FireFx;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

/**
 * A pillar of Stellar Fire: after {@code delay} ticks the ground kindles and glows ({@link #WARN} ticks), then a column
 * of fire bursts up out of it: what stands there is thrown up and burnt, and goes on burning while the column stands
 * ({@link #BURN} ticks); then it sinks back.
 */
public class FirePillar extends SpellEffectEntity {
    public static final int WARN = 12;
    public static final int RISE = 5;
    public static final int BURN = 30;
    public static final int SINK = 10;
    public static final float RADIUS = 1.1f;
    public static final float HEIGHT = 6.5f;

    private static final EntityDataAccessor<Integer> DATA_DELAY = SynchedEntityData.defineId(FirePillar.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_HEAT = SynchedEntityData.defineId(FirePillar.class, EntityDataSerializers.INT);

    @Nullable
    private String spellId;
    private boolean erupted;
    private int eruptedAt;

    public FirePillar(EntityType<? extends FirePillar> entityType, Level level) {
        super(entityType, level);
    }

    /** {@code warned} false: it bursts out at once (no kindling first). */
    public static FirePillar raise(Level level, @Nullable LivingEntity caster, AbstractSpell spell, Vec3 at, int delay, boolean warned, float damage, int tier) {
        FirePillar pillar = new FirePillar(ModEntities.FIRE_PILLAR.get(), level);
        pillar.setOwner(caster);
        pillar.spellId = spell.getSpellId();
        pillar.damage = damage;
        pillar.moveTo(at.x, at.y, at.z, 0, 0);
        pillar.entityData.set(DATA_DELAY, delay - (warned ? 0 : WARN));
        pillar.entityData.set(DATA_HEAT, tier);
        level.addFreshEntity(pillar);
        return pillar;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_DELAY, 0);
        builder.define(DATA_HEAT, 0);
    }

    public int getHeat() {
        return this.entityData.get(DATA_HEAT);
    }

    /** Its own clock: ticks since it began to kindle (negative before). */
    public float since(float age) {
        return age - this.entityData.get(DATA_DELAY);
    }

    public float warn(float age) {
        return Mth.clamp(since(age) / WARN, 0, 1);
    }

    public float rise(float age) {
        return Mth.clamp((since(age) - WARN) / RISE, 0, 1);
    }

    public float sink(float age) {
        return Mth.clamp((since(age) - WARN - RISE - BURN) / SINK, 0, 1);
    }

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel level)) {
            return;
        }
        float t = since(tickCount);
        if (t > WARN + RISE + BURN + SINK) {
            discard();
            return;
        }
        if (t < 0) {
            return;
        }
        if (t < WARN && tickCount % 3 == 0) {
            FireFx.embers(level, position().add(0, .2, 0), 3, RADIUS * .6, getHeat());
            FireFx.smoke(level, position().add(0, .2, 0), 1, RADIUS * .4);
        }
        if (t < WARN || t > WARN + RISE + BURN) {
            return;
        }
        boolean burst = !erupted;
        if (burst) {
            erupted = true;
            eruptedAt = tickCount;
            FireFx.burst(level, position(), 1.6f, getHeat());
            FireFx.sound(level, position(), SoundEvents.GENERIC_EXPLODE.value(), .6f, 1.4f);
        } else if ((tickCount - eruptedAt) % 10 != 0) {
            return;
        }
        AbstractSpell spell = spellId == null ? null : SpellRegistry.getSpell(spellId);
        LivingEntity caster = getLivingOwner();
        for (LivingEntity e : SpellUtil.enemiesAround(level, position().add(0, HEIGHT * .5, 0), HEIGHT * .5 + RADIUS, caster,
                x -> Math.hypot(x.getX() - getX(), x.getZ() - getZ()) < RADIUS + x.getBbWidth() * .5 && x.getY() < getY() + HEIGHT && x.getY() + x.getBbHeight() > getY() - .5)) {
            FireFx.burn(e, burst ? damage : damage * .3f, spell, this, caster, getHeat());
            if (burst) {
                double resist = 1 - Mth.clamp(e.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE), 0, 1);
                e.setDeltaMovement(e.getDeltaMovement().multiply(.5, 0, .5).add(0, .9 * Math.max(.4, resist), 0));
                e.hurtMarked = true;
            }
        }
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 128 * 128;
    }
}
