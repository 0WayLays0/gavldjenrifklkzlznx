package com.waylays.arcanes.client.render.lotus;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.waylays.arcanes.client.render.boss.BossMesh;
import com.waylays.arcanes.client.render.boss.Sculpt;
import com.waylays.arcanes.client.render.ritual.RitualMesh;
import com.waylays.arcanes.client.render.water.WaterMesh;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Lotus Mandala: a vast mandala of lotus petals unfolds in pink light on the ground, turning, a smaller one of gold
 * turning the other way at its heart, where a lotus floats and slowly rises. Its three rings flare one after another
 * from the heart outward: each lights up white, a crown of petals of light springs up all round it and a curtain of
 * light rises off its edge.
 */
public final class MandalaModel {
    private static final Vector3f U = new Vector3f(1, 0, 0), V = new Vector3f(0, 0, -1);
    /** As the scene does it: drawn by this tick, rings this many ticks apart, where each ring begins and ends. */
    private static final int DRAWN = 22, STEP = 10;
    private static final float[] BANDS = {0, .36f, .68f, 1};
    private static final int[] CROWN = {8, 14, 20};

    private MandalaModel() {}

    /** On the ground at the pose's origin, {@code radius} round, {@code life} ticks long. */
    public static void draw(PoseStack poseStack, MultiBufferSource buffer, WaterMesh.View view, float age, int life, float radius, int seed, int light) {
        float drawn = BossMesh.ramp(age, 0, DRAWN);
        float fade = 1 - BossMesh.ramp(age, life - 15, life);
        PoseStack.Pose p = poseStack.last();
        // ---- the mandala, unfolding as it turns
        float unfold = 1 - (1 - drawn) * (1 - drawn);
        VertexConsumer mandala = BossMesh.light(buffer, LotusMesh.MANDALA);
        BossMesh.pool(mandala, p, 0, .06f, 0, radius * 2.15f * unfold, age * .006f, BossMesh.glow(LotusMesh.PINK, .55f * fade), light);
        BossMesh.pool(mandala, p, 0, .07f, 0, radius * 1.1f * unfold, -age * .012f + .3f, BossMesh.glow(LotusMesh.GOLD, .45f * fade), light);
        VertexConsumer pool = BossMesh.light(buffer, BossMesh.GLOW);
        BossMesh.pool(pool, p, 0, .05f, 0, radius * 2.6f * unfold, 0, BossMesh.glow(LotusMesh.PINK, .1f * fade), light);

        // ---- the rings flaring in turn
        VertexConsumer ring = BossMesh.light(buffer, BossMesh.RING);
        float[] flare = new float[3];
        for (int k = 0; k < 3; k++) {
            float local = age - (DRAWN + k * STEP);
            flare[k] = BossMesh.ramp(local, -4, 0) * (1 - BossMesh.ramp(local, 2, 18));
            if (flare[k] > .01f) {
                float r0 = BANDS[k] * radius, r1 = BANDS[k + 1] * radius;
                BossMesh.annulus(ring, p, new Vector3f(0, .1f, 0), U, V, Math.max(.01f, r0), r1, 56, 1, 0, 0, BossMesh.glow(LotusMesh.PINK, .3f * flare[k]),
                        BossMesh.glow(LotusMesh.BLUSH, .55f * flare[k]), light, false);
            }
        }
        // Curtains of light rising off each ring's edge
        VertexConsumer beam = BossMesh.light(buffer, BossMesh.BEAM);
        for (int k = 0; k < 3; k++) {
            if (flare[k] <= .01f) {
                continue;
            }
            float r1 = BANDS[k + 1] * radius, h = 1.5f + 2.5f * flare[k];
            int bottom = BossMesh.glow(LotusMesh.PINK, .5f * flare[k]), top = BossMesh.glow(LotusMesh.PINK, 0);
            Sculpt.lathe(beam, p, new float[] {r1, .05f, r1 * 1.02f, h}, 48, 6, .3f, bottom, top, light);
            Sculpt.lathe(beam, p, new float[] {r1 * 1.02f, h, r1, .05f}, 48, 6, .3f, top, bottom, light);
        }
        // Crowns of petals of light springing up
        VertexConsumer ghost = BossMesh.light(buffer, LotusMesh.PETAL);
        for (int k = 0; k < 3; k++) {
            if (flare[k] <= .01f) {
                continue;
            }
            float rm = (BANDS[k] + BANDS[k + 1]) * .5f * radius;
            float local = age - (DRAWN + k * STEP);
            float stand = BossMesh.ramp(local, -3, 3);
            for (int i = 0; i < CROWN[k]; i++) {
                float a = (i + .5f * k) * Mth.TWO_PI / CROWN[k];
                Vector3f out = new Vector3f(Mth.cos(a), 0, Mth.sin(a));
                Quaternionf q = LotusMesh.frame(new Vector3f(out).mul(.35f).add(0, 1, 0), out);
                LotusMesh.ghost(ghost, p, LotusMesh.petal(1, .58f, 0, .5f, .25f, 5, 4).moved(q, new Vector3f(out).mul(rm), (.9f + .4f * k) * stand),
                        BossMesh.glow(i % 2 == 0 ? LotusMesh.PINK : LotusMesh.BLUSH, .6f * flare[k]), light);
            }
        }

        // ---- the lotus at its heart, rising as the rings flare
        float lift = BossMesh.ramp(age, DRAWN - 4, DRAWN + 2 * STEP + 6);
        float size = (1.3f + .7f * lift) * BossMesh.ramp(age, 4, 18) * fade;
        Vector3f heart = new Vector3f(0, .25f + 1.4f * lift + .08f * Mth.sin(age * .1f), 0);
        LotusMesh.bloom(poseStack, buffer, view, LotusMesh.LOTUS, heart, size, .3f + .7f * drawn, age * .01f, age, BossMesh.argb(0xFFF8FA, 1),
                BossMesh.argb(LotusMesh.TIP, 1), .5f + .5f * Math.max(flare[0], Math.max(flare[1], flare[2])), LotusMesh.PINK, seed, light);
        VertexConsumer glow = BossMesh.light(buffer, BossMesh.GLOW);
        RitualMesh.billboard(glow, poseStack, view, new Vector3f(heart).add(0, .3f, 0), 3f * size, 0, -1, BossMesh.glow(LotusMesh.GOLD, .25f * fade), light);
        LotusMesh.motes(poseStack, buffer, view, new Vector3f(0, .1f, 0), radius * unfold, 3, age, 30, LotusMesh.PINK, .8f * fade, seed, light);
        LotusMesh.motes(poseStack, buffer, view, new Vector3f(0, .1f, 0), radius * .5f * unfold, 2.5f, age + 30, 14, LotusMesh.GOLD, .8f * fade, seed + 5, light);
    }
}
