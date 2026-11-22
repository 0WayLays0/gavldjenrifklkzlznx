package com.waylays.cinematic.studio;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/** A block the actor placed or broke during its take. */
public record BlockChange(int tick, BlockPos pos, BlockState before, BlockState after) {
    public void write(FriendlyByteBuf buf) {
        buf.writeVarInt(tick);
        buf.writeBlockPos(pos);
        buf.writeNbt(NbtUtils.writeBlockState(before));
        buf.writeNbt(NbtUtils.writeBlockState(after));
    }

    public static BlockChange read(FriendlyByteBuf buf) {
        int tick = buf.readVarInt();
        BlockPos pos = buf.readBlockPos();
        return new BlockChange(tick, pos, state(buf.readNbt()), state(buf.readNbt()));
    }

    private static BlockState state(CompoundTag tag) {
        return tag == null ? Blocks.AIR.defaultBlockState() : NbtUtils.readBlockState(BuiltInRegistries.BLOCK.asLookup(), tag);
    }
}
