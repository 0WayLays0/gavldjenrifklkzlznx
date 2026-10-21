package com.waylays.arcanes.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.waylays.arcanes.entity.rune.RuneBulwark;
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

/** A slab of the Runic Bulwark: deepslate tiles and a polished capstone, a great rune glowing on each face. */
public class RuneBulwarkRenderer extends EntityRenderer<RuneBulwark> {
    private static final int FULL = LightTexture.FULL_BRIGHT;
    private static final BlockState TILES = Blocks.DEEPSLATE_TILES.defaultBlockState();
    private static final BlockState POLISHED = Blocks.POLISHED_DEEPSLATE.defaultBlockState();
    private static final BlockState BRICKS = Blocks.DEEPSLATE_BRICKS.defaultBlockState();
    private static final float WIDTH = 1.35f;
    private static final float DEPTH = .55f;

    public RuneBulwarkRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(RuneBulwark wall, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        float standing = wall.standing(partialTicks);
        if (standing <= .001f) {
            return;
        }
        float age = wall.tickCount + partialTicks;
        boolean crumbling = age > wall.getLife();
        float shake = crumbling ? .05f * Mth.sin(age * 2.7f) : 0;
        poseStack.pushPose();
        poseStack.translate(shake, -(1 - standing) * 2.6f, 0);
        poseStack.mulPose(Axis.YP.rotationDegrees(-wall.getYRot()));
        RuneRender.block(TILES, poseStack, buffer, packedLight, 0, 0, 0, WIDTH, 1.25f, DEPTH, 0, 0);
        RuneRender.block(BRICKS, poseStack, buffer, packedLight, 0, 1.25f, 0, WIDTH * .94f, .95f, DEPTH * .92f, 0, 0);
        RuneRender.block(POLISHED, poseStack, buffer, packedLight, 0, 2.2f, 0, WIDTH * .78f, .38f, DEPTH * .8f, 0, (RuneRender.hash(wall.getId()) - .5f) * 6);
        float glow = (.65f + .35f * Mth.sin(age * .15f + wall.getId())) * Mth.clamp(age / RuneBulwark.RISE_TICKS, 0, 1) * (crumbling ? 1 - (age - wall.getLife()) / 6 : 1);
        int color = RuneRender.additive(RuneFx.GLOW, glow);
        for (int side = 0; side < 2; side++) {
            poseStack.pushPose();
            poseStack.mulPose(Axis.YP.rotationDegrees(side * 180));
            poseStack.translate(0, .8f, DEPTH * .5f + .012f);
            RuneRender.glyph(buffer.getBuffer(RenderType.eyes(RuneRender.GLYPHS)), poseStack.last(), wall.getId() * 3 + side, .9f, color, FULL);
            poseStack.translate(0, .95f, -.03f);
            RuneRender.glyph(buffer.getBuffer(RenderType.eyes(RuneRender.GLYPHS)), poseStack.last(), wall.getId() * 3 + side + 7, .5f, color, FULL);
            poseStack.popPose();
        }
        poseStack.popPose();
        super.render(wall, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(RuneBulwark entity) {
        return RuneRender.GLYPHS;
    }
}
