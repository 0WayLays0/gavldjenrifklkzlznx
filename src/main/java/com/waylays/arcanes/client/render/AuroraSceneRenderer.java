package com.waylays.arcanes.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.waylays.arcanes.client.render.aurora.AuroraMesh;
import com.waylays.arcanes.client.render.aurora.AuroraModels;
import com.waylays.arcanes.client.render.aurora.AuroraSkyModels;
import com.waylays.arcanes.client.render.water.WaterMesh;
import com.waylays.arcanes.entity.aurora.AuroraScene;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import org.joml.Vector3f;

/** Aurora Magic's scenes, each drawn by its own model from where it stands. */
public class AuroraSceneRenderer extends EntityRenderer<AuroraScene> {
    public AuroraSceneRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(AuroraScene e, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        float age = e.tickCount + partialTicks;
        WaterMesh.View view = WaterViews.of(this.entityRenderDispatcher, e, partialTicks);
        int light = LightTexture.FULL_BRIGHT;
        int seed = e.getId() * 31;
        float size = e.getSize();
        float since = e.sinceStruck(partialTicks);
        Vector3f target = since < 60 ? e.getTarget() : null;
        switch (e.getKind()) {
            case LANCE -> AuroraModels.lance(poseStack, buffer, view, e.getDir(), age, e.isDone(), since, seed, light);
            case WISP -> AuroraModels.wisp(poseStack, buffer, view, new Vector3f((float) (e.getX() - e.xo), (float) (e.getY() - e.yo), (float) (e.getZ() - e.zo)), age,
                    e.isDone(), since, e.getId() % 5, seed, light);
            case CURTAIN -> AuroraModels.curtain(poseStack, buffer, view, e.getDir(), age, e.getLife(), size, AuroraScene.CURTAIN_HEIGHT, seed, light);
            case GLIDE -> AuroraModels.glide(poseStack, buffer, view, e.getTarget(), age, e.getLife(), AuroraScene.GLIDE_TICKS, seed, light);
            case MANTLE -> AuroraModels.mantle(poseStack, buffer, view, age, e.getLife(), size, seed, light);
            case BLESSING -> AuroraModels.blessing(poseStack, buffer, view, age, e.getLife(), size, AuroraScene.BLESSING_LAND, seed, light);
            case SPIRAL -> AuroraSkyModels.spiral(poseStack, buffer, view, age, e.getLife(), size, seed, light);
            case SERPENT -> AuroraSkyModels.serpent(poseStack, buffer, view, age, e.getLife(), since, target, AuroraScene.SERPENT_RADIUS, AuroraScene.SERPENT_HEIGHT,
                    AuroraScene.SERPENT_SPIN, AuroraScene.SERPENT_DIVE, seed, light);
            case HALO -> AuroraSkyModels.halo(poseStack, buffer, view, age, e.getLife(), size, AuroraScene.HALO_HEIGHT, AuroraScene.HALO_OPEN, since, target, seed, light);
            case NIGHT -> AuroraSkyModels.night(poseStack, buffer, view, age, e.getLife(), size, AuroraScene.NIGHT_FALL, AuroraScene.NIGHT_WAVE, AuroraScene.NIGHT_SWEEP,
                    AuroraScene.NIGHT_GATHER, seed, light);
        }
        super.render(e, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    @Override
    public boolean shouldRender(AuroraScene e, Frustum frustum, double x, double y, double z) {
        return true;
    }

    @Override
    public ResourceLocation getTextureLocation(AuroraScene e) {
        return AuroraMesh.RAYS;
    }
}
