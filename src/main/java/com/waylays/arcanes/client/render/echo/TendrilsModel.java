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
 * Sculk Tendrils: sculk spreads over the ground from the spot, its veins pulsing with light, and tendrils of it burst
 * up, writhing, their tips glowing; they lash down at whatever stands there, then wither back into the ground.
 */
public final class TendrilsModel {
    private static final int COUNT = 9;

    private TendrilsModel() {}

    /** On the ground in the middle of the patch (the pose's origin), {@code radius} across; {@code age} of {@code life} ticks. */
    public static void draw(PoseStack poseStack, MultiBufferSource buffer, WaterMesh.View view, float radius, float age, float life, int seed, int light) {
        float spread = BossMesh.ramp(age, 0, 10);
        float wither = BossMesh.ramp(age, life - 12, life);
        float pulse = .6f + .4f * Mth.sin(age * .35f);
        EchoMesh.patch(poseStack, buffer, 0, .03f, 0, radius * 2.3f * (.3f + .7f * spread), seed * .1f, 1 - wither, pulse, light);
        VertexConsumer skin = BossMesh.see(buffer, BossMesh.SPIKE);
        PoseStack.Pose p = poseStack.last();
        Vector3f[] tips = new Vector3f[COUNT];
        for (int k = 0; k < COUNT; k++) {
            float a = k * Mth.TWO_PI / COUNT + BossMesh.hash(seed + k) * .5f;
            float r = radius * (.25f + .65f * BossMesh.hash(seed + k * 3));
            float len = 1.6f + 1.4f * BossMesh.hash(seed + k * 5);
            float rise = BossMesh.ramp(age - k * .8f, 2, 10);
            float y = -len * (1 - rise) - len * wither;
            Vector3f base = new Vector3f(Mth.cos(a) * r, y, Mth.sin(a) * r);
            // Writhing: their bend swings round, and every so often they lash down toward the middle
            float lash = Math.max(0, Mth.sin(age * .25f + k * 1.7f));
            float sw = age * .15f + k;
            Vector3f bend = new Vector3f(Mth.cos(sw) * .45f, -lash * .9f, Mth.sin(sw) * .45f).add(new Vector3f(-Mth.cos(a), 0, -Mth.sin(a)).mul(.5f * lash));
            EchoMesh.tendril(skin, p, base, new Vector3f(0, 1, 0), bend, len, .16f + .06f * BossMesh.hash(seed + k * 7), 1, light);
            tips[k] = new Vector3f(0, len, 0).add(new Vector3f(bend).mul(len)).add(base);
        }
        VertexConsumer glow = BossMesh.light(buffer, BossMesh.GLOW);
        for (Vector3f tip : tips) {
            if (tip.y > .1f) {
                RitualMesh.billboard(glow, poseStack, view, tip, .6f, 0, -1, BossMesh.glow(EchoMesh.ECHO, .9f * (1 - wither)), light);
            }
        }
        EchoMesh.souls(poseStack, buffer, view, radius, 2.5f, age, 10, 1 - wither, seed, light);
    }
}
