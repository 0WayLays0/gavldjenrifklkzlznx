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
import org.joml.Vector3f;

/** An arc of lightning leaping through a few points (a chain of victims), gone in a flash. Only a picture. */
public class GlyphArc extends SpellEffectEntity {
    public static final int MAX_POINTS = 6;
    public static final int LIFE = 8;

    private static final EntityDataAccessor<Integer> DATA_ELEMENT = SynchedEntityData.defineId(GlyphArc.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_COUNT = SynchedEntityData.defineId(GlyphArc.class, EntityDataSerializers.INT);
    /** The points after the first one (which is the arc's position), relative to it. */
    @SuppressWarnings("unchecked")
    private static final EntityDataAccessor<Vector3f>[] DATA_POINTS = new EntityDataAccessor[MAX_POINTS - 1];

    static {
        for (int i = 0; i < DATA_POINTS.length; i++) {
            DATA_POINTS[i] = SynchedEntityData.defineId(GlyphArc.class, EntityDataSerializers.VECTOR3);
        }
    }

    public GlyphArc(EntityType<? extends GlyphArc> entityType, Level level) {
        super(entityType, level);
    }

    /** An arc through {@code points} (2 to {@link #MAX_POINTS}; the rest are left out). */
    public static void zap(Level level, GlyphElement element, Vec3... points) {
        if (points.length < 2) {
            return;
        }
        GlyphArc arc = new GlyphArc(ModEntities.GLYPH_ARC.get(), level);
        Vec3 origin = points[0];
        arc.moveTo(origin.x, origin.y, origin.z, 0, 0);
        int count = Math.min(points.length, MAX_POINTS);
        arc.entityData.set(DATA_ELEMENT, element.ordinal());
        arc.entityData.set(DATA_COUNT, count);
        for (int i = 1; i < count; i++) {
            Vec3 d = points[i].subtract(origin);
            arc.entityData.set(DATA_POINTS[i - 1], new Vector3f((float) d.x, (float) d.y, (float) d.z));
        }
        level.addFreshEntity(arc);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_ELEMENT, GlyphElement.STORM.ordinal());
        builder.define(DATA_COUNT, 2);
        for (EntityDataAccessor<Vector3f> point : DATA_POINTS) {
            builder.define(point, new Vector3f());
        }
    }

    public GlyphElement getElement() {
        return GlyphElement.byId(this.entityData.get(DATA_ELEMENT));
    }

    public int getCount() {
        return Mth.clamp(this.entityData.get(DATA_COUNT), 2, MAX_POINTS);
    }

    /** Point {@code i}, relative to the arc's position. */
    public Vec3 point(int i) {
        if (i <= 0) {
            return Vec3.ZERO;
        }
        Vector3f p = this.entityData.get(DATA_POINTS[i - 1]);
        return new Vec3(p.x, p.y, p.z);
    }

    /** 1 when it strikes, fading to 0. */
    public float strength(float partialTicks) {
        return Mth.clamp(1 - (tickCount + partialTicks) / LIFE, 0, 1);
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide && tickCount > LIFE) {
            discard();
        }
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 96 * 96;
    }
}
