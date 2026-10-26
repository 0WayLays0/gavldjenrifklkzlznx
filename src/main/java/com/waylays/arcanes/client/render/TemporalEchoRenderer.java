package com.waylays.arcanes.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.waylays.arcanes.entity.cosmos.TemporalEcho;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;

/**
 * An echo in time: the creature it echoes drawn where the echo stands, in scanlines of its light, flickering as it
 * fades (and slipping sideways now and then, like a bad signal).
 */
public class TemporalEchoRenderer extends EntityRenderer<TemporalEcho> {
    public TemporalEchoRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0;
    }

    @Override
    public void render(TemporalEcho echo, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        Entity target = echo.getTarget();
        if (target == null || target == echo) {
            return;
        }
        float age = echo.tickCount + partialTicks;
        float fade = 1 - Mth.clamp(age / Math.max(1, echo.getLife()), 0, 1);
        float flicker = .75f + .25f * Mth.sin(age * 2.9f) * Mth.sin(age * 1.3f + echo.getId());
        poseStack.pushPose();
        // A glitch: a sideways slip for a tick, now and then
        if (RuneRender.hash(echo.getId() * 31 + (int) age) < .12f) {
            poseStack.translate((RuneRender.hash((int) age * 7) - .5f) * .3f, 0, 0);
        }
        CosmosRender.echo(target, poseStack, buffer, partialTicks, echo.getColor(), fade * flicker * .9f, age * .02f);
        poseStack.popPose();
    }

    @Override
    public ResourceLocation getTextureLocation(TemporalEcho echo) {
        return CosmosRender.SCANLINES;
    }
}
