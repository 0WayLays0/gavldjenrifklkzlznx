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
 * The Dome of Silence: a hush made visible, a dome of faint rings of sound stacked from the ground to its crown, slow
 * waves of light sliding down it, still ripples at its crown, sculk spread over the ground inside, its veins glowing,
 * tendrils of it holding the rim down; motes of sound fall in and the floor swallows them, ripples running in from the
 * rim to die in the middle.
 */
public final class DomeModel {
    private static final int LATITUDES = 7, MERIDIANS = 10;

    private DomeModel() {}

    /** On the ground at its middle (the pose's origin), {@code radius} across; {@code age} of {@code life} ticks. */
    public static void draw(PoseStack poseStack, MultiBufferSource buffer, WaterMesh.View view, float radius, float age, float life, int seed, int light) {
        float rise = BossMesh.ramp(age, 0, 14);
        float fade = 1 - BossMesh.ramp(age, life - 14, life);
        float k = rise * fade;
        EchoMesh.patch(poseStack, buffer, 0, .03f, 0, radius * 2.2f * rise, seed, fade, .6f + .2f * Mth.sin(age * .1f), light);
        // The shell, faint
        VertexConsumer shell = BossMesh.light(buffer, EchoMesh.VEINS);
        BossMesh.sphere(shell, poseStack.last(), new Vector3f(), radius * rise, 8, 24, age * .002f, 0, .5f, BossMesh.glow(EchoMesh.SCULK_TEAL, .5f * k), light);
        VertexConsumer wave = BossMesh.light(buffer, EchoMesh.WAVE);
        PoseStack.Pose p = poseStack.last();
        Vector3f x = new Vector3f(1, 0, 0), z = new Vector3f(0, 0, 1);
        for (int i = 0; i < LATITUDES; i++) {
            float lat = (i + .5f) / LATITUDES * Mth.HALF_PI;
            // A wave of light sliding down from the crown now and then
            float slide = (age * .02f) % 1;
            float bright = .35f + .65f * Mth.clamp(1 - Math.abs((1 - (float) i / LATITUDES) - slide) * 4, 0, 1);
            float y = Mth.sin(lat) * radius * rise, r = Mth.cos(lat) * radius * rise;
            EchoMesh.ring(wave, p, new Vector3f(0, y, 0), x, z, r, BossMesh.glow(i % 2 == 0 ? EchoMesh.ECHO : EchoMesh.PALE, .5f * bright * k), light);
        }
        VertexConsumer arc = BossMesh.light(buffer, EchoMesh.ARC);
        p = poseStack.last();
        for (int m = 0; m < MERIDIANS; m++) {
            float a = m * Mth.PI / MERIDIANS + age * .004f;
            Vector3f u = new Vector3f(Mth.cos(a), 0, Mth.sin(a));
            EchoMesh.arc(arc, p, new Vector3f(), u, new Vector3f(0, 1, 0), radius * rise, 0, Mth.PI, .2f, BossMesh.glow(EchoMesh.ECHO, .35f * k), light);
        }
        // Its crown: ripples turning, still
        VertexConsumer ripple = BossMesh.light(buffer, EchoMesh.RIPPLE);
        p = poseStack.last();
        EchoMesh.ripple(ripple, p, new Vector3f(0, radius * rise * .98f, 0), x, z, radius * .3f, BossMesh.glow(EchoMesh.PALE, .7f * k), light);
        EchoMesh.ripple(ripple, p, new Vector3f(0, radius * rise * .9f, 0), x, z, radius * .5f, BossMesh.glow(EchoMesh.ECHO, .4f * k), light);
        // At its rim, ripples running in toward the middle: sound swallowed
        float in = 1 - (age * .04f) % 1;
        EchoMesh.groundRipple(ripple, p, 0, .06f, 0, radius * rise * (.2f + .8f * in), age * .01f, BossMesh.glow(EchoMesh.ECHO, .45f * (1 - in) * k), light);
        // Tendrils of sculk holding its rim down
        VertexConsumer skin = BossMesh.see(buffer, BossMesh.SPIKE);
        p = poseStack.last();
        for (int m = 0; m < 12; m++) {
            float a = m * Mth.TWO_PI / 12 + BossMesh.hash(seed + m) * .3f;
            Vector3f foot = new Vector3f(Mth.cos(a) * radius * rise * .98f, 0, Mth.sin(a) * radius * rise * .98f);
            Vector3f up = new Vector3f(-Mth.cos(a) * .5f, 1, -Mth.sin(a) * .5f).normalize();
            float len = (1.1f + .6f * BossMesh.hash(seed + m * 3)) * rise * fade;
            float sw = age * .1f + m;
            if (len > .05f) {
                EchoMesh.tendril(skin, p, foot, up, new Vector3f(Mth.cos(sw) * .2f, -.25f, Mth.sin(sw) * .2f), len, .12f, 1, light);
            }
        }
        // Motes of sound falling in and swallowed by the floor
        VertexConsumer motes = BossMesh.light(buffer, BossMesh.MOTE);
        for (int m = 0; m < 16; m++) {
            float t = (age * .015f + BossMesh.hash(seed + m * 5)) % 1;
            float a = BossMesh.hash(seed + m * 7) * Mth.TWO_PI + t;
            float r = radius * rise * (.9f - .7f * t);
            Vector3f at = new Vector3f(Mth.cos(a) * r, radius * rise * .8f * (1 - t), Mth.sin(a) * r);
            RitualMesh.billboard(motes, poseStack, view, at, .25f, 0, -1, BossMesh.glow(m % 3 == 0 ? EchoMesh.PALE : EchoMesh.ECHO, Mth.sin(t * Mth.PI) * k), light);
        }
        EchoMesh.souls(poseStack, buffer, view, radius * .8f, radius * .9f, age, 18, k, seed, light);
    }
}
