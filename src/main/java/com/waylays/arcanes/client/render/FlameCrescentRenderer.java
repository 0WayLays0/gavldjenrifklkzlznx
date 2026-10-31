package com.waylays.arcanes.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.waylays.arcanes.client.render.fire.CrescentModel;
import com.waylays.arcanes.client.render.fire.FireMesh;
import com.waylays.arcanes.entity.fire.FlameCrescent;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

/** Flame Crescent: drawn by {@link CrescentModel}. */
public class FlameCrescentRenderer extends EntityRenderer<FlameCrescent> {
    public FlameCrescentRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(FlameCrescent e, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int light) {
        float age = e.tickCount + partialTicks;
        CrescentModel.draw(poseStack, buffer, WaterViews.of(this.entityRenderDispatcher, e, partialTicks), e.getDir().toVector3f(), FlameCrescent.SPAN, e.getTilt(), age,
                e.faded(age), e.getHeat(), e.getId() * 53, light);
        super.render(e, entityYaw, partialTicks, poseStack, buffer, light);
    }

    @Override
    public boolean shouldRender(FlameCrescent e, Frustum frustum, double x, double y, double z) {
        return frustum.isVisible(e.getBoundingBox().inflate(4));
    }

    @Override
    public ResourceLocation getTextureLocation(FlameCrescent e) {
        return FireMesh.GLOW;
    }
}
