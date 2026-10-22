package com.waylays.arcanes.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.waylays.arcanes.entity.gold.GoldfallIngot;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * Goldfall's giant ingot, point down: gathering out of nothing under the vault, white-hot; spinning as it falls with a
 * tail of molten gold; then planted askew in the ground with a shudder, cooling from orange to polished gold, and at
 * last sinking away as it melts into coins. The sun of Eldorado is stamped on its face.
 */
public class GoldfallIngotRenderer extends EntityRenderer<GoldfallIngot> {
    public GoldfallIngotRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(GoldfallIngot ingot, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        float age = ingot.tickCount + partialTicks;
        int seed = ingot.getId();
        float size = GoldfallIngot.SIZE;
        float centre = ingot.getBbHeight() * .5f;
        float spin = seed * 47 % 360;
        float tilt = 0;
        float scale = 1;
        float heat;
        boolean falling = false;
        if (ingot.isPlanted()) {
            float since = age - ingot.plantedAt();
            // It strikes upright, then settles askew with a shudder
            tilt = Mth.clamp(since / 4f, 0, 1) * (10 + seed % 3 * 7) + Mth.sin(since * 1.4f) * 6 * Math.max(0, 1 - since / 12f);
            heat = Mth.clamp(.65f - since / 30f, 0, .65f);
            float left = GoldfallIngot.PLANTED_TICKS - since;
            if (left < 8) {
                scale = Math.max(.05f, left / 8f);
            }
        } else if (age < ingot.getWait()) {
            scale = Mth.clamp(age / 8f, .05f, 1);
            heat = .95f;
            spin += age * 4;
        } else {
            falling = true;
            heat = .7f;
            spin += age * 16;
        }

        poseStack.pushPose();
        // Melting away, it sinks into the ground as it shrinks
        poseStack.translate(0, centre - size * .5f * (1 - scale), 0);
        poseStack.mulPose(Axis.YP.rotationDegrees(spin));
        poseStack.mulPose(Axis.ZP.rotationDegrees(tilt));
        poseStack.mulPose(Axis.XP.rotationDegrees(-90));
        poseStack.scale(size * scale, size * scale, size * scale);
        poseStack.translate(0, -.11f, 0);
        int light = heat > .05f ? GlyphRender.FULL : LightTexture.pack(Math.max(10, LightTexture.block(packedLight)), LightTexture.sky(packedLight));
        Solid.draw(GoldRender.metal(buffer), poseStack.last(), GoldRender.INGOT, GoldRender.heat(heat), 255, age, .9f, light);
        stamp(buffer.getBuffer(RenderType.entityCutoutNoCull(GoldRender.COIN)), poseStack.last(), heat, light);
        poseStack.popPose();

        if (heat > .05f) {
            GemRender.glow(poseStack, buffer, new Vec3(0, centre - size * .5f * scale, 0), GoldRender.MOLTEN, 1.8f * scale, heat);
        }
        if (falling) {
            GemRender.ray(poseStack, buffer, new Vec3(0, centre, 0), new Vec3(0, centre + 3.5f, 0), GoldRender.MOLTEN, .45f, .7f, age);
            GemRender.glow(poseStack, buffer, new Vec3(0, centre, 0), GoldRender.GOLD, 2.6f, .5f);
        }
        super.render(ingot, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    /** The sun of Eldorado stamped on the ingot's top face. */
    private static void stamp(VertexConsumer face, PoseStack.Pose pose, float heat, int light) {
        int argb = 0xFF000000 | GlyphRender.whiten(GoldRender.heat(heat), .2f);
        float y = .2215f, h = .15f, l = .15f;
        RenderHelper.vertex(face, pose, -h, y, -l, 0, 0, argb, light, 0, 1, 0);
        RenderHelper.vertex(face, pose, -h, y, l, 0, 1, argb, light, 0, 1, 0);
        RenderHelper.vertex(face, pose, h, y, l, 1, 1, argb, light, 0, 1, 0);
        RenderHelper.vertex(face, pose, h, y, -l, 1, 0, argb, light, 0, 1, 0);
    }

    @Override
    public ResourceLocation getTextureLocation(GoldfallIngot ingot) {
        return GoldRender.METAL;
    }
}
