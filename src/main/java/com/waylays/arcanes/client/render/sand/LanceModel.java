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
 * A Sand Lance in flight: a spear of sand packed hard, the sand racing back along it, three ribbons of sand winding
 * round it as it spins, a point of glass, and a wake of sand clouds.
 */
public final class LanceModel {
    private LanceModel() {}

    /** At the lance's middle (the pose's origin), flying along {@code forward}. */
    public static void draw(PoseStack poseStack, MultiBufferSource buffer, WaterMesh.View view, Vector3f forward, float age, int seed, int light) {
        Vector3f f = new Vector3f(forward).normalize();
        Vector3f s1 = BossMesh.across(f), s2 = new Vector3f(f).cross(s1).normalize();
        float spin = age * .9f;
        // The shaft
        VertexConsumer flow = BossMesh.see(buffer, SandMesh.FLOW);
        PoseStack.Pose p = poseStack.last();
        int n = 10;
        Vector3f[] pts = new Vector3f[n + 1];
        float[] r = new float[n + 1];
        int[] col = new int[n + 1];
        for (int i = 0; i <= n; i++) {
            float t = (float) i / n;
            pts[i] = new Vector3f(f).mul(-1.4f + 2.2f * t);
            r[i] = .03f + .13f * Mth.sin(Mth.PI * Math.min(1, t * 1.05f)) * (1 - .35f * t);
            col[i] = BossMesh.argb(0xFFFFFF, Math.min(1, .3f + t * 2));
        }
        SandMesh.tube(flow, p, pts, r, 8, 1.2f, age * .3f, col, light);
        // Ribbons of sand winding round it
        for (int k = 0; k < 3; k++) {
            Vector3f[] rib = new Vector3f[n + 1];
            float[] w = new float[n + 1];
            int[] rc = new int[n + 1];
            for (int i = 0; i <= n; i++) {
                float t = (float) i / n;
                float a = spin + k * Mth.TWO_PI / 3 + t * 9;
                float rr = .2f * (1 - t * .55f);
                rib[i] = new Vector3f(f).mul(-1.7f + 2.3f * t).add(new Vector3f(s1).mul(Mth.cos(a) * rr)).add(new Vector3f(s2).mul(Mth.sin(a) * rr));
                w[i] = .13f * (1 - t * .6f);
                rc[i] = BossMesh.argb(k == 1 ? SandMesh.SAND_PALE : SandMesh.SAND, .85f * Math.min(1, t * 2.5f));
            }
            SandMesh.ribbon(flow, p, view.eye(), rib, w, 1.5f, age * .35f + k, rc, light);
        }
        // Its point of glass
        VertexConsumer glass = BossMesh.see(buffer, SandMesh.GLASS);
        RitualMesh.crystal(glass, poseStack.last(), new Vector3f(f).mul(.7f), f, .6f, .11f, spin, BossMesh.argb(SandMesh.GLASS_TINT, .9f), light);
        // Its wake
        VertexConsumer dust = BossMesh.see(buffer, BossMesh.DUST);
        for (int k = 0; k < 6; k++) {
            float t = (age * .3f + k / 6f) % 1;
            Vector3f at = new Vector3f(f).mul(-1.6f - t * 3.2f).add(new Vector3f(s1).mul((BossMesh.hash(seed + k) - .5f) * .4f * t));
            SandMesh.cloud(dust, poseStack, view, at, .4f + 1.1f * t, k + age * .1f, k % 2 == 0 ? SandMesh.SAND : SandMesh.SAND_PALE, .55f * (1 - t), light);
        }
        VertexConsumer glow = BossMesh.light(buffer, BossMesh.GLOW);
        RitualMesh.billboard(glow, poseStack, view, new Vector3f(f).mul(1.25f), .8f, 0, -1, BossMesh.glow(SandMesh.SUN, .6f), light);
    }
}
