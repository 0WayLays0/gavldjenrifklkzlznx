package com.waylays.eraphones.client.smart.apps;

import com.waylays.eraphones.Numbers;
import com.waylays.eraphones.client.PhoneClient;
import com.waylays.eraphones.client.PhoneUI;
import com.waylays.eraphones.client.smart.PhoneApp;
import com.waylays.eraphones.network.PhoneNet;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import org.lwjgl.glfw.GLFW;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * Messages: the conversations of this line with other numbers, kept on the network (a message waits for a phone that
 * is off or out of reach). The list, latest first, with unread counts; a conversation in bubbles and its field; a new
 * message to a number or to a name of the directory.
 */
public class MessagesApp extends PhoneApp {
    /** The other number of the conversation open. */
    @Nullable
    private String with;
    private boolean composing;
    private EditBox input;
    private EditBox to;

    public MessagesApp() {
        super("messages");
    }

    @Override
    public int[] background() {
        return new int[] {0xFF101418, 0xFF07090C};
    }

    @Override
    public void open() {
        PhoneClient.seen(id);
        input = field(X0 + 12, Y1 - 15, X1 - X0 - 44, PhoneNet.TEXT, "phone.eraphones.write");
        to = field(X0 + 14, Y0 + 6, X1 - X0 - 28, PhoneNet.NAME, "phone.eraphones.to");
        scroll = 0;
        PhoneClient.ask(11, "");
    }

    /** Opens the conversation with {@code number}. */
    public void openWith(String number) {
        with = number;
        composing = false;
        scroll = 0;
        PhoneClient.thread = null;
        PhoneClient.ask(1, number);
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
        List<PhoneNet.Conv> convs = PhoneClient.inbox;
        if (convs.isEmpty()) {
            PhoneUI.icon(g, "messages", (X0 + X1) / 2f - 16, Y0 + 60, 32);
            PhoneUI.paragraph(g, Component.translatable("phone.eraphones.no_messages").getString(), (X0 + X1) / 2f, Y0 + 100, X1 - X0 - 28, PhoneUI.DIM, .7f);
            return;
        }
        clip(g, Y0, Y1);
        int y = Y0 + 4 - (int) scroll;
        for (PhoneNet.Conv c : convs) {
            boolean hover = PhoneUI.in(mx, my, X0 + 4, y, X1 - X0 - 8, 30);
            if (hover) {
                PhoneUI.round(g, X0 + 4, y, X1 - X0 - 8, 30, 8, 0x22FFFFFF);
            }
            String name = PhoneClient.name(c.number(), c.display());
            avatar(g, name, c.number(), X0 + 10, y + 6, 18);
            PhoneUI.text(g, PhoneUI.fit(name, 80, .8f), X0 + 34, y + 6, PhoneUI.WHITE, .8f);
            PhoneUI.right(g, PhoneUI.ago(c.time()), X1 - 10, y + 6, PhoneUI.FAINT, .55f);
            String last = (c.lastMine() ? Component.translatable("phone.eraphones.you").getString() + " " : "") + c.last();
            PhoneUI.text(g, PhoneUI.fit(last, c.unread() > 0 ? 96 : 112, .6f), X0 + 34, y + 17, c.unread() > 0 ? PhoneUI.WHITE : PhoneUI.DIM, .6f);
            if (c.unread() > 0) {
                PhoneUI.round(g, X1 - 22, y + 15, 12, 10, 5, 0xFF2E8CFF);
                PhoneUI.centered(g, String.valueOf(c.unread()), X1 - 16, y + 16.5f, PhoneUI.WHITE, .6f);
            }
            g.fill(X0 + 34, y + 29, X1 - 8, y + 30, 0x14FFFFFF);
            y += 31;
        }
        unclip(g);
    }

