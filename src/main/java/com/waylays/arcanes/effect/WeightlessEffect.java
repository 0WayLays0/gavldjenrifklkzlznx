package com.waylays.arcanes.effect;

import com.waylays.arcanes.ArcanesOublies;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/** Weightless (caught in a zero-gravity field): gravity turns slightly the wrong way, and it drifts up off the ground. */
public class WeightlessEffect extends MobEffect {
    public WeightlessEffect() {
        super(MobEffectCategory.HARMFUL, 0x9FB4FF);
        addAttributeModifier(Attributes.GRAVITY, ArcanesOublies.id("effect.weightless.gravity"), -1.06, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    }
}
