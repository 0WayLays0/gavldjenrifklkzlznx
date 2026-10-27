package com.waylays.arcanes.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.waylays.arcanes.entity.bone.SpineWhip;
import com.waylays.arcanes.spells.bone.BoneFx;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/** The Spine Whip: a chain of vertebrae, smaller towards the tip, a spike on the end, the marrow glowing between them. */
public class SpineWhipRenderer extends EntityRenderer<SpineWhip> {
    public SpineWhipRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(SpineWhip whip, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        float age = whip.tickCount + partialTicks;
        if (SpineWhip.extension(age) <= .02f) {
            return;
        }
        int n = SpineWhip.SEGMENTS;
        Vector3f[] p = new Vector3f[n + 1];
        p[0] = new Vector3f(0, (float) SpineWhip.HEIGHT, 0);
        for (int i = 0; i < n; i++) {
            Vec3 v = whip.point(i, age);
            p[i + 1] = new Vector3f((float) v.x, (float) v.y, (float) v.z);
        }
        VertexConsumer bone = BoneRender.buffer(buffer);
        PoseStack.Pose pose = poseStack.last();
        Vector3f up = new Vector3f(0, 1, 0);
        for (int i = 1; i < n; i++) {
            Vector3f along = new Vector3f(p[i + 1]).sub(p[i - 1]);
            if (along.lengthSquared() < 1.0E-6f) {
                continue;
            }
            along.normalize();
            float s = .48f - .26f * i / n;
            BoneRender.vertebra(bone, pose, p[i], along, up, s, packedLight, BoneRender.WHITE);
        }
        Vector3f tipDir = new Vector3f(p[n]).sub(p[n - 1]);
        if (tipDir.lengthSquared() > 1.0E-6f) {
            tipDir.normalize();
            BoneRender.spike(bone, pose, p[n - 1], new Vector3f(p[n]).add(new Vector3f(tipDir).mul(1.1f)), .17f, new Vector3f(0, .1f, 0), packedLight, BoneRender.WHITE);
        }
        // The marrow glowing along it (after the bone)
        VertexConsumer stroke = buffer.getBuffer(RenderType.eyes(GlyphRender.STROKE));
        for (int i = 0; i < n; i++) {
            GlyphRender.beam(stroke, pose, new Vec3(p[i].x, p[i].y, p[i].z), new Vec3(p[i + 1].x, p[i + 1].y, p[i + 1].z), .12f,
                    RuneRender.additive(BoneFx.MARROW, .55f));
        }
        super.render(whip, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    @Override
    public boolean shouldRender(SpineWhip whip, Frustum frustum, double x, double y, double z) {
        return frustum.isVisible(whip.getBoundingBox().inflate(whip.getReach() + 2));
    }

    @Override
    public ResourceLocation getTextureLocation(SpineWhip whip) {
        return BoneRender.BONE;
    }
}
