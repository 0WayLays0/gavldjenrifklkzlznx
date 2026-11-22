package com.waylays.cinematic;

import com.waylays.cinematic.net.Net;
import com.waylays.cinematic.studio.StudioConfig;
import com.waylays.cinematic.studio.StudioRegistry;
import com.waylays.cinematic.studio.StudioServer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.loading.FMLEnvironment;

/**
 * The film look and cameras are client-side; the studio (actors, recording, scenes) needs the mod on the server too.
 */
@Mod(CinematicMod.MOD_ID)
public final class CinematicMod {
    public static final String MOD_ID = "cinematic";

    public CinematicMod() {
        StudioRegistry.register(FMLJavaModLoadingContext.get().getModEventBus());
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, StudioConfig.SPEC);
        Net.register();
        StudioServer.init();
        if (FMLEnvironment.dist == Dist.CLIENT) {
            ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, CinematicConfig.SPEC);
            CinematicClient.init();
        }
    }
}
