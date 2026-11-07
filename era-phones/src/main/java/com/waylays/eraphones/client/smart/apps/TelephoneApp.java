package com.waylays.eraphones.client.smart.apps;

import com.waylays.eraphones.Numbers;
import com.waylays.eraphones.client.PhoneClient;
import com.waylays.eraphones.client.PhoneHud;
import com.waylays.eraphones.client.PhoneUI;
import com.waylays.eraphones.client.smart.PhoneApp;
import com.waylays.eraphones.network.PhoneNet;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

/**
 * Téléphone: the keypad (typed or clicked, the number found in the directory as it is typed) and the green button,
 * the recent calls (made, received, missed in red, refused), each to call back.
 */
public class TelephoneApp extends PhoneApp {
    private static final String[] KEYS = {"1", "2", "3", "4", "5", "6", "7", "8", "9", "*", "0", "#"};
    private static final String[] LETTERS = {"", "ABC", "DEF", "GHI", "JKL", "MNO", "PQRS", "TUV", "WXYZ", "", "+", ""};
    private String typed = "";
    private boolean recents;

    public TelephoneApp() {
        super("telephone");
    }

    @Override
    public int[] background() {
        return new int[] {0xFF0C0E12, 0xFF050608};
    }

    @Override
    public void open() {
        PhoneClient.seen(id);
        PhoneClient.ask(9, "");
        scroll = 0;
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        header(g, title(), mx, my, null);
        // The two tabs
        int tw = (X1 - X0 - 20) / 2;
        for (int k = 0; k < 2; k++) {
            boolean on = recents == (k == 1);
            int x = X0 + 8 + k * (tw + 4);
            PhoneUI.round(g, x, Y0 + 1, tw, 13, 6, on ? 0x44FFFFFF : 0x14FFFFFF);
            PhoneUI.centered(g, Component.translatable(k == 0 ? "phone.eraphones.keypad" : "phone.eraphones.recents").getString(), x + tw / 2f, Y0 + 4,
                    on ? PhoneUI.WHITE : PhoneUI.DIM, .65f);
        }
        if (recents) {
            recents(g, mx, my);
        } else {
            keypad(g, mx, my);
        }
    }

    private void keypad(GuiGraphics g, int mx, int my) {
        float cx = (X0 + X1) / 2f;
        String shown = typed.isEmpty() ? "" : typed;
        PhoneUI.centered(g, PhoneUI.fit(shown, X1 - X0 - 16, 1.6f), cx, Y0 + 26, PhoneUI.WHITE, 1.6f);
        String n = Numbers.normalize(typed);
        if (n != null) {
            String name = PhoneClient.name(n);
            if (!name.equals(n)) {
                PhoneUI.centered(g, name, cx, Y0 + 42, 0xFF5CB4FF, .7f);
            }
        }
        for (int i = 0; i < 12; i++) {
            float kx = cx + (i % 3 - 1) * 40, ky = Y0 + 72 + i / 3 * 38;
            boolean hover = PhoneUI.in(mx, my, kx - 15, ky - 15, 30, 30);
            PhoneUI.round(g, (int) kx - 15, (int) ky - 15, 30, 30, 15, hover ? 0x55FFFFFF : 0x2EFFFFFF);
            PhoneUI.centered(g, KEYS[i], kx, ky - 7, PhoneUI.WHITE, 1.3f);
            if (!LETTERS[i].isEmpty()) {
                PhoneUI.centered(g, LETTERS[i], kx, ky + 5, PhoneUI.DIM, .45f);
            }
        }
        float by = Y0 + 72 + 4 * 38;
        boolean hc = PhoneUI.in(mx, my, cx - 15, by - 15, 30, 30);
        PhoneUI.round(g, (int) cx - 15, (int) by - 15, 30, 30, 15, hc ? 0xFF46D07A : 0xFF2EB85C);
        PhoneUI.centered(g, "✆", cx, by - 6, PhoneUI.WHITE, 1.3f);
        if (!typed.isEmpty()) {
            boolean hd = PhoneUI.in(mx, my, cx + 25, by - 15, 30, 30);
            PhoneUI.centered(g, "⌫", cx + 40, by - 5, hd ? PhoneUI.WHITE : PhoneUI.DIM, 1f);
        }
    }

