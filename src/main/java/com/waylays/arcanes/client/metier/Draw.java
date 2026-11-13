package com.waylays.arcanes.client.metier;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.math.Axis;
import com.waylays.arcanes.ArcanesOublies;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * Drawing for the trades' screens: the painted scenes and pieces (tools/metier_art.py), smooth sprites (glow, disc,
 * ring, flame, star) tinted and drawn plain or additive, straight thick lines at any angle, fire and spark particles,
 * key caps, panels, scaled text and items, colours.
 */
final class Draw {
    static final ResourceLocation SOFT = tex("soft"), DISC = tex("disc"), RING = tex("ring"), FLAME = tex("flame"), STAR = tex("star");
    private static final Set<ResourceLocation> SMOOTHED = new HashSet<>();

    private Draw() {}

    static ResourceLocation tex(String name) {
        return ArcanesOublies.id("textures/gui/metier/" + name + ".png");
    }

    static int argb(int a, int rgb) {
        return Mth.clamp(a, 0, 255) << 24 | (rgb & 0xFFFFFF);
    }

    static int lerp(int c0, int c1, float t) {
        t = Mth.clamp(t, 0, 1);
        int a = (int) Mth.lerp(t, c0 >>> 24, c1 >>> 24), r = (int) Mth.lerp(t, c0 >> 16 & 255, c1 >> 16 & 255);
        int g = (int) Mth.lerp(t, c0 >> 8 & 255, c1 >> 8 & 255), b = (int) Mth.lerp(t, c0 & 255, c1 & 255);
        return a << 24 | r << 16 | g << 8 | b;
    }

    /** The colour of metal at {@code t} (0 cold grey, 0.5 cherry red, 0.75 orange, 1 white-yellow). */
    static int heat(float t) {
        int[] stops = {0xFF3A3A40, 0xFF5A2020, 0xFFB0201A, 0xFFE0561A, 0xFFFFA030, 0xFFFFF0B0};
        float x = Mth.clamp(t, 0, 1) * (stops.length - 1);
        int i = Math.min(stops.length - 2, (int) x);
        return lerp(stops[i], stops[i + 1], x - i);
    }

    static int hsv(float h, float s, float v) {
        return 0xFF000000 | Mth.hsvToRgb(((h % 1) + 1) % 1, s, v);
    }

    // ---------------------------------------------------------------- textures

    private static void smooth(ResourceLocation tex) {
        if (SMOOTHED.add(tex)) {
            Minecraft.getInstance().getTextureManager().getTexture(tex).setFilter(true, false);
        }
    }

    /** A whole picture stretched over x0..x1, y0..y1 (tinted by {@code argb}). */
    static void picture(GuiGraphics g, ResourceLocation tex, float x0, float y0, float x1, float y1, int texW, int texH, int argb) {
        smooth(tex);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        g.setColor((argb >> 16 & 255) / 255f, (argb >> 8 & 255) / 255f, (argb & 255) / 255f, (argb >>> 24) / 255f);
        g.pose().pushPose();
        g.pose().translate(x0, y0, 0);
        g.pose().scale((x1 - x0) / texW, (y1 - y0) / texH, 1);
        g.blit(tex, 0, 0, 0, 0, texW, texH, texW, texH);
        g.pose().popPose();
        g.setColor(1, 1, 1, 1);
    }

    /** A sprite centred on cx, cy, w by h, tinted; {@code additive}: it lights what is under it. */
    static void sprite(GuiGraphics g, ResourceLocation tex, float cx, float cy, float w, float h, int argb, boolean additive, int texW, int texH) {
        smooth(tex);
        RenderSystem.enableBlend();
        if (additive) {
            RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        } else {
            RenderSystem.defaultBlendFunc();
        }
        g.setColor((argb >> 16 & 255) / 255f, (argb >> 8 & 255) / 255f, (argb & 255) / 255f, (argb >>> 24) / 255f);
        g.pose().pushPose();
        g.pose().translate(cx - w / 2, cy - h / 2, 0);
        g.pose().scale(w / texW, h / texH, 1);
        g.blit(tex, 0, 0, 0, 0, texW, texH, texW, texH);
        g.pose().popPose();
        g.setColor(1, 1, 1, 1);
        RenderSystem.defaultBlendFunc();
    }

    static void glow(GuiGraphics g, float cx, float cy, float r, int argb) {
        sprite(g, SOFT, cx, cy, r * 2, r * 2, argb, true, 64, 64);
    }

    static void shadow(GuiGraphics g, float cx, float cy, float w, float h, int alpha) {
        sprite(g, SOFT, cx, cy, w, h, argb(alpha, 0), false, 64, 64);
    }

    static void disc(GuiGraphics g, float cx, float cy, float r, int argb) {
        sprite(g, DISC, cx, cy, r * 2.05f, r * 2.05f, argb, false, 64, 64);
    }

