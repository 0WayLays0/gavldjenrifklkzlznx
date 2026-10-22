package com.waylays.arcanes.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.waylays.arcanes.entity.ritual.RitualPortal;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

/** The Portal: a tall swirling gate of violet light with a bright heart, turning, opening and closing. */
public class RitualPortalRenderer extends EntityRenderer<RitualPortal> {
    private static final int COLOR = 0xC27BFF;

    public RitualPortalRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(RitualPortal portal, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        float strength = portal.fade(partialTicks, 30, 30);
        if (strength <= 0) {
            return;
        }
        float age = portal.tickCount + partialTicks;
        poseStack.pushPose();
        poseStack.translate(0, 1.5, 0);
        poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
        poseStack.scale(strength, 1.5f * strength, 1);
        for (int layer = 0; layer < 3; layer++) {
            poseStack.pushPose();
            poseStack.mulPose(Axis.ZP.rotationDegrees(age * (layer % 2 == 0 ? 6 : -4) + layer * 40));
            GlyphRender.halo(buffer, poseStack.last(), GlyphRender.SWIRL, 1.4f - layer * .3f, RuneRender.additive(GlyphRender.whiten(COLOR, layer * .25f), .8f));
            poseStack.popPose();
        }
        GlyphRender.halo(buffer, poseStack.last(), GlyphRender.GLOW, .7f, RuneRender.additive(0xFFFFFF, .7f));
        poseStack.popPose();
        poseStack.pushPose();
        poseStack.translate(0, .05, 0);
        poseStack.mulPose(Axis.YP.rotationDegrees(age * 2));
        RenderHelper.groundQuad(buffer.getBuffer(RenderType.eyes(RuneRender.RING_GLOW)), poseStack.last(), 1.6f, RuneRender.additive(COLOR, .6f * strength), GlyphRender.FULL);
        poseStack.popPose();
        super.render(portal, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    @Override
    public boolean shouldRender(RitualPortal portal, Frustum frustum, double x, double y, double z) {
        return true;
    }

    @Override
    public ResourceLocation getTextureLocation(RitualPortal portal) {
        return GlyphRender.GLOW;
    }
}
