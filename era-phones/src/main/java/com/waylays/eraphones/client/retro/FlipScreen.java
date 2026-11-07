package com.waylays.eraphones.client.retro;

import com.waylays.eraphones.EraPhones;
import com.waylays.eraphones.Numbers;
import com.waylays.eraphones.client.PhoneClient;
import com.waylays.eraphones.client.PhoneHud;
import com.waylays.eraphones.client.PhoneSettings;
import com.waylays.eraphones.client.PhoneUI;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;

import javax.annotation.Nullable;
import java.nio.file.Path;
import java.util.List;

/**
 * The 2004 flip phone, opened: a brushed silver shell, a colour screen in the lid (a wallpaper, the time large, the
 * operator), and below the hinge two soft keys, a four-way pad round its OK, the green and the red keys and the digits.
 * Its menu is a grid of nine glossy icons; it has a small camera (photos of 160 by 120, a little washed out), a gallery,
 * polyphonic rings and colour themes.
 */
public class FlipScreen extends MenuPhone {
    private static final String[] ITEMS = {"messages", "contacts", "calls", "camera", "gallery", "snake", "ring", "calc", "settings"};
    private static final String GALLERY = "gallery", SETTINGS = "settings", THEME = "theme";
    /** Each theme: the bars' top and bottom colours, the highlight. */
    private static final int[][] THEMES = {{0xFF3A7AE0, 0xFF143A8A, 0xFF7AC0FF}, {0xFFE04A9A, 0xFF8A1A5A, 0xFFFF9AD0}, {0xFF3AB06A, 0xFF145A30, 0xFF8AF0B0},
            {0xFFE0802A, 0xFF8A3A0A, 0xFFFFC07A}};
    private static final String[] THEME_NAMES = {"blue", "pink", "green", "orange"};
    private int photo;

    public FlipScreen() {
        this(null);
    }

    public FlipScreen(@Nullable String app) {
        super("clapet", 140, 320);
        int silver = 0xFF8A929C, dark = 0xFF1A1E24, light = 0xFFF2F4F6;
        keys.add(new Key("soft1", 14, 176, 30, 11, "▬", "", silver, dark));
        keys.add(new Key("soft2", 96, 176, 30, 11, "▬", "", silver, dark));
        keys.add(new Key("up", 60, 171, 20, 11, "▲", "", silver, dark));
        keys.add(new Key("down", 60, 201, 20, 11, "▼", "", silver, dark));
        keys.add(new Key("left", 45, 183, 14, 17, "◀", "", silver, dark));
        keys.add(new Key("right", 81, 183, 14, 17, "▶", "", silver, dark));
        keys.add(new Key("ok", 60, 183, 20, 17, "OK", "", 0xFF5A6A80, light));
        keys.add(new Key("call", 14, 192, 30, 13, "✆", "", 0xFF3EA05A, light));
        keys.add(new Key("end", 96, 192, 30, 13, "✆", "", 0xFFC04040, light));
        String[] digits = {"1", "2", "3", "4", "5", "6", "7", "8", "9", "*", "0", "#"};
        String[] subs = {"", "ABC", "DEF", "GHI", "JKL", "MNO", "PQRS", "TUV", "WXYZ", "", "␣", ""};
        for (int i = 0; i < 12; i++) {
            keys.add(new Key(digits[i], 20 + i % 3 * 36, 218 + i / 3 * 21, 28, 16, digits[i], subs[i], silver, dark));
        }
        if ("photo".equals(app)) {
            go(GALLERY);
        }
    }

    @Override
    protected void opened() {
        PhoneClient.beep(SoundEvents.BOOK_PAGE_TURN, 1.6f, .6f);
    }

    @Override
    protected int[] screenBox() {
        return new int[] {20, 30, 120, 154};
    }

    @Override
    protected String[] menu() {
        return ITEMS;
    }

    private int[] theme() {
        return THEMES[Math.floorMod(PhoneSettings.get().theme, THEMES.length)];
    }

    @Override
    protected int fg() {
        return 0xFFFFFFFF;
    }

    @Override
    protected float textScale() {
        return .68f;
    }

