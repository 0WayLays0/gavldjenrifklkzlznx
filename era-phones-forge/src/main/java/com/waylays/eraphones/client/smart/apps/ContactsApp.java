package com.waylays.eraphones.client.smart.apps;

import com.waylays.eraphones.Numbers;
import com.waylays.eraphones.client.PhoneClient;
import com.waylays.eraphones.client.PhoneUI;
import com.waylays.eraphones.client.smart.PhoneApp;
import com.waylays.eraphones.network.PhoneNet;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

/**
 * Contacts: the directory of this line (a name written by its owner, a number), "my number" at its top, a switch to
 * share one's position with one's contacts; a new contact is a name and a number; a contact opens to call, write, find
 * on the map (if they share their position with this number) or remove.
 */
public class ContactsApp extends PhoneApp {
    private EditBox name;
    private EditBox number;
    private String chosen;

    public ContactsApp() {
        super("contacts");
    }

    @Override
    public int[] background() {
        return new int[] {0xFF12161C, 0xFF080A0E};
    }

    @Override
    public void open() {
        name = field(X0 + 14, Y0 + 44, 62, PhoneNet.NAME, "phone.eraphones.contact_name");
        number = field(X0 + 86, Y0 + 44, 44, 8, "phone.eraphones.contact_number");
        chosen = null;
        scroll = 0;
        PhoneClient.ask(10, "");
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        header(g, title(), mx, my, null);
        // My number, and the switch to share my position
        PhoneUI.glass(g, X0 + 6, Y0 + 2, X1 - X0 - 12, 34, 9, 0x22FFFFFF);
        PhoneUI.text(g, Component.translatable("phone.eraphones.my_number").getString(), X0 + 12, Y0 + 7, PhoneUI.DIM, .55f);
        PhoneUI.text(g, PhoneClient.number, X0 + 12, Y0 + 15, PhoneUI.WHITE, .85f);
        PhoneUI.text(g, Component.translatable("phone.eraphones.share_position").getString(), X0 + 12, Y0 + 27, PhoneUI.DIM, .5f);
        toggle(g, X1 - 30, Y0 + 24, PhoneClient.sharing);
        drawField(g, name, mx, my, pt);
        drawField(g, number, mx, my, pt);
        button(g, X1 - 26, Y0 + 41, 18, 13, "+", 0xFF2E8CFF, mx, my);
        clip(g, Y0 + 58, Y1);
        int y = Y0 + 60 - (int) scroll;
        PhoneUI.text(g, Component.translatable("phone.eraphones.directory").getString().toUpperCase(), X0 + 10, y, PhoneUI.FAINT, .55f);
        y += 9;
        if (PhoneClient.contacts.isEmpty()) {
            PhoneUI.paragraph(g, Component.translatable("phone.eraphones.no_contacts").getString(), (X0 + X1) / 2f, y + 6, X1 - X0 - 30, PhoneUI.DIM, .6f);
        }
        for (PhoneNet.ContactView p : PhoneClient.contacts) {
            y = row(g, p, y, mx, my);
        }
        unclip(g);
    }

    public static void toggle(GuiGraphics g, int x, int y, boolean on) {
        PhoneUI.round(g, x, y, 20, 10, 5, on ? 0xFF5CE08A : 0x66FFFFFF);
        PhoneUI.round(g, on ? x + 11 : x + 1, y + 1, 8, 8, 4, 0xFFFFFFFF);
    }

