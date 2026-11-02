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
 * Titan's Fist: a colossal gauntlet of black iron trimmed with gold, its runes glowing, plunges out of the sky fist
 * first, its arm reaching back up out of sight; it slams into the ground, cracks of light racing out round it and dust
 * billowing, then draws back up into the sky.
 */
public final class FistModel {
    private FistModel() {}

    /**
     * Where it lands (the pose's origin), coming from {@code from}; it lands {@code hit} ticks ago (negative before) after
     * falling {@code fall} ticks; facing {@code yaw} (degrees); {@code size} the blow's radius.
     */
    public static void draw(PoseStack poseStack, MultiBufferSource buffer, WaterMesh.View view, Vector3f from, float age, float hit, float fall, float yaw, float size,
                            int zone, int seed, int light) {
        float down = hit < 0 ? Mth.clamp(age / Math.max(1, fall), 0, 1) : 1;
        down *= down;
        float rise = BossMesh.ramp(hit, 14, 36);
        float k0 = BossMesh.ramp(age, 0, 6) * (1 - BossMesh.ramp(hit, 30, 38));
        if (k0 < .01f) {
            return;
        }
        float s = size * .5f;
        Vector3f at = new Vector3f(from).lerp(new Vector3f(0, s * 1.05f, 0), down).add(0, rise * 20, 0);
        poseStack.pushPose();
        poseStack.translate(at.x, at.y, at.z);
        poseStack.mulPose(new Quaternionf().rotationY(-yaw * Mth.DEG_TO_RAD));
        poseStack.scale(s, s, s);
        PoseStack.Pose p = poseStack.last();
        VertexConsumer iron = BossMesh.solid(buffer, BossMesh.IRON);
        int dark = 0xFF7A7684, steel = 0xFFB0ACBA;
        // The fist: four knuckles, the back of the hand, the thumb wrapped over
        for (int k = 0; k < 4; k++) {
            float x = -.75f + k * .5f;
            BossMesh.box(iron, p, new Vector3f(x, -.55f, .25f), new Vector3f(.24f, 0, 0), new Vector3f(0, .42f, 0), new Vector3f(0, 0, .55f), k % 2 == 0 ? steel : dark, light);
            BossMesh.box(iron, p, new Vector3f(x, -.95f, .55f), new Vector3f(.22f, 0, 0), new Vector3f(0, .16f, 0), new Vector3f(0, 0, .26f), steel, light);
        }
        BossMesh.box(iron, p, new Vector3f(0, .15f, 0), new Vector3f(1.05f, 0, 0), new Vector3f(0, .55f, 0), new Vector3f(0, 0, .75f), dark, light);
        BossMesh.box(iron, p, new Vector3f(1.15f, -.35f, .35f), new Vector3f(.22f, 0, 0), new Vector3f(0, .45f, 0), new Vector3f(0, 0, .3f), steel, light);
        // The arm, plated, reaching back up into the sky
        for (int k = 0; k < 6; k++) {
            float y = .9f + k * 1.15f;
            float w = .95f - k * .03f;
            BossMesh.box(iron, p, new Vector3f(0, y, -.1f - k * .12f), new Vector3f(w, 0, 0), new Vector3f(0, .55f, 0), new Vector3f(0, 0, .7f), k % 2 == 0 ? dark : steel, light);
        }
        // Gold trim
        VertexConsumer gold = BossMesh.solid(buffer, SandMesh.GOLD_SKIN);
        p = poseStack.last();
        BossMesh.box(gold, p, new Vector3f(0, .72f, 0), new Vector3f(1.1f, 0, 0), new Vector3f(0, .08f, 0), new Vector3f(0, 0, .8f), 0xFFFFFFFF, light);
        for (int k = 0; k < 4; k++) {
            BossMesh.box(gold, p, new Vector3f(-.75f + k * .5f, -.2f, .82f), new Vector3f(.18f, 0, 0), new Vector3f(0, .06f, 0), new Vector3f(0, 0, .03f), 0xFFFFFFFF, light);
        }
        // Its runes glowing on the back of the hand
        VertexConsumer runes = BossMesh.light(buffer, BossMesh.RUNES);
        BossMesh.plate(runes, poseStack.last(), new Vector3f(0, .15f, .77f), new Vector3f(1, 0, 0), new Vector3f(0, 1, 0), 1.2f, BossMesh.glow(zone, k0), light);
        for (int k = 0; k < 6; k++) {
            BossMesh.plate(runes, poseStack.last(), new Vector3f(0, .9f + k * 1.15f, .62f - k * .12f), new Vector3f(1, 0, 0), new Vector3f(0, 1, 0), .9f,
                    BossMesh.glow(zone, .7f * k0), light);
        }
        poseStack.popPose();
        // The blow: cracks of light, a flash, dust
        if (hit >= 0) {
            float t = Mth.clamp(hit / 30, 0, 1);
            VertexConsumer cracks = BossMesh.light(buffer, BossMesh.MOON_CRACKS);
            BossMesh.pool(cracks, poseStack.last(), 0, .06f, 0, size * (2 + 1.5f * Math.min(1, hit / 6)), seed, BossMesh.glow(zone, 1 - t), light);
            VertexConsumer dust = BossMesh.see(buffer, BossMesh.DUST);
            for (int k = 0; k < 12; k++) {
                float a = k * Mth.TWO_PI / 12 + BossMesh.hash(seed + k);
                float r = size * (.6f + 1.1f * t);
                SandMesh.cloud(dust, poseStack, view, new Vector3f(Mth.cos(a) * r, .5f + t * 1.5f, Mth.sin(a) * r), 2 + 3 * t, a, 0x9A8A78, .7f * (1 - t), light);
            }
            VertexConsumer glow = BossMesh.light(buffer, BossMesh.GLOW);
            RitualMesh.billboard(glow, poseStack, view, new Vector3f(0, 1, 0), size * 3 * (1 - t), 0, -1, BossMesh.glow(zone, 1 - t), light);
        }
    }
}
