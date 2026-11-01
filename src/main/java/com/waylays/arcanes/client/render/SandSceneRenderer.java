package com.waylays.arcanes.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.waylays.arcanes.client.render.sand.BlastModel;
import com.waylays.arcanes.client.render.sand.DuneWaveModel;
import com.waylays.arcanes.client.render.sand.ObeliskModel;
import com.waylays.arcanes.client.render.sand.PillarModel;
import com.waylays.arcanes.client.render.sand.PyramidModel;
import com.waylays.arcanes.client.render.sand.QuicksandModel;
import com.waylays.arcanes.client.render.sand.SandMesh;
import com.waylays.arcanes.client.render.sand.SandstormModel;
import com.waylays.arcanes.client.render.sand.SarcophagusModel;
import com.waylays.arcanes.client.render.sand.WormModel;
import com.waylays.arcanes.client.render.water.WaterMesh;
import com.waylays.arcanes.entity.sand.SandScene;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

/** Sand Magic's scenes, each drawn by its own model from where it stands. */
public class SandSceneRenderer extends EntityRenderer<SandScene> {
    /** A sand blast's cone: radians either side of ahead. */
    public static final float BLAST_HALF = .52f;

    public SandSceneRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(SandScene e, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int light) {
        float age = e.tickCount + partialTicks;
        WaterMesh.View view = WaterViews.of(this.entityRenderDispatcher, e, partialTicks);
        int seed = e.getId() * 31;
        float size = e.getSize();
        float life = e.getLife();
        switch (e.getKind()) {
            case BLAST -> BlastModel.draw(poseStack, buffer, view, e.getDir(), size, BLAST_HALF, age, seed, light);
            case QUICKSAND -> QuicksandModel.draw(poseStack, buffer, view, size, age, life, seed, light);
            case PILLAR -> PillarModel.draw(poseStack, buffer, view, size, age, life, seed, light);
            case STORM -> SandstormModel.draw(poseStack, buffer, view, size, age, life, seed, light);
            case WAVE -> DuneWaveModel.draw(poseStack, buffer, view, e.getDir(), size, SandScene.WAVE_HEIGHT, age, 1 - Mth.clamp((age - life + 8) / 8, 0, 1), seed,
                    light);
            case WORM -> WormModel.draw(poseStack, buffer, view, e.getDir(), size, age, seed, light);
            case SARCOPHAGUS -> SarcophagusModel.draw(poseStack, buffer, view, 2.1f, age, life, seed, light);
            case OBELISK -> {
                Vector3f target = e.sinceStruck(partialTicks) < ObeliskModel.BEAM ? e.getTarget() : null;
                ObeliskModel.draw(poseStack, buffer, view, size, age, life, e.sinceStruck(partialTicks), target, seed, light);
            }
            case PYRAMID -> PyramidModel.draw(poseStack, buffer, view, size, SandScene.PYRAMID_HEIGHT, age, age - (SandScene.PYRAMID_RISE + SandScene.PYRAMID_HOLD),
                    seed, light);
        }
        super.render(e, entityYaw, partialTicks, poseStack, buffer, light);
    }

    @Override
    public boolean shouldRender(SandScene e, Frustum frustum, double x, double y, double z) {
        return true;
    }

    @Override
    public ResourceLocation getTextureLocation(SandScene e) {
        return SandMesh.SANDSTONE;
    }
}
