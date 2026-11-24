package com.waylays.arcanes.effect;

import com.waylays.arcanes.ArcanesOublies;
import com.waylays.arcanes.registry.ModEffects;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * Crushing Gravity (the Gale): the air weighs down on whoever is under it. They are slowed to a crawl, cannot jump and
 * fall fast; a player is bowed under the weight, crouched, until it lifts (their own game and the server both bow
 * them, so everyone sees it).
 */
@EventBusSubscriber(modid = ArcanesOublies.MODID)
public class CrushingGravityEffect extends MobEffect {
    /** The players this effect bowed (on each side), so that only its own crouch is lifted. */
    private static final Set<Player> BOWED = Collections.newSetFromMap(new WeakHashMap<>());

    public CrushingGravityEffect() {
        super(MobEffectCategory.HARMFUL, 0xB48CFF);
        addAttributeModifier(Attributes.MOVEMENT_SPEED, ArcanesOublies.id("effect.crushing_gravity.speed"), -.6, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        addAttributeModifier(Attributes.JUMP_STRENGTH, ArcanesOublies.id("effect.crushing_gravity.jump"), -1, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        addAttributeModifier(Attributes.GRAVITY, ArcanesOublies.id("effect.crushing_gravity.gravity"), 1.5, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        boolean heavy = player.hasEffect(ModEffects.CRUSHING_GRAVITY) && !player.isSpectator();
        if (heavy && !BOWED.contains(player)) {
            BOWED.add(player);
            player.setForcedPose(Pose.CROUCHING);
        } else if (!heavy && BOWED.remove(player) && player.getForcedPose() == Pose.CROUCHING) {
            player.setForcedPose(null);
        }
    }
}
