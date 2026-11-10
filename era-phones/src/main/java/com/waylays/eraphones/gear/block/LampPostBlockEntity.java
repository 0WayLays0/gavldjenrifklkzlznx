package com.waylays.eraphones.gear.block;

import com.waylays.eraphones.gear.Power;
import com.waylays.eraphones.registry.GearRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The lamp's little mind, kept at its foot: night or day, power or none; warming up (a buzz, a few flickers) before it
 * shines, going out with a flicker and a click when the power goes, flickering as long as the power falters.
 */
public class LampPostBlockEntity extends BlockEntity {
    /** Lamps do not all come on at once: each waits up to this many ticks after dusk, by its place. */
    private static final int STAGGER = 500;
    private int flicker;
    private boolean want;

    public LampPostBlockEntity(BlockPos pos, BlockState state) {
        super(GearRegistry.LAMP_POST_BE.get(), pos, state);
    }

    private int offset() {
        return Math.floorMod(worldPosition.hashCode() * 31 + 7, STAGGER);
    }

    private boolean night(Level level) {
        long t = level.getDayTime() % 24000;
        int off = offset();
        return level.dimensionType().hasSkyLight() ? t >= 12600 + off / 2 && t < 23200 + off / 5 : true;
    }

    public void serverTick() {
        Level level = this.level;
        if (level == null || level.getGameTime() % 2 != Math.floorMod(worldPosition.hashCode(), 2)) {
            return;
        }
        BlockPos top = worldPosition.above(2);
        BlockState head = level.getBlockState(top);
        if (!head.is(GearRegistry.LAMP_POST.get()) || head.getValue(LampPostBlock.PART) != 2) {
            return;
        }
        boolean lit = head.getValue(LampPostBlock.LIT);
        RandomSource r = level.random;
        if (Power.flickers(level, worldPosition) && night(level)) {
            // Faltering power: dark, then a stutter of light, then on, with the buzz
            boolean v = r.nextFloat() < .55f;
            if (v != lit) {
                set(level, top, head, v);
            }
            if (r.nextInt(14) == 0) {
                level.playSound(null, top, GearRegistry.LAMP_BUZZ.get(), SoundSource.BLOCKS, .6f, .9f + r.nextFloat() * .2f);
            }
            return;
        }
        boolean should = night(level) && !Power.cut(level, worldPosition);
        if (should != want) {
            want = should;
            flicker = should ? 16 + r.nextInt(12) : 6 + r.nextInt(6);
            level.playSound(null, top, GearRegistry.LAMP_BUZZ.get(), SoundSource.BLOCKS, should ? .7f : .9f, should ? 1f : .8f);
        }
        if (flicker > 0) {
            flicker--;
            boolean v = flicker == 0 ? want : r.nextFloat() < (want ? .35f + .6f * (1 - flicker / 28f) : .45f);
            if (flicker == 0 && !want) {
                level.playSound(null, top, GearRegistry.LAMP_CLICK.get(), SoundSource.BLOCKS, .8f, 1f);
            }
            if (v != lit) {
                set(level, top, head, v);
            }
        } else if (lit != want) {
            set(level, top, head, want);
        }
    }

    private static void set(Level level, BlockPos top, BlockState head, boolean lit) {
        level.setBlock(top, head.setValue(LampPostBlock.LIT, lit), 2);
    }
}
