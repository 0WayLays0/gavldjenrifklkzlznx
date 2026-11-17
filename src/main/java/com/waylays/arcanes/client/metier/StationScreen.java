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

import java.util.List;

/**
 * A station's recipes (the crafting table, the smithing table, the still), as cards over the station's scene: what
 * each makes, what it takes (green: in the bag, red: missing), what a good and a perfect job give, how hard it is.
 * Choosing one starts its minigame. The cards are compact, so that up to six show at once.
 */
public class StationScreen extends Screen {
    private static final int W = 330, CARD = 38, GAP = 4, LIST_Y = 52;
    private static final int[] THEMES = {0xFF8A7458, 0xFF5A8A48, 0xFF8A6438, 0xFFB0502A, 0xFF7A4AA8};
    private static final String[] SCENES = {"bg_mining", "bg_harvest", "bg_assembly", "bg_forge", "bg_distill"};
    private static final ResourceLocation SLOT = Draw.tex("slot");

    private final MetierNet.Station station;
    private final int theme;
    private final ResourceLocation scene;
    private float left, top, k = 1;
    private int h, scroll, visible;
    private int hovered = -1;
    private long opened = net.minecraft.Util.getMillis();

    public StationScreen(MetierNet.Station station) {
        super(Component.literal(station.title()));
        this.station = station;
        int game = Mth.clamp(station.game(), 0, THEMES.length - 1);
        this.theme = THEMES[game];
        this.scene = Draw.tex(SCENES[game]);
    }

    @Override
    protected void init() {
        int n = Math.max(1, station.recipes().size());
        visible = Math.max(1, Math.min(n, 6));
        h = LIST_Y + visible * (CARD + GAP) + 10;
        k = Mth.clamp(Math.min((width - 16) / (float) W, (height - 12) / (float) h), .5f, 2.6f);
        left = (width - W * k) / 2;
        top = (height - h * k) / 2;
        scroll = Mth.clamp(scroll, 0, Math.max(0, station.recipes().size() - visible));
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
        ItemStack tip = ItemStack.EMPTY;
        List<MetierNet.RecipeView> recipes = station.recipes();
        if (recipes.isEmpty()) {
            Draw.centered(g, Component.translatable("metier.arcanes_oublies.station.empty"), W / 2f, LIST_Y + 20, 0xFFA8A090, 1);
        }
        for (int i = 0; i < visible && i + scroll < recipes.size(); i++) {
            int index = i + scroll;
            MetierNet.RecipeView r = recipes.get(index);
            float y = LIST_Y + i * (CARD + GAP);
            boolean over = mx >= 10 && mx < W - 10 && my >= y && my < y + CARD;
            boolean ready = r.ready() && station.cooldown() <= 0;
            if (over) {
                hovered = index;
            }
            float lift = over ? -1.5f : 0;
            Draw.shadow(g, W / 2f, y + CARD / 2f + 3, W - 10, CARD + 12, over ? 150 : 90);
            Draw.vgrad(g, 10, y + lift, W - 10, y + CARD + lift, over && ready ? Draw.lerp(theme, 0xFF2A2420, .55f) : 0xF0201A16,
                    over && ready ? Draw.lerp(theme, 0xFF100C0A, .8f) : 0xF0120E0C);
            int edge = over ? (ready ? Draw.lerp(theme, 0xFFFFF0D0, .6f) : 0xFFB04A3A) : Draw.argb(120, Draw.lerp(theme, 0xFFFFFFFF, .3f));
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
                Component c = Component.translatable("metier.arcanes_oublies.station.wait", seconds(r.cooldown()));
                Draw.text(g, c, W - 16 - Draw.width(c, .7f), y + 4 + lift, 0xFFFFC060, .7f);
            }
        }
        if (scroll > 0) {
            Draw.centered(g, Component.literal("▲"), W / 2f, LIST_Y - 9, 0xFFFFFFFF, .8f);
        }
        if (scroll + visible < recipes.size()) {
            Draw.centered(g, Component.literal("▼"), W / 2f, LIST_Y + visible * (CARD + GAP) - 4, 0xFFFFFFFF, .8f);
        }
        g.pose().popPose();
        if (!tip.isEmpty()) {
            g.renderTooltip(font, tip, mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
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
        scroll = Mth.clamp(scroll - (int) Math.signum(scrollY), 0, Math.max(0, station.recipes().size() - visible));
        return true;
    }

    /** The game shown for this station (for the smoke test). */
    public MetierDefs.Game game() {
        return MetierDefs.Game.values()[Mth.clamp(station.game(), 0, MetierDefs.Game.values().length - 1)];
    }
}
