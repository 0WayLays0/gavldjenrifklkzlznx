package com.waylays.arcanes.client.render.sand;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.waylays.arcanes.client.render.boss.BossMesh;
import com.waylays.arcanes.client.render.water.WaterMesh;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

/**
 * A Dune Wave: a wall of sand rolling over the ground, its crest curling forward and spilling, sand streaming off its
 * top and spraying back behind it.
 */
public final class DuneWaveModel {
    private DuneWaveModel() {}

    /** At the foot of the wave's front (the pose's origin), rolling along {@code forward}, {@code width} wide, {@code height} tall. */
    public static void draw(PoseStack poseStack, MultiBufferSource buffer, WaterMesh.View view, Vector3f forward, float width, float height, float age,
                            float fade, int seed, int light) {
        Vector3f fwd = new Vector3f(forward.x, 0, forward.z).normalize();
        Vector3f side = new Vector3f(-fwd.z, 0, fwd.x);
        float h = height * (.4f + .6f * BossMesh.ramp(age, 0, 6)) * fade;
        VertexConsumer dust = BossMesh.see(buffer, BossMesh.DUST);
        int across = Math.max(4, (int) (width / .9f));
        for (int i = 0; i <= across; i++) {
            float s = (i / (float) across - .5f) * width;
            float bump = .85f + .3f * Mth.sin(s * 1.3f + age * .3f);
            for (int j = 0; j < 4; j++) {
                float t = j / 3f;
                // The wall leans forward as it rises: its crest curls over
                Vector3f at = new Vector3f(side).mul(s).add(new Vector3f(fwd).mul(-1.2f + 1.3f * t * t)).add(0, h * bump * t + .3f, 0);
                SandMesh.cloud(dust, poseStack, view, at, 1.6f + .6f * (1 - t), age * .05f + i + j, j == 3 ? SandMesh.SAND_PALE : SandMesh.SAND, .8f * fade, light);
            }
        }
        // Sand streaming off its top and back
        VertexConsumer streaks = BossMesh.see(buffer, SandMesh.STREAK);
        PoseStack.Pose p = poseStack.last();
        for (int k = 0; k < 24; k++) {
            float t = (age * .1f + BossMesh.hash(seed + k)) % 1;
            float s = (BossMesh.hash(seed + k * 3) - .5f) * width;
            Vector3f head = new Vector3f(side).mul(s).add(new Vector3f(fwd).mul(-t * 2.5f)).add(0, h * (1.05f - .4f * t * t), 0);
            SandMesh.streak(streaks, p, view, head, new Vector3f(head).add(new Vector3f(fwd).mul(.9f)).add(0, .3f, 0), .12f, SandMesh.SAND_PALE, .8f * (1 - t) * fade,
                    light);
        }
    }
}
