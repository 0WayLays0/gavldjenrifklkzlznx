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
 * Spectral Kings: a phantom king in black armour kneels at the end of his strip, crowned, his cloak of violet light
 * spread behind him, his greatsword planted; he rises and charges through the strip in a blink, a trail of ghosts of
 * himself fading behind him, and is gone.
 */
public final class KingModel {
    private KingModel() {}

    /**
     * From his strip's start (the pose's origin) to {@code to}, facing {@code yaw} (degrees); he charges {@code hit} ticks ago
     * (negative before).
     */
    public static void draw(PoseStack poseStack, MultiBufferSource buffer, WaterMesh.View view, Vector3f to, float age, float hit, float yaw, int zone, int seed,
                            int light) {
        float appear = BossMesh.ramp(age, 0, 14);
        float fade = 1 - BossMesh.ramp(hit, 4, 14);
        float k0 = appear * fade;
        if (k0 < .01f) {
            return;
        }
        float dash = Mth.clamp(hit / 4, 0, 1);
        float kneel = 1 - BossMesh.ramp(hit, -6, 0);
        // His ghosts trailing behind as he charges
        if (hit > 0) {
            for (int g = 3; g >= 1; g--) {
                float back = Math.max(0, dash - g * .12f);
                king(poseStack, buffer, new Vector3f().lerp(to, back), yaw, 0, .25f * k0 / g, zone, light);
            }
        }
        king(poseStack, buffer, new Vector3f().lerp(to, dash), yaw, kneel, k0, zone, light);
        VertexConsumer glow = BossMesh.light(buffer, BossMesh.GLOW);
        RitualMesh.billboard(glow, poseStack, view, new Vector3f().lerp(to, dash).add(0, 1.3f, 0), 3, 0, -1, BossMesh.glow(zone, .4f * k0), light);
    }

    private static void king(PoseStack poseStack, MultiBufferSource buffer, Vector3f at, float yaw, float kneel, float alpha, int zone, int light) {
        poseStack.pushPose();
        poseStack.translate(at.x, at.y, at.z);
        poseStack.mulPose(new Quaternionf().rotationY(-yaw * Mth.DEG_TO_RAD));
        float s = 1.25f;
        poseStack.scale(s, s, s);
        PoseStack.Pose p = poseStack.last();
        float drop = .45f * kneel;
        int armour = BossMesh.argb(0x30283A, .9f * alpha), plate = BossMesh.argb(0x4A3E5A, .9f * alpha);
        VertexConsumer iron = BossMesh.see(buffer, BossMesh.IRON);
        // Legs (one knee down when kneeling), body, arms, helm
        BossMesh.box(iron, p, new Vector3f(-.14f, .38f - drop * .4f, 0), new Vector3f(.12f, 0, 0), new Vector3f(0, .38f, 0), new Vector3f(0, 0, .13f), armour, light);
        BossMesh.box(iron, p, new Vector3f(.14f, .38f - drop * .4f, -drop * .5f), new Vector3f(.12f, 0, 0), new Vector3f(0, .38f - drop * .3f, 0), new Vector3f(0, 0, .13f),
                armour, light);
        BossMesh.box(iron, p, new Vector3f(0, 1.15f - drop, 0), new Vector3f(.3f, 0, 0), new Vector3f(0, .4f, 0), new Vector3f(0, 0, .18f), plate, light);
        BossMesh.box(iron, p, new Vector3f(0, 1.5f - drop, 0), new Vector3f(.44f, 0, 0), new Vector3f(0, .1f, 0), new Vector3f(0, 0, .22f), armour, light);
        BossMesh.box(iron, p, new Vector3f(-.42f, 1.15f - drop, .1f), new Vector3f(.11f, 0, 0), new Vector3f(0, .36f, 0), new Vector3f(0, 0, .11f), armour, light);
        BossMesh.box(iron, p, new Vector3f(.42f, 1.15f - drop, .1f), new Vector3f(.11f, 0, 0), new Vector3f(0, .36f, 0), new Vector3f(0, 0, .11f), armour, light);
        BossMesh.box(iron, p, new Vector3f(0, 1.78f - drop, 0), new Vector3f(.19f, 0, 0), new Vector3f(0, .2f, 0), new Vector3f(0, 0, .19f), plate, light);
        // The greatsword, planted before him or held forward as he charges
        BossMesh.box(iron, p, new Vector3f(0, .9f - drop, .45f), new Vector3f(.05f, 0, 0), new Vector3f(0, .85f, 0), new Vector3f(0, 0, .02f),
                BossMesh.argb(0x8A80A0, alpha), light);
        // His crown and his eyes, his cloak of light
        VertexConsumer gold = BossMesh.see(buffer, SandMesh.GOLD_SKIN);
        p = poseStack.last();
        for (int k = 0; k < 5; k++) {
            float a = k * Mth.TWO_PI / 5;
            BossMesh.horn(gold, p, new Vector3f(Mth.cos(a) * .17f, 2.0f - drop, Mth.sin(a) * .17f), new Vector3f(0, 1, 0), new Vector3f(), .22f, .045f, 4, 2,
                    BossMesh.argb(0xFFFFFF, alpha), BossMesh.argb(0xFFFFFF, alpha), light);
        }
        VertexConsumer cloak = BossMesh.light(buffer, BossMesh.EDGE);
        p = poseStack.last();
        BossMesh.quad2(cloak, p, new Vector3f(-.42f, 1.55f - drop, -.2f), new Vector3f(.42f, 1.55f - drop, -.2f), new Vector3f(.6f, .1f, -.7f), new Vector3f(-.6f, .1f, -.7f),
                0, 0, 1, 0, 1, 1, 0, 1, BossMesh.glow(zone, .7f * alpha), BossMesh.glow(zone, .15f * alpha), light);
        BossMesh.plate(cloak, p, new Vector3f(0, 1.8f - drop, .2f), new Vector3f(1, 0, 0), new Vector3f(0, 1, 0), .3f, BossMesh.glow(0xE0B0FF, alpha), light);
        poseStack.popPose();
    }
}
