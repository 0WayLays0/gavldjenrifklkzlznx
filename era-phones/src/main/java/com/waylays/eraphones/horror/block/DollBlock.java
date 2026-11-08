package com.waylays.eraphones.horror.block;

import com.mojang.serialization.MapCodec;
import com.waylays.eraphones.gear.block.GearBlock;
import com.waylays.eraphones.registry.GearRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;

/**
 * A porcelain doll sitting in a lace dress. Its head turns to whoever is nearest, but only while they do not look at
 * it: look back, and it is still.
 */
public class DollBlock extends GearBlock implements EntityBlock {
    public static final MapCodec<DollBlock> CODEC = simpleCodec(DollBlock::new);

    public DollBlock(Properties properties) {
        super(properties, 4, 0, 4, 12, 14, 12);
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new DollBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (!level.isClientSide || type != GearRegistry.DOLL_BE.get()) {
            return null;
        }
        return (l, p, s, be) -> ((DollBlockEntity) be).clientTick();
    }
}
