package com.waylays.eraphones.gear;

import com.waylays.eraphones.registry.GearRegistry;
import net.minecraft.world.item.ItemStack;

/** What the battery-powered things share: a charge (thousandths, full when new) and a switch. */
public final class Gear {
    public static final int FULL = 1000;

    private Gear() {}

    public static int battery(ItemStack stack) {
        return com.waylays.eraphones.registry.EraRegistry.BATTERY.get().getOrDefault(stack, FULL);
    }

    public static void setBattery(ItemStack stack, int value) {
        com.waylays.eraphones.registry.EraRegistry.BATTERY.get().set(stack, Math.max(0, Math.min(FULL, value)));
    }

    public static boolean on(ItemStack stack) {
        return GearRegistry.POWER.get().getOrDefault(stack, false) && battery(stack) > 0 && !broken(stack);
    }

    public static boolean broken(ItemStack stack) {
        return GearRegistry.BROKEN.get().getOrDefault(stack, false);
    }

    public static void setOn(ItemStack stack, boolean on) {
        GearRegistry.POWER.get().set(stack, on);
    }

    public static int channel(ItemStack stack) {
        return Math.max(1, Math.min(16, GearRegistry.CHANNEL.get().getOrDefault(stack, 1)));
    }

    public static int barWidth(ItemStack stack) {
        return Math.round(13f * battery(stack) / FULL);
    }

    public static int barColor(ItemStack stack) {
        float f = battery(stack) / (float) FULL;
        return f < .15f ? 0xFF4A4A : f < .4f ? 0xFFC040 : 0x60E070;
    }
}
