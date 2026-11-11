package com.waylays.eraphones.size.client;

import com.waylays.eraphones.EraPhones;
import com.waylays.eraphones.size.Sizes;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

/** A player drawn at their height (1.20.1 has no size attribute to draw it by itself). */
@EventBusSubscriber(modid = EraPhones.MODID, value = Dist.CLIENT)
public final class SizeClient {
    private SizeClient() {}

    @SubscribeEvent
    public static void onRenderPre(RenderPlayerEvent.Pre event) {
        float k = Sizes.scale(event.getEntity());
        event.getPoseStack().pushPose();
        if (k != 1f) {
            event.getPoseStack().scale(k, k, k);
        }
    }

    @SubscribeEvent
    public static void onRenderPost(RenderPlayerEvent.Post event) {
        event.getPoseStack().popPose();
    }
}
