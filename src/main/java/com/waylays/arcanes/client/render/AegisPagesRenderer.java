package com.waylays.arcanes.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.waylays.arcanes.ArcanesOublies;
import com.waylays.arcanes.entity.book.AegisPages;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * Parchment Aegis: the remaining pages circle their bearer on a gently tilted orbit, standing upright and facing
 * out like the plates of an armour, their runes and gilded edges glowing.
 */
public class AegisPagesRenderer extends EntityRenderer<AegisPages> {
    private static final ResourceLocation EDGE = ArcanesOublies.id("textures/entity/razor_edge.png");

    public AegisPagesRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(AegisPages entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        LivingEntity bearer = entity.getTarget();
        int pages = entity.getPages();
        if (bearer == null || pages <= 0) {
            return;
        }
        float age = entity.tickCount + partialTicks;
        float appear = Mth.clamp(age / 10f, 0f, 1f);
        Vec3 bearerPos = bearer.getPosition(partialTicks);
        Vec3 selfPos = entity.getPosition(partialTicks);
        float radius = (bearer.getBbWidth() * .5f + .75f) * (1.6f - .6f * appear);
        float w = .5f;
        float h = .68f;
        poseStack.pushPose();
        poseStack.translate(bearerPos.x - selfPos.x, bearerPos.y - selfPos.y + bearer.getBbHeight() * .5f, bearerPos.z - selfPos.z);
        // Tilted orbit, wobbling slowly
        poseStack.mulPose(Axis.XP.rotationDegrees(Mth.sin(age * .05f) * 12f));
        poseStack.mulPose(Axis.ZP.rotationDegrees(Mth.cos(age * .04f) * 10f));
        for (int i = 0; i < pages; i++) {
            float a = age * .09f + i * Mth.TWO_PI / pages;
            poseStack.pushPose();
            poseStack.translate(Mth.cos(a) * radius, Mth.sin(age * .2f + i * 1.9f) * .12f, Mth.sin(a) * radius);
            // Facing out, a little flutter
            poseStack.mulPose(Axis.YP.rotation(Mth.HALF_PI - a));
            poseStack.mulPose(Axis.XP.rotationDegrees(Mth.sin(age * .3f + i) * 10f));
            poseStack.scale(appear, appear, appear);
            PageStormRenderer.drawPage(buffer, poseStack, w, h, packedLight, .8f + .2f * Mth.sin(age * .25f + i));
            float edge = .55f + .2f * Mth.sin(age * .4f + i * 2);
            RenderHelper.rect(buffer.getBuffer(RenderType.eyes(EDGE)), poseStack.last(), w * 1.16f, h * 1.16f,
                    BlastEffectRenderer.rgb(edge, edge * .84f, edge * .5f), LightTexture.FULL_BRIGHT, true);
            poseStack.popPose();
        }
        poseStack.popPose();
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(AegisPages entity) {
        return PageStormRenderer.PAGE;
    }
}
