package com.waylays.arcanes.network;

import com.waylays.arcanes.ArcanesOublies;
import com.waylays.arcanes.ritual.AltarOfRites;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Client to server: a ritual chosen in an Altar of Rites' book, to prepare round the altar (or set in the chalk held). */
public record AltarRitualPacket(BlockPos pos, int ritual) implements CustomPacketPayload {
    public static final Type<AltarRitualPacket> TYPE = new Type<>(ArcanesOublies.id("altar_ritual"));
    public static final StreamCodec<ByteBuf, AltarRitualPacket> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, AltarRitualPacket::pos,
            ByteBufCodecs.VAR_INT, AltarRitualPacket::ritual,
            AltarRitualPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /** Handled on the server (the payload is registered server-bound). */
    public void handle(IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player) {
            AltarOfRites.choose(player, pos, ritual);
        }
    }
}
