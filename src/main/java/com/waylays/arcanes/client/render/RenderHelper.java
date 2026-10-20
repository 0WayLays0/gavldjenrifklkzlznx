package com.waylays.arcanes.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.phys.Vec3;

/**
 * Small vertex helpers for spell renderers (entity vertex format: position, color, uv, overlay, light, normal).
 */
public final class RenderHelper {
    private RenderHelper() {}

    public static void vertex(VertexConsumer consumer, PoseStack.Pose pose, float x, float y, float z, float u, float v,
                              int argb, int light, float nx, float ny, float nz) {
        consumer.addVertex(pose, x, y, z)
                .setColor((argb >> 16) & 0xFF, (argb >> 8) & 0xFF, argb & 0xFF, (argb >>> 24) & 0xFF)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light)
                .setNormal(pose, nx, ny, nz);
    }

    /** Square in the XY plane centred on the origin, facing +Z. */
    public static void billboard(VertexConsumer consumer, PoseStack.Pose pose, float half, int argb, int light) {
        vertex(consumer, pose, -half, -half, 0, 0, 1, argb, light, 0, 0, 1);
        vertex(consumer, pose, half, -half, 0, 1, 1, argb, light, 0, 0, 1);
        vertex(consumer, pose, half, half, 0, 1, 0, argb, light, 0, 0, 1);
        vertex(consumer, pose, -half, half, 0, 0, 0, argb, light, 0, 0, 1);
    }

    /** Rectangle {@code width} x {@code height} in the XY plane centred on the origin, facing +Z (and -Z if {@code twoSided}). */
    public static void rect(VertexConsumer consumer, PoseStack.Pose pose, float width, float height, int argb, int light, boolean twoSided) {
        rectUv(consumer, pose, width, height, 0, 0, 1, 1, argb, light, twoSided);
    }

    /** Like {@link #rect} with a part of the texture: (u0, v0) top left to (u1, v1) bottom right. */
    public static void rectUv(VertexConsumer consumer, PoseStack.Pose pose, float width, float height, float u0, float v0, float u1, float v1,
                              int argb, int light, boolean twoSided) {
        float w = width * .5f;
        float h = height * .5f;
        vertex(consumer, pose, -w, -h, 0, u0, v1, argb, light, 0, 0, 1);
        vertex(consumer, pose, w, -h, 0, u1, v1, argb, light, 0, 0, 1);
        vertex(consumer, pose, w, h, 0, u1, v0, argb, light, 0, 0, 1);
        vertex(consumer, pose, -w, h, 0, u0, v0, argb, light, 0, 0, 1);
        if (twoSided) {
            vertex(consumer, pose, -w, h, 0, u0, v0, argb, light, 0, 0, -1);
            vertex(consumer, pose, w, h, 0, u1, v0, argb, light, 0, 0, -1);
            vertex(consumer, pose, w, -h, 0, u1, v1, argb, light, 0, 0, -1);
            vertex(consumer, pose, -w, -h, 0, u0, v1, argb, light, 0, 0, -1);
        }
    }

    /** Square in the XZ plane centred on the origin, facing up. */
    public static void groundQuad(VertexConsumer consumer, PoseStack.Pose pose, float half, int argb, int light) {
        vertex(consumer, pose, -half, 0, -half, 0, 0, argb, light, 0, 1, 0);
        vertex(consumer, pose, -half, 0, half, 0, 1, argb, light, 0, 1, 0);
        vertex(consumer, pose, half, 0, half, 1, 1, argb, light, 0, 1, 0);
        vertex(consumer, pose, half, 0, -half, 1, 0, argb, light, 0, 1, 0);
    }

    /**
     * A textured strand from {@code from} to {@code to}, drawn as two crossed quads so it reads from every angle.
     * The texture repeats every {@code tileLength} blocks along the strand (one tile = 0..1 in V).
     */
    public static void strand(VertexConsumer consumer, PoseStack.Pose pose, Vec3 from, Vec3 to, float width, float tileLength,
                              int argb, int light) {
        Vec3 delta = to.subtract(from);
        double length = delta.length();
        if (length < 1.0E-4) {
            return;
        }
        Vec3 dir = delta.scale(1 / length);
        Vec3 side = dir.cross(new Vec3(0, 1, 0));
        if (side.lengthSqr() < 1.0E-6) {
            side = new Vec3(1, 0, 0);
        }
        side = side.normalize();
        Vec3 side2 = dir.cross(side).normalize();
        float half = width * .5f;
        for (double start = 0; start < length; start += tileLength) {
            double end = Math.min(length, start + tileLength);
            float vEnd = (float) ((end - start) / tileLength);
            Vec3 a = from.add(dir.scale(start));
            Vec3 b = from.add(dir.scale(end));
            crossQuad(consumer, pose, a, b, side.scale(half), side2, vEnd, argb, light);
            crossQuad(consumer, pose, a, b, side2.scale(half), side, vEnd, argb, light);
        }
    }

    private static void crossQuad(VertexConsumer consumer, PoseStack.Pose pose, Vec3 a, Vec3 b, Vec3 offset, Vec3 normal,
                                  float vEnd, int argb, int light) {
        float nx = (float) normal.x;
        float ny = (float) normal.y;
        float nz = (float) normal.z;
        vertex(consumer, pose, (float) (a.x - offset.x), (float) (a.y - offset.y), (float) (a.z - offset.z), 0, 0, argb, light, nx, ny, nz);
        vertex(consumer, pose, (float) (a.x + offset.x), (float) (a.y + offset.y), (float) (a.z + offset.z), 1, 0, argb, light, nx, ny, nz);
        vertex(consumer, pose, (float) (b.x + offset.x), (float) (b.y + offset.y), (float) (b.z + offset.z), 1, vEnd, argb, light, nx, ny, nz);
        vertex(consumer, pose, (float) (b.x - offset.x), (float) (b.y - offset.y), (float) (b.z - offset.z), 0, vEnd, argb, light, nx, ny, nz);
    }
}
