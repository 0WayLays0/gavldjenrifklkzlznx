package com.waylays.cinematic.net;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

/** The studio's contents (scenes, actors), without the takes (server to client). */
public record SyncMsg(boolean canDirect, List<SceneInfo> scenes) {
    public record ActorInfo(UUID id, String name, String skin, boolean slim, int length, int blocks, int keys) {}

    public record SceneInfo(String name, List<ActorInfo> actors) {}

    public SyncMsg(FriendlyByteBuf buf) {
        this(buf.readBoolean(), readScenes(buf));
    }

    private static List<SceneInfo> readScenes(FriendlyByteBuf buf) {
        int n = buf.readVarInt();
        List<SceneInfo> scenes = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            String name = buf.readUtf(64);
            int m = buf.readVarInt();
            List<ActorInfo> actors = new ArrayList<>();
            for (int j = 0; j < m; j++) {
                actors.add(new ActorInfo(buf.readUUID(), buf.readUtf(64), buf.readUtf(64), buf.readBoolean(), buf.readVarInt(),
                        buf.readVarInt(), buf.readVarInt()));
            }
            scenes.add(new SceneInfo(name, actors));
        }
        return scenes;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBoolean(canDirect);
        buf.writeVarInt(scenes.size());
        for (SceneInfo s : scenes) {
            buf.writeUtf(s.name(), 64);
            buf.writeVarInt(s.actors().size());
            for (ActorInfo a : s.actors()) {
                buf.writeUUID(a.id());
                buf.writeUtf(a.name(), 64);
                buf.writeUtf(a.skin(), 64);
                buf.writeBoolean(a.slim());
                buf.writeVarInt(a.length());
                buf.writeVarInt(a.blocks());
                buf.writeVarInt(a.keys());
            }
        }
    }

    public static void handle(SyncMsg msg, Supplier<NetworkEvent.Context> ctx) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> com.waylays.cinematic.client.ClientStudio.onSync(msg));
        ctx.get().setPacketHandled(true);
    }
}
