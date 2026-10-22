package com.waylays.arcanes.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.waylays.arcanes.entity.wall.WallImpact;
import com.waylays.arcanes.spells.wall.WallFx;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/** Where Wall Magic strikes: a ring of embers racing out over the ground, the sigil cracked under it, bricks flying. */
public class WallImpactRenderer extends EntityRenderer<WallImpact> {
    public WallImpactRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(WallImpact impact, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        float age = impact.tickCount + partialTicks;
        float t = Mth.clamp(age / WallImpact.LIFE, 0, 1);
        float size = impact.getSize();
        float out = 1 - (1 - t) * (1 - t) * (1 - t);
        poseStack.pushPose();
        poseStack.translate(0, .06, 0);
        poseStack.mulPose(Axis.YP.rotationDegrees(impact.getYRot() + age * 2));
        RenderHelper.groundQuad(buffer.getBuffer(RenderType.eyes(RuneRender.RING_GLOW)), poseStack.last(), size * (.3f + 1.1f * out),
                WallRender.ember(1 - t, (1 - t) * .95f), GlyphRender.FULL);
        poseStack.popPose();
        WallRender.sigil(poseStack, buffer, size * .8f, (1 - t) * .6f, impact.getYRot());
        int seed = impact.getId() * 53;
        for (int i = 0; i < impact.getBricks(); i++) {
            float a = (i + RuneRender.hash(seed + i)) * Mth.TWO_PI / impact.getBricks();
            float speed = (.12f + .14f * RuneRender.hash(seed * 3 + i)) * Math.max(1, size * .5f);
            WallRender.fragment(WallFx.brick(seed + i), poseStack, buffer, packedLight, 0, .3f, 0,
                    Mth.cos(a) * speed, .35f + .25f * RuneRender.hash(seed * 7 + i), Mth.sin(a) * speed, age, WallImpact.LIFE,
                    .3f + .25f * RuneRender.hash(seed * 11 + i), seed + i);
        }
        super.render(impact, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    @Override
    public boolean shouldRender(WallImpact impact, Frustum frustum, double x, double y, double z) {
        return frustum.isVisible(impact.getBoundingBox().inflate(impact.getSize() + 6));
    }

    @Override
    public ResourceLocation getTextureLocation(WallImpact impact) {
        return WallRender.SIGIL;
    }
}
