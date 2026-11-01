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
 * A Sound Wave in flight: a bright heart of sound behind a curved front of rings bulging forward like a shell, arcs of
 * it turning, ripples left behind one after another, each wider and fainter than the last, and motes of the deep dark
 * spiralling in its wake.
 */
public final class WaveModel {
    private static final int RIPPLES = 6, MOTES = 10;

    private WaveModel() {}

    /** Drawn at the wave (the pose's origin), flying along {@code forward}; {@code size} how wide its front is. */
    public static void draw(PoseStack poseStack, MultiBufferSource buffer, WaterMesh.View view, Vector3f forward, float age, float size, float fade, int seed,
                            int light) {
        Vector3f[] f = EchoMesh.frame(forward);
        // The wake: ripples born at the head and left behind, widening as they go
        VertexConsumer ripple = BossMesh.light(buffer, EchoMesh.RIPPLE);
        PoseStack.Pose p = poseStack.last();
        for (int k = 0; k < RIPPLES; k++) {
            float t = ((age * .35f + k / (float) RIPPLES) % 1);
            Vector3f at = new Vector3f(forward).mul(-.4f - t * 3.4f);
            EchoMesh.ripple(ripple, p, at, f[0], f[1], size * (.45f + 1.0f * t), BossMesh.glow(k % 2 == 0 ? EchoMesh.PALE : EchoMesh.ECHO, (1 - t) * .9f * fade),
                    light);
        }
        // The front: a shell of rings bulging forward, and a second fainter one just behind it, pulsing
        VertexConsumer wave = BossMesh.light(buffer, EchoMesh.WAVE);
        p = poseStack.last();
        float pulse = .85f + .15f * Mth.sin(age * 1.3f);
        EchoMesh.front(wave, p, new Vector3f(forward).mul(-.1f), forward, f[0], f[1], size * .75f * pulse, size * .45f, 4, fade, light);
        EchoMesh.front(wave, p, new Vector3f(forward).mul(-.55f), forward, f[0], f[1], size * .95f, size * .35f, 3, .55f * fade, light);
        // Arcs of its front, turning
        VertexConsumer arc = BossMesh.light(buffer, EchoMesh.ARC);
        p = poseStack.last();
        for (int k = 0; k < 4; k++) {
            float a0 = age * (k % 2 == 0 ? .25f : -.2f) + k * Mth.TWO_PI / 4;
            float r = size * (k % 2 == 0 ? .62f : .85f);
            EchoMesh.arc(arc, p, new Vector3f(forward).mul(k % 2 == 0 ? .15f : -.3f), f[0], f[1], r, a0, a0 + 1.2f, .2f, BossMesh.glow(EchoMesh.ECHO, .9f * fade),
                    light);
        }
        // Motes spiralling in its wake
        VertexConsumer motes = BossMesh.light(buffer, BossMesh.MOTE);
        for (int k = 0; k < MOTES; k++) {
            float t = (age * .08f + k / (float) MOTES) % 1;
            float a = t * 9 + k * 2.4f + age * .3f;
            float r = size * (.35f + .6f * t);
            Vector3f at = new Vector3f(forward).mul(-t * 3.6f).add(new Vector3f(f[0]).mul(Mth.cos(a) * r)).add(new Vector3f(f[1]).mul(Mth.sin(a) * r));
            RitualMesh.billboard(motes, poseStack, view, at, .22f * (1 - t * .5f), 0, -1, BossMesh.glow(k % 3 == 0 ? EchoMesh.PALE : EchoMesh.ECHO, (1 - t) * fade),
                    light);
        }
        VertexConsumer glow = BossMesh.light(buffer, BossMesh.GLOW);
        RitualMesh.billboard(glow, poseStack, view, new Vector3f(), size * 1.6f, 0, -1, BossMesh.glow(EchoMesh.ECHO, .6f * fade), light);
        RitualMesh.billboard(glow, poseStack, view, new Vector3f(forward).mul(.2f), size * .6f, 0, -1, BossMesh.glow(EchoMesh.PALE, fade), light);
    }
}
