package com.waylays.arcanes.piege.client;

import com.waylays.arcanes.piege.Pieges;
import com.waylays.arcanes.piege.TrapKind;
import com.waylays.arcanes.piege.TrapSetup;
import com.waylays.arcanes.rp.arms.Proc;
import com.waylays.arcanes.rp.arms.client.Effects;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * /piege: what the trap in hand does to whoever it catches. Its damage (empty: the trap's own), and up to eight
 * effects, each with its chance, its level and how long it lasts: setting alight, freezing, lightning, throwing back,
 * and every harmful (or neutral) effect of the game and of the mods, chosen in a list one scrolls or searches; the
 * good ones too if one asks for them.
 */
public class TrapScreen extends Screen {
    private static final int W = 356, H = 262, ROW_H = 20, ROWS_Y = 92;
    private static final int BG = 0xF2141210, HEAD = 0xFF221D18, LINE = 0xFF3A3028, GOLD = 0xFFE8B86A, TEXT = 0xFFEEE6DA, DIM = 0xFF9A8E80;
    private static final int RED = 0xFFE0705A, GREEN = 0xFF8CD47A, FIELD = 0xFF0C0A08;

    private static final class Row {
        String type, chance = "25", power = "1", seconds = "5";

        Row(String type) {
            this.type = type;
        }
    }

    private final ItemStack stack;
    private final TrapKind kind;
    private String damage;
    private final List<Row> rows = new ArrayList<>();
    private int x0, y0;
    // The effect list, while one is chosen for row {@code picking} (rows.size(): a new row)
    private int picking = -1, pickScroll;
    private boolean everyEffect;
    private EditBox search;
    private List<Effects.Entry> shown = List.of();

    private TrapScreen(ItemStack stack) {
        super(Component.translatable("gui.arcanes_oublies.piege.title", stack.getHoverName()));
        this.stack = stack.copy();
        this.kind = Pieges.kindOf(stack);
        TrapSetup s = stack.getOrDefault(Pieges.SETUP.get(), TrapSetup.NONE);
        damage = s.damage() < 0 ? "" : Pieges.number(s.damage());
        for (Proc raw : s.effects()) {
            Proc p = raw.normalized();
            Row r = new Row(p.type());
            r.chance = Pieges.number(p.chance());
            r.power = String.valueOf(Math.max(1, p.power()));
            r.seconds = String.valueOf(Math.max(1, p.seconds()));
            rows.add(r);
        }
    }

    public static void open() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && Pieges.kindOf(mc.player.getMainHandItem()) != null) {
            mc.setScreen(new TrapScreen(mc.player.getMainHandItem()));
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    // ---------------------------------------------------------------- widgets

    private final class Flat extends Button {
        private final int accent;

        Flat(int x, int y, int w, int h, Component msg, OnPress press, int accent) {
            super(x, y, w, h, msg, press, DEFAULT_NARRATION);
            this.accent = accent;
        }

        @Override
        protected void renderWidget(GuiGraphics g, int mx, int my, float pt) {
            boolean hover = isHoveredOrFocused() && active;
            g.fill(getX(), getY(), getX() + width, getY() + height, hover ? accent : LINE);
            g.fillGradient(getX() + 1, getY() + 1, getX() + width - 1, getY() + height - 1, hover ? 0xFF3A3028 : 0xFF2A231C, hover ? 0xFF2C241C : 0xFF1C1712);
            g.drawCenteredString(font, getMessage(), getX() + width / 2, getY() + (height - 8) / 2, active ? (hover ? 0xFFFFFFFF : accent) : DIM);
        }
    }

    private Flat flat(int x, int y, int w, int h, Component msg, Button.OnPress press, int accent) {
        Flat f = new Flat(x, y, w, h, msg, press, accent);
        addRenderableWidget(f);
        return f;
    }

    private EditBox field(int x, int y, int w, String value, java.util.function.Consumer<String> to) {
        EditBox e = new EditBox(font, x + 3, y + 3, w - 6, 10, Component.empty());
        e.setBordered(false);
        e.setMaxLength(8);
        e.setValue(value);
        e.setTextColor(TEXT);
        e.setFilter(s -> s.matches("[0-9.,]*"));
        e.setResponder(to);
        addRenderableWidget(e);
        return e;
    }

    @Override
    protected void init() {
        x0 = Math.max(4, (width - W) / 2);
        y0 = Math.max(4, (height - H) / 2);
        clearWidgets();
        search = null;
        if (picking >= 0) {
            initPicker();
            return;
        }
        // Its damage
        EditBox dmg = field(x0 + 96, y0 + 46, 44, damage, s -> damage = s);
        dmg.setHint(Component.literal(Pieges.number(kind.damage)).withColor(DIM));
        // Its effects
        for (int i = 0; i < rows.size(); i++) {
            int k = i;
            Row r = rows.get(i);
            int ry = y0 + ROWS_Y + i * ROW_H;
            Flat pick = flat(x0 + 30, ry, 130, 17, Component.empty(), b -> {
                picking = k;
                pickScroll = 0;
                init();
            }, GOLD);
            pick.setTooltip(Tooltip.create(Effects.name(r.type).copy().append("\n").append(Component.literal(r.type).withColor(DIM))));
            field(x0 + 166, ry, 46, r.chance, s -> r.chance = s);
            field(x0 + 218, ry, 40, r.power, s -> r.power = s);
            field(x0 + 264, ry, 46, r.seconds, s -> r.seconds = s);
            flat(x0 + 316, ry, 18, 17, Component.literal("✕"), b -> {
                rows.remove(k);
                init();
            }, RED);
        }
        if (rows.size() < TrapSetup.MAX_EFFECTS) {
            flat(x0 + 12, y0 + ROWS_Y + rows.size() * ROW_H + 2, 150, 17, Component.translatable("gui.arcanes_oublies.piege.add"), b -> {
                picking = rows.size();
                pickScroll = 0;
                init();
            }, GREEN);
        }
        flat(x0 + W - 190, y0 + H - 26, 84, 18, Component.translatable("gui.arcanes_oublies.piege.cancel"), b -> onClose(), DIM);
        flat(x0 + W - 98, y0 + H - 26, 86, 18, Component.translatable("gui.arcanes_oublies.piege.save"), b -> save(), GOLD);
    }

    // ---------------------------------------------------------------- the effect list

    private static final int PICK_W = 316, PICK_H = 230, PICK_ROW = 18, LIST_Y = 50;

    private int px() {
        return x0 + (W - PICK_W) / 2;
    }

    private int py() {
        return y0 + (H - PICK_H) / 2;
    }

    private int visibleRows() {
        return (PICK_H - LIST_Y - 8) / PICK_ROW;
    }

    private void initPicker() {
        search = new EditBox(font, px() + 26, py() + 30, 180, 12, Component.empty());
        search.setBordered(false);
        search.setTextColor(TEXT);
        search.setHint(Component.translatable("gui.arcanes_oublies.piege.search").withColor(DIM));
        search.setResponder(s -> {
            filter(s);
            pickScroll = 0;
        });
        addRenderableWidget(search);
        setInitialFocus(search);
        flat(px() + PICK_W - 104, py() + 26, 96, 16, Component.translatable(everyEffect ? "gui.arcanes_oublies.piege.harmful_only"
                : "gui.arcanes_oublies.piege.every_effect"), b -> {
            everyEffect = !everyEffect;
            init();
        }, GOLD);
        filter("");
    }

    /** The effects a trap can give: its own actions, then the harmful and neutral effects (all of them if asked). */
    private List<Effects.Entry> choices() {
        List<Effects.Entry> out = new ArrayList<>();
        for (Effects.Entry e : Effects.all()) {
            if (e.id().startsWith("*")) {
                if (Pieges.SPECIALS.contains(e.id())) {
                    out.add(e);
                }
                continue;
            }
            ResourceLocation rl = ResourceLocation.tryParse(e.id());
            MobEffect effect = rl == null ? null : BuiltInRegistries.MOB_EFFECT.get(rl);
            if (effect != null && (everyEffect || effect.getCategory() != MobEffectCategory.BENEFICIAL)) {
                out.add(e);
            }
        }
        return out;
    }

    private void filter(String s) {
        String q = s.toLowerCase(Locale.ROOT).trim();
        shown = new ArrayList<>();
        for (Effects.Entry e : choices()) {
            if (q.isEmpty() || e.name().getString().toLowerCase(Locale.ROOT).contains(q) || e.id().toLowerCase(Locale.ROOT).contains(q)) {
                shown.add(e);
            }
        }
    }

    private void choose(String id) {
        if (picking >= rows.size()) {
            Row r = new Row(id);
            if (id.equals("*feu") || id.equals("*foudre") || id.equals("*recul")) {
                r.seconds = "4";
            }
            rows.add(r);
        } else {
            rows.get(picking).type = id;
        }
        picking = -1;
        init();
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (picking >= 0) {
            int lx = px() + 8, ly = py() + LIST_Y;
            if (mx >= lx && mx < lx + PICK_W - 16 && my >= ly && my < ly + visibleRows() * PICK_ROW) {
                int i = pickScroll + (int) ((my - ly) / PICK_ROW);
                if (i < shown.size()) {
                    choose(shown.get(i).id());
                }
                return true;
            }
            if (mx < px() || mx > px() + PICK_W || my < py() || my > py() + PICK_H) {
                picking = -1;
                init();
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
        if (picking >= 0 && key == GLFW.GLFW_KEY_ESCAPE) {
            picking = -1;
            init();
            return true;
        }
        if (picking >= 0 && (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) && !shown.isEmpty()) {
            choose(shown.get(0).id());
            return true;
        }
        return super.keyPressed(key, scan, mods);
    }

    // ---------------------------------------------------------------- saving

    private static float num(String s, float fallback) {
        try {
            return Float.parseFloat(s.trim().replace(',', '.'));
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private void save() {
        float d = damage.isBlank() ? -1 : Math.max(0, num(damage, -1));
        List<Proc> effects = new ArrayList<>();
        for (Row r : rows) {
            float chance = Mth.clamp(num(r.chance, 25), 0, 100);
            if (chance > 0) {
                effects.add(new Proc(r.type, chance, (int) Mth.clamp(num(r.power, 1), 1, 255), (int) Mth.clamp(num(r.seconds, 5), 1, 3600), false, false));
            }
        }
        PacketDistributor.sendToServer(new Pieges.Edit(new TrapSetup(d, effects)));
        onClose();
    }

    // ---------------------------------------------------------------- drawing

    @Override
    public void renderBackground(GuiGraphics g, int mx, int my, float pt) {
        g.fillGradient(0, 0, width, height, 0x90000000, 0xB8000000);
    }

    private void panel(GuiGraphics g, int x, int y, int w, int h) {
        g.fill(x - 1, y - 1, x + w + 1, y + h + 1, 0xFF5A4630);
        g.fill(x, y, x + w, y + h, BG);
        g.fillGradient(x, y, x + w, y + 22, HEAD, BG);
        g.fill(x, y + 22, x + w, y + 23, LINE);
    }

    private void well(GuiGraphics g, int x, int y, int w) {
        g.fill(x, y, x + w, y + 16, LINE);
        g.fill(x + 1, y + 1, x + w - 1, y + 15, FIELD);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        renderBackground(g, mx, my, pt);
        panel(g, x0, y0, W, H);
        g.pose().pushPose();
        g.pose().translate(x0 + 8, y0 + 3, 0);
        g.renderItem(stack, 0, 0);
        g.pose().popPose();
        g.drawString(font, title, x0 + 28, y0 + 7, GOLD, false);
        g.drawString(font, Component.translatable("gui.arcanes_oublies.piege.subtitle"), x0 + 12, y0 + 30, DIM, false);
        // Damage
        g.drawString(font, Component.translatable("gui.arcanes_oublies.piege.damage"), x0 + 12, y0 + 50, TEXT, false);
        well(g, x0 + 96, y0 + 46, 44);
        g.drawString(font, Component.translatable("gui.arcanes_oublies.piege.damage_hint", Pieges.number(kind.damage)), x0 + 148, y0 + 50, DIM, false);
        // Effects
        g.fill(x0 + 8, y0 + 68, x0 + W - 8, y0 + 69, LINE);
        int hy = y0 + 76;
        g.drawString(font, Component.translatable("gui.arcanes_oublies.piege.col.effect"), x0 + 30, hy, DIM, false);
        g.drawString(font, Component.translatable("gui.arcanes_oublies.piege.col.chance"), x0 + 166, hy, DIM, false);
        g.drawString(font, Component.translatable("gui.arcanes_oublies.piege.col.level"), x0 + 218, hy, DIM, false);
        g.drawString(font, Component.translatable("gui.arcanes_oublies.piege.col.seconds"), x0 + 264, hy, DIM, false);
        if (picking < 0) {
            for (int i = 0; i < rows.size(); i++) {
                int ry = y0 + ROWS_Y + i * ROW_H;
                Row r = rows.get(i);
                well(g, x0 + 166, ry, 46);
                well(g, x0 + 218, ry, 40);
                well(g, x0 + 264, ry, 46);
                Effects.icon(g, r.type, x0 + 11, ry, 16);
            }
            if (rows.isEmpty()) {
                g.drawString(font, Component.translatable("gui.arcanes_oublies.piege.none"), x0 + 12, y0 + ROWS_Y + 26, DIM, false);
            }
        }
        super.render(g, mx, my, pt);
        if (picking < 0) {
            // The effects' names over their buttons, in their colour
            for (int i = 0; i < rows.size(); i++) {
                int ry = y0 + ROWS_Y + i * ROW_H;
                Row r = rows.get(i);
                String name = font.plainSubstrByWidth(Effects.name(r.type).getString(), 122);
                g.drawString(font, name, x0 + 34, ry + 5, Effects.colour(r.type), false);
            }
            return;
        }
        // The effect list over everything
        g.pose().pushPose();
        g.pose().translate(0, 0, 400);
        panel(g, px(), py(), PICK_W, PICK_H);
        g.drawString(font, Component.translatable("gui.arcanes_oublies.piege.pick"), px() + 10, py() + 7, GOLD, false);
        g.fill(px() + 8, py() + 26, px() + 210, py() + 44, LINE);
        g.fill(px() + 9, py() + 27, px() + 209, py() + 43, FIELD);
        int lx = px() + 8, ly = py() + LIST_Y;
        for (int i = 0; i < visibleRows() && pickScroll + i < shown.size(); i++) {
            Effects.Entry e = shown.get(pickScroll + i);
            int ry = ly + i * PICK_ROW;
            boolean over = mx >= lx && mx < lx + PICK_W - 16 && my >= ry && my < ry + PICK_ROW;
            if (over) {
                g.fill(lx, ry, lx + PICK_W - 16, ry + PICK_ROW, 0xFF2E261E);
            }
            Effects.icon(g, e.id(), lx + 2, ry + 1, 16);
            g.drawString(font, e.name(), lx + 22, ry + 5, Effects.colour(e.id()), false);
            String mod = e.id().startsWith("*") ? "" : e.mod();
            g.drawString(font, mod, lx + PICK_W - 20 - font.width(mod), ry + 5, DIM, false);
        }
        if (shown.size() > visibleRows()) {
            int track = visibleRows() * PICK_ROW;
            int bar = Math.max(12, track * visibleRows() / shown.size());
            int at = (int) ((track - bar) * (pickScroll / (float) Math.max(1, shown.size() - visibleRows())));
            g.fill(px() + PICK_W - 7, ly + at, px() + PICK_W - 4, ly + at + bar, GOLD);
        }
        g.pose().popPose();
        // The search field and the toggle drawn on top of the list's panel
        g.pose().pushPose();
        g.pose().translate(0, 0, 450);
        if (search != null) {
            search.render(g, mx, my, pt);
        }
        for (var w : children()) {
            if (w instanceof Flat f) {
                f.render(g, mx, my, pt);
            }
        }
        g.pose().popPose();
    }
}
