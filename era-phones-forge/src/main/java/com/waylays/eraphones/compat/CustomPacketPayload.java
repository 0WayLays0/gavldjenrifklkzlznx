package com.waylays.eraphones.compat;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

/** A packet of the mod's, as Minecraft 1.21 has them: its type (an id) and its fields' codec. */
public interface CustomPacketPayload {
    Type<? extends CustomPacketPayload> type();

    record Type<T extends CustomPacketPayload>(ResourceLocation id) {}

    @FunctionalInterface
    interface StreamMemberEncoder<B, T> {
        void encode(T value, B buf);
    }

    @FunctionalInterface
    interface StreamDecoder<B, T> {
        T decode(B buf);
    }

    static <B extends FriendlyByteBuf, T extends CustomPacketPayload> StreamCodec<B, T> codec(StreamMemberEncoder<B, T> encoder,
                                                                                         StreamDecoder<B, T> decoder) {
        return StreamCodec.of((b, v) -> encoder.encode(v, b), decoder::decode);
    }
}
