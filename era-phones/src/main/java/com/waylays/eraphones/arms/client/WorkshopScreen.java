package com.waylays.eraphones.arms.client;

import com.waylays.eraphones.arms.Arms;
import com.waylays.eraphones.arms.Proc;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemLore;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

/**
 * The weapons' workshop: the item in hand shown big, and three tabs. General: its name, four lines of lore ("&c" and
 * the like for colours), unbreakable, shining. Stats: attack damage and speed, armour, toughness, knockback
 * resistance, speed (percent), health, reach. Effects: up to six chance effects, each its kind (click to change it),
 * its chance in percent, its strength and its seconds.
 */
public class WorkshopScreen extends Screen {
    private static final int W = 330, H = 236, MAX_PROCS = 6;
    private static final int BG = 0xF0141A22, PANEL = 0xFF1E2833, EDGE = 0xFF3C5068, GOLD = 0xFFE8C677, TEXT = 0xFFE6ECF2, DIM = 0xFF8A98A8;
    private static final String[] STAT_KEYS = {"damage", "attack_speed", "armor", "toughness", "knockback", "speed", "health", "reach"};

    private final ItemStack stack;
    private int tab;
    private String name;
    private final String[] lore = new String[4];
    private boolean unbreakable, glint;
    private final String[] stats = new String[Arms.STATS.size()];
    private final List<String[]> procs = new ArrayList<>();
    private int x0, y0;

