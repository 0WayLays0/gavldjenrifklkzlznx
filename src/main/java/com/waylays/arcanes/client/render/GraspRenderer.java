package com.waylays.arcanes.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.waylays.arcanes.entity.bone.Grasp;
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
 * The Skeletal Grasp: a giant bony hand and its forearm (two long bones) bursting out of the ground, fingers clawed;
 * it closes into a fist, rises, slams down and sinks. The ground glows crimson where it broke out and where it struck.
 */
public class GraspRenderer extends EntityRenderer<Grasp> {
    public GraspRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(Grasp grasp, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        float age = grasp.tickCount + partialTicks;
        float wrist = Grasp.wrist(age);
        float s = Grasp.SIZE;
        poseStack.pushPose();
        poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(-grasp.getYRot()));
        VertexConsumer bone = BoneRender.buffer(buffer);
        PoseStack.Pose p = poseStack.last();
        // The forearm: two long bones up out of the ground, leaning back a little
        for (int side = -1; side <= 1; side += 2) {
            BoneRender.bone(bone, p, new Vector3f(side * .32f * s, wrist - 2.8f * s, -.9f), new Vector3f(side * .2f * s, wrist - .3f * s, -.05f), .17f * s,
                    packedLight, BoneRender.WHITE);
        }
        poseStack.pushPose();
        poseStack.translate(0, wrist, 0);
        // Tilted back as it opens, upright as it closes
        float curl = Grasp.curl(age);
        poseStack.mulPose(com.mojang.math.Axis.XP.rotation(-.35f * (1 - curl)));
        BoneRender.hand(bone, poseStack.last(), s, curl, packedLight, BoneRender.WHITE);
        poseStack.popPose();
        VertexConsumer glow = buffer.getBuffer(RenderType.eyes(GlyphRender.GLOW));
        float broke = Mth.clamp(1 - age / 25, 0, 1);
        float struck = age >= Grasp.IMPACT ? Mth.clamp(1 - (age - Grasp.IMPACT) / 18, 0, 1) : 0;
        if (broke > 0 || struck > 0) {
            poseStack.pushPose();
            poseStack.translate(0, .05f, s * .3f);
            RenderHelper.groundQuad(glow, poseStack.last(), 1.6f + 1.5f * (1 - broke) + (struck > 0 ? Grasp.SLAM_RADIUS * (1.4f - struck) : 0),
                    RuneRender.additive(BoneFx.MARROW, Math.max(broke, struck)), GlyphRender.FULL);
            poseStack.popPose();
        }
        poseStack.popPose();
        super.render(grasp, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    @Override
    public boolean shouldRender(Grasp grasp, Frustum frustum, double x, double y, double z) {
        return frustum.isVisible(grasp.getBoundingBox().inflate(4, 7, 4));
    }

    @Override
    public ResourceLocation getTextureLocation(Grasp grasp) {
        return BoneRender.BONE;
    }
}
