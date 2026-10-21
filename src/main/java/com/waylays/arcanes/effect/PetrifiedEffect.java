package com.waylays.arcanes.effect;

import com.waylays.arcanes.ArcanesOublies;
import com.waylays.arcanes.registry.ModEffects;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingEvent;

/** Petrified (the Petrifying Rune): stone up to the knees, no walking and no jumping until it shatters. */
@EventBusSubscriber(modid = ArcanesOublies.MODID)
public class PetrifiedEffect extends MobEffect {
    public PetrifiedEffect() {
        super(MobEffectCategory.HARMFUL, 0x6E7178);
        addAttributeModifier(Attributes.MOVEMENT_SPEED, ArcanesOublies.id("effect.petrified.movement_speed"), -1, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        addAttributeModifier(Attributes.KNOCKBACK_RESISTANCE, ArcanesOublies.id("effect.petrified.knockback_resistance"), .6, AttributeModifier.Operation.ADD_VALUE);
    }

    /** Runs on both sides: a player's own client makes their jumps. */
    @SubscribeEvent
    public static void onJump(LivingEvent.LivingJumpEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.hasEffect(ModEffects.PETRIFIED)) {
            entity.setDeltaMovement(entity.getDeltaMovement().multiply(1, 0, 1));
        }
    }
}
