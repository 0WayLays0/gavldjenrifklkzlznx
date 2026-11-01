package com.waylays.arcanes.client.render.echo;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.waylays.arcanes.client.render.boss.BossMesh;
import com.waylays.arcanes.client.render.ritual.RitualMesh;
import com.waylays.arcanes.client.render.water.WaterMesh;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

/**
 * A Sound Wave in flight: a bright heart of sound, and rings spreading behind it one after another, each wider and
 * fainter than the last, square to its way, like the wake of a shout.
 */
public final class WaveModel {
    private static final int RINGS = 6;

    private WaveModel() {}

    /** Drawn at the wave (the pose's origin), flying along {@code forward}; {@code size} how wide its front is. */
    public static void draw(PoseStack poseStack, MultiBufferSource buffer, WaterMesh.View view, Vector3f forward, float age, float size, float fade, int seed,
                            int light) {
        Vector3f[] f = EchoMesh.frame(forward);
        VertexConsumer wave = BossMesh.light(buffer, EchoMesh.WAVE);
        PoseStack.Pose p = poseStack.last();
        for (int k = 0; k < RINGS; k++) {
            // Each ring born at the head and left behind, widening as it goes
            float t = ((age * .35f + k / (float) RINGS) % 1);
            Vector3f at = new Vector3f(forward).mul(-t * 3.2f);
            float r = size * (.35f + .9f * t);
            EchoMesh.ring(wave, p, at, f[0], f[1], r, BossMesh.glow(k % 2 == 0 ? EchoMesh.PALE : EchoMesh.ECHO, (1 - t) * fade), light);
        }
        // Arcs of its front, turning
        VertexConsumer arc = BossMesh.light(buffer, EchoMesh.ARC);
        p = poseStack.last();
        for (int k = 0; k < 3; k++) {
            float a0 = age * .2f + k * Mth.TWO_PI / 3;
            EchoMesh.arc(arc, p, new Vector3f(forward).mul(.2f), f[0], f[1], size * .55f, a0, a0 + 1.4f, .22f, BossMesh.glow(EchoMesh.ECHO, .9f * fade), light);
        }
        VertexConsumer glow = BossMesh.light(buffer, BossMesh.GLOW);
        RitualMesh.billboard(glow, poseStack, view, new Vector3f(), size * 1.3f, 0, -1, BossMesh.glow(EchoMesh.ECHO, .7f * fade), light);
        RitualMesh.billboard(glow, poseStack, view, new Vector3f(), size * .5f, 0, -1, BossMesh.glow(EchoMesh.PALE, fade), light);
    }
}
