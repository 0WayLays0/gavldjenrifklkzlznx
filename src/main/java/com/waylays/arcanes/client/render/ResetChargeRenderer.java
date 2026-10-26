package com.waylays.arcanes.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.waylays.arcanes.entity.cosmos.ResetCharge;
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
 * A TVA reset charge: a squat bronze drum on three feet, a ring of orange light round its waist and a clock face
 * glowing on its top, its hands running backward faster and faster as it counts down; a light on its crown blinking
 * quicker each second; on the ground the TVA's rings of orange marked off in seconds, a dashed ring at the edge of
 * what it will reset. When it goes off: a dome of orange light running out to its edge, scanlines of time sweeping
 * over it, then fading.
 */
public class ResetChargeRenderer extends EntityRenderer<ResetCharge> {
    private static final float[] DRUM = drum();

    public ResetChargeRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    private static float[] drum() {
        Solid.Builder b = new Solid.Builder();
        int n = 16;
        float[][] foot = Solid.ring(n, .12f, .34f, .34f, 0), waist = Solid.ring(n, .3f, .4f, .4f, 0), shoulder = Solid.ring(n, .62f, .4f, .4f, 0);
        float[][] top = Solid.ring(n, .72f, .3f, .3f, 0);
        b.band(foot, waist).band(waist, shoulder).band(shoulder, top);
        b.fan(top, new float[]{0, .74f, 0});
        float[][] bottom = Solid.ring(n, .12f, .34f, .34f, 0);
        for (int i = 0; i < n; i++) {
            b.tri(bottom[i], bottom[(i + 1) % n], new float[]{0, .1f, 0});
        }
        // Three feet
        for (int k = 0; k < 3; k++) {
            float a = k * Mth.TWO_PI / 3;
            float x = Mth.cos(a) * .3f, z = Mth.sin(a) * .3f;
            b.box(x - .06f, 0, z - .06f, x + .06f, .16f, z + .06f);
        }
        return b.build();
    }

    @Override
    public boolean shouldRender(ResetCharge charge, Frustum frustum, double x, double y, double z) {
        return true;
    }

