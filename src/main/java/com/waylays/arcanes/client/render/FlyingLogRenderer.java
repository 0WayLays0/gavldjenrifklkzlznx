package com.waylays.arcanes.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.waylays.arcanes.entity.wood.FlyingLog;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/** A hurled log: an oak log turning end over end as it flies, a few leaves still on it. */
public class FlyingLogRenderer extends EntityRenderer<FlyingLog> {
    public FlyingLogRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(FlyingLog log, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        float age = log.tickCount + partialTicks;
        Vec3 motion = log.getDeltaMovement();
        float yaw = (float) Mth.atan2(motion.x, motion.z);
        poseStack.pushPose();
        poseStack.translate(0, log.getBbHeight() * .5f, 0);
        poseStack.mulPose(Axis.YP.rotation(yaw));
        // End over end
        poseStack.mulPose(Axis.XP.rotationDegrees(age * 28));
        WoodRender.log(WoodRender.LOG, poseStack, buffer, packedLight, 0, -.85f, 0, .75f, 1.7f);
        RuneRender.block(WoodRender.leaves(log.getId()), poseStack, buffer, packedLight, .35f, .5f, .1f, .35f, .35f, .35f, 20, 15);
        poseStack.popPose();
        super.render(log, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(FlyingLog log) {
        return WoodRender.BARK;
    }
}
