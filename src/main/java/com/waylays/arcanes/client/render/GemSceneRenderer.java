package com.waylays.arcanes.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.waylays.arcanes.client.render.gem.AmethystModels;
import com.waylays.arcanes.client.render.gem.DiamondModels;
import com.waylays.arcanes.client.render.gem.EmeraldModels;
import com.waylays.arcanes.client.render.gem.GemMesh;
import com.waylays.arcanes.client.render.gem.LapisModels;
import com.waylays.arcanes.client.render.gem.RubyModels;
import com.waylays.arcanes.client.render.water.WaterMesh;
import com.waylays.arcanes.entity.gem.GemScene;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import org.joml.Vector3f;

/** Gem Magic's newer scenes, each drawn by its own model from where it stands. */
public class GemSceneRenderer extends EntityRenderer<GemScene> {
    public GemSceneRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(GemScene e, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        float age = e.tickCount + partialTicks;
        WaterMesh.View view = WaterViews.of(this.entityRenderDispatcher, e, partialTicks);
        int light = LightTexture.FULL_BRIGHT;
        int seed = e.getId() * 31;
        float size = e.getSize();
        float since = e.sinceStruck(partialTicks);
        Vector3f target = since < 40 ? e.getTarget() : null;
        switch (e.getKind()) {
            case SHARD -> RubyModels.shard(poseStack, buffer, view, e.getDir(), age, e.isDone(), since, seed, light);
            case NOVA -> RubyModels.nova(poseStack, buffer, view, age, e.getLife(), GemScene.NOVA_CHARGE, size, seed, light);
            case METEOR -> RubyModels.meteor(poseStack, buffer, view, age, e.getLife(), GemScene.METEOR_FALL, GemScene.METEOR_HEIGHT, size, seed, light);
            case STAR -> LapisModels.star(poseStack, buffer, view, e.getDir(), age, e.isDone(), since, seed, light);
            case VAULT -> LapisModels.vault(poseStack, buffer, view, age, e.getLife(), size, GemScene.VAULT_HEIGHT, since, target, seed, light);
            case MIRROR -> LapisModels.mirror(poseStack, buffer, view, e.getDir(), age, e.getLife(), since, target, seed, light);
            case LANCE -> DiamondModels.lance(poseStack, buffer, view, e.getDir(), age, e.getLife(), size, GemScene.LANCE_FLIGHT, seed, light);
            case DUST -> DiamondModels.dust(poseStack, buffer, view, age, e.getLife(), size, seed, light);
            case BASTION -> DiamondModels.bastion(poseStack, buffer, view, age, e.getLife(), size, GemScene.BASTION_SHATTER, seed, light);
            case SPIKES -> AmethystModels.spikes(poseStack, buffer, view, e.getDir(), age, e.getLife(), GemScene.SPIKES, seed, light);
            case BUDDING -> AmethystModels.budding(poseStack, buffer, view, age, e.getLife(), size, GemScene.BUD_STAGE, seed, light);
            case CHIMES -> AmethystModels.chimes(poseStack, buffer, view, age, e.getLife(), GemScene.CHIME_EVERY, GemScene.CHIME_EVERY, since, target, seed, light);
            case LIFELINE -> EmeraldModels.lifeline(poseStack, buffer, view, age, e.getLife(), e.getTarget(), since, seed, light);
            case AEGIS -> EmeraldModels.aegis(poseStack, buffer, view, age, e.getLife(), seed, light);
            case TREE -> EmeraldModels.tree(poseStack, buffer, view, age, e.getLife(), GemScene.TREE_GROW, size, since, seed, light);
            case THORNS -> EmeraldModels.thorns(poseStack, buffer, view, age, e.getLife(), GemScene.THORNS_RISE, size, seed, light);
        }
        super.render(e, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    @Override
    public boolean shouldRender(GemScene e, Frustum frustum, double x, double y, double z) {
        return true;
    }

    @Override
    public ResourceLocation getTextureLocation(GemScene e) {
        return GemMesh.FACET;
    }
}
