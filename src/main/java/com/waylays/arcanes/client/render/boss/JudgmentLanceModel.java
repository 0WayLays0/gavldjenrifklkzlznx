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
 * Lances of Judgment: a giant lance of light hangs high over its strip, glowing brighter, then plunges onto it, lying
 * along it the whole length, and breaks up into motes of light.
 */
public final class JudgmentLanceModel {
    private JudgmentLanceModel() {}

    /**
     * From the strip's start (the pose's origin) to {@code to}, falling from {@code from}; it lands {@code hit} ticks ago
     * (negative before); {@code width} the strip's width.
     */
    public static void draw(PoseStack poseStack, MultiBufferSource buffer, WaterMesh.View view, Vector3f from, Vector3f to, float age, float hit, float width,
                            int zone, int seed, int light) {
        float appear = BossMesh.ramp(age, 0, 10);
        float fade = 1 - BossMesh.ramp(hit, 6, 16);
        float k0 = appear * fade;
        if (k0 < .01f) {
            return;
        }
        float drop = hit < -4 ? 0 : Mth.clamp((hit + 4) / 4, 0, 1);
        drop *= drop;
        float y = Mth.lerp(drop, from.y, .4f);
        Vector3f a = new Vector3f(0, y, 0), b = new Vector3f(to).add(0, y, 0);
        Vector3f d = new Vector3f(b).sub(a).normalize();
        VertexConsumer beam = BossMesh.light(buffer, BossMesh.BEAM);
        PoseStack.Pose p = poseStack.last();
        // The shaft, its core white
        RitualMesh.shaft(beam, p, view.eye(), new Vector3f(a), new Vector3f(b).sub(new Vector3f(d).mul(2)), width * .9f, BossMesh.glow(zone, .8f * k0), light);
        RitualMesh.shaft(beam, p, view.eye(), new Vector3f(a), new Vector3f(b).sub(new Vector3f(d).mul(2)), width * .3f, BossMesh.glow(0xFFFFFF, k0), light);
        // Its head: a long blade of crystal light
        VertexConsumer crystal = BossMesh.light(buffer, BossMesh.PRISM);
        RitualMesh.crystal(crystal, poseStack.last(), new Vector3f(b).sub(new Vector3f(d).mul(2.4f)), d, 3.2f, width * .7f, age * .1f, BossMesh.glow(0xFFFFFF, k0), light);
        // Its guard: a ring of light
        VertexConsumer ring = BossMesh.light(buffer, BossMesh.RING);
        Vector3f s1 = BossMesh.across(d), s2 = new Vector3f(d).cross(s1).normalize();
        BossMesh.plate(ring, poseStack.last(), new Vector3f(b).sub(new Vector3f(d).mul(2.4f)), s1, s2, width * 2.6f, BossMesh.glow(zone, k0), light);
        VertexConsumer glow = BossMesh.light(buffer, BossMesh.GLOW);
        float charge = hit < 0 ? appear : 1;
        RitualMesh.billboard(glow, poseStack, view, new Vector3f(a).add(b).mul(.5f), width * 6 * charge, 0, -1, BossMesh.glow(zone, .35f * k0), light);
        if (hit >= 0) {
            float t = Mth.clamp(hit / 14, 0, 1);
            VertexConsumer motes = BossMesh.light(buffer, BossMesh.MOTE);
            for (int k = 0; k < 14; k++) {
                float u = BossMesh.hash(seed + k);
                Vector3f at = new Vector3f(a).lerp(b, u).add((BossMesh.hash(seed + k * 3) - .5f) * width, .3f + t * 2.5f * BossMesh.hash(seed + k * 5), 0);
                RitualMesh.billboard(motes, poseStack, view, at, .35f, 0, -1, BossMesh.glow(k % 2 == 0 ? zone : 0xFFFFFF, 1 - t), light);
            }
        }
    }
}
