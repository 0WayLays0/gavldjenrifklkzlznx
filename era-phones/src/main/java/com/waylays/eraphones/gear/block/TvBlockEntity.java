package com.waylays.eraphones.gear.block;

import com.waylays.eraphones.registry.GearRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** A television's place, for its screen to be drawn: what it shows comes from the video recorder beside it. */
public class TvBlockEntity extends BlockEntity {
    public TvBlockEntity(BlockPos pos, BlockState state) {
        super(GearRegistry.TV_BE.get(), pos, state);
    }

    public boolean on() {
        return getBlockState().getValue(TvBlock.LIT);
    }
}
