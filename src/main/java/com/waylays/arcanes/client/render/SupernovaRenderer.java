package com.waylays.arcanes.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.waylays.arcanes.client.render.fire.FireMesh;
import com.waylays.arcanes.client.render.fire.NovaModel;
import com.waylays.arcanes.entity.fire.Supernova;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

/** A supernova: drawn by {@link NovaModel}. */
public class SupernovaRenderer extends EntityRenderer<Supernova> {
    public SupernovaRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(Supernova e, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int light) {
        float age = e.tickCount + partialTicks;
        NovaModel.draw(poseStack, buffer, WaterViews.of(this.entityRenderDispatcher, e, partialTicks), Math.min(1, age / Supernova.LIFE), e.getRadius(), e.getHeat(),
                e.getId() * 67, light);
        super.render(e, entityYaw, partialTicks, poseStack, buffer, light);
    }

    @Override
    public boolean shouldRender(Supernova e, Frustum frustum, double x, double y, double z) {
        return frustum.isVisible(e.getBoundingBox().inflate(e.getRadius() * 2 + 4));
    }

    @Override
    public ResourceLocation getTextureLocation(Supernova e) {
        return FireMesh.GLOW;
    }
}
