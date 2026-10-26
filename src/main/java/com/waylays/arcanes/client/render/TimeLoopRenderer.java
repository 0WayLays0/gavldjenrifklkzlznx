package com.waylays.arcanes.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.waylays.arcanes.entity.cosmos.TimeLoop;
import com.waylays.arcanes.spells.cosmos.Cosmos;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * A time loop round its prisoner: a golden clock ring at its waist (sixty ticks, twelve hour marks) slowly turning, an
 * arc of orange light sweeping it once a loop, a bead of light for each loop still to come, and the prisoner itself
 * shimmering with the loop's light.
 */
public class TimeLoopRenderer extends EntityRenderer<TimeLoop> {
    public TimeLoopRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(TimeLoop loop, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        LivingEntity target = loop.getTarget();
        if (target == null) {
            return;
        }
        float age = loop.tickCount + partialTicks;
        float appear = Mth.clamp(age / 8, 0, 1);
        int done = (int) (age / TimeLoop.PERIOD);
        float sweep = (age % TimeLoop.PERIOD) / TimeLoop.PERIOD;
        Vec3 off = target.getPosition(partialTicks).subtract(loop.getPosition(partialTicks));
        poseStack.pushPose();
        poseStack.translate(off.x, off.y, off.z);
        // The prisoner, shimmering
        if (!CosmosRender.inEcho()) {
            CosmosRender.echo(target, poseStack, buffer, partialTicks, Cosmos.AMBER, .22f + .12f * Mth.sin(age * .4f), age * .03f);
        }
        float radius = target.getBbWidth() * .5f + .85f;
        poseStack.translate(0, target.getBbHeight() * .55f, 0);
        poseStack.mulPose(Axis.YP.rotationDegrees(age * 1.2f));
        poseStack.mulPose(Axis.XP.rotationDegrees(8 * Mth.sin(age * .05f)));
        VertexConsumer stroke = buffer.getBuffer(RenderType.eyes(GlyphRender.STROKE));
        int gold = RuneRender.additive(Cosmos.GOLD, .9f * appear);
        int dim = RuneRender.additive(Cosmos.GOLD, .4f * appear);
        int n = 60;
        for (int k = 0; k < n; k++) {
            float a0 = k * Mth.TWO_PI / n, a1 = (k + 1) * Mth.TWO_PI / n;
            Vec3 p0 = new Vec3(Mth.cos(a0) * radius, 0, Mth.sin(a0) * radius);
            Vec3 p1 = new Vec3(Mth.cos(a1) * radius, 0, Mth.sin(a1) * radius);
            GlyphRender.beam(stroke, poseStack.last(), p0, p1, .07f, gold);
            boolean hour = k % 5 == 0;
            float in = hour ? .2f : .09f;
            GlyphRender.beam(stroke, poseStack.last(), p0, p0.scale(1 - in / radius), hour ? .06f : .025f, hour ? gold : dim);
            // The sweep of the loop: an arc of orange behind its hand
            float t = (float) k / n;
            if (t <= sweep) {
                float trail = 1 - (sweep - t) * 2.5f;
                if (trail > 0) {
                    GlyphRender.beam(stroke, poseStack.last(), p0.scale(1.06), p1.scale(1.06), .22f * trail, RuneRender.additive(Cosmos.ORANGE, .8f * trail * appear));
                }
            }
        }
        float ha = sweep * Mth.TWO_PI;
        Vec3 hand = new Vec3(Mth.cos(ha) * radius * 1.12f, 0, Mth.sin(ha) * radius * 1.12f);
        GlyphRender.beam(stroke, poseStack.last(), hand.scale(.7), hand, .09f, RuneRender.additive(Cosmos.CREAM, appear));
        // A bead for each loop still to come
        int left = Math.max(0, loop.getLoops() - done);
        for (int k = 0; k < left; k++) {
            float a = Mth.PI + (k - (left - 1) * .5f) * .28f;
            poseStack.pushPose();
            poseStack.translate(Mth.cos(a) * radius, .22f, Mth.sin(a) * radius);
            CosmosRender.star(poseStack, buffer, .3f, Cosmos.AMBER, appear, age * 4);
            poseStack.popPose();
        }
        poseStack.popPose();
        super.render(loop, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(TimeLoop loop) {
        return GlyphRender.STROKE;
    }
}