    public WorkshopScreen(ItemStack stack) {
        super(Component.translatable("arms.eraphones.title"));
        this.stack = stack.copy();
        Component custom = stack.get(DataComponents.CUSTOM_NAME);
        name = custom == null ? "" : custom.getString();
        ItemLore l = stack.get(DataComponents.LORE);
        for (int i = 0; i < lore.length; i++) {
            lore[i] = l != null && i < l.lines().size() ? l.lines().get(i).getString() : "";
        }
        unbreakable = stack.has(DataComponents.UNBREAKABLE);
        glint = Boolean.TRUE.equals(stack.get(DataComponents.ENCHANTMENT_GLINT_OVERRIDE));
        double[] v = Arms.stats(stack);
        for (int i = 0; i < stats.length; i++) {
            stats[i] = Double.isNaN(v[i]) ? "" : trim(v[i]);
        }
        List<Proc> list = stack.get(Arms.PROCS.get());
        if (list != null) {
            for (Proc p : list) {
                procs.add(new String[] {p.type(), trim(p.chance()), String.valueOf(p.power()), String.valueOf(p.seconds())});
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
        return d == Math.rint(d) ? String.valueOf((long) d) : String.format(java.util.Locale.ROOT, "%.2f", d);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    protected void init() {
        x0 = (width - W) / 2;
        y0 = (height - H) / 2;
        clearWidgets();
        String[] tabs = {"general", "stats", "effects"};
        for (int i = 0; i < tabs.length; i++) {
            int t = i;
            Button b = Button.builder(Component.translatable("arms.eraphones.tab." + tabs[i]), x -> {
                tab = t;
                init();
            }).bounds(x0 + 64 + i * 88, y0 + 26, 84, 16).build();
            b.active = tab != i;
            addRenderableWidget(b);
        }
        switch (tab) {
            case 0 -> general();
            case 1 -> statsTab();
            default -> effectsTab();
        }
        addRenderableWidget(Button.builder(Component.translatable("arms.eraphones.save"), b -> save()).bounds(x0 + W - 172, y0 + H - 24, 80, 18).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.cancel"), b -> onClose()).bounds(x0 + W - 88, y0 + H - 24, 80, 18).build());
    }

    private EditBox box(int x, int y, int w, String value, int max, java.util.function.Consumer<String> to) {
        EditBox e = new EditBox(font, x, y, w, 16, Component.empty());
        e.setMaxLength(max);
        e.setValue(value);
        e.setResponder(to);
        addRenderableWidget(e);
        return e;
    }

    private void general() {
        int x = x0 + 16, y = y0 + 58;
        box(x + 70, y, W - 102, name, 100, s -> name = s);
        for (int i = 0; i < lore.length; i++) {
            int k = i;
            box(x + 70, y + 26 + i * 20, W - 102, lore[i], 120, s -> lore[k] = s);
        }
        addRenderableWidget(Button.builder(toggle("unbreakable", unbreakable), b -> {
            unbreakable = !unbreakable;
            b.setMessage(toggle("unbreakable", unbreakable));
        }).bounds(x, y + 112, 140, 18).build());
        addRenderableWidget(Button.builder(toggle("glint", glint), b -> {
            glint = !glint;
            b.setMessage(toggle("glint", glint));
        }).bounds(x + 150, y + 112, 140, 18).build());
    }

    private static Component toggle(String key, boolean on) {
        return Component.translatable("arms.eraphones." + key).append(": ").append(Component.translatable(on ? "options.on" : "options.off"));
    }

    private void statsTab() {
        for (int i = 0; i < stats.length; i++) {
            int k = i;
            int col = i / 4, row = i % 4;
            box(x0 + 16 + col * 156 + 96, y0 + 58 + row * 26, 48, stats[i], 10, s -> stats[k] = s);
        }
    }

    private void effectsTab() {
        int y = y0 + 64;
        for (int i = 0; i < procs.size(); i++) {
            String[] p = procs.get(i);
            int k = i;
            int ry = y + i * 22;
            addRenderableWidget(Button.builder(kindName(p[0]), b -> {
                p[0] = next(p[0]);
                b.setMessage(kindName(p[0]));
            }).bounds(x0 + 12, ry, 118, 18).build());
            box(x0 + 136, ry + 1, 38, p[1], 6, s -> p[1] = s);
            box(x0 + 184, ry + 1, 30, p[2], 3, s -> p[2] = s);
            box(x0 + 224, ry + 1, 34, p[3], 4, s -> p[3] = s);
            addRenderableWidget(Button.builder(Component.literal("✕"), b -> {
                procs.remove(k);
                init();
            }).bounds(x0 + 266, ry, 18, 18).build());
        }
        if (procs.size() < MAX_PROCS) {
            addRenderableWidget(Button.builder(Component.translatable("arms.eraphones.add"), b -> {
                procs.add(new String[] {"feu", "5", "1", "4"});
                init();
            }).bounds(x0 + 12, y + procs.size() * 22, 118, 18).build());
        }
    }

    private static Component kindName(String id) {
        Proc.Kind k = Proc.Kind.of(id);
        return Component.translatable("arms.eraphones.proc." + id).withColor(k != null && k.strike ? 0xFFFF9A6A : 0xFF8ACBFF);
    }

    private static String next(String id) {
        Proc.Kind[] all = Proc.Kind.values();
        Proc.Kind k = Proc.Kind.of(id);
        return all[k == null ? 0 : (k.ordinal() + 1) % all.length].id;
    }

    private static float num(String s, float fallback) {
        try {
            return Float.parseFloat(s.trim().replace(',', '.'));
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private void save() {
        List<Float> st = new ArrayList<>();
        for (String s : stats) {
            st.add(s.isBlank() ? Float.NaN : num(s, Float.NaN));
        }
        List<Proc> list = new ArrayList<>();
        for (String[] p : procs) {
            list.add(new Proc(p[0], num(p[1], 0), (int) num(p[2], 1), (int) num(p[3], 0)));
        }
        PacketDistributor.sendToServer(new Arms.Edit(name, List.of(lore), unbreakable, glint, st, list));
        onClose();
    }

    @Override
    public void renderBackground(GuiGraphics g, int mx, int my, float pt) {
        g.fill(0, 0, width, height, 0x90000000);
        g.fill(x0 - 1, y0 - 1, x0 + W + 1, y0 + H + 1, EDGE);
        g.fill(x0, y0, x0 + W, y0 + H, BG);
        g.fillGradient(x0, y0, x0 + W, y0 + 22, 0xFF2A3A4C, 0xFF18222E);
        g.fill(x0 + 8, y0 + 48, x0 + W - 8, y0 + H - 30, PANEL);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        super.render(g, mx, my, pt);
        // The item, big, beside the title
        g.pose().pushPose();
        g.pose().translate(x0 + 12, y0 + 2, 0);
        g.pose().scale(2.4f, 2.4f, 1);
        g.renderItem(stack, 0, 0);
        g.pose().popPose();
        g.drawString(font, title, x0 + 64, y0 + 7, GOLD, true);
        switch (tab) {
            case 0 -> {
                g.drawString(font, Component.translatable("arms.eraphones.name"), x0 + 16, y0 + 62, TEXT, false);
                g.drawString(font, Component.translatable("arms.eraphones.lore"), x0 + 16, y0 + 88, TEXT, false);
                g.drawString(font, Component.translatable("arms.eraphones.colours"), x0 + 16, y0 + H - 46, DIM, false);
            }
            case 1 -> {
                for (int i = 0; i < STAT_KEYS.length; i++) {
                    int col = i / 4, row = i % 4;
                    g.drawString(font, Component.translatable("arms.eraphones.stat." + STAT_KEYS[i]), x0 + 16 + col * 156, y0 + 62 + row * 26, TEXT, false);
                }
                g.drawString(font, Component.translatable("arms.eraphones.stats_help"), x0 + 16, y0 + H - 46, DIM, false);
            }
            default -> {
                g.drawString(font, Component.translatable("arms.eraphones.col.kind"), x0 + 14, y0 + 52, DIM, false);
                g.drawString(font, "%", x0 + 150, y0 + 52, DIM, false);
                g.drawString(font, Component.translatable("arms.eraphones.col.power"), x0 + 182, y0 + 52, DIM, false);
                g.drawString(font, Component.translatable("arms.eraphones.col.seconds"), x0 + 224, y0 + 52, DIM, false);
                g.drawString(font, Component.translatable("arms.eraphones.effects_help"), x0 + 16, y0 + H - 46, DIM, false);
            }
        }
    }
}
