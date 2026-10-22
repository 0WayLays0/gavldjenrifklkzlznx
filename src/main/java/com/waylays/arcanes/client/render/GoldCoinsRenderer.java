package com.waylays.arcanes.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.waylays.arcanes.entity.gold.GoldCoins;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/** A spray of gold coins: each thrown out spinning on its own, falling, bouncing once on the ground, glinting, fading. */
public class GoldCoinsRenderer extends EntityRenderer<GoldCoins> {
    public GoldCoinsRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(GoldCoins coins, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        float age = coins.tickCount + partialTicks;
        float force = coins.getForce();
        float floor = coins.getFloor();
        int seed = coins.getId() * 37;
        for (int i = 0; i < coins.getCount(); i++) {
            float h1 = RuneRender.hash(seed + i), h2 = RuneRender.hash(seed + i * 5 + 1), h3 = RuneRender.hash(seed + i * 9 + 2);
            float yaw = (i + h1 * .8f) * Mth.TWO_PI / coins.getCount();
            float out = force * (.35f + .65f * h2);
            float up = force * (.6f + .8f * h3);
            Vec3 p = flight(age, Mth.cos(yaw) * out, up, Mth.sin(yaw) * out, floor);
            float fade = 1 - Mth.clamp((age - GoldCoins.LIFE * .7f) / (GoldCoins.LIFE * .3f), 0, 1);
            if (fade <= 0) {
                continue;
            }
            poseStack.pushPose();
            poseStack.translate(p.x, p.y, p.z);
            boolean landed = p.y <= floor + .02f && age > 4;
            if (landed) {
                // Lying flat where it fell, spinning down on its rim
                poseStack.mulPose(Axis.YP.rotationDegrees(h1 * 360));
                poseStack.mulPose(Axis.XP.rotationDegrees(8 * Mth.sin(age * (1 + h2)) * fade));
            } else {
                poseStack.mulPose(Axis.YP.rotationDegrees(age * (20 + 30 * h1)));
                poseStack.mulPose(Axis.XP.rotationDegrees(age * (25 + 40 * h2) + h3 * 90));
            }
            GoldRender.coin(poseStack, buffer, .22f * fade, age + i * 7, i);
            poseStack.popPose();
        }
        GemRender.glow(poseStack, buffer, Vec3.ZERO, GoldRender.PALE, 1.2f * force + .5f, Math.max(0, 1 - age / 6) * .8f);
        super.render(coins, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    /** Where a coin thrown with velocity (vx, vy, vz) per tick is after {@code t} ticks: falling, bouncing once on the floor. */
    private static Vec3 flight(float t, float vx, float vy, float vz, float floor) {
        float g = .045f;
        float y = vy * t - g * t * t * .5f;
        float drag = 1 / (1 + t * .02f);
        if (y < floor) {
            // The bounce: from the moment it hits, up again at a third of the speed, then still
            float hit = (vy + Mth.sqrt(Math.max(0, vy * vy - 2 * g * floor))) / g;
            float after = t - hit;
            float bounce = Math.max(0, (g * hit - vy) * .3f);
            y = floor + Math.max(0, bounce * after - g * after * after * .5f);
            t = hit + Math.min(after, bounce / g * 2) * .4f;
        }
        return new Vec3(vx * t * drag, y, vz * t * drag);
    }

    @Override
    public boolean shouldRender(GoldCoins coins, Frustum frustum, double x, double y, double z) {
        return frustum.isVisible(coins.getBoundingBox().inflate(6));
    }

    @Override
    public ResourceLocation getTextureLocation(GoldCoins coins) {
        return GoldRender.COIN;
    }
}
