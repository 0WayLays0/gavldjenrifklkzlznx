package com.waylays.eraphones.horror;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/** A red balloon on its string, held high: a thing to carry, to leave in a drain, to hand to a child. */
public class BalloonItem extends Item {
    public BalloonItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.eraphones.ballon_rouge.help").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
    }
}
