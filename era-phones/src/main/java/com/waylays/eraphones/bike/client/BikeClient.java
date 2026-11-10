package com.waylays.eraphones.bike.client;

import com.waylays.eraphones.EraPhones;
import com.waylays.eraphones.registry.GearRegistry;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;

/** The bicycle's parts, loaded as models of their own, and its renderer. */
@EventBusSubscriber(modid = EraPhones.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class BikeClient {
    private BikeClient() {}

    @SubscribeEvent
    public static void onModels(ModelEvent.RegisterAdditional event) {
        for (var id : new net.minecraft.client.resources.model.ModelResourceLocation[] {BikeRenderer.FRAME, BikeRenderer.WHEEL, BikeRenderer.CRANK,
                BikeRenderer.PEDAL, BikeRenderer.STEER}) {
            event.register(id);
        }
    }

    @SubscribeEvent
    public static void onRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(GearRegistry.BIKE.get(), BikeRenderer::new);
    }
}
