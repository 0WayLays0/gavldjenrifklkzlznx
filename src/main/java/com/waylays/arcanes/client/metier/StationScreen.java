package com.waylays.arcanes.client.metier;

import com.waylays.arcanes.metier.MetierDefs;
import com.waylays.arcanes.metier.MetierNet;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * A station's recipes (the crafting table, the smithing table, the still...), as cards over the station's scene: what
 * each makes, what it takes (green: in the bag, red: missing), what a good and a perfect job give, how hard it is.
 * Choosing one starts its minigame. The cards are compact, so that up to six show at once.
 * <p>
 * Recipes can be sorted into folders, listed first; a folder opens on its own recipes. A game master organises them
 * here ("Organiser"): new folders, renamed, moved up or down, deleted (their recipes go back out), and each recipe moved
 * up or down or into another folder. Every change is sent to the server at once and saved with the trades.
 */
public class StationScreen extends Screen {
    private static final int W = 330, CARD = 38, GAP = 4, LIST_Y = 52;
    /** Per game ({@link MetierDefs.Game} order): the colour of the frame and the scene behind the header. */
    private static final int[] THEMES = {0xFF8A7458, 0xFF5A8A48, 0xFF8A6438, 0xFFB0502A, 0xFF7A4AA8, 0xFF7A7A8A, 0xFF9A6A38, 0xFF3E9A86};
    private static final String[] SCENES = {"bg_mining", "bg_harvest", "bg_assembly", "bg_forge", "bg_distill", "bg_forge", "bg_carpentry", "bg_medicine"};
    private static final ResourceLocation SLOT = Draw.tex("slot");

    /** Something on screen that does something when clicked, in window units. */
    private record Zone(float x0, float y0, float x1, float y1, Runnable action) {
        boolean contains(float x, float y) {
            return x >= x0 && x < x1 && y >= y0 && y < y1;
        }
    }

    /** A line of the list: a folder, or a recipe (its index in the station's recipes). */
    private record Entry(@Nullable String folder, int recipe) {}

    private MetierNet.Station station;
    private final int theme;
    private final ResourceLocation scene;
    private float left, top, k = 1;
    private int h, scroll, visible;
    private long opened = net.minecraft.Util.getMillis();
    /** The folder open, or null (the station's own list). */
    @Nullable
    private String current;
    private boolean organizing;
    /** The organisation as shown: folders in order, recipes in order (station indices), the folder of each recipe. */
    private final List<String> folders = new ArrayList<>();
    private final List<Integer> order = new ArrayList<>();
    private final List<String> folderOf = new ArrayList<>();
    private final List<Zone> zones = new ArrayList<>();
    /** The recipe being moved to another folder (its picker is open), or -1. */
    private int moving = -1;
    /** The folder being renamed, and its box. */
    @Nullable
    private String renaming;
    @Nullable
    private EditBox nameBox;

    public StationScreen(MetierNet.Station station) {
        super(Component.literal(station.title()));
        this.station = station;
        int game = Mth.clamp(station.game(), 0, THEMES.length - 1);
        this.theme = THEMES[game];
        this.scene = Draw.tex(SCENES[game]);
        take(station);
    }

    /** Whether {@code other} is this station again (its screen is then refreshed in place). */
    public boolean sameStation(MetierNet.Station other) {
        return other.def().equals(station.def()) && other.pos().equals(station.pos());
    }

    /** The server's new list for this station (after an organisation): shown in place, the open folder kept if it is still there. */
    public void update(MetierNet.Station next) {
        this.station = next;
        take(next);
        if (current != null && !folders.contains(current)) {
            current = null;
        }
        if (renaming != null && !folders.contains(renaming)) {
            stopRenaming(false);
        }
        rebuild();
    }

    private void take(MetierNet.Station s) {
        folders.clear();
        order.clear();
        folderOf.clear();
        for (String f : s.folders()) {
            if (!f.isEmpty() && !folders.contains(f)) {
                folders.add(f);
            }
        }
        for (int i = 0; i < s.recipes().size(); i++) {
            String f = s.recipes().get(i).folder();
            order.add(i);
            folderOf.add(f == null ? "" : f);
            if (f != null && !f.isEmpty() && !folders.contains(f)) {
                folders.add(f);
            }
        }
    }

