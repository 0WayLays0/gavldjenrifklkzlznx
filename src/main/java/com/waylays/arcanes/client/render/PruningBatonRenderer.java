package com.waylays.arcanes.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.waylays.arcanes.entity.cosmos.PruningBaton;
import com.waylays.arcanes.spells.cosmos.Cosmos;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/**
 * The pruning baton swung in front of its caster from right to left: a rod of dark gunmetal with a black grip and
 * bands of brass, its tip a capsule of orange light, and the arc the tip cuts through the air glowing behind it.
 */
public class PruningBatonRenderer extends EntityRenderer<PruningBaton> {
    /** The rod, one block long along +Y, eight-sided. */
    private static final float[] ROD = new Solid.Builder()
            .band(Solid.ring(8, 0, .045f, .045f, .5f), Solid.ring(8, 1, .04f, .04f, .5f))
            .fan(Solid.ring(8, 1, .04f, .04f, .5f), new float[]{0, 1.01f, 0})
            .build();
    private static final float[] BAND = new Solid.Builder()
            .band(Solid.ring(8, 0, .06f, .06f, .5f), Solid.ring(8, .05f, .06f, .06f, .5f))
            .build();
    private static final float LENGTH = 1.15f;
    private static final float SHOULDER = 1.3f;

    public PruningBatonRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    /**
     * Where the tip is at swing {@code s} (0 to 1) of blow {@code stage}, in the caster's frame (+Z forward, +X their
     * left): the first sweeps from their right to their left, the second back, the third comes down overhead.
     */
    private static Vec3 tip(float s, float reach, int stage) {
        if (stage >= 3) {
            float e = Mth.lerp(s, 110, -38) * Mth.DEG_TO_RAD;
            return new Vec3(-.15, SHOULDER + .15 + Mth.sin(e) * reach, Mth.cos(e) * reach);
        }
        float a = (stage == 2 ? Mth.lerp(s, 75, -75) : Mth.lerp(s, -75, 75)) * Mth.DEG_TO_RAD;
        float drop = Mth.lerp(s, .15f, -.25f);
        return new Vec3(Mth.sin(a) * reach, SHOULDER + drop, Mth.cos(a) * reach);
    }

    @Override
    public void render(PruningBaton baton, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        Entity caster = baton.getCaster();
        float age = baton.tickCount + partialTicks;
        float s = PruningBaton.swing(age);
        int stage = baton.getStage();
        float fade = 1 - Mth.clamp((age - PruningBaton.SWING_TICKS) / (PruningBaton.LIFE - PruningBaton.SWING_TICKS), 0, 1);
        poseStack.pushPose();
        if (caster != null) {
            Vec3 off = caster.getPosition(partialTicks).subtract(baton.getPosition(partialTicks));
            poseStack.translate(off.x, off.y, off.z);
            poseStack.mulPose(Axis.YP.rotationDegrees(-Mth.rotLerp(partialTicks, caster.yRotO, caster.getYRot())));
        } else {
            poseStack.mulPose(Axis.YP.rotationDegrees(-baton.getYRot()));
        }
        // The arc cut by the tip
        VertexConsumer stroke = buffer.getBuffer(RenderType.eyes(GlyphRender.STROKE));
        int steps = 12;
        for (int k = 0; k < steps; k++) {
            float s0 = s * k / steps, s1 = s * (k + 1) / steps;
            float w = (float) (k + 1) / steps;
            float wide = stage >= 3 ? 1.6f : 1;
            GlyphRender.beam(stroke, poseStack.last(), tip(s0, LENGTH + .3f, stage), tip(s1, LENGTH + .3f, stage), .32f * w * wide, RuneRender.additive(Cosmos.ORANGE, .55f * w * fade));
            GlyphRender.beam(stroke, poseStack.last(), tip(s0, LENGTH + .3f, stage), tip(s1, LENGTH + .3f, stage), .1f * w, RuneRender.additive(Cosmos.CREAM, .8f * w * fade));
        }
        // The baton itself, from the shoulder out toward the tip
        Vec3 shoulder = stage >= 3 ? new Vec3(-.2, SHOULDER + .1, .05) : new Vec3(-.3, SHOULDER - .1, .1);
        Vec3 dir = tip(s, LENGTH + .3f, stage).subtract(shoulder).normalize();
        poseStack.pushPose();
        poseStack.translate(shoulder.x + dir.x * .25, shoulder.y + dir.y * .25, shoulder.z + dir.z * .25);
        poseStack.mulPose(new org.joml.Quaternionf().rotationTo(0, 1, 0, (float) dir.x, (float) dir.y, (float) dir.z));
        float scale = fade;
        poseStack.scale(scale, scale, scale);
        VertexConsumer metal = GoldRender.metal(buffer);
        poseStack.pushPose();
        poseStack.scale(1, LENGTH, 1);
        Solid.draw(metal, poseStack.last(), ROD, 0x3C3F4A, 255, age, .4f, GlyphRender.FULL);
        poseStack.popPose();
        // The grip and two brass bands
        poseStack.pushPose();
        poseStack.scale(1.15f, .28f / .05f, 1.15f);
        Solid.draw(metal, poseStack.last(), BAND, 0x1C1A1E, 255, age, 0, GlyphRender.FULL);
        poseStack.popPose();
        for (float y : new float[]{.3f, .9f}) {
            poseStack.pushPose();
            poseStack.translate(0, y, 0);
            Solid.draw(metal, poseStack.last(), BAND, 0xC8902E, 255, age, .8f, GlyphRender.FULL);
            poseStack.popPose();
        }
        // The glowing tip
        poseStack.translate(0, LENGTH, 0);
        float pulse = 1 + .15f * Mth.sin(age * 1.7f);
        GemRender.glow(poseStack, buffer, Vec3.ZERO, Cosmos.ORANGE, .55f * pulse, .9f * fade);
        GemRender.glow(poseStack, buffer, Vec3.ZERO, Cosmos.CREAM, .2f * pulse, fade);
        poseStack.popPose();
        poseStack.popPose();
        super.render(baton, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(PruningBaton baton) {
        return GoldRender.METAL;
    }
}
