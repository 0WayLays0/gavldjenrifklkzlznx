package com.waylays.arcanes.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.waylays.arcanes.entity.cosmos.TimeStopBubble;
import com.waylays.arcanes.spells.cosmos.Cosmos;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * A bubble of stopped time: an orange haze within a glowing rim, three great clock rings round it that spin up and
 * then stop dead, a clock face lying on the ground under it with its hands stopped, motes of light hanging still in
 * the air, and every frozen creature shimmering with the bubble's light. It bursts outward when time starts again.
 */
public class TimeStopBubbleRenderer extends EntityRenderer<TimeStopBubble> {
    private static final int MOTES = 48;

    public TimeStopBubbleRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public boolean shouldRender(TimeStopBubble bubble, Frustum frustum, double x, double y, double z) {
        return true;
    }

    @Override
    public void render(TimeStopBubble bubble, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        float age = bubble.tickCount + partialTicks;
        float r = bubble.getRadius();
        float grow = Mth.clamp(age / 6, 0, 1);
        float left = bubble.getLife() - age;
        float burst = Mth.clamp(1 - left / 6, 0, 1);
        float strength = grow * (1 - burst);
        float radius = r * (1 - (1 - grow) * (1 - grow)) * (1 + burst * .35f);
        // The frozen, shimmering
        if (!CosmosRender.inEcho()) {
            for (LivingEntity e : bubble.level().getEntitiesOfClass(LivingEntity.class, new AABB(bubble.position(), bubble.position()).inflate(r),
                    e -> e.getId() != bubble.getCasterId() && bubble.inside(e))) {
                Vec3 off = e.getPosition(partialTicks).subtract(bubble.getPosition(partialTicks));
                poseStack.pushPose();
                poseStack.translate(off.x, off.y, off.z);
                CosmosRender.echo(e, poseStack, buffer, partialTicks, Cosmos.AMBER, .3f * strength, 0);
                poseStack.popPose();
            }
        }
        poseStack.pushPose();
        poseStack.translate(0, 1, 0);
        // The haze and its rim
        CosmosRender.skySphere(poseStack, buffer, CosmosRender.NEBULA, radius, 2, Cosmos.ORANGE, .1f * strength);
        poseStack.pushPose();
        poseStack.scale(-1, 1, 1);
        CosmosRender.skySphere(poseStack, buffer, CosmosRender.NEBULA, radius, 2, Cosmos.ORANGE, .08f * strength);
        poseStack.popPose();
        CosmosRender.atmosphere(poseStack, buffer, radius, Cosmos.ORANGE, .8f * Math.max(strength, burst * (1 - burst) * 4));
        // Three great clock rings: they spin up, then time stops and so do they
        float spin = age < 12 ? 120 * (1 - (1 - age / 12) * (1 - age / 12)) : 120;
        VertexConsumer stroke = buffer.getBuffer(RenderType.eyes(GlyphRender.STROKE));
        int gold = RuneRender.additive(Cosmos.GOLD, .7f * strength);
        int ticks = RuneRender.additive(Cosmos.AMBER, .6f * strength);
        for (int ring = 0; ring < 3; ring++) {
            poseStack.pushPose();
            poseStack.mulPose(Axis.YP.rotationDegrees(ring * 60 + spin * (ring % 2 == 0 ? 1 : -1)));
            poseStack.mulPose(Axis.XP.rotationDegrees(ring == 0 ? 90 : 90 - ring * 35));
            float rr = radius * (1.02f + ring * .04f);
            int n = 60;
            for (int k = 0; k < n; k++) {
                float a0 = k * Mth.TWO_PI / n, a1 = (k + 1) * Mth.TWO_PI / n;
                Vec3 p0 = new Vec3(Mth.cos(a0) * rr, 0, Mth.sin(a0) * rr);
                Vec3 p1 = new Vec3(Mth.cos(a1) * rr, 0, Mth.sin(a1) * rr);
                GlyphRender.beam(stroke, poseStack.last(), p0, p1, .1f, gold);
                if (k % 5 == 0) {
                    GlyphRender.beam(stroke, poseStack.last(), p0, p0.scale(.94), .12f, ticks);
                }
            }
            poseStack.popPose();
        }
        poseStack.popPose();
        // Motes of light hanging still
        for (int k = 0; k < MOTES; k++) {
            float u = RuneRender.hash(bubble.getId() * 97 + k), v = RuneRender.hash(bubble.getId() * 89 + k * 3), w = RuneRender.hash(k * 7 + 5);
            float theta = u * Mth.TWO_PI, phi = (float) Math.acos(2 * v - 1), rad = radius * (float) Math.cbrt(w) * .95f;
            poseStack.pushPose();
            poseStack.translate(Mth.sin(phi) * Mth.cos(theta) * rad, 1 + Mth.cos(phi) * rad, Mth.sin(phi) * Mth.sin(theta) * rad);
            CosmosRender.star(poseStack, buffer, .18f + w * .2f, k % 3 == 0 ? Cosmos.CREAM : Cosmos.AMBER, strength * .9f, k * 37);
            poseStack.popPose();
        }
        // The stopped clock on the ground
        poseStack.pushPose();
        poseStack.translate(0, .06, 0);
        poseStack.mulPose(Axis.XP.rotationDegrees(-90));
        CosmosRender.clock(poseStack, buffer, radius * .55f, .75f * strength, .25f, .5f, Math.min(age, 12) / 60f);
        poseStack.popPose();
        super.render(bubble, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(TimeStopBubble bubble) {
        return CosmosRender.NEBULA;
    }
}
