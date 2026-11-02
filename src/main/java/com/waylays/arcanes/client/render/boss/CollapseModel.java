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
 * Collapse: a sphere of utter black opens over the spot, a disc of violet light whirling round it ever faster, streaks
 * of light being dragged in from all round; it falls in on itself to a point of light, then bursts in a shell of
 * light racing outward and a flash.
 */
public final class CollapseModel {
    private CollapseModel() {}

    /** On the ground (the pose's origin), the sphere at {@code core}; it falls in {@code hit} ticks ago (negative before); dragging from {@code pull} away. */
    public static void draw(PoseStack poseStack, MultiBufferSource buffer, WaterMesh.View view, Vector3f core, float age, float hit, float pull, int zone, int seed,
                            int light) {
        float grow = BossMesh.ramp(age, 0, 20);
        float in = hit < 0 ? 0 : BossMesh.ramp(hit, 0, 5);
        float burst = hit < 8 ? 0 : Mth.clamp((hit - 8) / 20, 0, 1);
        float r = 2.2f * grow * (1 - in);
        // The black sphere
        if (r > .02f) {
            VertexConsumer black = BossMesh.solid(buffer, BossMesh.VOID);
            BossMesh.sphere(black, poseStack.last(), core, r, 10, 18, age * .01f, 0, 1, 0xFF050208, light);
            // Its disc, whirling
            VertexConsumer swirl = BossMesh.light(buffer, BossMesh.SWIRL);
            float tilt = .35f;
            Vector3f u = new Vector3f(1, 0, 0), v = new Vector3f(0, Mth.sin(tilt), Mth.cos(tilt));
            BossMesh.annulus(swirl, poseStack.last(), core, u, v, r * 1.15f, r * 3.2f, 32, 2, -age * (.05f + .02f * grow), 0, BossMesh.glow(0xFFFFFF, grow),
                    BossMesh.glow(zone, .6f * grow), light, true);
            // Streaks dragged in
            VertexConsumer beam = BossMesh.light(buffer, BossMesh.BEAM);
            PoseStack.Pose p = poseStack.last();
            for (int k = 0; k < 16; k++) {
                float t = (age * .05f + BossMesh.hash(seed + k)) % 1;
                float a = BossMesh.hash(seed + k * 3) * Mth.TWO_PI, h = (BossMesh.hash(seed + k * 5) - .3f) * 4;
                float d = pull * (1 - t);
                Vector3f from = new Vector3f(Mth.cos(a) * d, core.y + h * (1 - t), Mth.sin(a) * d);
                Vector3f to = new Vector3f(from).lerp(core, .25f);
                RitualMesh.shaft(beam, p, view.eye(), from, to, .15f, BossMesh.glow(zone, .8f * t * grow), light);
            }
        }
        // Its fall into a point of light, and the burst
        VertexConsumer glow = BossMesh.light(buffer, BossMesh.GLOW);
        if (hit >= 0 && hit < 10) {
            float f = 1 - hit / 10;
            RitualMesh.billboard(glow, poseStack, view, core, 2 + 10 * (1 - f), 0, -1, BossMesh.glow(0xFFFFFF, f), light);
        }
        if (burst > 0 && burst < 1) {
            VertexConsumer shell = BossMesh.light(buffer, BossMesh.RING);
            BossMesh.pool(shell, poseStack.last(), 0, .1f, 0, pull * 2.4f * burst, 0, BossMesh.glow(zone, 1 - burst), light);
            VertexConsumer veil = BossMesh.light(buffer, BossMesh.SWIRL);
            BossMesh.sphere(veil, poseStack.last(), core, 1 + pull * burst, 8, 16, age * .02f, 0, 1, BossMesh.glow(zone, .5f * (1 - burst)), light);
            VertexConsumer flash = BossMesh.light(buffer, BossMesh.GLOW);
            RitualMesh.billboard(flash, poseStack, view, core, 6 + 20 * burst, 0, -1, BossMesh.glow(zone, (1 - burst) * .8f), light);
        } else if (hit < 0) {
            RitualMesh.billboard(glow, poseStack, view, core, 7 * grow, 0, -1, BossMesh.glow(zone, .35f * grow), light);
        }
    }
}
