package com.waylays.arcanes.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/** Sundered (left by an axe that split the armor): a quarter of the armor and its toughness gone per level. */
public class SunderedEffect extends MobEffect {
    public SunderedEffect() {
        super(MobEffectCategory.HARMFUL, 0x7A6E62);
        addAttributeModifier(Attributes.ARMOR, "6c4f2a1e-58b4-4c1e-9a1d-3f7d2e9b6a11", -.25, AttributeModifier.Operation.MULTIPLY_TOTAL);
        addAttributeModifier(Attributes.ARMOR_TOUGHNESS, "0b9d6e52-1c7a-4f43-8e25-6a4c1d2f8b37", -.25, AttributeModifier.Operation.MULTIPLY_TOTAL);
    }
}
