package com.waylays.arcanes.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.waylays.arcanes.ArcanesOublies;
import com.waylays.arcanes.entity.physical.StrikeTrail;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * Traces of fighting moves, pale and short-lived:
 * <ul>
 * <li>a slash is a crescent that grows behind the blade's edge as it sweeps the arc, thickest in the middle, brightest
 * at the edge, then fades;</li>
 * <li>a spin is the same crescent all the way round;</li>
 * <li>a thrust is a thin streak shooting forward;</li>
 * <li>a punch is a ring of air bursting out in front of the fist, with speed lines.</li>
 * </ul>
 */
public class StrikeTrailRenderer extends EntityRenderer<StrikeTrail> {
    private static final ResourceLocation TRAIL = ArcanesOublies.id("textures/entity/strike_trail.png");
    private static final int SEGMENTS = 32;

    public StrikeTrailRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(StrikeTrail entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        float age = entity.tickCount + partialTicks;
        if (age >= StrikeTrail.LIFE) {
            return;
        }
        VertexConsumer consumer = buffer.getBuffer(RenderType.entityTranslucent(TRAIL));
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(-entity.getYRot()));
        poseStack.mulPose(Axis.XP.rotationDegrees(entity.getXRot()));
        poseStack.mulPose(Axis.ZP.rotationDegrees(entity.getTilt()));
        PoseStack.Pose pose = poseStack.last();
        int rgb = entity.getColor() & 0xFFFFFF;
        switch (entity.getStyle()) {
            case SLASH, SPIN -> crescent(consumer, pose, entity, age, rgb);
            case STAB -> thrust(consumer, pose, entity.getRadius(), age, rgb);
            case PUNCH -> burst(consumer, pose, entity.getRadius(), age, rgb);
        }
        poseStack.popPose();
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    /** The crescent left by a blade: from the side it started on to where the edge is now. */
    private static void crescent(VertexConsumer consumer, PoseStack.Pose pose, StrikeTrail entity, float age, int rgb) {
        float span = Math.min(360, entity.getSpan()) * Mth.DEG_TO_RAD;
        float radius = entity.getRadius();
        float swept = Mth.clamp(age / StrikeTrail.SWEEP_TICKS, 0f, 1f);
        float fade = 1 - Mth.clamp((age - StrikeTrail.SWEEP_TICKS) / (StrikeTrail.LIFE - StrikeTrail.SWEEP_TICKS), 0f, 1f);
        // Forehand: from the right (-X) to the left (+X); backhand the other way
        float dir = entity.isReverse() ? -1 : 1;
        float start = -dir * span * .5f;
        int count = Math.max(2, (int) (SEGMENTS * swept));
        for (int i = 0; i < count; i++) {
            float s0 = swept * i / count;
            float s1 = swept * (i + 1) / count;
            float a0 = start + dir * span * s0;
            float a1 = start + dir * span * s1;
            // Brightest at the edge of the blade, fading toward where it started
            float lead0 = swept <= 0 ? 0 : s0 / swept;
            float lead1 = swept <= 0 ? 0 : s1 / swept;
            int alpha0 = (int) (220 * fade * (.15f + .85f * lead0 * lead0));
            int alpha1 = (int) (220 * fade * (.15f + .85f * lead1 * lead1));
            // Thickest in the middle of the arc
            float t0 = radius * .3f * Mth.sin(Mth.PI * s0) + .03f;
            float t1 = radius * .3f * Mth.sin(Mth.PI * s1) + .03f;
            vertex(consumer, pose, Mth.sin(a0) * (radius - t0), Mth.cos(a0) * (radius - t0), s0, 0, rgb, alpha0);
            vertex(consumer, pose, Mth.sin(a0) * radius, Mth.cos(a0) * radius, s0, 1, rgb, alpha0);
            vertex(consumer, pose, Mth.sin(a1) * radius, Mth.cos(a1) * radius, s1, 1, rgb, alpha1);
            vertex(consumer, pose, Mth.sin(a1) * (radius - t1), Mth.cos(a1) * (radius - t1), s1, 0, rgb, alpha1);
        }
    }

    /** A point of a slash's plane: {@code x} to the left, {@code z} forward. */
    private static void vertex(VertexConsumer consumer, PoseStack.Pose pose, float x, float z, float u, float v, int rgb, int alpha) {
        RenderHelper.vertex(consumer, pose, x, 0, z, u, v, (alpha << 24) | rgb, LightTexture.FULL_BRIGHT, 0, 1, 0);
    }

