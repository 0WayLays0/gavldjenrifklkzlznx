package com.waylays.arcanes.client.render.aurora;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.waylays.arcanes.ArcanesOublies;
import com.waylays.arcanes.client.render.boss.BossMesh;
import com.waylays.arcanes.client.render.boss.Sculpt;
import com.waylays.arcanes.client.render.gem.GemMesh;
import com.waylays.arcanes.client.render.ritual.RitualMesh;
import com.waylays.arcanes.client.render.sand.SandMesh;
import com.waylays.arcanes.client.render.water.WaterMesh;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

/**
 * What Aurora Magic's models are made of: the northern lights. Curtains of light hanging in folds, their rays rising
 * from a bright hem and fading upward, green at the hem, then turquoise, blue, violet and rose at the top, rippling as
 * they drift; ribbons of the same light; stars twinkling; points of pale ice.
 */
public final class AuroraMesh {
    /** Rays of light running up (V) from a bright hem (V = 0) and fading out at the top (V = 1), streaked across (U). */
    public static final ResourceLocation RAYS = ArcanesOublies.id("textures/entity/aurora/rays.png");
    /** A soft band of light, bright down its middle (U), for ribbons. */
    public static final ResourceLocation BAND = ArcanesOublies.id("textures/entity/aurora/band.png");
    /** The night sky, deep blue strewn with stars, for the dome of the Boreal Night. */
    public static final ResourceLocation NIGHT = ArcanesOublies.id("textures/entity/aurora/night.png");
    /** The curtains' rays as a veil: white, as opaque as the rays are bright (tinted and laid over what is behind them). */
    public static final ResourceLocation VEIL = ArcanesOublies.id("textures/entity/aurora/veil.png");

    public static final int GREEN = 0x46FFA0;
    public static final int TEAL = 0x3FE8E0;
    public static final int BLUE = 0x5A8CFF;
    public static final int VIOLET = 0xA46BFF;
    public static final int ROSE = 0xFF6AD0;
    public static final int ICE = 0xD8F4FF;
    public static final int NIGHT_BLUE = 0x0B1640;

    private AuroraMesh() {}

    /** The aurora's colour {@code t} of the way up a curtain (0 green at the hem, 1 rose at the top), shifted by {@code shift}. */
    public static int hue(float t) {
        t = Mth.clamp(t, 0, 1);
        // Mostly green, as the northern lights are, then a short turquoise, violet and rose at the top
        if (t < .42f) {
            return BossMesh.mix(GREEN, 0x7CFF9A, t / .42f);
        }
        if (t < .58f) {
            return BossMesh.mix(0x7CFF9A, TEAL, (t - .42f) / .16f);
        }
        if (t < .8f) {
            return BossMesh.mix(TEAL, VIOLET, (t - .58f) / .22f);
        }
        return BossMesh.mix(VIOLET, ROSE, (t - .8f) / .2f);
    }

    /** The colour of a ribbon {@code t} along it (0 to 1), its hues running round as {@code time} goes. */
    public static int band(float t, float time) {
        float k = (t + time * .01f) % 1;
        return hue(k < .5f ? k * 2 : (1 - k) * 2);
    }

