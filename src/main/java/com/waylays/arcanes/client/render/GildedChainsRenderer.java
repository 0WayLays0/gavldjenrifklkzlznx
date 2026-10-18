package com.waylays.arcanes.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.waylays.arcanes.ArcanesOublies;
import com.waylays.arcanes.entity.gold.GildedChainsEntity;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/**
 * Gilded Chains: a turning golden rune circle on the ground, chains that shoot out of it into the target,
 * and a chain loop locked around the target's waist.
 */
public class GildedChainsRenderer extends EntityRenderer<GildedChainsEntity> {
    private static final ResourceLocation CHAIN = ArcanesOublies.id("textures/entity/gilded_chain.png");
    private static final ResourceLocation CIRCLE = ArcanesOublies.id("textures/entity/gold_circle.png");
    private static final float LINK_LENGTH = .5f;
    private static final float CHAIN_WIDTH = .34f;
    private static final int LOOP_SEGMENTS = 8;

    public GildedChainsRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(GildedChainsEntity entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        float age = entity.tickCount + partialTicks;
        float t = Mth.clamp(age / GildedChainsEntity.EMERGE_TICKS, 0f, 1f);
        float emerge = 1 - (1 - t) * (1 - t) * (1 - t);
        Entity bound = entity.getFirstPassenger();
        float width = bound == null ? .6f : bound.getBbWidth();
        float height = bound == null ? 1.8f : bound.getBbHeight();

        // Rune circle, fading out over the last second
        int remaining = entity.getDuration() - entity.tickCount;
        int alpha = (int) (255 * Mth.clamp(remaining / 20f, .25f, 1f));
        int circleColor = (alpha << 24) | (alpha << 16) | (alpha << 8) | alpha;
        poseStack.pushPose();
        poseStack.translate(0, .03, 0);
        poseStack.mulPose(Axis.YP.rotationDegrees(age * 2f));
        RenderHelper.groundQuad(buffer.getBuffer(RenderType.eyes(CIRCLE)), poseStack.last(), (width * .5f + 1.5f) * emerge, circleColor, LightTexture.FULL_BRIGHT);
        poseStack.popPose();

        // Gold glints: chains are lit as if by a torch even in the dark
        int light = LightTexture.pack(Math.max(12, LightTexture.block(packedLight)), LightTexture.sky(packedLight));
        VertexConsumer chains = buffer.getBuffer(RenderType.entityCutoutNoCull(CHAIN));
        PoseStack.Pose pose = poseStack.last();
        float waistRadius = width * .5f + .06f;
        for (int i = 0; i < GildedChainsEntity.CHAIN_COUNT; i++) {
            Vec3 anchor = GildedChainsEntity.anchorOffset(i, width);
            Vec3 inward = new Vec3(anchor.x, 0, anchor.z).normalize();
            float attachHeight = height * (i % 2 == 0 ? .42f : .58f);
            // A little tension tremor once the chains are taut
            float tremor = t >= 1 ? Mth.sin(age * 1.7f + i * 2.1f) * .025f : 0f;
            Vec3 attach = inward.scale(waistRadius).add(0, attachHeight + tremor, 0);
            Vec3 tip = anchor.add(attach.subtract(anchor).scale(emerge));
            RenderHelper.strand(chains, pose, anchor, tip, CHAIN_WIDTH, LINK_LENGTH, 0xFFFFFFFF, light);
        }

        if (t >= 1) {
            float loopHeight = height * .5f;
            for (int k = 0; k < LOOP_SEGMENTS; k++) {
                float a0 = (float) (k * Math.PI * 2 / LOOP_SEGMENTS);
                float a1 = (float) ((k + 1) * Math.PI * 2 / LOOP_SEGMENTS);
                Vec3 p0 = new Vec3(Mth.cos(a0) * waistRadius, loopHeight, Mth.sin(a0) * waistRadius);
                Vec3 p1 = new Vec3(Mth.cos(a1) * waistRadius, loopHeight, Mth.sin(a1) * waistRadius);
                RenderHelper.strand(chains, pose, p0, p1, CHAIN_WIDTH * .8f, LINK_LENGTH * .8f, 0xFFFFFFFF, light);
            }
        }

        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(GildedChainsEntity entity) {
        return CHAIN;
    }
}
