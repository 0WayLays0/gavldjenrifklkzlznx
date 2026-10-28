package com.waylays.arcanes.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.waylays.arcanes.ArcanesOublies;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Drawing shared by the Water Magic renderers: see-through water (its texture scrolled so it flows), foam, caustic
 * light, droplets; columns and funnels of water turning round an axis, flat rings and discs of it, ribbons of it.
 * Water is drawn see-through and two-sided: its colour and alpha are given per vertex.
 */
public final class WaterRender {
    public static final ResourceLocation WATER = ArcanesOublies.id("textures/entity/water/water.png");
    public static final ResourceLocation FOAM = ArcanesOublies.id("textures/entity/water/foam.png");
    /** Caustic light (bright lines on black, drawn additive). */
    public static final ResourceLocation CAUSTICS = ArcanesOublies.id("textures/entity/water/caustics.png");
    public static final ResourceLocation DROP = ArcanesOublies.id("textures/entity/water/drop.png");
    public static final ResourceLocation RAIN = ArcanesOublies.id("textures/entity/water/rain.png");
    public static final ResourceLocation CLOUD = ArcanesOublies.id("textures/entity/water/cloud.png");
    /** A soap bubble's skin: nearly clear, a rainbow sheen swirling on it. */
    public static final ResourceLocation BUBBLE = ArcanesOublies.id("textures/entity/water/bubble.png");
    /** A jellyfish's bell seen from above: rings and radial canals, its rim glowing. */
    public static final ResourceLocation JELLY = ArcanesOublies.id("textures/entity/water/jelly.png");
    /** A small fish seen from the side, head to the right (pale, so it can be tinted). */
    public static final ResourceLocation FISH = ArcanesOublies.id("textures/entity/water/fish.png");
    /** A manta ray seen from above: wings across, head to the top, its tail trailing down. */
    public static final ResourceLocation MANTA = ArcanesOublies.id("textures/entity/water/manta.png");
    /** A frond of kelp lying along the texture (it tiles along its length). */
    public static final ResourceLocation KELP = ArcanesOublies.id("textures/entity/water/kelp.png");
    /** A shaft of light (bright at its top, fading down and to its sides, on black: drawn additive). */
    public static final ResourceLocation RAY = ArcanesOublies.id("textures/entity/water/ray.png");

    private WaterRender() {}

    public static VertexConsumer water(MultiBufferSource buffer) {
        return buffer.getBuffer(RenderType.entityTranslucent(WATER));
    }

    public static VertexConsumer foam(MultiBufferSource buffer) {
        return buffer.getBuffer(RenderType.entityTranslucent(FOAM));
    }

    public static VertexConsumer drops(MultiBufferSource buffer) {
        return buffer.getBuffer(RenderType.entityTranslucent(DROP));
    }

    /** Additive light (caustics, glows): its colour fades it. */
    public static VertexConsumer light(MultiBufferSource buffer, ResourceLocation texture) {
        return buffer.getBuffer(RenderType.eyes(texture));
    }

    /**
     * A wall of water turning round the vertical axis: from radius {@code r0} at height {@code y0} to {@code r1} at
     * {@code y1} ({@code rings} steps), {@code segs} round; the texture wrapped {@code wraps} times round, twisted by
     * {@code twist} turns from foot to top, scrolled by {@code flow}. {@code alpha} fades it from foot to top.
     */
    public static void funnel(VertexConsumer c, PoseStack.Pose p, float r0, float y0, float r1, float y1, int rings, int segs, float wraps, float twist,
                              float flow, int rgb, float alpha0, float alpha1, int light) {
        for (int j = 0; j < rings; j++) {
            float t0 = (float) j / rings, t1 = (float) (j + 1) / rings;
            float ra = Mth.lerp(t0, r0, r1), rb = Mth.lerp(t1, r0, r1);
            float ya = Mth.lerp(t0, y0, y1), yb = Mth.lerp(t1, y0, y1);
            int ca = RuneRender.alpha(rgb, Mth.lerp(t0, alpha0, alpha1)), cb = RuneRender.alpha(rgb, Mth.lerp(t1, alpha0, alpha1));
            for (int i = 0; i < segs; i++) {
                float a0 = i * Mth.TWO_PI / segs, a1 = (i + 1) * Mth.TWO_PI / segs;
                float u0 = (float) i / segs * wraps + flow, u1 = (float) (i + 1) / segs * wraps + flow;
                float wa = t0 * twist, wb = t1 * twist;
                RenderHelper.vertex(c, p, Mth.cos(a0) * ra, ya, Mth.sin(a0) * ra, u0 + wa, t0 * 2 - flow, ca, light, Mth.cos(a0), 0, Mth.sin(a0));
                RenderHelper.vertex(c, p, Mth.cos(a0) * rb, yb, Mth.sin(a0) * rb, u0 + wb, t1 * 2 - flow, cb, light, Mth.cos(a0), 0, Mth.sin(a0));
                RenderHelper.vertex(c, p, Mth.cos(a1) * rb, yb, Mth.sin(a1) * rb, u1 + wb, t1 * 2 - flow, cb, light, Mth.cos(a1), 0, Mth.sin(a1));
                RenderHelper.vertex(c, p, Mth.cos(a1) * ra, ya, Mth.sin(a1) * ra, u1 + wa, t0 * 2 - flow, ca, light, Mth.cos(a1), 0, Mth.sin(a1));
            }
        }
    }

