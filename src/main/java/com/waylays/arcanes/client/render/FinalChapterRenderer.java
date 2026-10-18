package com.waylays.arcanes.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
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
    private static final ResourceLocation CIRCLE = ArcanesOublies.id("textures/entity/ruin_sigil.png");

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
            TomeModel.draw(buffer, poseStack, TomeModel.COVER, angle, glow, packedLight);
            poseStack.popPose();
        }

        drawDome(entity, buffer, poseStack, age, glow, packedLight);
        drawCircle(entity, buffer, poseStack, age, open * (1 - slam));
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
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
            float sy = -TomeModel.W * .6f;
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

    @Override
    public ResourceLocation getTextureLocation(FinalChapter entity) {
        return TomeModel.COVER;
    }
}
