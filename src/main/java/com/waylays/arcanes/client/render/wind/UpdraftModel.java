package com.waylays.arcanes.client.render.wind;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.waylays.arcanes.client.render.water.WaterMesh;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

/**
 * Updraft (and the lifting gust of Skyfall): a column of wind spiralling up, lines racing round and up it, a ring of
 * blown air and dust bursting out over the ground at its foot, leaves whirled up it.
 */
public final class UpdraftModel {
    private UpdraftModel() {}

    /** {@code t} (0 to 1) how far through it is; {@code height}, {@code radius} its size; {@code speed} how fast it climbs. */
    public static void draw(PoseStack poseStack, MultiBufferSource buffer, WaterMesh.View view, float age, float t, float height, float radius, float speed,
                            int seed, int light) {
        Vector3f eye = view.eye();
        float fade = 1 - t * t;
        VertexConsumer lines = WindMesh.lines(buffer);
        PoseStack.Pose p = poseStack.last();
        for (int k = 0; k < 12; k++) {
            float a0 = k * Mth.TWO_PI / 12 + age * .2f;
            Vector3f[] path = WindMesh.helix(0, 0, radius * (.5f + .2f * WindMesh.hash(seed + k)), radius * (1.1f + .4f * WindMesh.hash(seed + k * 3)), a0,
                    3.4f + WindMesh.hash(seed + k * 5), 0, height, 18);
            float head = (age * .045f * speed + WindMesh.hash(seed + k * 7)) % 1 * 1.35f;
            WindMesh.flow(lines, p, eye, path, head, .35f, .11f + .06f * WindMesh.hash(seed + k * 9), WaterMesh.mix(WindMesh.AIR, WindMesh.JADE,
                    WindMesh.hash(seed + k * 11) * .7f), .9f * fade);
        }
        // The ring of blown air and dust over the ground
        VertexConsumer ring = WindMesh.see(buffer, WindMesh.RING);
        p = poseStack.last();
        float r = radius * (1 + 3 * Math.min(1, t * 2.5f));
        WindMesh.ring(ring, p, .05f, r, age * .1f, WindMesh.AIR, .7f * Math.max(0, 1 - t * 2));
        VertexConsumer dust = WindMesh.see(buffer, WindMesh.DUST);
        for (int k = 0; k < 9; k++) {
            float a = k * Mth.TWO_PI / 9 + WindMesh.hash(seed + k * 13);
            float rr = radius * (1 + 2.6f * Math.min(1, t * 2));
            WaterMesh.puff(dust, poseStack, view, Mth.cos(a) * rr, .4f, Mth.sin(a) * rr, 1.2f + 1.4f * t, k + age * .03f, -1,
                    WaterMesh.argb(WindMesh.DUSTY, .45f * (1 - t)), light);
        }
        // Leaves whirled up it
        VertexConsumer leaves = WindMesh.leaves(buffer);
        p = poseStack.last();
        for (int k = 0; k < 10; k++) {
            float u = (age * .035f * speed + WindMesh.hash(seed + k * 17)) % 1;
            float a = age * .25f + k * 1.7f + u * 5;
            float rr = radius * (.7f + .5f * u);
            WindMesh.leaf(leaves, p, new Vector3f(Mth.cos(a) * rr, u * height, Mth.sin(a) * rr), .26f, age * .5f + k, age * .4f + k, k & 3, light);
        }
    }
}
