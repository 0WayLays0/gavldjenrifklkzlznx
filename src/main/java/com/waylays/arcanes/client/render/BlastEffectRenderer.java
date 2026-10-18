package com.waylays.arcanes.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.waylays.arcanes.ArcanesOublies;
import com.waylays.arcanes.entity.explosion.BlastEffect;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * An Explosion school blast, all additive light: a turbulent fireball that swells and burns out, a white-hot flash,
 * a shock ring racing along the ground and a second ring facing the camera.
 */
public class BlastEffectRenderer extends EntityRenderer<BlastEffect> {
    static final ResourceLocation FIRE = ArcanesOublies.id("textures/entity/blast_fire.png");
    static final ResourceLocation CORE = ArcanesOublies.id("textures/entity/blast_core.png");
    static final ResourceLocation RING = ArcanesOublies.id("textures/entity/shock_ring.png");

    public BlastEffectRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(BlastEffect entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        float size = entity.getSize();
        int style = entity.getStyle();
        float t = Mth.clamp((entity.tickCount + partialTicks) / entity.lifetime(), 0f, 1f);
        float grow = 1 - (1 - t) * (1 - t) * (1 - t);
        float fade = 1 - t;

        // Ground shock ring
        poseStack.pushPose();
        poseStack.translate(0, .05, 0);
        poseStack.mulPose(Axis.YP.rotationDegrees(entity.getYRot()));
        int ringColor = style == BlastEffect.STYLE_FIRE ? rgb(.75f * fade, .3f * fade, fade) : rgb(fade, fade * .9f, fade);
        RenderHelper.groundQuad(buffer.getBuffer(RenderType.eyes(RING)), poseStack.last(), size * (1 + 6 * grow), ringColor, LightTexture.FULL_BRIGHT);
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.translate(0, size * .3f, 0);
        poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
        poseStack.mulPose(Axis.YP.rotationDegrees(180f));

        // Fireball: swells, darkens from yellow-white to deep red, burns out
        float fireFade = (float) Math.pow(fade, 1.4);
        int fireColor = switch (style) {
            case BlastEffect.STYLE_ARCANE -> rgb(.8f * fireFade, .35f * fireFade, fireFade);
            case BlastEffect.STYLE_WHITE -> rgb(fireFade, fireFade * .92f, fireFade * .75f);
            default -> rgb(fireFade, (.55f + .4f * fade) * fireFade, .35f * fade * fireFade);
        };
        layer(buffer, FIRE, poseStack, size * (1.4f + 2.4f * grow), entity.getYRot() + t * 40f, fireColor);
        layer(buffer, FIRE, poseStack, size * (1.0f + 1.8f * grow), -entity.getYRot() - t * 70f, fireColor);

        // White-hot flash, gone in the first third
        float flash = Mth.clamp(1 - t * 3, 0f, 1f);
        if (flash > 0) {
            layer(buffer, CORE, poseStack, size * 2.2f * (.6f + .4f * flash), 0, rgb(flash, flash, flash));
        }

        // Camera-facing shock ring
        float ringFade = fade * fade;
        layer(buffer, RING, poseStack, size * (.8f + 4.5f * grow), 0, rgb(ringFade, ringFade * .9f, ringFade));
        poseStack.popPose();

        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    static void layer(MultiBufferSource buffer, ResourceLocation texture, PoseStack poseStack, float size, float rollDegrees, int argb) {
        poseStack.pushPose();
        poseStack.mulPose(Axis.ZP.rotationDegrees(rollDegrees));
        poseStack.scale(size, size, size);
        RenderHelper.billboard(buffer.getBuffer(RenderType.eyes(texture)), poseStack.last(), .5f, argb, LightTexture.FULL_BRIGHT);
        poseStack.popPose();
    }

    static int rgb(float r, float g, float b) {
        return 0xFF000000 | ((int) (Mth.clamp(r, 0f, 1f) * 255) << 16) | ((int) (Mth.clamp(g, 0f, 1f) * 255) << 8) | (int) (Mth.clamp(b, 0f, 1f) * 255);
    }

    @Override
    public ResourceLocation getTextureLocation(BlastEffect entity) {
        return FIRE;
    }
}
