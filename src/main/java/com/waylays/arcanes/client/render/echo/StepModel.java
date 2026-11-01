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
 * Echo Step: where its caster stood, an echo of them lingers, a figure of rings of sound humming louder and louder,
 * then it bursts in rings racing out over the ground and up.
 */
public final class StepModel {
    private StepModel() {}

    /** At the echo's feet (the pose's origin); {@code until} ticks until it bursts (negative after), {@code radius} how far the burst goes. */
    public static void draw(PoseStack poseStack, MultiBufferSource buffer, WaterMesh.View view, float age, float until, float radius, int seed, int light) {
        Vector3f x = new Vector3f(1, 0, 0), z = new Vector3f(0, 0, 1);
        VertexConsumer wave = BossMesh.light(buffer, EchoMesh.WAVE);
        PoseStack.Pose p = poseStack.last();
        if (until > 0) {
            // The figure: rings stacked like a body, shimmering, quickening
            float hum = .78f + .22f * Mth.sin(age * (.5f + 1.5f * (1 - until / 30f)));
            float[] ys = {.15f, .45f, .75f, 1.0f, 1.25f, 1.5f, 1.75f, 1.95f};
            float[] rs = {.3f, .3f, .34f, .42f, .44f, .4f, .3f, .34f};
            for (int k = 0; k < ys.length; k++) {
                float wobble = .05f * Mth.sin(age * .7f + k);
                EchoMesh.ring(wave, p, new Vector3f(0, ys[k], 0), x, z, rs[k] + wobble, BossMesh.glow(k % 2 == 0 ? EchoMesh.ECHO : EchoMesh.PALE, .9f * hum),
                        light);
            }
            // The figure's ghost: a column of light for its body, a glow for its head
            VertexConsumer beam = BossMesh.light(buffer, BossMesh.BEAM);
            RitualMesh.shaft(beam, poseStack.last(), view.eye(), new Vector3f(0, .1f, 0), new Vector3f(0, 1.5f, 0), .8f, BossMesh.glow(EchoMesh.ECHO, .45f * hum), light);
            VertexConsumer glow = BossMesh.light(buffer, BossMesh.GLOW);
            RitualMesh.billboard(glow, poseStack, view, new Vector3f(0, 1.75f, 0), 1.1f, 0, -1, BossMesh.glow(EchoMesh.PALE, .7f * hum), light);
            RitualMesh.billboard(glow, poseStack, view, new Vector3f(0, 1.1f, 0), 2.8f, 0, -1, BossMesh.glow(EchoMesh.ECHO, .35f * hum), light);
            EchoMesh.souls(poseStack, buffer, view, .5f, 2.2f, age, 8, .8f, seed, light);
            return;
        }
        float since = -until;
        float t = BossMesh.ramp(since, 0, 12);
        float fade = 1 - t;
        for (int k = 0; k < 3; k++) {
            float tk = Mth.clamp(t * 1.3f - k * .15f, 0, 1);
            EchoMesh.ring(wave, p, new Vector3f(0, .1f + k * .05f, 0), x, z, .5f + radius * tk, BossMesh.glow(k == 1 ? EchoMesh.PALE : EchoMesh.ECHO, (1 - tk)),
                    light);
            EchoMesh.ring(wave, p, new Vector3f(0, 1, 0), x, new Vector3f(0, 1, 0), .5f + radius * .7f * tk, BossMesh.glow(EchoMesh.ECHO, .6f * (1 - tk)), light);
            EchoMesh.ring(wave, p, new Vector3f(0, 1, 0), z, new Vector3f(0, 1, 0), .5f + radius * .7f * tk, BossMesh.glow(EchoMesh.ECHO, .6f * (1 - tk)), light);
        }
        VertexConsumer glow = BossMesh.light(buffer, BossMesh.GLOW);
        RitualMesh.billboard(glow, poseStack, view, new Vector3f(0, 1, 0), 2 + radius * t, 0, -1, BossMesh.glow(EchoMesh.PALE, fade), light);
    }
}
