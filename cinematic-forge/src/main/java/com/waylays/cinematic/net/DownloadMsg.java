package com.waylays.cinematic.net;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** A piece of a skin (key = SHA-1) or of a take (key = actor id), server to client. */
public record DownloadMsg(int kind, String key, int total, int offset, byte[] data) {
    public static final int SKIN = 0, TAKE = 1;

    public DownloadMsg(FriendlyByteBuf buf) {
        this(buf.readVarInt(), buf.readUtf(64), buf.readVarInt(), buf.readVarInt(), buf.readByteArray(Net.DOWN_CHUNK + 16));
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(kind);
        buf.writeUtf(key, 64);
        buf.writeVarInt(total);
        buf.writeVarInt(offset);
        buf.writeByteArray(data);
    }

    public static void handle(DownloadMsg msg, Supplier<NetworkEvent.Context> ctx) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> com.waylays.cinematic.client.ClientStudio.onDownload(msg));
        ctx.get().setPacketHandled(true);
    }
}
