package com.waylays.arcanes.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.waylays.arcanes.entity.wall.WallPiece;
import com.waylays.arcanes.spells.wall.WallFx;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * A wall of Wall Magic, laid brick block by brick block (stone, mossy and cracked stone bricks only, picked steadily
 * from its id), crenellated on top; a pillar is one block wide with a wider cap. It bursts out of the ground, slides
 * when it slides, shudders as it sinks.
 */
public class WallPieceRenderer extends EntityRenderer<WallPiece> {
    public WallPieceRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(WallPiece wall, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        float risen = wall.risen(partialTicks);
        if (risen <= .001f) {
            return;
        }
        float age = wall.tickCount + partialTicks;
        if (wall.getKind() == WallPiece.Kind.BURST && age >= WallPiece.RISE + WallPiece.BURST_DELAY) {
            return;
        }
        float h = wall.getHeight();
        int seed = wall.getId() * 97;
        Vec3 off = wall.offset(partialTicks);
        boolean sinking = age > wall.getLife();
        // Shudders as it sinks, and trembles before a burst
        float shake = sinking ? .05f * Mth.sin(age * 3.3f)
                : wall.getKind() == WallPiece.Kind.BURST && age > WallPiece.RISE ? .04f * (age - WallPiece.RISE) / WallPiece.BURST_DELAY * Mth.sin(age * 5.1f) : 0;
        poseStack.pushPose();
        poseStack.translate(off.x + shake, -(1 - risen) * h, off.z + shake * .6f);
        poseStack.mulPose(Axis.YP.rotationDegrees(-wall.getYRot()));
        if (wall.getKind() == WallPiece.Kind.PILLAR) {
            pillar(poseStack, buffer, packedLight, seed, h);
        } else {
            wall(poseStack, buffer, packedLight, seed, wall.getWidth(), h);
        }
        poseStack.popPose();
        super.render(wall, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    /** Rows of blocks, the last row a part block, merlons on every other block of the top. */
    static void wall(PoseStack poseStack, MultiBufferSource buffer, int light, int seed, float width, float height) {
        int columns = Math.max(1, Math.round(width));
        float cell = width / columns;
        float body = height - .5f;
        int rows = Mth.ceil(body);
        for (int c = 0; c < columns; c++) {
            float x = -width * .5f + cell * (c + .5f);
            for (int r = 0; r < rows; r++) {
                float rowHeight = Math.min(1, body - r);
                // Every other row is offset a little, as bricks are laid
                float jog = (r & 1) * .02f;
                RuneRender.block(WallFx.brick(seed + c * 13 + r * 7), poseStack, buffer, light, x + jog, r, 0, cell + .002f, rowHeight + .002f, WallPiece.THICK, 0, 0);
            }
            if ((c & 1) == 0 || columns == 1) {
                RuneRender.block(WallFx.brick(seed + c * 13 + 101), poseStack, buffer, light, x, body, 0, cell * .9f, .5f, WallPiece.THICK * 1.05f, 0, 0);
            }
        }
    }

    private static void pillar(PoseStack poseStack, MultiBufferSource buffer, int light, int seed, float height) {
        int rows = Mth.ceil(height - .35f);
        for (int r = 0; r < rows; r++) {
            float rowHeight = Math.min(1, height - .35f - r);
            RuneRender.block(WallFx.brick(seed + r * 7), poseStack, buffer, light, 0, r, 0, .8f, rowHeight, .8f, 0, 0);
        }
        RuneRender.block(WallFx.brick(seed + 55), poseStack, buffer, light, 0, height - .35f, 0, 1f, .35f, 1f, 0, 0);
    }

    @Override
    public boolean shouldRender(WallPiece wall, Frustum frustum, double x, double y, double z) {
        return frustum.isVisible(wall.getBoundingBox().inflate(wall.getWidth() + 12, wall.getHeight() + 1, wall.getWidth() + 12));
    }

    @Override
    public ResourceLocation getTextureLocation(WallPiece wall) {
        return RuneRender.GLOW;
    }
}