    @Override
    protected int rows() {
        return (h - 22) / lineHeight();
    }

    @Override
    protected void flashBox(GuiGraphics g, int x, int y, int w) {
        PhoneUI.gradient(g, x, y, w, 14, 3, theme()[0], theme()[1]);
    }

    @Override
    protected void background(GuiGraphics g, long now) {
        if (view.equals(IDLE) && !PhoneClient.inCall()) {
            PhoneUI.blend();
            g.blit(EraPhones.id("textures/gui/phone/clapet_wall_" + THEME_NAMES[Math.floorMod(PhoneSettings.get().theme, 4)] + ".png"), x0, y0, w, h, 0, 0,
                    200, 248, 200, 248);
        } else {
            g.fillGradient(x0, y0, x0 + w, y0 + h, 0xFF101A2A, 0xFF05080E);
        }
        status(g);
    }

    /** The status bar: the network's bars, a letter waiting, the battery. */
    private void status(GuiGraphics g) {
        g.fill(x0, y0, x0 + w, y0 + 9, 0x88000000);
        int bars = PhoneClient.bars();
        for (int k = 0; k < 4; k++) {
            int bh = 2 + k;
            g.fill(x0 + 3 + k * 3, y0 + 7 - bh, x0 + 5 + k * 3, y0 + 7, bars > k ? 0xFFFFFFFF : 0x44FFFFFF);
        }
        if (PhoneClient.badge("messages") > 0) {
            PhoneUI.text(g, "✉", x0 + 18, y0 + 1.5f, 0xFFFFD050, .55f);
        }
        if (PhoneClient.silent) {
            PhoneUI.text(g, "⊘", x0 + 26, y0 + 1.5f, 0xFFFF9A9A, .55f);
        }
        PhoneUI.centered(g, PhoneClient.clock(), x0 + w / 2f, y0 + 1.5f, 0xFFFFFFFF, .55f);
        float b = PhoneClient.battery() / 1000f;
        g.fill(x0 + w - 15, y0 + 2, x0 + w - 4, y0 + 7, 0xFFFFFFFF);
        g.fill(x0 + w - 4, y0 + 3, x0 + w - 3, y0 + 6, 0xFFFFFFFF);
        g.fill(x0 + w - 14, y0 + 3, x0 + w - 5, y0 + 6, 0xFF101010);
        g.fill(x0 + w - 14, y0 + 3, x0 + w - 14 + Math.round(9 * b), y0 + 6, b < .2f ? 0xFFFF4040 : 0xFF6AE07A);
    }

    @Override
    protected void idle(GuiGraphics g, long now) {
        float cx = x0 + w / 2f;
        int bars = PhoneClient.bars();
        PhoneUI.centered(g, bars > 0 ? "VOXA" : tr("phone.eraphones.no_network_short"), cx, y0 + 14, 0xDDFFFFFF, .65f);
        String clock = PhoneClient.clock();
        PhoneUI.centered(g, clock, cx + 1, y0 + 33, 0x88000000, 2.2f);
        PhoneUI.centered(g, clock, cx, y0 + 32, 0xFFFFFFFF, 2.2f);
        PhoneUI.centered(g, PhoneUI.fit(PhoneClient.dateLine(), w - 6, .5f), cx, y0 + 54, 0xDDFFFFFF, .5f);
        int unread = PhoneClient.badge("messages"), missed = PhoneClient.badge("telephone");
        int y = y0 + 70;
        if (unread > 0) {
            PhoneUI.round(g, x0 + 10, y, w - 20, 12, 4, 0xAA000000);
            PhoneUI.centered(g, tr("phone.eraphones.new_messages", unread), cx, y + 3, 0xFFFFE070, .58f);
            y += 15;
        }
        if (missed > 0) {
            PhoneUI.round(g, x0 + 10, y, w - 20, 12, 4, 0xAA000000);
            PhoneUI.centered(g, tr("phone.eraphones.missed_short", missed), cx, y + 3, 0xFFFF9A9A, .58f);
        }
        softkeys(g, tr("phone.eraphones.menu"), tr("phone.eraphones.directory"));
    }

