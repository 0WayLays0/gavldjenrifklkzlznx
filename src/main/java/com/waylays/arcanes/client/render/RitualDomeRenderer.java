package com.waylays.arcanes.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.waylays.arcanes.entity.ritual.RitualDome;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/** The Dome of Protection: a lattice of light (meridians and rings) rising from the ground into a dome, shimmering. */
public class RitualDomeRenderer extends EntityRenderer<RitualDome> {
    private static final int COLOR = 0x7FD8FF;
    private static final int MERIDIANS = 16;
    private static final int RINGS = 6;
    private static final int SEGMENTS = 12;

    public RitualDomeRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(RitualDome dome, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        float strength = dome.fade(partialTicks, RitualDome.RISE, 60);
        if (strength <= 0) {
            return;
        }
        float age = dome.tickCount + partialTicks;
        float r = dome.getSize();
        // It grows up from the ground: the lattice is shown up to this height of the dome (0 to 1)
        float rise = Mth.clamp(age / RitualDome.RISE, 0, 1);
        VertexConsumer stroke = buffer.getBuffer(RenderType.eyes(GlyphRender.STROKE));
        PoseStack.Pose pose = poseStack.last();
        float spin = age * .15f * Mth.DEG_TO_RAD;
        for (int m = 0; m < MERIDIANS; m++) {
            float a = m * Mth.TWO_PI / MERIDIANS + spin;
            float shimmer = .3f + .15f * Mth.sin(age * .1f + m);
            for (int k = 0; k < SEGMENTS * rise; k++) {
                float t0 = k * Mth.HALF_PI / SEGMENTS;
                float t1 = (k + 1) * Mth.HALF_PI / SEGMENTS;
                GlyphRender.beam(stroke, pose, point(r, a, t0), point(r, a, t1), .14f, RuneRender.additive(COLOR, shimmer * strength));
            }
        }
        for (int ring = 1; ring <= RINGS; ring++) {
            float t = ring * Mth.HALF_PI / (RINGS + 1);
            if (t > rise * Mth.HALF_PI) {
                continue;
            }
            float shimmer = .25f + .15f * Mth.sin(age * .13f + ring * 1.7f);
            for (int k = 0; k < 48; k++) {
                float a0 = k * Mth.TWO_PI / 48;
                float a1 = (k + 1) * Mth.TWO_PI / 48;
                GlyphRender.beam(stroke, pose, point(r, a0, t), point(r, a1, t), .1f, RuneRender.additive(COLOR, shimmer * strength));
            }
        }
        poseStack.pushPose();
        poseStack.translate(0, .06, 0);
        poseStack.mulPose(Axis.YP.rotationDegrees(age * .5f));
        RenderHelper.groundQuad(buffer.getBuffer(RenderType.eyes(RuneRender.RING_GLOW)), poseStack.last(), r, RuneRender.additive(COLOR, .45f * strength), GlyphRender.FULL);
        poseStack.popPose();
        super.render(dome, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    /** A point of the dome: {@code a} around, {@code t} up from the ground (0) to the top (pi/2). */
    private static Vec3 point(float r, float a, float t) {
        return new Vec3(Mth.cos(a) * Mth.cos(t) * r, Mth.sin(t) * r, Mth.sin(a) * Mth.cos(t) * r);
    }

    @Override
    public boolean shouldRender(RitualDome dome, Frustum frustum, double x, double y, double z) {
        return true;
    }

    @Override
    public ResourceLocation getTextureLocation(RitualDome dome) {
        return GlyphRender.GLOW;
    }
}
