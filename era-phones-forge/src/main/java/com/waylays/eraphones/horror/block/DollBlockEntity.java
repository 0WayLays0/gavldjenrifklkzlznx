package com.waylays.eraphones.horror.block;

import com.waylays.eraphones.registry.GearRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** A doll's head, on the players' side: where it looks (degrees from its body's front), and where it looked a tick ago. */
public class DollBlockEntity extends BlockEntity {
    public float yaw, lastYaw, pitch, lastPitch;

    public DollBlockEntity(BlockPos pos, BlockState state) {
        super(GearRegistry.DOLL_BE.get(), pos, state);
    }

    public void clientTick() {
        com.waylays.eraphones.horror.client.DollClient.tick(this);
    }
}
