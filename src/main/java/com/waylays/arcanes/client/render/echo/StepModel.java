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
 * Echo Step: where its caster stood, an echo of them lingers, a figure of light in their shape, drawn in lines, rings
 * of sound round its feet, waist and head, ripples spreading under it; it hums louder and louder, two ghosts of it
 * slipping apart from it, then it bursts: the figure flies apart, ripples race out over the ground and up, a column of
 * light, shards of sound.
 */
public final class StepModel {
    private StepModel() {}

    /**
     * At the echo's feet (the pose's origin), facing {@code yaw} (radians, see {@link EchoMesh#figure}); {@code until}
     * ticks until it bursts (negative after), {@code radius} how far the burst goes.
     */
    public static void draw(PoseStack poseStack, MultiBufferSource buffer, WaterMesh.View view, float age, float until, float radius, float yaw, int seed,
                            int light) {
        Vector3f x = new Vector3f(1, 0, 0), z = new Vector3f(0, 0, 1);
        if (until > 0) {
            float charge = Mth.clamp(1 - until / 30f, 0, 1);
            float hum = .78f + .22f * Mth.sin(age * (.5f + 1.5f * charge));
            float flicker = BossMesh.hash(seed + (int) (age * 2)) > .88f ? .45f : 1;
            // The figure of light, and two ghosts of it slipping apart as it hums louder
            VertexConsumer scan = BossMesh.light(buffer, EchoMesh.SCAN);
            EchoMesh.figure(scan, poseStack.last(), yaw, charge, BossMesh.glow(EchoMesh.ECHO, hum * flicker), light);
            for (int side = -1; side <= 1; side += 2) {
                poseStack.pushPose();
                float slip = side * charge * (.1f + .06f * Mth.sin(age * .9f + side));
                poseStack.translate(Mth.cos(yaw) * slip, .02f * side * charge, Mth.sin(yaw) * slip);
                EchoMesh.figure(scan, poseStack.last(), yaw, charge, BossMesh.glow(EchoMesh.ECHO, .22f * charge * hum), light);
                poseStack.popPose();
            }
            // Rings of sound round its feet, waist and head, tightening as it hums louder
            VertexConsumer wave = BossMesh.light(buffer, EchoMesh.WAVE);
            PoseStack.Pose p = poseStack.last();
            float[] ys = {.12f, 1.0f, 1.8f};
            for (int k = 0; k < ys.length; k++) {
                float r = .62f - .12f * charge + .05f * Mth.sin(age * .7f + k * 2);
                EchoMesh.ring(wave, p, new Vector3f(0, ys[k] + .05f * Mth.sin(age * .5f + k), 0), x, z, r,
                        BossMesh.glow(k == 1 ? EchoMesh.PALE : EchoMesh.ECHO, .85f * hum), light);
            }
            // Ripples under it
            VertexConsumer ripple = BossMesh.light(buffer, EchoMesh.RIPPLE);
            p = poseStack.last();
            for (int k = 0; k < 2; k++) {
                float t = (age * .05f + k * .5f) % 1;
                EchoMesh.groundRipple(ripple, p, 0, .04f + k * .01f, 0, .6f + 1.4f * t, age * .03f, BossMesh.glow(EchoMesh.ECHO, .7f * (1 - t) * hum), light);
            }
            VertexConsumer glow = BossMesh.light(buffer, BossMesh.GLOW);
            RitualMesh.billboard(glow, poseStack, view, new Vector3f(0, 1.75f, 0), .7f, 0, -1, BossMesh.glow(EchoMesh.PALE, .4f * hum), light);
            RitualMesh.billboard(glow, poseStack, view, new Vector3f(0, 1.1f, 0), 1.8f + charge, 0, -1, BossMesh.glow(EchoMesh.ECHO, .14f * hum), light);
            EchoMesh.souls(poseStack, buffer, view, .6f, 2.4f, age, 10, .8f, seed, light);
            return;
        }
        float since = -until;
        float t = BossMesh.ramp(since, 0, 12);
        float fade = 1 - t;
        // The figure flies apart
        float gone = 1 - BossMesh.ramp(since, 0, 5);
        if (gone > 0) {
            VertexConsumer scan = BossMesh.light(buffer, EchoMesh.SCAN);
            poseStack.pushPose();
            poseStack.translate(0, .3f * (1 - gone), 0);
            EchoMesh.figure(scan, poseStack.last(), yaw, 1 + 5 * (1 - gone), BossMesh.glow(EchoMesh.PALE, gone), light);
            poseStack.popPose();
        }
        // Ripples racing out over the ground, and up
        VertexConsumer ripple = BossMesh.light(buffer, EchoMesh.RIPPLE);
        PoseStack.Pose p = poseStack.last();
        for (int k = 0; k < 3; k++) {
            float tk = Mth.clamp(t * 1.3f - k * .15f, 0, 1);
            EchoMesh.groundRipple(ripple, p, 0, .05f + k * .01f, 0, .5f + radius * tk, k, BossMesh.glow(k == 1 ? EchoMesh.PALE : EchoMesh.ECHO, 1 - tk), light);
            EchoMesh.ripple(ripple, p, new Vector3f(0, 1, 0), x, new Vector3f(0, 1, 0), .5f + radius * .7f * tk, BossMesh.glow(EchoMesh.ECHO, .5f * (1 - tk)), light);
            EchoMesh.ripple(ripple, p, new Vector3f(0, 1, 0), z, new Vector3f(0, 1, 0), .5f + radius * .7f * tk, BossMesh.glow(EchoMesh.ECHO, .5f * (1 - tk)), light);
        }
        // A column of light, and shards of sound
        VertexConsumer beam = BossMesh.light(buffer, BossMesh.BEAM);
        p = poseStack.last();
        RitualMesh.shaft(beam, p, view.eye(), new Vector3f(0, 0, 0), new Vector3f(0, 2 + 4 * t, 0), 1.4f * fade, BossMesh.glow(EchoMesh.ECHO, fade), light);
        RitualMesh.shaft(beam, p, view.eye(), new Vector3f(0, 0, 0), new Vector3f(0, 2 + 4 * t, 0), .5f * fade, BossMesh.glow(EchoMesh.PALE, fade), light);
        for (int k = 0; k < 12; k++) {
            float a = BossMesh.hash(seed + k * 7) * Mth.TWO_PI, up = BossMesh.hash(seed + k * 11) * 1.4f - .2f;
            Vector3f d = new Vector3f(Mth.cos(a), up, Mth.sin(a)).normalize();
            Vector3f head = new Vector3f(d).mul(radius * 1.1f * (float) Math.sqrt(t)).add(0, 1, 0);
            RitualMesh.shaft(beam, p, view.eye(), new Vector3f(head).sub(new Vector3f(d).mul(.6f)), head, .12f, BossMesh.glow(EchoMesh.PALE, fade), light);
        }
        VertexConsumer glow = BossMesh.light(buffer, BossMesh.GLOW);
        RitualMesh.billboard(glow, poseStack, view, new Vector3f(0, 1, 0), 2 + radius * t, 0, -1, BossMesh.glow(EchoMesh.PALE, fade), light);
    }
}
