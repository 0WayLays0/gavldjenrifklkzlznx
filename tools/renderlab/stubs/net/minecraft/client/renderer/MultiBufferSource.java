package net.minecraft.client.renderer;

import com.mojang.blaze3d.vertex.VertexConsumer;

/** Render lab stand-in for Minecraft's MultiBufferSource. */
public interface MultiBufferSource {
    VertexConsumer getBuffer(RenderType type);
}
