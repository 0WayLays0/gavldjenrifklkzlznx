package com.waylays.arcanes.compat.epicnpc;

import com.waylays.arcanes.ArcanesOublies;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddPackFindersEvent;

/**
 * Custom NPCs' NPCs with Epic Fight's animations, when both mods are there (nothing happens otherwise, and neither
 * mod is needed): a built-in data pack gives the human-shaped NPCs an Epic Fight body (how they stand, walk, run,
 * fall, die, take a blow, hold a two-handed weapon), and {@link EpicNpcBridge} turns each of their melee blows into
 * the attack animation of the weapon they hold, the blow landing when the swing does.
 */
public final class EpicNpc {
    private EpicNpc() {}

    public static boolean active() {
        ModList mods = ModList.get();
        return mods != null && mods.isLoaded("epicfight") && mods.isLoaded("customnpcs");
    }

    public static void init(IEventBus modBus) {
        if (!active()) {
            return;
        }
        ArcanesOublies.LOGGER.info("Epic Fight and Custom NPCs are both here: the NPCs walk and fight with Epic Fight's animations");
        modBus.addListener(EpicNpc::onPackFinders);
        NeoForge.EVENT_BUS.register(EpicNpcBridge.class);
    }

    private static void onPackFinders(AddPackFindersEvent event) {
        if (event.getPackType() == PackType.SERVER_DATA) {
            event.addPackFinders(ArcanesOublies.id("epicfight_npc"), PackType.SERVER_DATA, Component.literal("Arcanes Oubliés × Epic Fight"),
                    PackSource.BUILT_IN, true, Pack.Position.TOP);
        }
    }
}