    @Override
    public void render(ResetCharge charge, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        float age = charge.tickCount + partialTicks;
        int fire = charge.getFireTick();
        float after = fire < 0 ? -1 : age - fire;
        float r = charge.getRadius();
        float progress = fire < 0 ? Mth.clamp(age / ResetCharge.COUNTDOWN, 0, 1) : 1;
        // The device
        if (after < 6) {
            float sink = after < 0 ? Mth.clamp(age / 6, 0, 1) : 1 - after / 6;
            poseStack.pushPose();
            poseStack.translate(0, (1 - sink) * .8f, 0);
            poseStack.mulPose(Axis.YP.rotationDegrees(-charge.getYRot()));
            Solid.draw(GoldRender.metal(buffer), poseStack.last(), DRUM, 0x8A5A2E, 255, age, .3f, GlyphRender.FULL);
            poseStack.popPose();
            // The clock on its top, hands running back
            poseStack.pushPose();
            poseStack.translate(0, .76f + (1 - sink) * .8f, 0);
            poseStack.mulPose(Axis.XP.rotationDegrees(-90));
            float back = progress * progress * 6;
            CosmosRender.clock(poseStack, buffer, .27f, sink, -back, -back * 12, 1 - (age * .05f) % 1);
            poseStack.popPose();
            // Its waist band and its light
            float blink = .5f + .5f * Mth.cos(age * (.3f + 1.2f * progress));
            VertexConsumer stroke = buffer.getBuffer(RenderType.eyes(GlyphRender.STROKE));
            band(stroke, poseStack, .41f, .46f, RuneRender.additive(Cosmos.ORANGE, .9f * sink));
            band(stroke, poseStack, .41f, .3f, RuneRender.additive(Cosmos.AMBER, .5f * sink));
            GemRender.glow(poseStack, buffer, new Vec3(0, 1.05, 0), Cosmos.ORANGE, .5f + .5f * progress, (.4f + .6f * blink) * sink);
            GemRender.glow(poseStack, buffer, new Vec3(0, .5, 0), Cosmos.ORANGE, 1.6f, .25f * sink);
        }
        // The ground: the TVA's rings, marked off in seconds
        float ground = after < 0 ? Mth.clamp(age / 10, 0, 1) : 1 - Mth.clamp(after / 10, 0, 1);
        if (ground > 0) {
            VertexConsumer stroke = buffer.getBuffer(RenderType.eyes(GlyphRender.STROKE));
            dashed(stroke, poseStack, r, age * .004f, 72, RuneRender.additive(Cosmos.ORANGE, .6f * ground), .12f);
            dashed(stroke, poseStack, r * .5f, -age * .008f, 40, RuneRender.additive(Cosmos.AMBER, .4f * ground), .08f);
            // The countdown runs in to the device
            float in = r * (1 - progress) + .6f;
            dashed(stroke, poseStack, in, age * .03f, 24, RuneRender.additive(Cosmos.CREAM, .7f * ground), .1f);
            for (int k = 0; k < 5; k++) {
                float a = k * Mth.TWO_PI / 5 - Mth.HALF_PI;
                boolean lit = progress * 5 > k;
                Vec3 p0 = new Vec3(Mth.cos(a) * r * .55f, .05, Mth.sin(a) * r * .55f);
                Vec3 p1 = new Vec3(Mth.cos(a) * r * .95f, .05, Mth.sin(a) * r * .95f);
                GlyphRender.beam(stroke, poseStack.last(), p0, p1, lit ? .18f : .08f, RuneRender.additive(lit ? Cosmos.ORANGE : Cosmos.BRONZE, (lit ? .8f : .5f) * ground));
            }
        }
        // Going off: the dome
        if (after >= 0) {
            float t = Mth.clamp(after / ResetCharge.AFTER_TICKS, 0, 1);
            float reach = r * Math.min(1, t * 4);
            float fade = 1 - t;
            CosmosRender.dome(poseStack, buffer, CosmosRender.SCANLINES, reach, 6, 4, age * .03f, RuneRender.additive(Cosmos.ORANGE, .6f * fade));
            CosmosRender.dome(poseStack, buffer, CosmosRender.HAZE, reach * .98f, 2, 1, -age * .01f, RuneRender.additive(Cosmos.AMBER, .35f * fade));
            GemRender.glow(poseStack, buffer, new Vec3(0, 1, 0), Cosmos.CREAM, 3 + 6 * t, fade);
            GemRender.glow(poseStack, buffer, new Vec3(0, 1, 0), Cosmos.ORANGE, 6 + r * t, .7f * fade);
        }
        super.render(charge, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    /** A ring of light round the drum at height {@code y}. */
    private static void band(VertexConsumer stroke, PoseStack poseStack, float radius, float y, int argb) {
        int n = 24;
        for (int k = 0; k < n; k++) {
            float a0 = k * Mth.TWO_PI / n, a1 = (k + 1) * Mth.TWO_PI / n;
            GlyphRender.beam(stroke, poseStack.last(), new Vec3(Mth.cos(a0) * radius, y, Mth.sin(a0) * radius), new Vec3(Mth.cos(a1) * radius, y, Mth.sin(a1) * radius), .06f, argb);
        }
    }

    /** A dashed ring on the ground. */
    private static void dashed(VertexConsumer stroke, PoseStack poseStack, float r, float turn, int segments, int argb, float width) {
        for (int k = 0; k < segments; k += 2) {
            float a0 = k * Mth.TWO_PI / segments + turn, a1 = (k + 1.3f) * Mth.TWO_PI / segments + turn;
            GlyphRender.beam(stroke, poseStack.last(), new Vec3(Mth.cos(a0) * r, .05, Mth.sin(a0) * r), new Vec3(Mth.cos(a1) * r, .05, Mth.sin(a1) * r), width, argb);
        }
    }

    @Override
    public ResourceLocation getTextureLocation(ResetCharge charge) {
        return CosmosRender.CLOCK;
    }
}
