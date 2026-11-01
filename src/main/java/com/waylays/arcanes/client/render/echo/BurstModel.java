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
 * Resonance bursting in a creature: rings of sound flying apart in every direction from it, shards of light spinning
 * away, a flash; the more resonance it held, the bigger.
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
        VertexConsumer wave = BossMesh.light(buffer, EchoMesh.WAVE);
        PoseStack.Pose p = poseStack.last();
        for (int k = 0; k < 5; k++) {
            float a = BossMesh.hash(seed + k) * Mth.PI, b = BossMesh.hash(seed + k * 3) * Mth.TWO_PI;
            Vector3f n = new Vector3f(Mth.sin(a) * Mth.cos(b), Mth.cos(a), Mth.sin(a) * Mth.sin(b));
            Vector3f[] f = EchoMesh.frame(n);
            float tk = Mth.clamp(t * 1.4f - k * .08f, 0, 1);
            EchoMesh.ring(wave, p, new Vector3f(), f[0], f[1], .3f + size * 2 * tk, BossMesh.glow(k % 2 == 0 ? EchoMesh.PALE : EchoMesh.ECHO, 1 - tk), light);
        }
        VertexConsumer shard = BossMesh.light(buffer, BossMesh.BEAM);
        p = poseStack.last();
        for (int k = 0; k < 10; k++) {
            float a = BossMesh.hash(seed + k * 7) * Mth.TWO_PI, up = BossMesh.hash(seed + k * 11) * 2 - .5f;
            Vector3f d = new Vector3f(Mth.cos(a), up, Mth.sin(a)).normalize();
            Vector3f head = new Vector3f(d).mul(size * 2.4f * (float) Math.sqrt(t));
            RitualMesh.shaft(shard, p, view.eye(), new Vector3f(head).sub(new Vector3f(d).mul(.5f)), head, .12f, BossMesh.glow(EchoMesh.PALE, fade), light);
        }
        VertexConsumer glow = BossMesh.light(buffer, BossMesh.GLOW);
        RitualMesh.billboard(glow, poseStack, view, new Vector3f(), size * 2.5f * (1 - t * .5f), 0, -1, BossMesh.glow(EchoMesh.ECHO, fade), light);
        RitualMesh.billboard(glow, poseStack, view, new Vector3f(), size * (1 - t), 0, -1, BossMesh.glow(0xFFFFFF, fade), light);
    }
}
