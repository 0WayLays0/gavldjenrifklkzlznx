package com.waylays.arcanes.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.waylays.arcanes.client.render.water.SplashModel;
import com.waylays.arcanes.client.render.water.WaterMesh;
import com.waylays.arcanes.entity.water.SplashCrown;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/** A splash of Water Magic: drawn by {@link SplashModel}. */
public class SplashCrownRenderer extends EntityRenderer<SplashCrown> {
    public SplashCrownRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(SplashCrown crown, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int light) {
        float t = Mth.clamp((crown.tickCount + partialTicks) / SplashCrown.LIFE, 0, 1);
        SplashModel.draw(poseStack, buffer, WaterViews.of(this.entityRenderDispatcher, crown, partialTicks), t, crown.getSize(), crown.getId() * 97, light);
        super.render(crown, entityYaw, partialTicks, poseStack, buffer, light);
    }

    @Override
    public ResourceLocation getTextureLocation(SplashCrown crown) {
        return WaterMesh.WATER;
    }
}
