package com.waylays.arcanes.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/**
 * Stellar Heat: how hot a Stellar Fire caster burns (its level: I red, II orange, III yellow, IV white, V blue, VI
 * violet). Each Stellar Fire spell takes its colour and strength from it and stokes it higher; left alone it cools a
 * step at a time (see {@link com.waylays.arcanes.spells.fire.Heat}).
 */
public class StellarHeatEffect extends MobEffect {
    public StellarHeatEffect() {
        super(MobEffectCategory.BENEFICIAL, 0xFF8A1E);
    }
}
