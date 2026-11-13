package com.waylays.eraphones.pack;

import com.waylays.eraphones.EraPhones;
import com.waylays.eraphones.rp.RpConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * A limited inventory: players (not in creative or spectator) carry only the first few hotbar slots (five, unless the
 * server says otherwise); anything that lands elsewhere goes into a free hand slot, else to the ground; they pick up
 * nothing they have no room for.
 */
@EventBusSubscriber(modid = EraPhones.MODID)
public final class Packs {
    private Packs() {}

    public static boolean limited(Player p) {
        return RpConfig.get(RpConfig.LIMITED_INVENTORY, true) && !p.isCreative() && !p.isSpectator();
    }

    /** How many hotbar slots may be used. */
    public static int slots() {
        return Math.max(1, Math.min(9, RpConfig.get(RpConfig.HOTBAR_SLOTS, 5)));
    }

    /** Puts what it can of {@code stack} into {@code list} (merging first); returns what is left. */
    private static ItemStack fill(NonNullList<ItemStack> list, int from, int to, ItemStack stack) {
        for (int i = from; i < to && !stack.isEmpty(); i++) {
            ItemStack in = list.get(i);
            if (!in.isEmpty() && ItemStack.isSameItemSameComponents(in, stack) && in.getCount() < in.getMaxStackSize()) {
                int move = Math.min(stack.getCount(), in.getMaxStackSize() - in.getCount());
                in.grow(move);
                stack.shrink(move);
            }
        }
        for (int i = from; i < to && !stack.isEmpty(); i++) {
            if (list.get(i).isEmpty()) {
                list.set(i, stack.copy());
                stack.setCount(0);
            }
        }
        return stack;
    }

    private static ItemStack intoHands(Inventory inv, ItemStack stack) {
        NonNullList<ItemStack> hot = NonNullList.withSize(slots(), ItemStack.EMPTY);
        for (int i = 0; i < hot.size(); i++) {
            hot.set(i, inv.getItem(i));
        }
        stack = fill(hot, 0, hot.size(), stack);
        for (int i = 0; i < hot.size(); i++) {
            if (inv.getItem(i) != hot.get(i)) {
                inv.setItem(i, hot.get(i));
            }
        }
        return stack;
    }

    /** Whether {@code p} has room somewhere for some of {@code stack}. */
    public static boolean room(Player p, ItemStack stack) {
        Inventory inv = p.getInventory();
        for (int i = 0; i < slots(); i++) {
            ItemStack in = inv.getItem(i);
            if (in.isEmpty() || ItemStack.isSameItemSameComponents(in, stack) && in.getCount() < in.getMaxStackSize()) {
                return true;
            }
        }
        return false;
    }

    // ---------------------------------------------------------------- keeping to it

    @SubscribeEvent
    public static void onTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || !limited(p)) {
            return;
        }
        Inventory inv = p.getInventory();
        int n = slots();
        boolean dropped = false;
        for (int i = n; i < 36; i++) {
            ItemStack s = inv.getItem(i);
            if (s.isEmpty()) {
                continue;
            }
            inv.setItem(i, ItemStack.EMPTY);
            s = intoHands(inv, s);
            if (!s.isEmpty()) {
                p.drop(s, false, true);
                dropped = true;
            }
        }
        if (dropped) {
            p.displayClientMessage(Component.translatable("pack.eraphones.no_room").withStyle(ChatFormatting.GRAY), true);
        }
    }

    @SubscribeEvent
    public static void onPickup(ItemEntityPickupEvent.Pre event) {
        Player p = event.getPlayer();
        if (!p.level().isClientSide && limited(p) && !room(p, event.getItemEntity().getItem())) {
            event.setCanPickup(TriState.FALSE);
        }
    }
}
