package com.waylays.eraphones.client.retro;

import com.waylays.eraphones.EraPhones;
import com.waylays.eraphones.Numbers;
import com.waylays.eraphones.client.PhoneClient;
import com.waylays.eraphones.client.PhoneHud;
import com.waylays.eraphones.client.PhoneSettings;
import com.waylays.eraphones.client.PhoneUI;
import net.minecraft.client.gui.GuiGraphics;

import java.util.List;

/**
 * The 1997 GSM handset: a charcoal candybar with a stub aerial, a green backlit screen of dark pixels (the network's
 * bars standing on its left, the battery's on its right, the operator's name in the middle), one menu key between the
 * green and the red, C to go back, two arrows, and rubber digits that write letters when pressed again.
 */
public class GsmScreen extends MenuPhone {
    private static final String[] ITEMS = {"messages", "contacts", "calls", "snake", "calc", "ring", "number"};
    private static final int[] CONTRAST = {0xFFB4CC84, 0xFFA2BE70, 0xFF94B262, 0xFF84A452};

    public GsmScreen() {
        super("gsm", 120, 260);
        int gray = 0xFF4E565E, light = 0xFFE8ECEE;
        keys.add(new Key("call", 16, 114, 24, 13, "✆", "", 0xFF3E8A4E, light));
        keys.add(new Key("ok", 44, 114, 32, 13, "MENU", "", 0xFF6A747E, light));
        keys.add(new Key("end", 80, 114, 24, 13, "✆", "", 0xFFA43A3A, light));
        keys.add(new Key("back", 16, 131, 24, 12, "C", "", gray, light));
        keys.add(new Key("up", 48, 131, 24, 12, "▲", "", gray, light));
        keys.add(new Key("down", 80, 131, 24, 12, "▼", "", gray, light));
        String[] digits = {"1", "2", "3", "4", "5", "6", "7", "8", "9", "*", "0", "#"};
        String[] subs = {"", "ABC", "DEF", "GHI", "JKL", "MNO", "PQRS", "TUV", "WXYZ", "", "␣", ""};
        for (int i = 0; i < 12; i++) {
            keys.add(new Key(digits[i], 16 + i % 3 * 32, 150 + i / 3 * 18, 24, 15, digits[i], subs[i], gray, light));
        }
    }

    @Override
    protected int[] screenBox() {
        return new int[] {18, 46, 102, 106};
    }

    @Override
    protected String[] menu() {
        return ITEMS;
    }

    private int lcd() {
        return CONTRAST[Math.floorMod(PhoneSettings.get().contrast, CONTRAST.length)];
    }

    @Override
    protected int fg() {
        return 0xFF1C2812;
    }

    @Override
    protected float textScale() {
        return .62f;
    }

    @Override
    protected void flashBox(GuiGraphics g, int x, int y, int w) {
        g.fill(x, y, x + w, y + 14, lcd());
    }

    @Override
    protected void background(GuiGraphics g, long now) {
        g.fill(x0, y0, x0 + w, y0 + h, lcd());
        // The faint grid of the pixels and a shade under the bezel's top
        for (int x = x0; x < x0 + w; x += 2) {
            g.fill(x, y0, x + 1, y0 + h, 0x08000000);
        }
        g.fillGradient(x0, y0, x0 + w, y0 + 6, 0x30000000, 0x00000000);
    }

    @Override
    protected void idle(GuiGraphics g, long now) {
        int bars = PhoneClient.bars();
        // The network: bars standing up the left side, the aerial on top
        PhoneUI.text(g, "Y", x0 + 2, y0 + 2, fg(), .55f);
        for (int k = 0; k < 4; k++) {
            int bw = 2 + k;
            if (bars > k) {
                g.fill(x0 + 2, y0 + 40 - k * 7, x0 + 2 + bw, y0 + 45 - k * 7, fg());
            }
        }
        // The battery: bars standing up the right side
        int level = Math.round(PhoneClient.battery() / 250f);
        PhoneUI.right(g, "▮", x0 + w - 2, y0 + 2, fg(), .55f);
        for (int k = 0; k < 4; k++) {
            int bw = 2 + k;
            if (level > k) {
                g.fill(x0 + w - 2 - bw, y0 + 40 - k * 7, x0 + w - 2, y0 + 45 - k * 7, fg());
            }
        }
        float cx = x0 + w / 2f;
        PhoneUI.centered(g, bars > 0 ? "VOXA" : tr("phone.eraphones.no_network_short"), cx, y0 + 12, fg(), .8f);
        PhoneUI.centered(g, PhoneClient.clock(), cx, y0 + 24, fg(), .75f);
        int unread = PhoneClient.badge("messages"), missed = PhoneClient.badge("telephone");
        if (unread > 0) {
            PhoneUI.centered(g, "✉ " + unread, cx, y0 + 34, fg(), .6f);
        } else if (missed > 0) {
            PhoneUI.centered(g, tr("phone.eraphones.missed_short", missed), cx, y0 + 34, fg(), .55f);
        }
        softkeys(g, tr("phone.eraphones.menu"), "");
    }

    @Override
    protected void menuScreen(GuiGraphics g, long now) {
        String id = ITEMS[sel];
        PhoneUI.right(g, (sel + 1) + "", x0 + w - 3, y0 + 2, fg(), .55f);
        PhoneUI.blend();
        g.setColor(0.11f, 0.16f, 0.07f, 1);
        g.blit(EraPhones.id("textures/gui/phone/gsm_" + id + ".png"), (int) (x0 + w / 2f - 15), y0 + 10, 30, 20, 0, 0, 24, 16, 24, 16);
        g.setColor(1, 1, 1, 1);
        PhoneUI.centered(g, tr("phone.eraphones.menu." + id), x0 + w / 2f, y0 + 36, fg(), .7f);
        softkeys(g, tr("phone.eraphones.select"), "");
    }

