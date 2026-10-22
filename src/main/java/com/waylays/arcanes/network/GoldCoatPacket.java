package com.waylays.arcanes.network;

import com.waylays.arcanes.ArcanesOublies;
import com.waylays.arcanes.client.gold.ClientGoldCoat;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Server to client: how much gold covers a body (see {@link com.waylays.arcanes.spells.gold.GoldCoat}): its coats of
 * Gilding, when its Midas Curse began and how long it lasts (0 if none), and whether it wears Gilded Armor.
 */
public record GoldCoatPacket(int entityId, int coats, long midasStart, int midasDuration, boolean armor) implements CustomPacketPayload {
    public static final Type<GoldCoatPacket> TYPE = new Type<>(ArcanesOublies.id("gold_coat"));
    public static final StreamCodec<ByteBuf, GoldCoatPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, GoldCoatPacket::entityId,
            ByteBufCodecs.VAR_INT, GoldCoatPacket::coats,
            ByteBufCodecs.VAR_LONG, GoldCoatPacket::midasStart,
            ByteBufCodecs.VAR_INT, GoldCoatPacket::midasDuration,
            ByteBufCodecs.BOOL, GoldCoatPacket::armor,
            GoldCoatPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /** Only ever handled on a client (the payload is registered client-bound). */
    public void handle(IPayloadContext context) {
        ClientGoldCoat.set(this);
    }
}
