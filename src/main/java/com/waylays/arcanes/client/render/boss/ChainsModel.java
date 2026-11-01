package com.waylays.arcanes.client.render.boss;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.waylays.arcanes.client.render.fire.FireMesh;
import com.waylays.arcanes.client.render.ritual.RitualMesh;
import com.waylays.arcanes.client.render.water.WaterMesh;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

/**
 * The Chains of the Damned: four gates of red runes open in the ground round the doomed spot and black chains burst out
 * of them, arcing up and in to a seal of runes over its middle, then drawing taut, red light running along them toward
 * it. When the sentence falls the chains shatter, their links flung away, and a column of hellfire roars up.
 */
public final class ChainsModel {
    /** How long the hellfire burns (ticks). */
    public static final int BURN = 20;
    private static final int CHAINS = 4;
    private static final float LINK = .56f;
    private static final int RED = 0xFF2A30;

    private ChainsModel() {}

    /**
     * Drawn from the middle of the doomed spot (the pose's origin, on the ground), {@code radius} across; {@code age}
     * ticks since the chains burst out, {@code until} ticks until the sentence falls (negative after).
     */
    public static void draw(PoseStack poseStack, MultiBufferSource buffer, WaterMesh.View view, float radius, float age, float until, int seed, int light) {
        float since = -until;
        float shatter = until < 0 ? BossMesh.ramp(since, 0, 14) : 0;
        float reach = BossMesh.ramp(age, 0, 12);
        float taut = BossMesh.ramp(age, 8, 30);
        float gates = BossMesh.ramp(age, 0, 6) * (1 - BossMesh.ramp(since, 4, 16));
        Vector3f bind = new Vector3f(0, 1.15f, 0);
        float a0 = BossMesh.hash(seed) * Mth.TWO_PI;

        // The gates
        VertexConsumer runes = BossMesh.light(buffer, BossMesh.RUNES);
        for (int k = 0; k < CHAINS; k++) {
            float a = a0 + k * Mth.HALF_PI;
            BossMesh.pool(runes, poseStack.last(), Mth.cos(a) * radius * 1.05f, .05f, Mth.sin(a) * radius * 1.05f, 1.9f, age * .08f, BossMesh.glow(RED, .9f * gates), light);
        }
        VertexConsumer glow = BossMesh.light(buffer, BossMesh.GLOW);
        for (int k = 0; k < CHAINS; k++) {
            float a = a0 + k * Mth.HALF_PI;
            BossMesh.pool(glow, poseStack.last(), Mth.cos(a) * radius * 1.05f, .06f, Mth.sin(a) * radius * 1.05f, 2.6f, 0, BossMesh.glow(0xB01018, .8f * gates), light);
        }

        // The chains
        Vector3f[][] paths = new Vector3f[CHAINS][];
        for (int k = 0; k < CHAINS; k++) {
            float a = a0 + k * Mth.HALF_PI;
            Vector3f anchor = new Vector3f(Mth.cos(a) * radius * 1.05f, -.3f, Mth.sin(a) * radius * 1.05f);
            Vector3f slack = new Vector3f(anchor).mul(.6f).add(0, 3.2f, 0);
            Vector3f tight = new Vector3f(anchor).add(bind).mul(.5f).add(0, .5f, 0);
            Vector3f ctrl = new Vector3f(slack).lerp(tight, taut);
            int n = 32;
            Vector3f[] path = new Vector3f[n + 1];
            for (int i = 0; i <= n; i++) {
                float t = (float) i / n * reach;
                float u = 1 - t;
                Vector3f p = new Vector3f(anchor).mul(u * u).add(new Vector3f(ctrl).mul(2 * u * t)).add(new Vector3f(bind).mul(t * t));
                // Rattling as it strains
                float shiver = .06f * taut * Mth.sin(age * 1.3f + t * 11 + k) * Mth.sin(t * Mth.PI);
                p.add(-Mth.sin(a) * shiver, shiver * .5f, Mth.cos(a) * shiver);
                path[i] = p;
            }
            paths[k] = path;
        }
        VertexConsumer iron = BossMesh.see(buffer, BossMesh.CHAIN);
        PoseStack.Pose p = poseStack.last();
        for (int k = 0; k < CHAINS; k++) {
            links(iron, p, paths[k], bind, since, shatter, seed + k * 101, light);
        }
        if (shatter < 1) {
            // Red light running along them toward the seal
            VertexConsumer heat = BossMesh.light(buffer, BossMesh.BEAM);
            p = poseStack.last();
            for (int k = 0; k < CHAINS; k++) {
                Vector3f[] path = paths[k];
                int[] col = new int[path.length];
                for (int i = 0; i < path.length; i++) {
                    float t = (float) i / (path.length - 1);
                    float pulse = Mth.clamp(1 - Math.abs(((age * .045f + k * .25f) % 1) - t) * 6, 0, 1);
                    col[i] = BossMesh.glow(RED, (.4f + .6f * pulse) * (1 - shatter));
                }
                RitualMesh.thread(heat, p, view.eye(), path, .75f, col, light);
            }
            // The seal they bind to
            float seal = reach * (1 - shatter);
            VertexConsumer circle = BossMesh.light(buffer, BossMesh.RUNES);
            RitualMesh.billboard(circle, poseStack, view, bind, 1.9f * seal, age * .1f, -1, BossMesh.glow(RED, .95f * seal), light);
            BossMesh.plate(circle, poseStack.last(), bind, new Vector3f(Mth.cos(age * .07f), 0, Mth.sin(age * .07f)),
                    new Vector3f(-Mth.sin(age * .07f), 0, Mth.cos(age * .07f)), 2.6f * seal, BossMesh.glow(0xFF6040, .6f * seal), light);
            VertexConsumer core = BossMesh.light(buffer, BossMesh.GLOW);
            RitualMesh.billboard(core, poseStack, view, bind, 1.6f * seal, 0, -1, BossMesh.glow(0xFFC0A0, .8f * seal), light);
        }

        // The hellfire
        if (until <= 0 && since < BURN + 6) {
            float rise = Mth.sin(Mth.PI * Mth.clamp(since / BURN, 0, 1));
            FireMesh.Batch fire = new FireMesh.Batch(view, 0, age, light);
            for (int j = 0; j < 3; j++) {
                float r = radius * (.2f + .3f * j);
                int n = Math.max(5, (int) (Mth.TWO_PI * r / .8f));
                for (int i = 0; i < n; i++) {
                    float a = i * Mth.TWO_PI / n + j;
                    fire.lick(Mth.cos(a) * r, 0, Mth.sin(a) * r, 1.1f, (5.5f - 1.4f * j) * rise * (.8f + .4f * BossMesh.hash(seed + i * 3 + j)), 0, seed + i * 7 + j * 13,
                            rise);
                }
            }
            fire.flush(buffer, poseStack);
            VertexConsumer column = BossMesh.light(buffer, BossMesh.BEAM);
            RitualMesh.shaft(column, poseStack.last(), view.eye(), new Vector3f(), new Vector3f(0, 12 * rise + 2, 0), radius * 1.2f, BossMesh.glow(RED, .8f * rise), light);
            RitualMesh.shaft(column, poseStack.last(), view.eye(), new Vector3f(), new Vector3f(0, 9 * rise + 2, 0), radius * .5f, BossMesh.glow(0xFFD0A0, .8f * rise),
                    light);
        }
    }

