package com.waylays.cinematic.client;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/** The studio's look: airy panels with a title and a short explanation, the same margins everywhere. */
final class Ui {
    static final int MARGIN = 12;
    static final int GAP = 6;
    static final int TOP = 40;
    static final int TITLE = 0xFFFFE070;
    static final int TEXT = 0xFFE0E0E0;
    static final int HINT = 0xFFA8A8A8;

    private Ui() {}

    /** Space between two rows of buttons: roomier when the screen allows it. */
    static int pitch(int height) {
        return height >= 330 ? 28 : height >= 290 ? 25 : 22;
    }

    static void header(GuiGraphics g, Font font, int width, Component title, Component subtitle) {
        g.fill(0, 0, width, 32, 0xA0000000);
        g.drawString(font, title, MARGIN, 7, 0xFFFFFFFF);
        g.drawString(font, subtitle, MARGIN, 19, HINT);
    }

    static void panel(GuiGraphics g, int x, int y, int w, int h) {
        g.fill(x - 6, y - 6, x + w + 6, y + h + 6, 0x80000000);
        g.renderOutline(x - 6, y - 6, w + 12, h + 12, 0x40FFFFFF);
    }

    /** A section title with a thin line under it. Returns the y where the section's content starts. */
    static int section(GuiGraphics g, Font font, int x, int y, int w, Component title) {
        g.drawString(font, title, x, y, TITLE);
        g.fill(x, y + 10, x + w, y + 11, 0x50FFE070);
        return y + 15;
    }
}
