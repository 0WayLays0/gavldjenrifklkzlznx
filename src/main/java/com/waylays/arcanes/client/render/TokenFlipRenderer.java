package com.waylays.arcanes.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.waylays.arcanes.entity.token.TokenFlip;
import com.waylays.arcanes.spells.token.TokenFx;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/**
 * Coin Flip: a big token tossed up from the caster's hand, spinning, caught again; then it turns its face to whoever
 * looks: heads in gold, tails in cyan.
 */
public class TokenFlipRenderer extends EntityRenderer<TokenFlip> {
    private static final int GOLD = 0xFFC94A;

    public TokenFlipRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(TokenFlip flip, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        Entity bearer = flip.getBearer();
        if (bearer == null) {
            return;
        }
        float age = flip.tickCount + partialTicks;
        Vec3 hand = bearer.getPosition(partialTicks).subtract(flip.getPosition(partialTicks)).add(TokenFlip.hand(bearer));
        int result = flip.getResult();
        float shown = Mth.clamp((age - TokenFlip.TOSS_TICKS) / 4, 0, 1);
        float fade = Mth.clamp(1 - (age - TokenFlip.TOSS_TICKS - TokenFlip.SHOW_TICKS + 4) / 4, 0, 1);
        poseStack.pushPose();
        poseStack.translate(hand.x, hand.y + flip.height(partialTicks) + .25 * shown, hand.z);
        poseStack.pushPose();
        if (result == TokenFlip.UNKNOWN) {
            poseStack.mulPose(Axis.YP.rotationDegrees(-bearer.getYRot()));
            poseStack.mulPose(Axis.XP.rotationDegrees(age * 55));
        } else {
            poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
            poseStack.mulPose(Axis.XP.rotationDegrees(90));
        }
        boolean heads = result == TokenFlip.HEADS;
        TokenRender.token(poseStack, buffer, .5f * fade, GlyphRender.FULL, heads ? 1.6f : 1, heads ? 1.1f : 1, heads ? .35f : 1);
        poseStack.popPose();
        if (result != TokenFlip.UNKNOWN) {
            poseStack.pushPose();
            poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
            GlyphRender.halo(buffer, poseStack.last(), GlyphRender.GLOW, .8f + .4f * shown, RuneRender.additive(heads ? GOLD : TokenFx.CYAN, fade));
            GlyphRender.halo(buffer, poseStack.last(), GlyphRender.RIPPLE, .5f + 1.2f * shown, RuneRender.additive(heads ? GOLD : TokenFx.CYAN, (1 - shown * .5f) * fade));
            poseStack.popPose();
        } else {
            TokenRender.glow(poseStack, buffer, this.entityRenderDispatcher, .4f, .5f);
        }
        poseStack.popPose();
        super.render(flip, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    @Override
    public boolean shouldRender(TokenFlip entity, Frustum frustum, double x, double y, double z) {
        return true;
    }

    @Override
    public ResourceLocation getTextureLocation(TokenFlip entity) {
        return GlyphRender.GLOW;
    }
}
