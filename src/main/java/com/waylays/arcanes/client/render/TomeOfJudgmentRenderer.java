package com.waylays.arcanes.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.waylays.arcanes.ArcanesOublies;
import com.waylays.arcanes.entity.book.TomeOfJudgment;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * Tome of Judgment: a crimson-bound giant book high in the sky, opening flat with its glowing pages turned to the
 * ground as it comes down; a rune frame burns on the ground under it; then the halves swing down and clap shut, and
 * the closed book shrinks away into pages.
 */
public class TomeOfJudgmentRenderer extends EntityRenderer<TomeOfJudgment> {
    private static final ResourceLocation COVER = ArcanesOublies.id("textures/entity/judgment_cover.png");
    private static final ResourceLocation FRAME = ArcanesOublies.id("textures/entity/judgment_frame.png");

    public TomeOfJudgmentRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(TomeOfJudgment entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        float age = entity.tickCount + partialTicks;
        float appear = ease(Mth.clamp(age / TomeOfJudgment.APPEAR_TICKS, 0f, 1f));
        float open = ease(Mth.clamp((age - TomeOfJudgment.APPEAR_TICKS) / (TomeOfJudgment.OPEN_END - TomeOfJudgment.APPEAR_TICKS), 0f, 1f));
        // The clap accelerates all the way down
        float slam = Mth.clamp((age - TomeOfJudgment.SLAM_START) / (TomeOfJudgment.IMPACT_TICK - TomeOfJudgment.SLAM_START), 0f, 1f);
        float angle = 90f * open * (1 - slam * slam);
        float vanish = Mth.clamp((age - TomeOfJudgment.VANISH_TICK) / (TomeOfJudgment.END_TICK - TomeOfJudgment.VANISH_TICK), 0f, 1f);
        float bounce = age > TomeOfJudgment.IMPACT_TICK ? Mth.sin(Math.min(1f, (age - TomeOfJudgment.IMPACT_TICK) / 6f) * Mth.PI) * .25f : 0f;
        float glow = Mth.clamp(.4f + .5f * open + Mth.sin(age * .4f) * .1f, 0f, 1f) * (1 - slam);
        float scale = TomeOfJudgment.PAGE_WIDTH / TomeModel.W;

        if (vanish < 1) {
            poseStack.pushPose();
            poseStack.translate(0, TomeOfJudgment.spineHeight(age) + bounce - vanish * TomeOfJudgment.SPINE_HEIGHT * .5f, 0);
            poseStack.mulPose(Axis.YP.rotationDegrees(-entity.getYRot()));
            float s = scale * appear * (1 - vanish * vanish);
            poseStack.scale(s, s, s);
            // Lit from where it stands on the ground, never pitch black up in the night sky
            int light = LightTexture.pack(Math.max(LightTexture.block(packedLight), 7), LightTexture.sky(packedLight));
            TomeModel.draw(buffer, poseStack, COVER, angle, glow, light);
            poseStack.popPose();
        }

        // The condemned area, burning brighter until the clap, flaring at it
        float warn = Mth.clamp(age / TomeOfJudgment.OPEN_END, 0f, 1f) * (age < TomeOfJudgment.IMPACT_TICK ? .55f + .45f * Mth.clamp((age - TomeOfJudgment.OPEN_END) / 10f, 0f, 1f) : 0f);
        float flare = age >= TomeOfJudgment.IMPACT_TICK ? 1 - Mth.clamp((age - TomeOfJudgment.IMPACT_TICK) / 10f, 0f, 1f) : 0f;
        float f = Math.max(warn * (.85f + .15f * Mth.sin(age * .6f)), flare);
        if (f > 0) {
            poseStack.pushPose();
            poseStack.translate(0, .06, 0);
            poseStack.mulPose(Axis.YP.rotationDegrees(-entity.getYRot()));
            float hx = TomeOfJudgment.PAGE_WIDTH + .3f;
            float hz = TomeOfJudgment.PAGE_HEIGHT * .5f + .3f;
            int color = BlastEffectRenderer.rgb(f, f * (.45f + .4f * flare), f * (.3f + .5f * flare));
            var consumer = buffer.getBuffer(RenderType.eyes(FRAME));
            var pose = poseStack.last();
            RenderHelper.vertex(consumer, pose, -hx, 0, -hz, 0, 0, color, LightTexture.FULL_BRIGHT, 0, 1, 0);
            RenderHelper.vertex(consumer, pose, -hx, 0, hz, 0, 1, color, LightTexture.FULL_BRIGHT, 0, 1, 0);
            RenderHelper.vertex(consumer, pose, hx, 0, hz, 1, 1, color, LightTexture.FULL_BRIGHT, 0, 1, 0);
            RenderHelper.vertex(consumer, pose, hx, 0, -hz, 1, 0, color, LightTexture.FULL_BRIGHT, 0, 1, 0);
            poseStack.popPose();
        }
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    private static float ease(float t) {
        return 1 - (1 - t) * (1 - t) * (1 - t);
    }

    @Override
    public ResourceLocation getTextureLocation(TomeOfJudgment entity) {
        return COVER;
    }
}
