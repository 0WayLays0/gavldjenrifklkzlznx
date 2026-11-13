package com.waylays.eraphones.pack.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.waylays.eraphones.EraPhones;
import com.waylays.eraphones.pack.Packs;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

/**
 * The limited inventory on the player's side: a hotbar of only the slots that may be used (drawn shorter, centred),
 * the selection kept to them (scrolling wraps round them), the other slots of every inventory screen shaded and
 * closed.
 */
@EventBusSubscriber(modid = EraPhones.MODID, value = Dist.CLIENT)
public final class PackClient {
    private static final ResourceLocation HOTBAR = ResourceLocation.withDefaultNamespace("hud/hotbar");
    private static final ResourceLocation SELECTION = ResourceLocation.withDefaultNamespace("hud/hotbar_selection");
    private static final ResourceLocation OFFHAND = ResourceLocation.withDefaultNamespace("hud/hotbar_offhand_left");
    private static int last;

    private PackClient() {}

    private static boolean limited() {
        LocalPlayer p = Minecraft.getInstance().player;
        return p != null && Packs.limited(p);
    }

    @SubscribeEvent
    public static void onTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        if (p == null) {
            return;
        }
        if (!limited()) {
            last = p.getInventory().selected;
            return;
        }
        int n = Packs.slots();
        Inventory inv = p.getInventory();
        if (inv.selected >= n) {
            // Scrolling on past the last wraps to the first, back past the first to the last; a number key out of reach does nothing
            int to = last == n - 1 && inv.selected == n ? 0 : last == 0 && inv.selected == 8 ? n - 1 : Math.min(last, n - 1);
            inv.selected = Math.max(0, to);
        }
        last = inv.selected;
    }

    /** The hotbar drawn with only its usable slots, centred, and the off hand's slot beside it. */
    @SubscribeEvent
    public static void onHotbar(RenderGuiLayerEvent.Pre event) {
        if (!event.getName().equals(VanillaGuiLayers.HOTBAR) || !limited()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui) {
            return;
        }
        event.setCanceled(true);
        GuiGraphics g = event.getGuiGraphics();
        LocalPlayer p = mc.player;
        int n = Packs.slots();
        int w = n * 20 + 2;
        int left = g.guiWidth() / 2 - w / 2, top = g.guiHeight() - 22;
        com.mojang.blaze3d.systems.RenderSystem.enableBlend();
        g.pose().pushPose();
        g.pose().translate(0, 0, -90);
        g.blitSprite(HOTBAR, 182, 22, 0, 0, left, top, n * 20 + 1, 22);
        g.blitSprite(HOTBAR, 182, 22, 181, 0, left + n * 20 + 1, top, 1, 22);
        g.blitSprite(SELECTION, left - 1 + p.getInventory().selected * 20, top - 1, 24, 23);
        ItemStack off = p.getOffhandItem();
        if (!off.isEmpty()) {
            g.blitSprite(OFFHAND, left - 29, top - 1, 29, 24);
        }
        g.pose().popPose();
        for (int i = 0; i < n; i++) {
            ItemStack s = p.getInventory().getItem(i);
            if (!s.isEmpty()) {
                g.renderItem(p, s, left + 3 + i * 20, top + 3, i + 1);
                g.renderItemDecorations(mc.font, s, left + 3 + i * 20, top + 3);
            }
        }
        if (!off.isEmpty()) {
            g.renderItem(p, off, left - 26, top + 3, 99);
            g.renderItemDecorations(mc.font, off, left - 26, top + 3);
        }
        com.mojang.blaze3d.systems.RenderSystem.disableBlend();
    }

    private static boolean closed(Slot slot) {
        return slot.container instanceof Inventory && slot.getContainerSlot() >= Packs.slots() && slot.getContainerSlot() < 36;
    }

    private static boolean applies(Object screen) {
        return screen instanceof AbstractContainerScreen<?> && !(screen instanceof CreativeModeInventoryScreen) && limited();
    }

    /** The slots out of reach, shaded over in every inventory screen. */
    @SubscribeEvent
    public static void onScreen(ScreenEvent.Render.Post event) {
        if (!applies(event.getScreen())) {
            return;
        }
        AbstractContainerScreen<?> s = (AbstractContainerScreen<?>) event.getScreen();
        GuiGraphics g = event.getGuiGraphics();
        g.pose().pushPose();
        g.pose().translate(0, 0, 350);
        for (Slot slot : s.getMenu().slots) {
            if (closed(slot)) {
                int x = s.getGuiLeft() + slot.x, y = s.getGuiTop() + slot.y;
                g.fill(x - 1, y - 1, x + 17, y + 17, 0xE0282420);
                g.fill(x + 3, y + 7, x + 13, y + 9, 0x60000000);
            }
        }
        g.pose().popPose();
    }

    @SubscribeEvent
    public static void onClick(ScreenEvent.MouseButtonPressed.Pre event) {
        if (applies(event.getScreen())) {
            Slot slot = ((AbstractContainerScreen<?>) event.getScreen()).getSlotUnderMouse();
            if (slot != null && closed(slot)) {
                event.setCanceled(true);
            }
        }
    }
}
