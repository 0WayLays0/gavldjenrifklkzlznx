package com.waylays.eraphones.gear.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;

/**
 * A street lamp of the time, three blocks tall: a grey pole on its concrete foot, an arm reaching over the street
 * and the long lamp head at its end, its sodium glass glowing orange at night. The lamps come on one after another
 * as night falls, buzzing and flickering while they warm up, and go out at dawn; a power cut puts them out.
 */
public class LampPostBlock extends HorizontalDirectionalBlock implements EntityBlock {
    public static final MapCodec<LampPostBlock> CODEC = simpleCodec(LampPostBlock::new);
    /** Which block of the three: 0 the foot, 1 the middle, 2 the top with the arm and the lamp. */
    public static final IntegerProperty PART = IntegerProperty.create("part", 0, 2);
    public static final BooleanProperty LIT = BlockStateProperties.LIT;
    private static final VoxelShape POLE = Block.box(6, 0, 6, 10, 16, 10), FOOT = Block.box(4.5, 0, 4.5, 11.5, 3, 11.5);

    public LampPostBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.SOUTH).setValue(PART, 0).setValue(LIT, false));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, PART, LIT);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        BlockPos pos = ctx.getClickedPos();
        Level level = ctx.getLevel();
        if (pos.getY() >= level.getMaxBuildHeight() - 2) {
            return null;
        }
        for (int k = 1; k <= 2; k++) {
            if (!level.getBlockState(pos.above(k)).canBeReplaced(ctx)) {
                return null;
            }
        }
        // The arm reaches the way its placer looks
        return defaultBlockState().setValue(FACING, ctx.getHorizontalDirection());
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        level.setBlock(pos.above(), state.setValue(PART, 1), 3);
        level.setBlock(pos.above(2), state.setValue(PART, 2), 3);
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction dir, BlockState neighbour, LevelAccessor level, BlockPos pos, BlockPos neighbourPos) {
        int part = state.getValue(PART);
        boolean needUp = part < 2 && dir == Direction.UP, needDown = part > 0 && dir == Direction.DOWN;
        if (needUp || needDown) {
            int want = needUp ? part + 1 : part - 1;
            if (!neighbour.is(this) || neighbour.getValue(PART) != want) {
                return Blocks.AIR.defaultBlockState();
            }
        }
        return super.updateShape(state, dir, neighbour, level, pos, neighbourPos);
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        // Broken from above by a creative player: the foot goes without dropping the lamp
        if (!level.isClientSide && player.isCreative() && state.getValue(PART) > 0) {
            BlockPos foot = pos.below(state.getValue(PART));
            BlockState f = level.getBlockState(foot);
            if (f.is(this) && f.getValue(PART) == 0) {
                level.setBlock(foot, Blocks.AIR.defaultBlockState(), 35);
            }
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(PART) == 0 ? Shapes.or(FOOT, POLE) : POLE;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return state.getValue(PART) == 0 ? new LampPostBlockEntity(pos, state) : null;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide || type != com.waylays.eraphones.registry.GearRegistry.LAMP_POST_BE.get()) {
            return null;
        }
        return (l, p, s, be) -> ((LampPostBlockEntity) be).serverTick();
    }
}
