package com.waylays.arcanes.effect;

import com.waylays.arcanes.ArcanesOublies;
import com.waylays.arcanes.registry.ModEffects;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Pinned (a throwing knife through the foot): no walking and no jumping until it wears off. It can still be knocked
 * about, and it can still fight back.
 */
@Mod.EventBusSubscriber(modid = ArcanesOublies.MODID)
public class PinnedEffect extends MobEffect {
    public PinnedEffect() {
        super(MobEffectCategory.HARMFUL, 0x5E5A66);
        addAttributeModifier(Attributes.MOVEMENT_SPEED, "e1a07c3d-2b6f-4d85-93c4-7f0e5a1b2c48", -1, AttributeModifier.Operation.MULTIPLY_TOTAL);
    }

    /** Runs on both sides: a player's own client makes their jumps. */
    @SubscribeEvent
    public static void onJump(LivingEvent.LivingJumpEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.hasEffect(ModEffects.PINNED.get())) {
            entity.setDeltaMovement(entity.getDeltaMovement().multiply(1, 0, 1));
        }
    }
}
