package com.waylays.arcanes.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.waylays.arcanes.entity.rune.Runestone;
import com.waylays.arcanes.spells.rune.RuneFx;
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
 * Standing stones carved with runes, built from deepslate: the smooth Mending Runestone, its sea-green runes pulsing
 * with every heal and a crown of glyphs turning over it, and the rough menhirs of the Menhir Surge, their runes
 * flaring as they burst out.
 */
public class RunestoneRenderer extends EntityRenderer<Runestone> {
    private static final int FULL = LightTexture.FULL_BRIGHT;
    private static final BlockState POLISHED = Blocks.POLISHED_DEEPSLATE.defaultBlockState();
    private static final BlockState TILES = Blocks.DEEPSLATE_TILES.defaultBlockState();
    private static final BlockState CHISELED = Blocks.CHISELED_DEEPSLATE.defaultBlockState();
    private static final BlockState COBBLED = Blocks.COBBLED_DEEPSLATE.defaultBlockState();
    private static final BlockState ROUGH = Blocks.DEEPSLATE.defaultBlockState();

    public RunestoneRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(Runestone stone, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        float risen = stone.risen(partialTicks);
        if (risen <= .001f) {
            return;
        }
        boolean mending = stone.getKind() == Runestone.Kind.MENDING;
        float s = stone.getScale();
        float height = (mending ? 2.4f : 2.1f) * s;
        int seed = stone.getId();
        float sinking = stone.sinking(partialTicks);
        // Shudders as it crumbles back into the ground
        float shake = sinking > 0 ? .04f * Mth.sin((stone.tickCount + partialTicks) * 3.1f) : 0;

        poseStack.pushPose();
        poseStack.translate(shake, -(1 - risen) * height, shake * .6f);
        poseStack.mulPose(Axis.YP.rotationDegrees(-stone.getYRot()));
        int glyphColor;
        float glow;
        if (mending) {
            RuneRender.block(POLISHED, poseStack, buffer, packedLight, 0, 0, 0, 1.0f * s, .9f * s, .78f * s, 0, 0);
            RuneRender.block(TILES, poseStack, buffer, packedLight, 0, .9f * s, 0, .86f * s, .9f * s, .68f * s, 0, 0);
            RuneRender.block(CHISELED, poseStack, buffer, packedLight, 0, 1.8f * s, 0, .7f * s, .6f * s, .56f * s, 1.5f, -2);
            float flash = Math.max(0, 1 - stone.sincePulse(partialTicks) / 14f);
            glow = (.5f + .5f * flash) * Mth.clamp(1 - sinking / 6, 0, 1);
            glyphColor = RuneFx.MEND;
            faceGlyphs(poseStack, buffer, seed, s, .78f * s, .68f * s, glyphColor, glow);
        } else {
            float lean = (RuneRender.hash(seed) - .5f) * 12;
            float lean2 = (RuneRender.hash(seed * 7) - .5f) * 12;
            RuneRender.block(COBBLED, poseStack, buffer, packedLight, 0, 0, 0, .95f * s, 1.0f * s, .85f * s, lean * .3f, lean2 * .3f);
            RuneRender.block(ROUGH, poseStack, buffer, packedLight, .04f * s, .95f * s, 0, .74f * s, .85f * s, .66f * s, lean * .6f, lean2 * .6f);
            RuneRender.block(COBBLED, poseStack, buffer, packedLight, .02f * s, 1.75f * s, .04f * s, .5f * s, .42f * s, .48f * s, lean, lean2);
            float since = (int) stone.level().getGameTime() + partialTicks - stone.getStart();
            glow = (.35f + .65f * Math.max(0, 1 - since / 16f)) * Mth.clamp(1 - sinking / 6, 0, 1);
            glyphColor = RuneFx.GLOW;
            faceGlyphs(poseStack, buffer, seed, s, .85f * s, .66f * s, glyphColor, glow);
        }
        poseStack.popPose();

        if (mending) {
            mendingCrown(stone, partialTicks, poseStack, buffer, height, risen);
        }
        super.render(stone, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    /** Glowing runes carved down the front and back of the stone, and one on each side. */
    private static void faceGlyphs(PoseStack poseStack, MultiBufferSource buffer, int seed, float s, float depth, float width, int color, float glow) {
        int c = RuneRender.additive(color, glow);
        for (int side = 0; side < 4; side++) {
            poseStack.pushPose();
            poseStack.mulPose(Axis.YP.rotationDegrees(side * 90));
            float face = (side % 2 == 0 ? depth : width) * .5f + .012f;
            int count = side % 2 == 0 ? 3 : 1;
            for (int k = 0; k < count; k++) {
                poseStack.pushPose();
                poseStack.translate(0, (count == 1 ? 1.05f : .42f + k * .62f) * s, face);
                float size = (count == 1 ? .42f : .5f - k * .07f) * s;
                RuneRender.glyph(buffer.getBuffer(RenderType.eyes(RuneRender.GLYPHS)), poseStack.last(), seed * 3 + side * 5 + k, size, c, FULL);
                poseStack.popPose();
            }
            poseStack.popPose();
        }
    }

    /** The healing stone's crown of glyphs, its glowing foot and the ring that spreads with every pulse. */
    private void mendingCrown(Runestone stone, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, float height, float risen) {
        float age = stone.tickCount + partialTicks;
        float fade = Mth.clamp(1 - stone.sinking(partialTicks) / 6, 0, 1) * risen;
        for (int i = 0; i < 3; i++) {
            float a = age * .06f + i * Mth.TWO_PI / 3;
            poseStack.pushPose();
            poseStack.translate(Mth.cos(a) * .75f, height + .35f + .1f * Mth.sin(age * .13f + i), Mth.sin(a) * .75f);
            poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
            RuneRender.glyph(buffer.getBuffer(RenderType.eyes(RuneRender.GLYPHS)), poseStack.last(), stone.getId() + 11 + i, .32f,
                    RuneRender.additive(RuneFx.MEND, .9f * fade), FULL);
            RenderHelper.billboard(buffer.getBuffer(RenderType.eyes(RuneRender.GLOW)), poseStack.last(), .3f, RuneRender.additive(RuneFx.MEND, .5f * fade), FULL);
            poseStack.popPose();
        }
        poseStack.pushPose();
        poseStack.translate(0, .03, 0);
        poseStack.mulPose(Axis.YP.rotationDegrees(age * .8f));
        RenderHelper.groundQuad(buffer.getBuffer(RenderType.eyes(RuneRender.RING_GLOW)), poseStack.last(), 1.5f, RuneRender.additive(RuneFx.MEND, .55f * fade), FULL);
        float since = stone.sincePulse(partialTicks);
        if (since >= 0 && since < 16) {
            float t = since / 16;
            float r = Runestone.MENDING_RADIUS * (1 - (1 - t) * (1 - t));
            poseStack.translate(0, .01, 0);
            RenderHelper.groundQuad(buffer.getBuffer(RenderType.eyes(RuneRender.PULSE)), poseStack.last(), r, RuneRender.additive(RuneFx.MEND, (1 - t) * .9f), FULL);
        }
        poseStack.popPose();
    }

    @Override
    public boolean shouldRender(Runestone entity, Frustum frustum, double x, double y, double z) {
        return true;
    }

    @Override
    public ResourceLocation getTextureLocation(Runestone entity) {
        return RuneRender.GLYPHS;
    }
}
