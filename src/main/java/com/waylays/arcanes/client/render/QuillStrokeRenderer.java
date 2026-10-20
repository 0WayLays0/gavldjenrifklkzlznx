package com.waylays.arcanes.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.waylays.arcanes.ArcanesOublies;
import com.waylays.arcanes.entity.book.QuillStroke;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * Quill of Fate: a huge ghostly quill sweeps along the ground writing a line of glossy ink in cursive strokes; the
 * runes in the ink light up, then pillars of rune light burst out of the writing, one after the other along it.
 */
public class QuillStrokeRenderer extends EntityRenderer<QuillStroke> {
    private static final ResourceLocation INK = ArcanesOublies.id("textures/entity/ink_stroke.png");
    private static final ResourceLocation INK_RUNES = ArcanesOublies.id("textures/entity/ink_stroke_runes.png");
    private static final ResourceLocation QUILL = ArcanesOublies.id("textures/entity/quill.png");
    private static final ResourceLocation PILLAR = ArcanesOublies.id("textures/entity/rune_pillar.png");
    private static final float INK_WIDTH = .8f;
    /** Blocks of line per texture tile. */
    private static final float TILE = 2f;

    public QuillStrokeRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(QuillStroke entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        float age = entity.tickCount + partialTicks;
        float length = entity.getLength();
        float written = length * QuillStroke.progress(age);
        float fade = Mth.clamp((age - QuillStroke.ERUPT_TICK - 8) / (QuillStroke.END_TICK - QuillStroke.ERUPT_TICK - 8), 0f, 1f);
        float runes = age < QuillStroke.WRITE_TICKS ? .3f : Mth.clamp(.3f + (age - QuillStroke.WRITE_TICKS) / (QuillStroke.ERUPT_TICK - QuillStroke.WRITE_TICKS) * .7f, 0f, 1f);
        if (age >= QuillStroke.ERUPT_TICK) {
            runes = 1 - fade;
        }

        poseStack.pushPose();
        // Local +Z along the line
        poseStack.mulPose(Axis.YP.rotationDegrees(-entity.getYRot()));
        if (written > .05f && fade < 1) {
            int alpha = (int) (255 * (1 - fade));
            strip(buffer.getBuffer(RenderType.entityTranslucent(INK)), poseStack.last(), written, .03f, (alpha << 24) | 0xFFFFFF, packedLight);
            int r = (int) (255 * runes);
            strip(buffer.getBuffer(RenderType.eyes(INK_RUNES)), poseStack.last(), written, .04f, 0xFF000000 | r << 16 | r << 8 | r, LightTexture.FULL_BRIGHT);
        }

        // The quill: nib on the line, feather leaning back, swaying as it writes, then lifting away
        float lift = Mth.clamp((age - QuillStroke.WRITE_TICKS) / 8f, 0f, 1f);
        if (lift < 1) {
            poseStack.pushPose();
            float wiggle = Mth.sin(age * 1.3f) * .3f * (1 - lift);
            poseStack.translate(wiggle, lift * 2.5f, written);
            poseStack.mulPose(Axis.YP.rotationDegrees(Mth.sin(age * .9f) * 15f));
            poseStack.mulPose(Axis.XP.rotationDegrees(-32f - lift * 20f));
            float s = 1 - lift;
            int q = (int) (255 * (.9f + .1f * Mth.sin(age * .7f)) * s);
            int color = 0xFF000000 | q << 16 | q << 8 | q;
            VertexConsumer quill = buffer.getBuffer(RenderType.eyes(QUILL));
            // Two crossed planes so it reads from every side, nib at the origin
            for (int k = 0; k < 2; k++) {
                poseStack.pushPose();
                poseStack.mulPose(Axis.YP.rotationDegrees(k * 90f));
                poseStack.translate(0, 1.2f, 0);
                RenderHelper.rect(quill, poseStack.last(), .9f, 2.4f, color, LightTexture.FULL_BRIGHT, true);
                poseStack.popPose();
            }
            poseStack.popPose();
        }

        // Rune pillars bursting out one after the other along the line
        if (age >= QuillStroke.ERUPT_TICK) {
            int count = entity.pillarCount();
            for (int i = 0; i < count; i++) {
                float t = Mth.clamp((age - QuillStroke.ERUPT_TICK - i * .35f) / 4f, 0f, 1f);
                if (t <= 0) {
                    continue;
                }
                float rise = 1 - (1 - t) * (1 - t);
                float glow = (1 - fade) * (1 - fade);
                int g = (int) (255 * glow);
                int color = 0xFF000000 | g << 16 | g << 8 | g;
                float height = 3.6f * rise * (1 + .15f * Mth.sin(i * 1.7f));
                poseStack.pushPose();
                poseStack.translate(0, 0, Math.min(length, i * QuillStroke.PILLAR_SPACING));
                poseStack.mulPose(Axis.YP.rotationDegrees(i * 37f + age * 3f));
                // Asked for again each time: the ring below switches the buffer away
                VertexConsumer pillar = buffer.getBuffer(RenderType.eyes(PILLAR));
                for (int k = 0; k < 2; k++) {
                    poseStack.pushPose();
                    poseStack.mulPose(Axis.YP.rotationDegrees(k * 90f));
                    poseStack.translate(0, height * .5f, 0);
                    RenderHelper.rect(pillar, poseStack.last(), 1.1f * (1 - fade * .5f), height, color, LightTexture.FULL_BRIGHT, true);
                    poseStack.popPose();
                }
                poseStack.translate(0, .06f, 0);
                RenderHelper.groundQuad(buffer.getBuffer(RenderType.eyes(BlastEffectRenderer.RING)), poseStack.last(), .6f + rise * .9f,
                        BlastEffectRenderer.rgb(.6f * glow, .36f * glow, glow), LightTexture.FULL_BRIGHT);
                poseStack.popPose();
            }
        }
        poseStack.popPose();
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    /** The ink on the ground from the start of the line to {@code length}, the texture tiling along it. */
    private static void strip(VertexConsumer c, PoseStack.Pose pose, float length, float y, int argb, int light) {
        float w = INK_WIDTH * .5f;
        for (float start = 0; start < length; start += TILE) {
            float end = Math.min(length, start + TILE);
            float v = (end - start) / TILE;
            RenderHelper.vertex(c, pose, -w, y, start, 0, 0, argb, light, 0, 1, 0);
            RenderHelper.vertex(c, pose, -w, y, end, 0, v, argb, light, 0, 1, 0);
            RenderHelper.vertex(c, pose, w, y, end, 1, v, argb, light, 0, 1, 0);
            RenderHelper.vertex(c, pose, w, y, start, 1, 0, argb, light, 0, 1, 0);
        }
    }

    @Override
    public ResourceLocation getTextureLocation(QuillStroke entity) {
        return INK;
    }
}
