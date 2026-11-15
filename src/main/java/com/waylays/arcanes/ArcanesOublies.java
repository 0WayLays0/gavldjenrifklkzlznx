package com.waylays.arcanes;

import com.mojang.logging.LogUtils;
import com.waylays.arcanes.network.ArcanesNetwork;
import com.waylays.arcanes.registry.ModAttributes;
import com.waylays.arcanes.registry.ModCreativeTabs;
import com.waylays.arcanes.registry.ModEffects;
import com.waylays.arcanes.registry.ModEntities;
import com.waylays.arcanes.registry.ModBlocks;
import com.waylays.arcanes.registry.ModItems;
import com.waylays.arcanes.registry.ModSchools;
import com.waylays.arcanes.registry.ModSpells;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import org.slf4j.Logger;

/**
 * Arcanes Oubliés: an addon for Iron's Spells 'n Spellbooks that adds new schools of magic.
 * Content so far: the Gold and Explosion schools (10 spells and a spellbook each), the Book school (16 spells and a
 * spellbook), Errethys, the school of summoned mana creatures (5 spells), and Physical Prowess, fighting moves that
 * are not magic (37 moves, with weapons from fists to crossbows).
 */
@Mod(ArcanesOublies.MODID)
public class ArcanesOublies {
    public static final String MODID = "arcanes_oublies";
    public static final Logger LOGGER = LogUtils.getLogger();

    public ArcanesOublies(IEventBus modBus, ModContainer container) {
        ModAttributes.ATTRIBUTES.register(modBus);
        ModSchools.SCHOOLS.register(modBus);
        ModSpells.SPELLS.register(modBus);
        ModBlocks.BLOCKS.register(modBus);
        ModItems.ITEMS.register(modBus);
        ModEntities.ENTITIES.register(modBus);
        ModEffects.EFFECTS.register(modBus);
        ModCreativeTabs.TABS.register(modBus);
        container.registerConfig(ModConfig.Type.SERVER, ArcanesConfig.SPEC);
        // The role play and the game masters' tools (once part of Époque RP)
        com.waylays.arcanes.rp.RpRegistry.register(modBus);
        com.waylays.arcanes.rp.Legacy.warnOldEpoqueRp();
        container.registerConfig(ModConfig.Type.SERVER, com.waylays.arcanes.rp.RpConfig.SPEC, MODID + "-roleplay-server.toml");
        modBus.addListener(ArcanesNetwork::register);
        // Custom NPCs' NPCs with Epic Fight's animations (only when both mods are there)
        com.waylays.arcanes.compat.epicnpc.EpicNpc.init(modBus);
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }
}
