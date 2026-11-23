package com.waylays.arcanes.client.metier;

import com.waylays.arcanes.metier.MetierDefs;
import com.waylays.arcanes.metier.MetierNet;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A station's recipes (the crafting table, the smithing table, the still), as cards over the station's scene: what
 * each makes, what it takes (green: in the bag, red: missing), what a good and a perfect job give, how hard it is.
 * Choosing one starts its minigame. The cards are compact, so that up to six show at once. The recipes the game masters
 * put in folders (in /arcanes metiers) are listed under their folder's name, which a click opens or closes.
 */
public class StationScreen extends Screen {
    private static final int W = 330, CARD = 38, GAP = 4, LIST_Y = 52, HEAD = 18;
    /** The folders a player opened, per station, for as long as the game runs. */
    private static final Map<String, Set<String>> OPENED = new HashMap<>();

    /** A row of the list: a folder's name ({@code recipe} -1), or a recipe (its index in the station's list). */
    private record Row(String folder, int recipe, int count) {
        int height() {
            return recipe < 0 ? HEAD + GAP : CARD + GAP;
        }
    }
    /** Per game ({@link MetierDefs.Game} order): the colour of the frame and the scene behind the header. */
    private static final int[] THEMES = {0xFF8A7458, 0xFF5A8A48, 0xFF8A6438, 0xFFB0502A, 0xFF7A4AA8, 0xFF7A7A8A, 0xFF9A6A38, 0xFF3E9A86};
    private static final String[] SCENES = {"bg_mining", "bg_harvest", "bg_assembly", "bg_forge", "bg_distill", "bg_forge", "bg_carpentry", "bg_medicine"};
    private static final ResourceLocation SLOT = Draw.tex("slot");

    private final MetierNet.Station station;
    private final int theme;
    private final ResourceLocation scene;
    private float left, top, k = 1;
    private int h, scroll, listH;
    private int hovered = -1;
    @javax.annotation.Nullable
    private String hoveredFolder;
    private final Set<String> open;
    private long opened = net.minecraft.Util.getMillis();

    public StationScreen(MetierNet.Station station) {
        super(Component.literal(station.title()));
        this.station = station;
        int game = Mth.clamp(station.game(), 0, THEMES.length - 1);
        this.theme = THEMES[game];
        this.scene = Draw.tex(SCENES[game]);
        // Folders start open on a short list, closed on a long one
        this.open = OPENED.computeIfAbsent(station.def(), d -> station.recipes().size() <= 6 ? new HashSet<>(station.folders()) : new HashSet<>());
    }

    /** The list as shown: the recipes in no folder, then each folder, its recipes under it if it is open. */
    private List<Row> rows() {
        List<Row> rows = new ArrayList<>();
        List<MetierNet.RecipeView> recipes = station.recipes();
        for (int i = 0; i < recipes.size(); i++) {
            if (!station.folders().contains(recipes.get(i).folder())) {
                rows.add(new Row("", i, 0));
            }
        }
        for (String f : station.folders()) {
            List<Integer> in = new ArrayList<>();
            for (int i = 0; i < recipes.size(); i++) {
                if (recipes.get(i).folder().equals(f)) {
                    in.add(i);
                }
            }
            if (in.isEmpty()) {
                continue;
            }
            rows.add(new Row(f, -1, in.size()));
            if (open.contains(f)) {
                in.forEach(i -> rows.add(new Row(f, i, 0)));
            }
        }
        return rows;
    }

    /** The furthest the list scrolls: the first row from which all the rest fit. */
    private int maxScroll(List<Row> rows) {
        int used = 0;
        for (int i = rows.size() - 1; i >= 0; i--) {
            used += rows.get(i).height();
            if (used > listH) {
                return i + 1;
            }
        }
        return 0;
    }

