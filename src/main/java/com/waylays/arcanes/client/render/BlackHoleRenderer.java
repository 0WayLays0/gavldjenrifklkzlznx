package com.waylays.arcanes.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.waylays.arcanes.entity.cosmos.BlackHole;
import com.waylays.arcanes.spells.cosmos.Cosmos;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * A black hole: the horizon a sphere of pure black, a thin ring of light hugging it, its accretion disk whirling round
 * it (tilted, white-hot inside, orange and red outside) and the far side of the disk bent over and under it into a
 * ring facing whoever looks; streaks of light spiralling in from all round. Collapsing, it shrinks to a point, then
 * bursts in a flash with a ring of light running out and two jets shooting up and down.
 */
public class BlackHoleRenderer extends EntityRenderer<BlackHole> {
    private static final int STREAKS = 28;

    public BlackHoleRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public boolean shouldRender(BlackHole hole, Frustum frustum, double x, double y, double z) {
        return true;
    }

    @Override
    public void render(BlackHole hole, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        float age = hole.tickCount + partialTicks;
        float r = hole.horizon(age);
        int start = hole.getCollapseStart();
        float after = start < 0 ? -1 : age - start - BlackHole.COLLAPSE_TICKS;
        float range = hole.getRange();
        if (after < 0 && r > .02f) {
            // The horizon, then what lies on and round it
            CosmosRender.blackSphere(poseStack, buffer, r);
            poseStack.pushPose();
            poseStack.mulPose(Axis.XP.rotationDegrees(14));
            poseStack.mulPose(Axis.ZP.rotationDegrees(8));
            CosmosRender.disk(poseStack, buffer, r * 1.35f, r * 4.4f, age * .012f, 0xFFB060, .95f);
            CosmosRender.disk(poseStack, buffer, r * 1.25f, r * 2.2f, age * .02f, Cosmos.CREAM, .6f);
            poseStack.popPose();
            CosmosRender.lensedDisk(poseStack, buffer, r * 1.08f, r * 1.75f, age * .015f, 0xFFC27A, .85f);
            CosmosRender.atmosphere(poseStack, buffer, r * .98f, Cosmos.CREAM, .9f);
            GemRender.glow(poseStack, buffer, Vec3.ZERO, Cosmos.ORANGE, r * 7, .3f);
            GemRender.glow(poseStack, buffer, Vec3.ZERO, Cosmos.VIOLET, r * 14, .18f);
            // Light falling in
            VertexConsumer stroke = buffer.getBuffer(RenderType.eyes(GlyphRender.STROKE));
            float grow = Mth.clamp(age / BlackHole.GROW_TICKS, 0, 1);
            for (int k = 0; k < STREAKS; k++) {
                float h = RuneRender.hash(k * 13 + 7);
                float phase = (age * (.012f + .01f * h) + h) % 1;
                Vec3 a = spiral(k, phase, range, r), b = spiral(k, Math.min(1, phase + .06f), range, r);
                float fade = Mth.sin(phase * Mth.PI) * grow;
                GlyphRender.beam(stroke, poseStack.last(), a, b, .1f + .1f * phase, RuneRender.additive(phase > .7f ? Cosmos.CREAM : Cosmos.AMBER, .55f * fade));
            }
        }
        if (start >= 0 && after < 0) {
            float c = Mth.clamp((age - start) / BlackHole.COLLAPSE_TICKS, 0, 1);
            GemRender.glow(poseStack, buffer, Vec3.ZERO, Cosmos.STAR, .5f + c * 2, c);
        }
        if (after >= 0) {
            float t = Mth.clamp(after / BlackHole.JET_TICKS, 0, 1);
            float fade = 1 - t;
            // The flash and the ring running out
            GemRender.glow(poseStack, buffer, Vec3.ZERO, Cosmos.STAR, 4 + 10 * t, fade * fade);
            GemRender.glow(poseStack, buffer, Vec3.ZERO, Cosmos.ORANGE, 8 + 16 * t, .7f * fade);
            CosmosRender.ring(poseStack, buffer, BlackHole.BURST_RADIUS * 2 * Math.min(1, t * 3) - 1.2f, BlackHole.BURST_RADIUS * 2 * Math.min(1, t * 3) + .6f, 0xFFD8A0, .7f * fade);
            // The two jets
            VertexConsumer stroke = buffer.getBuffer(RenderType.eyes(GlyphRender.STROKE));
            float length = 30 * Math.min(1, t * 4);
            float width = 1.6f * fade;
            for (int side = -1; side <= 1; side += 2) {
                Vec3 tip = new Vec3(0, side * length, 0);
                GlyphRender.beam(stroke, poseStack.last(), Vec3.ZERO, tip, width * 2.2f, RuneRender.additive(Cosmos.BLUE, .5f * fade));
                GlyphRender.beam(stroke, poseStack.last(), Vec3.ZERO, tip, width, RuneRender.additive(Cosmos.ICE, .8f * fade));
                GlyphRender.beam(stroke, poseStack.last(), Vec3.ZERO, tip.scale(.9), width * .35f, RuneRender.additive(Cosmos.STAR, fade));
            }
        }
        super.render(hole, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    /** Streak {@code k}'s spiral in, from the edge of the pull ({@code phase} 0) to the horizon (1). */
    private static Vec3 spiral(int k, float phase, float range, float horizon) {
        float h1 = RuneRender.hash(k * 31 + 1), h2 = RuneRender.hash(k * 31 + 2);
        float d = Mth.lerp(phase * phase, range * (.5f + .4f * h1), horizon * 1.2f);
        float a = h2 * Mth.TWO_PI + phase * 5;
        float tilt = (h1 - .5f) * 1.2f * (1 - phase);
        return new Vec3(Mth.cos(a) * d, Mth.sin(a * .5f + k) * tilt * d * .4f, Mth.sin(a) * d);
    }

    @Override
    public ResourceLocation getTextureLocation(BlackHole hole) {
        return CosmosRender.ACCRETION;
    }
}