    @Override
    protected void menuScreen(GuiGraphics g, long now) {
        int[] t = theme();
        PhoneUI.gradient(g, x0, y0 + 9, w, 11, 0, t[0], t[1]);
        PhoneUI.centered(g, tr("phone.eraphones.menu." + ITEMS[sel]), x0 + w / 2f, y0 + 11.5f, 0xFFFFFFFF, .65f);
        int cw = w / 3, ch = 30;
        for (int i = 0; i < 9; i++) {
            int cx = x0 + i % 3 * cw, cy = y0 + 22 + i / 3 * ch;
            if (i == sel) {
                float pulse = .5f + .5f * (float) Math.sin(now / 200.0);
                PhoneUI.round(g, cx + 2, cy + 1, cw - 4, ch - 2, 5, PhoneUI.alpha(t[2], .35f + .25f * pulse));
            }
            PhoneUI.blend();
            int s = i == sel ? 26 : 22;
            g.blit(EraPhones.id("textures/gui/phone/clapet_" + ITEMS[i] + ".png"), cx + cw / 2 - s / 2, cy + ch / 2 - s / 2, s, s, 0, 0, 32, 32, 32, 32);
        }
        softkeys(g, tr("phone.eraphones.select"), tr("phone.eraphones.back"));
    }

    @Override
    protected void header(GuiGraphics g, String title) {
        int[] t = theme();
        PhoneUI.gradient(g, x0, y0 + 9, w, 11, 0, t[0], t[1]);
        PhoneUI.centered(g, PhoneUI.fit(title, w - 6, .65f), x0 + w / 2f, y0 + 11.5f, 0xFFFFFFFF, .65f);
    }

    @Override
    protected void list(GuiGraphics g, List<String> items, List<String> right, int selected, int top) {
        int lh = lineHeight() + 2, y = y0 + 22;
        int[] t = theme();
        int rows = (h - 34) / lh;
        if (selected < top) {
            top = selected;
        } else if (selected >= top + rows) {
            top = selected - rows + 1;
        }
        for (int i = top; i < items.size() && i < top + rows; i++) {
            boolean on = i == selected;
            if (on) {
                PhoneUI.gradient(g, x0 + 2, y - 1, w - 4, lh, 3, t[2], t[0]);
            }
            PhoneUI.text(g, PhoneUI.fit(items.get(i), w - 22, textScale()), x0 + 5, y + 1, on ? 0xFF0A1020 : 0xFFE8EEF8, textScale());
            if (i < right.size() && !right.get(i).isEmpty()) {
                PhoneUI.right(g, right.get(i), x0 + w - 5, y + 1, on ? 0xFF0A1020 : 0xFFB0C0D8, textScale());
            }
            y += lh;
        }
    }

    @Override
    protected void softkeys(GuiGraphics g, String left, String right) {
        int[] t = theme();
        PhoneUI.gradient(g, x0, y0 + h - 11, w, 11, 0, t[1], 0xFF000000);
        PhoneUI.text(g, left, x0 + 4, y0 + h - 8, 0xFFFFFFFF, .6f);
        PhoneUI.right(g, right, x0 + w - 4, y0 + h - 8, 0xFFFFFFFF, .6f);
    }

    @Override
    protected void paragraph(GuiGraphics g, List<String> lines, int top) {
        int y = y0 + 23;
        for (String l : lines) {
            PhoneUI.text(g, PhoneUI.fit(l, w - 8, textScale()), x0 + 4, y, 0xFFE8EEF8, textScale());
            y += lineHeight();
        }
    }

