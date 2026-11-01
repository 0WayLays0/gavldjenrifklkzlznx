package com.waylays.arcanes.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.waylays.arcanes.client.render.wind.GaleModel;
import com.waylays.arcanes.entity.wind.GaleBurst;
import com.waylays.arcanes.client.render.wind.WindMesh;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/** A Gale blasting out of the hand. */
public class GaleBurstRenderer extends EntityRenderer<GaleBurst> {
    public GaleBurstRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(GaleBurst e, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int light) {
        float age = e.tickCount + partialTicks;
        GaleModel.draw(poseStack, buffer, WaterViews.of(this.entityRenderDispatcher, e, partialTicks), e.getDir(), Math.min(1, age / GaleBurst.LIFE), e.getSize(),
                e.getId() * 17, light);
        super.render(e, entityYaw, partialTicks, poseStack, buffer, light);
    }

    @Override
    public boolean shouldRender(GaleBurst e, Frustum frustum, double x, double y, double z) {
        return true;
    }

    @Override
    public ResourceLocation getTextureLocation(GaleBurst e) {
        return WindMesh.STREAK;
    }
}
