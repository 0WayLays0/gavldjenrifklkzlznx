package com.waylays.arcanes.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.waylays.arcanes.entity.token.TokenMarker;
import com.waylays.arcanes.spells.token.TokenFx;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/** A thrown token at rest: standing on its edge, turning slowly over a ring of light, a thread of light rising from it. */
public class TokenMarkerRenderer extends EntityRenderer<TokenMarker> {
    public TokenMarkerRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(TokenMarker marker, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        float strength = marker.strength(partialTicks);
        if (strength <= 0) {
            return;
        }
        float age = marker.tickCount + partialTicks;
        poseStack.pushPose();
        poseStack.translate(0, .4 + .08 * Mth.sin(age * .12f), 0);
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(age * 3));
        poseStack.mulPose(Axis.XP.rotationDegrees(90));
        TokenRender.token(poseStack, buffer, .4f * strength, GlyphRender.FULL);
        poseStack.popPose();
        GlyphRender.beam(buffer.getBuffer(RenderType.eyes(GlyphRender.STROKE)), poseStack.last(), Vec3.ZERO, new Vec3(0, 2.6, 0), .12f,
                RuneRender.additive(TokenFx.CYAN, .4f * strength));
        TokenRender.glow(poseStack, buffer, this.entityRenderDispatcher, .45f, .6f * strength);
        poseStack.popPose();
        poseStack.pushPose();
        poseStack.translate(0, .03, 0);
        float pulse = .7f + .1f * Mth.sin(age * .2f);
        RenderHelper.groundQuad(buffer.getBuffer(RenderType.eyes(GlyphRender.RIPPLE)), poseStack.last(), pulse, RuneRender.additive(TokenFx.CYAN, .55f * strength), GlyphRender.FULL);
        poseStack.popPose();
        super.render(marker, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    @Override
    public boolean shouldRender(TokenMarker entity, Frustum frustum, double x, double y, double z) {
        return true;
    }

    @Override
    public ResourceLocation getTextureLocation(TokenMarker entity) {
        return GlyphRender.GLOW;
    }
}
