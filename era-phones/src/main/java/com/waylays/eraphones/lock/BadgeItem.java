package com.waylays.eraphones.lock;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/** An access badge: swiped on a card reader that knows it, it opens the doors beside the reader. */
public class BadgeItem extends Item {
    public BadgeItem(Properties properties) {
        super(properties);
    }

    public static String id(ItemStack stack) {
        return stack.get(LockRegistry.BADGE.get());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        String id = id(stack);
        tooltip.add(id == null ? Component.translatable("lock.eraphones.badge_blank").withStyle(ChatFormatting.DARK_GRAY)
                : Component.translatable("lock.eraphones.badge_number", id).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("lock.eraphones.badge_help").withStyle(ChatFormatting.DARK_GRAY));
    }
}
