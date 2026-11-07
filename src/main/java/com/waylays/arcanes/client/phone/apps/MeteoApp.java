package com.waylays.arcanes.client.phone.apps;

import com.waylays.arcanes.client.phone.PhoneApp;
import com.waylays.arcanes.client.phone.PhoneClient;
import com.waylays.arcanes.client.phone.PhoneScreen;
import com.waylays.arcanes.client.phone.PhoneUI;
import com.waylays.arcanes.network.PhonePackets;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

/**
 * Météo: the sky where the player stands, large: the place, the temperature, the sky, the hours to come, when the rain
 * will come or go (the server knows), the moon's phase drawn, and the sun's way across the day.
 */
public class MeteoApp extends PhoneApp {
    public MeteoApp() {
        super("meteo");
    }

    @Override
    public void open() {
        PhoneClient.ask(2, "");
    }

    @Override
    public int[] background() {
        PhoneClient.Sky s = PhoneClient.sky();
        return s.night() ? new int[] {0xFF1C2458, 0xFF0A0E24} : s.rain() ? new int[] {0xFF485670, 0xFF242C3C} : new int[] {0xFF3E8CE0, 0xFF1C4C98};
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        float time = System.currentTimeMillis() / 1000f;
        PhoneClient.Sky s = PhoneClient.sky();
        PhoneUI.text(g, "‹", X0 + 6, TOP + 3, PhoneUI.WHITE, 1.6f);
        float cx = (X0 + X1) / 2f;
        PhoneUI.centered(g, s.place(), cx, TOP + 10, PhoneUI.WHITE, .95f);
        String t = s.temperature() + "°";
        PhoneUI.centered(g, t, cx + 4, TOP + 22, PhoneUI.WHITE, 4f);
        PhoneUI.centered(g, s.condition(), cx, TOP + 56, 0xEEFFFFFF, .8f);
        PhoneUI.centered(g, s.range(), cx, TOP + 66, 0xDDFFFFFF, .7f);
        // The hours
        int y = TOP + 80;
        PhoneUI.glass(g, X0 + 8, y, X1 - X0 - 16, 50, 10, 0x33FFFFFF);
        PhoneUI.text(g, Component.translatable("phone.arcanes_oublies.next_hours").getString().toUpperCase(), X0 + 14, y + 5, 0xCCFFFFFF, .5f);
        for (int k = 0; k < 6; k++) {
            PhoneClient.Hour h = s.hours().get(k);
            float hx = X0 + 20 + k * (X1 - X0 - 40) / 5f;
            PhoneUI.centered(g, h.label(), hx, y + 15, PhoneUI.WHITE, .55f);
            PhoneScreen.weatherGlyph(g, h.glyph(), hx, y + 29, 4.5f, time + k);
            PhoneUI.centered(g, h.temperature() + "°", hx, y + 39, PhoneUI.WHITE, .6f);
        }
        // What is coming
        y += 56;
        PhoneUI.glass(g, X0 + 8, y, X1 - X0 - 16, 26, 10, 0x33FFFFFF);
        PhoneUI.text(g, Component.translatable("phone.arcanes_oublies.forecast").getString().toUpperCase(), X0 + 14, y + 5, 0xCCFFFFFF, .5f);
        PhoneUI.text(g, forecast(), X0 + 14, y + 15, PhoneUI.WHITE, .62f);
        // The moon, and the day
        y += 32;
        int half = (X1 - X0 - 20) / 2;
        PhoneUI.glass(g, X0 + 8, y, half, 54, 10, 0x33FFFFFF);
        PhoneUI.text(g, Component.translatable("phone.arcanes_oublies.moon").getString().toUpperCase(), X0 + 14, y + 5, 0xCCFFFFFF, .5f);
        int phase = mc.level == null ? 0 : mc.level.getMoonPhase();
        moon(g, X0 + 8 + half / 2f, y + 30, 11, phase);
        PhoneUI.centered(g, PhoneUI.fit(Component.translatable("phone.arcanes_oublies.moon." + phase).getString(), half - 4, .5f), X0 + 8 + half / 2f, y + 45,
                PhoneUI.WHITE, .5f);
        int dx = X0 + 12 + half;
        PhoneUI.glass(g, dx, y, half, 54, 10, 0x33FFFFFF);
        PhoneUI.text(g, Component.translatable("phone.arcanes_oublies.sun").getString().toUpperCase(), dx + 6, y + 5, 0xCCFFFFFF, .5f);
        long dt = mc.level == null ? 6000 : Math.floorMod(mc.level.getDayTime(), 24000);
        float day = dt / 12000f;
        // The sun's arc, from 6:00 to 18:00
        for (int k = 0; k <= 20; k++) {
            float a = Mth.PI * k / 20;
            g.fill((int) (dx + half / 2f - Mth.cos(a) * (half / 2f - 8)), (int) (y + 40 - Mth.sin(a) * 18), (int) (dx + half / 2f - Mth.cos(a) * (half / 2f - 8)) + 1,
                    (int) (y + 40 - Mth.sin(a) * 18) + 1, 0x88FFFFFF);
        }
        if (day <= 1) {
            float a = Mth.PI * day;
            PhoneScreen.weatherGlyph(g, 0, dx + half / 2f - Mth.cos(a) * (half / 2f - 8), y + 40 - Mth.sin(a) * 18, 3, time);
        }
        PhoneUI.text(g, "6:00", dx + 4, y + 45, PhoneUI.WHITE, .45f);
        PhoneUI.right(g, "18:00", dx + half - 4, y + 45, PhoneUI.WHITE, .45f);
    }

    private String forecast() {
        PhonePackets.Weather w = PhoneClient.weather;
        if (w == null) {
            return "…";
        }
        long since = (System.currentTimeMillis() - PhoneClient.weatherAt) / 50;
        if (w.thundering() && w.thunder() > 0) {
            return Component.translatable("phone.arcanes_oublies.storm_ends", minutes(w.thunder() - since)).getString();
        }
        if (w.raining() && w.rain() > 0) {
            return Component.translatable("phone.arcanes_oublies.rain_ends", minutes(w.rain() - since)).getString();
        }
        if (!w.raining() && w.clear() > 0) {
            return Component.translatable("phone.arcanes_oublies.clear_for", minutes(w.clear() - since)).getString();
        }
        if (!w.raining() && w.rain() > 0) {
            return Component.translatable("phone.arcanes_oublies.rain_in", minutes(w.rain() - since)).getString();
        }
        return Component.translatable("phone.arcanes_oublies.no_change").getString();
    }

    private static long minutes(long ticks) {
        return Math.max(1, ticks / 1200);
    }

    /** The moon in its phase (0 full, 4 new): a disc, the shadow slid over it. */
    static void moon(GuiGraphics g, float cx, float cy, int r, int phase) {
        PhoneUI.glow(g, cx, cy, r * 4, 0x55C8D8FF);
        PhoneUI.round(g, (int) cx - r, (int) cy - r, r * 2, r * 2, r, 0xFFEFF2FF);
        float lit = Math.abs(4 - phase) / 4f;
        if (phase != 0) {
            int shift = (int) (r * 2 * lit) * (phase < 4 ? 1 : -1);
            PhoneUI.round(g, (int) cx - r + shift, (int) cy - r, r * 2, r * 2, r, 0xEE1A1F3A);
        }
    }

    @Override
    public boolean click(double mx, double my, int button) {
        if (headerBack(mx, my)) {
            phone.home();
        }
        return true;
    }
}
