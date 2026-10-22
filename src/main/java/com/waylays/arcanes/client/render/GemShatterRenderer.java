package com.waylays.arcanes.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.waylays.arcanes.entity.gem.GemShatter;
import com.waylays.arcanes.spells.gem.Gem;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/** A gem breaking: a white flash, then crystal shards of it flying out in every direction, tumbling, glinting, fading. */
public class GemShatterRenderer extends EntityRenderer<GemShatter> {
    public GemShatterRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(GemShatter shatter, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        float age = shatter.tickCount + partialTicks;
        float t = Mth.clamp(age / GemShatter.LIFE, 0, 1);
        float size = shatter.getSize();
        Gem gem = shatter.getGem();
        int seed = shatter.getId() * 71;
        int n = shatter.getShards();
        // The shards
        for (int i = 0; i < n; i++) {
            float h1 = RuneRender.hash(seed + i), h2 = RuneRender.hash(seed + i * 3 + 1), h3 = RuneRender.hash(seed + i * 7 + 2);
            float yaw = (i + h1) * Mth.TWO_PI / n;
            float pitch = (h2 - .3f) * 1.4f;
            float speed = (.12f + .16f * h3) * Math.max(.8f, size * .6f);
            Vec3 v = new Vec3(Mth.cos(yaw) * Mth.cos(pitch), Mth.sin(pitch) + .25f, Mth.sin(yaw) * Mth.cos(pitch)).scale(speed);
            Vec3 p = v.scale(age).add(0, -.02 * age * age, 0);
            float shrink = 1 - Mth.clamp((t - .55f) / .45f, 0, 1);
            if (shrink <= 0) {
                continue;
            }
            poseStack.pushPose();
            poseStack.translate(p.x, p.y, p.z);
            poseStack.mulPose(Axis.XP.rotationDegrees(age * (20 + 30 * h1)));
            poseStack.mulPose(Axis.ZP.rotationDegrees(age * (14 + 26 * h2)));
            GemRender.gem(poseStack, buffer, GemRender.Cut.CRYSTAL, gem.color, (.18f + .22f * h3) * Math.max(1, size * .5f) * shrink, .85f, age * 3 + i * 10);
            poseStack.popPose();
        }
        // The flash, and a glint on some shards
        float flash = Math.max(0, 1 - age / 7);
        GemRender.glow(poseStack, buffer, Vec3.ZERO, gem.color, size * 1.6f, flash * .9f);
        GemRender.glow(poseStack, buffer, Vec3.ZERO, 0xFFFFFF, size * .7f, flash);
        super.render(shatter, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    @Override
    public boolean shouldRender(GemShatter shatter, Frustum frustum, double x, double y, double z) {
        return frustum.isVisible(shatter.getBoundingBox().inflate(6));
    }

    @Override
    public ResourceLocation getTextureLocation(GemShatter shatter) {
        return GemRender.FACET;
    }
}
