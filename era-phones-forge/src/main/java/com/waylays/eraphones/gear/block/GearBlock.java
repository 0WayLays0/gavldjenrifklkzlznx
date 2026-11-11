package com.waylays.eraphones.gear.block;

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

/** A thing set on a table, turned to face whoever puts it down; its shape a box given facing north. */
public abstract class GearBlock extends HorizontalDirectionalBlock {
    private final VoxelShape[] shapes = new VoxelShape[4];

    protected GearBlock(Properties properties, double x0, double y0, double z0, double x1, double y1, double z1) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
        for (Direction d : Direction.Plane.HORIZONTAL) {
            // The box turned with the block (north as given)
            double ax0 = x0, az0 = z0, ax1 = x1, az1 = z1;
            double[] a = switch (d) {
                case SOUTH -> new double[] {16 - ax1, 16 - az1, 16 - ax0, 16 - az0};
                case EAST -> new double[] {16 - az1, ax0, 16 - az0, ax1};
                case WEST -> new double[] {az0, 16 - ax1, az1, 16 - ax0};
                default -> new double[] {ax0, az0, ax1, az1};
            };
            shapes[d.get2DDataValue()] = Shapes.box(a[0] / 16, y0 / 16, a[1] / 16, a[2] / 16, y1 / 16, a[3] / 16);
        }
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
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shapes[state.getValue(FACING).get2DDataValue()];
    }
}
