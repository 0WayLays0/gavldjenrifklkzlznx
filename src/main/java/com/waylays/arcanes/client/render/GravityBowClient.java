package com.waylays.arcanes.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.waylays.arcanes.ArcanesOublies;
import com.waylays.arcanes.effect.CrushingGravityEffect;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLivingEvent;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * Creatures under Crushing Gravity, crushed down: squashed under the weight, straining against it, and a humanoid
 * (zombie, skeleton, villager...) bent over as a crouching player is. Players are bowed for real, by their pose
 * ({@link CrushingGravityEffect}).
 */
@EventBusSubscriber(modid = ArcanesOublies.MODID, value = Dist.CLIENT)
public final class GravityBowClient {
    /** Those drawn crushed this frame, to be set straight once drawn. */
    private static final Set<LivingEntity> CRUSHED = Collections.newSetFromMap(new WeakHashMap<>());

    private GravityBowClient() {}

    @SubscribeEvent
    public static void before(RenderLivingEvent.Pre<?, ?> event) {
        LivingEntity e = event.getEntity();
        if (e instanceof Player || !CrushingGravityEffect.isHeavy(e)) {
            return;
        }
        PoseStack poseStack = event.getPoseStack();
        poseStack.pushPose();
        float t = e.tickCount + event.getPartialTick();
        // Pressed flat, then straining back up a little, over and over
        float squash = .8f + .04f * Mth.sin(t * .5f);
        float spread = 1 + (1 - squash) * .45f;
        poseStack.scale(spread, squash, spread);
        if (event.getRenderer().getModel() instanceof HumanoidModel<?> humanoid) {
            humanoid.crouching = true;
            // Down as far as a crouching player goes
            poseStack.translate(0, -.125f, 0);
        }
        CRUSHED.add(e);
    }

    @SubscribeEvent
    public static void after(RenderLivingEvent.Post<?, ?> event) {
        if (!CRUSHED.remove(event.getEntity())) {
            return;
        }
        if (event.getRenderer().getModel() instanceof HumanoidModel<?> humanoid) {
            humanoid.crouching = false;
        }
        event.getPoseStack().popPose();
    }
}
