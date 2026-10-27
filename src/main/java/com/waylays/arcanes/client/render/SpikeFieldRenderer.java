package com.waylays.arcanes.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.waylays.arcanes.entity.bone.SpikeField;
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
 * Bone spikes bursting up: each a cluster of three horns of bone leaning out, bowed, the tallest in the middle; they
 * shoot up in a few ticks, stand, then sink back into the ground. A crimson glow flares on the ground where each bursts.
 */
public class SpikeFieldRenderer extends EntityRenderer<SpikeField> {
    public SpikeFieldRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(SpikeField field, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        float age = field.tickCount + partialTicks;
        VertexConsumer bone = BoneRender.buffer(buffer);
        PoseStack.Pose pose = poseStack.last();
        for (SpikeField.Spike s : field.spikes()) {
            float since = age - s.start();
            if (since < 0 || since > SpikeField.SPIKE_LIFE) {
                continue;
            }
            float up = Math.min(1, since / SpikeField.RISE);
            up = 1 - (1 - up) * (1 - up);
            float sink = Mth.clamp((since - (SpikeField.SPIKE_LIFE - 7)) / 7, 0, 1);
            float shown = up * (1 - sink * sink);
            for (int k = 0; k < 3; k++) {
                float h1 = BoneFx.hash(s.seed() + k * 5), h2 = BoneFx.hash(s.seed() + k * 5 + 1), h3 = BoneFx.hash(s.seed() + k * 5 + 2);
                float height = s.height() * (k == 0 ? 1 : .5f + .25f * h1);
                float ox = k == 0 ? 0 : (h2 - .5f) * .7f, oz = k == 0 ? 0 : (h3 - .5f) * .7f;
                float lx = s.leanX() + (k == 0 ? 0 : ox * .6f), lz = s.leanZ() + (k == 0 ? 0 : oz * .6f);
                float r = (k == 0 ? .2f : .13f) * (.85f + .3f * h1);
                // The spike as it stands, sunk into the ground by however much is not shown yet
                float drop = height * (1 - shown);
                Vector3f base = new Vector3f(s.x() + ox, -drop - .1f, s.z() + oz);
                Vector3f tip = new Vector3f(s.x() + ox + lx * height, height - drop, s.z() + oz + lz * height);
                Vector3f bend = new Vector3f(-lx * .25f * height, 0, -lz * .25f * height);
                BoneRender.spike(bone, pose, base, tip, r, bend, packedLight, BoneRender.WHITE);
            }
        }
        // The glow where each bursts, after the bone (a buffer is not written to again once another has been asked for)
        VertexConsumer glow = buffer.getBuffer(RenderType.eyes(GlyphRender.GLOW));
        for (SpikeField.Spike s : field.spikes()) {
            float since = age - s.start();
            if (since < 0 || since > 10) {
                continue;
            }
            float a = 1 - since / 10;
            poseStack.pushPose();
            poseStack.translate(s.x(), .05f, s.z());
            RenderHelper.groundQuad(glow, poseStack.last(), .9f + since * .06f, RuneRender.additive(BoneFx.MARROW, a * .9f), GlyphRender.FULL);
            poseStack.popPose();
        }
        super.render(field, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    @Override
    public boolean shouldRender(SpikeField field, Frustum frustum, double x, double y, double z) {
        float r = field.getSize() + 4;
        return frustum.isVisible(field.getBoundingBox().inflate(r, 4, r));
    }

    @Override
    public ResourceLocation getTextureLocation(SpikeField field) {
        return BoneRender.BONE;
    }
}
