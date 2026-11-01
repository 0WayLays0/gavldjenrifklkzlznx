package com.waylays.arcanes.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.waylays.arcanes.client.render.wind.WindWardModel;
import com.waylays.arcanes.entity.wind.WindWard;
import com.waylays.arcanes.client.render.wind.WindMesh;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/** Tailwind's rings of wind round the one it wards. */
public class WindWardRenderer extends EntityRenderer<WindWard> {
    public WindWardRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(WindWard e, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int light) {
        float age = e.tickCount + partialTicks;
        net.minecraft.world.entity.Entity holder = e.getHolder();
        float height = holder != null ? holder.getBbHeight() : 1.8f;
        float alive = Math.min(1, age / 8) * Math.max(0, Math.min(1, (e.getLife() - age) / 20));
        if (holder != null && alive > 0) {
            Vec3 at = holder.getPosition(partialTicks).subtract(e.getPosition(partialTicks));
            poseStack.pushPose();
            poseStack.translate(at.x, at.y, at.z);
            WindWardModel.draw(poseStack, buffer, WaterViews.of(this.entityRenderDispatcher, e, partialTicks, at, null), age, height, alive, e.getId() * 23, light);
            poseStack.popPose();
        }
        super.render(e, entityYaw, partialTicks, poseStack, buffer, light);
    }

    @Override
    public boolean shouldRender(WindWard e, Frustum frustum, double x, double y, double z) {
        return true;
    }

    @Override
    public ResourceLocation getTextureLocation(WindWard e) {
        return WindMesh.STREAK;
    }
}
