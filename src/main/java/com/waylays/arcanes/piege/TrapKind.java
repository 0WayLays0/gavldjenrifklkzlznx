package com.waylays.arcanes.piege;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The traps, what sets them off and what they do by default (their damage in health points: 2 is a heart):
 * <ul>
 * <li>{@link #SPIKES}: stepped on, iron spikes shoot up from the plate a moment later, then sink back.</li>
 * <li>{@link #BEAR}: stepped on, its jaws snap shut on the leg and hold it a few seconds; then it opens again.</li>
 * <li>{@link #CALTROPS}, {@link #NAILS}: whoever walks over them is pricked, once a second, and slowed.</li>
 * <li>{@link #SAW}: a blade turning in a slot cuts whoever stands on it, twice a second.</li>
 * <li>{@link #FIRE}: stepped on, it hisses, then a column of flame bursts from its grate and sets alight.</li>
 * <li>{@link #GAS}: stepped on, it breathes out a cloud of poison (or of the effects set on it).</li>
 * <li>{@link #SNARE}: stepped on, the noose tightens round the foot and holds it.</li>
 * <li>{@link #WALL}: spikes on a wall, that wound and push back whoever runs into them.</li>
 * <li>{@link #TAR}: sticky tar that bogs down whoever wades in it.</li>
 * <li>{@link #MINE}: a keg of black powder under a loose board: stepped on, the board sinks and lights the fuse; a
 * second later it blows up (the blocks round it are spared).</li>
 * <li>{@link #DARTS}: a gargoyle's face carved in stone that spits darts at whoever passes in front of it (up to ten
 * blocks).</li>
 * </ul>
 */
public enum TrapKind {
    SPIKES("floor_spikes", 6, 40, Block.box(0, 0, 0, 16, 3, 16)),
    BEAR("bear_trap", 8, 60, Block.box(1, 0, 1, 15, 3, 15)),
    CALTROPS("caltrops", 2, 20, Block.box(1, 0, 1, 15, 3, 15)),
    NAILS("nail_board", 3, 20, Block.box(1, 0, 1, 15, 4, 15)),
    SAW("saw_trap", 3, 10, Block.box(0, 0, 0, 16, 4, 16)),
    FIRE("fire_vent", 2, 60, Block.box(0, 0, 0, 16, 4, 16)),
    GAS("gas_vent", 0, 120, Block.box(0, 0, 0, 16, 3, 16)),
    SNARE("snare", 1, 60, Block.box(1, 0, 4, 14, 9, 12)),
    WALL("wall_spikes", 4, 10, Shapes.block()),
    TAR("tar_pit", 0, 20, Block.box(0, 0, 0, 16, 1, 16)),
    MINE("pressure_mine", 4, 200, Block.box(3, 0, 3, 13, 3, 13)),
    DARTS("dart_trap", 4, 30, Shapes.block());

    /** Its id (the block's), its default damage, how long (ticks) before it can strike again, its outline. */
    public final String id;
    public final float damage;
    public final int rest;
    public final VoxelShape shape;

    TrapKind(String id, float damage, int rest, VoxelShape shape) {
        this.id = id;
        this.damage = damage;
        this.rest = rest;
        this.shape = shape;
    }

    /** Whether it lies on the floor (and so is set off by stepping on it). */
    public boolean onFloor() {
        return this != WALL && this != DARTS;
    }

    /** Whether it holds whoever it caught in place while it is shut. */
    public boolean holds() {
        return this == BEAR || this == SNARE;
    }

    /** Whether it strikes whoever is in it again and again (rather than once, when set off). */
    public boolean continuous() {
        return this == CALTROPS || this == NAILS || this == SAW || this == WALL || this == TAR;
    }
}
