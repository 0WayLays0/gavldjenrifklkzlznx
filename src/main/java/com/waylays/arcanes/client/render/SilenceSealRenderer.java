package com.waylays.arcanes.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.waylays.arcanes.ArcanesOublies;
import com.waylays.arcanes.entity.book.SilenceSeal;
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
 * Seal of Silence on its victim: a big violet wax seal tumbles down and stamps on the head, then stays as a small
 * seal pressed over the mouth while a ring of runes turns around the head; it bursts when the silence ends.
 */
public class SilenceSealRenderer extends EntityRenderer<SilenceSeal> {
    private static final ResourceLocation WAX = ArcanesOublies.id("textures/entity/silence_seal.png");
    private static final ResourceLocation GLYPH = ArcanesOublies.id("textures/entity/silence_glyph.png");
    private static final ResourceLocation RING = ArcanesOublies.id("textures/entity/silence_ring.png");

    public SilenceSealRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(SilenceSeal entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        LivingEntity target = entity.getTarget();
        if (target == null) {
            return;
        }
        float age = entity.tickCount + partialTicks;
        float breakAge = entity.breakAge(partialTicks);
        float broken = breakAge < 0 ? 0 : Mth.clamp(breakAge / SilenceSeal.BREAK_TICKS, 0f, 1f);
        Vec3 targetPos = target.getPosition(partialTicks);
        Vec3 selfPos = entity.getPosition(partialTicks);
        float eye = target.getEyeHeight();
        poseStack.pushPose();
        poseStack.translate(targetPos.x - selfPos.x, targetPos.y - selfPos.y, targetPos.z - selfPos.z);

        if (age < SilenceSeal.STAMP_TICK) {
            // Falling and spinning onto the head
            float t = age / SilenceSeal.STAMP_TICK;
            poseStack.pushPose();
            poseStack.translate(0, target.getBbHeight() + .4f + (1 - t * t) * 2.5f, 0);
            poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
            poseStack.mulPose(Axis.YP.rotationDegrees(180f));
            poseStack.mulPose(Axis.ZP.rotationDegrees((1 - t) * 300f));
            float s = 1.3f * Mth.clamp(t * 3, 0f, 1f);
            RenderHelper.rect(buffer.getBuffer(RenderType.entityCutoutNoCull(WAX)), poseStack.last(), s, s, 0xFFFFFFFF, LightTexture.FULL_BRIGHT, false);
            RenderHelper.rect(buffer.getBuffer(RenderType.eyes(GLYPH)), poseStack.last(), s, s, 0xFFFFFFFF, LightTexture.FULL_BRIGHT, true);
            poseStack.popPose();
        } else {
            float since = age - SilenceSeal.STAMP_TICK;
            float stamp = since < 4 ? 1 + (1 - since / 4f) * .6f : 1;
            float pulse = .8f + .2f * Mth.sin(age * .3f);
            float fade = 1 - broken;
            // Seal pressed over the mouth, facing where the head looks
            poseStack.pushPose();
            float headYaw = Mth.rotLerp(partialTicks, target.yHeadRotO, target.yHeadRot);
            poseStack.mulPose(Axis.YP.rotationDegrees(-headYaw));
            poseStack.translate(0, eye - .18f * (eye / 1.62f), target.getBbWidth() * .5f + .06f);
            float s = .5f * stamp * (1 + broken * .8f) * (eye / 1.62f * .6f + .4f);
            if (broken < 1) {
                RenderHelper.rect(buffer.getBuffer(RenderType.entityCutoutNoCull(WAX)), poseStack.last(), s * (1 - broken), s * (1 - broken), 0xFFFFFFFF, LightTexture.FULL_BRIGHT, false);
            }
            float g = pulse * fade;
            RenderHelper.rect(buffer.getBuffer(RenderType.eyes(GLYPH)), poseStack.last(), s * 1.1f, s * 1.1f, BlastEffectRenderer.rgb(g, g, g), LightTexture.FULL_BRIGHT, false);
            poseStack.popPose();
            // Ring of runes around the head
            poseStack.pushPose();
            poseStack.translate(0, eye + .05f, 0);
            poseStack.mulPose(Axis.YP.rotationDegrees(age * 4f));
            float r = (target.getBbWidth() * .5f + .45f) * (since < 4 ? since / 4f : 1) * (1 + broken * 1.5f);
            int ring = BlastEffectRenderer.rgb(.6f * g, .36f * g, g);
            RenderHelper.groundQuad(buffer.getBuffer(RenderType.eyes(RING)), poseStack.last(), r, ring, LightTexture.FULL_BRIGHT);
            // Seen from below too
            poseStack.mulPose(Axis.XP.rotationDegrees(180f));
            RenderHelper.groundQuad(buffer.getBuffer(RenderType.eyes(RING)), poseStack.last(), r, ring, LightTexture.FULL_BRIGHT);
            poseStack.popPose();
        }
        poseStack.popPose();
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(SilenceSeal entity) {
        return WAX;
    }
}
