package com.waylays.arcanes.client.metier;

import com.waylays.arcanes.metier.MetierDefs;
import com.waylays.arcanes.metier.MetierDefs.Def;
import com.waylays.arcanes.metier.MetierDefs.Game;
import com.waylays.arcanes.metier.MetierDefs.Ingredient;
import com.waylays.arcanes.metier.MetierDefs.Recipe;
import com.waylays.arcanes.metier.MetierDefs.Reward;
import com.waylays.arcanes.metier.MetierDefs.Tool;
import com.waylays.arcanes.metier.MetierNet;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * The trades' editor ({@code /arcanes metiers}), laid out to be read at a glance: the trades on the left, deposits and
 * stations apart; for the one chosen, a few short tabs (settings, blocks and tools, rewards or recipes) of cards. Items,
 * blocks and tools are shown as chips; a click opens a small window to change or remove one. The stations' recipes
 * have their own list, which can be sorted. Saved to the server, which writes {@code config/arcanes_oublies/metiers.json}.
 */
public class MetierEditorScreen extends Screen {
    private static final int GOLD = 0xFFDEB25C, GOLD_LIGHT = 0xFFFFE6A0, TEXT = 0xFFF2ECE0, DIM = 0xFFA89E8C, RED = 0xFFFF6A5A, GREEN = 0xFF8AE07A,
            BLUE = 0xFF8FD4FF, CARD = 0xC01C1714, CARD_EDGE = 0x605A4630;
    private static final int SIDE = 150, ROW = 22, CHIP_H = 20;
    private static final int[] GAME_COLORS = {0xFFB09A80, 0xFF7ABA62, 0xFFC09060, 0xFFE07848, 0xFFA87AE0};
    private static final ItemStack[] GAME_ICONS = {new ItemStack(Items.IRON_PICKAXE), new ItemStack(Items.TORCHFLOWER),
            new ItemStack(Items.CRAFTING_TABLE), new ItemStack(Items.SMITHING_TABLE), new ItemStack(Items.BREWING_STAND)};

    private enum Tab {SETTINGS, BLOCKS, REWARDS, RECIPES}

    /** Something to click. Style: 0 button, 1 list row, 2 chip, 3 tab, 4 "add" chip. */
    private record Btn(int x, int y, int w, int h, Supplier<Component> label, Runnable action, @Nullable Component tip, int color,
                       Supplier<Boolean> lit, @Nullable Supplier<ItemStack> icon, int style) {}

    private record Label(int x, int y, Component text, int color, float scale) {}

    private record Card(int x0, int y0, int x1, int y1, Component title, int color) {}

    private record Num(DoubleConsumer set, Supplier<Double> get, double step, double min, double max, boolean integer) {}

    private MetierDefs.Config config;
    private String savedJson;
    private int selected, recipe;
    private Tab tab = Tab.SETTINGS;
    private int scroll, listScroll, contentBottom, listBottom;
    private int left, top, w, h;
    private int mx0, mx1, cy0, cy1;
    private final List<Btn> buttons = new ArrayList<>(), popupButtons = new ArrayList<>();
    private final List<Label> labels = new ArrayList<>(), popupLabels = new ArrayList<>();
    private final List<Card> cards = new ArrayList<>();
    private final List<int[]> icons = new ArrayList<>();
    private final List<ItemStack> iconStacks = new ArrayList<>();
    private final Map<EditBox, Num> numbers = new HashMap<>();
    private final List<AbstractWidget> background = new ArrayList<>();
    // The small window open over the rest (to change one item, block or tool)
    @Nullable
    private Component popupTitle;
    @Nullable
    private Consumer<int[]> popupBuild;
    private int px0, py0, px1, py1;
    private boolean buildingPopup;
    @Nullable
    private Component status;
    private int statusColor;
    private long statusAt, confirmAt = -10000;

    public MetierEditorScreen(MetierDefs.Config config) {
        super(Component.translatable("metier.arcanes_oublies.editor.title"));
        this.config = MetierDefs.copy(config);
        this.savedJson = MetierDefs.toJson(this.config);
    }

    /** The server's trades, after a save. */
    void reload(MetierDefs.Config next) {
        config = MetierDefs.copy(next);
        savedJson = MetierDefs.toJson(config);
        selected = Mth.clamp(selected, 0, Math.max(0, config.metiers.size() - 1));
        rebuildWidgets();
    }

    private boolean dirty() {
        return !MetierDefs.toJson(config).equals(savedJson);
    }

    @Nullable
    private Def def() {
        return selected >= 0 && selected < config.metiers.size() ? config.metiers.get(selected) : null;
    }

    @Nullable
    private Recipe currentRecipe() {
        Def d = def();
        return d == null || d.recipes.isEmpty() ? null : d.recipes.get(Mth.clamp(recipe, 0, d.recipes.size() - 1));
    }

    private static Component tr(String key, Object... args) {
        return Component.translatable("metier.arcanes_oublies.editor." + key, args);
    }

    private static HolderLookup.Provider registries() {
        Minecraft mc = Minecraft.getInstance();
        return mc.level != null ? mc.level.registryAccess() : net.minecraft.core.RegistryAccess.EMPTY;
    }

    private static boolean itemExists(String id) {
        ResourceLocation rl = ResourceLocation.tryParse(id);
        return rl != null && BuiltInRegistries.ITEM.containsKey(rl);
    }

    private static boolean blockExists(String id) {
        ResourceLocation rl = ResourceLocation.tryParse(id);
        return rl != null && BuiltInRegistries.BLOCK.containsKey(rl);
    }

    private static ItemStack icon(String id) {
        return new ItemStack(MetierDefs.item(id));
    }

    private static ItemStack blockIcon(String id) {
        BlockState state = MetierDefs.block(id);
        return state == null ? ItemStack.EMPTY : new ItemStack(state.getBlock().asItem());
    }

    private static Component gameName(Game game) {
        return Component.translatable("metier.arcanes_oublies.game." + game.name().toLowerCase(Locale.ROOT));
    }

    private static Component time(int s) {
        if (s >= 3600) {
            return Component.translatable("metier.arcanes_oublies.time_h", s / 3600, (s % 3600) / 60);
        }
        if (s >= 60) {
            return Component.translatable("metier.arcanes_oublies.time_m", s / 60, s % 60);
        }
        return Component.translatable("metier.arcanes_oublies.time_s", s);
    }

    private static String num(double v) {
        String s = String.format(Locale.ROOT, "%.2f", v).replaceAll("0+$", "");
        return s.endsWith(".") ? s.substring(0, s.length() - 1) : s;
    }

    // ---------------------------------------------------------------- layout

