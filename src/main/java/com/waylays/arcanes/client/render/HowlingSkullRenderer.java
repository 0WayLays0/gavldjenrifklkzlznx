package com.waylays.arcanes.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.waylays.arcanes.entity.bone.HowlingSkull;
import com.waylays.arcanes.spells.bone.BoneFx;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/** The Howling Skull: a great skull flying jaws first, snapping, wreathed in crimson, a streak of it behind. */
public class HowlingSkullRenderer extends EntityRenderer<HowlingSkull> {
    private static final float SIZE = 1.15f;

    public HowlingSkullRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(HowlingSkull skull, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        float age = skull.tickCount + partialTicks;
        Vec3 motion = skull.getDeltaMovement();
        Vector3f dir = motion.lengthSqr() < 1.0E-6 ? new Vector3f(0, 0, 1) : new Vector3f((float) motion.x, (float) motion.y, (float) motion.z).normalize();
        float mid = skull.getBbHeight() * .5f;
        // The streak of crimson behind (first: the skull asks for its own buffers after)
        VertexConsumer stroke = buffer.getBuffer(RenderType.eyes(GlyphRender.STROKE));
        Vec3 c = new Vec3(0, mid, 0);
        Vec3 back = new Vec3(-dir.x, -dir.y, -dir.z);
        GlyphRender.beam(stroke, poseStack.last(), c, c.add(back.scale(3.2)), 1.1f, RuneRender.additive(BoneFx.MARROW, .6f));
        GlyphRender.beam(stroke, poseStack.last(), c, c.add(back.scale(1.8)), .5f, RuneRender.additive(BoneFx.MARROW_CORE, .8f));
        poseStack.pushPose();
        poseStack.translate(0, mid, 0);
        poseStack.mulPose(new Quaternionf().rotationTo(new Vector3f(0, 0, 1), dir));
        poseStack.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(Mth.sin(age * .2f) * 12));
        poseStack.translate(0, -SIZE * .55f, 0);
        BoneRender.skull(poseStack, buffer, SIZE, .2f + .55f * Math.abs(Mth.sin(age * .45f)), packedLight, 1);
        poseStack.popPose();
        // A crimson wreath round it, facing the camera
        VertexConsumer glow = buffer.getBuffer(RenderType.eyes(GlyphRender.GLOW));
        poseStack.pushPose();
        poseStack.translate(0, mid, 0);
        poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
        RenderHelper.billboard(glow, poseStack.last(), 1.3f + .1f * Mth.sin(age * .7f), RuneRender.additive(BoneFx.MARROW, .55f), GlyphRender.FULL);
        poseStack.popPose();
        super.render(skull, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(HowlingSkull skull) {
        return BoneRender.BONE;
    }
}
