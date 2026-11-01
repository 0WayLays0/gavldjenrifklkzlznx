package com.waylays.arcanes.client.render.wind;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.waylays.arcanes.client.render.water.WaterMesh;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

/**
 * Vacuum Sphere: the air drawn out of a place: lines of wind rushing in from all round to a dark, churning heart that
 * swells as it pulls, dust dragged in; then it bursts, rings of air racing out, lines flung outward.
 */
public final class VacuumModel {
    private VacuumModel() {}

    /**
     * Its heart at the pose's origin; {@code pull} (0 to 1) how far it has drawn in, {@code burst} (0 to 1) how far it
     * has burst (below 0 before); {@code radius} how far it reaches.
     */
    public static void draw(PoseStack poseStack, MultiBufferSource buffer, WaterMesh.View view, float age, float pull, float burst, float radius, int seed,
                            int light) {
        Vector3f eye = view.eye();
        VertexConsumer lines = WindMesh.lines(buffer);
        PoseStack.Pose p = poseStack.last();
        if (burst < 0) {
            for (int k = 0; k < 40; k++) {
                Vector3f dir = dir(seed + k);
                Vector3f[] path = new Vector3f[7];
                for (int i = 0; i < 7; i++) {
                    float u = (float) i / 6;
                    float r = radius * (1 - u) + .35f;
                    float swirl = .8f * u * u;
                    path[i] = new Vector3f(dir.x * Mth.cos(swirl) - dir.z * Mth.sin(swirl), dir.y, dir.x * Mth.sin(swirl) + dir.z * Mth.cos(swirl)).mul(r);
                }
                float head = (age * .06f + WindMesh.hash(seed + k * 7)) % 1 * 1.3f;
                WindMesh.flow(lines, p, eye, path, head, .4f, .08f + .06f * WindMesh.hash(seed + k), WaterMesh.mix(WindMesh.AIR, WindMesh.JADE, .4f),
                        .85f * Math.min(1, pull * 3));
            }
        } else {
            for (int k = 0; k < 34; k++) {
                Vector3f dir = dir(seed + k);
                Vector3f[] path = {new Vector3f(dir).mul(.3f), new Vector3f(dir).mul(radius * .5f), new Vector3f(dir).mul(radius * 1.3f)};
                WindMesh.flow(lines, p, eye, path, burst * 1.4f, .45f, .14f, WindMesh.AIR, .9f * (1 - burst));
            }
        }
        // Its dark heart
        VertexConsumer wisps = WindMesh.see(buffer, WindMesh.WISP);
        if (burst < 0) {
            float size = .6f + 1.6f * pull;
            for (int k = 0; k < 3; k++) {
                WaterMesh.puff(wisps, poseStack, view, 0, 0, 0, size * (1.4f - .25f * k), -age * (.3f + .15f * k) + k * 2, k & 3,
                        WaterMesh.argb(k == 0 ? WindMesh.DEEP : WindMesh.TEAL, (.55f - .1f * k) * Math.min(1, pull * 2)), light);
            }
        } else {
            WaterMesh.puff(wisps, poseStack, view, 0, 0, 0, 1.5f + radius * burst, burst * 4, seed & 3, WaterMesh.argb(WindMesh.AIR, .6f * (1 - burst)), light);
        }
        VertexConsumer dust = WindMesh.see(buffer, WindMesh.DUST);
        for (int k = 0; k < 10 && burst < 0; k++) {
            float u = (age * .03f + WindMesh.hash(seed + k * 13)) % 1;
            Vector3f at = dir(seed + k * 3 + 100).mul(radius * (1 - u));
            WaterMesh.puff(dust, poseStack, view, at.x, at.y, at.z, .9f + .8f * (1 - u), k + age * .05f, -1, WaterMesh.argb(WindMesh.DUSTY, .4f * pull * u), light);
        }
        // Rings racing out as it bursts
        if (burst >= 0) {
            VertexConsumer ring = WindMesh.see(buffer, WindMesh.RING);
            float r = 1 + radius * 1.6f * (1 - (1 - burst) * (1 - burst));
            for (int k = 0; k < 3; k++) {
                poseStack.pushPose();
                poseStack.mulPose(new org.joml.Quaternionf().rotationXYZ(k * 1.05f, k * .7f, 0));
                WindMesh.ring(ring, poseStack.last(), 0, r * (1 - .12f * k), burst, WindMesh.AIR, .8f * (1 - burst));
                poseStack.popPose();
            }
        }
    }

    private static Vector3f dir(int seed) {
        float a = WindMesh.hash(seed * 3 + 1) * Mth.TWO_PI, b = (float) Math.acos(1 - 2 * WindMesh.hash(seed * 5 + 2));
        return new Vector3f(Mth.cos(a) * Mth.sin(b), Mth.cos(b) * .8f, Mth.sin(a) * Mth.sin(b));
    }
}