    @Override
    protected void header(GuiGraphics g, String title) {
        PhoneUI.text(g, PhoneUI.fit(title, w - 6, textScale()), x0 + 3, y0 + 2, fg(), textScale());
        g.fill(x0 + 2, y0 + 9, x0 + w - 2, y0 + 10, fg());
    }

    @Override
    protected void list(GuiGraphics g, List<String> items, List<String> right, int selected, int top) {
        int lh = lineHeight(), y = y0 + 12;
        for (int i = top; i < items.size() && i < top + rows(); i++) {
            boolean on = i == selected;
            if (on) {
                g.fill(x0 + 1, y - 1, x0 + w - 1, y + lh - 1, fg());
            }
            int c = on ? lcd() : fg();
            PhoneUI.text(g, PhoneUI.fit(items.get(i), w - 18, textScale()), x0 + 3, y, c, textScale());
            if (i < right.size() && !right.get(i).isEmpty()) {
                PhoneUI.right(g, right.get(i), x0 + w - 3, y, c, textScale());
            }
            y += lh;
        }
    }

    @Override
    protected void softkeys(GuiGraphics g, String left, String right) {
        if (!left.isEmpty()) {
            PhoneUI.centered(g, left, x0 + w / 2f, y0 + h - 7, fg(), .58f);
        }
    }

    @Override
    protected void paragraph(GuiGraphics g, List<String> lines, int top) {
        int y = y0 + 12;
        for (String l : lines) {
            PhoneUI.text(g, PhoneUI.fit(l, w - 6, textScale()), x0 + 3, y, fg(), textScale());
            y += lineHeight();
        }
    }

    @Override
    protected void callScreen(GuiGraphics g, long now) {
        float cx = x0 + w / 2f;
        int st = PhoneClient.callState;
        String who = PhoneClient.name(PhoneClient.callNumber, PhoneClient.callDisplay);
        switch (st) {
            case 2 -> {
                if (now % 800 < 500) {
                    PhoneUI.centered(g, tr("phone.eraphones.call_short"), cx, y0 + 8, fg(), .9f);
                }
                PhoneUI.centered(g, PhoneUI.fit(who, w - 6, .65f), cx, y0 + 26, fg(), .65f);
                softkeys(g, tr("phone.eraphones.accept"), "");
            }
            case 1 -> {
                PhoneUI.centered(g, tr("phone.eraphones.calling") + ".".repeat((int) (now / 400 % 4)), cx, y0 + 12, fg(), .62f);
                PhoneUI.centered(g, PhoneUI.fit(who, w - 6, .65f), cx, y0 + 26, fg(), .65f);
                softkeys(g, tr("phone.eraphones.end"), "");
            }
            case 3 -> {
                PhoneUI.centered(g, PhoneUI.fit(who, w - 6, .65f), cx, y0 + 10, fg(), .65f);
                PhoneUI.centered(g, PhoneHud.timer(now - PhoneClient.callSince), cx, y0 + 24, fg(), .9f);
                softkeys(g, tr("phone.eraphones.end"), "");
            }
            default -> {
                PhoneUI.centered(g, tr("phone.eraphones.call_over"), cx, y0 + 12, fg(), .65f);
                PhoneUI.centered(g, PhoneUI.fit(endReason(), w - 6, .58f), cx, y0 + 26, fg(), .58f);
                if (PhoneClient.callSince > 0 && !Numbers.hidden(PhoneClient.callNumber)) {
                    PhoneUI.centered(g, PhoneHud.timer(PhoneClient.callSince * 1000L), cx, y0 + 38, fg(), .6f);
                }
            }
        }
    }

    @Override
    protected void snakeScreen(GuiGraphics g, long now) {
        snake.update(now);
        snakeScore();
        PhoneUI.text(g, String.valueOf(snake.score), x0 + 3, y0 + 2, fg(), .55f);
        PhoneUI.right(g, tr("phone.eraphones.best_short", PhoneSettings.get().snakeBest), x0 + w - 3, y0 + 2, fg(), .5f);
        int gx = x0 + 2, gy = y0 + 9;
        g.fill(gx - 1, gy - 1, gx + snakeW() * 2 + 1, gy, fg());
        g.fill(gx - 1, gy + snakeH() * 2, gx + snakeW() * 2 + 1, gy + snakeH() * 2 + 1, fg());
        for (int[] p : snake.body) {
            g.fill(gx + p[0] * 2, gy + p[1] * 2, gx + p[0] * 2 + 2, gy + p[1] * 2 + 2, fg());
        }
        if (snake.food != null && now % 600 < 450) {
            g.fill(gx + snake.food[0] * 2, gy + snake.food[1] * 2, gx + snake.food[0] * 2 + 2, gy + snake.food[1] * 2 + 2, fg());
        }
        if (snake.over) {
            g.fill(x0 + 8, y0 + 22, x0 + w - 8, y0 + 40, lcd());
            g.fill(x0 + 8, y0 + 22, x0 + w - 8, y0 + 23, fg());
            g.fill(x0 + 8, y0 + 39, x0 + w - 8, y0 + 40, fg());
            PhoneUI.centered(g, tr("phone.eraphones.game_over"), x0 + w / 2f, y0 + 27, fg(), .65f);
        }
    }

    @Override
    protected void click(String id) {
        // The menu key says "End" during a call
        if ((PhoneClient.callState == 1 || PhoneClient.callState == 3) && id.equals("ok")) {
            id = "end";
        }
        super.click(id);
    }

    @Override
    protected int snakeW() {
        return 40;
    }

    @Override
    protected int snakeH() {
        return 25;
    }
}
