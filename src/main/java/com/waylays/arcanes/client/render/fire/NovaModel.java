package com.waylays.arcanes.client.render.fire;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.waylays.arcanes.client.render.water.WaterMesh;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

import static com.waylays.arcanes.client.render.water.WaterMesh.*;

/**
 * Supernova: all the caster's heat let go at once, a blinding flash, a sphere of flame swelling out from them, rings of
 * light racing over the ground and standing up round it, sparks streaking out in every direction, and for the coolest
 * heats a pall of smoke after.
 */
public final class NovaModel {
    private NovaModel() {}

    /** {@code t} (0 to 1) how far through it is, {@code radius} how far it reaches, at heat {@code tier}. */
    public static void draw(PoseStack poseStack, MultiBufferSource buffer, View view, float t, float radius, int tier, int seed, int light) {
        Vector3f eye = view.eye();
        float bloom = 1 - (1 - t) * (1 - t) * (1 - t);
        float fade = 1 - t;
        float r = radius * bloom;
        VertexConsumer glow = FireMesh.light(buffer, FireMesh.GLOW);
        FireMesh.halo(glow, poseStack, view, 0, 1, 0, r * 3, FireMesh.glowOf(tier), fade, light);
        FireMesh.halo(glow, poseStack, view, 0, 1, 0, radius * 1.6f, FireMesh.core(tier), Math.max(0, 1 - t * 4), light);
        VertexConsumer ring = FireMesh.light(buffer, FireMesh.RING);
        FireMesh.groundRing(ring, poseStack.last(), .06f, r * 1.25f, FireMesh.glowOf(tier), fade, light);
        FireMesh.groundRing(ring, poseStack.last(), .07f, r * .8f, FireMesh.core(tier), fade * .6f, light);
        // A ring standing up round it, facing the eye
        poseStack.pushPose();
        poseStack.translate(0, 1, 0);
        poseStack.mulPose(view.camera());
        PoseStack.Pose rp = poseStack.last();
        int c = glow(FireMesh.glowOf(tier), fade * .8f);
        float q = r * 1.2f;
        com.waylays.arcanes.client.render.RenderHelper.vertex(ring, rp, -q, -q, 0, 0, 1, c, light, 0, 1, 0);
        com.waylays.arcanes.client.render.RenderHelper.vertex(ring, rp, q, -q, 0, 1, 1, c, light, 0, 1, 0);
        com.waylays.arcanes.client.render.RenderHelper.vertex(ring, rp, q, q, 0, 1, 0, c, light, 0, 1, 0);
        com.waylays.arcanes.client.render.RenderHelper.vertex(ring, rp, -q, q, 0, 0, 0, c, light, 0, 1, 0);
        poseStack.popPose();
        // The sphere of flame
        FireMesh.Batch fire = new FireMesh.Batch(view, tier, t * 30, light);
        PoseStack.Pose p = poseStack.last();
        for (int k = 0; k < 60; k++) {
            float a = hash(seed + k * 3) * Mth.TWO_PI, b = (float) Math.acos(1 - 2 * hash(seed + k * 5));
            float rr = r * (.75f + .3f * hash(seed + k * 7));
            float x = Mth.cos(a) * Mth.sin(b) * rr, y = 1 + Mth.cos(b) * rr * .7f, z = Mth.sin(a) * Mth.sin(b) * rr;
            if (y < -.3f) {
                continue;
            }
            float h = (1.2f + 1.4f * hash(seed + k)) * (.5f + radius * .1f) * fade;
            fire.lick(x, Math.max(0, y - h * .4f), z, h * .7f, h, 0, seed + 31 * (k), Math.min(1, fade * 1.5f));
        }
        // Smoke after, for the coolest heats
        VertexConsumer smoke = WaterMesh.translucent(buffer, FireMesh.SMOKE);
        float soot = Math.max(0, 1 - tier * .25f);
        for (int k = 0; k < 10; k++) {
            float a = hash(seed + k * 11) * Mth.TWO_PI;
            puff(smoke, poseStack, view, Mth.cos(a) * r * .7f, 1 + r * .4f + 2 * t, Mth.sin(a) * r * .7f, r * .9f + 1, k, -1,
                    argb(0xC8C0BC, .4f * soot * Mth.sin(t * Mth.PI)), light);
        }
        // Sparks streaking out every way
        VertexConsumer sparks = FireMesh.light(buffer, FireMesh.SPARK);
        p = poseStack.last();
        for (int k = 0; k < 50; k++) {
            float a = hash(seed + k * 13) * Mth.TWO_PI, b = (float) Math.acos(1 - 2 * hash(seed + k * 17));
            Vector3f dir = new Vector3f(Mth.cos(a) * Mth.sin(b), Math.abs(Mth.cos(b)) * .8f, Mth.sin(a) * Mth.sin(b));
            Vector3f head = new Vector3f(dir).mul(r * 1.3f).add(0, 1, 0);
            streak(sparks, p, eye, head, new Vector3f(dir).mul(r * 1.3f - 1.2f).add(0, 1, 0), .12f, glow(FireMesh.core(tier), fade), light);
        }
        fire.flush(buffer, poseStack);
    }
}
