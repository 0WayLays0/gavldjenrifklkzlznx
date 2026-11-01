package com.waylays.arcanes.client.render.boss;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.waylays.arcanes.client.render.ritual.RitualMesh;
import com.waylays.arcanes.client.render.water.WaterMesh;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

/**
 * The Pentacle of Thorns: along one stroke of the star, the ground splits in a line of red light and giant thorns burst
 * out of it one after another from its start to its end, dark at their roots and blood-red to their glowing points,
 * curving outward, in a spray of red mist. They stand a moment, then sink back into the earth.
 */
public final class ThornsModel {
    /** How long the thorns take to burst out from the start of the stroke to its end (ticks). */
    public static final int SWEEP = 6;
    /** How long they stand once all are out (ticks). */
    public static final int STAND = 22;
    /** How long they take to sink (ticks). */
    public static final int SINK = 10;
    private static final int RED = 0xFF3A48;

    private ThornsModel() {}

    /**
     * Drawn from the start of the stroke (the pose's origin, on the ground); {@code run} the stroke, {@code width} how
     * wide the band of thorns is; {@code age} ticks since they began to burst out (negative before: the split glows).
     */
    public static void draw(PoseStack poseStack, MultiBufferSource buffer, WaterMesh.View view, Vector3f run, float width, float age, int seed, int light) {
        float length = run.length();
        Vector3f fwd = new Vector3f(run.x, 0, run.z).normalize();
        Vector3f side = new Vector3f(-fwd.z, 0, fwd.x);
        float sink = BossMesh.ramp(age, SWEEP + STAND, SWEEP + STAND + SINK);
        float split = age < 0 ? Mth.clamp(1 + age / 20, 0, 1) : 1 - sink;

        // The split in the ground
        VertexConsumer crack = BossMesh.light(buffer, BossMesh.NOISE);
        PoseStack.Pose p = poseStack.last();
        int segs = Math.max(2, (int) (length / 1.2f));
        for (int i = 0; i < segs; i++) {
            float s0 = length * i / segs, s1 = length * (i + 1) / segs;
            float w0 = .35f + .25f * BossMesh.hash(seed + i), w1 = .35f + .25f * BossMesh.hash(seed + i + 1);
            float j0 = (BossMesh.hash(seed + i * 3) - .5f) * .5f, j1 = (BossMesh.hash(seed + i * 3 + 3) - .5f) * .5f;
            Vector3f c0 = new Vector3f(fwd).mul(s0).add(new Vector3f(side).mul(j0)).add(0, .05f, 0);
            Vector3f c1 = new Vector3f(fwd).mul(s1).add(new Vector3f(side).mul(j1)).add(0, .05f, 0);
            int col = BossMesh.glow(RED, .9f * split);
            BossMesh.faced(crack, p, new Vector3f(c0).add(new Vector3f(side).mul(w0)), new Vector3f(c0).sub(new Vector3f(side).mul(w0)),
                    new Vector3f(c1).sub(new Vector3f(side).mul(w1)), new Vector3f(c1).add(new Vector3f(side).mul(w1)),
                    new float[] {1, s0 * .3f - age * .02f, 0, s0 * .3f - age * .02f, 0, s1 * .3f - age * .02f, 1, s1 * .3f - age * .02f}, col, col, col, col,
                    light, BossMesh.UP);
        }
        if (age < 0) {
            return;
        }
        // The thorns
        int n = Math.max(3, (int) (length / .5f));
        VertexConsumer thorn = BossMesh.see(buffer, BossMesh.SPIKE);
        p = poseStack.last();
        Vector3f[] tips = new Vector3f[n];
        float[] shine = new float[n];
        for (int i = 0; i < n; i++) {
            float s = (i + .5f + (BossMesh.hash(seed + i * 5) - .5f) * .6f) / n * length;
            float out = Mth.clamp(age - s / length * SWEEP, 0, 99);
            if (out <= 0) {
                continue;
            }
            float f = BossMesh.ramp(out, 0, 3);
            float over = 1 + .18f * Mth.sin(Mth.PI * BossMesh.ramp(out, 1.5f, 6));
            float lat = (BossMesh.hash(seed + i * 7) - .5f) * width * .85f;
            float len = (1.3f + 2.4f * BossMesh.hash(seed + i * 11)) * (1 - .35f * Math.abs(lat) / Math.max(.1f, width)) * over;
            float y = -len * (1 - f) - len * 1.15f * sink;
            Vector3f base = new Vector3f(fwd).mul(s).add(new Vector3f(side).mul(lat)).add(0, y, 0);
            float lean = lat / Math.max(.1f, width) * 1.4f;
            Vector3f dir = new Vector3f(side).mul(lean).add(new Vector3f(fwd).mul((BossMesh.hash(seed + i * 13) - .5f) * .5f)).add(0, 1, 0).normalize();
            Vector3f bend = new Vector3f(side).mul(Math.signum(lat) * .22f);
            BossMesh.horn(thorn, p, base, dir, bend, len, len * .17f, 6, 5, BossMesh.argb(0x2A0408, 1), BossMesh.argb(0xE8303E, 1), light);
            tips[i] = new Vector3f(dir).mul(len).add(new Vector3f(bend).mul(len)).add(base);
            shine[i] = f * (1 - sink);
        }
        // Their points glowing, the red mist they burst out in
        VertexConsumer glow = BossMesh.light(buffer, BossMesh.GLOW);
        for (int i = 0; i < n; i++) {
            if (tips[i] != null && tips[i].y > .1f) {
                RitualMesh.billboard(glow, poseStack, view, tips[i], .7f, 0, -1, BossMesh.glow(0xFF6A70, .7f * shine[i]), light);
            }
        }
        VertexConsumer mist = BossMesh.see(buffer, BossMesh.SMOKE);
        for (int i = 0; i < n; i += 2) {
            float s = (i + .5f) / n * length;
            float since = age - s / length * SWEEP;
            if (since < 0 || since > 14) {
                continue;
            }
            float t = since / 14;
            WaterMesh.puff(mist, poseStack, view, fwd.x * s + side.x * (BossMesh.hash(seed + i) - .5f) * width, .4f + 1.6f * t, fwd.z * s
                    + side.z * (BossMesh.hash(seed + i) - .5f) * width, 1.2f + 2 * t, i + since * .05f, -1, BossMesh.argb(0xA01828, .6f * (1 - t)), light);
        }
    }
}
