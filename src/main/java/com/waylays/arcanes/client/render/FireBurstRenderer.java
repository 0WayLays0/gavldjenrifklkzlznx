package com.waylays.arcanes.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.waylays.arcanes.client.render.fire.FireBurstModel;
import com.waylays.arcanes.client.render.fire.FireMesh;
import com.waylays.arcanes.entity.fire.FireBurst;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

/** A burst of Stellar Fire: drawn by {@link FireBurstModel}. */
public class FireBurstRenderer extends EntityRenderer<FireBurst> {
    public FireBurstRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(FireBurst e, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int light) {
        float age = e.tickCount + partialTicks;
        FireBurstModel.draw(poseStack, buffer, WaterViews.of(this.entityRenderDispatcher, e, partialTicks), Math.min(1, age / FireBurst.LIFE), e.getSize(),
                e.getHeat(), e.getId() * 29, light);
        super.render(e, entityYaw, partialTicks, poseStack, buffer, light);
    }

    @Override
    public boolean shouldRender(FireBurst e, Frustum frustum, double x, double y, double z) {
        return frustum.isVisible(e.getBoundingBox().inflate(e.getSize() * 3 + 2));
    }

    @Override
    public ResourceLocation getTextureLocation(FireBurst e) {
        return FireMesh.GLOW;
    }
}
