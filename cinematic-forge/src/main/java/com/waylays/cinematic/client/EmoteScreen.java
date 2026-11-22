package com.waylays.cinematic.client;

import com.waylays.cinematic.studio.Emote;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Picks the gesture recorded from now on (it stays until another one, or "None", is picked). */
public class EmoteScreen extends Screen {
    public EmoteScreen() {
        super(Component.translatable("cinematic.emotes.title"));
    }

    @Override
    protected void init() {
        Emote[] all = Emote.values();
        int cols = 3, w = Math.min(130, (width - 2 * Ui.MARGIN - 2 * Ui.GAP) / 3), h = 20, gap = Ui.GAP, pitch = Ui.pitch(height);
        int rows = (all.length + cols - 1) / cols;
        int x0 = width / 2 - (cols * w + (cols - 1) * gap) / 2;
        int y0 = Math.max(Ui.TOP + 16, height / 2 - (rows * pitch) / 2 + 10);
        for (int i = 0; i < all.length; i++) {
            Emote e = all[i];
            Component label = Component.translatable(e.key());
            if (e.ordinal() == ClientStudio.emote) label = Component.literal("> ").append(label).append(" <");
            addRenderableWidget(Button.builder(label, b -> {
                ClientStudio.emote = e.ordinal();
                onClose();
            }).bounds(x0 + (i % cols) * (w + gap), y0 + (i / cols) * pitch, w, h).build());
        }
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        g.fill(0, 0, width, height, 0x66000000);
        Ui.header(g, font, width, title, Component.translatable("cinematic.emotes.hint"));
        super.render(g, mx, my, pt);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
