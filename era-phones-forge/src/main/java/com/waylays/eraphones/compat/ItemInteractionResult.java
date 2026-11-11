package com.waylays.eraphones.compat;

import net.minecraft.world.InteractionResult;

/** What using an item on a block came to, as Minecraft 1.21 has it (1.20.1 has a single use() for both). */
public enum ItemInteractionResult {
    SUCCESS, CONSUME, CONSUME_PARTIAL, PASS_TO_DEFAULT_BLOCK_INTERACTION, SKIP_DEFAULT_BLOCK_INTERACTION, FAIL;

    public static ItemInteractionResult sidedSuccess(boolean client) {
        return client ? SUCCESS : CONSUME;
    }

    public InteractionResult result() {
        return switch (this) {
            case SUCCESS -> InteractionResult.SUCCESS;
            case CONSUME -> InteractionResult.CONSUME;
            case CONSUME_PARTIAL -> InteractionResult.CONSUME_PARTIAL;
            case FAIL -> InteractionResult.FAIL;
            default -> InteractionResult.PASS;
        };
    }
}