    /** A thin streak shooting forward from the chest, crossed so it shows from every side. */
    private static void thrust(VertexConsumer consumer, PoseStack.Pose pose, float length, float age, int rgb) {
        float out = Mth.clamp(age / 2f, 0f, 1f);
        float fade = 1 - Mth.clamp((age - 2) / (StrikeTrail.LIFE - 2), 0f, 1f);
        float tip = .4f + (length - .4f) * out;
        float back = Math.max(.4f, tip - length * .8f);
        float mid = (tip + back) * .5f;
        int bright = (int) (230 * fade) << 24 | rgb;
        int dim = (int) (40 * fade) << 24 | rgb;
        float w = .09f;
        for (int k = 0; k < 2; k++) {
            float wx = k == 0 ? w : 0;
            float wy = k == 0 ? 0 : w;
            RenderHelper.vertex(consumer, pose, 0, 0, back, 0, .5f, dim, LightTexture.FULL_BRIGHT, 0, 1, 0);
            RenderHelper.vertex(consumer, pose, wx, wy, mid, .5f, 1, bright, LightTexture.FULL_BRIGHT, 0, 1, 0);
            RenderHelper.vertex(consumer, pose, 0, 0, tip, 1, .5f, bright, LightTexture.FULL_BRIGHT, 0, 1, 0);
            RenderHelper.vertex(consumer, pose, -wx, -wy, mid, .5f, 1, bright, LightTexture.FULL_BRIGHT, 0, 1, 0);
        }
        ring(consumer, pose, tip, .08f + .35f * out, .05f, (int) (180 * fade), rgb);
    }

    /** A ring of air bursting out in front of a punch, and speed lines leading up to it. */
    private static void burst(VertexConsumer consumer, PoseStack.Pose pose, float distance, float age, int rgb) {
        float t = age / StrikeTrail.LIFE;
        float fade = 1 - t;
        ring(consumer, pose, distance, .15f + .6f * t, .08f + .1f * t, (int) (200 * fade), rgb);
        ring(consumer, pose, distance * .8f, .1f + .35f * t, .05f, (int) (120 * fade), rgb);
        for (int k = 0; k < 6; k++) {
            float a = k * Mth.TWO_PI / 6 + .4f;
            float r = .28f + .1f * (k % 2);
            float x = Mth.cos(a) * r;
            float y = Mth.sin(a) * r;
            float from = .3f + t * distance * .5f;
            float to = Math.max(from + .05f, distance * (.75f + .2f * t));
            int bright = (int) (150 * fade) << 24 | rgb;
            int clear = rgb;
            float nx = -Mth.sin(a) * .02f;
            float ny = Mth.cos(a) * .02f;
            RenderHelper.vertex(consumer, pose, x - nx, y - ny, from, 0, 0, clear, LightTexture.FULL_BRIGHT, 0, 0, 1);
            RenderHelper.vertex(consumer, pose, x + nx, y + ny, from, 0, 1, clear, LightTexture.FULL_BRIGHT, 0, 0, 1);
            RenderHelper.vertex(consumer, pose, x + nx, y + ny, to, 1, 1, bright, LightTexture.FULL_BRIGHT, 0, 0, 1);
            RenderHelper.vertex(consumer, pose, x - nx, y - ny, to, 1, 0, bright, LightTexture.FULL_BRIGHT, 0, 0, 1);
        }
    }

    /** A flat ring facing forward, {@code z} blocks ahead. */
    private static void ring(VertexConsumer consumer, PoseStack.Pose pose, float z, float radius, float width, int alpha, int rgb) {
        if (alpha <= 0) {
            return;
        }
        int outer = alpha << 24 | rgb;
        int inner = (alpha / 4) << 24 | rgb;
        for (int i = 0; i < 24; i++) {
            float a0 = i * Mth.TWO_PI / 24;
            float a1 = (i + 1) * Mth.TWO_PI / 24;
            float r0 = radius - width;
            RenderHelper.vertex(consumer, pose, Mth.cos(a0) * r0, Mth.sin(a0) * r0, z, 0, 0, inner, LightTexture.FULL_BRIGHT, 0, 0, 1);
            RenderHelper.vertex(consumer, pose, Mth.cos(a0) * radius, Mth.sin(a0) * radius, z, 0, 1, outer, LightTexture.FULL_BRIGHT, 0, 0, 1);
            RenderHelper.vertex(consumer, pose, Mth.cos(a1) * radius, Mth.sin(a1) * radius, z, 1, 1, outer, LightTexture.FULL_BRIGHT, 0, 0, 1);
            RenderHelper.vertex(consumer, pose, Mth.cos(a1) * r0, Mth.sin(a1) * r0, z, 1, 0, inner, LightTexture.FULL_BRIGHT, 0, 0, 1);
        }
    }

    @Override
    public ResourceLocation getTextureLocation(StrikeTrail entity) {
        return TRAIL;
    }
}
