package com.waylays.arcanes.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.waylays.arcanes.client.render.sand.LanceModel;
import com.waylays.arcanes.client.render.sand.SandMesh;
import com.waylays.arcanes.entity.sand.SandLance;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/** A Sand Lance, spinning along the way it flies. */
public class SandLanceRenderer extends EntityRenderer<SandLance> {
    public SandLanceRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(SandLance e, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int light) {
        float age = e.tickCount + partialTicks;
        Vec3 v = e.getDeltaMovement();
        Vector3f forward = v.lengthSqr() > 1.0E-6 ? new Vector3f((float) v.x, (float) v.y, (float) v.z).normalize() : new Vector3f(0, 0, 1);
        float mid = e.getBbHeight() * .5f;
        poseStack.pushPose();
        poseStack.translate(0, mid, 0);
        LanceModel.draw(poseStack, buffer, WaterViews.of(this.entityRenderDispatcher, e, partialTicks, new Vec3(0, mid, 0), null), forward, age, e.getId() * 13,
                light);
        poseStack.popPose();
        super.render(e, entityYaw, partialTicks, poseStack, buffer, light);
    }

    @Override
    public boolean shouldRender(SandLance e, Frustum frustum, double x, double y, double z) {
        return true;
    }

    @Override
    public ResourceLocation getTextureLocation(SandLance e) {
        return SandMesh.FLOW;
    }
}
