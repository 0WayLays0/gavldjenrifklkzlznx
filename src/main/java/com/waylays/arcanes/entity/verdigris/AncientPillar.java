package com.waylays.arcanes.entity.verdigris;

import com.waylays.arcanes.entity.SpellEffectEntity;
import com.waylays.arcanes.entity.glyph.GlyphArc;
import com.waylays.arcanes.registry.ModEntities;
import com.waylays.arcanes.spells.SpellUtil;
import com.waylays.arcanes.spells.glyph.GlyphElement;
import com.waylays.arcanes.spells.verdigris.VerdigrisFx;
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
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.Comparator;

/**
 * An ancient column of verdigrised copper, overgrown with moss and hung with vines, bursting out of the ground.
 * A Pillar bursts under an enemy and throws it high; a Column (of the Colonnade) bursts beside the caster and throws
 * enemies away from them; a Rod is a tall column crowned with a lightning rod that draws lightning down on the enemies
 * around it. Then it crumbles back into the ground.
 */
public class AncientPillar extends SpellEffectEntity {
    public enum Kind { PILLAR, COLUMN, ROD }

    public static final int SINK_TICKS = 12;
    /** How often the rod strikes, and how far it reaches. */
    public static final int ROD_EVERY = 30;
    public static final float ROD_RANGE = 8;

    private static final EntityDataAccessor<Integer> DATA_KIND = SynchedEntityData.defineId(AncientPillar.class, EntityDataSerializers.INT);
    /** Game ticks when it bursts out and when it starts to crumble. */
    private static final EntityDataAccessor<Integer> DATA_START = SynchedEntityData.defineId(AncientPillar.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_END = SynchedEntityData.defineId(AncientPillar.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DATA_SCALE = SynchedEntityData.defineId(AncientPillar.class, EntityDataSerializers.FLOAT);
    /** Game tick the rod last struck (its bulb flares). */
    private static final EntityDataAccessor<Integer> DATA_STRUCK = SynchedEntityData.defineId(AncientPillar.class, EntityDataSerializers.INT);

    @Nullable
    private String spellId;
    /** For a column: whom it throws enemies away from. */
    private Vec3 center = Vec3.ZERO;
    private boolean erupted;

    public AncientPillar(EntityType<? extends AncientPillar> entityType, Level level) {
        super(entityType, level);
    }

    public static AncientPillar raise(Level level, LivingEntity caster, AbstractSpell spell, Kind kind, Vec3 at, int delay, int life, float scale, float damage) {
        AncientPillar pillar = new AncientPillar(ModEntities.ANCIENT_PILLAR.get(), level);
        pillar.setOwner(caster);
        pillar.spellId = spell.getSpellId();
        pillar.damage = damage;
        pillar.center = caster.position();
        pillar.moveTo(at.x, at.y, at.z, level.random.nextFloat() * 360, 0);
        int now = (int) level.getGameTime();
        pillar.entityData.set(DATA_KIND, kind.ordinal());
        pillar.entityData.set(DATA_START, now + delay);
        pillar.entityData.set(DATA_END, now + delay + life);
        pillar.entityData.set(DATA_SCALE, scale);
        level.addFreshEntity(pillar);
        return pillar;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_KIND, 0);
        builder.define(DATA_START, 0);
        builder.define(DATA_END, 100);
        builder.define(DATA_SCALE, 1f);
        builder.define(DATA_STRUCK, -1000);
    }

    public Kind getKind() {
        Kind[] all = Kind.values();
        return all[Mth.clamp(this.entityData.get(DATA_KIND), 0, all.length - 1)];
    }

    public float getScale() {
        return this.entityData.get(DATA_SCALE);
    }

    /** Its full height. */
    public float height() {
        float base = switch (getKind()) {
            case PILLAR -> 3.6f;
            case COLUMN -> 2.8f;
            case ROD -> 4.6f;
        };
        return base * getScale();
    }

    public int riseTicks() {
        return getKind() == Kind.ROD ? 10 : 4;
    }

    private float now(float partialTicks) {
        return (int) level().getGameTime() + partialTicks;
    }

    /** How far out of the ground: 0 buried, 1 fully risen (it overshoots a little as it bursts out). */
    public float risen(float partialTicks) {
        float t = now(partialTicks);
        float up = Mth.clamp((t - this.entityData.get(DATA_START)) / riseTicks(), 0, 1);
        float rise = getKind() == Kind.ROD ? 1 - (1 - up) * (1 - up) : up * (1 + .2f * Mth.sin(up * Mth.PI));
        float down = Mth.clamp((t - this.entityData.get(DATA_END)) / SINK_TICKS, 0, 1);
        return rise * (1 - down * down);
    }

    /** Ticks since it started to crumble (negative before). */
    public float sinking(float partialTicks) {
        return now(partialTicks) - this.entityData.get(DATA_END);
    }

    /** Ticks since the rod last struck. */
    public float sinceStruck(float partialTicks) {
        return now(partialTicks) - this.entityData.get(DATA_STRUCK);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            return;
        }
        int now = (int) level().getGameTime();
        int start = this.entityData.get(DATA_START);
        int end = this.entityData.get(DATA_END);
        if (now >= end + SINK_TICKS || tickCount > 20 * 60) {
            discard();
            return;
        }
        if (now == end) {
            VerdigrisFx.debris(level(), position().add(0, .5, 0), 16, .6);
            VerdigrisFx.sound(level(), position(), SoundEvents.COPPER_BREAK, 1f, .5f);
        }
        if (!erupted && now >= start) {
            erupted = true;
            erupt((ServerLevel) level());
        }
        if (getKind() == Kind.ROD && now > start + riseTicks() && now < end && (now - start - riseTicks()) % ROD_EVERY == 0) {
            strike((ServerLevel) level(), now);
        }
    }

