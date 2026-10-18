package com.waylays.arcanes.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.waylays.arcanes.ArcanesOublies;
import com.waylays.arcanes.entity.gold.GoldSpikeEntity;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * A cluster of gold crystal spikes that slides out of the ground, holds, then sinks back.
 */
public class GoldSpikeRenderer extends EntityRenderer<GoldSpikeEntity> {
    private static final ResourceLocation CRYSTAL = ArcanesOublies.id("textures/entity/gold_crystal.png");
    private static final ResourceLocation GLOW = GoldItemProjectileRenderer.GLOW;

    public GoldSpikeRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(GoldSpikeEntity entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        float t = entity.tickCount + partialTicks - entity.getWarmup();
        if (t < 0) {
            return;
        }
        float rise = Mth.clamp(t / GoldSpikeEntity.RISE_TICKS, 0f, 1f);
        float sink = Mth.clamp((t - (GoldSpikeEntity.OUT_TICKS - GoldSpikeEntity.SINK_TICKS)) / GoldSpikeEntity.SINK_TICKS, 0f, 1f);
        float out = rise * (1 - sink * sink);
        int light = LightTexture.pack(Math.max(12, LightTexture.block(packedLight)), LightTexture.sky(packedLight));

        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(entity.getYRot()));

        // Flash of light on the ground right when the spike comes out
        if (t < 8) {
            int a = (int) (255 * (1 - t / 8f));
            poseStack.pushPose();
            poseStack.translate(0, .02, 0);
            RenderHelper.groundQuad(buffer.getBuffer(RenderType.eyes(GLOW)), poseStack.last(), 1.1f, (a << 24) | (a << 16) | (a << 8) | a, LightTexture.FULL_BRIGHT);
            poseStack.popPose();
        }

        VertexConsumer crystal = buffer.getBuffer(RenderType.entityCutoutNoCull(CRYSTAL));
        spike(crystal, poseStack, 0, 0, 0f, .34f, 1.8f, out, light);
        spike(crystal, poseStack, .32f, .1f, 22f, .2f, 1.0f, out, light);
        spike(crystal, poseStack, -.26f, .28f, -26f, .18f, .8f, out, light);
        spike(crystal, poseStack, -.05f, -.34f, 18f, .16f, .7f, out, light);
        poseStack.popPose();

        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    /** A four-sided pyramid standing at (x, z), tilted outward, pushed {@code (1 - out)} of its height into the ground. */
    private static void spike(VertexConsumer consumer, PoseStack poseStack, float x, float z, float tiltDegrees, float half, float height, float out, int light) {
        poseStack.pushPose();
        poseStack.translate(x, 0, z);
        if (tiltDegrees != 0) {
            float yaw = (float) Mth.atan2(z, x);
            poseStack.mulPose(Axis.YP.rotation(-yaw));
            poseStack.mulPose(Axis.ZP.rotationDegrees(-tiltDegrees));
            poseStack.mulPose(Axis.YP.rotation(yaw));
        }
        poseStack.translate(0, -(1 - out) * height, 0);
        PoseStack.Pose pose = poseStack.last();
        float[][] corners = {{-half, -half}, {half, -half}, {half, half}, {-half, half}};
        for (int i = 0; i < 4; i++) {
            float[] a = corners[i];
            float[] b = corners[(i + 1) % 4];
            // Outward normal of this face, leaning up because of the slope
            float nx = (a[0] + b[0]) * .5f;
            float nz = (a[1] + b[1]) * .5f;
            float len = Mth.sqrt(nx * nx + nz * nz + half * half * .1f);
            RenderHelper.vertex(consumer, pose, a[0], 0, a[1], 0, 1, 0xFFFFFFFF, light, nx / len, .3f, nz / len);
            RenderHelper.vertex(consumer, pose, b[0], 0, b[1], 1, 1, 0xFFFFFFFF, light, nx / len, .3f, nz / len);
            RenderHelper.vertex(consumer, pose, 0, height, 0, .5f, 0, 0xFFFFFFFF, light, nx / len, .3f, nz / len);
            RenderHelper.vertex(consumer, pose, 0, height, 0, .5f, 0, 0xFFFFFFFF, light, nx / len, .3f, nz / len);
        }
        poseStack.popPose();
    }

    @Override
    public ResourceLocation getTextureLocation(GoldSpikeEntity entity) {
        return CRYSTAL;
    }
}
