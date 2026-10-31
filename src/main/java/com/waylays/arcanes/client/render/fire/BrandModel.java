package com.waylays.arcanes.client.render.fire;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

import static com.waylays.arcanes.client.render.water.WaterMesh.*;

/**
 * Searing Brand: a sigil of fire burning over a marked creature, a ring of light turning, six spokes of flame meeting at
 * a star in its middle, little tongues licking up round its rim, tightening and blazing brighter as its fuse runs out;
 * flames lick up the creature's sides.
 */
public final class BrandModel {
    private BrandModel() {}

    /** Round the pose's origin (over the creature's head), {@code fuse} (0 to 1) how far gone, {@code tall} how tall the creature is. */
    public static void draw(PoseStack poseStack, MultiBufferSource buffer, View view, float age, float fuse, float tall, int tier, int seed, int light) {
        Vector3f eye = view.eye();
        float r = 1.1f - .45f * fuse;
        float spin = age * (.05f + .25f * fuse * fuse);
        float bright = .5f + .5f * fuse + .2f * Mth.sin(age * (.3f + fuse));
        VertexConsumer ring = FireMesh.light(buffer, FireMesh.RING);
        poseStack.pushPose();
        poseStack.mulPose(new org.joml.Quaternionf().rotationY(spin));
        FireMesh.groundRing(ring, poseStack.last(), 0, r * 1.16f, FireMesh.glowOf(tier), bright, light);
        poseStack.popPose();
        // Six spokes meeting at a star
        VertexConsumer beam = FireMesh.light(buffer, FireMesh.BEAM);
        PoseStack.Pose p = poseStack.last();
        for (int k = 0; k < 6; k++) {
            float a = k * Mth.TWO_PI / 6 + spin;
            Vector3f[] pts = {new Vector3f(), new Vector3f(Mth.cos(a) * r, 0, Mth.sin(a) * r)};
            Vector3f[] side = {new Vector3f(-Mth.sin(a), 0, Mth.cos(a)), new Vector3f(-Mth.sin(a), 0, Mth.cos(a))};
            ribbon(beam, p, pts, side, new float[]{.25f, .08f}, new int[]{glow(FireMesh.core(tier), bright), glow(FireMesh.glowOf(tier), bright * .7f)}, 1, 0,
                    light, true);
        }
        VertexConsumer glow = FireMesh.light(buffer, FireMesh.GLOW);
        FireMesh.halo(glow, poseStack, view, 0, 0, 0, .8f + .8f * fuse, FireMesh.core(tier), bright, light);
        // Tongues round its rim, and licking up the creature
        VertexConsumer flames = FireMesh.flames(buffer, tier);
        p = poseStack.last();
        for (int k = 0; k < 8; k++) {
            float a = k * Mth.TWO_PI / 8 + spin;
            FireMesh.tongue(flames, p, eye, Mth.cos(a) * r, 0, Mth.sin(a) * r, .3f, .45f + .3f * fuse, 0, FireMesh.frame(age, k), argb(0xFFFFFF, .9f), light);
        }
        for (int k = 0; k < 6; k++) {
            float t = (age * .1f + hash(seed + k)) % 1;
            float a = hash(seed + k * 3) * Mth.TWO_PI;
            float h = (.3f + .5f * fuse) * (1 - t * .5f);
            FireMesh.tongue(flames, p, eye, Mth.cos(a) * .35f, -tall + tall * t, Mth.sin(a) * .35f, h * .7f, h, 0, FireMesh.frame(age, k + 3),
                    argb(0xFFFFFF, fuse * (1 - t)), light);
        }
    }
}
