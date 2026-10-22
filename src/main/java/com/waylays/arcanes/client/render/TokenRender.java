package com.waylays.arcanes.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.waylays.arcanes.registry.ModItems;
import com.waylays.arcanes.spells.token.TokenFx;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.item.ItemStack;

/**
 * Drawing shared by the Token Magic renderers: the Soul Token itself (the item's own model, the server team's
 * Blockbench token) at any size, and the cyan glow around it.
 */
public final class TokenRender {
    /** The model's token: 1.4 pixels wide, its middle at (8, 6.05, 8) in the model's sixteenths. */
    private static final float MODEL_WIDTH = 1.4f / 16;
    private static final float MODEL_MIDDLE_Y = 6.05f / 16;
    private static ItemStack stack;

    private TokenRender() {}

    /**
     * A token {@code size} blocks wide, centred on the origin, lying flat (its face up, along Y); turn the pose to
     * stand it up or spin it. Lit by {@code light} (full bright for a glowing token).
     */
    public static void token(PoseStack poseStack, MultiBufferSource buffer, float size, int light) {
        token(poseStack, buffer, size, light, 1, 1, 1);
    }

    /** Like {@link #token(PoseStack, MultiBufferSource, float, int)}, its colours multiplied by ({@code r}, {@code g}, {@code b}). */
    public static void token(PoseStack poseStack, MultiBufferSource buffer, float size, int light, float r, float g, float b) {
        if (stack == null) {
            stack = new ItemStack(ModItems.SOUL_TOKEN.get());
        }
        BakedModel model = Minecraft.getInstance().getItemRenderer().getModel(stack, null, null, 0);
        float k = size / MODEL_WIDTH;
        poseStack.pushPose();
        poseStack.scale(k, k, k);
        poseStack.translate(-.5, -MODEL_MIDDLE_Y, -.5);
        // Its own render type, sent off before any glow drawn over it (see RuneRender#block)
        BlockRenderDispatcher blocks = Minecraft.getInstance().getBlockRenderer();
        blocks.getModelRenderer().renderModel(poseStack.last(), buffer.getBuffer(RenderType.entityCutoutNoCull(TextureAtlas.LOCATION_BLOCKS)),
                null, model, r, g, b, light, OverlayTexture.NO_OVERLAY);
        poseStack.popPose();
    }

    /** A round cyan glow facing whoever looks, {@code radius} wide, at {@code strength} (0 to 1). */
    public static void glow(PoseStack poseStack, MultiBufferSource buffer, EntityRenderDispatcher dispatcher,
                            float radius, float strength) {
        poseStack.pushPose();
        poseStack.mulPose(dispatcher.cameraOrientation());
        GlyphRender.halo(buffer, poseStack.last(), GlyphRender.GLOW, radius, RuneRender.additive(TokenFx.CYAN, strength));
        poseStack.popPose();
    }
}