    /** A ring of outer radius {@code r} (its band about a tenth of it). */
    static void ring(GuiGraphics g, float cx, float cy, float r, int argb, boolean additive) {
        sprite(g, RING, cx, cy, r * 2.03f, r * 2.03f, argb, additive, 128, 128);
    }

    static void star(GuiGraphics g, float cx, float cy, float r, int argb) {
        sprite(g, STAR, cx, cy, r * 2, r * 2, argb, true, 64, 64);
    }

    /** A straight band from x0, y0 to x1, y1, {@code thick} wide. */
    static void line(GuiGraphics g, float x0, float y0, float x1, float y1, float thick, int color) {
        float dx = x1 - x0, dy = y1 - y0, len = (float) Math.sqrt(dx * dx + dy * dy);
        if (len < .01f) {
            return;
        }
        g.pose().pushPose();
        g.pose().translate(x0, y0, 0);
        g.pose().mulPose(Axis.ZP.rotation((float) Math.atan2(dy, dx)));
        g.pose().scale(len / 64, thick / 64, 1);
        g.fill(0, -32, 64, 32, color);
        g.pose().popPose();
    }

    /** A band with round ends. */
    static void stroke(GuiGraphics g, float x0, float y0, float x1, float y1, float thick, int color) {
        line(g, x0, y0, x1, y1, thick, color);
        disc(g, x0, y0, thick / 2, color);
        disc(g, x1, y1, thick / 2, color);
    }

    static void rect(GuiGraphics g, float x0, float y0, float x1, float y1, int color) {
        g.pose().pushPose();
        g.pose().translate(x0, y0, 0);
        g.pose().scale((x1 - x0) / 64, (y1 - y0) / 64, 1);
        g.fill(0, 0, 64, 64, color);
        g.pose().popPose();
    }

    static void vgrad(GuiGraphics g, float x0, float y0, float x1, float y1, int top, int bottom) {
        g.pose().pushPose();
        g.pose().translate(x0, y0, 0);
        g.pose().scale((x1 - x0) / 64, (y1 - y0) / 64, 1);
        g.fillGradient(0, 0, 64, 64, top, bottom);
        g.pose().popPose();
    }

    /** A rounded bar (a gauge's track or fill). */
    static void pill(GuiGraphics g, float x0, float y0, float x1, float y1, int color) {
        float r = (y1 - y0) / 2;
        if (x1 - x0 < r * 2) {
            disc(g, (x0 + x1) / 2, (y0 + y1) / 2, Math.max(.5f, (x1 - x0) / 2), color);
            return;
        }
        rect(g, x0 + r, y0, x1 - r, y1, color);
        disc(g, x0 + r, y0 + r, r, color);
        disc(g, x1 - r, y0 + r, r, color);
    }

    static void frame(GuiGraphics g, int x0, int y0, int x1, int y1, int fill, int border) {
        g.fill(x0, y0, x1, y1, fill);
        g.renderOutline(x0, y0, x1 - x0, y1 - y0, border);
    }

    /** A panel: soft shadow, dark body lit from the top, a fine light edge. */
    static void panel(GuiGraphics g, float x0, float y0, float x1, float y1, int theme) {
        shadow(g, (x0 + x1) / 2, (y0 + y1) / 2 + 4, (x1 - x0) * 1.12f, (y1 - y0) * 1.25f, 200);
        vgrad(g, x0, y0, x1, y1, argb(245, lerp(theme, 0xFF0C0A0A, .72f)), argb(250, lerp(theme, 0xFF060505, .88f)));
        int edge = lerp(theme, 0xFFFFF0D8, .45f);
        rect(g, x0, y0, x1, y0 + 1, edge);
        rect(g, x0, y1 - 1, x1, y1, argb(160, lerp(theme, 0, .5f)));
        rect(g, x0, y0, x0 + 1, y1, argb(170, edge));
        rect(g, x1 - 1, y0, x1, y1, argb(170, edge));
    }

    static void text(GuiGraphics g, Component c, float x, float y, int color, float scale) {
        Font font = Minecraft.getInstance().font;
        g.pose().pushPose();
        g.pose().translate(x, y, 0);
        g.pose().scale(scale, scale, 1);
        g.drawString(font, c, 0, 0, color, true);
        g.pose().popPose();
    }

    static void centered(GuiGraphics g, Component c, float cx, float y, int color, float scale) {
        Font font = Minecraft.getInstance().font;
        text(g, c, cx - font.width(c) * scale / 2, y, color, scale);
    }

    /** Text with a dark halo round it, readable over any scene. */
    static void title(GuiGraphics g, Component c, float cx, float y, int color, float scale) {
        Font font = Minecraft.getInstance().font;
        float w = font.width(c) * scale;
        shadow(g, cx, y + 4 * scale, w + 40 * scale, 22 * scale, 150);
        centered(g, c, cx, y, color, scale);
    }

