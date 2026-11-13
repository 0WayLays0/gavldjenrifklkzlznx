package com.waylays.arcanes.client.metier;

import com.google.gson.JsonObject;
import com.waylays.arcanes.metier.MetierDefs;
import com.waylays.arcanes.metier.MetierDefs.Def;
import com.waylays.arcanes.metier.MetierDefs.Game;
import com.waylays.arcanes.metier.MetierDefs.Ingredient;
import com.waylays.arcanes.metier.MetierDefs.Recipe;
import com.waylays.arcanes.metier.MetierDefs.Reward;
import com.waylays.arcanes.metier.MetierDefs.Tool;
import com.waylays.arcanes.metier.MetierNet;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
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
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.Supplier;

/**
 * The trades' editor ({@code /arcanes metiers}): every trade, its minigame, blocks, tools and how hard each makes it,
 * what each outcome gives (with the items' data: enchantments, names…), what a worked-out deposit becomes and for how
 * long, the stations' recipes, the cooldowns; a trial of the minigame as it is set; saved to the server, which writes
 * {@code config/arcanes_oublies/metiers.json}. Items and blocks can be typed, or taken from the hand and the block
 * looked at.
 */
public class MetierEditorScreen extends Screen {
    private static final int GOLD = 0xFFDEB25C, GOLD_LIGHT = 0xFFFFE6A0, TEXT = 0xFFF2ECE0, DIM = 0xFFA89E8C, RED = 0xFFFF6A5A, GREEN = 0xFF8AE07A,
            BLUE = 0xFF8FD4FF, PANEL = 0xF0161210, PANEL2 = 0xC0221C18;
    private static final int LIST_W = 160, ROW = 24;
    private static final int[] GAME_COLORS = {0xFF9A8A78, 0xFF6EAA5A, 0xFF5A8EC8, 0xFFD06438, 0xFF9A6AD0};
    private static final ItemStack[] GAME_ICONS = {new ItemStack(Items.IRON_PICKAXE), new ItemStack(Items.TORCHFLOWER),
            new ItemStack(Items.CRAFTING_TABLE), new ItemStack(Items.SMITHING_TABLE), new ItemStack(Items.BREWING_STAND)};

    private enum Tab {GENERAL, WORLD, REWARDS, RECIPES}

    /** A clickable area drawn by the screen. */
    private record Btn(int x, int y, int w, int h, Supplier<Component> label, Runnable action, @Nullable Component tip, int color,
                       Supplier<Boolean> lit) {}

    private record Label(int x, int y, Component text, int color, float scale) {}

    private record Icon(int x, int y, Supplier<ItemStack> stack, @Nullable Supplier<Component> tip) {}

    /** A number field: what it sets, its step for the wheel, its bounds. */
    private record Num(DoubleConsumer set, Supplier<Double> get, double step, double min, double max, boolean integer) {}

    private MetierDefs.Config config;
    private String savedJson;
    private int selected, recipe;
    private Tab tab = Tab.GENERAL;
    private int scroll, listScroll, contentBottom;
    private int left, top, w, h;
    private int cx0, cy0, cx1, cy1;
    private final List<Btn> buttons = new ArrayList<>();
    private final List<Label> labels = new ArrayList<>();
    private final List<Icon> icons = new ArrayList<>();
    private final List<int[]> sections = new ArrayList<>();
    private final Map<EditBox, Num> numbers = new HashMap<>();
    @Nullable
    private Component status;
    private int statusColor;
    private long statusAt, escapeAt = -10000;

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

    // ---------------------------------------------------------------- layout

    @Override
    protected void init() {
        w = Math.min(width - 12, 600);
        h = Math.min(height - 12, 400);
        left = (width - w) / 2;
        top = (height - h) / 2;
        cx0 = left + LIST_W + 12;
        cy0 = top + 50;
        cx1 = left + w - 8;
        cy1 = top + h - 30;
        buttons.clear();
        labels.clear();
        icons.clear();
        sections.clear();
        numbers.clear();
        buildList();
        buildTabs();
        buildBottom();
        buildContent();
    }

    private void btn(int x, int y, int bw, Supplier<Component> label, Runnable action, @Nullable Component tip, int color, Supplier<Boolean> lit) {
        buttons.add(new Btn(x, y, bw, 16, label, action, tip, color, lit));
    }

    private void btn(int x, int y, int bw, Component label, Runnable action, @Nullable Component tip) {
        btn(x, y, bw, () -> label, action, tip, GOLD, () -> false);
    }