    static void avatar(GuiGraphics g, String name, String number, int x, int y, int size) {
        if (Numbers.hidden(number)) {
            PhoneUI.round(g, x, y, size, size, size / 2, 0xFF1A0A0A);
            PhoneUI.centered(g, "?", x + size / 2f, y + size / 2f - size * .22f, 0xFFFF4040, size / 20f);
        } else {
            PhoneUI.avatar(g, name, number, x, y, size);
        }
    }

    private void compose(GuiGraphics g, int mx, int my, float pt) {
        header(g, Component.translatable("phone.eraphones.new_message"), mx, my, null);
        drawField(g, to, mx, my, pt);
        PhoneUI.text(g, Component.translatable("phone.eraphones.directory").getString().toUpperCase(), X0 + 10, Y0 + 26, PhoneUI.FAINT, .55f);
        int y = Y0 + 36;
        for (PhoneNet.ContactView p : matches()) {
            boolean hover = PhoneUI.in(mx, my, X0 + 4, y, X1 - X0 - 8, 22);
            if (hover) {
                PhoneUI.round(g, X0 + 4, y, X1 - X0 - 8, 22, 7, 0x22FFFFFF);
            }
            PhoneUI.avatar(g, p.name(), p.number(), X0 + 10, y + 4, 14);
            PhoneUI.text(g, PhoneUI.fit(p.name(), 80, .75f), X0 + 30, y + 4, PhoneUI.WHITE, .75f);
            PhoneUI.text(g, p.number(), X0 + 30, y + 13, PhoneUI.FAINT, .55f);
            y += 24;
            if (y > Y1 - 20) {
                break;
            }
        }
        String typed = Numbers.normalize(to.getValue());
        if (typed != null) {
            PhoneUI.text(g, Component.translatable("phone.eraphones.write_to", typed).getString(), X0 + 10, Math.min(y + 4, Y1 - 12), PhoneUI.ACCENT, .65f);
        }
    }

    private List<PhoneNet.ContactView> matches() {
        String q = to.getValue().toLowerCase().strip();
        List<PhoneNet.ContactView> out = new ArrayList<>();
        for (PhoneNet.ContactView c : PhoneClient.contacts) {
            if (q.isEmpty() || c.name().toLowerCase().contains(q) || c.number().replace("-", "").contains(q.replace("-", ""))) {
                out.add(c);
            }
        }
        return out;
    }

    private void conversation(GuiGraphics g, int mx, int my, float pt) {
        PhoneNet.Thread t = PhoneClient.thread;
        boolean hb = headerBack(mx, my);
        PhoneUI.text(g, "‹", X0 + 6, TOP + 3, hb ? PhoneUI.WHITE : PhoneUI.ACCENT, 1.6f);
        String name = PhoneClient.name(with, t != null && t.number().equals(with) ? t.display() : PhoneClient.name(with));
        avatar(g, name, with, (X0 + X1) / 2 - 7, TOP - 1, 13);
        PhoneUI.centered(g, PhoneUI.fit(name, 100, .6f), (X0 + X1) / 2f, TOP + 13, PhoneUI.WHITE, .6f);
        if (!Numbers.hidden(with)) {
            boolean hc = PhoneUI.in(mx, my, X1 - 26, TOP, 24, 18);
            PhoneUI.right(g, "✆", X1 - 8, TOP + 4, hc ? PhoneUI.WHITE : PhoneUI.ACCENT, 1.1f);
        }
        g.fill(X0 + 6, TOP + 21, X1 - 6, TOP + 22, 0x22FFFFFF);
        // The bubbles, newest at the bottom
        int bottom = Y1 - 22;
        clip(g, Y0 + 2, bottom);
        if (t != null && t.number().equals(with)) {
            int y = bottom + (int) scroll;
            for (int i = t.lines().size() - 1; i >= 0; i--) {
                PhoneNet.Line l = t.lines().get(i);
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
                PhoneUI.round(g, bx, y, bw, h, 7, l.mine() ? 0xFF2E8CFF : Numbers.hidden(with) ? 0xFF3A1010 : 0xFF2A2E36);
                if (l.mine()) {
                    PhoneUI.round(g, bx, y, bw, h / 2, 7, 0x22FFFFFF);
                }
                PhoneUI.lines(g, lines, bx + 6, y + 4, Numbers.hidden(with) && !l.mine() ? 0xFFFF9A9A : PhoneUI.WHITE, .7f);
            }
        } else {
            PhoneUI.centered(g, "…", (X0 + X1) / 2f, (Y0 + bottom) / 2f, PhoneUI.DIM, 1.5f);
        }
        unclip(g);
        if (Numbers.hidden(with)) {
            PhoneUI.centered(g, Component.translatable("phone.eraphones.cannot_reply").getString(), (X0 + X1) / 2f, Y1 - 13, PhoneUI.FAINT, .6f);
            return;
        }
        // The field and its send button
        PhoneUI.round(g, X0 + 6, Y1 - 19, X1 - X0 - 12, 18, 9, 0x40FFFFFF);
        input.render(g, mx, my, pt);
        boolean hs = PhoneUI.in(mx, my, X1 - 26, Y1 - 18, 16, 16);
        PhoneUI.round(g, X1 - 25, Y1 - 17, 14, 14, 7, hs ? 0xFF5CB4FF : 0xFF2E8CFF);
        PhoneUI.centered(g, "↑", X1 - 18, Y1 - 13.5f, PhoneUI.WHITE, .9f);
    }

