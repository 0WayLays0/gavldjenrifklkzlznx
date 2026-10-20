package com.waylays.arcanes.gametest;

import com.waylays.arcanes.ArcanesOublies;
import net.minecraft.gametest.framework.GameTestServer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

/** Timing and world set-up shared by the addon's GameTests. */
@EventBusSubscriber(modid = ArcanesOublies.MODID)
final class GameTestSetup {
    /**
     * Ticks between a test's platform being placed and the test starting. Entities added in the very tick the
     * platform's chunks are loaded land where the level cannot see them yet (searches miss them, a summon cannot find
     * its summoner), which made tests fail at random.
     */
    static final long TICKS = 5;

    private GameTestSetup() {}

    /**
     * Iron's builds its spell configs (each spell's school, levels, power) when the data packs are sent to players,
     * and nobody ever joins a test server: send them to nobody once, so the spells are set up as in a real world.
     */
    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        if (event.getServer() instanceof GameTestServer server) {
            NeoForge.EVENT_BUS.post(new OnDatapackSyncEvent(server.getPlayerList(), null));
        }
    }
}