    private void change(Runnable r) {
        r.run();
        rebuildWidgets();
    }

    private void buildList() {
        int x = left + 6, y = top + 30;
        int rows = (h - 30 - 64) / 21;
        listScroll = Mth.clamp(listScroll, 0, Math.max(0, config.metiers.size() - rows));
        for (int i = 0; i < rows && i + listScroll < config.metiers.size(); i++) {
            int index = i + listScroll;
            Def d = config.metiers.get(index);
            int color = GAME_COLORS[d.game.ordinal()];
            buttons.add(new Btn(x, y + i * 21, LIST_W, 19, () -> Component.literal(d.name), () -> {
                selected = index;
                recipe = 0;
                scroll = 0;
                if (tab == Tab.RECIPES && !d.isStation()) {
                    tab = Tab.GENERAL;
                }
                rebuildWidgets();
            }, Component.literal(d.id).withStyle(ChatFormatting.GRAY).append("\n").append(gameName(d.game)), color, () -> selected == index));
            icons.add(new Icon(x + 2, y + i * 21 + 1, () -> d.blocks.isEmpty() ? GAME_ICONS[d.game.ordinal()] : blockIcon(d.blocks.get(0)), null));
        }
        int by = top + h - 56;
        btn(x, by, 78, tr("new_deposit"), () -> change(() -> addDef(false)), tr("new_deposit.tip"));
        btn(x + 82, by, 78, tr("new_station"), () -> change(() -> addDef(true)), tr("new_station.tip"));
        btn(x, by + 18, 78, tr("duplicate"), () -> change(this::duplicateDef), null);
        btn(x + 82, by + 18, 78, () -> tr("delete"), () -> change(this::deleteDef), tr("delete.tip"), RED, () -> false);
        btn(x, by + 36, 38, Component.literal("▲"), () -> change(() -> moveDef(-1)), tr("move.tip"));
        btn(x + 40, by + 36, 38, Component.literal("▼"), () -> change(() -> moveDef(1)), tr("move.tip"));
        btn(x + 82, by + 36, 78, tr("defaults"), () -> change(() -> {
            config = MetierDefs.defaults();
            selected = 0;
            say(tr("defaults.done"), GOLD_LIGHT);
        }), tr("defaults.tip"));
    }

    private void buildTabs() {
        Def d = def();
        int x = cx0, y = top + 30;
        for (Tab t : Tab.values()) {
            if (t == Tab.RECIPES && (d == null || !d.isStation())) {
                continue;
            }
            if (t == Tab.REWARDS && d != null && d.isStation()) {
                continue;
            }
            Component label = tr("tab." + t.name().toLowerCase(Locale.ROOT));
            int bw = font.width(label) + 16;
            btn(x, y, bw, () -> label, () -> {
                tab = t;
                scroll = 0;
                rebuildWidgets();
            }, null, GOLD, () -> tab == t);
            x += bw + 3;
        }
        if (d != null && d.isStation() && tab == Tab.REWARDS) {
            tab = Tab.RECIPES;
        }
        if (d != null && !d.isStation() && tab == Tab.RECIPES) {
            tab = Tab.REWARDS;
        }
    }

    private void buildBottom() {
        int y = top + h - 22, x = cx0;
        btn(x, y, 80, () -> tr("save"), this::save, tr("save.tip"), GREEN, this::dirty);
        btn(x + 84, y, 80, tr("revert"), () -> change(() -> {
            config = MetierDefs.fromJson(savedJson);
            say(tr("reverted"), GOLD_LIGHT);
        }), tr("revert.tip"));
        btn(x + 168, y, 90, () -> tr("try"), this::trial, tr("try.tip"), BLUE, () -> false);
        btn(cx1 - 60, y, 60, tr("close"), this::onClose, null);
    }

    // ---------------------------------------------------------------- the content

    /** The rows' y on screen (after the scroll), or -1 when out of view. */
    private int rowY(int y) {
        int sy = y - scroll;
        return sy < cy0 || sy + 16 > cy1 ? -1 : sy;
    }

    private void label(int x, int y, Component text, int color) {
        int sy = rowY(y);
        if (sy >= 0) {
            labels.add(new Label(x, sy + 4, text, color, 1));
        }
    }

