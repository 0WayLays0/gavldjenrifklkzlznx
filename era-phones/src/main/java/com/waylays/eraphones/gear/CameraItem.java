package com.waylays.eraphones.gear;

import com.waylays.eraphones.registry.GearRegistry;
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
 * An instant camera of the time. Used, it takes what its holder sees (without their hand or anything on the screen):
 * a flash, the shutter, the motor, and a photograph comes out, pale at first, that develops in a minute. A pack of
 * film holds ten pictures; a pack in the other hand loads it.
 */
public class CameraItem extends Item {
    public static final int SHOTS = 10;

    public CameraItem(Properties properties) {
        super(properties);
    }

    public static int film(ItemStack camera) {
        return camera.getOrDefault(GearRegistry.FILM.get(), SHOTS);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        ItemStack other = player.getItemInHand(hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND);
        // A pack of film in the other hand: loaded
        if (other.is(GearRegistry.FILM_PACK.get()) && film(stack) < SHOTS) {
            if (!level.isClientSide) {
                other.consume(1, player);
                stack.set(GearRegistry.FILM.get(), SHOTS);
                level.playSound(null, player.blockPosition(), SoundEvents.IRON_TRAPDOOR_CLOSE, SoundSource.PLAYERS, .5f, 1.8f);
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }
        if (film(stack) <= 0) {
            if (!level.isClientSide) {
                player.displayClientMessage(Component.translatable("item.eraphones.appareil_photo.empty").withStyle(ChatFormatting.GRAY), true);
            }
            return InteractionResultHolder.fail(stack);
        }
        if (level.isClientSide) {
            com.waylays.eraphones.gear.client.PhotoClient.take(hand == InteractionHand.OFF_HAND);
        }
        player.getCooldowns().addCooldown(this, 30);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.eraphones.appareil_photo.film", film(stack), SHOTS).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.eraphones.appareil_photo.help").withStyle(ChatFormatting.DARK_GRAY));
    }
}
