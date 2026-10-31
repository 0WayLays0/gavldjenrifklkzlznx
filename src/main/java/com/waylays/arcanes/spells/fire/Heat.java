package com.waylays.arcanes.spells.fire;

import com.waylays.arcanes.ArcanesOublies;
import com.waylays.arcanes.registry.ModEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

/**
 * A caster's heat (0 red to 5 violet, see {@link HeatColors}), carried by the Stellar Heat effect: each Stellar Fire
 * spell is cast at the caster's heat (its colour, its strength) and then stokes it; left alone it cools a step every
 * {@link #COOL} ticks.
 */
@EventBusSubscriber(modid = ArcanesOublies.MODID)
public final class Heat {
    /** Ticks a heat lasts before cooling a step. */
    public static final int COOL = 140;

    private Heat() {}

    /** The caster's heat now (0 if it has none). */
    public static int of(LivingEntity caster) {
        MobEffectInstance heat = caster.getEffect(ModEffects.STELLAR_HEAT);
        return heat == null ? 0 : HeatColors.clamp(heat.getAmplifier());
    }

    /** How much harder its flames strike at its heat now. */
    public static float power(LivingEntity caster) {
        return HeatColors.POWER[of(caster)];
    }

    /** Raises the caster's heat by {@code steps} (it does not go past violet) and keeps it there a while. */
    public static void stoke(LivingEntity caster, int steps) {
        set(caster, of(caster) + steps);
    }

    public static void set(LivingEntity caster, int tier) {
        caster.removeEffect(ModEffects.STELLAR_HEAT);
        caster.addEffect(new MobEffectInstance(ModEffects.STELLAR_HEAT, COOL, HeatColors.clamp(tier), false, false, true));
    }

    /** All its heat spent at once. */
    public static void quench(LivingEntity caster) {
        caster.removeEffect(ModEffects.STELLAR_HEAT);
    }

    /** Left alone, the heat cools a step at a time instead of going out at once. */
    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Post event) {
        Entity entity = event.getEntity();
        if (!(entity instanceof LivingEntity living) || living.level().isClientSide) {
            return;
        }
        MobEffectInstance heat = living.getEffect(ModEffects.STELLAR_HEAT);
        if (heat != null && heat.getDuration() <= 2 && heat.getAmplifier() > 0) {
            set(living, heat.getAmplifier() - 1);
        }
    }
}
