package com.waylays.eraphones.compat;

import net.minecraft.world.entity.player.Player;

import java.util.concurrent.CompletableFuture;

/** Where a packet came in: the player (the sender on the server, the player on the client); handled on the main thread. */
public final class IPayloadContext {
    private final Player player;

    IPayloadContext(Player player) {
        this.player = player;
    }

    public Player player() {
        return player;
    }

    /** Already on the main thread: done now. */
    public CompletableFuture<Void> enqueueWork(Runnable work) {
        work.run();
        return CompletableFuture.completedFuture(null);
    }
}
