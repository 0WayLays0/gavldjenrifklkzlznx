package com.waylays.arcanes.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.waylays.arcanes.client.render.fire.FireMesh;
import com.waylays.arcanes.client.render.fire.FlameRingModel;
import com.waylays.arcanes.entity.fire.Corona;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

/** Corona: drawn by {@link FlameRingModel}. */
public class CoronaRenderer extends EntityRenderer<Corona> {
    public CoronaRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(Corona e, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int light) {
        float age = e.tickCount + partialTicks;
        FlameRingModel.draw(poseStack, buffer, WaterViews.of(this.entityRenderDispatcher, e, partialTicks), age, e.reach(age), Corona.HEIGHT, e.faded(age), e.getHeat(),
                e.getId() * 41, light);
        super.render(e, entityYaw, partialTicks, poseStack, buffer, light);
    }

    @Override
    public boolean shouldRender(Corona e, Frustum frustum, double x, double y, double z) {
        return frustum.isVisible(e.getBoundingBox().inflate(14, 4, 14));
    }

    @Override
    public ResourceLocation getTextureLocation(Corona e) {
        return FireMesh.GLOW;
    }
}
