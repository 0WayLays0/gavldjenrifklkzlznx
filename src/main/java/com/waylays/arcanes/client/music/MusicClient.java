package com.waylays.arcanes.client.music;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.math.Axis;
import com.waylays.arcanes.ArcanesOublies;
import net.minecraft.Util;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import org.lwjgl.glfw.GLFW;

/**
 * The music player's place in the game: the key that opens it (M by default), its ticks, the note that slides in at the
 * top right of the screen when a new track starts, and the music stopping on leaving a world.
 */
@EventBusSubscriber(modid = ArcanesOublies.MODID, value = Dist.CLIENT)
public final class MusicClient {
    public static final KeyMapping OPEN = new KeyMapping("key.arcanes_oublies.music", KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_M,
            "key.categories.arcanes_oublies");
    private static final ResourceLocation RECORD = ArcanesOublies.id("textures/gui/music/record.png");
    private static final long TOAST_MS = 4500;

    private MusicClient() {}

    @SubscribeEvent
    public static void onRegisterKeys(RegisterKeyMappingsEvent event) {
        event.register(OPEN);
    }

    @SubscribeEvent
    public static void onRegisterLayers(RegisterGuiLayersEvent event) {
        event.registerAboveAll(ArcanesOublies.id("music_now_playing"), MusicClient::nowPlaying);
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        while (OPEN.consumeClick()) {
            if (mc.screen == null && mc.player != null) {
                mc.setScreen(new MusicScreen());
            }
        }
        MusicPlayer.tick(mc);
        RemoteMusic.tick(mc);
    }

    @SubscribeEvent
    public static void onLeave(ClientPlayerNetworkEvent.LoggingOut event) {
        MusicPlayer.onLeave();
        RemoteMusic.onLeave();
    }

    /** A note sliding in at the top right when a track starts: its record turning, its title. */
    private static void nowPlaying(GuiGraphics g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        MusicLibrary.Track t = MusicPlayer.current();
        if (t == null || mc.options.hideGui || mc.screen instanceof MusicScreen) {
            return;
        }
        long age = System.currentTimeMillis() - MusicPlayer.changedAt();
        if (age < 0 || age > TOAST_MS) {
            return;
        }
        float in = Math.min(1, age / 300f), out = Math.min(1, (TOAST_MS - age) / 400f);
        float k = Math.min(in, out);
        Font font = mc.font;
        String title = t.title.length() > 40 ? t.title.substring(0, 39) + "…" : t.title;
        int w = Math.max(90, font.width(title) + 34);
        int x = g.guiWidth() - (int) (w * (1 - (1 - k) * (1 - k))) - 4, y = 4;
        RenderSystem.enableBlend();
        g.fill(x - 1, y - 1, x + w + 1, y + 23, 0xFFDEB25C);
        g.fillGradient(x, y, x + w, y + 22, 0xF0241642, 0xF00E0A22);
        g.pose().pushPose();
        g.pose().translate(x + 11, y + 11, 0);
        g.pose().mulPose(Axis.ZP.rotationDegrees((Util.getMillis() / 6f) % 360));
        g.blit(RECORD, -9, -9, 18, 18, 0, 0, 128, 128, 128, 128);
        g.pose().popPose();
        g.drawString(font, title, x + 24, y + 3, 0xFFFFE6A0, true);
        String sub = t.artist != null ? t.artist : "♪";
        g.pose().pushPose();
        g.pose().translate(x + 24, y + 13, 0);
        g.pose().scale(.75f, .75f, 1);
        g.drawString(font, sub, 0, 0, 0xFF9A8FB8, false);
        g.pose().popPose();
    }
}
