package com.waylays.eraphones.compat;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** Little things Minecraft 1.21 has that 1.20.1 has not. */
public final class Compat {
    private Compat() {}

    /** Takes {@code n} off the stack, unless its holder is a player in creative. */
    public static void consume(ItemStack stack, int n, LivingEntity holder) {
        if (!(holder instanceof Player p) || !p.getAbilities().instabuild) {
            stack.shrink(n);
        }
    }

    public static ResourceLocation rl(String namespace, String path) {
        return new ResourceLocation(namespace, path);
    }
}
