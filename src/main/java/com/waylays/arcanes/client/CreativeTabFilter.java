package com.waylays.arcanes.client;

import com.waylays.arcanes.ArcanesOublies;
import com.waylays.arcanes.registry.ModCreativeTabs;
import com.waylays.arcanes.registry.ModSpells;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.registries.ItemRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ContainerScreenEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;

/**
 * The addon's creative tab, dressed up. Beside the inventory stands a bar of schools in a gold frame (every school
 * first, then one entry per school, each with its spellbook or a scroll, its name and how many spells it has when
 * there is room for names); it scrolls with the mouse wheel, its arrows or its scroll bar, and choosing a school shows
 * only that school's spellbooks and scrolls. The tab has its own night-blue background, and takes on the chosen
 * school's colour: its name as the title, a wash of its colour over the title band and the slots, motes of it drifting
 * up behind the items.
 */
@EventBusSubscriber(modid = ArcanesOublies.MODID, value = Dist.CLIENT)
public final class CreativeTabFilter {
    private static final ResourceLocation PANEL = ArcanesOublies.id("creative/panel");
    private static final ResourceLocation ENTRY = ArcanesOublies.id("creative/entry");
    private static final ResourceLocation ENTRY_HOVER = ArcanesOublies.id("creative/entry_hover");
    private static final ResourceLocation ENTRY_SELECTED = ArcanesOublies.id("creative/entry_selected");
    private static final ResourceLocation ARROW_UP = ArcanesOublies.id("creative/arrow_up");
    private static final ResourceLocation ARROW_UP_LIT = ArcanesOublies.id("creative/arrow_up_lit");
    private static final ResourceLocation ARROW_DOWN = ArcanesOublies.id("creative/arrow_down");
    private static final ResourceLocation ARROW_DOWN_LIT = ArcanesOublies.id("creative/arrow_down_lit");
    /** The tab's background, drawn again under the title when it changes. */
    private static final ResourceLocation BACKGROUND = ArcanesOublies.id("textures/gui/creative/tab.png");
    private static final int GOLD = 0xFFD77A;
    private static final int LAVENDER = 0xD8D0F0;
    private static final int DIM = 0x8C82B4;

    private static final int ENTRY_SIZE = 22;
    private static final int ROW = 24;
    private static final int BORDER = 6;
    private static final int HEADER = 14;
    private static final int ARROW_ROW = 9;
    private static final int TRACK = 4;

    /** The school shown (null: all of them), kept while the game runs. */
    @Nullable
    private static ModCreativeTabs.Section shown;
    /** The tab's items as last filtered here: anything else means the game filled the tab again. */
    private static List<ItemStack> filtered = List.of();
    /** The bar's entries: null (every school), then the schools. */
    private static final List<ModCreativeTabs.Section> entries = new ArrayList<>();

    // The bar's layout, in screen coordinates, set each time the screen is laid out
    private static int barX, barY, barW, barH, listX, listY, rows, trackX;
    private static boolean wide;
    /** The first entry shown. */
    private static int scroll;
    private static boolean dragging;
    private static double dragFrom;

    private CreativeTabFilter() {}

    private static boolean onTab(Object screen) {
        return screen instanceof CreativeModeInventoryScreen && CreativeModeInventoryScreen.selectedTab == ModCreativeTabs.MAIN.get();
    }