    private void heading(int y, Component text, int color) {
        int sy = y - scroll;
        if (sy >= cy0 - 4 && sy + 12 <= cy1) {
            labels.add(new Label(cx0 + 2, sy + 2, text, color, 1));
            sections.add(new int[] {sy + 13, color});
        }
    }

    private void field(int x, int y, int fw, String value, Consumer<String> set, @Nullable java.util.function.Predicate<String> valid,
                       @Nullable Component tip) {
        int sy = rowY(y);
        if (sy < 0) {
            return;
        }
        EditBox box = new EditBox(font, x, sy, fw, 16, Component.empty());
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
        if (tip != null) {
            box.setTooltip(net.minecraft.client.gui.components.Tooltip.create(tip));
        }
        addRenderableWidget(box);
    }

    private void number(int x, int y, int fw, Supplier<Double> get, DoubleConsumer set, double step, double min, double max, boolean integer,
                        @Nullable Component tip) {
        int sy = rowY(y);
        if (sy < 0) {
            return;
        }
        EditBox box = new EditBox(font, x, sy, fw, 16, Component.empty());
        box.setMaxLength(12);
        box.setValue(fmt(get.get(), integer));
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
        box.setTooltip(net.minecraft.client.gui.components.Tooltip.create(tip == null ? tr("number.tip") : tip.copy().append("\n").append(tr("number.tip"))));
        numbers.put(box, new Num(set, get, step, min, max, integer));
        addRenderableWidget(box);
    }

    private static String fmt(double v, boolean integer) {
        if (integer) {
            return String.valueOf(Math.round(v));
        }
        String s = String.format(Locale.ROOT, "%.3f", v);
        s = s.replaceAll("0+$", "");
        return s.endsWith(".") ? s.substring(0, s.length() - 1) : s;
    }

    private void toggle(int x, int y, int bw, Component text, Supplier<Boolean> get, Consumer<Boolean> set, @Nullable Component tip) {
        int sy = rowY(y);
        if (sy < 0) {
            return;
        }
        buttons.add(new Btn(x, sy, bw, 16, () -> Component.literal(get.get() ? "☑ " : "☐ ").append(text), () -> change(() -> set.accept(!get.get())),
                tip, get.get() ? GREEN : DIM, get::get));
    }

    private void small(int x, int y, int bw, Component text, Runnable action, @Nullable Component tip, int color) {
        int sy = rowY(y);
        if (sy >= 0) {
            buttons.add(new Btn(x, sy, bw, 16, () -> text, action, tip, color, () -> false));
        }
    }

    private void item(int x, int y, Supplier<ItemStack> stack, @Nullable Supplier<Component> tip) {
        int sy = rowY(y);
        if (sy >= 0) {
            icons.add(new Icon(x, sy, stack, tip));
        }
    }

    private void buildContent() {
        Def d = def();
        int y = cy0 + 4;
        if (d == null) {
            labels.add(new Label(cx0 + 4, cy0 + 10, tr("none"), DIM, 1));
            contentBottom = 0;
            return;
        }
        switch (tab) {
            case GENERAL -> y = general(d, y);
            case WORLD -> y = world(d, y);
            case REWARDS -> {
                y = rewards(d.critical, tr("critical"), RED, y, tr("critical.tip"));
                y = rewards(d.partial, tr("partial"), BLUE, y, tr("partial.tip"));
                y = rewards(d.perfect, tr("perfect"), GOLD, y, tr("perfect.tip"));
            }
            case RECIPES -> y = recipes(d, y);
        }
        contentBottom = y;
        int max = Math.max(0, contentBottom - (cy1 - cy0) - cy0 + 8);
        if (scroll > max) {
            scroll = max;
            rebuildWidgets();
        }
    }

