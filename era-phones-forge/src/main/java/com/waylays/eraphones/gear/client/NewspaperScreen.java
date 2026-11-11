package com.waylays.eraphones.gear.client;

import com.waylays.eraphones.EraPhones;
import com.waylays.eraphones.client.PhoneUI;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.List;

/** The newspaper, open: yellowed paper, the masthead between double rules, the headline, the article (scrolled). */
public class NewspaperScreen extends GearScreen {
    private static final net.minecraft.resources.ResourceLocation PAPER = EraPhones.id("textures/gui/gear/paper.png");
    private final String title, text;
    private float scroll, max;

    public NewspaperScreen(String title, String text) {
        super("item.eraphones.journal_papier", 260, 300);
        this.title = title;
        this.text = text;
    }

    @Override
    protected void draw(GuiGraphics g, long now) {
        panel(g, PAPER, 0, 0, 260, 300);
        String mast = Component.translatable("phone.eraphones.journal_title").getString().toUpperCase();
        PhoneUI.centered(g, mast, 128.6f, 11, 0xFF1A1410, 1.45f);
        PhoneUI.centered(g, mast, 128, 11, 0xFF1A1410, 1.45f);
        PhoneUI.text(g, Component.translatable("gear.eraphones.paper_edition").getString(), 14, 35, 0xFF5A4A38, .55f);
        PhoneUI.right(g, Component.translatable("gear.eraphones.paper_price").getString(), 242, 35, 0xFF5A4A38, .55f);
        int top = 50, bottom = 286;
        // Clipped to the page's column
        g.enableScissor(Math.round(px + 10 * scale), Math.round(py + top * scale), Math.round(px + 246 * scale), Math.round(py + bottom * scale));
        float y = top + 2 - scroll;
        List<FormattedCharSequence> head = PhoneUI.wrap(title.isEmpty() ? "…" : title, 228, 1.25f);
        PhoneUI.lines(g, head, 14, y, 0xFF120E0A, 1.25f);
        y += head.size() * 12.5f + 6;
        g.fill(60, (int) y, 196, (int) y + 1, 0x501A1410);
        y += 6;
        List<FormattedCharSequence> body = PhoneUI.wrap(text, 228, .75f);
        PhoneUI.lines(g, body, 14, y, 0xFF2A2218, .75f);
        y += body.size() * 7.5f;
        g.disableScissor();
        max = Math.max(0, y + scroll - bottom + 6);
        scroll = Math.max(0, Math.min(scroll, max));
        if (max > 0) {
            int track = bottom - top, bar = Math.max(12, (int) (track * track / (track + max)));
            int by = top + (int) ((track - bar) * scroll / max);
            g.fill(248, top, 250, bottom, 0x20000000);
            g.fill(248, by, 250, by + bar, 0x80403020);
        }
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double sx, double sy) {
        scroll = Math.max(0, Math.min(max, scroll - (float) sy * 14));
        return true;
    }

    @Override
    protected void pressed(String id) {
    }
}
