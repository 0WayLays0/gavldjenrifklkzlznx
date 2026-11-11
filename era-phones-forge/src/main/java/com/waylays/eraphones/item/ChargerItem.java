package com.waylays.eraphones.item;

import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * A phone charger: held in one hand with the phone in the other, and kept pressed, it fills the battery (a full
 * charge in a dozen seconds).
 */
public class ChargerItem extends Item {
    private static final int PER_TICK = 4;

    public ChargerItem(Properties properties) {
        super(properties);
    }

    private static ItemStack phoneIn(Player player, InteractionHand chargerHand) {
        ItemStack other = player.getItemInHand(chargerHand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND);
        return other.getItem() instanceof PhoneItem ? other : ItemStack.EMPTY;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack self = player.getItemInHand(hand);
        ItemStack phone = phoneIn(player, hand);
        if (phone.isEmpty()) {
            if (!level.isClientSide) {
                player.displayClientMessage(Component.translatable("message.eraphones.charger_needs_phone"), true);
            }
            return InteractionResultHolder.fail(self);
        }
        if (PhoneItem.battery(phone) >= PhoneItem.FULL) {
            if (!level.isClientSide) {
                player.displayClientMessage(Component.translatable("message.eraphones.charged"), true);
            }
            return InteractionResultHolder.fail(self);
        }
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(self);
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return 72000;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.NONE;
    }

    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remaining) {
        if (level.isClientSide || !(entity instanceof Player player)) {
            return;
        }
        ItemStack phone = phoneIn(player, entity.getUsedItemHand());
        if (phone.isEmpty()) {
            player.stopUsingItem();
            return;
        }
        int b = PhoneItem.battery(phone) + PER_TICK;
        PhoneItem.setBattery(phone, b);
        int used = getUseDuration(stack) - remaining;
        if (used % 20 == 0) {
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.NOTE_BLOCK_BIT.value(), SoundSource.PLAYERS, .25f, 1.6f + Math.min(1, b / 1000f) * .4f);
            player.displayClientMessage(Component.translatable("message.eraphones.charging", Math.min(100, b / 10)), true);
        }
        if (b >= PhoneItem.FULL) {
            player.stopUsingItem();
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.NOTE_BLOCK_CHIME.value(), SoundSource.PLAYERS, .4f, 1.8f);
            player.displayClientMessage(Component.translatable("message.eraphones.charged"), true);
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, @javax.annotation.Nullable net.minecraft.world.level.Level context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.eraphones.charger"));
    }
}