    @Override
    protected void init() {
        w = Math.min(width - 16, 640);
        h = Math.min(height - 16, 410);
        left = (width - w) / 2;
        top = (height - h) / 2;
        mx0 = left + SIDE + 18;
        mx1 = left + w - 10;
        cy0 = top + 112;
        cy1 = top + h - 22;
        buttons.clear();
        popupButtons.clear();
        labels.clear();
        popupLabels.clear();
        cards.clear();
        icons.clear();
        iconStacks.clear();
        numbers.clear();
        background.clear();
        Def d = def();
        if (d != null) {
            if (tab == Tab.RECIPES && !d.isStation()) tab = Tab.REWARDS;
            if (tab == Tab.REWARDS && d.isStation()) tab = Tab.RECIPES;
        }
        buildHeader();
        buildList();
        buildTrade();
        buildContent();
        if (popupBuild != null) {
            buildingPopup = true;
            int pw = 290, ph = 168;
            px0 = (width - pw) / 2;
            py0 = (height - ph) / 2;
            px1 = px0 + pw;
            py1 = py0 + ph;
            popupBuild.accept(new int[] {px0 + 14, py0 + 32});
            buildingPopup = false;
        }
    }

    private void add(Btn b) {
        (buildingPopup ? popupButtons : buttons).add(b);
    }

    private void btn(int x, int y, int bw, Component label, Runnable action, @Nullable Component tip, int color) {
        add(new Btn(x, y, bw, 18, () -> label, action, tip, color, () -> false, null, 0));
    }

    private void change(Runnable r) {
        r.run();
        rebuildWidgets();
    }

    private void open(Component title, Consumer<int[]> build) {
        popupTitle = title;
        popupBuild = build;
        rebuildWidgets();
    }

    private void closePopup() {
        popupBuild = null;
        popupTitle = null;
        rebuildWidgets();
    }

    private void addBox(EditBox box) {
        if (buildingPopup || popupBuild == null) {
            addRenderableWidget(box);
        } else {
            box.setEditable(false);
            background.add(box);
        }
    }

    private EditBox field(int x, int y, int fw, String value, Consumer<String> set, @Nullable Predicate<String> valid) {
        EditBox box = new EditBox(font, x, y, fw, 18, Component.empty());
        box.setMaxLength(256);
        box.setValue(value == null ? "" : value);
        box.moveCursorToStart(false);
        if (valid != null) {
            box.setTextColor(valid.test(box.getValue()) ? TEXT : RED);
        }
        box.setResponder(s -> {
            set.accept(s);
            if (valid != null) {
                box.setTextColor(valid.test(s) ? TEXT : RED);
            }
        });
        addBox(box);
        return box;
    }

    private void number(int x, int y, int fw, Supplier<Double> get, DoubleConsumer set, double step, double min, double max, boolean integer,
                        @Nullable Component tip) {
        EditBox box = new EditBox(font, x, y, fw, 18, Component.empty());
        box.setMaxLength(12);
        box.setValue(integer ? String.valueOf(Math.round(get.get())) : num(get.get()));
        box.setFilter(s -> s.matches("-?[0-9]*[.,]?[0-9]*"));
        box.setResponder(s -> {
            try {
                double v = Double.parseDouble(s.replace(',', '.'));
                if (v >= min && v <= max) {
                    set.accept(integer ? Math.round(v) : v);
                    box.setTextColor(TEXT);
                    return;
                }
            } catch (NumberFormatException ignored) {
            }
            box.setTextColor(RED);
        });
        Component t = tip == null ? tr("number.tip") : tip.copy().append("\n").append(tr("number.tip"));
        box.setTooltip(Tooltip.create(t));
        numbers.put(box, new Num(set, get, step, min, max, integer));
        addBox(box);
    }

    private void label(int x, int y, Component text, int color) {
        (buildingPopup ? popupLabels : labels).add(new Label(x, y, text, color, 1));
    }

    private void toggle(int x, int y, int bw, Component text, Supplier<Boolean> get, Consumer<Boolean> set, @Nullable Component tip) {
        add(new Btn(x, y, bw, 18, () -> Component.literal(get.get() ? "☑  " : "☐  ").append(text), () -> change(() -> set.accept(!get.get())), tip,
                get.get() ? GREEN : DIM, get::get, null, 0));
    }

    // ---------------------------------------------------------------- header, list, the trade's own header

    private void buildHeader() {
        int y = top + 6, x = left + w - 10;
        btn(x - 60, y, 60, tr("close"), this::onClose, null, DIM);
        x -= 66;
        btn(x - 74, y, 74, tr("revert"), () -> change(() -> {
            config = MetierDefs.fromJson(savedJson);
            say(tr("reverted"), GOLD_LIGHT);
        }), tr("revert.tip"), DIM);
        x -= 80;
        add(new Btn(x - 90, y, 90, 18, () -> tr("save"), this::save, tr("save.tip"), GREEN, this::dirty, null, 0));
    }

    private boolean inList(int y) {
        return y >= top + 32 && y + 18 <= top + h - 8;
    }

    private void buildList() {
        int x = left + 8, y = top + 34 - listScroll;
        for (boolean stations : new boolean[] {false, true}) {
            int count = 0;
            for (Def d : config.metiers) {
                if (d.isStation() == stations) count++;
            }
            if (inList(y)) {
                labels.add(new Label(x + 2, y + 5, Component.translatable("metier.arcanes_oublies.editor.group." + (stations ? "stations" : "deposits"))
                        .append("  ").append(Component.literal(String.valueOf(count))), GOLD_LIGHT, 1));
                boolean st = stations;
                add(new Btn(x + SIDE - 20, y + 1, 18, 16, () -> Component.literal("+"), () -> change(() -> addDef(st)),
                        tr(stations ? "new_station.tip" : "new_deposit.tip"), GREEN, () -> false, null, 0));
            }
            y += 20;
            for (int i = 0; i < config.metiers.size(); i++) {
                Def d = config.metiers.get(i);
                if (d.isStation() != stations) {
                    continue;
                }
                int index = i;
                if (inList(y)) {
                    add(new Btn(x, y, SIDE, 19, () -> Component.literal(d.name), () -> {
                        selected = index;
                        recipe = 0;
                        scroll = 0;
                        rebuildWidgets();
                    }, null, d.enabled ? GAME_COLORS[d.game.ordinal()] : DIM, () -> selected == index,
                            () -> d.blocks.isEmpty() ? GAME_ICONS[d.game.ordinal()] : blockIcon(d.blocks.get(0)), 1));
                }
                y += 21;
            }
            y += 8;
        }
        listBottom = y + listScroll - (top + 34);
    }