    @Nullable
    private AbstractSpell spell() {
        return spellId == null ? null : SpellRegistry.getSpell(spellId);
    }

    private void hurt(LivingEntity e, float amount) {
        AbstractSpell spell = spell();
        if (spell != null) {
            e.invulnerableTime = 0;
            DamageSources.applyDamage(e, amount, SpellUtil.damageSource(spell, this, getOwner()));
        }
    }

    /** It bursts out of the ground: a pillar throws up what stands on it, a column throws enemies away from the caster. */
    private void erupt(ServerLevel level) {
        Vec3 c = position();
        Entity owner = getOwner();
        VerdigrisFx.debris(level, c.add(0, .4, 0), 24, .7);
        VerdigrisFx.rise(level, c, 1.4f);
        if (getKind() == Kind.ROD) {
            return;
        }
        boolean pillar = getKind() == Kind.PILLAR;
        float r = pillar ? .9f * getScale() + .5f : 1.8f;
        for (LivingEntity e : SpellUtil.enemiesAround(level, c.add(0, 1, 0), r + 1.5, owner,
                x -> x.distanceToSqr(c.x, x.getY(), c.z) <= r * r && x.getY() > c.y - 1.5 && x.getY() < c.y + 2.5)) {
            hurt(e, damage);
            double resist = 1 - Mth.clamp(e.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE), 0, 1);
            if (pillar) {
                e.setDeltaMovement(e.getDeltaMovement().multiply(.3, 0, .3).add(0, 1.25 * resist, 0));
            } else {
                Vec3 away = e.position().subtract(center).multiply(1, 0, 1);
                away = away.lengthSqr() < 1.0E-4 ? Vec3.ZERO : away.normalize();
                e.setDeltaMovement(e.getDeltaMovement().add(away.scale(1.2 * resist)).add(0, .45 * resist, 0));
            }
            e.hurtMarked = true;
            e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 1));
        }
        SpellUtil.cameraShake(level, c, 6, 10);
    }

    /** Lightning comes down on the rod and leaps from its tip to the nearest enemy around. */
    private void strike(ServerLevel level, int now) {
        Vec3 tip = position().add(0, height(), 0);
        LivingEntity target = SpellUtil.enemiesAround(level, position().add(0, 1, 0), ROD_RANGE, getOwner(), e -> true).stream()
                .min(Comparator.comparingDouble(e -> e.distanceToSqr(this))).orElse(null);
        if (target == null) {
            return;
        }
        this.entityData.set(DATA_STRUCK, now);
        LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
        if (bolt != null) {
            bolt.moveTo(tip.x, tip.y, tip.z);
            bolt.setVisualOnly(true);
            level.addFreshEntity(bolt);
        }
        Vec3 to = target.position().add(0, target.getBbHeight() * .5, 0);
        GlyphArc.zap(level, GlyphElement.STORM, tip, tip.lerp(to, .5).add(0, .6, 0), to);
        hurt(target, damage);
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20, 2));
        SpellUtil.particles(level, ParticleTypes.ELECTRIC_SPARK, to.x, to.y, to.z, 16, .3, .4, .3, .3);
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 128 * 128;
    }
}
