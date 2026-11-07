package com.waylays.arcanes.client.phone.apps;

import com.waylays.arcanes.client.phone.PhoneApp;
import com.waylays.arcanes.client.phone.PhoneClient;
import com.waylays.arcanes.client.phone.PhoneUI;
import com.waylays.arcanes.network.PhonePackets;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Missives: the conversations with other players, kept by the server (a message waits for an absent player). The list,
 * latest first, with unread counts; a conversation in bubbles, its field to write in; a new conversation by name.
 */
public class MissivesApp extends PhoneApp {
    @Nullable
    private String with;
    private boolean composing;
    private EditBox input;
    private EditBox to;

    public MissivesApp() {
        super("missives");
    }

    @Override
    public int[] background() {
        return new int[] {0xFF14111C, 0xFF0A0810};
    }

    @Override
    public void open() {
        PhoneClient.seen(id);
        input = field(X0 + 12, Y1 - 15, X1 - X0 - 44, PhonePackets.TEXT, "phone.arcanes_oublies.write");
        to = field(X0 + 14, Y0 + 6, X1 - X0 - 28, PhonePackets.NAME, "phone.arcanes_oublies.to");
        scroll = 0;
        PhoneClient.ask(0, "");
    }

    /** Opens the conversation with {@code name}. */
    public void openWith(String name) {
        with = name;
        composing = false;
        scroll = 0;
        PhoneClient.thread = null;
        PhoneClient.ask(1, name);
        input.setFocused(true);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        if (with != null) {
            conversation(g, mx, my, pt);
        } else if (composing) {
            compose(g, mx, my, pt);
        } else {
            list(g, mx, my);
        }
    }

    private void list(GuiGraphics g, int mx, int my) {
        header(g, title(), mx, my, "✎");
        List<PhonePackets.Conv> convs = PhoneClient.inbox;
        if (convs.isEmpty()) {
            PhoneUI.icon(g, "missives", (X0 + X1) / 2f - 16, Y0 + 60, 32);
            PhoneUI.paragraph(g, Component.translatable("phone.arcanes_oublies.no_messages").getString(), (X0 + X1) / 2f, Y0 + 100, X1 - X0 - 28, PhoneUI.DIM, .7f);
            return;
        }
        clip(g, Y0, Y1);
        int y = Y0 + 4 - (int) scroll;
        for (PhonePackets.Conv c : convs) {
            boolean hover = PhoneUI.in(mx, my, X0 + 4, y, X1 - X0 - 8, 30);
            if (hover) {
                PhoneUI.round(g, X0 + 4, y, X1 - X0 - 8, 30, 8, 0x22FFFFFF);
            }
            PhoneUI.face(g, c.id(), X0 + 10, y + 6, 18);
            if (c.online()) {
                PhoneUI.round(g, X0 + 24, y + 20, 6, 6, 3, 0xFF5CE08A);
            }
            PhoneUI.text(g, PhoneUI.fit(c.name(), 80, .8f), X0 + 34, y + 6, PhoneUI.WHITE, .8f);
            PhoneUI.right(g, PhoneUI.ago(c.time()), X1 - 10, y + 6, PhoneUI.FAINT, .55f);
            String last = (c.lastMine() ? Component.translatable("phone.arcanes_oublies.you").getString() + " " : "") + c.last();
            PhoneUI.text(g, PhoneUI.fit(last, c.unread() > 0 ? 96 : 112, .6f), X0 + 34, y + 17, c.unread() > 0 ? PhoneUI.WHITE : PhoneUI.DIM, .6f);
            if (c.unread() > 0) {
                PhoneUI.round(g, X1 - 22, y + 15, 12, 10, 5, 0xFF8A5CFF);
                PhoneUI.centered(g, String.valueOf(c.unread()), X1 - 16, y + 16.5f, PhoneUI.WHITE, .6f);
            }
            g.fill(X0 + 34, y + 29, X1 - 8, y + 30, 0x14FFFFFF);
            y += 31;
        }
        unclip(g);
    }

    private void compose(GuiGraphics g, int mx, int my, float pt) {
        header(g, Component.translatable("phone.arcanes_oublies.new_message"), mx, my, null);
        drawField(g, to, mx, my, pt);
        PhoneUI.text(g, Component.translatable("phone.arcanes_oublies.suggestions").getString(), X0 + 10, Y0 + 26, PhoneUI.FAINT, .6f);
        int y = Y0 + 36;
        String q = to.getValue().toLowerCase();
        for (PhonePackets.Person p : people()) {
            if (!q.isEmpty() && !p.name().toLowerCase().contains(q)) {
                continue;
            }
            boolean hover = PhoneUI.in(mx, my, X0 + 4, y, X1 - X0 - 8, 22);
            if (hover) {
                PhoneUI.round(g, X0 + 4, y, X1 - X0 - 8, 22, 7, 0x22FFFFFF);
            }
            PhoneUI.face(g, p.id(), X0 + 10, y + 4, 14);
            PhoneUI.text(g, p.name(), X0 + 30, y + 7, PhoneUI.WHITE, .75f);
            if (p.online()) {
                PhoneUI.right(g, "●", X1 - 10, y + 7, 0xFF5CE08A, .6f);
            }
            y += 24;
            if (y > Y1 - 20) {
                break;
            }
        }
    }

    private List<PhonePackets.Person> people() {
        java.util.LinkedHashMap<String, PhonePackets.Person> all = new java.util.LinkedHashMap<>();
        for (PhonePackets.Person p : PhoneClient.contacts) {
            all.put(p.name(), p);
        }
        for (PhonePackets.Person p : PhoneClient.online) {
            all.putIfAbsent(p.name(), p);
        }
        return List.copyOf(all.values());
    }

