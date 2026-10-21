package com.waylays.arcanes.entity.glyph;

import com.waylays.arcanes.entity.SpellEffectEntity;
import com.waylays.arcanes.registry.ModEntities;
import com.waylays.arcanes.spells.glyph.GlyphElement;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * A glyph written in light: its strokes are traced one after another by a glowing pen tip, then it flares as its
 * spell goes off and fades. Upright facing its yaw and pitch (written in the air before the caster), or lying flat on
 * the ground. Only a picture: the spell that writes it does the rest when the writing is done.
 */
public class GlyphSign extends SpellEffectEntity {
    /** Ticks it glows once written, and fades. */
    public static final int HOLD_TICKS = 6;
    public static final int FADE_TICKS = 10;

    private static final EntityDataAccessor<Integer> DATA_ELEMENT = SynchedEntityData.defineId(GlyphSign.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DATA_SIZE = SynchedEntityData.defineId(GlyphSign.class, EntityDataSerializers.FLOAT);
    /** Ticks before the pen starts, and ticks the writing takes. */
    private static final EntityDataAccessor<Integer> DATA_DELAY = SynchedEntityData.defineId(GlyphSign.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_DRAW = SynchedEntityData.defineId(GlyphSign.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_FLAT = SynchedEntityData.defineId(GlyphSign.class, EntityDataSerializers.BOOLEAN);

    public GlyphSign(EntityType<? extends GlyphSign> entityType, Level level) {
        super(entityType, level);
    }

    /** Writes a glyph at {@code at}, facing {@code yaw}/{@code pitch}, starting in {@code delay} ticks, over {@code draw} ticks. */
    public static GlyphSign write(Level level, GlyphElement element, Vec3 at, float yaw, float pitch, float size, int delay, int draw, boolean flat) {
        GlyphSign sign = new GlyphSign(ModEntities.GLYPH_SIGN.get(), level);
        sign.moveTo(at.x, at.y, at.z, yaw, pitch);
        sign.entityData.set(DATA_ELEMENT, element.ordinal());
        sign.entityData.set(DATA_SIZE, size);
        sign.entityData.set(DATA_DELAY, delay);
        sign.entityData.set(DATA_DRAW, Math.max(1, draw));
        sign.entityData.set(DATA_FLAT, flat);
        level.addFreshEntity(sign);
        return sign;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_ELEMENT, 0);
        builder.define(DATA_SIZE, 1f);
        builder.define(DATA_DELAY, 0);
        builder.define(DATA_DRAW, 6);
        builder.define(DATA_FLAT, false);
    }

    public GlyphElement getElement() {
        return GlyphElement.byId(this.entityData.get(DATA_ELEMENT));
    }

    public float getSize() {
        return this.entityData.get(DATA_SIZE);
    }

    public boolean isFlat() {
        return this.entityData.get(DATA_FLAT);
    }

    public int getDelay() {
        return this.entityData.get(DATA_DELAY);
    }

    public int getDraw() {
        return this.entityData.get(DATA_DRAW);
    }

    /** How much of the glyph is written: 0 to 1. */
    public float written(float partialTicks) {
        return Mth.clamp((tickCount + partialTicks - getDelay()) / getDraw(), 0, 1);
    }

    /** Ticks since it was finished (negative before). */
    public float sinceWritten(float partialTicks) {
        return tickCount + partialTicks - getDelay() - getDraw();
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide && tickCount > getDelay() + getDraw() + HOLD_TICKS + FADE_TICKS) {
            discard();
        }
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 96 * 96;
    }
}
