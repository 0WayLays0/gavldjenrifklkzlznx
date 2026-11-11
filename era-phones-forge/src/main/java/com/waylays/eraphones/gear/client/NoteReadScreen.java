package com.waylays.eraphones.gear.client;

import com.waylays.eraphones.client.PhoneUI;
import net.minecraft.client.gui.GuiGraphics;

/** A sheet of paper held up to read: the page as it was written; the mouse wheel goes down long texts. */
public class NoteReadScreen extends GearScreen {
    private final NotePage.Data data;
    private int scroll, more;

    public NoteReadScreen(NotePage.Data data) {
        super("item.eraphones.feuille", NotePage.W, NotePage.H);
        this.data = data;
    }

    @Override
    protected void draw(GuiGraphics g, long now) {
        more = NotePage.draw(g, 0, 0, data, scroll);
        if (more > 0 || scroll > 0) {
            PhoneUI.centered(g, (scroll > 0 ? "▲ " : "") + (more > scroll ? "▼" : ""), NotePage.W / 2f, NotePage.H - 8, 0x80000000, .7f);
        }
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double sy) {
        double sx = 0;
        scroll = Math.max(0, Math.min(more, scroll - (int) Math.signum(sy)));
        return true;
    }

    @Override
    protected void pressed(String id) {
    }
}
