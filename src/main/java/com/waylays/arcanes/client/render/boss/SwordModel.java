package com.waylays.arcanes.client.render.boss;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.waylays.arcanes.client.render.ritual.RitualMesh;
import com.waylays.arcanes.client.render.water.WaterMesh;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

/**
 * Thousand Blades: one of the swords of light. It forms in the air in a flash (high over its caster, among the others),
 * hangs there pointing at the spot it is meant for, then dives at it, streaking, and drives itself into the ground
 * point first; it stands there a moment, then breaks into motes of light.
 */
public final class SwordModel {
    /** How long its dive takes (ticks), ending as its warning runs out. */
    public static final int DIVE = 9;
    /** How long it stands in the ground after (ticks). */
    public static final int STAND = 18;
    private static final int BLADE_STEPS = 7;

    private SwordModel() {}

    /**
     * Drawn from the spot it is meant for (the pose's origin, on the ground); {@code from} where it hangs (in the same
     * frame), {@code age} ticks since it appeared, {@code until} ticks until it strikes (negative after),
     * {@code length} its length, {@code color} its light.
     */
    public static void draw(PoseStack poseStack, MultiBufferSource buffer, WaterMesh.View view, Vector3f from, float age, float until, float length, int color,
                            int seed, int light) {
        float appear = BossMesh.ramp(age, 0, 8);
        // Hanging point up, leaning toward its mark
        Vector3f lean = new Vector3f(-from.x, 0, -from.z);
        if (lean.lengthSquared() > 1.0E-6f) {
            lean.normalize().mul(.3f);
        }
        Vector3f raised = new Vector3f(0, 1, 0).add(lean).normalize();
        float bob = .18f * Mth.sin(age * .17f + seed);
        Vector3f hang = new Vector3f(raised).mul(length * .5f).add(from).add(0, bob, 0);
        Vector3f tip, dir;
        float alpha = 1;
        float speed = 0;
        float f = 0;
        if (until > DIVE) {
            tip = hang;
            dir = raised;
        } else if (until > 0) {
            // Up, over, and straight down onto its mark
            f = 1 - until / DIVE;
            float e = f * f * (1.6f - .6f * f);
            tip = path(hang, raised, length, e);
            dir = new Vector3f(path(hang, raised, length, Math.min(1, e + .02f))).sub(path(hang, raised, length, Math.max(0, e - .02f))).normalize();
            speed = f;
        } else {
            tip = new Vector3f(0, -length * .2f, 0);
            dir = new Vector3f(0, -1, 0);
            alpha = 1 - BossMesh.ramp(-until, STAND - 7, STAND);
        }
        float scale = .35f + .65f * appear;
        float l = length * scale;
        Vector3f flat = BossMesh.across(dir);
        // Turned a little about itself, so the swords of a volley do not all show the same face
        float roll = (BossMesh.hash(seed) - .5f) * 1.2f + (until > DIVE ? .15f * Mth.sin(age * .07f + seed) : 0);
        Vector3f thick = new Vector3f(dir).cross(flat).normalize();
        flat = new Vector3f(flat).mul(Mth.cos(roll)).add(new Vector3f(thick).mul(Mth.sin(roll)));
        thick = new Vector3f(dir).cross(flat).normalize();
        if (alpha > .01f) {
            sword(poseStack, buffer, view, tip, dir, flat, thick, l, color, alpha * appear, light);
        }
        // Streaking along its way
        if (speed > 0) {
            int n = 10;
            Vector3f[] trail = new Vector3f[n];
            int[] col = new int[n];
            int[] core = new int[n];
            float e = f * f * (1.6f - .6f * f);
            for (int i = 0; i < n; i++) {
                float t = (float) i / (n - 1);
                trail[i] = path(hang, raised, length, Math.max(0, e - t * .45f));
                col[i] = BossMesh.glow(color, (1 - t) * (.5f + .4f * speed));
                core[i] = BossMesh.glow(0xFFFFFF, (1 - t) * (1 - t) * .8f);
            }
            VertexConsumer streak = BossMesh.light(buffer, BossMesh.BEAM);
            RitualMesh.thread(streak, poseStack.last(), view.eye(), trail, 1.1f, col, light);
            RitualMesh.thread(streak, poseStack.last(), view.eye(), trail, .4f, core, light);
        }
        // The flash it forms in
        if (age < 12) {
            float k = 1 - age / 12;
            RitualMesh.billboard(BossMesh.light(buffer, BossMesh.GLOW), poseStack, view, new Vector3f(dir).mul(-length * .4f).add(tip), 1.5f + 4 * (1 - k), 0,
                    -1, BossMesh.glow(0xFFFFFF, k), light);
        }
        // Where it strikes: a burst of light, a ring racing out, then a glow at its foot as it stands
        if (until <= 0) {
            float since = -until;
            float k = 1 - BossMesh.ramp(since, 0, 8);
            VertexConsumer ring = BossMesh.light(buffer, BossMesh.RING);
            BossMesh.pool(ring, poseStack.last(), 0, .06f, 0, 1 + 5 * BossMesh.ramp(since, 0, 10), since * .05f, BossMesh.glow(color, k), light);
            VertexConsumer glow = BossMesh.light(buffer, BossMesh.GLOW);
            BossMesh.pool(glow, poseStack.last(), 0, .05f, 0, 2.6f, 0, BossMesh.glow(color, .7f * alpha), light);
            RitualMesh.billboard(glow, poseStack, view, new Vector3f(0, .4f, 0), 3 + 4 * (1 - k), 0, -1, BossMesh.glow(0xFFFFFF, k), light);
        } else if (until < 20) {
            // The spot lights up as it comes
            BossMesh.pool(BossMesh.light(buffer, BossMesh.GLOW), poseStack.last(), 0, .05f, 0, 1.2f + 1.4f * (1 - until / 20), 0,
                    BossMesh.glow(color, .5f * (1 - until / 20)), light);
        }
        // Breaking into motes
        if (until < -(STAND - 8)) {
            float t = BossMesh.ramp(-until, STAND - 8, STAND);
            VertexConsumer motes = BossMesh.light(buffer, BossMesh.MOTE);
            for (int k = 0; k < 14; k++) {
                float along = BossMesh.hash(seed + k * 3) * length * .9f;
                Vector3f at = new Vector3f(dir).mul(-along).add(tip);
                Vector3f drift = new Vector3f(BossMesh.hash(seed + k * 5) - .5f, .6f + BossMesh.hash(seed + k * 7), BossMesh.hash(seed + k * 11) - .5f).mul(2.2f * t);
                RitualMesh.billboard(motes, poseStack, view, at.add(drift), .35f * (1 - t * .5f), 0, -1, BossMesh.glow(k % 3 == 0 ? 0xFFFFFF : color, 1 - t),
                        light);
            }
        }
    }

