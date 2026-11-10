package com.waylays.eraphones.gear.block;

import com.mojang.serialization.MapCodec;
import com.waylays.eraphones.item.PhoneItem;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
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
 * An answering machine of the time: a beige box with two little cassettes, a red counter, its buttons. Used with a
 * phone in hand, it is linked to that phone's number: calls to it that no one answers are taken by the machine, which
 * beeps and records the caller's message. Used, it plays the messages; crouching, it erases them. Its red light blinks
 * while new messages wait.
 */
public class AnswerMachineBlock extends GearBlock implements EntityBlock {
    public static final MapCodec<AnswerMachineBlock> CODEC = simpleCodec(AnswerMachineBlock::new);
    /** New messages wait (the light blinks). */
    public static final BooleanProperty NEW = BooleanProperty.create("nouveau");

    public AnswerMachineBlock(Properties properties) {
        super(properties, 2.5, 0, 4.5, 13.5, 3.6, 11.5);
        registerDefaultState(defaultBlockState().setValue(NEW, false));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(NEW);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new AnswerMachineBlockEntity(pos, state);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
                                              BlockHitResult hit) {
        if (!(stack.getItem() instanceof PhoneItem)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof AnswerMachineBlockEntity m && player instanceof ServerPlayer sp) {
            String n = PhoneItem.ensure(stack, sp.serverLevel());
            if (n != null) {
                m.link(n, sp);
            }
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof AnswerMachineBlockEntity m && player instanceof ServerPlayer sp) {
            if (player.isSecondaryUseActive()) {
                m.erase(sp);
            } else {
                m.play(sp);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
