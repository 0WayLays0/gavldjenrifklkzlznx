package com.waylays.arcanes.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.waylays.arcanes.client.render.boss.PrisonModel;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.waylays.arcanes.ArcanesOublies;
import com.waylays.arcanes.entity.boss.RuneWall;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * The Rune Prison's wall: a ring of tall glowing runes rising out of the ground, drifting slowly round, with a bright
 * seam where it meets the ground. Seen from both sides.
 */
public class RuneWallRenderer extends EntityRenderer<RuneWall> {
    private static final ResourceLocation WALL = ArcanesOublies.id("textures/entity/boss/rune_wall.png");
    private static final int LIGHT = LightTexture.FULL_BRIGHT;
    private static final int VIOLET = 0xB45CFF;

    public RuneWallRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(RuneWall wall, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        float standing = wall.standing(partialTicks);
        if (standing <= 0) {
            return;
        }
        float age = wall.tickCount + partialTicks;
        float r = wall.getRadius();
        // Its standing stones, then the wall of runes between them
        PrisonModel.draw(poseStack, buffer, WaterViews.of(this.entityRenderDispatcher, wall, partialTicks), r, RuneWall.HEIGHT, standing, age, LIGHT);
        float h = RuneWall.HEIGHT * standing;
        int n = Mth.clamp(Mth.ceil(Mth.TWO_PI * r / 1.5f), 16, 96);
        float pulse = .75f + .25f * Mth.sin(age * .2f);
        int color = BossTelegraphRenderer.additive(VIOLET, pulse * standing);
        float drift = age * .01f;
        VertexConsumer c = buffer.getBuffer(RenderType.eyes(WALL));
        PoseStack.Pose pose = poseStack.last();
        // The texture repeats about every 2 blocks round the ring
        float uPerSegment = Mth.TWO_PI * r / n / 2f;
        for (int i = 0; i < n; i++) {
            float a0 = i * Mth.TWO_PI / n;
            float a1 = (i + 1) * Mth.TWO_PI / n;
            float x0 = Mth.cos(a0) * r;
            float z0 = Mth.sin(a0) * r;
            float x1 = Mth.cos(a1) * r;
            float z1 = Mth.sin(a1) * r;
            float u0 = i * uPerSegment + drift;
            float u1 = u0 + uPerSegment;
            float nx = -Mth.cos((a0 + a1) * .5f);
            float nz = -Mth.sin((a0 + a1) * .5f);
            // Inside face, then outside face
            RenderHelper.vertex(c, pose, x0, 0, z0, u0, 1, color, LIGHT, nx, 0, nz);
            RenderHelper.vertex(c, pose, x1, 0, z1, u1, 1, color, LIGHT, nx, 0, nz);
            RenderHelper.vertex(c, pose, x1, h, z1, u1, 0, color, LIGHT, nx, 0, nz);
            RenderHelper.vertex(c, pose, x0, h, z0, u0, 0, color, LIGHT, nx, 0, nz);
            RenderHelper.vertex(c, pose, x0, h, z0, u0, 0, color, LIGHT, -nx, 0, -nz);
            RenderHelper.vertex(c, pose, x1, h, z1, u1, 0, color, LIGHT, -nx, 0, -nz);
            RenderHelper.vertex(c, pose, x1, 0, z1, u1, 1, color, LIGHT, -nx, 0, -nz);
            RenderHelper.vertex(c, pose, x0, 0, z0, u0, 1, color, LIGHT, -nx, 0, -nz);
        }
        // A bright seam on the ground
        poseStack.pushPose();
        poseStack.translate(0, .06, 0);
        RenderHelper.groundQuad(buffer.getBuffer(RenderType.eyes(BlastEffectRenderer.RING)), poseStack.last(), r * 1.08f, color, LIGHT);
        poseStack.popPose();
        super.render(wall, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    @Override
    public boolean shouldRender(RuneWall entity, Frustum frustum, double x, double y, double z) {
        return true;
    }

    @Override
    public ResourceLocation getTextureLocation(RuneWall entity) {
        return WALL;
    }
}
