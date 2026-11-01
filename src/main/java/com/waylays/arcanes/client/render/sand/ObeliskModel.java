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
 * The Obelisk: a tall needle of carved sandstone rises out of the ground, bands of golden glyphs glowing up its sides,
 * its gilded tip catching the sun; it looses beams of sunlight at the foes round it, then sinks back into the sand.
 */
public final class ObeliskModel {
    /** How long a beam shows (ticks). */
    public static final int BEAM = 8;

    private ObeliskModel() {}

    /** At its foot (the pose's origin), {@code height} tall; {@code age} of {@code life} ticks; its last beam {@code sinceBeam} ago at {@code target}. */
    public static void draw(PoseStack poseStack, MultiBufferSource buffer, WaterMesh.View view, float height, float age, float life, float sinceBeam,
                            Vector3f target, int seed, int light) {
        float rise = BossMesh.ramp(age, 0, 14);
        float sink = BossMesh.ramp(age, life - 14, life);
        float y0 = -height * (1 - rise) - height * sink;
        float b0 = .7f, b1 = .4f;
        Vector3f[] base = new Vector3f[4], top = new Vector3f[4];
        for (int k = 0; k < 4; k++) {
            float a = k * Mth.HALF_PI + Mth.PI / 4;
            base[k] = new Vector3f(Mth.cos(a) * b0 * 1.414f, y0, Mth.sin(a) * b0 * 1.414f);
            top[k] = new Vector3f(Mth.cos(a) * b1 * 1.414f, y0 + height, Mth.sin(a) * b1 * 1.414f);
        }
        VertexConsumer stone = BossMesh.see(buffer, SandMesh.SANDSTONE);
        PoseStack.Pose p = poseStack.last();
        for (int k = 0; k < 4; k++) {
            int j = (k + 1) % 4;
            Vector3f out = new Vector3f(base[k]).add(base[j]).mul(.5f).sub(0, y0, 0);
            BossMesh.faced(stone, p, base[k], base[j], top[j], top[k], new float[] {0, height * .5f, 1, height * .5f, 1, 0, 0, 0}, 0xFFFFFFFF, 0xFFFFFFFF,
                    0xFFFFFFFF, 0xFFFFFFFF, light, out);
        }
        // Its gilded tip
        Vector3f apex = new Vector3f(0, y0 + height + .9f, 0);
        VertexConsumer gold = BossMesh.light(buffer, BossMesh.SPIKE);
        p = poseStack.last();
        for (int k = 0; k < 4; k++) {
            int j = (k + 1) % 4;
            Vector3f out = new Vector3f(top[k]).add(top[j]).add(apex).mul(1 / 3f).sub(0, y0 + height, 0);
            BossMesh.faced(gold, p, top[k], top[j], apex, apex, new float[] {0, 1, 1, 1, .5f, 0, .5f, 0}, BossMesh.glow(SandMesh.GOLD, 1), BossMesh.glow(SandMesh.GOLD, 1),
                    BossMesh.glow(0xFFF4C0, 1), BossMesh.glow(0xFFF4C0, 1), light, out);
        }
        // Bands of glowing glyphs up its sides
        VertexConsumer glyphs = BossMesh.light(buffer, SandMesh.GLYPHS);
        p = poseStack.last();
        for (int band = 0; band < 3; band++) {
            float t0 = .2f + band * .25f, t1 = t0 + .12f;
            float pulse = .6f + .4f * Mth.sin(age * .2f - band);
            for (int k = 0; k < 4; k++) {
                int j = (k + 1) % 4;
                Vector3f a0 = new Vector3f(base[k]).lerp(top[k], t0), a1 = new Vector3f(base[j]).lerp(top[j], t0);
                Vector3f c1 = new Vector3f(base[j]).lerp(top[j], t1), c0 = new Vector3f(base[k]).lerp(top[k], t1);
                Vector3f out = new Vector3f(a0).add(a1).mul(.5f).sub(0, a0.y, 0);
                Vector3f lift = new Vector3f(out).normalize().mul(.02f);
                BossMesh.faced(glyphs, p, a0.add(lift), a1.add(lift), c1.add(lift), c0.add(lift), new float[] {0, 1, 1, 1, 1, 0, 0, 0},
                        BossMesh.glow(SandMesh.GOLD, pulse), BossMesh.glow(SandMesh.GOLD, pulse), BossMesh.glow(SandMesh.GOLD, pulse), BossMesh.glow(SandMesh.GOLD, pulse),
                        light, out);
            }
        }
        VertexConsumer glow = BossMesh.light(buffer, BossMesh.GLOW);
        float flare = sinceBeam < BEAM ? 1 - sinceBeam / BEAM : 0;
        RitualMesh.billboard(glow, poseStack, view, apex, 1.6f + 2.5f * flare, 0, -1, BossMesh.glow(SandMesh.GOLD, (.7f + .3f * flare) * (1 - sink)), light);
        if (flare > 0 && target != null) {
            VertexConsumer beam = BossMesh.light(buffer, BossMesh.BEAM);
            RitualMesh.shaft(beam, poseStack.last(), view.eye(), apex, target, .9f, BossMesh.glow(SandMesh.GOLD, flare), light);
            RitualMesh.shaft(beam, poseStack.last(), view.eye(), apex, target, .3f, BossMesh.glow(0xFFFFFF, flare), light);
            VertexConsumer hit = BossMesh.light(buffer, BossMesh.GLOW);
            RitualMesh.billboard(hit, poseStack, view, target, 2.2f * flare, 0, -1, BossMesh.glow(SandMesh.GOLD, flare), light);
        }
        if (age < 16) {
            SandMesh.spray(poseStack, buffer, view, 2, 1.5f, age / 16, 10, seed, light);
        }
    }
}