    private void buildTrade() {
        Def d = def();
        if (d == null) {
            labels.add(new Label(mx0 + 10, top + 50, tr("none"), DIM, 1));
            return;
        }
        int y = top + 34;
        field(mx0 + 30, y + 2, Math.min(200, mx1 - mx0 - 150), d.name, s -> d.name = s, null).setTooltip(Tooltip.create(tr("name")));
        toggle(mx1 - 76, y + 2, 76, tr("enabled"), () -> d.enabled, v -> d.enabled = v, tr("enabled.tip"));
        int by = y + 26, bx = mx1;
        btn(bx - 70, by, 70, tr("delete"), () -> change(this::deleteDef), tr("delete.tip"), RED);
        bx -= 76;
        btn(bx - 74, by, 74, tr("duplicate"), () -> change(this::duplicateDef), null, DIM);
        bx -= 80;
        add(new Btn(bx - 74, by, 74, 18, () -> tr("try"), this::trial, tr("try.tip"), BLUE, () -> false, null, 0));
        // The tabs
        int tx = mx0, ty = top + 88;
        for (Tab t : Tab.values()) {
            if (t == Tab.RECIPES && !d.isStation() || t == Tab.REWARDS && d.isStation()) {
                continue;
            }
            Component label = tr("tab." + t.name().toLowerCase(Locale.ROOT));
            int tw = font.width(label) + 22;
            add(new Btn(tx, ty, tw, 20, () -> label, () -> {
                tab = t;
                scroll = 0;
                rebuildWidgets();
            }, null, GOLD, () -> tab == t, null, 3));
            tx += tw + 4;
        }
    }

    // ---------------------------------------------------------------- the content, in cards

    /** On screen if inside the content area (after the scroll). */
    private boolean visible(int y, int hgt) {
        return y >= cy0 && y + hgt <= cy1;
    }

    private int cardStart;
    private Component cardTitle = Component.empty();
    private int cardColor, cardX0, cardX1;

    private int card(int x0, int x1, int y, Component title, int color) {
        cardStart = y;
        cardTitle = title;
        cardColor = color;
        cardX0 = x0;
        cardX1 = x1;
        return y + 22;
    }

    private int endCard(int y) {
        cards.add(new Card(cardX0, cardStart, cardX1, y + 4, cardTitle, cardColor));
        return y + 12;
    }

    private int fieldX() {
        return cardX0 + Math.max(150, (cardX1 - cardX0) * 55 / 100);
    }

    /** Chips flowing across the current card, and an "add" chip at the end. */
    private int chips(int y, List<Object[]> chips, @Nullable Runnable add, @Nullable Component empty) {
        int x = cardX0 + 10, x1 = cardX1 - 10;
        if (chips.isEmpty() && empty != null && visible(y, 18)) {
            labels.add(new Label(x, y + 6, empty, DIM, 1));
            x += font.width(empty) + 10;
        }
        for (Object[] c : chips) {
            @SuppressWarnings("unchecked")
            Supplier<ItemStack> ic = (Supplier<ItemStack>) c[0];
            Component text = (Component) c[1];
            Runnable action = (Runnable) c[2];
            Component tip = (Component) c[3];
            int cw = 26 + font.width(text) + 8;
            if (x + cw > x1) {
                x = cardX0 + 10;
                y += CHIP_H + 4;
            }
            if (visible(y, CHIP_H)) {
                add(new Btn(x, y, cw, CHIP_H, () -> text, action, tip, (int) c[4], () -> false, ic, 2));
            }
            x += cw + 4;
        }
        if (add != null) {
            if (x + 24 > x1) {
                x = cardX0 + 10;
                y += CHIP_H + 4;
            }
            if (visible(y, CHIP_H)) {
                add(new Btn(x, y, 24, CHIP_H, () -> Component.literal("+"), add, tr("add"), GREEN, () -> false, null, 4));
            }
        }
        return y + CHIP_H + 6;
    }

    private static Object[] chip(Supplier<ItemStack> icon, Component text, Runnable action, @Nullable Component tip, int color) {
        return new Object[] {icon, text, action, tip, color};
    }

    private void buildContent() {
        Def d = def();
        contentBottom = 0;
        if (d == null) {
            return;
        }
        int y = cy0 + 6 - scroll;
        switch (tab) {
            case SETTINGS -> y = settings(d, y);
            case BLOCKS -> y = blocks(d, y);
            case REWARDS -> y = rewards(d, y);
            case RECIPES -> y = recipes(d, y);
        }
        contentBottom = y + scroll - cy0;
    }

    private int settings(Def d, int y) {
        int x0 = mx0, x1 = mx1;
        y = card(x0, x1, y, tr("card.minigame"), GAME_COLORS[d.game.ordinal()]);
        if (visible(y, 20)) {
            add(new Btn(cardX0 + 10, y, 190, 20, () -> Component.literal("◀  ").append(gameName(d.game)).append("  ▶"), () -> change(() ->
                    d.game = Game.values()[(d.game.ordinal() + 1) % Game.values().length]), tr("game.tip"), GAME_COLORS[d.game.ordinal()], () -> true,
                    () -> GAME_ICONS[d.game.ordinal()], 2));
        }
        y += 26;
        y = numberRow(y, tr("difficulty"), () -> d.difficulty, v -> d.difficulty = v, .05, .2, 5, false, tr("difficulty.tip"));
        y = endCard(y);
        y = card(x0, x1, y, tr("card.result"), GOLD);
        y = numberRow(y, tr("perfect_at"), () -> d.perfectAt * 100, v -> d.perfectAt = v / 100, 1, 5, 100, true, tr("perfect_at.tip"));
        y = numberRow(y, tr("critical_below"), () -> d.criticalBelow * 100, v -> d.criticalBelow = v / 100, 1, 0, 100, true, tr("critical_below.tip"));
        y = endCard(y);
        y = card(x0, x1, y, tr("card.costs"), 0xFFE08A60);
        y = numberRow(y, tr("durability"), () -> (double) d.durability, v -> d.durability = (int) v, 1, 0, 1000, true, tr("durability.tip"));
        y = numberRow(y, tr("durability_critical"), () -> (double) d.durabilityCritical, v -> d.durabilityCritical = (int) v, 1, 0, 1000, true,
                tr("durability_critical.tip"));
        y = numberRow(y, tr("player_cooldown"), () -> (double) d.playerCooldown, v -> d.playerCooldown = (int) v, 5, 0, 7 * 24 * 3600, true,
                tr("player_cooldown.tip"));
        y = endCard(y);
        y = card(x0, x1, y, tr("card.rules"), BLUE);
        if (visible(y, 18)) {
            toggle(cardX0 + 10, y, 230, tr("protect"), () -> d.protectBlocks, v -> d.protectBlocks = v, tr("protect.tip"));
        }
        y += ROW;
        if (d.isStation()) {
            if (visible(y, 18)) {
                toggle(cardX0 + 10, y, 260, tr("vanilla"), () -> d.vanillaButton, v -> d.vanillaButton = v, tr("vanilla.tip"));
            }
            y += ROW;
        }
        if (visible(y, 18)) {
            labels.add(new Label(cardX0 + 10, y + 5, tr("id"), TEXT, 1));
            field(fieldX(), y, Math.min(170, cardX1 - fieldX() - 10), d.id, s -> d.id = s, s -> s.matches("[a-z0-9_]+")).setTooltip(Tooltip.create(tr("id.tip")));
        }
        y += ROW;
        return endCard(y);
    }

