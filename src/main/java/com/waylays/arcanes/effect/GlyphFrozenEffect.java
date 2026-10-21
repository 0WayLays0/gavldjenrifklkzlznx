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

/** Frozen solid (the Freeze reaction of Glyph Magic): no walking and no jumping until the ice gives. */
@EventBusSubscriber(modid = ArcanesOublies.MODID)
public class GlyphFrozenEffect extends MobEffect {
    public GlyphFrozenEffect() {
        super(MobEffectCategory.HARMFUL, 0xB8F0FF);
        addAttributeModifier(Attributes.MOVEMENT_SPEED, ArcanesOublies.id("effect.glyph_frozen.movement_speed"), -1, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    }

    /** Runs on both sides: a player's own client makes their jumps. */
    @SubscribeEvent
    public static void onJump(LivingEvent.LivingJumpEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.hasEffect(ModEffects.GLYPH_FROZEN)) {
            entity.setDeltaMovement(entity.getDeltaMovement().multiply(1, 0, 1));
        }
    }
}
