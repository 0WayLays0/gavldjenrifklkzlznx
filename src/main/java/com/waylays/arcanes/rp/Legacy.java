package com.waylays.arcanes.rp;

import net.minecraft.resources.ResourceLocation;

/**
 * The names Époque RP gave what it kept (saved data, a player's own data, attribute modifiers) when these commands
 * were part of it: kept as they were, so a world played with Époque RP keeps its characters' names, sizes, health,
 * speeds, fear and triggers here.
 */
public final class Legacy {
    public static final String PREFIX = "eraphones_";

    private Legacy() {}

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("eraphones", path);
    }
}
