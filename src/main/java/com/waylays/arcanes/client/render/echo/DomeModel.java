package com.waylays.arcanes.client.render.echo;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.waylays.arcanes.client.render.boss.BossMesh;
import com.waylays.arcanes.client.render.water.WaterMesh;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

/**
 * The Dome of Silence: a hush made visible, a dome of faint rings of sound stacked from the ground to its crown, slow
 * waves of light sliding down it, sculk spread over the ground inside, its veins glowing; every sound that reaches it
 * dies on it.
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
        EchoMesh.souls(poseStack, buffer, view, radius * .8f, radius * .9f, age, 18, k, seed, light);
    }
}
