package com.waylays.arcanes.client.phone.apps;

import com.waylays.arcanes.client.phone.PhoneApp;
import com.waylays.arcanes.client.phone.PhoneSettings;
import com.waylays.arcanes.client.phone.PhoneUI;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import org.lwjgl.glfw.GLFW;

import java.util.List;

/** Notes: notes kept on this computer, their first line their title; one opens to write in (Enter for a new line). */
public class NotesApp extends PhoneApp {
    private int editing = -1;
    private StringBuilder text = new StringBuilder();
    private long typedAt;

    public NotesApp() {
        super("notes");
    }

    @Override
    public int[] background() {
        return new int[] {0xFF1C1A14, 0xFF100E0A};
    }

    @Override
    public void open() {
        editing = -1;
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        PhoneSettings s = PhoneSettings.get();
        if (editing >= 0) {
            header(g, Component.translatable("phone.arcanes_oublies.note"), mx, my, "✓");
            PhoneUI.round(g, X0 + 6, Y0 + 2, X1 - X0 - 12, Y1 - Y0 - 4, 8, 0xFFFFF6D6);
            for (int y = Y0 + 16; y < Y1 - 4; y += 7) {
                g.fill(X0 + 10, y, X1 - 10, y + 1, 0x22B08040);
            }
            String shown = text + ((System.currentTimeMillis() - typedAt) % 1000 < 500 ? "|" : " ");
            clip(g, Y0 + 2, Y1 - 2);
            int y = Y0 + 8;
            for (String para : shown.split("\n", -1)) {
                List<FormattedCharSequence> lines = PhoneUI.wrap(para.isEmpty() ? " " : para, X1 - X0 - 24, .7f);
                PhoneUI.lines(g, lines, X0 + 12, y, 0xFF3A2A10, .7f);
                y += lines.size() * 7;
            }
            unclip(g);
            return;
        }
        header(g, title(), mx, my, "+");
        if (s.notes.isEmpty()) {
            PhoneUI.icon(g, "notes", (X0 + X1) / 2f - 16, Y0 + 60, 32);
            PhoneUI.centered(g, Component.translatable("phone.arcanes_oublies.no_notes").getString(), (X0 + X1) / 2f, Y0 + 100, PhoneUI.DIM, .65f);
            return;
        }
        int y = Y0 + 4;
        for (int i = 0; i < s.notes.size(); i++) {
            String n = s.notes.get(i);
            String first = n.split("\n", 2)[0];
            String rest = n.contains("\n") ? n.substring(n.indexOf('\n') + 1).replace('\n', ' ') : "";
            boolean hover = PhoneUI.in(mx, my, X0 + 6, y, X1 - X0 - 12, 26);
            PhoneUI.round(g, X0 + 6, y, X1 - X0 - 12, 26, 8, hover ? 0x33FFFFFF : 0x1EFFFFFF);
            PhoneUI.text(g, PhoneUI.fit(first.isBlank() ? "…" : first, X1 - X0 - 30, .75f), X0 + 12, y + 5, PhoneUI.WHITE, .75f);
            PhoneUI.text(g, PhoneUI.fit(rest, X1 - X0 - 30, .6f), X0 + 12, y + 15, PhoneUI.FAINT, .6f);
            PhoneUI.right(g, "×", X1 - 12, y + 9, PhoneUI.FAINT, .8f);
            y += 29;
            if (y > Y1 - 26) {
                break;
            }
        }
    }

    private void save() {
        PhoneSettings s = PhoneSettings.get();
        if (editing >= 0 && editing < s.notes.size()) {
            if (text.toString().isBlank()) {
                s.notes.remove(editing);
            } else {
                s.notes.set(editing, text.toString());
            }
            s.save();
        }
        editing = -1;
    }

    @Override
    public boolean click(double mx, double my, int button) {
        PhoneSettings s = PhoneSettings.get();
        if (editing >= 0) {
            if (headerBack(mx, my) || headerAction(mx, my)) {
                click();
                save();
            }
            return true;
        }
        if (headerBack(mx, my)) {
            phone.home();
            return true;
        }
        if (headerAction(mx, my)) {
            click();
            s.notes.add(0, "");
            editing = 0;
            text = new StringBuilder();
            return true;
        }
        int y = Y0 + 4;
        for (int i = 0; i < s.notes.size(); i++) {
            if (PhoneUI.in(mx, my, X1 - 20, y, 14, 26)) {
                click();
                s.notes.remove(i);
                s.save();
                return true;
            }
            if (PhoneUI.in(mx, my, X0 + 6, y, X1 - X0 - 12, 26)) {
                click();
                editing = i;
                text = new StringBuilder(s.notes.get(i));
                return true;
            }
            y += 29;
        }
        return true;
    }

    @Override
    public boolean key(int key, int scan, int mods) {
        if (editing < 0) {
            return false;
        }
        typedAt = System.currentTimeMillis();
        if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) {
            text.append('\n');
            return true;
        }
        if (key == GLFW.GLFW_KEY_BACKSPACE && !text.isEmpty()) {
            text.setLength(text.length() - 1);
            return true;
        }
        return key != GLFW.GLFW_KEY_ESCAPE;
    }

    @Override
    public boolean typed(char c, int mods) {
        if (editing >= 0 && c >= ' ' && text.length() < 2000) {
            text.append(c);
            typedAt = System.currentTimeMillis();
            return true;
        }
        return false;
    }

    @Override
    public boolean back() {
        if (editing >= 0) {
            save();
            return true;
        }
        return false;
    }
}