    private int general(Def d, int y) {
        int lx = cx0 + 4, fx = cx0 + 190, fw = Math.min(200, cx1 - fx - 4);
        heading(y, tr("identity"), GOLD_LIGHT);
        y += 18;
        label(lx, y, tr("name"), TEXT);
        field(fx, y, fw, d.name, s -> d.name = s, null, null);
        y += ROW;
        label(lx, y, tr("id"), TEXT);
        field(fx, y, fw, d.id, s -> d.id = s, s -> s.matches("[a-z0-9_]+"), tr("id.tip"));
        y += ROW;
        toggle(lx, y, 120, tr("enabled"), () -> d.enabled, v -> d.enabled = v, tr("enabled.tip"));
        y += ROW + 4;
        heading(y, tr("minigame"), GOLD_LIGHT);
        y += 18;
        int sy = rowY(y);
        if (sy >= 0) {
            buttons.add(new Btn(lx, sy, 150, 16, () -> Component.literal("◀ ").append(gameName(d.game)).append(" ▶"), () -> change(() ->
                    d.game = Game.values()[(d.game.ordinal() + 1) % Game.values().length]), tr("game.tip"), GAME_COLORS[d.game.ordinal()], () -> true));
            icons.add(new Icon(lx + 154, sy, () -> GAME_ICONS[d.game.ordinal()], null));
        }
        y += ROW;
        y += 4;
        label(lx, y, tr("difficulty"), TEXT);
        number(fx, y, 60, () -> d.difficulty, v -> d.difficulty = v, .05, .2, 5, false, tr("difficulty.tip"));
        y += ROW;
        label(lx, y, tr("perfect_at"), TEXT);
        number(fx, y, 60, () -> d.perfectAt * 100, v -> d.perfectAt = v / 100, 1, 5, 100, true, tr("perfect_at.tip"));
        y += ROW;
        label(lx, y, tr("critical_below"), TEXT);
        number(fx, y, 60, () -> d.criticalBelow * 100, v -> d.criticalBelow = v / 100, 1, 0, 100, true, tr("critical_below.tip"));
        y += ROW + 4;
        heading(y, tr("costs"), GOLD_LIGHT);
        y += 18;
        label(lx, y, tr("durability"), TEXT);
        number(fx, y, 60, () -> (double) d.durability, v -> d.durability = (int) v, 1, 0, 1000, true, tr("durability.tip"));
        y += ROW;
        label(lx, y, tr("durability_critical"), TEXT);
        number(fx, y, 60, () -> (double) d.durabilityCritical, v -> d.durabilityCritical = (int) v, 1, 0, 1000, true, tr("durability_critical.tip"));
        y += ROW;
        label(lx, y, tr("player_cooldown"), TEXT);
        number(fx, y, 60, () -> (double) d.playerCooldown, v -> d.playerCooldown = (int) v, 5, 0, 7 * 24 * 3600, true, tr("player_cooldown.tip"));
        y += ROW + 4;
        heading(y, tr("rules"), GOLD_LIGHT);
        y += 18;
        toggle(lx, y, 200, tr("protect"), () -> d.protectBlocks, v -> d.protectBlocks = v, tr("protect.tip"));
        y += ROW;
        if (d.isStation()) {
            toggle(lx, y, 200, tr("vanilla"), () -> d.vanillaButton, v -> d.vanillaButton = v, tr("vanilla.tip"));
            y += ROW;
        }
        label(lx, y, d.isStation() ? tr("kind.station", d.recipes.size()) : tr("kind.deposit"), DIM);
        y += ROW;
        return y;
    }

    private static String asString(net.minecraft.util.FormattedCharSequence seq) {
        StringBuilder sb = new StringBuilder();
        seq.accept((i, style, cp) -> {
            sb.appendCodePoint(cp);
            return true;
        });
        return sb.toString();
    }

