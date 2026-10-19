package com.waylays.arcanes.spells.physical;

import com.waylays.arcanes.ArcanesOublies;
import io.redspace.ironsspellbooks.api.spells.SpellAnimations;
import io.redspace.ironsspellbooks.api.util.AnimationHolder;

import java.util.List;

/**
 * The fighters' body animations. The punches, the slash flurry, the dash and the leap are the addon's own
 * (assets/arcanes_oublies/player_animation, written by tools/physical_assets.py); the sword and scythe moves are
 * Iron's own, made for its sword and scythe wielding bosses. Each move times its blows on its animation.
 */
public final class PhysicalAnimations {
    public static final AnimationHolder PUNCH = own("punch", false);
    public static final AnimationHolder PUNCH_FLURRY = own("punch_flurry", false);
    public static final AnimationHolder SLASH_FLURRY = own("slash_flurry", false);
    public static final AnimationHolder DASH = own("dash", true);
    public static final AnimationHolder LEAP = own("leap", true);

    /** Diagonal slash, 0.28 s; the blade crosses at 0.15 s. */
    public static final AnimationHolder SLASH = SpellAnimations.SLASH_ANIMATION;
    /** Rising cut, 0.46 s; the blade sweeps up during the first 0.33 s. */
    public static final AnimationHolder RISING_CUT = irons("simple_sword_upward_swipe");
    /** Lunging thrust, 0.42 s; the blade drives forward from 0.21 s to 0.42 s. */
    public static final AnimationHolder LUNGE = irons("simple_sword_lunge_stab");
    /** Two quick alternating thrusts, 0.46 s. */
    public static final AnimationHolder QUICK_STABS = irons("simple_sword_stab_alternate");
    /** Scythe wind-up, then a low sweep from left to right at about 1.1 s, 2 s in all. */
    public static final AnimationHolder SCYTHE_SWEEP = irons("scythe_low_rightward_sweep");
    /** Scythe slash at about 0.75 s, then a full spin at about 1.85 s, 2.6 s in all. */
    public static final AnimationHolder SCYTHE_SPIN = irons("scythe_horizontal_slash_spin");

    /** Every animation the moves use, so the client test can check they all load. */
    public static final List<AnimationHolder> ALL = List.of(PUNCH, PUNCH_FLURRY, SLASH_FLURRY, DASH, LEAP, SLASH, RISING_CUT, LUNGE, QUICK_STABS,
            SCYTHE_SWEEP, SCYTHE_SPIN);

    private PhysicalAnimations() {}

    private static AnimationHolder own(String name, boolean animatesLegs) {
        return new AnimationHolder(ArcanesOublies.MODID + ":" + name, true, animatesLegs);
    }

    private static AnimationHolder irons(String name) {
        return new AnimationHolder(name, true, true);
    }
}
