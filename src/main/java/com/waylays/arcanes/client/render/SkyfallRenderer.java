package com.waylays.arcanes.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.waylays.arcanes.client.render.wind.SkyfallModel;
import com.waylays.arcanes.entity.wind.Skyfall;
import com.waylays.arcanes.client.render.wind.WindMesh;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/** Skyfall: the updraft carrying its victim up, then the downburst. */
public class SkyfallRenderer extends EntityRenderer<Skyfall> {
    public SkyfallRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(Skyfall e, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int light) {
        float age = e.tickCount + partialTicks;
        SkyfallModel.draw(poseStack, buffer, WaterViews.of(this.entityRenderDispatcher, e, partialTicks), age, e.lift(age), e.slam(age), Skyfall.HEIGHT,
                e.getId() * 43, light);
        super.render(e, entityYaw, partialTicks, poseStack, buffer, light);
    }

    @Override
    public boolean shouldRender(Skyfall e, Frustum frustum, double x, double y, double z) {
        return true;
    }

    @Override
    public ResourceLocation getTextureLocation(Skyfall e) {
        return WindMesh.STREAK;
    }
}
