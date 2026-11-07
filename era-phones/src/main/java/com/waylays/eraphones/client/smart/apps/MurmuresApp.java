package com.waylays.eraphones.client.smart.apps;

import com.waylays.eraphones.client.PhoneClient;
import com.waylays.eraphones.client.PhoneUI;
import com.waylays.eraphones.client.smart.PhoneApp;
import com.waylays.eraphones.network.PhoneNet;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import org.lwjgl.glfw.GLFW;

import java.util.List;

/**
 * Murmures: the town's social network. Each line signs with a name of its choosing (@name, chosen once); short posts,
 * newest first, each to be liked; a field to post one.
 */
public class MurmuresApp extends PhoneApp {
    private EditBox post;
    private EditBox handle;

    public MurmuresApp() {
        super("murmures");
    }

    @Override
    public void open() {
        PhoneClient.seen(id);
        post = field(X0 + 14, Y0 + 6, X1 - X0 - 60, PhoneNet.POST, "phone.eraphones.post_hint");
        handle = field(X0 + 24, Y0 + 92, X1 - X0 - 48, 20, "phone.eraphones.handle_hint");
        scroll = 0;
        PhoneClient.ask(5, "");
    }

    @Override
    public int[] background() {
        return new int[] {0xFF0E1A26, 0xFF060A10};
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        header(g, title(), mx, my, PhoneClient.handle.isEmpty() ? null : "@");
        if (PhoneClient.handle.isEmpty()) {
            // Choosing the name to sign with
            PhoneUI.icon(g, "murmures", (X0 + X1) / 2f - 16, Y0 + 12, 32);
            PhoneUI.paragraph(g, Component.translatable("phone.eraphones.handle_choose").getString(), (X0 + X1) / 2f, Y0 + 52, X1 - X0 - 30, PhoneUI.DIM, .65f);
            PhoneUI.text(g, "@", X0 + 14, Y0 + 92, PhoneUI.ACCENT, .9f);
            drawField(g, handle, mx, my, pt);
            button(g, (X0 + X1) / 2 - 30, Y0 + 112, 60, 14, Component.translatable("phone.eraphones.ok").getString(), 0xFF2E8CFF, mx, my);
            return;
        }
        drawField(g, post, mx, my, pt);
        button(g, X1 - 42, Y0 + 3, 34, 14, Component.translatable("phone.eraphones.post").getString(), 0xFF2E8CFF, mx, my);
        clip(g, Y0 + 22, Y1);
        int y = Y0 + 24 - (int) scroll;
        if (PhoneClient.feed.isEmpty()) {
            PhoneUI.paragraph(g, Component.translatable("phone.eraphones.no_posts").getString(), (X0 + X1) / 2f, Y0 + 60, X1 - X0 - 28, PhoneUI.DIM, .65f);
        }
        for (PhoneNet.PostView p : PhoneClient.feed) {
            List<FormattedCharSequence> lines = PhoneUI.wrap(p.text(), X1 - X0 - 26, .65f);
            int h = 22 + lines.size() * 7 + 12;
            PhoneUI.round(g, X0 + 6, y, X1 - X0 - 12, h, 9, p.mine() ? 0x2A5CB4FF : 0x22FFFFFF);
            PhoneUI.avatar(g, p.handle(), "@" + p.handle(), X0 + 12, y + 6, 14);
            PhoneUI.text(g, "@" + p.handle(), X0 + 31, y + 6, PhoneUI.WHITE, .7f);
            PhoneUI.text(g, PhoneUI.ago(p.time()), X0 + 31, y + 14, PhoneUI.FAINT, .5f);
            PhoneUI.lines(g, lines, X0 + 12, y + 24, 0xFFE4ECF4, .65f);
            boolean hl = PhoneUI.in(mx, my, X0 + 10, y + h - 12, 40, 10);
            PhoneUI.text(g, (p.liked() ? "♥ " : "♡ ") + p.likes(), X0 + 12, y + h - 10, p.liked() ? 0xFFFF5C8A : hl ? PhoneUI.WHITE : PhoneUI.DIM, .7f);
            y += h + 5;
        }
        unclip(g);
    }

    private void publish() {
        String t = post.getValue().strip();
        if (!t.isEmpty()) {
            PhoneClient.send(new PhoneNet.Post(t, -1));
            post.setValue("");
            click();
        }
    }

    private void chooseHandle() {
        String h = handle.getValue().strip();
        if (h.length() >= 2) {
            PhoneClient.send(new PhoneNet.Handle(h));
            click();
        }
    }

    @Override
    public boolean click(double mx, double my, int button) {
        if (headerBack(mx, my)) {
            phone.home();
            return true;
        }
        if (PhoneClient.handle.isEmpty()) {
            if (PhoneUI.in(mx, my, (X0 + X1) / 2f - 30, Y0 + 112, 60, 14)) {
                chooseHandle();
            } else {
                clickField(handle, mx, my, button);
            }
            return true;
        }
        if (PhoneUI.in(mx, my, X1 - 42, Y0 + 3, 34, 14)) {
            publish();
            return true;
        }
        if (clickField(post, mx, my, button)) {
            return true;
        }
        int y = Y0 + 24 - (int) scroll;
        for (PhoneNet.PostView p : PhoneClient.feed) {
            List<FormattedCharSequence> lines = PhoneUI.wrap(p.text(), X1 - X0 - 26, .65f);
            int h = 22 + lines.size() * 7 + 12;
            if (PhoneUI.in(mx, my, X0 + 10, y + h - 12, 40, 10) && my > Y0 + 22) {
                click();
                PhoneClient.send(new PhoneNet.Post("", p.id()));
                return true;
            }
            y += h + 5;
        }
        return true;
    }

    @Override
    public boolean key(int key, int scan, int mods) {
        EditBox box = PhoneClient.handle.isEmpty() ? handle : post;
        if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) {
            if (box == handle) {
                chooseHandle();
            } else if (post.isFocused()) {
                publish();
            }
            return true;
        }
        if (box == handle && key != GLFW.GLFW_KEY_ESCAPE) {
            handle.setFocused(true);
        }
        return key != GLFW.GLFW_KEY_ESCAPE && feed(box, key, scan, mods);
    }

    @Override
    public boolean typed(char c, int mods) {
        if (PhoneClient.handle.isEmpty()) {
            handle.setFocused(true);
            return feed(handle, c, mods);
        }
        return feed(post, c, mods);
    }
}
