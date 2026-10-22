package com.waylays.arcanes.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.waylays.arcanes.entity.token.TokenBolt;
import com.waylays.arcanes.spells.token.TokenFx;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/** A soul token in flight: flipping end over end along its path, glowing, a cyan streak behind it. */
public class TokenBoltRenderer extends EntityRenderer<TokenBolt> {
    public TokenBoltRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(TokenBolt bolt, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        float size = .42f * bolt.getSize();
        float age = bolt.tickCount + partialTicks;
        Vec3 motion = bolt.getDeltaMovement();
        poseStack.pushPose();
        poseStack.translate(0, bolt.getBbHeight() * .5, 0);
        poseStack.pushPose();
        if (motion.lengthSqr() > 1.0E-6) {
            float yaw = (float) (Mth.atan2(motion.x, motion.z) * Mth.RAD_TO_DEG);
            poseStack.mulPose(Axis.YP.rotationDegrees(yaw));
        }
        // Standing on its edge, flipping over and over
        poseStack.mulPose(Axis.XP.rotationDegrees(age * 45));
        TokenRender.token(poseStack, buffer, size, GlyphRender.FULL);
        poseStack.popPose();
        if (motion.lengthSqr() > 1.0E-4) {
            Vec3 tail = motion.normalize().scale(-1.1 * bolt.getSize());
            GlyphRender.beam(buffer.getBuffer(RenderType.eyes(GlyphRender.STROKE)), poseStack.last(), Vec3.ZERO, tail, .28f * bolt.getSize(),
                    RuneRender.additive(TokenFx.CYAN, .6f));
        }
        TokenRender.glow(poseStack, buffer, this.entityRenderDispatcher, .38f * bolt.getSize(), .7f);
        poseStack.popPose();
        super.render(bolt, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(TokenBolt entity) {
        return GlyphRender.GLOW;
    }
}
