package com.waylays.arcanes.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.waylays.arcanes.client.render.water.TyphoonModel;
import com.waylays.arcanes.client.render.water.WaterMesh;
import com.waylays.arcanes.entity.water.Typhoon;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/** Typhoon: drawn by {@link TyphoonModel}. */
public class TyphoonRenderer extends EntityRenderer<Typhoon> {
    public TyphoonRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(Typhoon typhoon, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int light) {
        float age = typhoon.tickCount + partialTicks;
        float formed = Mth.clamp(age / Typhoon.FORM, 0, 1);
        float gone = Mth.clamp((age - typhoon.getLife()) / Typhoon.BREAK, 0, 1);
        float flash = typhoon.flashing(age) ? 1 : 0;
        TyphoonModel.draw(poseStack, buffer, WaterViews.of(this.entityRenderDispatcher, typhoon, partialTicks), age, formed, gone, flash, (int) (age / 37),
                Typhoon.HEIGHT, Typhoon.RADIUS, typhoon.getId() * 13, light);
        super.render(typhoon, entityYaw, partialTicks, poseStack, buffer, light);
    }

    @Override
    public boolean shouldRender(Typhoon typhoon, Frustum frustum, double x, double y, double z) {
        return frustum.isVisible(typhoon.getBoundingBox().inflate(14, Typhoon.HEIGHT + 6, 14));
    }

    @Override
    public ResourceLocation getTextureLocation(Typhoon typhoon) {
        return WaterMesh.WATER;
    }
}
