package com.waylays.arcanes.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.waylays.arcanes.entity.book.PaperTempest;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * The paper tornado: about a hundred pages racing round a funnel, narrow at the ground and wide at the top, faster
 * near the bottom. It rises out of the ground when it forms and flies apart when it blows out.
 */
public class PaperTempestRenderer extends EntityRenderer<PaperTempest> {
    private static final int PAGES = 96;

    public PaperTempestRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(PaperTempest entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        float age = entity.tickCount + partialTicks;
        float grown = Mth.clamp(age / PaperTempest.FORM_TICKS, 0f, 1f);
        float fade = Mth.clamp((age - entity.getDuration() + PaperTempest.FADE_TICKS) / PaperTempest.FADE_TICKS, 0f, 1f);
        float radius = entity.getRadius();
        float height = PaperTempest.HEIGHT;
        for (int i = 0; i < PAGES; i++) {
            float phase = i / (float) PAGES;
            // Spread evenly up the funnel, slowly climbing
            float rise = (phase * 1.618f + age * .004f * (1 + i % 3)) % 1f;
            float h = rise * height * grown;
            float t = h / height;
            float r = radius * (.3f + .7f * t) + Mth.sin(age * .2f + i) * .25f;
            r *= 1 + fade * 1.5f;
            float angle = age * (.5f - .25f * t) + i * 2.39996f;
            poseStack.pushPose();
            poseStack.translate(Mth.cos(angle) * r, h + fade * 2 * t, Mth.sin(angle) * r);
            // Edge-on to the wind, tumbling
            poseStack.mulPose(Axis.YP.rotation(-angle));
            poseStack.mulPose(Axis.XP.rotationDegrees(Mth.sin(age * .35f + i) * 60f));
            poseStack.mulPose(Axis.ZP.rotationDegrees(age * (8 + i % 5 * 4) + i * 37f));
            float size = .8f + (i % 4) * .12f;
            PageStormRenderer.drawPage(buffer, poseStack, .45f * size, .6f * size, packedLight, i % 4 == 0 ? .6f * (1 - fade) : 0);
            poseStack.popPose();
        }
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(PaperTempest entity) {
        return PageStormRenderer.PAGE;
    }
}
