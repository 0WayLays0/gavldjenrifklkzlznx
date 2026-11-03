package com.waylays.arcanes.client.render.boss;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.waylays.arcanes.client.render.ritual.RitualMesh;
import com.waylays.arcanes.client.render.water.WaterMesh;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import net.minecraft.resources.ResourceLocation;
import com.waylays.arcanes.ArcanesOublies;
import org.joml.Matrix3f;
import org.joml.Quaternionf;
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
    private static final ResourceLocation STEEL = ArcanesOublies.id("textures/entity/boss/m_steel.png");

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
     * A sword, its point at {@code tip}, its blade running back from it against {@code dir}, flat across {@code flat};
     * {@code length} from point to pommel. A blade of folded steel with a burning fuller, a guard of gold with its
     * quillons swept toward the point, a grip bound in gold wire, a pommel set with a gem; light along its edges.
     * Sinking into the ground as {@code alpha} falls (solid steel does not fade).
     */
    public static void sword(PoseStack poseStack, MultiBufferSource buffer, WaterMesh.View view, Vector3f tip, Vector3f dir, Vector3f flat, Vector3f thick,
                             float length, int color, float alpha, int light) {
        Vector3f back = new Vector3f(dir).negate();
        Quaternionf frame = new Matrix3f(new Vector3f(flat).normalize(), back, new Vector3f(thick).normalize()).getNormalizedRotation(new Quaternionf());
        Vector3f at = new Vector3f(tip).add(new Vector3f(dir).mul((1 - alpha) * length * .7f));
        float l = length;
        float blade = l * .74f;
        float w = l * .085f;
        // ---- the blade (steel)
        VertexConsumer steel = BossMesh.solid(buffer, STEEL);
        place(poseStack, at, frame);
        int n = BLADE_STEPS + 1;
        Vector3f[] path = new Vector3f[n];
        float[] sx = new float[n], sy = new float[n];
        for (int i = 0; i < n; i++) {
            float t = (float) i / (n - 1);
            path[i] = new Vector3f(0, t * blade, 0);
            sx[i] = w * (float) Math.pow(Math.min(1, t / .22f), .7f) * (1 - .15f * t) + .005f;
            sy[i] = sx[i] * .2f + .006f;
        }
        Sculpt.sweep(steel, poseStack.last(), path, sx, sy, Sculpt.DIAMOND, true, new Vector3f(1, 0, 0), .5f / l, Sculpt.fill(0xFFE8ECF4, n), true, light);
        poseStack.popPose();
        // ---- the hilt (gold): the guard, the wire on the grip, the pommel
        VertexConsumer gold = BossMesh.solid(buffer, BellModel.GOLD);
        place(poseStack, at, frame);
        poseStack.translate(0, blade, 0);
        float g = l * .2f, gh = l * .02f;
        Sculpt.slab(gold, poseStack.last(), new float[] {-g * .55f, -gh, -g * .85f, -gh * 2.2f, -g, -gh * 3.6f, -g * .94f, -gh * .2f, -g * .55f, gh, -w * .9f, gh * 1.6f,
                0, gh * 2.4f, w * .9f, gh * 1.6f, g * .55f, gh, g * .94f, -gh * .2f, g, -gh * 3.6f, g * .85f, -gh * 2.2f, g * .55f, -gh, w * .9f, -gh * 1.4f, 0, -gh * 2.6f,
                -w * .9f, -gh * 1.4f}, w * .38f, w * .06f, 4 / l, 0xFFFFFFFF, 0xFFE0B060, light);
        float grip = l * .17f;
        for (int k = 0; k < 4; k++) {
            poseStack.pushPose();
            poseStack.translate(0, gh * 2.4f + (k + .5f) * grip / 4, 0);
            Sculpt.torus(gold, poseStack.last(), w * .3f, w * .07f, 10, 4, 0, Mth.TWO_PI, 2, 0xFFFFFFFF, light);
            poseStack.popPose();
        }
        poseStack.translate(0, gh * 2.4f + grip + w * .32f, 0);
        Sculpt.lathe(gold, poseStack.last(), BellModel.sphere(w * .42f, 8), 10, 1, 1, 0xFFFFFFFF, 0xFFFFFFFF, light);
        poseStack.popPose();
        // ---- the grip itself (dark leather) and the gems
        VertexConsumer dark = BossMesh.solid(buffer, BellModel.IRON);
        place(poseStack, at, frame);
        Vector3f[] gripPath = {new Vector3f(0, blade, 0), new Vector3f(0, blade + gh * 2.4f + grip, 0)};
        Sculpt.sweep(dark, poseStack.last(), gripPath, Sculpt.fill(w * .3f, 2), Sculpt.fill(w * .3f, 2), Sculpt.circle(8), false, null, 1, Sculpt.fill(0xFF6A4A3A, 2), true,
                light);
        poseStack.translate(0, blade + gh * .3f, w * .35f);
        poseStack.mulPose(new Quaternionf().rotationX(Mth.HALF_PI));
        Sculpt.gem(dark, poseStack.last(), w * .3f, w * .15f, w * .1f, 6, BossMesh.mix(color, 0xFFFFFF, .2f) | 0xFF000000, light);
        poseStack.popPose();
        // ---- light: the fuller, the edges, a halo at the guard
        VertexConsumer fuller = BossMesh.light(buffer, BossMesh.BEAM);
        place(poseStack, at, frame);
        int lit = BossMesh.glow(color, .9f * alpha);
        for (int side = -1; side <= 1; side += 2) {
            float z = side * (w * .2f + .006f) * 1.1f;
            Vector3f a0 = new Vector3f(-w * .18f, blade * .12f, z), a1 = new Vector3f(w * .18f, blade * .12f, z);
            Vector3f b1 = new Vector3f(w * .18f, blade * .92f, z), b0 = new Vector3f(-w * .18f, blade * .92f, z);
            if (side > 0) {
                Sculpt.put(fuller, poseStack.last(), a0, 0, 0, lit, light, new Vector3f(0, 0, 1));
                Sculpt.put(fuller, poseStack.last(), a1, 1, 0, lit, light, new Vector3f(0, 0, 1));
                Sculpt.put(fuller, poseStack.last(), b1, 1, 1, lit, light, new Vector3f(0, 0, 1));
                Sculpt.put(fuller, poseStack.last(), b0, 0, 1, lit, light, new Vector3f(0, 0, 1));
            } else {
                Sculpt.put(fuller, poseStack.last(), b0, 0, 1, lit, light, new Vector3f(0, 0, -1));
                Sculpt.put(fuller, poseStack.last(), b1, 1, 1, lit, light, new Vector3f(0, 0, -1));
                Sculpt.put(fuller, poseStack.last(), a1, 1, 0, lit, light, new Vector3f(0, 0, -1));
                Sculpt.put(fuller, poseStack.last(), a0, 0, 0, lit, light, new Vector3f(0, 0, -1));
            }
        }
        poseStack.popPose();
        VertexConsumer glow = BossMesh.light(buffer, BossMesh.GLOW);
        Vector3f guard = new Vector3f(dir).mul(-blade).add(at);
        RitualMesh.billboard(glow, poseStack, view, guard, length * .45f, 0, -1, BossMesh.glow(color, .5f * alpha), light);
        RitualMesh.billboard(glow, poseStack, view, new Vector3f(dir).mul(-blade * .45f).add(at), length * .7f, 0, -1, BossMesh.glow(color, .16f * alpha), light);
    }

    private static void place(PoseStack poseStack, Vector3f at, Quaternionf frame) {
        poseStack.pushPose();
        poseStack.translate(at.x, at.y, at.z);
        poseStack.mulPose(frame);
    }
}
