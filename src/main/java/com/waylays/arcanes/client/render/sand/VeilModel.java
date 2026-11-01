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
 * Sand Veil: three rings of sand whirl round the caster, each tilted its own way and broken into arcs that chase each
 * other, the sand streaming along them; shards of glass circle among them; dust hangs round their feet; a shot turned
 * aside bursts in a puff of sand.
 */
public final class VeilModel {
    private VeilModel() {}

    /**
     * Round its wearer's feet (the pose's origin), {@code radius} across; {@code age} of {@code life} ticks; {@code since}
     * ticks since it last turned a shot, at {@code at} (in the same frame, or null).
     */
    public static void draw(PoseStack poseStack, MultiBufferSource buffer, WaterMesh.View view, float radius, float age, float life, float since, Vector3f at,
                            int seed, int light) {
        float grow = BossMesh.ramp(age, 0, 10);
        float fade = 1 - BossMesh.ramp(age, life - 14, life);
        float k0 = grow * fade;
        if (k0 < .01f) {
            return;
        }
        VertexConsumer flow = BossMesh.see(buffer, SandMesh.FLOW);
        PoseStack.Pose p = poseStack.last();
        for (int j = 0; j < 3; j++) {
            float tiltA = j * 2.1f + .3f, tilt = .12f + .12f * j;
            Vector3f n = new Vector3f(Mth.sin(tilt) * Mth.cos(tiltA), Mth.cos(tilt), Mth.sin(tilt) * Mth.sin(tiltA));
            Vector3f u = BossMesh.across(n), v = new Vector3f(n).cross(u).normalize();
            float r = radius * (.85f + .12f * j) * (.6f + .4f * grow);
            Vector3f c = new Vector3f(0, .7f + .35f * j, 0);
            float spin = age * (.09f + .03f * j) * (j % 2 == 0 ? 1 : -1);
            for (int arc = 0; arc < 3; arc++) {
                int n2 = 12;
                Vector3f[] pts = new Vector3f[n2 + 1];
                float[] w = new float[n2 + 1];
                int[] col = new int[n2 + 1];
                for (int i = 0; i <= n2; i++) {
                    float t = (float) i / n2;
                    float a = spin + arc * Mth.TWO_PI / 3 + t * 1.6f;
                    pts[i] = new Vector3f(u).mul(Mth.cos(a) * r).add(new Vector3f(v).mul(Mth.sin(a) * r)).add(c);
                    w[i] = .34f * Mth.sin(t * Mth.PI) + .04f;
                    col[i] = BossMesh.argb(j == 1 ? SandMesh.SAND_PALE : SandMesh.SAND, .8f * k0 * Mth.sin(t * Mth.PI));
                }
                SandMesh.ribbon(flow, p, view.eye(), pts, w, 1.2f, age * (j % 2 == 0 ? .4f : -.4f), col, light);
            }
        }
        // Shards of glass among them
        VertexConsumer glass = BossMesh.see(buffer, SandMesh.GLASS);
        p = poseStack.last();
        for (int k = 0; k < 6; k++) {
            float a = age * .07f + k * Mth.TWO_PI / 6;
            float r = radius * (.95f + .1f * Mth.sin(age * .1f + k));
            Vector3f pos = new Vector3f(Mth.cos(a) * r, 1.0f + .6f * Mth.sin(a * 2 + k), Mth.sin(a) * r);
            Vector3f dir = new Vector3f(-Mth.sin(a), .3f, Mth.cos(a)).normalize();
            RitualMesh.crystal(glass, p, pos, dir, .45f, .09f, age * .2f + k, BossMesh.argb(SandMesh.GLASS_TINT, .8f * k0), light);
        }
        // Dust round its feet, and a shot turned aside bursting in sand
        VertexConsumer dust = BossMesh.see(buffer, BossMesh.DUST);
        for (int k = 0; k < 8; k++) {
            float a = k * Mth.TWO_PI / 8 + age * .04f;
            SandMesh.cloud(dust, poseStack, view, new Vector3f(Mth.cos(a) * radius * .9f, .3f, Mth.sin(a) * radius * .9f), 1.2f, a, SandMesh.SAND, .3f * k0, light);
        }
        float hit = at != null && since < 10 ? 1 - since / 10 : 0;
        if (hit > 0) {
            for (int k = 0; k < 4; k++) {
                float a = k * Mth.HALF_PI + seed;
                SandMesh.cloud(dust, poseStack, view, new Vector3f(at).add(Mth.cos(a) * .3f * (1 - hit), .2f * k * (1 - hit), Mth.sin(a) * .3f * (1 - hit)),
                        .6f + 1.4f * (1 - hit), a, SandMesh.SAND_PALE, .8f * hit, light);
            }
            VertexConsumer glow = BossMesh.light(buffer, BossMesh.GLOW);
            RitualMesh.billboard(glow, poseStack, view, at, 1.5f * hit + .3f, 0, -1, BossMesh.glow(SandMesh.SUN, hit), light);
        }
        SandMesh.grains(poseStack, buffer, view, radius, 2.6f, age, 14, k0, seed, light);
    }
}
