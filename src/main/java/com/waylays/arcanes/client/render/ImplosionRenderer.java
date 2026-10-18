package com.waylays.arcanes.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.waylays.arcanes.ArcanesOublies;
import com.waylays.arcanes.entity.explosion.ImplosionCore;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * A point of black light: a black disc swallowing the light around it, ringed by a violet swirl that spins faster
 * and tighter as the core contracts, and a white-hot heart just before it blows.
 */
public class ImplosionRenderer extends EntityRenderer<ImplosionCore> {
    private static final ResourceLocation CORE = ArcanesOublies.id("textures/entity/implosion_core.png");
    private static final ResourceLocation SWIRL = ArcanesOublies.id("textures/entity/implosion_swirl.png");

    public ImplosionRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(ImplosionCore entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        float age = entity.tickCount + partialTicks;
        float open = Mth.clamp(age / 10f, 0f, 1f);
        // 1 while pulling, falls to 0 while contracting
        float contract = 1 - Mth.clamp((age - ImplosionCore.CONTRACT_TICK) / (ImplosionCore.DETONATE_TICK - ImplosionCore.CONTRACT_TICK), 0f, 1f);
        float pulse = 1 + Mth.sin(age * .6f) * .06f;
        float coreSize = 1.7f * open * (.25f + .75f * contract) * pulse;
        float swirlSize = (2.2f + entity.getPullRadius() * .45f) * open * (.2f + .8f * contract);

        poseStack.pushPose();
        poseStack.translate(0, entity.getBbHeight() * .5f, 0);
        poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
        poseStack.mulPose(Axis.YP.rotationDegrees(180f));

        // Violet accretion swirl (two arms turning opposite ways, faster as it contracts)
        float spin = age * (8f + (1 - contract) * 30f);
        BlastEffectRenderer.layer(buffer, SWIRL, poseStack, swirlSize, spin, BlastEffectRenderer.rgb(.75f, .3f, 1f));
        BlastEffectRenderer.layer(buffer, SWIRL, poseStack, swirlSize * .75f, -spin * 1.4f + 60f, BlastEffectRenderer.rgb(.5f, .2f, .9f));
        // Thin glowing rim around the black core
        BlastEffectRenderer.layer(buffer, BlastEffectRenderer.RING, poseStack, coreSize * 1.25f, 0, BlastEffectRenderer.rgb(.85f, .5f, 1f));

        // The black core itself: plain alpha blending, so it darkens what is behind it
        poseStack.pushPose();
        poseStack.translate(0, 0, .01f);
        poseStack.scale(coreSize, coreSize, coreSize);
        RenderHelper.billboard(buffer.getBuffer(RenderType.entityTranslucent(CORE)), poseStack.last(), .5f, 0xF0FFFFFF, LightTexture.FULL_BRIGHT);
        poseStack.popPose();

        // White-hot heart in the last instants
        float heart = 1 - contract;
        if (heart > 0) {
            BlastEffectRenderer.layer(buffer, BlastEffectRenderer.CORE, poseStack, .4f + heart * 1.2f, 0, BlastEffectRenderer.rgb(heart, heart, heart));
        }
        poseStack.popPose();
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(ImplosionCore entity) {
        return CORE;
    }
}
