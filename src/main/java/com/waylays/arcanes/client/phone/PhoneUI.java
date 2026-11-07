package com.waylays.arcanes.client.phone;

import com.mojang.blaze3d.systems.RenderSystem;
import com.waylays.arcanes.ArcanesOublies;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.PlayerFaceRenderer;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;

import java.util.List;
import java.util.UUID;

/** Drawing for the Arcanophone, in the phone's own coordinates: rounded glass panels, glows, text, faces, icons. */
public final class PhoneUI {
    public static final ResourceLocation ROUND = ArcanesOublies.id("textures/gui/phone/round.png");
    public static final ResourceLocation GLOW = ArcanesOublies.id("textures/gui/phone/glow.png");
    public static final ResourceLocation FRAME = ArcanesOublies.id("textures/gui/phone/frame.png");

    public static final int WHITE = 0xFFFFFFFF, DIM = 0xFFB8B4C8, FAINT = 0xFF7C7890, ACCENT = 0xFFB98CFF, ACCENT_2 = 0xFFFF7AD9, GOLD = 0xFFFFD27A,
            GREEN = 0xFF5CE08A, RED = 0xFFFF5C6E, BLUE = 0xFF5CB4FF, PANEL = 0xCC16121F, GLASS = 0x66FFFFFF;

    private PhoneUI() {}

    public static ResourceLocation app(String id) {
        return ArcanesOublies.id("textures/gui/phone/app_" + id + ".png");
    }

    private static void tint(GuiGraphics g, int argb) {
        g.setColor((argb >> 16 & 255) / 255f, (argb >> 8 & 255) / 255f, (argb & 255) / 255f, (argb >>> 24) / 255f);
    }

    public static void blend() {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
    }

    /** A rounded rectangle of {@code argb}, its corners {@code r} round (at most half its height). */
    public static void round(GuiGraphics g, int x, int y, int w, int h, int r, int argb) {
        if (w <= 0 || h <= 0 || (argb >>> 24) == 0) {
            return;
        }
        r = Math.max(1, Math.min(r, Math.min(w, h) / 2));
        blend();
        tint(g, argb);
        int c = 12, t = 48;
        g.blit(ROUND, x, y, r, r, 0, 0, c, c, t, t);
        g.blit(ROUND, x + w - r, y, r, r, t - c, 0, c, c, t, t);
        g.blit(ROUND, x, y + h - r, r, r, 0, t - c, c, c, t, t);
        g.blit(ROUND, x + w - r, y + h - r, r, r, t - c, t - c, c, c, t, t);
        if (w > 2 * r) {
            g.blit(ROUND, x + r, y, w - 2 * r, r, c, 0, t - 2 * c, c, t, t);
            g.blit(ROUND, x + r, y + h - r, w - 2 * r, r, c, t - c, t - 2 * c, c, t, t);
        }
        if (h > 2 * r) {
            g.blit(ROUND, x, y + r, r, h - 2 * r, 0, c, c, t - 2 * c, t, t);
            g.blit(ROUND, x + w - r, y + r, r, h - 2 * r, t - c, c, c, t - 2 * c, t, t);
            if (w > 2 * r) {
                g.blit(ROUND, x + r, y + r, w - 2 * r, h - 2 * r, c, c, t - 2 * c, t - 2 * c, t, t);
            }
        }
        g.setColor(1, 1, 1, 1);
    }

    /** A panel of frosted glass: a translucent fill, a brighter rim at its top. */
    public static void glass(GuiGraphics g, int x, int y, int w, int h, int r, int argb) {
        round(g, x, y, w, h, r, argb);
        round(g, x + 1, y, w - 2, Math.min(h, r + 2), r, 0x18FFFFFF);
    }

    public static void glow(GuiGraphics g, float cx, float cy, float size, int argb) {
        blend();
        tint(g, argb);
        g.pose().pushPose();
        g.pose().translate(cx - size / 2, cy - size / 2, 0);
        g.pose().scale(size / 64f, size / 64f, 1);
        g.blit(GLOW, 0, 0, 64, 64, 0, 0, 64, 64, 64, 64);
        g.pose().popPose();
        g.setColor(1, 1, 1, 1);
    }

    public static void icon(GuiGraphics g, String app, float x, float y, float size) {
        blend();
        g.pose().pushPose();
        g.pose().translate(x, y, 0);
        g.pose().scale(size / 96f, size / 96f, 1);
        g.blit(app(app), 0, 0, 96, 96, 0, 0, 96, 96, 96, 96);
        g.pose().popPose();
    }

