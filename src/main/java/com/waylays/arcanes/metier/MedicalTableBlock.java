package com.waylays.arcanes.metier;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The Medical Table: a low wooden table under a white linen cloth marked with a red cross, a roll of bandage, a jar of
 * salve and a stoppered phial on it. The healers' station (its recipes are set in the trades' editor): stitching a
 * wound is its minigame.
 */
public class MedicalTableBlock extends HorizontalDirectionalBlock {
    public static final MapCodec<MedicalTableBlock> CODEC = simpleCodec(MedicalTableBlock::new);
    private static final VoxelShape SHAPE = Shapes.or(Block.box(0, 9, 0, 16, 12, 16), Block.box(1, 0, 1, 3, 9, 3), Block.box(13, 0, 1, 15, 9, 3),
            Block.box(1, 0, 13, 3, 9, 15), Block.box(13, 0, 13, 15, 9, 15), Block.box(2, 12, 2, 14, 16, 14));

    public MedicalTableBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }
}
