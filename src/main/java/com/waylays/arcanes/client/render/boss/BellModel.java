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
 * Bell of Doom: a great bronze bell hangs in the air over the caster from a ring of gold, runes turning round it. It
 * swings and tolls three times, each toll a ring of light pouring from its lip down to the ground and racing out over
 * it; then it fades.
 */
public final class BellModel {
    private static final float[] YS = {0, -.15f, -.45f, -1.0f, -1.7f, -2.3f, -2.65f, -2.8f};
    private static final float[] RS = {.35f, .75f, 1.05f, 1.15f, 1.25f, 1.6f, 2.05f, 2.15f};

    private BellModel() {}

    /**
     * Under it (the pose's origin), the bell at {@code bell}; {@code hit} ticks since its first toll (negative before),
     * tolling every {@code every} ticks, three times; its rings reach {@code radius}.
     */
    public static void draw(PoseStack poseStack, MultiBufferSource buffer, WaterMesh.View view, Vector3f bell, float age, float hit, int every, float radius,
                            int zone, int seed, int light) {
        float appear = BossMesh.ramp(age, 0, 14);
        float gone = BossMesh.ramp(hit, every * 2 + 10, every * 2 + 24);
        float k0 = appear * (1 - gone);
        if (k0 < .01f) {
            return;
        }
        // Its swing: a kick at each toll, dying away
        float swing = 0;
        for (int k = 0; k < 3; k++) {
            float s = hit - k * every;
            if (s >= 0) {
                swing += .3f * (float) Math.exp(-s / 7) * Mth.sin(s * .55f);
            }
        }
        poseStack.pushPose();
        poseStack.translate(bell.x, bell.y + 1.2f * (1 - appear), bell.z);
        poseStack.mulPose(new Quaternionf().rotationX(swing));
        poseStack.scale(1.5f, 1.5f, 1.5f);
        PoseStack.Pose p = poseStack.last();
        // The bronze
        VertexConsumer bronze = BossMesh.solid(buffer, SandMesh.GOLD_SKIN);
        SandMesh.lathe(bronze, p, new Vector3f(), YS, RS, 28, 6, 0, 0xFFB07A3C, light);
        // Its dark mouth, its clapper, the ring it hangs from
        SandMesh.lathe(bronze, p, new Vector3f(0, YS[YS.length - 1], 0), new float[] {0, .02f}, new float[] {RS[RS.length - 1], 0}, 28, 1, 0, 0xFF2A1A0C, light);
        int n = 20;
        Vector3f[] hook = new Vector3f[n + 1];
        float[] hr = new float[n + 1];
        for (int i = 0; i <= n; i++) {
            float a = i * Mth.TWO_PI / n;
            hook[i] = new Vector3f(0, .55f + Mth.sin(a) * .45f, Mth.cos(a) * .45f);
            hr[i] = .1f;
        }
        SandMesh.tube(bronze, p, hook, hr, 6, 2, 0, SandMesh.colours(0xFFFFE08A, n + 1), light);
        SandMesh.ellipsoid(bronze, poseStack, new Vector3f(0, -2.3f, 0), .32f, .32f, .32f, 0, 0, 0xFF6A4A26, light);
        poseStack.popPose();
        // Runes turning round it
        VertexConsumer runes = BossMesh.light(buffer, BossMesh.RUNES);
        BossMesh.annulus(runes, poseStack.last(), new Vector3f(bell).add(0, -2.1f, 0), new Vector3f(1, 0, 0), new Vector3f(0, 0, -1), 3.8f, 5.2f, 32, 2, age * .02f, 0,
                BossMesh.glow(zone, .7f * k0), BossMesh.glow(zone, .4f * k0), light, true);
        // The tolls: light pouring from its lip, a ring racing out over the ground
        VertexConsumer ring = BossMesh.light(buffer, BossMesh.RING);
        VertexConsumer glow;
        for (int k = 0; k < 3; k++) {
            float s = hit - k * every;
            if (s < 0 || s > 16) {
                continue;
            }
            float t = s / 16;
            float f = 1 - t;
            float y = Mth.lerp(Math.min(1, t * 2.5f), bell.y - 4.2f, .1f);
            float r = Mth.lerp(Math.min(1, t * 2.5f), 3.2f, 4.5f) + radius * Math.max(0, t * 1.3f - .3f);
            BossMesh.pool(ring, poseStack.last(), 0, y, 0, r * 2.2f, k, BossMesh.glow(zone, f), light);
            BossMesh.pool(ring, poseStack.last(), 0, y + .05f, 0, r * 1.7f, k + 1, BossMesh.glow(0xFFFFFF, f * .7f), light);
        }
        glow = BossMesh.light(buffer, BossMesh.GLOW);
        float flash = 0;
        for (int k = 0; k < 3; k++) {
            float s = hit - k * every;
            if (s >= 0 && s < 8) {
                flash = Math.max(flash, 1 - s / 8);
            }
        }
        RitualMesh.billboard(glow, poseStack, view, new Vector3f(bell).add(0, -2.1f, 0), 7 + 8 * flash, 0, -1, BossMesh.glow(zone, (.25f + .6f * flash) * k0), light);
    }
}
