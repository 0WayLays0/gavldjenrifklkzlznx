package com.waylays.arcanes.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.waylays.arcanes.client.render.wind.UpdraftModel;
import com.waylays.arcanes.entity.wind.UpdraftGust;
import com.waylays.arcanes.client.render.wind.WindMesh;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/** An Updraft spiralling up from the ground. */
public class UpdraftGustRenderer extends EntityRenderer<UpdraftGust> {
    public UpdraftGustRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(UpdraftGust e, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int light) {
        float age = e.tickCount + partialTicks;
        UpdraftModel.draw(poseStack, buffer, WaterViews.of(this.entityRenderDispatcher, e, partialTicks), age, Math.min(1, age / UpdraftGust.LIFE), e.getSize(),
                1.2f, 1.2f, e.getId() * 19, light);
        super.render(e, entityYaw, partialTicks, poseStack, buffer, light);
    }

    @Override
    public boolean shouldRender(UpdraftGust e, Frustum frustum, double x, double y, double z) {
        return true;
    }

    @Override
    public ResourceLocation getTextureLocation(UpdraftGust e) {
        return WindMesh.STREAK;
    }
}
