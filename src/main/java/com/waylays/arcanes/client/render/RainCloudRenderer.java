package com.waylays.arcanes.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.waylays.arcanes.client.render.water.RainCloudModel;
import com.waylays.arcanes.client.render.water.WaterMesh;
import com.waylays.arcanes.entity.water.RainCloud;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/** Cleansing Rain: drawn by {@link RainCloudModel} over the caster, where they are drawn (not a tick behind). */
public class RainCloudRenderer extends EntityRenderer<RainCloud> {
    public RainCloudRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(RainCloud cloud, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int light) {
        float age = cloud.tickCount + partialTicks;
        float thick = cloud.thickness(age);
        if (thick <= .01f) {
            return;
        }
        Entity owner = cloud.follows();
        Vec3 shift = owner != null ? owner.getPosition(partialTicks).subtract(cloud.getPosition(partialTicks)) : Vec3.ZERO;
        poseStack.pushPose();
        poseStack.translate(shift.x, shift.y, shift.z);
        RainCloudModel.draw(poseStack, buffer, WaterViews.of(this.entityRenderDispatcher, cloud, partialTicks, shift, null), age, thick, RainCloud.RADIUS,
                RainCloud.HEIGHT, cloud.getId() * 61, light);
        poseStack.popPose();
        super.render(cloud, entityYaw, partialTicks, poseStack, buffer, light);
    }

    @Override
    public boolean shouldRender(RainCloud cloud, Frustum frustum, double x, double y, double z) {
        return frustum.isVisible(cloud.getBoundingBox().inflate(RainCloud.RADIUS * 2.5, RainCloud.HEIGHT + 6, RainCloud.RADIUS * 2.5));
    }

    @Override
    public ResourceLocation getTextureLocation(RainCloud cloud) {
        return WaterMesh.CLOUD;
    }
}
