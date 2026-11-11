package com.waylays.eraphones.walkman;

import com.waylays.eraphones.registry.EraRegistry;
import net.minecraft.world.item.Item;
import com.waylays.eraphones.compat.DeferredHolder;

/** The walkman (its headphones are part of it). */
public final class WalkmanRegistry {
    public static final DeferredHolder<Item, Item> WALKMAN = EraRegistry.ITEMS.register("baladeur", () -> new WalkmanItem(new Item.Properties().stacksTo(1)));

    private WalkmanRegistry() {}

    /** Loads the class (so the item is registered with the others). */
    public static void init() {
    }
}
