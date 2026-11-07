package com.waylays.arcanes.client.phone.apps;

import com.waylays.arcanes.client.phone.PhoneApp;
import com.waylays.arcanes.client.phone.PhoneClient;
import com.waylays.arcanes.client.phone.PhoneUI;
import com.waylays.arcanes.network.PhonePackets;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/**
 * Contacts: the players kept in the phone (online or not, sharing their position or not), the players online to add,
 * a switch to share one's own position with one's contacts; a contact opens to write to them, find them on the map, or
 * remove them.
 */
public class ContactsApp extends PhoneApp {
    private EditBox add;
    private String chosen;

    public ContactsApp() {
        super("contacts");
    }

    @Override
    public void open() {
        add = field(X0 + 14, Y0 + 26, X1 - X0 - 50, PhonePackets.NAME, "phone.arcanes_oublies.add_contact");
        chosen = null;
        scroll = 0;
        PhoneClient.ask(0, "");
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        header(g, title(), mx, my, null);
        // Share my position
        PhoneUI.glass(g, X0 + 6, Y0 + 2, X1 - X0 - 12, 18, 8, 0x22FFFFFF);
        PhoneUI.text(g, Component.translatable("phone.arcanes_oublies.share_position").getString(), X0 + 12, Y0 + 8, PhoneUI.WHITE, .62f);
        toggle(g, X1 - 30, Y0 + 6, PhoneClient.sharing);
        drawField(g, add, mx, my, pt);
        button(g, X1 - 32, Y0 + 23, 24, 13, "+", 0xFF8A5CFF, mx, my);
        clip(g, Y0 + 40, Y1);
        int y = Y0 + 42 - (int) scroll;
        PhoneUI.text(g, Component.translatable("phone.arcanes_oublies.my_contacts").getString().toUpperCase(), X0 + 10, y, PhoneUI.FAINT, .55f);
        y += 9;
        if (PhoneClient.contacts.isEmpty()) {
            PhoneUI.text(g, Component.translatable("phone.arcanes_oublies.no_contacts").getString(), X0 + 10, y + 3, PhoneUI.DIM, .6f);
            y += 16;
        }
        for (PhonePackets.Person p : PhoneClient.contacts) {
            y = row(g, p, y, mx, my, true);
        }
        List<PhonePackets.Person> others = others();
        if (!others.isEmpty()) {
            y += 4;
            PhoneUI.text(g, Component.translatable("phone.arcanes_oublies.online_now").getString().toUpperCase(), X0 + 10, y, PhoneUI.FAINT, .55f);
            y += 9;
            for (PhonePackets.Person p : others) {
                y = row(g, p, y, mx, my, false);
            }
        }
        unclip(g);
    }

    static void toggle(GuiGraphics g, int x, int y, boolean on) {
        PhoneUI.round(g, x, y, 20, 10, 5, on ? 0xFF5CE08A : 0x66FFFFFF);
        PhoneUI.round(g, on ? x + 11 : x + 1, y + 1, 8, 8, 4, 0xFFFFFFFF);
    }

    private List<PhonePackets.Person> others() {
        List<PhonePackets.Person> out = new ArrayList<>();
        for (PhonePackets.Person o : PhoneClient.online) {
            if (PhoneClient.contacts.stream().noneMatch(c -> c.id().equals(o.id()))) {
                out.add(o);
            }
        }
        return out;
    }

