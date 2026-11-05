package com.waylays.arcanes.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.waylays.arcanes.client.render.melody.MelodyInstrumentModels;
import com.waylays.arcanes.client.render.melody.MelodyMesh;
import com.waylays.arcanes.client.render.melody.MelodyModels;
import com.waylays.arcanes.client.render.water.WaterMesh;
import com.waylays.arcanes.entity.melody.MelodyScene;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import org.joml.Vector3f;

/** Music Magic's notes, staves and phantom instruments, each drawn by its own model from where it stands. */
public class MelodySceneRenderer extends EntityRenderer<MelodyScene> {
    public MelodySceneRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(MelodyScene e, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        float age = e.tickCount + partialTicks;
        WaterMesh.View view = WaterViews.of(this.entityRenderDispatcher, e, partialTicks);
        int light = LightTexture.FULL_BRIGHT;
        int seed = e.getId() * 31;
        float size = e.getSize();
        float since = e.sinceStruck(partialTicks);
        boolean struck = since < 1.0E6f && since < e.tickCount + 2;
        Vector3f target = struck ? e.getTarget() : null;
        Vector3f dir = e.getDir();
        int life = e.getLife();
        switch (e.getKind()) {
            case NOTE -> MelodyModels.note(poseStack, buffer, view, dir, age, e.isDone(), since, seed, light);
            case STAFF -> MelodyModels.staffSlash(poseStack, buffer, view, dir, age, life, MelodyScene.SLASH_REACH, MelodyScene.SLASH_ARC, MelodyScene.SLASH_SWEEP, seed,
                    light);
            case SOOTHE -> MelodyModels.soothe(poseStack, buffer, view, age, life, size, MelodyScene.SOOTHE_EVERY, seed, light);
            case CRESCENDO -> MelodyModels.crescendo(poseStack, buffer, view, dir, age, e.isDone(), since, MelodyScene.CRESC_FLIGHT, MelodyScene.CRESC_FROM,
                    MelodyScene.CRESC_TO, seed, light);
            case LULLABY -> MelodyModels.lullaby(poseStack, buffer, view, age, life, size, MelodyScene.LULL_RISE, seed, light);
            case DRUM -> MelodyInstrumentModels.drum(poseStack, buffer, view, dir, age, life, MelodyScene.DRUM_RISE, MelodyScene.DRUM_FIRST, MelodyScene.DRUM_EVERY,
                    (float) MelodyScene.DRUM_REACH, seed, light);
            case HARP -> MelodyInstrumentModels.harp(poseStack, buffer, view, dir, MelodyScene.harpAt(dir), age, life, MelodyScene.HARP_RISE, MelodyScene.HARP_EVERY,
                    MelodyScene.HARP_RAY, since, target, seed, light);
            case TRUMPETS -> MelodyInstrumentModels.trumpets(poseStack, buffer, view, dir, age, life, MelodyScene.TRUMPETS, MelodyScene.TRUMP_RISE,
                    MelodyScene.TRUMP_FIRST, MelodyScene.TRUMP_EVERY, MelodyScene.TRUMP_BLASTS, k -> MelodyScene.trumpetAt(k, dir), (float) MelodyScene.TRUMP_REACH,
                    seed, light);
            case ORGAN -> MelodyInstrumentModels.organ(poseStack, buffer, view, dir, age, life, size, MelodyScene.ORGAN_RISE, MelodyScene.ORGAN_EVERY,
                    MelodyScene.ORGAN_FINALE, MelodyScene.ORGAN_FALL, since, target, seed, light);
            case ORCHESTRA -> MelodyInstrumentModels.orchestra(poseStack, buffer, view, age, life, size, MelodyScene.ORCH_RISE, MelodyScene.ORCH_VIOLINS,
                    k -> MelodyScene.violinAt(k, age), MelodyScene.ORCH_FINALE, MelodyScene.ORCH_FALL, since, target, seed, light);
        }
        super.render(e, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    @Override
    public boolean shouldRender(MelodyScene e, Frustum frustum, double x, double y, double z) {
        return true;
    }

    @Override
    public ResourceLocation getTextureLocation(MelodyScene e) {
        return MelodyMesh.GOLD_METAL;
    }
}
