package com.waylays.arcanes.client.render.boss;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.waylays.arcanes.client.render.ritual.RitualMesh;
import com.waylays.arcanes.client.render.water.WaterMesh;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

/**
 * The Seraph's Wings: two great wings of golden light unfold from where it was called, feather by feather, a halo
 * kindling over them; they rise and fall slowly, gathering light. Then they beat once, sweeping forward, and a storm of
 * feathers of light blasts out ahead across the whole fan of ground before them, a sheet of gold racing over it.
 */
public final class WingsModel {
    /** How long the beat lasts, and the storm of feathers flying (ticks). */
    public static final int BEAT = 6, STORM = 14;
    /** How long they take to fade after the beat (ticks). */
    public static final int FADE = 24;
    private static final int GOLD = 0xFFD25A;
    private static final int PALE = 0xFFF4DA;
    private static final int FLYING = 44;

    private WingsModel() {}

    /**
     * Drawn from where the wings were called (the pose's origin, on the ground), facing {@code +z}; {@code span} how far
     * each wing reaches; {@code age} ticks since they were called, {@code until} ticks until they beat (negative after);
     * the feathers fly over a fan {@code reach} long, {@code half} radians either side of ahead.
     */
    public static void draw(PoseStack poseStack, MultiBufferSource buffer, WaterMesh.View view, float span, float age, float until, float reach, float half,
                            int seed, int light) {
        float since = -until;
        float unfold = BossMesh.ramp(age, 0, 28);
        float sweep = until < 0 ? Mth.sin(Mth.PI * BossMesh.ramp(since, 0, BEAT * 2)) : 0;
        float fade = 1 - BossMesh.ramp(since, BEAT, FADE);
        float gather = until > 0 ? 1 - Mth.clamp(until / 40, 0, 1) : 1;
        float lift = .1f * Mth.sin(age * .12f) * (1 - sweep);
        if (fade > .01f) {
            for (int sd = -1; sd <= 1; sd += 2) {
                wing(poseStack, buffer, view, sd, span, unfold, lift, sweep, fade, gather, seed + (sd + 1) * 97, light);
            }
            // The halo, and the light where the seraph would stand
            VertexConsumer ring = BossMesh.light(buffer, BossMesh.RING);
            BossMesh.plate(ring, poseStack.last(), new Vector3f(0, 2.45f + lift, -.2f), new Vector3f(1, 0, 0), new Vector3f(0, 0, 1), 1.1f * unfold,
                    BossMesh.glow(GOLD, .95f * fade), light);
            VertexConsumer glow = BossMesh.light(buffer, BossMesh.GLOW);
            RitualMesh.billboard(glow, poseStack, view, new Vector3f(0, 1.6f, -.3f), 3.5f * unfold, 0, -1, BossMesh.glow(GOLD, (.3f + .3f * gather) * fade), light);
            RitualMesh.billboard(glow, poseStack, view, new Vector3f(0, 2.45f, -.2f), 1.4f * unfold, 0, -1, BossMesh.glow(PALE, .6f * fade), light);
            VertexConsumer motes = BossMesh.light(buffer, BossMesh.MOTE);
            for (int k = 0; k < 20; k++) {
                float t = (age * (.02f + .01f * BossMesh.hash(seed + k)) + BossMesh.hash(seed + k * 3)) % 1;
                float sd = k % 2 == 0 ? 1 : -1;
                float x = sd * span * (.15f + .8f * BossMesh.hash(seed + k * 5));
                Vector3f at = new Vector3f(x, .8f + 3.5f * BossMesh.hash(seed + k * 7) - 1.5f * t, -.6f - .4f * t);
                RitualMesh.billboard(motes, poseStack, view, at, .3f, 0, -1, BossMesh.glow(PALE, Mth.sin(t * Mth.PI) * unfold * fade), light);
            }
        }
        if (until < 0 && since < STORM + 8) {
            storm(poseStack, buffer, view, span, since, reach, half, seed, light);
        }
    }

