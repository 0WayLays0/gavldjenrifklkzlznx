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
 * Crimson Lotus: a lotus of crimson glass, petal over petal, rises out of the ground as a closed bud, glowing from
 * within; its heart bursts in a flash, then its six great petals fling open outward and lie flat, slicing the ground,
 * their edges burning; then it shatters into motes.
 */
public final class LotusModel {
    private LotusModel() {}

    /** On the ground (the pose's origin); its heart bursts {@code hit} ticks ago (negative before); its petals reach {@code radius}, turned {@code yaw} (degrees). */
    public static void draw(PoseStack poseStack, MultiBufferSource buffer, WaterMesh.View view, float age, float hit, float radius, float yaw, int zone, int seed,
                            int light) {
        float appear = BossMesh.ramp(age, 0, 16);
        float open = BossMesh.ramp(hit, 8, 12);
        float gone = BossMesh.ramp(hit, 22, 34);
        float k0 = appear * (1 - gone);
        if (k0 < .01f) {
            return;
        }
        float base = -yaw * Mth.DEG_TO_RAD;
        VertexConsumer glass = BossMesh.see(buffer, SandMesh.GLASS);
        PoseStack.Pose p = poseStack.last();
        for (int layer = 0; layer < 2; layer++) {
            int petals = 6;
            for (int k = 0; k < petals; k++) {
                float a = base + k * Mth.TWO_PI / petals + layer * Mth.PI / petals;
                Vector3f out = new Vector3f(Mth.sin(a), 0, Mth.cos(a));
                Vector3f side = new Vector3f(out.z, 0, -out.x);
                // Closed: standing up round the heart; open: lying flat, reaching out (the outer layer to the full reach)
                float len = (layer == 0 ? radius : radius * .55f) * (.25f + .75f * open) * appear;
                float tilt = Mth.lerp(open, .25f + .15f * layer, Mth.HALF_PI * (layer == 0 ? .97f : .8f));
                Vector3f dir = new Vector3f(out).mul(Mth.sin(tilt)).add(0, Mth.cos(tilt), 0);
                int n = 6;
                for (int i = 0; i < n; i++) {
                    float t0 = (float) i / n, t1 = (float) (i + 1) / n;
                    float w0 = len * .32f * Mth.sin(Mth.PI * (.1f + .9f * t0)), w1 = len * .32f * Mth.sin(Mth.PI * (.1f + .9f * t1));
                    Vector3f c0 = new Vector3f(dir).mul(len * t0).add(0, .1f + .2f * Mth.sin(Mth.PI * t0) * open, 0).add(new Vector3f(out).mul(.3f));
                    Vector3f c1 = new Vector3f(dir).mul(len * t1).add(0, .1f + .2f * Mth.sin(Mth.PI * t1) * open, 0).add(new Vector3f(out).mul(.3f));
                    int col0 = BossMesh.argb(BossMesh.mix(0x7A0814, zone, t0), .85f * k0), col1 = BossMesh.argb(BossMesh.mix(0x7A0814, zone, t1), .85f * k0);
                    BossMesh.quad2(glass, p, new Vector3f(c0).sub(new Vector3f(side).mul(w0)), new Vector3f(c0).add(new Vector3f(side).mul(w0)),
                            new Vector3f(c1).add(new Vector3f(side).mul(w1)), new Vector3f(c1).sub(new Vector3f(side).mul(w1)), 0, t0, 1, t0, 1, t1, 0, t1, col0, col1, light);
                }
            }
        }
        // Its edges burning as it opens, its heart glowing, bursting
        VertexConsumer glow = BossMesh.light(buffer, BossMesh.GLOW);
        float burst = hit >= 0 && hit < 10 ? 1 - hit / 10 : 0;
        RitualMesh.billboard(glow, poseStack, view, new Vector3f(0, .8f, 0), 2.4f + 6 * burst, 0, -1, BossMesh.glow(zone, (.6f + .4f * burst) * k0), light);
        RitualMesh.billboard(glow, poseStack, view, new Vector3f(0, .8f, 0), 1 + 2 * burst, 0, -1, BossMesh.glow(0xFFFFFF, (.5f + .5f * burst) * k0), light);
        if (open > 0) {
            VertexConsumer edge = BossMesh.light(buffer, BossMesh.EDGE);
            for (int k = 0; k < 6; k++) {
                float a = base + k * Mth.TWO_PI / 6;
                Vector3f out = new Vector3f(Mth.sin(a), 0, Mth.cos(a));
                Vector3f side = new Vector3f(out.z, 0, -out.x);
                float len = radius * open;
                BossMesh.quad2(edge, poseStack.last(), new Vector3f(out).mul(.3f).add(0, .12f, 0), new Vector3f(out).mul(len).add(new Vector3f(side).mul(.25f)).add(0, .12f, 0),
                        new Vector3f(out).mul(len).add(new Vector3f(side).mul(-.25f)).add(0, .12f, 0), new Vector3f(out).mul(.3f).add(0, .12f, 0), 0, 0, 1, 0, 1, 1, 0, 1,
                        BossMesh.glow(zone, k0 * (1 - BossMesh.ramp(hit, 12, 22))), BossMesh.glow(0xFFFFFF, .5f * k0 * (1 - BossMesh.ramp(hit, 12, 22))), light);
            }
        }
        if (gone > 0) {
            VertexConsumer motes = BossMesh.light(buffer, BossMesh.MOTE);
            for (int k = 0; k < 18; k++) {
                float a = BossMesh.hash(seed + k) * Mth.TWO_PI, r = radius * BossMesh.hash(seed + k * 3);
                RitualMesh.billboard(motes, poseStack, view, new Vector3f(Mth.cos(a) * r, .3f + gone * 2.5f * BossMesh.hash(seed + k * 5), Mth.sin(a) * r), .35f, 0, -1,
                        BossMesh.glow(zone, 1 - gone), light);
            }
        }
    }
}
