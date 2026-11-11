package com.waylays.eraphones.gear;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * An instant photograph pinned to a wall (a pin through its white frame, a little askew) or laid on a table, the
 * floor, anything flat. Used, it is looked at closely; struck, it comes off whole, its picture with it. Many side by
 * side make an investigation board. Put there with the photograph in hand, sneaking, on the face to put it on.
 */
public class PinnedPhotoBlock extends BaseEntityBlock {
    public static final MapCodec<PinnedPhotoBlock> CODEC = simpleCodec(PinnedPhotoBlock::new);
    /** The way the picture faces (UP: laid flat on what is under it). */
    public static final DirectionProperty FACING = BlockStateProperties.FACING;
    private static final VoxelShape[] SHAPES = new VoxelShape[6];

    static {
        double t = .6 / 16, a = 4.6 / 16, b = 11.4 / 16, lo = 4 / 16.0, hi = 12 / 16.0;
        SHAPES[Direction.SOUTH.get3DDataValue()] = Shapes.box(a, lo, 0, b, hi, t);
        SHAPES[Direction.NORTH.get3DDataValue()] = Shapes.box(a, lo, 1 - t, b, hi, 1);
        SHAPES[Direction.EAST.get3DDataValue()] = Shapes.box(0, lo, a, t, hi, b);
        SHAPES[Direction.WEST.get3DDataValue()] = Shapes.box(1 - t, lo, a, 1, hi, b);
        SHAPES[Direction.UP.get3DDataValue()] = Shapes.box(3 / 16.0, 0, 3 / 16.0, 13 / 16.0, .4 / 16, 13 / 16.0);
        SHAPES[Direction.DOWN.get3DDataValue()] = Shapes.box(3 / 16.0, 1 - .4 / 16, 3 / 16.0, 13 / 16.0, 1, 13 / 16.0);
    }

    public PinnedPhotoBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.SOUTH));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PinnedPhotoBlockEntity(pos, state);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES[state.getValue(FACING).get3DDataValue()];
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    /** It holds while what it is on is there (anything solid, a table's top included). */
    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockPos behind = pos.relative(state.getValue(FACING).getOpposite());
        return !level.getBlockState(behind).getCollisionShape(level, behind).isEmpty();
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction dir, BlockState other, LevelAccessor level, BlockPos pos, BlockPos otherPos) {
        if (dir == state.getValue(FACING).getOpposite() && !canSurvive(state, level, pos)) {
            return net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, dir, other, level, pos, otherPos);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide && level.getBlockEntity(pos) instanceof PinnedPhotoBlockEntity be && !be.photo().isEmpty()) {
            com.waylays.eraphones.gear.client.PhotoClient.open(be.photo());
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    /** Taken down (or what it was on gone): the photograph falls, whole. */
    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof PinnedPhotoBlockEntity be && !be.photo().isEmpty()) {
            Containers.dropItemStack(level, pos.getX() + .5, pos.getY() + .5, pos.getZ() + .5, be.photo().copy());
        }
        super.onRemove(state, level, pos, newState, moved);
    }

    @Override
    public net.minecraft.world.item.ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
        return level.getBlockEntity(pos) instanceof PinnedPhotoBlockEntity be ? be.photo().copy() : net.minecraft.world.item.ItemStack.EMPTY;
    }
}
