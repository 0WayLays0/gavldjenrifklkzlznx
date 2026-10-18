package com.waylays.arcanes.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.waylays.arcanes.ArcanesOublies;
import com.waylays.arcanes.entity.book.FinalChapter;
import com.waylays.arcanes.spells.book.FinalChapterSpell;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * The Final Chapter: a colossal grimoire hanging open above the area, its pages facing the ground; pages flying out
 * of it one by one to settle into a dome, a circle of runes on the ground under it; at the end the book slams shut
 * and the dome blows away.
 */
public class FinalChapterRenderer extends EntityRenderer<FinalChapter> {
    private static final ResourceLocation COVER = ArcanesOublies.id("textures/entity/tome_cover.png");
    private static final ResourceLocation EDGE = ArcanesOublies.id("textures/entity/tome_edge.png");
    private static final ResourceLocation CIRCLE = ArcanesOublies.id("textures/entity/ruin_sigil.png");

    /** Book size: page width (spine to edge), page height (along the spine), page block and cover thickness. */
    private static final float W = 3.4f;
    private static final float H = 4.6f;
    private static final float PAGES = .45f;
    private static final float COVER_T = .2f;
    private static final float OPEN_DEGREES = 68f;

    public FinalChapterRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(FinalChapter entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        float age = entity.tickCount + partialTicks;
        float appear = ease(Mth.clamp(age / FinalChapter.APPEAR_TICKS, 0f, 1f));
        float open = ease(Mth.clamp((age - FinalChapter.APPEAR_TICKS) / (FinalChapter.OPEN_TICKS - FinalChapter.APPEAR_TICKS), 0f, 1f));
        float slam = Mth.clamp((age - FinalChapter.CLOSE_TICK) / FinalChapter.SLAM_TICKS, 0f, 1f);
        float after = Mth.clamp((age - FinalChapter.CLOSE_TICK - FinalChapter.SLAM_TICKS) / (FinalChapter.END_TICK - FinalChapter.CLOSE_TICK - FinalChapter.SLAM_TICKS), 0f, 1f);
        float angle = OPEN_DEGREES * open * (1 - slam * slam);
        float scale = appear * (1 - after);
        // Runes burn brightest while the marks are on
        float marking = Mth.clamp((age - FinalChapter.MARK_TICK + 10) / 10f, 0f, 1f) * (1 - slam);
        float glow = Mth.clamp(.35f + .4f * open + .3f * marking + Mth.sin(age * .3f) * .08f, 0f, 1f) * (1 - after);

        if (scale > 0) {
            poseStack.pushPose();
            poseStack.translate(0, Mth.sin(age * .08f) * .15f, 0);
            poseStack.mulPose(Axis.YP.rotationDegrees(-entity.getYRot()));
            poseStack.scale(scale, scale, scale);
            drawHalf(buffer, poseStack, 1, angle, glow, packedLight);
            drawHalf(buffer, poseStack, -1, angle, glow, packedLight);
            // Spine
            float t = PAGES + COVER_T;
            drawBox(buffer.getBuffer(RenderType.entityCutoutNoCull(COVER)), poseStack.last(), -t * .7f, -.05f, -H * .5f - .15f, t * .7f, .35f, H * .5f + .15f, 0xFF8A80A0, packedLight);
            poseStack.popPose();
        }

        drawDome(entity, buffer, poseStack, age, glow, packedLight);
        drawCircle(entity, buffer, poseStack, age, open * (1 - slam));
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    /** One half of the open book, hinged on the spine (the Z axis), {@code side} = +1 or -1. */
    private void drawHalf(MultiBufferSource buffer, PoseStack poseStack, int side, float angle, float glow, int light) {
        poseStack.pushPose();
        poseStack.mulPose(Axis.ZP.rotationDegrees(side * angle));
        float s = side;
        // Page block
        VertexConsumer edge = buffer.getBuffer(RenderType.entityCutoutNoCull(EDGE));
        drawBox(edge, poseStack.last(), Math.min(0, s * PAGES), -W, -H * .5f, Math.max(0, s * PAGES), 0, H * .5f, 0xFFFFFFFF, light);
        // Cover, a little larger than the pages
        float c0 = s * PAGES;
        float c1 = s * (PAGES + COVER_T);
        drawBox(buffer.getBuffer(RenderType.entityCutoutNoCull(COVER)), poseStack.last(), Math.min(c0, c1), -W - .15f, -H * .5f - .15f, Math.max(c0, c1), .1f, H * .5f + .15f, 0xFFFFFFFF, light);
        // The open page facing the ground, with its glowing runes
        poseStack.pushPose();
        poseStack.translate(-s * .01f, -W * .5f, 0);
        poseStack.mulPose(Axis.YP.rotationDegrees(side > 0 ? -90f : 90f));
        PageStormRenderer.drawPage(buffer, poseStack, H * .94f, W * .94f, light, glow);
        poseStack.popPose();
        poseStack.popPose();
    }

    /** The pages flying out of the book and settling into a dome over the area. */
    private void drawDome(FinalChapter entity, MultiBufferSource buffer, PoseStack poseStack, float age, float glow, int light) {
        float radius = FinalChapterSpell.RADIUS;
        float height = entity.getHeight();
        float scatter = Mth.clamp((age - FinalChapter.CLOSE_TICK - FinalChapter.SLAM_TICKS) / (FinalChapter.END_TICK - FinalChapter.CLOSE_TICK - FinalChapter.SLAM_TICKS), 0f, 1f);
        if (scatter >= 1) {
            return;
        }
        for (int i = 0; i < FinalChapter.DOME_PAGES; i++) {
            float t = Mth.clamp((age - FinalChapter.PAGES_START - i * FinalChapter.PAGE_INTERVAL) / FinalChapter.FLY_TICKS, 0f, 1f);
            if (t <= 0) {
                break;
            }
            float e = ease(t);
            // Evenly spread over the upper hemisphere (golden angle spiral, uniform in height)
            float yFrac = 1 - (i + .5f) / FinalChapter.DOME_PAGES;
            float y = radius * yFrac * .95f + .4f;
            float flat = Mth.sqrt(Math.max(0, radius * radius - y * y));
            float az = i * 2.39996f;
            float nx = Mth.cos(az) * flat / radius;
            float nz = Mth.sin(az) * flat / radius;
            float ny = y / radius;
            float dx = nx * radius;
            float dz = nz * radius;
            float dy = y - height;
            // Out of the open pages, along an arc
            float sx = Mth.sin(az) * .6f;
            float sy = -W * .6f;
            float sz = Mth.cos(az) * 1.2f;
            float px = Mth.lerp(e, sx, dx) * (1 + scatter * .5f);
            float py = Mth.lerp(e, sy, dy) + Mth.sin(t * Mth.PI) * 2f + scatter * 3f;
            float pz = Mth.lerp(e, sz, dz) * (1 + scatter * .5f);
            float size = 1 - scatter;
            poseStack.pushPose();
            poseStack.translate(px, py, pz);
            poseStack.mulPose(Axis.YP.rotation((float) Math.atan2(nx, nz)));
            poseStack.mulPose(Axis.XP.rotation((float) -Math.asin(Mth.clamp(ny, -1f, 1f))));
            // Tumbling in flight, fluttering once in place
            poseStack.mulPose(Axis.ZP.rotationDegrees((1 - e) * 540f + i * 13f % 20f));
            poseStack.mulPose(Axis.XP.rotationDegrees((1 - e) * 200f + Mth.sin(age * .2f + i) * 7f));
            poseStack.scale(size, size, size);
            PageStormRenderer.drawPage(buffer, poseStack, 1.3f, 1.75f, light, glow);
            poseStack.popPose();
        }
    }

    /** A circle of runes on the ground at the foot of the dome. */
    private void drawCircle(FinalChapter entity, MultiBufferSource buffer, PoseStack poseStack, float age, float strength) {
        if (strength <= 0) {
            return;
        }
        float radius = FinalChapterSpell.RADIUS;
        poseStack.pushPose();
        poseStack.translate(0, -entity.getHeight() + .05f, 0);
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(age * .6f));
        RenderHelper.groundQuad(buffer.getBuffer(RenderType.eyes(CIRCLE)), poseStack.last(), radius * strength, BlastEffectRenderer.rgb(.6f * strength, .36f * strength, strength), LightTexture.FULL_BRIGHT);
        poseStack.popPose();
        poseStack.translate(0, .02f, 0);
        poseStack.mulPose(Axis.YP.rotationDegrees(-age * 1.1f));
        RenderHelper.groundQuad(buffer.getBuffer(RenderType.eyes(CIRCLE)), poseStack.last(), radius * .5f * strength, BlastEffectRenderer.rgb(strength, .84f * strength, .45f * strength), LightTexture.FULL_BRIGHT);
        poseStack.popPose();
    }

    private static float ease(float t) {
        return 1 - (1 - t) * (1 - t) * (1 - t);
    }

    /** An axis-aligned box, the whole texture on each face. */
    private static void drawBox(VertexConsumer c, PoseStack.Pose pose, float x0, float y0, float z0, float x1, float y1, float z1, int argb, int light) {
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

    @Override
    public ResourceLocation getTextureLocation(FinalChapter entity) {
        return COVER;
    }
}
