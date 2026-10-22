package com.waylays.arcanes.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.waylays.arcanes.entity.ritual.StormCloud;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/** The Call of the Storm: under the smoke of the cloud (its particles), a slow swirl of pale light turns, flickering. */
public class StormCloudRenderer extends EntityRenderer<StormCloud> {
    private static final int COLOR = 0x9FA8FF;

    public StormCloudRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(StormCloud storm, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        float strength = storm.fade(partialTicks, 60, 60);
        if (strength <= 0) {
            return;
        }
        float age = storm.tickCount + partialTicks;
        // Flickers now and then, as lightning moves in the cloud
        float flicker = .25f + (Mth.sin(age * 1.7f) * Mth.sin(age * .37f) > .7f ? .5f : 0);
        for (int layer = 0; layer < 2; layer++) {
            poseStack.pushPose();
            poseStack.translate(0, StormCloud.HEIGHT - layer * .5f, 0);
            poseStack.mulPose(Axis.YP.rotationDegrees(age * (layer == 0 ? 1.2f : -.8f)));
            poseStack.mulPose(Axis.XP.rotationDegrees(180));
            RenderHelper.groundQuad(buffer.getBuffer(RenderType.eyes(GlyphRender.SWIRL)), poseStack.last(), storm.getSize() * (layer == 0 ? 1 : .6f),
                    RuneRender.additive(COLOR, flicker * strength * (layer == 0 ? .6f : .9f)), GlyphRender.FULL);
            poseStack.popPose();
        }
        super.render(storm, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    @Override
    public boolean shouldRender(StormCloud storm, Frustum frustum, double x, double y, double z) {
        return true;
    }

    @Override
    public ResourceLocation getTextureLocation(StormCloud storm) {
        return GlyphRender.GLOW;
    }
}
