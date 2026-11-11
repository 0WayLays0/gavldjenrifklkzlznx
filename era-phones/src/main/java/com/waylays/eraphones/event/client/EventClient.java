package com.waylays.eraphones.event.client;

import com.waylays.eraphones.EraPhones;
import com.waylays.eraphones.event.EventNet;
import com.waylays.eraphones.event.EventRegistry;
import com.waylays.eraphones.event.StarFragment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/** The star fragment's renderer; and, while carrying it, where the player pushes it (their keys, turned by their eyes). */
@EventBusSubscriber(modid = EraPhones.MODID, value = Dist.CLIENT)
public final class EventClient {
    private static float lastX, lastZ;
    private static int sinceSent;

    private EventClient() {}

    @SubscribeEvent
    public static void onTick(ClientTickEvent.Post event) {
        LocalPlayer p = Minecraft.getInstance().player;
        if (p == null || !(p.getVehicle() instanceof StarFragment)) {
            return;
        }
        float forward = p.input.forwardImpulse, left = p.input.leftImpulse;
        float yaw = p.getYRot() * Mth.DEG_TO_RAD;
        float x = left * Mth.cos(yaw) - forward * Mth.sin(yaw);
        float z = forward * Mth.cos(yaw) + left * Mth.sin(yaw);
        float len = Mth.sqrt(x * x + z * z);
        if (len > 1) {
            x /= len;
            z /= len;
        }
        sinceSent++;
        if (Math.abs(x - lastX) > .02f || Math.abs(z - lastZ) > .02f || sinceSent >= 5) {
            lastX = x;
            lastZ = z;
            sinceSent = 0;
            PacketDistributor.sendToServer(new EventNet.Push(x, z));
        }
    }

    @EventBusSubscriber(modid = EraPhones.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
    public static final class Setup {
        private Setup() {}

        @SubscribeEvent
        public static void onRenderers(EntityRenderersEvent.RegisterRenderers event) {
            event.registerEntityRenderer(EventRegistry.FRAGMENT.get(), FragmentRenderer::new);
            event.registerBlockEntityRenderer(EventRegistry.PINNED_PHOTO_BE.get(), com.waylays.eraphones.gear.client.PinnedPhotoRenderer::new);
        }
    }
}
