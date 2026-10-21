package com.waylays.arcanes.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.waylays.arcanes.ArcanesOublies;
import com.waylays.arcanes.entity.explosion.CataclysmCore;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * The sphere of Cataclysm while it swells: a halo of darkness where it drinks the light, a churning ball of fire with
 * a white-hot heart, and shock rings that pulse faster and faster until it blows.
 */
public class CataclysmRenderer extends EntityRenderer<CataclysmCore> {
    private static final ResourceLocation DARK = ArcanesOublies.id("textures/entity/implosion_core.png");

    public CataclysmRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(CataclysmCore entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        float age = entity.tickCount + partialTicks;
        if (age >= CataclysmCore.GROW_TICKS) {
            return;
        }
        float t = age / CataclysmCore.GROW_TICKS;
        float grow = .3f + .7f * t;
        float throb = 1 + Mth.sin(age * (.4f + t * 1.2f)) * (.04f + t * .08f);
        float size = 5f * grow * throb;

        poseStack.pushPose();
        poseStack.translate(0, entity.getBbHeight() * .5f, 0);
        poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());

        // Halo of darkness: the light around is being drunk in (plain alpha blending, drawn first)
        poseStack.pushPose();
        float dark = size * 2.6f;
        poseStack.translate(0, 0, -.02f);
        poseStack.scale(dark, dark, dark);
        int alpha = (int) (110 * Mth.clamp(t * 2, 0f, 1f));
        RenderHelper.billboard(buffer.getBuffer(RenderType.entityTranslucent(DARK)), poseStack.last(), .5f, (alpha << 24) | 0xFFFFFF, LightTexture.FULL_BRIGHT);
        poseStack.popPose();

        // Churning ball of fire, whiter as it nears the end
        float white = t * t;
        int fire = BlastEffectRenderer.rgb(1f, .5f + .45f * white, .2f + .7f * white);
        BlastEffectRenderer.layer(buffer, BlastEffectRenderer.FIRE, poseStack, size, age * 6f, fire);
        BlastEffectRenderer.layer(buffer, BlastEffectRenderer.FIRE, poseStack, size * .8f, -age * 9f + 90f, fire);
        BlastEffectRenderer.layer(buffer, BlastEffectRenderer.CORE, poseStack, size * .9f, 0, BlastEffectRenderer.rgb(1f, .9f, .75f));
        BlastEffectRenderer.layer(buffer, BlastEffectRenderer.CORE, poseStack, size * 1.8f, 0, BlastEffectRenderer.rgb(.6f * t, .25f * t, .8f * t));

        // Shock rings pulsing outward, faster and faster
        float period = Mth.lerp(t, 20f, 5f);
        float phase = (age % period) / period;
        float ringFade = (1 - phase) * (.4f + .6f * t);
        BlastEffectRenderer.layer(buffer, BlastEffectRenderer.RING, poseStack, size * (1 + phase * 1.5f), 0, BlastEffectRenderer.rgb(ringFade * .8f, ringFade * .4f, ringFade));
        poseStack.popPose();
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(CataclysmCore entity) {
        return DARK;
    }
}
