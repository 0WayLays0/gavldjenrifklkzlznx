package com.waylays.arcanes.client;

import dev.kosmx.playerAnim.api.firstPerson.FirstPersonConfiguration;
import dev.kosmx.playerAnim.api.firstPerson.FirstPersonMode;
import dev.kosmx.playerAnim.api.layered.IAnimation;
import dev.kosmx.playerAnim.api.layered.KeyframeAnimationPlayer;
import dev.kosmx.playerAnim.api.layered.ModifierLayer;
import dev.kosmx.playerAnim.api.layered.modifier.AbstractFadeModifier;
import dev.kosmx.playerAnim.core.data.KeyframeAnimation;
import dev.kosmx.playerAnim.core.util.Ease;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationAccess;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationRegistry;
import io.redspace.ironsspellbooks.api.spells.SpellAnimations;
import io.redspace.ironsspellbooks.config.ClientConfigs;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;

/**
 * Client side: a body animation played on a player outside of a cast, in the same layer and the same way as Iron's
 * casting animations (faded in over two ticks, the arms shown in first person if Iron's client settings say so).
 */
public final class ClientAnimations {
    private ClientAnimations() {}

    public static void play(int entityId, ResourceLocation animation) {
        if (Minecraft.getInstance().level == null || !(Minecraft.getInstance().level.getEntity(entityId) instanceof AbstractClientPlayer player)) {
            return;
        }
        if (!(PlayerAnimationRegistry.getAnimation(animation) instanceof KeyframeAnimation keyframes)) {
            return;
        }
        @SuppressWarnings("unchecked")
        ModifierLayer<IAnimation> layer = (ModifierLayer<IAnimation>) PlayerAnimationAccess.getPlayerAssociatedData(player).get(SpellAnimations.ANIMATION_RESOURCE);
        if (layer == null) {
            return;
        }
        KeyframeAnimationPlayer animationPlayer = new KeyframeAnimationPlayer(keyframes);
        boolean arms = ClientConfigs.SHOW_FIRST_PERSON_ARMS.get();
        boolean items = ClientConfigs.SHOW_FIRST_PERSON_ITEMS.get();
        if (arms || items) {
            animationPlayer.setFirstPersonMode(FirstPersonMode.THIRD_PERSON_MODEL);
            animationPlayer.setFirstPersonConfiguration(new FirstPersonConfiguration(arms, arms, items, items));
        } else {
            animationPlayer.setFirstPersonMode(FirstPersonMode.DISABLED);
        }
        layer.replaceAnimationWithFade(AbstractFadeModifier.standardFadeIn(2, Ease.INOUTSINE), animationPlayer, true);
    }
}
