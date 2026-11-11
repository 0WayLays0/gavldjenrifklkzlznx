package com.waylays.eraphones.item;

import com.waylays.eraphones.Era;
import com.waylays.eraphones.registry.EraRegistry;
import com.waylays.eraphones.server.LineData;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

/**
 * A phone of one era. It is given its number the first time it is carried (and a full battery); the bar under it in
 * the inventory is its battery. It is opened by using it, or with the phone key while it sits in the hotbar.
 */
public class PhoneItem extends Item {
    public static final int FULL = 1000;
    public final Era era;

    public PhoneItem(Era era, Properties properties) {
        super(properties);
        this.era = era;
    }

    @Nullable
    public static String number(ItemStack stack) {
        return EraRegistry.NUMBER.get().get(stack);
    }

    public static int battery(ItemStack stack) {
        Integer b = EraRegistry.BATTERY.get().get(stack);
        return b == null ? FULL : b;
    }

    public static void setBattery(ItemStack stack, int value) {
        EraRegistry.BATTERY.get().set(stack, Mth.clamp(value, 0, FULL));
    }

    @Nullable
    public static Era era(ItemStack stack) {
        return stack.getItem() instanceof PhoneItem p ? p.era : null;
    }

    /** Gives the phone its number (and a full battery) if it has none yet; returns the number. */
    public static String ensure(ItemStack stack, ServerLevel level) {
        String n = number(stack);
        if (n == null && stack.getItem() instanceof PhoneItem p) {
            n = LineData.get(level.getServer()).newLine(p.era, level.getRandom());
            EraRegistry.NUMBER.get().set(stack, n);
        }
        if (EraRegistry.BATTERY.get().get(stack) == null) {
            EraRegistry.BATTERY.get().set(stack, FULL);
        }
        return n;
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (level instanceof ServerLevel server && (number(stack) == null || EraRegistry.BATTERY.get().get(stack) == null)) {
            ensure(stack, server);
        }
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return number(stack) != null;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.round(13f * battery(stack) / FULL);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        float f = battery(stack) / (float) FULL;
        return f < .15f ? 0xFF4A4A : f < .4f ? 0xFFC040 : 0x60E070;
    }

    @Override
    public void appendHoverText(ItemStack stack, @javax.annotation.Nullable net.minecraft.world.level.Level context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.eraphones." + era.item + ".era", era.year).withStyle(ChatFormatting.GRAY));
        String n = number(stack);
        if (n != null) {
            tooltip.add(Component.translatable("tooltip.eraphones.number", n).withStyle(ChatFormatting.AQUA));
            tooltip.add(Component.translatable("tooltip.eraphones.battery", battery(stack) / 10).withStyle(ChatFormatting.DARK_GREEN));
        }
        tooltip.add(Component.translatable("tooltip.eraphones.open").withStyle(ChatFormatting.DARK_GRAY));
    }
}
