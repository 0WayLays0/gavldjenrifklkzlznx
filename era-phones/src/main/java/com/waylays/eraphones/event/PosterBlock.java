package com.waylays.eraphones.event;

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
 * Paper stuck on a wall: the missing children's posters, alone or many together. It hangs flat against the face it
 * was put on (facing out of it); put on a floor or a ceiling, it goes on the wall before whoever puts it. Nothing
 * bumps into it.
 */
public class PosterBlock extends HorizontalDirectionalBlock {
    public static final MapCodec<PosterBlock> CODEC = simpleCodec(PosterBlock::new);
    private static final VoxelShape[] SHAPES = new VoxelShape[4];

    static {
        double t = .6 / 16;
        SHAPES[Direction.SOUTH.get2DDataValue()] = Shapes.box(0, 0, 0, 1, 1, t);
        SHAPES[Direction.NORTH.get2DDataValue()] = Shapes.box(0, 0, 1 - t, 1, 1, 1);
        SHAPES[Direction.EAST.get2DDataValue()] = Shapes.box(0, 0, 0, t, 1, 1);
        SHAPES[Direction.WEST.get2DDataValue()] = Shapes.box(1 - t, 0, 0, 1, 1, 1);
    }

    public PosterBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.SOUTH));
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
        Direction face = context.getClickedFace();
        return defaultBlockState().setValue(FACING, face.getAxis().isHorizontal() ? face : context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES[state.getValue(FACING).get2DDataValue()];
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }
}
