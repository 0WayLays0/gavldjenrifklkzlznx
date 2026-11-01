package com.waylays.arcanes.client.render.sand;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.waylays.arcanes.client.render.boss.BossMesh;
import com.waylays.arcanes.client.render.echo.EchoMesh;
import com.waylays.arcanes.client.render.water.WaterMesh;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

/** A Sand Blast: a gout of sand flung from the hand over a cone ahead, grains streaking, clouds of it billowing out. */
public final class BlastModel {
    /** How long it lasts (ticks). */
    public static final int LIFE = 16;

    private BlastModel() {}

    /** From the hand (the pose's origin), out along {@code forward}, {@code length} long, {@code half} radians wide. */
    public static void draw(PoseStack poseStack, MultiBufferSource buffer, WaterMesh.View view, Vector3f forward, float length, float half, float age,
                            int seed, int light) {
        Vector3f[] f = EchoMesh.frame(forward);
        float fade = 1 - BossMesh.ramp(age, LIFE - 8, LIFE);
        VertexConsumer streaks = BossMesh.see(buffer, SandMesh.STREAK);
        PoseStack.Pose p = poseStack.last();
        Vector3f[] heads = new Vector3f[40];
        for (int k = 0; k < 40; k++) {
            float t = Mth.clamp((age - BossMesh.hash(seed + k) * 4) / 9, 0, 1);
            if (t <= 0 || t >= 1) {
                continue;
            }
            float a = BossMesh.hash(seed + k * 3) * Mth.TWO_PI, r = Mth.sqrt(BossMesh.hash(seed + k * 5)) * Mth.sin(half);
            Vector3f d = new Vector3f(forward).add(new Vector3f(f[0]).mul(Mth.cos(a) * r)).add(new Vector3f(f[1]).mul(Mth.sin(a) * r)).normalize();
            Vector3f head = new Vector3f(d).mul(length * (1 - (1 - t) * (1 - t))).add(0, -.6f * t * t, 0);
            heads[k] = head;
            SandMesh.streak(streaks, p, view, head, new Vector3f(head).sub(new Vector3f(d).mul(1.2f + 1.5f * (1 - t))), .1f, k % 3 == 0 ? SandMesh.SAND_PALE
                    : SandMesh.SAND, .9f * fade, light);
        }
        VertexConsumer dust = BossMesh.see(buffer, BossMesh.DUST);
        for (int k = 0; k < 14; k++) {
            float t = Mth.clamp((age - k * .5f) / 12, 0, 1);
            if (t <= 0) {
                continue;
            }
            float a = BossMesh.hash(seed + k * 7) * Mth.TWO_PI, r = BossMesh.hash(seed + k * 11) * Mth.sin(half) * .8f;
            Vector3f d = new Vector3f(forward).add(new Vector3f(f[0]).mul(Mth.cos(a) * r)).add(new Vector3f(f[1]).mul(Mth.sin(a) * r)).normalize();
            Vector3f at = new Vector3f(d).mul(length * .9f * (float) Math.sqrt(t));
            SandMesh.cloud(dust, poseStack, view, at, .8f + 2.4f * t, a + t * 2, k % 2 == 0 ? SandMesh.SAND : SandMesh.SAND_PALE, .7f * (1 - t) * fade, light);
        }
    }
}
