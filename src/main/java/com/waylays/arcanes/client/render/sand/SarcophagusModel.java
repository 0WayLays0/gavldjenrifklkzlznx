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
 * The Sarcophagus: bands of old linen whip up out of a swirl of sand and wind round their victim from its feet to its
 * head, a band of golden glyphs glowing round it; when the curse ends they tear loose and fly apart in a gust of sand.
 */
public final class SarcophagusModel {
    private static final int BANDS = 3;

    private SarcophagusModel() {}

    /** At the victim's feet (the pose's origin), {@code height} tall; {@code age} of {@code life} ticks. */
    public static void draw(PoseStack poseStack, MultiBufferSource buffer, WaterMesh.View view, float height, float age, float life, int seed, int light) {
        float wrap = BossMesh.ramp(age, 0, 14);
        float tear = BossMesh.ramp(age, life - 10, life);
        float r = .55f;
        VertexConsumer swirl = BossMesh.ground(buffer, SandMesh.SWIRL);
        BossMesh.pool(swirl, poseStack.last(), 0, .04f, 0, 2.6f, age * .12f, BossMesh.argb(0xFFFFFF, .8f * (1 - tear)), light);
        VertexConsumer linen = BossMesh.see(buffer, SandMesh.LINEN);
        PoseStack.Pose p = poseStack.last();
        for (int b = 0; b < BANDS; b++) {
            int n = 40;
            Vector3f[] pts = new Vector3f[n + 1], sides = new Vector3f[n + 1];
            float[] w = new float[n + 1];
            int[] col = new int[n + 1];
            for (int i = 0; i <= n; i++) {
                float t = (float) i / n;
                float reach = Math.min(t, wrap);
                float a = reach * Mth.TWO_PI * 3.2f + b * Mth.TWO_PI / BANDS;
                float y = reach * height;
                // Tearing loose: each band flung outward and up, unwinding
                float fly = tear * (1 + 2 * t);
                float rr = r + .03f * Mth.sin(t * 40 + b) + fly;
                pts[i] = new Vector3f(Mth.cos(a) * rr, y + tear * t * 1.5f, Mth.sin(a) * rr);
                sides[i] = new Vector3f(0, 1, 0);
                w[i] = t <= wrap ? .32f : 0;
                col[i] = BossMesh.argb(0xFFFFFF, (1 - tear) * (t <= wrap ? 1 : 0));
            }
            WaterMesh.ribbon(linen, p, pts, sides, w, col, .9f, 0, light, true);
        }
        // A band of golden glyphs round it
        VertexConsumer glyphs = BossMesh.light(buffer, SandMesh.GLYPHS);
        p = poseStack.last();
        int n = 24;
        float gy = height * .55f;
        for (int i = 0; i < n; i++) {
            float a0 = i * Mth.TWO_PI / n + age * .04f, a1 = (i + 1) * Mth.TWO_PI / n + age * .04f;
            float gr = r + .25f;
            Vector3f q0 = new Vector3f(Mth.cos(a0) * gr, gy - .18f, Mth.sin(a0) * gr), q1 = new Vector3f(Mth.cos(a1) * gr, gy - .18f, Mth.sin(a1) * gr);
            Vector3f q2 = new Vector3f(q1).add(0, .36f, 0), q3 = new Vector3f(q0).add(0, .36f, 0);
            float u0 = (float) i / n * 3, u1 = (float) (i + 1) / n * 3;
            int c = BossMesh.glow(SandMesh.GOLD, .9f * wrap * (1 - tear));
            BossMesh.quad2(glyphs, p, q0, q1, q2, q3, u0, 1, u1, 1, u1, 0, u0, 0, c, c, light);
        }
        VertexConsumer glow = BossMesh.light(buffer, BossMesh.GLOW);
        RitualMesh.billboard(glow, poseStack, view, new Vector3f(0, height + .1f, 0), .9f, 0, -1, BossMesh.glow(SandMesh.GOLD, .7f * wrap * (1 - tear)), light);
        if (tear > 0) {
            SandMesh.spray(poseStack, buffer, view, 2.5f, 2, tear, 12, seed, light);
        }
    }
}
