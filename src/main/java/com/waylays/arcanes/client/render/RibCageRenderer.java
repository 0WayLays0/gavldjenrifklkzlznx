package com.waylays.arcanes.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.waylays.arcanes.entity.bone.RibCage;
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
 * The Rib Cage: pairs of great ribs bursting out of the ground and curving over until they lock above, a spine of
 * vertebrae along the top, the marrow glowing at the joins; at the end they snap tight, then sink and crumble away.
 */
public class RibCageRenderer extends EntityRenderer<RibCage> {
    private static final int POINTS = 9;

    public RibCageRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(RibCage cage, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        float age = cage.tickCount + partialTicks;
        float closed = cage.closed(age);
        float snapped = cage.snapped(age);
        float crumbled = cage.crumbled(age);
        float w = cage.getWidth() * (1 - .22f * snapped * snapped);
        float h = cage.getHeight();
        poseStack.pushPose();
        poseStack.translate(0, -crumbled * crumbled * (h + .6f), 0);
        poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(-cage.getYRot()));
        VertexConsumer bone = BoneRender.buffer(buffer);
        PoseStack.Pose p = poseStack.last();
        float length = Math.max(1.6f, w * 1.3f);
        for (int k = 0; k < RibCage.RIBS; k++) {
            float z = -length * .5f + length * k / (RibCage.RIBS - 1);
            // The middle ribs a little longer, like a chest
            float rib = 1 + .12f * Mth.sin(k * Mth.PI / (RibCage.RIBS - 1));
            for (int side = -1; side <= 1; side += 2) {
                Vector3f[] pts = new Vector3f[POINTS];
                for (int i = 0; i < POINTS; i++) {
                    float u = (float) i / (POINTS - 1);
                    // A quarter circle from the ground to the top, reached as the cage closes
                    float a = u * Mth.HALF_PI * closed;
                    float x = side * w * rib * Mth.cos(a);
                    float y = -.3f + (h + .3f) * Mth.sin(a);
                    // Bowing outwards in the middle of the curve
                    x += side * .25f * Mth.sin(u * Mth.PI) * closed;
                    pts[i] = new Vector3f(x, y, z + .1f * side * Mth.sin(u * Mth.PI));
                }
                BoneRender.curve(bone, p, pts, .17f, .1f, 7, packedLight, BoneRender.WHITE);
                BoneRender.ball(bone, p, pts[0], .22f, packedLight, BoneRender.WHITE);
            }
        }
        if (closed > .85f) {
            // The spine along the top where the ribs lock
            float s = (closed - .85f) / .15f;
            for (int k = 0; k < RibCage.RIBS + 2; k++) {
                float z = -length * .6f + length * 1.2f * k / (RibCage.RIBS + 1);
                BoneRender.vertebra(bone, p, new Vector3f(0, h + .1f, z), new Vector3f(0, 0, 1), new Vector3f(0, 1, 0), .5f * s, packedLight, BoneRender.WHITE);
            }
        }
        VertexConsumer glow = buffer.getBuffer(RenderType.eyes(GlyphRender.GLOW));
        float g = Math.max(closed * (1 - crumbled), snapped);
        RenderHelper.groundQuad(glow, poseStack.last(), w * 1.6f + .5f, RuneRender.additive(BoneFx.MARROW, .6f * g), GlyphRender.FULL);
        poseStack.popPose();
        super.render(cage, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    @Override
    public boolean shouldRender(RibCage cage, Frustum frustum, double x, double y, double z) {
        return frustum.isVisible(cage.getBoundingBox().inflate(cage.getWidth() + 2, cage.getHeight() + 1, cage.getWidth() + 2));
    }

    @Override
    public ResourceLocation getTextureLocation(RibCage cage) {
        return BoneRender.BONE;
    }
}
