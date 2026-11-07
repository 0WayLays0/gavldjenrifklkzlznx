package com.waylays.eraphones.gear;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * A torch of the time, on batteries: switched on and off by using it; held in either hand, it lights where it points
 * (see {@link Beams}) until its batteries are flat.
 */
public class FlashlightItem extends Item {
    public FlashlightItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide) {
            boolean on = !stack.getOrDefault(com.waylays.eraphones.registry.GearRegistry.POWER.get(), false);
            if (on && Gear.battery(stack) <= 0) {
                player.displayClientMessage(Component.translatable("gear.eraphones.flat").withStyle(ChatFormatting.GRAY), true);
                on = false;
            }
            Gear.setOn(stack, on);
            level.playSound(null, player.blockPosition(), SoundEvents.LEVER_CLICK, SoundSource.PLAYERS, .5f, on ? 1.6f : 1.2f);
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
        tooltip.add(Component.translatable(Gear.on(stack) ? "gear.eraphones.on" : "gear.eraphones.off").withStyle(Gear.on(stack) ? ChatFormatting.YELLOW : ChatFormatting.DARK_GRAY));
        tooltip.add(Component.translatable("tooltip.eraphones.battery", Gear.battery(stack) / 10).withStyle(ChatFormatting.DARK_GREEN));
        tooltip.add(Component.translatable("item.eraphones.lampe_torche.help").withStyle(ChatFormatting.DARK_GRAY));
    }
}
