package com.waylays.arcanes.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.waylays.arcanes.client.render.sand.DjinnModel;
import com.waylays.arcanes.client.render.sand.GraspModel;
import com.waylays.arcanes.client.render.sand.HourglassModel;
import com.waylays.arcanes.client.render.sand.LensModel;
import com.waylays.arcanes.client.render.sand.SandMesh;
import com.waylays.arcanes.client.render.sand.SandfallModel;
import com.waylays.arcanes.client.render.sand.SerpentModel;
import com.waylays.arcanes.client.render.sand.SimoomModel;
import com.waylays.arcanes.client.render.sand.SwarmModel;
import com.waylays.arcanes.client.render.sand.VeilModel;
import com.waylays.arcanes.client.render.water.WaterMesh;
import com.waylays.arcanes.entity.sand.SandScene;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import org.joml.Vector3f;

/** Sand Magic's scenes, each drawn by its own model from where it stands. */
public class SandSceneRenderer extends EntityRenderer<SandScene> {
    public SandSceneRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(SandScene e, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        float age = e.tickCount + partialTicks;
        WaterMesh.View view = WaterViews.of(this.entityRenderDispatcher, e, partialTicks);
        int light = LightTexture.FULL_BRIGHT;
        int seed = e.getId() * 31;
        float size = e.getSize();
        float since = e.sinceStruck(partialTicks);
        Vector3f target = since < 40 ? e.getTarget() : null;
        switch (e.getKind()) {
            case SWARM -> SwarmModel.draw(poseStack, buffer, view, age, e.getLife(), since, target, seed, light);
            case GRASP -> GraspModel.draw(poseStack, buffer, view, age, e.getLife(), size, SandScene.GRASP_RISE, SandScene.GRASP_CRUSH, seed, light);
            case VEIL -> VeilModel.draw(poseStack, buffer, view, size, age, e.getLife(), since, target, seed, light);
            case HOURGLASS -> HourglassModel.draw(poseStack, buffer, view, size, SandScene.HOURGLASS_HEIGHT, age, e.getLife(), 12, seed, light);
            case SERPENT -> SerpentModel.draw(poseStack, buffer, view, e.getDir(), age, e.getLife(), since, seed, light);
            case LENS -> LensModel.draw(poseStack, buffer, view, SandScene.LENS_HEIGHT, age, e.getLife(), seed, light);
            case SANDFALL -> SandfallModel.draw(poseStack, buffer, view, size, SandScene.SANDFALL_HEIGHT, age, e.getLife(), seed, light);
            case DJINN -> DjinnModel.draw(poseStack, buffer, view, e.getDir(), age, e.getLife(), since, target, seed, light);
            case SIMOOM -> SimoomModel.draw(poseStack, buffer, view, size, age, e.getLife(), since, target, seed, light);
        }
        super.render(e, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    @Override
    public boolean shouldRender(SandScene e, Frustum frustum, double x, double y, double z) {
        return true;
    }

    @Override
    public ResourceLocation getTextureLocation(SandScene e) {
        return SandMesh.FLOW;
    }
}
