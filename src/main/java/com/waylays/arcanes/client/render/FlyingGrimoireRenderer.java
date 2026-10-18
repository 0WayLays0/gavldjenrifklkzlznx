package com.waylays.arcanes.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.waylays.arcanes.ArcanesOublies;
import com.waylays.arcanes.entity.book.FlyingGrimoire;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * A flying grimoire: a small book flying spine first-backward, its covers working like jaws. Fangs line the edges of
 * its pages, two violet eyes glow on the upper cover and runes shine on the open pages. It chatters as it flies,
 * gapes and snaps shut when it bites, and opens wide when it spits a bolt. Turns red when hurt.
 */
public class FlyingGrimoireRenderer extends EntityRenderer<FlyingGrimoire> {
    private static final ResourceLocation COVER = ArcanesOublies.id("textures/entity/grimoire_cover.png");
    private static final ResourceLocation TEETH = ArcanesOublies.id("textures/entity/grimoire_teeth.png");
    private static final ResourceLocation EYES = ArcanesOublies.id("textures/entity/grimoire_eyes.png");

    /** Spine length (left to right), page length (back to front), page block and cover thickness. */
    private static final float L = .56f;
    private static final float W = .42f;
    private static final float PAGES = .045f;
    private static final float COVER_T = .025f;

    public FlyingGrimoireRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = .3f;
    }

    @Override
    public void render(FlyingGrimoire entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        float age = entity.tickCount + partialTicks;
        float jaw = 14 + 9 * Mth.sin(age * .45f);
        float bite = (entity.sinceBite() + partialTicks) / FlyingGrimoire.BITE_TICKS;
        if (bite >= 0 && bite < 1) {
            // Gapes, then snaps shut
            jaw = bite < .5f ? Mth.lerp(bite * 2, jaw, 70) : Mth.lerp((bite - .5f) * 2, 70, 0);
        }
        float spit = (entity.sinceSpit() + partialTicks) / FlyingGrimoire.SPIT_TICKS;
        if (spit >= 0 && spit < 1) {
            jaw = Math.max(jaw, 55 * Mth.sin(spit * Mth.PI));
        }
        int tint = entity.hurtTime > 0 ? 0xFFFF7070 : 0xFFFFFFFF;
        float yaw = Mth.rotLerp(partialTicks, entity.yBodyRotO, entity.yBodyRot);
        float pitch = Mth.lerp(partialTicks, entity.xRotO, entity.getXRot());

        poseStack.pushPose();
        poseStack.translate(0, .3 + Mth.sin(age * .15f) * .06f, 0);
        poseStack.mulPose(Axis.YP.rotationDegrees(-yaw));
        // Nose up a little, rolling as it flies
        poseStack.mulPose(Axis.XP.rotationDegrees(pitch * .6f - 10f));
        poseStack.mulPose(Axis.ZP.rotationDegrees(Mth.sin(age * .1f) * 8f));
        poseStack.translate(0, 0, -W * .45f);

        VertexConsumer cover = buffer.getBuffer(RenderType.entityCutoutNoCull(COVER));
        // Spine at the back
        TomeModel.box(cover, poseStack.last(), -L * .5f - .02f, -PAGES - COVER_T, -.03f, L * .5f + .02f, PAGES + COVER_T, 0, tint, packedLight);
        // Lower jaw moves a little, upper jaw most of the way
        drawJaw(buffer, poseStack, -1, jaw * .3f, tint, packedLight, age);
        drawJaw(buffer, poseStack, 1, jaw * .7f, tint, packedLight, age);
        poseStack.popPose();
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    /** One half of the book, hinged on the spine: {@code side} +1 is the upper jaw. */
    private void drawJaw(MultiBufferSource buffer, PoseStack poseStack, int side, float degrees, int tint, int light, float age) {
        poseStack.pushPose();
        poseStack.mulPose(Axis.XP.rotationDegrees(-side * degrees));
        float s = side;
        float p0 = Math.min(0, s * PAGES);
        float p1 = Math.max(0, s * PAGES);
        float c0 = Math.min(s * PAGES, s * (PAGES + COVER_T));
        float c1 = Math.max(s * PAGES, s * (PAGES + COVER_T));
        TomeModel.box(buffer.getBuffer(RenderType.entityCutoutNoCull(TomeModel.EDGE)), poseStack.last(), -L * .5f, p0, 0, L * .5f, p1, W, tint, light);
        TomeModel.box(buffer.getBuffer(RenderType.entityCutoutNoCull(COVER)), poseStack.last(), -L * .5f - .02f, c0, -.01f, L * .5f + .02f, c1, W + .025f, tint, light);
        // The open page with its runes, facing the other jaw
        poseStack.pushPose();
        poseStack.translate(0, -s * .002f, W * .5f);
        poseStack.mulPose(Axis.XP.rotationDegrees(side > 0 ? 90f : -90f));
        PageStormRenderer.drawPage(buffer, poseStack, L * .92f, W * .92f, light, .8f + .2f * Mth.sin(age * .3f));
        poseStack.popPose();
        // Fangs along the front edge, pointing at the other jaw (drawn pointing down)
        poseStack.pushPose();
        poseStack.translate(0, -s * .04f, W - .015f);
        if (side < 0) {
            poseStack.mulPose(Axis.ZP.rotationDegrees(180f));
        }
        RenderHelper.rect(buffer.getBuffer(RenderType.entityCutoutNoCull(TEETH)), poseStack.last(), L * .9f, .08f, tint, light, false);
        poseStack.popPose();
        // Glowing eyes on the upper cover
        if (side > 0) {
            poseStack.pushPose();
            poseStack.translate(0, c1 + .003f, W * .62f);
            float blink = (age % 70) < 3 ? .1f : 1f;
            int g = (int) (255 * blink);
            RenderHelper.groundQuad(buffer.getBuffer(RenderType.eyes(EYES)), poseStack.last(), L * .32f, 0xFF000000 | g << 16 | g << 8 | g, LightTexture.FULL_BRIGHT);
            poseStack.popPose();
        }
        poseStack.popPose();
    }

    @Override
    public ResourceLocation getTextureLocation(FlyingGrimoire entity) {
        return COVER;
    }
}
