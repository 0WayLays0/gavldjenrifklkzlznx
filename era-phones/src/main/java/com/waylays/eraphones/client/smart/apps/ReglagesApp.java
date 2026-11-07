package com.waylays.eraphones.client.smart.apps;

import com.waylays.eraphones.EraPhones;
import com.waylays.eraphones.client.smart.PhoneApp;
import com.waylays.eraphones.client.PhoneClient;
import com.waylays.eraphones.client.smart.SmartScreen;
import com.waylays.eraphones.client.PhoneSettings;
import com.waylays.eraphones.client.PhoneUI;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/** Réglages: the wallpaper, the body's finish, the sounds, silent mode, sharing one's position; the phone's number. */
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
        PhoneUI.text(g, Component.translatable("phone.eraphones.wallpaper").getString().toUpperCase(), X0 + 10, y, PhoneUI.FAINT, .55f);
        y += 9;
        int tw = (X1 - X0 - 28) / 4, th = tw * 2;
        for (int k = 0; k < 4; k++) {
            int x = X0 + 8 + k * (tw + 4);
            if (s.wallpaper == k) {
                PhoneUI.round(g, x - 2, y - 2, tw + 4, th + 4, 6, 0xFF5CB4FF);
            }
            PhoneUI.blend();
            g.blit(EraPhones.id("textures/gui/phone/wallpaper_" + WALLPAPERS[k] + ".png"), x, y, tw, th, 0, 0, SmartScreen.SW * 3, SmartScreen.SH * 3,
                    SmartScreen.SW * 3, SmartScreen.SH * 3);
        }
        y += th + 8;
        PhoneUI.text(g, Component.translatable("phone.eraphones.finish").getString().toUpperCase(), X0 + 10, y, PhoneUI.FAINT, .55f);
        y += 9;
        for (int k = 0; k < SmartScreen.FINISHES.length; k++) {
            int x = X0 + 12 + k * 24;
            if (s.finish == k) {
                PhoneUI.round(g, x - 2, y - 2, 18, 18, 9, 0xFFFFFFFF);
            }
            PhoneUI.round(g, x, y, 14, 14, 7, PhoneUI.mix(0xFF3A3E46, SmartScreen.FINISHES[k], .7f));
        }
        y += 22;
        y = row(g, y, "phone.eraphones.sounds", s.sounds);
        y = row(g, y, "phone.eraphones.silent", PhoneClient.silent);
        y = row(g, y, "phone.eraphones.share_position", PhoneClient.sharing);
        y += 6;
        PhoneUI.glass(g, X0 + 8, y, X1 - X0 - 16, 36, 9, 0x1EFFFFFF);
        PhoneUI.icon(g, "reglages", X0 + 14, y + 8, 20);
        PhoneUI.text(g, Component.translatable("phone.eraphones.name").getString(), X0 + 40, y + 9, PhoneUI.WHITE, .75f);
        PhoneUI.text(g, Component.translatable("phone.eraphones.about", PhoneClient.number).getString(), X0 + 40, y + 20, PhoneUI.FAINT, .55f);
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
        for (int k = 0; k < SmartScreen.FINISHES.length; k++) {
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
            click();
            PhoneClient.silent = !PhoneClient.silent;
            PhoneClient.ask(PhoneClient.silent ? 7 : 8, "");
        } else if (PhoneUI.in(mx, my, X0 + 8, y + 42, X1 - X0 - 16, 18)) {
            click();
            PhoneClient.sharing = !PhoneClient.sharing;
            PhoneClient.ask(PhoneClient.sharing ? 3 : 4, "");
        }
        return true;
    }
}
