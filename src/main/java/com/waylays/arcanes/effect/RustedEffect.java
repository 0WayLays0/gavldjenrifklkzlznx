package com.waylays.arcanes.effect;

import com.waylays.arcanes.ArcanesOublies;
import com.waylays.arcanes.spells.verdigris.VerdigrisFx;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;

/**
 * Rusted (Moss, Copper and Rust Magic): the victim's armour rusts. It protects 15% less per level (Armour and
 * Toughness), and every two seconds each piece worn loses some durability.
 */
public class RustedEffect extends MobEffect {
    public RustedEffect() {
        super(MobEffectCategory.HARMFUL, VerdigrisFx.RUST);
        addAttributeModifier(Attributes.ARMOR, ArcanesOublies.id("effect.rusted.armor"), -.15, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        addAttributeModifier(Attributes.ARMOR_TOUGHNESS, ArcanesOublies.id("effect.rusted.armor_toughness"), -.15, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return duration % 40 == 0;
    }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        if (!entity.level().isClientSide) {
            for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
                ItemStack armour = entity.getItemBySlot(slot);
                if (!armour.isEmpty() && armour.isDamageableItem()) {
                    armour.hurtAndBreak(1 + amplifier, entity, slot);
                }
            }
            VerdigrisFx.rust(entity.level(), entity.position().add(0, entity.getBbHeight() * .5, 0), 4, .3);
        }
        return true;
    }
}
