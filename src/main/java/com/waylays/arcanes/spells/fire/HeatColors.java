package com.waylays.arcanes.spells.fire;

/**
 * The colours of Stellar Fire: six like the stars of each class, red (class M, about 3000 K), orange (K, 4500 K),
 * yellow (G, 5800 K), white (F and A, 8000 K), blue (B, 20000 K) and violet-blue (O, 40000 K and more), and a
 * seventh, green, the flame of burning copper. Real flames take these colours too: red and orange embers, a yellow
 * candle, the white heart of a forge, the blue of burning gas, the violet of potassium, the green of copper. Every
 * colour strikes as hard and costs the same. Pure numbers, safe to read on either side.
 */
public final class HeatColors {
    public static final int TIERS = 7;
    /** The green flame of copper: no star burns that colour. */
    public static final int GREEN = 6;
    /** The white-hot heart of a flame of each heat. */
    public static final int[] CORE = {0xFFC890, 0xFFE2A6, 0xFFF8DC, 0xFFFFFF, 0xEAF2FF, 0xF2E8FF, 0xEAFFD8};
    /** Its body. */
    public static final int[] BODY = {0xF0401C, 0xFF861E, 0xFFCB38, 0xFFF2DC, 0x4F93FF, 0x9B5CFF, 0x3BE05A};
    /** Its tips, where it cools as it rises. */
    public static final int[] TIP = {0x7E1206, 0xB8400A, 0xD8760E, 0xC9D6FF, 0x1C3FD8, 0x4A1AB8, 0x0A6E2C};
    /** The light it throws. */
    public static final int[] GLOW = {0xFF5A28, 0xFF9A3A, 0xFFD860, 0xFFF4E6, 0x6AA8FF, 0xB27AFF, 0x5CFF7A};
    /** How hard every colour strikes, against the spell's own power: the same for all. */
    public static final float POWER = 1.4f;
    /** What every colour costs, against the spell's own mana cost: the same for all. */
    public static final float MANA = 1.3f;
    /** The star class of each colour, and its temperature (green, copper's, has neither: "Cu" and 0). */
    public static final String[] CLASS = {"M", "K", "G", "F", "B", "O", "Cu"};
    public static final int[] KELVIN = {3000, 4500, 5800, 8000, 20000, 40000, 0};
    /** Each colour's name in ids and lang keys. */
    public static final String[] ID = {"red", "orange", "yellow", "white", "blue", "violet", "green"};

    private HeatColors() {}

    public static int clamp(int tier) {
        return Math.max(0, Math.min(TIERS - 1, tier));
    }
}
