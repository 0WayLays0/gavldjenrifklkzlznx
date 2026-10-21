package com.waylays.arcanes.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.waylays.arcanes.entity.glyph.GlyphWave;
import com.waylays.arcanes.spells.glyph.GlyphElement;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * A wave rolling out from a glyph in an arc: for the Tide, a wall of water curling over at its crest, a lower swell
 * behind it; for the Gale, bands of wind streaking along at three heights.
 */
public class GlyphWaveRenderer extends EntityRenderer<GlyphWave> {
    private static final int SEGMENTS = 20;

    public GlyphWaveRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(GlyphWave wave, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        float strength = wave.strength(partialTicks);
        if (strength <= 0) {
            return;
        }
        GlyphElement element = wave.getElement();
        float r = wave.radius(partialTicks);
        float age = wave.tickCount + partialTicks;
        float grow = Mth.clamp(age / 4, 0, 1);
        PoseStack.Pose pose = poseStack.last();
        if (element == GlyphElement.GALE) {
            VertexConsumer wind = buffer.getBuffer(RenderType.eyes(GlyphRender.SWIRL));
            for (int k = 0; k < 3; k++) {
                float y = .3f + k * .65f;
                float rr = r - k * .35f;
                band(wind, pose, wave, rr, y, y + .5f, rr + .2f, age * .12f + k * .3f, RuneRender.additive(element.color, .75f * strength * grow));
            }
            VertexConsumer core = buffer.getBuffer(RenderType.eyes(GlyphRender.WAVE));
            band(core, pose, wave, r, 0, 2f * grow, r + .3f, age * .05f, RuneRender.additive(element.core, .35f * strength));
        } else {
            VertexConsumer water = buffer.getBuffer(RenderType.eyes(GlyphRender.WAVE));
            float height = 1.7f * grow * (.6f + .4f * strength);
            // The wall, its crest leaning forward as it rolls, and the swell behind it
            band(water, pose, wave, r, 0, height, r + .6f, age * .04f, RuneRender.additive(element.color, .9f * strength));
            band(water, pose, wave, r + .25f, height * .7f, height * 1.05f, r + .85f, age * .04f + .5f, RuneRender.additive(element.core, .55f * strength));
            band(water, pose, wave, r - 1.1f, 0, height * .55f, r - .8f, age * .03f + .25f, RuneRender.additive(element.color, .45f * strength));
        }
        super.render(wave, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    /**
     * A band along the wave's arc: from radius {@code r} at height {@code y0} to radius {@code topR} at height
     * {@code y1}, the texture scrolled by {@code scroll} along it, seen from both sides.
     */
    private static void band(VertexConsumer consumer, PoseStack.Pose pose, GlyphWave wave, float r, float y0, float y1, float topR,
                             float scroll, int argb) {
        if (r <= 0) {
            return;
        }
        float half = wave.getHalfAngle();
        float yaw = wave.getYRot();
        float tiles = Math.max(1, r * half / 45f);
        for (int k = 0; k < SEGMENTS; k++) {
            float t0 = k / (float) SEGMENTS, t1 = (k + 1) / (float) SEGMENTS;
            float a0 = (yaw - half + 2 * half * t0) * Mth.DEG_TO_RAD;
            float a1 = (yaw - half + 2 * half * t1) * Mth.DEG_TO_RAD;
            // The ends of the arc taper away
            float e0 = Mth.sin(t0 * Mth.PI), e1 = Mth.sin(t1 * Mth.PI);
            int c0 = scale(argb, Mth.sqrt(e0)), c1 = scale(argb, Mth.sqrt(e1));
            float x0 = -Mth.sin(a0), z0 = Mth.cos(a0), x1 = -Mth.sin(a1), z1 = Mth.cos(a1);
            float u0 = t0 * tiles + scroll, u1 = t1 * tiles + scroll;
            float top0 = y0 + (y1 - y0) * (.3f + .7f * e0), top1 = y0 + (y1 - y0) * (.3f + .7f * e1);
            RenderHelper.vertex(consumer, pose, x0 * r, y0, z0 * r, u0, 1, c0, GlyphRender.FULL, 0, 1);
            RenderHelper.vertex(consumer, pose, x1 * r, y0, z1 * r, u1, 1, c1, GlyphRender.FULL, 0, 1);
            RenderHelper.vertex(consumer, pose, x1 * topR, top1, z1 * topR, u1, 0, c1, GlyphRender.FULL, 0, 1);
            RenderHelper.vertex(consumer, pose, x0 * topR, top0, z0 * topR, u0, 0, c0, GlyphRender.FULL, 0, 1);
            RenderHelper.vertex(consumer, pose, x0 * topR, top0, z0 * topR, u0, 0, c0, GlyphRender.FULL, 0, 1);
            RenderHelper.vertex(consumer, pose, x1 * topR, top1, z1 * topR, u1, 0, c1, GlyphRender.FULL, 0, 1);
            RenderHelper.vertex(consumer, pose, x1 * r, y0, z1 * r, u1, 1, c1, GlyphRender.FULL, 0, 1);
            RenderHelper.vertex(consumer, pose, x0 * r, y0, z0 * r, u0, 1, c0, GlyphRender.FULL, 0, 1);
        }
    }

    /** An additive colour dimmed by {@code t}. */
    private static int scale(int argb, float t) {
        return RuneRender.additive(argb, t);
    }

    @Override
    public boolean shouldRender(GlyphWave entity, Frustum frustum, double x, double y, double z) {
        return true;
    }

    @Override
    public ResourceLocation getTextureLocation(GlyphWave entity) {
        return GlyphRender.WAVE;
    }
}
