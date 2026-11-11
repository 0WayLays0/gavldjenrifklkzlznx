package com.waylays.eraphones.pack.client;

import com.waylays.eraphones.EraPhones;
import com.waylays.eraphones.pack.BackpackMenu;
import com.waylays.eraphones.pack.PackRegistry;
import com.waylays.eraphones.pack.Packs;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * A backpack open: a panel of stitched canvas, the bag's own colour across its top with its name, its ten pockets,
 * and what the player has in hand below.
 */
public class BackpackScreen extends AbstractContainerScreen<BackpackMenu> {
    private static final ResourceLocation PANEL = EraPhones.id("textures/gui/pack/sac.png");

    public BackpackScreen(BackpackMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        imageWidth = 176;
        imageHeight = 140;
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        super.render(g, mx, my, pt);
        renderTooltip(g, mx, my);
    }

    @Override
    protected void renderBg(GuiGraphics g, float pt, int mx, int my) {
        int x = leftPos, y = topPos;
        g.blit(PANEL, x, y, imageWidth, imageHeight, 0, 0, imageWidth * 2, imageHeight * 2, imageWidth * 2, imageHeight * 2);
        // The bag's colour along the top flap
        int c = PackRegistry.COLORS[Math.max(0, Math.min(PackRegistry.COLORS.length - 1, menu.style()))];
        g.fill(x + 7, y + 6, x + imageWidth - 7, y + 20, c);
        g.fillGradient(x + 7, y + 6, x + imageWidth - 7, y + 20, 0x30FFFFFF, 0x40000000);
        for (int k = x + 10; k < x + imageWidth - 10; k += 4) {
            g.fill(k, y + 8, k + 2, y + 9, 0x50FFFFFF);
            g.fill(k, y + 17, k + 2, y + 18, 0x50FFFFFF);
        }
        // The wells: the pockets, the hands
        for (int i = 0; i < Packs.SIZE; i++) {
            well(g, x + 43 + (i % 5) * 18, y + BackpackMenu.POCKETS_Y - 1 + (i / 5) * 18);
        }
        int x0 = (176 - menu.hands() * 18) / 2;
        for (int j = 0; j < menu.hands(); j++) {
            well(g, x + x0 + j * 18, y + BackpackMenu.HANDS_Y - 1);
        }
    }

    private static void well(GuiGraphics g, int x, int y) {
        g.fill(x, y, x + 18, y + 18, 0xFF3A2C1E);
        g.fill(x + 1, y + 1, x + 18, y + 18, 0xFFC9B48E);
        g.fill(x + 1, y + 1, x + 17, y + 17, 0xFF6E5A40);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mx, int my) {
        g.drawString(font, title, (imageWidth - font.width(title)) / 2, 9, 0xFFFFF6E0, true);
        Component hands = Component.translatable("pack.eraphones.in_hand");
        g.drawString(font, hands, (imageWidth - font.width(hands)) / 2, BackpackMenu.HANDS_Y - 12, 0xFF4A3A26, false);
    }
}
