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
 * Sacred Pond: clear jade water spreads from the caster's feet, light rippling over its floor; round lotus leaves float
 * on it, bobbing, lotus flowers open on some of them and buds stand on their stems; rings of ripples spread here and
 * there, its rim glows pink, and golden motes rise off it like fireflies.
 */
public final class PondModel {
    private static final Vector3f U = new Vector3f(1, 0, 0), V = new Vector3f(0, 0, -1);

    private PondModel() {}

    /** On the ground at the pose's origin, {@code radius} round, {@code life} ticks long. */
    public static void draw(PoseStack poseStack, MultiBufferSource buffer, WaterMesh.View view, float age, int life, float radius, int seed, int light) {
        float spread = BossMesh.ramp(age, 0, 16);
        float fade = 1 - BossMesh.ramp(age, life - 20, life);
        float r = radius * spread;
        if (r < .05f || fade <= 0) {
            return;
        }
        PoseStack.Pose p = poseStack.last();

        // ---- the water: its body, its edge melting into the ground, light rippling over it
        VertexConsumer water = BossMesh.ground(buffer, WaterMesh.WATER);
        int deep = BossMesh.argb(0x2E9C94, .78f * fade), clear = BossMesh.argb(0x6FD8C8, .5f * fade);
        BossMesh.annulus(water, p, new Vector3f(0, .07f, 0), U, V, 0, r * .9f, 48, 3, age * .002f, age * .01f, deep, clear, light, false);
        BossMesh.annulus(water, p, new Vector3f(0, .07f, 0), U, V, r * .9f, r, 48, 3, age * .002f, age * .01f, clear, BossMesh.argb(0x9FE8DC, 0), light, false);
        VertexConsumer caustic = BossMesh.light(buffer, WaterMesh.CAUSTICS);
        BossMesh.annulus(caustic, p, new Vector3f(0, .08f, 0), U, V, 0, r * .95f, 48, 4, -age * .003f, age * .006f, BossMesh.glow(0x8FF0E0, .22f * fade),
                BossMesh.glow(0x8FF0E0, .1f * fade), light, false);
        // Rings of ripples spreading here and there
        VertexConsumer ring = BossMesh.light(buffer, BossMesh.RING);
        for (int k = 0; k < 5; k++) {
            float cycle = 46 + 10 * BossMesh.hash(seed + k);
            float t = ((age + k * 17) % cycle) / cycle;
            int round = (int) ((age + k * 17) / cycle);
            float a = BossMesh.hash(seed + k * 31 + round * 7) * Mth.TWO_PI, d = r * .75f * (float) Math.sqrt(BossMesh.hash(seed + k * 13 + round * 3));
            float rr = .2f + 1.6f * t;
            BossMesh.annulus(ring, p, new Vector3f(Mth.cos(a) * d, .1f, Mth.sin(a) * d), U, V, rr * .8f, rr, 28, 1, 0, 0, BossMesh.glow(0xFFFFFF, 0),
                    BossMesh.glow(0xDFFFF8, .45f * (1 - t) * fade), light, false);
        }
        // Its rim glowing pink
        BossMesh.annulus(ring, p, new Vector3f(0, .09f, 0), U, V, r * .93f, r * 1.04f, 64, 1, 0, 0, BossMesh.glow(LotusMesh.PINK, 0),
                BossMesh.glow(LotusMesh.PINK, .55f * fade), light, false);

        // ---- the leaves floating on it
        VertexConsumer leaf = BossMesh.solid(buffer, LotusMesh.LEAF);
        Vector3f[] spots = new Vector3f[9];
        for (int k = 0; k < spots.length; k++) {
            float a = k * 2.39996f + BossMesh.hash(seed + k) * .5f;
            float d = radius * .82f * (float) Math.sqrt((k + .5f) / spots.length);
            float bob = .015f * Mth.sin(age * .08f + k * 1.7f);
            spots[k] = new Vector3f(Mth.cos(a) * d * spread, .11f + bob, Mth.sin(a) * d * spread);
            float size = (.5f + .45f * BossMesh.hash(seed + k * 5)) * BossMesh.ramp(age, 4 + k, 14 + k) * fade;
            LotusMesh.leaf(leaf, p, spots[k], size, a + age * .003f * (k % 2 == 0 ? 1 : -1), .06f, seed + k, BossMesh.argb(0xFFFFFF, 1), light);
        }
        // ---- buds standing on their stems out of the water
        VertexConsumer fibre = BossMesh.solid(buffer, LotusMesh.FIBRE);
        Vector3f[] buds = new Vector3f[2];
        for (int k = 0; k < 2; k++) {
            float a = 1.1f + k * 3.4f + BossMesh.hash(seed + 90 + k);
            float d = radius * (.45f + .25f * k) * spread;
            float sway = .06f * Mth.sin(age * .05f + k * 2);
            Vector3f foot = new Vector3f(Mth.cos(a) * d, .05f, Mth.sin(a) * d);
            float h = (.9f + .3f * k) * BossMesh.ramp(age, 8 + 4 * k, 22 + 4 * k) * fade;
            buds[k] = new Vector3f(foot).add(sway, h, sway * .5f);
            LotusMesh.stem(fibre, p, LotusMesh.path(foot, new Vector3f(foot).add(0, h * .4f, 0), new Vector3f(buds[k]).add(0, -h * .3f, 0), buds[k], 6), .035f,
                    light);
        }
        for (int k = 0; k < 2; k++) {
            float h = BossMesh.ramp(age, 8 + 4 * k, 22 + 4 * k) * fade;
            LotusMesh.bloom(poseStack, buffer, view, LotusMesh.LOTUS, buds[k], .5f * h, .06f + .04f * Mth.sin(age * .03f + k), k * 1.3f, age,
                    BossMesh.argb(0xFFF6F8, 1), BossMesh.argb(LotusMesh.TIP, 1), 0, 0, seed + k, light);
        }
        // ---- flowers open on some of the leaves
        int[] on = {0, 3, 5, 8};
        for (int i = 0; i < on.length; i++) {
            int k = on[i];
            float open = BossMesh.ramp(age, 14 + 5 * i, 40 + 5 * i);
            float size = (.55f + .2f * BossMesh.hash(seed + k * 9)) * BossMesh.ramp(age, 6 + k, 16 + k) * fade;
            int tip = i % 2 == 0 ? LotusMesh.PINK : BossMesh.mix(LotusMesh.PINK, 0xFFFFFF, .45f);
            LotusMesh.bloom(poseStack, buffer, view, LotusMesh.LOTUS, new Vector3f(spots[k]).add(0, .02f, 0), size, .15f + .8f * open, k * .7f + age * .003f, age,
                    BossMesh.argb(0xFFF8FA, 1), BossMesh.argb(tip, 1), .25f * open * fade, LotusMesh.PINK, seed + k, light);
        }

        // ---- fireflies of gold and pink motes rising off it
        LotusMesh.motes(poseStack, buffer, view, new Vector3f(0, .15f, 0), r * .9f, 2.6f, age, 26, LotusMesh.GOLD, .9f * fade, seed, light);
        LotusMesh.motes(poseStack, buffer, view, new Vector3f(0, .15f, 0), r * .9f, 1.6f, age + 50, 12, LotusMesh.PINK, .7f * fade, seed + 7, light);
        VertexConsumer glow = BossMesh.light(buffer, BossMesh.GLOW);
        RitualMesh.billboard(glow, poseStack, view, new Vector3f(0, .4f, 0), r * 1.2f, 0, -1, BossMesh.glow(0x7FE8D8, .08f * fade), light);
    }
}
