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
 * Requiem: the deep dark rises round its caster. Sculk floods the ground far around, its veins pulsing; a great heart of
 * sculk hangs over the caster, beating, its light quickening; with each beat a wall of sound races out over the whole
 * field, rings stacked high; the last beat bursts in light.
 */
public final class RequiemModel {
    /** How long a wall of sound takes to cross the field (ticks). */
    public static final int CROSS = 16;

    private RequiemModel() {}

    /**
     * Round the caster (the pose's origin), {@code radius} across; {@code age} of {@code life} ticks; the walls of sound
     * leave at ticks {@code first + k * step}, {@code count} of them.
     */
    public static void draw(PoseStack poseStack, MultiBufferSource buffer, WaterMesh.View view, float radius, float age, float life, float first, float step,
                            int count, int seed, int light) {
        float flood = BossMesh.ramp(age, 0, first);
        float end = BossMesh.ramp(age, life - 16, life);
        float beat = 0;
        for (int k = 0; k < count; k++) {
            float s = age - (first + k * step);
            if (s >= 0 && s < 8) {
                beat = Math.max(beat, 1 - s / 8);
            }
        }
        EchoMesh.patch(poseStack, buffer, 0, .03f, 0, radius * 2.4f * flood, seed, 1 - end, .5f + .5f * beat, light);
        // The heart, hanging over the caster, beating
        Vector3f heart = new Vector3f(0, 5.5f + .3f * Mth.sin(age * .05f), 0);
        float size = (1.1f + .35f * beat) * BossMesh.ramp(age, 0, 20) * (1 - end * .8f);
        VertexConsumer sculk = BossMesh.solid(buffer, BossMesh.SPIKE);
        BossMesh.sphere(sculk, poseStack.last(), heart, size, 10, 16, age * .003f, 0, 1, 0xFF1A5A66, light);
        VertexConsumer veins = BossMesh.light(buffer, EchoMesh.VEINS);
        BossMesh.sphere(veins, poseStack.last(), heart, size * 1.02f, 10, 16, age * .003f, 0, 1, BossMesh.glow(EchoMesh.ECHO, (.5f + .5f * beat) * (1 - end)), light);
        VertexConsumer glow = BossMesh.light(buffer, BossMesh.GLOW);
        RitualMesh.billboard(glow, poseStack, view, heart, size * (3 + 2 * beat), 0, -1, BossMesh.glow(EchoMesh.ECHO, (.4f + .5f * beat) * (1 - end)), light);
        // Tendrils hanging from it down to the ground
        VertexConsumer skin = BossMesh.see(buffer, BossMesh.SPIKE);
        PoseStack.Pose p = poseStack.last();
        for (int k = 0; k < 6; k++) {
            float a = k * Mth.TWO_PI / 6 + age * .01f;
            Vector3f foot = new Vector3f(Mth.cos(a) * 2.2f, -.4f, Mth.sin(a) * 2.2f);
            Vector3f d = new Vector3f(heart).sub(foot);
            float len = d.length() * flood * (1 - end);
            if (len > .1f) {
                EchoMesh.tendril(skin, p, foot, d.normalize(), new Vector3f(Mth.sin(age * .07f + k) * .2f, 0, Mth.cos(age * .07f + k) * .2f), len, .2f, 1, light);
            }
        }
        // The walls of sound racing out
        VertexConsumer wave = BossMesh.light(buffer, EchoMesh.WAVE);
        p = poseStack.last();
        Vector3f x = new Vector3f(1, 0, 0), z = new Vector3f(0, 0, 1);
        for (int k = 0; k < count; k++) {
            float s = age - (first + k * step);
            if (s < 0 || s > CROSS) {
                continue;
            }
            float t = s / CROSS;
            float r = 1 + radius * t;
            for (int j = 0; j < 5; j++) {
                float y = .2f + j * .7f * (1 - t * .5f);
                EchoMesh.ring(wave, p, new Vector3f(0, y, 0), x, z, r * (1 - j * .03f), BossMesh.glow(j % 2 == 0 ? EchoMesh.PALE : EchoMesh.ECHO, (1 - t) * (1 - j * .15f)),
                        light);
            }
        }
        // The last beat bursts in light
        float last = age - (first + (count - 1) * step);
        if (last >= 0 && last < 14) {
            float f = 1 - last / 14;
            VertexConsumer flash = BossMesh.light(buffer, BossMesh.GLOW);
            RitualMesh.billboard(flash, poseStack, view, heart, 6 + 14 * (1 - f), 0, -1, BossMesh.glow(EchoMesh.PALE, f), light);
        }
        EchoMesh.souls(poseStack, buffer, view, radius, 7, age, 30, 1 - end, seed, light);
    }
}