    private int numberRow(int y, Component text, Supplier<Double> get, DoubleConsumer set, double step, double min, double max, boolean integer,
                          @Nullable Component tip) {
        if (visible(y, 18)) {
            labels.add(new Label(cardX0 + 10, y + 5, text, TEXT, 1));
            number(fieldX(), y, 64, get, set, step, min, max, integer, tip);
        }
        return y + ROW;
    }

    private int blocks(Def d, int y) {
        y = card(mx0, mx1, y, tr("card.blocks"), GOLD_LIGHT);
        List<Object[]> list = new ArrayList<>();
        for (int i = 0; i < d.blocks.size(); i++) {
            int index = i;
            String id = d.blocks.get(i);
            list.add(chip(() -> blockIcon(id), blockIcon(id).isEmpty() ? Component.literal(id) : blockIcon(id).getHoverName(),
                    () -> open(tr("popup.block"), p -> blockPopup(p, d.blocks, index)), Component.literal(id), GOLD_LIGHT));
        }
        y = chips(y, list, () -> {
            d.blocks.add("minecraft:stone");
            open(tr("popup.block"), p -> blockPopup(p, d.blocks, d.blocks.size() - 1));
        }, tr("no_blocks"));
        y = endCard(y);
        if (!d.isStation()) {
            y = card(mx0, mx1, y, tr("card.spent"), 0xFFB0A090);
            if (visible(y, CHIP_H)) {
                labels.add(new Label(cardX0 + 10, y + 6, tr("depleted"), TEXT, 1));
                ItemStack sp = blockIcon(d.depletedBlock);
                Component text = d.depletedBlock.isEmpty() || d.depletedBlock.equals("minecraft:air") ? tr("vanish") : sp.isEmpty()
                        ? Component.literal(d.depletedBlock) : sp.getHoverName();
                add(new Btn(fieldX(), y, 26 + font.width(text) + 8, CHIP_H, () -> text, () -> open(tr("popup.spent"), p -> spentPopup(p, d)), tr("depleted.tip"),
                        0xFFB0A090, () -> false, () -> blockIcon(d.depletedBlock), 2));
            }
            y += ROW + 2;
            if (visible(y, 18)) {
                labels.add(new Label(cardX0 + 10, y + 5, tr("respawn"), TEXT, 1));
                number(fieldX(), y, 64, () -> (double) d.respawnSeconds, v -> d.respawnSeconds = (int) v, 30, 0, 7 * 24 * 3600, true, tr("respawn.tip"));
                labels.add(new Label(fieldX() + 72, y + 5, Component.literal("= ").append(time(d.respawnSeconds)), DIM, 1));
            }
            y += ROW;
            y = numberRow(y, tr("uses"), () -> (double) d.uses, v -> d.uses = (int) v, 1, 1, 10000, true, tr("uses.tip"));
            if (visible(y, 18)) {
                toggle(cardX0 + 10, y, 280, tr("deplete_critical"), () -> d.depleteOnCritical, v -> d.depleteOnCritical = v, tr("deplete_critical.tip"));
            }
            y += ROW;
            y = endCard(y);
        }
        y = card(mx0, mx1, y, tr("card.tools"), 0xFF9AC0E0);
        List<Object[]> tools = new ArrayList<>();
        for (Tool t : d.tools) {
            tools.add(chip(() -> icon(t.item), Component.literal("×" + num(t.difficulty)), () -> open(tr("popup.tool"), p -> toolPopup(p, d, t)),
                    icon(t.item).getHoverName().copy().append(" — ").append(tr("tool_difficulty.tip")), 0xFF9AC0E0));
        }
        y = chips(y, tools, () -> {
            ItemStack held = held();
            Tool t = new Tool(held.isEmpty() ? "minecraft:iron_pickaxe" : BuiltInRegistries.ITEM.getKey(held.getItem()).toString(), 1);
            d.tools.add(t);
            open(tr("popup.tool"), p -> toolPopup(p, d, t));
        }, tr("no_tools"));
        return endCard(y);
    }

    private List<Object[]> rewardChips(List<Reward> list, int color) {
        List<Object[]> out = new ArrayList<>();
        for (Reward r : list) {
            String n = r.min == r.max ? "×" + r.min : r.min + "–" + r.max;
            if (r.chance < 1) {
                n += "  " + Math.round(r.chance * 100) + "%";
            }
            if (r.components != null) {
                n += " ✦";
            }
            Component text = Component.literal(n);
            out.add(chip(() -> r.components != null ? MetierDefs.stack(r, 1, registries()) : icon(r.item), text,
                    () -> open(tr("popup.reward"), p -> rewardPopup(p, list, r)), icon(r.item).getHoverName(), color));
        }
        return out;
    }

    private int outcomeCards(int x0, int x1, int y, List<Reward> critical, List<Reward> partial, List<Reward> perfect) {
        Object[][] rows = {{critical, "critical", RED}, {partial, "partial", BLUE}, {perfect, "perfect", GOLD}};
        for (Object[] o : rows) {
            @SuppressWarnings("unchecked")
            List<Reward> list = (List<Reward>) o[0];
            int color = (int) o[2];
            y = card(x0, x1, y, Component.translatable("metier.arcanes_oublies.outcome." + o[1]), color);
            y = chips(y, rewardChips(list, color), () -> {
                ItemStack held = held();
                Reward r = new Reward(held.isEmpty() ? "minecraft:raw_copper" : BuiltInRegistries.ITEM.getKey(held.getItem()).toString(), 1, 1);
                list.add(r);
                open(tr("popup.reward"), p -> rewardPopup(p, list, r));
            }, tr("nothing"));
            y = endCard(y);
        }
        return y;
    }

    private int rewards(Def d, int y) {
        return outcomeCards(mx0, mx1, y, d.critical, d.partial, d.perfect);
    }

