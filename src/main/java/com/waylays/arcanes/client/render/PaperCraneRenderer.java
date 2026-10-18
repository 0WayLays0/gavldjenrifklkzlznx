package com.waylays.arcanes.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.waylays.arcanes.ArcanesOublies;
import com.waylays.arcanes.entity.book.PaperCrane;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * An origami crane folded from glowing paper: a diamond body, a long neck with a bent head, a raised tail and two
 * flapping wings, lit from within and haloed in violet. It beats its wings hard while it climbs, then folds them back
 * for the dive.
 */
public class PaperCraneRenderer extends EntityRenderer<PaperCrane> {
    private static final ResourceLocation PAPER = ArcanesOublies.id("textures/entity/crane_paper.png");
    private static final float SIZE = 1.5f;

    public PaperCraneRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(PaperCrane entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        float age = entity.tickCount + partialTicks;
        float unfold = Mth.clamp(age / 4f, 0f, 1f);
        float dive = Mth.clamp((age - PaperCrane.LAUNCH_TICKS) / 10f, 0f, 1f);
        // Wide strokes while climbing, quick shallow ones in the dive
        float flap = Mth.sin(age * Mth.lerp(dive, .9f, 1.6f)) * Mth.lerp(dive, 45f, 18f) + Mth.lerp(dive, 5f, -20f);

        poseStack.pushPose();
        poseStack.translate(0, .15, 0);
        poseStack.pushPose();
        poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
        poseStack.mulPose(Axis.YP.rotationDegrees(180f));
        float pulse = .5f + .15f * Mth.sin(age * .5f);
        BlastEffectRenderer.layer(buffer, BlastEffectRenderer.CORE, poseStack, 1.3f, 0, BlastEffectRenderer.rgb(pulse * .6f, pulse * .36f, pulse));
        poseStack.popPose();

        poseStack.mulPose(Axis.YP.rotationDegrees(Mth.lerp(partialTicks, entity.yRotO, entity.getYRot())));
        poseStack.mulPose(Axis.XP.rotationDegrees(-Mth.lerp(partialTicks, entity.xRotO, entity.getXRot())));
        poseStack.scale(SIZE * unfold, SIZE * unfold, SIZE * unfold);
        VertexConsumer c = buffer.getBuffer(RenderType.entityCutoutNoCull(PAPER));
        PoseStack.Pose pose = poseStack.last();
        int light = LightTexture.FULL_BRIGHT;
        int white = 0xFFFFFFFF;
        int shade = 0xFFD8C8F0;
        // Body: a vertical diamond
        quad(c, pose, 0, .09f, 0, 0, 0, .16f, 0, -.07f, 0, 0, 0, -.16f, white, light);
        // Neck rising to the bent head, tail rising behind
        tri(c, pose, 0, 0, .1f, 0, .03f, .15f, 0, .26f, .3f, shade, light);
        tri(c, pose, 0, .26f, .3f, 0, .23f, .27f, 0, .22f, .38f, white, light);
        tri(c, pose, 0, 0, -.1f, 0, .03f, -.15f, 0, .28f, -.32f, shade, light);
        // Wings, hinged on the ridge of the back
        for (int side = -1; side <= 1; side += 2) {
            float a = flap * Mth.DEG_TO_RAD;
            float tipX = side * .42f * Mth.cos(a);
            float tipY = .07f + .42f * Mth.sin(a);
            float midX = side * .22f * Mth.cos(a * .6f);
            float midY = .07f + .22f * Mth.sin(a * .6f);
            quad(c, pose, 0, .07f, .09f, midX, midY, .06f, tipX, tipY, -.08f, 0, .07f, -.09f, side < 0 ? white : shade, light);
        }
        poseStack.popPose();
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    private static void quad(VertexConsumer c, PoseStack.Pose pose, float ax, float ay, float az, float bx, float by, float bz,
                             float cx, float cy, float cz, float dx, float dy, float dz, int argb, int light) {
        RenderHelper.vertex(c, pose, ax, ay, az, 0, 0, argb, light, 0, 1, 0);
        RenderHelper.vertex(c, pose, bx, by, bz, 1, 0, argb, light, 0, 1, 0);
        RenderHelper.vertex(c, pose, cx, cy, cz, 1, 1, argb, light, 0, 1, 0);
        RenderHelper.vertex(c, pose, dx, dy, dz, 0, 1, argb, light, 0, 1, 0);
    }

    /** A triangle, drawn as a quad with its last corner doubled. */
    private static void tri(VertexConsumer c, PoseStack.Pose pose, float ax, float ay, float az, float bx, float by, float bz,
                            float cx, float cy, float cz, int argb, int light) {
        RenderHelper.vertex(c, pose, ax, ay, az, 0, 1, argb, light, 1, 0, 0);
        RenderHelper.vertex(c, pose, bx, by, bz, 1, 1, argb, light, 1, 0, 0);
        RenderHelper.vertex(c, pose, cx, cy, cz, .5f, 0, argb, light, 1, 0, 0);
        RenderHelper.vertex(c, pose, cx, cy, cz, .5f, 0, argb, light, 1, 0, 0);
    }

    @Override
    public ResourceLocation getTextureLocation(PaperCrane entity) {
        return PAPER;
    }
}
