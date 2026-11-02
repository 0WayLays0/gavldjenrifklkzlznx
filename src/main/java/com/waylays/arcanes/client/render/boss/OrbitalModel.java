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
 * Orbital Strike: high above the mark a ring of light turns in the sky and a thin thread of light hangs from it to the
 * ground, narrowing onto the mark; then a pillar of white fire as wide as the mark falls from the ring, a flash, and
 * the ground glows where it struck.
 */
public final class OrbitalModel {
    private OrbitalModel() {}

    /** On the mark (the pose's origin), the ring at {@code sky}; it strikes {@code hit} ticks ago (negative before); {@code radius} the mark's. */
    public static void draw(PoseStack poseStack, MultiBufferSource buffer, WaterMesh.View view, Vector3f sky, float age, float hit, float radius, int zone, int seed,
                            int light) {
        float appear = BossMesh.ramp(age, 0, 16);
        float fade = 1 - BossMesh.ramp(hit, 10, 26);
        float k0 = appear * fade;
        if (k0 < .01f) {
            return;
        }
        float h = Math.min(sky.y, 40);
        // The ring in the sky, turning, and its runes
        VertexConsumer runes = BossMesh.light(buffer, BossMesh.RUNES);
        BossMesh.annulus(runes, poseStack.last(), new Vector3f(0, h, 0), new Vector3f(1, 0, 0), new Vector3f(0, 0, 1), radius * 1.2f, radius * 2.4f, 32, 2, age * .03f, 0,
                BossMesh.glow(zone, k0), BossMesh.glow(zone, .4f * k0), light, true);
        VertexConsumer beam = BossMesh.light(buffer, BossMesh.BEAM);
        PoseStack.Pose p = poseStack.last();
        if (hit < 0) {
            // The thread, narrowing onto the mark
            float lock = BossMesh.ramp(age, 0, Math.max(1, age - hit));
            RitualMesh.shaft(beam, p, view.eye(), new Vector3f(0, h, 0), new Vector3f(0, 0, 0), .2f + radius * (1 - lock), BossMesh.glow(zone, .5f * appear), light);
            RitualMesh.shaft(beam, p, view.eye(), new Vector3f(0, h, 0), new Vector3f(0, 0, 0), .08f, BossMesh.glow(0xFFFFFF, appear), light);
        } else {
            // The pillar of white fire
            float t = Mth.clamp(hit / 26, 0, 1);
            float w = radius * 2.2f * (1 - t * .6f);
            RitualMesh.shaft(beam, p, view.eye(), new Vector3f(0, h, 0), new Vector3f(0, -.5f, 0), w, BossMesh.glow(zone, k0), light);
            RitualMesh.shaft(beam, p, view.eye(), new Vector3f(0, h, 0), new Vector3f(0, -.5f, 0), w * .45f, BossMesh.glow(0xFFFFFF, k0), light);
            VertexConsumer scorch = BossMesh.light(buffer, BossMesh.MOON_CRACKS);
            BossMesh.pool(scorch, poseStack.last(), 0, .06f, 0, radius * 3.4f, seed, BossMesh.glow(zone, k0), light);
            VertexConsumer glow = BossMesh.light(buffer, BossMesh.GLOW);
            RitualMesh.billboard(glow, poseStack, view, new Vector3f(0, 1.5f, 0), radius * 6 * (1 - t * .5f), 0, -1, BossMesh.glow(0xFFFFFF, (1 - t) * k0), light);
            BossMesh.pool(glow, poseStack.last(), 0, .08f, 0, radius * 4.5f, 0, BossMesh.glow(zone, .8f * k0), light);
        }
    }
}
