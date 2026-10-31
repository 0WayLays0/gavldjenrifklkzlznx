package com.waylays.arcanes.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.waylays.arcanes.client.render.water.MaelstromModel;
import com.waylays.arcanes.client.render.water.WaterMesh;
import com.waylays.arcanes.entity.water.Maelstrom;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

/** Maelstrom: drawn by {@link MaelstromModel}. */
public class MaelstromRenderer extends EntityRenderer<Maelstrom> {
    public MaelstromRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(Maelstrom whirl, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int light) {
        float age = whirl.tickCount + partialTicks;
        MaelstromModel.draw(poseStack, buffer, WaterViews.of(this.entityRenderDispatcher, whirl, partialTicks), age, whirl.opened(age), Maelstrom.RADIUS,
                whirl.getId() * 71, light);
        super.render(whirl, entityYaw, partialTicks, poseStack, buffer, light);
    }

    @Override
    public boolean shouldRender(Maelstrom whirl, Frustum frustum, double x, double y, double z) {
        return frustum.isVisible(whirl.getBoundingBox().inflate(Maelstrom.RADIUS + 3, 3, Maelstrom.RADIUS + 3));
    }

    @Override
    public ResourceLocation getTextureLocation(Maelstrom whirl) {
        return WaterMesh.WATER;
    }
}
