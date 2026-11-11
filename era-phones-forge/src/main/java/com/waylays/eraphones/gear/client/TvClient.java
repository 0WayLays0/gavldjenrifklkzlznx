package com.waylays.eraphones.gear.client;

import com.mojang.blaze3d.platform.NativeImage;
import com.waylays.eraphones.EraPhones;
import com.waylays.eraphones.gear.TvNet;
import com.waylays.eraphones.gear.VideoDecode;
import com.waylays.eraphones.gear.block.TvBlockEntity;
import com.waylays.eraphones.registry.GearRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.client.event.EntityRenderersEvent;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * The televisions' screens on the players' side: the last frame each was sent, drawn through an old set's faults
 * (lines across, colours bleeding sideways, a picture that wavers and now and then tears, a band of interference
 * rolling down, the worn tape's ragged bottom edge, snow), twenty times a second while someone looks at it.
 */
@EventBusSubscriber(modid = EraPhones.MODID, value = Dist.CLIENT)
public final class TvClient {
    /** Nothing received yet (or for a while): snow. */
    static final int NONE = -3;
    static final int OW = VideoDecode.W * 2, OH = VideoDecode.H * 2;
    private static final Map<BlockPos, Screen> SCREENS = new HashMap<>();
    private static int made;
    private static float[] vignette;

    private TvClient() {}

    static final class Screen {
        final DynamicTexture texture;
        final ResourceLocation id;
        short[] frame;
        int state = NONE;
        long received, seen, drawn;
        TvHiss hiss;
        /** A film being read: how far (percent); one that cannot be: its format. */
        int progress;
        String why = "";
        int seed = (int) System.nanoTime();

        Screen() {
            texture = new DynamicTexture(OW, OH, false);
            id = EraPhones.id("tv_screen_" + made++);
            Minecraft.getInstance().getTextureManager().register(id, texture);
        }
    }

    public static void frame(TvNet.Frame p) {
        Screen s = SCREENS.computeIfAbsent(p.pos(), k -> new Screen());
        s.received = System.currentTimeMillis();
        if (p.index() >= 0) {
            try {
                s.frame = VideoDecode.unpack(p.data());
                s.state = p.index();
            } catch (Exception e) {
                s.state = NONE;
            }
        } else {
            s.state = p.index();
            if (p.index() == TvNet.LOADING && p.data().length > 0) {
                s.progress = p.data()[0] & 0xFF;
            } else if (p.index() == TvNet.FAILED) {
                s.why = new String(p.data(), java.nio.charset.StandardCharsets.UTF_8);
            }
        }
    }

    /** What the screen at {@code pos} should say over its picture: nothing, how far a film is read, or why it cannot be. */
    static net.minecraft.network.chat.Component caption(BlockPos pos) {
        Screen s = SCREENS.get(pos);
        if (s == null) {
            return null;
        }
        if (s.state == TvNet.LOADING) {
            return net.minecraft.network.chat.Component.translatable("tv.eraphones.loading", s.progress);
        }
        if (s.state == TvNet.FAILED) {
            return net.minecraft.network.chat.Component.translatable("tv.eraphones.unreadable", s.why);
        }
        return null;
    }

    /** The texture of the screen at {@code pos}, brought up to date; called as the set is drawn. */
    static ResourceLocation screen(TvBlockEntity tv) {
        Screen s = SCREENS.computeIfAbsent(tv.getBlockPos(), k -> new Screen());
        long now = System.currentTimeMillis();
        s.seen = now;
        // A recorder that stopped sending (taken away, out of reach): snow again
        if (s.state != NONE && now - s.received > 5000) {
            s.state = NONE;
        }
        if (now - s.drawn >= 50) {
            s.drawn = now;
            paint(s, now);
            s.texture.upload();
        }
        boolean snow = s.state == NONE || s.state == TvNet.LOADING || s.state == TvNet.FAILED;
        if (snow && (s.hiss == null || s.hiss.isStopped())) {
            s.hiss = new TvHiss(tv, s);
            Minecraft.getInstance().getSoundManager().play(s.hiss);
        }
        return s.id;
    }

    private static float[] vignette() {
        if (vignette == null) {
            vignette = new float[OW * OH];
            for (int y = 0; y < OH; y++) {
                for (int x = 0; x < OW; x++) {
                    // The tube's rounded corners, darker towards its edges
                    double nx = (x + .5) / OW * 2 - 1, ny = (y + .5) / OH * 2 - 1;
                    double r = Math.pow(Math.pow(Math.abs(nx), 5) + Math.pow(Math.abs(ny), 5), .2);
                    double v = r > 1 ? 0 : Math.max(0, 1 - Math.pow(r, 6) * .9) * (1 - .25 * (nx * nx + ny * ny) / 2);
                    vignette[y * OW + x] = (float) Math.min(1, Math.max(0, v * 1.08));
                }
            }
        }
        return vignette;
    }

    private static int rnd(int[] st) {
        int x = st[0];
        x ^= x << 13;
        x ^= x >>> 17;
        x ^= x << 5;
        st[0] = x;
        return x;
    }

    static void paint(Screen s, long now) {
        NativeImage img = s.texture.getPixels();
        if (img == null) {
            return;
        }
        float[] vig = vignette();
        float t = (now % 100000) / 1000f;
        int[] st = {s.seed ^ (int) (now / 50) * 0x9E3779B1 | 1};
        boolean picture = s.state >= 0 && s.frame != null;
        boolean blue = s.state == TvNet.BLUE;
        // Now and then a few lines torn sideways
        int tearAt = (rnd(st) & 63) < 6 ? (rnd(st) >>> 1) % OH : -100;
        int tearLen = 3 + (rnd(st) >>> 1) % 9, tearBy = ((rnd(st) >>> 1) % 9) - 4;
        float hum = (t * .18f) % 1.3f - .15f;
        int W = VideoDecode.W, H = VideoDecode.H;
        for (int y = 0; y < OH; y++) {
            float fy = y / (float) OH;
            int shift = (int) Math.round(Math.sin(t * 2.7 + y * .19) * .7 + Math.sin(t * 11 + y * .05) * .4);
            if (y >= tearAt && y < tearAt + tearLen) {
                shift += tearBy * 2;
            }
            boolean tracking = picture && y >= OH - 7;
            if (tracking) {
                shift += ((rnd(st) >>> 1) % 13) - 6;
            }
            float band = 1 + .1f * (float) Math.exp(-Math.pow((fy - hum) / .07, 2));
            float line = (y & 1) == 1 ? .62f : 1f;
            for (int x = 0; x < OW; x++) {
                int r, g, b;
                int noise = ((rnd(st) >>> 1) % 33) - 16;
                if (picture) {
                    int sx = Math.max(0, Math.min(W - 1, (x + shift) / 2)), sy = Math.min(H - 1, y / 2);
                    int row = sy * W;
                    // Colours bleeding: red a little right, blue a little left
                    int cr = s.frame[row + Math.min(W - 1, sx + 1)], cg = s.frame[row + sx], cb = s.frame[row + Math.max(0, sx - 1)];
                    r = ((cr >> 8) & 15) * 17;
                    g = ((cg >> 4) & 15) * 17;
                    b = (cb & 15) * 17;
                    // A set past its best: warm, a little washed out, contrast pushed
                    int lum = (r * 3 + g * 6 + b) / 10;
                    r = (int) ((r * .82f + lum * .18f - 128) * 1.12f + 136);
                    g = (int) ((g * .82f + lum * .18f - 128) * 1.08f + 128);
                    b = (int) ((b * .82f + lum * .18f - 128) * 1.02f + 116);
                    if (tracking) {
                        int v = (rnd(st) & 255);
                        r = (r + v) / 2;
                        g = (g + v) / 2;
                        b = (b + v) / 2;
                    }
                    r += noise;
                    g += noise;
                    b += noise;
                } else if (blue) {
                    r = 24 + noise / 3;
                    g = 46 + noise / 3;
                    b = 196 + noise / 2;
                } else {
                    // Snow: grains two pixels wide, bands sweeping through
                    int v = (rnd(st) >>> 1) % 256;
                    if ((x & 1) == 1) {
                        v = (v + 128) / 2;
                    }
                    float sweep = .75f + .25f * (float) Math.sin(fy * 9 - t * 6);
                    r = g = b = (int) (v * sweep);
                }
                float k = vig[y * OW + x] * line * band;
                r = Math.max(0, Math.min(255, (int) (r * k)));
                g = Math.max(0, Math.min(255, (int) (g * k)));
                b = Math.max(0, Math.min(255, (int) (b * k)));
                img.setPixelRGBA(x, y, 0xFF000000 | b << 16 | g << 8 | r);
            }
        }
    }

    @SubscribeEvent
    public static void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        long now = System.currentTimeMillis();
        Iterator<Map.Entry<BlockPos, Screen>> it = SCREENS.entrySet().iterator();
        while (it.hasNext()) {
            Screen s = it.next().getValue();
            if (now - s.seen > 15000 && now - s.received > 15000) {
                release(s);
                it.remove();
            }
        }
    }

    private static void release(Screen s) {
        if (s.hiss != null) {
            s.hiss.end();
        }
        Minecraft.getInstance().getTextureManager().release(s.id);
    }

    @SubscribeEvent
    public static void onLeave(ClientPlayerNetworkEvent.LoggingOut event) {
        for (Screen s : SCREENS.values()) {
            release(s);
        }
        SCREENS.clear();
    }

    @SubscribeEvent
    public static void onRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(GearRegistry.TV_BE.get(), c -> new TvRenderer());
    }

    /** The hiss of an empty channel, from a set that is on and gets nothing. */
    static final class TvHiss extends AbstractTickableSoundInstance {
        private final TvBlockEntity tv;
        private final Screen screen;

        TvHiss(TvBlockEntity tv, Screen screen) {
            super(GearRegistry.RADIO_STATIC.get(), SoundSource.RECORDS, RandomSource.create());
            this.tv = tv;
            this.screen = screen;
            this.looping = true;
            this.delay = 0;
            this.x = tv.getBlockPos().getX() + .5;
            this.y = tv.getBlockPos().getY() + .5;
            this.z = tv.getBlockPos().getZ() + .5;
            this.attenuation = SoundInstance.Attenuation.LINEAR;
            this.volume = .3f;
        }

        void end() {
            stop();
        }

        @Override
        public void tick() {
            boolean snow = screen.state == NONE || screen.state == TvNet.LOADING || screen.state == TvNet.FAILED;
            if (tv.isRemoved() || !tv.on() || !snow || System.currentTimeMillis() - screen.seen > 1500) {
                stop();
            }
        }
    }
}
