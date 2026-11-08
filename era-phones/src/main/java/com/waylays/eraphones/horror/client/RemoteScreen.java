package com.waylays.eraphones.horror.client;

import com.waylays.eraphones.client.PhoneUI;
import com.waylays.eraphones.horror.HorrorNet;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The game master's remote: the players about on the left (picked by clicking them), on the right a key for each
 * fright and each sound of /horreur, and words to whisper in their heads or to narrate over their screens.
 */
public class RemoteScreen extends Screen {
    static final String[] EFFECTS = {"brouillard", "coupure", "sang", "tremblement", "flotter", "vertige", "ballon", "stop"};
    static final String[] SOUNDS = {"pas", "toc", "porte", "rire", "enfants", "murmures", "souffle", "coeur", "cri", "manege", "glas", "gouttes"};
    private static final int W = 380, H = 236, LIST = 120, ROW = 14;
    private final List<String> ids, names;
    private final Set<String> picked = new HashSet<>();
    private int x0, y0, scroll;
    private EditBox words;

    public RemoteScreen(List<String> ids, List<String> names) {
        super(Component.translatable("item.eraphones.telecommande_mj"));
        this.ids = new ArrayList<>(ids);
        this.names = new ArrayList<>(names);
        picked.addAll(ids);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    protected void init() {
        x0 = (width - W) / 2;
        y0 = (height - H) / 2;
        addRenderableWidget(Button.builder(Component.translatable("remote.eraphones.all"), b -> {
            if (picked.size() == ids.size()) {
                picked.clear();
            } else {
                picked.addAll(ids);
            }
        }).bounds(x0 + 8, y0 + H - 26, LIST - 8, 18).build());
        int bx = x0 + LIST + 12, bw = (W - LIST - 20 - 9) / 4;
        for (int i = 0; i < EFFECTS.length; i++) {
            String e = EFFECTS[i];
            addRenderableWidget(Button.builder(Component.translatable("remote.eraphones." + e), b -> send(e, ""))
                    .bounds(bx + (i % 4) * (bw + 3), y0 + 30 + (i / 4) * 21, bw, 18).build());
        }
        for (int i = 0; i < SOUNDS.length; i++) {
            String s = SOUNDS[i];
            addRenderableWidget(Button.builder(Component.translatable("remote.eraphones.son." + s), b -> send("son:" + s, ""))
                    .bounds(bx + (i % 4) * (bw + 3), y0 + 92 + (i / 4) * 21, bw, 18).build());
        }
        words = new EditBox(font, bx, y0 + 172, W - LIST - 20, 18, Component.translatable("remote.eraphones.words"));
        words.setMaxLength(500);
        words.setHint(Component.translatable("remote.eraphones.words"));
        addRenderableWidget(words);
        int half = (W - LIST - 20 - 3) / 2;
        addRenderableWidget(Button.builder(Component.translatable("remote.eraphones.murmure"), b -> send("murmure", words.getValue()))
                .bounds(bx, y0 + 194, half, 18).build());
        addRenderableWidget(Button.builder(Component.translatable("remote.eraphones.narration"), b -> send("narration", words.getValue()))
                .bounds(bx + half + 3, y0 + 194, half, 18).build());
    }

    private void send(String action, String arg) {
        if (picked.isEmpty()) {
            return;
        }
        PacketDistributor.sendToServer(new HorrorNet.Remote(new ArrayList<>(picked), action, arg));
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        int listY = y0 + 30;
        int rows = (H - 30 - 34) / ROW;
        if (mx >= x0 + 8 && mx < x0 + LIST && my >= listY && my < listY + rows * ROW) {
            int i = scroll + (int) ((my - listY) / ROW);
            if (i < ids.size()) {
                String id = ids.get(i);
                if (!picked.remove(id)) {
                    picked.add(id);
                }
                return true;
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double sx, double sy) {
        int rows = (H - 30 - 34) / ROW;
        if (mx < x0 + LIST) {
            scroll = Math.max(0, Math.min(scroll - (int) Math.signum(sy), ids.size() - rows));
            return true;
        }
        return super.mouseScrolled(mx, my, sx, sy);
    }

    @Override
    public void renderBackground(GuiGraphics g, int mx, int my, float pt) {
        super.renderBackground(g, mx, my, pt);
        PhoneUI.round(g, x0, y0, W, H, 8, 0xF0140F12);
        PhoneUI.round(g, x0 + 1, y0 + 1, W - 2, 20, 7, 0xFF3A1418);
        PhoneUI.round(g, x0 + 6, y0 + 26, LIST - 4, H - 56, 4, 0xFF0B0809);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        super.render(g, mx, my, pt);
        PhoneUI.centered(g, title.getString(), x0 + W / 2f, y0 + 7, 0xFFFFD0D0, 1);
        int listY = y0 + 30, rows = (H - 30 - 34) / ROW;
        if (ids.isEmpty()) {
            PhoneUI.paragraph(g, Component.translatable("remote.eraphones.nobody").getString(), x0 + 6 + (LIST - 4) / 2f, listY + 20, LIST - 16, PhoneUI.DIM, 1);
        }
        for (int r = 0; r < rows && scroll + r < ids.size(); r++) {
            int i = scroll + r, y = listY + r * ROW;
            boolean on = picked.contains(ids.get(i));
            g.fill(x0 + 10, y + 3, x0 + 18, y + 11, on ? 0xFFE04848 : 0xFF3A3436);
            PhoneUI.text(g, PhoneUI.fit(names.get(i), LIST - 30, 1), x0 + 22, y + 3, on ? PhoneUI.WHITE : PhoneUI.FAINT, 1);
        }
        int bx = x0 + LIST + 12;
        PhoneUI.text(g, Component.translatable("remote.eraphones.frights").getString(), bx, y0 + 21, 0xFFC08080, .8f);
        PhoneUI.text(g, Component.translatable("remote.eraphones.sounds").getString(), bx, y0 + 83, 0xFFC08080, .8f);
        PhoneUI.text(g, Component.translatable("remote.eraphones.picked", picked.size()).getString(), bx, y0 + H - 18, PhoneUI.DIM, 1);
    }
}