    @Override
    protected void callScreen(GuiGraphics g, long now) {
        float cx = x0 + w / 2f;
        int st = PhoneClient.callState;
        boolean hidden = Numbers.hidden(PhoneClient.callNumber);
        String who = PhoneClient.name(PhoneClient.callNumber, PhoneClient.callDisplay);
        int[] t = theme();
        g.fillGradient(x0, y0 + 9, x0 + w, y0 + h, hidden ? 0xFF3A0808 : t[1], 0xFF000000);
        float pulse = (now % 1200) / 1200f;
        if (st == 1 || st == 2) {
            PhoneUI.glow(g, cx, y0 + 46, 40 + pulse * 30, PhoneUI.alpha(t[2], (1 - pulse) * .6f));
        }
        if (hidden) {
            PhoneUI.round(g, (int) cx - 14, y0 + 32, 28, 28, 6, 0xFF100000);
            PhoneUI.centered(g, "?", cx, y0 + 40, 0xFFFF3030, 1.6f);
        } else {
            PhoneUI.avatar(g, who, PhoneClient.callNumber, (int) cx - 14, y0 + 32, 28);
        }
        PhoneUI.centered(g, PhoneUI.fit(who, w - 8, .75f), cx, y0 + 66, 0xFFFFFFFF, .75f);
        String status = switch (st) {
            case 1 -> tr("phone.eraphones.calling") + ".".repeat((int) (now / 400 % 4));
            case 2 -> tr("phone.eraphones.incoming");
            case 3 -> PhoneHud.timer(now - PhoneClient.callSince);
            default -> endReason();
        };
        PhoneUI.centered(g, PhoneUI.fit(status, w - 8, .62f), cx, y0 + 78, st == 3 ? 0xFF9AF0B4 : 0xFFD0D8E8, .62f);
        if (st == 2) {
            softkeys(g, tr("phone.eraphones.accept"), tr("phone.eraphones.decline"));
        } else if (st != 0) {
            softkeys(g, "", tr("phone.eraphones.end"));
        }
    }

    @Override
    protected void snakeScreen(GuiGraphics g, long now) {
        snake.update(now);
        snakeScore();
        int[] t = theme();
        PhoneUI.gradient(g, x0, y0 + 9, w, 11, 0, t[0], t[1]);
        PhoneUI.text(g, tr("phone.eraphones.score", snake.score), x0 + 4, y0 + 11.5f, 0xFFFFFFFF, .6f);
        PhoneUI.right(g, tr("phone.eraphones.best_short", PhoneSettings.get().snakeBest), x0 + w - 4, y0 + 11.5f, 0xFFFFFFFF, .55f);
        int cell = 3, gx = x0 + (w - snakeW() * cell) / 2, gy = y0 + 22;
        g.fill(gx - 1, gy - 1, gx + snakeW() * cell + 1, gy + snakeH() * cell + 1, 0xFF2A6A2A);
        g.fill(gx, gy, gx + snakeW() * cell, gy + snakeH() * cell, 0xFF0A200A);
        int i = 0;
        for (int[] p : snake.body) {
            int c = i++ == 0 ? 0xFFB0FF60 : 0xFF60D040;
            g.fill(gx + p[0] * cell, gy + p[1] * cell, gx + p[0] * cell + cell, gy + p[1] * cell + cell, c);
        }
        if (snake.food != null) {
            g.fill(gx + snake.food[0] * cell, gy + snake.food[1] * cell, gx + snake.food[0] * cell + cell, gy + snake.food[1] * cell + cell, 0xFFFF4040);
        }
        if (snake.over) {
            PhoneUI.round(g, x0 + 10, y0 + 50, w - 20, 22, 4, 0xE0000000);
            PhoneUI.centered(g, tr("phone.eraphones.game_over"), x0 + w / 2f, y0 + 54, 0xFFFF7070, .7f);
            PhoneUI.centered(g, tr("phone.eraphones.play_again_short"), x0 + w / 2f, y0 + 63, 0xFFD0D0D0, .5f);
        }
    }

    @Override
    protected int snakeW() {
        return 32;
    }

    @Override
    protected int snakeH() {
        return 30;
    }

    // ---------------------------------------------------------------- the camera, the gallery, the settings

    @Override
    protected boolean openExtra(String id) {
        switch (id) {
            case "camera" -> {
                PhoneClient.photo();
                return true;
            }
            case "gallery" -> {
                go(GALLERY);
                photo = 0;
                return true;
            }
            case "settings" -> {
                go(SETTINGS);
                return true;
            }
            default -> {
                return false;
            }
        }
    }

