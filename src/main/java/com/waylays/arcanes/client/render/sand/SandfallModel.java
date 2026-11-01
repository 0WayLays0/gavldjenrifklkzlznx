package com.waylays.arcanes.client.render.sand;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.waylays.arcanes.client.render.boss.BossMesh;
import com.waylays.arcanes.client.render.ritual.RitualMesh;
import com.waylays.arcanes.client.render.water.WaterMesh;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

/**
 * Sandfall: a rift opens in the sky, dark and swirling, its rim of sand; a torrent of sand pours out of it in streams
 * wound round each other, crashing on the ground in billows of dust onto a heap that grows; when it stops, the last of
 * it falls away from the rift and the rift closes.
 */
public final class SandfallModel {
    private static final int STREAMS = 12;

    private SandfallModel() {}

    /** On the ground under it (the pose's origin), {@code radius} across, falling from {@code height}; {@code age} of {@code life} ticks. */
    public static void draw(PoseStack poseStack, MultiBufferSource buffer, WaterMesh.View view, float radius, float height, float age, float life, int seed,
                            int light) {
        float open = BossMesh.ramp(age, 0, 8);
        float stop = BossMesh.ramp(age, life - 16, life - 6);
        float close = BossMesh.ramp(age, life - 8, life);
        float pour = BossMesh.ramp(age, 3, 9);
        float heapH = (.3f + 1.3f * BossMesh.ramp(age, 6, life - 16)) * (1 - .4f * close);
        // The rift
        VertexConsumer swirl = BossMesh.light(buffer, BossMesh.SWIRL);
        float rift = radius * .8f * open * (1 - close);
        BossMesh.plate(swirl, poseStack.last(), new Vector3f(0, height, 0), new Vector3f(1, 0, 0), new Vector3f(0, 0, 1), rift * 2.4f, BossMesh.glow(SandMesh.GOLD, .6f),
                light);
        VertexConsumer voidC = BossMesh.see(buffer, BossMesh.VOID);
        BossMesh.plate(voidC, poseStack.last(), new Vector3f(0, height + .02f, 0), new Vector3f(1, 0, 0), new Vector3f(0, 0, 1), rift * 1.9f,
                BossMesh.argb(0x2A1A10, .9f), light);
        // The torrent: streams of sand wound round each other, and the heap
        VertexConsumer flow = BossMesh.see(buffer, SandMesh.FLOW);
        PoseStack.Pose p = poseStack.last();
        float top = height - (height - heapH) * stop;
        float bottom = heapH * .8f;
        if (pour > 0 && top > bottom + .2f) {
            for (int k = 0; k < STREAMS; k++) {
                float a0 = k * Mth.TWO_PI / STREAMS + BossMesh.hash(seed + k);
                float rr = radius * (.1f + .32f * BossMesh.hash(seed + k * 3));
                int n = 12;
                Vector3f[] pts = new Vector3f[n + 1];
                float[] r = new float[n + 1];
                int[] col = new int[n + 1];
                for (int i = 0; i <= n; i++) {
                    float t = (float) i / n;
                    float y = Mth.lerp(t, top, bottom);
                    float a = a0 + t * .7f + age * .03f;
                    float spread = rr * (1 + t * .9f) * pour;
                    pts[i] = new Vector3f(Mth.cos(a) * spread, y, Mth.sin(a) * spread);
                    r[i] = (.35f + .3f * BossMesh.hash(seed + k * 5)) * (1 + t * .6f) * pour;
                    col[i] = BossMesh.argb(k % 3 == 0 ? SandMesh.SAND_PALE : 0xFFFFFF, .85f * (1 - stop * .5f));
                }
                // Soft streams of sand, their edges melting into the air
                SandMesh.ribbon(flow, p, view.eye(), pts, r, 1.6f, -age * .9f - k * .3f, col, light);
            }
            // The heart of the torrent
            Vector3f[] core = {new Vector3f(0, top, 0), new Vector3f(0, (top + bottom) * .5f, 0), new Vector3f(0, bottom, 0)};
            float cr = radius * .22f * pour;
            SandMesh.ribbon(flow, p, view.eye(), core, new float[] {cr * 2, cr * 2.2f, cr * 2.8f}, 1.2f, -age * 1.1f,
                    SandMesh.colours(BossMesh.argb(0xFFFFFF, .95f * (1 - stop * .5f)), 3), light);
        }
        SandMesh.lathe(flow, p, new Vector3f(), new float[] {0, heapH * .5f, heapH * .85f, heapH}, new float[] {radius * .95f, radius * .6f, radius * .25f, 0}, 20, 3,
                age * .05f, BossMesh.argb(0xFFFFFF, 1 - close), light);
        // The rim of the rift: a ribbon of sand turning
        int m = 24;
        Vector3f[] rim = new Vector3f[m + 1];
        float[] w = new float[m + 1];
        int[] rc = new int[m + 1];
        for (int i = 0; i <= m; i++) {
            float a = i * Mth.TWO_PI / m + age * .05f;
            rim[i] = new Vector3f(Mth.cos(a) * rift, height - .1f, Mth.sin(a) * rift);
            w[i] = .5f;
            rc[i] = BossMesh.argb(SandMesh.SAND_PALE, .8f * open * (1 - close));
        }
        SandMesh.ribbon(flow, p, view.eye(), rim, w, 1, age * .2f, rc, light);
        // Billows of dust where it crashes down
        VertexConsumer dust = BossMesh.see(buffer, BossMesh.DUST);
        float crash = pour * (1 - stop);
        for (int k = 0; k < 12; k++) {
            float t = (age * .05f + k / 12f) % 1;
            float a = k * Mth.TWO_PI / 12 + BossMesh.hash(seed + k * 7);
            float r = radius * (.5f + .7f * t);
            SandMesh.cloud(dust, poseStack, view, new Vector3f(Mth.cos(a) * r, .3f + 1.6f * t, Mth.sin(a) * r), 1.4f + 2 * t, a + t, k % 3 == 0 ? SandMesh.SAND_PALE : SandMesh.SAND,
                    .7f * (1 - t) * crash, light);
        }
        VertexConsumer ground = BossMesh.ground(buffer, SandMesh.SWIRL);
        BossMesh.pool(ground, poseStack.last(), 0, .03f, 0, radius * 2.6f, age * .03f, BossMesh.argb(0xFFFFFF, .7f * open * (1 - close)), light);
        SandMesh.grains(poseStack, buffer, view, radius, height, age, 16, open * (1 - close), seed, light);
        VertexConsumer glow = BossMesh.light(buffer, BossMesh.GLOW);
        RitualMesh.billboard(glow, poseStack, view, new Vector3f(0, height, 0), rift * 3, 0, -1, BossMesh.glow(SandMesh.GOLD, .3f), light);
    }
}
