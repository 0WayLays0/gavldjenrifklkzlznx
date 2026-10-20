package com.waylays.arcanes.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.waylays.arcanes.ArcanesOublies;
import com.waylays.arcanes.entity.boss.VoidOrb;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * The Void Orb: a great black sphere swallowing the light, wrapped in violet swirls turning both ways and a ring of
 * runes circling it; it swells a little as it rolls and shrinks to nothing when it is spent.
 */
public class VoidOrbRenderer extends EntityRenderer<VoidOrb> {
    private static final ResourceLocation CORE = ArcanesOublies.id("textures/entity/implosion_core.png");
    private static final ResourceLocation SWIRL = ArcanesOublies.id("textures/entity/implosion_swirl.png");

    public VoidOrbRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(VoidOrb orb, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        float age = orb.age(partialTicks);
        float open = Mth.clamp(age / 8f, 0, 1) * Mth.clamp(orb.left(partialTicks) / 6f, 0, 1);
        if (open <= 0) {
            return;
        }
        float pulse = 1 + Mth.sin(age * .5f) * .07f;
        float size = VoidOrb.RADIUS * 2 * open * pulse;
        poseStack.pushPose();
        poseStack.translate(0, orb.getBbHeight() * .5f, 0);
        // A ring of runes circling the orb, tilted
        poseStack.pushPose();
        poseStack.mulPose(Axis.XP.rotationDegrees(70));
        poseStack.mulPose(Axis.YP.rotationDegrees(age * 6));
        RenderHelper.groundQuad(buffer.getBuffer(RenderType.eyes(BossTelegraphRenderer.CIRCLES[2])), poseStack.last(), size * 1.25f,
                BossTelegraphRenderer.additive(0xC070FF, .8f * open), LightTexture.FULL_BRIGHT);
        poseStack.popPose();
        poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
        poseStack.mulPose(Axis.YP.rotationDegrees(180f));
        BlastEffectRenderer.layer(buffer, SWIRL, poseStack, size * 1.9f, age * 9, BlastEffectRenderer.rgb(.7f * open, .25f * open, open));
        BlastEffectRenderer.layer(buffer, SWIRL, poseStack, size * 1.5f, -age * 14 + 60, BlastEffectRenderer.rgb(.45f * open, .15f * open, .85f * open));
        BlastEffectRenderer.layer(buffer, BlastEffectRenderer.RING, poseStack, size * 1.15f, 0, BlastEffectRenderer.rgb(.8f * open, .45f * open, open));
        poseStack.pushPose();
        poseStack.translate(0, 0, .01f);
        poseStack.scale(size, size, size);
        RenderHelper.billboard(buffer.getBuffer(RenderType.entityTranslucent(CORE)), poseStack.last(), .5f, 0xF8FFFFFF, LightTexture.FULL_BRIGHT);
        poseStack.popPose();
        poseStack.popPose();
        super.render(orb, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    @Override
    public boolean shouldRender(VoidOrb entity, Frustum frustum, double x, double y, double z) {
        return true;
    }

    @Override
    public ResourceLocation getTextureLocation(VoidOrb entity) {
        return CORE;
    }
}
