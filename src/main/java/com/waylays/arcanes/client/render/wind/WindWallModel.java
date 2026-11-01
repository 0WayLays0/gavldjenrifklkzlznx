package com.waylays.arcanes.client.render.wind;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.waylays.arcanes.client.render.RenderHelper;
import com.waylays.arcanes.client.render.water.WaterMesh;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

/**
 * Wind Wall: a wall of wind roaring up out of the ground: a sheet of moving air rushing up, lines of wind racing up it
 * and swirling off its top and ends, dust thrown up along its foot, leaves caught in it.
 */
public final class WindWallModel {
    private WindWallModel() {}

    /** Standing on the pose's origin along X ({@code width} wide, {@code height} tall), facing Z; {@code alive} how strong. */
    public static void draw(PoseStack poseStack, MultiBufferSource buffer, WaterMesh.View view, float age, float width, float height, float alive, int seed,
                            int light) {
        Vector3f eye = view.eye();
        float h = height * Math.min(1, alive * 1.5f);
        float w = width * .5f;
        VertexConsumer lines = WindMesh.lines(buffer);
        PoseStack.Pose p = poseStack.last();
        for (int k = 0; k < 30; k++) {
            float x0 = (WindMesh.hash(seed + k * 3) * 2 - 1) * w;
            float z0 = (WindMesh.hash(seed + k * 5) - .5f) * .5f;
            int n = 9;
            Vector3f[] path = new Vector3f[n];
            for (int i = 0; i < n; i++) {
                float u = (float) i / (n - 1);
                float sway = .35f * Mth.sin(u * 4 + k + age * .1f) * u;
                // Off its top the lines curl over and away
                float over = Math.max(0, u - .8f) * 5;
                path[i] = new Vector3f(Mth.clamp(x0 + sway, -w, w), u * h + over * .4f, z0 + over * over * .8f * (k % 2 == 0 ? 1 : -1));
            }
            float head = (age * .07f + WindMesh.hash(seed + k * 7)) % 1 * 1.35f;
            WindMesh.flow(lines, p, eye, path, head, .4f, .1f + .08f * WindMesh.hash(seed + k * 11), WaterMesh.mix(WindMesh.AIR, WindMesh.JADE,
                    WindMesh.hash(seed + k * 13) * .7f), .9f * alive);
        }
        // Swirls off its two ends
        for (int side = -1; side <= 1; side += 2) {
            for (int k = 0; k < 3; k++) {
                Vector3f c = new Vector3f(side * w, h * (.25f + .3f * k), 0);
                Vector3f[] curl = WindMesh.arc(c, new Vector3f(side, 0, 0), new Vector3f(0, 0, 1), .5f + .2f * k, age * .3f * side + k, 3.5f, 12);
                WindMesh.line(lines, p, eye, curl, .09f, WindMesh.AIR, .7f * alive);
            }
        }
        // The sheet of moving air
        VertexConsumer sheen = WindMesh.see(buffer, WindMesh.SHEEN);
        p = poseStack.last();
        int argb = WaterMesh.argb(WindMesh.AIR, .5f * alive);
        float v0 = -age * .08f;
        float uw = width / 6;
        RenderHelper.vertex(sheen, p, -w, 0, 0, v0, 0, argb, WindMesh.FULL, 0, 0, 1);
        RenderHelper.vertex(sheen, p, w, 0, 0, v0, uw, argb, WindMesh.FULL, 0, 0, 1);
        RenderHelper.vertex(sheen, p, w, h, 0, v0 + h / 3, uw, argb, WindMesh.FULL, 0, 0, 1);
        RenderHelper.vertex(sheen, p, -w, h, 0, v0 + h / 3, 0, argb, WindMesh.FULL, 0, 0, 1);
        // Dust thrown up along its foot
        VertexConsumer dust = WindMesh.see(buffer, WindMesh.DUST);
        for (int k = 0; k < 8; k++) {
            float x = (k + .5f) / 8 * width - w;
            float u = (age * .05f + WindMesh.hash(seed + k * 17)) % 1;
            WaterMesh.puff(dust, poseStack, view, x, .3f + 1.2f * u, (WindMesh.hash(seed + k) - .5f) * .6f, 1 + u, k + age * .05f, -1,
                    WaterMesh.argb(WindMesh.DUSTY, .45f * alive * (1 - u)), light);
        }
        VertexConsumer leaves = WindMesh.leaves(buffer);
        p = poseStack.last();
        for (int k = 0; k < 9; k++) {
            float u = (age * .04f + WindMesh.hash(seed + k * 19)) % 1;
            float x = (WindMesh.hash(seed + k * 23) * 2 - 1) * w + .4f * Mth.sin(age * .2f + k);
            WindMesh.leaf(leaves, p, new Vector3f(x, u * h * 1.1f, (WindMesh.hash(seed + k * 29) - .5f) * .6f), .26f * alive, age * .5f + k, age * .4f + k, k & 3,
                    light);
        }
    }
}
