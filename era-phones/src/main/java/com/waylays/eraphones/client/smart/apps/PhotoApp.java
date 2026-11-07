package com.waylays.eraphones.client.smart.apps;

import com.waylays.eraphones.client.smart.PhoneApp;
import com.waylays.eraphones.client.PhoneClient;
import com.waylays.eraphones.client.smart.SmartScreen;
import com.waylays.eraphones.client.PhoneUI;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/**
 * Photo: the screen becomes a viewfinder (the world seen through it), thirds drawn over it; the shutter takes the world
 * without the interface into the gallery, with a flash.
 */
public class PhotoApp extends PhoneApp {
    public PhotoApp() {
        super("photo");
    }

    @Override
    public boolean seeThrough() {
        return true;
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        int sx = SmartScreen.SX, sy = SmartScreen.SY, sw = SmartScreen.SW, sh = SmartScreen.SH;
        // Dark bands top and bottom, like a camera's
        g.fill(sx, sy, sx + sw, sy + 40, 0xAA000000);
        g.fill(sx, sy + sh - 74, sx + sw, sy + sh, 0xAA000000);
        // The thirds
        int top = sy + 40, bottom = sy + sh - 74;
        for (int k = 1; k < 3; k++) {
            g.fill(sx + sw * k / 3, top, sx + sw * k / 3 + 1, bottom, 0x44FFFFFF);
            g.fill(sx, top + (bottom - top) * k / 3, sx + sw, top + (bottom - top) * k / 3 + 1, 0x44FFFFFF);
        }
        // Focus brackets in the middle
        int cx = sx + sw / 2, cy = (top + bottom) / 2;
        for (int k = 0; k < 4; k++) {
            int dx = k % 2 == 0 ? -12 : 12, dy = k < 2 ? -12 : 12;
            g.fill(cx + dx - (dx > 0 ? 4 : 0), cy + dy, cx + dx + (dx < 0 ? 4 : 0), cy + dy + 1, 0xCCFFD27A);
            g.fill(cx + dx, cy + dy - (dy > 0 ? 4 : 0), cx + dx + 1, cy + dy + (dy < 0 ? 4 : 0), 0xCCFFD27A);
        }
        PhoneUI.centered(g, Component.translatable("phone.eraphones.photo_mode").getString(), cx, sy + sh - 66, PhoneUI.GOLD, .62f);
        // The shutter
        boolean hs = PhoneUI.in(mx, my, cx - 16, sy + sh - 52, 32, 32);
        PhoneUI.round(g, cx - 16, sy + sh - 52, 32, 32, 16, 0xFFFFFFFF);
        PhoneUI.round(g, cx - 13, sy + sh - 49, 26, 26, 13, 0xFF000000);
        PhoneUI.round(g, cx - 11, sy + sh - 47, 22, 22, 11, hs ? 0xFFE0E0E0 : 0xFFFFFFFF);
        // The last photo, to the gallery
        List<ResourceLocation> last = PhoneClient.recentPhotos(1);
        PhoneUI.round(g, sx + 18, sy + sh - 45, 20, 20, 5, 0x88FFFFFF);
        if (!last.isEmpty()) {
            PhoneUI.blend();
            g.blit(last.get(0), sx + 19, sy + sh - 44, 18, 18, 16, 0, 96, 96, 128, 96);
        }
        PhoneUI.round(g, sx + sw - 38, sy + sh - 45, 20, 20, 10, 0x55FFFFFF);
        PhoneUI.centered(g, "⟲", sx + sw - 28, sy + sh - 39, PhoneUI.WHITE, 1f);
        PhoneUI.text(g, "‹", PhoneApp.X0 + 6, PhoneApp.TOP + 3, PhoneUI.WHITE, 1.6f);
        // The flash
        long since = System.currentTimeMillis() - PhoneClient.flashAt;
        if (since < 500) {
            g.fill(sx, sy, sx + sw, sy + sh, PhoneUI.alpha(0xFFFFFFFF, 1 - since / 500f));
        }
    }

    @Override
    public boolean click(double mx, double my, int button) {
        int sx = SmartScreen.SX, sy = SmartScreen.SY, sw = SmartScreen.SW, sh = SmartScreen.SH;
        int cx = sx + sw / 2;
        if (headerBack(mx, my)) {
            phone.home();
            return true;
        }
        if (PhoneUI.in(mx, my, cx - 16, sy + sh - 52, 32, 32)) {
            PhoneClient.photo();
            return true;
        }
        if (PhoneUI.in(mx, my, sx + 18, sy + sh - 45, 20, 20)) {
            click();
            phone.open("galerie", sx + 28, sy + sh - 35);
            return true;
        }
        return true;
    }
}
