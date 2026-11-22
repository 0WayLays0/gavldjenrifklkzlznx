package com.waylays.cinematic;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.loading.FMLEnvironment;

/** Client-only mod: on a dedicated server it does nothing at all. */
@Mod(CinematicMod.MOD_ID)
public final class CinematicMod {
    public static final String MOD_ID = "cinematic";

    public CinematicMod() {
        if (FMLEnvironment.dist == Dist.CLIENT) {
            ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, CinematicConfig.SPEC);
            CinematicClient.init();
        }
    }
}
