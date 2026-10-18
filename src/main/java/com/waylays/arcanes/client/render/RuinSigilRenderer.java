package com.waylays.arcanes.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.waylays.arcanes.ArcanesOublies;
import com.waylays.arcanes.entity.explosion.RuinSigil;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * A rune of ruin burnt into the ground: it draws itself in, smoulders while armed, and spins up white-hot once lit.
 */
public class RuinSigilRenderer extends EntityRenderer<RuinSigil> {
    private static final ResourceLocation SIGIL = ArcanesOublies.id("textures/entity/ruin_sigil.png");

    public RuinSigilRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(RuinSigil entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        float age = entity.tickCount + partialTicks;
        float drawIn = Mth.clamp(age / RuinSigil.ARM_TICKS, 0f, 1f);
        boolean lit = entity.isLit();
        float glow = lit ? 1f : drawIn * (.55f + Mth.sin(age * .15f) * .15f);
        float spin = lit ? age * 18f : age * 1.2f;
        float size = 1.6f * (.4f + .6f * drawIn) * (lit ? 1.15f + Mth.sin(age * 1.7f) * .08f : 1f);

        poseStack.pushPose();
        poseStack.translate(0, .03, 0);
        poseStack.mulPose(Axis.YP.rotationDegrees(entity.getYRot() + spin));
        int outer = lit ? BlastEffectRenderer.rgb(1f, .85f, .6f) : BlastEffectRenderer.rgb(glow, glow * .4f, glow * .12f);
        RenderHelper.groundQuad(buffer.getBuffer(RenderType.eyes(SIGIL)), poseStack.last(), size, outer, LightTexture.FULL_BRIGHT);
        poseStack.popPose();

        // Inner counter-rotating ring, violet
        poseStack.pushPose();
        poseStack.translate(0, .05, 0);
        poseStack.mulPose(Axis.YP.rotationDegrees(-entity.getYRot() - spin * 1.6f));
        float inner = glow * .8f;
        RenderHelper.groundQuad(buffer.getBuffer(RenderType.eyes(SIGIL)), poseStack.last(), size * .55f, BlastEffectRenderer.rgb(inner * .7f, inner * .25f, inner), LightTexture.FULL_BRIGHT);
        poseStack.popPose();

        if (lit) {
            // About to blow: a hot core swelling above the rune
            poseStack.pushPose();
            poseStack.translate(0, .5, 0);
            poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
            poseStack.mulPose(Axis.YP.rotationDegrees(180f));
            BlastEffectRenderer.layer(buffer, BlastEffectRenderer.CORE, poseStack, 1.4f + Mth.sin(age * 2.1f) * .3f, 0, BlastEffectRenderer.rgb(1f, .7f, .4f));
            poseStack.popPose();
        }
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(RuinSigil entity) {
        return SIGIL;
    }
}
