package com.waylays.arcanes.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.waylays.arcanes.ArcanesOublies;
import com.waylays.arcanes.entity.book.InkHands;
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
 * Black hands of ink rising around the victim and leaning in to grip it, violet veins pulsing in the ink, sinking
 * back into a pool of ink when they let go.
 */
public class InkHandsRenderer extends EntityRenderer<InkHands> {
    private static final ResourceLocation HAND = ArcanesOublies.id("textures/entity/ink_hand.png");
    private static final ResourceLocation HAND_GLOW = ArcanesOublies.id("textures/entity/ink_hand_glow.png");
    private static final ResourceLocation SPLASH = ArcanesOublies.id("textures/entity/ink_splash.png");
    private static final int HANDS = 4;

    public InkHandsRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(InkHands entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        LivingEntity target = entity.getTarget();
        float age = entity.tickCount + partialTicks;
        float rise = Mth.clamp(age / InkHands.RISE_TICKS, 0f, 1f);
        float sink = Mth.clamp((age - InkHands.RISE_TICKS - entity.getHoldTicks()) / InkHands.SINK_TICKS, 0f, 1f);
        float out = rise * (1 - sink);
        if (out <= 0) {
            return;
        }
        float width = target == null ? .6f : target.getBbWidth();
        float tall = target == null ? 1.8f : target.getBbHeight();
        float handHeight = Math.min(2.2f, .6f + tall * .55f) * out;
        float ring = width * .5f + .35f;

        poseStack.pushPose();
        if (target != null) {
            Vec3 targetPos = target.getPosition(partialTicks);
            Vec3 selfPos = entity.getPosition(partialTicks);
            poseStack.translate(targetPos.x - selfPos.x, 0, targetPos.z - selfPos.z);
        }
        // Pool of ink the hands rise from
        poseStack.pushPose();
        poseStack.translate(0, .03, 0);
        poseStack.mulPose(Axis.YP.rotationDegrees(entity.getYRot()));
        RenderHelper.groundQuad(buffer.getBuffer(RenderType.entityTranslucent(SPLASH)), poseStack.last(), (ring + .8f) * out, ((int) (220 * out) << 24) | 0xFFFFFF, packedLight);
        poseStack.popPose();

        boolean holding = entity.isHolding();
        float squeeze = holding ? Mth.sin(age * .5f) * .5f + .5f : 0;
        float glow = out * (.45f + .4f * squeeze);
        int v = (int) (Mth.clamp(glow, 0f, 1f) * 255);
        int glowColor = 0xFF000000 | (v * 3 / 5 << 16) | (v / 3 << 8) | v;
        for (int k = 0; k < HANDS; k++) {
            float angle = entity.getYRot() + k * 360f / HANDS + (k % 2) * 20f;
            poseStack.pushPose();
            poseStack.mulPose(Axis.YP.rotationDegrees(angle));
            poseStack.translate(0, 0, ring);
            // Leaning in over the victim, closing tighter when it squeezes
            poseStack.mulPose(Axis.XP.rotationDegrees(-(18f + 14f * rise + 8f * squeeze)));
            poseStack.translate(0, handHeight * .5f, 0);
            float w = .75f * (1 - .08f * squeeze);
            RenderHelper.rect(buffer.getBuffer(RenderType.entityCutoutNoCull(HAND)), poseStack.last(), w, handHeight, 0xFFFFFFFF, packedLight, false);
            poseStack.translate(0, 0, .004f);
            RenderHelper.rect(buffer.getBuffer(RenderType.eyes(HAND_GLOW)), poseStack.last(), w, handHeight, glowColor, LightTexture.FULL_BRIGHT, true);
            poseStack.popPose();
        }
        poseStack.popPose();
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(InkHands entity) {
        return HAND;
    }
}
