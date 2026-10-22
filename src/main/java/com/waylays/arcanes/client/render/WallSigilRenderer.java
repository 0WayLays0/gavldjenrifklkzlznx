package com.waylays.arcanes.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.waylays.arcanes.entity.wall.WallSigil;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

/** The incantation of Wall Magic: the mason's sigil burning on the ground, turning, lighting up and dying out. */
public class WallSigilRenderer extends EntityRenderer<WallSigil> {
    public WallSigilRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(WallSigil sigil, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        float age = sigil.tickCount + partialTicks;
        WallRender.sigil(poseStack, buffer, sigil.getRadius(), sigil.strength(partialTicks), sigil.getYRot() + age * 1.5f);
        super.render(sigil, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    @Override
    public boolean shouldRender(WallSigil sigil, Frustum frustum, double x, double y, double z) {
        return frustum.isVisible(sigil.getBoundingBox().inflate(sigil.getRadius() + 1, 1, sigil.getRadius() + 1));
    }

    @Override
    public ResourceLocation getTextureLocation(WallSigil sigil) {
        return WallRender.SIGIL;
    }
}
