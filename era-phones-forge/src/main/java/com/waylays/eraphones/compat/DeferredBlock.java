package com.waylays.eraphones.compat;

import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.RegistryObject;

/** A registered block, as NeoForge names it. */
public class DeferredBlock<T extends Block> extends DeferredHolder<Block, T> {
    DeferredBlock(RegistryObject<T> object) {
        super(object);
    }
}
