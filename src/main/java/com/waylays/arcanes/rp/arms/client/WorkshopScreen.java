package com.waylays.arcanes.rp.arms.client;

import com.waylays.arcanes.rp.arms.Arms;
import com.waylays.arcanes.rp.arms.Proc;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The weapons' workshop: a dark steel panel with gold, the item at the top, four tabs, and beside them the item as it
 * will read (its name, its lore, its stats and effects, kept up to date while one types).
 * <ul>
 * <li>General: the name, unbreakable, shining.</li>
 * <li>Lore: a long text (sixteen lines), colour chips that put their code where one writes.</li>
 * <li>Stats: eight stats, each with its picture.</li>
 * <li>Effects: up to eight lines: on a blow struck or taken, the effect (any effect of the game or of a mod, chosen
 * in a list one scrolls or searches), on the other one or on oneself, its chance, its level, its seconds.</li>
 * </ul>
 */
public class WorkshopScreen extends Screen {
    private static final int CW = 286, MAX_LORE = 16, MAX_PROCS = 8;
    /** The window's size: as wide and tall as it may be (448 x 256), less on a small screen so all of it shows. */
    private int W = 448, H = 256;
    private static final int BG = 0xF2101418, HEAD = 0xFF1A2129, PANEL = 0xFF161C23, LINE = 0xFF2C3846, GOLD = 0xFFE8C677, GOLD_DIM = 0xFF8C7442;
    private static final int TEXT = 0xFFE6ECF2, DIM = 0xFF8592A0, FIELD = 0xFF0B0E12;
    private static final String[] TABS = {"general", "lore", "stats", "effects"};
    private static final String[] STAT_KEYS = {"damage", "attack_speed", "armor", "toughness", "knockback", "speed", "health", "reach"};
    private static final ItemStack[] STAT_ICONS = {new ItemStack(Items.IRON_SWORD), new ItemStack(Items.CLOCK), new ItemStack(Items.IRON_CHESTPLATE),
            new ItemStack(Items.DIAMOND), new ItemStack(Items.ANVIL), new ItemStack(Items.SUGAR), new ItemStack(Items.GOLDEN_APPLE),
            new ItemStack(Items.ENDER_EYE)};
    private static final String CODES = "0123456789abcdef";
    private static final int[] CODE_COLOURS = {0x000000, 0x0000AA, 0x00AA00, 0x00AAAA, 0xAA0000, 0xAA00AA, 0xFFAA00, 0xAAAAAA, 0x555555, 0x5555FF,
            0x55FF55, 0x55FFFF, 0xFF5555, 0xFF55FF, 0xFFFF55, 0xFFFFFF};

    private static final class Row {
        String type = "*feu", chance = "5", power = "1", seconds = "4";
        boolean guard, self;
    }

    private final ItemStack stack;
    private int tab;
    private String name;
    private String lore;
    private boolean unbreakable, glint;
    /** The durability left and the most (empty: the item has none, or it is left as it is). */
    private String durability = "", maxDurability = "";
    private final String[] stats = new String[Arms.STATS.size()];
    private final List<Row> rows = new ArrayList<>();
    private final List<int[]> fields = new ArrayList<>();
    private MultiLineEditBox loreBox;
    private int x0, y0;
    // The effect list, while one is being chosen for row {@code picking}
    private int picking = -1, pickScroll;
    private EditBox search;
    private List<Effects.Entry> all, shown = new ArrayList<>();

