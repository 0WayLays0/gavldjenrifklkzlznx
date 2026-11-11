package com.waylays.eraphones.gear.client;

import com.waylays.eraphones.client.PhoneUI;
import com.waylays.eraphones.gear.Tape;
import com.waylays.eraphones.gear.TapeNet;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * Recording a blank cassette: the MP3 files in the game master's cassettes folder (looked at again every two seconds,
 * so one dropped in shows at once), the one picked, the label to write on the cassette, and how far the sending is.
 */
public class BurnScreen extends Screen {
    private static final int W = 300, ROWS = 7, ROW = 16;
    private final boolean offhand;
    private final List<Path> files = new ArrayList<>();
    private final List<Long> sizes = new ArrayList<>();
    private int selected = -1, scroll, ticks;
    private int x0, y0, listY;
    private EditBox label;
    private Button record;

    public BurnScreen(boolean offhand) {
        super(Component.translatable("gear.eraphones.burn_title"));
        this.offhand = offhand;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    protected void init() {
        String keep = label == null ? "" : label.getValue();
        int h = 64 + ROWS * ROW + 76;
        x0 = (width - W) / 2;
        y0 = Math.max(4, (height - h) / 2);
        listY = y0 + 44;
        addRenderableWidget(Button.builder(Component.translatable("gear.eraphones.burn_open"), b -> TapeClient.openFolder())
                .bounds(x0 + 10, y0 + 22, 136, 18).build());
        addRenderableWidget(Button.builder(Component.translatable("gear.eraphones.burn_refresh"), b -> refresh())
                .bounds(x0 + W - 146, y0 + 22, 136, 18).build());
        int y = listY + ROWS * ROW + 8;
        label = new EditBox(font, x0 + 10, y + 10, W - 20, 18, Component.translatable("gear.eraphones.burn_label"));
        label.setMaxLength(TapeNet.TITLE);
        label.setValue(keep);
        addRenderableWidget(label);
        record = addRenderableWidget(Button.builder(Component.translatable("gear.eraphones.burn_go"), b -> go())
                .bounds(x0 + 10, y + 52, (W - 26) / 2, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gear.eraphones.burn_cancel"), b -> onClose())
                .bounds(x0 + 16 + (W - 26) / 2, y + 52, (W - 26) / 2, 20).build());
        refresh();
    }

    private void refresh() {
        Path keep = selected >= 0 && selected < files.size() ? files.get(selected) : null;
        files.clear();
        sizes.clear();
        try {
            Files.createDirectories(Tape.folder());
            try (Stream<Path> s = Files.list(Tape.folder())) {
                s.filter(p -> {
                            String n = p.getFileName().toString().toLowerCase();
                            return (n.endsWith(".mp3") || n.endsWith(".mp4")) && Files.isRegularFile(p);
                        })
                        .sorted((a, b) -> a.getFileName().toString().compareToIgnoreCase(b.getFileName().toString()))
                        .forEach(files::add);
            }
        } catch (IOException ignored) {
            // An unreadable folder lists nothing
        }
        for (Path p : files) {
            long n;
            try {
                n = Files.size(p);
            } catch (IOException e) {
                n = 0;
            }
            sizes.add(n);
        }
        selected = keep == null ? -1 : files.indexOf(keep);
        scroll = Math.max(0, Math.min(scroll, files.size() - ROWS));
    }

    private void go() {
        if (selected < 0 || selected >= files.size()) {
            return;
        }
        TapeClient.start(offhand, files.get(selected), label.getValue());
    }

    @Override
    public void tick() {
        super.tick();
        if (++ticks % 40 == 0 && !TapeClient.busy()) {
            refresh();
        }
        if (record != null) {
            record.active = selected >= 0 && !TapeClient.busy();
        }
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (mx >= x0 + 10 && mx < x0 + W - 10 && my >= listY && my < listY + ROWS * ROW && !TapeClient.busy()) {
            int i = scroll + (int) ((my - listY) / ROW);
            if (i < files.size()) {
                selected = i;
                String name = files.get(i).getFileName().toString();
                label.setValue(name.substring(0, name.length() - 4));
                return true;
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double sy) {
        double sx = 0;
        if (my >= listY && my < listY + ROWS * ROW) {
            scroll = Math.max(0, Math.min(scroll - (int) Math.signum(sy), files.size() - ROWS));
            return true;
        }
        return super.mouseScrolled(mx, my, sy);
    }

        public void renderBackground(GuiGraphics g, int mx, int my, float pt) {
        super.renderBackground(g);
        int h = 64 + ROWS * ROW + 76;
        PhoneUI.round(g, x0, y0, W, h, 8, 0xF0181A22);
        PhoneUI.round(g, x0 + 1, y0 + 1, W - 2, 16, 7, 0xFF262A36);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        renderBackground(g, mx, my, pt);
        super.render(g, mx, my, pt);
        PhoneUI.centered(g, title.getString(), x0 + W / 2f, y0 + 5, PhoneUI.WHITE, 1);
        // The folder, then its recordings
        PhoneUI.round(g, x0 + 10, listY, W - 20, ROWS * ROW, 4, 0xFF0E1016);
        if (files.isEmpty()) {
            PhoneUI.paragraph(g, Component.translatable("gear.eraphones.burn_empty").getString(), x0 + W / 2f, listY + 30, W - 50, PhoneUI.DIM, 1);
        }
        for (int r = 0; r < ROWS && scroll + r < files.size(); r++) {
            int i = scroll + r;
            int y = listY + r * ROW;
            boolean over = mx >= x0 + 10 && mx < x0 + W - 10 && my >= y && my < y + ROW;
            if (i == selected) {
                PhoneUI.round(g, x0 + 11, y + 1, W - 22, ROW - 2, 3, 0xFF2E5C9A);
            } else if (over) {
                PhoneUI.round(g, x0 + 11, y + 1, W - 22, ROW - 2, 3, 0xFF22262F);
            }
            String size = Component.translatable("gear.eraphones.burn_size", String.format("%.1f", sizes.get(i) / 1048576.0)).getString();
            PhoneUI.right(g, size, x0 + W - 16, y + 4, PhoneUI.FAINT, 1);
            String name = PhoneUI.fit(files.get(i).getFileName().toString(), W - 40 - PhoneUI.width(size, 1), 1);
            PhoneUI.text(g, name, x0 + 16, y + 4, PhoneUI.WHITE, 1);
        }
        if (files.size() > ROWS) {
            int bar = Math.max(8, ROWS * ROW * ROWS / files.size());
            int by = listY + (ROWS * ROW - bar) * scroll / Math.max(1, files.size() - ROWS);
            g.fill(x0 + W - 13, by, x0 + W - 11, by + bar, 0xFF5A6070);
        }
        int y = listY + ROWS * ROW + 8;
        PhoneUI.text(g, Component.translatable("gear.eraphones.burn_label").getString(), x0 + 10, y, PhoneUI.DIM, 1);
        // How it goes
        if (TapeClient.busy()) {
            float f = TapeClient.progress();
            g.fill(x0 + 10, y + 34, x0 + W - 10, y + 44, 0xFF0E1016);
            g.fill(x0 + 11, y + 35, x0 + 11 + (int) ((W - 22) * f), y + 43, 0xFF4AA8FF);
            PhoneUI.centered(g, Component.translatable("gear.eraphones.burn_sending", Math.round(f * 100)).getString(), x0 + W / 2f, y + 35, PhoneUI.WHITE, 1);
        } else if (TapeClient.status != null) {
            PhoneUI.centered(g, PhoneUI.fit(TapeClient.status.getString(), W - 20, 1), x0 + W / 2f, y + 35,
                    TapeClient.statusBad ? 0xFFFF8080 : PhoneUI.DIM, 1);
        } else {
            PhoneUI.centered(g, PhoneUI.fit(Tape.folder().toString(), W - 20, .8f), x0 + W / 2f, y + 36, PhoneUI.FAINT, .8f);
        }
    }

    @Override
    public void removed() {
        super.removed();
        if (!TapeClient.busy()) {
            TapeClient.status = null;
        }
    }
}
