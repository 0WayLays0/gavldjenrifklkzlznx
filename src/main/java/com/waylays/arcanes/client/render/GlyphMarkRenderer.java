package com.waylays.arcanes.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.waylays.arcanes.entity.glyph.GlyphMarkVisual;
import com.waylays.arcanes.spells.glyph.GlyphElement;
import com.waylays.arcanes.spells.glyph.GlyphReaction;
import com.waylays.arcanes.spells.glyph.GlyphReactions;
import com.waylays.arcanes.spells.glyph.GlyphStrokes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * The element glyph glowing over an inscribed creature's head. When a reaction goes off, the striking element's glyph
 * swoops down onto it, both blaze up and burst in a ring of their mixed colours; a creature frozen by the Freeze
 * reaction stands in a block of ice.
 */
public class GlyphMarkRenderer extends EntityRenderer<GlyphMarkVisual> {
    private static final BlockState ICE = Blocks.ICE.defaultBlockState();
    private static final float SIZE = .3f;

    public GlyphMarkRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(GlyphMarkVisual mark, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        Entity target = mark.getTarget();
        if (target == null) {
            return;
        }
        // Drawn over the creature where it is this frame, not where the mark was last moved
        Vec3 feet = target.getPosition(partialTicks).subtract(mark.getPosition(partialTicks));
        float age = mark.tickCount + partialTicks;
        GlyphElement element = mark.getElement();
        GlyphReaction reaction = mark.getReaction();
        float since = mark.sinceReaction(partialTicks);

        if (reaction == GlyphReaction.FREEZE && since >= 0 && since < GlyphReactions.FREEZE_TICKS) {
            ice(poseStack, buffer, target, feet, since, packedLight);
        }

        poseStack.pushPose();
        poseStack.translate(feet.x, feet.y + target.getBbHeight() + .55 + .06 * Mth.sin(age * .15f), feet.z);
        poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
        if (reaction == null) {
            float pop = Mth.clamp(mark.sinceMarked(partialTicks) / 5, 0, 1);
            float size = SIZE * (1.6f - .6f * pop);
            float pulse = .75f + .2f * Mth.sin(age * .2f);
            GlyphRender.halo(buffer, poseStack.last(), GlyphRender.GLOW, size * 1.6f, RuneRender.additive(element.color, .35f * pulse));
            poseStack.mulPose(Axis.ZP.rotationDegrees(Mth.sin(age * .05f) * 12));
            GlyphRender.strokes(buffer, poseStack.last(), GlyphStrokes.of(element), size, 1, element.color, element.core, pulse, 1.2f);
        } else if (since < GlyphMarkVisual.REACT_TICKS) {
            GlyphElement second = mark.getSecond();
            float t = since / GlyphMarkVisual.REACT_TICKS;
            float meet = Mth.clamp(t / .35f, 0, 1);
            float burst = Mth.clamp((t - .35f) / .65f, 0, 1);
            float fade = 1 - burst;
            float size = SIZE * (1 + 1.4f * burst);
            GlyphRender.strokes(buffer, poseStack.last(), GlyphStrokes.of(element), size, 1, element.color,
                    GlyphRender.whiten(element.core, meet), fade, 1.2f + meet);
            if (second != null) {
                poseStack.pushPose();
                poseStack.translate(0, 1.2f * (1 - meet * meet), 0);
                poseStack.mulPose(Axis.ZP.rotationDegrees((1 - meet) * 180));
                GlyphRender.strokes(buffer, poseStack.last(), GlyphStrokes.of(second), size, 1, second.color,
                        GlyphRender.whiten(second.core, meet), fade, 1.2f + meet);
                poseStack.popPose();
            }
            if (burst > 0) {
                int color = reaction.color();
                GlyphRender.halo(buffer, poseStack.last(), GlyphRender.GLOW, .5f + 1.2f * burst, RuneRender.additive(GlyphRender.whiten(color, .4f), fade));
                GlyphRender.halo(buffer, poseStack.last(), GlyphRender.RIPPLE, .4f + 2.4f * burst, RuneRender.additive(color, fade));
            }
        }
        poseStack.popPose();
        super.render(mark, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    /** The block of ice the frozen creature stands in: it sets in a few ticks and shrinks away at the end. */
    private static void ice(PoseStack poseStack, MultiBufferSource buffer, Entity target, Vec3 feet, float since, int light) {
        float grow = Mth.clamp(since / 4, 0, 1);
        float melt = Mth.clamp((GlyphReactions.FREEZE_TICKS - since) / 6, 0, 1);
        float k = grow * melt;
        if (k <= 0) {
            return;
        }
        float w = (target.getBbWidth() + .35f) * k;
        float h = (target.getBbHeight() + .25f) * k;
        poseStack.pushPose();
        poseStack.translate(feet.x, feet.y, feet.z);
        poseStack.scale(w, h, w);
        poseStack.translate(-.5, 0, -.5);
        BlockRenderDispatcher blocks = Minecraft.getInstance().getBlockRenderer();
        blocks.getModelRenderer().renderModel(poseStack.last(), buffer.getBuffer(RenderType.entityTranslucent(TextureAtlas.LOCATION_BLOCKS)),
                ICE, blocks.getBlockModel(ICE), 1, 1, 1, light, OverlayTexture.NO_OVERLAY);
        poseStack.popPose();
    }

    @Override
    public boolean shouldRender(GlyphMarkVisual entity, Frustum frustum, double x, double y, double z) {
        return true;
    }

    @Override
    public ResourceLocation getTextureLocation(GlyphMarkVisual entity) {
        return GlyphRender.STROKE;
    }
}
