package com.waylays.arcanes.effect;

import com.waylays.arcanes.ArcanesOublies;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/** Sundered (left by an axe that split the armor): a quarter of the armor and its toughness gone per level. */
public class SunderedEffect extends MobEffect {
    public SunderedEffect() {
        super(MobEffectCategory.HARMFUL, 0x7A6E62);
        addAttributeModifier(Attributes.ARMOR, ArcanesOublies.id("effect.sundered.armor"), -.25, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        addAttributeModifier(Attributes.ARMOR_TOUGHNESS, ArcanesOublies.id("effect.sundered.armor_toughness"), -.25, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    }
}
