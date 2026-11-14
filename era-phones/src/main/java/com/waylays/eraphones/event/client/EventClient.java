package com.waylays.eraphones.event.client;

import com.waylays.eraphones.EraPhones;
import com.waylays.eraphones.event.EventRegistry;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/** The renderer of the pinned photographs. */
@EventBusSubscriber(modid = EraPhones.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class EventClient {
    private EventClient() {}

    @SubscribeEvent
    public static void onRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(EventRegistry.PINNED_PHOTO_BE.get(), com.waylays.eraphones.gear.client.PinnedPhotoRenderer::new);
    }
}
