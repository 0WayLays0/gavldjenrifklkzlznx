package com.waylays.arcanes.client.render.fire;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.waylays.arcanes.client.render.water.WaterMesh;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

import static com.waylays.arcanes.client.render.water.WaterMesh.*;

/**
 * A burst of Stellar Fire: a flash of light, a ball of flame tongues blooming outward and rising as it burns out, a ring
 * of light racing over the ground, sparks thrown out, smoke rolling up off it (less the hotter it burns).
 */
public final class FireBurstModel {
    private FireBurstModel() {}

    /** {@code t} (0 to 1) how far through it is, {@code size} its radius, {@code tier} its heat. */
    public static void draw(PoseStack poseStack, MultiBufferSource buffer, View view, float t, float size, int tier, int seed, int light) {
        float bloom = 1 - (1 - t) * (1 - t);
        float fade = 1 - t;
        Vector3f eye = view.eye();
        // The flash and the ring of light racing out over the ground
        VertexConsumer glow = FireMesh.light(buffer, FireMesh.GLOW);
        FireMesh.halo(glow, poseStack, view, 0, size * .5f, 0, size * (2.5f + 2 * bloom), FireMesh.glowOf(tier), fade * fade, light);
        FireMesh.halo(glow, poseStack, view, 0, size * .5f, 0, size * 1.2f, FireMesh.core(tier), Math.max(0, 1 - t * 3), light);
        VertexConsumer ring = FireMesh.light(buffer, FireMesh.RING);
        FireMesh.groundRing(ring, poseStack.last(), .06f, size * (.5f + 2.2f * bloom), FireMesh.glowOf(tier), fade * .9f, light);

        // The ball of flame tongues, blooming out and rising
        VertexConsumer flames = FireMesh.flames(buffer, tier);
        PoseStack.Pose p = poseStack.last();
        for (int k = 0; k < 26; k++) {
            float a = hash(seed + k * 3) * Mth.TWO_PI, b = hash(seed + k * 5) * Mth.PI * .8f;
            float out = size * (.2f + .9f * bloom) * (.5f + .5f * hash(seed + k * 7));
            float x = Mth.cos(a) * Mth.sin(b) * out, z = Mth.sin(a) * Mth.sin(b) * out;
            float y = size * .3f + Mth.cos(b) * out * .6f + size * t * .8f;
            float h = size * (.9f + .7f * hash(seed + k)) * (1 - t * .6f);
            FireMesh.tongue(flames, p, eye, x, y - h * .3f, z, h * .6f, h, (hash(seed + k * 11) - .5f) * .4f, FireMesh.frame(t * 20, k), argb(0xFFFFFF, fade), light);
        }
        // Smoke rolling up off it
        VertexConsumer smoke = WaterMesh.translucent(buffer, FireMesh.SMOKE);
        float soot = Math.max(0, 1 - tier * .2f);
        for (int k = 0; k < 8; k++) {
            float a = hash(seed + k * 13) * Mth.TWO_PI;
            float out = size * .6f * bloom * hash(seed + k * 17);
            puff(smoke, poseStack, view, Mth.cos(a) * out, size * (.6f + 1.4f * t), Mth.sin(a) * out, size * (1 + 1.2f * t), k + t * 2, -1,
                    argb(0xC8C0BC, .45f * soot * Mth.sin(t * Mth.PI)), light);
        }
        // Sparks thrown out
        VertexConsumer sparks = FireMesh.light(buffer, FireMesh.SPARK);
        p = poseStack.last();
        float age = t * 20;
        for (int k = 0; k < 30; k++) {
            float a = hash(seed + k * 19) * Mth.TWO_PI, b = (hash(seed + k * 23) * .9f) * Mth.HALF_PI;
            Vector3f vel = new Vector3f(Mth.cos(a) * Mth.cos(b), Mth.sin(b) + .2f, Mth.sin(a) * Mth.cos(b)).mul(.2f + .25f * hash(seed + k) * size);
            Vector3f from = new Vector3f(0, size * .4f, 0);
            Vector3f head = thrown(from, vel, age, .02f);
            streak(sparks, p, eye, head, thrown(from, vel, Math.max(0, age - 2), .02f), .08f, glow(FireMesh.core(tier), fade), light);
        }
    }
}
