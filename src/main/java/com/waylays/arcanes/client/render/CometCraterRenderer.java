package com.waylays.arcanes.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.waylays.arcanes.entity.cosmos.CometCrater;
import com.waylays.arcanes.spells.cosmos.Cosmos;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * A comet's crater cooling: a rim of molten orange round a heart of ice-blue light, all of it dimming and going dark
 * over a few seconds.
 */
public class CometCraterRenderer extends EntityRenderer<CometCrater> {
    public CometCraterRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(CometCrater crater, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        float age = crater.tickCount + partialTicks;
        float heat = 1 - Mth.clamp(age / CometCrater.LIFE, 0, 1);
        heat *= heat;
        float r = crater.getRadius();
        poseStack.pushPose();
        poseStack.translate(0, .04, 0);
        poseStack.mulPose(Axis.YP.rotationDegrees(crater.getYRot()));
        RenderHelper.groundQuad(buffer.getBuffer(RenderType.eyes(RuneRender.RING_GLOW)), poseStack.last(), r * 1.1f,
                RuneRender.additive(GoldRender.lerp(Cosmos.EMBER, Cosmos.ORANGE, heat), heat), GlyphRender.FULL);
        poseStack.translate(0, .01, 0);
        poseStack.mulPose(Axis.YP.rotationDegrees(40));
        RenderHelper.groundQuad(buffer.getBuffer(RenderType.eyes(RuneRender.RING_GLOW)), poseStack.last(), r * .75f,
                RuneRender.additive(Cosmos.AMBER, heat * .7f), GlyphRender.FULL);
        poseStack.translate(0, .01, 0);
        RenderHelper.groundQuad(buffer.getBuffer(RenderType.eyes(GlyphRender.GLOW)), poseStack.last(), r * .55f,
                RuneRender.additive(Cosmos.ICE, heat * .6f), GlyphRender.FULL);
        poseStack.popPose();
        super.render(crater, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(CometCrater crater) {
        return RuneRender.RING_GLOW;
    }
}