    /** One wing: three bones (arm, forearm, hand) and the rows of feathers along them. */
    private static void wing(PoseStack poseStack, MultiBufferSource buffer, WaterMesh.View view, int sd, float span, float unfold, float lift, float sweep, float fade, float gather,
                             int seed, int light) {
        float u = span / 7;
        Vector3f shoulder = new Vector3f(sd * .3f, 1.65f, -.35f);
        float t1 = Mth.lerp(unfold, -1.15f, .78f) + lift;
        float t2 = Mth.lerp(unfold, -1.6f, .4f) + lift * 1.5f;
        float t3 = Mth.lerp(unfold, -1.9f, .05f) + lift * 2;
        Vector3f elbow = bone(shoulder, sd, t1, 2.2f * u, sweep);
        Vector3f wrist = bone(elbow, sd, t2, 2.3f * u, sweep);
        Vector3f tip = bone(wrist, sd, t3, 1.9f * u, sweep);
        // The plane the wing spreads in, facing back and a little up
        Vector3f across = new Vector3f(tip).sub(shoulder).normalize();
        Vector3f facing = new Vector3f(0, .5f, -1).normalize().rotateY(-sd * sweep * 1.1f);
        Vector3f down = new Vector3f(facing).cross(across).normalize();
        if (down.y > 0) {
            down.negate();
        }
        Vector3f[] bones = {shoulder, elbow, wrist, tip};
        VertexConsumer body = BossMesh.see(buffer, BossMesh.FEATHER);
        PoseStack.Pose p = poseStack.last();
        java.util.List<float[]> feathers = new java.util.ArrayList<>();
        // Primaries off the hand, fanning from along it to down; secondaries off the forearm; tertials off the arm
        row(feathers, wrist, tip, across, down, 12, 2.4f * u, 3.4f * u, 1f, .35f, seed);
        row(feathers, elbow, wrist, across, down, 12, 2.0f * u, 2.3f * u, .3f, .12f, seed + 31);
        row(feathers, shoulder, elbow, across, down, 8, 1.3f * u, 1.9f * u, .12f, 0, seed + 57);
        // Coverts over their roots
        row(feathers, elbow, tip, across, down, 12, .9f * u, 1.2f * u, .55f, .2f, seed + 77);
        row(feathers, shoulder, wrist, across, down, 10, .7f * u, .9f * u, .25f, .1f, seed + 91);
        for (float[] f : feathers) {
            feather(body, p, f, facing, BossMesh.argb(BossMesh.mix(PALE, GOLD, f[7]), .88f * fade), light);
        }
        VertexConsumer shine = BossMesh.light(buffer, BossMesh.FEATHER);
        p = poseStack.last();
        for (float[] f : feathers) {
            feather(shine, p, f, facing, BossMesh.glow(GOLD, (.25f + .25f * gather + .3f * sweep) * fade), light);
        }
        // The bones, light
        VertexConsumer beam = BossMesh.light(buffer, BossMesh.BEAM);
        int[] col = {BossMesh.glow(PALE, .8f * fade), BossMesh.glow(PALE, .7f * fade), BossMesh.glow(GOLD, .6f * fade), BossMesh.glow(GOLD, .3f * fade)};
        RitualMesh.thread(beam, poseStack.last(), view.eye(), bones, .3f * u, col, light);
    }

    /** The next joint, {@code len} from {@code from} at {@code angle} above the outward line, swept forward by {@code sweep}. */
    private static Vector3f bone(Vector3f from, int sd, float angle, float len, float sweep) {
        Vector3f d = new Vector3f(sd * Mth.cos(angle), Mth.sin(angle), -.28f).normalize().rotateY(-sd * sweep * 1.2f);
        return new Vector3f(d).mul(len).add(from);
    }

    /**
     * A row of {@code n} feathers rooted from {@code a} to {@code b}, {@code l0} to {@code l1} long, each pointing
     * between down ({@code 0}) and along the wing ({@code 1}): {@code along1} at {@code b}, {@code along0} at {@code a}.
     */
    private static void row(java.util.List<float[]> out, Vector3f a, Vector3f b, Vector3f across, Vector3f down, int n, float l0, float l1, float along1,
                            float along0, int seed) {
        for (int i = 0; i < n; i++) {
            float t = (i + .5f) / n;
            Vector3f root = new Vector3f(a).lerp(b, t);
            float al = Mth.lerp(t, along0, along1);
            Vector3f dir = new Vector3f(down).mul(1 - al).add(new Vector3f(across).mul(al + .05f)).normalize();
            float len = Mth.lerp(t, l0, l1) * (.92f + .16f * BossMesh.hash(seed + i));
            out.add(new float[] {root.x, root.y, root.z, dir.x, dir.y, dir.z, len, al});
        }
    }

    private static void feather(VertexConsumer c, PoseStack.Pose p, float[] f, Vector3f facing, int argb, int light) {
        Vector3f root = new Vector3f(f[0], f[1], f[2]);
        Vector3f dir = new Vector3f(f[3], f[4], f[5]);
        float len = f[6];
        Vector3f w = new Vector3f(dir).cross(facing).normalize().mul(len * .2f);
        Vector3f tip = new Vector3f(dir).mul(len).add(root);
        BossMesh.quad2(c, p, new Vector3f(root).sub(w), new Vector3f(root).add(w), new Vector3f(tip).add(w), new Vector3f(tip).sub(w), 0, 1, 1, 1, 1, 0, 0, 0, argb,
                argb, light);
    }

