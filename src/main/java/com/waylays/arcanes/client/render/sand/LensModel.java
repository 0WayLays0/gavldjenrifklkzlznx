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
 * Lens of the Sun: a great lens of glass in a rim of chased gold hangs in the sky, glyphs glowing round it; sunlight
 * pours into it from above and it gathers it into a cone of light onto a single burning point on the ground, where the
 * sand glows molten and turns to glass, embers and smoke rising.
 */
public final class LensModel {
    private LensModel() {}

    /** At the burning point (the pose's origin), the lens {@code height} above; {@code age} of {@code life} ticks. */
    public static void draw(PoseStack poseStack, MultiBufferSource buffer, WaterMesh.View view, float height, float age, float life, int seed, int light) {
        float form = BossMesh.ramp(age, 0, 12);
        float fade = 1 - BossMesh.ramp(age, life - 10, life);
        float burn = BossMesh.ramp(age, 8, 16) * fade;
        float R = 1.7f * form;
        Vector3f lens = new Vector3f(Mth.sin(age * .05f) * .2f, height, Mth.cos(age * .04f) * .2f);
        // The glass
        VertexConsumer glass = BossMesh.see(buffer, SandMesh.GLASS);
        SandMesh.lathe(glass, poseStack.last(), lens, new float[] {-.22f, -.16f, -.08f, 0, .08f, .16f, .22f}, new float[] {0, .55f * R, .85f * R, R, .85f * R, .55f * R, 0},
                24, 1, 0, BossMesh.argb(SandMesh.GLASS_TINT, .5f * fade), light);
        // Its rim of gold
        VertexConsumer gold = BossMesh.solid(buffer, SandMesh.GOLD_SKIN);
        int m = 32;
        Vector3f[] rim = new Vector3f[m + 1];
        float[] rr = new float[m + 1];
        for (int i = 0; i <= m; i++) {
            float a = i * Mth.TWO_PI / m;
            rim[i] = new Vector3f(Mth.cos(a) * R * 1.04f, 0, Mth.sin(a) * R * 1.04f).add(lens);
            rr[i] = .1f * form;
        }
        if (form > .05f && fade > .05f) {
            SandMesh.tube(gold, poseStack.last(), rim, rr, 6, 2, 0, SandMesh.colours(0xFFFFFFFF, m + 1), light);
        }
        // Glyphs round it, and the light pouring in and down
        VertexConsumer glyphs = BossMesh.light(buffer, SandMesh.GLYPHS);
        BossMesh.annulus(glyphs, poseStack.last(), new Vector3f(lens).add(0, .02f, 0), new Vector3f(1, 0, 0), new Vector3f(0, 0, -1), R * 1.1f, R * 1.45f, 32, 6,
                age * .01f, 0, BossMesh.glow(SandMesh.GOLD, .9f * form * fade), BossMesh.glow(SandMesh.GOLD, .5f * form * fade), light, true);
        VertexConsumer beam = BossMesh.light(buffer, BossMesh.BEAM);
        PoseStack.Pose p = poseStack.last();
        RitualMesh.shaft(beam, p, view.eye(), new Vector3f(lens).add(0, 10, 0), lens, R * 2.2f, BossMesh.glow(SandMesh.SUN, .22f * form * fade), light);
        RitualMesh.shaft(beam, p, view.eye(), lens, new Vector3f(0, .1f, 0), .9f * burn, BossMesh.glow(SandMesh.SUN, burn), light);
        RitualMesh.shaft(beam, p, view.eye(), lens, new Vector3f(0, .1f, 0), .3f * burn, BossMesh.glow(0xFFFFFF, burn), light);
        for (int k = 0; k < 8; k++) {
            float a = k * Mth.TWO_PI / 8 + age * .02f;
            RitualMesh.shaft(beam, p, view.eye(), new Vector3f(lens).add(Mth.cos(a) * R * .9f, 0, Mth.sin(a) * R * .9f), new Vector3f(0, .1f, 0), .12f,
                    BossMesh.glow(SandMesh.SUN, .35f * burn), light);
        }
        // The burning point: sand glowing molten, glass forming, scorched round it
        VertexConsumer scorch = BossMesh.ground(buffer, BossMesh.DISC);
        BossMesh.pool(scorch, poseStack.last(), 0, .02f, 0, 3.2f * burn, 0, BossMesh.argb(0x281808, .7f * burn), light);
        VertexConsumer melt = BossMesh.light(buffer, BossMesh.GLOW);
        float pulse = .85f + .15f * Mth.sin(age * .8f);
        BossMesh.pool(melt, poseStack.last(), 0, .04f, 0, 2.4f * burn * pulse, 0, BossMesh.glow(0xFF8A30, burn), light);
        BossMesh.pool(melt, poseStack.last(), 0, .05f, 0, 1.1f * burn, 0, BossMesh.glow(0xFFF0C0, burn), light);
        RitualMesh.billboard(melt, poseStack, view, new Vector3f(0, .3f, 0), 2.2f * burn * pulse, 0, -1, BossMesh.glow(SandMesh.SUN, burn), light);
        RitualMesh.billboard(melt, poseStack, view, lens, R * 1.8f, 0, -1, BossMesh.glow(SandMesh.SUN, .45f * form * fade), light);
        VertexConsumer embers = BossMesh.light(buffer, BossMesh.MOTE);
        for (int k = 0; k < 12; k++) {
            float t = (age * .04f + BossMesh.hash(seed + k)) % 1;
            float a = BossMesh.hash(seed + k * 3) * Mth.TWO_PI;
            RitualMesh.billboard(embers, poseStack, view, new Vector3f(Mth.cos(a) * .6f * t, t * 2.5f, Mth.sin(a) * .6f * t), .2f, 0, -1,
                    BossMesh.glow(k % 2 == 0 ? 0xFFB040 : SandMesh.SUN, (1 - t) * burn), light);
        }
        VertexConsumer smoke = BossMesh.see(buffer, BossMesh.SMOKE);
        for (int k = 0; k < 4; k++) {
            float t = (age * .03f + k / 4f) % 1;
            SandMesh.cloud(smoke, poseStack, view, new Vector3f(.2f * Mth.sin(k + age * .05f), .5f + t * 3, .2f * Mth.cos(k)), .8f + 1.6f * t, k, 0x504030, .4f * (1 - t) * burn,
                    light);
        }
    }
}
