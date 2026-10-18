package com.waylays.arcanes.spells.book;

import io.redspace.ironsspellbooks.api.spells.ICastDataSerializable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.UUID;

/** Where Bookmark's marker stands, kept in the caster's recast until the spell is cast again. */
public class BookmarkData implements ICastDataSerializable {
    public Vec3 pos = Vec3.ZERO;
    public float yRot;
    public float xRot;
    public String dimension = "";
    @Nullable
    public UUID marker;

    public BookmarkData() {}

    public BookmarkData(Vec3 pos, float yRot, float xRot, String dimension, @Nullable UUID marker) {
        this.pos = pos;
        this.yRot = yRot;
        this.xRot = xRot;
        this.dimension = dimension;
        this.marker = marker;
    }

    @Override
    public void reset() {
        pos = Vec3.ZERO;
        dimension = "";
        marker = null;
    }

    @Override
    public void writeToBuffer(FriendlyByteBuf buffer) {
        buffer.writeDouble(pos.x);
        buffer.writeDouble(pos.y);
        buffer.writeDouble(pos.z);
        buffer.writeFloat(yRot);
        buffer.writeFloat(xRot);
        buffer.writeUtf(dimension);
        buffer.writeBoolean(marker != null);
        if (marker != null) {
            buffer.writeUUID(marker);
        }
    }

    @Override
    public void readFromBuffer(FriendlyByteBuf buffer) {
        pos = new Vec3(buffer.readDouble(), buffer.readDouble(), buffer.readDouble());
        yRot = buffer.readFloat();
        xRot = buffer.readFloat();
        dimension = buffer.readUtf();
        marker = buffer.readBoolean() ? buffer.readUUID() : null;
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putDouble("X", pos.x);
        tag.putDouble("Y", pos.y);
        tag.putDouble("Z", pos.z);
        tag.putFloat("YRot", yRot);
        tag.putFloat("XRot", xRot);
        tag.putString("Dimension", dimension);
        if (marker != null) {
            tag.putUUID("Marker", marker);
        }
        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag tag) {
        pos = new Vec3(tag.getDouble("X"), tag.getDouble("Y"), tag.getDouble("Z"));
        yRot = tag.getFloat("YRot");
        xRot = tag.getFloat("XRot");
        dimension = tag.getString("Dimension");
        marker = tag.hasUUID("Marker") ? tag.getUUID("Marker") : null;
    }
}