    /** Where its point is {@code e} (0 to 1) of the way along its dive: up from where it hangs, over, and down onto the origin. */
    private static Vector3f path(Vector3f hang, Vector3f raised, float length, float e) {
        Vector3f p0 = hang;
        Vector3f p1 = new Vector3f(raised).mul(5).add(hang);
        Vector3f p2 = new Vector3f(0, Math.max(hang.y, 6) + 4, 0);
        Vector3f p3 = new Vector3f(0, -length * .2f, 0);
        float u = 1 - e;
        return new Vector3f(p0).mul(u * u * u).add(new Vector3f(p1).mul(3 * u * u * e)).add(new Vector3f(p2).mul(3 * u * e * e)).add(new Vector3f(p3).mul(e * e * e));
    }

    /**
     * A sword of light, its point at {@code tip}, its blade running back from it against {@code dir}, flat across
     * {@code flat}; {@code length} from point to pommel.
     */
    public static void sword(PoseStack poseStack, MultiBufferSource buffer, WaterMesh.View view, Vector3f tip, Vector3f dir, Vector3f flat, Vector3f thick,
                             float length, int color, float alpha, int light) {
        float blade = length * .74f;
        float width = length * .085f;
        Vector3f[][] cross = new Vector3f[BLADE_STEPS + 1][4];
        float[] v = new float[BLADE_STEPS + 1];
        for (int i = 0; i <= BLADE_STEPS; i++) {
            float t = (float) i / BLADE_STEPS;
            float x = t * blade;
            float hw = width * (float) Math.pow(Math.min(1, t / .2f), .75f) * (1 - .18f * t);
            float ht = hw * .24f + .01f;
            Vector3f at = new Vector3f(dir).mul(-x).add(tip);
            cross[i][0] = new Vector3f(flat).mul(hw).add(at);
            cross[i][1] = new Vector3f(thick).mul(ht).add(at);
            cross[i][2] = new Vector3f(flat).mul(-hw).add(at);
            cross[i][3] = new Vector3f(thick).mul(-ht).add(at);
            v[i] = t;
        }
        float[] u = {1, .5f, 0, .5f, 1};
        VertexConsumer body = BossMesh.see(buffer, BossMesh.SWORD);
        PoseStack.Pose p = poseStack.last();
        int steel = BossMesh.argb(BossMesh.mix(color, 0xFFFFFF, .55f), .62f * alpha);
        blade(body, p, cross, v, u, tip, dir, steel, light);
        VertexConsumer shine = BossMesh.light(buffer, BossMesh.SWORD);
        p = poseStack.last();
        blade(shine, p, cross, v, u, tip, dir, BossMesh.glow(color, .75f * alpha), light);

        // Guard, grip and pommel, gold
        Vector3f guard = new Vector3f(dir).mul(-blade).add(tip);
        float gw = length * .16f;
        Vector3f gt = new Vector3f(thick).mul(width * .5f);
        Vector3f gl = new Vector3f(dir).mul(length * .03f);
        Vector3f grip = new Vector3f(dir).mul(-length * .1f).add(guard);
        Vector3f pommel = new Vector3f(dir).mul(-length * .22f).add(guard);
        for (int pass = 0; pass < 2; pass++) {
            boolean lit = pass == 1;
            VertexConsumer gold = lit ? BossMesh.light(buffer, BossMesh.SPIKE) : BossMesh.see(buffer, BossMesh.SPIKE);
            p = poseStack.last();
            int metal = lit ? BossMesh.glow(color, .45f * alpha) : BossMesh.argb(0xFFF0C0, .9f * alpha);
            int wrap = lit ? BossMesh.glow(color, .2f * alpha) : BossMesh.argb(0xC08A40, .95f * alpha);
            BossMesh.box(gold, p, guard, new Vector3f(flat).mul(gw), gt, gl, metal, light);
            BossMesh.box(gold, p, new Vector3f(dir).mul(length * .02f).add(guard), new Vector3f(flat).mul(width * .7f), new Vector3f(thick).mul(width * .62f),
                    new Vector3f(dir).mul(length * .05f), metal, light);
            BossMesh.box(gold, p, grip, new Vector3f(flat).mul(width * .26f), new Vector3f(thick).mul(width * .26f), new Vector3f(dir).mul(length * .1f), wrap, light);
            BossMesh.box(gold, p, pommel, new Vector3f(flat).mul(width * .42f), new Vector3f(thick).mul(width * .42f), new Vector3f(dir).mul(width * .42f), metal, light);
            for (int side = -1; side <= 1; side += 2) {
                // The guard's ends, swept toward the point
                Vector3f end = new Vector3f(flat).mul(gw * side).add(guard);
                Vector3f out = new Vector3f(flat).mul(side).add(new Vector3f(dir).mul(.8f)).normalize();
                BossMesh.box(gold, p, new Vector3f(out).mul(length * .03f).add(end), new Vector3f(out).mul(length * .035f), new Vector3f(thick).mul(width * .38f),
                        new Vector3f(out).cross(thick).normalize().mul(length * .022f), metal, light);
            }
        }

        VertexConsumer glow = BossMesh.light(buffer, BossMesh.GLOW);
        RitualMesh.billboard(glow, poseStack, view, guard, length * .55f, 0, -1, BossMesh.glow(color, .55f * alpha), light);
        RitualMesh.billboard(glow, poseStack, view, new Vector3f(dir).mul(-blade * .45f).add(tip), length * .9f, 0, -1, BossMesh.glow(color, .22f * alpha),
                light);
        RitualMesh.billboard(glow, poseStack, view, pommel, length * .2f, 0, -1, BossMesh.glow(0xFFFFFF, .6f * alpha), light);
    }

    private static void blade(VertexConsumer c, PoseStack.Pose p, Vector3f[][] cross, float[] v, float[] u, Vector3f tip, Vector3f dir, int argb, int light) {
        for (int i = 0; i + 1 < cross.length; i++) {
            Vector3f mid = new Vector3f(cross[i][0]).add(cross[i][2]).mul(.5f);
            for (int f = 0; f < 4; f++) {
                Vector3f a = cross[i][f], b = cross[i][(f + 1) % 4], d = cross[i + 1][(f + 1) % 4], e = cross[i + 1][f];
                Vector3f out = new Vector3f(a).add(b).mul(.5f).sub(mid);
                BossMesh.faced(c, p, a, b, d, e, new float[] {u[f], v[i], u[f + 1], v[i], u[f + 1], v[i + 1], u[f], v[i + 1]}, argb, argb, argb, argb, light,
                        out);
            }
        }
    }
}