    private int row(GuiGraphics g, PhoneNet.ContactView p, int y, int mx, int my) {
        boolean open = p.number().equals(chosen);
        int h = open ? 44 : 24;
        boolean hover = PhoneUI.in(mx, my, X0 + 4, y, X1 - X0 - 8, 24);
        PhoneUI.round(g, X0 + 4, y, X1 - X0 - 8, h, 8, open ? 0x2AFFFFFF : hover ? 0x18FFFFFF : 0);
        PhoneUI.avatar(g, p.name(), p.number(), X0 + 10, y + 4, 16);
        PhoneUI.text(g, PhoneUI.fit(p.name(), 90, .75f), X0 + 32, y + 5, PhoneUI.WHITE, .75f);
        PhoneUI.text(g, p.number() + (p.sharing() ? "  · ⌖" : ""), X0 + 32, y + 14, PhoneUI.FAINT, .55f);
        if (open) {
            int bw = (X1 - X0 - 28) / 4;
            button(g, X0 + 8, y + 27, bw, 13, "✆", 0xFF2EB85C, mx, my);
            button(g, X0 + 12 + bw, y + 27, bw, 13, "✉", 0xFF2E8CFF, mx, my);
            button(g, X0 + 16 + 2 * bw, y + 27, bw, 13, "⌖", p.sharing() ? 0xFF6A5CFF : 0xFF3A3A48, mx, my);
            button(g, X0 + 20 + 3 * bw, y + 27, bw, 13, "✕", 0xFFC8344A, mx, my);
        }
        return y + h + 3;
    }

    @Override
    public boolean click(double mx, double my, int button) {
        if (headerBack(mx, my)) {
            phone.home();
            return true;
        }
        if (PhoneUI.in(mx, my, X1 - 32, Y0 + 22, 26, 14)) {
            click();
            PhoneClient.sharing = !PhoneClient.sharing;
            PhoneClient.ask(PhoneClient.sharing ? 3 : 4, "");
            return true;
        }
        if (PhoneUI.in(mx, my, X1 - 26, Y0 + 41, 18, 13)) {
            addTyped();
            return true;
        }
        if (clickField(name, mx, my, button) | clickField(number, mx, my, button)) {
            return true;
        }
        if (my < Y0 + 58) {
            return true;
        }
        int y = Y0 + 60 - (int) scroll + 9;
        for (PhoneNet.ContactView p : PhoneClient.contacts) {
            boolean open = p.number().equals(chosen);
            int h = open ? 44 : 24;
            if (open && PhoneUI.in(mx, my, X0 + 4, y + 26, X1 - X0 - 8, 16)) {
                int bw = (X1 - X0 - 28) / 4;
                click();
                int k = (int) Math.max(0, Math.min(3, (mx - X0 - 8) / (bw + 4)));
                switch (k) {
                    case 0 -> PhoneClient.dial(p.number());
                    case 1 -> {
                        phone.open("messages", (X0 + X1) / 2f, y + 30);
                        if (phone.current() instanceof MessagesApp m) {
                            m.openWith(p.number());
                        }
                    }
                    case 2 -> {
                        phone.open("carte", (X0 + X1) / 2f, y + 30);
                        if (phone.current() instanceof CarteApp c) {
                            c.focus(p.number());
                        }
                    }
                    default -> {
                        PhoneClient.send(new PhoneNet.ContactEdit(p.name(), p.number(), true));
                        chosen = null;
                    }
                }
                return true;
            }
            if (PhoneUI.in(mx, my, X0 + 4, y, X1 - X0 - 8, 24)) {
                click();
                chosen = open ? null : p.number();
                return true;
            }
            y += h + 3;
        }
        return true;
    }

    private void addTyped() {
        String n = name.getValue().strip();
        String num = Numbers.normalize(number.getValue());
        if (!n.isEmpty() && num != null) {
            click();
            PhoneClient.send(new PhoneNet.ContactEdit(n, num, false));
            name.setValue("");
            number.setValue("");
        } else if (num == null && !number.getValue().isBlank()) {
            number.setTextColor(0xFFFF7A7A);
        }
    }

    @Override
    public boolean key(int key, int scan, int mods) {
        if ((name.isFocused() || number.isFocused()) && (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER)) {
            addTyped();
            return true;
        }
        if (key == GLFW.GLFW_KEY_TAB) {
            boolean n = name.isFocused();
            name.setFocused(!n);
            number.setFocused(n);
            return true;
        }
        number.setTextColor(PhoneUI.WHITE);
        return key != GLFW.GLFW_KEY_ESCAPE && (feed(name, key, scan, mods) || feed(number, key, scan, mods));
    }

    @Override
    public boolean typed(char c, int mods) {
        if (number.isFocused() && !(Character.isDigit(c) || c == '-' || c == ' ')) {
            return true;
        }
        return feed(name, c, mods) || feed(number, c, mods);
    }
}
