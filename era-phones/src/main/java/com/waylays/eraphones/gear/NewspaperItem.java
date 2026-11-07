package com.waylays.eraphones.gear;

import com.waylays.eraphones.registry.GearRegistry;
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

/** The town's newspaper: printed by the game masters (/journal imprimer), opened and read by using it. */
public class NewspaperItem extends Item {
    public NewspaperItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) {
            com.waylays.eraphones.gear.client.GearClient.openNewspaper(stack.getOrDefault(GearRegistry.NEWS_TITLE.get(), ""),
                    stack.getOrDefault(GearRegistry.NEWS_TEXT.get(), ""));
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        String title = stack.get(GearRegistry.NEWS_TITLE.get());
        if (title != null) {
            tooltip.add(Component.literal("« " + title + " »").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        }
        tooltip.add(Component.translatable("item.eraphones.journal_papier.help").withStyle(ChatFormatting.DARK_GRAY));
    }
}
