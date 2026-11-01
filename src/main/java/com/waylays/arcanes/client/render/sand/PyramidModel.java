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
 * The Pyramid: the ground shakes, and a great stepped pyramid of sandstone rises out of the sand over its victims,
 * tier on tier, a golden capstone blazing at its tip, sand pouring off its steps. Its sentence spoken, the capstone
 * flares and the whole of it falls in on itself, tier by tier, in a storm of dust.
 */
public final class PyramidModel {
    public static final int TIERS = 7;

    private PyramidModel() {}

    /**
     * Over the middle of its ground (the pose's origin), {@code half} its half-width, {@code height} tall; {@code age}
     * ticks since it began to rise, {@code since} ticks since it fell in (negative before).
     */
    public static void draw(PoseStack poseStack, MultiBufferSource buffer, WaterMesh.View view, float half, float height, float age, float since, int seed,
                            int light) {
        float rise = BossMesh.ramp(age, 0, 30);
        float fall = since > 0 ? BossMesh.ramp(since, 0, 18) : 0;
        float th = height / TIERS;
        VertexConsumer stone = BossMesh.see(buffer, SandMesh.SANDSTONE);
        PoseStack.Pose p = poseStack.last();
        for (int k = 0; k < TIERS; k++) {
            float w = half * (1 - (float) k / TIERS);
            // Rising: the whole pushed up out of the ground; falling: the upper tiers drop first, into those below
            float y = (k + .5f) * th - height * (1 - rise);
            float drop = Mth.clamp(fall * 1.6f - (TIERS - 1 - k) * .1f, 0, 1);
            y -= drop * drop * (k + 1) * th;
            float alpha = 1 - BossMesh.ramp(since, 12, 18);
            SandMesh.block(stone, p, new Vector3f(0, y, 0), w * (1 + .05f * drop), th * .5f, w * (1 + .05f * drop), BossMesh.argb(0xFFFFFF, alpha), light);
        }
        // The capstone
        float capY = height * rise - height * fall * 1.2f + .6f;
        VertexConsumer gold = BossMesh.light(buffer, BossMesh.SPIKE);
        p = poseStack.last();
        float cw = half / TIERS * 1.1f;
        Vector3f apex = new Vector3f(0, capY + cw * 1.3f, 0);
        Vector3f[] base = new Vector3f[4];
        for (int k = 0; k < 4; k++) {
            float a = k * Mth.HALF_PI + Mth.PI / 4;
            base[k] = new Vector3f(Mth.cos(a) * cw * 1.414f, capY, Mth.sin(a) * cw * 1.414f);
        }
        float shine = since < 0 ? .8f + .2f * Mth.sin(age * .2f) : 1 - BossMesh.ramp(since, 4, 14);
        for (int k = 0; k < 4; k++) {
            int j = (k + 1) % 4;
            Vector3f out = new Vector3f(base[k]).add(base[j]).add(apex).mul(1 / 3f).sub(0, capY, 0);
            BossMesh.faced(gold, p, base[k], base[j], apex, apex, new float[] {0, 1, 1, 1, .5f, 0, .5f, 0}, BossMesh.glow(SandMesh.GOLD, shine),
                    BossMesh.glow(SandMesh.GOLD, shine), BossMesh.glow(0xFFF6D0, shine), BossMesh.glow(0xFFF6D0, shine), light, out);
        }
        VertexConsumer glow = BossMesh.light(buffer, BossMesh.GLOW);
        float flare = since >= 0 ? Math.max(0, 1 - Math.abs(since - 2) / 6) : 0;
        RitualMesh.billboard(glow, poseStack, view, apex, 3 + 10 * flare, 0, -1, BossMesh.glow(SandMesh.GOLD, (.6f + .4f * flare) * shine), light);
        RitualMesh.billboard(glow, poseStack, view, apex, 1.2f + 4 * flare, 0, -1, BossMesh.glow(0xFFFFFF, (.5f + .5f * flare) * shine), light);
        // Sand pouring off it as it rises; a storm of dust as it falls in
        VertexConsumer dust = BossMesh.see(buffer, BossMesh.DUST);
        if (rise < 1) {
            for (int k = 0; k < 20; k++) {
                float a = k * Mth.TWO_PI / 20;
                float r = half * (.9f + .2f * BossMesh.hash(seed + k));
                SandMesh.cloud(dust, poseStack, view, new Vector3f(Mth.cos(a) * r, .5f + 1.5f * BossMesh.hash(seed + k * 3), Mth.sin(a) * r), 2.5f, a, SandMesh.SAND,
                        .6f * (1 - rise), light);
            }
        }
        if (since > 0) {
            float t = BossMesh.ramp(since, 0, 30);
            for (int k = 0; k < 30; k++) {
                float a = k * Mth.TWO_PI / 30 + BossMesh.hash(seed + k) * .3f;
                float r = half * (.3f + 1.2f * t) * (.7f + .3f * BossMesh.hash(seed + k * 5));
                SandMesh.cloud(dust, poseStack, view, new Vector3f(Mth.cos(a) * r, 1 + height * .5f * (1 - t) * BossMesh.hash(seed + k * 7), Mth.sin(a) * r),
                        3 + 4 * t, a + t, k % 3 == 0 ? SandMesh.SAND_PALE : SandMesh.SAND, .75f * (1 - t), light);
            }
        }
    }
}