    private int row(GuiGraphics g, PhonePackets.Person p, int y, int mx, int my, boolean contact) {
        boolean open = contact && p.name().equals(chosen);
        int h = open ? 44 : 24;
        boolean hover = PhoneUI.in(mx, my, X0 + 4, y, X1 - X0 - 8, 24);
        PhoneUI.round(g, X0 + 4, y, X1 - X0 - 8, h, 8, open ? 0x2AFFFFFF : hover ? 0x18FFFFFF : 0);
        PhoneUI.face(g, p.id(), X0 + 10, y + 4, 16);
        if (p.online()) {
            PhoneUI.round(g, X0 + 22, y + 16, 6, 6, 3, 0xFF5CE08A);
        }
        PhoneUI.text(g, p.name(), X0 + 32, y + 5, PhoneUI.WHITE, .75f);
        String status = Component.translatable(p.online() ? "phone.arcanes_oublies.online" : "phone.arcanes_oublies.offline").getString()
                + (p.sharing() ? " · ⌖" : "");
        PhoneUI.text(g, status, X0 + 32, y + 14, p.online() ? 0xFF8FE8B0 : PhoneUI.FAINT, .55f);
        if (!contact) {
            button(g, X1 - 30, y + 6, 22, 12, "+", 0xFF8A5CFF, mx, my);
        }
        if (open) {
            int bw = (X1 - X0 - 24) / 3;
            button(g, X0 + 8, y + 27, bw, 13, Component.translatable("phone.arcanes_oublies.write_to").getString(), 0xFF7A4CF0, mx, my);
            button(g, X0 + 12 + bw, y + 27, bw, 13, Component.translatable("phone.arcanes_oublies.app.carte").getString(), 0xFF2E7AD0, mx, my);
            button(g, X0 + 16 + 2 * bw, y + 27, bw, 13, Component.translatable("phone.arcanes_oublies.remove").getString(), 0xFFC8344A, mx, my);
        }
        return y + h + 3;
    }

    @Override
    public boolean click(double mx, double my, int button) {
        if (headerBack(mx, my)) {
            phone.home();
            return true;
        }
        if (PhoneUI.in(mx, my, X1 - 32, Y0 + 4, 26, 14)) {
            click();
            PhoneClient.sharing = !PhoneClient.sharing;
            PhoneClient.ask(PhoneClient.sharing ? 3 : 4, "");
            return true;
        }
        if (PhoneUI.in(mx, my, X1 - 32, Y0 + 23, 24, 13)) {
            addTyped();
            return true;
        }
        if (clickField(add, mx, my, button)) {
            return true;
        }
        if (my < Y0 + 40) {
            return true;
        }
        int y = Y0 + 42 - (int) scroll + 9;
        if (PhoneClient.contacts.isEmpty()) {
            y += 16;
        }
        for (PhonePackets.Person p : PhoneClient.contacts) {
            boolean open = p.name().equals(chosen);
            int h = open ? 44 : 24;
            if (open && PhoneUI.in(mx, my, X0 + 4, y + 26, X1 - X0 - 8, 16)) {
                int bw = (X1 - X0 - 24) / 3;
                click();
                if (mx < X0 + 8 + bw) {
                    phone.open("missives", (X0 + X1) / 2f, y + 30);
                    if (phone.current() instanceof MissivesApp m) {
                        m.openWith(p.name());
                    }
                } else if (mx < X0 + 12 + 2 * bw) {
                    phone.open("carte", (X0 + X1) / 2f, y + 30);
                    if (phone.current() instanceof CarteApp c) {
                        c.focus(p.id());
                    }
                } else {
                    PacketDistributor.sendToServer(new PhonePackets.Contact(p.name(), false));
                    chosen = null;
                }
                return true;
            }
            if (PhoneUI.in(mx, my, X0 + 4, y, X1 - X0 - 8, 24)) {
                click();
                chosen = open ? null : p.name();
                return true;
            }
            y += h + 3;
        }
        y += 4 + 9;
        for (PhonePackets.Person p : others()) {
            if (PhoneUI.in(mx, my, X0 + 4, y, X1 - X0 - 8, 24)) {
                click();
                PacketDistributor.sendToServer(new PhonePackets.Contact(p.name(), true));
                return true;
            }
            y += 27;
        }
        return true;
    }

    private void addTyped() {
        String name = add.getValue().strip();
        if (!name.isEmpty()) {
            click();
            PacketDistributor.sendToServer(new PhonePackets.Contact(name, true));
            add.setValue("");
        }
    }

    @Override
    public boolean key(int key, int scan, int mods) {
        if (add.isFocused() && (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER)) {
            addTyped();
            return true;
        }
        return key != GLFW.GLFW_KEY_ESCAPE && feed(add, key, scan, mods);
    }

    @Override
    public boolean typed(char c, int mods) {
        return feed(add, c, mods);
    }
}