    @Override
    protected void init() {
        // As tall as six cards, or the whole list with its folders open if it is shorter
        int all = 0;
        for (MetierNet.RecipeView r : station.recipes()) {
            all += CARD + GAP;
        }
        for (String f : station.folders()) {
            if (station.recipes().stream().anyMatch(r -> r.folder().equals(f))) {
                all += HEAD + GAP;
            }
        }
        listH = Mth.clamp(all, CARD + GAP, 6 * (CARD + GAP));
        h = LIST_Y + listH + 10;
        k = Mth.clamp(Math.min((width - 16) / (float) W, (height - 12) / (float) h), .5f, 2.6f);
        left = (width - W * k) / 2;
        top = (height - h * k) / 2;
        scroll = Mth.clamp(scroll, 0, maxScroll(rows()));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        g.fillGradient(0, 0, width, height, 0x90000000, 0xB8000000);
    }

    private static Component seconds(int s) {
        if (s >= 3600) {
            return Component.translatable("metier.arcanes_oublies.time_h", s / 3600, (s % 3600) / 60);
        }
        if (s >= 60) {
            return Component.translatable("metier.arcanes_oublies.time_m", s / 60, s % 60);
        }
        return Component.translatable("metier.arcanes_oublies.time_s", Math.max(1, s));
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        float mx = (mouseX - left) / k, my = (mouseY - top) / k;
        float appear = Draw.ease((net.minecraft.Util.getMillis() - opened) / 250f);
        g.pose().pushPose();
        g.pose().translate(left, top + (1 - appear) * 10, 0);
        g.pose().scale(k, k, 1);
        Draw.panel(g, 0, 0, W, h, theme);
        // The station's scene, dimmed, behind the header
        g.enableScissor((int) left, (int) top, (int) Math.ceil(left + W * k), (int) Math.ceil(top + 44 * k));
        Draw.picture(g, scene, 0, -40, W, -40 + W * 360 / 784f, 784, 360, 0xFF8A8A8A);
        g.disableScissor();
        Draw.vgrad(g, 1, 1, W - 1, 44, Draw.argb(60, theme), Draw.argb(230, Draw.lerp(theme, 0xFF000000, .8f)));
        Draw.glow(g, 26, 22, 24, Draw.argb(90, 0xFFFFE6B0));
        Draw.itemAt(g, station.icon(), 26, 22, 28);
        Draw.text(g, title, 48, 10, 0xFFFFF6E6, 1.4f);
        Draw.text(g, Component.translatable("metier.arcanes_oublies.station.choose"), 48, 27, 0xFFE0D6C4, .85f);
        if (station.cooldown() > 0) {
            Component c = Component.translatable("metier.arcanes_oublies.station.wait", seconds(station.cooldown()));
            Draw.text(g, c, W - 12 - Draw.width(c, .85f), 18, 0xFFFFC060, .85f);
        }
        hovered = -1;
        hoveredFolder = null;
        ItemStack tip = ItemStack.EMPTY;
        List<MetierNet.RecipeView> recipes = station.recipes();
        if (recipes.isEmpty()) {
            Draw.centered(g, Component.translatable("metier.arcanes_oublies.station.empty"), W / 2f, LIST_Y + 20, 0xFFA8A090, 1);
        }
        List<Row> rows = rows();
        scroll = Mth.clamp(scroll, 0, maxScroll(rows));
        float y = LIST_Y;
        int last = scroll;
        for (int ri = scroll; ri < rows.size() && y + rows.get(ri).height() - GAP <= LIST_Y + listH; ri++) {
            Row row = rows.get(ri);
            last = ri;
            if (row.recipe() < 0) {
                // A folder: its name, how many recipes it holds, open or closed
                boolean isOpen = open.contains(row.folder());
                boolean over = mx >= 10 && mx < W - 10 && my >= y && my < y + HEAD;
                if (over) {
                    hoveredFolder = row.folder();
                }
                Draw.vgrad(g, 10, y, W - 10, y + HEAD, over ? Draw.lerp(theme, 0xFF2A2420, .45f) : Draw.lerp(theme, 0xFF1A1410, .7f),
                        Draw.lerp(theme, 0xFF0C0A08, .82f));
                Draw.rect(g, 10, y, 12, y + HEAD, Draw.lerp(theme, 0xFFFFF0D0, .45f));
                Draw.rect(g, 10, y + HEAD - 1, W - 10, y + HEAD, Draw.argb(110, Draw.lerp(theme, 0xFFFFFFFF, .3f)));
                Draw.text(g, Component.literal(isOpen ? "▾" : "▸"), 18, y + 5, 0xFFFFE6B0, 1);
                Component name = Component.literal(row.folder());
                Draw.text(g, name, 30, y + 5, over ? 0xFFFFF6E6 : 0xFFE8DCC4, 1);
                Component count = Component.translatable("metier.arcanes_oublies.station.folder_count", row.count());
                Draw.text(g, count, W - 16 - Draw.width(count, .75f), y + 6, 0xFFB8AA9A, .75f);
                y += row.height();
                continue;
            }
            int index = row.recipe();
            MetierNet.RecipeView r = recipes.get(index);
            if (!row.folder().isEmpty()) {
                // A recipe in a folder: a thread down its left, from the folder's name
                Draw.rect(g, 11, y - GAP, 12, y + CARD, Draw.argb(120, Draw.lerp(theme, 0xFFFFF0D0, .45f)));
            }
            float cardY = y;
            y += row.height();
            boolean over = mx >= 10 && mx < W - 10 && my >= cardY && my < cardY + CARD;
            boolean ready = r.ready() && station.cooldown() <= 0;
            if (over) {
                hovered = index;
            }
            float lift = over ? -1.5f : 0;
            Draw.shadow(g, W / 2f, cardY + CARD / 2f + 3, W - 10, CARD + 12, over ? 150 : 90);
            Draw.vgrad(g, 10, cardY + lift, W - 10, cardY + CARD + lift, over && ready ? Draw.lerp(theme, 0xFF2A2420, .55f) : 0xF0201A16,
                    over && ready ? Draw.lerp(theme, 0xFF100C0A, .8f) : 0xF0120E0C);
            int edge = over ? (ready ? Draw.lerp(theme, 0xFFFFF0D0, .6f) : 0xFFB04A3A) : Draw.argb(120, Draw.lerp(theme, 0xFFFFFFFF, .3f));
            Draw.rect(g, 10, cardY + lift, W - 10, cardY + lift + 1, edge);
            Draw.rect(g, 10, cardY + CARD + lift - 1, W - 10, cardY + CARD + lift, Draw.argb(90, edge));
            // What it makes
            ItemStack shown = r.perfect().isEmpty() ? r.partial() : r.perfect();
            Draw.picture(g, SLOT, 14, cardY + 4 + lift, 44, cardY + 34 + lift, 72, 72, 0xFFFFFFFF);
            if (over && ready) {
                Draw.glow(g, 29, cardY + 19 + lift, 18, Draw.argb(90, 0xFFFFE0A0));
            }
            Draw.itemAt(g, shown, 29, cardY + 19 + lift, 20);
            Draw.text(g, Component.literal(r.name()), 50, cardY + 4 + lift, ready ? 0xFFFFF6E6 : 0xFFB8AA9A, .95f);
            int pips = (int) Mth.clamp(Math.round(r.difficulty() * 2), 1, 6);
            int pc = r.difficulty() > 1.25 ? 0xFFFF6A50 : r.difficulty() < .9 ? 0xFF8AE07A : 0xFFFFC850;
            for (int p = 0; p < 6; p++) {
                Draw.disc(g, 52 + Draw.width(Component.literal(r.name()), .95f) + 6 + p * 6, cardY + 8 + lift, 2f, p < pips ? pc : 0x38FFFFFF);
            }
            // What it takes: green in the bag, red missing
            float x = 50;
            for (int q = 0; q < r.inputs().size(); q++) {
                ItemStack in = r.inputs().get(q);
                boolean have = q < r.have().size() && r.have().get(q);
                float ix = x + q * 19, iy = cardY + 17 + lift;
                Draw.rect(g, ix - 1, iy - 1, ix + 17, iy + 17, have ? 0x5040C050 : 0x70D04030);
                Draw.rect(g, ix - 1, iy + 16, ix + 17, iy + 17, have ? 0xFF60D060 : 0xFFE05040);
                Draw.itemAt(g, in, ix + 8, iy + 8, 14);
                g.pose().pushPose();
                g.pose().translate(0, 0, 250);
                Component n = Component.literal(String.valueOf(in.getCount()));
                Draw.text(g, n, ix + 17 - Draw.width(n, .7f), iy + 10, 0xFFFFFFFF, .7f);
                g.pose().popPose();
                if (mx >= ix && mx < ix + 16 && my >= iy && my < iy + 16) {
                    tip = in;
                }
            }
            // What it gives: a good job, a perfect one
            ItemStack[] outs = {r.partial(), r.perfect()};
            int[] colors = {0xFF9AD8FF, 0xFFFFD25A};
            for (int q = 0; q < 2; q++) {
                ItemStack out = outs[q];
                if (out.isEmpty()) {
                    continue;
                }
                float ix = W - 58 + q * 24, iy = cardY + 15 + lift;
                Draw.glow(g, ix + 8, iy + 8, 12, Draw.argb(60, colors[q]));
                Draw.rect(g, ix - 1, iy + 17, ix + 17, iy + 18, colors[q]);
                Draw.itemAt(g, out, ix + 8, iy + 8, 16);
                if (q == 1) {
                    Draw.star(g, ix + 17, iy, 4, 0xFFFFE070);
                }
                g.pose().pushPose();
                g.pose().translate(0, 0, 250);
                if (out.getCount() > 1) {
                    Component n = Component.literal(String.valueOf(out.getCount()));
                    Draw.text(g, n, ix + 17 - Draw.width(n, .7f), iy + 10, 0xFFFFFFFF, .7f);
                }
                g.pose().popPose();
                if (mx >= ix && mx < ix + 16 && my >= iy && my < iy + 16) {
                    tip = out;
                }
            }
            if (r.cooldown() > 0) {
                Component c = Component.translatable("metier.arcanes_oublies.station.wait", seconds(r.cooldown()));
                Draw.text(g, c, W - 16 - Draw.width(c, .7f), cardY + 4 + lift, 0xFFFFC060, .7f);
            }
        }
        if (scroll > 0) {
            Draw.centered(g, Component.literal("▲"), W / 2f, LIST_Y - 9, 0xFFFFFFFF, .8f);
        }
        if (last + 1 < rows.size()) {
            Draw.centered(g, Component.literal("▼"), W / 2f, LIST_Y + listH - 4, 0xFFFFFFFF, .8f);
        }
        g.pose().popPose();
        if (!tip.isEmpty()) {
            g.renderTooltip(font, tip, mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && hoveredFolder != null) {
            if (!open.remove(hoveredFolder)) {
                open.add(hoveredFolder);
            }
            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.BOOK_PAGE_TURN, 1, 1.1f));
            scroll = Mth.clamp(scroll, 0, maxScroll(rows()));
            return true;
        }
        if (button == 0 && hovered >= 0) {
            MetierNet.RecipeView r = station.recipes().get(hovered);
            if (!r.ready() || station.cooldown() > 0) {
                Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.VILLAGER_NO, 1, .5f));
                return true;
            }
            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK.value(), 1, .6f));
            PacketDistributor.sendToServer(new MetierNet.Pick(station.def(), station.pos(), hovered));
            onClose();
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        scroll = Mth.clamp(scroll - (int) Math.signum(scrollY), 0, maxScroll(rows()));
        return true;
    }

    /** The game shown for this station (for the smoke test). */
    public MetierDefs.Game game() {
        return MetierDefs.Game.values()[Mth.clamp(station.game(), 0, MetierDefs.Game.values().length - 1)];
    }
}
