package com.waylays.arcanes.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.waylays.arcanes.client.render.fire.FireMesh;
import com.waylays.arcanes.client.render.fire.PhoenixModel;
import com.waylays.arcanes.entity.fire.Phoenix;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;

/** The Phoenix: drawn by {@link PhoenixModel}, turned to its heading. */
public class PhoenixRenderer extends EntityRenderer<Phoenix> {
    public PhoenixRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(Phoenix e, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int light) {
        float age = e.tickCount + partialTicks;
        Vec3 head = e.heading(age);
        Quaternionf turn = new Quaternionf().rotationY((float) Math.atan2(head.x, head.z)).rotateX((float) -Math.asin(Math.max(-1, Math.min(1, head.y))));
        poseStack.pushPose();
        poseStack.mulPose(turn);
        PhoenixModel.draw(poseStack, buffer, WaterViews.of(this.entityRenderDispatcher, e, partialTicks, Vec3.ZERO, turn), age, 0, e.faded(age), 1.2f, e.getHeat(),
                e.getId() * 61, light);
        poseStack.popPose();
        super.render(e, entityYaw, partialTicks, poseStack, buffer, light);
    }

    @Override
    public boolean shouldRender(Phoenix e, Frustum frustum, double x, double y, double z) {
        return frustum.isVisible(e.getBoundingBox().inflate(8));
    }

    @Override
    public ResourceLocation getTextureLocation(Phoenix e) {
        return FireMesh.GLOW;
    }
}