    @Override
    protected boolean extraScreen(GuiGraphics g, long now) {
        switch (view) {
            case GALLERY -> {
                List<Path> files = PhoneClient.photoFiles();
                header(g, tr("phone.eraphones.menu.gallery") + (files.isEmpty() ? "" : " " + (photo + 1) + "/" + files.size()));
                if (files.isEmpty()) {
                    paragraph(g, List.of(tr("phone.eraphones.empty")), 0);
                } else {
                    photo = Math.floorMod(photo, files.size());
                    ResourceLocation tex = PhoneClient.thumb(files.get(photo));
                    if (tex != null) {
                        int pw = w - 8, ph = pw * 3 / 4;
                        PhoneUI.blend();
                        g.blit(tex, x0 + 4, y0 + 24, pw, ph, 0, 0, 128, 96, 128, 96);
                    }
                }
                softkeys(g, "◀ ▶", tr("phone.eraphones.back"));
                return true;
            }
            case SETTINGS -> {
                header(g, tr("phone.eraphones.menu.settings"));
                list(g, List.of(tr("phone.eraphones.theme"), tr("phone.eraphones.my_number")), List.of("", PhoneClient.number), sel, 0);
                softkeys(g, tr("phone.eraphones.select"), tr("phone.eraphones.back"));
                return true;
            }
            case THEME -> {
                header(g, tr("phone.eraphones.theme"));
                List<String> names = new java.util.ArrayList<>();
                List<String> checks = new java.util.ArrayList<>();
                for (int k = 0; k < THEME_NAMES.length; k++) {
                    names.add(tr("phone.eraphones.theme." + THEME_NAMES[k]));
                    checks.add(PhoneSettings.get().theme == k ? "✓" : "");
                }
                list(g, names, checks, sel, 0);
                softkeys(g, tr("phone.eraphones.select"), tr("phone.eraphones.back"));
                return true;
            }
            default -> {
                return false;
            }
        }
    }

    @Override
    protected boolean extraPress(String id) {
        switch (view) {
            case GALLERY -> {
                List<Path> files = PhoneClient.photoFiles();
                if (id.equals("left") || id.equals("up") || id.equals("4")) {
                    photo--;
                } else if (id.equals("right") || id.equals("down") || id.equals("6")) {
                    photo++;
                } else if (id.equals("end")) {
                    return false;
                } else if (id.equals("back")) {
                    go(MENU);
                    sel = 4;
                } else if (id.equals("ok") && files.isEmpty()) {
                    PhoneClient.photo();
                }
                return true;
            }
            case SETTINGS -> {
                if (id.equals("up") || id.equals("down")) {
                    sel = 1 - sel;
                } else if (id.equals("ok")) {
                    if (sel == 0) {
                        go(THEME);
                        sel = Math.floorMod(PhoneSettings.get().theme, 4);
                    } else {
                        go(NUMBER);
                    }
                } else if (id.equals("end")) {
                    return false;
                } else if (id.equals("back")) {
                    go(MENU);
                    sel = 8;
                }
                return true;
            }
            case THEME -> {
                if (id.equals("up")) {
                    sel = Math.floorMod(sel - 1, 4);
                } else if (id.equals("down")) {
                    sel = Math.floorMod(sel + 1, 4);
                } else if (id.equals("ok")) {
                    PhoneSettings s = PhoneSettings.get();
                    s.theme = sel;
                    s.save();
                } else if (id.equals("end")) {
                    return false;
                } else if (id.equals("back")) {
                    go(SETTINGS);
                }
                return true;
            }
            default -> {
                return false;
            }
        }
    }

    @Override
    protected void press(String id) {
        // The soft keys say what the screen shows above them
        if (id.equals("soft1")) {
            id = "ok";
        } else if (id.equals("soft2")) {
            id = view.equals(IDLE) ? "right" : "back";
        }
        if (view.equals(MENU) && (id.equals("up") || id.equals("down"))) {
            sel = Math.floorMod(sel + (id.equals("up") ? -3 : 3), 9);
            return;
        }
        super.press(id);
    }

    @Override
    protected void click(String id) {
        // On a call coming in, the left soft key answers too, the right one refuses
        if (PhoneClient.callState == 2 && id.equals("soft1")) {
            id = "call";
        } else if (PhoneClient.callState != 0 && id.equals("soft2")) {
            id = "end";
        }
        super.click(id);
    }
}
