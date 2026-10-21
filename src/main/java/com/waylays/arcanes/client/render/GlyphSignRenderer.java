package com.waylays.arcanes.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.waylays.arcanes.entity.glyph.GlyphSign;
import com.waylays.arcanes.spells.glyph.GlyphElement;
import com.waylays.arcanes.spells.glyph.GlyphStrokes;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * A glyph written in light: its strokes traced one after another by a blazing pen tip, then a flare and a ring of
 * light as its spell goes off, and it fades, swelling a little. Hung upright before the caster (reading the right way
 * for them) or lying on the ground.
 */
public class GlyphSignRenderer extends EntityRenderer<GlyphSign> {
    public GlyphSignRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(GlyphSign sign, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        float written = sign.written(partialTicks);
        if (written <= 0) {
            return;
        }
        GlyphElement element = sign.getElement();
        float since = sign.sinceWritten(partialTicks);
        float fade = since <= GlyphSign.HOLD_TICKS ? 1 : Mth.clamp(1 - (since - GlyphSign.HOLD_TICKS) / GlyphSign.FADE_TICKS, 0, 1);
        float flare = since < 0 ? 0 : Mth.clamp(1 - since / GlyphSign.HOLD_TICKS, 0, 1);
        float swell = 1 + .3f * Mth.clamp(since / (GlyphSign.HOLD_TICKS + GlyphSign.FADE_TICKS), 0, 1);
        float size = sign.getSize() * swell;
        float age = sign.tickCount + partialTicks;

        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(-sign.getYRot()));
        if (sign.isFlat()) {
            poseStack.translate(0, .02, 0);
            poseStack.mulPose(Axis.XP.rotationDegrees(90));
        } else {
            poseStack.mulPose(Axis.XP.rotationDegrees(sign.getXRot()));
            // Floats as it waits
            poseStack.translate(0, .03f * Mth.sin(age * .25f), 0);
        }
        // Mirrored, so it reads the right way for whoever wrote it
        poseStack.scale(-1, 1, 1);
        PoseStack.Pose pose = poseStack.last();
        GlyphRender.halo(buffer, pose, GlyphRender.GLOW, size * 1.5f, RuneRender.additive(element.color, (.25f + .35f * flare) * fade * written));
        float width = 1 + .6f * flare;
        GlyphRender.strokes(buffer, pose, GlyphStrokes.of(element), size, written, element.color,
                GlyphRender.whiten(element.core, flare * .6f), fade, width);
        if (since >= 0 && since < GlyphSign.HOLD_TICKS + GlyphSign.FADE_TICKS) {
            // The ring of light that leaves the glyph as it goes off
            float t = since / (GlyphSign.HOLD_TICKS + GlyphSign.FADE_TICKS);
            GlyphRender.halo(buffer, pose, GlyphRender.RIPPLE, size * (1.1f + 1.4f * t), RuneRender.additive(element.color, (1 - t) * .9f));
        }
        poseStack.popPose();
        super.render(sign, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    @Override
    public boolean shouldRender(GlyphSign entity, Frustum frustum, double x, double y, double z) {
        return true;
    }

    @Override
    public ResourceLocation getTextureLocation(GlyphSign entity) {
        return GlyphRender.STROKE;
    }
}