    private int world(Def d, int y) {
        int lx = cx0 + 4;
        heading(y, tr("blocks"), GOLD_LIGHT);
        y += 18;
        for (int i = 0; i < d.blocks.size(); i++) {
            int index = i;
            item(lx, y, () -> index < d.blocks.size() ? blockIcon(d.blocks.get(index)) : ItemStack.EMPTY, null);
            field(lx + 20, y, 190, d.blocks.get(i), s -> {
                if (index < d.blocks.size()) {
                    d.blocks.set(index, s);
                }
            }, MetierEditorScreen::blockExists, null);
            small(lx + 214, y, 16, Component.literal("✕"), () -> change(() -> d.blocks.remove(index)), tr("remove"), RED);
            y += ROW;
        }
        small(lx, y, 110, tr("add_block"), () -> change(() -> d.blocks.add("minecraft:stone")), tr("add_block.tip"), GOLD);
        small(lx + 114, y, 116, tr("add_looked"), () -> change(() -> {
            String id = lookedBlock();
            if (id != null && !d.blocks.contains(id)) {
                d.blocks.add(id);
            }
        }), tr("add_looked.tip"), GOLD);
        y += ROW + 4;
        if (!d.isStation()) {
            heading(y, tr("deposit"), GOLD_LIGHT);
            y += 18;
            label(lx, y, tr("depleted"), TEXT);
            y += 14;
            item(lx, y, () -> blockIcon(d.depletedBlock), null);
            field(lx + 20, y, 150, d.depletedBlock, s -> d.depletedBlock = s, s -> s.isEmpty() || blockExists(s), tr("depleted.tip"));
            small(lx + 174, y, 70, tr("looked"), () -> change(() -> {
                String id = lookedBlock();
                if (id != null) {
                    d.depletedBlock = id;
                }
            }), tr("looked.tip"), GOLD);
            small(lx + 248, y, 50, tr("vanish"), () -> change(() -> d.depletedBlock = "minecraft:air"), tr("vanish.tip"), GOLD);
            y += ROW;
            int fx = lx + 170;
            label(lx, y, tr("respawn"), TEXT);
            number(fx, y, 60, () -> (double) d.respawnSeconds, v -> d.respawnSeconds = (int) v, 30, 0, 7 * 24 * 3600, true, tr("respawn.tip"));
            y += ROW;
            label(lx, y, tr("uses"), TEXT);
            number(fx, y, 60, () -> (double) d.uses, v -> d.uses = (int) v, 1, 1, 10000, true, tr("uses.tip"));
            y += ROW;
            toggle(lx, y, 230, tr("deplete_critical"), () -> d.depleteOnCritical, v -> d.depleteOnCritical = v, tr("deplete_critical.tip"));
            y += ROW + 4;
        }
        heading(y, tr("tools"), GOLD_LIGHT);
        y += 18;
        if (d.tools.isEmpty()) {
            label(lx, y, tr("no_tools"), DIM);
            y += ROW;
        }
        for (int i = 0; i < d.tools.size(); i++) {
            int index = i;
            Tool t = d.tools.get(i);
            item(lx, y, () -> icon(t.item), null);
            field(lx + 20, y, 160, t.item, s -> t.item = s, MetierEditorScreen::itemExists, null);
            label(lx + 186, y, Component.literal("×"), DIM);
            number(lx + 196, y, 40, () -> t.difficulty, v -> t.difficulty = v, .05, .2, 5, false, tr("tool_difficulty.tip"));
            small(lx + 240, y, 16, Component.literal("✕"), () -> change(() -> d.tools.remove(index)), tr("remove"), RED);
            y += ROW;
        }
        small(lx, y, 110, tr("add_tool"), () -> change(() -> d.tools.add(new Tool("minecraft:iron_pickaxe", 1))), null, GOLD);
        small(lx + 114, y, 116, tr("add_held"), () -> change(() -> {
            ItemStack held = held();
            if (!held.isEmpty()) {
                d.tools.add(new Tool(BuiltInRegistries.ITEM.getKey(held.getItem()).toString(), 1));
            }
        }), tr("add_held.tip"), GOLD);
        y += ROW;
        return y;
    }

    private int rewards(List<Reward> list, Component title, int color, int y, Component tip) {
        int lx = cx0 + 4;
        heading(y, title, color);
        int hy = rowY(y);
        if (hy >= 0) {
            labels.add(new Label(cx1 - 4 - (int) (font.width(tr("columns")) * .75f), hy + 4, tr("columns"), DIM, .75f));
        }
        y += 18;
        if (list.isEmpty()) {
            label(lx, y, tr("nothing"), DIM);
            y += ROW;
        }
        for (int i = 0; i < list.size(); i++) {
            int index = i;
            Reward r = list.get(i);
            item(lx, y, () -> icon(r.item), r.components == null ? null : () -> Component.literal(r.components.toString()).withStyle(ChatFormatting.GRAY));
            field(lx + 20, y, 130, r.item, s -> r.item = s, MetierEditorScreen::itemExists, null);
            number(lx + 154, y, 30, () -> (double) r.min, v -> r.min = (int) v, 1, 0, 64 * 36, true, tr("min.tip"));
            number(lx + 188, y, 30, () -> (double) r.max, v -> r.max = (int) v, 1, 0, 64 * 36, true, tr("max.tip"));
            number(lx + 222, y, 34, () -> r.chance * 100, v -> r.chance = v / 100, 5, 0, 100, true, tr("chance.tip"));
            if (r.components != null) {
                small(lx + 260, y, 16, Component.literal("✦"), () -> change(() -> r.components = null), tr("data.tip"), 0xFFD8A0FF);
            }
            small(lx + 280, y, 16, Component.literal("✕"), () -> change(() -> list.remove(index)), tr("remove"), RED);
            y += ROW;
        }
        small(lx, y, 110, tr("add_item"), () -> change(() -> list.add(new Reward("minecraft:raw_copper", 1, 1))), tip, GOLD);
        small(lx + 114, y, 130, tr("add_held_data"), () -> change(() -> {
            ItemStack held = held();
            if (!held.isEmpty()) {
                Reward r = new Reward(BuiltInRegistries.ITEM.getKey(held.getItem()).toString(), held.getCount(), held.getCount());
                JsonObject data = MetierDefs.components(held, registries());
                r.components = data;
                list.add(r);
            }
        }), tr("add_held_data.tip"), GOLD);
        y += ROW + 6;
        return y;
    }

