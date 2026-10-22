package com.waylays.arcanes.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.waylays.arcanes.entity.wall.BrickStorm;
import com.waylays.arcanes.spells.wall.WallFx;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * The Brick Rain: the sigil burning on the ground where it will fall, and over it a vortex of bricks turning in three
 * rings round a glowing heart, the beams of embers that bind them to it, breaking up at the end.
 */
public class BrickStormRenderer extends EntityRenderer<BrickStorm> {
    private static final int BRICKS = 26;

    public BrickStormRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(BrickStorm storm, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        float age = storm.tickCount + partialTicks;
        float r = storm.getRadius();
        float tele = Mth.clamp(age / BrickStorm.TELEGRAPH, 0, 1);
        float vortex = storm.vortex(partialTicks);
        float end = BrickStorm.RAIN_START + storm.getDuration();
        float ground = tele * Mth.clamp(1 - (age - end) / BrickStorm.FADE, 0, 1) * (.55f + .25f * Mth.sin(age * .3f));
        WallRender.sigil(poseStack, buffer, r + .6f, ground, age * 2);
        if (vortex <= .01f) {
            super.render(storm, entityYaw, partialTicks, poseStack, buffer, packedLight);
            return;
        }
        Vec3 heart = new Vec3(0, BrickStorm.HEIGHT + 1, 0);
        int seed = storm.getId() * 61;
        Vec3[] at = new Vec3[BRICKS];
        for (int i = 0; i < BRICKS; i++) {
            int ring = i % 3;
            float ringR = r * (.35f + .35f * ring) * (.6f + .4f * vortex) + (1 - vortex) * 2;
            float speed = (3.2f - ring) * 2.2f;
            float a = (i * Mth.TWO_PI / BRICKS * 3 + age * speed * Mth.DEG_TO_RAD * 3) * (ring == 1 ? -1 : 1);
            float y = BrickStorm.HEIGHT + (ring - 1) * .8f + .3f * Mth.sin(age * .2f + i);
            at[i] = new Vec3(Mth.cos(a) * ringR, y, Mth.sin(a) * ringR);
            float s = (.5f + .2f * RuneRender.hash(seed + i)) * vortex;
            poseStack.pushPose();
            poseStack.translate(at[i].x, at[i].y, at[i].z);
            poseStack.mulPose(Axis.YP.rotationDegrees(age * 9 + i * 40));
            poseStack.mulPose(Axis.XP.rotationDegrees(age * 7 + i * 25));
            RuneRender.block(WallFx.brick(seed + i), poseStack, buffer, packedLight, 0, -s * .5f, 0, s, s, s, 0, 0);
            poseStack.popPose();
        }
        // Threads of embers from the heart of the vortex to its bricks (after the bricks: one buffer at a time)
        VertexConsumer stroke = buffer.getBuffer(RenderType.eyes(GlyphRender.STROKE));
        for (int i = 0; i < BRICKS; i += 3) {
            GlyphRender.beam(stroke, poseStack.last(), heart, at[i], .06f, WallRender.ember(.5f, .35f * vortex));
        }
        poseStack.pushPose();
        poseStack.translate(heart.x, heart.y, heart.z);
        poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
        GlyphRender.halo(buffer, poseStack.last(), GlyphRender.GLOW, 2.2f * vortex, WallRender.ember(.8f, .8f * vortex));
        poseStack.popPose();
        super.render(storm, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    @Override
    public boolean shouldRender(BrickStorm storm, Frustum frustum, double x, double y, double z) {
        return true;
    }

    @Override
    public ResourceLocation getTextureLocation(BrickStorm storm) {
        return WallRender.SIGIL;
    }
}
