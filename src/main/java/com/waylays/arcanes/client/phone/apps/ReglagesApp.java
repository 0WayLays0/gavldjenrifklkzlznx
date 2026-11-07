package com.waylays.arcanes.client.phone.apps;

import com.waylays.arcanes.ArcanesOublies;
import com.waylays.arcanes.client.phone.PhoneApp;
import com.waylays.arcanes.client.phone.PhoneClient;
import com.waylays.arcanes.client.phone.PhoneScreen;
import com.waylays.arcanes.client.phone.PhoneSettings;
import com.waylays.arcanes.client.phone.PhoneUI;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/** Réglages: the wallpaper, the body's finish, the sounds, do-not-disturb, sharing one's position; about the phone. */
public class ReglagesApp extends PhoneApp {
    private static final String[] WALLPAPERS = {"nebula", "aurora", "abyss", "ember"};

    public ReglagesApp() {
        super("reglages");
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        header(g, title(), mx, my, null);
        PhoneSettings s = PhoneSettings.get();
        int y = Y0 + 2;
        PhoneUI.text(g, Component.translatable("phone.arcanes_oublies.wallpaper").getString().toUpperCase(), X0 + 10, y, PhoneUI.FAINT, .55f);
        y += 9;
        int tw = (X1 - X0 - 28) / 4, th = tw * 2;
        for (int k = 0; k < 4; k++) {
            int x = X0 + 8 + k * (tw + 4);
            if (s.wallpaper == k) {
                PhoneUI.round(g, x - 2, y - 2, tw + 4, th + 4, 6, 0xFFB98CFF);
            }
            PhoneUI.blend();
            g.blit(ArcanesOublies.id("textures/gui/phone/wallpaper_" + WALLPAPERS[k] + ".png"), x, y, tw, th, 0, 0, PhoneScreen.SW * 3, PhoneScreen.SH * 3,
                    PhoneScreen.SW * 3, PhoneScreen.SH * 3);
        }
        y += th + 8;
        PhoneUI.text(g, Component.translatable("phone.arcanes_oublies.finish").getString().toUpperCase(), X0 + 10, y, PhoneUI.FAINT, .55f);
        y += 9;
        for (int k = 0; k < PhoneScreen.FINISHES.length; k++) {
            int x = X0 + 12 + k * 24;
            if (s.finish == k) {
                PhoneUI.round(g, x - 2, y - 2, 18, 18, 9, 0xFFFFFFFF);
            }
            PhoneUI.round(g, x, y, 14, 14, 7, PhoneUI.mix(0xFF4A2E6A, PhoneScreen.FINISHES[k], .6f));
        }
        y += 22;
        y = row(g, y, "phone.arcanes_oublies.sounds", s.sounds);
        y = row(g, y, "phone.arcanes_oublies.quiet", s.quiet);
        y = row(g, y, "phone.arcanes_oublies.share_position", PhoneClient.sharing);
        y += 6;
        PhoneUI.glass(g, X0 + 8, y, X1 - X0 - 16, 36, 9, 0x1EFFFFFF);
        PhoneUI.icon(g, "reglages", X0 + 14, y + 8, 20);
        PhoneUI.text(g, Component.translatable("phone.arcanes_oublies.name").getString(), X0 + 40, y + 9, PhoneUI.WHITE, .75f);
        PhoneUI.text(g, Component.translatable("phone.arcanes_oublies.about").getString(), X0 + 40, y + 20, PhoneUI.FAINT, .55f);
    }

    private int row(GuiGraphics g, int y, String key, boolean on) {
        PhoneUI.glass(g, X0 + 8, y, X1 - X0 - 16, 18, 8, 0x1EFFFFFF);
        PhoneUI.text(g, Component.translatable(key).getString(), X0 + 14, y + 6, PhoneUI.WHITE, .65f);
        ContactsApp.toggle(g, X1 - 32, y + 4, on);
        return y + 21;
    }

    @Override
    public boolean click(double mx, double my, int button) {
        if (headerBack(mx, my)) {
            phone.home();
            return true;
        }
        PhoneSettings s = PhoneSettings.get();
        int y = Y0 + 11;
        int tw = (X1 - X0 - 28) / 4, th = tw * 2;
        for (int k = 0; k < 4; k++) {
            if (PhoneUI.in(mx, my, X0 + 8 + k * (tw + 4), y, tw, th)) {
                click();
                s.wallpaper = k;
                s.save();
                return true;
            }
        }
        y += th + 17;
        for (int k = 0; k < PhoneScreen.FINISHES.length; k++) {
            if (PhoneUI.in(mx, my, X0 + 12 + k * 24, y, 14, 14)) {
                click();
                s.finish = k;
                s.save();
                return true;
            }
        }
        y += 22;
        if (PhoneUI.in(mx, my, X0 + 8, y, X1 - X0 - 16, 18)) {
            s.sounds = !s.sounds;
            s.save();
            click();
        } else if (PhoneUI.in(mx, my, X0 + 8, y + 21, X1 - X0 - 16, 18)) {
            s.quiet = !s.quiet;
            s.save();
            click();
        } else if (PhoneUI.in(mx, my, X0 + 8, y + 42, X1 - X0 - 16, 18)) {
            click();
            PhoneClient.sharing = !PhoneClient.sharing;
            PhoneClient.ask(PhoneClient.sharing ? 3 : 4, "");
        }
        return true;
    }
}
