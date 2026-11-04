package com.waylays.arcanes.client.render.lotus;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.waylays.arcanes.client.render.boss.BossMesh;
import com.waylays.arcanes.client.render.boss.Sculpt;
import com.waylays.arcanes.client.render.sand.SandMesh;
import com.waylays.arcanes.client.render.water.WaterMesh;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Petal Storm: a whirlwind of lotus petals round the caster, in three bands (the inner ones faster), every petal
 * tumbling as it flies and climbing as it goes round, white ones and pale pink and deep rose; streaks of pink wind
 * spiral through them, and a ring of pink light lies on the ground under them.
 */
public final class PetalStormModel {
    private static final int PETALS = 170;

    private PetalStormModel() {}

    /** Round the caster (the pose's origin), {@code radius} round, {@code life} ticks long. */
    public static void draw(PoseStack poseStack, MultiBufferSource buffer, WaterMesh.View view, float age, int life, float radius, int seed, int light) {
        float grow = BossMesh.ramp(age, 0, 12);
        float fade = 1 - BossMesh.ramp(age, life - 15, life);
        float k = grow * fade;
        if (k <= .01f) {
            return;
        }
        float scale = radius / 4.5f;
        PoseStack.Pose p = poseStack.last();
        // ---- the petals
        VertexConsumer solid = BossMesh.solid(buffer, LotusMesh.PETAL);
        int[] tips = {LotusMesh.TIP, LotusMesh.PINK, 0xFFF0F6};
        for (int i = 0; i < PETALS; i++) {
            float h = BossMesh.hash(seed + i * 7), h2 = BossMesh.hash(seed + i * 13), h3 = BossMesh.hash(seed + i * 29);
            int band = i % 3;
            float r = (1.5f + band * 1.0f + h * .6f + .25f * Mth.sin(age * .05f + i)) * scale * (.55f + .45f * grow);
            float w = (.2f - band * .035f) * (1 + .3f * h2);
            float a = h * Mth.TWO_PI + age * w;
            float climb = (h2 + age * (.008f + .006f * h3)) % 1;
            float y = .1f + climb * 3.4f;
            // Thinned out where they climb out of the top and drop in at the bottom
            float size = (.36f + .2f * h3) * k * Math.min(1, Math.min(climb * 6, (1 - climb) * 6));
            if (size < .02f) {
                continue;
            }
            Vector3f at = new Vector3f(Mth.cos(a) * r, y, Mth.sin(a) * r);
            Vector3f tangent = new Vector3f(-Mth.sin(a), .25f, Mth.cos(a));
            Quaternionf q = LotusMesh.frame(new Vector3f(tangent).rotateAxis(age * (.15f + .2f * h), Mth.cos(a), 0, Mth.sin(a)),
                    new Vector3f(Mth.cos(a), 0, Mth.sin(a)).rotateAxis(age * (.2f + .3f * h2) + i, tangent.x, tangent.y, tangent.z));
            LotusMesh.petal(1, .62f, .15f, .45f, .15f, 4, 3).moved(q, at, size).draw(solid, p, BossMesh.argb(0xFFF6F8, 1), BossMesh.argb(tips[i % 3], 1), light);
        }
        // ---- streaks of wind spiralling through them
        VertexConsumer beam = BossMesh.light(buffer, BossMesh.BEAM);
        for (int s = 0; s < 6; s++) {
            int n = 18;
            Vector3f[] pts = new Vector3f[n];
            float r = (1.6f + (s % 3) * 1.0f) * scale;
            float a0 = s * 1.9f + age * (.2f - (s % 3) * .035f);
            for (int j = 0; j < n; j++) {
                float t = (float) j / (n - 1);
                float a = a0 - t * 2.2f;
                pts[j] = new Vector3f(Mth.cos(a) * r, .4f + s * .45f + t * .5f, Mth.sin(a) * r);
            }
            int[] col = new int[n];
            for (int j = 0; j < n; j++) {
                float t = (float) j / (n - 1);
                col[j] = BossMesh.glow(LotusMesh.PINK, .45f * k * Mth.sin(t * Mth.PI));
            }
            SandMesh.ribbon(beam, p, view.eye(), pts, Sculpt.fill(.3f, n), 1, 0, col, light);
        }
        // ---- the ring of light under them
        VertexConsumer ring = BossMesh.light(buffer, BossMesh.RING);
        BossMesh.annulus(ring, p, new Vector3f(0, .08f, 0), new Vector3f(1, 0, 0), new Vector3f(0, 0, -1), radius * .85f, radius * 1.05f, 56, 1, age * .02f, 0,
                BossMesh.glow(LotusMesh.PINK, 0), BossMesh.glow(LotusMesh.PINK, .5f * k), light, false);
        VertexConsumer mandala = BossMesh.light(buffer, LotusMesh.MANDALA);
        BossMesh.pool(mandala, p, 0, .07f, 0, radius * 2, -age * .01f, BossMesh.glow(LotusMesh.PINK, .25f * k), light);
    }
}
