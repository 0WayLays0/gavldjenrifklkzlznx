package com.waylays.eraphones.horror;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/**
 * Words on a wall, a floor or a ceiling, put there by a game master (/ecrire): in blood, chalk, spray paint or soot.
 * The letters come one after another as if written then and there; blood keeps running down for a while. Rubbed out,
 * they fade and are gone.
 */
public class WallWriting extends Entity {
    public static final EntityDataAccessor<String> TEXT = SynchedEntityData.defineId(WallWriting.class, EntityDataSerializers.STRING);
    public static final EntityDataAccessor<String> STYLE = SynchedEntityData.defineId(WallWriting.class, EntityDataSerializers.STRING);
    /** The face it is on (a direction's index), the size of a letter (blocks), the turn of words on a floor or ceiling. */
    public static final EntityDataAccessor<Integer> FACE = SynchedEntityData.defineId(WallWriting.class, EntityDataSerializers.INT);
    public static final EntityDataAccessor<Float> SIZE = SynchedEntityData.defineId(WallWriting.class, EntityDataSerializers.FLOAT);
    public static final EntityDataAccessor<Float> TURN = SynchedEntityData.defineId(WallWriting.class, EntityDataSerializers.FLOAT);
    /** When it was written, and when it was rubbed out (game ticks; -1 while it stays). */
    public static final EntityDataAccessor<Long> BORN = SynchedEntityData.defineId(WallWriting.class, EntityDataSerializers.LONG);
    public static final EntityDataAccessor<Long> GONE = SynchedEntityData.defineId(WallWriting.class, EntityDataSerializers.LONG);
    /** How long rubbing out takes (ticks). */
    public static final int FADE = 40;

    public WallWriting(EntityType<?> type, Level level) {
        super(type, level);
        noPhysics = true;
        setNoGravity(true);
    }

    @Override
    protected void defineSynchedData() {
        b.define(TEXT, "");
        b.define(STYLE, "sang");
        b.define(FACE, 2);
        b.define(SIZE, .45f);
        b.define(TURN, 0f);
        b.define(BORN, 0L);
        b.define(GONE, -1L);
    }

    public void setup(String text, String style, int face, float size, float turn) {
        entityData.set(TEXT, text);
        entityData.set(STYLE, style);
        entityData.set(FACE, face);
        entityData.set(SIZE, size);
        entityData.set(TURN, turn);
        entityData.set(BORN, level().getGameTime());
    }

    /** Starts rubbing it out. */
    public void rubOut() {
        if (entityData.get(GONE) < 0) {
            entityData.set(GONE, level().getGameTime());
        }
    }

    @Override
    public void tick() {
        if (!level().isClientSide) {
            long gone = entityData.get(GONE);
            if (gone >= 0 && level().getGameTime() > gone + FADE) {
                discard();
            }
        }
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
    protected void readAdditionalSaveData(CompoundTag tag) {
        entityData.set(TEXT, tag.getString("Text"));
        entityData.set(STYLE, tag.getString("Style"));
        entityData.set(FACE, tag.getInt("Face"));
        entityData.set(SIZE, tag.getFloat("Size"));
        entityData.set(TURN, tag.getFloat("Turn"));
        // Written long ago: all of it there, the blood run
        entityData.set(BORN, -100000L);
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putString("Text", entityData.get(TEXT));
        tag.putString("Style", entityData.get(STYLE));
        tag.putInt("Face", entityData.get(FACE));
        tag.putFloat("Size", entityData.get(SIZE));
        tag.putFloat("Turn", entityData.get(TURN));
    }
}
