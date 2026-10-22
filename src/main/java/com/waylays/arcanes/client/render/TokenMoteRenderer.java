package com.waylays.arcanes.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.waylays.arcanes.entity.token.TokenMote;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

/** A soul token drawn out of a creature, spinning as it flies to the caster. */
public class TokenMoteRenderer extends EntityRenderer<TokenMote> {
    public TokenMoteRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(TokenMote mote, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        float age = mote.tickCount + partialTicks;
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(age * 25));
        poseStack.mulPose(Axis.XP.rotationDegrees(90));
        TokenRender.token(poseStack, buffer, .3f, GlyphRender.FULL);
        poseStack.popPose();
        TokenRender.glow(poseStack, buffer, this.entityRenderDispatcher, .35f, .8f);
        super.render(mote, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(TokenMote entity) {
        return GlyphRender.GLOW;
    }
}
