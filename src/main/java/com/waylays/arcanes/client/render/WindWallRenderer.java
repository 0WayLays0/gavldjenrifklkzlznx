package com.waylays.arcanes.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.waylays.arcanes.client.render.wind.WindWallModel;
import com.waylays.arcanes.entity.wind.WindWall;
import com.waylays.arcanes.client.render.wind.WindMesh;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/** The Wind Wall, turned across the way it faces. */
public class WindWallRenderer extends EntityRenderer<WindWall> {
    public WindWallRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(WindWall e, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int light) {
        float age = e.tickCount + partialTicks;
        org.joml.Quaternionf turn = com.mojang.math.Axis.YP.rotationDegrees(-e.getYRot());
        poseStack.pushPose();
        poseStack.mulPose(turn);
        WindWallModel.draw(poseStack, buffer, WaterViews.of(this.entityRenderDispatcher, e, partialTicks, Vec3.ZERO, turn), age, WindWall.WIDTH, WindWall.HEIGHT,
                e.alive(age), e.getId() * 37, light);
        poseStack.popPose();
        super.render(e, entityYaw, partialTicks, poseStack, buffer, light);
    }

    @Override
    public boolean shouldRender(WindWall e, Frustum frustum, double x, double y, double z) {
        return true;
    }

    @Override
    public ResourceLocation getTextureLocation(WindWall e) {
        return WindMesh.STREAK;
    }
}
