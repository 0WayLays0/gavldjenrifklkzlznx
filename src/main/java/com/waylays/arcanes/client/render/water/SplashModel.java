package com.waylays.arcanes.client.render.water;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

import static com.waylays.arcanes.client.render.water.WaterMesh.*;

/**
 * A splash, the way water splashes when something strikes it hard: a crown of water thrown up in a thin wall that
 * flares outward, its rim breaking into points each with a drop at its tip; spray bursting up inside it; drops
 * arcing out and falling; a ring of foam running out over the ground; then it all falls back.
 */
public final class SplashModel {
    private static final int SEGS = 28;

    private SplashModel() {}

    /** {@code t} (0 to 1) how far through its life it is, {@code size} how big. */
    public static void draw(PoseStack poseStack, MultiBufferSource buffer, View view, float t, float size, int seed, int light) {
        float up = Mth.sin(Mth.PI * Math.min(1, t * 1.25f)) * (1 - t * .35f);
        float fade = 1 - t * t;
        float r = size * (.35f + .8f * t);
        float h = size * 1.25f * up;
        // The crown's wall, flaring outward, its rim broken into points
        int rows = 5;
        Vector3f[][] wall = new Vector3f[rows][SEGS + 1];
        float[][] u = new float[rows][SEGS + 1], v = new float[rows][SEGS + 1];
        int[][] col = new int[rows][SEGS + 1];
        float[] tipH = new float[SEGS + 1];
        for (int s = 0; s <= SEGS; s++) {
            float a = s * Mth.TWO_PI / SEGS;
            float points = (float) Math.pow(Math.abs(Mth.sin(a * 5 + seed)), 3);
            tipH[s] = h * (.6f + .5f * points + .15f * hash(seed + s % SEGS));
            for (int j = 0; j < rows; j++) {
                float k = (float) j / (rows - 1);
                float y = tipH[s] * k;
                float out = r * (1 + .45f * k * k);
                wall[j][s] = new Vector3f(Mth.cos(a) * out, y, Mth.sin(a) * out);
                u[j][s] = (float) s / SEGS * 4;
                v[j][s] = k;
                col[j][s] = argb(mix(SHALLOW, FOAM_WHITE, .4f + .6f * k), (.65f - .25f * k) * fade);
            }
        }
        VertexConsumer water = translucent(buffer, WATER);
        PoseStack.Pose p = poseStack.last();
        grid(water, p, wall, u, v, col, light, .7f);

        VertexConsumer foam = translucent(buffer, FOAM);
        p = poseStack.last();
        disc(foam, p, .04f, r * .5f, r * (1.8f + t), 24, 1.2f, seed * .1f, 0, argb(FOAM_WHITE, .75f * fade), argb(FOAM_WHITE, 0), light);

        // Spray bursting up inside it, clumps at the points
        VertexConsumer spray = translucent(buffer, SPRAY);
        for (int k = 0; k < 7; k++) {
            float a = hash(seed + k * 3) * Mth.TWO_PI;
            float out = r * .5f * hash(seed + k * 5);
            puff(spray, poseStack, view, Mth.cos(a) * out, h * (.4f + .5f * hash(seed + k)) + .2f, Mth.sin(a) * out, size * (.6f + .5f * t), k, k & 3,
                    argb(FOAM_WHITE, .85f * fade), light);
        }
        // Drops off the points, arcing out and falling
        VertexConsumer drops = translucent(buffer, STREAK);
        p = poseStack.last();
        float age = t * 16;
        for (int k = 0; k < 30; k++) {
            float a = hash(seed + k * 7) * Mth.TWO_PI;
            float sp = (.05f + .06f * hash(seed + k * 11)) * size;
            Vector3f from = new Vector3f(Mth.cos(a) * r * .5f, 0, Mth.sin(a) * r * .5f);
            Vector3f vel = new Vector3f(Mth.cos(a) * sp, (.16f + .16f * hash(seed + k * 13)) * (float) Math.sqrt(size), Mth.sin(a) * sp);
            Vector3f head = thrown(from, vel, age, .035f);
            if (head.y < 0) {
                continue;
            }
            streak(drops, p, view.eye(), head, thrown(from, vel, Math.max(0, age - 1.5f), .035f), .07f * size, argb(FOAM_WHITE, .9f * fade), light);
        }
    }
}
