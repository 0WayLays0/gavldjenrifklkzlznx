package com.waylays.arcanes.client.render.boss;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.waylays.arcanes.client.render.ritual.RitualMesh;
import com.waylays.arcanes.client.render.sand.SandMesh;
import com.waylays.arcanes.client.render.water.WaterMesh;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Sweeping Light: a great eye of light opens over the caster, its golden iris turning to follow its beam, rings of runes
 * round it; the beam sweeps all the way round over the ground, a burning furrow following it.
 */
public final class LaserModel {
    private LaserModel() {}

    /**
     * Under it (the pose's origin), the eye at {@code eye}; the sweep starts {@code hit} ticks ago (negative before) at
     * {@code yaw} (degrees), going round in {@code slices} slices {@code step} ticks apart, out to {@code radius}.
     */
    public static void draw(PoseStack poseStack, MultiBufferSource buffer, WaterMesh.View view, Vector3f eye, float age, float hit, float yaw, int slices, int step,
                            float radius, int zone, int seed, int light) {
        float appear = BossMesh.ramp(age, 0, 12);
        float span = slices * step;
        float fade = 1 - BossMesh.ramp(hit, span + 4, span + 14);
        float k0 = appear * fade;
        if (k0 < .01f) {
            return;
        }
        float turn = hit < 0 ? 0 : Math.min(1, hit / span);
        // Its slices go round the way the yaw grows: the angle (from +Z toward +X) the other way
        float world0 = -yaw * Mth.DEG_TO_RAD;
        float world = world0 - turn * Mth.TWO_PI;
        Vector3f aim = new Vector3f(Mth.sin(world), 0, Mth.cos(world));
        Vector3f spot = new Vector3f(aim).mul(radius * .85f).add(0, .1f, 0);
        Vector3f look = new Vector3f(spot).sub(eye).normalize();
        // The eye: white, its iris and pupil turned to its beam
        VertexConsumer white = BossMesh.see(buffer, BossMesh.MOON);
        SandMesh.ellipsoid(white, poseStack, eye, .9f * appear, .9f * appear, .9f * appear, 0, age * .01f, BossMesh.argb(0xF8F4E8, k0), light);
        VertexConsumer iris = BossMesh.light(buffer, BossMesh.RING);
        Vector3f s1 = BossMesh.across(look), s2 = new Vector3f(look).cross(s1).normalize();
        BossMesh.plate(iris, poseStack.last(), new Vector3f(eye).add(new Vector3f(look).mul(.86f * appear)), s1, s2, 1.0f * appear, BossMesh.glow(zone, k0), light);
        VertexConsumer pupil = BossMesh.see(buffer, BossMesh.DISC);
        BossMesh.plate(pupil, poseStack.last(), new Vector3f(eye).add(new Vector3f(look).mul(.9f * appear)), s1, s2, .45f * appear, BossMesh.argb(0x100808, k0), light);
        // Rings of runes round it
        VertexConsumer runes = BossMesh.light(buffer, BossMesh.RUNES);
        for (int k = 0; k < 2; k++) {
            float a = age * (k == 0 ? .03f : -.04f);
            Vector3f u = new Vector3f(Mth.cos(a + k), .3f * k, Mth.sin(a + k)).normalize();
            Vector3f v = new Vector3f(0, 1, 0).cross(u).normalize();
            BossMesh.annulus(runes, poseStack.last(), eye, u, k == 0 ? new Vector3f(0, 1, 0) : v, 1.3f, 1.8f, 24, 2, a, 0, BossMesh.glow(zone, .7f * k0),
                    BossMesh.glow(zone, .3f * k0), light, true);
        }
        VertexConsumer beam = BossMesh.light(buffer, BossMesh.BEAM);
        PoseStack.Pose p = poseStack.last();
        if (hit >= 0 && hit <= span + 4) {
            Vector3f far = new Vector3f(aim).mul(radius).add(0, .1f, 0);
            RitualMesh.shaft(beam, p, view.eye(), new Vector3f(eye), far, .9f, BossMesh.glow(zone, k0), light);
            RitualMesh.shaft(beam, p, view.eye(), new Vector3f(eye), far, .3f, BossMesh.glow(0xFFFFFF, k0), light);
            // The furrow it burns
            int m = 24;
            for (int i = 0; i < m; i++) {
                float t0 = (float) i / m, t1 = (float) (i + 1) / m;
                float b0 = world0 - turn * Mth.TWO_PI * t0, b1 = world0 - turn * Mth.TWO_PI * t1;
                Vector3f a0 = new Vector3f(Mth.sin(b0), 0, Mth.cos(b0)), a1 = new Vector3f(Mth.sin(b1), 0, Mth.cos(b1));
                BossMesh.quad2(beam, p, new Vector3f(a0).mul(radius * .3f).add(0, .08f, 0), new Vector3f(a0).mul(radius).add(0, .08f, 0),
                        new Vector3f(a1).mul(radius).add(0, .08f, 0), new Vector3f(a1).mul(radius * .3f).add(0, .08f, 0), 0, 0, 1, 0, 1, 1, 0, 1,
                        BossMesh.glow(zone, .45f * t0 * k0), BossMesh.glow(zone, .45f * t1 * k0), light);
            }
        }
        VertexConsumer glow = BossMesh.light(buffer, BossMesh.GLOW);
        RitualMesh.billboard(glow, poseStack, view, eye, 4, 0, -1, BossMesh.glow(zone, .5f * k0), light);
    }
}
