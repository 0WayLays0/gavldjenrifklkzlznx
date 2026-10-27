package com.waylays.arcanes.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.waylays.arcanes.entity.bone.GiantSpike;
import com.waylays.arcanes.spells.bone.BoneFx;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

/**
 * The Colossal Spike: the ground glowing crimson where it will burst, then a spike of bone as tall as a tree, leaning a
 * little and bowed like a horn, three great thorns off its foot and a ring of lesser spikes round it.
 */
public class GiantSpikeRenderer extends EntityRenderer<GiantSpike> {
    public GiantSpikeRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(GiantSpike spike, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        float age = spike.tickCount + partialTicks;
        float risen = spike.risen(age);
        float h = spike.getHeight();
        int seed = spike.getId() * 131;
        if (risen > 0) {
            VertexConsumer bone = BoneRender.buffer(buffer);
            PoseStack.Pose p = poseStack.last();
            float drop = h * (1 - risen);
            float lean = .12f * h;
            float a = BoneFx.hash(seed) * Mth.TWO_PI;
            float lx = Mth.cos(a) * lean, lz = Mth.sin(a) * lean;
            BoneRender.spike(bone, p, new Vector3f(0, -1.2f - drop, 0), new Vector3f(lx, h - drop, lz), 1.7f,
                    new Vector3f(-lx * .3f, 0, -lz * .3f), packedLight, BoneRender.WHITE);
            // Thorns off its foot
            for (int k = 0; k < 3; k++) {
                float b = a + Mth.PI + (k - 1) * 1.6f;
                float y = (.8f + k * .9f) * risen - drop * .3f;
                Vector3f base = new Vector3f(Mth.cos(b) * .9f, y, Mth.sin(b) * .9f);
                BoneRender.spike(bone, p, base, new Vector3f(Mth.cos(b) * 2.6f, y + 1.6f, Mth.sin(b) * 2.6f), .45f, new Vector3f(0, -.3f, 0),
                        packedLight, BoneRender.WHITE);
            }
            // The ring of lesser spikes
            for (int k = 0; k < 9; k++) {
                float b = a + k * Mth.TWO_PI / 9 + BoneFx.hash(seed + k) * .3f;
                float r = 2.8f + BoneFx.hash(seed + k + 9) * 1.2f;
                float sh = (1.6f + 1.6f * BoneFx.hash(seed + k + 19)) * Mth.clamp(risen * 1.4f - .2f, 0, 1);
                Vector3f base = new Vector3f(Mth.cos(b) * r, -.4f, Mth.sin(b) * r);
                BoneRender.spike(bone, p, base, new Vector3f(Mth.cos(b) * (r + .9f), sh - .4f, Mth.sin(b) * (r + .9f)), .32f, new Vector3f(),
                        packedLight, BoneRender.WHITE);
            }
        }
        // The glow on the cracking ground, flaring as it bursts
        float glowing = age < GiantSpike.TELEGRAPH ? age / GiantSpike.TELEGRAPH : Mth.clamp(1 - (age - GiantSpike.TELEGRAPH) / 20, 0, 1);
        if (glowing > 0) {
            VertexConsumer glow = buffer.getBuffer(RenderType.eyes(GlyphRender.GLOW));
            poseStack.pushPose();
            poseStack.translate(0, .05f, 0);
            RenderHelper.groundQuad(glow, poseStack.last(), GiantSpike.RING * (.6f + .5f * glowing), RuneRender.additive(BoneFx.MARROW, glowing), GlyphRender.FULL);
            RenderHelper.groundQuad(glow, poseStack.last(), GiantSpike.CORE * 1.3f, RuneRender.additive(BoneFx.MARROW_CORE, glowing * .8f), GlyphRender.FULL);
            poseStack.popPose();
        }
        super.render(spike, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    @Override
    public boolean shouldRender(GiantSpike spike, Frustum frustum, double x, double y, double z) {
        return frustum.isVisible(spike.getBoundingBox().inflate(6, spike.getHeight() + 2, 6));
    }

    @Override
    public ResourceLocation getTextureLocation(GiantSpike spike) {
        return BoneRender.BONE;
    }
}
