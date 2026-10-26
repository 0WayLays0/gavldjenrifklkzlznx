package com.waylays.arcanes.effect;

import com.waylays.arcanes.ArcanesOublies;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/** Low Gravity (the caster's side in a zero-gravity field): the moon's gravity, great bounding leaps and soft landings. */
public class LowGravityEffect extends MobEffect {
    public LowGravityEffect() {
        super(MobEffectCategory.BENEFICIAL, 0xDDE6FF);
        addAttributeModifier(Attributes.GRAVITY, ArcanesOublies.id("effect.low_gravity.gravity"), -.8, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        addAttributeModifier(Attributes.SAFE_FALL_DISTANCE, ArcanesOublies.id("effect.low_gravity.safe_fall_distance"), 16, AttributeModifier.Operation.ADD_VALUE);
    }
}
