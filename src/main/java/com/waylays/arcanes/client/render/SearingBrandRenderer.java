package com.waylays.arcanes.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.waylays.arcanes.client.render.fire.BrandModel;
import com.waylays.arcanes.client.render.fire.FireMesh;
import com.waylays.arcanes.entity.fire.SearingBrand;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/** Searing Brand: drawn by {@link BrandModel} over its creature's head. */
public class SearingBrandRenderer extends EntityRenderer<SearingBrand> {
    public SearingBrandRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(SearingBrand e, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int light) {
        float age = e.tickCount + partialTicks;
        Entity target = e.getTarget();
        float tall = target != null ? target.getBbHeight() : 1.8f;
        Vec3 shift = target != null ? target.getPosition(partialTicks).subtract(e.getPosition(partialTicks)).add(0, tall + .6f, 0) : new Vec3(0, tall + .6f, 0);
        poseStack.pushPose();
        poseStack.translate(shift.x, shift.y, shift.z);
        BrandModel.draw(poseStack, buffer, WaterViews.of(this.entityRenderDispatcher, e, partialTicks, shift, null), age, e.fuse(age), tall + .6f, e.getHeat(),
                e.getId() * 59, light);
        poseStack.popPose();
        super.render(e, entityYaw, partialTicks, poseStack, buffer, light);
    }

    @Override
    public boolean shouldRender(SearingBrand e, Frustum frustum, double x, double y, double z) {
        return frustum.isVisible(e.getBoundingBox().inflate(3, 4, 3));
    }

    @Override
    public ResourceLocation getTextureLocation(SearingBrand e) {
        return FireMesh.GLOW;
    }
}
