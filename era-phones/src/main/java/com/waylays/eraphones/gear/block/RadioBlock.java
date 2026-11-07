package com.waylays.eraphones.gear.block;

import com.mojang.serialization.MapCodec;
import com.waylays.eraphones.registry.GearRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import javax.annotation.Nullable;

/**
 * A table radio of the time: a wooden case, a speaker behind its cloth, a lit dial. Opened by using it (switch,
 * frequency, volume); switched on, it hisses between stations and speaks whatever is on the air on its frequency.
 */
public class RadioBlock extends GearBlock implements EntityBlock {
    public static final MapCodec<RadioBlock> CODEC = simpleCodec(RadioBlock::new);

    public RadioBlock(Properties properties) {
        super(properties, 2, 0, 5, 14, 10, 12);
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new RadioBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide) {
            com.waylays.eraphones.gear.client.GearClient.openRadio(pos);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (!level.isClientSide || type != GearRegistry.RADIO_BE.get()) {
            return null;
        }
        return (l, p, s, be) -> com.waylays.eraphones.gear.client.GearClient.radioTick((RadioBlockEntity) be);
    }
}
