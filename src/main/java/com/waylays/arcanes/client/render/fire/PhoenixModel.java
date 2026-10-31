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

        VertexConsumer walls = FireMesh.walls(buffer, tier);
        PoseStack.Pose p = poseStack.last();
        // The wings: a surface from the leading edge (dense fire) back to the trailing edge (tongues streaming off it)
        for (int side = -1; side <= 1; side += 2) {
            Vector3f[][] pts = new Vector3f[SPAN][CHORD];
            float[][] u = new float[SPAN][CHORD], v = new float[SPAN][CHORD];
            int[][] col = new int[SPAN][CHORD];
            for (int i = 0; i < SPAN; i++) {
                float e = (float) i / (SPAN - 1);
                float spanX = (.25f + 3.1f * e) * s;
                // The wing bends as it beats: the further out, the higher (or lower) it goes
                float lift = beat * (.25f * e + .9f * e * e) * s + .12f * Mth.sin(age * .6f + e * 4) * e * s;
                float lead = (.55f - .5f * e * e) * s;
                float chord = (1.5f - 1.05f * e) * s;
                for (int j = 0; j < CHORD; j++) {
                    float c = (float) j / (CHORD - 1);
                    float flutter = .08f * Mth.sin(age * .8f + i + j * 2) * c * s;
                    pts[i][j] = new Vector3f(side * spanX, lift + flutter - .1f * c * s, lead - chord * c - .35f * e * c * s);
                    u[i][j] = e * 2.4f + age * .02f;
                    v[i][j] = 1 - c * .95f;
                    col[i][j] = argb(0xFFFFFF, alive * (.95f - .15f * c));
                }
            }
            grid(walls, p, pts, u, v, col, light, 1);
        }
        // The tail: three broad streamers of fire rippling behind
        for (int k = -1; k <= 1; k++) {
            int n = 9;
            Vector3f[][] pts = new Vector3f[n][2];
            float[][] u = new float[n][2], v = new float[n][2];
            int[][] col = new int[n][2];
            for (int i = 0; i < n; i++) {
                float t = (float) i / (n - 1);
                float ripple = Mth.sin(age * .35f - t * 5 + k) * .35f * t * s;
                float x = (k * .35f * t * 2.2f) * s + ripple * .4f;
                float y = (-.15f - .35f * t) * s + ripple;
                float z = (-.6f - 3.4f * t) * s;
                float half = (.32f + .2f * Mth.sin(t * Mth.PI)) * s;
                pts[i][0] = new Vector3f(x - half, y, z);
                pts[i][1] = new Vector3f(x + half, y, z);
                for (int j = 0; j < 2; j++) {
                    u[i][j] = j * .6f + k * .3f;
                    v[i][j] = 1 - t * .95f;
                    col[i][j] = argb(0xFFFFFF, alive * (1 - t * .3f));
                }
            }
            grid(walls, p, pts, u, v, col, light, 1);
        }
        // Its body and head of flame, the crest, the long primaries at each wingtip
        VertexConsumer flames = FireMesh.flames(buffer, tier);
        p = poseStack.last();
        Vector3f back = new Vector3f(0, .1f, -1).normalize();
        for (int k = 0; k < 8; k++) {
            float z = (-.6f + 1.9f * k / 7) * s;
            float h = (1.2f - .35f * Math.abs(k - 4) / 4) * s;
            FireMesh.tongueAlong(flames, p, eye, new Vector3f(0, -.15f * s, z + h * .35f), back, h * .75f, h, FireMesh.frame(age, k), argb(0xFFFFFF, alive), light);
        }
        for (int k = 0; k < 3; k++) {
            Vector3f dir = new Vector3f((k - 1) * .35f, .8f, -.5f).normalize();
            FireMesh.tongueAlong(flames, p, eye, new Vector3f(0, .35f * s, 1.2f * s), dir, .35f * s, (.9f - .2f * Math.abs(k - 1)) * s, FireMesh.frame(age, k + 3),
                    argb(0xFFFFFF, alive), light);
        }
        for (int side = -1; side <= 1; side += 2) {
            float tipLift = beat * 1.15f * s;
            for (int k = 0; k < 4; k++) {
                Vector3f at = new Vector3f(side * 3.2f * s, tipLift, (.05f - .3f * k) * s);
                Vector3f dir = new Vector3f(side * .55f, beat * .2f, -1 - .2f * k).normalize();
                FireMesh.tongueAlong(flames, p, eye, at, dir, .4f * s, (1.3f - .15f * k) * s, FireMesh.frame(age, k + 5), argb(0xFFFFFF, alive), light);
            }
        }
        // Flames shed off it, sparks in its wake
        for (int k = 0; k < 12; k++) {
            float t = (age * .15f + hash(seed + k * 3)) % 1;
            Vector3f c = new Vector3f((hash(seed + k * 5) - .5f) * 5 * s, -.6f * t * s, -(.5f + 3.5f * t) * s);
            float h = .7f * (1 - t) * s;
            FireMesh.tongue(flames, p, eye, c.x, c.y - h * .4f, c.z, h * .7f, h, 0, FireMesh.frame(age, k + 4), argb(0xFFFFFF, (1 - t) * alive), light);
        }
        VertexConsumer sparks = FireMesh.light(buffer, FireMesh.SPARK);
        p = poseStack.last();
        for (int k = 0; k < 24; k++) {
            float t = (age * .12f + hash(seed + k * 7)) % 1;
            Vector3f head = new Vector3f((hash(seed + k * 11) - .5f) * 6 * s, -1.5f * t * t * s, -(1 + 5 * t) * s);
            streak(sparks, p, eye, head, new Vector3f(head).add(0, .35f, .25f), .07f, glow(FireMesh.core(tier), (1 - t) * alive), light);
        }
    }
}