    public static void text(GuiGraphics g, String s, float x, float y, int color, float scale) {
        Font font = Minecraft.getInstance().font;
        g.pose().pushPose();
        g.pose().translate(x, y, 0);
        g.pose().scale(scale, scale, 1);
        g.drawString(font, s, 0, 0, color, false);
        g.pose().popPose();
    }

    public static void text(GuiGraphics g, Component s, float x, float y, int color, float scale) {
        text(g, s.getString(), x, y, color, scale);
    }

    public static void centered(GuiGraphics g, String s, float cx, float y, int color, float scale) {
        Font font = Minecraft.getInstance().font;
        text(g, s, cx - font.width(s) * scale / 2, y, color, scale);
    }

    public static void right(GuiGraphics g, String s, float rx, float y, int color, float scale) {
        Font font = Minecraft.getInstance().font;
        text(g, s, rx - font.width(s) * scale, y, color, scale);
    }

    public static int width(String s, float scale) {
        return Math.round(Minecraft.getInstance().font.width(s) * scale);
    }

    /** {@code s} cut to fit {@code w} at {@code scale}, with an ellipsis. */
    public static String fit(String s, float w, float scale) {
        Font font = Minecraft.getInstance().font;
        if (font.width(s) * scale <= w) {
            return s;
        }
        String cut = font.plainSubstrByWidth(s, (int) (w / scale) - font.width("…"));
        return cut + "…";
    }

    /** {@code s} wrapped to {@code w} at {@code scale}. */
    public static List<FormattedCharSequence> wrap(String s, float w, float scale) {
        return Minecraft.getInstance().font.split(Component.literal(s), Math.max(10, (int) (w / scale)));
    }

    public static void lines(GuiGraphics g, List<FormattedCharSequence> lines, float x, float y, int color, float scale) {
        Font font = Minecraft.getInstance().font;
        g.pose().pushPose();
        g.pose().translate(x, y, 0);
        g.pose().scale(scale, scale, 1);
        for (int i = 0; i < lines.size(); i++) {
            g.drawString(font, lines.get(i), 0, i * 10, color, false);
        }
        g.pose().popPose();
    }

    /** A player's face, {@code size} across, in a rounded frame. */
    public static void face(GuiGraphics g, UUID id, int x, int y, int size) {
        Minecraft mc = Minecraft.getInstance();
        PlayerInfo info = mc.getConnection() != null ? mc.getConnection().getPlayerInfo(id) : null;
        PlayerSkin skin = info != null ? info.getSkin() : DefaultPlayerSkin.get(id);
        round(g, x - 1, y - 1, size + 2, size + 2, Math.max(2, size / 3), 0x55FFFFFF);
        PlayerFaceRenderer.draw(g, skin, x, y, size);
    }

    public static boolean in(double mx, double my, float x, float y, float w, float h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    /** How long ago {@code epochMs} was, briefly. */
    public static String ago(long epochMs) {
        long s = Math.max(0, (System.currentTimeMillis() - epochMs) / 1000);
        if (s < 60) {
            return Component.translatable("phone.arcanes_oublies.now").getString();
        }
        if (s < 3600) {
            return Component.translatable("phone.arcanes_oublies.minutes", s / 60).getString();
        }
        if (s < 86400) {
            return Component.translatable("phone.arcanes_oublies.hours", s / 3600).getString();
        }
        return Component.translatable("phone.arcanes_oublies.days", s / 86400).getString();
    }

    public static float ease(float t) {
        t = Math.max(0, Math.min(1, t));
        return 1 - (1 - t) * (1 - t) * (1 - t);
    }

    /** Overshoots a little and settles. */
    public static float spring(float t) {
        t = Math.max(0, Math.min(1, t));
        float c1 = 1.4f, c3 = c1 + 1;
        return 1 + c3 * (float) Math.pow(t - 1, 3) + c1 * (float) Math.pow(t - 1, 2);
    }

    public static int alpha(int argb, float a) {
        int al = Math.round((argb >>> 24) * Math.max(0, Math.min(1, a)));
        return al << 24 | (argb & 0xFFFFFF);
    }

    public static int mix(int a, int b, float t) {
        int ar = a >> 16 & 255, ag = a >> 8 & 255, ab = a & 255, aa = a >>> 24;
        int br = b >> 16 & 255, bg = b >> 8 & 255, bb = b & 255, ba = b >>> 24;
        return Math.round(aa + (ba - aa) * t) << 24 | Math.round(ar + (br - ar) * t) << 16 | Math.round(ag + (bg - ag) * t) << 8 | Math.round(ab + (bb - ab) * t);
    }
}
