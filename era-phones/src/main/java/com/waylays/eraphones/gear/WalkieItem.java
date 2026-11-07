package com.waylays.eraphones.gear;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * A walkie-talkie: using it opens its face (switch, channel 1 to 16, battery). Switched on, it carries the voice
 * (with Simple Voice Chat) of whoever speaks holding it to every other walkie-talkie switched on to the same channel,
 * wherever they are carried, and out of its little speaker for those around them to hear.
 */
public class WalkieItem extends Item {
    public WalkieItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) {
            com.waylays.eraphones.gear.client.GearClient.openWalkie(hand);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return true;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Gear.barWidth(stack);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return Gear.barColor(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable(Gear.on(stack) ? "gear.eraphones.on" : "gear.eraphones.off").withStyle(Gear.on(stack) ? ChatFormatting.GREEN : ChatFormatting.DARK_GRAY));
        tooltip.add(Component.translatable("gear.eraphones.channel", Gear.channel(stack)).withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable("tooltip.eraphones.battery", Gear.battery(stack) / 10).withStyle(ChatFormatting.DARK_GREEN));
        tooltip.add(Component.translatable("item.eraphones.talkie_walkie.help").withStyle(ChatFormatting.DARK_GRAY));
    }
}
