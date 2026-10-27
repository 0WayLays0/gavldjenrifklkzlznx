package com.waylays.arcanes.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.waylays.arcanes.entity.wood.LeafStorm;
import com.waylays.arcanes.spells.wood.WoodFx;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * Leaf Storm: hundreds of leaves (and pink petals) whirling up round an axis, the funnel narrow at its foot and wide at
 * its top, each leaf tumbling as it goes; green wisps of wind spiralling with them, the ground stirred below. When it
 * scatters the leaves fly outwards and away.
 */
public class LeafStormRenderer extends EntityRenderer<LeafStorm> {
    private static final int LEAVES = 220;
    private static final int WISPS = 5;

    public LeafStormRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(LeafStorm storm, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        float age = storm.tickCount + partialTicks;
        float strength = storm.strength(age);
        float scatter = Mth.clamp((age - storm.getLife()) / LeafStorm.SCATTER, 0, 1);
        if (strength <= .01f && scatter >= 1) {
            return;
        }
        int seed = storm.getId() * 4099;
        VertexConsumer leaf = WoodRender.leafBuffer(buffer);
        int shown = (int) (LEAVES * Mth.clamp(age / LeafStorm.GATHER, .15f, 1));
        for (int i = 0; i < shown; i++) {
            float h = WoodFx.hash(seed + i * 5);
            // Each leaf climbs the funnel and starts again at its foot
            float speed = .006f + .006f * WoodFx.hash(seed + i * 5 + 1);
            float climb = (h + age * speed) % 1;
            float y = climb * LeafStorm.HEIGHT;
            float r = (.5f + LeafStorm.RADIUS * (.35f + .65f * climb)) * (.75f + .35f * WoodFx.hash(seed + i * 5 + 2));
            float a = WoodFx.hash(seed + i * 5 + 3) * Mth.TWO_PI + age * (.32f - .14f * climb);
            r += scatter * (4 + 6 * WoodFx.hash(seed + i * 5 + 4));
            y += scatter * 2 * WoodFx.hash(seed + i * 7);
            poseStack.pushPose();
            poseStack.translate(Mth.cos(a) * r, y, Mth.sin(a) * r);
            // Along the whirl, tumbling
            poseStack.mulPose(Axis.YP.rotation(-a));
            poseStack.mulPose(Axis.XP.rotation(age * (.2f + .3f * h) + i));
            poseStack.mulPose(Axis.ZP.rotation(age * .15f + i * 2));
            float size = (.28f + .18f * WoodFx.hash(seed + i * 5 + 4)) * Math.max(.2f, 1 - scatter);
            WoodRender.leaf(leaf, poseStack.last(), size, WoodRender.leafColour(seed + i), packedLight);
            poseStack.popPose();
        }
        // Green wisps of wind spiralling up with the leaves
        VertexConsumer wind = buffer.getBuffer(RenderType.eyes(GlyphRender.STROKE));
        float fade = strength * .55f;
        if (fade > 0) {
            for (int w = 0; w < WISPS; w++) {
                float phase = w * Mth.TWO_PI / WISPS;
                net.minecraft.world.phys.Vec3 prev = null;
                for (int k = 0; k <= 16; k++) {
                    float u = k / 16f;
                    float y = u * LeafStorm.HEIGHT * .9f + .2f;
                    float r = .4f + LeafStorm.RADIUS * (.3f + .6f * u);
                    float a = phase + age * .28f + u * 4.2f;
                    net.minecraft.world.phys.Vec3 at = new net.minecraft.world.phys.Vec3(Mth.cos(a) * r, y, Mth.sin(a) * r);
                    if (prev != null) {
                        float alpha = fade * Mth.sin(u * Mth.PI);
                        GlyphRender.beam(wind, poseStack.last(), prev, at, .09f, RuneRender.additive(WoodFx.LIFE, alpha));
                    }
                    prev = at;
                }
            }
            VertexConsumer glow = buffer.getBuffer(RenderType.eyes(GlyphRender.GLOW));
            poseStack.pushPose();
            poseStack.translate(0, .05f, 0);
            poseStack.mulPose(Axis.YP.rotation(age * .2f));
            RenderHelper.groundQuad(glow, poseStack.last(), LeafStorm.RADIUS + 1, RuneRender.additive(WoodFx.LIFE, strength * .35f), GlyphRender.FULL);
            poseStack.popPose();
        }
        super.render(storm, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    @Override
    public boolean shouldRender(LeafStorm storm, Frustum frustum, double x, double y, double z) {
        return frustum.isVisible(storm.getBoundingBox().inflate(LeafStorm.RADIUS + 8, LeafStorm.HEIGHT + 2, LeafStorm.RADIUS + 8));
    }

    @Override
    public ResourceLocation getTextureLocation(LeafStorm storm) {
        return WoodRender.LEAF;
    }
}
