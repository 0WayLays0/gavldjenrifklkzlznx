package com.waylays.arcanes.rp.admin;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * /invsee's chest: another player's inventory, live, in five rows (the game's own five-row chest, so nothing is needed
 * on the players' side). Rows 1 to 3: their bag; row 4: their hotbar; row 5: helmet, chestplate, leggings, boots,
 * off hand, then four locked panes. Whatever is taken or put in changes their inventory at once.
 */
public final class InvseeMenu extends ChestMenu {
    private static final int ROWS = 5;

    public InvseeMenu(int id, Inventory viewer, Box box) {
        super(MenuType.GENERIC_9x5, id, viewer, box, ROWS);
        // The last four places of the fifth row hold nothing of theirs: locked
        for (int i = Box.REAL; i < ROWS * 9; i++) {
            Slot old = slots.get(i);
            Slot locked = new Locked(box, i, old.x, old.y);
            locked.index = i;
            slots.set(i, locked);
        }
    }

    private static final class Locked extends Slot {
        Locked(Container c, int i, int x, int y) {
            super(c, i, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }

        @Override
        public boolean mayPickup(Player player) {
            return false;
        }
    }

    /** The other player's inventory, laid out as the chest's places. */
    public static final class Box implements Container {
        /** The places that are theirs (the others are locked panes). */
        static final int REAL = 41;
        /** For each place of the chest, the place in their inventory. */
        private static final int[] SLOT = new int[REAL];

        static {
            for (int i = 0; i < 27; i++) {
                SLOT[i] = 9 + i;
            }
            for (int i = 0; i < 9; i++) {
                SLOT[27 + i] = i;
            }
            // Helmet, chestplate, leggings, boots (inventory 39 to 36), then the off hand (40)
            SLOT[36] = 39;
            SLOT[37] = 38;
            SLOT[38] = 37;
            SLOT[39] = 36;
            SLOT[40] = 40;
        }

        private final ServerPlayer target;
        private final ItemStack pane;

        public Box(ServerPlayer target) {
            this.target = target;
            this.pane = new ItemStack(Items.GRAY_STAINED_GLASS_PANE);
            this.pane.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME, Component.literal(" "));
        }

        private Inventory inv() {
            return target.getInventory();
        }

        @Override
        public int getContainerSize() {
            return ROWS * 9;
        }

        @Override
        public boolean isEmpty() {
            return inv().isEmpty();
        }

        @Override
        public ItemStack getItem(int i) {
            return i < REAL ? inv().getItem(SLOT[i]) : pane;
        }

        @Override
        public ItemStack removeItem(int i, int count) {
            if (i >= REAL) {
                return ItemStack.EMPTY;
            }
            ItemStack out = inv().removeItem(SLOT[i], count);
            setChanged();
            return out;
        }

        @Override
        public ItemStack removeItemNoUpdate(int i) {
            return i < REAL ? inv().removeItemNoUpdate(SLOT[i]) : ItemStack.EMPTY;
        }

        @Override
        public void setItem(int i, ItemStack stack) {
            if (i < REAL) {
                inv().setItem(SLOT[i], stack);
                setChanged();
            }
        }

        @Override
        public void setChanged() {
            inv().setChanged();
        }

        @Override
        public boolean stillValid(Player viewer) {
            // Closes when they leave the game or die
            return !target.hasDisconnected() && target.isAlive();
        }

        @Override
        public void clearContent() {
            // Never everything at once
        }
    }
}
