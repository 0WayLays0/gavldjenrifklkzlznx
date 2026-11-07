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

import java.util.List;

/** Échos: the server's own feed: short posts by the players, newest first, each to be liked; a field to post one. */
public class EchosApp extends PhoneApp {
    private EditBox post;

    public EchosApp() {
        super("echos");
    }

    @Override
    public void open() {
        PhoneClient.seen(id);
        post = field(X0 + 14, Y0 + 6, X1 - X0 - 60, PhonePackets.POST, "phone.arcanes_oublies.echo_hint");
        scroll = 0;
        PhoneClient.ask(0, "");
    }

    @Override
    public int[] background() {
        return new int[] {0xFF1E0E2A, 0xFF0A0610};
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        header(g, title(), mx, my, null);
        drawField(g, post, mx, my, pt);
        button(g, X1 - 42, Y0 + 3, 34, 14, Component.translatable("phone.arcanes_oublies.post").getString(), 0xFFE0508A, mx, my);
        clip(g, Y0 + 22, Y1);
        int y = Y0 + 24 - (int) scroll;
        if (PhoneClient.feed.isEmpty()) {
            PhoneUI.centered(g, Component.translatable("phone.arcanes_oublies.no_echoes").getString(), (X0 + X1) / 2f, Y0 + 60, PhoneUI.DIM, .65f);
        }
        for (PhonePackets.PostView p : PhoneClient.feed) {
            List<FormattedCharSequence> lines = PhoneUI.wrap(p.text(), X1 - X0 - 26, .65f);
            int h = 22 + lines.size() * 7 + 12;
            PhoneUI.round(g, X0 + 6, y, X1 - X0 - 12, h, 9, 0x22FFFFFF);
            PhoneUI.face(g, p.authorId(), X0 + 12, y + 6, 14);
            PhoneUI.text(g, p.author(), X0 + 31, y + 6, PhoneUI.WHITE, .7f);
            PhoneUI.text(g, PhoneUI.ago(p.time()), X0 + 31, y + 14, PhoneUI.FAINT, .5f);
            PhoneUI.lines(g, lines, X0 + 12, y + 24, 0xFFE8E4F0, .65f);
            boolean hl = PhoneUI.in(mx, my, X0 + 10, y + h - 12, 40, 10);
            PhoneUI.text(g, (p.liked() ? "♥ " : "♡ ") + p.likes(), X0 + 12, y + h - 10, p.liked() ? 0xFFFF5C8A : hl ? PhoneUI.WHITE : PhoneUI.DIM, .7f);
            y += h + 5;
        }
        unclip(g);
    }

    private void publish() {
        String t = post.getValue().strip();
        if (!t.isEmpty()) {
            PacketDistributor.sendToServer(new PhonePackets.Echo(t, -1));
            post.setValue("");
            click();
        }
    }

    @Override
    public boolean click(double mx, double my, int button) {
        if (headerBack(mx, my)) {
            phone.home();
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
        for (PhonePackets.PostView p : PhoneClient.feed) {
            List<FormattedCharSequence> lines = PhoneUI.wrap(p.text(), X1 - X0 - 26, .65f);
            int h = 22 + lines.size() * 7 + 12;
            if (PhoneUI.in(mx, my, X0 + 10, y + h - 12, 40, 10) && my > Y0 + 22) {
                click();
                PacketDistributor.sendToServer(new PhonePackets.Echo("", p.id()));
                return true;
            }
            y += h + 5;
        }
        return true;
    }

    @Override
    public boolean key(int key, int scan, int mods) {
        if (post.isFocused() && (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER)) {
            publish();
            return true;
        }
        return key != GLFW.GLFW_KEY_ESCAPE && feed(post, key, scan, mods);
    }

    @Override
    public boolean typed(char c, int mods) {
        return feed(post, c, mods);
    }
}
