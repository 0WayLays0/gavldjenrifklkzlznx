package com.waylays.arcanes.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.waylays.arcanes.entity.glyph.GlyphArc;
import com.waylays.arcanes.spells.glyph.GlyphElement;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

/** An arc of lightning leaping from point to point, flickering (its kinks change every other tick) as it fades. */
public class GlyphArcRenderer extends EntityRenderer<GlyphArc> {
    public GlyphArcRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(GlyphArc arc, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        float strength = arc.strength(partialTicks);
        if (strength <= 0) {
            return;
        }
        GlyphElement element = arc.getElement();
        int flicker = arc.tickCount / 2;
        for (int i = 1; i < arc.getCount(); i++) {
            Vec3 a = arc.point(i - 1);
            Vec3 b = arc.point(i);
            GlyphRender.lightning(buffer, poseStack, a, b, arc.getId() * 977 + i * 131 + flicker * 17, element.color, element.core, strength, 1);
            // A thinner fork beside the main bolt
            GlyphRender.lightning(buffer, poseStack, a, b, arc.getId() * 541 + i * 37 + flicker * 29, element.color, element.core, strength * .5f, .5f);
        }
        super.render(arc, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    @Override
    public boolean shouldRender(GlyphArc entity, Frustum frustum, double x, double y, double z) {
        return true;
    }

    @Override
    public ResourceLocation getTextureLocation(GlyphArc entity) {
        return GlyphRender.STROKE;
    }
}
