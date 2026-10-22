package com.waylays.arcanes.effect;

import com.waylays.arcanes.ArcanesOublies;
import com.waylays.arcanes.registry.ModEffects;
import com.waylays.arcanes.registry.ModSchools;
import com.waylays.arcanes.spells.gold.GoldCoat;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;

import javax.annotation.Nullable;

/**
 * Gilding (Dorure): each hit of Gold magic lays a coat of gold on its victim, up to five, the gold climbing from its
 * feet as the coats pile up (see {@link GoldCoat}). Each coat slows it and makes it take more from Gold magic; with all
 * five it is gilded through, and a Midas Curse turns it to a statue at once.
 */
@EventBusSubscriber(modid = ArcanesOublies.MODID)
public class GildedEffect extends MobEffect {
    public static final int MAX_COATS = 5;
    /** Per coat: slower, and more hurt by Gold magic. */
    public static final double SLOW_PER_COAT = .06;
    public static final float BONUS_PER_COAT = .08f;

    public GildedEffect() {
        super(MobEffectCategory.HARMFUL, ModSchools.GOLD_COLOR);
        // Per effect level (amplifier + 1): one coat
        addAttributeModifier(Attributes.MOVEMENT_SPEED, ArcanesOublies.id("effect.gilded.movement_speed"), -SLOW_PER_COAT, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    }

    /** How many coats of gold {@code entity} wears (0 to 5). */
    public static int coats(LivingEntity entity) {
        MobEffectInstance gilded = entity.getEffect(ModEffects.GILDED);
        return gilded == null ? 0 : Math.min(MAX_COATS, gilded.getAmplifier() + 1);
    }

    /** Gold magic bites deeper into gold. */
    @SubscribeEvent
    public static void onHurt(LivingDamageEvent.Pre event) {
        LivingEntity victim = event.getEntity();
        int coats = coats(victim);
        if (coats > 0 && event.getSource().is(ModSchools.GOLD_MAGIC)) {
            event.setNewDamage(event.getNewDamage() * (1 + BONUS_PER_COAT * coats));
        }
    }

    /** The effects the client draws as gold on the body. */
    private static boolean drawn(@Nullable MobEffectInstance effect) {
        return effect != null && (effect.is(ModEffects.GILDED) || effect.is(ModEffects.MIDAS_CURSE) || effect.is(ModEffects.GILDED_ARMOR));
    }

    @SubscribeEvent
    public static void onAdded(MobEffectEvent.Added event) {
        if (drawn(event.getEffectInstance())) {
            GoldCoat.syncLater(event.getEntity());
        }
    }

    @SubscribeEvent
    public static void onRemoved(MobEffectEvent.Remove event) {
        if (drawn(event.getEffectInstance())) {
            GoldCoat.syncLater(event.getEntity());
        }
    }

    @SubscribeEvent
    public static void onExpired(MobEffectEvent.Expired event) {
        if (drawn(event.getEffectInstance())) {
            GoldCoat.syncLater(event.getEntity());
        }
    }
}
