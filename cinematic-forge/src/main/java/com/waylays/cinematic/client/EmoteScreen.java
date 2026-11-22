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
        int cols = 3, w = 110, h = 20, gap = 4;
        int rows = (all.length + cols - 1) / cols;
        int x0 = width / 2 - (cols * w + (cols - 1) * gap) / 2;
        int y0 = height / 2 - (rows * (h + gap)) / 2;
        for (int i = 0; i < all.length; i++) {
            Emote e = all[i];
            Component label = Component.translatable(e.key());
            if (e.ordinal() == ClientStudio.emote) label = Component.literal("> ").append(label).append(" <");
            addRenderableWidget(Button.builder(label, b -> {
                ClientStudio.emote = e.ordinal();
                onClose();
            }).bounds(x0 + (i % cols) * (w + gap), y0 + (i / cols) * (h + gap), w, h).build());
        }
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        g.fill(0, 0, width, height, 0x66000000);
        g.drawCenteredString(font, title, width / 2, height / 2 - 80, 0xFFFFFF);
        g.drawCenteredString(font, Component.translatable("cinematic.emotes.hint"), width / 2, height / 2 - 66, 0xA0A0A0);
        super.render(g, mx, my, pt);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
