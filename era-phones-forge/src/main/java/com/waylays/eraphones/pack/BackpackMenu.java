package com.waylays.eraphones.pack;

import net.minecraft.core.NonNullList;
import com.waylays.eraphones.compat.RegistryFriendlyByteBuf;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * A backpack open: its ten pockets (two rows of five) and, under them, what the player has in hand (their hotbar
 * slots they may use). The open backpack itself cannot be moved; a backpack never goes in a backpack.
 */
public class BackpackMenu extends AbstractContainerMenu {
    public static final int POCKETS_Y = 32, HANDS_Y = 112;
    private final SimpleContainer pockets = new SimpleContainer(Packs.SIZE);
    private final ItemStack bag;
    private final int style, hands;
    private boolean loading;

    /** On the player's side: what the server opened. */
    public BackpackMenu(int id, Inventory inv, net.minecraft.network.FriendlyByteBuf buf) {
        this(id, inv, ItemStack.EMPTY, buf.readVarInt(), buf.readVarInt());
    }

    public BackpackMenu(int id, Inventory inv, ItemStack bag, int style, int hands) {
        super(PackRegistry.MENU.get(), id);
        this.bag = bag;
        this.style = style;
        this.hands = Math.max(1, Math.min(9, hands));
        if (!bag.isEmpty()) {
            loading = true;
            NonNullList<ItemStack> list = Packs.contents(bag);
            for (int i = 0; i < Packs.SIZE; i++) {
                pockets.setItem(i, list.get(i));
            }
            loading = false;
        }
        pockets.addListener(c -> save());
        for (int i = 0; i < Packs.SIZE; i++) {
            addSlot(new Slot(pockets, i, 44 + (i % 5) * 18, POCKETS_Y + (i / 5) * 18) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return !(stack.getItem() instanceof BackpackItem) && stack.getItem().canFitInsideContainerItems();
                }
            });
        }
        int x0 = (176 - this.hands * 18) / 2 + 1;
        for (int j = 0; j < this.hands; j++) {
            addSlot(new Slot(inv, j, x0 + j * 18, HANDS_Y) {
                @Override
                public boolean mayPickup(Player player) {
                    return getItem() != BackpackMenu.this.bag && super.mayPickup(player);
                }
            });
        }
    }

    public int style() {
        return style;
    }

    public int hands() {
        return hands;
    }

    private void save() {
        if (loading || bag.isEmpty()) {
            return;
        }
        NonNullList<ItemStack> list = NonNullList.withSize(Packs.SIZE, ItemStack.EMPTY);
        for (int i = 0; i < Packs.SIZE; i++) {
            list.set(i, pockets.getItem(i).copy());
        }
        Packs.store(bag, list);
    }

    @Override
    public boolean stillValid(Player player) {
        if (bag.isEmpty()) {
            return true;
        }
        return player.getItemBySlot(EquipmentSlot.CHEST) == bag || player.getMainHandItem() == bag || player.getOffhandItem() == bag;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem() || !slot.mayPickup(player)) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack before = stack.copy();
        if (index < Packs.SIZE) {
            if (!moveItemStackTo(stack, Packs.SIZE, Packs.SIZE + hands, false)) {
                return ItemStack.EMPTY;
            }
        } else if (stack.getItem() instanceof BackpackItem || !moveItemStackTo(stack, 0, Packs.SIZE, false)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return before;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        save();
    }
}
