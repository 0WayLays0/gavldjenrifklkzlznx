package com.waylays.arcanes.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.waylays.arcanes.entity.wood.SeedPod;
import com.waylays.arcanes.spells.wood.WoodFx;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

/**
 * A seed pod: a fat acorn-like husk of bark under a scaly cap and its stalk, split along its seams with the green life
 * glowing out of them, a few thorns already pushing through; it tumbles as it flies.
 */
public class SeedPodRenderer extends EntityRenderer<SeedPod> {
    public SeedPodRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(SeedPod pod, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        float age = pod.tickCount + partialTicks;
        poseStack.pushPose();
        poseStack.translate(0, pod.getBbHeight() * .5f, 0);
        poseStack.mulPose(Axis.YP.rotation(age * .3f + pod.getId()));
        poseStack.mulPose(Axis.XP.rotation(age * .45f));
        float pulse = 1 + .06f * Mth.sin(age * .8f);
        poseStack.scale(pulse, pulse, pulse);
        VertexConsumer bark = WoodRender.bark(buffer);
        PoseStack.Pose p = poseStack.last();
        // The husk and its cap
        BoneRender.blob(bark, p, new Vector3f(0, -.05f, 0), new Vector3f(.26f, 0, 0), new Vector3f(0, .32f, 0), new Vector3f(0, 0, .26f), 10, 7, packedLight, 0xFFD9A066);
        BoneRender.blob(bark, p, new Vector3f(0, .18f, 0), new Vector3f(.3f, 0, 0), new Vector3f(0, .14f, 0), new Vector3f(0, 0, .3f), 10, 5, packedLight, 0xFF8A6440);
        BoneRender.curve(bark, p, new Vector3f[]{new Vector3f(0, .3f, 0), new Vector3f(.04f, .42f, 0), new Vector3f(.1f, .5f, .02f)}, .04f, .02f, 4, packedLight, 0xFF8A6440);
        for (int k = 0; k < 5; k++) {
            float a = k * Mth.TWO_PI / 5 + .3f;
            Vector3f base = new Vector3f(Mth.cos(a) * .24f, -.12f + .05f * (k % 2), Mth.sin(a) * .24f);
            Vector3f tip = new Vector3f(base).mul(1.6f).add(0, -.05f, 0);
            WoodRender.thorn(bark, p, base, tip, .04f, packedLight, WoodRender.THORN);
        }
        // The life glowing out of its seams
        VertexConsumer glow = buffer.getBuffer(RenderType.eyes(GlyphRender.GLOW));
        float g = .55f + .3f * Mth.sin(age * .8f);
        for (int k = 0; k < 4; k++) {
            poseStack.pushPose();
            poseStack.mulPose(Axis.YP.rotation(k * Mth.HALF_PI));
            poseStack.translate(0, -.05f, .262f);
            RenderHelper.rect(glow, poseStack.last(), .09f, .5f, RuneRender.additive(WoodFx.LIFE, g), GlyphRender.FULL, true);
            poseStack.popPose();
        }
        poseStack.popPose();
        // A halo round it, facing the camera
        poseStack.pushPose();
        poseStack.translate(0, pod.getBbHeight() * .5f, 0);
        poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
        RenderHelper.billboard(glow, poseStack.last(), .55f, RuneRender.additive(WoodFx.LIFE, .35f * g), GlyphRender.FULL);
        poseStack.popPose();
        super.render(pod, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(SeedPod pod) {
        return WoodRender.BARK;
    }
}
