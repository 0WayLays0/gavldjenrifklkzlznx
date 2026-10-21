package com.waylays.arcanes.client;

import com.waylays.arcanes.ArcanesOublies;
import com.waylays.arcanes.registry.ModCreativeTabs;
import io.redspace.ironsspellbooks.registries.ItemRegistry;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;

/**
 * The sub-parts of the addon's creative tab: while it is open, a column of buttons at the left of the inventory
 * (everything, then one per school) shows the whole tab or a single school's spellbooks and scrolls.
 */
@EventBusSubscriber(modid = ArcanesOublies.MODID, value = Dist.CLIENT)
public final class CreativeTabFilter {
    private static final ResourceLocation BUTTON = ResourceLocation.withDefaultNamespace("widget/button");
    private static final ResourceLocation BUTTON_LIT = ResourceLocation.withDefaultNamespace("widget/button_highlighted");
    private static final int SIZE = 20;
    private static final int GAP = 2;

    /** The school shown (null: all of them), kept while the game runs. */
    @Nullable
    private static ModCreativeTabs.Section shown;
    /** The tab's items as last filtered here: anything else means the game filled the tab again. */
    private static List<ItemStack> filtered = List.of();
    private static final List<SectionButton> buttons = new ArrayList<>();

    private CreativeTabFilter() {}

    @SubscribeEvent
    public static void onInit(ScreenEvent.Init.Post event) {
        if (!(event.getScreen() instanceof CreativeModeInventoryScreen screen)) {
            return;
        }
        buttons.clear();
        int x = screen.getGuiLeft() - SIZE - 4;
        int y = screen.getGuiTop() + 2;
        List<ModCreativeTabs.Section> sections = new ArrayList<>();
        sections.add(null);
        sections.addAll(ModCreativeTabs.SECTIONS);
        for (int i = 0; i < sections.size(); i++) {
            SectionButton button = new SectionButton(x, y + i * (SIZE + GAP), sections.get(i), screen);
            buttons.add(button);
            event.addListener(button);
        }
        filtered = List.of();
    }

    /** Shows the buttons only on the addon's tab, and filters it again whenever the game has filled it anew. */
    @SubscribeEvent
    public static void onRender(ScreenEvent.Render.Pre event) {
        if (!(event.getScreen() instanceof CreativeModeInventoryScreen screen)) {
            return;
        }
        boolean ours = CreativeModeInventoryScreen.selectedTab == ModCreativeTabs.MAIN.get();
        for (SectionButton button : buttons) {
            button.visible = ours;
        }
        if (ours && shown != null && !screen.getMenu().items.equals(filtered)) {
            show(screen, shown);
        }
    }

    /** Presses the {@code index}-th button (0: every school), as a click would. For the client smoke test. */
    public static void press(int index) {
        buttons.get(index).onPress();
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

    /** A school's button: its spellbook (or a scroll) drawn on a small button, lit while that school is shown. */
    private static final class SectionButton extends Button {
        @Nullable
        private final ModCreativeTabs.Section section;
        private final ItemStack icon;

        SectionButton(int x, int y, @Nullable ModCreativeTabs.Section section, CreativeModeInventoryScreen screen) {
            super(x, y, SIZE, SIZE, title(section), button -> show(screen, section), DEFAULT_NARRATION);
            this.section = section;
            this.icon = section == null ? new ItemStack(ItemRegistry.SCROLL.get()) : section.icon().get();
            setTooltip(Tooltip.create(title(section)));
        }

        private static Component title(@Nullable ModCreativeTabs.Section section) {
            return section == null ? Component.translatable("gui." + ArcanesOublies.MODID + ".creative.all") : section.school().get().getDisplayName();
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            boolean lit = shown == section;
            graphics.blitSprite(lit || isHovered() ? BUTTON_LIT : BUTTON, getX(), getY(), width, height);
            if (lit) {
                // A bar in the school's colour on the side facing the items
                TextColor color = section == null ? null : section.school().get().getDisplayName().getStyle().getColor();
                graphics.fill(getX() + width - 3, getY() + 2, getX() + width - 1, getY() + height - 2, 0xFF000000 | (color == null ? 0xFFFFFF : color.getValue()));
            }
            graphics.renderItem(icon, getX() + (width - 16) / 2, getY() + (height - 16) / 2);
        }
    }
}
