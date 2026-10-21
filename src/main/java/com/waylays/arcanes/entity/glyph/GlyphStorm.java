package com.waylays.arcanes.entity.glyph;

import com.waylays.arcanes.entity.SpellEffectEntity;
import com.waylays.arcanes.registry.ModEntities;
import com.waylays.arcanes.spells.SpellUtil;
import com.waylays.arcanes.spells.glyph.GlyphElement;
import com.waylays.arcanes.spells.glyph.GlyphFx;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.List;

/**
 * The Primordial Glyph: a great glyph written across the sky over the caster. Its ring is traced first, then the six
 * element glyphs on it one after another, then the star joining them; then, one after another and round the ring,
 * each element hurls its bolt down on the enemies below, so their reactions go off on every side.
 */
public class GlyphStorm extends SpellEffectEntity {
    public static final float HEIGHT = 9;
    /** The ring's radius in the sky, and how far from its centre the enemies it strikes may be. */
    public static final float RING = 4.5f;
    public static final float RADIUS = 14;
    public static final int RING_TICKS = 10;
    public static final int GLYPH_TICKS = 4;
    public static final int STAR_TICKS = 8;
    public static final int WRITE_TICKS = RING_TICKS + 6 * GLYPH_TICKS + STAR_TICKS;
    public static final int STRIKE_TICKS = 4;
    public static final int FADE_TICKS = 20;

    private static final EntityDataAccessor<Integer> DATA_LIFE = SynchedEntityData.defineId(GlyphStorm.class, EntityDataSerializers.INT);
    /** The element that struck last, and when. */
    private static final EntityDataAccessor<Integer> DATA_STRUCK = SynchedEntityData.defineId(GlyphStorm.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_STRIKE_TICK = SynchedEntityData.defineId(GlyphStorm.class, EntityDataSerializers.INT);

    @Nullable
    private String spellId;
    private int strikes;

    public GlyphStorm(EntityType<? extends GlyphStorm> entityType, Level level) {
        super(entityType, level);
    }

    public static GlyphStorm write(Level level, LivingEntity caster, AbstractSpell spell, Vec3 ground, int life, float damage) {
        GlyphStorm storm = new GlyphStorm(ModEntities.GLYPH_STORM.get(), level);
        storm.setOwner(caster);
        storm.spellId = spell.getSpellId();
        storm.damage = damage;
        storm.moveTo(ground.x, ground.y, ground.z, caster.getYRot(), 0);
        storm.entityData.set(DATA_LIFE, life);
        level.addFreshEntity(storm);
        return storm;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_LIFE, 200);
        builder.define(DATA_STRUCK, -1);
        builder.define(DATA_STRIKE_TICK, -1000);
    }

    public int getLife() {
        return this.entityData.get(DATA_LIFE);
    }

    public int getStruck() {
        return this.entityData.get(DATA_STRUCK);
    }

    public float sinceStrike(float partialTicks) {
        return tickCount + partialTicks - this.entityData.get(DATA_STRIKE_TICK);
    }

    /** How far the ring, the glyph of element {@code i} and the star are written (0 to 1). */
    public float ringWritten(float partialTicks) {
        return Mth.clamp((tickCount + partialTicks) / RING_TICKS, 0, 1);
    }

    public float glyphWritten(int i, float partialTicks) {
        return Mth.clamp((tickCount + partialTicks - RING_TICKS - i * GLYPH_TICKS) / GLYPH_TICKS, 0, 1);
    }

    public float starWritten(float partialTicks) {
        return Mth.clamp((tickCount + partialTicks - RING_TICKS - 6 * GLYPH_TICKS) / STAR_TICKS, 0, 1);
    }

    /** 1 while it holds, fading at the end. */
    public float strength(float partialTicks) {
        return Mth.clamp(1 - (tickCount + partialTicks - getLife()) / FADE_TICKS, 0, 1);
    }

    /** Where the glyph of element {@code i} hangs in the sky, relative to the ground under the centre. */
    public Vec3 glyphOffset(int i) {
        float a = getYRot() * Mth.DEG_TO_RAD + i * Mth.TWO_PI / 6;
        return new Vec3(Mth.cos(a) * RING, HEIGHT, Mth.sin(a) * RING);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            return;
        }
        ServerLevel level = (ServerLevel) level();
        if (tickCount > getLife() + FADE_TICKS) {
            discard();
            return;
        }
        if (tickCount == RING_TICKS + 6 * GLYPH_TICKS) {
            GlyphFx.sound(level, position().add(0, HEIGHT, 0), SoundEvents.BEACON_ACTIVATE, 3f, 1.4f);
        } else if (tickCount > RING_TICKS && tickCount < RING_TICKS + 6 * GLYPH_TICKS && (tickCount - RING_TICKS) % GLYPH_TICKS == 0) {
            int i = (tickCount - RING_TICKS) / GLYPH_TICKS;
            GlyphFx.writing(level, GlyphElement.byId(i), position().add(glyphOffset(i)));
        }
        if (tickCount < WRITE_TICKS || tickCount >= getLife() || (tickCount - WRITE_TICKS) % STRIKE_TICKS != 0) {
            return;
        }
        AbstractSpell spell = spellId == null ? null : SpellRegistry.getSpell(spellId);
        if (spell == null) {
            return;
        }
        Entity owner = getOwner();
        GlyphElement element = GlyphElement.byId(strikes % 6);
        Vec3 from = position().add(glyphOffset(element.ordinal()));
        List<LivingEntity> enemies = SpellUtil.enemiesAround(level, position(), RADIUS, owner, e -> e.getY() < getY() + HEIGHT - 1);
        Vec3 to;
        if (!enemies.isEmpty()) {
            LivingEntity target = enemies.get(random.nextInt(enemies.size()));
            to = target.position().add(0, target.getBbHeight() * .4, 0);
        } else {
            // No one below: the bolts rain on the ground around
            double a = random.nextDouble() * Mth.TWO_PI;
            double d = 2 + random.nextDouble() * (RADIUS - 4);
            to = SpellUtil.groundAt(level, position().add(Math.cos(a) * d, 0, Math.sin(a) * d), 6, 1);
        }
        GlyphBolt.shoot(level, owner instanceof LivingEntity living ? living : null, spell, element, from, to.subtract(from), 1.7f, damage)
                .withSplash(1.8f).withSize(1.4f);
        GlyphFx.burst(level, element, from, 8, .4);
        this.entityData.set(DATA_STRUCK, element.ordinal());
        this.entityData.set(DATA_STRIKE_TICK, tickCount);
        strikes++;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 128 * 128;
    }
}
