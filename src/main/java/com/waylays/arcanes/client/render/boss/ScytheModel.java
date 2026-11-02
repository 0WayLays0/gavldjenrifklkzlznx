package com.waylays.arcanes.client.render.boss;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.waylays.arcanes.client.render.ritual.RitualMesh;
import com.waylays.arcanes.client.render.sand.SandMesh;
import com.waylays.arcanes.client.render.water.WaterMesh;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Reaper's Scythe: a giant spectral scythe rises behind the caster, its long dark haft wound with violet light, its
 * blade a great crescent of ghostly steel; it reaps round them twice, half a turn in front, then half a turn behind, a
 * sheet of violet light trailing its edge.
 */
public final class ScytheModel {
    /** Ticks each half-turn takes. */
    private static final float REAP = 5;

    private ScytheModel() {}

    /** The blade's angle (radians, from the caster's facing round to its left) at {@code hit} ticks after the first reap. */
    private static float angle(float hit, int sweep) {
        if (hit < 0) {
            return Mth.PI;
        }
        if (hit < REAP) {
            return Mth.PI * .5f - Mth.PI * hit / REAP;
        }
        if (hit < sweep) {
            return -Mth.PI * .5f;
        }
        return -Mth.PI * .5f - Mth.PI * Math.min(1, (hit - sweep) / REAP);
    }

    /** Round the caster (the pose's origin), facing {@code yaw} (degrees); {@code hit} ticks since the first reap; reaching {@code radius}. */
    public static void draw(PoseStack poseStack, MultiBufferSource buffer, WaterMesh.View view, float age, float hit, float yaw, float radius, int sweep, int zone,
                            int seed, int light) {
        float appear = BossMesh.ramp(age, 0, 12);
        float gone = BossMesh.ramp(hit, sweep + REAP + 4, sweep + REAP + 16);
        float k0 = appear * (1 - gone);
        if (k0 < .01f) {
            return;
        }
        float face = -yaw * Mth.DEG_TO_RAD;
        float a = angle(hit, sweep);
        // Where the blade is, round the caster: forward is +Z turned by face
        float world = face + a;
        float reach = radius * .8f;
        float h = 2.6f + (hit < 0 ? 1.4f * (1 - appear) : 0);
        Vector3f dir = new Vector3f(Mth.sin(world), 0, Mth.cos(world));
        Vector3f tangent = new Vector3f(Mth.cos(world), 0, -Mth.sin(world));
        // The haft: from the caster out toward the blade
        Vector3f grip = new Vector3f(dir).mul(-.6f).add(0, h, 0);
        Vector3f head = new Vector3f(dir).mul(reach * .55f).add(0, h + .4f, 0);
        VertexConsumer iron = BossMesh.solid(buffer, BossMesh.IRON);
        SandMesh.tube(iron, poseStack.last(), new Vector3f[] {grip, new Vector3f(grip).lerp(head, .5f).add(0, .15f, 0), head}, new float[] {.2f, .17f, .22f}, 6, 1, 0,
                SandMesh.colours(0xFF2A2238, 3), light);
        // The blade: a crescent from the head, curving forward along the sweep
        VertexConsumer edge = BossMesh.light(buffer, BossMesh.EDGE);
        PoseStack.Pose p = poseStack.last();
        int n = 14;
        float sense = hit >= sweep ? 1 : -1;
        for (int i = 0; i < n; i++) {
            float t0 = (float) i / n, t1 = (float) (i + 1) / n;
            Vector3f o0 = blade(head, dir, tangent, reach, t0, sense), o1 = blade(head, dir, tangent, reach, t1, sense);
            float w0 = 1.9f * Mth.sin(Mth.PI * (.15f + .85f * t0)) * (1 - t0 * .7f), w1 = 1.9f * Mth.sin(Mth.PI * (.15f + .85f * t1)) * (1 - t1 * .7f);
            Vector3f i0 = new Vector3f(o0).add(0, -w0, 0), i1 = new Vector3f(o1).add(0, -w1, 0);
            BossMesh.quad2(edge, p, o0, o1, i1, i0, t0, 0, t1, 0, t1, 1, t0, 1, BossMesh.glow(0xE8D8FF, k0), BossMesh.glow(zone, .7f * k0), light);
        }
        // The sheet of light trailing its edge as it reaps
        float reaping = hit >= 0 && (hit < REAP + 3 || (hit >= sweep && hit < sweep + REAP + 3)) ? 1 : 0;
        if (reaping > 0) {
            float start = hit < sweep ? Mth.PI * .5f : -Mth.PI * .5f;
            float span = a - start;
            int m = 16;
            for (int i = 0; i < m; i++) {
                float b0 = face + start + span * i / m, b1 = face + start + span * (i + 1) / m;
                float f0 = (float) i / m, f1 = (float) (i + 1) / m;
                Vector3f out0 = new Vector3f(Mth.sin(b0), 0, Mth.cos(b0)).mul(radius), out1 = new Vector3f(Mth.sin(b1), 0, Mth.cos(b1)).mul(radius);
                Vector3f in0 = new Vector3f(out0).mul(.25f), in1 = new Vector3f(out1).mul(.25f);
                BossMesh.quad2(edge, p, new Vector3f(in0).add(0, h * .6f, 0), new Vector3f(out0).add(0, .4f, 0), new Vector3f(out1).add(0, .4f, 0),
                        new Vector3f(in1).add(0, h * .6f, 0), 0, 0, 1, 0, 1, 1, 0, 1, BossMesh.glow(zone, .6f * f0 * k0), BossMesh.glow(zone, .6f * f1 * k0), light);
            }
        }
        VertexConsumer glow = BossMesh.light(buffer, BossMesh.GLOW);
        RitualMesh.billboard(glow, poseStack, view, head, 4f, 0, -1, BossMesh.glow(zone, .6f * k0), light);
        RitualMesh.billboard(glow, poseStack, view, blade(head, dir, tangent, reach, .6f, sense), 5f, 0, -1, BossMesh.glow(zone, .35f * k0), light);
    }

    private static Vector3f blade(Vector3f head, Vector3f dir, Vector3f tangent, float reach, float t, float sense) {
        float out = .2f + .45f * t;
        float curl = Mth.sin(t * 1.4f) * reach * .85f;
        return new Vector3f(head).add(new Vector3f(dir).mul(out * reach * .4f)).add(new Vector3f(tangent).mul(sense * curl)).add(0, .3f - t * .5f, 0);
    }
}
