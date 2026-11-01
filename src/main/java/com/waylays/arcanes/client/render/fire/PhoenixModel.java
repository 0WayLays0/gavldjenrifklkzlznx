package com.waylays.arcanes.client.render.fire;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

import static com.waylays.arcanes.client.render.water.WaterMesh.*;

/**
 * The Phoenix: a great bird of Stellar Fire. Two broad wings of fire, beating slowly and bending as they beat, flames
 * streaming back off their trailing edges and their long primaries; a white-hot heart in a body of flame; a head with a
 * crest of three plumes; a long tail of fire rippling behind; flames shed off it and sparks falling in its wake.
 */
public final class PhoenixModel {
    private static final int SPAN = 9;
    private static final int CHORD = 4;

    private PhoenixModel() {}

    /**
     * Drawn in the pose's frame flying along +Z (the renderer turns the pose to its heading); {@code fade} (0 to 1) how
     * far it has burnt out; {@code scale} its size.
     */
    public static void draw(PoseStack poseStack, MultiBufferSource buffer, View view, float age, float bank, float fade, float scale, int tier, int seed,
                            int light) {
        Vector3f eye = view.eye();
        float alive = 1 - fade;
        float s = scale;
        float beat = Mth.sin(age * .3f);
        // Its light
        VertexConsumer glow = FireMesh.light(buffer, FireMesh.GLOW);
        FireMesh.halo(glow, poseStack, view, 0, 0, 0, 8 * s, FireMesh.glowOf(tier), .55f * alive, light);
        FireMesh.halo(glow, poseStack, view, 0, 0, .2f * s, 1.8f * s, FireMesh.core(tier), alive, light);
        FireMesh.halo(glow, poseStack, view, 0, .35f * s, 1.25f * s, .8f * s, FireMesh.core(tier), alive, light);

        FireMesh.Batch fire = new FireMesh.Batch(view, tier, age, light);
        PoseStack.Pose p = poseStack.last();
        // The wings: flames along the leading edge streaming back across them, more off their middle
        for (int side = -1; side <= 1; side += 2) {
            Vector3f[] lead = new Vector3f[SPAN], mid = new Vector3f[SPAN];
            Vector3f[] back = new Vector3f[SPAN], back2 = new Vector3f[SPAN];
            float[] a1 = new float[SPAN], a2 = new float[SPAN];
            for (int i = 0; i < SPAN; i++) {
                float e = (float) i / (SPAN - 1);
                float spanX = (.25f + 3.1f * e) * s;
                // The wing bends as it beats: the further out, the higher (or lower) it goes
                float lift = beat * (.25f * e + .9f * e * e) * s + .12f * Mth.sin(age * .6f + e * 4) * e * s;
                float leadZ = (.55f - .5f * e * e) * s;
                float chord = (1.5f - 1.05f * e) * s;
                lead[i] = new Vector3f(side * spanX, lift, leadZ);
                Vector3f trail = new Vector3f(side * spanX, lift - .1f * s, leadZ - chord - .35f * e * s);
                back[i] = new Vector3f(trail).sub(lead[i]).mul(1.35f).add(0, .1f * s, 0);
                mid[i] = new Vector3f(lead[i]).lerp(trail, .5f);
                back2[i] = new Vector3f(trail).sub(lead[i]).mul(.9f).add(0, .15f * s, 0);
                a1[i] = alive;
                a2[i] = alive * .8f;
            }
            fire.sheet(lead, back, a1, .2f * s, seed + side * 101);
            fire.sheet(mid, back2, a2, .28f * s, seed + side * 307);
        }
        // The tail: three streamers of fire rippling behind
        for (int k = -1; k <= 1; k++) {
            int n = 9;
            for (int i = 0; i < n; i++) {
                float t = (float) i / (n - 1);
                float ripple = Mth.sin(age * .35f - t * 5 + k) * .35f * t * s;
                Vector3f at = new Vector3f((k * .35f * t * 2.2f) * s + ripple * .4f, (-.15f - .35f * t) * s + ripple, (-.6f - 3.4f * t) * s);
                Vector3f dir = new Vector3f(k * .25f, -.1f + .2f * Mth.cos(age * .35f - t * 5 + k), -1).normalize();
                fire.lickAlong(at, dir, (.55f + .3f * Mth.sin(t * Mth.PI)) * s, (.9f - .3f * t) * s, seed + 500 + k * 37 + i, alive * (1 - t * .3f));
            }
        }
        // Its body and head of flame, the crest, the long primaries at each wingtip
        p = poseStack.last();
        Vector3f back = new Vector3f(0, .1f, -1).normalize();
        for (int k = 0; k < 8; k++) {
            float z = (-.6f + 1.9f * k / 7) * s;
            float h = (1.2f - .35f * Math.abs(k - 4) / 4) * s;
            fire.lickAlong(new Vector3f(0, -.15f * s, z + h * .35f), back, h * .75f, h, seed + 31 * (k), alive);
        }
        for (int k = 0; k < 3; k++) {
            Vector3f dir = new Vector3f((k - 1) * .35f, .8f, -.5f).normalize();
            fire.lickAlong(new Vector3f(0, .35f * s, 1.2f * s), dir, .35f * s, (.9f - .2f * Math.abs(k - 1)) * s, seed + 31 * (k + 3), alive);
        }
        for (int side = -1; side <= 1; side += 2) {
            float tipLift = beat * 1.15f * s;
            for (int k = 0; k < 4; k++) {
                Vector3f at = new Vector3f(side * 3.2f * s, tipLift, (.05f - .3f * k) * s);
                Vector3f dir = new Vector3f(side * .55f, beat * .2f, -1 - .2f * k).normalize();
                fire.lickAlong(at, dir, .4f * s, (1.3f - .15f * k) * s, seed + 31 * (k + 5), alive);
            }
        }
        // Flames shed off it, sparks in its wake
        for (int k = 0; k < 12; k++) {
            float t = (age * .15f + hash(seed + k * 3)) % 1;
            Vector3f c = new Vector3f((hash(seed + k * 5) - .5f) * 5 * s, -.6f * t * s, -(.5f + 3.5f * t) * s);
            float h = .7f * (1 - t) * s;
            fire.lick(c.x, c.y - h * .4f, c.z, h * .7f, h, 0, seed + 31 * (k + 4), (1 - t) * alive);
        }
        VertexConsumer sparks = FireMesh.light(buffer, FireMesh.SPARK);
        p = poseStack.last();
        for (int k = 0; k < 24; k++) {
            float t = (age * .12f + hash(seed + k * 7)) % 1;
            Vector3f head = new Vector3f((hash(seed + k * 11) - .5f) * 6 * s, -1.5f * t * t * s, -(1 + 5 * t) * s);
            streak(sparks, p, eye, head, new Vector3f(head).add(0, .35f, .25f), .07f, glow(FireMesh.core(tier), (1 - t) * alive), light);
        }
        fire.flush(buffer, poseStack);
    }
}
