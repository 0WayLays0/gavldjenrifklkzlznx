package com.waylays.eraphones.mixin;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerPlayerConnection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.Set;

/** /voir: who sees a tracked entity; taking one player off it and pairing them again. */
@Mixin(targets = "net.minecraft.server.level.ChunkMap$TrackedEntity")
public interface TrackedEntityAccessor {
    @Accessor("seenBy")
    Set<ServerPlayerConnection> eraphones$seenBy();

    @Invoker("removePlayer")
    void eraphones$removePlayer(ServerPlayer player);

    @Invoker("updatePlayer")
    void eraphones$updatePlayer(ServerPlayer player);
}
