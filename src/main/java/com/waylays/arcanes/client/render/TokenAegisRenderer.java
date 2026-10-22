package com.waylays.arcanes.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.waylays.arcanes.entity.token.TokenAegis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/** Token Aegis: soul tokens standing on edge, turning around the caster's chest; they flash when one catches a blow. */
public class TokenAegisRenderer extends EntityRenderer<TokenAegis> {
    public TokenAegisRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(TokenAegis aegis, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        Entity bearer = aegis.getBearer();
        float strength = aegis.strength(partialTicks);
        if (bearer == null || strength <= 0) {
            return;
        }
        int count = aegis.getCount();
        float flash = Math.max(0, 1 - aegis.sinceCaught(partialTicks) / 8);
        Vec3 feet = bearer.getPosition(partialTicks).subtract(aegis.getPosition(partialTicks));
        for (int i = 0; i < count; i++) {
            Vec3 at = feet.add(aegis.offset(i, count, bearer, partialTicks));
            float a = aegis.angle(i, count, partialTicks);
            poseStack.pushPose();
            poseStack.translate(at.x, at.y, at.z);
            poseStack.pushPose();
            // Facing outward, on edge
            poseStack.mulPose(Axis.YP.rotationDegrees(90 - a));
            poseStack.mulPose(Axis.XP.rotationDegrees(90));
            TokenRender.token(poseStack, buffer, .34f * strength, GlyphRender.FULL);
            poseStack.popPose();
            TokenRender.glow(poseStack, buffer, this.entityRenderDispatcher, .3f + .3f * flash, (.45f + .5f * flash) * strength);
            poseStack.popPose();
        }
        super.render(aegis, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    @Override
    public boolean shouldRender(TokenAegis entity, Frustum frustum, double x, double y, double z) {
        return true;
    }

    @Override
    public ResourceLocation getTextureLocation(TokenAegis entity) {
        return GlyphRender.GLOW;
    }
}
