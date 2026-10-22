package com.waylays.arcanes.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.waylays.arcanes.entity.gold.GoldSigil;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

/**
 * The Sun Stone of Eldorado burning on the ground (or face down in the sky), drawn in from small, its rings turning
 * against each other.
 */
public class GoldSigilRenderer extends EntityRenderer<GoldSigil> {
    public GoldSigilRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(GoldSigil sigil, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        float age = sigil.tickCount + partialTicks;
        float grow = Math.min(1, .55f + age / 14f);
        poseStack.pushPose();
        if (sigil.isVault()) {
            poseStack.mulPose(Axis.XP.rotationDegrees(180));
        }
        GoldRender.sigil(poseStack, buffer, sigil.getRadius() * grow, sigil.strength(partialTicks), sigil.getYRot() + age * 1.2f);
        poseStack.popPose();
        super.render(sigil, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(GoldSigil sigil) {
        return GoldRender.SIGIL;
    }
}
