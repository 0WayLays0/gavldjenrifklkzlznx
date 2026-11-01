package com.waylays.arcanes.effect;

import com.waylays.arcanes.registry.ModSchools;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/**
 * Resonance (Echo Magic): sound still ringing in a creature, one level for each echo that struck it (up to five).
 * Harmless by itself; Echo Magic's blows strike harder for it, and some of its spells make it burst (see
 * {@link com.waylays.arcanes.spells.echo.EchoFx}).
 */
public class ResonanceEffect extends MobEffect {
    public ResonanceEffect() {
        super(MobEffectCategory.HARMFUL, ModSchools.ECHO_COLOR);
    }
}