    private int recipes(Def d, int y) {
        int lx = cx0 + 4;
        heading(y, tr("recipes"), GOLD_LIGHT);
        y += 18;
        // The recipes, as chips
        int x = lx;
        for (int i = 0; i < d.recipes.size(); i++) {
            int index = i;
            Recipe r = d.recipes.get(i);
            String name = r.name.length() > 18 ? r.name.substring(0, 17) + "…" : r.name;
            int bw = font.width(name) + 12;
            if (x + bw > cx1 - 4) {
                x = lx;
                y += 18;
            }
            int sy = rowY(y);
            if (sy >= 0) {
                buttons.add(new Btn(x, sy, bw, 16, () -> Component.literal(name), () -> {
                    recipe = index;
                    rebuildWidgets();
                }, null, GOLD, () -> recipe == index));
            }
            x += bw + 3;
        }
        y += ROW;
        small(lx, y, 70, tr("recipe_new"), () -> change(() -> {
            Recipe r = new Recipe();
            r.name = tr("recipe_default").getString();
            r.partial.add(new Reward("minecraft:stick", 1, 1));
            d.recipes.add(r);
            recipe = d.recipes.size() - 1;
        }), null, GOLD);
        small(lx + 74, y, 70, tr("duplicate"), () -> change(() -> {
            Recipe r = currentRecipe();
            if (r != null) {
                MetierDefs.Config tmp = new MetierDefs.Config();
                Def holder = new Def();
                holder.recipes.add(r);
                tmp.metiers.add(holder);
                Recipe copy = MetierDefs.copy(tmp).metiers.get(0).recipes.get(0);
                copy.name = copy.name + " 2";
                d.recipes.add(recipe + 1, copy);
                recipe++;
            }
        }), null, GOLD);
        small(lx + 148, y, 70, tr("delete"), () -> change(() -> {
            if (!d.recipes.isEmpty()) {
                d.recipes.remove(Mth.clamp(recipe, 0, d.recipes.size() - 1));
                recipe = Math.max(0, recipe - 1);
            }
        }), tr("recipe_delete.tip"), RED);
        small(lx + 222, y, 24, Component.literal("◀"), () -> change(() -> moveRecipe(d, -1)), tr("move.tip"), GOLD);
        small(lx + 248, y, 24, Component.literal("▶"), () -> change(() -> moveRecipe(d, 1)), tr("move.tip"), GOLD);
        y += ROW + 4;
        Recipe r = currentRecipe();
        if (r == null) {
            label(lx, y, tr("no_recipes"), DIM);
            return y + ROW;
        }
        int fx = lx + 150;
        label(lx, y, tr("recipe_name"), TEXT);
        field(fx, y, Math.min(160, cx1 - fx - 4), r.name, s -> r.name = s, null, null);
        y += ROW;
        label(lx, y, tr("recipe_difficulty"), TEXT);
        number(fx, y, 60, () -> r.difficulty, v -> r.difficulty = v, .05, .2, 5, false, tr("recipe_difficulty.tip"));
        y += ROW;
        label(lx, y, tr("recipe_cooldown"), TEXT);
        number(fx, y, 60, () -> (double) r.cooldown, v -> r.cooldown = (int) v, 5, 0, 7 * 24 * 3600, true, tr("recipe_cooldown.tip"));
        y += ROW;
        toggle(lx, y, 240, tr("lose_inputs"), () -> r.loseInputsOnFail, v -> r.loseInputsOnFail = v, tr("lose_inputs.tip"));
        y += ROW + 4;
        heading(y, tr("inputs"), GOLD_LIGHT);
        y += 18;
        for (int i = 0; i < r.inputs.size(); i++) {
            int index = i;
            Ingredient in = r.inputs.get(i);
            item(lx, y, () -> icon(in.item), null);
            field(lx + 20, y, 150, in.item, s -> in.item = s, MetierEditorScreen::itemExists, null);
            label(lx + 176, y, Component.literal("×"), DIM);
            number(lx + 186, y, 36, () -> (double) in.count, v -> in.count = (int) v, 1, 1, 64 * 36, true, null);
            small(lx + 226, y, 16, Component.literal("✕"), () -> change(() -> r.inputs.remove(index)), tr("remove"), RED);
            y += ROW;
        }
        small(lx, y, 110, tr("add_input"), () -> change(() -> r.inputs.add(new Ingredient("minecraft:iron_ingot", 1))), null, GOLD);
        small(lx + 114, y, 116, tr("add_held"), () -> change(() -> {
            ItemStack held = held();
            if (!held.isEmpty()) {
                r.inputs.add(new Ingredient(BuiltInRegistries.ITEM.getKey(held.getItem()).toString(), held.getCount()));
            }
        }), tr("add_held_input.tip"), GOLD);
        y += ROW + 6;
        y = rewards(r.critical, tr("critical"), RED, y, tr("recipe_critical.tip"));
        y = rewards(r.partial, tr("partial"), BLUE, y, tr("partial.tip"));
        y = rewards(r.perfect, tr("perfect"), GOLD, y, tr("perfect.tip"));
        return y;
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
        tab = Tab.GENERAL;
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
        if (now - escapeAt > 3000) {
            escapeAt = now;
            say(tr("delete.confirm"), RED);
            return;
        }
        escapeAt = -10000;
        config.metiers.remove(selected);
        selected = Math.max(0, selected - 1);
        scroll = 0;
    }

