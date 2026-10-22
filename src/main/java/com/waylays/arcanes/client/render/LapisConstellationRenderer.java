package com.waylays.arcanes.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.waylays.arcanes.entity.gem.LapisConstellation;
import com.waylays.arcanes.spells.gem.Gem;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/**
 * The Lapis Constellation: the stars still on the ring, each a little block of lapis turning, a gold glint on it and a
 * glow round it, joined one to the next by threads of golden light like a figure in the night sky.
 */
public class LapisConstellationRenderer extends EntityRenderer<LapisConstellation> {
    public LapisConstellationRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(LapisConstellation sky, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        float age = sky.tickCount + partialTicks;
        Entity caster = sky.getCaster();
        Vec3 base = sky.getPosition(partialTicks);
        Vec3 feet = caster != null ? caster.getPosition(partialTicks).subtract(base) : Vec3.ZERO;
        float appear = Mth.clamp(age / 10, 0, 1);
        float fade = Mth.clamp((sky.getLife() - age) / 15, 0, 1);
        int stars = sky.getStars();
        Vec3[] at = new Vec3[LapisConstellation.STARS];
        for (int i = 0; i < LapisConstellation.STARS; i++) {
            if ((stars & (1 << i)) == 0) {
                continue;
            }
            at[i] = feet.add(sky.star(i, age).multiply(appear, 1, appear));
            poseStack.pushPose();
            poseStack.translate(at[i].x, at[i].y, at[i].z);
            poseStack.mulPose(Axis.YP.rotationDegrees(age * 4 + i * 50));
            poseStack.mulPose(Axis.XP.rotationDegrees(age * 3 + i * 20));
            float s = .3f * appear * fade;
            RuneRender.block(Blocks.LAPIS_BLOCK.defaultBlockState(), poseStack, buffer, GlyphRender.FULL, 0, -s * .5f, 0, s, s, s, 0, 0);
            poseStack.popPose();
        }
        // The figure
        VertexConsumer stroke = buffer.getBuffer(RenderType.eyes(GlyphRender.STROKE));
        Vec3 first = null, prev = null;
        for (Vec3 p : at) {
            if (p == null) {
                continue;
            }
            if (prev != null) {
                GlyphRender.beam(stroke, poseStack.last(), prev, p, .035f, RuneRender.additive(Gem.LAPIS.light, .55f * fade));
            } else {
                first = p;
            }
            prev = p;
        }
        if (first != null && prev != first && Integer.bitCount(stars) > 2) {
            GlyphRender.beam(stroke, poseStack.last(), prev, first, .035f, RuneRender.additive(Gem.LAPIS.light, .55f * fade));
        }
        for (int i = 0; i < at.length; i++) {
            if (at[i] != null) {
                float tw = .6f + .4f * Mth.sin(age * .25f + i * 2.1f);
                GemRender.glow(poseStack, buffer, at[i], Gem.LAPIS.color, .55f, .55f * fade);
                GemRender.glow(poseStack, buffer, at[i], Gem.LAPIS.light, .22f * tw, fade);
            }
        }
        super.render(sky, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(LapisConstellation sky) {
        return GemRender.FACET;
    }
}