    public WorkshopScreen(ItemStack stack) {
        super(Component.translatable("arms.arcanes_oublies.title"));
        this.stack = stack.copy();
        Component custom = stack.get(DataComponents.CUSTOM_NAME);
        name = custom == null ? "" : custom.getString().replace('§', '&');
        ItemLore l = stack.get(DataComponents.LORE);
        StringBuilder b = new StringBuilder();
        if (l != null) {
            for (Component c : l.lines()) {
                if (b.length() > 0) {
                    b.append('\n');
                }
                b.append(c.getString().replace('§', '&'));
            }
        }
        lore = b.toString();
        unbreakable = stack.has(DataComponents.UNBREAKABLE);
        glint = Boolean.TRUE.equals(stack.get(DataComponents.ENCHANTMENT_GLINT_OVERRIDE));
        if (stack.isDamageableItem()) {
            maxDurability = String.valueOf(stack.getMaxDamage());
            durability = String.valueOf(stack.getMaxDamage() - stack.getDamageValue());
        }
        double[] v = Arms.stats(stack);
        for (int i = 0; i < stats.length; i++) {
            stats[i] = Double.isNaN(v[i]) ? "" : trim(v[i]);
        }
        List<Proc> list = stack.get(Arms.PROCS.get());
        if (list != null) {
            for (Proc raw : list) {
                Proc p = raw.normalized();
                Row r = new Row();
                r.type = p.type();
                r.chance = trim(p.chance());
                r.power = String.valueOf(p.power());
                r.seconds = String.valueOf(p.seconds());
                r.guard = p.guard();
                r.self = p.self();
                rows.add(r);
            }
        }
    }

