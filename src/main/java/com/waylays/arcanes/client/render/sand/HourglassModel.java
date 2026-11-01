package com.waylays.arcanes.client.render.sand;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.waylays.arcanes.client.render.boss.BossMesh;
import com.waylays.arcanes.client.render.ritual.RitualMesh;
import com.waylays.arcanes.client.render.water.WaterMesh;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

/**
 * Hourglass: a great hourglass hangs over the field, two bulbs of glass between round caps of chased gold held by three
 * twisted pillars; its sand runs from the upper bulb into a heap in the lower, a thin stream between them; a ring of
 * glowing glyphs turns on the sand below and the sand swirls in it. When the last grain falls it shatters: shards of
 * glass flying, the gold falling, the sand bursting out.
 */
public final class HourglassModel {
    private static final int RINGS = 14;

    private HourglassModel() {}

    /** The bulbs' radius at height {@code y} (from -h to h round the neck). */
    private static float bulb(float y, float h) {
        float t = Math.min(1, Math.abs(y) / h);
        return .08f + .66f * Mth.sin(Mth.HALF_PI * Math.min(1, t / .7f)) - .1f * Math.max(0, (t - .85f) / .15f);
    }

    /**
     * On the ground under it (the pose's origin), the field {@code radius} across, its middle {@code height} up;
     * {@code age} of {@code life} ticks, shattering {@code shatterTicks} before its end.
     */
    public static void draw(PoseStack poseStack, MultiBufferSource buffer, WaterMesh.View view, float radius, float height, float age, float life, int shatterTicks,
                            int seed, int light) {
        float shatter = life - shatterTicks;
        float run = Mth.clamp(age / shatter, 0, 1);
        float appear = BossMesh.ramp(age, 0, 12);
        float since = age - shatter;
        float h = 1.5f;
        Vector3f c = new Vector3f(0, height, 0);
        // The field: the sand swirling and a ring of glyphs turning on it
        float field = appear * (1 - BossMesh.ramp(age, life - 8, life));
        VertexConsumer swirl = BossMesh.ground(buffer, SandMesh.SWIRL);
        BossMesh.pool(swirl, poseStack.last(), 0, .03f, 0, radius * 2.1f, -age * .02f, BossMesh.argb(0xFFFFFF, .7f * field), light);
        VertexConsumer glyphs = BossMesh.light(buffer, SandMesh.GLYPHS);
        BossMesh.annulus(glyphs, poseStack.last(), new Vector3f(0, .05f, 0), new Vector3f(1, 0, 0), new Vector3f(0, 0, -1), radius * .86f, radius, 32, 8, age * .01f, 0,
                BossMesh.glow(SandMesh.GOLD, .8f * field), BossMesh.glow(SandMesh.GOLD, .8f * field), light, false);
        if (since < 0) {
            poseStack.pushPose();
            poseStack.translate(c.x, c.y, c.z);
            float s = appear * (.9f + .1f * appear);
            poseStack.scale(s, s, s);
            poseStack.translate(-c.x, -c.y, -c.z);
            drawWhole(poseStack, buffer, view, c, h, run, age, light);
            poseStack.popPose();
        } else {
            drawShattered(poseStack, buffer, view, c, h, since, shatterTicks, seed, light);
        }
        SandMesh.grains(poseStack, buffer, view, radius, height + 1, age, 18, field, seed, light);
    }

