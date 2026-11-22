package com.waylays.cinematic.net;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * What the studio is doing (server to client): nothing, a countdown, playing a scene, or recording an actor.
 * While playing, the scene's tick is {@code baseTick + (gameTime - baseGameTime)}.
 */
public record PlayStateMsg(String scene, boolean playing, int baseTick, long baseGameTime, int countdown, UUID recordingActor, UUID recorder) {
    private static final UUID NONE = new UUID(0, 0);

    public static PlayStateMsg idle() {
        return new PlayStateMsg("", false, 0, 0, 0, null, null);
    }

    public PlayStateMsg(FriendlyByteBuf buf) {
        this(buf.readUtf(64), buf.readBoolean(), buf.readVarInt(), buf.readLong(), buf.readVarInt(), id(buf.readUUID()), id(buf.readUUID()));
    }

    private static UUID id(UUID u) {
        return NONE.equals(u) ? null : u;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(scene, 64);
        buf.writeBoolean(playing);
        buf.writeVarInt(baseTick);
        buf.writeLong(baseGameTime);
        buf.writeVarInt(countdown);
        buf.writeUUID(recordingActor == null ? NONE : recordingActor);
        buf.writeUUID(recorder == null ? NONE : recorder);
    }

    public static void handle(PlayStateMsg msg, Supplier<NetworkEvent.Context> ctx) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> com.waylays.cinematic.client.ClientStudio.onPlayState(msg));
        ctx.get().setPacketHandled(true);
    }
}
