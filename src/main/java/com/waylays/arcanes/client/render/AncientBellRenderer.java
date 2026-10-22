package com.waylays.arcanes.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.waylays.arcanes.entity.verdigris.AncientBell;
import com.waylays.arcanes.spells.verdigris.VerdigrisFx;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The Ancient Bell: a great bronze bell gone green, built up in rings (each laid twice, turned an eighth, so it reads
 * round), moss on its shoulder and vines down its sides, falling on its chain. Its toll spreads over the ground in
 * green rings of old glyphs.
 */
public class AncientBellRenderer extends EntityRenderer<AncientBell> {
    private static final BlockState CHAIN = Blocks.CHAIN.defaultBlockState();
    /** The bell's rings, bottom up: block, bottom, height, width. */
    private static final BlockState[] RING_BLOCKS = {
            VerdigrisRender.OXIDIZED_CUT, VerdigrisRender.OXIDIZED, VerdigrisRender.OXIDIZED,
            VerdigrisRender.OXIDIZED_CHISELED, VerdigrisRender.OXIDIZED, VerdigrisRender.OXIDIZED_CUT, VerdigrisRender.WEATHERED_CUT};
    private static final float[][] RINGS = {
            {0, .3f, 2.3f}, {.3f, .55f, 1.9f}, {.85f, .35f, 1.55f}, {1.2f, .3f, 1.62f}, {1.5f, .3f, 1.5f}, {1.8f, .35f, 1.15f}, {2.15f, .35f, .55f}};
    private static final float TOP = 2.5f;

    public AncientBellRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(AncientBell bell, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        float drop = bell.height(partialTicks);
        float sunk = bell.sunk(partialTicks);
        float sway = bell.sway(partialTicks);
        int seed = bell.getId();
        int light = drop > 0 ? LightTexture.FULL_BRIGHT : packedLight;

        poseStack.pushPose();
        poseStack.translate(0, drop - sunk * 2.8f, 0);
        poseStack.mulPose(Axis.YP.rotationDegrees(-bell.getYRot()));
        // Swings from its crown
        poseStack.translate(0, TOP, 0);
        poseStack.mulPose(Axis.ZP.rotationDegrees(sway));
        poseStack.mulPose(Axis.XP.rotationDegrees(sway * .4f));
        poseStack.translate(0, -TOP, 0);
        for (int i = 0; i < RINGS.length; i++) {
            float[] r = RINGS[i];
            VerdigrisRender.turned(RING_BLOCKS[i], poseStack, buffer, light, 0, r[0], 0, r[2], r[1], r[2], 0);
            VerdigrisRender.turned(RING_BLOCKS[i], poseStack, buffer, light, 0, r[0] + .002f, 0, r[2] * .94f, r[1], r[2] * .94f, 45);
        }
        VerdigrisRender.turned(VerdigrisRender.MOSS_CARPET, poseStack, buffer, light, 0, 2.15f, 0, 1.2f, 1, 1.2f, 20);
        VerdigrisRender.vines(poseStack, buffer, light, 2.1f, 1.5f, 1.5f, VerdigrisRender.hanging(seed, 1.4f));
        // The chain it hung from: long while it falls, a broken stub once down
        int links = drop > .5f ? 6 : 1;
        for (int k = 0; k < links; k++) {
            VerdigrisRender.block(CHAIN, poseStack, buffer, light, 0, TOP + k, 0, 1, 1, 1);
        }
        poseStack.popPose();

        toll(bell, partialTicks, poseStack, buffer);
        super.render(bell, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    /** Green rings of glyphs racing out over the ground as the bell tolls. */
    private static void toll(AncientBell bell, float partialTicks, PoseStack poseStack, MultiBufferSource buffer) {
        float since = bell.tickCount + partialTicks - AncientBell.FALL;
        for (int k = 0; k < 2; k++) {
            float t = (since - k * 5) / 14f;
            if (t < 0 || t > 1) {
                continue;
            }
            poseStack.pushPose();
            poseStack.translate(0, .05 + k * .01, 0);
            poseStack.mulPose(Axis.YP.rotationDegrees(since * 4 + k * 30));
            float half = 1 + (AncientBell.AROUND - .5f) * (1 - (1 - t) * (1 - t));
            RenderHelper.groundQuad(buffer.getBuffer(RenderType.eyes(RuneRender.RING_GLOW)), poseStack.last(), half,
                    RuneRender.additive(VerdigrisFx.VERDIGRIS, (1 - t) * (k == 0 ? .9f : .5f)), LightTexture.FULL_BRIGHT);
            poseStack.popPose();
        }
        if (since < 0) {
            // Where it will land: a faint mark on the ground, growing as it comes
            float t = Mth.clamp(1 + since / AncientBell.FALL, 0, 1);
            poseStack.pushPose();
            poseStack.translate(0, .04, 0);
            poseStack.mulPose(Axis.YP.rotationDegrees(bell.tickCount * 6 + partialTicks * 6));
            RenderHelper.groundQuad(buffer.getBuffer(RenderType.eyes(RuneRender.RING_GLOW)), poseStack.last(), AncientBell.UNDER,
                    RuneRender.additive(VerdigrisFx.VERDIGRIS, .25f + .45f * t), LightTexture.FULL_BRIGHT);
            poseStack.popPose();
        }
    }

    @Override
    public boolean shouldRender(AncientBell bell, Frustum frustum, double x, double y, double z) {
        return frustum.isVisible(bell.getBoundingBox().inflate(AncientBell.AROUND).expandTowards(0, AncientBell.DROP + 6, 0));
    }

    @Override
    public ResourceLocation getTextureLocation(AncientBell bell) {
        return RuneRender.GLOW;
    }
}
