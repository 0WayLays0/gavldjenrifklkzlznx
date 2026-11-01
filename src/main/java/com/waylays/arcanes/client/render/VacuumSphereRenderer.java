package com.waylays.arcanes.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.waylays.arcanes.client.render.wind.VacuumModel;
import com.waylays.arcanes.entity.wind.VacuumSphere;
import com.waylays.arcanes.client.render.wind.WindMesh;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/** The Vacuum Sphere drawing the air in, then bursting. */
public class VacuumSphereRenderer extends EntityRenderer<VacuumSphere> {
    public VacuumSphereRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(VacuumSphere e, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int light) {
        float age = e.tickCount + partialTicks;
        float pull = Math.min(1, age / VacuumSphere.PULL);
        float burst = age < VacuumSphere.PULL ? -1 : Math.min(1, (age - VacuumSphere.PULL) / VacuumSphere.BURST);
        poseStack.pushPose();
        poseStack.translate(0, VacuumSphere.HEART, 0);
        VacuumModel.draw(poseStack, buffer, WaterViews.of(this.entityRenderDispatcher, e, partialTicks, new Vec3(0, VacuumSphere.HEART, 0), null), age, pull, burst,
                VacuumSphere.RADIUS, e.getId() * 31, light);
        poseStack.popPose();
        super.render(e, entityYaw, partialTicks, poseStack, buffer, light);
    }

    @Override
    public boolean shouldRender(VacuumSphere e, Frustum frustum, double x, double y, double z) {
        return true;
    }

    @Override
    public ResourceLocation getTextureLocation(VacuumSphere e) {
        return WindMesh.STREAK;
    }
}
