package com.waylays.arcanes.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.waylays.arcanes.entity.wall.WallPiece;
import com.waylays.arcanes.spells.wall.WallFx;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * A moving or bursting wall: brick blocks flying up into place in a ripple from the middle, crenellated, the mortar
 * glowing (hotter as a burst wall is about to blow, with a shudder); sliding when it slides; then its bricks fly
 * apart: forward for a charge or a burst, up and out for the crushing walls.
 */
public class WallPieceRenderer extends EntityRenderer<WallPiece> {
    public WallPieceRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(WallPiece wall, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        float age = wall.tickCount + partialTicks;
        int columns = wall.columns();
        int rows = wall.rows();
        float height = wall.getHeight();
        float width = wall.getWidth();
        float cell = width / columns;
        int seed = wall.getId() * 97;
        int shattered = wall.getShattered();
        Vec3 off = wall.offset(partialTicks);
        float heat = wall.heat(partialTicks);
        float shake = wall.getKind() == WallPiece.Kind.BURST && heat > .6f ? (heat - .6f) * .25f * Mth.sin(age * 4.3f) : 0;

        if (age < wall.built() + 10) {
            WallRender.sigil(poseStack, buffer, width * .6f + 1, Mth.clamp(age / 5, 0, 1) * Mth.clamp(1 - (age - wall.built()) / 10, 0, 1), age * 1.4f);
        }
        poseStack.pushPose();
        poseStack.translate(off.x + shake, 0, off.z + shake * .7f);
        poseStack.mulPose(Axis.YP.rotationDegrees(-wall.getYRot()));
        // Two passes, the bricks then their mortar: a buffer must not be written to again once another one has been
        // asked for (asking for it sends the first off)
        for (int pass = 0; pass < 2; pass++) {
            VertexConsumer stroke = pass == 1 && shattered < 0 ? buffer.getBuffer(RenderType.eyes(GlyphRender.STROKE)) : null;
            for (int c = 0; c < columns; c++) {
                float x = -width * .5f + cell * (c + .5f);
                for (int r = 0; r <= rows; r++) {
                    boolean merlon = r == rows;
                    if (merlon && (c % 2 != 0 && columns > 1)) {
                        continue;
                    }
                    float y = merlon ? height : r;
                    int s = seed + c * 13 + r * 7;
                    BlockState state = WallFx.brick(s);
                    if (shattered >= 0) {
                        if (pass == 0) {
                            fly(wall, state, poseStack, buffer, packedLight, x, y + .5f, age - shattered, s);
                        }
                        continue;
                    }
                    float since = age - wall.cellDelay(c, Math.min(r, rows - 1)) - (merlon ? 2 : 0);
                    if (since < 0) {
                        continue;
                    }
                    float t = Math.min(1, since / WallPiece.CELL_RISE);
                    if (pass == 0) {
                        float lift = -(1 - t) * (1 - t) * 2.4f + .15f * Mth.sin(t * Mth.PI);
                        if (merlon) {
                            RuneRender.block(state, poseStack, buffer, packedLight, x, y + lift, 0, .9f, .5f, WallPiece.THICK, 0, 0);
                        } else {
                            WallRender.brick(state, poseStack, buffer, packedLight, x, y + lift, 0, cell);
                        }
                    } else if (stroke != null && !merlon && t >= 1) {
                        float flare = Math.max(0, 1 - (since - WallPiece.CELL_RISE) / 8);
                        float pulse = .35f + .15f * Mth.sin(age * .2f + c + r);
                        WallRender.mortar(stroke, poseStack.last(), x, y, 0, 0, .07f + heat * .05f,
                                WallRender.ember(Math.max(heat, flare), Math.min(1, pulse + heat * .6f + flare)));
                    }
                }
            }
        }
        poseStack.popPose();
        super.render(wall, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    /** One brick flying apart: forward for a charge or a burst, up and out for the crushing walls. */
    private static void fly(WallPiece wall, BlockState state, PoseStack poseStack, MultiBufferSource buffer, int light, float x, float y, float t, int seed) {
        float h1 = RuneRender.hash(seed), h2 = RuneRender.hash(seed + 1), h3 = RuneRender.hash(seed + 2);
        float vx, vy, vz;
        if (wall.getKind() == WallPiece.Kind.SLAM) {
            vx = (h1 - .5f) * .35f;
            vy = .3f + .3f * h2;
            vz = -.1f - .15f * h3;
        } else {
            vx = x * .06f + (h1 - .5f) * .2f;
            vy = .18f + .25f * h2;
            vz = .35f + .35f * h3;
        }
        WallRender.fragment(state, poseStack, buffer, light, x, y, 0, vx, vy, vz, t, WallPiece.SHATTER_TICKS, .55f + .3f * h1, seed);
    }

    @Override
    public boolean shouldRender(WallPiece wall, Frustum frustum, double x, double y, double z) {
        return frustum.isVisible(wall.getBoundingBox().inflate(wall.getWidth() + 14, wall.getHeight() + 4, wall.getWidth() + 14));
    }

    @Override
    public ResourceLocation getTextureLocation(WallPiece wall) {
        return WallRender.SIGIL;
    }
}
