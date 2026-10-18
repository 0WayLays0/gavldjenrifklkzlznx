package com.waylays.arcanes.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.waylays.arcanes.ArcanesOublies;
import com.waylays.arcanes.entity.book.RazorPage;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * A razor page: lying flat along its flight, spinning like a thrown blade, its edges burning white-gold.
 */
public class RazorPageRenderer extends EntityRenderer<RazorPage> {
    private static final ResourceLocation EDGE = ArcanesOublies.id("textures/entity/razor_edge.png");
    private static final float WIDTH = .5f;
    private static final float HEIGHT = .66f;

    public RazorPageRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(RazorPage entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        float age = entity.tickCount + partialTicks;
        poseStack.pushPose();
        // Local +Z along the flight
        poseStack.mulPose(Axis.YP.rotationDegrees(Mth.lerp(partialTicks, entity.yRotO, entity.getYRot())));
        poseStack.mulPose(Axis.XP.rotationDegrees(-Mth.lerp(partialTicks, entity.xRotO, entity.getXRot())));
        // Flat, spinning, with a slight wobble
        poseStack.mulPose(Axis.ZP.rotationDegrees(Mth.sin(age * .7f) * 12f));
        poseStack.mulPose(Axis.XP.rotationDegrees(90f));
        poseStack.mulPose(Axis.ZP.rotationDegrees(age * 45f));
        PageStormRenderer.drawPage(buffer, poseStack, WIDTH, HEIGHT, packedLight, .9f);
        float pulse = .8f + .2f * Mth.sin(age * 1.3f);
        RenderHelper.rect(buffer.getBuffer(RenderType.eyes(EDGE)), poseStack.last(), WIDTH * 1.18f, HEIGHT * 1.18f,
                BlastEffectRenderer.rgb(pulse, pulse * .86f, pulse * .55f), LightTexture.FULL_BRIGHT, true);
        poseStack.popPose();
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(RazorPage entity) {
        return PageStormRenderer.PAGE;
    }
}