    /**
     * A curtain of light hanging from {@code tops} down {@code height} (or rising from {@code hems} up {@code height}: the
     * same), its folds rippling with {@code time}: rays rising from its hem, green there and rose at the top. Drawn on
     * both sides, {@code alpha} bright; its hem lit the brightest. Under its light, a veil of its colours is laid over what
     * lies behind it, so its greens, violets and roses still show against a bright sky.
     */
    public static void curtain(MultiBufferSource buffer, PoseStack.Pose p, Vector3f[] hems, float height, float time, float alpha, float flow, int light2) {
        int n = hems.length;
        if (n < 2 || alpha <= .01f) {
            return;
        }
        int rows = 5;
        float len = 0;
        float[] us = new float[n];
        for (int i = 1; i < n; i++) {
            len += hems[i].distance(hems[i - 1]);
            us[i] = len;
        }
        Vector3f[][] grid = new Vector3f[n][rows + 1];
        for (int i = 0; i < n; i++) {
            Vector3f side = new Vector3f(hems[Math.min(n - 1, i + 1)]).sub(hems[Math.max(0, i - 1)]);
            Vector3f across = new Vector3f(side.z, 0, -side.x);
            if (across.lengthSquared() > 1.0E-6f) {
                across.normalize();
            }
            for (int r = 0; r <= rows; r++) {
                float v = (float) r / rows;
                // The folds sway more toward the top, and the top drifts
                float sway = (.35f * Mth.sin(us[i] * .7f + time * .06f + v * 2) + .2f * Mth.sin(us[i] * 1.7f - time * .09f)) * v;
                grid[i][r] = new Vector3f(hems[i]).add(new Vector3f(across).mul(sway)).add(0, height * v + .3f * v * v * Mth.sin(us[i] + time * .05f), 0);
            }
        }
        float[] flick = new float[n];
        for (int i = 0; i < n; i++) {
            // The light flickers along the curtain
            flick[i] = .75f + .25f * Mth.sin(us[i] * 2.3f + time * .21f);
        }
        VertexConsumer veil = buffer.getBuffer(RenderType.entityTranslucentEmissive(VEIL));
        for (int i = 0; i + 1 < n; i++) {
            float u0 = us[i] * .25f + flow, u1 = us[i + 1] * .25f + flow;
            for (int r = 0; r < rows; r++) {
                float v0 = (float) r / rows, v1 = (float) (r + 1) / rows;
                float a0 = alpha * .5f * (.45f + .55f * v0), a1 = alpha * .5f * (.45f + .55f * v1);
                Vector3f a = grid[i][r], b = grid[i + 1][r], d = grid[i + 1][r + 1], e = grid[i][r + 1];
                Vector3f nrm = new Vector3f(b).sub(a).cross(new Vector3f(e).sub(a));
                nrm = nrm.lengthSquared() < 1.0E-12f ? new Vector3f(0, 1, 0) : nrm.normalize();
                WaterMesh.put(veil, p, a, u0, 1 - v0, BossMesh.argb(hue(v0), a0 * flick[i]), light2, nrm);
                WaterMesh.put(veil, p, b, u1, 1 - v0, BossMesh.argb(hue(v0), a0 * flick[i + 1]), light2, nrm);
                WaterMesh.put(veil, p, d, u1, 1 - v1, BossMesh.argb(hue(v1), a1 * flick[i + 1]), light2, nrm);
                WaterMesh.put(veil, p, e, u0, 1 - v1, BossMesh.argb(hue(v1), a1 * flick[i]), light2, nrm);
            }
        }
        VertexConsumer rays = BossMesh.light(buffer, RAYS);
        for (int i = 0; i + 1 < n; i++) {
            float u0 = us[i] * .25f + flow, u1 = us[i + 1] * .25f + flow;
            float f0 = flick[i], f1 = flick[i + 1];
            for (int r = 0; r < rows; r++) {
                float v0 = (float) r / rows, v1 = (float) (r + 1) / rows;
                float a0 = alpha * (1 - v0 * .4f), a1 = alpha * (1 - v1 * .4f);
                int c00 = BossMesh.glow(hue(v0), a0 * f0), c10 = BossMesh.glow(hue(v0), a0 * f1);
                int c01 = BossMesh.glow(hue(v1), a1 * f0), c11 = BossMesh.glow(hue(v1), a1 * f1);
                both(rays, p, grid[i][r], grid[i + 1][r], grid[i + 1][r + 1], grid[i][r + 1], u0, v0, u1, v1, c00, c10, c11, c01, light2);
            }
        }
    }

    /** A quad of light seen from both sides, its corners' colours given round it. */
    public static void both(VertexConsumer c, PoseStack.Pose p, Vector3f a, Vector3f b, Vector3f d, Vector3f e, float u0, float v0, float u1, float v1, int ca, int cb,
                            int cd, int ce, int light) {
        Vector3f n = new Vector3f(b).sub(a).cross(new Vector3f(e).sub(a));
        n = n.lengthSquared() < 1.0E-12f ? new Vector3f(0, 1, 0) : n.normalize();
        Vector3f back = new Vector3f(n).negate();
        WaterMesh.put(c, p, a, u0, 1 - v0, ca, light, n);
        WaterMesh.put(c, p, b, u1, 1 - v0, cb, light, n);
        WaterMesh.put(c, p, d, u1, 1 - v1, cd, light, n);
        WaterMesh.put(c, p, e, u0, 1 - v1, ce, light, n);
        WaterMesh.put(c, p, e, u0, 1 - v1, ce, light, back);
        WaterMesh.put(c, p, d, u1, 1 - v1, cd, light, back);
        WaterMesh.put(c, p, b, u1, 1 - v0, cb, light, back);
        WaterMesh.put(c, p, a, u0, 1 - v0, ca, light, back);
    }