    private void send() {
        String text = input.getValue().strip();
        if (!text.isEmpty() && with != null && !Numbers.hidden(with)) {
            PhoneClient.send(new PhoneNet.Sms(with, text));
            input.setValue("");
            scroll = 0;
            click();
        }
    }

    /** The number the "to" field means: a number typed, or the name of a contact. */
    @Nullable
    private String target() {
        String raw = to.getValue().strip();
        String n = Numbers.normalize(raw);
        if (n != null) {
            return n;
        }
        for (PhoneNet.ContactView c : PhoneClient.contacts) {
            if (c.name().equalsIgnoreCase(raw)) {
                return c.number();
            }
        }
        List<PhoneNet.ContactView> m = matches();
        return m.size() == 1 ? m.get(0).number() : null;
    }

    @Override
    public boolean click(double mx, double my, int button) {
        if (with != null) {
            if (headerBack(mx, my)) {
                return back();
            }
            if (!Numbers.hidden(with) && PhoneUI.in(mx, my, X1 - 26, TOP, 24, 18)) {
                click();
                PhoneClient.dial(with);
                return true;
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
            for (PhoneNet.ContactView p : matches()) {
                if (PhoneUI.in(mx, my, X0 + 4, y, X1 - X0 - 8, 22)) {
                    click();
                    openWith(p.number());
                    return true;
                }
                y += 24;
            }
            String typed = Numbers.normalize(to.getValue());
            if (typed != null && my > y) {
                click();
                openWith(typed);
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
        for (PhoneNet.Conv c : PhoneClient.inbox) {
            if (PhoneUI.in(mx, my, X0 + 4, y, X1 - X0 - 8, 30) && my >= Y0) {
                click();
                openWith(c.number());
                return true;
            }
            y += 31;
        }
        return true;
    }

    @Override
    public boolean key(int key, int scan, int mods) {
        if (with != null) {
            if (Numbers.hidden(with)) {
                return false;
            }
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
            if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) {
                String n = target();
                if (n != null) {
                    openWith(n);
                }
                return true;
            }
            return key != GLFW.GLFW_KEY_ESCAPE && (feed(to, key, scan, mods) || true);
        }
        return false;
    }

    @Override
    public boolean typed(char c, int mods) {
        if (with != null) {
            if (Numbers.hidden(with)) {
                return false;
            }
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
            PhoneClient.ask(11, "");
            return true;
        }
        if (composing) {
            composing = false;
            return true;
        }
        return false;
    }
}
