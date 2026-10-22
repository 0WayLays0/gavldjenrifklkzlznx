package com.waylays.arcanes.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.waylays.arcanes.entity.token.TokenStorm;
import com.waylays.arcanes.spells.token.TokenFx;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/**
 * Token Storm: soul tokens whirling around the caster on three rings at their own heights and speeds, tumbling as they
 * fly; at the end they fly outward and scatter.
 */
public class TokenStormRenderer extends EntityRenderer<TokenStorm> {
    private static final float[] RINGS = {1.4f, 2.4f, 3.3f};

    public TokenStormRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(TokenStorm storm, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        float strength = storm.strength(partialTicks);
        Entity bearer = storm.getBearer();
        if (strength <= 0 || bearer == null) {
            return;
        }
        float age = storm.tickCount + partialTicks;
        boolean ending = age > storm.getLife();
        // Gathers in at the start; flies outward at the end
        float reach = ending ? 1 + (1 - strength) * 1.2f : .3f + .7f * strength;
        Vec3 feet = bearer.getPosition(partialTicks).subtract(storm.getPosition(partialTicks));
        poseStack.pushPose();
        poseStack.translate(feet.x, feet.y, feet.z);
        for (int i = 0; i < TokenStorm.TOKENS; i++) {
            int ring = i % RINGS.length;
            float r = RINGS[ring] * reach;
            float speed = (7 - ring * 1.6f) * (ring == 1 ? -1 : 1);
            float a = (age * speed + i * 360f / TokenStorm.TOKENS * 3) * Mth.DEG_TO_RAD;
            float h = .3f + (i * .37f) % 2.5f + .2f * Mth.sin(age * .2f + i);
            poseStack.pushPose();
            poseStack.translate(Mth.cos(a) * r, h, Mth.sin(a) * r);
            poseStack.mulPose(Axis.YP.rotationDegrees(-a * Mth.RAD_TO_DEG));
            poseStack.mulPose(Axis.XP.rotationDegrees(age * 30 + i * 40));
            TokenRender.token(poseStack, buffer, .34f * Math.min(1, strength * 1.5f), GlyphRender.FULL);
            poseStack.popPose();
        }
        poseStack.translate(0, .05, 0);
        RenderHelper.groundQuad(buffer.getBuffer(RenderType.eyes(GlyphRender.RIPPLE)), poseStack.last(), TokenStorm.RADIUS * reach,
                RuneRender.additive(TokenFx.CYAN, .45f * strength), GlyphRender.FULL);
        poseStack.popPose();
        super.render(storm, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    @Override
    public boolean shouldRender(TokenStorm entity, Frustum frustum, double x, double y, double z) {
        return true;
    }

    @Override
    public ResourceLocation getTextureLocation(TokenStorm entity) {
        return GlyphRender.GLOW;
    }
}
