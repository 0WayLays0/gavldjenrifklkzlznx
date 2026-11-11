package com.waylays.eraphones.lock;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;

import java.util.List;

/**
 * A padlock. Used on a door, a trapdoor, a gate or a chest, it locks it and gives its key; with a key held in the
 * other hand, it takes that key's secret (one key for several doors).
 */
public class PadlockItem extends Item {
    public PadlockItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext c) {
        if (c.getLevel().isClientSide || !(c.getPlayer() instanceof ServerPlayer p)) {
            return InteractionResult.sidedSuccess(c.getLevel().isClientSide);
        }
        InteractionHand other = c.getHand() == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        ItemStack otherStack = p.getItemInHand(other);
        String key = otherStack.getItem() instanceof KeyItem ? KeyItem.id(otherStack) : null;
        Locks.lock(p, c.getClickedPos(), key, c.getItemInHand());
        return InteractionResult.CONSUME;
    }

    @Override
    public void appendHoverText(ItemStack stack, @javax.annotation.Nullable net.minecraft.world.level.Level context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("lock.eraphones.padlock_help").withStyle(ChatFormatting.DARK_GRAY));
    }
}
