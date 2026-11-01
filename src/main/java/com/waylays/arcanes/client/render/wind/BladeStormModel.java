package com.waylays.arcanes.client.render.wind;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.waylays.arcanes.client.render.water.WaterMesh;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

/** Storm of Blades: blades of wind wheeling round the caster, rings of wind whirling with them, leaves caught up. */
public final class BladeStormModel {
    private BladeStormModel() {}

    /** Where blade {@code k} of {@code count} is at {@code age}, round the caster's feet at the origin. */
    public static Vector3f blade(int k, int count, float age, float radius) {
        float a = age * .22f + k * Mth.TWO_PI / count;
        return new Vector3f(Mth.cos(a) * radius, 1.1f + .35f * Mth.sin(age * .12f + k * 1.7f), Mth.sin(a) * radius);
    }

    public static void draw(PoseStack poseStack, MultiBufferSource buffer, WaterMesh.View view, float age, int count, float radius, float alive, int seed,
                            int light) {
        Vector3f eye = view.eye();
        VertexConsumer lines = WindMesh.lines(buffer);
        PoseStack.Pose p = poseStack.last();
        for (int k = 0; k < count; k++) {
            Vector3f at = blade(k, count, age, radius);
            float a = age * .22f + k * Mth.TWO_PI / count;
            Vector3f forward = new Vector3f(-Mth.sin(a), 0, Mth.cos(a));
            GustBladeModel.blade(lines, p, eye, at, forward, 1.3f, age, alive, seed + k * 31);
        }
        for (int k = 0; k < 4; k++) {
            float spin = age * (.22f + .04f * k) + k * 1.3f;
            Vector3f c = new Vector3f(0, .5f + .45f * k, 0);
            WindMesh.line(lines, p, eye, WindMesh.arc(c, new Vector3f(1, 0, 0), new Vector3f(0, 0, 1), radius * (.8f + .12f * k), spin, 2.4f, 18), .1f,
                    WaterMesh.mix(WindMesh.AIR, WindMesh.JADE, .25f * k), .7f * alive);
        }
        VertexConsumer leaves = WindMesh.leaves(buffer);
        p = poseStack.last();
        for (int k = 0; k < 10; k++) {
            float a = age * .3f + k * .63f;
            float r = radius * (.6f + .6f * WindMesh.hash(seed + k));
            WindMesh.leaf(leaves, p, new Vector3f(Mth.cos(a) * r, .3f + 1.8f * WindMesh.hash(seed + k * 3), Mth.sin(a) * r), .24f * alive, age * .6f + k,
                    age * .4f, k & 3, light);
        }
    }
}
