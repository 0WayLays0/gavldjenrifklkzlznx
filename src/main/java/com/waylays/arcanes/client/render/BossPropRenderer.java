package com.waylays.arcanes.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.waylays.arcanes.ArcanesOublies;
import com.waylays.arcanes.entity.boss.BossProp;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Boss Magic scenery: a meteor (a burning mass of magma and blackstone in a halo of fire), a falling star, the giant
 * blade raised then brought down, a pillar of light.
 */
public class BossPropRenderer extends EntityRenderer<BossProp> {
    static final ResourceLocation BLADE = ArcanesOublies.id("textures/entity/boss/judgment_blade.png");
    static final ResourceLocation BEAM = ArcanesOublies.id("textures/entity/boss/beam.png");
    private static final int LIGHT = LightTexture.FULL_BRIGHT;
    /** The blade's swing down takes this many ticks, ending as its warning runs out. */
    private static final float SWING_TICKS = 6;

    public BossPropRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(BossProp prop, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        float age = prop.age(partialTicks);
        float since = prop.sinceArrival(partialTicks);
        if (age < 0 || since > prop.lingerTicks()) {
            return;
        }
        switch (prop.getKind()) {
            case METEOR -> meteor(prop, partialTicks, age, poseStack, buffer);
            case STAR -> star(prop, partialTicks, age, poseStack, buffer);
            case BLADE -> blade(prop, partialTicks, age, since, poseStack, buffer);
            case PILLAR -> pillar(prop, age, poseStack, buffer);
        }
        super.render(prop, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    /** The renderer's origin is the entity's drawn position; a falling prop is drawn where its schedule puts it. */
    private void toFall(BossProp prop, float partialTicks, PoseStack poseStack) {
        Vec3 at = prop.fallPosition(partialTicks);
        double x = Mth.lerp(partialTicks, prop.xOld, prop.getX());
        double y = Mth.lerp(partialTicks, prop.yOld, prop.getY());
        double z = Mth.lerp(partialTicks, prop.zOld, prop.getZ());
        poseStack.translate(at.x - x, at.y - y, at.z - z);
    }

    private void meteor(BossProp prop, float partialTicks, float age, PoseStack poseStack, MultiBufferSource buffer) {
        float size = prop.getSize();
        poseStack.pushPose();
        toFall(prop, partialTicks, poseStack);
        // Halo of fire, trailing up behind the fall
        poseStack.pushPose();
        poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
        BlastEffectRenderer.layer(buffer, BlastEffectRenderer.FIRE, poseStack, size * 2.6f, age * 9, BlastEffectRenderer.rgb(1f, .55f, .2f));
        BlastEffectRenderer.layer(buffer, BlastEffectRenderer.FIRE, poseStack, size * 2f, -age * 13, BlastEffectRenderer.rgb(1f, .75f, .35f));
        BlastEffectRenderer.layer(buffer, BlastEffectRenderer.CORE, poseStack, size * 1.4f, 0, BlastEffectRenderer.rgb(1f, .85f, .6f));
        poseStack.popPose();
        // The rock: a big block of magma with blackstone chunks stuck to it, tumbling
        poseStack.mulPose(Axis.YP.rotationDegrees(age * 7));
        poseStack.mulPose(Axis.XP.rotationDegrees(age * 11));
        block(Blocks.MAGMA_BLOCK.defaultBlockState(), size, 0, 0, 0, poseStack, buffer);
        float s = size * .55f;
        block(Blocks.BLACKSTONE.defaultBlockState(), s, size * .45f, size * .1f, 0, poseStack, buffer);
        block(Blocks.BLACKSTONE.defaultBlockState(), s, -size * .4f, -size * .2f, size * .15f, poseStack, buffer);
        block(Blocks.BLACKSTONE.defaultBlockState(), s * .8f, 0, size * .4f, -size * .35f, poseStack, buffer);
        poseStack.popPose();
    }

    private static void block(BlockState state, float size, float x, float y, float z, PoseStack poseStack, MultiBufferSource buffer) {
        poseStack.pushPose();
        poseStack.translate(x, y, z);
        poseStack.scale(size, size, size);
        poseStack.translate(-.5, -.5, -.5);
        Minecraft.getInstance().getBlockRenderer().renderSingleBlock(state, poseStack, buffer, LIGHT, OverlayTexture.NO_OVERLAY);
        poseStack.popPose();
    }

    private void star(BossProp prop, float partialTicks, float age, PoseStack poseStack, MultiBufferSource buffer) {
        float size = prop.getSize();
        int color = prop.getStyle().rune;
        poseStack.pushPose();
        toFall(prop, partialTicks, poseStack);
        // Streak along the fall
        Vec3 d = prop.getTo().subtract(prop.getFrom()).normalize();
        Vec3 tail = d.scale(-4 * size);
        VertexConsumer c = buffer.getBuffer(RenderType.eyes(BEAM));
        RenderHelper.strand(c, poseStack.last(), Vec3.ZERO, tail, size * .7f, 4 * size, BossTelegraphRenderer.additive(color, .8f), LIGHT);
        poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
        BlastEffectRenderer.layer(buffer, BlastEffectRenderer.CORE, poseStack, size * 2.2f, age * 20, BossTelegraphRenderer.additive(color, 1));
        BlastEffectRenderer.layer(buffer, BlastEffectRenderer.RING, poseStack, size * 1.4f, 0, BossTelegraphRenderer.additive(0xFFFFFF, .9f));
        poseStack.popPose();
    }

    /**
     * The blade: fades in standing on its hilt at the start of the strip, point to the sky, then swings down along the
     * strip in the last ticks before its warning runs out, and fades out lying in it.
     */
    private void blade(BossProp prop, float partialTicks, float age, float since, PoseStack poseStack, MultiBufferSource buffer) {
        float length = prop.getSize();
        float width = Math.max(1.5f, length * .14f);
        float appear = Mth.clamp(age / 15f, 0, 1);
        float fade = since > 0 ? 1 - since / prop.lingerTicks() : appear;
        // 0 standing, 90 lying along the strip
        float swing = Mth.clamp((since + SWING_TICKS) / SWING_TICKS, 0, 1);
        float tilt = 90 * swing * swing;
        // Hovering and swaying a little while it waits
        float hover = (1 - swing) * (1.5f + Mth.sin(age * .15f) * .25f);
        poseStack.pushPose();
        poseStack.translate(0, hover, 0);
        poseStack.mulPose(Axis.YP.rotationDegrees(-prop.getYRot()));
        poseStack.mulPose(Axis.XP.rotationDegrees(tilt));
        int color = BossTelegraphRenderer.additive(0xFFF0C0, Mth.clamp(fade, 0, 1));
        VertexConsumer c = buffer.getBuffer(RenderType.eyes(BLADE));
        // Two crossed planes along the blade (+Y, point up)
        for (int k = 0; k < 2; k++) {
            poseStack.pushPose();
            poseStack.mulPose(Axis.YP.rotationDegrees(k * 90));
            PoseStack.Pose pose = poseStack.last();
            float h = width * .5f;
            RenderHelper.vertex(c, pose, -h, 0, 0, 0, 1, color, LIGHT, 0, 0, 1);
            RenderHelper.vertex(c, pose, h, 0, 0, 1, 1, color, LIGHT, 0, 0, 1);
            RenderHelper.vertex(c, pose, h, length, 0, 1, 0, color, LIGHT, 0, 0, 1);
            RenderHelper.vertex(c, pose, -h, length, 0, 0, 0, color, LIGHT, 0, 0, 1);
            RenderHelper.vertex(c, pose, -h, length, 0, 0, 0, color, LIGHT, 0, 0, -1);
            RenderHelper.vertex(c, pose, h, length, 0, 1, 0, color, LIGHT, 0, 0, -1);
            RenderHelper.vertex(c, pose, h, 0, 0, 1, 1, color, LIGHT, 0, 0, -1);
            RenderHelper.vertex(c, pose, -h, 0, 0, 0, 1, color, LIGHT, 0, 0, -1);
            poseStack.popPose();
        }
        poseStack.popPose();
    }

    /** A pillar of light shooting up, then thinning out. */
    private void pillar(BossProp prop, float age, PoseStack poseStack, MultiBufferSource buffer) {
        float life = Mth.clamp(age / (prop.lingerTicks() + 1), 0, 1);
        float grow = Mth.clamp(age / 3f, 0, 1);
        float width = prop.getSize() * (1 - life * .7f);
        float height = 14 * grow;
        int color = BossTelegraphRenderer.additive(BossTelegraphRenderer.lighten(prop.getStyle().zone, .4f), 1 - life);
        VertexConsumer c = buffer.getBuffer(RenderType.eyes(BEAM));
        RenderHelper.strand(c, poseStack.last(), Vec3.ZERO, new Vec3(0, height, 0), width, 4, color, LIGHT);
        RenderHelper.strand(c, poseStack.last(), Vec3.ZERO, new Vec3(0, height * .8, 0), width * .45f, 4, BossTelegraphRenderer.additive(0xFFFFFF, 1 - life), LIGHT);
        poseStack.pushPose();
        poseStack.translate(0, .1, 0);
        RenderHelper.groundQuad(buffer.getBuffer(RenderType.eyes(BlastEffectRenderer.RING)), poseStack.last(), width * (1.2f + life * 2), color, LIGHT);
        poseStack.popPose();
    }

    @Override
    public boolean shouldRender(BossProp entity, Frustum frustum, double x, double y, double z) {
        return true;
    }

    @Override
    public ResourceLocation getTextureLocation(BossProp entity) {
        return BLADE;
    }
}
