package com.waylays.eraphones.client.smart.apps;

import com.mojang.math.Axis;
import com.waylays.eraphones.client.smart.PhoneApp;
import com.waylays.eraphones.client.PhoneClient;
import com.waylays.eraphones.client.PhoneUI;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/** Horloge: the world's time on a clock face with hands, the real time beside it, and a stopwatch. */
public class HorlogeApp extends PhoneApp {
    private static long started = -1, stopped;

    public HorlogeApp() {
        super("horloge");
    }

    @Override
    public int[] background() {
        return new int[] {0xFF000000, 0xFF000000};
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        header(g, title(), mx, my, null);
        float cx = (X0 + X1) / 2f, cy = Y0 + 56;
        int r = 48;
        PhoneUI.glow(g, cx, cy, r * 3, 0x33B98CFF);
        PhoneUI.round(g, (int) cx - r, (int) cy - r, r * 2, r * 2, r, 0xFF1A1A1E);
        PhoneUI.round(g, (int) cx - r + 2, (int) cy - r + 2, r * 2 - 4, r * 2 - 4, r - 2, 0xFF0A0A0C);
        for (int k = 0; k < 12; k++) {
            float a = k * Mth.TWO_PI / 12;
            float rr = r - 9;
            PhoneUI.centered(g, String.valueOf(k == 0 ? 12 : k), cx + Mth.sin(a) * rr, cy - Mth.cos(a) * rr - 3, k % 3 == 0 ? PhoneUI.WHITE : PhoneUI.DIM,
                    k % 3 == 0 ? .7f : .55f);
        }
        long t = mc.level == null ? 6000 : Math.floorMod(mc.level.getDayTime(), 24000);
        float hours = (t / 1000f + 6) % 12, minutes = t % 1000 * 60 / 1000f;
        hand(g, cx, cy, hours / 12 * 360 + minutes / 2, r * .5f, 2, 0xFFFFFFFF);
        hand(g, cx, cy, minutes * 6, r * .75f, 1, 0xFFFFFFFF);
        hand(g, cx, cy, (System.currentTimeMillis() % 60000) / 60000f * 360, r * .8f, 1, 0xFFFF9F0A);
        PhoneUI.round(g, (int) cx - 2, (int) cy - 2, 4, 4, 2, 0xFFFF9F0A);
        PhoneUI.centered(g, PhoneClient.clock(), cx, cy + r + 6, PhoneUI.WHITE, 1.2f);
        PhoneUI.centered(g, Component.translatable("phone.eraphones.world_time").getString(), cx, cy + r + 18, PhoneUI.FAINT, .55f);
        int y = (int) cy + r + 30;
        PhoneUI.glass(g, X0 + 8, y, X1 - X0 - 16, 20, 8, 0x22FFFFFF);
        PhoneUI.text(g, Component.translatable("phone.eraphones.real_time").getString(), X0 + 14, y + 7, PhoneUI.DIM, .6f);
        PhoneUI.right(g, LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")), X1 - 14, y + 6, PhoneUI.WHITE, .8f);
        // The stopwatch
        y += 26;
        long ms = started < 0 ? stopped : System.currentTimeMillis() - started;
        PhoneUI.centered(g, String.format("%02d:%02d.%02d", ms / 60000, ms / 1000 % 60, ms / 10 % 100), cx, y, PhoneUI.WHITE, 1.5f);
        button(g, X0 + 14, y + 16, 56, 16, Component.translatable(started < 0 ? "phone.eraphones.start" : "phone.eraphones.stop").getString(),
                started < 0 ? 0xFF1E8A3C : 0xFFC8344A, mx, my);
        button(g, X1 - 70, y + 16, 56, 16, Component.translatable("phone.eraphones.reset").getString(), 0xFF444448, mx, my);
    }

    private static void hand(GuiGraphics g, float cx, float cy, float degrees, float len, int w, int color) {
        g.pose().pushPose();
        g.pose().translate(cx, cy, 0);
        g.pose().mulPose(Axis.ZP.rotationDegrees(degrees));
        g.fill(-w, (int) -len, w, 2, color);
        g.pose().popPose();
    }

    @Override
    public boolean click(double mx, double my, int button) {
        if (headerBack(mx, my)) {
            phone.home();
            return true;
        }
        int y = Y0 + 56 + 48 + 30 + 26;
        if (PhoneUI.in(mx, my, X0 + 14, y + 16, 56, 16)) {
            click();
            if (started < 0) {
                started = System.currentTimeMillis() - stopped;
            } else {
                stopped = System.currentTimeMillis() - started;
                started = -1;
            }
        } else if (PhoneUI.in(mx, my, X1 - 70, y + 16, 56, 16)) {
            click();
            started = -1;
            stopped = 0;
        }
        return true;
    }
}
