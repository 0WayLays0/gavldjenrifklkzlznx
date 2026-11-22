package com.waylays.cinematic.studio;

import net.minecraft.network.FriendlyByteBuf;

/** An edit on top of a performance: at {@code tick}, {@code part} is turned by x/y/z degrees (eased between keys). */
public record PoseKey(int tick, BodyPart part, float x, float y, float z) {
    public void write(FriendlyByteBuf buf) {
        buf.writeVarInt(tick);
        buf.writeEnum(part);
        buf.writeFloat(x);
        buf.writeFloat(y);
        buf.writeFloat(z);
    }

    public static PoseKey read(FriendlyByteBuf buf) {
        return new PoseKey(buf.readVarInt(), buf.readEnum(BodyPart.class), buf.readFloat(), buf.readFloat(), buf.readFloat());
    }
}
