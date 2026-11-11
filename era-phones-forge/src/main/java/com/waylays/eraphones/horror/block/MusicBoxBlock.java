package com.waylays.eraphones.horror.block;

import com.waylays.eraphones.gear.block.GearBlock;
import com.waylays.eraphones.registry.GearRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import java.util.HashMap;
import java.util.Map;

/**
 * A music box, its lid open, its key at the side. Wound (used), it plays its little tune, a waltz in a minor key that
 * slows as the spring runs down; it cannot be wound again until it has stopped.
 */
public class MusicBoxBlock extends GearBlock {
    /** How long the tune lasts (ticks). */
    public static final int TUNE = 24 * 20;
    private static final Map<Long, Long> PLAYING = new HashMap<>();

    public MusicBoxBlock(Properties properties) {
        super(properties, 4, 0, 5, 12, 7, 11);
    }

        public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level instanceof ServerLevel sl) {
            long now = sl.getGameTime();
            Long until = PLAYING.get(pos.asLong());
            if (until != null && until > now) {
                return InteractionResult.SUCCESS;
            }
            PLAYING.put(pos.asLong(), now + TUNE);
            sl.playSound(null, pos, GearRegistry.MUSIC_BOX.get(), SoundSource.BLOCKS, 1f, 1f);
            sl.sendParticles(ParticleTypes.NOTE, pos.getX() + .5, pos.getY() + .7, pos.getZ() + .5, 1, 0, 0, 0, 0);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    /** 1.20.1's single use(): the item's use first, then the hand's (as 1.21 splits them). */
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, net.minecraft.world.entity.player.Player player,
                                 net.minecraft.world.InteractionHand hand, net.minecraft.world.phys.BlockHitResult hit) {
        return hand == net.minecraft.world.InteractionHand.MAIN_HAND ? useWithoutItem(state, level, pos, player, hit) : InteractionResult.PASS;
    }
}
