package com.waylays.arcanes.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.waylays.arcanes.entity.verdigris.ToppleWall;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The Toppling Wall: a stretch of ancient rampart, mossy and cracked stone with a band of green copper set in it and
 * a carved copper seal in the middle, crenellated, hung with vines. It bursts up, sways, and falls flat forward.
 */
public class ToppleWallRenderer extends EntityRenderer<ToppleWall> {
    private static final float THICK = .8f;

    public ToppleWallRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(ToppleWall wall, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        float risen = wall.risen(partialTicks);
        if (risen <= .001f) {
            return;
        }
        int seed = wall.getId();
        int columns = Math.round(ToppleWall.WIDTH);
        float left = -ToppleWall.WIDTH * .5f + .5f;

        poseStack.pushPose();
        poseStack.translate(0, -(1 - risen) * ToppleWall.HEIGHT, 0);
        poseStack.mulPose(Axis.YP.rotationDegrees(-wall.getYRot()));
        // It falls forward (+Z here), turning on its front foot
        poseStack.translate(0, 0, THICK * .5f);
        poseStack.mulPose(Axis.XP.rotationDegrees(wall.fallen(partialTicks)));
        poseStack.translate(0, 0, -THICK * .5f);
        for (int c = 0; c < columns; c++) {
            float x = left + c;
            for (int row = 0; row < 3; row++) {
                BlockState stone;
                if (row == 1) {
                    stone = c == columns / 2 ? VerdigrisRender.OXIDIZED_CHISELED : VerdigrisRender.OXIDIZED_CUT;
                } else {
                    float h = RuneRender.hash(seed * 17 + c * 5 + row);
                    stone = h < .45f ? VerdigrisRender.MOSSY_BRICKS : h < .75f ? VerdigrisRender.CRACKED_BRICKS : h < .9f ? VerdigrisRender.CHISELED_BRICKS : VerdigrisRender.MOSSY_COBBLE;
                }
                // The copper seal stands a little proud of the wall
                float depth = row == 1 && c == columns / 2 ? THICK + .14f : THICK;
                VerdigrisRender.block(stone, poseStack, buffer, packedLight, x, row, 0, 1.001f, 1.001f, depth);
            }
            if (c % 2 == 0) {
                VerdigrisRender.block(VerdigrisRender.MOSSY_COBBLE, poseStack, buffer, packedLight, x, 3, 0, 1, .5f, THICK);
                VerdigrisRender.block(VerdigrisRender.MOSS_CARPET, poseStack, buffer, packedLight, x, 3.5f, 0, 1, 1, THICK);
            } else {
                VerdigrisRender.block(VerdigrisRender.MOSS_CARPET, poseStack, buffer, packedLight, x, 3, 0, 1, 1, THICK);
            }
            // Vines hang down the front and back, longer on some stones than others
            float front = RuneRender.hash(seed * 7 + c) < .3f ? 0 : 1 + 1.6f * RuneRender.hash(seed * 11 + c);
            float back = RuneRender.hash(seed * 13 + c) < .5f ? 0 : .6f + RuneRender.hash(seed * 19 + c);
            poseStack.pushPose();
            poseStack.translate(x, 0, 0);
            VerdigrisRender.vines(poseStack, buffer, packedLight, 3, 1, THICK, new float[]{back, 0, front, 0});
            poseStack.popPose();
        }
        poseStack.popPose();
        super.render(wall, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    @Override
    public boolean shouldRender(ToppleWall wall, Frustum frustum, double x, double y, double z) {
        return frustum.isVisible(wall.getBoundingBox().inflate(ToppleWall.HEIGHT + 1));
    }

    @Override
    public ResourceLocation getTextureLocation(ToppleWall wall) {
        return RuneRender.GLOW;
    }
}