    @SubscribeEvent
    public static void onInit(ScreenEvent.Init.Post event) {
        if (!(event.getScreen() instanceof CreativeModeInventoryScreen screen)) {
            return;
        }
        entries.clear();
        entries.add(null);
        entries.addAll(ModCreativeTabs.SECTIONS);
        int left = screen.getGuiLeft();
        // Names beside the icons when there is room for them
        int room = Math.min(132, left - 5);
        wide = room >= 96;
        barW = wide ? room : BORDER * 2 + ENTRY_SIZE + TRACK + 3;
        barX = Math.max(2, left - barW - 3);
        int header = wide ? HEADER : 0;
        int height = Math.min(screen.height - 8, 136 + 64) - BORDER * 2 - header - ARROW_ROW * 2;
        rows = Mth.clamp(height / ROW, 1, entries.size());
        barH = BORDER * 2 + header + ARROW_ROW * 2 + rows * ROW - 2;
        barY = Math.max(4, screen.getGuiTop() + 68 - barH / 2);
        listX = barX + BORDER;
        listY = barY + BORDER + header + ARROW_ROW;
        trackX = barX + barW - BORDER - TRACK;
        reveal(entries.indexOf(shown));
        dragging = false;
        filtered = List.of();
    }

    /** Filters the tab again whenever the game has filled it anew. */
    @SubscribeEvent
    public static void onRender(ScreenEvent.Render.Pre event) {
        if (onTab(event.getScreen()) && shown != null && !((CreativeModeInventoryScreen) event.getScreen()).getMenu().items.equals(filtered)) {
            show((CreativeModeInventoryScreen) event.getScreen(), shown);
        }
    }

    /** Presses the {@code index}-th entry (0: every school), as a click would. For the client smoke test. */
    public static void press(int index) {
        if (Minecraft.getInstance().screen instanceof CreativeModeInventoryScreen screen) {
            show(screen, entries.get(index));
            reveal(index);
        }
    }

    private static void show(CreativeModeInventoryScreen screen, @Nullable ModCreativeTabs.Section section) {
        shown = section;
        List<ItemStack> items = screen.getMenu().items;
        items.clear();
        for (ItemStack stack : ModCreativeTabs.MAIN.get().getDisplayItems()) {
            if (section == null || ModCreativeTabs.sectionOf(stack) == section) {
                items.add(stack);
            }
        }
        filtered = List.copyOf(items);
        screen.scrollOffs = 0;
        screen.getMenu().scrollTo(0);
    }

    /** Scrolls the bar so that entry {@code index} is in sight. */
    private static void reveal(int index) {
        if (index < scroll) {
            scroll = index;
        } else if (index >= scroll + rows) {
            scroll = index - rows + 1;
        }
        scroll = Mth.clamp(scroll, 0, maxScroll());
    }

    private static int maxScroll() {
        return Math.max(0, entries.size() - rows);
    }

    // ------------------------------------------------------------------ what an entry is

    private static Component name(@Nullable ModCreativeTabs.Section section) {
        return section == null ? Component.translatable("gui." + ArcanesOublies.MODID + ".creative.all") : section.school().get().getDisplayName();
    }

    private static int color(@Nullable ModCreativeTabs.Section section) {
        if (section == null) {
            return GOLD;
        }
        TextColor color = section.school().get().getDisplayName().getStyle().getColor();
        return color == null ? LAVENDER : color.getValue() & 0xFFFFFF;
    }

    private static ItemStack icon(@Nullable ModCreativeTabs.Section section) {
        return section == null ? new ItemStack(ItemRegistry.SCROLL.get()) : section.icon().get();
    }

    /** How many spells the school has (every school: all of the addon's). */
    private static int spells(@Nullable ModCreativeTabs.Section section) {
        int count = 0;
        for (var entry : ModSpells.SPELLS.getEntries()) {
            AbstractSpell spell = entry.get();
            if (spell.isEnabled() && (section == null || spell.getSchoolType() == section.school().get())) {
                count++;
            }
        }
        return count;
    }

    private static Component countLine(@Nullable ModCreativeTabs.Section section) {
        return Component.translatable("gui." + ArcanesOublies.MODID + ".creative.count", spells(section));
    }

    // ------------------------------------------------------------------ the bar's parts

    private static int thumbHeight() {
        int track = rows * ROW - 2;
        return entries.size() <= rows ? track : Math.max(10, track * rows / entries.size());
    }

    private static int thumbY() {
        int track = rows * ROW - 2;
        return listY + (maxScroll() == 0 ? 0 : (track - thumbHeight()) * scroll / maxScroll());
    }

