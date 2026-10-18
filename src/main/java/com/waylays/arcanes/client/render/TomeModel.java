package com.waylays.arcanes.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.waylays.arcanes.ArcanesOublies;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

/**
 * The Book school's tomes, built from boxes: two halves hinged on a spine running along Z, each a page block under a
 * cover a little larger than the pages, and the open page with its glowing runes. Drawn centred on the spine, the
 * halves hanging down at {@code angle} 0 (closed) and lying flat at 90 (wide open, pages facing down).
 * Size before scaling: {@link #W} page width (spine to edge), {@link #H} page height (along the spine).
 */
final class TomeModel {
    static final ResourceLocation COVER = ArcanesOublies.id("textures/entity/tome_cover.png");
    static final ResourceLocation EDGE = ArcanesOublies.id("textures/entity/tome_edge.png");

    static final float W = 3.4f;
    static final float H = 4.6f;
    private static final float PAGES = .45f;
    private static final float COVER_T = .2f;

    private TomeModel() {}

    static void draw(MultiBufferSource buffer, PoseStack poseStack, ResourceLocation cover, float angle, float glow, int light) {
        drawHalf(buffer, poseStack, cover, 1, angle, glow, light);
        drawHalf(buffer, poseStack, cover, -1, angle, glow, light);
        float t = PAGES + COVER_T;
        box(buffer.getBuffer(RenderType.entityCutoutNoCull(cover)), poseStack.last(), -t * .7f, -.05f, -H * .5f - .15f, t * .7f, .35f, H * .5f + .15f, 0xFF8A80A0, light);
    }

    /** One half of the book, hinged on the spine, {@code side} = +1 or -1. */
    private static void drawHalf(MultiBufferSource buffer, PoseStack poseStack, ResourceLocation cover, int side, float angle, float glow, int light) {
        poseStack.pushPose();
        poseStack.mulPose(Axis.ZP.rotationDegrees(side * angle));
        float s = side;
        // Page block
        VertexConsumer edge = buffer.getBuffer(RenderType.entityCutoutNoCull(EDGE));
        box(edge, poseStack.last(), Math.min(0, s * PAGES), -W, -H * .5f, Math.max(0, s * PAGES), 0, H * .5f, 0xFFFFFFFF, light);
        // Cover, a little larger than the pages
        float c0 = s * PAGES;
        float c1 = s * (PAGES + COVER_T);
        box(buffer.getBuffer(RenderType.entityCutoutNoCull(cover)), poseStack.last(), Math.min(c0, c1), -W - .15f, -H * .5f - .15f, Math.max(c0, c1), .1f, H * .5f + .15f, 0xFFFFFFFF, light);
        // The open page, with its glowing runes
        if (angle > 1) {
            poseStack.pushPose();
            poseStack.translate(-s * .01f, -W * .5f, 0);
            poseStack.mulPose(Axis.YP.rotationDegrees(side > 0 ? -90f : 90f));
            PageStormRenderer.drawPage(buffer, poseStack, H * .94f, W * .94f, light, glow);
            poseStack.popPose();
        }
        poseStack.popPose();
    }

    /** An axis-aligned box, the whole texture on each face. */
    static void box(VertexConsumer c, PoseStack.Pose pose, float x0, float y0, float z0, float x1, float y1, float z1, int argb, int light) {
        // -Y, +Y
        face(c, pose, x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1, argb, light, 0, -1, 0);
        face(c, pose, x0, y1, z1, x1, y1, z1, x1, y1, z0, x0, y1, z0, argb, light, 0, 1, 0);
        // -Z, +Z
        face(c, pose, x1, y0, z0, x0, y0, z0, x0, y1, z0, x1, y1, z0, argb, light, 0, 0, -1);
        face(c, pose, x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1, argb, light, 0, 0, 1);
        // -X, +X
        face(c, pose, x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0, argb, light, -1, 0, 0);
        face(c, pose, x1, y0, z1, x1, y0, z0, x1, y1, z0, x1, y1, z1, argb, light, 1, 0, 0);
    }

    private static void face(VertexConsumer c, PoseStack.Pose pose, float ax, float ay, float az, float bx, float by, float bz,
                             float cx, float cy, float cz, float dx, float dy, float dz, int argb, int light, float nx, float ny, float nz) {
        RenderHelper.vertex(c, pose, ax, ay, az, 0, 1, argb, light, nx, ny, nz);
        RenderHelper.vertex(c, pose, bx, by, bz, 1, 1, argb, light, nx, ny, nz);
        RenderHelper.vertex(c, pose, cx, cy, cz, 1, 0, argb, light, nx, ny, nz);
        RenderHelper.vertex(c, pose, dx, dy, dz, 0, 0, argb, light, nx, ny, nz);
    }
}
