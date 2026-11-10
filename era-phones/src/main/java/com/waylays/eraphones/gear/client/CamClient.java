package com.waylays.eraphones.gear.client;

import com.mojang.blaze3d.platform.NativeImage;
import com.waylays.eraphones.EraPhones;
import com.waylays.eraphones.gear.CamNet;
import com.waylays.eraphones.gear.VideoDecode;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.io.ByteArrayOutputStream;
import java.util.zip.Deflater;

/**
 * Filming on the filmer's side: ten times a second the world as drawn (before the hand and anything on the screen),
 * cut to the film's shape, brought down to its size and sent; and the viewfinder over the screen: the blinking red
 * REC, the time filmed, the frame's corners, the date stamp of the time.
 */
@EventBusSubscriber(modid = EraPhones.MODID, value = Dist.CLIENT)
public final class CamClient {
    private static boolean recording;
    private static long started, last;
    private static final Deflater DEFLATER = new Deflater(6);

    private CamClient() {}

    public static void rec(boolean on) {
        recording = on;
        started = System.currentTimeMillis();
        last = 0;
    }

    public static boolean recording() {
        return recording;
    }

    @SubscribeEvent
    public static void onLevel(RenderLevelStageEvent event) {
        if (!recording || event.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL) {
            return;
        }
        long now = System.currentTimeMillis();
        if (now - last < 1000 / VideoDecode.FPS) {
            return;
        }
        last = now;
        Minecraft mc = Minecraft.getInstance();
        try (NativeImage shot = Screenshot.takeScreenshot(mc.getMainRenderTarget())) {
            byte[] data = pack(shot);
            if (data.length <= CamNet.MAX) {
                PacketDistributor.sendToServer(new CamNet.Frame(data));
            }
        } catch (Throwable e) {
            EraPhones.LOGGER.warn("A film frame could not be taken: {}", e.toString());
        }
    }

    /** The middle of the screen in the film's shape (4:3), each pixel the mean of its patch, 4 bits a colour, squeezed. */
    static byte[] pack(NativeImage shot) {
        int W = VideoDecode.W, H = VideoDecode.H;
        int w = shot.getWidth(), h = shot.getHeight();
        int cw = Math.min(w, h * W / H), ch = cw * H / W;
        int x0 = (w - cw) / 2, y0 = (h - ch) / 2;
        byte[] raw = new byte[W * H * 2];
        for (int j = 0; j < H; j++) {
            for (int i = 0; i < W; i++) {
                int ax = x0 + i * cw / W, bx = Math.max(ax + 1, x0 + (i + 1) * cw / W);
                int ay = y0 + j * ch / H, by = Math.max(ay + 1, y0 + (j + 1) * ch / H);
                long r = 0, g = 0, b = 0, n = 0;
                for (int y = ay; y < by; y += 2) {
                    for (int x = ax; x < bx; x += 2) {
                        int c = shot.getPixelRGBA(x, y);
                        r += c & 255;
                        g += c >> 8 & 255;
                        b += c >> 16 & 255;
                        n++;
                    }
                }
                int px = (int) (r / n >> 4) << 8 | (int) (g / n >> 4) << 4 | (int) (b / n >> 4);
                int o = (j * W + i) * 2;
                raw[o] = (byte) (px >> 8);
                raw[o + 1] = (byte) px;
            }
        }
        DEFLATER.reset();
        DEFLATER.setInput(raw);
        DEFLATER.finish();
        ByteArrayOutputStream out = new ByteArrayOutputStream(raw.length / 3);
        byte[] buf = new byte[8192];
        while (!DEFLATER.finished()) {
            out.write(buf, 0, DEFLATER.deflate(buf));
        }
        return out.toByteArray();
    }

    @SubscribeEvent
    public static void onLayers(RegisterGuiLayersEvent event) {
        event.registerAboveAll(EraPhones.id("camcorder"), CamClient::viewfinder);
    }

    /** The viewfinder: what a camcorder of the time showed over its picture. */
    private static void viewfinder(GuiGraphics g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        if (!recording || mc.level == null) {
            return;
        }
        int w = g.guiWidth(), h = g.guiHeight();
        long t = (System.currentTimeMillis() - started) / 1000;
        int m = 14, len = 18, white = 0xE6FFFFFF;
        // The frame's corners
        for (int[] c : new int[][] {{m, m, 1, 1}, {w - m, m, -1, 1}, {m, h - m, 1, -1}, {w - m, h - m, -1, -1}}) {
            g.fill(Math.min(c[0], c[0] + c[2] * len), c[1] - (c[3] < 0 ? 2 : 0), Math.max(c[0], c[0] + c[2] * len), c[1] + (c[3] > 0 ? 2 : 0), white);
            g.fill(c[0] - (c[2] < 0 ? 2 : 0), Math.min(c[1], c[1] + c[3] * len), c[0] + (c[2] > 0 ? 2 : 0), Math.max(c[1], c[1] + c[3] * len), white);
        }
        // REC, blinking, the time filmed
        if (System.currentTimeMillis() / 500 % 2 == 0) {
            g.fill(m + 8, m + 9, m + 15, m + 16, 0xFFE02020);
        }
        g.drawString(mc.font, "REC", m + 19, m + 8, 0xFFFFFFFF, true);
        g.drawString(mc.font, String.format("%d:%02d:%02d", t / 3600, t / 60 % 60, t % 60), w - m - 60, m + 8, 0xFFFFFFFF, true);
        // The date stamp, in the game's own days and time
        long day = mc.level.getDayTime() / 24000 + 1, time = (mc.level.getDayTime() + 6000) % 24000;
        String stamp = Component.translatable("camcorder.eraphones.stamp", day, String.format("%02d:%02d", time / 1000, time % 1000 * 60 / 1000)).getString();
        g.drawString(mc.font, stamp, w - m - 8 - mc.font.width(stamp), h - m - 18, 0xFFFFD040, true);
        // A battery, full
        g.fill(m + 8, h - m - 18, m + 26, h - m - 10, 0xFFFFFFFF);
        g.fill(m + 9, h - m - 17, m + 25, h - m - 11, 0xFF101010);
        g.fill(m + 10, h - m - 16, m + 24, h - m - 12, 0xFF60E060);
        g.fill(m + 26, h - m - 16, m + 28, h - m - 12, 0xFFFFFFFF);
    }
}