    public static void open() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && !mc.player.getMainHandItem().isEmpty()) {
            mc.setScreen(new WorkshopScreen(mc.player.getMainHandItem()));
        }
    }

    private static String trim(double d) {
        return d == Math.rint(d) ? String.valueOf((long) d) : String.format(Locale.ROOT, "%.2f", d);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    // ---------------------------------------------------------------- widgets

    /** A flat button: dark, a gold edge when it matters, lit under the pointer. */
    private final class Flat extends Button {
        private final int accent;

        Flat(int x, int y, int w, int h, Component msg, OnPress press, int accent) {
            super(x, y, w, h, msg, press, DEFAULT_NARRATION);
            this.accent = accent;
        }

        @Override
        protected void renderWidget(GuiGraphics g, int mx, int my, float pt) {
            boolean hover = isHoveredOrFocused() && active;
            int top = !active ? 0xFF2A3440 : hover ? 0xFF34404E : 0xFF222B35, bottom = !active ? 0xFF222B35 : hover ? 0xFF283240 : 0xFF1A2129;
            g.fill(getX(), getY(), getX() + width, getY() + height, !active ? accent : hover ? accent : LINE);
            g.fillGradient(getX() + 1, getY() + 1, getX() + width - 1, getY() + height - 1, top, bottom);
            int c = !active ? accent | 0xFF000000 : hover ? 0xFFFFFFFF : TEXT;
            g.drawCenteredString(font, getMessage(), getX() + width / 2, getY() + (height - 8) / 2, c);
        }
    }

    private Flat flat(int x, int y, int w, int h, Component msg, Button.OnPress press, int accent) {
        Flat f = new Flat(x, y, w, h, msg, press, accent);
        addRenderableWidget(f);
        return f;
    }

    /** A text field on a dark well (drawn under it). */
    private EditBox field(int x, int y, int w, String value, int max, java.util.function.Consumer<String> to) {
        EditBox e = new EditBox(font, x + 4, y + 4, w - 8, 12, Component.empty());
        e.setBordered(false);
        e.setMaxLength(max);
        e.setValue(value);
        e.setTextColor(TEXT);
        e.setResponder(to);
        addRenderableWidget(e);
        fields.add(new int[] {x, y, w, 16});
        return e;
    }

    @Override
    protected void init() {
        W = Math.max(CW + 100, Math.min(448, width - 8));
        H = Math.max(220, Math.min(256, height - 8));
        x0 = Math.max(4, (width - W) / 2);
        y0 = Math.max(4, (height - H) / 2);
        clearWidgets();
        fields.clear();
        loreBox = null;
        search = null;
        if (picking >= 0) {
            initPicker();
            return;
        }
        for (int i = 0; i < TABS.length; i++) {
            int t = i;
            Flat b = flat(x0 + 10 + i * 72, y0 + 40, 68, 16, Component.translatable("arms.arcanes_oublies.tab." + TABS[i]), x -> {
                tab = t;
                init();
            }, GOLD);
            b.active = tab != i;
        }
        switch (tab) {
            case 0 -> general();
            case 1 -> loreTab();
            case 2 -> statsTab();
            default -> effectsTab();
        }
        // Saving and leaving, in the header: always on the screen, whatever the interface's scale
        Flat ok = flat(x0 + W - 186, y0 + 8, 86, 18, Component.translatable("arms.arcanes_oublies.save"), b -> save(), GOLD);
        ok.setTooltip(net.minecraft.client.gui.components.Tooltip.create(Component.translatable("arms.arcanes_oublies.save_hint")));
        flat(x0 + W - 94, y0 + 8, 86, 18, Component.translatable("gui.cancel"), b -> onClose(), LINE);
    }

    private void general() {
        int x = x0 + 10, y = y0 + 64;
        field(x, y + 12, CW, name, 120, s -> name = s);
        flat(x, y + 44, CW / 2 - 4, 18, toggle("unbreakable", unbreakable), b -> {
            unbreakable = !unbreakable;
            b.setMessage(toggle("unbreakable", unbreakable));
        }, GOLD);
        flat(x + CW / 2 + 4, y + 44, CW / 2 - 4, 18, toggle("glint", glint), b -> {
            glint = !glint;
            b.setMessage(toggle("glint", glint));
        }, GOLD);
        chips(x, y + 76, s -> {
            name = name + s;
            init();
        });
        // The durability: what is left, the most, and a button that fills it up
        EditBox left = field(x, y + 126, 70, durability, 7, s -> durability = s);
        field(x + 86, y + 126, 70, maxDurability, 7, s -> maxDurability = s);
        flat(x + 164, y + 125, 60, 18, Component.translatable("arms.arcanes_oublies.durability_full"), b -> {
            durability = maxDurability;
            left.setValue(maxDurability);
        }, GOLD).setTooltip(net.minecraft.client.gui.components.Tooltip.create(Component.translatable("arms.arcanes_oublies.durability_hint")));
    }

    private static Component toggle(String key, boolean on) {
        return Component.translatable("arms.arcanes_oublies." + key).append(" : ")
                .append(Component.translatable(on ? "options.on" : "options.off").withColor(on ? 0xFF8CE38C : 0xFFFF8A80));
    }

    /** The colour chips: each puts its code where one writes. */
    private void chips(int x, int y, java.util.function.Consumer<String> put) {
        for (int i = 0; i < CODES.length(); i++) {
            String code = "&" + CODES.charAt(i);
            int c = CODE_COLOURS[i];
            Button b = new Button(x + i * 17, y, 15, 12, Component.empty(), btn -> put.accept(code), narration -> narration.get()) {
                @Override
                protected void renderWidget(GuiGraphics g, int mx, int my, float pt) {
                    g.fill(getX(), getY(), getX() + width, getY() + height, isHovered() ? 0xFFFFFFFF : LINE);
                    g.fill(getX() + 1, getY() + 1, getX() + width - 1, getY() + height - 1, 0xFF000000 | c);
                }
            };
            b.setTooltip(net.minecraft.client.gui.components.Tooltip.create(Component.literal(code)));
            addRenderableWidget(b);
        }
        String[] styles = {"&l", "&o", "&n", "&r"};
        for (int i = 0; i < styles.length; i++) {
            String st = styles[i];
            flat(x + i * 34, y + 16, 32, 12, Component.literal(st), b -> put.accept(st), LINE);
        }
    }

    private void loreTab() {
        int x = x0 + 10, y = y0 + 64;
        loreBox = new MultiLineEditBox(font, x, y, CW, 140, Component.translatable("arms.arcanes_oublies.lore_hint"), Component.empty());
        loreBox.setCharacterLimit(MAX_LORE * 80);
        loreBox.setValue(lore);
        loreBox.setValueListener(s -> lore = s);
        addRenderableWidget(loreBox);
        chips(x, y + 146, s -> {
            setFocused(loreBox);
            loreBox.setFocused(true);
            for (char ch : s.toCharArray()) {
                loreBox.charTyped(ch, 0);
            }
        });
    }

    private void statsTab() {
        for (int i = 0; i < stats.length; i++) {
            int k = i;
            int col = i / 4, row = i % 4;
            field(x0 + 10 + col * 146 + 92, y0 + 72 + row * 30, 46, stats[i], 10, s -> stats[k] = s);
        }
    }

    private void effectsTab() {
        int y = y0 + 78;
        for (int i = 0; i < rows.size(); i++) {
            Row r = rows.get(i);
            int k = i, ry = y + i * 21, x = x0 + 10;
            flat(x, ry, 46, 17, Component.translatable(r.guard ? "arms.arcanes_oublies.when_struck" : "arms.arcanes_oublies.when_strike")
                    .withColor(r.guard ? 0xFF7FD4FF : 0xFFFFB45A), b -> {
                r.guard = !r.guard;
                init();
            }, LINE);
            Flat pick = flat(x + 48, ry, 98, 17, Component.empty(), b -> {
                picking = k;
                pickScroll = 0;
                init();
            }, LINE);
            pick.setTooltip(net.minecraft.client.gui.components.Tooltip.create(Effects.name(r.type).copy().append("\n").append(Component.literal(r.type)
                    .withColor(DIM))));
            flat(x + 148, ry, 34, 17, Component.translatable(r.self ? "arms.arcanes_oublies.self" : "arms.arcanes_oublies.other"), b -> {
                r.self = !r.self;
                init();
            }, LINE);
            field(x + 184, ry, 30, r.chance, 6, s -> r.chance = s);
            field(x + 216, ry, 24, r.power, 3, s -> r.power = s);
            field(x + 242, ry, 30, r.seconds, 4, s -> r.seconds = s);
            flat(x + 274, ry, 12, 17, Component.literal("✕"), b -> {
                rows.remove(k);
                init();
            }, 0xFFAA4444);
        }
        if (rows.size() < MAX_PROCS) {
            flat(x0 + 10, y + rows.size() * 21, 146, 17, Component.translatable("arms.arcanes_oublies.add"), b -> {
                rows.add(new Row());
                picking = rows.size() - 1;
                pickScroll = 0;
                init();
            }, GOLD);
        }
    }

    // ---------------------------------------------------------------- the effect list

    private int px() {
        return x0 + Math.max(4, Math.min(70, W - PICK_W - 4));
    }

    private int py() {
        return y0 + Math.max(4, Math.min(30, H - PICK_H - 4));
    }

    private void initPicker() {
        if (all == null) {
            all = Effects.all();
        }
        search = new EditBox(font, px() + 26, py() + 30, 260, 12, Component.empty());
        search.setBordered(false);
        search.setTextColor(TEXT);
        search.setHint(Component.translatable("arms.arcanes_oublies.search").withColor(DIM));
        search.setResponder(s -> {
            filter(s);
            pickScroll = 0;
        });
        addRenderableWidget(search);
        setInitialFocus(search);
        filter("");
    }

    private void filter(String s) {
        String q = s.toLowerCase(Locale.ROOT).trim();
        shown = new ArrayList<>();
        for (Effects.Entry e : all) {
            if (q.isEmpty() || e.name().getString().toLowerCase(Locale.ROOT).contains(q) || e.id().toLowerCase(Locale.ROOT).contains(q)) {
                shown.add(e);
            }
        }
    }

    private static final int PICK_W = 308, PICK_H = 216, ROW_H = 18, LIST_Y = 50;

    private int visibleRows() {
        return (PICK_H - LIST_Y - 8) / ROW_H;
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (picking >= 0) {
            int lx = px() + 8, ly = py() + LIST_Y;
            if (mx >= lx && mx < lx + PICK_W - 22 && my >= ly && my < ly + visibleRows() * ROW_H) {
                int i = pickScroll + (int) ((my - ly) / ROW_H);
                if (i < shown.size() && picking < rows.size()) {
                    rows.get(picking).type = shown.get(i).id();
                    closePicker();
                }
                return true;
            }
            if (mx < px() || mx > px() + PICK_W || my < py() || my > py() + PICK_H) {
                closePicker();
                return true;
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double dx, double dy) {
        if (picking >= 0) {
            pickScroll = Mth.clamp(pickScroll - (int) Math.signum(dy) * 2, 0, Math.max(0, shown.size() - visibleRows()));
            return true;
        }
        return super.mouseScrolled(mx, my, dx, dy);
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        // Ctrl+S saves, from any tab
        if (picking < 0 && key == GLFW.GLFW_KEY_S && (mods & GLFW.GLFW_MOD_CONTROL) != 0) {
            save();
            return true;
        }
        if (picking >= 0 && key == GLFW.GLFW_KEY_ESCAPE) {
            closePicker();
            return true;
        }
        if (picking >= 0 && (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) && !shown.isEmpty()) {
            rows.get(picking).type = shown.get(0).id();
            closePicker();
            return true;
        }
        return super.keyPressed(key, scan, mods);
    }

    private void closePicker() {
        picking = -1;
        init();
    }

    // ---------------------------------------------------------------- saving

    private static float num(String s, float fallback) {
        try {
            return Float.parseFloat(s.trim().replace(',', '.'));
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    /** A whole number typed in a field, or -1 (empty, or not a number). */
    private static int whole(String s) {
        try {
            int v = Integer.parseInt(s.trim());
            return v > 0 ? v : -1;
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private List<String> loreLines() {
        List<String> out = new ArrayList<>();
        for (String l : lore.split("\n", -1)) {
            if (out.size() < MAX_LORE) {
                out.add(l.length() > 280 ? l.substring(0, 280) : l);
            }
        }
        while (!out.isEmpty() && out.get(out.size() - 1).isBlank()) {
            out.remove(out.size() - 1);
        }
        return out;
    }

    private List<Proc> procs() {
        List<Proc> list = new ArrayList<>();
        for (Row r : rows) {
            list.add(new Proc(r.type, num(r.chance, 0), (int) num(r.power, 1), (int) num(r.seconds, 0), r.guard, r.self));
        }
        return list;
    }

    private void save() {
        List<Float> st = new ArrayList<>();
        for (String s : stats) {
            st.add(s.isBlank() ? Float.NaN : num(s, Float.NaN));
        }
        PacketDistributor.sendToServer(new Arms.Edit(name, loreLines(), unbreakable, glint, st, procs(), whole(durability), whole(maxDurability)));
        onClose();
    }

    // ---------------------------------------------------------------- drawing

    @Override
    public void renderBackground(GuiGraphics g, int mx, int my, float pt) {
        g.fill(0, 0, width, height, 0xA0000000);
        g.fill(x0 - 2, y0 - 2, x0 + W + 2, y0 + H + 2, GOLD_DIM);
        g.fill(x0 - 1, y0 - 1, x0 + W + 1, y0 + H + 1, 0xFF05070A);
        g.fill(x0, y0, x0 + W, y0 + H, BG);
        g.fillGradient(x0, y0, x0 + W, y0 + 34, 0xFF243140, HEAD);
        g.fill(x0, y0 + 34, x0 + W, y0 + 35, GOLD_DIM);
        // The content well, the preview's
        g.fill(x0 + 6, y0 + 60, x0 + 10 + CW + 4, y0 + H - 8, PANEL);
        g.fill(x0 + CW + 20, y0 + 40, x0 + W - 6, y0 + H - 8, 0xFF0D1116);
        g.fill(x0 + CW + 20, y0 + 40, x0 + W - 6, y0 + 41, GOLD_DIM);
        for (int[] f : fields) {
            g.fill(f[0] - 1, f[1] - 1, f[0] + f[2] + 1, f[1] + f[3] + 1, LINE);
            g.fill(f[0], f[1], f[0] + f[2], f[1] + f[3], FIELD);
        }
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        super.render(g, mx, my, pt);
        // The header: the item big, the title, what it is
        g.pose().pushPose();
        g.pose().translate(x0 + 8, y0 + 4, 0);
        g.pose().scale(1.6f, 1.6f, 1);
        g.renderItem(stack, 0, 0);
        g.pose().popPose();
        int room = W - 186 - 46;
        g.drawString(font, font.plainSubstrByWidth(title.getString(), room), x0 + 40, y0 + 7, GOLD, true);
        g.drawString(font, font.plainSubstrByWidth(Component.translatable("arms.arcanes_oublies.subtitle").getString(), room), x0 + 40, y0 + 19, DIM, false);
        int x = x0 + 10, y = y0 + 64;
        switch (tab) {
            case 0 -> {
                g.drawString(font, Component.translatable("arms.arcanes_oublies.name"), x, y, GOLD, false);
                g.drawString(font, Component.translatable("arms.arcanes_oublies.colours_title"), x, y + 66, GOLD, false);
                g.drawString(font, Component.translatable("arms.arcanes_oublies.durability"), x, y + 114, GOLD, false);
                g.drawString(font, "/", x + 76, y + 130, DIM, false);
            }
            case 1 -> {
                int lines = lore.isEmpty() ? 0 : lore.split("\n", -1).length;
                g.drawString(font, Component.translatable("arms.arcanes_oublies.lore_count", lines, MAX_LORE), x + CW - 70, y0 + 44, lines > MAX_LORE ? 0xFFFF8A80 : DIM, false);
            }
            case 2 -> {
                for (int i = 0; i < STAT_KEYS.length; i++) {
                    int col = i / 4, row = i % 4;
                    int sx = x0 + 10 + col * 146, sy = y0 + 72 + row * 30;
                    g.renderItem(STAT_ICONS[i], sx, sy);
                    g.drawString(font, Component.translatable("arms.arcanes_oublies.stat." + STAT_KEYS[i]), sx + 20, sy + 4, TEXT, false);
                }
                g.drawWordWrap(font, Component.translatable("arms.arcanes_oublies.stats_help"), x, y0 + H - 58, CW, DIM);
            }
            default -> {
                g.drawString(font, Component.translatable("arms.arcanes_oublies.col.when"), x + 2, y0 + 66, DIM, false);
                g.drawString(font, Component.translatable("arms.arcanes_oublies.col.kind"), x + 50, y0 + 66, DIM, false);
                g.drawString(font, Component.translatable("arms.arcanes_oublies.col.on"), x + 150, y0 + 66, DIM, false);
                g.drawString(font, "%", x + 194, y0 + 66, DIM, false);
                g.drawString(font, Component.translatable("arms.arcanes_oublies.col.power"), x + 216, y0 + 66, DIM, false);
                g.drawString(font, Component.translatable("arms.arcanes_oublies.col.seconds"), x + 244, y0 + 66, DIM, false);
                // Each row's effect: its picture and name on its button
                for (int i = 0; i < rows.size() && picking < 0; i++) {
                    int ry = y0 + 78 + i * 21;
                    Effects.icon(g, rows.get(i).type, x + 51, ry + 2, 13);
                    String n = font.plainSubstrByWidth(Effects.name(rows.get(i).type).getString(), 76);
                    g.drawString(font, n, x + 66, ry + 5, Effects.colour(rows.get(i).type), false);
                }
            }
        }
        preview(g);
        if (picking >= 0) {
            picker(g, mx, my);
        }
    }

    /** The item as it will read, on the right. */
    private void preview(GuiGraphics g) {
        int x = x0 + CW + 26, y = y0 + 46, w = W - CW - 38;
        g.drawString(font, Component.translatable("arms.arcanes_oublies.preview"), x, y, GOLD, false);
        y += 14;
        List<Component> lines = new ArrayList<>();
        lines.add(name.isBlank() ? stack.getHoverName() : Component.literal(name.replaceAll("&([0-9a-fk-or])", "§$1")));
        for (String l : loreLines()) {
            lines.add(Component.literal("§7" + l.replaceAll("&([0-9a-fk-or])", "§$1")));
        }
        for (int i = 0; i < stats.length; i++) {
            if (!stats[i].isBlank()) {
                lines.add(Component.literal("§2" + stats[i] + (i == 5 ? "% " : " ")).append(Component.translatable("arms.arcanes_oublies.stat." + STAT_KEYS[i])
                        .withColor(0xFF55AA55)));
            }
        }
        if (unbreakable) {
            lines.add(Component.translatable("item.unbreakable").withColor(0xFF5555FF));
        } else if (whole(maxDurability) > 0) {
            int most = whole(maxDurability), left = whole(durability) > 0 ? Math.min(most, whole(durability)) : most;
            lines.add(Component.translatable("item.durability", left, most).withColor(0xFFAAAAAA));
        }
        for (Proc p : procs()) {
            lines.add(ArmsClient.line(p));
        }
        int bottom = y0 + H - 12;
        for (Component c : lines) {
            for (FormattedCharSequence s : font.split(c, w)) {
                if (y > bottom - 9) {
                    g.drawString(font, "…", x, y, DIM, false);
                    return;
                }
                g.drawString(font, s, x, y, TEXT, false);
                y += 10;
            }
        }
    }

    /** The effect list over the rest: a search bar, every effect with its picture, its name and where it comes from. */
    private void picker(GuiGraphics g, int mx, int my) {
        int x = px(), y = py();
        g.pose().pushPose();
        g.pose().translate(0, 0, 400);
        g.fill(x0, y0, x0 + W, y0 + H, 0xB0000000);
        g.fill(x - 2, y - 2, x + PICK_W + 2, y + PICK_H + 2, GOLD_DIM);
        g.fill(x, y, x + PICK_W, y + PICK_H, 0xFF12171D);
        g.drawString(font, Component.translatable("arms.arcanes_oublies.pick_title"), x + 8, y + 8, GOLD, false);
        g.drawString(font, Component.translatable("arms.arcanes_oublies.pick_count", shown.size()), x + PICK_W - 70, y + 8, DIM, false);
        g.fill(x + 8, y + 25, x + PICK_W - 8, y + 43, LINE);
        g.fill(x + 9, y + 26, x + PICK_W - 9, y + 42, FIELD);
        g.drawString(font, "⌕", x + 13, y + 30, DIM, false);
        int ly = y + LIST_Y, n = visibleRows();
        for (int i = 0; i < n && pickScroll + i < shown.size(); i++) {
            Effects.Entry e = shown.get(pickScroll + i);
            int ry = ly + i * ROW_H;
            boolean hover = mx >= x + 8 && mx < x + PICK_W - 14 && my >= ry && my < ry + ROW_H;
            if (hover) {
                g.fill(x + 8, ry, x + PICK_W - 14, ry + ROW_H, 0xFF26323F);
            }
            Effects.icon(g, e.id(), x + 11, ry + 1, 16);
            g.drawString(font, font.plainSubstrByWidth(e.name().getString(), 170), x + 32, ry + 5, Effects.colour(e.id()), false);
            String mod = e.mod();
            g.drawString(font, mod, x + PICK_W - 18 - font.width(mod), ry + 5, DIM, false);
        }
        // The scroll bar
        if (shown.size() > n) {
            int track = n * ROW_H, bar = Math.max(12, track * n / shown.size());
            int by = ly + (track - bar) * pickScroll / Math.max(1, shown.size() - n);
            g.fill(x + PICK_W - 10, ly, x + PICK_W - 7, ly + track, LINE);
            g.fill(x + PICK_W - 10, by, x + PICK_W - 7, by + bar, GOLD);
        }
        g.pose().popPose();
        if (search != null) {
            g.pose().pushPose();
            g.pose().translate(0, 0, 410);
            search.render(g, mx, my, 0);
            g.pose().popPose();
        }
    }
}
