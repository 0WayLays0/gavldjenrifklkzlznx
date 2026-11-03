package com.waylays.arcanes.client.render.boss;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.waylays.arcanes.client.render.ritual.RitualMesh;
import com.waylays.arcanes.client.render.water.WaterMesh;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
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
     * A golden sword, its point at {@code tip}, its blade running back from it against {@code dir}, flat across
     * {@code flat}; {@code length} from point to pommel. Its blade is light (a pale body seen through, the same shape
     * glowing over it, runes burning down its fuller, its edges glittering); its hilt is chased gold: a guard of two
     * feathered wings swept toward the point round a glowing gem, a grip bound in gold wire, a pommel like a little sun
     * with a gem at its heart. A halo shines behind the guard, sparks drift off the blade. All of it fades with {@code alpha}.
     */
    public static void sword(PoseStack poseStack, MultiBufferSource buffer, WaterMesh.View view, Vector3f tip, Vector3f dir, Vector3f flat, Vector3f thick,
                             float length, int color, float alpha, int light) {
        Vector3f back = new Vector3f(dir).negate();
        Quaternionf frame = new Matrix3f(new Vector3f(flat).normalize(), back, new Vector3f(thick).normalize()).getNormalizedRotation(new Quaternionf());
        float l = length, blade = l * .74f, w = l * .085f;
        int pale = BossMesh.mix(color, 0xFFFFFF, .6f);
        // The blade: pale light seen through, the same shape glowing over it
        VertexConsumer body = BossMesh.see(buffer, BossMesh.SWORD);
        place(poseStack, tip, frame);
        blade(body, poseStack.last(), l, BossMesh.argb(pale, .62f * alpha), light);
        poseStack.popPose();
        VertexConsumer shine = BossMesh.light(buffer, BossMesh.SWORD);
        place(poseStack, tip, frame);
        blade(shine, poseStack.last(), l, BossMesh.glow(color, .75f * alpha), light);
        poseStack.popPose();
        // The hilt: chased gold, seen through as it fades
        VertexConsumer gold = BossMesh.see(buffer, BellModel.GOLD);
        place(poseStack, tip, frame);
        hilt(gold, poseStack, l, BossMesh.argb(0xFFFFFF, alpha), light);
        poseStack.popPose();
        // Runes burning down the fuller, both faces
        VertexConsumer runes = BossMesh.light(buffer, BellModel.RUNE_BAND);
        place(poseStack, tip, frame);
        int lit = BossMesh.glow(BossMesh.mix(color, 0xFFFFFF, .3f), alpha);
        for (int side = -1; side <= 1; side += 2) {
            float z = side * (w * .2f + .006f) * 1.1f;
            Vector3f a0 = new Vector3f(-w * .3f, blade * .1f, z), a1 = new Vector3f(w * .3f, blade * .1f, z);
            Vector3f b1 = new Vector3f(w * .3f, blade * .95f, z), b0 = new Vector3f(-w * .3f, blade * .95f, z);
            Vector3f n = new Vector3f(0, 0, side);
            float u = blade / (w * 4);
            if (side > 0) {
                Sculpt.put(runes, poseStack.last(), a0, 0, 1, lit, light, n);
                Sculpt.put(runes, poseStack.last(), a1, 0, 0, lit, light, n);
                Sculpt.put(runes, poseStack.last(), b1, u, 0, lit, light, n);
                Sculpt.put(runes, poseStack.last(), b0, u, 1, lit, light, n);
            } else {
                Sculpt.put(runes, poseStack.last(), b0, u, 1, lit, light, n);
                Sculpt.put(runes, poseStack.last(), b1, u, 0, lit, light, n);
                Sculpt.put(runes, poseStack.last(), a1, 0, 0, lit, light, n);
                Sculpt.put(runes, poseStack.last(), a0, 0, 1, lit, light, n);
            }
        }
        poseStack.popPose();
        // Its edges glittering, a halo behind the guard, a gem's light, sparks drifting off it
        VertexConsumer edge = BossMesh.light(buffer, BossMesh.BEAM);
        for (int side = -1; side <= 1; side += 2) {
            Vector3f e0 = new Vector3f(tip), e1 = new Vector3f(dir).mul(-blade * .2f).add(tip).add(new Vector3f(flat).mul(side * w * .98f)),
                    e2 = new Vector3f(dir).mul(-blade).add(tip).add(new Vector3f(flat).mul(side * w * .85f));
            RitualMesh.thread(edge, poseStack.last(), view.eye(), new Vector3f[] {e0, e1, e2}, w * .35f, new int[] {BossMesh.glow(0xFFFFFF, alpha),
                    BossMesh.glow(0xFFFFFF, .8f * alpha), BossMesh.glow(color, .5f * alpha)}, light);
        }
        Vector3f guard = new Vector3f(dir).mul(-blade).add(tip);
        VertexConsumer halo = BossMesh.light(buffer, BossMesh.RING);
        RitualMesh.billboard(halo, poseStack, view, guard, length * .6f, 0, -1, BossMesh.glow(color, .6f * alpha), light);
        VertexConsumer glow = BossMesh.light(buffer, BossMesh.GLOW);
        RitualMesh.billboard(glow, poseStack, view, guard, length * .5f, 0, -1, BossMesh.glow(color, .55f * alpha), light);
        RitualMesh.billboard(glow, poseStack, view, guard, length * .14f, 0, -1, BossMesh.glow(0xFFFFFF, .9f * alpha), light);
        RitualMesh.billboard(glow, poseStack, view, new Vector3f(dir).mul(-blade * .45f).add(tip), length * .9f, 0, -1, BossMesh.glow(color, .2f * alpha), light);
        RitualMesh.billboard(glow, poseStack, view, new Vector3f(dir).mul(-length * .99f).add(tip), length * .16f, 0, -1, BossMesh.glow(0xFFFFFF, .7f * alpha), light);
        VertexConsumer motes = BossMesh.light(buffer, BossMesh.MOTE);
        int seed = (int) (tip.x * 31 + tip.z * 17);
        for (int k = 0; k < 8; k++) {
            float t = (Math.abs(tip.y * .7f + k * .37f + BossMesh.hash(seed + k) * 3)) % 1;
            Vector3f at = new Vector3f(dir).mul(-blade * BossMesh.hash(seed + k * 3)).add(tip).add(new Vector3f(flat).mul((BossMesh.hash(seed + k * 5) - .5f) * w * 3))
                    .add(0, t * 1.2f, 0);
            RitualMesh.billboard(motes, poseStack, view, at, .22f, 0, -1, BossMesh.glow(k % 2 == 0 ? 0xFFFFFF : color, Mth.sin(t * Mth.PI) * alpha), light);
        }
    }

    /** The blade's shape, its point at the origin running up +Y: a flat diamond, drawn to a point. */
    private static void blade(VertexConsumer c, PoseStack.Pose p, float l, int argb, int light) {
        float blade = l * .74f, w = l * .085f;
        int n = BLADE_STEPS + 1;
        Vector3f[] path = new Vector3f[n];
        float[] sx = new float[n], sy = new float[n];
        for (int i = 0; i < n; i++) {
            float t = (float) i / (n - 1);
            path[i] = new Vector3f(0, t * blade, 0);
            sx[i] = w * (float) Math.pow(Math.min(1, t / .22f), .7f) * (1 - .15f * t) + .005f;
            sy[i] = sx[i] * .2f + .006f;
        }
        Sculpt.sweep(c, p, path, sx, sy, Sculpt.DIAMOND, true, new Vector3f(1, 0, 0), 1 / blade, Sculpt.fill(argb, n), true, light);
    }

    /** The hilt, the blade's point at the origin: the winged guard and its gem, the bound grip, the sunburst pommel. */
    private static void hilt(VertexConsumer c, PoseStack poseStack, float l, int argb, int light) {
        float blade = l * .74f, w = l * .085f;
        float g = l * .24f, gh = l * .018f;
        poseStack.pushPose();
        poseStack.translate(0, blade, 0);
        // The two wings, feathered, swept toward the point
        float[] wing = {w * .5f, gh * 1.4f, g * .35f, gh * 2.4f, g * .7f, gh * 1.6f, g * .95f, -gh * 1.2f, g * 1.06f, -gh * 5.2f, g * .86f, -gh * 3.4f, g * .78f,
                -gh * 4.8f, g * .62f, -gh * 2.6f, g * .52f, -gh * 3.8f, g * .4f, -gh * 1.8f, g * .28f, -gh * 2.4f, w * .5f, -gh * 1.2f};
        float[] mirror = new float[wing.length];
        int m = wing.length / 2;
        for (int i = 0; i < m; i++) {
            mirror[2 * i] = -wing[2 * (m - 1 - i)];
            mirror[2 * i + 1] = wing[2 * (m - 1 - i) + 1];
        }
        // The outline runs clockwise round the right wing as written: turn it about
        Sculpt.slab(c, poseStack.last(), mirror, w * .3f, w * .05f, 5 / l, argb, argb, light);
        float[] right = new float[wing.length];
        for (int i = 0; i < m; i++) {
            right[2 * i] = -mirror[2 * i];
            right[2 * i + 1] = mirror[2 * i + 1];
        }
        float[] rightCcw = new float[wing.length];
        for (int i = 0; i < m; i++) {
            rightCcw[2 * i] = right[2 * (m - 1 - i)];
            rightCcw[2 * i + 1] = right[2 * (m - 1 - i) + 1];
        }
        Sculpt.slab(c, poseStack.last(), rightCcw, w * .3f, w * .05f, 5 / l, argb, argb, light);
        // The boss at the wings' heart, a gem set in each face
        Sculpt.lathe(c, poseStack.last(), BellModel.sphere(w * .55f, 8), 12, 1, 1, argb, argb, light);
        for (int side = -1; side <= 1; side += 2) {
            poseStack.pushPose();
            poseStack.translate(0, 0, side * w * .45f);
            poseStack.mulPose(new Quaternionf().rotationX(side * Mth.HALF_PI));
            Sculpt.gem(c, poseStack.last(), w * .38f, w * .18f, w * .12f, 8, BossMesh.argb(0xFF8090, (argb >>> 24) / 255f), light);
            poseStack.popPose();
        }
        // The grip: a dark core bound in gold wire
        float grip = l * .17f;
        Vector3f[] gripPath = {new Vector3f(0, gh * 2.4f, 0), new Vector3f(0, gh * 2.4f + grip, 0)};
        Sculpt.sweep(c, poseStack.last(), gripPath, Sculpt.fill(w * .24f, 2), Sculpt.fill(w * .24f, 2), Sculpt.circle(8), false, null, 1,
                Sculpt.fill(BossMesh.argb(0x8A6030, (argb >>> 24) / 255f), 2), true, light);
        for (int k = 0; k < 6; k++) {
            poseStack.pushPose();
            poseStack.translate(0, gh * 2.4f + (k + .5f) * grip / 6, 0);
            poseStack.mulPose(new Quaternionf().rotationZ(.25f));
            Sculpt.torus(c, poseStack.last(), w * .27f, w * .05f, 10, 4, 0, Mth.TWO_PI, 2, argb, light);
            poseStack.popPose();
        }
        // The pommel: a ring like a little sun, rays round it, a gem at its heart
        poseStack.translate(0, gh * 2.4f + grip + w * .55f, 0);
        poseStack.pushPose();
        poseStack.mulPose(new Quaternionf().rotationX(Mth.HALF_PI));
        Sculpt.torus(c, poseStack.last(), w * .42f, w * .1f, 16, 6, 0, Mth.TWO_PI, 3, argb, light);
        poseStack.popPose();
        for (int k = 0; k < 8; k++) {
            float a = k * Mth.TWO_PI / 8;
            BossMesh.horn(c, poseStack.last(), new Vector3f(Mth.cos(a) * w * .5f, Mth.sin(a) * w * .5f, 0), new Vector3f(Mth.cos(a), Mth.sin(a), 0), new Vector3f(),
                    w * (k % 2 == 0 ? .5f : .3f), w * .08f, 4, 2, argb, argb, light);
        }
        poseStack.mulPose(new Quaternionf().rotationX(Mth.HALF_PI));
        Sculpt.gem(c, poseStack.last(), w * .3f, w * .14f, w * .14f, 8, BossMesh.argb(0xFF8090, (argb >>> 24) / 255f), light);
        poseStack.popPose();
    }

    private static void place(PoseStack poseStack, Vector3f at, Quaternionf frame) {
        poseStack.pushPose();
        poseStack.translate(at.x, at.y, at.z);
        poseStack.mulPose(frame);
    }
}
