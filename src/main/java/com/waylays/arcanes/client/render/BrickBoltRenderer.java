package com.waylays.arcanes.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.waylays.arcanes.entity.wall.BrickBolt;
import com.waylays.arcanes.spells.wall.WallFx;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

/** A flying brick, tumbling: one stone brick block for a bolt, a lump of four for a mortar, a half block in the rain. */
public class BrickBoltRenderer extends EntityRenderer<BrickBolt> {
    public BrickBoltRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(BrickBolt bolt, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        float age = bolt.tickCount + partialTicks;
        int seed = bolt.getId() * 41;
        poseStack.pushPose();
        poseStack.translate(0, bolt.getBbHeight() * .5f, 0);
        poseStack.mulPose(Axis.YP.rotationDegrees(age * 19 + seed));
        poseStack.mulPose(Axis.XP.rotationDegrees(age * 27));
        switch (bolt.getKind()) {
            case BOLT -> RuneRender.block(WallFx.brick(seed), poseStack, buffer, packedLight, 0, -.25f, 0, .5f, .5f, .5f, 0, 0);
            case FALLING -> RuneRender.block(WallFx.brick(seed), poseStack, buffer, packedLight, 0, -.3f, 0, .6f, .6f, .6f, 0, 0);
            case MORTAR -> {
                for (int i = 0; i < 4; i++) {
                    float x = (i & 1) * .5f - .25f;
                    float z = (i >> 1) * .5f - .25f;
                    RuneRender.block(WallFx.brick(seed + i), poseStack, buffer, packedLight, x, -.5f + (i == 3 ? .4f : 0), z, .55f, .55f, .55f, 0, 0);
                }
            }
        }
        poseStack.popPose();
        super.render(bolt, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(BrickBolt bolt) {
        return RuneRender.GLOW;
    }
}
