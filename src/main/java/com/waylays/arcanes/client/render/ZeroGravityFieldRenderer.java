package com.waylays.arcanes.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.waylays.arcanes.entity.cosmos.ZeroGravityField;
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
 * A zero-gravity field: a dome of faint drifting starlight with a glowing rim, two rings of pale blue turning opposite
 * ways on the ground, and pebbles and rocks torn from the ground drifting slowly up and turning inside. When gravity
 * comes back the dome falls in, the drifting stones drop, and streaks of light rain down.
 */
public class ZeroGravityFieldRenderer extends EntityRenderer<ZeroGravityField> {
    private static final int PEBBLES = 14;

    public ZeroGravityFieldRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public boolean shouldRender(ZeroGravityField field, Frustum frustum, double x, double y, double z) {
        return true;
    }

    @Override
    public void render(ZeroGravityField field, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        float age = field.tickCount + partialTicks;
        float open = field.openness(age);
        float r = field.getRadius();
        int slam = field.getSlamStart();
        float fall = slam < 0 ? 0 : Mth.clamp((age - slam) / 8, 0, 1);
        // Drifting stones
        if (fall < 1) {
            VertexConsumer rocks = AsteroidRender.buffer(buffer);
            for (int k = 0; k < PEBBLES; k++) {
                float h1 = RuneRender.hash(field.getId() * 31 + k), h2 = RuneRender.hash(field.getId() * 31 + k + 101), h3 = RuneRender.hash(field.getId() * 31 + k + 211);
                float rise = (age * (.01f + .015f * h3) + h1) % 1;
                float a = h2 * Mth.TWO_PI + age * .004f;
                float d = (.25f + .7f * h1) * r;
                float y = rise * r * .75f;
                if (slam >= 0) {
                    y = Math.max(0, y - (age - slam) * (age - slam) * .2f);
                }
                float size = (.12f + .3f * h3 * h3) * Mth.clamp(Math.min(rise * 5, (1 - rise) * 5), 0, 1) * open;
                if (size <= .01f) {
                    continue;
                }
                poseStack.pushPose();
                poseStack.translate(Mth.cos(a) * d, y + .1, Mth.sin(a) * d);
                AsteroidRender.rock(poseStack, rocks, k, size, age * (1 + 3 * h2), AsteroidRender.TINTS[k % AsteroidRender.SHAPES], age);
                poseStack.popPose();
            }
        }
        // Rings on the ground and falling streaks
        VertexConsumer stroke = buffer.getBuffer(RenderType.eyes(GlyphRender.STROKE));
        if (open > .01f) {
            ring(stroke, poseStack, r, .05f, age * .006f, 48, RuneRender.additive(Cosmos.ICE, .45f * open), .1f);
            ring(stroke, poseStack, r * .72f, .05f, -age * .009f, 36, RuneRender.additive(Cosmos.BLUE, .45f * open), .07f);
            ring(stroke, poseStack, r * .4f, .05f, age * .014f, 24, RuneRender.additive(Cosmos.STAR, .3f * open), .05f);
        }
        if (slam >= 0 && age - slam < 10) {
            float t = (age - slam) / 10;
            for (int k = 0; k < 16; k++) {
                float a = k * Mth.TWO_PI / 16 + RuneRender.hash(k + 5) * .3f;
                float d = (.2f + .75f * RuneRender.hash(k + 50)) * r;
                float top = r * (1 - t) + 1;
                Vec3 p = new Vec3(Mth.cos(a) * d, top, Mth.sin(a) * d);
                GlyphRender.beam(stroke, poseStack.last(), p, p.add(0, 2.5, 0), .12f, RuneRender.additive(Cosmos.ICE, .7f * (1 - t)));
            }
        }
        // The dome
        if (open > .01f) {
            CosmosRender.dome(poseStack, buffer, CosmosRender.STARS, r * (.9f + .1f * open), 3, .8f, age * .0015f, RuneRender.additive(0xB8C8FF, .22f * open));
            poseStack.pushPose();
            poseStack.translate(0, r * .35f, 0);
            CosmosRender.atmosphere(poseStack, buffer, r * .82f, Cosmos.BLUE, .12f * open);
            poseStack.popPose();
        }
        if (slam >= 0 && age - slam < 8) {
            GemRender.glow(poseStack, buffer, new Vec3(0, .3, 0), Cosmos.ICE, r * (1 - (age - slam) / 8) + 1, .6f * (1 - (age - slam) / 8));
        }
        super.render(field, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    /** A dashed ring on the ground. */
    private static void ring(VertexConsumer stroke, PoseStack poseStack, float r, float y, float turn, int segments, int argb, float width) {
        for (int k = 0; k < segments; k += 2) {
            float a0 = k * Mth.TWO_PI / segments + turn, a1 = (k + 1.4f) * Mth.TWO_PI / segments + turn;
            GlyphRender.beam(stroke, poseStack.last(), new Vec3(Mth.cos(a0) * r, y, Mth.sin(a0) * r), new Vec3(Mth.cos(a1) * r, y, Mth.sin(a1) * r), width, argb);
        }
    }

    @Override
    public ResourceLocation getTextureLocation(ZeroGravityField field) {
        return CosmosRender.STARS;
    }
}
