package com.waylays.arcanes.client.render.sand;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.waylays.arcanes.ArcanesOublies;
import com.waylays.arcanes.client.render.boss.BossMesh;
import com.waylays.arcanes.client.render.water.WaterMesh;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

/**
 * What Sand Magic is drawn with: sand flowing in ribbons and tubes (its texture scrolling along them), sand in clouds
 * and streaming grains, shapes turned on a lathe (glass bulbs, heaps of sand), glass, chased gold, the shell of a
 * scarab, glowing glyphs, the swirl of sand on the ground.
 */
public final class SandMesh {
    public static final ResourceLocation FLOW = ArcanesOublies.id("textures/entity/sand/sand_flow.png");
    public static final ResourceLocation SANDSTONE = ArcanesOublies.id("textures/entity/sand/sandstone.png");
    public static final ResourceLocation GLASS = ArcanesOublies.id("textures/entity/sand/glass.png");
    public static final ResourceLocation GOLD_SKIN = ArcanesOublies.id("textures/entity/sand/gold.png");
    public static final ResourceLocation SCARAB = ArcanesOublies.id("textures/entity/sand/scarab.png");
    public static final ResourceLocation GLYPHS = ArcanesOublies.id("textures/entity/sand/hieroglyphs.png");
    public static final ResourceLocation SWIRL = ArcanesOublies.id("textures/entity/sand/swirl.png");
    public static final ResourceLocation STREAK = ArcanesOublies.id("textures/entity/water/streak.png");

    public static final int SAND_PALE = 0xF6E2AA;
    public static final int SAND = 0xE0B866;
    public static final int SANDSTONE_TONE = 0xCEA05C;
    public static final int UMBER = 0x784E28;
    public static final int GOLD = 0xFFCC50;
    public static final int TURQUOISE = 0x40C8C4;
    public static final int GLASS_TINT = 0xD8F6F2;
    public static final int SUN = 0xFFE9A0;

    private SandMesh() {}

    /** A cloud of sand at {@code at}, {@code size} across, turned by {@code spin}, {@code alpha} see-through. */
    public static void cloud(VertexConsumer c, PoseStack poseStack, WaterMesh.View view, Vector3f at, float size, float spin, int rgb, float alpha, int light) {
        if (alpha > .01f && size > .01f) {
            WaterMesh.puff(c, poseStack, view, at.x, at.y, at.z, size, spin, -1, BossMesh.argb(rgb, alpha), light);
        }
    }

    /** A streak of flying sand from {@code tail} to {@code head}, {@code width} wide. */
    public static void streak(VertexConsumer c, PoseStack.Pose p, WaterMesh.View view, Vector3f head, Vector3f tail, float width, int rgb, float alpha, int light) {
        if (alpha > .01f) {
            WaterMesh.streak(c, p, view.eye(), head, tail, width, BossMesh.argb(rgb, alpha), light);
        }
    }

    /**
     * A tube along {@code pts}, {@code radii} thick at each, {@code sides} round: its texture wrapped round it (U) and
     * running along it (V), {@code tile} repeats a block, slid by {@code flow} (so the sand runs along it); coloured
     * {@code argb} at each point.
     */
    public static void tube(VertexConsumer c, PoseStack.Pose p, Vector3f[] pts, float[] radii, int sides, float tile, float flow, int[] argb, int light) {
        int n = pts.length;
        if (n < 2) {
            return;
        }
        Vector3f[][] ring = new Vector3f[n][sides + 1];
        Vector3f[][] nrm = new Vector3f[n][sides + 1];
        float[] v = new float[n];
        Vector3f side = null;
        for (int i = 0; i < n; i++) {
            Vector3f tan = new Vector3f(pts[Math.min(n - 1, i + 1)]).sub(pts[Math.max(0, i - 1)]);
            if (tan.lengthSquared() < 1.0E-10f) {
                tan.set(0, 1, 0);
            }
            tan.normalize();
            // Carried along the tube so that it does not twist
            side = side == null ? BossMesh.across(tan) : new Vector3f(side).sub(new Vector3f(tan).mul(side.dot(tan)));
            if (side.lengthSquared() < 1.0E-8f) {
                side = BossMesh.across(tan);
            }
            side.normalize();
            Vector3f up = new Vector3f(tan).cross(side).normalize();
            v[i] = i == 0 ? 0 : v[i - 1] + pts[i].distance(pts[i - 1]) * tile;
            for (int s = 0; s <= sides; s++) {
                float a = s * Mth.TWO_PI / sides;
                Vector3f out = new Vector3f(side).mul(Mth.cos(a)).add(new Vector3f(up).mul(Mth.sin(a)));
                ring[i][s] = new Vector3f(out).mul(radii[i]).add(pts[i]);
                nrm[i][s] = out;
            }
        }
        for (int i = 0; i + 1 < n; i++) {
            for (int s = 0; s < sides; s++) {
                float u0 = (float) s / sides, u1 = (float) (s + 1) / sides;
                // Wound to face out
                WaterMesh.put(c, p, ring[i][s], u0, v[i] - flow, argb[i], light, nrm[i][s]);
                WaterMesh.put(c, p, ring[i][s + 1], u1, v[i] - flow, argb[i], light, nrm[i][s + 1]);
                WaterMesh.put(c, p, ring[i + 1][s + 1], u1, v[i + 1] - flow, argb[i + 1], light, nrm[i + 1][s + 1]);
                WaterMesh.put(c, p, ring[i + 1][s], u0, v[i + 1] - flow, argb[i + 1], light, nrm[i + 1][s]);
            }
        }
    }