    private List<Entry> entries(@Nullable String folder) {
        List<Entry> out = new ArrayList<>();
        if (folder == null) {
            for (String f : folders) {
                out.add(new Entry(f, -1));
            }
        }
        String want = folder == null ? "" : folder;
        for (int i : order) {
            if (folderOf.get(i).equals(want)) {
                out.add(new Entry(null, i));
            }
        }
        return out;
    }

    private int countIn(String folder) {
        int n = 0;
        for (String f : folderOf) {
            if (f.equals(folder)) {
                n++;
            }
        }
        return n;
    }

    @Override
    protected void init() {
        int most = entries(null).size();
        for (String f : folders) {
            most = Math.max(most, countIn(f));
        }
        visible = Math.max(1, Math.min(Math.max(1, most), 6));
        h = LIST_Y + visible * (CARD + GAP) + 10;
        k = Mth.clamp(Math.min((width - 16) / (float) W, (height - 12) / (float) h), .5f, 2.6f);
        left = (width - W * k) / 2;
        top = (height - h * k) / 2;
        scroll = Mth.clamp(scroll, 0, Math.max(0, entries(current).size() - visible));
        if (nameBox != null) {
            addWidget(nameBox);
        }
    }

    private void rebuild() {
        if (minecraft != null) {
            rebuildWidgets();
        }
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

    private static Component tr(String key, Object... args) {
        return Component.translatable("metier.arcanes_oublies.station." + key, args);
    }

    /** A rounded button; returns its width. */
    private float button(GuiGraphics g, float x, float y, Component label, boolean over, int color, Runnable action) {
        float w = Draw.width(label, .8f) + 12;
        Draw.pill(g, x, y, x + w, y + 13, over ? Draw.lerp(color, 0xFFFFFFFF, .25f) : Draw.argb(220, Draw.lerp(color, 0xFF000000, .35f)));
        Draw.centered(g, label, x + w / 2, y + 3, 0xFFFFFFFF, .8f);
        zones.add(new Zone(x, y, x + w, y + 13, action));
        return w;
    }

    /** A small square button with one sign on it, at the right of a card. */
    private void tiny(GuiGraphics g, float x, float y, String sign, boolean over, int color, Runnable action) {
        Draw.rect(g, x, y, x + 13, y + 13, over ? Draw.lerp(color, 0xFFFFFFFF, .3f) : Draw.argb(200, Draw.lerp(color, 0xFF000000, .45f)));
        Draw.rect(g, x, y + 12, x + 13, y + 13, Draw.argb(200, Draw.lerp(color, 0xFF000000, .7f)));
        Draw.centered(g, Component.literal(sign), x + 6.5f, y + 3, 0xFFFFFFFF, .8f);
        zones.add(new Zone(x, y, x + 13, y + 13, action));
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        zones.clear();
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
        Draw.text(g, title, 48, 8, 0xFFFFF6E6, 1.4f);
        Component sub = current != null ? tr("in_folder", current) : organizing ? tr("organizing") : tr("choose");
        Draw.text(g, sub, 48, 27, current != null ? 0xFFFFE0A0 : 0xFFE0D6C4, .85f);
        // Buttons on the right of the header
        float bx = W - 10;
        if (station.canEdit()) {
            Component label = organizing ? tr("done") : tr("organize");
            float w = Draw.width(label, .8f) + 12;
            boolean over = mx >= bx - w && mx < bx && my >= 6 && my < 19;
            button(g, bx - w, 6, label, over, organizing ? 0xFF4AA060 : theme, () -> {
                organizing = !organizing;
                moving = -1;
                stopRenaming(true);
            });
        }
        float rowX = W - 10;
        if (current != null) {
            Component back = tr("back");
            float w = Draw.width(back, .8f) + 12;
            rowX -= w;
            button(g, rowX, 25, back, mx >= rowX && mx < rowX + w && my >= 25 && my < 38, theme, () -> openFolder(null));
            rowX -= 4;
        } else if (organizing) {
            Component add = tr("new_folder");
            float w = Draw.width(add, .8f) + 12;
            rowX -= w;
            button(g, rowX, 25, add, mx >= rowX && mx < rowX + w && my >= 25 && my < 38, 0xFF4A80C0, this::newFolder);
            rowX -= 4;
        }
        if (station.cooldown() > 0) {
            Component c = tr("wait", seconds(station.cooldown()));
            Draw.text(g, c, rowX - Draw.width(c, .8f), 28, 0xFFFFC060, .8f);
        }
        ItemStack tip = ItemStack.EMPTY;
        List<Entry> list = entries(current);
        scroll = Mth.clamp(scroll, 0, Math.max(0, list.size() - visible));
        if (list.isEmpty()) {
            Draw.centered(g, current != null ? tr("folder_empty") : tr("empty"), W / 2f, LIST_Y + 20, 0xFFA8A090, 1);
        }
        for (int i = 0; i < visible && i + scroll < list.size(); i++) {
            Entry e = list.get(i + scroll);
            float y = LIST_Y + i * (CARD + GAP);
            boolean over = mx >= 10 && mx < W - 10 && my >= y && my < y + CARD;
            ItemStack t = e.folder() != null ? folderCard(g, e.folder(), y, over, mx, my) : recipeCard(g, e.recipe(), y, over, mx, my);
            if (!t.isEmpty()) {
                tip = t;
            }
        }
        if (scroll > 0) {
            Draw.centered(g, Component.literal("▲"), W / 2f, LIST_Y - 9, 0xFFFFFFFF, .8f);
        }
        if (scroll + visible < list.size()) {
            Draw.centered(g, Component.literal("▼"), W / 2f, LIST_Y + visible * (CARD + GAP) - 4, 0xFFFFFFFF, .8f);
        }
        if (moving >= 0) {
            drawPicker(g, mx, my);
        }
        g.pose().popPose();
        if (nameBox != null) {
            // The box is drawn by the screen (over the panel)
            g.pose().pushPose();
            g.pose().translate(0, 0, 400);
            nameBox.render(g, mouseX, mouseY, partialTick);
            g.pose().popPose();
        }
        if (!tip.isEmpty() && moving < 0) {
            g.renderTooltip(font, tip, mouseX, mouseY);
        }
    }

    /** A folder: a drawn folder, its name, how many recipes, a few of what they make. */
    private ItemStack folderCard(GuiGraphics g, String folder, float y, boolean over, float mx, float my) {
        // The card opens the folder; its buttons, added after, are found first
        zones.add(new Zone(10, y, W - 10, y + CARD, () -> openFolder(folder)));
        float lift = over ? -1.5f : 0;
        Draw.shadow(g, W / 2f, y + CARD / 2f + 3, W - 10, CARD + 12, over ? 150 : 90);
        Draw.vgrad(g, 10, y + lift, W - 10, y + CARD + lift, over ? Draw.lerp(theme, 0xFF2A2420, .5f) : 0xF0241E18,
                over ? Draw.lerp(theme, 0xFF100C0A, .78f) : 0xF0140F0C);
        int edge = over ? Draw.lerp(theme, 0xFFFFF0D0, .6f) : Draw.argb(140, Draw.lerp(theme, 0xFFFFE0A0, .4f));
        Draw.rect(g, 10, y + lift, W - 10, y + lift + 1, edge);
        Draw.rect(g, 10, y + CARD + lift - 1, W - 10, y + CARD + lift, Draw.argb(90, edge));
        // The folder itself: a tab, a back, a front a little open
        int paper = Draw.lerp(theme, 0xFFFFD890, .55f);
        float fx = 15, fy = y + 7 + lift;
        Draw.rect(g, fx, fy, fx + 11, fy + 4, Draw.lerp(paper, 0xFF000000, .3f));
        Draw.rect(g, fx, fy + 3, fx + 28, fy + 24, Draw.lerp(paper, 0xFF000000, .35f));
        Draw.rect(g, fx + 3, fy + 5, fx + 26, fy + 12, 0xFFF4EEE0);
        Draw.vgrad(g, fx + (over ? 2 : 0), fy + 9, fx + 28 + (over ? 2 : 0), fy + 24, paper, Draw.lerp(paper, 0xFF000000, .2f));
        Draw.rect(g, fx + (over ? 2 : 0), fy + 9, fx + 28 + (over ? 2 : 0), fy + 10, Draw.lerp(paper, 0xFFFFFFFF, .5f));
        int n = countIn(folder);
        Draw.text(g, Component.literal(folder), 50, y + 6 + lift, 0xFFFFF6E6, 1);
        Draw.text(g, tr(n == 1 ? "recipes_one" : "recipes", n), 50, y + 20 + lift, 0xFFB8AA9A, .75f);
        // A few of what is inside
        ItemStack tip = ItemStack.EMPTY;
        float ix = 50 + Math.max(Draw.width(Component.literal(folder), 1), Draw.width(tr("recipes", n), .75f)) + 10;
        int shown = 0;
        for (int i : order) {
            if (shown >= 6 || ix > W - (organizing ? 80 : 30)) {
                break;
            }
            if (!folderOf.get(i).equals(folder)) {
                continue;
            }
            MetierNet.RecipeView r = station.recipes().get(i);
            ItemStack out = r.perfect().isEmpty() ? r.partial() : r.perfect();
            Draw.itemAt(g, out, ix + 8, y + 19 + lift, 14);
            if (mx >= ix && mx < ix + 16 && my >= y + 11 && my < y + 27) {
                tip = out;
            }
            ix += 18;
            shown++;
        }
        if (organizing) {
            float bx = W - 16 - 4 * 15, by = y + 12 + lift;
            int index = folders.indexOf(folder);
            tiny(g, bx, by, "▲", hit(mx, my, bx, by), theme, () -> moveFolder(index, -1));
            tiny(g, bx + 15, by, "▼", hit(mx, my, bx + 15, by), theme, () -> moveFolder(index, 1));
            tiny(g, bx + 30, by, "✎", hit(mx, my, bx + 30, by), 0xFF4A80C0, () -> startRenaming(folder, y));
            tiny(g, bx + 45, by, "✕", hit(mx, my, bx + 45, by), 0xFFB04A3A, () -> deleteFolder(folder));
        } else {
            Draw.text(g, Component.literal("›"), W - 22, y + 12 + lift, over ? 0xFFFFFFFF : 0xFF9A9080, 1.4f);
        }
        return tip;
    }

    private static boolean hit(float mx, float my, float x, float y) {
        return mx >= x && mx < x + 13 && my >= y && my < y + 13;
    }

    /** A recipe: what it makes, its name and difficulty, what it takes, what it gives (or, organising, its buttons). */
    private ItemStack recipeCard(GuiGraphics g, int index, float y, boolean over, float mx, float my) {
        MetierNet.RecipeView r = station.recipes().get(index);
        boolean ready = r.ready() && station.cooldown() <= 0;
        ItemStack tip = ItemStack.EMPTY;
        float lift = over ? -1.5f : 0;
        Draw.shadow(g, W / 2f, y + CARD / 2f + 3, W - 10, CARD + 12, over ? 150 : 90);
        Draw.vgrad(g, 10, y + lift, W - 10, y + CARD + lift, over && ready ? Draw.lerp(theme, 0xFF2A2420, .55f) : 0xF0201A16,
                over && ready ? Draw.lerp(theme, 0xFF100C0A, .8f) : 0xF0120E0C);
        int edge = over ? (ready || organizing ? Draw.lerp(theme, 0xFFFFF0D0, .6f) : 0xFFB04A3A) : Draw.argb(120, Draw.lerp(theme, 0xFFFFFFFF, .3f));
        Draw.rect(g, 10, y + lift, W - 10, y + lift + 1, edge);
        Draw.rect(g, 10, y + CARD + lift - 1, W - 10, y + CARD + lift, Draw.argb(90, edge));
        // What it makes
        ItemStack shown = r.perfect().isEmpty() ? r.partial() : r.perfect();
        Draw.picture(g, SLOT, 14, y + 4 + lift, 44, y + 34 + lift, 72, 72, 0xFFFFFFFF);
        if (over && ready) {
            Draw.glow(g, 29, y + 19 + lift, 18, Draw.argb(90, 0xFFFFE0A0));
        }
        Draw.itemAt(g, shown, 29, y + 19 + lift, 20);
        Draw.text(g, Component.literal(r.name()), 50, y + 4 + lift, ready ? 0xFFFFF6E6 : 0xFFB8AA9A, .95f);
        int pips = (int) Mth.clamp(Math.round(r.difficulty() * 2), 1, 6);
        int pc = r.difficulty() > 1.25 ? 0xFFFF6A50 : r.difficulty() < .9 ? 0xFF8AE07A : 0xFFFFC850;
        for (int p = 0; p < 6; p++) {
            Draw.disc(g, 52 + Draw.width(Component.literal(r.name()), .95f) + 6 + p * 6, y + 8 + lift, 2f, p < pips ? pc : 0x38FFFFFF);
        }
        // What it takes: green in the bag, red missing
        float x = 50;
        for (int q = 0; q < r.inputs().size(); q++) {
            ItemStack in = r.inputs().get(q);
            boolean have = q < r.have().size() && r.have().get(q);
            float ix = x + q * 19, iy = y + 17 + lift;
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
        if (organizing) {
            float bx = W - 16 - 3 * 15, by = y + 12 + lift;
            tiny(g, bx, by, "▲", hit(mx, my, bx, by), theme, () -> moveRecipe(index, -1));
            tiny(g, bx + 15, by, "▼", hit(mx, my, bx + 15, by), theme, () -> moveRecipe(index, 1));
            tiny(g, bx + 30, by, "⇄", hit(mx, my, bx + 30, by), 0xFF4A80C0, () -> moving = index);
            return tip;
        }
        // What it gives: a good job, a perfect one
        ItemStack[] outs = {r.partial(), r.perfect()};
        int[] colors = {0xFF9AD8FF, 0xFFFFD25A};
        for (int q = 0; q < 2; q++) {
            ItemStack out = outs[q];
            if (out.isEmpty()) {
                continue;
            }
            float ix = W - 58 + q * 24, iy = y + 15 + lift;
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
            Component c = tr("wait", seconds(r.cooldown()));
            Draw.text(g, c, W - 16 - Draw.width(c, .7f), y + 4 + lift, 0xFFFFC060, .7f);
        }
        zones.add(new Zone(10, y, W - 10, y + CARD, () -> pick(index)));
        return tip;
    }

    /** The folders a recipe can be moved to, over the list. */
    private void drawPicker(GuiGraphics g, float mx, float my) {
        zones.clear();
        List<String> targets = new ArrayList<>();
        targets.add("");
        targets.addAll(folders);
        float pw = 180, ph = 26 + targets.size() * 16;
        float px = (W - pw) / 2, py = Math.max(8, (h - ph) / 2);
        g.pose().pushPose();
        g.pose().translate(0, 0, 300);
        Draw.rect(g, 0, 0, W, h, 0x90000000);
        Draw.panel(g, px, py, px + pw, py + ph, theme);
        MetierNet.RecipeView r = station.recipes().get(moving);
        Draw.centered(g, tr("move_to", r.name()), px + pw / 2, py + 7, 0xFFFFF6E6, .85f);
        String now = folderOf.get(moving);
        for (int i = 0; i < targets.size(); i++) {
            String f = targets.get(i);
            float y = py + 22 + i * 16;
            boolean over = mx >= px + 6 && mx < px + pw - 6 && my >= y && my < y + 14;
            Draw.rect(g, px + 6, y, px + pw - 6, y + 14, over ? Draw.lerp(theme, 0xFFFFFFFF, .2f) : f.equals(now) ? 0x60FFFFFF : 0x30FFFFFF);
            Component label = f.isEmpty() ? tr("no_folder") : Component.literal(f);
            Draw.text(g, label, px + 12, y + 3, f.equals(now) ? 0xFFFFE0A0 : 0xFFFFFFFF, .8f);
            int recipe = moving;
            zones.add(new Zone(px + 6, y, px + pw - 6, y + 14, () -> {
                folderOf.set(recipe, f);
                moving = -1;
                send();
            }));
        }
        g.pose().popPose();
        // A click anywhere else closes it
        zones.add(0, new Zone(-10000, -10000, 10000, 10000, () -> moving = -1));
    }

    // ---------------------------------------------------------------- doing

    private void click() {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK.value(), 1, .6f));
    }

