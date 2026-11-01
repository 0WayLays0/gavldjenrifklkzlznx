package com.waylays.arcanes.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.waylays.arcanes.client.render.wind.CycloneModel;
import com.waylays.arcanes.entity.wind.Cyclone;
import com.waylays.arcanes.client.render.wind.WindMesh;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/** The Cyclone: drawn by CycloneModel. */
public class CycloneRenderer extends EntityRenderer<Cyclone> {
    public CycloneRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(Cyclone e, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int light) {
        float age = e.tickCount + partialTicks;
        CycloneModel.draw(poseStack, buffer, WaterViews.of(this.entityRenderDispatcher, e, partialTicks), age, e.grow(age), e.fade(age), Cyclone.HEIGHT, Cyclone.TOP,
                e.getId() * 29, light);
        super.render(e, entityYaw, partialTicks, poseStack, buffer, light);
    }

    @Override
    public boolean shouldRender(Cyclone e, Frustum frustum, double x, double y, double z) {
        return true;
    }

    @Override
    public ResourceLocation getTextureLocation(Cyclone e) {
        return WindMesh.STREAK;
    }
}