    /**
     * A ribbon along {@code pts}, turned to face the eye at every point, {@code widths} wide: its texture across it (U)
     * and running along it (V), {@code tile} repeats a block, slid by {@code flow}.
     */
    public static void ribbon(VertexConsumer c, PoseStack.Pose p, Vector3f eye, Vector3f[] pts, float[] widths, float tile, float flow, int[] argb, int light) {
        int n = pts.length;
        if (n < 2) {
            return;
        }
        Vector3f[] left = new Vector3f[n], right = new Vector3f[n];
        float[] v = new float[n];
        for (int i = 0; i < n; i++) {
            Vector3f tan = new Vector3f(pts[Math.min(n - 1, i + 1)]).sub(pts[Math.max(0, i - 1)]);
            Vector3f toEye = new Vector3f(eye).sub(pts[i]);
            Vector3f across = new Vector3f(tan).cross(toEye);
            if (across.lengthSquared() < 1.0E-10f) {
                across = BossMesh.across(tan.lengthSquared() < 1.0E-10f ? new Vector3f(0, 1, 0) : new Vector3f(tan).normalize());
            }
            across.normalize().mul(widths[i] * .5f);
            left[i] = new Vector3f(pts[i]).sub(across);
            right[i] = new Vector3f(pts[i]).add(across);
            v[i] = i == 0 ? 0 : v[i - 1] + pts[i].distance(pts[i - 1]) * tile;
        }
        for (int i = 0; i + 1 < n; i++) {
            Vector3f nrm = new Vector3f(eye).sub(pts[i]).normalize();
            // Wound to face the eye
            WaterMesh.put(c, p, left[i], 0, v[i] - flow, argb[i], light, nrm);
            WaterMesh.put(c, p, right[i], 1, v[i] - flow, argb[i], light, nrm);
            WaterMesh.put(c, p, right[i + 1], 1, v[i + 1] - flow, argb[i + 1], light, nrm);
            WaterMesh.put(c, p, left[i + 1], 0, v[i + 1] - flow, argb[i + 1], light, nrm);
        }
    }

    /**
     * A shape turned on a lathe round the upright through {@code base}: {@code rs[i]} across at height {@code ys[i]},
     * {@code segs} round, facing out; its texture wrapped {@code wrap} times round it (U) and running up it (V, one
     * repeat a block, slid by {@code flow}).
     */
    public static void lathe(VertexConsumer c, PoseStack.Pose p, Vector3f base, float[] ys, float[] rs, int segs, float wrap, float flow, int argb, int light) {
        for (int i = 0; i + 1 < ys.length; i++) {
            float dy = ys[i + 1] - ys[i], dr = rs[i + 1] - rs[i];
            float len = Mth.sqrt(dy * dy + dr * dr);
            float ny = len < 1.0E-6f ? 0 : -dr / len, nr = len < 1.0E-6f ? 1 : dy / len;
            for (int s = 0; s < segs; s++) {
                float a0 = s * Mth.TWO_PI / segs, a1 = (s + 1) * Mth.TWO_PI / segs;
                float u0 = (float) s / segs * wrap, u1 = (float) (s + 1) / segs * wrap;
                Vector3f n0 = new Vector3f(Mth.cos(a0) * nr, ny, Mth.sin(a0) * nr), n1 = new Vector3f(Mth.cos(a1) * nr, ny, Mth.sin(a1) * nr);
                WaterMesh.put(c, p, new Vector3f(Mth.cos(a0) * rs[i], ys[i], Mth.sin(a0) * rs[i]).add(base), u0, -ys[i] - flow, argb, light, n0);
                WaterMesh.put(c, p, new Vector3f(Mth.cos(a0) * rs[i + 1], ys[i + 1], Mth.sin(a0) * rs[i + 1]).add(base), u0, -ys[i + 1] - flow, argb, light, n0);
                WaterMesh.put(c, p, new Vector3f(Mth.cos(a1) * rs[i + 1], ys[i + 1], Mth.sin(a1) * rs[i + 1]).add(base), u1, -ys[i + 1] - flow, argb, light, n1);
                WaterMesh.put(c, p, new Vector3f(Mth.cos(a1) * rs[i], ys[i], Mth.sin(a1) * rs[i]).add(base), u1, -ys[i] - flow, argb, light, n1);
            }
        }
    }

