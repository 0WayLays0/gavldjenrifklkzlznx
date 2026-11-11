package com.waylays.eraphones.event;

import com.waylays.eraphones.gear.block.GearBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;

import javax.annotation.Nullable;

/**
 * An old steel strongbox with four brass number wheels on its front. Locked, used, it shows its wheels to turn: the
 * right code opens it (a red balloon rising out of it, if the game master wants one) and what it holds can be taken.
 * A game master in creative sets its code on the wheels themselves, fills it crouching, and closes it again with
 * /boitecode.
 */
public class CodeBoxBlock extends GearBlock implements EntityBlock {
    public static final BooleanProperty OPEN = BooleanProperty.create("ouverte");

    public CodeBoxBlock(Properties properties) {
        super(properties, 2.5, 0, 4, 13.5, 6.4, 12);
        registerDefaultState(defaultBlockState().setValue(OPEN, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(OPEN);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CodeBoxBlockEntity(pos, state);
    }

        public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof CodeBoxBlockEntity box && player instanceof ServerPlayer sp) {
            box.use(sp);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof CodeBoxBlockEntity box) {
            Containers.dropContents(level, pos, box.items());
        }
        super.onRemove(state, level, pos, newState, moved);
    }

    /** 1.20.1's single use(): the item's use first, then the hand's (as 1.21 splits them). */
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, net.minecraft.world.entity.player.Player player,
                                 net.minecraft.world.InteractionHand hand, net.minecraft.world.phys.BlockHitResult hit) {
        return hand == net.minecraft.world.InteractionHand.MAIN_HAND ? useWithoutItem(state, level, pos, player, hit) : InteractionResult.PASS;
    }
}