    private void pick(int index) {
        if (organizing) {
            return;
        }
        MetierNet.RecipeView r = station.recipes().get(index);
        if (!r.ready() || station.cooldown() > 0) {
            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.VILLAGER_NO, 1, .5f));
            return;
        }
        click();
        PacketDistributor.sendToServer(new MetierNet.Pick(station.def(), station.pos(), index));
        onClose();
    }

    private void openFolder(@Nullable String folder) {
        if (Objects.equals(folder, current)) {
            return;
        }
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.BOOK_PAGE_TURN, 1, .8f));
        stopRenaming(true);
        current = folder;
        scroll = 0;
    }

    private void moveFolder(int index, int by) {
        int to = index + by;
        if (index < 0 || to < 0 || to >= folders.size()) {
            return;
        }
        folders.add(to, folders.remove(index));
        send();
    }

    private void deleteFolder(String folder) {
        folders.remove(folder);
        for (int i = 0; i < folderOf.size(); i++) {
            if (folderOf.get(i).equals(folder)) {
                folderOf.set(i, "");
            }
        }
        send();
        rebuild();
    }

    /** Moves a recipe before the previous (or after the next) recipe of its own folder. */
    private void moveRecipe(int recipe, int by) {
        String folder = folderOf.get(recipe);
        int at = order.indexOf(recipe);
        int to = at + by;
        while (to >= 0 && to < order.size() && !folderOf.get(order.get(to)).equals(folder)) {
            to += by;
        }
        if (to < 0 || to >= order.size()) {
            return;
        }
        order.set(at, order.get(to));
        order.set(to, recipe);
        send();
    }

    private void newFolder() {
        String base = tr("new_folder_name").getString();
        String name = base;
        for (int i = 2; folders.contains(name); i++) {
            name = base + " " + i;
        }
        folders.add(name);
        send();
        rebuild();
        // Its card, at the end of the folders: scrolled to, and renamed straight away
        int at = folders.size() - 1;
        scroll = Mth.clamp(at - visible + 1, 0, Math.max(0, entries(null).size() - visible));
        startRenaming(name, LIST_Y + (at - scroll) * (CARD + GAP));
    }

    private void startRenaming(String folder, float cardY) {
        stopRenaming(false);
        renaming = folder;
        int bx = (int) (left + 48 * k), by = (int) (top + (cardY + 3) * k);
        nameBox = new EditBox(font, bx, by, (int) Math.min(220, (W - 140) * k), 18, Component.empty());
        nameBox.setMaxLength(40);
        nameBox.setValue(folder);
        nameBox.setFocused(true);
        addWidget(nameBox);
        setFocused(nameBox);
    }

    /** Ends the renaming, taking the new name if {@code keep}. */
    private void stopRenaming(boolean keep) {
        if (renaming != null && nameBox != null && keep) {
            String name = MetierDefs.folderName(nameBox.getValue());
            int at = folders.indexOf(renaming);
            if (!name.isEmpty() && !name.equals(renaming) && !folders.contains(name) && at >= 0) {
                folders.set(at, name);
                for (int i = 0; i < folderOf.size(); i++) {
                    if (folderOf.get(i).equals(renaming)) {
                        folderOf.set(i, name);
                    }
                }
                if (renaming.equals(current)) {
                    current = name;
                }
                send();
            }
        }
        if (nameBox != null) {
            removeWidget(nameBox);
        }
        nameBox = null;
        renaming = null;
    }

    /** Sends the organisation as it now stands (the server saves it and sends the list back). */
    private void send() {
        click();
        List<String> perRecipe = new ArrayList<>();
        for (int i : order) {
            perRecipe.add(folderOf.get(i));
        }
        PacketDistributor.sendToServer(new MetierNet.Organize(station.def(), station.pos(), List.copyOf(folders), List.copyOf(order), perRecipe));
    }

    // ---------------------------------------------------------------- input

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (nameBox != null && nameBox.isMouseOver(mouseX, mouseY)) {
            return super.mouseClicked(mouseX, mouseY, button);
        }
        if (nameBox != null) {
            stopRenaming(true);
        }
        if (button == 0) {
            float mx = (float) ((mouseX - left) / k), my = (float) ((mouseY - top) / k);
            for (int i = zones.size() - 1; i >= 0; i--) {
                Zone z = zones.get(i);
                if (z.contains(mx, my)) {
                    z.action().run();
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int key, int scan, int modifiers) {
        if (nameBox != null) {
            if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) {
                stopRenaming(true);
                return true;
            }
            if (key == GLFW.GLFW_KEY_ESCAPE) {
                stopRenaming(false);
                return true;
            }
            return nameBox.keyPressed(key, scan, modifiers) || super.keyPressed(key, scan, modifiers);
        }
        if (key == GLFW.GLFW_KEY_ESCAPE && moving >= 0) {
            moving = -1;
            return true;
        }
        if ((key == GLFW.GLFW_KEY_ESCAPE || key == GLFW.GLFW_KEY_BACKSPACE) && current != null) {
            openFolder(null);
            return true;
        }
        return super.keyPressed(key, scan, modifiers);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        scroll = Mth.clamp(scroll - (int) Math.signum(scrollY), 0, Math.max(0, entries(current).size() - visible));
        return true;
    }

    /** The game shown for this station (for the smoke test). */
    public MetierDefs.Game game() {
        return MetierDefs.Game.values()[Mth.clamp(station.game(), 0, MetierDefs.Game.values().length - 1)];
    }

    /** Opens a folder, or the station's own list (null) (for the smoke test). */
    public void showFolder(@Nullable String folder) {
        current = folder != null && folders.contains(folder) ? folder : null;
        scroll = 0;
    }
}
