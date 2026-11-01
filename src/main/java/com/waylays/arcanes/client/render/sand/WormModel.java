package com.waylays.arcanes.client.render.sand;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.waylays.arcanes.client.render.boss.BossMesh;
import com.waylays.arcanes.client.render.ritual.RitualMesh;
import com.waylays.arcanes.client.render.water.WaterMesh;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

/**
 * The Sandworm: the ground trembles and cracks in a ring; then a vast worm bursts up out of it, its round maw ringed
 * with teeth gaping at the sky, arcs over and dives back into the sand ahead, its ridged body following it down, sand
 * erupting where it broke out and where it plunges back.
 */
public final class WormModel {
    /** When it bursts out (ticks after the ground starts to tremble), and how long its leap takes. */
    public static final int BURST = 20, LEAP = 30;
    private static final int TEETH = 12;
    private static final float LENGTH = 16;

    private WormModel() {}

    /** Where it bursts out (the pose's origin, on the ground), leaping along {@code forward}; {@code size} its girth. */
    public static void draw(PoseStack poseStack, MultiBufferSource buffer, WaterMesh.View view, Vector3f forward, float size, float age, int seed, int light) {
        Vector3f fwd = new Vector3f(forward.x, 0, forward.z).normalize();
        // The trembling ground before it
        if (age < BURST + 6) {
            float k = BossMesh.ramp(age, 0, BURST) * (1 - BossMesh.ramp(age, BURST, BURST + 6));
            VertexConsumer swirl = BossMesh.ground(buffer, SandMesh.SWIRL);
            BossMesh.pool(swirl, poseStack.last(), 0, .04f, 0, size * 4.5f * (.5f + .5f * k), age * .1f, BossMesh.argb(0xFFFFFF, .9f * k), light);
            VertexConsumer dust = BossMesh.see(buffer, BossMesh.DUST);
            for (int j = 0; j < 10; j++) {
                float a = j * Mth.TWO_PI / 10 + age * .05f;
                float hop = Math.abs(Mth.sin(age * .9f + j)) * .3f * k;
                SandMesh.cloud(dust, poseStack, view, new Vector3f(Mth.cos(a) * size * 1.8f, .2f + hop, Mth.sin(a) * size * 1.8f), 1.1f, a, SandMesh.SAND, .6f * k, light);
            }
        }
        float t = (age - BURST) / LEAP;
        if (t <= 0 || t >= 1.6f) {
            return;
        }
        // Its path: up out of the ground, over, and back down ahead; the head leads, the body follows LENGTH behind
        float span = size * 7;
        float head = t * (1 + LENGTH / (span * 1.6f));
        int n = 20;
        Vector3f[] axis = new Vector3f[n + 1];
        float[] rad = new float[n + 1];
        int[] col = new int[n + 1];
        int used = 0;
        for (int i = 0; i <= n; i++) {
            float s = head - (float) i / n * LENGTH / (span * 1.6f);
            if (s < -.05f) {
                break;
            }
            axis[used] = arc(fwd, span, Math.min(1.05f, Math.max(-.05f, s)), size);
            float taper = i < 2 ? .75f + .125f * i : 1 - .45f * (float) i / n;
            rad[used] = size * taper;
            col[used] = 0xFFFFFFFF;
            used++;
        }
        if (used >= 2) {
            Vector3f[] ax = java.util.Arrays.copyOf(axis, used);
            float[] rd = java.util.Arrays.copyOf(rad, used);
            int[] cl = java.util.Arrays.copyOf(col, used);
            VertexConsumer hide = BossMesh.see(buffer, SandMesh.WORM);
            WaterMesh.tube(hide, poseStack.last(), ax, rd, cl, 12, .06f, 2, 0, .35f, 0, seed, light, .3f);
            // The maw: a dark gullet ringed with teeth, gaping where it leads
            Vector3f mouth = ax[0];
            Vector3f d = new Vector3f(ax[0]).sub(ax[1]).normalize();
            Vector3f[] fr = com.waylays.arcanes.client.render.echo.EchoMesh.frame(d);
            VertexConsumer gullet = BossMesh.see(buffer, BossMesh.DISC);
            BossMesh.plate(gullet, poseStack.last(), new Vector3f(d).mul(.05f).add(mouth), fr[0], fr[1], rd[0] * 1.7f, BossMesh.argb(0x1A0C08, 1), light);
            VertexConsumer teeth = BossMesh.see(buffer, BossMesh.SPIKE);
            PoseStack.Pose p = poseStack.last();
            for (int k = 0; k < TEETH; k++) {
                float a = k * Mth.TWO_PI / TEETH;
                Vector3f out = new Vector3f(fr[0]).mul(Mth.cos(a)).add(new Vector3f(fr[1]).mul(Mth.sin(a)));
                Vector3f root = new Vector3f(out).mul(rd[0] * .8f).add(mouth);
                Vector3f dir = new Vector3f(d).add(new Vector3f(out).mul(-.6f)).normalize();
                BossMesh.horn(teeth, p, root, dir, new Vector3f(out).mul(-.2f), size * .55f, size * .12f, 5, 3, 0xFF8A6A50, 0xFFF2EAD6, light);
            }
        }
        // Sand erupting where it broke out, then where it dives back in
        float out = BossMesh.ramp(t, 0, .5f);
        if (out < 1) {
            SandMesh.spray(poseStack, buffer, view, size * 3, size * 3, out, 18, seed, light);
        }
        float dive = BossMesh.ramp(t, .6f, 1.2f);
        if (dive > 0 && dive < 1) {
            poseStack.pushPose();
            Vector3f end = new Vector3f(fwd).mul(span);
            poseStack.translate(end.x, end.y, end.z);
            WaterMesh.View at = new WaterMesh.View(view.camera(), new Vector3f(view.eye()).sub(end));
            SandMesh.spray(poseStack, buffer, at, size * 3, size * 2.5f, dive, 16, seed + 7, light);
            poseStack.popPose();
        }
    }

    /** A point of the leap, {@code s} 0 at the hole it bursts from to 1 where it dives back in: up, over, down. */
    private static Vector3f arc(Vector3f fwd, float span, float s, float size) {
        float height = span * .75f;
        return new Vector3f(fwd).mul(span * s).add(0, height * 4 * s * (1 - s) - size * 1.5f * (s < 0 || s > 1 ? 1 : 0), 0);
    }
}
