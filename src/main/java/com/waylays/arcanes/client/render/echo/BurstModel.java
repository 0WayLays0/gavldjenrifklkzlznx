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
 * Resonant Burst on one creature: its resonance bursts out of it, a bright core swelling and fading, shells of ripples
 * flying apart every which way, needles of sound shooting out, a ripple racing over the ground under it.
 */
public final class BurstModel {
    /** How long it lasts (ticks). */
    public static final int LIFE = 14;

    private BurstModel() {}

    /** Round the creature's middle (the pose's origin); {@code power} how big (1 for one stack of resonance, up to 5). */
    public static void draw(PoseStack poseStack, MultiBufferSource buffer, WaterMesh.View view, float age, float power, int seed, int light) {
        float t = BossMesh.ramp(age, 0, LIFE);
        float fade = 1 - t;
        float size = .8f + .45f * power;
        // Shells of ripples flying apart, every which way
        VertexConsumer ripple = BossMesh.light(buffer, EchoMesh.RIPPLE);
        PoseStack.Pose p = poseStack.last();
        int shells = 4 + Math.round(power);
        for (int k = 0; k < shells; k++) {
            float a = BossMesh.hash(seed + k) * Mth.PI, b = BossMesh.hash(seed + k * 3) * Mth.TWO_PI;
            Vector3f n = new Vector3f(Mth.sin(a) * Mth.cos(b), Mth.cos(a), Mth.sin(a) * Mth.sin(b));
            Vector3f[] f = EchoMesh.frame(n);
            float tk = Mth.clamp(t * 1.4f - k * .06f, 0, 1);
            EchoMesh.ripple(ripple, p, new Vector3f(), f[0], f[1], .3f + size * 2 * tk, BossMesh.glow(k % 2 == 0 ? EchoMesh.PALE : EchoMesh.ECHO, 1 - tk), light);
        }
        // Under it, over the ground
        EchoMesh.groundRipple(ripple, p, 0, -.95f, 0, .5f + size * 2.6f * t, seed, BossMesh.glow(EchoMesh.ECHO, .8f * fade), light);
        // The rim of the first shell, sharp
        VertexConsumer wave = BossMesh.light(buffer, EchoMesh.WAVE);
        p = poseStack.last();
        for (int k = 0; k < 3; k++) {
            float a = k * Mth.PI / 3;
            Vector3f u = new Vector3f(Mth.cos(a), 0, Mth.sin(a));
            EchoMesh.ring(wave, p, new Vector3f(), u, new Vector3f(0, 1, 0), .3f + size * 1.6f * (float) Math.sqrt(t), BossMesh.glow(EchoMesh.PALE, .7f * fade), light);
        }
        // The core, swelling then fading
        VertexConsumer core = BossMesh.light(buffer, EchoMesh.VEINS);
        p = poseStack.last();
        float swell = Mth.sin(Math.min(1, t * 2) * Mth.PI);
        BossMesh.sphere(core, p, new Vector3f(), size * (.35f + .45f * swell), 6, 12, age * .05f, 0, 1, BossMesh.glow(EchoMesh.PALE, swell), light);
        // Needles of sound
        VertexConsumer shard = BossMesh.light(buffer, BossMesh.BEAM);
        p = poseStack.last();
        for (int k = 0; k < 14; k++) {
            float a = BossMesh.hash(seed + k * 7) * Mth.TWO_PI, up = BossMesh.hash(seed + k * 11) * 2 - .5f;
            Vector3f d = new Vector3f(Mth.cos(a), up, Mth.sin(a)).normalize();
            Vector3f head = new Vector3f(d).mul(size * 2.6f * (float) Math.sqrt(t));
            RitualMesh.shaft(shard, p, view.eye(), new Vector3f(head).sub(new Vector3f(d).mul(.7f)), head, .12f, BossMesh.glow(k % 3 == 0 ? EchoMesh.ECHO : EchoMesh.PALE, fade),
                    light);
        }
        VertexConsumer glow = BossMesh.light(buffer, BossMesh.GLOW);
        RitualMesh.billboard(glow, poseStack, view, new Vector3f(), size * 2.8f * (1 - t * .5f), 0, -1, BossMesh.glow(EchoMesh.ECHO, fade), light);
        RitualMesh.billboard(glow, poseStack, view, new Vector3f(), size * (1 - t), 0, -1, BossMesh.glow(0xFFFFFF, fade), light);
    }
}
