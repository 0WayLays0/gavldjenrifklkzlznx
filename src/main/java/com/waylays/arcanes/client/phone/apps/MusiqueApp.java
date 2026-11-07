package com.waylays.arcanes.client.phone.apps;

import com.mojang.math.Axis;
import com.waylays.arcanes.ArcanesOublies;
import com.waylays.arcanes.client.music.MusicLibrary;
import com.waylays.arcanes.client.music.MusicPlayer;
import com.waylays.arcanes.client.music.MusicScreen;
import com.waylays.arcanes.client.phone.PhoneApp;
import com.waylays.arcanes.client.phone.PhoneUI;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/**
 * Musique: the music player in the phone: the record turning, what plays, where it is, the controls, and the library to
 * choose from; the full player opens from its corner.
 */
public class MusiqueApp extends PhoneApp {
    private static final ResourceLocation RECORD = ArcanesOublies.id("textures/gui/music/record.png");
    private float angle;
    private long last;

    public MusiqueApp() {
        super("musique");
    }

    @Override
    public int[] background() {
        return new int[] {0xFF3A1230, 0xFF0E0610};
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        header(g, title(), mx, my, "⤢");
        long now = System.currentTimeMillis();
        if (MusicPlayer.playing()) {
            angle = (angle + (now - last) * .12f) % 360;
        }
        last = now;
        float cx = (X0 + X1) / 2f;
        int cy = Y0 + 42;
        PhoneUI.glow(g, cx, cy, 110, MusicPlayer.playing() ? 0x66FF5AA0 : 0x33FF5AA0);
        g.pose().pushPose();
        g.pose().translate(cx, cy, 0);
        g.pose().mulPose(Axis.ZP.rotationDegrees(angle));
        PhoneUI.blend();
        g.blit(RECORD, -36, -36, 72, 72, 0, 0, 128, 128, 128, 128);
        g.pose().popPose();
        MusicLibrary.Track t = MusicPlayer.current();
        String title = t == null ? Component.translatable("ui.arcanes_oublies.music.nothing").getString() : t.title;
        String artist = t == null || t.artist == null ? "" : t.artist;
        PhoneUI.centered(g, PhoneUI.fit(title, X1 - X0 - 16, .85f), cx, cy + 44, PhoneUI.WHITE, .85f);
        PhoneUI.centered(g, PhoneUI.fit(artist, X1 - X0 - 16, .65f), cx, cy + 54, PhoneUI.DIM, .65f);
        // Where it is
        int len = Math.max(1, MusicPlayer.lengthMs());
        float at = Math.min(1, MusicPlayer.positionMs() / (float) len);
        int bx = X0 + 14, bw = X1 - X0 - 28, by = cy + 66;
        PhoneUI.round(g, bx, by, bw, 3, 1, 0x44FFFFFF);
        PhoneUI.round(g, bx, by, Math.max(2, (int) (bw * at)), 3, 1, 0xFFFF7AC8);
        PhoneUI.text(g, time(MusicPlayer.positionMs()), bx, by + 5, PhoneUI.DIM, .5f);
        PhoneUI.right(g, time(len), bx + bw, by + 5, PhoneUI.DIM, .5f);
        // The controls
        int ry = by + 14;
        control(g, "⏮", cx - 30, ry, mx, my, false);
        control(g, MusicPlayer.playing() ? "❚❚" : "▶", cx, ry, mx, my, true);
        control(g, "⏭", cx + 30, ry, mx, my, false);
        // The library
        int ly = ry + 26;
        PhoneUI.text(g, Component.translatable("ui.arcanes_oublies.music.library").getString().toUpperCase(), X0 + 10, ly, PhoneUI.FAINT, .55f);
        clip(g, ly + 8, Y1);
        int y = ly + 10 - (int) scroll;
        for (MusicLibrary.Track tr : MusicLibrary.get().all()) {
            boolean cur = tr == t;
            boolean hover = PhoneUI.in(mx, my, X0 + 6, y, X1 - X0 - 12, 16);
            if (cur || hover) {
                PhoneUI.round(g, X0 + 6, y, X1 - X0 - 12, 16, 6, cur ? 0x44FF7AC8 : 0x18FFFFFF);
            }
            PhoneUI.text(g, PhoneUI.fit(tr.title, X1 - X0 - 30, .65f), X0 + 12, y + 5, cur ? 0xFFFFB0E0 : PhoneUI.WHITE, .65f);
            y += 17;
        }
        unclip(g);
    }

    private static String time(long ms) {
        long s = ms / 1000;
        return s / 60 + ":" + String.format("%02d", s % 60);
    }

    private void control(GuiGraphics g, String glyph, float x, int y, int mx, int my, boolean main) {
        int r = main ? 11 : 8;
        boolean hover = PhoneUI.in(mx, my, x - r, y, r * 2, r * 2);
        PhoneUI.round(g, (int) x - r, y, r * 2, r * 2, r, main ? (hover ? 0xFFFFFFFF : 0xEEFFFFFF) : hover ? 0x44FFFFFF : 0x22FFFFFF);
        PhoneUI.centered(g, glyph, x + .5f, y + r - 3.5f, main ? 0xFF3A1230 : PhoneUI.WHITE, .8f);
    }

    @Override
    public boolean click(double mx, double my, int button) {
        if (headerBack(mx, my)) {
            phone.home();
            return true;
        }
        if (headerAction(mx, my)) {
            mc.setScreen(new MusicScreen());
            return true;
        }
        float cx = (X0 + X1) / 2f;
        int ry = Y0 + 42 + 66 + 14;
        if (PhoneUI.in(mx, my, cx - 38, ry, 16, 16)) {
            click();
            MusicPlayer.previous();
        } else if (PhoneUI.in(mx, my, cx - 11, ry, 22, 22)) {
            click();
            if (MusicPlayer.current() == null) {
                List<MusicLibrary.Track> all = MusicLibrary.get().all();
                if (!all.isEmpty()) {
                    MusicPlayer.play(all.get(0), all);
                }
            } else {
                MusicPlayer.togglePause();
            }
        } else if (PhoneUI.in(mx, my, cx + 22, ry, 16, 16)) {
            click();
            MusicPlayer.next();
        } else {
            int y = ry + 26 + 10 - (int) scroll;
            List<MusicLibrary.Track> all = MusicLibrary.get().all();
            for (MusicLibrary.Track tr : all) {
                if (PhoneUI.in(mx, my, X0 + 6, y, X1 - X0 - 12, 16) && my > ry + 34) {
                    click();
                    MusicPlayer.play(tr, all);
                    break;
                }
                y += 17;
            }
        }
        return true;
    }
}
