package com.waylays.arcanes.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.waylays.arcanes.client.render.lotus.BudModel;
import com.waylays.arcanes.client.render.lotus.LanternModel;
import com.waylays.arcanes.client.render.lotus.LotusMesh;
import com.waylays.arcanes.client.render.lotus.MandalaModel;
import com.waylays.arcanes.client.render.lotus.PetalModel;
import com.waylays.arcanes.client.render.lotus.PodModel;
import com.waylays.arcanes.client.render.lotus.PondModel;
import com.waylays.arcanes.client.render.lotus.RootsModel;
import com.waylays.arcanes.client.render.lotus.PetalStormModel;
import com.waylays.arcanes.client.render.lotus.ThousandModel;
import com.waylays.arcanes.client.render.lotus.ThroneModel;
import com.waylays.arcanes.client.render.water.WaterMesh;
import com.waylays.arcanes.entity.lotus.LotusScene;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import org.joml.Vector3f;

/** Lotus Magic's scenes, each drawn by its own model from where it stands. */
public class LotusSceneRenderer extends EntityRenderer<LotusScene> {
    public LotusSceneRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(LotusScene e, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        float age = e.tickCount + partialTicks;
        WaterMesh.View view = WaterViews.of(this.entityRenderDispatcher, e, partialTicks);
        int light = LightTexture.FULL_BRIGHT;
        int seed = e.getId() * 31;
        float size = e.getSize();
        float since = e.sinceStruck(partialTicks);
        Vector3f target = since < 40 ? e.getTarget() : null;
        switch (e.getKind()) {
            case PETAL -> PetalModel.draw(poseStack, buffer, view, e.getDir(), age, e.isDone(), since, seed, light);
            case BUD -> BudModel.draw(poseStack, buffer, view, age, e.getLife(), size, seed, light);
            case POND -> PondModel.draw(poseStack, buffer, view, age, e.getLife(), size, seed, light);
            case THRONE -> ThroneModel.draw(poseStack, buffer, view, e.getDir(), age, e.getLife(), since, target, seed, light);
            case ROOTS -> RootsModel.draw(poseStack, buffer, view, age, e.getLife(), size, seed, light);
            case POD -> PodModel.draw(poseStack, buffer, view, e.getDir(), age, e.getLife(), since, target, seed, light);
            case MANDALA -> MandalaModel.draw(poseStack, buffer, view, age, e.getLife(), size, seed, light);
            case LANTERN -> LanternModel.draw(poseStack, buffer, view, age, e.getLife(), e.isDone(), since, e.getId() % 7, seed, light);
            case STORM -> PetalStormModel.draw(poseStack, buffer, view, age, e.getLife(), size, seed, light);
            case THOUSAND -> ThousandModel.draw(poseStack, buffer, view, age, e.getLife(), size, since, target, seed, light);
        }
        super.render(e, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    @Override
    public boolean shouldRender(LotusScene e, Frustum frustum, double x, double y, double z) {
        return true;
    }

    @Override
    public ResourceLocation getTextureLocation(LotusScene e) {
        return LotusMesh.PETAL;
    }
}
