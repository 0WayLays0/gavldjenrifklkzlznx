package com.waylays.arcanes.client.render.fire;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

import static com.waylays.arcanes.client.render.water.WaterMesh.*;

/**
 * Flame Crescent: a slash of fire flying flat through the air, a crescent bowed forward, a white-hot edge in a band of
 * its colour, flames standing up off it and streaming back, sparks shed from its horns.
 */
public final class CrescentModel {
    private static final int POINTS = 17;

    private CrescentModel() {}

    /** Round the pose's origin, flying along {@code forward} (unit), {@code span} wide, at heat {@code tier}; {@code tilt} rolls it. */
    public static void draw(PoseStack poseStack, MultiBufferSource buffer, View view, Vector3f forward, float span, float tilt, float age, float fade, int tier,
                            int seed, int light) {
        Vector3f eye = view.eye();
        float alive = 1 - fade;
        Vector3f f = new Vector3f(forward).normalize();
        Vector3f side = Math.abs(f.y) < .95f ? new Vector3f(f).cross(0, 1, 0).normalize() : new Vector3f(1, 0, 0);
        Vector3f up = new Vector3f(side).cross(f).normalize();
        // Rolled by tilt
        Vector3f across = new Vector3f(side).mul(Mth.cos(tilt)).add(new Vector3f(up).mul(Mth.sin(tilt)));
        Vector3f normal = new Vector3f(across).cross(f).normalize();
        Vector3f[] arc = new Vector3f[POINTS], band = new Vector3f[POINTS], edge = new Vector3f[POINTS];
        float[] w = new float[POINTS], w2 = new float[POINTS];
        int[] c1 = new int[POINTS], c2 = new int[POINTS];
        float r = span * .6f;
        for (int i = 0; i < POINTS; i++) {
            float s = (float) i / (POINTS - 1) * 2 - 1;
            float a = s * 1.15f;
            float taper = 1 - s * s;
            arc[i] = new Vector3f(across).mul(Mth.sin(a) * r).add(new Vector3f(f).mul((Mth.cos(a) - .55f) * r));
            Vector3f out = new Vector3f(across).mul(Mth.sin(a)).add(new Vector3f(f).mul(Mth.cos(a))).normalize();
            band[i] = out;
            edge[i] = normal;
            w[i] = .55f * taper + .04f;
            w2[i] = 1.4f * taper + .1f;
            c1[i] = glow(FireMesh.core(tier), alive * (.4f + .6f * taper));
            c2[i] = glow(FireMesh.glowOf(tier), .8f * alive * taper);
        }
        // Its glow
        VertexConsumer beam = FireMesh.light(buffer, FireMesh.BEAM);
        PoseStack.Pose p = poseStack.last();
        ribbon(beam, p, arc, band, w2, c2, .5f, 0, light, true);
        ribbon(beam, p, arc, edge, w2, c2, .5f, 0, light, true);
        // The blade: flames streaming back off its whole edge
        FireMesh.Batch fire = new FireMesh.Batch(view, tier, age, light);
        Vector3f[] back = new Vector3f[POINTS];
        float[] wc = new float[POINTS];
        for (int i = 0; i < POINTS; i++) {
            float s = (float) i / (POINTS - 1) * 2 - 1;
            float taper = 1 - s * s;
            back[i] = new Vector3f(f).mul(-(1.5f * taper + .25f)).add(new Vector3f(band[i]).mul(-.25f * taper)).add(0, .2f * taper, 0);
            wc[i] = alive * (.35f + .65f * taper);
        }
        fire.sheet(arc, back, wc, .16f * span / 3, seed);
        // Its white-hot edge
        beam = FireMesh.light(buffer, FireMesh.BEAM);
        p = poseStack.last();
        ribbon(beam, p, arc, band, w, c1, .5f, 0, light, true);
        ribbon(beam, p, arc, edge, w, c1, .5f, 0, light, true);
        // Flames streaming back off it
        p = poseStack.last();
        Vector3f behind = new Vector3f(f).negate();
        for (int k = 0; k < 22; k++) {
            int i = (int) (hash(seed + k * 3) * POINTS);
            float s = (float) i / (POINTS - 1) * 2 - 1;
            float t = (age * .3f + hash(seed + k * 5)) % 1;
            Vector3f c = new Vector3f(arc[i]).sub(new Vector3f(f).mul(t * 1.4f));
            float h = (1.1f * (1 - s * s) + .3f) * (1 - t * .6f);
            fire.lickAlong(c, behind, h * .6f, h, seed + 31 * (k), (1 - t) * alive);
        }
        VertexConsumer sparks = FireMesh.light(buffer, FireMesh.SPARK);
        p = poseStack.last();
        for (int k = 0; k < 14; k++) {
            float t = (age * .25f + hash(seed + k * 7)) % 1;
            Vector3f horn = k % 2 == 0 ? arc[0] : arc[POINTS - 1];
            Vector3f head = new Vector3f(horn).sub(new Vector3f(f).mul(.3f + 1.6f * t)).add((hash(seed + k) - .5f) * t, -.3f * t * t, (hash(seed + k * 3) - .5f) * t);
            streak(sparks, p, eye, head, new Vector3f(head).add(new Vector3f(f).mul(.3f)), .06f, glow(FireMesh.core(tier), (1 - t) * alive), light);
        }
        fire.flush(buffer, poseStack);
    }
}