    /** The links along {@code path}, each square to the one before; flung off from the seal once it shatters. */
    private static void links(VertexConsumer c, PoseStack.Pose p, Vector3f[] path, Vector3f bind, float since, float shatter, int seed, int light) {
        float[] along = new float[path.length];
        for (int i = 1; i < path.length; i++) {
            along[i] = along[i - 1] + path[i].distance(path[i - 1]);
        }
        float total = along[path.length - 1];
        int count = (int) (total / (LINK * .78f));
        int seg = 0;
        for (int j = 0; j < count; j++) {
            float s = (j + .5f) * LINK * .78f;
            while (seg + 2 < path.length && along[seg + 1] < s) {
                seg++;
            }
            float t = (s - along[seg]) / Math.max(1.0E-4f, along[seg + 1] - along[seg]);
            Vector3f at = new Vector3f(path[seg]).lerp(path[seg + 1], Mth.clamp(t, 0, 1));
            Vector3f dir = new Vector3f(path[seg + 1]).sub(path[seg]).normalize();
            Vector3f side = BossMesh.across(dir);
            if (j % 2 == 1) {
                side = new Vector3f(dir).cross(side).normalize();
            }
            float alpha = 1;
            if (shatter > 0) {
                Vector3f out = new Vector3f(at).sub(bind);
                out.y = Math.abs(out.y) + .5f;
                out.normalize().mul(.25f + .2f * BossMesh.hash(seed + j));
                at = WaterMesh.thrown(at, out, since, .04f);
                dir.rotateY(since * .3f * (BossMesh.hash(seed + j * 3) - .5f));
                alpha = 1 - shatter;
            }
            if (alpha <= .01f) {
                continue;
            }
            Vector3f h = new Vector3f(dir).mul(LINK * .5f), w = new Vector3f(side).mul(LINK * .26f);
            int col = BossMesh.argb(0xC8B8C0, alpha);
            BossMesh.quad(c, p, new Vector3f(at).sub(h).sub(w), new Vector3f(at).sub(h).add(w), new Vector3f(at).add(h).add(w), new Vector3f(at).add(h).sub(w), 0, 1,
                    1, 1, 1, 0, 0, 0, col, col, light, new Vector3f(dir).cross(side).normalize());
        }
    }
}
