package com.waylays.arcanes.entity.water;

import com.waylays.arcanes.entity.SpellEffectEntity;
import com.waylays.arcanes.registry.ModEntities;
import com.waylays.arcanes.spells.SpellUtil;
import com.waylays.arcanes.spells.water.WaterFx;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.damage.DamageSources;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import net.minecraft.core.particles.ParticleTypes;
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
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

/**
 * Geyser: the ground where the caster points bubbles and steams, then a column of water bursts out of it: what stood
 * there is hurled high; for a moment the column keeps spouting, throwing up whatever comes into it; then it falls back
 * in a great splash. Both sides draw it from its age.
 */
public class Geyser extends SpellEffectEntity {
    public static final int WARN = 14;
    public static final int RISE = 4;
    public static final int SPOUT = 36;
    public static final int FALL = 10;
    public static final float RADIUS = 1.8f;
    public static final float HEIGHT = 9;

    private static final EntityDataAccessor<Float> DATA_HEIGHT = SynchedEntityData.defineId(Geyser.class, EntityDataSerializers.FLOAT);

    private final Int2IntOpenHashMap lastHit = new Int2IntOpenHashMap();
    @Nullable
    private String spellId;

    public Geyser(EntityType<? extends Geyser> entityType, Level level) {
        super(entityType, level);
    }

    public static Geyser burst(Level level, LivingEntity caster, AbstractSpell spell, Vec3 at, float height, float damage) {
        Geyser geyser = new Geyser(ModEntities.GEYSER.get(), level);
        geyser.setOwner(caster);
        geyser.spellId = spell.getSpellId();
        geyser.damage = damage;
        geyser.moveTo(at.x, at.y, at.z, level.random.nextFloat() * 360, 0);
        geyser.entityData.set(DATA_HEIGHT, height);
        level.addFreshEntity(geyser);
        WaterFx.sound(level, at, SoundEvents.BUBBLE_COLUMN_UPWARDS_AMBIENT, 1.5f, .6f);
        return geyser;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_HEIGHT, HEIGHT);
    }

    public float getHeight() {
        return this.entityData.get(DATA_HEIGHT);
    }

    /** How high the column stands (0 to 1). */
    public float column(float age) {
        if (age < WARN) {
            return 0;
        }
        float up = Mth.clamp((age - WARN) / RISE, 0, 1);
        float down = Mth.clamp((age - WARN - RISE - SPOUT) / FALL, 0, 1);
        return (1 - (1 - up) * (1 - up)) * (1 - down * down);
    }

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel level)) {
            return;
        }
        if (tickCount > WARN + RISE + SPOUT + FALL) {
            discard();
            return;
        }
        if (tickCount < WARN) {
            if (tickCount % 2 == 0) {
                WaterFx.bubbles(level, position().add(0, .1, 0), 6, RADIUS * .6);
                SpellUtil.particles(level, ParticleTypes.CLOUD, getX(), getY() + .1, getZ(), 2, RADIUS * .5, 0, RADIUS * .5, .02);
            }
            return;
        }
        AbstractSpell spell = spellId == null ? null : SpellRegistry.getSpell(spellId);
        LivingEntity caster = getLivingOwner();
        if (tickCount == WARN) {
            WaterFx.crash(level, position(), 2);
            WaterFx.sound(level, position(), SoundEvents.GENERIC_EXPLODE.value(), .6f, 1.4f);
            WaterFx.splash(level, position().add(0, 1, 0), 40, 1);
            SpellUtil.cameraShake(level, position(), 6, 10);
        }
        if (tickCount == WARN + RISE + SPOUT) {
            SplashCrown.at(level, position(), 2.2f);
            WaterFx.crash(level, position(), 1.6f);
        }
        if (tickCount > WARN + RISE + SPOUT) {
            return;
        }
        if (tickCount % 3 == 0) {
            WaterFx.spray(level, position().add(0, getHeight() * column(tickCount), 0), 6, 1.2);
        }
        if (tickCount % 15 == 0) {
            WaterFx.sound(level, position(), SoundEvents.BUBBLE_COLUMN_WHIRLPOOL_AMBIENT, 1.2f, .7f);
        }
        float h = getHeight() * column(tickCount);
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(position(), position()).inflate(RADIUS + .5, h + 1, RADIUS + .5))) {
            if (!e.isAlive() || e == caster || SpellUtil.isAlly(caster, e) || Math.hypot(e.getX() - getX(), e.getZ() - getZ()) > RADIUS + e.getBbWidth() * .5
                    || e.getY() > getY() + h || e.getY() + e.getBbHeight() < getY() - .5) {
                continue;
            }
            double resist = 1 - Mth.clamp(e.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE), 0, 1);
            boolean burst = tickCount <= WARN + 1;
            e.setDeltaMovement(e.getDeltaMovement().multiply(.6, 0, .6).add(0, (burst ? 1.35 : .5) * Math.max(.4, resist), 0));
            e.hurtMarked = true;
            e.fallDistance = 0;
            WaterFx.soak(e, 60);
            if (spell != null && (burst || tickCount - lastHit.getOrDefault(e.getId(), -100) >= 10)) {
                lastHit.put(e.getId(), tickCount);
                e.invulnerableTime = 0;
                DamageSources.applyDamage(e, burst ? damage : damage * .25f, SpellUtil.damageSource(spell, this, caster));
            }
        }
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 128 * 128;
    }
}