    /** An egg round {@code at}, {@code rx, ry, rz} across, turned {@code yaw} (radians) round the upright: its own z then points (sin yaw, 0, cos yaw). */
    public static void ellipsoid(VertexConsumer c, PoseStack poseStack, Vector3f at, float rx, float ry, float rz, float yaw, float turn, int argb, int light) {
        poseStack.pushPose();
        poseStack.translate(at.x, at.y, at.z);
        poseStack.mulPose(new org.joml.Quaternionf().rotateY(yaw));
        poseStack.scale(rx, ry, rz);
        BossMesh.sphere(c, poseStack.last(), new Vector3f(), 1, 6, 10, turn, 0, 1, argb, light);
        poseStack.popPose();
    }

    /** Points along a curve through {@code a}, {@code mid} and {@code b} (a quadratic bezier), {@code n + 1} of them. */
    public static Vector3f[] curve(Vector3f a, Vector3f mid, Vector3f b, int n) {
        Vector3f[] pts = new Vector3f[n + 1];
        for (int i = 0; i <= n; i++) {
            float t = (float) i / n;
            pts[i] = new Vector3f(a).mul((1 - t) * (1 - t)).add(new Vector3f(mid).mul(2 * t * (1 - t))).add(new Vector3f(b).mul(t * t));
        }
        return pts;
    }

    /** {@code n} times the same colour. */
    public static int[] colours(int argb, int n) {
        int[] out = new int[n];
        java.util.Arrays.fill(out, argb);
        return out;
    }

    /** Grains of sand thrown up from round {@code (0, 0, 0)}, falling back: {@code t} 0 to 1 through their flight. */
    public static void spray(PoseStack poseStack, MultiBufferSource buffer, WaterMesh.View view, float radius, float height, float t, int count, int seed,
                             int light) {
        VertexConsumer dust = BossMesh.see(buffer, BossMesh.DUST);
        for (int k = 0; k < count; k++) {
            float a = BossMesh.hash(seed + k) * Mth.TWO_PI;
            float r = radius * (.3f + .7f * BossMesh.hash(seed + k * 3)) * (.4f + .6f * t);
            float h = height * (.5f + .5f * BossMesh.hash(seed + k * 5)) * 4 * t * (1 - t);
            cloud(dust, poseStack, view, new Vector3f(Mth.cos(a) * r, h + .2f, Mth.sin(a) * r), .8f + 1.6f * t, a + t, k % 3 == 0 ? SAND_PALE : SAND,
                    .75f * (1 - t * t), light);
        }
    }

    /** Grains of sand glinting in the light, drifting round {@code (0, 0, 0)} within {@code radius}, up to {@code height}. */
    public static void grains(PoseStack poseStack, MultiBufferSource buffer, WaterMesh.View view, float radius, float height, float age, int count, float alpha,
                              int seed, int light) {
        VertexConsumer motes = BossMesh.light(buffer, BossMesh.MOTE);
        for (int k = 0; k < count; k++) {
            float t = (age * (.01f + .01f * BossMesh.hash(seed + k)) + BossMesh.hash(seed + k * 3)) % 1;
            float a = BossMesh.hash(seed + k * 5) * Mth.TWO_PI + age * .03f;
            float r = radius * Mth.sqrt(BossMesh.hash(seed + k * 7));
            Vector3f at = new Vector3f(Mth.cos(a) * r, height * t, Mth.sin(a) * r);
            com.waylays.arcanes.client.render.ritual.RitualMesh.billboard(motes, poseStack, view, at, .18f, 0, -1,
                    BossMesh.glow(k % 3 == 0 ? SUN : SAND_PALE, Mth.sin(t * Mth.PI) * alpha * .8f), light);
        }
    }
}
