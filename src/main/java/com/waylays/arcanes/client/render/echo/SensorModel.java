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
 * A Sculk Sensor: a squat block of sculk on a patch of it, four feelers on top swaying, listening; when something moves
 * near, the feelers flare, a ring of sound jumps off it and a quivering line of light leaps to whatever moved.
 */
public final class SensorModel {
    /** How long a pulse shows (ticks). */
    public static final int PULSE = 10;

    private SensorModel() {}

    /**
     * At the sensor's foot (the pose's origin); {@code age} of {@code life} ticks; {@code sincePulse} ticks since it last
     * struck (large if never), at {@code target} (in the same frame, or null).
     */
    public static void draw(PoseStack poseStack, MultiBufferSource buffer, WaterMesh.View view, float age, float life, float sincePulse, Vector3f target,
                            int seed, int light) {
        float grow = BossMesh.ramp(age, 0, 8);
        float fade = 1 - BossMesh.ramp(age, life - 10, life);
        float flare = sincePulse < PULSE ? 1 - sincePulse / PULSE : 0;
        EchoMesh.patch(poseStack, buffer, 0, .03f, 0, 3.6f * grow, seed * .3f, fade, .5f + .5f * flare, light);
        float h = .55f * grow;
        VertexConsumer block = BossMesh.see(buffer, EchoMesh.SCULK);
        PoseStack.Pose p = poseStack.last();
        BossMesh.box(block, p, new Vector3f(0, h * .5f - (1 - grow) * .3f, 0), new Vector3f(.42f, 0, 0), new Vector3f(0, h * .5f, 0), new Vector3f(0, 0, .42f),
                BossMesh.argb(0xFFFFFF, fade), light);
        VertexConsumer glowBand = BossMesh.light(buffer, EchoMesh.VEINS);
        BossMesh.box(glowBand, poseStack.last(), new Vector3f(0, h * .55f, 0), new Vector3f(.43f, 0, 0), new Vector3f(0, h * .18f, 0), new Vector3f(0, 0, .43f),
                BossMesh.glow(EchoMesh.ECHO, (.4f + .6f * flare) * fade), light);
        VertexConsumer skin = BossMesh.see(buffer, BossMesh.SPIKE);
        p = poseStack.last();
        Vector3f[] tips = new Vector3f[4];
        for (int k = 0; k < 4; k++) {
            float a = k * Mth.HALF_PI + Mth.PI / 4;
            Vector3f base = new Vector3f(Mth.cos(a) * .25f, h, Mth.sin(a) * .25f);
            float sway = Mth.sin(age * .12f + k * 1.3f) * .2f + flare * .3f;
            Vector3f bend = new Vector3f(Mth.cos(a) * (.25f + sway), 0, Mth.sin(a) * (.25f + sway));
            float len = (.7f + .15f * (k % 2)) * grow;
            EchoMesh.tendril(skin, p, base, new Vector3f(0, 1, 0), bend, len, .07f, fade, light);
            tips[k] = new Vector3f(0, len, 0).add(new Vector3f(bend).mul(len)).add(base);
        }
        VertexConsumer glow = BossMesh.light(buffer, BossMesh.GLOW);
        for (Vector3f tip : tips) {
            RitualMesh.billboard(glow, poseStack, view, tip, .35f + .5f * flare, 0, -1, BossMesh.glow(EchoMesh.ECHO, (.6f + .4f * flare) * fade), light);
        }
        if (flare <= 0) {
            return;
        }
        // The pulse: a ring jumping off it, and the line of light to what it heard
        VertexConsumer wave = BossMesh.light(buffer, EchoMesh.WAVE);
        float t = 1 - flare;
        EchoMesh.groundRing(wave, poseStack.last(), 0, .08f, 0, .6f + 4 * t, BossMesh.glow(EchoMesh.PALE, flare), light);
        if (target != null) {
            Vector3f from = new Vector3f(0, h + .6f, 0);
            int n = 16;
            Vector3f[] pts = new Vector3f[n + 1];
            int[] col = new int[n + 1];
            Vector3f d = new Vector3f(target).sub(from);
            Vector3f[] f = EchoMesh.frame(new Vector3f(d).normalize());
            for (int i = 0; i <= n; i++) {
                float s = (float) i / n;
                float wig = Mth.sin(s * 18 + age * 2) * .18f * Mth.sin(s * Mth.PI);
                pts[i] = new Vector3f(from).lerp(target, s).add(0, Mth.sin(s * Mth.PI) * d.length() * .15f, 0).add(new Vector3f(f[0]).mul(wig));
                float head = Mth.clamp(1 - Math.abs(s - Math.min(1, t * 3)) * 4, 0, 1);
                col[i] = BossMesh.glow(head > .5f ? EchoMesh.PALE : EchoMesh.ECHO, flare * (.4f + .6f * head));
            }
            VertexConsumer line = BossMesh.light(buffer, BossMesh.BEAM);
            RitualMesh.thread(line, poseStack.last(), view.eye(), pts, .35f, col, light);
        }
    }
}