    private int recipes(Def d, int y) {
        int split = mx0 + 168;
        // The list, on the left
        int ly = card(mx0, split - 6, y, tr("card.recipes"), GOLD_LIGHT);
        if (visible(ly, 18)) {
            btn(cardX0 + 8, ly, 72, tr("recipe_new"), () -> change(() -> {
                Recipe r = new Recipe();
                r.name = tr("recipe_default").getString();
                r.partial.add(new Reward("minecraft:stick", 1, 1));
                d.recipes.add(r);
                recipe = d.recipes.size() - 1;
            }), null, GREEN);
            btn(cardX0 + 84, ly, cardX1 - cardX0 - 92, tr("sort"), () -> change(() -> {
                Recipe cur = currentRecipe();
                d.recipes.sort(Comparator.comparing(r -> r.name.toLowerCase(Locale.ROOT)));
                recipe = Math.max(0, d.recipes.indexOf(cur));
            }), tr("sort.tip"), DIM);
        }
        ly += 24;
        for (int i = 0; i < d.recipes.size(); i++) {
            int index = i;
            Recipe r = d.recipes.get(i);
            if (visible(ly, 19)) {
                add(new Btn(cardX0 + 6, ly, cardX1 - cardX0 - 12, 19, () -> Component.literal(r.name), () -> {
                    recipe = index;
                    rebuildWidgets();
                }, null, GOLD, () -> recipe == index, () -> resultIcon(r), 1));
            }
            ly += 21;
        }
        ly = endCard(ly);
        // The recipe chosen, on the right
        Recipe r = currentRecipe();
        int ry = y;
        if (r != null) {
            ry = card(split, mx1, ry, tr("card.recipe"), GOLD);
            if (visible(ry, 18)) {
                field(cardX0 + 10, ry, Math.min(180, cardX1 - cardX0 - 20), r.name, s -> r.name = s, null).setTooltip(Tooltip.create(tr("recipe_name")));
            }
            ry += ROW + 2;
            ry = numberRow(ry, tr("recipe_difficulty"), () -> r.difficulty, v -> r.difficulty = v, .05, .2, 5, false, tr("recipe_difficulty.tip"));
            ry = numberRow(ry, tr("recipe_cooldown"), () -> (double) r.cooldown, v -> r.cooldown = (int) v, 5, 0, 7 * 24 * 3600, true,
                    tr("recipe_cooldown.tip"));
            if (visible(ry, 18)) {
                toggle(cardX0 + 10, ry, Math.min(280, cardX1 - cardX0 - 20), tr("lose_inputs"), () -> r.loseInputsOnFail, v -> r.loseInputsOnFail = v,
                        tr("lose_inputs.tip"));
            }
            ry += ROW;
            if (visible(ry, 18)) {
                btn(cardX0 + 10, ry, 80, tr("duplicate"), () -> change(() -> {
                    MetierDefs.Config tmp = new MetierDefs.Config();
                    Def holder = new Def();
                    holder.recipes.add(r);
                    tmp.metiers.add(holder);
                    Recipe copy = MetierDefs.copy(tmp).metiers.get(0).recipes.get(0);
                    copy.name = copy.name + " 2";
                    d.recipes.add(recipe + 1, copy);
                    recipe++;
                }), null, DIM);
                btn(cardX0 + 96, ry, 80, tr("delete"), () -> change(() -> {
                    if (d.recipes.size() <= 1) {
                        say(tr("last_recipe"), RED);
                        return;
                    }
                    d.recipes.remove(r);
                    recipe = Math.max(0, recipe - 1);
                }), tr("recipe_delete.tip"), RED);
            }
            ry += ROW;
            ry = endCard(ry);
            ry = card(split, mx1, ry, tr("card.ingredients"), 0xFFC0A070);
            List<Object[]> ins = new ArrayList<>();
            for (Ingredient in : r.inputs) {
                ins.add(chip(() -> icon(in.item), Component.literal("×" + in.count), () -> open(tr("popup.ingredient"), p -> ingredientPopup(p, r, in)),
                        icon(in.item).getHoverName(), 0xFFC0A070));
            }
            ry = chips(ry, ins, () -> {
                ItemStack held = held();
                Ingredient in = new Ingredient(held.isEmpty() ? "minecraft:iron_ingot" : BuiltInRegistries.ITEM.getKey(held.getItem()).toString(),
                        held.isEmpty() ? 1 : held.getCount());
                r.inputs.add(in);
                open(tr("popup.ingredient"), p -> ingredientPopup(p, r, in));
            }, tr("nothing"));
            ry = endCard(ry);
            ry = outcomeCards(split, mx1, ry, r.critical, r.partial, r.perfect);
        }
        return Math.max(ly, ry);
    }

    private static ItemStack resultIcon(Recipe r) {
        List<Reward> shown = !r.perfect.isEmpty() ? r.perfect : r.partial;
        return shown.isEmpty() ? new ItemStack(Items.PAPER) : icon(shown.get(0).item);
    }

    // ---------------------------------------------------------------- the small windows

    private void popupActions(int x, int y, @Nullable Runnable remove) {
        btn(px1 - 74, py1 - 26, 62, tr("popup.ok"), this::closePopup, null, GREEN);
        if (remove != null) {
            btn(px0 + 12, py1 - 26, 70, tr("popup.remove"), () -> {
                remove.run();
                closePopup();
            }, null, RED);
        }
    }

    private void blockPopup(int[] p, List<String> list, int index) {
        int x = p[0], y = p[1];
        if (index < 0 || index >= list.size()) {
            return;
        }
        label(x, y + 5, tr("popup.block_id"), TEXT);
        field(x + 70, y, 180, list.get(index), s -> list.set(index, s), MetierEditorScreen::blockExists);
        y += 26;
        btn(x + 70, y, 110, tr("looked"), () -> {
            String id = lookedBlock();
            if (id != null) {
                list.set(index, id);
                rebuildWidgets();
            }
        }, tr("looked.tip"), GOLD);
        popupActions(x, y, () -> list.remove(index));
    }

    private void spentPopup(int[] p, Def d) {
        int x = p[0], y = p[1];
        label(x, y + 5, tr("popup.block_id"), TEXT);
        field(x + 70, y, 180, d.depletedBlock, s -> d.depletedBlock = s, s -> s.isEmpty() || blockExists(s));
        y += 26;
        btn(x + 70, y, 100, tr("looked"), () -> {
            String id = lookedBlock();
            if (id != null) {
                d.depletedBlock = id;
                rebuildWidgets();
            }
        }, tr("looked.tip"), GOLD);
        btn(x + 176, y, 74, tr("vanish"), () -> {
            d.depletedBlock = "minecraft:air";
            rebuildWidgets();
        }, tr("vanish.tip"), GOLD);
        popupActions(x, y, null);
    }

