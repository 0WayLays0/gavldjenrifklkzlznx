package com.waylays.arcanes.client.render.sand;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.waylays.arcanes.client.render.boss.BossMesh;
import com.waylays.arcanes.client.render.water.WaterMesh;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

/**
 * A Sandstorm round its caster: walls of blown sand wheeling round at three heights, streaks of grains racing round with
 * them, a haze of dust hanging in the air; its middle, where the caster stands, clear.
 */
public final class SandstormModel {
    private SandstormModel() {}

    /** Round the caster (the pose's origin), {@code radius} across; {@code age} of {@code life} ticks. */
    public static void draw(PoseStack poseStack, MultiBufferSource buffer, WaterMesh.View view, float radius, float age, float life, int seed, int light) {
        float rise = BossMesh.ramp(age, 0, 16);
        float fall = BossMesh.ramp(age, life - 16, life);
        float k = rise * (1 - fall);
        VertexConsumer dust = BossMesh.see(buffer, BossMesh.DUST);
        for (int layer = 0; layer < 3; layer++) {
            int n = 18;
            float y = .5f + layer * 1.4f;
            float speed = .06f - .012f * layer;
            for (int j = 0; j < n; j++) {
                float a = j * Mth.TWO_PI / n + age * speed + layer;
                float r = radius * (.55f + .45f * BossMesh.hash(seed + j * 7 + layer)) * (.5f + .5f * rise);
                SandMesh.cloud(dust, poseStack, view, new Vector3f(Mth.cos(a) * r, y + .4f * Mth.sin(age * .1f + j), Mth.sin(a) * r), 2.4f + 1.4f * layer, a * 2,
                        (j + layer) % 3 == 0 ? SandMesh.SAND_PALE : SandMesh.SAND, .5f * k, light);
            }
        }
        VertexConsumer streaks = BossMesh.see(buffer, SandMesh.STREAK);
        PoseStack.Pose p = poseStack.last();
        for (int j = 0; j < 28; j++) {
            float a = BossMesh.hash(seed + j) * Mth.TWO_PI + age * .12f;
            float r = radius * (.4f + .6f * BossMesh.hash(seed + j * 3));
            float y = .3f + 4 * BossMesh.hash(seed + j * 5);
            Vector3f head = new Vector3f(Mth.cos(a) * r, y, Mth.sin(a) * r);
            Vector3f tail = new Vector3f(Mth.cos(a - .35f) * r, y, Mth.sin(a - .35f) * r);
            SandMesh.streak(streaks, p, view, head, tail, .12f, SandMesh.SAND_PALE, .8f * k, light);
        }
    }
}
