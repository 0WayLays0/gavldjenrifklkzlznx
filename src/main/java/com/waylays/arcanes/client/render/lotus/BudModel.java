package com.waylays.arcanes.client.render.lotus;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.waylays.arcanes.client.render.boss.BossMesh;
import com.waylays.arcanes.client.render.ritual.RitualMesh;
import com.waylays.arcanes.client.render.water.WaterMesh;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import org.joml.Vector3f;


/**
 * Lotus Bud: the ground splits and a great bud pushes up round its prey, its broad leaves unrolling on the ground about
 * it; closed, it trembles, light leaking along the seams of its petals and shining up out of its tip. Then it bursts
 * open all at once, a flash and a ring of pollen and light, and stands open a moment before it sinks back.
 */
public final class BudModel {
    /** Rise, hold, burst (ticks), as the scene does them. */
    private static final int RISE = 8, BURST = 18;

    private BudModel() {}

    /** On the ground at the pose's origin, {@code radius} round; {@code life} ticks long. */
    public static void draw(PoseStack poseStack, MultiBufferSource buffer, WaterMesh.View view, float age, int life, float radius, int seed, int light) {
        float rise = BossMesh.ramp(age, 0, RISE);
        float hit = age - BURST;
        float sink = BossMesh.ramp(age, life - 10, life);
        // Closed while it holds; flung open (a little past open, then settling) when it bursts
        float open = .02f + .06f * BossMesh.ramp(age, RISE, BURST);
        if (hit >= 0) {
            float k = 1 - (float) Math.exp(-hit / 1.6f);
            open = Mth.lerp(k, .08f, 1.05f) + .18f * (float) Math.exp(-hit / 3) * Mth.sin(hit * .9f);
        }
        float size = radius * 2 * (.3f + .7f * rise) * (1 - .6f * sink);
        float tremble = hit < 0 && age > RISE ? .04f * Mth.sin(age * 2.7f) * BossMesh.ramp(age, RISE, BURST) : 0;
        Vector3f at = new Vector3f(tremble, Mth.lerp(rise, -size * .9f, 0) - sink * 1.2f, tremble * .6f);

        // ---- its leaves, unrolling on the ground round it
        VertexConsumer leaf = BossMesh.solid(buffer, LotusMesh.LEAF);
        PoseStack.Pose p = poseStack.last();
        for (int k = 0; k < 5; k++) {
            float a = k * Mth.TWO_PI / 5 + BossMesh.hash(seed + k) * .5f;
            float unroll = BossMesh.ramp(age, 2 + k, 10 + k) * (1 - sink);
            float r = radius * (1.25f + .25f * BossMesh.hash(seed + k * 3));
            Vector3f c = new Vector3f(Mth.cos(a) * r, .04f + .01f * k, Mth.sin(a) * r);
            LotusMesh.leaf(leaf, p, c, radius * .7f * unroll, a + age * .002f, .18f, seed + k, BossMesh.argb(0xFFFFFF, 1), light);
        }

        // ---- the flower
        int tip = LotusMesh.TIP;
        float seam = hit < 0 ? BossMesh.ramp(age, RISE, BURST) : (float) Math.exp(-hit / 5);
        LotusMesh.bloom(poseStack, buffer, view, LotusMesh.LOTUS, at, size, open, seed * .3f + age * .004f, age,
                BossMesh.argb(0xFFF6F8, 1), BossMesh.argb(tip, 1), .25f + .6f * seam, LotusMesh.PINK, seed, light);

        if (hit < 0 && age > RISE - 2) {
            // Light shining up out of the closed tip
            float s = BossMesh.ramp(age, RISE - 2, BURST);
            VertexConsumer beam = BossMesh.light(buffer, BossMesh.BEAM);
            Vector3f top = new Vector3f(at).add(0, size * .95f, 0);
            RitualMesh.shaft(beam, p, view.eye(), top, new Vector3f(top).add(0, 3.5f * s, 0), .9f * s, BossMesh.glow(LotusMesh.GOLD, .55f * s), light);
            VertexConsumer glow = BossMesh.light(buffer, BossMesh.GLOW);
            RitualMesh.billboard(glow, poseStack, view, top, 1.4f * s + .1f * Mth.sin(age * 1.3f), 0, -1, BossMesh.glow(LotusMesh.GOLD, .8f * s), light);
        }
        if (hit >= 0) {
            // The burst: a flash, the heart's light, a ring running out over the ground
            float flash = (float) Math.exp(-hit / 3);
            VertexConsumer glow = BossMesh.light(buffer, BossMesh.GLOW);
            Vector3f heart = new Vector3f(at).add(0, size * .35f, 0);
            RitualMesh.billboard(glow, poseStack, view, heart, size * (1.2f + 2.5f * (1 - flash)), 0, -1, BossMesh.glow(0xFFFFFF, flash), light);
            RitualMesh.billboard(glow, poseStack, view, heart, size * 2.4f, 0, -1, BossMesh.glow(LotusMesh.PINK, .5f * flash + .15f * (1 - sink)), light);
            VertexConsumer ring = BossMesh.light(buffer, BossMesh.RING);
            float rr = radius * (1 + 3.5f * (1 - (float) Math.exp(-hit / 4)));
            BossMesh.annulus(ring, p, new Vector3f(0, .08f, 0), new Vector3f(1, 0, 0), new Vector3f(0, 0, -1), rr * .7f, rr, 40, 1, 0, 0,
                    BossMesh.glow(LotusMesh.PINK, 0), BossMesh.glow(LotusMesh.BLUSH, .8f * flash), light, true);
            // Pollen flung out
            VertexConsumer mote = BossMesh.light(buffer, BossMesh.MOTE);
            for (int k = 0; k < 36; k++) {
                float a = BossMesh.hash(seed + k * 7) * Mth.TWO_PI;
                float up = BossMesh.hash(seed + k * 11);
                float dist = (1 - (float) Math.exp(-hit / 5)) * radius * (1.5f + 2 * BossMesh.hash(seed + k * 13));
                Vector3f m = new Vector3f(heart).add(Mth.cos(a) * dist, up * dist * .8f - .02f * hit * hit * .1f, Mth.sin(a) * dist);
                RitualMesh.billboard(mote, poseStack, view, m, .18f, 0, -1, BossMesh.glow(k % 3 == 0 ? LotusMesh.PINK : LotusMesh.GOLD, flash * .9f + .1f), light);
            }
        }
        LotusMesh.motes(poseStack, buffer, view, new Vector3f(0, .2f, 0), radius * 1.6f, 2.5f, age, 10, LotusMesh.GOLD, .6f * rise * (1 - sink), seed, light);
    }
}
