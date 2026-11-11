package com.waylays.eraphones.gear.block;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;

import javax.annotation.Nullable;

/**
 * A cathode-ray television of the time in its wooden cabinet, rabbit ears on top, a video player built in. Used with a
 * cassette, it takes it and plays it; used, it switches on or off (pausing the tape); crouching, the cassette comes
 * out. Without one of its own it shows what a video recorder beside it (four blocks round) plays; or snow.
 */
public class TvBlock extends GearBlock implements EntityBlock {
    public static final BooleanProperty LIT = BlockStateProperties.LIT;

    public TvBlock(Properties properties) {
        super(properties, 1, 0, 2, 15, 13, 14);
        registerDefaultState(defaultBlockState().setValue(LIT, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(LIT);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TvBlockEntity(pos, state);
    }

        public com.waylays.eraphones.compat.ItemInteractionResult useItemOn(net.minecraft.world.item.ItemStack stack, BlockState state, Level level, BlockPos pos,
            Player player, net.minecraft.world.InteractionHand hand, BlockHitResult hit) {
        if (!(stack.getItem() instanceof com.waylays.eraphones.gear.CassetteItem)) {
            return com.waylays.eraphones.compat.ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof TvBlockEntity tv) {
            if (tv.hasCassette()) {
                player.displayClientMessage(net.minecraft.network.chat.Component.translatable("gear.eraphones.vcr_full")
                        .withStyle(net.minecraft.ChatFormatting.GRAY), true);
            } else {
                tv.insert(stack.split(1));
                level.setBlock(pos, state.setValue(LIT, true), 3);
                level.playSound(null, pos, SoundEvents.IRON_TRAPDOOR_CLOSE, SoundSource.BLOCKS, .35f, 1.7f);
            }
        }
        return com.waylays.eraphones.compat.ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof TvBlockEntity tv) {
            net.minecraft.world.item.ItemStack tape = tv.takeCassette();
            if (!tape.isEmpty()) {
                net.minecraft.world.Containers.dropItemStack(level, pos.getX() + .5, pos.getY() + .3, pos.getZ() + .5, tape);
            }
        }
        super.onRemove(state, level, pos, newState, moved);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> net.minecraft.world.level.block.entity.BlockEntityTicker<T> getTicker(Level level, BlockState state,
            net.minecraft.world.level.block.entity.BlockEntityType<T> type) {
        if (level.isClientSide || type != com.waylays.eraphones.registry.GearRegistry.TV_BE.get()) {
            return null;
        }
        return (l, p, s, be) -> ((TvBlockEntity) be).serverTick();
    }

        public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide && player.isShiftKeyDown() && level.getBlockEntity(pos) instanceof TvBlockEntity tv && tv.hasCassette()) {
            // Crouching: the cassette comes out
            net.minecraft.world.item.ItemStack tape = tv.takeCassette();
            if (!player.getInventory().add(tape)) {
                player.drop(tape, false);
            }
            level.playSound(null, pos, SoundEvents.IRON_TRAPDOOR_OPEN, SoundSource.BLOCKS, .35f, 1.8f);
            return InteractionResult.SUCCESS;
        }
        if (!level.isClientSide) {
            boolean on = !state.getValue(LIT);
            level.setBlock(pos, state.setValue(LIT, on), 3);
            if (level.getBlockEntity(pos) instanceof TvBlockEntity tv) {
                tv.power(on);
            }
            level.playSound(null, pos, on ? SoundEvents.STONE_BUTTON_CLICK_ON : SoundEvents.STONE_BUTTON_CLICK_OFF, SoundSource.BLOCKS, .5f, on ? .8f : .7f);
            if (on) {
                level.playSound(null, pos, SoundEvents.BEACON_POWER_SELECT, SoundSource.BLOCKS, .15f, 1.9f);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    /** 1.20.1's single use(): the item's use first, then the hand's (as 1.21 splits them). */
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, net.minecraft.world.entity.player.Player player,
                                 net.minecraft.world.InteractionHand hand, net.minecraft.world.phys.BlockHitResult hit) {
        com.waylays.eraphones.compat.ItemInteractionResult r = useItemOn(player.getItemInHand(hand), state, level, pos, player, hand, hit);
        if (r != com.waylays.eraphones.compat.ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION) {
            return r.result();
        }
        return hand == net.minecraft.world.InteractionHand.MAIN_HAND ? useWithoutItem(state, level, pos, player, hit) : InteractionResult.PASS;
    }
}
