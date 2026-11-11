package com.waylays.eraphones.gear.client;

import com.mojang.blaze3d.platform.NativeImage;
import com.waylays.eraphones.EraPhones;
import com.waylays.eraphones.gear.PhotoNet;
import com.waylays.eraphones.registry.GearRegistry;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.io.ByteArrayOutputStream;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

/**
 * Taking a photograph on the player's side: the hand and everything on the screen hidden for one frame, that frame
 * taken, cut square, brought down and sent to the server, which prints it; the flash seen by the one who took it.
 * And reading a photograph back into its pixels.
 */
@EventBusSubscriber(modid = EraPhones.MODID, value = Dist.CLIENT)
public final class PhotoClient {
    private static boolean pending, hidGui, offhand;
    /** What became of the last picture on this side (for the tests). */
    public static volatile String last = "none";
    private static int frames;
    private static long flashAt;

    private PhotoClient() {}

    public static void take(boolean offhandCamera) {
        if (pending) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        pending = true;
        last = "waiting for a frame";
        offhand = offhandCamera;
        frames = 0;
        hidGui = mc.options.hideGui;
        mc.options.hideGui = true;
    }

    public static void open(ItemStack photo) {
        Minecraft.getInstance().setScreen(new PhotoScreen(photo));
    }

    @SubscribeEvent
    public static void onFrame(RenderFrameEvent.Post event) {
        if (!pending || frames++ < 1) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        pending = false;
        mc.options.hideGui = hidGui;
        flashAt = System.currentTimeMillis();
        try (NativeImage shot = Screenshot.takeScreenshot(mc.getMainRenderTarget())) {
            byte[] data = pack(shot, PhotoNet.SIDE);
            if (data.length > PhotoNet.MAX) {
                data = pack(shot, 96);
            }
            PacketDistributor.sendToServer(new PhotoNet.Taken(offhand, data));
            last = "sent " + data.length + " bytes (" + shot.getWidth() + "x" + shot.getHeight() + ")";
        } catch (Throwable e) {
            last = "failed: " + e;
            EraPhones.LOGGER.warn("The photograph could not be taken", e);
        }
    }

    /** The middle square of {@code shot} brought down to {@code side} pixels (each the mean of its patch), 4 bits a colour, squeezed. */
    public static byte[] pack(NativeImage shot, int side) {
        int w = shot.getWidth(), h = shot.getHeight(), sq = Math.min(w, h);
        int x0 = (w - sq) / 2, y0 = (h - sq) / 2;
        byte[] raw = new byte[side * side * 2];
        for (int j = 0; j < side; j++) {
            for (int i = 0; i < side; i++) {
                int ax = x0 + i * sq / side, bx = Math.max(ax + 1, x0 + (i + 1) * sq / side);
                int ay = y0 + j * sq / side, by = Math.max(ay + 1, y0 + (j + 1) * sq / side);
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
                int o = (j * side + i) * 2;
                raw[o] = (byte) (r / n >> 4);
                raw[o + 1] = (byte) ((g / n >> 4) << 4 | (b / n >> 4));
            }
        }
        Deflater def = new Deflater(9);
        def.setInput(raw);
        def.finish();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(side);
        byte[] buf = new byte[8192];
        while (!def.finished()) {
            out.write(buf, 0, def.deflate(buf));
        }
        def.end();
        return out.toByteArray();
    }

    /** A photograph's pixels as 0xRRGGBB, and its side; null if it cannot be read. */
    static int[] unpack(byte[] data) {
        if (data == null || data.length < 2) {
            return null;
        }
        int side = data[0] & 255;
        Inflater inf = new Inflater();
        inf.setInput(data, 1, data.length - 1);
        byte[] raw = new byte[side * side * 2];
        try {
            int got = 0;
            while (got < raw.length && !inf.finished()) {
                int k = inf.inflate(raw, got, raw.length - got);
                if (k == 0 && inf.needsInput()) {
                    break;
                }
                got += k;
            }
        } catch (DataFormatException e) {
            return null;
        } finally {
            inf.end();
        }
        int[] px = new int[side * side + 1];
        px[0] = side;
        for (int i = 0; i < side * side; i++) {
            int r = (raw[i * 2] & 15) * 17, g = (raw[i * 2 + 1] >> 4 & 15) * 17, b = (raw[i * 2 + 1] & 15) * 17;
            px[i + 1] = r << 16 | g << 8 | b;
        }
        return px;
    }

    @SubscribeEvent
    public static void onLayers(RegisterGuiLayersEvent event) {
        event.registerAboveAll(EraPhones.id("camera_flash"), PhotoClient::flash);
    }

    /** The flash, white and gone in a moment. */
    private static void flash(GuiGraphics g, DeltaTracker delta) {
        float t = (System.currentTimeMillis() - flashAt) / 450f;
        if (t < 0 || t > 1) {
            return;
        }
        int a = (int) (235 * (1 - t) * (1 - t));
        g.fill(0, 0, g.guiWidth(), g.guiHeight(), a << 24 | 0xFFF8F0);
    }

    static long now() {
        Minecraft mc = Minecraft.getInstance();
        return mc.level == null ? 0 : mc.level.getGameTime();
    }

    static long taken(ItemStack photo) {
        return photo.getOrDefault(GearRegistry.PHOTO_TIME.get(), 0L);
    }
}
