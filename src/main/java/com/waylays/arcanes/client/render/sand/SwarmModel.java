package com.waylays.arcanes.client.render.sand;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.waylays.arcanes.client.render.boss.BossMesh;
import com.waylays.arcanes.client.render.water.WaterMesh;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Scarab Swarm: golden scarabs, each with its shell of blue-green shining to gold, its dark head, its legs tucked in
 * and its wings of glass a blur, whirling round each other in a swarm; at each bite they dive at their prey together.
 * A haze of golden dust hangs round them.
 */
public final class SwarmModel {
    private static final int COUNT = 16;

    private SwarmModel() {}

    /**
     * At the swarm's middle (the pose's origin); {@code age} of {@code life} ticks; {@code sinceBite} ticks since it last
     * bit, at {@code target} (in the same frame, or null).
     */
    public static void draw(PoseStack poseStack, MultiBufferSource buffer, WaterMesh.View view, float age, float life, float sinceBite, Vector3f target,
                            int seed, int light) {
        float grow = BossMesh.ramp(age, 0, 8);
        float fade = 1 - BossMesh.ramp(age, life - 10, life);
        float dive = target != null && sinceBite < 8 ? 1 - sinceBite / 8 : 0;
        Vector3f[] at = new Vector3f[COUNT];
        Vector3f[] head = new Vector3f[COUNT];
        for (int k = 0; k < COUNT; k++) {
            float h1 = BossMesh.hash(seed + k), h2 = BossMesh.hash(seed + k * 3), h3 = BossMesh.hash(seed + k * 7);
            float ph = age * .22f * (1.4f + h1) + h2 * Mth.TWO_PI;
            float r = (.35f + .55f * h3) * grow;
            Vector3f n = new Vector3f(h1 - .5f, 1.4f, h2 - .5f).normalize();
            Vector3f u = BossMesh.across(n), v = new Vector3f(n).cross(u).normalize();
            Vector3f pos = new Vector3f(u).mul(Mth.cos(ph) * r).add(new Vector3f(v).mul(Mth.sin(ph) * r)).add(0, .15f * Mth.sin(ph * 2 + k), 0);
            Vector3f dir = new Vector3f(u).mul(-Mth.sin(ph)).add(new Vector3f(v).mul(Mth.cos(ph)));
            if (dive > 0) {
                Vector3f to = new Vector3f(target).add(new Vector3f(pos).mul(.4f));
                float k2 = dive * (k % 2 == 0 ? .8f : .45f);
                dir.lerp(new Vector3f(to).sub(pos).normalize(), k2);
                pos.lerp(to, k2);
            }
            at[k] = pos;
            head[k] = dir.lengthSquared() < 1.0E-6f ? new Vector3f(0, 0, 1) : dir.normalize();
        }
        float size = grow * fade;
        if (size < .02f) {
            return;
        }
        // Shells and heads
        VertexConsumer shell = BossMesh.see(buffer, SandMesh.SCARAB);
        for (int k = 0; k < COUNT; k++) {
            poseStack.pushPose();
            poseStack.translate(at[k].x, at[k].y, at[k].z);
            poseStack.mulPose(new Quaternionf().rotationTo(new Vector3f(0, 0, 1), head[k]));
            poseStack.scale(size, size, size);
            poseStack.pushPose();
            poseStack.scale(.12f, .075f, .17f);
            BossMesh.sphere(shell, poseStack.last(), new Vector3f(), 1, 6, 10, .25f, 0, 1, 0xFFFFFFFF, light);
            poseStack.popPose();
            poseStack.pushPose();
            poseStack.translate(0, -.005f, .17f);
            poseStack.scale(.075f, .05f, .06f);
            BossMesh.sphere(shell, poseStack.last(), new Vector3f(), 1, 4, 8, 0, .2f, 1, 0xFF406070, light);
            poseStack.popPose();
            poseStack.popPose();
        }
        // Legs
        VertexConsumer leg = BossMesh.solid(buffer, BossMesh.SPIKE);
        for (int k = 0; k < COUNT; k++) {
            poseStack.pushPose();
            poseStack.translate(at[k].x, at[k].y, at[k].z);
            poseStack.mulPose(new Quaternionf().rotationTo(new Vector3f(0, 0, 1), head[k]));
            poseStack.scale(size, size, size);
            PoseStack.Pose p = poseStack.last();
            for (int side = -1; side <= 1; side += 2) {
                for (int j = 0; j < 3; j++) {
                    Vector3f base = new Vector3f(side * .07f, -.04f, .08f - j * .08f);
                    Vector3f dir = new Vector3f(side, -.6f, .3f - j * .3f).normalize();
                    BossMesh.horn(leg, p, base, dir, new Vector3f(0, -.3f, 0), .13f, .012f, 3, 2, 0xFF141008, 0xFF2A2010, light);
                }
            }
            poseStack.popPose();
        }
        // Wings of glass, a blur
        VertexConsumer wing = BossMesh.see(buffer, SandMesh.GLASS);
        for (int k = 0; k < COUNT; k++) {
            poseStack.pushPose();
            poseStack.translate(at[k].x, at[k].y, at[k].z);
            poseStack.mulPose(new Quaternionf().rotationTo(new Vector3f(0, 0, 1), head[k]));
            poseStack.scale(size, size, size);
            PoseStack.Pose p = poseStack.last();
            float flap = .5f + .5f * Mth.sin(age * 4.5f + k * 1.7f);
            for (int side = -1; side <= 1; side += 2) {
                float a = (.3f + .9f * flap) * side;
                Vector3f root = new Vector3f(side * .03f, .06f, .05f);
                Vector3f out = new Vector3f(side * Mth.cos(a), Math.abs(Mth.sin(a)), 0).mul(.28f);
                Vector3f back = new Vector3f(0, 0, -.22f);
                BossMesh.quad2(wing, p, root, new Vector3f(root).add(out), new Vector3f(root).add(out).add(back), new Vector3f(root).add(back), 0, 0, 1, 0, 1, 1, 0, 1,
                        BossMesh.argb(SandMesh.GLASS_TINT, .45f), BossMesh.argb(SandMesh.GLASS_TINT, .25f), light);
            }
            poseStack.popPose();
        }
        // A haze of golden dust round them, and glints
        VertexConsumer dust = BossMesh.see(buffer, BossMesh.DUST);
        for (int k = 0; k < 5; k++) {
            float a = k * Mth.TWO_PI / 5 + age * .05f;
            SandMesh.cloud(dust, poseStack, view, new Vector3f(Mth.cos(a) * .5f, .1f * Mth.sin(age * .2f + k), Mth.sin(a) * .5f), 1.3f, a, SandMesh.GOLD, .18f * size,
                    light);
        }
        SandMesh.grains(poseStack, buffer, view, 1, 1.2f, age, 10, size, seed, light);
    }
}
