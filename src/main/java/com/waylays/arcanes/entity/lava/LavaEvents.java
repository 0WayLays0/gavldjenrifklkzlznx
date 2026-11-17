package com.waylays.arcanes.entity.lava;

import com.waylays.arcanes.ArcanesOublies;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/** Obsidian Carapace's answer: whoever strikes its wearer in close catches fire. */
@EventBusSubscriber(modid = ArcanesOublies.MODID)
public final class LavaEvents {
    private LavaEvents() {}

    @SubscribeEvent
    public static void onHurt(LivingIncomingDamageEvent event) {
        LivingEntity victim = event.getEntity();
        if (victim.level().isClientSide()) {
            return;
        }
        LavaScene carapace = LavaScene.carapaceOf(victim);
        if (carapace != null && event.getSource().getDirectEntity() instanceof LivingEntity attacker && attacker != victim
                && attacker.distanceToSqr(victim) < 5 * 5) {
            carapace.strikeBack(attacker);
        }
    }
}