    /** {@code n} points round a circle of {@code r} at height {@code y}, starting at {@code turn}, over {@code arc} radians. */
    public static Vector3f[] arc(float r, float y, int n, float turn, float arc) {
        Vector3f[] pts = new Vector3f[n];
        for (int i = 0; i < n; i++) {
            float a = turn + arc * i / (n - 1);
            pts[i] = new Vector3f(Mth.cos(a) * r, y, Mth.sin(a) * r);
        }
        return pts;
    }

    /** A ribbon of aurora light along {@code pts} facing the eye, {@code width} wide (tapering to its ends), its hues running along it. */
    public static void ribbon(VertexConsumer band, PoseStack.Pose p, Vector3f eye, Vector3f[] pts, float width, float alpha, float time, int light) {
        int n = pts.length;
        float[] w = new float[n];
        int[] col = new int[n];
        for (int i = 0; i < n; i++) {
            float t = (float) i / (n - 1);
            w[i] = width * Mth.sin(Mth.PI * Math.min(1, .15f + t * .85f)) * (1 - .6f * t);
            col[i] = BossMesh.glow(band((float) i / n, time), alpha * (1 - .5f * t));
        }
        SandMesh.ribbon(band, p, eye, pts, w, .5f, time * .02f, col, light);
    }

    /** Stars twinkling at {@code count} spots scattered by {@code seed} in a box {@code r} round and {@code h} high round {@code at}. */
    public static void stars(PoseStack poseStack, MultiBufferSource buffer, WaterMesh.View view, Vector3f at, float r, float h, float time, int count, float alpha, int seed,
                             int light) {
        if (alpha <= .01f) {
            return;
        }
        VertexConsumer c = BossMesh.light(buffer, GemMesh.SPARKLE);
        for (int k = 0; k < count; k++) {
            float hh = BossMesh.hash(seed + k * 13);
            float tw = Mth.sin(time * (.1f + .15f * hh) + k * 2.7f);
            if (tw < 0) {
                continue;
            }
            float a = BossMesh.hash(seed + k * 7) * Mth.TWO_PI, d = r * (float) Math.sqrt(BossMesh.hash(seed + k * 3));
            Vector3f s = new Vector3f(at).add(Mth.cos(a) * d, h * BossMesh.hash(seed + k * 5), Mth.sin(a) * d);
            RitualMesh.billboard(c, poseStack, view, s, .25f + .35f * tw, time * .02f + k, -1, BossMesh.glow(k % 4 == 0 ? hue(hh) : ICE, alpha * tw), light);
        }
    }

    /** A point of pale ice at {@code at}, pointing along {@code dir}, {@code size} across. */
    public static void ice(MultiBufferSource buffer, PoseStack.Pose p, Vector3f at, Vector3f dir, float size, float stretch, float time, int light) {
        GemMesh.gem(BossMesh.see(buffer, GemMesh.FACET), p, GemMesh.Cut.CRYSTAL, at, com.waylays.arcanes.client.render.lotus.LotusMesh.frame(dir, BossMesh.across(dir)),
                size, stretch, ICE, .7f, time, light);
    }

    /** A soft glow of the aurora at {@code at}. */
    public static void glow(PoseStack poseStack, MultiBufferSource buffer, WaterMesh.View view, Vector3f at, float size, int rgb, float alpha, int light) {
        RitualMesh.billboard(BossMesh.light(buffer, BossMesh.GLOW), poseStack, view, at, size, 0, -1, BossMesh.glow(rgb, alpha), light);
    }

    /** A ring of light lying flat at height {@code y}, from {@code r0} to {@code r1}, bright at {@code r1}. */
    public static void ring(VertexConsumer c, PoseStack.Pose p, float y, float r0, float r1, int rgb, float alpha, int light) {
        BossMesh.annulus(c, p, new Vector3f(0, y, 0), new Vector3f(1, 0, 0), new Vector3f(0, 0, -1), Math.max(.01f, r0), r1, 56, 1, 0, 0, BossMesh.glow(rgb, 0),
                BossMesh.glow(rgb, alpha), light, true);
    }

    static float[] fill(float v, int n) {
        return Sculpt.fill(v, n);
    }
}