    /** The storm of feathers blasting out over the fan, and the sheet of gold racing across it. */
    private static void storm(PoseStack poseStack, MultiBufferSource buffer, WaterMesh.View view, float span, float since, float reach, float half, int seed,
                              int light) {
        float t = BossMesh.ramp(since, 0, STORM);
        float end = 1 - BossMesh.ramp(since, STORM, STORM + 8);
        // The sheet of light over the ground
        VertexConsumer sheet = BossMesh.light(buffer, BossMesh.NOISE);
        PoseStack.Pose p = poseStack.last();
        int segs = 18;
        float r1 = 1 + (reach - 1) * t, r0 = Math.max(1, r1 - 5);
        for (int s = 0; s < segs; s++) {
            float a0 = -half + 2 * half * s / segs, a1 = -half + 2 * half * (s + 1) / segs;
            Vector3f i0 = new Vector3f(Mth.sin(a0) * r0, .07f, Mth.cos(a0) * r0), o0 = new Vector3f(Mth.sin(a0) * r1, .07f, Mth.cos(a0) * r1);
            Vector3f o1 = new Vector3f(Mth.sin(a1) * r1, .07f, Mth.cos(a1) * r1), i1 = new Vector3f(Mth.sin(a1) * r0, .07f, Mth.cos(a1) * r0);
            int ci = BossMesh.glow(GOLD, .15f * end), co = BossMesh.glow(PALE, .85f * end);
            BossMesh.faced(sheet, p, i0, o0, o1, i1, new float[] {(float) s / segs, 0, (float) s / segs, 1, (float) (s + 1) / segs, 1, (float) (s + 1) / segs, 0}, ci, co,
                    co, ci, light, BossMesh.UP);
        }
        // The feathers
        VertexConsumer fly = BossMesh.light(buffer, BossMesh.FEATHER);
        Vector3f eye = view.eye();
        Vector3f[] heads = new Vector3f[FLYING], tails = new Vector3f[FLYING];
        float[] alpha = new float[FLYING];
        for (int k = 0; k < FLYING; k++) {
            float delay = BossMesh.hash(seed + k * 3) * 4;
            float f = BossMesh.ramp(since - delay, 0, STORM - 4);
            if (f <= 0 || f >= 1) {
                continue;
            }
            float sd = k % 2 == 0 ? 1 : -1;
            Vector3f from = new Vector3f(sd * span * (.2f + .7f * BossMesh.hash(seed + k * 5)), 1.5f + 2.5f * BossMesh.hash(seed + k * 7), -.4f);
            float a = (BossMesh.hash(seed + k * 11) * 2 - 1) * half;
            float r = reach * (.35f + .65f * BossMesh.hash(seed + k * 13));
            Vector3f to = new Vector3f(Mth.sin(a) * r, .6f + 1.4f * BossMesh.hash(seed + k * 17), Mth.cos(a) * r);
            float e = 1 - (1 - f) * (1 - f);
            Vector3f at = new Vector3f(from).lerp(to, e);
            Vector3f d = new Vector3f(to).sub(from).normalize();
            Vector3f side = new Vector3f(d).cross(new Vector3f(eye).sub(at));
            if (side.lengthSquared() < 1.0E-6f) {
                continue;
            }
            side.normalize().mul(.22f);
            Vector3f back = new Vector3f(d).mul(-1.4f).add(at);
            BossMesh.quad2(fly, poseStack.last(), new Vector3f(back).sub(side), new Vector3f(back).add(side), new Vector3f(at).add(side), new Vector3f(at).sub(side), 0, 1,
                    1, 1, 1, 0, 0, 0, BossMesh.glow(PALE, 1 - f * .5f), BossMesh.glow(PALE, 1 - f * .5f), light);
            heads[k] = at;
            tails[k] = new Vector3f(d).mul(-(2 + 3 * (1 - f))).add(at);
            alpha[k] = 1 - f;
        }
        VertexConsumer streak = BossMesh.light(buffer, BossMesh.BEAM);
        for (int k = 0; k < FLYING; k++) {
            if (heads[k] != null) {
                RitualMesh.shaft(streak, poseStack.last(), eye, tails[k], heads[k], .35f, BossMesh.glow(GOLD, .7f * alpha[k]), light);
            }
        }
    }
}
