package com.waylays.arcanes.entity.boss;

import com.waylays.arcanes.registry.ModEntities;
import com.waylays.arcanes.spells.boss.DivinePrismSpell;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/**
 * Scenery of a Boss Magic attack, played out on a fixed schedule both sides compute from the game clock: a meteor or
 * a star falling from the sky onto its warning zone, a giant blade raised then brought down, a pillar of light. It
 * hurts no one (the warning zone deals the damage when it runs out); it only shows what is coming.
 */
public class BossProp extends Entity {
    /**
     * What it shows. The first four are drawn by their renderer itself; the rest are the great attacks' scenery, each a
     * model of its own (client/render/boss).
     */
    public enum Kind {
        METEOR, STAR, BLADE, PILLAR, SWORD, WHEEL, ECLIPSE, MOON, MAW, PRISM, CHAINS, STORM, BOLT, THORNS, WINGS,
        BELL, FIST, LANCE, SCYTHE, ORBITAL, KING, LASER, LOTUS, COLLAPSE
    }

    private static final EntityDataAccessor<Integer> DATA_KIND = SynchedEntityData.defineId(BossProp.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_STYLE = SynchedEntityData.defineId(BossProp.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DATA_SIZE = SynchedEntityData.defineId(BossProp.class, EntityDataSerializers.FLOAT);
    /** When it appears and when it arrives (game ticks). */
    private static final EntityDataAccessor<Integer> DATA_START = SynchedEntityData.defineId(BossProp.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_END = SynchedEntityData.defineId(BossProp.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Vector3f> DATA_FROM = SynchedEntityData.defineId(BossProp.class, EntityDataSerializers.VECTOR3);
    private static final EntityDataAccessor<Vector3f> DATA_TO = SynchedEntityData.defineId(BossProp.class, EntityDataSerializers.VECTOR3);
    /** One more number some scenery needs (how long a wheel rolls, how far apart an eclipse's bands strike, ...). */
    private static final EntityDataAccessor<Float> DATA_ARG = SynchedEntityData.defineId(BossProp.class, EntityDataSerializers.FLOAT);

    public BossProp(EntityType<? extends BossProp> entityType, Level level) {
        super(entityType, level);
        noPhysics = true;
    }

    /**
     * A prop that shows up at game tick {@code start} at {@code from} and reaches {@code to} at {@code end}, then
     * lingers a little ({@link #lingerTicks()}). A blade swings down about {@code from}, toward {@code to}.
     */
    public static BossProp spawn(Level level, Kind kind, BossTelegraph.Style style, float size, int start, int end, Vec3 from, Vec3 to) {
        BossProp prop = new BossProp(ModEntities.BOSS_PROP.get(), level);
        prop.entityData.set(DATA_KIND, kind.ordinal());
        prop.entityData.set(DATA_STYLE, style.ordinal());
        prop.entityData.set(DATA_SIZE, size);
        prop.entityData.set(DATA_START, start);
        prop.entityData.set(DATA_END, end);
        prop.entityData.set(DATA_FROM, from.toVector3f());
        prop.entityData.set(DATA_TO, to.toVector3f());
        prop.setPos(from);
        if (kind == Kind.BLADE) {
            Vec3 d = to.subtract(from);
            prop.setYRot((float) (Mth.atan2(-d.x, d.z) * Mth.RAD_TO_DEG));
        }
        level.addFreshEntity(prop);
        return prop;
    }

    /**
     * Scenery standing at {@code at} (it does not move: its model works out where everything is from the clock),
     * showing from game tick {@code start}, its moment at {@code end}; {@code from} and {@code to} two points its model
     * needs, {@code arg} one more number, {@code yaw} which way it faces.
     */
    public static BossProp scene(Level level, Kind kind, BossTelegraph.Style style, float size, int start, int end, Vec3 at, Vec3 from, Vec3 to, float arg,
                                 float yaw) {
        BossProp prop = new BossProp(ModEntities.BOSS_PROP.get(), level);
        prop.entityData.set(DATA_KIND, kind.ordinal());
        prop.entityData.set(DATA_STYLE, style.ordinal());
        prop.entityData.set(DATA_SIZE, size);
        prop.entityData.set(DATA_START, start);
        prop.entityData.set(DATA_END, end);
        prop.entityData.set(DATA_FROM, from.toVector3f());
        prop.entityData.set(DATA_TO, to.toVector3f());
        prop.entityData.set(DATA_ARG, arg);
        prop.moveTo(at.x, at.y, at.z, yaw, 0);
        level.addFreshEntity(prop);
        return prop;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_KIND, 0);
        builder.define(DATA_STYLE, 0);
        builder.define(DATA_SIZE, 1f);
        builder.define(DATA_START, 0);
        builder.define(DATA_END, 0);
        builder.define(DATA_FROM, new Vector3f());
        builder.define(DATA_TO, new Vector3f());
        builder.define(DATA_ARG, 0f);
    }

    public Kind getKind() {
        Kind[] all = Kind.values();
        return all[Mth.clamp(this.entityData.get(DATA_KIND), 0, all.length - 1)];
    }

    public BossTelegraph.Style getStyle() {
        return BossTelegraph.Style.byId(this.entityData.get(DATA_STYLE));
    }

    public float getSize() {
        return this.entityData.get(DATA_SIZE);
    }

    public Vec3 getFrom() {
        return new Vec3(this.entityData.get(DATA_FROM));
    }

    public Vec3 getTo() {
        return new Vec3(this.entityData.get(DATA_TO));
    }

    public float getArg() {
        return this.entityData.get(DATA_ARG);
    }

    public int getStartTick() {
        return this.entityData.get(DATA_START);
    }

    public int getEndTick() {
        return this.entityData.get(DATA_END);
    }

    /** Ticks past its start (negative before it shows). */
    public float age(float partialTicks) {
        return (int) level().getGameTime() + partialTicks - this.entityData.get(DATA_START);
    }

    /** 0 when it shows up, 1 when it arrives. */
    public float travel(float partialTicks) {
        int start = this.entityData.get(DATA_START);
        int end = this.entityData.get(DATA_END);
        return end <= start ? 1 : Mth.clamp(age(partialTicks) / (end - start), 0, 1);
    }

    /** Ticks since it arrived (negative before). */
    public float sinceArrival(float partialTicks) {
        return (int) level().getGameTime() + partialTicks - this.entityData.get(DATA_END);
    }

    public int lingerTicks() {
        return switch (getKind()) {
            case METEOR, STAR -> 2;
            case BLADE -> 14;
            case PILLAR -> 14;
            // As long as each model's aftermath lasts (see its model)
            case SWORD -> 18;
            case WHEEL -> 14;
            case ECLIPSE -> (int) (2 * getArg()) + 50;
            case MOON -> 40;
            case MAW -> 26;
            case PRISM -> 11 * DivinePrismSpell.INTERVAL + 44;
            case CHAINS -> 28;
            case STORM -> 32;
            case BOLT -> 9;
            case THORNS -> 40;
            case WINGS -> 26;
            // Their aftermath is in their own time: they end when it does
            case BELL, FIST, LANCE, SCYTHE, ORBITAL, KING, LASER, LOTUS, COLLAPSE -> 1;
        };
    }

    /** Where a falling prop is: it speeds up as it comes down. */
    public Vec3 fallPosition(float partialTicks) {
        float f = travel(partialTicks);
        return getFrom().lerp(getTo(), f * f);
    }

    @Override
    public void tick() {
        super.tick();
        Kind kind = getKind();
        if (kind == Kind.METEOR || kind == Kind.STAR) {
            setPos(fallPosition(0));
        }
        float age = age(0);
        if (level().isClientSide) {
            if (age >= 0 && sinceArrival(0) < 0) {
                trail(kind);
            }
            return;
        }
        if (age == 0) {
            switch (kind) {
                case METEOR -> level().playSound(null, getX(), getY(), getZ(), SoundEvents.BLAZE_SHOOT, SoundSource.HOSTILE, 3f, .4f);
                case STAR -> level().playSound(null, getX(), getY(), getZ(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.HOSTILE, 2f, 1.6f);
                case BLADE -> level().playSound(null, getX(), getY(), getZ(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.HOSTILE, 3f, .5f);
                case PILLAR -> level().playSound(null, getX(), getY(), getZ(), SoundEvents.BEACON_ACTIVATE, SoundSource.HOSTILE, 1.5f, 1.8f);
            }
        }
        if (sinceArrival(0) > lingerTicks() || tickCount > 1200) {
            discard();
        }
    }

    private void trail(Kind kind) {
        switch (kind) {
            case METEOR -> {
                float s = getSize() * .4f;
                for (int i = 0; i < 4; i++) {
                    level().addParticle(ParticleTypes.FLAME, getX() + (random.nextDouble() - .5) * s, getY() + (random.nextDouble() - .5) * s, getZ() + (random.nextDouble() - .5) * s, 0, .05, 0);
                }
                level().addParticle(ParticleTypes.LARGE_SMOKE, getX(), getY() + s, getZ(), 0, .1, 0);
                level().addParticle(ParticleTypes.LAVA, getX(), getY(), getZ(), 0, 0, 0);
            }
            case STAR -> level().addParticle(ParticleTypes.END_ROD, getX(), getY() + .5, getZ(), 0, .05, 0);
            default -> {
            }
        }
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 192 * 192;
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
    }
}
