package com.waylays.eraphones.pack;

import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Equipable;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * A backpack: worn on the back (in the chest's place), it holds ten more things. Used in hand, it opens; crouching,
 * it is put on. Worn, a key (B) opens it.
 */
public class BackpackItem extends Item implements Equipable {
    private final int style;

    public BackpackItem(int style, Properties properties) {
        super(properties);
        this.style = style;
    }

    public int style() {
        return style;
    }

    @Override
    public EquipmentSlot getEquipmentSlot() {
        return EquipmentSlot.CHEST;
    }

    @Override
    public SoundEvent getEquipSound() {
        return SoundEvents.ARMOR_EQUIP_LEATHER;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (player.isSecondaryUseActive()) {
            return swapWithEquipmentSlot(this, level, player, hand);
        }
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide && player instanceof ServerPlayer sp) {
            Packs.open(sp, stack);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, @javax.annotation.Nullable net.minecraft.world.level.Level context, List<Component> tooltip, TooltipFlag flag) {
        int n = Packs.count(stack);
        tooltip.add(Component.translatable("pack.eraphones.holds", n, Packs.SIZE).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("pack.eraphones.help").withStyle(ChatFormatting.DARK_GRAY));
    }

    @Override
    public boolean canFitInsideContainerItems() {
        return false;
    }
}
