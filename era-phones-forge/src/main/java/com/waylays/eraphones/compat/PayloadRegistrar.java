package com.waylays.eraphones.compat;

/** Registers the mod's packets (as NeoForge's registrar does), each with its codec and what is done on receipt. */
public final class PayloadRegistrar {
    PayloadRegistrar() {}

    public <T extends CustomPacketPayload> PayloadRegistrar playToServer(CustomPacketPayload.Type<T> type,
                                                                         StreamCodec<? super RegistryFriendlyByteBuf, T> codec, IPayloadHandler<T> handler) {
        Net.add(type, codec, handler, true);
        return this;
    }

    public <T extends CustomPacketPayload> PayloadRegistrar playToClient(CustomPacketPayload.Type<T> type,
                                                                         StreamCodec<? super RegistryFriendlyByteBuf, T> codec, IPayloadHandler<T> handler) {
        Net.add(type, codec, handler, false);
        return this;
    }

    public <T extends CustomPacketPayload> PayloadRegistrar playBidirectional(CustomPacketPayload.Type<T> type,
                                                                              StreamCodec<? super RegistryFriendlyByteBuf, T> codec, IPayloadHandler<T> handler) {
        Net.add(type, codec, handler, true);
        return this;
    }
}
