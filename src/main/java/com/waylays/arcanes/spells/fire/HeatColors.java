package com.waylays.arcanes.spells.fire;

/**
 * The six heats of Stellar Fire, coolest to hottest, coloured like the stars of each class: red (class M, about
 * 3000 K), orange (K, 4500 K), yellow (G, 5800 K), white (F and A, 8000 K), blue (B, 20000 K) and violet-blue (O,
 * 40000 K and more). Real flames take these colours too: red and orange embers, a yellow candle, the white heart of
 * a forge, the blue of burning gas, the violet of potassium. Pure numbers, safe to read on either side.
 */
public final class HeatColors {
    public static final int TIERS = 6;
    /** The white-hot heart of a flame of each heat. */
    public static final int[] CORE = {0xFFC890, 0xFFE2A6, 0xFFF8DC, 0xFFFFFF, 0xEAF2FF, 0xF2E8FF};
    /** Its body. */
    public static final int[] BODY = {0xF0401C, 0xFF861E, 0xFFCB38, 0xFFF2DC, 0x4F93FF, 0x9B5CFF};
    /** Its tips, where it cools as it rises. */
    public static final int[] TIP = {0x7E1206, 0xB8400A, 0xD8760E, 0xC9D6FF, 0x1C3FD8, 0x4A1AB8};
    /** The light it throws. */
    public static final int[] GLOW = {0xFF5A28, 0xFF9A3A, 0xFFD860, 0xFFF4E6, 0x6AA8FF, 0xB27AFF};
    /** How much harder a flame of each heat strikes. */
    public static final float[] POWER = {1f, 1.15f, 1.3f, 1.5f, 1.75f, 2.1f};
    /** The star class of each heat, and its temperature. */
    public static final String[] CLASS = {"M", "K", "G", "F", "B", "O"};
    public static final int[] KELVIN = {3000, 4500, 5800, 8000, 20000, 40000};
    /** Each heat's name in ids and lang keys. */
    public static final String[] ID = {"red", "orange", "yellow", "white", "blue", "violet"};

    private HeatColors() {}

    public static int clamp(int tier) {
        return Math.max(0, Math.min(TIERS - 1, tier));
    }
}
