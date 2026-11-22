package com.waylays.cinematic.net;

import com.waylays.cinematic.studio.StudioServer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** A piece of a skin (key = its SHA-1) or of a take (key = the actor's id), client to server. */
public record UploadMsg(int kind, String key, int total, int offset, byte[] data) {
    public static final int SKIN = 0, TAKE = 1, SKIN_REQUEST = 2;

    public UploadMsg(FriendlyByteBuf buf) {
        this(buf.readVarInt(), buf.readUtf(64), buf.readVarInt(), buf.readVarInt(), buf.readByteArray(Net.UP_CHUNK + 16));
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(kind);
        buf.writeUtf(key, 64);
        buf.writeVarInt(total);
        buf.writeVarInt(offset);
        buf.writeByteArray(data);
    }

    public static void handle(UploadMsg msg, Supplier<NetworkEvent.Context> ctx) {
        if (ctx.get().getSender() != null) StudioServer.INSTANCE.onUpload(ctx.get().getSender(), msg);
        ctx.get().setPacketHandled(true);
    }
}