    static float width(Component c, float scale) {
        return Minecraft.getInstance().font.width(c) * scale;
    }

    /** A key cap with its key ({@code key}) and what it does ({@code label}); returns the width taken. */
    static float keycap(GuiGraphics g, Component key, Component label, float x, float y, int alpha) {
        float kw = Math.max(14, width(key, .8f) + 8);
        rect(g, x, y + 1, x + kw, y + 13, argb(alpha, 0xFF1A1714));
        rect(g, x, y, x + kw, y + 11, argb(alpha, 0xFFE8E0D0));
        rect(g, x + 1, y + 1, x + kw - 1, y + 10, argb(alpha, 0xFFFFFAF0));
        text(g, key, x + (kw - width(key, .8f)) / 2, y + 2, argb(alpha, 0xFF2A2420), .8f);
        g.pose().pushPose();
        text(g, label, x + kw + 4, y + 2, argb(alpha, 0xFFF4EEE0), .85f);
        g.pose().popPose();
        return kw + 4 + width(label, .85f) + 12;
    }

    static void item(GuiGraphics g, ItemStack stack, float x, float y, float scale) {
        if (stack.isEmpty()) {
            return;
        }
        g.pose().pushPose();
        g.pose().translate(x, y, 0);
        g.pose().scale(scale, scale, 1);
        g.renderItem(stack, 0, 0);
        g.pose().popPose();
    }

    /** An item centred on cx, cy, {@code size} across. */
    static void itemAt(GuiGraphics g, ItemStack stack, float cx, float cy, float size) {
        item(g, stack, cx - size / 2, cy - size / 2, size / 16);
    }

    /** An item drawn turned by {@code degrees} round its centre. */
    static void itemRotated(GuiGraphics g, ItemStack stack, float cx, float cy, float scale, float degrees) {
        if (stack.isEmpty()) {
            return;
        }
        g.pose().pushPose();
        g.pose().translate(cx, cy, 120);
        g.pose().mulPose(Axis.ZP.rotationDegrees(degrees));
        g.pose().scale(scale, scale, 1);
        g.renderItem(stack, -8, -8);
        g.pose().popPose();
    }

    static float ease(float t) {
        t = Mth.clamp(t, 0, 1);
        return 1 - (1 - t) * (1 - t) * (1 - t);
    }

    /** Overshoots a little before settling (pop-ins). */
    static float back(float t) {
        t = Mth.clamp(t, 0, 1);
        float c = 1.9f;
        return 1 + (c + 1) * (float) Math.pow(t - 1, 3) + c * (float) Math.pow(t - 1, 2);
    }

    // ---------------------------------------------------------------- particles

    /**
     * Fire: tongues of flame rising from a hearth, white-yellow at the root, orange, red, then a wisp of smoke; drawn
     * additively over a soft glow. {@code cool}: a gas flame (blue at the root).
     */
    static final class Fire {
        private record P(float x, float y, float vx, float vy, float born, float life, float size, float wobble) {}

        private final List<P> ps = new ArrayList<>();
        private final Random random;
        private final boolean cool;
        private float debt;

        Fire(Random random, boolean cool) {
            this.random = random;
            this.cool = cool;
        }

        /** Feeds the fire along x0..x1 at y, {@code power} 0 to 1 (how many and how tall). */
        void update(float time, float dt, float x0, float x1, float y, float power) {
            debt += dt * (8 + 70 * power);
            while (debt >= 1) {
                debt--;
                float x = x0 + random.nextFloat() * (x1 - x0);
                float mid = 1 - Math.abs((x - (x0 + x1) / 2) / ((x1 - x0) / 2 + .01f));
                ps.add(new P(x, y, (random.nextFloat() - .5f) * 8, -(18 + 40 * power) * (.6f + .6f * mid), time,
                        .35f + random.nextFloat() * .35f + .35f * power * mid, 7 + random.nextFloat() * 5 + 8 * power * mid, random.nextFloat() * 6));
            }
            ps.removeIf(p -> time - p.born() > p.life());
        }

