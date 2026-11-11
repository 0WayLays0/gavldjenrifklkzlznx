package com.waylays.eraphones.horror.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.waylays.eraphones.EraPhones;
import com.waylays.eraphones.horror.block.DollBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;

/** A doll's porcelain head on its neck, turned where its block entity says. */
public class DollRenderer implements BlockEntityRenderer<DollBlockEntity> {
    /** The head's picture: front, back, right, left in a row; top and bottom under them. */
    public static final ResourceLocation HEAD = EraPhones.id("textures/block/poupee_tete.png");
    /** Where the neck is (model sixteenths, the doll facing south) and the head's half width and height. */
    public static final float NX = 8, NY = 9.4f, NZ = 7.6f, HALF = 2.6f, TALL = 5.2f;

    @Override
    public void render(DollBlockEntity doll, float pt, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        float facing = doll.getBlockState().getValue(HorizontalDirectionalBlock.FACING).toYRot();
        float yaw = Mth.rotLerp(pt, doll.lastYaw, doll.yaw), pitch = Mth.lerp(pt, doll.lastPitch, doll.pitch);
        pose.pushPose();
        pose.translate(.5, 0, .5);
        pose.mulPose(Axis.YP.rotationDegrees(-facing));
        pose.translate(-.5, 0, -.5);
        pose.translate(NX / 16, NY / 16, NZ / 16);
        pose.mulPose(Axis.YP.rotationDegrees(-yaw));
        pose.mulPose(Axis.XP.rotationDegrees(pitch));
        VertexConsumer vc = buffers.getBuffer(RenderType.entityCutoutNoCull(HEAD));
        PoseStack.Pose p = pose.last();
        float h = HALF / 16, t = TALL / 16;
        // front, back, right (east), left (west), top, bottom
        face(vc, p, light, overlay, 0, 0, 0, 0, 1, -h, t, h, h, t, h, h, 0, h, -h, 0, h);
        face(vc, p, light, overlay, 1, 0, 0, 0, -1, h, t, -h, -h, t, -h, -h, 0, -h, h, 0, -h);
        face(vc, p, light, overlay, 2, 0, 1, 0, 0, h, t, h, h, t, -h, h, 0, -h, h, 0, h);
        face(vc, p, light, overlay, 3, 0, -1, 0, 0, -h, t, -h, -h, t, h, -h, 0, h, -h, 0, -h);
        face(vc, p, light, overlay, 0, 1, 0, 1, 0, -h, t, -h, h, t, -h, h, t, h, -h, t, h);
        face(vc, p, light, overlay, 1, 1, 0, -1, 0, -h, 0, h, h, 0, h, h, 0, -h, -h, 0, -h);
        pose.popPose();
    }

    /** One face, its picture the cell (col, row) of a 4 x 2 grid, its corners top left, top right, bottom right, bottom left. */
    private static void face(VertexConsumer vc, PoseStack.Pose p, int light, int overlay, int col, int row, float nx, float ny, float nz,
                             float... c) {
        float u0 = col / 4f, u1 = (col + 1) / 4f, v0 = row / 2f, v1 = (row + 1) / 2f;
        float[][] uv = {{u0, v0}, {u1, v0}, {u1, v1}, {u0, v1}};
        for (int i = 0; i < 4; i++) {
            vc.addVertex(p, c[i * 3], c[i * 3 + 1], c[i * 3 + 2]).setColor(255, 255, 255, 255).setUv(uv[i][0], uv[i][1])
                    .setOverlay(overlay).setLight(light).setNormal(p, nx, ny, nz);
        }
    }
}
