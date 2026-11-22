package com.waylays.cinematic.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** How to make a film, in numbered steps, and the keys. */
public class HelpScreen extends Screen {
    private static final int STEPS = 6;
    private final Screen back;

    public HelpScreen(Screen back) {
        super(Component.translatable("cinematic.help.title"));
        this.back = back;
    }

    @Override
    protected void init() {
        addRenderableWidget(Button.builder(Component.translatable("cinematic.help.back"), b -> minecraft.setScreen(back))
                .bounds(width - Ui.MARGIN - 110, 6, 110, 20).build());
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        g.fill(0, 0, width, height, 0xB0000000);
        Ui.header(g, font, width, title, Component.translatable("cinematic.help.subtitle"));
        int m = Ui.MARGIN, colW = (width - 3 * m - 12) / 2;
        int lx = m, rx = m + colW + 18;
        Ui.panel(g, lx, Ui.TOP, colW, height - Ui.TOP - m);
        Ui.panel(g, rx, Ui.TOP, colW, height - Ui.TOP - m);
        int y = Ui.section(g, font, lx, Ui.TOP, colW, Component.translatable("cinematic.help.steps")) + 2;
        for (int i = 1; i <= STEPS; i++) {
            g.drawString(font, Component.literal(i + "."), lx, y, Ui.TITLE);
            for (var line : font.split(Component.translatable("cinematic.help.step" + i), colW - 14)) {
                g.drawString(font, line, lx + 12, y, Ui.TEXT);
                y += 10;
            }
            y += 5;
        }
        y = Ui.section(g, font, rx, Ui.TOP, colW, Component.translatable("cinematic.help.keys")) + 2;
        String[][] keys = {{"F6", "studio"}, {"N", "freecam"}, {"K", "point"}, {"P", "play"}, {"← →", "time"}, {"B", "gestures"},
                {"F7", "look"}, {"F8", "auto"}, {"F10", "dof"}};
        for (String[] k : keys) {
            g.drawString(font, Component.literal(k[0]), rx, y, Ui.TITLE);
            for (var line : font.split(Component.translatable("cinematic.help.key_" + k[1]), colW - 34)) {
                g.drawString(font, line, rx + 32, y, Ui.TEXT);
                y += 10;
            }
            y += 4;
        }
        super.render(g, mx, my, pt);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
