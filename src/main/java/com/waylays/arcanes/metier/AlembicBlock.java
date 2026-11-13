package com.waylays.arcanes.metier;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The Alembic: a brick firebox with its embers, a hammered copper boiler on it, the swan neck running down to the coil
 * of the condenser and a glass flask that fills. The station of the alchemists and the healers (its recipes are set in
 * the trades' editor); on its own it only smokes and glows.
 */
public class AlembicBlock extends HorizontalDirectionalBlock {
    public static final MapCodec<AlembicBlock> CODEC = simpleCodec(AlembicBlock::new);
    private static final VoxelShape SHAPE = Shapes.or(Block.box(1, 0, 1, 15, 7, 15), Block.box(3, 7, 3, 13, 15, 13));

    public AlembicBlock(Properties properties) {
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
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        // Embers under the boiler, a wisp of steam off its head, now and then a drop in the flask
        if (random.nextInt(3) == 0) {
            level.addParticle(ParticleTypes.SMALL_FLAME, pos.getX() + .5 + (random.nextDouble() - .5) * .3, pos.getY() + .2,
                    pos.getZ() + .5 + (random.nextDouble() - .5) * .3, 0, .005, 0);
        }
        if (random.nextInt(4) == 0) {
            level.addParticle(ParticleTypes.WHITE_SMOKE, pos.getX() + .5, pos.getY() + 1.02, pos.getZ() + .5, 0, .02, 0);
        }
        if (random.nextInt(14) == 0) {
            level.addParticle(ParticleTypes.DRIPPING_WATER, pos.getX() + .5, pos.getY() + .6, pos.getZ() + .5, 0, 0, 0);
        }
    }
}
