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

/**
 * A sheet of paper. Using it opens it: blank or a draft, to write on (a title, the text, the paper, the ink, by hand
 * or typed), kept as a draft or signed; signed, it can only be read, and shows who signed it.
 */
public class NoteItem extends Item {
    public NoteItem(Properties properties) {
        super(properties);
    }

    public static boolean signed(ItemStack stack) {
        return stack.get(GearRegistry.NOTE_AUTHOR.get()) != null;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) {
            com.waylays.eraphones.gear.client.GearClient.openNote(hand, stack);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public Component getName(ItemStack stack) {
        String title = stack.get(GearRegistry.NOTE_TITLE.get());
        return title == null || title.isBlank() ? super.getName(stack) : Component.translatable("item.eraphones.feuille.titled", title);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        String author = stack.get(GearRegistry.NOTE_AUTHOR.get());
        if (author != null) {
            tooltip.add(Component.translatable("item.eraphones.feuille.signed", author).withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        } else if (stack.get(GearRegistry.NOTE_TEXT.get()) != null) {
            tooltip.add(Component.translatable("item.eraphones.feuille.draft").withStyle(ChatFormatting.GRAY));
        }
        tooltip.add(Component.translatable("item.eraphones.feuille.help").withStyle(ChatFormatting.DARK_GRAY));
    }
}
