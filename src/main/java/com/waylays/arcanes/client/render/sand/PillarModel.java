package com.waylays.arcanes.client.render.sand;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.waylays.arcanes.client.render.boss.BossMesh;
import com.waylays.arcanes.client.render.water.WaterMesh;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

/**
 * A Sandstone Pillar: blocks of carved sandstone burst up out of the ground one on another, overshooting and settling,
 * sand spraying round its foot; it stands a moment, then crumbles, its blocks sliding apart and sinking in dust.
 */
public final class PillarModel {
    private static final int BLOCKS = 4;

    private PillarModel() {}

    /** On the ground at its foot (the pose's origin), {@code height} tall; {@code age} of {@code life} ticks. */
    public static void draw(PoseStack poseStack, MultiBufferSource buffer, WaterMesh.View view, float height, float age, float life, int seed, int light) {
        float rise = BossMesh.ramp(age, 0, 4);
        float over = 1 + .12f * Mth.sin(Mth.PI * BossMesh.ramp(age, 3, 9));
        float crumble = BossMesh.ramp(age, life - 14, life);
        VertexConsumer stone = BossMesh.see(buffer, SandMesh.SANDSTONE);
        PoseStack.Pose p = poseStack.last();
        float bh = height / BLOCKS;
        for (int k = 0; k < BLOCKS; k++) {
            float w = .75f - .06f * k;
            float y = (k + .5f) * bh * rise * over - height * (1 - rise);
            // Crumbling: each block slides off a little its own way and sinks
            float a = BossMesh.hash(seed + k) * Mth.TWO_PI;
            float slide = crumble * (.4f + .3f * k);
            Vector3f at = new Vector3f(Mth.cos(a) * slide, y - crumble * crumble * (k + 1) * bh, Mth.sin(a) * slide);
            SandMesh.block(stone, p, at, w, bh * .5f, w, BossMesh.argb(0xFFFFFF, 1 - crumble * crumble), light);
        }
        float burst = 1 - BossMesh.ramp(age, 0, 16);
        if (burst > 0) {
            SandMesh.spray(poseStack, buffer, view, 2.2f, 2.5f, 1 - burst, 14, seed, light);
        }
        if (crumble > 0) {
            VertexConsumer dust = BossMesh.see(buffer, BossMesh.DUST);
            for (int k = 0; k < 10; k++) {
                float a = k * Mth.TWO_PI / 10;
                SandMesh.cloud(dust, poseStack, view, new Vector3f(Mth.cos(a) * .9f, height * .5f * (1 - crumble) + .3f, Mth.sin(a) * .9f), 1.2f + 1.5f * crumble,
                        a, SandMesh.SAND, .6f * Mth.sin(crumble * Mth.PI), light);
            }
        }
    }
}
