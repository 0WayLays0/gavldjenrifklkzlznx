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
 * The Thousand-Petal Lotus: a pond of light opens, a mandala burning on it, and out of it rises an immense bud that
 * opens ring after ring, fifty petals blushing from white to pink, edged in gold, round a heart that grows into a sun.
 * In full bloom a pillar of light falls through its heart to the ground and rings of light run out over the field;
 * then its petals rain down, each a blade of light falling on a foe, while small lotus float round the pond. At last it
 * closes, sinks and fades.
 */
public final class ThousandModel {
    private static final Vector3f U = new Vector3f(1, 0, 0), V = new Vector3f(0, 0, -1);
    /** As the scene does it: full bloom, the end of the rain of petals, how high it floats. */
    private static final int BLOOM = 50, RAIN_END = 110;
    private static final float HEIGHT = 3.5f;
    /** A petal falls on its foe in this many ticks. */
    private static final float FALL = 7;

    private ThousandModel() {}

    /** Over the pond at the pose's origin, {@code radius} round; its last petal fell {@code since} ticks ago on {@code target} (or null). */
    public static void draw(PoseStack poseStack, MultiBufferSource buffer, WaterMesh.View view, float age, int life, float radius, float since, Vector3f target,
                            int seed, int light) {
        float spread = BossMesh.ramp(age, 0, 16);
        float end = BossMesh.ramp(age, life - 40, life);
        float fade = 1 - BossMesh.ramp(age, life - 15, life);
        float rise = BossMesh.ramp(age, 6, 32);
        float hit = age - BLOOM;
        float open = Mth.lerp(end, BossMesh.ramp(age, 26, BLOOM), .25f);
        float size = radius * .6f * (.35f + .65f * rise) * (1 - .8f * end);
        Vector3f base = new Vector3f(0, Mth.lerp(rise, -2.5f, HEIGHT) - 2.5f * end, 0);
        PoseStack.Pose p = poseStack.last();

        // ---- the pond of light, a mandala burning on it
        VertexConsumer water = BossMesh.ground(buffer, WaterMesh.WATER);
        float r = radius * spread;
        BossMesh.annulus(water, p, new Vector3f(0, .07f, 0), U, V, 0, r * .9f, 56, 4, age * .002f, age * .01f, BossMesh.argb(0x3A9CB0, .6f * fade),
                BossMesh.argb(0x8FE0E8, .4f * fade), light, false);
        BossMesh.annulus(water, p, new Vector3f(0, .07f, 0), U, V, r * .9f, r, 56, 4, age * .002f, age * .01f, BossMesh.argb(0x8FE0E8, .4f * fade),
                BossMesh.argb(0x8FE0E8, 0), light, false);
        VertexConsumer mandala = BossMesh.light(buffer, LotusMesh.MANDALA);
        BossMesh.pool(mandala, p, 0, .09f, 0, r * 2.1f, age * .004f, BossMesh.glow(LotusMesh.PINK, .7f * fade), light);
        BossMesh.pool(mandala, p, 0, .1f, 0, r * 1.2f, -age * .009f, BossMesh.glow(LotusMesh.GOLD, .55f * fade), light);
        VertexConsumer caustic = BossMesh.light(buffer, WaterMesh.CAUSTICS);
        BossMesh.annulus(caustic, p, new Vector3f(0, .11f, 0), U, V, 0, r * .95f, 56, 6, -age * .002f, age * .006f, BossMesh.glow(0xFFE0F0, .2f * fade),
                BossMesh.glow(0xFFE0F0, .08f * fade), light, false);

        // ---- small lotus floating round its rim
        VertexConsumer leaf = BossMesh.solid(buffer, LotusMesh.LEAF);
        Vector3f[] small = new Vector3f[8];
        for (int k = 0; k < small.length; k++) {
            float a = k * Mth.TWO_PI / small.length + .3f + age * .003f;
            small[k] = new Vector3f(Mth.cos(a) * r * .82f, .12f + .02f * Mth.sin(age * .08f + k), Mth.sin(a) * r * .82f);
            LotusMesh.leaf(leaf, p, small[k], .7f * spread * fade, a, .06f, seed + k, BossMesh.argb(0xFFFFFF, 1), light);
        }
        for (int k = 0; k < small.length; k++) {
            float s = BossMesh.ramp(age, 10 + k * 2, 24 + k * 2) * fade;
            LotusMesh.bloom(poseStack, buffer, view, LotusMesh.LOTUS, new Vector3f(small[k]).add(0, .02f, 0), .55f * s, .5f + .5f * open, k, age,
                    BossMesh.argb(0xFFF8FA, 1), BossMesh.argb(LotusMesh.TIP, 1), .3f * s, LotusMesh.PINK, seed + k, light);
        }

        // ---- the great lotus
        if (size > .05f) {
            float glowEdge = .35f + .5f * open + (hit >= 0 ? .8f * (float) Math.exp(-hit / 8) : 0);
            LotusMesh.bloom(poseStack, buffer, view, LotusMesh.THOUSAND, base, size, open, age * .004f, age,
                    BossMesh.argb(0xFFFFFF, 1), BossMesh.argb(0xFFF0F6, 1), glowEdge * fade, LotusMesh.GOLD, seed, light);
            // Its heart, growing into a sun
            float sun = BossMesh.ramp(age, 20, BLOOM) * (1 - end) + (hit >= 0 ? 2 * (float) Math.exp(-hit / 6) : 0);
            Vector3f heart = new Vector3f(base).add(0, size * .3f, 0);
            VertexConsumer glow = BossMesh.light(buffer, BossMesh.GLOW);
            RitualMesh.billboard(glow, poseStack, view, heart, size * (.8f + .6f * sun), 0, -1, BossMesh.glow(0xFFFFFF, Math.min(1, .5f * sun)), light);
            RitualMesh.billboard(glow, poseStack, view, heart, size * (2 + 1.5f * sun), 0, -1, BossMesh.glow(LotusMesh.GOLD, Math.min(1, .35f * sun)), light);
            RitualMesh.billboard(glow, poseStack, view, heart, size * 4.5f, 0, -1, BossMesh.glow(LotusMesh.PINK, .12f * (1 - end) * fade), light);
            // Rays turning round its heart
            VertexConsumer beam = BossMesh.light(buffer, BossMesh.BEAM);
            for (int k = 0; k < 12; k++) {
                float a = k * Mth.TWO_PI / 12 + age * .01f;
                Vector3f out = new Vector3f(Mth.cos(a), .25f, Mth.sin(a)).normalize();
                RitualMesh.shaft(beam, p, view.eye(), heart, new Vector3f(heart).add(new Vector3f(out).mul(size * (.9f + .3f * Math.min(1, sun)))), size * .14f,
                        BossMesh.glow(LotusMesh.GOLD, .16f * Math.min(1, sun)), light);
            }
        }

        // ---- full bloom: the pillar of light, rings running out over the field
        if (hit >= 0 && hit < 40) {
            float t = hit / 40;
            float strength = (float) Math.exp(-hit / 10);
            VertexConsumer beam = BossMesh.light(buffer, BossMesh.BEAM);
            float w = (3.2f - 2.2f * t) * (radius / 10);
            int core = BossMesh.glow(0xFFFFFF, strength), body = BossMesh.glow(LotusMesh.GOLD, strength * .8f);
            for (int side = 0; side < 2; side++) {
                float[] prof = side == 0 ? new float[] {w, 0, w, 40} : new float[] {w, 40, w, 0};
                Sculpt.lathe(beam, p, prof, 32, 4, .05f, body, body, light);
                float[] in = side == 0 ? new float[] {w * .4f, 0, w * .4f, 40} : new float[] {w * .4f, 40, w * .4f, 0};
                Sculpt.lathe(beam, p, in, 24, 4, .05f, core, core, light);
            }
            VertexConsumer ring = BossMesh.light(buffer, BossMesh.RING);
            for (int k = 0; k < 3; k++) {
                float rt = Mth.clamp((hit - k * 4) / 22, 0, 1);
                if (rt <= 0 || rt >= 1) {
                    continue;
                }
                float rr = radius * (.2f + 1.3f * rt);
                BossMesh.annulus(ring, p, new Vector3f(0, .15f, 0), U, V, rr * .8f, rr, 64, 1, 0, 0, BossMesh.glow(LotusMesh.GOLD, 0),
                        BossMesh.glow(k == 0 ? 0xFFFFFF : LotusMesh.PINK, .9f * (1 - rt)), light, true);
            }
        }

        // ---- the rain of petals: one falling on its foe, others drifting down all round
        VertexConsumer solid = BossMesh.solid(buffer, LotusMesh.PETAL);
        if (hit > 0 && age < RAIN_END + 20) {
            float drift = BossMesh.ramp(age, BLOOM, BLOOM + 10) * (1 - BossMesh.ramp(age, RAIN_END, RAIN_END + 20));
            for (int k = 0; k < 40; k++) {
                float h = BossMesh.hash(seed + k * 7);
                float t = ((age - BLOOM) * (.012f + .01f * h) + h) % 1;
                float a = BossMesh.hash(seed + k * 11) * Mth.TWO_PI + t * 3;
                float d = radius * (.25f + .7f * BossMesh.hash(seed + k * 3));
                Vector3f at = new Vector3f(Mth.cos(a) * d, HEIGHT + 3 - t * (HEIGHT + 3), Mth.sin(a) * d);
                Quaternionf q = LotusMesh.frame(new Vector3f(0, 1, 0).rotateX(age * .1f + k).rotateY(k), new Vector3f(1, 0, 0).rotateY(age * .05f + k));
                LotusMesh.petal(1, .62f, .2f, .5f, .2f, 4, 3).moved(q, at, .55f * drift * Math.min(1, (1 - t) * 5)).draw(solid, p, BossMesh.argb(0xFFF4E8, 1),
                        BossMesh.argb(LotusMesh.TIP, 1), light);
            }
        }
        if (target != null && since >= 0 && since < FALL + 8 && hit > 0) {
            float t = Math.min(1, since / FALL);
            Vector3f from = new Vector3f(target).mul(.3f).add(0, HEIGHT + 2.5f, 0);
            if (t < 1) {
                Vector3f at = new Vector3f(from).lerp(target, t * t);
                Vector3f d = new Vector3f(target).sub(from).normalize();
                Quaternionf q = LotusMesh.frame(d, BossMesh.across(d));
                LotusMesh.Petal pt = LotusMesh.petal(1, .58f, 0, .35f, -.1f).moved(q, new Vector3f(at).sub(new Vector3f(d).mul(.8f)), 1.6f);
                pt.draw(BossMesh.solid(buffer, LotusMesh.PETAL), p, BossMesh.argb(0xFFF4E8, 1), BossMesh.argb(LotusMesh.TIP, 1), light);
                VertexConsumer beam = BossMesh.light(buffer, BossMesh.BEAM);
                pt.rim(beam, p, view.eye(), .1f, BossMesh.glow(LotusMesh.GOLD, .9f), light);
                RitualMesh.shaft(beam, p, view.eye(), at, new Vector3f(at).sub(new Vector3f(d).mul(3)), .7f, BossMesh.glow(LotusMesh.PINK, .5f), light);
            } else {
                float b = (since - FALL) / 8;
                VertexConsumer flash = BossMesh.light(buffer, BossMesh.GLOW);
                RitualMesh.billboard(flash, poseStack, view, target, 1 + 2.5f * b, 0, -1, BossMesh.glow(LotusMesh.GOLD, (1 - b) * .9f), light);
            }
        }
        LotusMesh.motes(poseStack, buffer, view, new Vector3f(0, .2f, 0), r, 7, age, 40, LotusMesh.GOLD, .9f * fade, seed, light);
        LotusMesh.motes(poseStack, buffer, view, new Vector3f(0, .2f, 0), r, 5, age + 40, 30, LotusMesh.PINK, .8f * fade, seed + 3, light);
    }
}
