package com.waylays.eraphones.gear.block;

import com.mojang.serialization.MapCodec;
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
 * A cathode-ray television of the time in its wooden cabinet, rabbit ears on top. Used, it switches on or off; on, it
 * shows what a video recorder beside it (four blocks round) plays, through a worn picture, or snow when nothing comes.
 */
public class TvBlock extends GearBlock implements EntityBlock {
    public static final MapCodec<TvBlock> CODEC = simpleCodec(TvBlock::new);
    public static final BooleanProperty LIT = BlockStateProperties.LIT;

    public TvBlock(Properties properties) {
        super(properties, 1, 0, 2, 15, 13, 14);
        registerDefaultState(defaultBlockState().setValue(LIT, false));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
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

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide) {
            boolean on = !state.getValue(LIT);
            level.setBlock(pos, state.setValue(LIT, on), 3);
            level.playSound(null, pos, on ? SoundEvents.STONE_BUTTON_CLICK_ON : SoundEvents.STONE_BUTTON_CLICK_OFF, SoundSource.BLOCKS, .5f, on ? .8f : .7f);
            if (on) {
                level.playSound(null, pos, SoundEvents.BEACON_POWER_SELECT, SoundSource.BLOCKS, .15f, 1.9f);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
