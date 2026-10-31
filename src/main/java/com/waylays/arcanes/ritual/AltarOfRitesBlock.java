package com.waylays.arcanes.ritual;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The Altar of Rites: a plinth of dark stone with an open book and two candles. Used, it opens the book of rites (a
 * panel listing every ritual: what it does, how many kneel, how long, how it is drawn, what it asks). From there the
 * altar's own rituals (the Colossus, the Bridge) are prepared round it, their offering given; the others set the
 * Ritual Chalk in hand.
 */
public class AltarOfRitesBlock extends HorizontalDirectionalBlock {
    public static final MapCodec<AltarOfRitesBlock> CODEC = simpleCodec(AltarOfRitesBlock::new);
    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 14, 16);

    public AltarOfRitesBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide) {
            com.waylays.arcanes.client.ritual.AltarClient.open(pos);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        // The candles' flames, and now and then a mote off the book
        Direction f = state.getValue(FACING);
        double[][] candles = {{.1, .62, .1}, {.9, .56, .9}};
        for (double[] c : candles) {
            double x = c[0], z = c[2];
            if (f == Direction.EAST || f == Direction.WEST) {
                double t = x;
                x = 1 - z;
                z = t;
            }
            level.addParticle(ParticleTypes.SMALL_FLAME, pos.getX() + x, pos.getY() + c[1], pos.getZ() + z, 0, 0, 0);
        }
        if (random.nextInt(4) == 0) {
            level.addParticle(ParticleTypes.ENCHANT, pos.getX() + .5, pos.getY() + 1.3, pos.getZ() + .5, (random.nextDouble() - .5) * .6, .3,
                    (random.nextDouble() - .5) * .6);
        }
    }
}
