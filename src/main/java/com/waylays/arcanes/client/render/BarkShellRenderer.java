package com.waylays.arcanes.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.waylays.arcanes.entity.wood.BarkShell;
import com.waylays.arcanes.spells.wood.WoodFx;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.joml.Vector3f;

/**
 * Barkskin: curved plates of bark round the legs, the body and the shoulders, growing up from the feet, overlapping a
 * little, thorns sticking out of them, tufts of leaves in the gaps, the green life glowing through the cracks between
 * them. Drawn where the wearer is drawn (not a tick behind), turned with their body.
 */
public class BarkShellRenderer extends EntityRenderer<BarkShell> {
    /** Bands of plates: bottom, top, radius, plates round. */
    private static final float[][] BANDS = {
            {0, .5f, .42f, 6},
            {.42f, .95f, .44f, 7},
            {.88f, 1.42f, .5f, 7},
    };

    public BarkShellRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(BarkShell shell, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        float age = shell.tickCount + partialTicks;
        float grown = shell.grown(age);
        if (grown <= .01f) {
            return;
        }
        Entity wearer = shell.wearer();
        // In first person the bark would close round the camera: only the parts below the waist then
        boolean self = wearer == Minecraft.getInstance().getCameraEntity() && Minecraft.getInstance().options.getCameraType().isFirstPerson();
        poseStack.pushPose();
        float bodyYaw = shell.getYRot();
        if (wearer != null) {
            poseStack.translate(Mth.lerp(partialTicks, wearer.xo, wearer.getX()) - Mth.lerp(partialTicks, shell.xo, shell.getX()),
                    Mth.lerp(partialTicks, wearer.yo, wearer.getY()) - Mth.lerp(partialTicks, shell.yo, shell.getY()),
                    Mth.lerp(partialTicks, wearer.zo, wearer.getZ()) - Mth.lerp(partialTicks, shell.zo, shell.getZ()));
            bodyYaw = wearer instanceof LivingEntity living ? Mth.rotLerp(partialTicks, living.yBodyRotO, living.yBodyRot) : wearer.getYRot();
        }
        poseStack.mulPose(Axis.YP.rotationDegrees(-bodyYaw));
        float scale = wearer == null ? 1 : Mth.clamp(wearer.getBbHeight() / 1.8f, .5f, 3);
        float width = wearer == null ? 1 : Mth.clamp(wearer.getBbWidth() / .6f, .6f, 3);
        poseStack.scale(width, scale, width);
        float top = grown * 1.45f;
        int seed = shell.getId() * 31;
        VertexConsumer bark = WoodRender.bark(buffer);
        PoseStack.Pose p = poseStack.last();
        int bands = self ? 1 : BANDS.length;
        for (int b = 0; b < bands; b++) {
            float[] band = BANDS[b];
            float y0 = band[0], y1 = Math.min(band[1], top);
            if (y1 <= y0 + .02f) {
                continue;
            }
            int plates = (int) band[3];
            for (int k = 0; k < plates; k++) {
                float a = (k + .5f * b) * Mth.TWO_PI / plates + (WoodFx.hash(seed + b * 17 + k) - .5f) * .3f;
                float half = Mth.PI / plates * (.92f + .2f * WoodFx.hash(seed + b * 13 + k));
                float r = band[2] + .03f * WoodFx.hash(seed + k * 7 + b);
                // The shoulders: a gap where the arms are (to the sides) for the top band
                if (b == 2 && Math.abs(Mth.cos(a)) > .85f) {
                    r += .08f;
                }
                WoodRender.plate(bark, p, a - half, a + half, r, y0, y1, .07f, packedLight, BoneRender.WHITE, seed + b * 101 + k * 11);
            }
        }
        // Thorns off the plates
        for (int b = 0; b < bands; b++) {
            float[] band = BANDS[b];
            for (int k = 0; k < 4; k++) {
                float y = Mth.lerp(.3f + .4f * WoodFx.hash(seed + k + b * 9), band[0], band[1]);
                if (y > top) {
                    continue;
                }
                float a = WoodFx.hash(seed + k * 5 + b * 3) * Mth.TWO_PI;
                Vector3f base = new Vector3f(Mth.cos(a) * band[2], y, Mth.sin(a) * band[2]);
                Vector3f tip = new Vector3f(Mth.cos(a) * (band[2] + .22f), y + .1f, Mth.sin(a) * (band[2] + .22f));
                WoodRender.thorn(bark, p, base, tip, .045f, packedLight, WoodRender.THORN);
            }
        }
        // Tufts of leaves in the gaps
        VertexConsumer leaf = WoodRender.leafBuffer(buffer);
        for (int k = 0; k < 9; k++) {
            float y = (.15f + 1.2f * WoodFx.hash(seed + k * 3 + 1)) * (self ? .4f : 1);
            if (y > top) {
                continue;
            }
            float a = WoodFx.hash(seed + k * 3 + 2) * Mth.TWO_PI;
            poseStack.pushPose();
            poseStack.translate(Mth.cos(a) * .5f, y, Mth.sin(a) * .5f);
            poseStack.mulPose(Axis.YP.rotation(-a + Mth.HALF_PI));
            poseStack.mulPose(Axis.XP.rotation(-.6f + .1f * Mth.sin(age * .12f + k)));
            WoodRender.leaf(leaf, poseStack.last(), .26f, WoodRender.leafColour(seed + k), packedLight);
            poseStack.popPose();
        }
        // The green life glowing in the cracks between the plates
        VertexConsumer glow = buffer.getBuffer(RenderType.eyes(GlyphRender.GLOW));
        float pulse = .5f + .25f * Mth.sin(age * .15f);
        for (int b = 0; b < bands; b++) {
            float[] band = BANDS[b];
            float y1 = Math.min(band[1], top);
            if (y1 <= band[0] + .02f) {
                continue;
            }
            int plates = (int) band[3];
            for (int k = 0; k < plates; k += 2) {
                float a = (k + .5f * b + .5f) * Mth.TWO_PI / plates;
                poseStack.pushPose();
                poseStack.translate(Mth.cos(a) * (band[2] - .02f), (band[0] + y1) * .5f, Mth.sin(a) * (band[2] - .02f));
                poseStack.mulPose(Axis.YP.rotation(-a + Mth.HALF_PI));
                RenderHelper.rect(glow, poseStack.last(), .12f, (y1 - band[0]) * .9f, RuneRender.additive(WoodFx.LIFE, pulse * grown), GlyphRender.FULL, true);
                poseStack.popPose();
            }
        }
        poseStack.popPose();
        super.render(shell, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(BarkShell shell) {
        return WoodRender.BARK;
    }
}
