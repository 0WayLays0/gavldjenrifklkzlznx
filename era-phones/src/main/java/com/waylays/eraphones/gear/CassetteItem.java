package com.waylays.eraphones.gear;

import com.waylays.eraphones.registry.GearRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * A video cassette. The game masters record it once (/cassette graver: a sound file put on the server) and no one can
 * change it after; it is played in a video recorder, and keeps where its tape was wound to when ejected.
 */
public class CassetteItem extends Item {
    public CassetteItem(Properties properties) {
        super(properties);
    }

    @Override
    public Component getName(ItemStack stack) {
        String label = stack.get(GearRegistry.TAPE_LABEL.get());
        return label == null || label.isEmpty() ? super.getName(stack) : Component.translatable("item.eraphones.cassette.labeled", label);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        if (stack.get(GearRegistry.TAPE.get()) == null) {
            tooltip.add(Component.translatable("item.eraphones.cassette.blank").withStyle(ChatFormatting.DARK_GRAY));
            return;
        }
        long pos = stack.getOrDefault(GearRegistry.TAPE_POS.get(), 0L);
        tooltip.add(Component.translatable("item.eraphones.cassette.wound", Tape.clock(pos)).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.eraphones.cassette.help").withStyle(ChatFormatting.DARK_GRAY));
    }
}
