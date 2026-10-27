package com.waylays.arcanes.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.waylays.arcanes.entity.wood.SuddenGrove;
import com.waylays.arcanes.spells.wood.WoodFx;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * Sudden Grove (its trees are real blocks): the ground of the grove glowing with a soft green ring, turning slowly, and
 * motes of life drifting up inside it.
 */
public class SuddenGroveRenderer extends EntityRenderer<SuddenGrove> {
    private static final int MOTES = 40;

    public SuddenGroveRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(SuddenGrove grove, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        float age = grove.tickCount + partialTicks;
        float shown = Mth.clamp(age / 20, 0, 1) * Mth.clamp((grove.getLife() - age) / 10, 0, 1);
        if (shown <= 0) {
            return;
        }
        poseStack.pushPose();
        poseStack.translate(0, .06f, 0);
        poseStack.mulPose(Axis.YP.rotation(age * .01f));
        VertexConsumer ring = buffer.getBuffer(RenderType.eyes(RuneRender.RING_GLOW));
        RenderHelper.groundQuad(ring, poseStack.last(), SuddenGrove.RADIUS, RuneRender.additive(WoodFx.LIFE, .55f * shown), GlyphRender.FULL);
        VertexConsumer glow = buffer.getBuffer(RenderType.eyes(GlyphRender.GLOW));
        RenderHelper.groundQuad(glow, poseStack.last(), SuddenGrove.RADIUS * .9f, RuneRender.additive(WoodFx.LIFE, .18f * shown * (.8f + .2f * Mth.sin(age * .08f))), GlyphRender.FULL);
        poseStack.popPose();
        int seed = grove.getId() * 131;
        for (int i = 0; i < MOTES; i++) {
            float t = (WoodFx.hash(seed + i) + age * (.004f + .003f * WoodFx.hash(seed + i * 3))) % 1;
            float a = WoodFx.hash(seed + i * 5) * Mth.TWO_PI + t * 2;
            float r = Mth.sqrt(WoodFx.hash(seed + i * 7)) * SuddenGrove.RADIUS * .9f;
            poseStack.pushPose();
            poseStack.translate(Mth.cos(a) * r, .2f + t * 4, Mth.sin(a) * r);
            poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
            float fade = Mth.sin(t * Mth.PI) * shown;
            RenderHelper.billboard(glow, poseStack.last(), .12f, RuneRender.additive(i % 5 == 0 ? WoodFx.LIFE_CORE : WoodFx.LIFE, fade), GlyphRender.FULL);
            poseStack.popPose();
        }
        super.render(grove, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    @Override
    public boolean shouldRender(SuddenGrove grove, Frustum frustum, double x, double y, double z) {
        return frustum.isVisible(grove.getBoundingBox().inflate(SuddenGrove.RADIUS + 1, 5, SuddenGrove.RADIUS + 1));
    }

    @Override
    public ResourceLocation getTextureLocation(SuddenGrove grove) {
        return WoodRender.BARK;
    }
}
