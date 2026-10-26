package com.waylays.arcanes.client;

import com.waylays.arcanes.ArcanesOublies;
import com.waylays.arcanes.entity.cosmos.TimeStopBubble;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * What stopped time looks like from inside: when time stops round the camera the world flashes into its negative for
 * a blink, then stays in the warm sepia of an old photograph (the TVA's own colours) until time starts again.
 */
@EventBusSubscriber(modid = ArcanesOublies.MODID, value = Dist.CLIENT)
public final class TimeStopVision {
    public static final ResourceLocation SEPIA = ArcanesOublies.id("shaders/post/time_stop.json");
    public static final ResourceLocation FLASH = ArcanesOublies.id("shaders/post/time_stop_flash.json");
    private static final int FLASH_TICKS = 4;
    /** Whether the effect on screen is ours (so another one, a spectator's view, is never shut down). */
    private static boolean ours;

    private TimeStopVision() {}

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        Entity camera = mc.getCameraEntity();
        ResourceLocation want = null;
        if (mc.level != null && camera != null) {
            for (TimeStopBubble bubble : mc.level.getEntitiesOfClass(TimeStopBubble.class, camera.getBoundingBox().inflate(16))) {
                if (bubble.tickCount < bubble.getLife() && bubble.inside(camera)) {
                    want = bubble.tickCount < FLASH_TICKS ? FLASH : SEPIA;
                }
            }
        }
        PostChain current = mc.gameRenderer.currentEffect();
        String name = current == null ? null : current.getName();
        try {
            if (want != null && !want.toString().equals(name)) {
                mc.gameRenderer.loadEffect(want);
                ours = true;
            } else if (want == null && ours) {
                if (name != null && (name.equals(SEPIA.toString()) || name.equals(FLASH.toString()))) {
                    mc.gameRenderer.shutdownEffect();
                }
                ours = false;
            }
        } catch (RuntimeException e) {
            // A screen effect that cannot load is not worth a crash
            ours = false;
        }
    }
}
