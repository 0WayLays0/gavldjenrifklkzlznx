package com.waylays.eraphones.lock;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/** A key: it opens the padlocks made with it (its number written on its tag). */
public class KeyItem extends Item {
    public KeyItem(Properties properties) {
        super(properties);
    }

    public static String id(ItemStack stack) {
        return LockRegistry.KEY.get().get(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, @javax.annotation.Nullable net.minecraft.world.level.Level context, List<Component> tooltip, TooltipFlag flag) {
        String id = id(stack);
        tooltip.add(id == null ? Component.translatable("lock.eraphones.key_blank").withStyle(ChatFormatting.DARK_GRAY)
                : Component.translatable("lock.eraphones.key_number", id).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("lock.eraphones.key_help").withStyle(ChatFormatting.DARK_GRAY));
    }
}
