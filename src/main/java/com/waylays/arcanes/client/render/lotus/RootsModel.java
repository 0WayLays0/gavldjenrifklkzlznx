package com.waylays.arcanes.client.render.lotus;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.waylays.arcanes.client.render.boss.BossMesh;
import com.waylays.arcanes.client.render.boss.Sculpt;
import com.waylays.arcanes.client.render.water.WaterMesh;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

/**
 * Roots of the Mire: a pool of black mud, glistening, bubbles swelling and bursting on it, dark puddles here and there,
 * old leaves half sunk in it; out of it rear the knotted roots of the lotus, swaying, pale and swollen between their
 * knots, hairy rootlets at each knot; every second and a half they lash in toward its middle. Out of the mud, pure, a
 * few lotus stand on their stems: the lotus grows out of the mire.
 */
public final class RootsModel {
    /** When the roots first seize, then how often (ticks), as the scene does it. */
    private static final int FIRST = 12, EVERY = 30;

    private RootsModel() {}

    /** On the ground at the pose's origin, {@code radius} round, {@code life} ticks long. */
    public static void draw(PoseStack poseStack, MultiBufferSource buffer, WaterMesh.View view, float age, int life, float radius, int seed, int light) {
        float spread = BossMesh.ramp(age, 0, 10);
        float fade = 1 - BossMesh.ramp(age, life - 12, life);
        float r = radius * spread;
        if (r < .05f || fade <= 0) {
            return;
        }
        PoseStack.Pose p = poseStack.last();

        // ---- the mud: a ragged pool, its edge melting into the ground
        VertexConsumer mud = BossMesh.ground(buffer, LotusMesh.MUD);
        int segs = 40;
        for (int s = 0; s < segs; s++) {
            float a0 = s * Mth.TWO_PI / segs, a1 = (s + 1) * Mth.TWO_PI / segs;
            float e0 = edge(a0, seed), e1 = edge(a1, seed);
            Vector3f c = new Vector3f(0, .06f, 0);
            Vector3f i0 = at(a0, r * .8f * e0, .06f), i1 = at(a1, r * .8f * e1, .06f), o0 = at(a0, r * e0, .05f), o1 = at(a1, r * e1, .05f);
            int full = BossMesh.argb(0xFFFFFF, .97f * fade), none = BossMesh.argb(0xFFFFFF, 0);
            BossMesh.faced(mud, p, c, c, i1, i0, new float[] {0, 0, 0, 0, uv(i1.x), uv(i1.z), uv(i0.x), uv(i0.z)}, full, full, full, full, light,
                    BossMesh.UP);
            BossMesh.faced(mud, p, i0, i1, o1, o0, new float[] {uv(i0.x), uv(i0.z), uv(i1.x), uv(i1.z), uv(o1.x), uv(o1.z), uv(o0.x), uv(o0.z)}, full, full, none, none,
                    light, BossMesh.UP);
        }
        // Dark puddles catching the light
        VertexConsumer wet = BossMesh.light(buffer, BossMesh.SHEEN);
        for (int k = 0; k < 4; k++) {
            float a = BossMesh.hash(seed + k * 5) * Mth.TWO_PI, d = r * .55f * (float) Math.sqrt(BossMesh.hash(seed + k * 9));
            BossMesh.pool(wet, p, Mth.cos(a) * d, .075f, Mth.sin(a) * d, .9f + .6f * BossMesh.hash(seed + k), a + age * .004f, BossMesh.glow(0x6A8A7A, .18f * fade),
                    light);
        }

        // ---- bubbles swelling and bursting
        VertexConsumer bubble = BossMesh.solid(buffer, LotusMesh.MUD);
        for (int k = 0; k < 9; k++) {
            float cycle = 24 + 14 * BossMesh.hash(seed + k * 3);
            float t = ((age + k * 11) % cycle) / cycle;
            int round = (int) ((age + k * 11) / cycle);
            float a = BossMesh.hash(seed + k * 7 + round) * Mth.TWO_PI, d = r * .75f * (float) Math.sqrt(BossMesh.hash(seed + k * 13 + round * 5));
            float size = .22f * (float) Math.sin(Math.min(t * 1.15f, 1) * Mth.HALF_PI) * (t > .87f ? 0 : 1) * fade;
            if (size > .01f) {
                BossMesh.sphere(bubble, p, new Vector3f(Mth.cos(a) * d, .04f, Mth.sin(a) * d), size, 4, 8, 0, 0, .5f, BossMesh.argb(0xB8A890, 1), light);
            }
        }

        // ---- old leaves, half sunk
        VertexConsumer leaf = BossMesh.solid(buffer, LotusMesh.LEAF);
        for (int k = 0; k < 3; k++) {
            float a = k * 2.2f + 1 + BossMesh.hash(seed + k);
            float d = r * (.5f + .3f * BossMesh.hash(seed + k * 3));
            poseStack.pushPose();
            poseStack.translate(Mth.cos(a) * d, .03f, Mth.sin(a) * d);
            poseStack.mulPose(new org.joml.Quaternionf().rotationY(a).rotateX(.25f + .1f * k));
            LotusMesh.leaf(leaf, poseStack.last(), new Vector3f(), .6f * spread, a, .15f, seed + k, BossMesh.argb(0x8A8A5A, 1), light);
            poseStack.popPose();
        }

        // ---- the roots
        VertexConsumer root = BossMesh.solid(buffer, LotusMesh.ROOT);
        Vector3f[][] paths = new Vector3f[8][];
        for (int k = 0; k < 8; k++) {
            float a = k * Mth.TWO_PI / 8 + BossMesh.hash(seed + k * 17) * .6f;
            float d = r * (.55f + .3f * BossMesh.hash(seed + k * 19));
            Vector3f out = new Vector3f(Mth.cos(a), 0, Mth.sin(a));
            Vector3f foot = new Vector3f(out).mul(d).add(0, -.4f, 0);
            // Rising out of the mud, then a lash in toward the middle every so often
            float rise = BossMesh.ramp(age, 2 + k * 1.5f, 12 + k * 1.5f) * fade;
            float phase = age - FIRST + 4 - k * .6f;
            float lash = phase < 0 ? 0 : lashCurve((phase % EVERY) / 12f);
            float sway = Mth.sin(age * .09f + k * 1.9f);
            float h = (1.9f + .8f * BossMesh.hash(seed + k * 23)) * rise * (1 + .3f * lash);
            Vector3f side = new Vector3f(-out.z, 0, out.x);
            Vector3f c1 = new Vector3f(foot).add(new Vector3f(out).mul(.5f)).add(0, h * .9f, 0).add(new Vector3f(side).mul(.6f * sway));
            Vector3f tip = new Vector3f(foot).add(new Vector3f(out).mul(-(.4f + 1.6f * lash) * rise)).add(0, h * (1 - .45f * lash), 0)
                    .add(new Vector3f(side).mul(.5f * sway));
            Vector3f c2 = new Vector3f(tip).add(new Vector3f(out).mul(.9f - .3f * lash)).add(new Vector3f(side).mul(-.7f * sway)).add(0, .8f, 0);
            paths[k] = LotusMesh.path(foot, c1, c2, tip, 26);
            LotusMesh.root(root, p, paths[k], .34f * (.6f + .4f * rise), 4, light);
        }
        // Rootlets bristling at the knots
        VertexConsumer hair = BossMesh.solid(buffer, LotusMesh.FIBRE);
        for (int k = 0; k < paths.length; k++) {
            Vector3f[] path = paths[k];
            for (int n = 1; n < 4; n++) {
                int i = Math.min(path.length - 2, n * (path.length - 1) / 4);
                Vector3f at = path[i];
                if (at.y < .05f) {
                    continue;
                }
                for (int h = 0; h < 4; h++) {
                    float a = h * Mth.HALF_PI + k + n;
                    Vector3f end = new Vector3f(at).add(Mth.cos(a) * .25f, -.08f - .1f * BossMesh.hash(k * 7 + n * 3 + h), Mth.sin(a) * .25f);
                    Vector3f[] s = Sculpt.curve(at, new Vector3f(at).lerp(end, .5f).add(0, .05f, 0), end, 3);
                    Sculpt.sweep(hair, p, s, new float[] {.012f, .008f, .003f}, new float[] {.012f, .008f, .003f}, Sculpt.circle(3), false, null, 1,
                            Sculpt.fill(BossMesh.argb(0x6A5034, 1), 3), false, light);
                }
            }
        }

        // ---- lotus standing pure out of the mire
        VertexConsumer fibre = BossMesh.solid(buffer, LotusMesh.FIBRE);
        Vector3f[] heads = new Vector3f[3];
        for (int k = 0; k < 3; k++) {
            float a = k * 2.1f + .4f;
            float d = r * (.22f + .12f * k);
            Vector3f foot = new Vector3f(Mth.cos(a) * d, .05f, Mth.sin(a) * d);
            float h = (.8f + .35f * k) * BossMesh.ramp(age, 4 + 3 * k, 16 + 3 * k) * fade;
            heads[k] = new Vector3f(foot).add(.05f * Mth.sin(age * .06f + k), h, 0);
            LotusMesh.stem(fibre, p, LotusMesh.path(foot, new Vector3f(foot).add(0, h * .5f, 0), new Vector3f(heads[k]).add(0, -h * .3f, 0), heads[k], 6), .03f,
                    light);
        }
        for (int k = 0; k < 3; k++) {
            float s = BossMesh.ramp(age, 4 + 3 * k, 16 + 3 * k) * fade;
            LotusMesh.bloom(poseStack, buffer, view, LotusMesh.LOTUS, heads[k], .42f * s, k == 1 ? .85f : .1f, k, age, BossMesh.argb(0xFFF8FA, 1),
                    BossMesh.argb(LotusMesh.TIP, 1), .3f * s, LotusMesh.PINK, seed + k, light);
        }
    }

    /** How far a root has lashed in, {@code t} into its lash (0 to 1 strikes and comes back; past 1, at rest). */
    private static float lashCurve(float t) {
        if (t >= 1) {
            return 0;
        }
        return t < .3f ? BossMesh.ramp(t, 0, .3f) : 1 - BossMesh.ramp(t, .3f, 1);
    }

    /** How far the mud reaches round its rim at {@code a} (about 1). */
    private static float edge(float a, int seed) {
        return 1 + .07f * Mth.sin(a * 5 + seed) + .05f * Mth.sin(a * 11 + seed * 2.3f);
    }

    private static Vector3f at(float a, float r, float y) {
        return new Vector3f(Mth.cos(a) * r, y, Mth.sin(a) * r);
    }

    /** Laid flat, the mud's texture repeating every two blocks. */
    private static float uv(float x) {
        return x * .5f;
    }
}
