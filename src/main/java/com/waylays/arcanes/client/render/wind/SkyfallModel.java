package com.waylays.arcanes.client.render.wind;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.waylays.arcanes.client.render.water.WaterMesh;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

/**
 * Skyfall: first a whirling updraft round its victim carrying it up, then a downburst: lines of wind slamming down from
 * the sky round it, a ring of blown air and dust bursting out over the ground where it lands.
 */
public final class SkyfallModel {
    private SkyfallModel() {}

    /**
     * On the ground under its victim (the pose's origin); {@code lift} (0 to 1) how far up it has been carried,
     * {@code slam} (0 to 1) how far through the downburst (below 0 before); {@code height} how high it is carried.
     */
    public static void draw(PoseStack poseStack, MultiBufferSource buffer, WaterMesh.View view, float age, float lift, float slam, float height, int seed,
                            int light) {
        if (slam < 0) {
            UpdraftModel.draw(poseStack, buffer, view, age, lift * .6f, height * lift + 2.5f, 1.1f, 1.8f, seed, light);
            return;
        }
        Vector3f eye = view.eye();
        float fade = 1 - slam;
        VertexConsumer lines = WindMesh.lines(buffer);
        PoseStack.Pose p = poseStack.last();
        for (int k = 0; k < 22; k++) {
            float a = k * Mth.TWO_PI / 22 + WindMesh.hash(seed + k);
            float r = 1 + 1.4f * WindMesh.hash(seed + k * 3);
            Vector3f[] path = {new Vector3f(Mth.cos(a) * r * .4f, height + 4, Mth.sin(a) * r * .4f), new Vector3f(Mth.cos(a) * r * .8f, height * .5f, Mth.sin(a) * r * .8f),
                    new Vector3f(Mth.cos(a) * r, .2f, Mth.sin(a) * r), new Vector3f(Mth.cos(a) * (r + 3), .3f, Mth.sin(a) * (r + 3))};
            WindMesh.flow(lines, p, eye, path, slam * 1.5f + .1f * WindMesh.hash(seed + k * 5), .35f, .14f, WindMesh.AIR, .9f * fade);
        }
        VertexConsumer ring = WindMesh.see(buffer, WindMesh.RING);
        WindMesh.ring(ring, poseStack.last(), .06f, 1 + 7 * (1 - fade * fade), slam, WindMesh.AIR, .85f * fade);
        VertexConsumer dust = WindMesh.see(buffer, WindMesh.DUST);
        for (int k = 0; k < 12; k++) {
            float a = k * Mth.TWO_PI / 12;
            float rr = 1 + 5 * slam;
            WaterMesh.puff(dust, poseStack, view, Mth.cos(a) * rr, .5f + .5f * slam, Mth.sin(a) * rr, 1.5f + 2 * slam, k + slam * 2, -1,
                    WaterMesh.argb(WindMesh.DUSTY, .55f * fade), light);
        }
    }
}
