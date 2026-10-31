package com.mojang.blaze3d.vertex;

/** Render lab stand-in for Minecraft's VertexConsumer (the 1.21 builder calls the addon uses). */
public interface VertexConsumer {
    VertexConsumer addVertex(float x, float y, float z);

    VertexConsumer setColor(int r, int g, int b, int a);

    VertexConsumer setUv(float u, float v);

    VertexConsumer setUv1(int u, int v);

    VertexConsumer setUv2(int u, int v);

    VertexConsumer setNormal(float x, float y, float z);

    default VertexConsumer addVertex(PoseStack.Pose pose, float x, float y, float z) {
        org.joml.Vector3f v = pose.pose().transformPosition(x, y, z, new org.joml.Vector3f());
        return addVertex(v.x, v.y, v.z);
    }

    default VertexConsumer addVertex(org.joml.Matrix4f m, float x, float y, float z) {
        org.joml.Vector3f v = m.transformPosition(x, y, z, new org.joml.Vector3f());
        return addVertex(v.x, v.y, v.z);
    }

    default VertexConsumer setColor(float r, float g, float b, float a) {
        return setColor((int) (r * 255), (int) (g * 255), (int) (b * 255), (int) (a * 255));
    }

    default VertexConsumer setColor(int argb) {
        return setColor((argb >> 16) & 0xFF, (argb >> 8) & 0xFF, argb & 0xFF, (argb >>> 24) & 0xFF);
    }

    default VertexConsumer setOverlay(int overlay) {
        return setUv1(overlay & 0xFFFF, overlay >> 16 & 0xFFFF);
    }

    default VertexConsumer setLight(int light) {
        return setUv2(light & 0xFFFF, light >> 16 & 0xFFFF);
    }

    default VertexConsumer setNormal(PoseStack.Pose pose, float x, float y, float z) {
        org.joml.Vector3f n = pose.normal().transform(x, y, z, new org.joml.Vector3f());
        return setNormal(n.x, n.y, n.z);
    }
}
