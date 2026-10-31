package com.waylays.arcanes.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.waylays.arcanes.client.render.water.TidalWaveModel;
import com.waylays.arcanes.client.render.water.WaterMesh;
import com.waylays.arcanes.entity.water.TidalWave;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;

/** Tidal Wave: drawn by {@link TidalWaveModel}, turned the way it rolls. */
public class TidalWaveRenderer extends EntityRenderer<TidalWave> {
    public TidalWaveRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(TidalWave wave, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int light) {
        float age = wave.tickCount + partialTicks;
        Quaternionf turn = Axis.YP.rotationDegrees(-wave.getYRot());
        poseStack.pushPose();
        poseStack.mulPose(turn);
        TidalWaveModel.draw(poseStack, buffer, WaterViews.of(this.entityRenderDispatcher, wave, partialTicks, Vec3.ZERO, turn), age, wave.stand(age),
                wave.broken(age), TidalWave.WIDTH, TidalWave.HEIGHT, wave.getId() * 83, light);
        poseStack.popPose();
        super.render(wave, entityYaw, partialTicks, poseStack, buffer, light);
    }

    @Override
    public boolean shouldRender(TidalWave wave, Frustum frustum, double x, double y, double z) {
        return frustum.isVisible(wave.getBoundingBox().inflate(TidalWave.WIDTH, TidalWave.HEIGHT + 4, TidalWave.WIDTH));
    }

    @Override
    public ResourceLocation getTextureLocation(TidalWave wave) {
        return WaterMesh.WATER;
    }
}
