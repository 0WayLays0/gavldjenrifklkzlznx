package com.waylays.arcanes.client.render.echo;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.waylays.arcanes.client.render.boss.BossMesh;
import com.waylays.arcanes.client.render.ritual.RitualMesh;
import com.waylays.arcanes.client.render.water.WaterMesh;
import net.minecraft.util.Mth;
import net.minecraft.client.renderer.MultiBufferSource;
import org.joml.Vector3f;

/**
 * Shriek: a cone of sound bursting from the caster's mouth: ripples racing out and widening along it, arcs of its
 * edge, rays of sound shooting out inside it, a flash at the mouth.
 */
public final class ShriekModel {
    /** How long it lasts (ticks). */
    public static final int LIFE = 16;
    private static final int RINGS = 7, RAYS = 14;

    private ShriekModel() {}

    /** Drawn from the mouth (the pose's origin), out along {@code forward}, {@code length} long, {@code half} radians wide. */
    public static void draw(PoseStack poseStack, MultiBufferSource buffer, WaterMesh.View view, Vector3f forward, float length, float half, float age,
                            int seed, int light) {
        Vector3f[] f = EchoMesh.frame(forward);
        float fade = 1 - BossMesh.ramp(age, LIFE - 6, LIFE);
        // Ripples racing out along the cone, widening with it
        VertexConsumer ripple = BossMesh.light(buffer, EchoMesh.RIPPLE);
        PoseStack.Pose p = poseStack.last();
        for (int k = 0; k < RINGS; k++) {
            float t = (age - k * 1.3f) / 8;
            if (t <= 0 || t >= 1) {
                continue;
            }
            float d = length * (float) Math.sqrt(t);
            float r = Math.max(.3f, d * Mth.sin(half));
            EchoMesh.ripple(ripple, p, new Vector3f(forward).mul(d * Mth.cos(half)), f[0], f[1], r,
                    BossMesh.glow(k % 2 == 0 ? EchoMesh.PALE : EchoMesh.ECHO, (1 - t) * fade), light);
        }
        // The leading front of each burst, bulging
        VertexConsumer wave = BossMesh.light(buffer, EchoMesh.WAVE);
        p = poseStack.last();
        for (int k = 0; k < 3; k++) {
            float t = (age - k * 3) / 9;
            if (t <= 0 || t >= 1) {
                continue;
            }
            float d = length * (float) Math.sqrt(t);
            EchoMesh.front(wave, p, new Vector3f(forward).mul(d * Mth.cos(half)), forward, f[0], f[1], Math.max(.4f, d * Mth.sin(half)), d * .25f, 3,
                    (1 - t) * fade, light);
        }
        // Arcs of its edge
        VertexConsumer arc = BossMesh.light(buffer, EchoMesh.ARC);
        p = poseStack.last();
        for (int k = 0; k < 8; k++) {
            float t = Mth.clamp((age - k) / 10, 0, 1);
            if (t <= 0 || t >= 1) {
                continue;
            }
            float d = length * t;
            float a0 = BossMesh.hash(seed + k) * Mth.TWO_PI;
            EchoMesh.arc(arc, p, new Vector3f(forward).mul(d * .9f), f[0], f[1], d * Mth.sin(half) * .8f, a0, a0 + 1.1f, .35f,
                    BossMesh.glow(EchoMesh.ECHO, (1 - t) * fade), light);
        }
        // Rays of sound shooting out inside the cone
        VertexConsumer ray = BossMesh.light(buffer, BossMesh.BEAM);
        p = poseStack.last();
        for (int k = 0; k < RAYS; k++) {
            float born = BossMesh.hash(seed + k * 13) * 6;
            float t = (age - born) / 7;
            if (t <= 0 || t >= 1) {
                continue;
            }
            float a = BossMesh.hash(seed + k * 5) * Mth.TWO_PI, spread = half * Mth.sqrt(BossMesh.hash(seed + k * 9));
            Vector3f d = new Vector3f(forward).mul(Mth.cos(spread))
                    .add(new Vector3f(f[0]).mul(Mth.sin(spread) * Mth.cos(a))).add(new Vector3f(f[1]).mul(Mth.sin(spread) * Mth.sin(a)));
            float head = length * t, tail = Math.max(0, head - 1.6f);
            RitualMesh.shaft(ray, p, view.eye(), new Vector3f(d).mul(tail), new Vector3f(d).mul(head), .1f,
                    BossMesh.glow(k % 3 == 0 ? EchoMesh.PALE : EchoMesh.ECHO, (1 - t) * fade), light);
        }
        // The flash at the mouth
        float mouth = 1 - BossMesh.ramp(age, 0, 8);
        if (mouth > 0) {
            VertexConsumer glow = BossMesh.light(buffer, BossMesh.GLOW);
            RitualMesh.billboard(glow, poseStack, view, new Vector3f(forward).mul(.4f), 2.4f * mouth + .5f, 0, -1, BossMesh.glow(EchoMesh.ECHO, mouth), light);
            RitualMesh.billboard(glow, poseStack, view, new Vector3f(forward).mul(.4f), .9f, 0, -1, BossMesh.glow(EchoMesh.PALE, mouth), light);
        }
    }
}
