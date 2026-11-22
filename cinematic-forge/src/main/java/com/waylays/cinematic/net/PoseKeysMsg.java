package com.waylays.cinematic.net;

import com.waylays.cinematic.studio.PoseKey;
import com.waylays.cinematic.studio.StudioServer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

/** The pose edits of an actor's take, saved from the pose editor (client to server). */
public record PoseKeysMsg(UUID actor, List<PoseKey> keys) {
    private static final int MAX = 1200;

    public PoseKeysMsg(FriendlyByteBuf buf) {
        this(buf.readUUID(), readKeys(buf));
    }

    private static List<PoseKey> readKeys(FriendlyByteBuf buf) {
        int n = Math.min(buf.readVarInt(), MAX);
        List<PoseKey> keys = new ArrayList<>(n);
        for (int i = 0; i < n; i++) keys.add(PoseKey.read(buf));
        return keys;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUUID(actor);
        int n = Math.min(keys.size(), MAX);
        buf.writeVarInt(n);
        for (int i = 0; i < n; i++) keys.get(i).write(buf);
    }

    public static void handle(PoseKeysMsg msg, Supplier<NetworkEvent.Context> ctx) {
        if (ctx.get().getSender() != null) StudioServer.INSTANCE.onPoseKeys(ctx.get().getSender(), msg);
        ctx.get().setPacketHandled(true);
    }
}
