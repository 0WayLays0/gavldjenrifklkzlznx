package com.waylays.eraphones.client.smart.apps;

import com.waylays.eraphones.client.PhoneClient;
import com.waylays.eraphones.client.PhoneUI;
import com.waylays.eraphones.client.smart.PhoneApp;
import com.waylays.eraphones.network.PhoneNet;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.List;

/** Journal: the town's news, as the game masters publish it: a masthead, then each article, newest first. */
public class JournalApp extends PhoneApp {
    public JournalApp() {
        super("journal");
    }

    @Override
    public int[] background() {
        return new int[] {0xFFF4EFE4, 0xFFE4DCCB};
    }

    @Override
    public void open() {
        PhoneClient.seen(id);
        PhoneClient.ask(6, "");
        scroll = 0;
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        boolean hb = headerBack(mx, my);
        PhoneUI.text(g, "‹", X0 + 6, TOP + 3, hb ? 0xFF000000 : 0xFF8A2A1A, 1.6f);
        PhoneUI.centered(g, Component.translatable("phone.eraphones.journal_title").getString(), (X0 + X1) / 2f, TOP + 4, 0xFF1A1410, 1.15f);
        g.fill(X0 + 8, TOP + 17, X1 - 8, TOP + 18, 0xFF1A1410);
        g.fill(X0 + 8, TOP + 19, X1 - 8, TOP + 20, 0xFF1A1410);
        PhoneUI.centered(g, PhoneClient.dateLine(), (X0 + X1) / 2f, TOP + 22, 0xFF5A4E40, .5f);
        clip(g, Y0 + 10, Y1);
        int y = Y0 + 12 - (int) scroll;
        if (PhoneClient.news.isEmpty()) {
            PhoneUI.paragraph(g, Component.translatable("phone.eraphones.no_news").getString(), (X0 + X1) / 2f, Y0 + 50, X1 - X0 - 30, 0xFF6A5E50, .65f);
        }
        boolean first = true;
        for (PhoneNet.NewsView n : PhoneClient.news) {
            float ts = first ? .95f : .75f;
            List<FormattedCharSequence> title = PhoneUI.wrap(n.title(), X1 - X0 - 20, ts);
            PhoneUI.lines(g, title, X0 + 10, y, 0xFF1A1410, ts);
            y += Math.round(title.size() * 10 * ts) + 2;
            PhoneUI.text(g, PhoneUI.ago(n.time()), X0 + 10, y, 0xFF8A2A1A, .5f);
            y += 7;
            if (!n.text().isEmpty()) {
                List<FormattedCharSequence> body = PhoneUI.wrap(n.text(), X1 - X0 - 20, .6f);
                PhoneUI.lines(g, body, X0 + 10, y, 0xFF3A3028, .6f);
                y += Math.round(body.size() * 6) + 4;
            }
            g.fill(X0 + 30, y + 2, X1 - 30, y + 3, 0x401A1410);
            y += 9;
            first = false;
        }
        unclip(g);
    }

    @Override
    public boolean click(double mx, double my, int button) {
        if (headerBack(mx, my)) {
            phone.home();
        }
        return true;
    }
}
