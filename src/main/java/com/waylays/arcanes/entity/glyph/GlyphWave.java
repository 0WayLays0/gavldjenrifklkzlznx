package com.waylays.arcanes.entity.glyph;

import com.waylays.arcanes.entity.SpellEffectEntity;
import com.waylays.arcanes.registry.ModEntities;
import com.waylays.arcanes.spells.SpellUtil;
import com.waylays.arcanes.spells.glyph.GlyphElement;
import com.waylays.arcanes.spells.glyph.GlyphReactions;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

/**
 * A wave rolling out from a glyph in an arc: a wall of water (Tide) that throws enemies back, or a gust (Gale) that
 * lifts them off their feet. Each enemy it rolls over is struck once.
 */
public class GlyphWave extends SpellEffectEntity {
    private static final EntityDataAccessor<Integer> DATA_ELEMENT = SynchedEntityData.defineId(GlyphWave.class, EntityDataSerializers.INT);
    /** Half the arc's width in degrees, how far it rolls and how long it takes. */
    private static final EntityDataAccessor<Float> DATA_HALF_ANGLE = SynchedEntityData.defineId(GlyphWave.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_RANGE = SynchedEntityData.defineId(GlyphWave.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> DATA_DURATION = SynchedEntityData.defineId(GlyphWave.class, EntityDataSerializers.INT);
    public static final int FADE_TICKS = 6;

    private final IntOpenHashSet hit = new IntOpenHashSet();
    @Nullable
    private String spellId;

    public GlyphWave(EntityType<? extends GlyphWave> entityType, Level level) {
        super(entityType, level);
    }

    public static GlyphWave roll(Level level, LivingEntity caster, AbstractSpell spell, GlyphElement element, Vec3 from, float yaw,
                                 float halfAngle, float range, int duration, float damage) {
        GlyphWave wave = new GlyphWave(ModEntities.GLYPH_WAVE.get(), level);
        wave.setOwner(caster);
        wave.spellId = spell.getSpellId();
        wave.moveTo(from.x, from.y, from.z, yaw, 0);
        wave.entityData.set(DATA_ELEMENT, element.ordinal());
        wave.entityData.set(DATA_HALF_ANGLE, halfAngle);
        wave.entityData.set(DATA_RANGE, range);
        wave.entityData.set(DATA_DURATION, duration);
        wave.damage = damage;
        level.addFreshEntity(wave);
        return wave;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_ELEMENT, GlyphElement.TIDE.ordinal());
        builder.define(DATA_HALF_ANGLE, 45f);
        builder.define(DATA_RANGE, 8f);
        builder.define(DATA_DURATION, 12);
    }

    public GlyphElement getElement() {
        return GlyphElement.byId(this.entityData.get(DATA_ELEMENT));
    }

    public float getHalfAngle() {
        return this.entityData.get(DATA_HALF_ANGLE);
    }

    public float getRange() {
        return this.entityData.get(DATA_RANGE);
    }

    public int getDuration() {
        return this.entityData.get(DATA_DURATION);
    }

    /** 0 to 1 as it rolls out (fast at first, slowing down). */
    public float progress(float partialTicks) {
        float t = Mth.clamp((tickCount + partialTicks) / getDuration(), 0, 1);
        return 1 - (1 - t) * (1 - t);
    }

    public float radius(float partialTicks) {
        return .8f + (getRange() - .8f) * progress(partialTicks);
    }

    /** Fades as it breaks at the end. */
    public float strength(float partialTicks) {
        return Mth.clamp(1 - (tickCount + partialTicks - getDuration()) / FADE_TICKS, 0, 1);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            return;
        }
        if (tickCount > getDuration() + FADE_TICKS) {
            discard();
            return;
        }
        if (tickCount > getDuration()) {
            return;
        }
        AbstractSpell spell = spellId == null ? null : SpellRegistry.getSpell(spellId);
        if (spell == null) {
            return;
        }
        float r = radius(0);
        float range = getRange();
        Entity owner = getOwner();
        Vec3 c = position();
        float yaw = getYRot() * Mth.DEG_TO_RAD;
        Vec3 f = new Vec3(-Mth.sin(yaw), 0, Mth.cos(yaw));
        double cosLimit = Math.cos(Math.toRadians(getHalfAngle()));
        for (LivingEntity e : level().getEntitiesOfClass(LivingEntity.class, new AABB(c, c).inflate(range + 1, 3, range + 1))) {
            if (hit.contains(e.getId()) || !e.isAlive() || SpellUtil.isAlly(owner, e)) {
                continue;
            }
            Vec3 d = new Vec3(e.getX() - c.x, 0, e.getZ() - c.z);
            double dist = d.length();
            if (dist > r + .6 || dist < r - 1.8 || e.getY() < c.y - 1.5 || e.getY() > c.y + 3) {
                continue;
            }
            if (dist > .3 && d.scale(1 / dist).dot(f) < cosLimit) {
                continue;
            }
            hit.add(e.getId());
            GlyphReactions.strike((ServerLevel) level(), owner, this, e, getElement(), damage, spell);
            Vec3 away = dist > .3 ? d.scale(1 / dist) : f;
            double resist = 1 - Mth.clamp(e.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE), 0, 1);
            boolean gale = getElement() == GlyphElement.GALE;
            e.setDeltaMovement(e.getDeltaMovement().add(away.scale((gale ? .8 : 1.15) * resist)).add(0, (gale ? .75 : .25) * resist, 0));
            e.hurtMarked = true;
        }
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 96 * 96;
    }
}
