package com.waylays.arcanes.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.waylays.arcanes.entity.bone.BoneLance;
import com.waylays.arcanes.spells.bone.BoneFx;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/**
 * A flying bone: the lance, a long bone sharpened to a barbed point, its marrow glowing in a streak behind it; or a
 * splinter, a small curved spike.
 */
public class BoneLanceRenderer extends EntityRenderer<BoneLance> {
    public BoneLanceRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(BoneLance lance, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        Vec3 motion = lance.getDeltaMovement();
        if (motion.lengthSqr() < 1.0E-6) {
            return;
        }
        Vec3 dir = motion.normalize();
        boolean big = lance.getKind() == BoneLance.Kind.LANCE;
        float length = big ? 2.4f : .8f;
        float mid = lance.getBbHeight() * .5f;
        Vector3f f = new Vector3f((float) dir.x, (float) dir.y, (float) dir.z);
        Vector3f tip = new Vector3f(f).mul(length * .5f).add(0, mid, 0);
        Vector3f tail = new Vector3f(f).mul(-length * .5f).add(0, mid, 0);
        VertexConsumer bone = BoneRender.buffer(buffer);
        PoseStack.Pose p = poseStack.last();
        int seed = lance.getId() * 31;
        if (big) {
            // The shaft: a long bone, then a sharpened point, two barbs near it
            Vector3f neck = new Vector3f(f).mul(length * .1f).add(0, mid, 0);
            BoneRender.bone(bone, p, tail, neck, .16f, packedLight, BoneRender.WHITE);
            BoneRender.spike(bone, p, neck, tip, .14f, new Vector3f(), packedLight, BoneRender.WHITE);
            Vector3f side = new Vector3f(f).cross(new Vector3f(0, 1, 0));
            if (side.lengthSquared() < 1.0E-4f) {
                side.set(1, 0, 0);
            }
            side.normalize();
            for (int s = -1; s <= 1; s += 2) {
                Vector3f base = new Vector3f(f).mul(length * .2f).add(0, mid, 0);
                Vector3f barb = new Vector3f(base).add(new Vector3f(side).mul(s * .32f)).add(new Vector3f(f).mul(-.3f));
                BoneRender.spike(bone, p, base, barb, .06f, new Vector3f(), packedLight, BoneRender.WHITE);
            }
        } else {
            float bow = (BoneFx.hash(seed) - .5f) * .2f;
            BoneRender.spike(bone, p, tail, tip, .1f, new Vector3f(0, bow, 0), packedLight, BoneRender.WHITE);
        }
        VertexConsumer stroke = buffer.getBuffer(RenderType.eyes(GlyphRender.STROKE));
        Vec3 a = new Vec3(tail.x, tail.y, tail.z);
        Vec3 back = a.subtract(dir.scale(big ? 2.2 : 1));
        GlyphRender.beam(stroke, p, a, back, big ? .32f : .14f, RuneRender.additive(BoneFx.MARROW, .7f));
        GlyphRender.beam(stroke, p, a, a.subtract(dir.scale(big ? 1.1 : .5)), big ? .12f : .06f, RuneRender.additive(BoneFx.MARROW_CORE, .9f));
        super.render(lance, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(BoneLance lance) {
        return BoneRender.BONE;
    }
}