    /** The entry under the mouse, or -1. */
    private static int entryAt(double mx, double my) {
        if (mx < listX || mx >= trackX - 2 || my < listY || my >= listY + rows * ROW) {
            return -1;
        }
        int row = (int) (my - listY) / ROW;
        if ((my - listY) - row * ROW >= ENTRY_SIZE) {
            return -1;
        }
        int index = scroll + row;
        return index < entries.size() ? index : -1;
    }

    private static boolean overBar(double mx, double my) {
        return mx >= barX && mx < barX + barW && my >= barY && my < barY + barH;
    }

    private static boolean overUp(double mx, double my) {
        return mx >= listX && mx < trackX && my >= listY - ARROW_ROW && my < listY;
    }

    private static boolean overDown(double mx, double my) {
        int bottom = listY + rows * ROW - 2;
        return mx >= listX && mx < trackX && my >= bottom && my < bottom + ARROW_ROW;
    }

    private static boolean overTrack(double mx, double my) {
        return mx >= trackX - 1 && mx < trackX + TRACK + 1 && my >= listY && my < listY + rows * ROW - 2;
    }

    private static void click(float pitch) {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, pitch));
    }

    // ------------------------------------------------------------------ input

    @SubscribeEvent
    public static void onClick(ScreenEvent.MouseButtonPressed.Pre event) {
        if (!onTab(event.getScreen()) || event.getButton() != 0 || !overBar(event.getMouseX(), event.getMouseY())) {
            return;
        }
        double mx = event.getMouseX(), my = event.getMouseY();
        CreativeModeInventoryScreen screen = (CreativeModeInventoryScreen) event.getScreen();
        int index = entryAt(mx, my);
        if (index >= 0) {
            if (entries.get(index) != shown) {
                show(screen, entries.get(index));
                Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.AMETHYST_BLOCK_CHIME, 1.2f + .05f * (index % 5), .8f));
            }
        } else if (overUp(mx, my) && scroll > 0) {
            scroll--;
            click(1.2f);
        } else if (overDown(mx, my) && scroll < maxScroll()) {
            scroll++;
            click(1.2f);
        } else if (overTrack(mx, my) && maxScroll() > 0) {
            int thumb = thumbY();
            if (my < thumb || my >= thumb + thumbHeight()) {
                // A click on the track brings the thumb there
                dragFrom = thumbHeight() / 2.0;
                dragTo(my);
            } else {
                dragFrom = my - thumb;
            }
            dragging = true;
        }
        // The bar takes every click on it, so nothing behind it is clicked
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onDrag(ScreenEvent.MouseDragged.Pre event) {
        if (dragging && onTab(event.getScreen())) {
            dragTo(event.getMouseY());
            event.setCanceled(true);
        }
    }

    private static void dragTo(double my) {
        int span = rows * ROW - 2 - thumbHeight();
        if (span > 0) {
            scroll = Mth.clamp((int) Math.round((my - dragFrom - listY) / span * maxScroll()), 0, maxScroll());
        }
    }

    @SubscribeEvent
    public static void onRelease(ScreenEvent.MouseButtonReleased.Pre event) {
        if (dragging) {
            dragging = false;
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onScroll(ScreenEvent.MouseScrolled.Pre event) {
        if (onTab(event.getScreen()) && overBar(event.getMouseX(), event.getMouseY())) {
            scroll = Mth.clamp(scroll - (int) Math.signum(event.getScrollDeltaY()), 0, maxScroll());
            event.setCanceled(true);
        }
    }

    // ------------------------------------------------------------------ drawing

    /** The chosen school's colour over the tab's background, under the items: its title band, its slots, its motes. */
    @SubscribeEvent
    public static void onBackground(ContainerScreenEvent.Render.Background event) {
        if (!onTab(event.getContainerScreen())) {
            return;
        }
        GuiGraphics g = event.getGuiGraphics();
        CreativeModeInventoryScreen screen = (CreativeModeInventoryScreen) event.getContainerScreen();
        int l = screen.getGuiLeft(), t = screen.getGuiTop();
        int rgb = color(shown);
        // The frame's inner edge in the school's colour
        int edge = argb(rgb, .55f);
        g.fill(l + 4, t + 4, l + 191, t + 5, edge);
        g.fill(l + 4, t + 131, l + 191, t + 132, edge);
        g.fill(l + 4, t + 5, l + 5, t + 131, edge);
        g.fill(l + 190, t + 5, l + 191, t + 131, edge);
        // A glow of it in every slot
        int glow = argb(rgb, shown == null ? .05f : .1f);
        for (int row = 0; row < 5; row++) {
            for (int col = 0; col < 9; col++) {
                g.fill(l + 9 + col * 18, t + 18 + row * 18, l + 25 + col * 18, t + 34 + row * 18, glow);
            }
        }
        // Motes of it drifting up behind the items
        Minecraft mc = Minecraft.getInstance();
        float time = (mc.level == null ? 0 : mc.level.getGameTime()) + mc.getTimer().getGameTimeDeltaPartialTick(false);
        for (int k = 0; k < 18; k++) {
            float h1 = hash(k * 7 + 1), h2 = hash(k * 7 + 2), h3 = hash(k * 7 + 3);
            float rise = (time * (.004f + .006f * h2) + h1) % 1;
            int x = l + 8 + (int) (h3 * 162) + (int) (Mth.sin(time * .03f + k) * 3);
            int y = t + 128 - (int) (rise * 112);
            float a = Mth.sin(rise * Mth.PI) * .7f;
            int size = k % 3 == 0 ? 2 : 1;
            g.fill(x, y, x + size, y + size, argb(lighten(rgb), a));
        }
    }

    /** The title: the chosen school's name in its colour (or the addon's name in gold), and how many spells it has. */
    @SubscribeEvent
    public static void onForeground(ContainerScreenEvent.Render.Foreground event) {
        if (!onTab(event.getContainerScreen())) {
            return;
        }
        GuiGraphics g = event.getGuiGraphics();
        Font font = Minecraft.getInstance().font;
        int rgb = color(shown);
        // The game has written the tab's own name there: the background's title band again over it
        g.blit(BACKGROUND, 5, 4, 5, 4, 166, 11);
        for (int i = 0; i < 40; i++) {
            g.fill(5 + i * 4, 4, 9 + i * 4, 15, argb(rgb, .3f * (1 - i / 40f)));
        }
        Component title = shown == null ? Component.translatable("itemGroup." + ArcanesOublies.MODID) : name(shown);
        Component count = countLine(shown);
        int countWidth = font.width(count);
        int room = 166 - (countWidth + 8);
        String text = font.plainSubstrByWidth(title.getString(), room);
        g.drawString(font, Component.literal(text).withStyle(title.getStyle()), 8, 6, rgb, false);
        g.drawString(font, count, 170 - countWidth, 6, DIM, false);
    }

    /** The bar of schools beside the inventory, and the tooltip of the entry under the mouse. */
    @SubscribeEvent
    public static void onRenderBar(ScreenEvent.Render.Post event) {
        if (!onTab(event.getScreen())) {
            return;
        }
        GuiGraphics g = event.getGuiGraphics();
        Font font = Minecraft.getInstance().font;
        int mx = event.getMouseX(), my = event.getMouseY();
        g.blitSprite(PANEL, barX, barY, barW, barH);
        if (wide) {
            Component header = Component.translatable("gui." + ArcanesOublies.MODID + ".creative.schools");
            g.drawString(font, header, barX + (barW - font.width(header)) / 2, barY + BORDER + 2, GOLD, false);
        }
        // Arrows, lit when they can scroll and are pointed at
        int arrowX = listX + (trackX - listX - 9) / 2;
        arrow(g, scroll > 0, overUp(mx, my), ARROW_UP, ARROW_UP_LIT, arrowX, listY - ARROW_ROW + 2);
        arrow(g, scroll < maxScroll(), overDown(mx, my), ARROW_DOWN, ARROW_DOWN_LIT, arrowX, listY + rows * ROW);
        // The entries
        int hovered = dragging ? -1 : entryAt(mx, my);
        int rowWidth = trackX - 3 - listX;
        for (int i = 0; i < rows && scroll + i < entries.size(); i++) {
            int index = scroll + i;
            ModCreativeTabs.Section section = entries.get(index);
            int x = listX, y = listY + i * ROW;
            boolean chosen = section == shown, hover = index == hovered;
            int rgb = color(section);
            if (wide && (chosen || hover)) {
                g.fill(x + ENTRY_SIZE, y + 1, x + rowWidth, y + ENTRY_SIZE - 1, argb(rgb, chosen ? .22f : .1f));
                if (chosen) {
                    g.fill(x + rowWidth - 2, y + 1, x + rowWidth, y + ENTRY_SIZE - 1, argb(rgb, .9f));
                }
            }
            g.blitSprite(chosen ? ENTRY_SELECTED : hover ? ENTRY_HOVER : ENTRY, x, y, ENTRY_SIZE, ENTRY_SIZE);
            if (chosen) {
                g.fill(x + 3, y + 3, x + ENTRY_SIZE - 3, y + ENTRY_SIZE - 3, argb(rgb, .35f));
            }
            g.renderItem(icon(section), x + 3, y + 3);
            if (wide) {
                int textX = x + ENTRY_SIZE + 4;
                String label = font.plainSubstrByWidth(name(section).getString(), rowWidth - ENTRY_SIZE - 8);
                g.drawString(font, label, textX, y + 3, chosen ? rgb : hover ? 0xFFFFFF : LAVENDER, false);
                g.drawString(font, countLine(section), textX, y + 12, DIM, false);
            }
        }
        // The scroll bar
        if (maxScroll() > 0) {
            int bottom = listY + rows * ROW - 2;
            g.fill(trackX, listY, trackX + TRACK, bottom, 0xFF0B0816);
            int thumb = thumbY();
            boolean lit = dragging || overTrack(mx, my);
            g.fill(trackX, thumb, trackX + TRACK, thumb + thumbHeight(), lit ? 0xFFFFE29C : 0xFFDEAC4E);
            g.fill(trackX + TRACK - 1, thumb, trackX + TRACK, thumb + thumbHeight(), 0xFF7A521E);
        }
        if (hovered >= 0) {
            ModCreativeTabs.Section section = entries.get(hovered);
            List<Component> lines = new ArrayList<>();
            lines.add(name(section).copy().withColor(color(section)));
            lines.add(countLine(section).copy().withStyle(ChatFormatting.GRAY));
            g.renderComponentTooltip(font, lines, mx, my);
        }
    }

    private static void arrow(GuiGraphics g, boolean active, boolean hover, ResourceLocation normal, ResourceLocation lit, int x, int y) {
        if (!active) {
            return;
        }
        g.blitSprite(hover ? lit : normal, x, y, 9, 5);
    }

    private static int argb(int rgb, float alpha) {
        return Mth.clamp((int) (alpha * 255), 0, 255) << 24 | (rgb & 0xFFFFFF);
    }

    private static float hash(int seed) {
        int h = seed * 0x27d4eb2d;
        h ^= h >>> 15;
        h *= 0x85ebca6b;
        h ^= h >>> 13;
        return (h & 0xFFFF) / 65535f;
    }

    /** Halfway to white. */
    private static int lighten(int rgb) {
        int r = (rgb >> 16) & 0xFF, g = (rgb >> 8) & 0xFF, b = rgb & 0xFF;
        return (r + (255 - r) / 2) << 16 | (g + (255 - g) / 2) << 8 | (b + (255 - b) / 2);
    }
}
