package com.waylays.arcanes.registry;

import com.waylays.arcanes.ArcanesOublies;
import com.waylays.arcanes.ritual.AltarOfRitesBlock;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** The addon's blocks. */
public final class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(Registries.BLOCK, ArcanesOublies.MODID);

    /** The Altar of Rites: opens the book of rites; the altar's own rituals are prepared round it. */
    public static final DeferredHolder<Block, Block> ALTAR_OF_RITES = BLOCKS.register("altar_of_rites",
            () -> new AltarOfRitesBlock(BlockBehaviour.Properties.of().mapColor(MapColor.DEEPSLATE).strength(3.5f, 9).sound(SoundType.DEEPSLATE_TILES)
                    .lightLevel(state -> 7).noOcclusion()));

    private ModBlocks() {}
}
