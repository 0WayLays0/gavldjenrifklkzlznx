package com.waylays.arcanes.effect;

import com.waylays.arcanes.ArcanesOublies;
import com.waylays.arcanes.registry.ModEffects;
import net.minecraft.core.Holder;
import net.minecraft.network.protocol.game.ClientboundRemoveMobEffectPacket;
import net.minecraft.network.protocol.game.ClientboundUpdateMobEffectPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * Crushing Gravity (the Gale): the air weighs down on whoever is under it. They are slowed to a crawl, cannot jump and
 * fall fast; a player is bowed under the weight, crouched, until it lifts, and a creature is crushed down (drawn by
 * {@code GravityBowClient}).
 * <p>
 * The game tells only a player of their own effects: this one is also sent to everyone who can see its bearer, so
 * that every game shows them bowed, not only theirs.
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

    private static boolean isThis(MobEffectInstance effect) {
        return effect != null && effect.getEffect().value() == ModEffects.CRUSHING_GRAVITY.get();
    }

    /**
     * Whether {@code e} is under the weight now, on either side. Looked for among its effects by what they are: one
     * the client was sent is held under another handle than the registry's, which a lookup by handle can miss; and a
     * client copy whose time ran out stays until the server says it is gone.
     */
    public static boolean isHeavy(LivingEntity e) {
        for (MobEffectInstance effect : e.getActiveEffects()) {
            if (isThis(effect)) {
                return effect.getDuration() != 0;
            }
        }
        return false;
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        boolean heavy = isHeavy(player) && !player.isSpectator();
        if (heavy && !BOWED.contains(player)) {
            BOWED.add(player);
            player.setForcedPose(Pose.CROUCHING);
        } else if (!heavy && BOWED.remove(player) && player.getForcedPose() == Pose.CROUCHING) {
            player.setForcedPose(null);
        }
    }

    /** Sent to everyone around when it falls on someone, and again now and then while it is renewed. */
    @SubscribeEvent
    public static void onAdded(MobEffectEvent.Added event) {
        MobEffectInstance effect = event.getEffectInstance();
        LivingEntity e = event.getEntity();
        if (!isThis(effect) || !(e.level() instanceof ServerLevel server)) {
            return;
        }
        if (event.getOldEffectInstance() == null || e.tickCount % 10 == 0) {
            server.getChunkSource().broadcast(e, new ClientboundUpdateMobEffectPacket(e.getId(), effect, false));
        }
    }

    @SubscribeEvent
    public static void onRemoved(MobEffectEvent.Remove event) {
        if (event.getEffect().value() == ModEffects.CRUSHING_GRAVITY.get()) {
            lifted(event.getEntity(), event.getEffect());
        }
    }

    @SubscribeEvent
    public static void onExpired(MobEffectEvent.Expired event) {
        if (isThis(event.getEffectInstance())) {
            lifted(event.getEntity(), event.getEffectInstance().getEffect());
        }
    }

    private static void lifted(LivingEntity e, Holder<MobEffect> effect) {
        if (e.level() instanceof ServerLevel server) {
            server.getChunkSource().broadcast(e, new ClientboundRemoveMobEffectPacket(e.getId(), effect));
        }
    }
}