    private void toolPopup(int[] p, Def d, Tool t) {
        int x = p[0], y = p[1];
        label(x, y + 5, tr("popup.item"), TEXT);
        field(x + 70, y, 180, t.item, s -> t.item = s, MetierEditorScreen::itemExists);
        y += 26;
        label(x, y + 5, tr("popup.difficulty"), TEXT);
        number(x + 70, y, 60, () -> t.difficulty, v -> t.difficulty = v, .05, .2, 5, false, tr("tool_difficulty.tip"));
        y += 26;
        btn(x + 70, y, 110, tr("held"), () -> {
            ItemStack held = held();
            if (!held.isEmpty()) {
                t.item = BuiltInRegistries.ITEM.getKey(held.getItem()).toString();
                rebuildWidgets();
            }
        }, tr("held.tip"), GOLD);
        popupActions(x, y, () -> d.tools.remove(t));
    }

    private void ingredientPopup(int[] p, Recipe r, Ingredient in) {
        int x = p[0], y = p[1];
        label(x, y + 5, tr("popup.item"), TEXT);
        field(x + 70, y, 180, in.item, s -> in.item = s, MetierEditorScreen::itemExists);
        y += 26;
        label(x, y + 5, tr("popup.count"), TEXT);
        number(x + 70, y, 60, () -> (double) in.count, v -> in.count = (int) v, 1, 1, 64 * 36, true, null);
        y += 26;
        btn(x + 70, y, 110, tr("held"), () -> {
            ItemStack held = held();
            if (!held.isEmpty()) {
                in.item = BuiltInRegistries.ITEM.getKey(held.getItem()).toString();
                in.count = held.getCount();
                rebuildWidgets();
            }
        }, tr("held.tip"), GOLD);
        popupActions(x, y, () -> r.inputs.remove(in));
    }

    private void rewardPopup(int[] p, List<Reward> list, Reward r) {
        int x = p[0], y = p[1];
        label(x, y + 5, tr("popup.item"), TEXT);
        field(x + 70, y, 180, r.item, s -> r.item = s, MetierEditorScreen::itemExists);
        y += 24;
        label(x, y + 5, tr("popup.amount"), TEXT);
        number(x + 70, y, 40, () -> (double) r.min, v -> r.min = (int) v, 1, 0, 64 * 36, true, tr("min.tip"));
        label(x + 116, y + 5, Component.literal("→"), DIM);
        number(x + 130, y, 40, () -> (double) r.max, v -> r.max = (int) v, 1, 0, 64 * 36, true, tr("max.tip"));
        y += 24;
        label(x, y + 5, tr("popup.chance"), TEXT);
        number(x + 70, y, 40, () -> r.chance * 100, v -> r.chance = v / 100, 5, 0, 100, true, tr("chance.tip"));
        label(x + 116, y + 5, Component.literal("%"), DIM);
        y += 24;
        btn(x + 70, y, 110, tr("held"), () -> {
            ItemStack held = held();
            if (!held.isEmpty()) {
                r.item = BuiltInRegistries.ITEM.getKey(held.getItem()).toString();
                r.min = r.max = held.getCount();
                r.components = MetierDefs.components(held, registries());
                rebuildWidgets();
            }
        }, tr("held_data.tip"), GOLD);
        if (r.components != null) {
            btn(x + 186, y, 74, tr("popup.clear_data"), () -> {
                r.components = null;
                rebuildWidgets();
            }, Component.literal(r.components.toString()), 0xFFD8A0FF);
        }
        popupActions(x, y, () -> list.remove(r));
    }

    // ---------------------------------------------------------------- actions

    private static ItemStack held() {
        Minecraft mc = Minecraft.getInstance();
        return mc.player == null ? ItemStack.EMPTY : mc.player.getMainHandItem();
    }

