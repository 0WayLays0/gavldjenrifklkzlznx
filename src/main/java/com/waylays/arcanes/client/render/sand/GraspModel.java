package com.waylays.arcanes.client.render.sand;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.waylays.arcanes.client.render.boss.BossMesh;
import com.waylays.arcanes.client.render.water.WaterMesh;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

/**
 * Grasp of the Dunes: the sand churns, then five great fingers of flowing sand burst up round the victim, knuckled and
 * tapering, curling in over it; the sand pours off them as they rise; at the end they clench tight, then crumble back
 * into the heap they rose from.
 */
public final class GraspModel {
    private GraspModel() {}

    /** At the victim's feet (the pose's origin); {@code age} of {@code life} ticks; it rises over {@code rise} ticks and closes {@code crush} ticks before its end. */
    public static void draw(PoseStack poseStack, MultiBufferSource buffer, WaterMesh.View view, float age, float life, float size, int riseTicks, int crushTicks,
                            int seed, int light) {
        float rise = BossMesh.ramp(age, 0, riseTicks);
        rise = 1 - (1 - rise) * (1 - rise);
        float close = BossMesh.ramp(age, life - crushTicks - 5, life - crushTicks);
        float crumble = BossMesh.ramp(age, life - crushTicks + 2, life);
        float alive = 1 - crumble;
        float reach = size * .75f;
        // The heap of churned sand they rise from
        VertexConsumer flow = BossMesh.see(buffer, SandMesh.FLOW);
        PoseStack.Pose p = poseStack.last();
        float heap = Math.min(1, age / 4f) * (1 - crumble * .5f);
        SandMesh.lathe(flow, p, new Vector3f(), new float[] {-.05f, .18f * heap, .42f * heap}, new float[] {reach + 1.1f, reach + .55f, reach * .55f}, 20, 3,
                age * .02f, BossMesh.argb(0xFFFFFF, .95f), light);
        // The fingers (and a thicker, shorter thumb), clenching at the end
        for (int k = 0; k < 5; k++) {
            boolean thumb = k == 4;
            // Four fingers fanned on one side, the thumb across from them: a hand cupped round its prey
            float a = thumb ? 2.5f : -1.55f + k * .72f + (BossMesh.hash(seed + k) - .5f) * .15f;
            Vector3f out = new Vector3f(Mth.cos(a), 0, Mth.sin(a));
            float tall = (thumb ? 2.0f : 2.7f + .3f * BossMesh.hash(seed + k * 3)) * rise;
            float sink = -1.6f * crumble;
            Vector3f base = new Vector3f(out).mul(reach + .25f).add(0, -.2f + sink, 0);
            Vector3f mid = new Vector3f(out).mul(reach + .7f).add(0, tall * .75f + sink, 0);
            float in = .35f - .3f * close;
            Vector3f tip = new Vector3f(out).mul(reach * in).add(0, tall * (1.05f - .25f * close) + sink, 0);
            Vector3f[] pts = SandMesh.curve(base, mid, tip, 12);
            float[] r = new float[pts.length];
            int[] col = new int[pts.length];
            float thick = (thumb ? .42f : .32f) * (.6f + .4f * rise) * (1 - .2f * close);
            for (int i = 0; i < pts.length; i++) {
                float t = (float) i / (pts.length - 1);
                float knuckle = 1 + .16f * (float) Math.exp(-Math.pow((t - .45f) / .07f, 2)) + .12f * (float) Math.exp(-Math.pow((t - .75f) / .06f, 2));
                r[i] = Math.max(.05f, thick * (1 - .35f * t) * knuckle);
                col[i] = BossMesh.argb(0xFFFFFF, alive);
            }
            SandMesh.tube(flow, p, pts, r, 9, .9f, -age * .12f, col, light);
            // Its rounded fingertip
            float tipR = r[r.length - 1] * 1.05f;
            SandMesh.ellipsoid(flow, poseStack, pts[pts.length - 1], tipR, tipR, tipR, 0, 0, BossMesh.argb(0xFFFFFF, alive), light);
            // Sand pouring off its knuckle
            Vector3f from = pts[5];
            Vector3f[] fall = {new Vector3f(from), new Vector3f(from).add(new Vector3f(out).mul(.15f)).add(0, -from.y * .5f, 0), new Vector3f(from).add(new Vector3f(out).mul(.25f)).sub(0, from.y + .1f, 0)};
            SandMesh.ribbon(flow, p, view.eye(), fall, new float[] {.16f, .1f, .05f}, 2, age * .5f,
                    new int[] {BossMesh.argb(SandMesh.SAND_PALE, .8f * alive * rise), BossMesh.argb(SandMesh.SAND, .6f * alive * rise), BossMesh.argb(SandMesh.SAND, 0)}, light);
        }
        // Dust thrown up as they burst out, and as they crumble
        VertexConsumer dust = BossMesh.see(buffer, BossMesh.DUST);
        float burst = Math.max(1 - BossMesh.ramp(age, 2, riseTicks + 8), crumble * (1 - crumble) * 4);
        for (int k = 0; k < 10; k++) {
            float a = k * Mth.TWO_PI / 10 + BossMesh.hash(seed + k * 5);
            float r = reach + .4f + 1.2f * BossMesh.hash(seed + k * 7);
            SandMesh.cloud(dust, poseStack, view, new Vector3f(Mth.cos(a) * r, .4f + .9f * BossMesh.hash(seed + k), Mth.sin(a) * r), 1.4f + .8f * burst, a + age * .02f,
                    k % 2 == 0 ? SandMesh.SAND : SandMesh.SAND_PALE, .6f * burst, light);
        }
        SandMesh.grains(poseStack, buffer, view, reach + 1, 3, age, 12, alive * rise, seed, light);
    }
}
