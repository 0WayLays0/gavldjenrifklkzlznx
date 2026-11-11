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
        vc.vertex(m, x0, y0, z).color(255, 255, 255, 255).uv(0, 1).uv2(full).endVertex();
        vc.vertex(m, x1, y0, z).color(255, 255, 255, 255).uv(1, 1).uv2(full).endVertex();
        vc.vertex(m, x1, y1, z).color(255, 255, 255, 255).uv(1, 0).uv2(full).endVertex();
        vc.vertex(m, x0, y1, z).color(255, 255, 255, 255).uv(0, 0).uv2(full).endVertex();
        // Words over the snow: a film being read, or one that cannot be
        net.minecraft.network.chat.Component caption = TvClient.caption(tv.getBlockPos());
        if (caption != null) {
            net.minecraft.client.gui.Font font = net.minecraft.client.Minecraft.getInstance().font;
            java.util.List<net.minecraft.util.FormattedCharSequence> lines = font.split(caption, 110);
            float scale = (x1 - x0) / 130f;
            pose.translate((x0 + x1) / 2, (y0 + y1) / 2, z + .002f);
            pose.scale(scale, -scale, scale);
            float y = -lines.size() * 10 / 2f;
            for (net.minecraft.util.FormattedCharSequence line : lines) {
                float w = font.width(line);
                font.drawInBatch(line, -w / 2 + 1, y + 1, 0xFF000000, false, pose.last().pose(), buffers, net.minecraft.client.gui.Font.DisplayMode.NORMAL, 0, full);
                font.drawInBatch(line, -w / 2, y, 0xFFFFFFFF, false, pose.last().pose(), buffers, net.minecraft.client.gui.Font.DisplayMode.NORMAL, 0, full);
                y += 10;
            }
        }
        pose.popPose();
    }
}
