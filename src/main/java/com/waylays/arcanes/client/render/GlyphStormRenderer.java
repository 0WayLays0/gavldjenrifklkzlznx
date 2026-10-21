package com.waylays.arcanes.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.waylays.arcanes.entity.glyph.GlyphStorm;
import com.waylays.arcanes.registry.ModSchools;
import com.waylays.arcanes.spells.glyph.GlyphElement;
import com.waylays.arcanes.spells.glyph.GlyphStrokes;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * The Primordial Glyph across the sky: a great gold ring traced first, the six element glyphs written on it one after
 * another, then the star joining them. A glyph flares as it hurls its bolt; the whole glyph breathes with light.
 */
public class GlyphStormRenderer extends EntityRenderer<GlyphStorm> {
    private static final int GOLD_CORE = 0xFFF4DC;

    public GlyphStormRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(GlyphStorm storm, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        float strength = storm.strength(partialTicks);
        if (strength <= 0) {
            return;
        }
        float age = storm.tickCount + partialTicks;
        float breathe = .8f + .2f * Mth.sin(age * .12f);
        int struck = storm.getStruck();
        float sinceStrike = storm.sinceStrike(partialTicks);
        float fade = strength;

        poseStack.pushPose();
        poseStack.translate(0, GlyphStorm.HEIGHT, 0);
        // Laid flat in the sky: glyph space (x, y) becomes the ground plane (x, z)
        poseStack.mulPose(Axis.XP.rotationDegrees(90));
        PoseStack.Pose pose = poseStack.last();
        GlyphRender.halo(buffer, pose, GlyphRender.GLOW, GlyphStorm.RING * 2.4f, RuneRender.additive(ModSchools.GLYPH_COLOR, .22f * fade * storm.ringWritten(partialTicks)));
        GlyphRender.strokes(buffer, pose, GlyphStrokes.RING, GlyphStorm.RING + 1.4f, storm.ringWritten(partialTicks), ModSchools.GLYPH_COLOR, GOLD_CORE,
                breathe * fade, 1.6f);
        GlyphRender.strokes(buffer, pose, GlyphStrokes.RING, GlyphStorm.RING - 1.4f, storm.ringWritten(partialTicks), ModSchools.GLYPH_COLOR, GOLD_CORE,
                .7f * breathe * fade, 1.2f);
        // The star turns with the ring so its points meet the glyphs
        poseStack.pushPose();
        poseStack.mulPose(Axis.ZP.rotationDegrees(storm.getYRot()));
        GlyphRender.strokes(buffer, poseStack.last(), GlyphStrokes.STAR, GlyphStorm.RING, storm.starWritten(partialTicks), ModSchools.GLYPH_COLOR, GOLD_CORE,
                .8f * breathe * fade, 1.4f);
        poseStack.popPose();
        for (int i = 0; i < 6; i++) {
            float written = storm.glyphWritten(i, partialTicks);
            if (written <= 0) {
                continue;
            }
            GlyphElement element = GlyphElement.byId(i);
            Vec3 at = storm.glyphOffset(i);
            float flare = i == struck && sinceStrike >= 0 && sinceStrike < 6 ? 1 - sinceStrike / 6 : 0;
            poseStack.pushPose();
            poseStack.translate(at.x, at.z, 0);
            // Each glyph upright towards the outside of the ring
            poseStack.mulPose(Axis.ZP.rotationDegrees((float) Math.toDegrees(Math.atan2(at.z, at.x)) - 90));
            float size = 1.25f * (1 + .25f * flare);
            GlyphRender.halo(buffer, poseStack.last(), GlyphRender.GLOW, size * 1.7f, RuneRender.additive(element.color, (.3f + .5f * flare) * fade));
            GlyphRender.strokes(buffer, poseStack.last(), GlyphStrokes.of(element), size, written, element.color,
                    GlyphRender.whiten(element.core, flare), fade, 1.5f + flare);
            poseStack.popPose();
        }
        poseStack.popPose();
        super.render(storm, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    @Override
    public boolean shouldRender(GlyphStorm entity, Frustum frustum, double x, double y, double z) {
        return true;
    }

    @Override
    public ResourceLocation getTextureLocation(GlyphStorm entity) {
        return GlyphRender.STROKE;
    }
}
