package com.waylays.eraphones.client.smart.apps;

import com.mojang.math.Axis;
import com.waylays.eraphones.client.smart.PhoneApp;
import com.waylays.eraphones.client.PhoneSettings;
import com.waylays.eraphones.client.PhoneUI;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

/** Boussole: a compass rose turning as the player turns, the heading, the way and distance to the world's spawn and to the first place marked on the map. */
public class BoussoleApp extends PhoneApp {
    private static final String[] CARDINALS = {"N", "NE", "E", "SE", "S", "SO", "O", "NO"};

    public BoussoleApp() {
        super("boussole");
    }

    @Override
    public int[] background() {
        return new int[] {0xFF000000, 0xFF000000};
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        header(g, title(), mx, my, null);
        if (mc.player == null || mc.level == null) {
            return;
        }
        float yaw = Mth.wrapDegrees(mc.player.getViewYRot(pt) + 180);
        float heading = (yaw + 360) % 360;
        float cx = (X0 + X1) / 2f, cy = Y0 + 72;
        int r = 58;
        PhoneUI.centered(g, Math.round(heading) + "° " + CARDINALS[Math.round(heading / 45) % 8], cx, Y0 + 2, PhoneUI.WHITE, 1.3f);
        g.fill((int) cx, (int) cy - r - 6, (int) cx + 1, (int) cy - r + 4, 0xFFFF5C6E);
        g.pose().pushPose();
        g.pose().translate(cx, cy, 0);
        g.pose().mulPose(Axis.ZP.rotationDegrees(-heading));
        PhoneUI.round(g, -r, -r, r * 2, r * 2, r, 0xFF141418);
        for (int k = 0; k < 72; k++) {
            g.pose().pushPose();
            g.pose().mulPose(Axis.ZP.rotationDegrees(k * 5));
            int len = k % 18 == 0 ? 8 : k % 6 == 0 ? 5 : 3;
            g.fill(0, -r + 2, 1, -r + 2 + len, k % 18 == 0 ? 0xFFFFFFFF : 0x99FFFFFF);
            g.pose().popPose();
        }
        for (int k = 0; k < 8; k += 2) {
            float a = k * Mth.TWO_PI / 8;
            PhoneUI.centered(g, CARDINALS[k], Mth.sin(a) * (r - 17), -Mth.cos(a) * (r - 17) - 3, k == 0 ? 0xFFFF5C6E : PhoneUI.WHITE, .8f);
        }
        g.pose().popPose();
        PhoneUI.round(g, (int) cx - 3, (int) cy - 3, 6, 6, 3, 0xFFFFFFFF);
        // Ways to places
        int y = (int) cy + r + 10;
        BlockPos spawn = mc.level.getSharedSpawnPos();
        y = way(g, y, Component.translatable("phone.eraphones.spawn").getString(), spawn.getX(), spawn.getZ(), heading);
        for (String w : PhoneSettings.get().waypoints) {
            String[] p = w.split(";");
            if (p.length >= 3) {
                try {
                    y = way(g, y, p[0], Double.parseDouble(p[1]), Double.parseDouble(p[2]), heading);
                } catch (NumberFormatException ignored) {
                }
            }
            if (y > Y1 - 18) {
                break;
            }
        }
    }

    private int way(GuiGraphics g, int y, String name, double x, double z, float heading) {
        double dx = x - mc.player.getX(), dz = z - mc.player.getZ();
        float bearing = (float) Math.toDegrees(Math.atan2(dx, -dz));
        float rel = Mth.wrapDegrees(bearing - heading);
        PhoneUI.glass(g, X0 + 8, y, X1 - X0 - 16, 18, 8, 0x22FFFFFF);
        g.pose().pushPose();
        g.pose().translate(X0 + 20, y + 9, 0);
        g.pose().mulPose(Axis.ZP.rotationDegrees(rel));
        g.fill(-1, -5, 1, 4, 0xFFFFD27A);
        g.fill(-3, -3, 3, -1, 0xFFFFD27A);
        g.pose().popPose();
        PhoneUI.text(g, PhoneUI.fit(name, 80, .65f), X0 + 30, y + 6, PhoneUI.WHITE, .65f);
        PhoneUI.right(g, (int) Math.hypot(dx, dz) + " m", X1 - 14, y + 6, PhoneUI.DIM, .65f);
        return y + 21;
    }

    @Override
    public boolean click(double mx, double my, int button) {
        if (headerBack(mx, my)) {
            phone.home();
        }
        return true;
    }
}
