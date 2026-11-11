package com.waylays.eraphones.compat;

/** The frame's partial tick, as Minecraft 1.21 hands it to the screen's layers. */
public record DeltaTracker(float partialTick) {
    public float getGameTimeDeltaPartialTick(boolean ignored) {
        return partialTick;
    }

    public float getGameTimeDeltaTicks() {
        return 1f;
    }
}
