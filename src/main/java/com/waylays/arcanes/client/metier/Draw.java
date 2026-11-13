package com.waylays.arcanes.client.metier;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

/** Small drawing helpers for the trades' screens: discs, rings, thick lines, scaled text and items, colours. */
final class Draw {
    private Draw() {}

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

    static void disc(GuiGraphics g, float cx, float cy, float r, int color) {
        int r0 = (int) Math.ceil(r);
        for (int dy = -r0; dy <= r0; dy++) {
            float w = (float) Math.sqrt(Math.max(0, r * r - dy * dy));
            if (w > 0) {
                g.fill(Math.round(cx - w), Math.round(cy + dy), Math.round(cx + w), Math.round(cy + dy + 1), color);
            }
        }
    }

    static void ring(GuiGraphics g, float cx, float cy, float r, float thick, int color) {
        int n = Math.max(16, (int) (r * 2.2f));
        for (int i = 0; i < n; i++) {
            float a0 = i * Mth.TWO_PI / n, a1 = (i + 1) * Mth.TWO_PI / n;
            line(g, cx + Mth.cos(a0) * r, cy + Mth.sin(a0) * r, cx + Mth.cos(a1) * r, cy + Mth.sin(a1) * r, thick, color);
        }
    }

    static void line(GuiGraphics g, float x0, float y0, float x1, float y1, float thick, int color) {
        float dx = x1 - x0, dy = y1 - y0;
        int steps = Math.max(1, (int) (Math.max(Math.abs(dx), Math.abs(dy))));
        int h = Math.max(1, Math.round(thick));
        for (int i = 0; i <= steps; i++) {
            float t = i / (float) steps;
            int x = Math.round(x0 + dx * t - h / 2f), y = Math.round(y0 + dy * t - h / 2f);
            g.fill(x, y, x + h, y + h, color);
        }
    }

    static void frame(GuiGraphics g, int x0, int y0, int x1, int y1, int fill, int border) {
        g.fill(x0, y0, x1, y1, fill);
        g.renderOutline(x0, y0, x1 - x0, y1 - y0, border);
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

    /** An item drawn turned by {@code degrees} round its centre. */
    static void itemRotated(GuiGraphics g, ItemStack stack, float cx, float cy, float scale, float degrees) {
        if (stack.isEmpty()) {
            return;
        }
        g.pose().pushPose();
        g.pose().translate(cx, cy, 120);
        g.pose().mulPose(com.mojang.math.Axis.ZP.rotationDegrees(degrees));
        g.pose().scale(scale, scale, 1);
        g.renderItem(stack, -8, -8);
        g.pose().popPose();
    }
}
