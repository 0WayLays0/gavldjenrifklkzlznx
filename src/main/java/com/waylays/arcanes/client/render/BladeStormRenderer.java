package com.waylays.arcanes.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.waylays.arcanes.client.render.wind.BladeStormModel;
import com.waylays.arcanes.entity.wind.BladeStorm;
import com.waylays.arcanes.client.render.wind.WindMesh;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/** The Storm of Blades wheeling round its caster. */
public class BladeStormRenderer extends EntityRenderer<BladeStorm> {
    public BladeStormRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(BladeStorm e, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int light) {
        float age = e.tickCount + partialTicks;
        BladeStormModel.draw(poseStack, buffer, WaterViews.of(this.entityRenderDispatcher, e, partialTicks), age, e.getCount(), BladeStorm.RADIUS, e.alive(age),
                e.getId() * 41, light);
        super.render(e, entityYaw, partialTicks, poseStack, buffer, light);
    }

    @Override
    public boolean shouldRender(BladeStorm e, Frustum frustum, double x, double y, double z) {
        return true;
    }

    @Override
    public ResourceLocation getTextureLocation(BladeStorm e) {
        return WindMesh.STREAK;
    }
}
