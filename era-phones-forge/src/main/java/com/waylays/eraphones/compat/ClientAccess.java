package com.waylays.eraphones.compat;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;

/** The client's own player (only called on the client). */
public final class ClientAccess {
    private ClientAccess() {}

    public static Player player() {
        return Minecraft.getInstance().player;
    }
}
