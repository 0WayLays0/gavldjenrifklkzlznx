package com.waylays.eraphones;

import com.mojang.logging.LogUtils;
import com.waylays.eraphones.network.PhoneNet;
import com.waylays.eraphones.registry.EraRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import org.slf4j.Logger;

/**
 * Téléphones d'Époque: a role-play server's mod. Four telephones, one for each era (a 1985 brick radiotelephone, a 1997
 * GSM handset, a 2004 flip phone, a 2026 smartphone): each has its own number; the directory, the messages and the calls
 * belong to that number, so whoever holds the phone holds the line. And the role play itself: characters' names, speech
 * heard around the speaker, /me, /do, /roll, /try, whispers and shouts, bubbles over the heads, and /voir (seen and
 * heard by one player only, Simple Voice Chat included). Nothing speaks of anything outside the story.
 */
@Mod(EraPhones.MODID)
public final class EraPhones {
    public static final String MODID = "eraphones";
    public static final Logger LOGGER = LogUtils.getLogger();

    public EraPhones() {
        IEventBus modBus = net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext.get().getModEventBus();
        EraRegistry.register(modBus);
        com.waylays.eraphones.registry.GearRegistry.register(modBus);
        com.waylays.eraphones.event.EventRegistry.register(modBus);
        com.waylays.eraphones.pack.PackRegistry.register(modBus);
        com.waylays.eraphones.walkman.WalkmanRegistry.init();
        com.waylays.eraphones.lock.LockRegistry.register(modBus);
        com.waylays.eraphones.arms.Arms.init();
        com.waylays.eraphones.compat.Net.open(PhoneNet::register);
        net.minecraftforge.fml.ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, com.waylays.eraphones.rp.RpConfig.SPEC);
        if (net.minecraftforge.fml.loading.FMLEnvironment.dist.isClient()) {
            com.waylays.eraphones.client.EmoteInstaller.install();
        }
    }

    public static ResourceLocation id(String path) {
        return new ResourceLocation(MODID, path);
    }
}
