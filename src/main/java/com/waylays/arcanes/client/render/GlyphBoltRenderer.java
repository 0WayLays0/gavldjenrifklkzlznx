package com.waylays.arcanes.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.waylays.arcanes.entity.glyph.GlyphBolt;
import com.waylays.arcanes.spells.glyph.GlyphElement;
import com.waylays.arcanes.spells.glyph.GlyphStrokes;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * A bolt of written element: a blazing orb trailing a comet's tail of its colour, its glyph turning over it, facing
 * whoever looks. The stone bolt is a tumbling boulder, its glyph glowing on it.
 */
public class GlyphBoltRenderer extends EntityRenderer<GlyphBolt> {
    private static final BlockState ROCK = Blocks.COBBLESTONE.defaultBlockState();
    private static final BlockState ROCK_CORE = Blocks.TUFF.defaultBlockState();

    public GlyphBoltRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(GlyphBolt bolt, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        GlyphElement element = bolt.getElement();
        float s = bolt.getSize();
        float age = bolt.tickCount + partialTicks;
        float h = bolt.getBbHeight() * .5f;

        poseStack.pushPose();
        poseStack.translate(0, h, 0);
        if (element == GlyphElement.STONE) {
            // A boulder, tumbling as it flies
            poseStack.pushPose();
            poseStack.mulPose(Axis.XP.rotationDegrees(age * 23));
            poseStack.mulPose(Axis.ZP.rotationDegrees(age * 17));
            RuneRender.block(ROCK, poseStack, buffer, packedLight, 0, -.25f * s, 0, .5f * s, .5f * s, .5f * s, 0, 0);
            RuneRender.block(ROCK_CORE, poseStack, buffer, packedLight, .08f * s, -.18f * s, .05f * s, .42f * s, .36f * s, .44f * s, 30, 20);
            poseStack.popPose();
        }
        // The comet's tail, back along its flight
        Vec3 motion = bolt.getDeltaMovement();
        if (motion.lengthSqr() > 1.0E-4) {
            Vec3 tail = motion.normalize().scale(-(element == GlyphElement.FROST ? 1.6 : 1.1) * s);
            GlyphRender.beam(buffer.getBuffer(RenderType.eyes(GlyphRender.STROKE)), poseStack.last(), Vec3.ZERO, tail, .5f * s,
                    RuneRender.additive(element.color, .7f));
            GlyphRender.beam(buffer.getBuffer(RenderType.eyes(GlyphRender.STROKE)), poseStack.last(), Vec3.ZERO, tail.scale(.6), .16f * s,
                    RuneRender.additive(element.core, .9f));
        }
        poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
        PoseStack.Pose pose = poseStack.last();
        float orb = element == GlyphElement.STONE ? .45f : .38f;
        GlyphRender.halo(buffer, pose, GlyphRender.GLOW, orb * 1.8f * s, RuneRender.additive(element.color, .8f));
        GlyphRender.halo(buffer, pose, GlyphRender.GLOW, orb * .7f * s, RuneRender.additive(element.core, 1));
        poseStack.mulPose(Axis.ZP.rotationDegrees(age * 14));
        GlyphRender.strokes(buffer, poseStack.last(), GlyphStrokes.of(element), .42f * s, 1, element.color, element.core, .9f, 1.1f);
        poseStack.popPose();
        super.render(bolt, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(GlyphBolt entity) {
        return GlyphRender.STROKE;
    }
}
