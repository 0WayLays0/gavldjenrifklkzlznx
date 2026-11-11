package com.waylays.eraphones.pack;

import com.waylays.eraphones.EraPhones;
import com.waylays.eraphones.rp.RpConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.core.NonNullList;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import com.waylays.eraphones.compat.StreamCodec;
import com.waylays.eraphones.compat.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;
import net.minecraftforge.event.TickEvent;
import com.waylays.eraphones.compat.IPayloadContext;
import com.waylays.eraphones.compat.PayloadRegistrar;

/**
 * A limited inventory: players (not in creative or spectator) carry only the first few hotbar slots (five, unless the
 * server says otherwise); anything that lands elsewhere goes into their backpack, else into a free hand slot, else to
 * the ground; they pick up nothing they have no room for. A backpack worn (in the chest's place) holds ten more.
 */
@EventBusSubscriber(modid = EraPhones.MODID)
public final class Packs {
    /** What a backpack holds. */
    public static final int SIZE = 10;

    private Packs() {}

    public static boolean limited(Player p) {
        return RpConfig.get(RpConfig.LIMITED_INVENTORY, true) && !p.isCreative() && !p.isSpectator();
    }

    /** How many hotbar slots may be used. */
    public static int slots() {
        return Math.max(1, Math.min(9, RpConfig.get(RpConfig.HOTBAR_SLOTS, 5)));
    }

    /** The backpack {@code p} wears, or empty. */
    public static ItemStack worn(Player p) {
        ItemStack s = p.getItemBySlot(EquipmentSlot.CHEST);
        return s.getItem() instanceof BackpackItem ? s : ItemStack.EMPTY;
    }

    // ---------------------------------------------------------------- what a backpack holds

    public static NonNullList<ItemStack> contents(ItemStack bag) {
        NonNullList<ItemStack> list = NonNullList.withSize(SIZE, ItemStack.EMPTY);
        java.util.List<ItemStack> c = com.waylays.eraphones.compat.ItemData.items(bag);
        for (int i = 0; i < c.size() && i < SIZE; i++) {
            list.set(i, c.get(i));
        }
        return list;
    }

    public static void store(ItemStack bag, NonNullList<ItemStack> list) {
        com.waylays.eraphones.compat.ItemData.setItems(bag, list);
    }

    public static int count(ItemStack bag) {
        int n = 0;
        for (ItemStack s : contents(bag)) {
            if (!s.isEmpty()) {
                n++;
            }
        }
        return n;
    }

    /** Puts what it can of {@code stack} into {@code list} (merging first); returns what is left. */
    private static ItemStack fill(NonNullList<ItemStack> list, int from, int to, ItemStack stack) {
        for (int i = from; i < to && !stack.isEmpty(); i++) {
            ItemStack in = list.get(i);
            if (!in.isEmpty() && ItemStack.isSameItemSameTags(in, stack) && in.getCount() < in.getMaxStackSize()) {
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

    private static ItemStack intoBag(Player p, ItemStack stack) {
        ItemStack bag = worn(p);
        if (bag.isEmpty() || stack.getItem() instanceof BackpackItem) {
            return stack;
        }
        NonNullList<ItemStack> list = contents(bag);
        int before = stack.getCount();
        stack = fill(list, 0, SIZE, stack);
        if (stack.getCount() != before) {
            store(bag, list);
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
            if (in.isEmpty() || ItemStack.isSameItemSameTags(in, stack) && in.getCount() < in.getMaxStackSize()) {
                return true;
            }
        }
        ItemStack bag = worn(p);
        if (bag.isEmpty() || stack.getItem() instanceof BackpackItem) {
            return false;
        }
        for (ItemStack in : contents(bag)) {
            if (in.isEmpty() || ItemStack.isSameItemSameTags(in, stack) && in.getCount() < in.getMaxStackSize()) {
                return true;
            }
        }
        return false;
    }

    // ---------------------------------------------------------------- keeping to it

    @SubscribeEvent
    public static void onTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        if (!(event.player instanceof ServerPlayer p) || !limited(p)) {
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
            s = intoBag(p, s);
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
    public static void onPickup(EntityItemPickupEvent event) {
        Player p = event.getEntity();
        if (!p.level().isClientSide && limited(p) && !room(p, event.getItem().getItem())) {
            event.setCanceled(true);
        }
    }

    // ---------------------------------------------------------------- opening it

    /** Opens {@code bag} (worn or in hand) for {@code p}. */
    public static void open(ServerPlayer p, ItemStack bag) {
        if (!(bag.getItem() instanceof BackpackItem b)) {
            return;
        }
        int hands = limited(p) ? slots() : 9;
        p.level().playSound(null, p.blockPosition(), SoundEvents.ARMOR_EQUIP_LEATHER, SoundSource.PLAYERS, .6f, 1.3f);
        net.minecraftforge.network.NetworkHooks.openScreen(p, new SimpleMenuProvider((id, inv, pl) -> new BackpackMenu(id, inv, bag, b.style(), hands), bag.getHoverName()), buf -> {
            buf.writeVarInt(b.style());
            buf.writeVarInt(hands);
        });
    }

    public static void register(PayloadRegistrar r) {
        r.playToServer(Open.TYPE, Open.CODEC, (o, c) -> c.enqueueWork(() -> openWorn(c)));
    }

    private static void openWorn(IPayloadContext c) {
        if (!(c.player() instanceof ServerPlayer p)) {
            return;
        }
        ItemStack bag = worn(p);
        if (bag.isEmpty()) {
            p.displayClientMessage(Component.translatable("pack.eraphones.none").withStyle(ChatFormatting.GRAY), true);
            return;
        }
        open(p, bag);
    }

    /** The backpack key was pressed. */
    public record Open() implements CustomPacketPayload {
        public static final Type<Open> TYPE = new Type<>(EraPhones.id("pack_open"));
        public static final StreamCodec<FriendlyByteBuf, Open> CODEC = StreamCodec.unit(new Open());

        @Override
        public Type<Open> type() {
            return TYPE;
        }
    }
}
