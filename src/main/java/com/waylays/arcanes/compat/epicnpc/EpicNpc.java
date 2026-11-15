package com.waylays.arcanes.compat.epicnpc;

import com.waylays.arcanes.ArcanesOublies;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddPackFindersEvent;

import java.util.Set;

/**
 * Custom NPCs' NPCs with Epic Fight's animations, when both mods are there (nothing happens otherwise, and neither
 * mod is needed): a built-in data pack gives the human-shaped NPCs an Epic Fight body (how they stand, walk, run,
 * fall, die, take a blow, hold a two-handed weapon), {@link EpicNpcBridge} turns each of their melee blows into the
 * attack animation of the weapon they hold, whose own hit is the only one, and on the client
 * {@code client.EpicNpcClient} draws them with the body that fits their skin or the mob they are disguised as.
 */
public final class EpicNpc {
    /** Custom NPCs' human-shaped NPCs: the ones given an Epic Fight body. */
    public static final Set<ResourceLocation> NPC_TYPES = Set.of(ResourceLocation.fromNamespaceAndPath("customnpcs", "customnpc"),
            ResourceLocation.fromNamespaceAndPath("customnpcs", "customnpc64x32"), ResourceLocation.fromNamespaceAndPath("customnpcs", "customnpcalex"),
            ResourceLocation.fromNamespaceAndPath("customnpcs", "customnpcclassic"));

    private EpicNpc() {}

    public static boolean isNpc(Entity e) {
        return e instanceof Mob && NPC_TYPES.contains(BuiltInRegistries.ENTITY_TYPE.getKey(e.getType()));
    }

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
        if (FMLEnvironment.dist.isClient()) {
            com.waylays.arcanes.compat.epicnpc.client.EpicNpcClient.init();
        }
    }

    private static void onPackFinders(AddPackFindersEvent event) {
        if (event.getPackType() == PackType.SERVER_DATA) {
            event.addPackFinders(ArcanesOublies.id("epicfight_npc"), PackType.SERVER_DATA, Component.literal("Arcanes Oubliés × Epic Fight"),
                    PackSource.BUILT_IN, true, Pack.Position.TOP);
        }
    }
}
