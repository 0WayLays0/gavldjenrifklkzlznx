package com.waylays.arcanes.client.ritual;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;

/** Client side of the Altar of Rites: opens its book. */
public final class AltarClient {
    private AltarClient() {}

    public static void open(BlockPos pos) {
        Minecraft.getInstance().setScreen(new AltarOfRitesScreen(pos));
    }
}
