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
 * Sculk Tendrils: sculk floods the ground where the caster aims, veins glowing, mounds of it swelling up; great tendrils
 * burst out of it, writhing and lashing down at the middle, their tips glowing bulbs, smaller ones swaying between
 * them; ripples of sound pulse out with each lash, motes of the deep dark rise.
 */
public final class TendrilsModel {
    private static final int COUNT = 9, SMALL = 14, MOUNDS = 6;

    private TendrilsModel() {}

    /** On the ground in the middle of the patch (the pose's origin), {@code radius} across; {@code age} of {@code life} ticks. */
    public static void draw(PoseStack poseStack, MultiBufferSource buffer, WaterMesh.View view, float radius, float age, float life, int seed, int light) {
        float spread = BossMesh.ramp(age, 0, 10);
        float wither = BossMesh.ramp(age, life - 12, life);
        float pulse = .6f + .4f * Mth.sin(age * .35f);
        float alive = 1 - wither;
        EchoMesh.patch(poseStack, buffer, 0, .03f, 0, radius * 2.3f * (.3f + .7f * spread), seed * .1f, alive, pulse, light);
        // Mounds of sculk swelling up out of the patch
        VertexConsumer sculk = BossMesh.see(buffer, EchoMesh.SCULK);
        PoseStack.Pose p = poseStack.last();
        Vector3f[] mounds = new Vector3f[MOUNDS];
        float[] mr = new float[MOUNDS];
        for (int k = 0; k < MOUNDS; k++) {
            float a = BossMesh.hash(seed + k * 17) * Mth.TWO_PI, r = radius * (.3f + .55f * BossMesh.hash(seed + k * 19));
            mr[k] = (.3f + .3f * BossMesh.hash(seed + k * 23)) * BossMesh.ramp(age, 2 + k, 12 + k) * alive;
            mounds[k] = new Vector3f(Mth.cos(a) * r, -mr[k] * .35f, Mth.sin(a) * r);
            if (mr[k] > .02f) {
                BossMesh.sphere(sculk, p, mounds[k], mr[k], 4, 10, k * .2f, 0, .5f, 0xFFFFFFFF, light);
            }
        }
        // The great tendrils, and the small ones between them
        VertexConsumer skin = BossMesh.see(buffer, BossMesh.SPIKE);
        p = poseStack.last();
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
            EchoMesh.tendril(skin, p, base, new Vector3f(0, 1, 0), bend, len, .17f + .07f * BossMesh.hash(seed + k * 7), 1, light);
            tips[k] = new Vector3f(0, len, 0).add(new Vector3f(bend).mul(len)).add(base);
        }
        for (int k = 0; k < SMALL; k++) {
            float a = BossMesh.hash(seed + k * 31) * Mth.TWO_PI, r = radius * (.15f + .8f * BossMesh.hash(seed + k * 37));
            float len = (.5f + .6f * BossMesh.hash(seed + k * 41)) * BossMesh.ramp(age - k * .5f, 3, 9) * alive;
            if (len < .05f) {
                continue;
            }
            float sw = age * .3f + k * 2.1f;
            Vector3f bend = new Vector3f(Mth.cos(sw) * .5f, 0, Mth.sin(sw) * .5f);
            EchoMesh.tendril(skin, p, new Vector3f(Mth.cos(a) * r, 0, Mth.sin(a) * r), new Vector3f(0, 1, 0), bend, len, .07f, 1, light);
        }
        // Their bulbs of light
        VertexConsumer bulb = BossMesh.light(buffer, EchoMesh.VEINS);
        p = poseStack.last();
        for (Vector3f tip : tips) {
            if (tip.y > .1f) {
                BossMesh.sphere(bulb, p, tip, .13f, 4, 8, 0, 0, 1, BossMesh.glow(EchoMesh.PALE, .9f * alive), light);
            }
        }
        // The veins of the mounds glowing
        for (int k = 0; k < MOUNDS; k++) {
            if (mr[k] > .02f) {
                BossMesh.sphere(bulb, p, mounds[k], mr[k] * 1.03f, 4, 10, k * .2f, 0, .5f, BossMesh.glow(EchoMesh.ECHO, .6f * pulse * alive), light);
            }
        }
        // A ripple out of the middle with each lash
        VertexConsumer ripple = BossMesh.light(buffer, EchoMesh.RIPPLE);
        p = poseStack.last();
        float t = (age % 10) / 10;
        EchoMesh.groundRipple(ripple, p, 0, .06f, 0, radius * (.3f + .9f * t), age * .02f, BossMesh.glow(EchoMesh.ECHO, .5f * (1 - t) * alive * spread), light);
        VertexConsumer glow = BossMesh.light(buffer, BossMesh.GLOW);
        for (Vector3f tip : tips) {
            if (tip.y > .1f) {
                RitualMesh.billboard(glow, poseStack, view, tip, .7f, 0, -1, BossMesh.glow(EchoMesh.ECHO, .9f * alive), light);
            }
        }
        EchoMesh.souls(poseStack, buffer, view, radius, 2.5f, age, 14, alive, seed, light);
    }
}
