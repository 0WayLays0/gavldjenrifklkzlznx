package com.waylays.arcanes.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.waylays.arcanes.entity.wood.FlyingLog;
import com.waylays.arcanes.spells.wood.WoodFx;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * Log Hurl: a great oak log lying across its path (bark, knots, sawn-off branch stubs, ivy wound round it, its ends
 * showing their rings), torn up out of the ground shaking, turning over as it flies, rolling along the ground; at the
 * end it bursts into short lengths of log and split planks flying apart, tumbling, and falling.
 */
public class FlyingLogRenderer extends EntityRenderer<FlyingLog> {
    private static final int CHUNKS = 4;
    private static final int SHARDS = 14;

    public FlyingLogRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(FlyingLog log, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int light) {
        float age = log.tickCount + partialTicks;
        float since = age - log.getPhaseTick();
        int seed = log.getId() * 389;
        poseStack.pushPose();
        poseStack.translate(0, FlyingLog.RADIUS, 0);
        poseStack.mulPose(Axis.YP.rotationDegrees(-log.getYRot()));
        if (log.getPhase() == FlyingLog.Phase.SHATTER) {
            shatter(poseStack, buffer, light, since, seed);
        } else {
            if (log.getPhase() == FlyingLog.Phase.RISE) {
                // Shaking as it is torn up
                poseStack.mulPose(Axis.ZP.rotation(.06f * Mth.sin(age * 2.7f)));
                poseStack.mulPose(Axis.XP.rotation(.05f * Mth.sin(age * 3.1f)));
            }
            poseStack.mulPose(Axis.XP.rotation(Mth.lerp(partialTicks, log.spinO, log.spin)));
            WoodRender.log(poseStack, buffer, light, FlyingLog.LENGTH, FlyingLog.RADIUS, seed, true);
        }
        poseStack.popPose();
        super.render(log, entityYaw, partialTicks, poseStack, buffer, light);
    }

    /** Lengths of log and split planks flying apart from where it burst. */
    private static void shatter(PoseStack poseStack, MultiBufferSource buffer, int light, float t, int seed) {
        float shrink = Mth.clamp((FlyingLog.SHATTER_TICKS - t) / 5, 0, 1);
        if (shrink <= 0) {
            return;
        }
        float gravity = .045f;
        for (int k = 0; k < CHUNKS; k++) {
            float x0 = (k - (CHUNKS - 1) * .5f) * FlyingLog.LENGTH / CHUNKS;
            float vx = x0 * .12f + (WoodFx.hash(seed + k) - .5f) * .1f;
            float vy = .28f + .14f * WoodFx.hash(seed + k + 9);
            float vz = (WoodFx.hash(seed + k + 17) - .4f) * .25f;
            float y = Math.max(-FlyingLog.RADIUS * .6f, vy * t - gravity * t * t * .5f);
            poseStack.pushPose();
            poseStack.translate(x0 + vx * t, y, vz * t);
            poseStack.mulPose(Axis.ZP.rotation(t * (WoodFx.hash(seed + k + 3) - .5f) * .5f));
            poseStack.mulPose(Axis.XP.rotation(t * .3f * (k % 2 == 0 ? 1 : -1)));
            poseStack.scale(shrink, shrink, shrink);
            WoodRender.log(poseStack, buffer, light, FlyingLog.LENGTH / CHUNKS * .9f, FlyingLog.RADIUS * .92f, seed + k * 13, false);
            poseStack.popPose();
        }
        VertexConsumer grain = WoodRender.grain(buffer);
        for (int k = 0; k < SHARDS; k++) {
            float a = WoodFx.hash(seed + k * 5) * Mth.TWO_PI;
            float speed = .2f + .25f * WoodFx.hash(seed + k * 5 + 1);
            float vy = .25f + .3f * WoodFx.hash(seed + k * 5 + 2);
            float y = Math.max(-FlyingLog.RADIUS + .05f, vy * t - gravity * t * t * .5f);
            poseStack.pushPose();
            poseStack.translate(Mth.cos(a) * speed * t + (WoodFx.hash(seed + k) - .5f) * FlyingLog.LENGTH, y, Mth.sin(a) * speed * t);
            poseStack.mulPose(Axis.YP.rotation(a));
            poseStack.mulPose(Axis.XP.rotation(t * (.4f + .6f * WoodFx.hash(seed + k * 7))));
            poseStack.mulPose(Axis.ZP.rotation(t * .3f));
            float len = (.35f + .4f * WoodFx.hash(seed + k * 11)) * shrink;
            WoodRender.box(grain, poseStack.last(), .06f * shrink, .03f * shrink, len, light, 0xFFFFFFFF);
            poseStack.popPose();
        }
    }

    @Override
    public ResourceLocation getTextureLocation(FlyingLog log) {
        return WoodRender.BARK;
    }
}