    @Nullable
    private String lookedBlock() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null && mc.hitResult instanceof BlockHitResult hit && hit.getType() == HitResult.Type.BLOCK) {
            BlockState state = mc.level.getBlockState(hit.getBlockPos());
            return BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();
        }
        say(tr("no_look"), RED);
        return null;
    }

    private void addDef(boolean station) {
        Def d = new Def();
        d.id = station ? "station_" + (config.metiers.size() + 1) : "gisement_" + (config.metiers.size() + 1);
        d.name = (station ? tr("new_station_name") : tr("new_deposit_name")).getString();
        if (station) {
            d.game = Game.ASSEMBLY;
            d.blocks.add("minecraft:crafting_table");
            d.durability = 0;
            d.durabilityCritical = 0;
            Recipe r = new Recipe();
            r.name = tr("recipe_default").getString();
            r.inputs.add(new Ingredient("minecraft:oak_planks", 2));
            r.partial.add(new Reward("minecraft:stick", 4, 4));
            r.perfect.add(new Reward("minecraft:stick", 6, 8));
            d.recipes.add(r);
        } else {
            d.blocks.add("minecraft:iron_ore");
            d.tools.add(new Tool("minecraft:stone_pickaxe", 1.5));
            d.tools.add(new Tool("minecraft:iron_pickaxe", 1));
            d.partial.add(new Reward("minecraft:raw_iron", 1, 1));
            d.perfect.add(new Reward("minecraft:raw_iron", 2, 3));
        }
        config.metiers.add(d);
        selected = config.metiers.size() - 1;
        recipe = 0;
        tab = Tab.SETTINGS;
        scroll = 0;
    }

    private void duplicateDef() {
        Def d = def();
        if (d == null) {
            return;
        }
        MetierDefs.Config tmp = new MetierDefs.Config();
        tmp.metiers.add(d);
        Def copy = MetierDefs.copy(tmp).metiers.get(0);
        copy.id = d.id + "_2";
        copy.name = d.name + " 2";
        config.metiers.add(selected + 1, copy);
        selected++;
    }

    private void deleteDef() {
        if (def() == null) {
            return;
        }
        long now = Util.getMillis();
        if (now - confirmAt > 3000) {
            confirmAt = now;
            say(tr("delete.confirm"), RED);
            return;
        }
        confirmAt = -10000;
        config.metiers.remove(selected);
        selected = Math.max(0, selected - 1);
        scroll = 0;
    }

    private void say(Component text, int color) {
        status = text;
        statusColor = color;
        statusAt = Util.getMillis();
    }

    private void save() {
        String json = MetierDefs.toJson(config);
        for (MetierNet.Save part : MetierNet.Save.split(json)) {
            PacketDistributor.sendToServer(part);
        }
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK.value(), 1.2f, .6f));
        say(tr("sent"), GREEN);
    }

    /** Plays the selected trade's minigame as it is set, as a trial: nothing is sent, nothing given. */
    private void trial() {
        Def d = def();
        if (d == null) {
            return;
        }
        Recipe r = d.isStation() ? currentRecipe() : null;
        ItemStack heldStack = held();
        Tool tool = d.tool(heldStack);
        double difficulty = d.difficulty * (r == null ? 1 : r.difficulty) * (tool == null ? (d.tools.isEmpty() ? 1 : d.tools.get(0).difficulty) : tool.difficulty);
        ItemStack toolStack = tool != null ? heldStack.copyWithCount(1) : d.tools.isEmpty() ? ItemStack.EMPTY : icon(d.tools.get(0).item);
        List<ItemStack> inputs = new ArrayList<>();
        ItemStack subject;
        if (r != null) {
            for (Ingredient in : r.inputs) {
                inputs.add(new ItemStack(MetierDefs.item(in.item), in.count));
            }
            List<Reward> shown = !r.perfect.isEmpty() ? r.perfect : r.partial;
            subject = shown.isEmpty() ? ItemStack.EMPTY : MetierDefs.stack(shown.get(0), 1, registries());
        } else {
            subject = d.blocks.isEmpty() ? GAME_ICONS[d.game.ordinal()] : blockIcon(d.blocks.get(0));
        }
        if (subject.isEmpty()) {
            subject = GAME_ICONS[d.game.ordinal()];
        }
        Minecraft.getInstance().setScreen(MetierClient.create(d.game, null, difficulty, new java.util.Random().nextLong(), subject, toolStack, inputs,
                Component.literal(r != null ? r.name : d.name), tr("trial_sub"), (float) d.perfectAt, (float) d.criticalBelow).returnTo(this));
    }

    // ---------------------------------------------------------------- drawing

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
    }

    private void drawButton(GuiGraphics g, Btn b, int mouseX, int mouseY, boolean live) {
        boolean over = live && mouseX >= b.x() && mouseX < b.x() + b.w() && mouseY >= b.y() && mouseY < b.y() + b.h();
        boolean lit = b.lit().get();
        int c = b.color();
        switch (b.style()) {
            case 1 -> {
                if (lit || over) {
                    Draw.rect(g, b.x(), b.y(), b.x() + b.w(), b.y() + b.h(), lit ? Draw.argb(150, Draw.lerp(c, 0xFF000000, .55f)) : 0x40FFFFFF);
                }
                if (lit) {
                    Draw.rect(g, b.x(), b.y(), b.x() + 2, b.y() + b.h(), c);
                }
            }
            case 2, 4 -> {
                Draw.pill(g, b.x(), b.y(), b.x() + b.w(), b.y() + b.h(), over ? Draw.argb(220, Draw.lerp(c, 0xFF000000, .5f)) : Draw.argb(200,
                        Draw.lerp(c, 0xFF000000, .78f)));
                if (over) {
                    Draw.pill(g, b.x(), b.y(), b.x() + b.w(), b.y() + 1.5f, Draw.argb(160, c));
                }
            }
            case 3 -> {
                Draw.rect(g, b.x(), b.y(), b.x() + b.w(), b.y() + b.h(), lit ? Draw.argb(200, Draw.lerp(c, 0xFF000000, .6f)) : over ? 0x50FFFFFF : 0x30000000);
                if (lit) {
                    Draw.rect(g, b.x(), b.y() + b.h() - 2, b.x() + b.w(), b.y() + b.h(), c);
                }
            }
            default -> {
                Draw.rect(g, b.x(), b.y(), b.x() + b.w(), b.y() + b.h(), lit ? Draw.argb(over ? 230 : 190, Draw.lerp(c, 0xFF000000, .5f))
                        : over ? 0xC0302820 : 0xA0141010);
                Draw.rect(g, b.x(), b.y() + b.h() - 1, b.x() + b.w(), b.y() + b.h(), Draw.argb(over || lit ? 255 : 120, c));
            }
        }
        int tx = b.x() + 6;
        if (b.icon() != null) {
            ItemStack s = b.icon().get();
            if (!s.isEmpty()) {
                g.renderItem(s, b.x() + (b.style() == 1 ? 3 : 4), b.y() + (b.h() - 16) / 2);
            }
            tx = b.x() + (b.style() == 1 ? 24 : 24);
        }
        Component label = b.label().get();
        int maxW = b.w() - (tx - b.x()) - 4;
        String text = label.getString();
        if (font.width(text) > maxW) {
            text = font.plainSubstrByWidth(text, Math.max(0, maxW - font.width("…"))) + "…";
        }
        Component shown = Component.literal(text).withStyle(label.getStyle());
        int color = b.style() == 1 && !lit ? (b.color() == DIM ? DIM : TEXT) : over || lit ? 0xFFFFFFFF : TEXT;
        int ty = b.y() + (b.h() - 8) / 2;
        if (b.style() == 0 || b.style() == 3 || b.style() == 4) {
            g.drawString(font, shown, b.x() + (b.w() - font.width(shown)) / 2, ty, color, true);
        } else {
            g.drawString(font, shown, tx, ty, color, true);
        }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        boolean popup = popupBuild != null;
        g.fillGradient(0, 0, width, height, 0xA0000000, 0xC0000000);
        Draw.panel(g, left, top, left + w, top + h, 0xFF8A6438);
        g.fillGradient(left + 1, top + 1, left + w - 1, top + 30, 0xA05A4020, 0x005A4020);
        Draw.item(g, new ItemStack(Items.SMITHING_TABLE), left + 8, top + 7, 1);
        Draw.text(g, title, left + 30, top + 8, GOLD_LIGHT, 1.2f);
        if (dirty()) {
            Draw.text(g, tr("unsaved"), left + 34 + font.width(title) * 1.2f, top + 10, 0xFFFFB060, 1);
        }
        // Sidebar and main area
        Draw.rect(g, left + 4, top + 30, left + SIDE + 12, top + h - 4, 0x70000000);
        Draw.rect(g, left + SIDE + 12, top + 30, left + SIDE + 13, top + h - 4, CARD_EDGE);
        Def d = def();
        if (d != null) {
            Draw.itemAt(g, d.blocks.isEmpty() ? GAME_ICONS[d.game.ordinal()] : blockIcon(d.blocks.get(0)), mx0 + 12, top + 45, 22);
            Component kind = (d.isStation() ? tr("kind_short.station") : tr("kind_short.deposit")).copy().append("  ·  ").append(gameName(d.game));
            Draw.text(g, kind, mx0 + 4, top + 66, GAME_COLORS[d.game.ordinal()], 1);
            Draw.rect(g, mx0, top + 108, mx1, top + 109, CARD_EDGE);
        }
        // Content: cards, then what is on them
        g.enableScissor(mx0 - 2, cy0, mx1 + 2, cy1);
        for (Card c : cards) {
            Draw.rect(g, c.x0(), c.y0(), c.x1(), c.y1(), CARD);
            Draw.rect(g, c.x0(), c.y0(), c.x1(), c.y0() + 1, Draw.argb(170, c.color()));
            Draw.rect(g, c.x0(), c.y0(), c.x0() + 1, c.y1(), CARD_EDGE);
            Draw.rect(g, c.x1() - 1, c.y0(), c.x1(), c.y1(), CARD_EDGE);
            Draw.rect(g, c.x0(), c.y1() - 1, c.x1(), c.y1(), CARD_EDGE);
            g.drawString(font, c.title(), c.x0() + 10, c.y0() + 7, c.color(), true);
        }
        g.disableScissor();
        Component tip = null;
        for (Btn b : buttons) {
            drawButton(g, b, mouseX, mouseY, !popup);
            if (!popup && b.tip() != null && mouseX >= b.x() && mouseX < b.x() + b.w() && mouseY >= b.y() && mouseY < b.y() + b.h()) {
                tip = b.tip();
            }
        }
        for (Label l : labels) {
            Draw.text(g, l.text(), l.x(), l.y(), l.color(), l.scale());
        }
        for (AbstractWidget wdg : background) {
            wdg.render(g, mouseX, mouseY, partialTick);
        }
        if (contentBottom > cy1 - cy0 + scroll) {
            Draw.centered(g, Component.literal("▼"), (mx0 + mx1) / 2f, cy1 - 9, 0xC0FFFFFF, 1);
        }
        if (status != null && Util.getMillis() - statusAt < 4000) {
            Draw.text(g, status, mx0, top + h - 16, statusColor, 1);
        } else {
            Draw.text(g, tr("hint"), mx0, top + h - 16, 0xFF6A6050, 1);
        }
        if (popup) {
            g.pose().pushPose();
            g.pose().translate(0, 0, 300);
            g.fill(0, 0, width, height, 0x90000000);
            Draw.panel(g, px0, py0, px1, py1, 0xFFB08A50);
            if (popupTitle != null) {
                g.drawString(font, popupTitle, px0 + 14, py0 + 10, GOLD_LIGHT, true);
            }
            for (Btn b : popupButtons) {
                drawButton(g, b, mouseX, mouseY, true);
                if (b.tip() != null && mouseX >= b.x() && mouseX < b.x() + b.w() && mouseY >= b.y() && mouseY < b.y() + b.h()) {
                    tip = b.tip();
                }
            }
            for (Label l : popupLabels) {
                Draw.text(g, l.text(), l.x(), l.y(), l.color(), l.scale());
            }
            g.pose().popPose();
        }
        g.pose().pushPose();
        if (popup) {
            g.pose().translate(0, 0, 400);
        }
        super.render(g, mouseX, mouseY, partialTick);
        g.pose().popPose();
        if (tip != null) {
            g.renderTooltip(font, font.split(tip, 240), mouseX, mouseY);
        }
    }

    // ---------------------------------------------------------------- input

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (popupBuild != null) {
            if (button == 0) {
                for (Btn b : new ArrayList<>(popupButtons)) {
                    if (mouseX >= b.x() && mouseX < b.x() + b.w() && mouseY >= b.y() && mouseY < b.y() + b.h()) {
                        click();
                        b.action().run();
                        return true;
                    }
                }
            }
            if (mouseX < px0 || mouseX >= px1 || mouseY < py0 || mouseY >= py1) {
                closePopup();
                return true;
            }
            return super.mouseClicked(mouseX, mouseY, button);
        }
        if (button == 0) {
            for (Btn b : new ArrayList<>(buttons)) {
                if (mouseX >= b.x() && mouseX < b.x() + b.w() && mouseY >= b.y() && mouseY < b.y() + b.h()) {
                    click();
                    setFocused(null);
                    b.action().run();
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private static void click() {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK.value(), 1, .45f));
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        for (var entry : numbers.entrySet()) {
            EditBox box = entry.getKey();
            if (box.isMouseOver(mouseX, mouseY) && (popupBuild == null || !background.contains(box))) {
                Num n = entry.getValue();
                double step = n.step() * (hasShiftDown() ? 10 : 1);
                double v = Mth.clamp(n.get().get() + Math.signum(scrollY) * step, n.min(), n.max());
                n.set().accept(n.integer() ? Math.round(v) : Math.round(v * 1000) / 1000.0);
                box.setValue(n.integer() ? String.valueOf(Math.round(n.get().get())) : num(n.get().get()));
                return true;
            }
        }
        if (popupBuild != null) {
            return true;
        }
        if (mouseX < left + SIDE + 12) {
            int max = Math.max(0, listBottom - (h - 44));
            listScroll = Mth.clamp(listScroll - (int) Math.signum(scrollY) * 21, 0, max);
            rebuildWidgets();
            return true;
        }
        int max = Math.max(0, contentBottom - (cy1 - cy0) + 8);
        int next = Mth.clamp(scroll - (int) Math.signum(scrollY) * ROW, 0, max);
        if (next != scroll) {
            scroll = next;
            rebuildWidgets();
        }
        return true;
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (key == GLFW.GLFW_KEY_ESCAPE) {
            if (getFocused() instanceof EditBox) {
                setFocused(null);
                return true;
            }
            if (popupBuild != null) {
                closePopup();
                return true;
            }
            if (dirty() && Util.getMillis() - confirmAt > 3000) {
                confirmAt = Util.getMillis();
                say(tr("unsaved.confirm"), 0xFFFFB060);
                return true;
            }
            onClose();
            return true;
        }
        if (key == GLFW.GLFW_KEY_S && hasControlDown()) {
            save();
            return true;
        }
        return super.keyPressed(key, scan, mods);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    // ---------------------------------------------------------------- for the smoke test

    public int trades() {
        return config.metiers.size();
    }

    public void select(int index, String tabName) {
        selected = Mth.clamp(index, 0, Math.max(0, config.metiers.size() - 1));
        String t = tabName.toUpperCase(Locale.ROOT);
        tab = t.equals("WORLD") ? Tab.BLOCKS : t.equals("GENERAL") ? Tab.SETTINGS : Tab.valueOf(t);
        scroll = 0;
        popupBuild = null;
        rebuildWidgets();
    }

    public void startTrial() {
        trial();
    }
}
