package com.waylays.arcanes.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.waylays.arcanes.client.render.fire.FireMesh;
import com.waylays.arcanes.client.render.fire.FirePillarModel;
import com.waylays.arcanes.entity.fire.FirePillar;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

/** A pillar of Stellar Fire: drawn by {@link FirePillarModel}. */
public class FirePillarRenderer extends EntityRenderer<FirePillar> {
    public FirePillarRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(FirePillar e, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int light) {
        float age = e.tickCount + partialTicks;
        if (e.since(age) < 0) {
            return;
        }
        FirePillarModel.draw(poseStack, buffer, WaterViews.of(this.entityRenderDispatcher, e, partialTicks), e.since(age), e.warn(age), e.rise(age), e.sink(age),
                FirePillar.HEIGHT, FirePillar.RADIUS, e.getHeat(), e.getId() * 43, light);
        super.render(e, entityYaw, partialTicks, poseStack, buffer, light);
    }

    @Override
    public boolean shouldRender(FirePillar e, Frustum frustum, double x, double y, double z) {
        return frustum.isVisible(e.getBoundingBox().inflate(4, FirePillar.HEIGHT + 5, 4));
    }

    @Override
    public ResourceLocation getTextureLocation(FirePillar e) {
        return FireMesh.GLOW;
    }
}
