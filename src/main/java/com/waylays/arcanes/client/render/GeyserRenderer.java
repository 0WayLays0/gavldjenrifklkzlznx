package com.waylays.arcanes.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.waylays.arcanes.client.render.water.GeyserModel;
import com.waylays.arcanes.client.render.water.WaterMesh;
import com.waylays.arcanes.entity.water.Geyser;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/** Geyser: drawn by {@link GeyserModel}. */
public class GeyserRenderer extends EntityRenderer<Geyser> {
    public GeyserRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(Geyser geyser, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int light) {
        float age = geyser.tickCount + partialTicks;
        float warn = Mth.clamp(age / Geyser.WARN, 0, 1);
        float end = Mth.clamp((age - Geyser.WARN - Geyser.RISE - Geyser.SPOUT) / Geyser.FALL, 0, 1);
        GeyserModel.draw(poseStack, buffer, WaterViews.of(this.entityRenderDispatcher, geyser, partialTicks), age, warn, geyser.column(age), end,
                geyser.getHeight(), Geyser.RADIUS, geyser.getId() * 53, light);
        super.render(geyser, entityYaw, partialTicks, poseStack, buffer, light);
    }

    @Override
    public boolean shouldRender(Geyser geyser, Frustum frustum, double x, double y, double z) {
        return frustum.isVisible(geyser.getBoundingBox().inflate(Geyser.RADIUS * 4, geyser.getHeight() + 6, Geyser.RADIUS * 4));
    }

    @Override
    public ResourceLocation getTextureLocation(Geyser geyser) {
        return WaterMesh.WATER;
    }
}