        void draw(GuiGraphics g, float time, float x0, float x1, float y, float power) {
            float cx = (x0 + x1) / 2;
            glow(g, cx, y - 6 - 10 * power, (x1 - x0) * .8f + 20 * power, argb((int) (60 + 90 * power), 0xFFFF7A20));
            for (P p : ps) {
                float t = (time - p.born()) / p.life();
                if (t > .78f) {
                    // Smoke
                    float st = (t - .78f) / .22f;
                    float x = p.x() + p.vx() * (time - p.born()) + Mth.sin(time * 3 + p.wobble()) * 3;
                    float yy = p.y() + p.vy() * (time - p.born());
                    sprite(g, SOFT, x, yy, p.size() * (1.2f + st), p.size() * (1.2f + st), argb((int) (40 * (1 - st)), 0xFF201A18), false, 64, 64);
                }
            }
            for (P p : ps) {
                float age = time - p.born(), t = age / p.life();
                if (t > .8f) {
                    continue;
                }
                float x = p.x() + p.vx() * age + Mth.sin(time * 9 + p.wobble()) * 1.5f * t;
                float yy = p.y() + p.vy() * age;
                float s = p.size() * (t < .2f ? .6f + 2 * t : 1 - (t - .2f) * .9f);
                int c = flameColor(t);
                int a = (int) (230 * (1 - t / .8f));
                sprite(g, FLAME, x, yy - s * .4f, s, s * 1.5f, argb(a, c), true, 64, 96);
            }
        }

        private int flameColor(float t) {
            if (cool && t < .18f) {
                return lerp(0xFF6AA0FF, 0xFFFFE6A0, t / .18f);
            }
            if (t < .2f) {
                return lerp(0xFFFFF4C8, 0xFFFFC050, t / .2f);
            }
            if (t < .5f) {
                return lerp(0xFFFFC050, 0xFFFF6A18, (t - .2f) / .3f);
            }
            return lerp(0xFFFF6A18, 0xFF901808, (t - .5f) / .3f);
        }
    }

    /** Sparks, chips and dust: thrown, falling, fading. */
    static final class Bits {
        private record B(float x, float y, float vx, float vy, float born, float life, float size, int color, float gravity, boolean light) {}

        private final List<B> bs = new ArrayList<>();

        void add(float time, float x, float y, float vx, float vy, float life, float size, int color, float gravity, boolean light) {
            bs.add(new B(x, y, vx, vy, time, life, size, color, gravity, light));
        }

        /** A burst of {@code n} glowing sparks from x, y, upwards. */
        void sparks(Random r, float time, float x, float y, int n, float speed, int color) {
            for (int i = 0; i < n; i++) {
                float a = -Mth.HALF_PI + (r.nextFloat() - .5f) * 2.4f, v = speed * (.4f + r.nextFloat() * .8f);
                add(time, x, y, Mth.cos(a) * v, Mth.sin(a) * v, .35f + r.nextFloat() * .45f, 1.6f + r.nextFloat() * 1.4f, color, 260, true);
            }
        }

        /** {@code n} chips of {@code color} and a puff of dust. */
        void chips(Random r, float time, float x, float y, int n, int color) {
            for (int i = 0; i < n; i++) {
                float a = -Mth.HALF_PI + (r.nextFloat() - .5f) * 2.8f, v = 50 + r.nextFloat() * 90;
                add(time, x, y, Mth.cos(a) * v, Mth.sin(a) * v, .6f + r.nextFloat() * .5f, 2 + r.nextFloat() * 2.5f, color, 330, false);
            }
            for (int i = 0; i < 4; i++) {
                add(time, x + (r.nextFloat() - .5f) * 10, y, (r.nextFloat() - .5f) * 30, -10 - r.nextFloat() * 15, .9f, 12 + r.nextFloat() * 8,
                        0xFF8A8278, -10, false);
            }
        }

        void draw(GuiGraphics g, float time) {
            bs.removeIf(b -> time - b.born() > b.life());
            for (B b : bs) {
                float age = time - b.born(), t = age / b.life();
                float x = b.x() + b.vx() * age, y = b.y() + b.vy() * age + .5f * b.gravity() * age * age;
                if (b.light()) {
                    float vy = b.vy() + b.gravity() * age;
                    int a = (int) (255 * (1 - t));
                    line(g, x, y, x - b.vx() * .025f, y - vy * .025f, b.size() * .7f, argb(a, lerp(0xFFFFFFFF, b.color(), t)));
                    glow(g, x, y, b.size() * 2.5f, argb(a / 2, b.color()));
                } else if (b.gravity() < 0) {
                    sprite(g, SOFT, x, y, b.size() * (1 + t), b.size() * (1 + t), argb((int) (90 * (1 - t)), b.color()), false, 64, 64);
                } else {
                    float s = b.size();
                    g.pose().pushPose();
                    g.pose().translate(x, y, 0);
                    g.pose().mulPose(Axis.ZP.rotation(age * 8 + b.vx()));
                    rect(g, -s / 2, -s / 2, s / 2, s / 2, argb((int) (255 * Math.min(1, (1 - t) * 3)), b.color()));
                    rect(g, -s / 2, -s / 2, s / 2, -s / 2 + Math.max(.6f, s * .3f), argb((int) (120 * (1 - t)), 0xFFFFFFFF));
                    g.pose().popPose();
                }
            }
        }
    }
}
