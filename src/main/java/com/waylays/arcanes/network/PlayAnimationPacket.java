package com.waylays.arcanes.network;

import com.waylays.arcanes.ArcanesOublies;
import com.waylays.arcanes.client.ClientAnimations;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Server to client: play a player animation on the player with this entity id. */
public record PlayAnimationPacket(int entityId, ResourceLocation animation) implements CustomPacketPayload {
    public static final Type<PlayAnimationPacket> TYPE = new Type<>(ArcanesOublies.id("play_animation"));
    public static final StreamCodec<ByteBuf, PlayAnimationPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, PlayAnimationPacket::entityId,
            ResourceLocation.STREAM_CODEC, PlayAnimationPacket::animation,
            PlayAnimationPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /** Only ever handled on a client (the payload is registered client-bound). */
    public void handle(IPayloadContext context) {
        ClientAnimations.play(entityId, animation);
    }
}
