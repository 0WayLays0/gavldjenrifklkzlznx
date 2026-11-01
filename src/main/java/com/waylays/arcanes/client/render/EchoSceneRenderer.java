package com.waylays.arcanes.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.waylays.arcanes.client.render.echo.BoomModel;
import com.waylays.arcanes.client.render.echo.BurstModel;
import com.waylays.arcanes.client.render.echo.DomeModel;
import com.waylays.arcanes.client.render.echo.EchoMesh;
import com.waylays.arcanes.client.render.echo.RequiemModel;
import com.waylays.arcanes.client.render.echo.SensorModel;
import com.waylays.arcanes.client.render.echo.ShriekModel;
import com.waylays.arcanes.client.render.echo.ShriekerModel;
import com.waylays.arcanes.client.render.echo.StepModel;
import com.waylays.arcanes.client.render.echo.TendrilsModel;
import com.waylays.arcanes.client.render.water.WaterMesh;
import com.waylays.arcanes.entity.echo.EchoScene;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import org.joml.Vector3f;

/** Echo Magic's scenes, each drawn by its own model from where it stands. */
public class EchoSceneRenderer extends EntityRenderer<EchoScene> {
    /** A shriek's cone: radians either side of ahead. */
    public static final float SHRIEK_HALF = .62f;

    public EchoSceneRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(EchoScene e, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        float age = e.tickCount + partialTicks;
        WaterMesh.View view = WaterViews.of(this.entityRenderDispatcher, e, partialTicks);
        int light = LightTexture.FULL_BRIGHT;
        int seed = e.getId() * 29;
        float size = e.getSize();
        Vector3f target = e.sinceStruck(partialTicks) < 40 ? e.getTarget() : null;
        switch (e.getKind()) {
            case SHRIEK -> ShriekModel.draw(poseStack, buffer, view, e.getDir(), size, SHRIEK_HALF, age, seed, light);
            case STEP -> StepModel.draw(poseStack, buffer, view, age, EchoScene.STEP_DELAY - age, size, (float) Math.atan2(-e.getDir().x, e.getDir().z), seed,
                    light);
            case TENDRILS -> TendrilsModel.draw(poseStack, buffer, view, size, age, e.getLife(), seed, light);
            case SENSOR -> SensorModel.draw(poseStack, buffer, view, age, e.getLife(), e.sinceStruck(partialTicks), target, seed, light);
            case BURST -> BurstModel.draw(poseStack, buffer, view, age, size, seed, light);
            case BOOM -> BoomModel.draw(poseStack, buffer, view, e.getDir(), size, e.getArg(), age, light);
            case DOME -> DomeModel.draw(poseStack, buffer, view, size, age, e.getLife(), seed, light);
            case SHRIEKER -> ShriekerModel.draw(poseStack, buffer, view, age, e.getLife(), e.sinceStruck(partialTicks), target, seed, light);
            case REQUIEM -> RequiemModel.draw(poseStack, buffer, view, size, age, e.getLife(), EchoScene.REQUIEM_FIRST, EchoScene.REQUIEM_STEP,
                    EchoScene.REQUIEM_WAVES, seed, light);
        }
        super.render(e, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    @Override
    public boolean shouldRender(EchoScene e, Frustum frustum, double x, double y, double z) {
        return true;
    }

    @Override
    public ResourceLocation getTextureLocation(EchoScene e) {
        return EchoMesh.WAVE;
    }
}
