package com.waylays.arcanes.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.waylays.arcanes.entity.wall.BrickQuake;
import com.waylays.arcanes.spells.wall.WallFx;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * The Brick Quake's waves: behind each ring, columns of bricks burst up out of the ground one after another, lean out,
 * stand a moment and sink back, the ground glowing like embers under the ring.
 */
public class BrickQuakeRenderer extends EntityRenderer<BrickQuake> {
    /** Blocks between the rings of columns, and between the columns along a ring. */
    private static final float STEP = 1.3f;
    private static final float SPACING = 1.45f;

    public BrickQuakeRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(BrickQuake quake, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        float age = quake.tickCount + partialTicks;
        float radius = quake.getRadius();
        int seed = quake.getId() * 131;
        // The stamp's sigil, then each ring's glow on the ground (the stone, then the glow: a buffer is not written to
        // again once another has been asked for)
        for (int k = 0; k < BrickQuake.WAVES; k++) {
            float front = BrickQuake.front(k, age);
            if (front < 0 || front > radius + 2) {
                continue;
            }
            for (int ring = 0; ring * STEP < radius; ring++) {
                float d = 1.6f + ring * STEP;
                float since = age - BrickQuake.start(k) - d / BrickQuake.SPEED;
                if (since < 0 || since > BrickQuake.COLUMN_LIFE) {
                    continue;
                }
                int count = Math.max(5, Math.round(Mth.TWO_PI * d / SPACING));
                float turn = RuneRender.hash(seed + ring * 7 + k * 31) * Mth.TWO_PI;
                for (int i = 0; i < count; i++) {
                    float a = turn + i * Mth.TWO_PI / count;
                    int s = seed + k * 1009 + ring * 97 + i * 13;
                    column(poseStack, buffer, packedLight, Mth.cos(a) * d, Mth.sin(a) * d, a, since, k, s);
                }
            }
        }
        VertexConsumer glow = buffer.getBuffer(RenderType.eyes(RuneRender.RING_GLOW));
        for (int k = 0; k < BrickQuake.WAVES; k++) {
            float front = BrickQuake.front(k, age);
            if (front < .5f || front > radius + 2) {
                continue;
            }
            float fade = Mth.clamp((radius + 2 - front) / 3, 0, 1);
            poseStack.pushPose();
            poseStack.translate(0, .06 + k * .01, 0);
            RenderHelper.groundQuad(glow, poseStack.last(), front + .6f, RuneRender.additive(WallFx.EMBER, .85f * fade), GlyphRender.FULL);
            poseStack.popPose();
        }
        if (age < BrickQuake.FIRST + 6) {
            float t = Mth.clamp(age / BrickQuake.FIRST, 0, 1);
            WallRender.sigil(poseStack, buffer, 3.5f * t, t * Mth.clamp((BrickQuake.FIRST + 6 - age) / 6, 0, 1), age * 4);
        }
        super.render(quake, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    /** A column bursting up at (x, z), {@code since} ticks after the ring passed, leaning out along {@code angle}. */
    private static void column(PoseStack poseStack, MultiBufferSource buffer, int light, float x, float z, float angle, float since, int wave, int seed) {
        int bricks = 1 + (RuneRender.hash(seed) > .55f ? 1 : 0) + (wave == BrickQuake.WAVES - 1 && RuneRender.hash(seed + 1) > .4f ? 1 : 0);
        // Up fast, a moment standing, then down
        float up = since < 3 ? since / 3 : since > 9 ? Math.max(0, 1 - (since - 9) / 5) : 1;
        float rise = up * up * (3 - 2 * up);
        float height = bricks * .85f;
        float lean = 12 + 14 * RuneRender.hash(seed + 2);
        poseStack.pushPose();
        poseStack.translate(x, height * (rise - 1) - .2f, z);
        // Leaning out from the middle
        poseStack.mulPose(com.mojang.math.Axis.YP.rotation(-angle));
        poseStack.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(-lean));
        for (int b = 0; b < bricks; b++) {
            float size = b == bricks - 1 ? .7f : .85f;
            float twist = (RuneRender.hash(seed + 5 + b) - .5f) * 30;
            poseStack.pushPose();
            poseStack.translate(0, b * .85f, 0);
            poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(twist));
            RuneRender.block(WallFx.brick(seed + b), poseStack, buffer, light, 0, 0, 0, size, .85f, size, 0, 0);
            poseStack.popPose();
        }
        poseStack.popPose();
    }

    @Override
    public boolean shouldRender(BrickQuake quake, Frustum frustum, double x, double y, double z) {
        float r = quake.getRadius() + 3;
        return frustum.isVisible(quake.getBoundingBox().inflate(r, 4, r));
    }

    @Override
    public ResourceLocation getTextureLocation(BrickQuake quake) {
        return WallRender.SIGIL;
    }
}
