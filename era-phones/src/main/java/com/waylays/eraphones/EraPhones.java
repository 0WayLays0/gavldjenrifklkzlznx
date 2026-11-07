package com.waylays.eraphones;

import com.mojang.logging.LogUtils;
import com.waylays.eraphones.network.PhoneNet;
import com.waylays.eraphones.registry.EraRegistry;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

/**
 * Téléphones d'Époque: four telephones for role play, one for each era (a 1985 brick radiotelephone, a 1997 GSM
 * handset, a 2004 flip phone, a 2026 smartphone). Each phone has its own number; the directory, the messages and the
 * calls belong to that number, so whoever holds the phone holds the line. Nothing on them speaks of anything outside
 * the story: no account names, only numbers and the names their owners write.
 */
@Mod(EraPhones.MODID)
public final class EraPhones {
    public static final String MODID = "eraphones";
    public static final Logger LOGGER = LogUtils.getLogger();

    public EraPhones(IEventBus modBus, ModContainer container) {
        EraRegistry.register(modBus);
        modBus.addListener(PhoneNet::register);
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }
}
