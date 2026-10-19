package com.waylays.arcanes.entity.physical;

import com.waylays.arcanes.entity.SpellEffectEntity;
import com.waylays.arcanes.registry.ModEntities;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * The trace a fighting move leaves in the air for a few ticks, nothing magic: the pale streak of a blade sweeping
 * an arc (or a full circle), the line of a thrust, the burst of air in front of a punch. Drawn at the fighter's chest,
 * turned the way they faced when they struck.
 */
public class StrikeTrail extends SpellEffectEntity {
    public enum Style { SLASH, SPIN, STAB, PUNCH }

    public static final int LIFE = 8;
    /** How long the blade takes to sweep the arc. */
    public static final int SWEEP_TICKS = 3;

    public static final int STEEL = 0xE9F0FF;
    public static final int DARK_STEEL = 0xB4BCCB;
    public static final int AIR = 0xFFFFFF;

    private static final EntityDataAccessor<Integer> DATA_STYLE = SynchedEntityData.defineId(StrikeTrail.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DATA_RADIUS = SynchedEntityData.defineId(StrikeTrail.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_SPAN = SynchedEntityData.defineId(StrikeTrail.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_TILT = SynchedEntityData.defineId(StrikeTrail.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> DATA_REVERSE = SynchedEntityData.defineId(StrikeTrail.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> DATA_COLOR = SynchedEntityData.defineId(StrikeTrail.class, EntityDataSerializers.INT);

    public StrikeTrail(EntityType<? extends StrikeTrail> entityType, Level level) {
        super(entityType, level);
    }

    /**
     * Leaves a trace in front of {@code fighter}. {@code span} is the arc in degrees (slashes), {@code tilt} turns the
     * plane of the blade around the line of sight (0 level, 90 upright), {@code reverse} sweeps left to right.
     */
    public static void spawn(Level level, LivingEntity fighter, Style style, float radius, float span, float tilt, boolean reverse, int color) {
        spawn(level, fighter, style, radius, span, tilt, reverse, color, .62f);
    }

    /** Like {@link #spawn}, at {@code height} of the fighter's height (a low sweep cuts at the knees). */
    public static void spawn(Level level, LivingEntity fighter, Style style, float radius, float span, float tilt, boolean reverse, int color, float height) {
        StrikeTrail trail = new StrikeTrail(ModEntities.STRIKE_TRAIL.get(), level);
        Vec3 at = fighter.position().add(0, fighter.getBbHeight() * height, 0);
        float pitch = style == Style.SLASH || style == Style.SPIN ? fighter.getXRot() * .4f : fighter.getXRot();
        trail.moveTo(at.x, at.y, at.z, fighter.getYRot(), style == Style.SPIN ? 0 : pitch);
        trail.entityData.set(DATA_STYLE, style.ordinal());
        trail.entityData.set(DATA_RADIUS, radius);
        trail.entityData.set(DATA_SPAN, span);
        trail.entityData.set(DATA_TILT, tilt);
        trail.entityData.set(DATA_REVERSE, reverse);
        trail.entityData.set(DATA_COLOR, color);
        level.addFreshEntity(trail);
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(DATA_STYLE, 0);
        this.entityData.define(DATA_RADIUS, 3f);
        this.entityData.define(DATA_SPAN, 120f);
        this.entityData.define(DATA_TILT, 0f);
        this.entityData.define(DATA_REVERSE, false);
        this.entityData.define(DATA_COLOR, STEEL);
    }

    public Style getStyle() {
        Style[] styles = Style.values();
        return styles[Math.floorMod(this.entityData.get(DATA_STYLE), styles.length)];
    }

    public float getRadius() {
        return this.entityData.get(DATA_RADIUS);
    }

    public float getSpan() {
        return this.entityData.get(DATA_SPAN);
    }

    public float getTilt() {
        return this.entityData.get(DATA_TILT);
    }

    public boolean isReverse() {
        return this.entityData.get(DATA_REVERSE);
    }

    public int getColor() {
        return this.entityData.get(DATA_COLOR);
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide && tickCount >= LIFE) {
            discard();
        }
    }

    @Override
    public AABB getBoundingBoxForCulling() {
        return getBoundingBox().inflate(getRadius() + 1);
    }
}
