package com.waylays.arcanes.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.waylays.arcanes.entity.verdigris.VineSnare;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;

/** Strangling Vines: a patch of moss under the victim and vines climbing all around it, two layers deep, then withering. */
public class VineSnareRenderer extends EntityRenderer<VineSnare> {
    public VineSnareRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(VineSnare snare, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        Entity target = snare.getTarget();
        float grown = snare.grown(partialTicks);
        if (target == null || grown <= .001f) {
            return;
        }
        float width = target.getBbWidth() + .25f;
        float height = target.getBbHeight() * .85f * grown;
        poseStack.pushPose();
        // Drawn on the victim, wherever the snare entity last was
        poseStack.translate(Mth.lerp(partialTicks, target.xo, target.getX()) - Mth.lerp(partialTicks, snare.xo, snare.getX()),
                Mth.lerp(partialTicks, target.yo, target.getY()) - Mth.lerp(partialTicks, snare.yo, snare.getY()),
                Mth.lerp(partialTicks, target.zo, target.getZ()) - Mth.lerp(partialTicks, snare.zo, snare.getZ()));
        poseStack.mulPose(Axis.YP.rotationDegrees(-snare.getYRot()));
        float carpet = (width + 1.2f) * grown;
        VerdigrisRender.turned(VerdigrisRender.MOSS_CARPET, poseStack, buffer, packedLight, 0, .01f, 0, carpet, 1, carpet, 0);
        VerdigrisRender.turned(VerdigrisRender.MOSS_CARPET, poseStack, buffer, packedLight, 0, .015f, 0, carpet * .8f, 1, carpet * .8f, 45);
        VerdigrisRender.wrap(poseStack, buffer, packedLight, 0, width, height);
        poseStack.mulPose(Axis.YP.rotationDegrees(45));
        VerdigrisRender.wrap(poseStack, buffer, packedLight, 0, width * .92f, height * .8f);
        poseStack.popPose();
        super.render(snare, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    @Override
    public boolean shouldRender(VineSnare snare, Frustum frustum, double x, double y, double z) {
        Entity target = snare.getTarget();
        return target != null && frustum.isVisible(target.getBoundingBox().inflate(1));
    }

    @Override
    public ResourceLocation getTextureLocation(VineSnare snare) {
        return RuneRender.GLOW;
    }
}
