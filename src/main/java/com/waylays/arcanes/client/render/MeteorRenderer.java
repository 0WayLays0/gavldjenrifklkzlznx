package com.waylays.arcanes.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.waylays.arcanes.entity.ritual.Meteor;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/** The Guided Meteor: a tumbling lump of molten and black rock in a fiery glow, a long trail of fire behind; a glowing crater once landed. */
public class MeteorRenderer extends EntityRenderer<Meteor> {
    private static final BlockState[] ROCK = {Blocks.MAGMA_BLOCK.defaultBlockState(), Blocks.BLACKSTONE.defaultBlockState(),
            Blocks.OBSIDIAN.defaultBlockState(), Blocks.BASALT.defaultBlockState()};
    private static final int FIRE = 0xFF7A2E;

    public MeteorRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(Meteor meteor, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        float age = meteor.tickCount + partialTicks;
        float s = meteor.getSize();
        if (age < Meteor.FALL) {
            Vec3 at = Meteor.FROM.scale(1 - meteor.fallen(partialTicks));
            VertexConsumer stroke = buffer.getBuffer(RenderType.eyes(GlyphRender.STROKE));
            Vec3 back = Meteor.FROM.normalize();
            GlyphRender.beam(stroke, poseStack.last(), at, at.add(back.scale(14)), s * 1.4f, RuneRender.additive(FIRE, .6f));
            GlyphRender.beam(stroke, poseStack.last(), at, at.add(back.scale(8)), s * .7f, RuneRender.additive(0xFFE0A0, .8f));
            poseStack.pushPose();
            poseStack.translate(at.x, at.y, at.z);
            poseStack.pushPose();
            poseStack.mulPose(Axis.YP.rotationDegrees(age * 9));
            poseStack.mulPose(Axis.XP.rotationDegrees(age * 6));
            for (int i = 0; i < 9; i++) {
                float x = (RuneRender.hash(i * 3 + 1) - .5f) * s * .6f;
                float y = (RuneRender.hash(i * 3 + 2) - .5f) * s * .6f - s * .3f;
                float z = (RuneRender.hash(i * 3 + 3) - .5f) * s * .6f;
                float k = s * (.45f + .25f * RuneRender.hash(i * 7));
                RuneRender.block(ROCK[i % ROCK.length], poseStack, buffer, GlyphRender.FULL, x, y, z, k, k, k, 0, 0);
            }
            poseStack.popPose();
            poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
            GlyphRender.halo(buffer, poseStack.last(), GlyphRender.GLOW, s * 1.6f, RuneRender.additive(FIRE, .9f));
            GlyphRender.halo(buffer, poseStack.last(), GlyphRender.GLOW, s * .9f, RuneRender.additive(0xFFF0C0, .8f));
            poseStack.popPose();
        } else {
            // The crater glows and cools
            float cool = 1 - Mth.clamp((age - Meteor.FALL) / 80f, 0, 1);
            poseStack.pushPose();
            poseStack.translate(0, .05, 0);
            poseStack.mulPose(Axis.YP.rotationDegrees(age));
            RenderHelper.groundQuad(buffer.getBuffer(RenderType.eyes(RuneRender.RING_GLOW)), poseStack.last(), Meteor.RADIUS * (1.2f - .2f * cool),
                    RuneRender.additive(FIRE, cool * .9f), GlyphRender.FULL);
            RenderHelper.groundQuad(buffer.getBuffer(RenderType.eyes(GlyphRender.GLOW)), poseStack.last(), Meteor.RADIUS * .6f,
                    RuneRender.additive(0xFFB040, cool), GlyphRender.FULL);
            poseStack.popPose();
        }
        super.render(meteor, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    @Override
    public boolean shouldRender(Meteor meteor, Frustum frustum, double x, double y, double z) {
        return true;
    }

    @Override
    public ResourceLocation getTextureLocation(Meteor meteor) {
        return GlyphRender.GLOW;
    }
}
