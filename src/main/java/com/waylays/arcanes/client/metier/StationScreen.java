package com.waylays.arcanes.client.metier;

import com.waylays.arcanes.metier.MetierDefs;
import com.waylays.arcanes.metier.MetierNet;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

/**
 * A station's recipes (the crafting table, the smithing table, the still): what each takes, whether the player has
 * it, what a good and a perfect job give, how hard it is and how long before it can be made again. Choosing one starts
 * its minigame; the station's own screen can still be opened when the trade allows it.
 */
public class StationScreen extends Screen {
    private static final int W = 300, CARD = 46, LIST_Y = 34;
    private static final int[] THEMES = {0xFF7A6A58, 0xFF4E8A44, 0xFF3A6EA8, 0xFFA8442A, 0xFF6A4A9A};

    private final MetierNet.Station station;
    private final int theme;
    private int left, top, h, scroll, visible;
    private int hovered = -1;
    private boolean hoverVanilla;
    private final List<ItemStack> tooltipStacks = new ArrayList<>();

    public StationScreen(MetierNet.Station station) {
        super(Component.literal(station.title()));
        this.station = station;
        this.theme = THEMES[Mth.clamp(station.game(), 0, THEMES.length - 1)];
    }

    @Override
    protected void init() {
        visible = Math.max(1, Math.min(station.recipes().size(), (height - 90) / (CARD + 4)));
        h = LIST_Y + visible * (CARD + 4) + (station.vanilla() ? 30 : 10);
        left = (width - W) / 2;
        top = (height - h) / 2;
        scroll = Mth.clamp(scroll, 0, Math.max(0, station.recipes().size() - visible));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        g.pose().pushPose();
        g.pose().translate(left, top, 0);
        int mx = mouseX - left, my = mouseY - top;
        g.fill(-3, -2, W + 4, h + 5, 0x70000000);
        g.fillGradient(0, 0, W, h, Draw.argb(250, Draw.lerp(theme, 0xFF000000, .6f)), Draw.argb(250, Draw.lerp(theme, 0xFF000000, .85f)));
        g.renderOutline(0, 0, W, h, Draw.lerp(theme, 0xFFFFFFFF, .3f));
        g.fillGradient(2, 2, W - 2, 28, Draw.argb(200, theme), Draw.argb(30, theme));
        Draw.item(g, station.icon(), 8, 6, 1.1f);
        Draw.text(g, title, 30, 6, 0xFFFFF4E0, 1.1f);
        Draw.text(g, Component.translatable("metier.arcanes_oublies.station.choose"), 30, 18, 0xFFD0C8B8, .75f);
        if (station.cooldown() > 0) {
            Component c = Component.translatable("metier.arcanes_oublies.station.wait", seconds(station.cooldown()));
            Draw.text(g, c, W - 8 - font.width(c) * .75f, 12, 0xFFFFC060, .75f);
        }
        hovered = -1;
        tooltipStacks.clear();
        ItemStack tip = ItemStack.EMPTY;
        List<MetierNet.RecipeView> recipes = station.recipes();
        if (recipes.isEmpty()) {
            Draw.centered(g, Component.translatable("metier.arcanes_oublies.station.empty"), W / 2f, LIST_Y + 14, 0xFFA09888, 1);
        }
        for (int i = 0; i < visible && i + scroll < recipes.size(); i++) {
            int index = i + scroll;
            MetierNet.RecipeView r = recipes.get(index);
            int y = LIST_Y + i * (CARD + 4);
            boolean over = mx >= 8 && mx < W - 8 && my >= y && my < y + CARD;
            boolean ready = r.ready() && station.cooldown() <= 0;
            if (over) {
                hovered = index;
            }
            int fill = over ? (ready ? Draw.argb(170, Draw.lerp(theme, 0xFFFFFFFF, .15f)) : 0xA0402020) : 0x90000000;
            Draw.frame(g, 8, y, W - 8, y + CARD, fill, over ? Draw.lerp(theme, 0xFFFFFFFF, .6f) : Draw.argb(160, theme));
            Draw.item(g, r.perfect().isEmpty() ? r.partial() : r.perfect(), 13, y + 7, 2);
            Draw.text(g, Component.literal(r.name()), 52, y + 4, ready ? 0xFFFFF4E0 : 0xFFC8B8A8, 1);
            int stars = (int) Mth.clamp(Math.round(r.difficulty() * 2), 1, 8);
            Component diff = Component.literal("★".repeat(stars)).withStyle(r.difficulty() > 1.25 ? ChatFormatting.RED : r.difficulty() < .9
                    ? ChatFormatting.GREEN : ChatFormatting.GOLD);
            Draw.text(g, diff, W - 14 - font.width(diff) * .75f, y + 5, 0xFFFFFFFF, .75f);
            // What it takes
            int x = 52;
            Draw.text(g, Component.translatable("metier.arcanes_oublies.station.takes"), x, y + 17, 0xFFA09888, .65f);
            x += 4;
            for (int k = 0; k < r.inputs().size(); k++) {
                ItemStack in = r.inputs().get(k);
                boolean have = k < r.have().size() && r.have().get(k);
                int ix = x + k * 20, iy = y + 26;
                g.fill(ix - 1, iy - 1, ix + 17, iy + 17, have ? 0x4040A040 : 0x60C03030);
                g.renderItem(in, ix, iy);
                g.renderItemDecorations(font, in, ix, iy);
                if (mx >= ix && mx < ix + 16 && my >= iy && my < iy + 16) {
                    tip = in;
                }
            }
            // What it gives
            int gx = W - 92;
            Draw.text(g, Component.translatable("metier.arcanes_oublies.station.gives"), gx, y + 17, 0xFFA09888, .65f);
            ItemStack[] outs = {r.partial(), r.perfect()};
            int[] colors = {0xFFB8E0FF, 0xFFFFD25A};
            for (int k = 0; k < 2; k++) {
                ItemStack out = outs[k];
                int ix = gx + 4 + k * 36, iy = y + 26;
                if (out.isEmpty()) {
                    continue;
                }
                g.renderOutline(ix - 2, iy - 2, 20, 20, colors[k]);
                g.renderItem(out, ix, iy);
                g.renderItemDecorations(font, out, ix, iy);
                if (mx >= ix && mx < ix + 16 && my >= iy && my < iy + 16) {
                    tip = out;
                }
            }
            if (r.cooldown() > 0) {
                Component c = Component.translatable("metier.arcanes_oublies.station.wait", seconds(r.cooldown()));
                Draw.text(g, c, W - 14 - font.width(c) * .7f, y + CARD - 9, 0xFFFFC060, .7f);
            } else if (r.have().contains(false)) {
                Component c = Component.translatable("metier.arcanes_oublies.station.lacking");
                Draw.text(g, c, W - 14 - font.width(c) * .7f, y + CARD - 9, 0xFFFF8070, .7f);
            }
        }
        if (scroll > 0) {
            Draw.centered(g, Component.literal("▲"), W / 2f, LIST_Y - 6, 0xFFFFFFFF, .7f);
        }
        if (scroll + visible < recipes.size()) {
            Draw.centered(g, Component.literal("▼"), W / 2f, LIST_Y + visible * (CARD + 4) - 3, 0xFFFFFFFF, .7f);
        }
        hoverVanilla = false;
        if (station.vanilla()) {
            int by = h - 24;
            hoverVanilla = mx >= W / 2 - 70 && mx < W / 2 + 70 && my >= by && my < by + 16;
            Draw.frame(g, W / 2 - 70, by, W / 2 + 70, by + 16, hoverVanilla ? 0xC0403830 : 0x90000000, hoverVanilla ? 0xFFFFE6A0 : 0xFF8A8070);
            Draw.centered(g, Component.translatable("metier.arcanes_oublies.station.vanilla"), W / 2f, by + 4, 0xFFF0EAD8, .85f);
        }
        g.pose().popPose();
        if (!tip.isEmpty()) {
            g.renderTooltip(font, tip, mouseX, mouseY);
        }
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
        if (button == 0 && hoverVanilla) {
            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK.value(), 1, .6f));
            PacketDistributor.sendToServer(new MetierNet.Pick(station.def(), station.pos(), -1));
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
