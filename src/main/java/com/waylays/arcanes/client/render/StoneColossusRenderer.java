package com.waylays.arcanes.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.waylays.arcanes.client.render.ritual.ColossusModel;
import com.waylays.arcanes.client.render.ritual.RitualMesh;
import com.waylays.arcanes.entity.ritual.StoneColossus;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;

/**
 * The Stone Colossus, drawn as real blocks (cobblestone, mossy cobblestone, stone, andesite, cobbled deepslate, tuff)
 * laid out by {@link ColossusModel}, turned to its body's heading. No name over its head.
 */
public class StoneColossusRenderer extends EntityRenderer<StoneColossus> {
    private static final BlockState[] KINDS = {Blocks.COBBLESTONE.defaultBlockState(), Blocks.MOSSY_COBBLESTONE.defaultBlockState(),
            Blocks.STONE.defaultBlockState(), Blocks.ANDESITE.defaultBlockState(), Blocks.COBBLED_DEEPSLATE.defaultBlockState(), Blocks.TUFF.defaultBlockState()};

    private final BlockRenderDispatcher blocks;

    public StoneColossusRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.blocks = context.getBlockRenderDispatcher();
        this.shadowRadius = 1.4f;
    }

    @Override
    public void render(StoneColossus colossus, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        float yaw = Mth.rotLerp(partialTicks, colossus.yBodyRotO, colossus.yBodyRot);
        int overlay = OverlayTexture.pack(OverlayTexture.u(0), OverlayTexture.v(colossus.hurtTime > 0));
        poseStack.pushPose();
        Quaternionf turn = Axis.YP.rotationDegrees(-yaw);
        poseStack.mulPose(turn);
        ColossusModel.Stones stones = (ps, kind) -> blocks.renderSingleBlock(KINDS[Mth.clamp(kind, 0, KINDS.length - 1)], ps, buffer, packedLight, overlay);
        ColossusModel.draw(poseStack, buffer, stones, WaterViews.of(this.entityRenderDispatcher, colossus, partialTicks, Vec3.ZERO, turn),
                colossus.age(partialTicks), colossus.walkAnimation.position(partialTicks), Math.min(1, colossus.walkAnimation.speed(partialTicks) * 1.6f),
                colossus.sinceSlam(partialTicks), colossus.sinceEnd(partialTicks), colossus.getId() * 13, packedLight);
        poseStack.popPose();
    }

    @Override
    public ResourceLocation getTextureLocation(StoneColossus colossus) {
        return RitualMesh.MENHIR;
    }
}
