package com.waylays.arcanes.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.waylays.arcanes.entity.gem.RubyRay;
import com.waylays.arcanes.spells.gem.Gem;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * The Ruby Ray: a cut ruby turning before the caster's hand, a beam of red light pouring out of it (a wide glow, a
 * bright band, a white core), two threads of light winding round its first blocks, sparks of light racing along it,
 * and a blaze where it ends. Split by a prism, three thinner rays fan out of the diamond.
 */
public class RubyRayRenderer extends EntityRenderer<RubyRay> {
    private static final int[] SPLIT_TINTS = {0xFF4060, 0xFF7A3A, 0xE040A0};

    public RubyRayRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(RubyRay ray, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        float age = ray.tickCount + partialTicks;
        Vec3 base = ray.getPosition(partialTicks);
        Entity caster = ray.getCaster();
        Vec3 from = (caster != null ? RubyRay.origin(caster, partialTicks) : base).subtract(base);
        Vec3 end = ray.getEnd().subtract(base);
        Vec3 dir = end.subtract(from);
        double length = dir.length();
        float warm = Mth.clamp(age / 6, 0, 1);
        // The ruby
        poseStack.pushPose();
        poseStack.translate(from.x, from.y, from.z);
        poseStack.mulPose(Axis.YP.rotationDegrees(age * 9));
        poseStack.mulPose(Axis.XP.rotationDegrees(20 * Mth.sin(age * .2f)));
        GemRender.jewel(poseStack, buffer, GemRender.Cut.BRILLIANT, Gem.RUBY.color, .42f, .8f, age * 2, .9f, false, ray.getId());
        poseStack.popPose();
        if (length < .1) {
            return;
        }
        Vec3 unit = dir.scale(1 / length);
        // The beam
        GemRender.ray(poseStack, buffer, from, end, Gem.RUBY.color, .22f * warm, warm, age);
        // Two threads winding round its first blocks
        Vec3 side = unit.cross(new Vec3(0, 1, 0));
        side = side.lengthSqr() < 1.0E-4 ? new Vec3(1, 0, 0) : side.normalize();
        Vec3 up = side.cross(unit).normalize();
        double coil = Math.min(length, 6);
        var stroke = buffer.getBuffer(net.minecraft.client.renderer.RenderType.eyes(GlyphRender.STROKE));
        for (int strand = 0; strand < 2; strand++) {
            Vec3 prev = null;
            for (int k = 0; k <= 24; k++) {
                double s = coil * k / 24;
                float a = (float) (s * 2.6 - age * .5f + strand * Math.PI);
                float r = .3f * (1 - (float) (s / coil) * .6f);
                Vec3 p = from.add(unit.scale(s)).add(side.scale(Mth.cos(a) * r)).add(up.scale(Mth.sin(a) * r));
                if (prev != null) {
                    GlyphRender.beam(stroke, poseStack.last(), prev, p, .05f, RuneRender.additive(Gem.RUBY.light, .7f * warm * (1 - (float) (s / coil))));
                }
                prev = p;
            }
        }
        // Sparks racing along it
        for (int k = 0; k < 7; k++) {
            double s = ((age * .9 + k * length / 7) % length);
            GemRender.glow(poseStack, buffer, from.add(unit.scale(s)), Gem.RUBY.light, .28f, .8f * warm);
        }
        // The blaze where it ends
        float pulse = 1 + .2f * Mth.sin(age * 1.3f);
        GemRender.glow(poseStack, buffer, end, Gem.RUBY.color, 1.2f * pulse, .85f * warm);
        GemRender.glow(poseStack, buffer, end, 0xFFE0C0, .45f * pulse, warm);
        // Split by a prism
        List<Vec3> splits = ray.getSplits();
        for (int i = 0; i < splits.size(); i++) {
            Vec3 to = splits.get(i).subtract(base);
            GemRender.ray(poseStack, buffer, end, to, SPLIT_TINTS[i % SPLIT_TINTS.length], .12f, .9f, age + i * 3);
            GemRender.glow(poseStack, buffer, to, SPLIT_TINTS[i % SPLIT_TINTS.length], .8f * pulse, .8f);
        }
        super.render(ray, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    @Override
    public boolean shouldRender(RubyRay ray, Frustum frustum, double x, double y, double z) {
        return true;
    }

    @Override
    public ResourceLocation getTextureLocation(RubyRay ray) {
        return GemRender.FACET;
    }
}
