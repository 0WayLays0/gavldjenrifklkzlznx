package com.waylays.arcanes;

import net.minecraft.resources.ResourceLocation;

/** Render lab stand-in for the mod class: only its id. */
public final class ArcanesOublies {
    public static final String MODID = "arcanes_oublies";

    private ArcanesOublies() {}

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }
}
