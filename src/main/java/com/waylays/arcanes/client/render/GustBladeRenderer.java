package com.waylays.arcanes.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.waylays.arcanes.client.render.wind.GustBladeModel;
import com.waylays.arcanes.entity.wind.GustBlade;
import com.waylays.arcanes.client.render.wind.WindMesh;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/** The Gust Blade, drawn along the way it flies. */
public class GustBladeRenderer extends EntityRenderer<GustBlade> {
    public GustBladeRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(GustBlade e, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int light) {
        float age = e.tickCount + partialTicks;
        Vec3 v = e.getDeltaMovement();
        Vector3f forward = v.lengthSqr() > 1.0E-6 ? new Vector3f((float) v.x, (float) v.y, (float) v.z).normalize() : new Vector3f(0, 0, 1);
        float mid = e.getBbHeight() * .5f;
        poseStack.pushPose();
        poseStack.translate(0, mid, 0);
        GustBladeModel.draw(poseStack, buffer, WaterViews.of(this.entityRenderDispatcher, e, partialTicks, new Vec3(0, mid, 0), null), forward, age, 2.4f,
                Math.min(1, age / 2), e.getId() * 13, light);
        poseStack.popPose();
        super.render(e, entityYaw, partialTicks, poseStack, buffer, light);
    }

    @Override
    public boolean shouldRender(GustBlade e, Frustum frustum, double x, double y, double z) {
        return true;
    }

    @Override
    public ResourceLocation getTextureLocation(GustBlade e) {
        return WindMesh.STREAK;
    }
}
