package com.waylays.arcanes.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

/**
 * Small vertex helpers for spell renderers (entity vertex format: position, color, uv, overlay, light, normal).
 */
public final class RenderHelper {
    private RenderHelper() {}

    public static void vertex(VertexConsumer consumer, PoseStack.Pose pose, float x, float y, float z, float u, float v,
                              int argb, int light, float nx, float ny, float nz) {
        Matrix4f matrix = pose.pose();
        Matrix3f normal = pose.normal();
        consumer.vertex(matrix, x, y, z)
                .color((argb >> 16) & 0xFF, (argb >> 8) & 0xFF, argb & 0xFF, (argb >>> 24) & 0xFF)
                .uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(light)
                .normal(normal, nx, ny, nz)
                .endVertex();
    }

    /** Square in the XY plane centred on the origin, facing +Z. */
    public static void billboard(VertexConsumer consumer, PoseStack.Pose pose, float half, int argb, int light) {
        vertex(consumer, pose, -half, -half, 0, 0, 1, argb, light, 0, 0, 1);
        vertex(consumer, pose, half, -half, 0, 1, 1, argb, light, 0, 0, 1);
        vertex(consumer, pose, half, half, 0, 1, 0, argb, light, 0, 0, 1);
        vertex(consumer, pose, -half, half, 0, 0, 0, argb, light, 0, 0, 1);
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
