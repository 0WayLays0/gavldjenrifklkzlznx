package com.waylays.arcanes.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.waylays.arcanes.ArcanesOublies;
import com.waylays.arcanes.entity.book.BookmarkMarker;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * Bookmark's marker: a tall silk ribbon with a gold clip, its embroidered runes glowing, swaying as if in a breeze
 * above a slowly turning rune circle. When it fades it rises and shrinks away.
 */
public class BookmarkRenderer extends EntityRenderer<BookmarkMarker> {
    private static final ResourceLocation RIBBON = ArcanesOublies.id("textures/entity/bookmark.png");
    private static final ResourceLocation RIBBON_GLOW = ArcanesOublies.id("textures/entity/bookmark_glow.png");
    private static final ResourceLocation CIRCLE = ArcanesOublies.id("textures/entity/chapter_rune.png");
    private static final float WIDTH = .44f;
    private static final float HEIGHT = 2.2f;
    private static final int SEGMENTS = 8;

    public BookmarkRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(BookmarkMarker entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        float age = entity.tickCount + partialTicks;
        float appear = Mth.clamp(age / 8f, 0f, 1f);
        float fadeAge = entity.fadeAge(partialTicks);
        float fade = fadeAge < 0 ? 0 : Mth.clamp(fadeAge / BookmarkMarker.FADE_TICKS, 0f, 1f);
        float size = appear * (1 - fade);
        if (size <= 0) {
            return;
        }
        float glow = (.75f + .25f * Mth.sin(age * .2f)) * (1 - fade);

        // Rune circle on the ground
        poseStack.pushPose();
        poseStack.translate(0, .04, 0);
        poseStack.mulPose(Axis.YP.rotationDegrees(age * 1.5f));
        RenderHelper.groundQuad(buffer.getBuffer(RenderType.eyes(CIRCLE)), poseStack.last(), .95f * size,
                BlastEffectRenderer.rgb(.6f * glow, .36f * glow, glow), LightTexture.FULL_BRIGHT);
        poseStack.popPose();

        // The ribbon, turned to the camera around the vertical
        poseStack.pushPose();
        poseStack.translate(0, .35 + Mth.sin(age * .08f) * .06f + fade * 1.2f, 0);
        poseStack.mulPose(Axis.YP.rotationDegrees(180f - this.entityRenderDispatcher.camera.getYRot()));
        poseStack.scale(size, size, size);
        ribbon(buffer.getBuffer(RenderType.entityCutoutNoCull(RIBBON)), poseStack.last(), age, 0xFFFFFFFF, packedLight, false);
        int g = (int) (Mth.clamp(glow, 0f, 1f) * 255);
        ribbon(buffer.getBuffer(RenderType.eyes(RIBBON_GLOW)), poseStack.last(), age, 0xFF000000 | g << 16 | g << 8 | g, LightTexture.FULL_BRIGHT, true);
        poseStack.popPose();
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    /** A vertical strip hanging from its top, rippling more toward the loose end. */
    private static void ribbon(VertexConsumer c, PoseStack.Pose pose, float age, int argb, int light, boolean twoSided) {
        float w = WIDTH * .5f;
        float seg = HEIGHT / SEGMENTS;
        for (int k = 0; k < SEGMENTS; k++) {
            float y0 = k * seg;
            float y1 = y0 + seg;
            float o0 = sway(age, y0);
            float o1 = sway(age, y1);
            float v0 = 1 - y0 / HEIGHT;
            float v1 = 1 - y1 / HEIGHT;
            RenderHelper.vertex(c, pose, -w + o0, y0, 0, 0, v0, argb, light, 0, 0, 1);
            RenderHelper.vertex(c, pose, w + o0, y0, 0, 1, v0, argb, light, 0, 0, 1);
            RenderHelper.vertex(c, pose, w + o1, y1, 0, 1, v1, argb, light, 0, 0, 1);
            RenderHelper.vertex(c, pose, -w + o1, y1, 0, 0, v1, argb, light, 0, 0, 1);
            if (twoSided) {
                RenderHelper.vertex(c, pose, -w + o1, y1, 0, 0, v1, argb, light, 0, 0, -1);
                RenderHelper.vertex(c, pose, w + o1, y1, 0, 1, v1, argb, light, 0, 0, -1);
                RenderHelper.vertex(c, pose, w + o0, y0, 0, 1, v0, argb, light, 0, 0, -1);
                RenderHelper.vertex(c, pose, -w + o0, y0, 0, 0, v0, argb, light, 0, 0, -1);
            }
        }
    }

    private static float sway(float age, float y) {
        float loose = 1 - y / HEIGHT;
        return Mth.sin(age * .14f - y * 1.7f) * .14f * loose * loose + Mth.sin(age * .31f - y * 3f) * .03f * loose;
    }

    @Override
    public ResourceLocation getTextureLocation(BookmarkMarker entity) {
        return RIBBON;
    }
}
