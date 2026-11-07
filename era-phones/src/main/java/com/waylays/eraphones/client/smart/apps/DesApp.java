package com.waylays.eraphones.client.smart.apps;

import com.waylays.eraphones.client.smart.PhoneApp;
import com.waylays.eraphones.client.PhoneUI;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Dés: a die to throw (d4 to d20) for role-play; it tumbles a moment and lands; the last throws are kept below. */
public class DesApp extends PhoneApp {
    private static final int[] FACES = {4, 6, 8, 10, 12, 20, 100};
    private final Random random = new Random();
    private int faces = 20;
    private int result = 20;
    private long thrownAt = -10000;
    private final List<String> last = new ArrayList<>();

    public DesApp() {
        super("des");
    }

    @Override
    public int[] background() {
        return new int[] {0xFF2A0E14, 0xFF0E0408};
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        header(g, title(), mx, my, null);
        int w = (X1 - X0 - 16) / FACES.length;
        for (int k = 0; k < FACES.length; k++) {
            int x = X0 + 8 + k * w;
            boolean on = faces == FACES[k];
            PhoneUI.round(g, x + 1, Y0 + 2, w - 2, 14, 7, on ? 0xFFE0405A : 0x33FFFFFF);
            PhoneUI.centered(g, "d" + FACES[k], x + w / 2f, Y0 + 6, PhoneUI.WHITE, .6f);
        }
        long since = System.currentTimeMillis() - thrownAt;
        boolean rolling = since < 800;
        int shown = rolling ? 1 + random.nextInt(faces) : result;
        float cx = (X0 + X1) / 2f, cy = Y0 + 80;
        float wobble = rolling ? (float) Math.sin(since / 40.0) * 8 : 0;
        g.pose().pushPose();
        g.pose().translate(cx, cy, 0);
        g.pose().mulPose(com.mojang.math.Axis.ZP.rotationDegrees(wobble * 3));
        PhoneUI.glow(g, 0, 0, 110, rolling ? 0x88FF6070 : 0x55FF6070);
        PhoneUI.round(g, -32, -32, 64, 64, 14, 0xFFF4EEE8);
        PhoneUI.round(g, -32, 0, 64, 32, 14, 0xFFE0D6CC);
        PhoneUI.round(g, -32, -4, 64, 8, 1, 0xFFEAE2D8);
        PhoneUI.centered(g, String.valueOf(shown), 0, -10, 0xFF8A1E2C, 2.6f);
        g.pose().popPose();
        if (!rolling && since < 1400) {
            PhoneUI.glow(g, cx, cy, 160 * (since - 800) / 600f, PhoneUI.alpha(0xFFFFD27A, 1 - (since - 800) / 600f));
        }
        button(g, (int) cx - 34, Y0 + 124, 68, 18, Component.translatable("phone.eraphones.throw").getString(), 0xFFE0405A, mx, my);
        int y = Y0 + 152;
        PhoneUI.text(g, Component.translatable("phone.eraphones.last_throws").getString().toUpperCase(), X0 + 10, y, PhoneUI.FAINT, .55f);
        y += 10;
        for (String s : last) {
            PhoneUI.text(g, s, X0 + 12, y, PhoneUI.DIM, .65f);
            y += 9;
        }
    }

    @Override
    public boolean click(double mx, double my, int button) {
        if (headerBack(mx, my)) {
            phone.home();
            return true;
        }
        int w = (X1 - X0 - 16) / FACES.length;
        for (int k = 0; k < FACES.length; k++) {
            if (PhoneUI.in(mx, my, X0 + 8 + k * w, Y0 + 2, w, 14)) {
                click();
                faces = FACES[k];
                return true;
            }
        }
        float cx = (X0 + X1) / 2f;
        if (PhoneUI.in(mx, my, cx - 34, Y0 + 124, 68, 18) || PhoneUI.in(mx, my, cx - 32, Y0 + 48, 64, 64)) {
            result = 1 + random.nextInt(faces);
            thrownAt = System.currentTimeMillis();
            mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.BONE_BLOCK_PLACE, 1.4f, .7f));
            last.add(0, "d" + faces + " → " + result);
            while (last.size() > 8) {
                last.remove(last.size() - 1);
            }
        }
        return true;
    }
}
