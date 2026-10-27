package com.waylays.arcanes.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.waylays.arcanes.entity.bone.BoneWard;
import com.waylays.arcanes.spells.bone.BoneFx;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

/**
 * The Bone Ward: shoulder blades of bone circling the caster, each turned outwards with a ridge and a spike along it,
 * bobbing as they go round, a crimson glow behind each.
 */
public class BoneWardRenderer extends EntityRenderer<BoneWard> {
    public BoneWardRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(BoneWard ward, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        float age = ward.tickCount + partialTicks;
        int plates = ward.getPlates();
        float grow = Mth.clamp(age / 8, 0, 1);
        float fade = Mth.clamp((ward.getDuration() - age) / 10, 0, 1);
        float scale = grow * fade;
        if (plates <= 0 || scale <= 0) {
            return;
        }
        VertexConsumer bone = BoneRender.buffer(buffer);
        float[][] where = new float[plates][];
        for (int i = 0; i < plates; i++) {
            float a = age * .07f + i * Mth.TWO_PI / plates;
            float y = 1.05f + .35f * Mth.sin(age * .1f + i * 1.7f);
            float r = BoneWard.RADIUS * (.6f + .4f * grow);
            where[i] = new float[]{Mth.cos(a) * r, y, Mth.sin(a) * r, a};
            poseStack.pushPose();
            poseStack.translate(where[i][0], y, where[i][2]);
            // Facing out, tilted a little back
            poseStack.mulPose(com.mojang.math.Axis.YP.rotation(-a + Mth.HALF_PI));
            poseStack.mulPose(com.mojang.math.Axis.XP.rotation(-.2f));
            poseStack.scale(scale, scale, scale);
            PoseStack.Pose p = poseStack.last();
            // A shoulder blade: a thin triangle of bone narrowing to a point below, a ridge across its top and a spike
            BoneRender.blob(bone, p, new Vector3f(0, .14f, 0), new Vector3f(.34f, 0, 0), new Vector3f(0, .3f, 0), new Vector3f(0, 0, .035f), 10, 6, packedLight, BoneRender.WHITE);
            BoneRender.blob(bone, p, new Vector3f(-.03f, -.22f, 0), new Vector3f(.19f, 0, 0), new Vector3f(0, .3f, 0), new Vector3f(0, 0, .03f), 8, 5, packedLight, BoneRender.WHITE);
            BoneRender.spike(bone, p, new Vector3f(-.05f, -.3f, 0), new Vector3f(-.08f, -.68f, .02f), .09f, new Vector3f(), packedLight, BoneRender.WHITE);
            BoneRender.bone(bone, p, new Vector3f(-.34f, .3f, .04f), new Vector3f(.36f, .24f, .04f), .055f, packedLight, BoneRender.WHITE);
            BoneRender.spike(bone, p, new Vector3f(.1f, .28f, .06f), new Vector3f(.2f, .42f, .6f), .065f, new Vector3f(0, .06f, 0), packedLight, BoneRender.WHITE);
            poseStack.popPose();
        }
        VertexConsumer glow = buffer.getBuffer(RenderType.eyes(GlyphRender.GLOW));
        for (int i = 0; i < plates; i++) {
            poseStack.pushPose();
            poseStack.translate(where[i][0] * .85f, where[i][1], where[i][2] * .85f);
            poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
            RenderHelper.billboard(glow, poseStack.last(), .42f * scale, RuneRender.additive(BoneFx.MARROW, .3f * scale), GlyphRender.FULL);
            poseStack.popPose();
        }
        super.render(ward, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(BoneWard ward) {
        return BoneRender.BONE;
    }
}
