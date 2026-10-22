package com.waylays.arcanes.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.waylays.arcanes.entity.token.TokenMine;
import com.waylays.arcanes.spells.token.TokenFx;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/** A trap token: it drops onto the ground and lies there turning slowly; once armed it blinks. */
public class TokenMineRenderer extends EntityRenderer<TokenMine> {
    public TokenMineRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(TokenMine mine, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        float age = mine.tickCount + partialTicks;
        poseStack.pushPose();
        poseStack.translate(0, .04 + mine.drop(partialTicks), 0);
        poseStack.mulPose(Axis.YP.rotationDegrees(mine.getYRot() + age * 1.5f));
        TokenRender.token(poseStack, buffer, .38f, GlyphRender.FULL);
        poseStack.popPose();
        if (mine.isArmed()) {
            // A blink every second
            float blink = Math.max(0, 1 - Mth.positiveModulo(age, 20) / 8);
            poseStack.pushPose();
            poseStack.translate(0, .06, 0);
            RenderHelper.groundQuad(buffer.getBuffer(RenderType.eyes(GlyphRender.GLOW)), poseStack.last(), .5f + .3f * blink,
                    RuneRender.additive(TokenFx.CYAN, .3f + .6f * blink), GlyphRender.FULL);
            poseStack.popPose();
        }
        super.render(mine, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(TokenMine entity) {
        return GlyphRender.GLOW;
    }
}
