package com.waylays.arcanes.client.render.fire;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

import static com.waylays.arcanes.client.render.water.WaterMesh.*;

/**
 * Corona: a ring of Stellar Fire racing out from the caster over the ground, a wall of flame on its edge, tongues
 * leaping off it, a band of light where it burns and scorched glow left behind it, sparks thrown out ahead.
 */
public final class FlameRingModel {
    private static final int SEGS = 56;

    private FlameRingModel() {}

    /** {@code radius} how far it has spread, {@code height} its wall's height, {@code fade} (0 to 1) how faded, at heat {@code tier}. */
    public static void draw(PoseStack poseStack, MultiBufferSource buffer, View view, float age, float radius, float height, float fade, int tier, int seed,
                            int light) {
        Vector3f eye = view.eye();
        float alive = 1 - fade;
        // The light: a band on the ground where it burns, a glow left behind
        VertexConsumer ring = FireMesh.light(buffer, FireMesh.RING);
        FireMesh.groundRing(ring, poseStack.last(), .05f, radius * 1.16f, FireMesh.glowOf(tier), alive, light);
        VertexConsumer glow = FireMesh.light(buffer, FireMesh.GLOW);
        FireMesh.groundRing(glow, poseStack.last(), .04f, radius * 1.2f, FireMesh.glowOf(tier), .45f * alive, light);
        // The wall of flame on its edge
        VertexConsumer walls = FireMesh.walls(buffer, tier);
        Vector3f[] base = new Vector3f[SEGS + 1];
        float[] h = new float[SEGS + 1];
        int[] col = new int[SEGS + 1];
        for (int i = 0; i <= SEGS; i++) {
            float a = i * Mth.TWO_PI / SEGS;
            base[i] = new Vector3f(Mth.cos(a) * radius, 0, Mth.sin(a) * radius);
            h[i] = height * (.75f + .35f * noise(a * 3, age * .15f, seed)) * alive;
            col[i] = argb(0xFFFFFF, .95f * alive);
        }
        FireMesh.curtain(walls, poseStack.last(), base, h, col, 1.4f, age * .02f, light);
        // Tongues leaping off it
        VertexConsumer flames = FireMesh.flames(buffer, tier);
        PoseStack.Pose p = poseStack.last();
        for (int k = 0; k < 40; k++) {
            float a = hash(seed + k * 3) * Mth.TWO_PI;
            float t = (age * .12f + hash(seed + k * 5)) % 1;
            float rr = radius + (hash(seed + k * 7) - .5f) * .6f;
            float hh = height * (.8f + 1.1f * hash(seed + k)) * (1 - t * .5f) * alive;
            FireMesh.tongue(flames, p, eye, Mth.cos(a) * rr, height * .4f * t, Mth.sin(a) * rr, hh * .6f, hh, 0, FireMesh.frame(age, k), argb(0xFFFFFF, (1 - t) * alive),
                    light);
        }
        // Sparks thrown out ahead
        VertexConsumer sparks = FireMesh.light(buffer, FireMesh.SPARK);
        p = poseStack.last();
        for (int k = 0; k < 30; k++) {
            float a = hash(seed + k * 11) * Mth.TWO_PI;
            float t = (age * .15f + hash(seed + k * 13)) % 1;
            float rr = radius + 1.4f * t;
            Vector3f head = new Vector3f(Mth.cos(a) * rr, .3f + 1.4f * t - 1.2f * t * t, Mth.sin(a) * rr);
            Vector3f tail = new Vector3f(Mth.cos(a) * (rr - .5f), head.y - .2f, Mth.sin(a) * (rr - .5f));
            streak(sparks, p, eye, head, tail, .07f, glow(FireMesh.core(tier), (1 - t) * alive), light);
        }
    }
}
