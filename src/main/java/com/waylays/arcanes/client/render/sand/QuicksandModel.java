package com.waylays.arcanes.client.render.sand;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.waylays.arcanes.client.render.boss.BossMesh;
import com.waylays.arcanes.client.render.water.WaterMesh;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

/**
 * Quicksand: the ground turns to sand swirling down into a dark hollow at its middle, a lip of sand heaped round it,
 * grains and little clouds of dust drawn round and in.
 */
public final class QuicksandModel {
    private QuicksandModel() {}

    /** On the ground in its middle (the pose's origin), {@code radius} across; {@code age} of {@code life} ticks. */
    public static void draw(PoseStack poseStack, MultiBufferSource buffer, WaterMesh.View view, float radius, float age, float life, int seed, int light) {
        float open = BossMesh.ramp(age, 0, 10);
        float close = BossMesh.ramp(age, life - 14, life);
        float k = open * (1 - close);
        float r = radius * (.3f + .7f * open) * (1 - .6f * close);
        VertexConsumer swirl = BossMesh.ground(buffer, SandMesh.SWIRL);
        BossMesh.pool(swirl, poseStack.last(), 0, .04f, 0, r * 2.2f, age * .08f, BossMesh.argb(0xFFFFFF, .95f * k), light);
        BossMesh.pool(swirl, poseStack.last(), 0, .05f, 0, r * 1.3f, age * .15f + 1, BossMesh.argb(0xFFFFFF, .7f * k), light);
        VertexConsumer dark = BossMesh.ground(buffer, BossMesh.DISC);
        BossMesh.pool(dark, poseStack.last(), 0, .06f, 0, r * .45f, 0, BossMesh.argb(0x281808, .85f * k), light);
        VertexConsumer dust = BossMesh.see(buffer, BossMesh.DUST);
        // The lip of sand heaped round it
        for (int j = 0; j < 16; j++) {
            float a = j * Mth.TWO_PI / 16 + age * .01f;
            SandMesh.cloud(dust, poseStack, view, new Vector3f(Mth.cos(a) * r, .25f, Mth.sin(a) * r), 1.3f, a, SandMesh.SAND, .55f * k, light);
        }
        // Grains drawn round and in
        for (int j = 0; j < 18; j++) {
            float t = (age * (.02f + .01f * BossMesh.hash(seed + j)) + BossMesh.hash(seed + j * 3)) % 1;
            float a = BossMesh.hash(seed + j * 5) * Mth.TWO_PI + t * 5;
            float rr = r * (1 - t);
            SandMesh.cloud(dust, poseStack, view, new Vector3f(Mth.cos(a) * rr, .15f + .1f * (1 - t), Mth.sin(a) * rr), .45f, a, j % 2 == 0 ? SandMesh.SAND_PALE
                    : SandMesh.UMBER, .7f * Mth.sin(t * Mth.PI) * k, light);
        }
    }
}
