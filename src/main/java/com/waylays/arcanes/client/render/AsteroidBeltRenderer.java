package com.waylays.arcanes.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.waylays.arcanes.entity.cosmos.AsteroidBelt;
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
 * An asteroid belt: tumbling rocks of every size and colour going round in a ring of dust, a dashed line of amber on
 * the ground where the cage stands. Falling in, the rocks heat red then orange as they close, trailing fire, and
 * crash in a flash of light and a ring of dust.
 */
public class AsteroidBeltRenderer extends EntityRenderer<AsteroidBelt> {
    private static final int DUST = 0xC8B8A0;

    public AsteroidBeltRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public boolean shouldRender(AsteroidBelt belt, Frustum frustum, double x, double y, double z) {
        return true;
    }

    @Override
    public void render(AsteroidBelt belt, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        float age = belt.tickCount + partialTicks;
        float crush = belt.crushProgress(age);
        int start = belt.getCrushStart();
        float after = start < 0 ? -1 : age - start - AsteroidBelt.CRUSH_TICKS;
        float r = belt.radiusAt(age);
        float grow = Mth.clamp(age / AsteroidBelt.GATHER_TICKS, 0, 1);
        int n = belt.getCount();
        // The rocks
        if (after < 0) {
            VertexConsumer rocks = AsteroidRender.buffer(buffer);
            for (int k = 0; k < n; k++) {
                Vec3 p = belt.rock(k, age);
                float size = belt.rockSize(k) * Mth.clamp(age / 6 - k * .05f, 0, 1);
                if (size <= .01f) {
                    continue;
                }
                int tint = AsteroidRender.TINTS[(int) (belt.trait(k, 6) * AsteroidRender.SHAPES) % AsteroidRender.SHAPES];
                tint = GoldRender.lerp(tint, crush < .5f ? Cosmos.EMBER : Cosmos.ORANGE, Mth.clamp(crush * 1.4f, 0, 1));
                poseStack.pushPose();
                poseStack.translate(p.x, p.y, p.z);
                float spin = age * (2 + 5 * belt.trait(k, 7)) * (1 + crush * 4);
                AsteroidRender.rock(poseStack, rocks, k, size, spin, tint, age + k * 11);
                poseStack.popPose();
            }
        }
        // Lines and trails (light added)
        VertexConsumer stroke = buffer.getBuffer(RenderType.eyes(GlyphRender.STROKE));
        if (after < 0) {
            float line = .35f * grow * (1 - crush);
            int segments = 64;
            for (int k = 0; k < segments; k += 2) {
                float a0 = k * Mth.TWO_PI / segments + age * .01f, a1 = (k + 1) * Mth.TWO_PI / segments + age * .01f;
                GlyphRender.beam(stroke, poseStack.last(), new Vec3(Mth.cos(a0) * r, .06, Mth.sin(a0) * r), new Vec3(Mth.cos(a1) * r, .06, Mth.sin(a1) * r),
                        .09f, RuneRender.additive(Cosmos.AMBER, line));
            }
            // Fire trails while falling in; dust trails while going round
            for (int k = 0; k < n; k++) {
                float size = belt.rockSize(k);
                Vec3 p = belt.rock(k, age);
                Vec3 back = belt.rock(k, age - (crush > 0 ? 2.5f : 4));
                if (crush > 0) {
                    GlyphRender.beam(stroke, poseStack.last(), back, p, size * .9f, RuneRender.additive(Cosmos.ORANGE, .7f * crush));
                    GlyphRender.beam(stroke, poseStack.last(), back, p, size * .35f, RuneRender.additive(Cosmos.CREAM, .6f * crush));
                } else if (size > .6f) {
                    GlyphRender.beam(stroke, poseStack.last(), back, p, size * .5f, RuneRender.additive(DUST, .12f * grow));
                }
            }
        }
        // The dust of the belt
        if (after < 0) {
            poseStack.pushPose();
            poseStack.translate(0, 1.2, 0);
            CosmosRender.ring(poseStack, buffer, Math.max(.1f, r - AsteroidBelt.BAND), r + AsteroidBelt.BAND, DUST, .22f * grow * (1 - crush));
            poseStack.translate(0, -.8, 0);
            CosmosRender.ring(poseStack, buffer, Math.max(.1f, r - AsteroidBelt.BAND * .6f), r + AsteroidBelt.BAND * .6f, DUST, .14f * grow * (1 - crush));
            poseStack.popPose();
        }
        // Closing in: the heat in the middle; then the crash
        if (crush > 0 && after < 0) {
            GemRender.glow(poseStack, buffer, new Vec3(0, 1.1, 0), Cosmos.ORANGE, 1 + 2 * crush, .5f * crush);
        }
        if (after >= 0) {
            float t = after / AsteroidBelt.AFTER_TICKS;
            float fade = 1 - t;
            GemRender.glow(poseStack, buffer, new Vec3(0, 1.1, 0), Cosmos.CREAM, 3 + 4 * t, fade);
            GemRender.glow(poseStack, buffer, new Vec3(0, 1.1, 0), Cosmos.ORANGE, 5 + 7 * t, .8f * fade);
            poseStack.pushPose();
            poseStack.translate(0, .15, 0);
            CosmosRender.ring(poseStack, buffer, 1 + 6 * t, 2 + 8 * t, DUST, .5f * fade);
            poseStack.popPose();
        }
        super.render(belt, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(AsteroidBelt belt) {
        return CosmosRender.MOON;
    }
}
