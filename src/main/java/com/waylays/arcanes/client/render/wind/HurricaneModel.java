package com.waylays.arcanes.client.render.wind;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.waylays.arcanes.client.render.water.WaterMesh;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

/**
 * Hurricane (Wind Magic's ultimate): a great storm wheeling round the caster, who stands in its calm eye: a towering
 * wall of wind, its lines racing round faster the nearer the eye, bands of curling air and storm-grey cloud turning in
 * it, a canopy of dark cloud over it, leaves and dust whirled round it, a skirt of dust where it meets the ground.
 */
public final class HurricaneModel {
    private HurricaneModel() {}

    /** Round the pose's origin; {@code grow}, {@code fade} (0 to 1) how far it has risen and died; {@code eye} the calm. */
    public static void draw(PoseStack poseStack, MultiBufferSource buffer, WaterMesh.View view, float age, float grow, float fade, float eyeRadius, float radius,
                            float height, int seed, int light) {
        Vector3f eye = view.eye();
        float alive = grow * (1 - fade);
        float h = height * (.4f + .6f * grow);
        float reach = eyeRadius + (radius - eyeRadius) * (.3f + .7f * grow);
        VertexConsumer lines = WindMesh.lines(buffer);
        PoseStack.Pose p = poseStack.last();
        for (int k = 0; k < 90; k++) {
            float r = eyeRadius + (reach - eyeRadius) * WindMesh.hash(seed + k * 3);
            float y0 = h * .85f * WindMesh.hash(seed + k * 5);
            float spin = age * (.04f + .09f * eyeRadius / r);
            Vector3f[] path = WindMesh.helix(0, 0, r, r * (1.02f + .06f * WindMesh.hash(seed + k)), WindMesh.hash(seed + k * 7) * Mth.TWO_PI + spin, 1.5f, y0,
                    y0 + h * .12f, 14);
            float head = (age * .04f + WindMesh.hash(seed + k * 11)) % 1 * 1.3f;
            WindMesh.flow(lines, p, eye, path, head, .5f, .2f + .3f * WindMesh.hash(seed + k * 13), WaterMesh.mix(WindMesh.AIR, WindMesh.STORM,
                    WindMesh.hash(seed + k * 17) * .55f), .85f * alive);
        }
        // Bands of curling air, and the canopy of storm cloud
        VertexConsumer wisps = WindMesh.see(buffer, WindMesh.WISP);
        for (int k = 0; k < 40; k++) {
            float r = eyeRadius + 1 + (reach - eyeRadius - 1) * WindMesh.hash(seed + k * 19);
            float a = WindMesh.hash(seed + k * 23) * Mth.TWO_PI + age * (.03f + .06f * eyeRadius / r);
            float y = 1 + (h - 1) * WindMesh.hash(seed + k * 29);
            WaterMesh.puff(wisps, poseStack, view, Mth.cos(a) * r, y, Mth.sin(a) * r, 3 + 2.5f * WindMesh.hash(seed + k), age * .1f + k, k & 3,
                    WaterMesh.argb(WaterMesh.mix(WindMesh.JADE, WindMesh.STORM, .4f), .45f * alive), light);
        }
        VertexConsumer cloud = WindMesh.see(buffer, WindMesh.DUST);
        for (int k = 0; k < 18; k++) {
            float r = 2 + reach * 1.05f * WindMesh.hash(seed + k * 31);
            float a = WindMesh.hash(seed + k * 37) * Mth.TWO_PI + age * .015f;
            WaterMesh.puff(cloud, poseStack, view, Mth.cos(a) * r, h + 1 + 1.5f * WindMesh.hash(seed + k * 41), Mth.sin(a) * r, 6 + 3 * WindMesh.hash(seed + k), k,
                    -1, WaterMesh.argb(WindMesh.STORM, .5f * alive), light);
        }
        for (int k = 0; k < 16; k++) {
            float a = k * Mth.TWO_PI / 16 + age * .05f;
            WaterMesh.puff(cloud, poseStack, view, Mth.cos(a) * reach, .8f, Mth.sin(a) * reach, 3, k + age * .02f, -1, WaterMesh.argb(WindMesh.DUSTY, .45f * alive),
                    light);
        }
        VertexConsumer ring = WindMesh.see(buffer, WindMesh.RING);
        WindMesh.ring(ring, poseStack.last(), .06f, reach * 1.08f, age * .05f, WindMesh.AIR, .5f * alive);
        // Leaves whirled round it
        VertexConsumer leaves = WindMesh.leaves(buffer);
        p = poseStack.last();
        for (int k = 0; k < 36; k++) {
            float r = eyeRadius + (reach - eyeRadius) * WindMesh.hash(seed + k * 43);
            float a = WindMesh.hash(seed + k * 47) * Mth.TWO_PI + age * (.06f + .1f * eyeRadius / r);
            float y = h * WindMesh.hash(seed + k * 53) + .5f * Mth.sin(age * .1f + k);
            WindMesh.leaf(leaves, p, new Vector3f(Mth.cos(a) * r, y, Mth.sin(a) * r), .35f * alive, age * .5f + k, age * .4f + k, k & 3, light);
        }
    }
}
