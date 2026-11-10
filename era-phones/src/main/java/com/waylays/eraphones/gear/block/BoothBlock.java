package com.waylays.eraphones.gear.block;

import com.mojang.serialization.MapCodec;
import com.waylays.eraphones.gear.BoothNet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;

/**
 * A phone booth, two blocks tall and open at the front: a payphone inside on its back wall, a lit sign on its roof. It
 * has a number of its own (555-9xxx) on the phone network: a coin of 25 cents calls any phone from it, and when it is
 * called it rings out loud in the street until someone picks up its handset (uses it).
 */
public class BoothBlock extends HorizontalDirectionalBlock implements EntityBlock {
    public static final MapCodec<BoothBlock> CODEC = simpleCodec(BoothBlock::new);
    public static final EnumProperty<DoubleBlockHalf> HALF = BlockStateProperties.DOUBLE_BLOCK_HALF;
    /** Whether the booth has power (a power cut leaves it dark and dead). */
    public static final net.minecraft.world.level.block.state.properties.BooleanProperty POWERED = BlockStateProperties.POWERED;
    /** Its shapes by half (0 lower, 1 upper), then by facing; the boxes given for a booth open to the south. */
    private static final VoxelShape[][] SHAPES = new VoxelShape[2][4];

    static {
        double[][] lower = {{0, 0, 0, 16, 1, 16}, {0, 1, 0, 16, 16, 1.2}, {0, 1, 0, 1.2, 16, 16}, {14.8, 1, 0, 16, 16, 16},
                {2.5, 9.6, 1, 13.5, 10.1, 5.6}, {3.2, 12.4, 1, 11.4, 16, 3.8}};
        // The roof a little above the lamp, so that a player fits inside under it
        double[][] upper = {{0, 0, 0, 16, 14, 1.2}, {0, 0, 0, 1.2, 14, 16}, {14.8, 0, 0, 16, 14, 16}, {0, 14, 0, 16, 16, 16},
                {3.2, 0, 1, 11.4, 6.6, 3.8}};
        for (Direction d : Direction.Plane.HORIZONTAL) {
            SHAPES[0][d.get2DDataValue()] = shape(lower, d);
            SHAPES[1][d.get2DDataValue()] = shape(upper, d);
        }
    }

    private static VoxelShape shape(double[][] boxes, Direction facing) {
        VoxelShape s = Shapes.empty();
        for (double[] b : boxes) {
            double x0 = b[0], z0 = b[2], x1 = b[3], z1 = b[5];
            // Turned as the model is turned: west 90, north 180, east 270 degrees
            double[] a = switch (facing) {
                case WEST -> new double[] {16 - z1, x0, 16 - z0, x1};
                case NORTH -> new double[] {16 - x1, 16 - z1, 16 - x0, 16 - z0};
                case EAST -> new double[] {z0, 16 - x1, z1, 16 - x0};
                default -> new double[] {x0, z0, x1, z1};
            };
            s = Shapes.join(s, Shapes.box(a[0] / 16, b[1] / 16, a[1] / 16, a[2] / 16, b[4] / 16, a[3] / 16), BooleanOp.OR);
        }
        return s.optimize();
    }

    public BoothBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.SOUTH).setValue(HALF, DoubleBlockHalf.LOWER)
                .setValue(POWERED, true));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, HALF, POWERED);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockPos pos = context.getClickedPos();
        Level level = context.getLevel();
        if (pos.getY() >= level.getMaxBuildHeight() - 1 || !level.getBlockState(pos.above()).canBeReplaced(context)) {
            return null;
        }
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite()).setValue(HALF, DoubleBlockHalf.LOWER);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        level.setBlock(pos.above(), state.setValue(HALF, DoubleBlockHalf.UPPER), 3);
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction dir, BlockState neighbour, LevelAccessor level, BlockPos pos, BlockPos neighbourPos) {
        DoubleBlockHalf half = state.getValue(HALF);
        if (dir.getAxis() == Direction.Axis.Y && (half == DoubleBlockHalf.LOWER) == (dir == Direction.UP)) {
            return neighbour.is(this) && neighbour.getValue(HALF) != half ? state : Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, dir, neighbour, level, pos, neighbourPos);
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        // Broken from above by a creative player: the lower half goes without dropping the booth
        if (!level.isClientSide && player.isCreative() && state.getValue(HALF) == DoubleBlockHalf.UPPER) {
            BlockPos below = pos.below();
            BlockState b = level.getBlockState(below);
            if (b.is(this) && b.getValue(HALF) == DoubleBlockHalf.LOWER) {
                level.setBlock(below, Blocks.AIR.defaultBlockState(), 35);
                level.levelEvent(player, 2001, below, Block.getId(b));
            }
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES[state.getValue(HALF) == DoubleBlockHalf.LOWER ? 0 : 1][state.getValue(FACING).get2DDataValue()];
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide && player instanceof ServerPlayer sp) {
            BlockPos base = state.getValue(HALF) == DoubleBlockHalf.UPPER ? pos.below() : pos;
            if (level.getBlockEntity(base) instanceof BoothBlockEntity booth) {
                BoothNet.use(sp, booth);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return state.getValue(HALF) == DoubleBlockHalf.LOWER ? new BoothBlockEntity(pos, state) : null;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> net.minecraft.world.level.block.entity.BlockEntityTicker<T> getTicker(Level level, BlockState state,
            net.minecraft.world.level.block.entity.BlockEntityType<T> type) {
        if (level.isClientSide || type != com.waylays.eraphones.registry.GearRegistry.BOOTH_BE.get()) {
            return null;
        }
        return (l, p, s, be) -> ((BoothBlockEntity) be).serverTick();
    }
}