    private void recents(GuiGraphics g, int mx, int my) {
        clip(g, Y0 + 18, Y1);
        int y = Y0 + 20 - (int) scroll;
        if (PhoneClient.calls.isEmpty()) {
            PhoneUI.paragraph(g, Component.translatable("phone.eraphones.no_calls").getString(), (X0 + X1) / 2f, Y0 + 60, X1 - X0 - 30, PhoneUI.DIM, .65f);
        }
        for (PhoneNet.CallView c : PhoneClient.calls) {
            boolean hover = PhoneUI.in(mx, my, X0 + 4, y, X1 - X0 - 8, 24);
            if (hover) {
                PhoneUI.round(g, X0 + 4, y, X1 - X0 - 8, 24, 7, 0x18FFFFFF);
            }
            String name = PhoneClient.name(c.number(), c.display());
            MessagesApp.avatar(g, name, c.number(), X0 + 10, y + 4, 16);
            int color = c.kind() == 2 ? 0xFFFF5C5C : PhoneUI.WHITE;
            PhoneUI.text(g, PhoneUI.fit(name, 86, .75f), X0 + 32, y + 4, color, .75f);
            String kind = Component.translatable("phone.eraphones.call_kind." + c.kind()).getString()
                    + (c.seconds() > 0 ? " · " + PhoneHud.timer(c.seconds() * 1000L) : "");
            PhoneUI.text(g, (c.kind() == 0 ? "↗ " : "↙ ") + kind, X0 + 32, y + 14, PhoneUI.FAINT, .55f);
            PhoneUI.right(g, PhoneUI.ago(c.time()), X1 - 24, y + 5, PhoneUI.FAINT, .55f);
            if (!Numbers.hidden(c.number())) {
                PhoneUI.right(g, "✆", X1 - 9, y + 7, 0xFF5CE08A, .9f);
            }
            y += 26;
        }
        unclip(g);
    }

    private void call() {
        String n = Numbers.normalize(typed);
        if (n != null) {
            click();
            PhoneClient.dial(n);
            typed = "";
        }
    }

    @Override
    public boolean click(double mx, double my, int button) {
        if (headerBack(mx, my)) {
            phone.home();
            return true;
        }
        int tw = (X1 - X0 - 20) / 2;
        if (PhoneUI.in(mx, my, X0 + 8, Y0 + 1, tw * 2 + 4, 13)) {
            click();
            recents = mx >= X0 + 8 + tw + 4;
            if (recents) {
                PhoneClient.ask(9, "");
            }
            return true;
        }
        if (recents) {
            int y = Y0 + 20 - (int) scroll;
            for (PhoneNet.CallView c : PhoneClient.calls) {
                if (PhoneUI.in(mx, my, X0 + 4, y, X1 - X0 - 8, 24) && my > Y0 + 18) {
                    if (!Numbers.hidden(c.number())) {
                        click();
                        PhoneClient.dial(c.number());
                    }
                    return true;
                }
                y += 26;
            }
            return true;
        }
        float cx = (X0 + X1) / 2f;
        for (int i = 0; i < 12; i++) {
            float kx = cx + (i % 3 - 1) * 40, ky = Y0 + 72 + i / 3 * 38;
            if (PhoneUI.in(mx, my, kx - 15, ky - 15, 30, 30)) {
                press(KEYS[i]);
                return true;
            }
        }
        float by = Y0 + 72 + 4 * 38;
        if (PhoneUI.in(mx, my, cx - 15, by - 15, 30, 30)) {
            call();
        } else if (PhoneUI.in(mx, my, cx + 25, by - 15, 30, 30) && !typed.isEmpty()) {
            typed = typed.substring(0, typed.length() - 1);
            click();
        }
        return true;
    }

    private void press(String k) {
        if (typed.length() < 12) {
            typed += k;
        }
        phone.sound(net.minecraft.sounds.SoundEvents.NOTE_BLOCK_BIT.value(), 1.4f + "123456789*0#".indexOf(k) * .04f);
    }

    @Override
    public boolean key(int key, int scan, int mods) {
        if (recents) {
            return false;
        }
        if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) {
            call();
            return true;
        }
        if (key == GLFW.GLFW_KEY_BACKSPACE && !typed.isEmpty()) {
            typed = typed.substring(0, typed.length() - 1);
            return true;
        }
        return false;
    }

    @Override
    public boolean typed(char c, int mods) {
        if (!recents && (Character.isDigit(c) || c == '*' || c == '#' || c == '-')) {
            if (c != '-') {
                press(String.valueOf(c));
            }
            return true;
        }
        return false;
    }

    /** Puts {@code number} on the keypad (from elsewhere). */
    public void prefill(String number) {
        typed = number.replace("-", "");
        recents = false;
    }
}