    private static void drawWhole(PoseStack poseStack, MultiBufferSource buffer, WaterMesh.View view, Vector3f c, float h, float run, float age, int light) {
        // The sand: what is left above, the heap below, the thin stream
        VertexConsumer flow = BossMesh.see(buffer, SandMesh.FLOW);
        PoseStack.Pose p = poseStack.last();
        float top = .1f + .8f * h * (1 - run);
        int n = 6;
        float[] ys = new float[n + 2], rs = new float[n + 2];
        for (int i = 0; i <= n; i++) {
            float y = .06f + (top - .06f) * i / n;
            ys[i] = y;
            rs[i] = bulb(y, h) * .9f;
        }
        ys[n + 1] = top;
        rs[n + 1] = 0;
        if (run < .98f) {
            SandMesh.lathe(flow, p, c, ys, rs, 16, 2, 0, 0xFFFFFFFF, light);
        }
        float heap = (.15f + .75f * run) * h;
        float base = bulb(-h * .92f, h) * .9f;
        SandMesh.lathe(flow, p, new Vector3f(c).add(0, -h * .92f, 0), new float[] {0, heap * .4f, heap * .8f, heap}, new float[] {base, base * .7f, base * .3f, 0}, 16,
                2, 0, 0xFFFFFFFF, light);
        if (run < .98f) {
            SandMesh.tube(flow, p, new Vector3f[] {new Vector3f(c).add(0, .05f, 0), new Vector3f(c).add(0, -h * .92f + heap, 0)}, new float[] {.03f, .03f}, 5, 3,
                    age * .6f, new int[] {0xFFFFFFFF, 0xFFFFFFFF}, light);
        }
        // The glass
        VertexConsumer glass = BossMesh.see(buffer, SandMesh.GLASS);
        p = poseStack.last();
        float[] gy = new float[RINGS * 2 + 1], gr = new float[RINGS * 2 + 1];
        for (int i = 0; i <= RINGS * 2; i++) {
            float y = -h + h * i / RINGS;
            gy[i] = y;
            gr[i] = bulb(y, h);
        }
        SandMesh.lathe(glass, p, c, gy, gr, 20, 1, 0, BossMesh.argb(SandMesh.GLASS_TINT, .45f), light);
        // The gold: its caps and pillars
        VertexConsumer gold = BossMesh.solid(buffer, SandMesh.GOLD_SKIN);
        p = poseStack.last();
        for (int side = -1; side <= 1; side += 2) {
            Vector3f cap = new Vector3f(c).add(0, side * (h + .08f), 0);
            SandMesh.lathe(gold, p, cap, new float[] {-.1f, -.1f, .1f, .1f}, new float[] {0, .95f, .95f, 0}, 20, 4, 0, 0xFFFFFFFF, light);
            SandMesh.lathe(gold, p, new Vector3f(cap).add(0, side * .16f, 0), new float[] {-.06f, -.06f, .06f, .06f}, new float[] {0, .6f, .6f, 0}, 16, 3, 0,
                    0xFFFFFFFF, light);
        }
        for (int k = 0; k < 3; k++) {
            float a = k * Mth.TWO_PI / 3;
            int m = 10;
            Vector3f[] pts = new Vector3f[m + 1];
            float[] r = new float[m + 1];
            for (int i = 0; i <= m; i++) {
                float t = (float) i / m;
                float tw = a + .25f * Mth.sin(t * Mth.TWO_PI * 2);
                pts[i] = new Vector3f(Mth.cos(tw) * .86f, -h + 2 * h * t, Mth.sin(tw) * .86f).add(c);
                r[i] = .06f + .02f * Mth.sin(t * Mth.PI * 6);
            }
            SandMesh.tube(gold, p, pts, r, 6, 2, 0, SandMesh.colours(0xFFFFFFFF, m + 1), light);
        }
        VertexConsumer glow = BossMesh.light(buffer, BossMesh.GLOW);
        RitualMesh.billboard(glow, poseStack, view, c, 3.2f, 0, -1, BossMesh.glow(SandMesh.SUN, .25f), light);
    }

    private static void drawShattered(PoseStack poseStack, MultiBufferSource buffer, WaterMesh.View view, Vector3f c, float h, float since, int shatterTicks,
                                      int seed, int light) {
        float t = Mth.clamp(since / shatterTicks, 0, 1);
        float fade = 1 - t;
        // Shards of glass flying out, falling
        VertexConsumer glass = BossMesh.see(buffer, SandMesh.GLASS);
        PoseStack.Pose p = poseStack.last();
        for (int k = 0; k < 18; k++) {
            float a = BossMesh.hash(seed + k) * Mth.TWO_PI, up = BossMesh.hash(seed + k * 3) * 2 - 1;
            Vector3f d = new Vector3f(Mth.cos(a), up * .6f, Mth.sin(a)).normalize();
            Vector3f pos = new Vector3f(c).add(new Vector3f(d).mul(.6f + 4 * t)).add(0, -5 * t * t, 0);
            RitualMesh.crystal(glass, p, pos, new Vector3f(d).add(0, .5f, 0).normalize(), .4f + .3f * BossMesh.hash(seed + k * 5), .1f, since * .4f + k,
                    BossMesh.argb(SandMesh.GLASS_TINT, .85f * fade), light);
        }
        // The caps falling
        VertexConsumer gold = BossMesh.solid(buffer, SandMesh.GOLD_SKIN);
        p = poseStack.last();
        for (int side = -1; side <= 1; side += 2) {
            Vector3f cap = new Vector3f(c).add(side * .4f * t, side * (h + .08f) - 6 * t * t, 0);
            if (cap.y > -.5f) {
                SandMesh.lathe(gold, p, cap, new float[] {-.1f, -.1f, .1f, .1f}, new float[] {0, .95f, .95f, 0}, 20, 4, 0, 0xFFFFFFFF, light);
            }
        }
        // The sand bursting out
        VertexConsumer dust = BossMesh.see(buffer, BossMesh.DUST);
        for (int k = 0; k < 12; k++) {
            float a = k * Mth.TWO_PI / 12;
            float r = .5f + 3.5f * t;
            SandMesh.cloud(dust, poseStack, view, new Vector3f(c).add(Mth.cos(a) * r, -2.5f * t * t + (BossMesh.hash(seed + k) - .5f), Mth.sin(a) * r), 1.2f + 2 * t, a,
                    k % 2 == 0 ? SandMesh.SAND : SandMesh.SAND_PALE, .8f * fade, light);
        }
        VertexConsumer glow = BossMesh.light(buffer, BossMesh.GLOW);
        RitualMesh.billboard(glow, poseStack, view, c, 3 + 6 * t, 0, -1, BossMesh.glow(0xFFFFFF, fade * fade), light);
    }
}
