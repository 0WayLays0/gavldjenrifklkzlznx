package com.waylays.eraphones;

import com.mojang.logging.LogUtils;
import com.waylays.eraphones.network.PhoneNet;
import com.waylays.eraphones.registry.EraRegistry;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
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

    public EraPhones(IEventBus modBus, ModContainer container) {
        EraRegistry.register(modBus);
        com.waylays.eraphones.registry.GearRegistry.register(modBus);
        com.waylays.eraphones.event.EventRegistry.register(modBus);
        modBus.addListener(PhoneNet::register);
        container.registerConfig(ModConfig.Type.SERVER, com.waylays.eraphones.rp.RpConfig.SPEC);
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }
}
