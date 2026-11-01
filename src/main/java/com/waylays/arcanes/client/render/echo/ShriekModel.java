package com.waylays.arcanes.client.render.echo;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.waylays.arcanes.client.render.boss.BossMesh;
import com.waylays.arcanes.client.render.water.WaterMesh;
import net.minecraft.util.Mth;
import net.minecraft.client.renderer.MultiBufferSource;
import org.joml.Vector3f;

/**
 * A Shriek: rings of sound bursting from the caster's mouth and racing out over the cone ahead, each wider than the
 * last, cyan and pale by turns, fading as they reach its end; arcs of sound flickering between them.
 */
public final class ShriekModel {
    /** How long it lasts (ticks). */
    public static final int LIFE = 16;
    private static final int RINGS = 7;

    private ShriekModel() {}

    /** Drawn from the mouth (the pose's origin), out along {@code forward}, {@code length} long, {@code half} radians wide. */
    public static void draw(PoseStack poseStack, MultiBufferSource buffer, WaterMesh.View view, Vector3f forward, float length, float half, float age,
                            int seed, int light) {
        Vector3f[] f = EchoMesh.frame(forward);
        float fade = 1 - BossMesh.ramp(age, LIFE - 6, LIFE);
        VertexConsumer wave = BossMesh.light(buffer, EchoMesh.WAVE);
        PoseStack.Pose p = poseStack.last();
        for (int k = 0; k < RINGS; k++) {
            float born = k * 1.3f;
            float t = (age - born) / 8;
            if (t <= 0 || t >= 1) {
                continue;
            }
            float d = length * (float) Math.sqrt(t);
            float r = Math.max(.3f, d * Mth.sin(half));
            EchoMesh.ring(wave, p, new Vector3f(forward).mul(d * Mth.cos(half)), f[0], f[1], r,
                    BossMesh.glow(k % 2 == 0 ? EchoMesh.PALE : EchoMesh.ECHO, (1 - t) * fade), light);
        }
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
    }
}