    private void moveDef(int dir) {
        int to = selected + dir;
        if (def() == null || to < 0 || to >= config.metiers.size()) {
            return;
        }
        config.metiers.add(to, config.metiers.remove(selected));
        selected = to;
    }

    private void moveRecipe(Def d, int dir) {
        int from = Mth.clamp(recipe, 0, Math.max(0, d.recipes.size() - 1)), to = from + dir;
        if (to < 0 || to >= d.recipes.size()) {
            return;
        }
        d.recipes.add(to, d.recipes.remove(from));
        recipe = to;
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
        Component heading = Component.literal(r != null ? r.name : d.name);
        Component sub = tr("trial_sub");
        Minecraft.getInstance().setScreen(MetierClient.create(d.game, null, difficulty, new java.util.Random().nextLong(), subject, toolStack, inputs, heading,
                sub, (float) d.perfectAt, (float) d.criticalBelow).returnTo(this));
    }

    // ---------------------------------------------------------------- drawing

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderTransparentBackground(g);
        g.fill(left - 3, top - 2, left + w + 4, top + h + 5, 0x70000000);
        Draw.panel(g, left, top, left + w, top + h, 0xFF8A6438);
        g.fillGradient(left + 1, top + 1, left + w - 1, top + 26, 0xA05A4020, 0x005A4020);
        Draw.item(g, new ItemStack(Items.SMITHING_TABLE), left + 6, top + 5, 1);
        Draw.text(g, title, left + 26, top + 6, GOLD_LIGHT, 1.1f);
        Component count = tr("count", config.metiers.size());
        Draw.text(g, count, left + w - 10 - font.width(count) * .75f, top + 9, DIM, .75f);
        if (dirty()) {
            Component c = tr("unsaved");
            Draw.text(g, c, left + w - 10 - font.width(c) * .75f - font.width(count) * .75f - 10, top + 9, 0xFFFFB060, .75f);
        }
        // The list's and the content's panels
        Draw.frame(g, left + 4, top + 28, left + LIST_W + 8, top + h - 58, PANEL2, 0x807A5A22);
        Draw.frame(g, cx0 - 4, cy0 - 4, cx1 + 4, cy1 + 2, PANEL2, 0x807A5A22);
        Def d = def();
        if (d != null) {
            Component kind = d.isStation() ? tr("kind_short.station") : tr("kind_short.deposit");
            Draw.text(g, kind, cx1 - font.width(kind) * .75f, top + 34, GAME_COLORS[d.game.ordinal()], .75f);
        }
        for (int[] s : sections) {
            g.fill(cx0, s[0], cx1 - 4, s[0] + 1, Draw.argb(120, s[1]));
        }
        Component tip = null;
        for (Btn b : buttons) {
            boolean over = mouseX >= b.x() && mouseX < b.x() + b.w() && mouseY >= b.y() && mouseY < b.y() + b.h();
            boolean lit = b.lit().get();
            int fill = lit ? Draw.argb(over ? 200 : 150, Draw.lerp(b.color(), 0xFF000000, .55f)) : over ? 0xC0302820 : 0x90000000;
            Draw.frame(g, b.x(), b.y(), b.x() + b.w(), b.y() + b.h(), fill, over || lit ? b.color() : Draw.argb(150, Draw.lerp(b.color(), 0xFF000000, .4f)));
            Component label = b.label().get();
            boolean listRow = b.h() == 19;
            float scale = .9f;
            int maxW = (int) ((b.w() - (listRow ? 24 : 6)) / scale);
            String text = label.getString();
            if (font.width(text) > maxW) {
                text = font.plainSubstrByWidth(text, maxW - font.width("…")) + "…";
            }
            Component shown = Component.literal(text).withStyle(label.getStyle());
            if (listRow) {
                Draw.text(g, shown, b.x() + 22, b.y() + 5, lit ? 0xFFFFFFFF : TEXT, scale);
            } else {
                Draw.centered(g, shown, b.x() + b.w() / 2f, b.y() + 5, over || lit ? 0xFFFFFFFF : TEXT, scale);
            }
            if (over && b.tip() != null) {
                tip = b.tip();
            }
        }
        for (Label l : labels) {
            Draw.text(g, l.text(), l.x(), l.y(), l.color(), l.scale());
        }
        ItemStack itemTip = ItemStack.EMPTY;
        for (Icon i : icons) {
            ItemStack s = i.stack().get();
            if (s.isEmpty()) {
                g.fill(i.x(), i.y(), i.x() + 16, i.y() + 16, 0x60FF4040);
                continue;
            }
            g.renderItem(s, i.x(), i.y());
            if (mouseX >= i.x() && mouseX < i.x() + 16 && mouseY >= i.y() && mouseY < i.y() + 16) {
                if (i.tip() != null) {
                    tip = i.tip().get();
                } else {
                    itemTip = s;
                }
            }
        }
        if (contentBottom > cy1 + scroll) {
            Draw.centered(g, Component.literal("▼"), (cx0 + cx1) / 2f, cy1 - 8, 0xC0FFFFFF, .8f);
        }
        if (scroll > 0) {
            Draw.centered(g, Component.literal("▲"), (cx0 + cx1) / 2f, cy0 - 2, 0xC0FFFFFF, .8f);
        }
        super.render(g, mouseX, mouseY, partialTick);
        if (status != null && Util.getMillis() - statusAt < 4000) {
            Draw.text(g, status, cx0 + 270, top + h - 18, statusColor, .8f);
        }
        if (tip != null) {
            g.renderTooltip(font, font.split(tip, 220), mouseX, mouseY);
        } else if (!itemTip.isEmpty()) {
            g.renderTooltip(font, itemTip, mouseX, mouseY);
        }
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        // Drawn in render, under the panel
    }

    // ---------------------------------------------------------------- input

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            for (Btn b : new ArrayList<>(buttons)) {
                if (mouseX >= b.x() && mouseX < b.x() + b.w() && mouseY >= b.y() && mouseY < b.y() + b.h()) {
                    Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK.value(), 1, .45f));
                    setFocused(null);
                    b.action().run();
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        for (var entry : numbers.entrySet()) {
            EditBox box = entry.getKey();
            if (box.isMouseOver(mouseX, mouseY)) {
                Num n = entry.getValue();
                double step = n.step() * (hasShiftDown() ? 10 : 1);
                double v = Mth.clamp(n.get().get() + Math.signum(scrollY) * step, n.min(), n.max());
                n.set().accept(n.integer() ? Math.round(v) : Math.round(v * 1000) / 1000.0);
                box.setValue(fmt(n.get().get(), n.integer()));
                return true;
            }
        }
        if (mouseX < left + LIST_W + 8) {
            listScroll = Math.max(0, listScroll - (int) Math.signum(scrollY));
            rebuildWidgets();
            return true;
        }
        int max = Math.max(0, contentBottom - cy1 + 8);
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
            if (dirty() && Util.getMillis() - escapeAt > 3000) {
                escapeAt = Util.getMillis();
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
        tab = Tab.valueOf(tabName.toUpperCase(Locale.ROOT));
        scroll = 0;
        rebuildWidgets();
    }

    public void startTrial() {
        trial();
    }
}
