package com.waylays.arcanes.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.waylays.arcanes.entity.verdigris.RustBolt;
import com.waylays.arcanes.spells.verdigris.VerdigrisFx;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

/** Rust: a tumbling clot of rusted and green-flaked copper in a dull red glow. */
public class RustBoltRenderer extends EntityRenderer<RustBolt> {
    public RustBoltRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(RustBolt bolt, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        float age = bolt.tickCount + partialTicks;
        poseStack.pushPose();
        poseStack.translate(0, bolt.getBbHeight() * .5f, 0);
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(age * 23));
        poseStack.mulPose(Axis.XP.rotationDegrees(age * 17));
        int light = LightTexture.FULL_BRIGHT;
        VerdigrisRender.block(VerdigrisRender.RAW_COPPER, poseStack, buffer, light, 0, -.16f, 0, .32f, .32f, .32f);
        RuneRender.block(VerdigrisRender.EXPOSED, poseStack, buffer, light, .14f, -.02f, .08f, .2f, .2f, .2f, 35, 20);
        RuneRender.block(VerdigrisRender.OXIDIZED, poseStack, buffer, light, -.12f, .02f, -.1f, .17f, .17f, .17f, -25, 40);
        poseStack.popPose();
        poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
        RenderHelper.billboard(buffer.getBuffer(RenderType.eyes(RuneRender.GLOW)), poseStack.last(), .55f,
                RuneRender.additive(VerdigrisFx.RUST, .75f), LightTexture.FULL_BRIGHT);
        poseStack.popPose();
        super.render(bolt, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(RustBolt bolt) {
        return RuneRender.GLOW;
    }
}
