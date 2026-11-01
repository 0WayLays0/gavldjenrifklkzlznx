package com.waylays.arcanes.client.render.wind;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.waylays.arcanes.client.render.water.WaterMesh;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

/** Tailwind's ward on someone: three rings of wind circling them, each tilted its own way, a few leaves riding them. */
public final class WindWardModel {
    private WindWardModel() {}

    /** Round someone standing at the pose's origin, {@code height} tall; {@code alpha} how strong it is now. */
    public static void draw(PoseStack poseStack, MultiBufferSource buffer, WaterMesh.View view, float age, float height, float alpha, int seed, int light) {
        Vector3f eye = view.eye();
        VertexConsumer lines = WindMesh.lines(buffer);
        PoseStack.Pose p = poseStack.last();
        Vector3f[][] rings = new Vector3f[3][];
        for (int k = 0; k < 3; k++) {
            float tilt = (k - 1) * .45f + .1f * Mth.sin(age * .05f + k);
            Vector3f u = new Vector3f(1, 0, 0);
            Vector3f v = new Vector3f(0, Mth.sin(tilt), Mth.cos(tilt));
            float spin = age * (.22f + .05f * k) * (k == 1 ? -1 : 1);
            Vector3f c = new Vector3f(0, height * (.3f + .25f * k), 0);
            rings[k] = WindMesh.arc(c, u, v, .7f + .1f * k, spin, 2.6f, 14);
            WindMesh.line(lines, p, eye, rings[k], .09f, WaterMesh.mix(WindMesh.AIR, WindMesh.JADE, .3f * k), .85f * alpha);
            WindMesh.line(lines, p, eye, WindMesh.arc(c, u, v, .7f + .1f * k, spin + Mth.PI, 1.4f, 9), .06f, WindMesh.AIR, .5f * alpha);
        }
        VertexConsumer leaves = WindMesh.leaves(buffer);
        p = poseStack.last();
        for (int k = 0; k < 3; k++) {
            WindMesh.leaf(leaves, p, rings[k][rings[k].length - 1], .2f * alpha, age * .5f + k, age * .3f, (seed + k) & 3, light);
        }
    }
}
