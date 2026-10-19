package com.waylays.arcanes.spells.physical;

import com.waylays.arcanes.ArcanesOublies;
import io.redspace.ironsspellbooks.api.spells.SpellAnimations;
import io.redspace.ironsspellbooks.api.util.AnimationHolder;

import java.util.List;

/**
 * The fighters' body animations. Nearly all are the addon's own (assets/arcanes_oublies/player_animation, written by
 * tools/physical_animations.py, keyed on every tick); the plain slash, the rising cut and the two scythe moves are
 * Iron's own, made for its sword and scythe wielding bosses. Each move times its blows on its animation: the tick a
 * blow lands is in the animation's own description.
 */
public final class PhysicalAnimations {
    public static final AnimationHolder PUNCH = own("punch");
    public static final AnimationHolder PUNCH_FLURRY = own("punch_flurry");
    public static final AnimationHolder SLASH_FLURRY = own("slash_flurry");
    public static final AnimationHolder DASH = own("dash");
    public static final AnimationHolder LEAP = own("leap");
    public static final AnimationHolder EVADE = own("evade");
    public static final AnimationHolder SWORD_LUNGE = own("sword_lunge");
    public static final AnimationHolder SWORD_DISARM = own("sword_disarm");
    public static final AnimationHolder DAGGER_STABS = own("dagger_stabs");
    public static final AnimationHolder DAGGER_FINISHER = own("dagger_finisher");
    public static final AnimationHolder SCYTHE_HOOK = own("scythe_hook");
    public static final AnimationHolder HAMMER_CONCUSSION = own("hammer_concussion");
    public static final AnimationHolder HAMMER_QUAKE = own("hammer_quake");
    public static final AnimationHolder HAMMER_RAM = own("hammer_ram");
    public static final AnimationHolder AXE_CLEAVE = own("axe_cleave");
    public static final AnimationHolder AXE_THROW = own("axe_throw");
    public static final AnimationHolder AXE_SUNDER = own("axe_sunder");
    public static final AnimationHolder SPEAR_THRUST = own("spear_thrust");
    public static final AnimationHolder SPEAR_CHARGE = own("spear_charge");
    public static final AnimationHolder SPEAR_JAVELIN = own("spear_javelin");
    public static final AnimationHolder LONGSWORD_CUT = own("longsword_cut");
    public static final AnimationHolder LONGSWORD_SWEEP = own("longsword_sweep");
    public static final AnimationHolder LONGSWORD_GUARD = own("longsword_guard");
    /** Played on its own (not as a cast) when a parry answers a blow. */
    public static final AnimationHolder LONGSWORD_RIPOSTE = own("longsword_riposte");
    public static final AnimationHolder BOW_SHOT = own("bow_shot");
    public static final AnimationHolder BOW_RAIN = own("bow_rain");
    public static final AnimationHolder CROSSBOW_SHOT = own("crossbow_shot");
    public static final AnimationHolder CROSSBOW_BURST = own("crossbow_burst");
    public static final AnimationHolder KNIFE_THROW = own("knife_throw");
    public static final AnimationHolder KNIFE_FAN = own("knife_fan");
    public static final AnimationHolder KNIFE_PIN = own("knife_pin");

    /** Diagonal slash, 0.28 s; the blade crosses at 0.15 s. */
    public static final AnimationHolder SLASH = SpellAnimations.SLASH_ANIMATION;
    /** Rising cut, 0.46 s; the blade sweeps up during the first 0.33 s. */
    public static final AnimationHolder RISING_CUT = irons("simple_sword_upward_swipe");
    /** Scythe wind-up, then a low sweep from left to right at about 1.1 s, 2 s in all. */
    public static final AnimationHolder SCYTHE_SWEEP = irons("scythe_low_rightward_sweep");
    /** Scythe slash at about 0.75 s, then a full spin at about 1.85 s, 2.6 s in all. */
    public static final AnimationHolder SCYTHE_SPIN = irons("scythe_horizontal_slash_spin");

    /** Every animation the moves use, so the client test can check they all load. */
    public static final List<AnimationHolder> ALL = List.of(PUNCH, PUNCH_FLURRY, SLASH_FLURRY, DASH, LEAP, EVADE, SWORD_LUNGE, SWORD_DISARM,
            DAGGER_STABS, DAGGER_FINISHER, SCYTHE_HOOK, HAMMER_CONCUSSION, HAMMER_QUAKE, HAMMER_RAM, AXE_CLEAVE, AXE_THROW, AXE_SUNDER,
            SPEAR_THRUST, SPEAR_CHARGE, SPEAR_JAVELIN, LONGSWORD_CUT, LONGSWORD_SWEEP, LONGSWORD_GUARD, LONGSWORD_RIPOSTE, BOW_SHOT, BOW_RAIN,
            CROSSBOW_SHOT, CROSSBOW_BURST, KNIFE_THROW, KNIFE_FAN, KNIFE_PIN, SLASH, RISING_CUT, SCYTHE_SWEEP, SCYTHE_SPIN);

    private PhysicalAnimations() {}

    private static AnimationHolder own(String name) {
        return new AnimationHolder(ArcanesOublies.MODID + ":" + name, true, true);
    }

    private static AnimationHolder irons(String name) {
        return new AnimationHolder(name, true, true);
    }
}
