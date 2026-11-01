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
 * A sonic boom: an orb of sound swelling at where it is loosed, then rings and ripples racing out along its line one
 * after another, a beam of light through them with a thread of light winding round it, and a burst of ripples where
 * it ends.
 */
public final class BoomModel {
    /** How long it lasts (ticks). */
    public static final int LIFE = 16;
    /** How fast its rings race out (blocks a tick). */
    public static final float SPEED = 4;

    private BoomModel() {}

    /** From where it is loosed (the pose's origin), along {@code forward}, {@code length} long; {@code width} its rings' size. */
    public static void draw(PoseStack poseStack, MultiBufferSource buffer, WaterMesh.View view, Vector3f forward, float length, float width, float age, int light) {
        Vector3f[] f = EchoMesh.frame(forward);
        float reach = Math.min(length, age * SPEED);
        float fade = 1 - BossMesh.ramp(age, LIFE - 8, LIFE);
        // Rings and ripples by turns along its line
        VertexConsumer wave = BossMesh.light(buffer, EchoMesh.WAVE);
        PoseStack.Pose p = poseStack.last();
        int i = 0;
        for (float d = .8f; d < reach; d += 1.4f, i++) {
            if (i % 2 == 1) {
                continue;
            }
            float k = Mth.clamp(1 - (reach - d) / SPEED / 8, 0, 1) * fade;
            EchoMesh.ring(wave, p, new Vector3f(forward).mul(d), f[0], f[1], width * (.6f + .5f * (1 - k)), BossMesh.glow(EchoMesh.PALE, k), light);
        }
        VertexConsumer ripple = BossMesh.light(buffer, EchoMesh.RIPPLE);
        p = poseStack.last();
        i = 0;
        for (float d = .8f; d < reach; d += 1.4f, i++) {
            if (i % 2 == 0) {
                continue;
            }
            float k = Mth.clamp(1 - (reach - d) / SPEED / 8, 0, 1) * fade;
            EchoMesh.ripple(ripple, p, new Vector3f(forward).mul(d), f[0], f[1], width * (.7f + .7f * (1 - k)), BossMesh.glow(EchoMesh.ECHO, k), light);
        }
        // Where it ends: a burst of ripples
        Vector3f end = new Vector3f(forward).mul(reach);
        if (reach >= length) {
            float since = age - length / SPEED;
            float t = Mth.clamp(since / 8, 0, 1);
            EchoMesh.ripple(ripple, p, end, f[0], f[1], width * (1 + 3 * t), BossMesh.glow(EchoMesh.PALE, (1 - t) * fade), light);
        }
        // The beam, and a thread of light winding round it
        VertexConsumer beam = BossMesh.light(buffer, BossMesh.BEAM);
        p = poseStack.last();
        RitualMesh.shaft(beam, p, view.eye(), new Vector3f(), end, width * .9f, BossMesh.glow(EchoMesh.ECHO, .6f * fade), light);
        RitualMesh.shaft(beam, p, view.eye(), new Vector3f(), end, width * .3f, BossMesh.glow(EchoMesh.PALE, .8f * fade), light);
        int n = Math.max(2, (int) (reach * 4));
        if (reach > .5f) {
            Vector3f[] pts = new Vector3f[n + 1];
            int[] col = new int[n + 1];
            for (int j = 0; j <= n; j++) {
                float s = (float) j / n;
                float a = s * reach * 2.2f - age * .8f;
                pts[j] = new Vector3f(forward).mul(s * reach).add(new Vector3f(f[0]).mul(Mth.cos(a) * width * .55f)).add(new Vector3f(f[1]).mul(Mth.sin(a) * width * .55f));
                col[j] = BossMesh.glow(EchoMesh.PALE, .7f * fade * Math.min(1, s * 6));
            }
            RitualMesh.thread(beam, p, view.eye(), pts, .1f, col, light);
        }
        // The orb it was loosed from
        float orb = 1 - BossMesh.ramp(age, 2, 10);
        VertexConsumer core = BossMesh.light(buffer, EchoMesh.VEINS);
        BossMesh.sphere(core, poseStack.last(), new Vector3f(), width * (.4f + .3f * orb), 6, 12, age * .06f, 0, 1, BossMesh.glow(EchoMesh.PALE, orb * fade), light);
        VertexConsumer glow = BossMesh.light(buffer, BossMesh.GLOW);
        RitualMesh.billboard(glow, poseStack, view, new Vector3f(), width * (3 + 2 * orb), 0, -1, BossMesh.glow(EchoMesh.ECHO, fade), light);
        RitualMesh.billboard(glow, poseStack, view, end, width * 2.5f, 0, -1, BossMesh.glow(EchoMesh.PALE, fade * (reach < length ? 1 : .6f)), light);
    }
}
