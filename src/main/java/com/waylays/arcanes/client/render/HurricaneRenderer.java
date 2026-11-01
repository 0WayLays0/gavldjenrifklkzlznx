package com.waylays.arcanes.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.waylays.arcanes.client.render.wind.HurricaneModel;
import com.waylays.arcanes.entity.wind.Hurricane;
import com.waylays.arcanes.client.render.wind.WindMesh;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/** The Hurricane wheeling round its caster. */
public class HurricaneRenderer extends EntityRenderer<Hurricane> {
    public HurricaneRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(Hurricane e, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int light) {
        float age = e.tickCount + partialTicks;
        HurricaneModel.draw(poseStack, buffer, WaterViews.of(this.entityRenderDispatcher, e, partialTicks), age, e.grow(age), e.fade(age), Hurricane.EYE,
                Hurricane.RADIUS, Hurricane.HEIGHT, e.getId() * 47, light);
        super.render(e, entityYaw, partialTicks, poseStack, buffer, light);
    }

    @Override
    public boolean shouldRender(Hurricane e, Frustum frustum, double x, double y, double z) {
        return true;
    }

    @Override
    public ResourceLocation getTextureLocation(Hurricane e) {
        return WindMesh.STREAK;
    }
}