    /** A flat ring of water at height {@code y} from radius {@code r0} to {@code r1}, its texture swirling by {@code spin}. */
    public static void ring(VertexConsumer c, PoseStack.Pose p, float y, float r0, float r1, int segs, float spin, int argb0, int argb1, int light) {
        for (int i = 0; i < segs; i++) {
            float a0 = i * Mth.TWO_PI / segs, a1 = (i + 1) * Mth.TWO_PI / segs;
            float u0 = (float) i / segs * 4 + spin, u1 = (float) (i + 1) / segs * 4 + spin;
            RenderHelper.vertex(c, p, Mth.cos(a0) * r0, y, Mth.sin(a0) * r0, u0, 0, argb0, light, 0, 1, 0);
            RenderHelper.vertex(c, p, Mth.cos(a0) * r1, y, Mth.sin(a0) * r1, u0, 1, argb1, light, 0, 1, 0);
            RenderHelper.vertex(c, p, Mth.cos(a1) * r1, y, Mth.sin(a1) * r1, u1, 1, argb1, light, 0, 1, 0);
            RenderHelper.vertex(c, p, Mth.cos(a1) * r0, y, Mth.sin(a1) * r0, u1, 0, argb0, light, 0, 1, 0);
        }
    }

    /**
     * A ribbon of water along {@code points}, {@code width} wide across {@code side} (one vector per point), its texture
     * running along it and scrolled by {@code flow}; {@code alpha} per point.
     */
    public static void ribbon(VertexConsumer c, PoseStack.Pose p, Vector3f[] points, Vector3f[] side, float[] width, float[] alpha, int rgb, float flow, int light) {
        for (int i = 0; i + 1 < points.length; i++) {
            Vector3f a = points[i], b = points[i + 1];
            Vector3f sa = new Vector3f(side[i]).mul(width[i] * .5f), sb = new Vector3f(side[i + 1]).mul(width[i + 1] * .5f);
            Vector3f n = new Vector3f(b).sub(a).cross(side[i]);
            if (n.lengthSquared() < 1.0E-8f) {
                n.set(0, 1, 0);
            }
            n.normalize();
            float ua = (float) i / (points.length - 1) * 3 + flow, ub = (float) (i + 1) / (points.length - 1) * 3 + flow;
            int ca = RuneRender.alpha(rgb, alpha[i]), cb = RuneRender.alpha(rgb, alpha[i + 1]);
            RenderHelper.vertex(c, p, a.x - sa.x, a.y - sa.y, a.z - sa.z, ua, 0, ca, light, n.x, n.y, n.z);
            RenderHelper.vertex(c, p, a.x + sa.x, a.y + sa.y, a.z + sa.z, ua, 1, ca, light, n.x, n.y, n.z);
            RenderHelper.vertex(c, p, b.x + sb.x, b.y + sb.y, b.z + sb.z, ub, 1, cb, light, n.x, n.y, n.z);
            RenderHelper.vertex(c, p, b.x - sb.x, b.y - sb.y, b.z - sb.z, ub, 0, cb, light, n.x, n.y, n.z);
        }
    }

    /** A drop of water facing the camera at (x, y, z), {@code size} across. */
    public static void drop(VertexConsumer c, PoseStack poseStack, Quaternionf camera, float x, float y, float z, float size, int argb, int light) {
        poseStack.pushPose();
        poseStack.translate(x, y, z);
        poseStack.mulPose(camera);
        RenderHelper.billboard(c, poseStack.last(), size * .5f, argb, light);
        poseStack.popPose();
    }

    /** A sphere of water {@code r} wide round {@code centre}, seen through (two-sided, so both its walls show). */
    public static void sphere(VertexConsumer c, PoseStack.Pose p, Vector3f centre, float rx, float ry, float rz, int segs, int rings, int argb, int light) {
        BoneRender.blob(c, p, centre, new Vector3f(rx, 0, 0), new Vector3f(0, ry, 0), new Vector3f(0, 0, rz), segs, rings, light, argb);
    }

    /** A colour between deep and shallow water. */
    public static int sea(float t) {
        return GoldRender.lerp(com.waylays.arcanes.spells.water.WaterFx.DEEP, com.waylays.arcanes.spells.water.WaterFx.SHALLOW, t);
    }
}
