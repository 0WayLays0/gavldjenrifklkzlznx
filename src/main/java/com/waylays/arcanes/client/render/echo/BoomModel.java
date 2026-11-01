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
 * A Sonic Boom: a beam of sound as the warden looses it, a string of rings racing out one after another along its line,
 * a bright core through them, each ring flaring then fading where it passed.
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
        VertexConsumer wave = BossMesh.light(buffer, EchoMesh.WAVE);
        PoseStack.Pose p = poseStack.last();
        for (float d = .8f; d < reach; d += 1.4f) {
            float passed = (reach - d) / SPEED;
            float k = Mth.clamp(1 - passed / 8, 0, 1) * fade;
            float r = width * (.6f + .5f * (1 - k));
            EchoMesh.ring(wave, p, new Vector3f(forward).mul(d), f[0], f[1], r, BossMesh.glow(((int) (d / 1.4f)) % 2 == 0 ? EchoMesh.PALE : EchoMesh.ECHO, k), light);
        }
        VertexConsumer beam = BossMesh.light(buffer, BossMesh.BEAM);
        Vector3f end = new Vector3f(forward).mul(reach);
        RitualMesh.shaft(beam, poseStack.last(), view.eye(), new Vector3f(), end, width * .9f, BossMesh.glow(EchoMesh.ECHO, .6f * fade), light);
        RitualMesh.shaft(beam, poseStack.last(), view.eye(), new Vector3f(), end, width * .3f, BossMesh.glow(EchoMesh.PALE, .8f * fade), light);
        VertexConsumer glow = BossMesh.light(buffer, BossMesh.GLOW);
        RitualMesh.billboard(glow, poseStack, view, new Vector3f(), width * 3, 0, -1, BossMesh.glow(EchoMesh.ECHO, fade), light);
        RitualMesh.billboard(glow, poseStack, view, end, width * 2.5f, 0, -1, BossMesh.glow(EchoMesh.PALE, fade * (reach < length ? 1 : .6f)), light);
    }
}
