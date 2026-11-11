package com.waylays.eraphones.compat;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.FriendlyByteBuf;

/** Minecraft 1.21's buffer with the registries; on 1.20.1 a plain FriendlyByteBuf. */
public class RegistryFriendlyByteBuf extends FriendlyByteBuf {
    public RegistryFriendlyByteBuf(ByteBuf source) {
        super(source);
    }
}
