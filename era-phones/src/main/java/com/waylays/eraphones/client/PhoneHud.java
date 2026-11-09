package com.waylays.eraphones.client;

import com.waylays.eraphones.Era;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

/**
 * What the phones show while put away: a call coming in (who calls, the key to answer), the call going on (the last
 * lines said, as subtitles), a notification sliding down, and the screen's edges going wrong when a phone is haunted.
 */
public final class PhoneHud {
    private PhoneHud() {}

    public static void render(GuiGraphics g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui || mc.player == null) {
            return;
        }
        boolean open = PhoneClient.phoneOpen();
        if (!open) {
            call(g, mc);
            toast(g);
        }
        if (PhoneClient.haunted() && !open) {
            haunt(g, mc);
        }
    }

    private static String key() {
        return PhoneClient.OPEN.getTranslatedKeyMessage().getString();
    }

    private static void call(GuiGraphics g, Minecraft mc) {
        int state = PhoneClient.callState;
        long now = System.currentTimeMillis();
        int w = g.guiWidth();
        if (state == 2) {
            // Coming in: a dark pill at the top, shaking a little in time with the ring
            float shake = Mth.sin(now / 40f) * (now % 1600 < 900 ? 1.2f : 0);
            int pw = 220, ph = 40, x = (int) (w / 2f - pw / 2f + shake), y = 6;
            PhoneUI.round(g, x - 1, y - 1, pw + 2, ph + 2, 21, 0x885CE08A);
            PhoneUI.round(g, x, y, pw, ph, 20, 0xF0101014);
            PhoneUI.round(g, x + 8, y + 8, 24, 24, 12, 0xFF2EA85A);
            PhoneUI.centered(g, "✆", x + 20, y + 13, 0xFFFFFFFF, 1.2f);
            PhoneUI.text(g, Component.translatable("phone.eraphones.incoming").getString(), x + 40, y + 8, 0xFF9AE6B4, .65f);
            PhoneUI.text(g, PhoneUI.fit(PhoneClient.name(PhoneClient.callNumber, PhoneClient.callDisplay), pw - 50, .9f), x + 40, y + 18, 0xFFFFFFFF, .9f);
            PhoneUI.right(g, Component.translatable("phone.eraphones.answer_key", key()).getString(), x + pw - 12, y + 8, 0xFFB8B4C8, .55f);
        } else if (state == 1 || state == 3) {
            // Calling or talking: a small green pill with the time, and the last lines said
            String label = state == 1 ? Component.translatable("phone.eraphones.calling").getString() : timer(now - PhoneClient.callSince);
            String who = PhoneUI.fit(PhoneClient.name(PhoneClient.callNumber, PhoneClient.callDisplay), 110, .65f);
            int pw = 40 + PhoneUI.width(who, .65f) + PhoneUI.width(label, .65f), x = w / 2 - pw / 2, y = 6;
            PhoneUI.round(g, x, y, pw, 16, 8, 0xE0101014);
            PhoneUI.round(g, x + 5, y + 5, 6, 6, 3, 0xFF5CE08A);
            PhoneUI.text(g, who, x + 15, y + 5, 0xFFFFFFFF, .65f);
            PhoneUI.right(g, label, x + pw - 8, y + 5, 0xFF9AE6B4, .65f);
            int ly = g.guiHeight() - 92;
            int shown = 0;
            for (int i = PhoneClient.SAID.size() - 1; i >= 0 && shown < 3; i--) {
                PhoneClient.Said s = PhoneClient.SAID.get(i);
                long age = now - s.at();
                if (age > 9000) {
                    break;
                }
                float a = 1 - PhoneUI.ease((age - 8000) / 1000f);
                String who2 = s.mine() ? Component.translatable("phone.eraphones.you").getString() : PhoneClient.name(PhoneClient.callNumber, PhoneClient.callDisplay);
                String line = "✆ " + who2 + " : " + s.text();
                int lw = Math.min(PhoneUI.width(line, .8f), w - 40);
                PhoneUI.round(g, w / 2 - lw / 2 - 5, ly - 2, lw + 10, 12, 4, PhoneUI.alpha(0xA0000000, a));
                PhoneUI.centered(g, PhoneUI.fit(line, w - 40, .8f), w / 2f, ly, PhoneUI.alpha(s.mine() ? 0xFFB8B4C8 : 0xFFFFFFFF, a), .8f);
                ly -= 14;
                shown++;
            }
            if (state == 3) {
                PhoneUI.centered(g, Component.translatable(PhoneClient.voiceChat() ? "phone.eraphones.voice_hint" : "phone.eraphones.speak_hint", key()).getString(),
                        w / 2f, g.guiHeight() - 72, PhoneClient.voiceChat() ? 0xCC9AE6B4 : 0x88FFFFFF, .55f);
            }
        } else if (now - PhoneClient.callEndedAt < 2500 && PhoneClient.callEndedAt > 0) {
            String r = Component.translatable("phone.eraphones.call_end." + PhoneClient.callReason).getString();
            int pw = PhoneUI.width(r, .65f) + 20, x = w / 2 - pw / 2;
            PhoneUI.round(g, x, 6, pw, 16, 8, 0xE0101014);
            PhoneUI.centered(g, r, w / 2f, 11, 0xFFFF8A8A, .65f);
        }
    }

    public static String timer(long ms) {
        long s = Math.max(0, ms / 1000);
        return String.format("%d:%02d", s / 60, s % 60);
    }

    /** A notification while the phone is put away, in the style of the phone in use. */
    private static void toast(GuiGraphics g) {
        PhoneClient.Note n = PhoneClient.fresh();
        if (n == null || PhoneClient.callState == 2) {
            return;
        }
        long age = System.currentTimeMillis() - n.shownAt();
        float in = PhoneUI.spring(age / 450f) * (1 - PhoneUI.ease((age - 3500) / 400f));
        if (in <= 0) {
            return;
        }
        int gw = g.guiWidth();
        if (PhoneClient.era == Era.SMART) {
            int w = (int) (60 + 150 * Math.max(0, in)), h = (int) (14 + 20 * Math.max(0, in));
            int x = gw / 2 - w / 2, y = 4;
            PhoneUI.round(g, x - 1, y - 1, w + 2, h + 2, h / 2 + 1, 0x66FFFFFF);
            PhoneUI.round(g, x, y, w, h, h / 2, 0xF0000000);
            if (in > .7f) {
                PhoneUI.icon(g, n.app(), x + 7, y + 7, 20);
                PhoneUI.text(g, PhoneUI.fit(n.title(), w - 70, .75f), x + 32, y + 8, 0xFFFFFFFF, .75f);
                PhoneUI.text(g, PhoneUI.fit(n.text(), w - 40, .65f), x + 32, y + 19, 0xFFB8B4C8, .65f);
                PhoneUI.right(g, key(), x + w - 10, y + 8, 0xFF8A86A0, .6f);
            }
            return;
        }
        // The older phones: a small screen-coloured box at the top right, sliding in
        int w = 150, h = 30;
        int x = (int) (gw - (w + 8) * Math.max(0, Math.min(1, in))), y = 8;
        int bg = PhoneClient.era == Era.GSM ? 0xF09DBE6A : PhoneClient.era == Era.FLIP ? 0xF01A3A7A : 0xF0200A0A;
        int fg = PhoneClient.era == Era.GSM ? 0xFF1E2A12 : PhoneClient.era == Era.FLIP ? 0xFFFFFFFF : 0xFFFF4A3A;
        PhoneUI.round(g, x - 1, y - 1, w + 2, h + 2, 4, 0xFF303030);
        PhoneUI.round(g, x, y, w, h, 3, bg);
        PhoneUI.text(g, "✉ " + PhoneUI.fit(n.title(), w - 20, .7f), x + 6, y + 5, fg, .7f);
        PhoneUI.text(g, PhoneUI.fit(n.text(), w - 12, .6f), x + 6, y + 17, fg, .6f);
    }

    /** Static creeping in at the edges, the screen flickering red, while a phone is haunted. */
    private static void haunt(GuiGraphics g, Minecraft mc) {
        long now = System.currentTimeMillis();
        int w = g.guiWidth(), h = g.guiHeight();
        java.util.Random r = new java.util.Random(now / 60);
        float left = (PhoneClient.hauntUntil - now) / 1000f;
        float a = Math.min(1, left) * .5f;
        for (int k = 0; k < 40; k++) {
            int x = r.nextInt(w), y = r.nextInt(h);
            boolean edge = x < 40 || x > w - 40 || y < 30 || y > h - 30;
            if (!edge) {
                continue;
            }
            int c = r.nextBoolean() ? 0xFFFFFF : 0x000000;
            g.fill(x, y, x + 2 + r.nextInt(14), y + 1, PhoneUI.alpha(0xFF000000 | c, a));
        }
        if (r.nextInt(12) == 0) {
            g.fill(0, 0, w, h, PhoneUI.alpha(0xFF400000, a * .4f));
        }
    }
}
