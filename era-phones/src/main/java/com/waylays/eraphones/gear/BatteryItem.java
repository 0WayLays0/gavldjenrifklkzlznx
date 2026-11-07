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

/** Batteries: used with a torch or a walkie-talkie in the other hand, they replace its flat ones. */
public class BatteryItem extends Item {
    public BatteryItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack batteries = player.getItemInHand(hand);
        ItemStack other = player.getItemInHand(hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND);
        if (!(other.getItem() instanceof FlashlightItem) && !(other.getItem() instanceof WalkieItem)) {
            if (!level.isClientSide) {
                player.displayClientMessage(Component.translatable("item.eraphones.piles.help").withStyle(ChatFormatting.GRAY), true);
            }
            return InteractionResultHolder.fail(batteries);
        }
        if (!level.isClientSide) {
            Gear.setBattery(other, Gear.FULL);
            if (!player.getAbilities().instabuild) {
                batteries.shrink(1);
            }
            level.playSound(null, player.blockPosition(), SoundEvents.IRON_TRAPDOOR_CLOSE, SoundSource.PLAYERS, .4f, 1.8f);
            player.displayClientMessage(Component.translatable("gear.eraphones.recharged").withStyle(ChatFormatting.GREEN), true);
        }
        return InteractionResultHolder.sidedSuccess(batteries, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.eraphones.piles.help").withStyle(ChatFormatting.DARK_GRAY));
    }
}
