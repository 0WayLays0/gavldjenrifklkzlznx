package com.waylays.eraphones.gear.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.waylays.eraphones.gear.block.TvBlock;
import com.waylays.eraphones.gear.block.TvBlockEntity;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.core.Direction;
import org.joml.Matrix4f;

/** A television's lit screen, laid over the glass of its tube when it is on. */
public class TvRenderer implements BlockEntityRenderer<TvBlockEntity> {
    /** Where the glass is on the set facing south (model sixteenths), as its model draws it. */
    public static final float X0 = 2.0f, X1 = 10.4f, Y0 = 3.2f, Y1 = 9.5f, Z = 13.47f;

    @Override
    public void render(TvBlockEntity tv, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        if (!tv.on()) {
            return;
        }
        Direction facing = tv.getBlockState().getValue(TvBlock.FACING);
        pose.pushPose();
        pose.translate(.5, 0, .5);
        pose.mulPose(Axis.YP.rotationDegrees(-facing.toYRot()));
        pose.translate(-.5, 0, -.5);
        VertexConsumer vc = buffers.getBuffer(RenderType.text(TvClient.screen(tv)));
        Matrix4f m = pose.last().pose();
        float x0 = X0 / 16, x1 = X1 / 16, y0 = Y0 / 16, y1 = Y1 / 16, z = Z / 16;
        int full = LightTexture.FULL_BRIGHT;
        vc.addVertex(m, x0, y0, z).setColor(255, 255, 255, 255).setUv(0, 1).setLight(full);
        vc.addVertex(m, x1, y0, z).setColor(255, 255, 255, 255).setUv(1, 1).setLight(full);
        vc.addVertex(m, x1, y1, z).setColor(255, 255, 255, 255).setUv(1, 0).setLight(full);
        vc.addVertex(m, x0, y1, z).setColor(255, 255, 255, 255).setUv(0, 0).setLight(full);
        pose.popPose();
    }
}
