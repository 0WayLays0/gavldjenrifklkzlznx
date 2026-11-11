package com.waylays.eraphones.lock;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;

/**
 * A card reader on a wall, beside a door: a grey box with a slot and a light. A badge it knows, swiped through it,
 * makes it beep green and opens the doors (and gates, trapdoors) within two blocks for a few seconds; one it does not
 * know, a buzz and red. A game master in creative teaches it a badge by swiping it (again: it forgets it), and
 * crouching with an empty hand empties its list.
 */
public class CardReaderBlock extends HorizontalDirectionalBlock implements EntityBlock {
    public static final MapCodec<CardReaderBlock> CODEC = simpleCodec(CardReaderBlock::new);
    /** Its light: 0 waiting (a small amber), 1 green, 2 red. */
    public static final IntegerProperty LIGHT = IntegerProperty.create("voyant", 0, 2);
    private static final VoxelShape[] SHAPES = new VoxelShape[4];

    static {
        SHAPES[Direction.SOUTH.get2DDataValue()] = Block.box(5, 4, 0, 11, 13, 2);
        SHAPES[Direction.NORTH.get2DDataValue()] = Block.box(5, 4, 14, 11, 13, 16);
        SHAPES[Direction.EAST.get2DDataValue()] = Block.box(0, 4, 5, 2, 13, 11);
        SHAPES[Direction.WEST.get2DDataValue()] = Block.box(14, 4, 5, 16, 13, 11);
    }

    public CardReaderBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.SOUTH).setValue(LIGHT, 0));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, LIGHT);
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

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CardReaderBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide || type != LockRegistry.READER_BE.get() ? null
                : (l, p, s, be) -> ((CardReaderBlockEntity) be).tick((ServerLevel) l);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
                                              BlockHitResult hit) {
        if (!(stack.getItem() instanceof BadgeItem)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof CardReaderBlockEntity r && player instanceof ServerPlayer p) {
            r.swipe(p, stack);
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof CardReaderBlockEntity r && player instanceof ServerPlayer p) {
            r.info(p);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
