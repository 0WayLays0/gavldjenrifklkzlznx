package com.waylays.arcanes.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.waylays.arcanes.entity.rune.RuneShard;
import com.waylays.arcanes.spells.rune.RuneFx;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/** A shard of polished deepslate spinning along its flight, a glowing rune riding on it. */
public class RuneShardRenderer extends EntityRenderer<RuneShard> {
    private static final int FULL = LightTexture.FULL_BRIGHT;
    private static final BlockState STONE = Blocks.POLISHED_DEEPSLATE.defaultBlockState();

    public RuneShardRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(RuneShard shard, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        float age = shard.tickCount + partialTicks;
        Vec3 v = shard.getDeltaMovement();
        float yaw = (float) (Mth.atan2(v.x, v.z) * Mth.RAD_TO_DEG);
        float pitch = (float) (Mth.atan2(v.y, v.horizontalDistance()) * Mth.RAD_TO_DEG);
        poseStack.pushPose();
        poseStack.translate(0, shard.getBbHeight() * .5f, 0);
        // The stone, along the flight, spinning (first: the glow over it writes no depth)
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(yaw));
        poseStack.mulPose(Axis.XP.rotationDegrees(-pitch));
        poseStack.mulPose(Axis.ZP.rotationDegrees(age * 30));
        poseStack.translate(0, -.1f, -.3f);
        RuneRender.block(STONE, poseStack, buffer, packedLight, 0, 0, 0, .2f, .2f, .62f, 0, 0);
        poseStack.translate(0, .1f, .62f);
        poseStack.mulPose(Axis.ZP.rotationDegrees(45));
        RuneRender.block(STONE, poseStack, buffer, packedLight, 0, -.07f, 0, .14f, .14f, .2f, 0, 0);
        poseStack.popPose();
        // Then its glow and rune, turned to the viewer
        poseStack.pushPose();
        poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
        RenderHelper.billboard(buffer.getBuffer(RenderType.eyes(RuneRender.GLOW)), poseStack.last(), .45f, RuneRender.additive(RuneFx.GLOW, .7f), FULL);
        RuneRender.glyph(buffer.getBuffer(RenderType.eyes(RuneRender.GLYPHS)), poseStack.last(), shard.getId(), .38f, RuneRender.additive(RuneFx.SILVER, 1), FULL);
        poseStack.popPose();
        poseStack.popPose();
        super.render(shard, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(RuneShard entity) {
        return RuneRender.GLYPHS;
    }
}
