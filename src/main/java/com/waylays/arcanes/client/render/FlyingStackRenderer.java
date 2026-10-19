package com.waylays.arcanes.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.waylays.arcanes.ArcanesOublies;
import com.waylays.arcanes.entity.book.FlyingStack;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * The flying stack: a messy pile of parchment (sheets sticking out at odd angles, loose ones fluttering on top) above a
 * slowly turning violet rune circle. It leans into its turns and tips its nose up or down as it climbs or dives.
 */
public class FlyingStackRenderer extends EntityRenderer<FlyingStack> {
    private static final ResourceLocation EDGE = ArcanesOublies.id("textures/entity/tome_edge.png");
    private static final ResourceLocation CIRCLE = ArcanesOublies.id("textures/entity/chapter_rune.png");
    private static final float WIDTH = 1.3f;
    private static final float DEPTH = 1f;
    private static final float TOP = .48f;
    private static final float BOTTOM = .06f;

    public FlyingStackRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = .6f;
    }

    @Override
    public void render(FlyingStack entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        float age = entity.tickCount + partialTicks;
        Vec3 motion = entity.position().subtract(entity.xo, entity.yo, entity.zo);
        float yawRad = entityYaw * Mth.DEG_TO_RAD;
        double sideways = motion.x * Mth.cos(yawRad) + motion.z * Mth.sin(yawRad);
        float pitch = Mth.clamp((float) -motion.y * 45f, -25f, 25f);
        float roll = Mth.clamp((float) -sideways * 50f, -22f, 22f);
        float flutter = Mth.clamp((float) motion.length() * 3f, .15f, 1f);

        // The rune circle it floats on, turning slowly, flat on the ground under it
        poseStack.pushPose();
        poseStack.translate(0, -.02, 0);
        poseStack.mulPose(Axis.YP.rotation(age * .03f));
        int glow = (int) ((.55f + .25f * Mth.sin(age * .15f)) * 255);
        RenderHelper.groundQuad(buffer.getBuffer(RenderType.eyes(CIRCLE)), poseStack.last(), .95f, 0xFF000000 | (glow * 154 / 255 << 16) | (glow * 92 / 255 << 8) | glow, LightTexture.FULL_BRIGHT);
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(-entityYaw));
        poseStack.translate(0, .25, 0);
        poseStack.mulPose(Axis.XP.rotationDegrees(pitch));
        poseStack.mulPose(Axis.ZP.rotationDegrees(roll));
        poseStack.translate(0, -.25, 0);

        // The pile itself: striped page edges round a block of paper
        box(buffer.getBuffer(RenderType.entityCutoutNoCull(EDGE)), poseStack.last(), packedLight);
        // Sheets sticking out of the pile at odd angles
        for (int i = 0; i < 6; i++) {
            poseStack.pushPose();
            poseStack.translate(((i * 7) % 5 - 2) * .02f, BOTTOM + .03f + i * .07f, ((i * 3) % 5 - 2) * .025f);
            poseStack.mulPose(Axis.YP.rotationDegrees(((i * 37) % 13 - 6) * 1.6f));
            sheet(buffer, poseStack, WIDTH + .08f, DEPTH + .06f, packedLight);
            poseStack.popPose();
        }
        // Loose sheets on top, their leading edge lifting in the wind
        for (int i = 0; i < 3; i++) {
            poseStack.pushPose();
            poseStack.translate((i - 1) * .06f, TOP + .012f + i * .012f, -DEPTH * .5f + .02f);
            poseStack.mulPose(Axis.YP.rotationDegrees((i - 1) * 9f));
            float lift = flutter * (10f + 8f * Mth.sin(age * (.7f + i * .2f) + i * 2f));
            poseStack.mulPose(Axis.XP.rotationDegrees(-lift));
            poseStack.translate(0, 0, DEPTH * .5f);
            sheet(buffer, poseStack, WIDTH - .12f, DEPTH - .1f, packedLight);
            poseStack.popPose();
        }
        poseStack.popPose();
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    /** A horizontal sheet of parchment centred on the origin, its runes faintly lit. */
    private static void sheet(MultiBufferSource buffer, PoseStack poseStack, float width, float depth, int light) {
        poseStack.pushPose();
        poseStack.mulPose(Axis.XP.rotationDegrees(-90));
        poseStack.mulPose(Axis.ZP.rotationDegrees(90));
        PageStormRenderer.drawPage(buffer, poseStack, depth, width, light, .35f);
        poseStack.popPose();
    }

    /** The four sides and the bottom of the pile. */
    private static void box(VertexConsumer consumer, PoseStack.Pose pose, int light) {
        float x = WIDTH * .5f - .02f;
        float z = DEPTH * .5f - .02f;
        float v = 1;
        int c = 0xFFFFFFFF;
        // Front and back
        RenderHelper.vertex(consumer, pose, -x, BOTTOM, z, 0, v, c, light, 0, 0, 1);
        RenderHelper.vertex(consumer, pose, x, BOTTOM, z, 1, v, c, light, 0, 0, 1);
        RenderHelper.vertex(consumer, pose, x, TOP, z, 1, 0, c, light, 0, 0, 1);
        RenderHelper.vertex(consumer, pose, -x, TOP, z, 0, 0, c, light, 0, 0, 1);
        RenderHelper.vertex(consumer, pose, x, BOTTOM, -z, 0, v, c, light, 0, 0, -1);
        RenderHelper.vertex(consumer, pose, -x, BOTTOM, -z, 1, v, c, light, 0, 0, -1);
        RenderHelper.vertex(consumer, pose, -x, TOP, -z, 1, 0, c, light, 0, 0, -1);
        RenderHelper.vertex(consumer, pose, x, TOP, -z, 0, 0, c, light, 0, 0, -1);
        // Sides
        RenderHelper.vertex(consumer, pose, x, BOTTOM, z, 0, v, c, light, 1, 0, 0);
        RenderHelper.vertex(consumer, pose, x, BOTTOM, -z, 1, v, c, light, 1, 0, 0);
        RenderHelper.vertex(consumer, pose, x, TOP, -z, 1, 0, c, light, 1, 0, 0);
        RenderHelper.vertex(consumer, pose, x, TOP, z, 0, 0, c, light, 1, 0, 0);
        RenderHelper.vertex(consumer, pose, -x, BOTTOM, -z, 0, v, c, light, -1, 0, 0);
        RenderHelper.vertex(consumer, pose, -x, BOTTOM, z, 1, v, c, light, -1, 0, 0);
        RenderHelper.vertex(consumer, pose, -x, TOP, z, 1, 0, c, light, -1, 0, 0);
        RenderHelper.vertex(consumer, pose, -x, TOP, -z, 0, 0, c, light, -1, 0, 0);
        // Bottom
        RenderHelper.vertex(consumer, pose, -x, BOTTOM, -z, 0, 0, c, light, 0, -1, 0);
        RenderHelper.vertex(consumer, pose, x, BOTTOM, -z, 1, 0, c, light, 0, -1, 0);
        RenderHelper.vertex(consumer, pose, x, BOTTOM, z, 1, 1, c, light, 0, -1, 0);
        RenderHelper.vertex(consumer, pose, -x, BOTTOM, z, 0, 1, c, light, 0, -1, 0);
    }

    @Override
    public ResourceLocation getTextureLocation(FlyingStack entity) {
        return EDGE;
    }
}
