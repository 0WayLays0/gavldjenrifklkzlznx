package com.waylays.arcanes;

import com.mojang.logging.LogUtils;
import com.waylays.arcanes.registry.ModAttributes;
import com.waylays.arcanes.registry.ModCreativeTabs;
import com.waylays.arcanes.registry.ModEffects;
import com.waylays.arcanes.registry.ModEntities;
import com.waylays.arcanes.registry.ModItems;
import com.waylays.arcanes.registry.ModSchools;
import com.waylays.arcanes.registry.ModSpells;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

/**
 * Arcanes Oubliés: an addon for Iron's Spells 'n Spellbooks that adds new schools of magic.
 * Content so far: the Gold school (10 spells, the Golden Grimoire).
 */
@Mod(ArcanesOublies.MODID)
public class ArcanesOublies {
    public static final String MODID = "arcanes_oublies";
    public static final Logger LOGGER = LogUtils.getLogger();

    public ArcanesOublies() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        ModAttributes.ATTRIBUTES.register(modBus);
        ModSchools.SCHOOLS.register(modBus);
        ModSpells.SPELLS.register(modBus);
        ModItems.ITEMS.register(modBus);
        ModEntities.ENTITIES.register(modBus);
        ModEffects.EFFECTS.register(modBus);
        ModCreativeTabs.TABS.register(modBus);
    }

    public static ResourceLocation id(String path) {
        return new ResourceLocation(MODID, path);
    }
}