    private void conversation(GuiGraphics g, int mx, int my, float pt) {
        PhonePackets.Thread t = PhoneClient.thread;
        boolean hb = headerBack(mx, my);
        PhoneUI.text(g, "‹", X0 + 6, TOP + 3, hb ? PhoneUI.WHITE : PhoneUI.ACCENT, 1.6f);
        java.util.UUID id = t != null ? t.id() : null;
        if (id != null) {
            PhoneUI.face(g, id, (X0 + X1) / 2 - 7, TOP + 1, 12);
        }
        PhoneUI.centered(g, with, (X0 + X1) / 2f, TOP + 14, PhoneUI.WHITE, .6f);
        g.fill(X0 + 6, TOP + 21, X1 - 6, TOP + 22, 0x22FFFFFF);
        // The bubbles, newest at the bottom
        int bottom = Y1 - 22;
        clip(g, Y0 + 2, bottom);
        if (t != null) {
            int y = bottom + (int) scroll;
            for (int i = t.lines().size() - 1; i >= 0; i--) {
                PhonePackets.Line l = t.lines().get(i);
                List<FormattedCharSequence> lines = PhoneUI.wrap(l.text(), 100, .7f);
                int w = 0;
                for (FormattedCharSequence s : lines) {
                    w = Math.max(w, (int) (mc.font.width(s) * .7f));
                }
                int h = lines.size() * 7 + 8;
                y -= h + 4;
                if (y + h < Y0 - 40) {
                    break;
                }
                int bw = w + 12;
                int bx = l.mine() ? X1 - 8 - bw : X0 + 8;
                PhoneUI.round(g, bx, y, bw, h, 7, l.mine() ? 0xFF7A4CF0 : 0xFF2C2638);
                if (l.mine()) {
                    PhoneUI.round(g, bx, y, bw, h / 2, 7, 0x22FFFFFF);
                }
                PhoneUI.lines(g, lines, bx + 6, y + 4, PhoneUI.WHITE, .7f);
            }
        } else {
            PhoneUI.centered(g, "…", (X0 + X1) / 2f, (Y0 + bottom) / 2f, PhoneUI.DIM, 1.5f);
        }
        unclip(g);
        // The field and its send button
        PhoneUI.round(g, X0 + 6, Y1 - 19, X1 - X0 - 12, 18, 9, 0x40FFFFFF);
        input.render(g, mx, my, pt);
        boolean hs = PhoneUI.in(mx, my, X1 - 26, Y1 - 18, 16, 16);
        PhoneUI.round(g, X1 - 25, Y1 - 17, 14, 14, 7, hs ? 0xFFA07CFF : 0xFF8A5CFF);
        PhoneUI.centered(g, "↑", X1 - 18, Y1 - 13.5f, PhoneUI.WHITE, .9f);
    }

    private void send() {
        String text = input.getValue().strip();
        if (!text.isEmpty() && with != null) {
            PacketDistributor.sendToServer(new PhonePackets.Send(with, text));
            input.setValue("");
            scroll = 0;
            click();
        }
    }

    @Override
    public boolean click(double mx, double my, int button) {
        if (with != null) {
            if (headerBack(mx, my)) {
                return back();
            }
            if (PhoneUI.in(mx, my, X1 - 26, Y1 - 18, 16, 16)) {
                send();
                return true;
            }
            input.setFocused(PhoneUI.in(mx, my, X0 + 6, Y1 - 19, X1 - X0 - 36, 18));
            return true;
        }
        if (composing) {
            if (headerBack(mx, my)) {
                return back();
            }
            if (clickField(to, mx, my, button)) {
                return true;
            }
            int y = Y0 + 36;
            String q = to.getValue().toLowerCase();
            for (PhonePackets.Person p : people()) {
                if (!q.isEmpty() && !p.name().toLowerCase().contains(q)) {
                    continue;
                }
                if (PhoneUI.in(mx, my, X0 + 4, y, X1 - X0 - 8, 22)) {
                    click();
                    openWith(p.name());
                    return true;
                }
                y += 24;
            }
            return true;
        }
        if (headerBack(mx, my)) {
            phone.home();
            return true;
        }
        if (headerAction(mx, my)) {
            click();
            composing = true;
            to.setValue("");
            to.setFocused(true);
            return true;
        }
        int y = Y0 + 4 - (int) scroll;
        for (PhonePackets.Conv c : PhoneClient.inbox) {
            if (PhoneUI.in(mx, my, X0 + 4, y, X1 - X0 - 8, 30) && my >= Y0) {
                click();
                openWith(c.name());
                return true;
            }
            y += 31;
        }
        return true;
    }

    @Override
    public boolean key(int key, int scan, int mods) {
        if (with != null) {
            if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) {
                send();
                return true;
            }
            if (key == GLFW.GLFW_KEY_ESCAPE) {
                return false;
            }
            input.setFocused(true);
            return input.keyPressed(key, scan, mods) || true;
        }
        if (composing) {
            if ((key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) && !to.getValue().isBlank()) {
                openWith(to.getValue().strip());
                return true;
            }
            return key != GLFW.GLFW_KEY_ESCAPE && (feed(to, key, scan, mods) || true);
        }
        return false;
    }

    @Override
    public boolean typed(char c, int mods) {
        if (with != null) {
            input.setFocused(true);
            return input.charTyped(c, mods);
        }
        return composing && to.charTyped(c, mods);
    }

    @Override
    public boolean scrolled(double mx, double my, double dy) {
        scroll = Math.max(0, scroll + (float) dy * (with != null ? 14 : -14));
        return true;
    }

    @Override
    public boolean back() {
        if (with != null) {
            with = null;
            scroll = 0;
            PhoneClient.ask(0, "");
            return true;
        }
        if (composing) {
            composing = false;
            return true;
        }
        return false;
    }
}
