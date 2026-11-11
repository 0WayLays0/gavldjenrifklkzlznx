package com.waylays.eraphones.bike.client;

import com.waylays.eraphones.EraPhones;
import com.waylays.eraphones.registry.GearRegistry;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.ModelEvent;

/** The bicycle's parts, loaded as models of their own, and its renderer. */
@EventBusSubscriber(modid = EraPhones.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class BikeClient {
    private BikeClient() {}

    @SubscribeEvent
    public static void onModels(ModelEvent.RegisterAdditional event) {
        event.register(BikeRenderer.CRANK);
        event.register(BikeRenderer.PEDAL);
        for (var group : java.util.List.of(BikeRenderer.FRAMES, BikeRenderer.STEERS, BikeRenderer.WHEELS)) {
            for (var id : group) {
                event.register(id);
            }
        }
    }

    @SubscribeEvent
    public static void onRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(GearRegistry.BIKE.get(), BikeRenderer::new);
    }
}
