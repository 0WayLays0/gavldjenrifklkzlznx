package com.waylays.arcanes.client.render.wind;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.waylays.arcanes.client.render.water.WaterMesh;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

/**
 * Cyclone: a whirlwind, a funnel narrow at the ground and opening as it rises, lines of wind racing round and up it,
 * curls of air and its own dust turning in it, leaves whirled round it, a ring of dust skirting its foot.
 */
public final class CycloneModel {
    private CycloneModel() {}

    /** The funnel's radius at height {@code y} of {@code h}. */
    public static float radius(float y, float h, float base, float top) {
        return base + (top - base) * (float) Math.pow(Mth.clamp(y / Math.max(.1f, h), 0, 1), 1.3f);
    }

    /** {@code grow}, {@code fade} (0 to 1) how far it has formed and died; {@code height}, {@code top} its size. */
    public static void draw(PoseStack poseStack, MultiBufferSource buffer, WaterMesh.View view, float age, float grow, float fade, float height, float top,
                            int seed, int light) {
        Vector3f eye = view.eye();
        float alive = grow * (1 - fade);
        float h = height * (.3f + .7f * grow);
        float base = .45f;
        VertexConsumer lines = WindMesh.lines(buffer);
        PoseStack.Pose p = poseStack.last();
        for (int k = 0; k < 22; k++) {
            float a0 = k * Mth.TWO_PI / 22 + age * .4f;
            int n = 20;
            Vector3f[] path = new Vector3f[n];
            for (int i = 0; i < n; i++) {
                float u = (float) i / (n - 1);
                float y = u * h;
                float a = a0 + u * 5f;
                float r = radius(y, h, base, top) * (.85f + .25f * WindMesh.hash(seed + k));
                path[i] = new Vector3f(Mth.cos(a) * r, y, Mth.sin(a) * r);
            }
            float head = (age * .05f + WindMesh.hash(seed + k * 7)) % 1 * 1.3f;
            WindMesh.flow(lines, p, eye, path, head, .32f, .1f + .1f * WindMesh.hash(seed + k * 3), WaterMesh.mix(WindMesh.AIR, WindMesh.JADE,
                    WindMesh.hash(seed + k * 5) * .8f), .9f * alive);
        }
        // Curls of air turning in it
        VertexConsumer wisps = WindMesh.see(buffer, WindMesh.WISP);
        for (int k = 0; k < 14; k++) {
            float u = (k + .5f) / 14;
            float y = u * h;
            float a = age * .35f + k * 2.4f;
            float r = radius(y, h, base, top) * .8f;
            WaterMesh.puff(wisps, poseStack, view, Mth.cos(a) * r, y, Mth.sin(a) * r, .8f + r * .7f, age * .2f + k, k & 3,
                    WaterMesh.argb(WindMesh.JADE, .55f * alive), light);
        }
        VertexConsumer dust = WindMesh.see(buffer, WindMesh.DUST);
        for (int k = 0; k < 10; k++) {
            float a = age * .3f + k * Mth.TWO_PI / 10;
            float rr = 1.2f + .9f * WindMesh.hash(seed + k * 11);
            float y = .3f + 1.2f * WindMesh.hash(seed + k * 13);
            WaterMesh.puff(dust, poseStack, view, Mth.cos(a) * rr, y, Mth.sin(a) * rr, 1.3f, k + age * .05f, -1, WaterMesh.argb(WindMesh.DUSTY, .5f * alive), light);
        }
        VertexConsumer ring = WindMesh.see(buffer, WindMesh.RING);
        WindMesh.ring(ring, poseStack.last(), .05f, 2.6f, age * .25f, WindMesh.AIR, .55f * alive);
        // Leaves whirled round it
        VertexConsumer leaves = WindMesh.leaves(buffer);
        p = poseStack.last();
        for (int k = 0; k < 16; k++) {
            float u = (age * .02f + WindMesh.hash(seed + k * 17)) % 1;
            float y = u * h;
            float a = age * .45f + k * 1.9f;
            float r = radius(y, h, base, top) * (.9f + .3f * WindMesh.hash(seed + k));
            WindMesh.leaf(leaves, p, new Vector3f(Mth.cos(a) * r, y, Mth.sin(a) * r), .3f * alive, age * .6f + k, age * .5f + k, k & 3, light);
        }
    }
}
