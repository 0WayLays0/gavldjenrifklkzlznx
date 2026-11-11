package com.waylays.eraphones;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * The network's reach, in bars from 0 to 4: full under the open sky, less under a roof, less again below ground, and
 * none deep down where no daylight comes (cellars, tunnels, sewers) nor anywhere but the overworld. The 1985 brick
 * loses a bar as soon as it is indoors.
 */
public final class Signal {
    private Signal() {}

    public static int bars(Level level, BlockPos pos, Era era) {
        if (!level.dimension().equals(Level.OVERWORLD) || !level.isLoaded(pos)) {
            return 0;
        }
        int surface = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, pos.getX(), pos.getZ());
        // How many blocks stand over the head
        int depth = surface - (pos.getY() + 2);
        boolean daylight = level.getBrightness(LightLayer.SKY, pos.above()) > 0;
        int bars;
        if (depth <= 0) {
            bars = 4;
        } else if (depth <= 4) {
            bars = daylight ? 3 : 2;
        } else if (depth <= 10) {
            bars = daylight ? 2 : 1;
        } else if (depth <= 18) {
            bars = daylight ? 1 : 0;
        } else {
            bars = 0;
        }
        if (era.weakIndoors && depth > 0) {
            bars--;
        }
        return Mth.clamp(bars, 0, 4);
    }
}
