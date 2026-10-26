package com.waylays.arcanes.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.waylays.arcanes.entity.cosmos.TimeStorm;
import com.waylays.arcanes.spells.cosmos.Cosmos;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import java.util.Arrays;
import java.util.Comparator;

/**
 * The End of Time: a churning mass of dark violet cloud, lit orange from within by flickering light, turning slowly
 * on itself with puffs rising and sinking; a dark stain on the ground under it; lightning crawling inside it, and
 * tendrils of orange lightning lashing out of it at what it strikes.
 */
public class TimeStormRenderer extends EntityRenderer<TimeStorm> {
    private static final int PUFFS = 48;
    private static final int DARK = 0x2A1038;
    private static final int PURPLE = 0x5E2A80;
    private static final int LIT = 0xC8642A;

    public TimeStormRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public boolean shouldRender(TimeStorm storm, Frustum frustum, double x, double y, double z) {
        return true;
    }

    /** Where puff {@code k} is at {@code age}, the storm {@code s} grown. */
    private static Vec3 puff(TimeStorm storm, int k, float age, float s) {
        float h1 = RuneRender.hash(k * 7 + 1), h2 = RuneRender.hash(k * 7 + 2), h3 = RuneRender.hash(k * 7 + 3);
        float a = h1 * Mth.TWO_PI + age * (.008f + .018f * h2) * (k % 3 == 0 ? -1 : 1);
        float layer = h3;
        float d = TimeStorm.RADIUS * s * (.15f + .85f * Mth.sqrt(h2)) * (1 - .35f * layer);
        float y = TimeStorm.HEIGHT * s * (.12f + .8f * layer) + Mth.sin(age * .05f + k) * .35f;
        return new Vec3(Mth.cos(a) * d, y, Mth.sin(a) * d);
    }

    @Override
    public void render(TimeStorm storm, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        float age = storm.tickCount + partialTicks;
        float s = storm.size(age);
        if (s <= .01f) {
            return;
        }
        // The stain on the ground
        poseStack.pushPose();
        poseStack.translate(0, .04, 0);
        RenderHelper.groundQuad(buffer.getBuffer(RenderType.entityTranslucent(CosmosRender.HAZE)), poseStack.last(), TimeStorm.RADIUS * s * 1.5f,
                RuneRender.alpha(DARK, .75f), GlyphRender.FULL);
        poseStack.popPose();
        // The clouds, far ones first
        Vec3 camera = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition().subtract(storm.getPosition(partialTicks));
        Integer[] order = new Integer[PUFFS];
        Vec3[] at = new Vec3[PUFFS];
        for (int k = 0; k < PUFFS; k++) {
            order[k] = k;
            at[k] = puff(storm, k, age, s);
        }
        Arrays.sort(order, Comparator.comparingDouble(k -> -at[k].distanceToSqr(camera)));
        VertexConsumer clouds = buffer.getBuffer(RenderType.entityTranslucent(CosmosRender.CLOUD));
        for (int k : order) {
            float h = RuneRender.hash(k * 7 + 4);
            float size = (1.6f + 1.8f * h) * s;
            float lit = Mth.clamp(.5f + .5f * Mth.sin(age * (.1f + .2f * h) + k * 2.3f), 0, 1) * (1 - (float) at[k].y / (TimeStorm.HEIGHT * s + 1));
            int colour = GoldRender.lerp(h < .5f ? DARK : PURPLE, LIT, lit * .55f);
            poseStack.pushPose();
            poseStack.translate(at[k].x, at[k].y, at[k].z);
            poseStack.mulPose(Minecraft.getInstance().getEntityRenderDispatcher().cameraOrientation());
            poseStack.mulPose(Axis.ZP.rotationDegrees(h * 360 + age * (h - .5f) * 2));
            RenderHelper.billboard(clouds, poseStack.last(), size, RuneRender.alpha(colour, .82f), GlyphRender.FULL);
            poseStack.popPose();
        }
        // The light inside it
        for (int k = 0; k < 6; k++) {
            float flicker = Mth.clamp(Mth.sin(age * (.3f + k * .07f) + k * 5) * 1.6f - .4f, 0, 1);
            Vec3 p = puff(storm, k * 7 + 3, age, s * .7f);
            GemRender.glow(poseStack, buffer, p, k % 2 == 0 ? Cosmos.ORANGE : Cosmos.VIOLET, 3.5f * s, .35f * flicker);
        }
        GemRender.glow(poseStack, buffer, new Vec3(0, TimeStorm.HEIGHT * s * .45f, 0), Cosmos.EMBER, TimeStorm.RADIUS * s * 2.2f, .22f);
        // Lightning crawling inside
        int seed = (int) (age / 3);
        for (int k = 0; k < 2; k++) {
            if (RuneRender.hash(seed * 5 + k) < .55f) {
                Vec3 a = puff(storm, (seed + k * 17) % PUFFS, age, s), b = puff(storm, (seed * 3 + k * 29 + 5) % PUFFS, age, s);
                GlyphRender.lightning(buffer, poseStack, a, b, seed * 13 + k, Cosmos.ORANGE, Cosmos.CREAM, .8f, .8f);
            }
        }
        // Tendrils lashing out
        float lash = age - storm.getLashTick();
        if (lash >= 0 && lash < 7) {
            float strength = 1 - lash / 7;
            Vec3 origin = storm.getPosition(partialTicks);
            for (int k = 0; k < TimeStorm.TENDRILS; k++) {
                Entity target = storm.level().getEntity(storm.getTendrilTarget(k));
                if (target == null) {
                    continue;
                }
                Vec3 to = target.getPosition(partialTicks).add(0, target.getBbHeight() * .5, 0).subtract(origin);
                Vec3 from = puff(storm, k * 11 + 2, age, s * .6f);
                int bolt = storm.getLashTick() * 7 + k * 101;
                GlyphRender.lightning(buffer, poseStack, from, to, bolt, Cosmos.ORANGE, Cosmos.CREAM, strength, 1.6f);
                GlyphRender.lightning(buffer, poseStack, from, to, bolt + 3, Cosmos.VIOLET, Cosmos.AMBER, strength * .6f, 2.4f);
            }
        }
        super.render(storm, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(TimeStorm storm) {
        return CosmosRender.CLOUD;
    }
}
