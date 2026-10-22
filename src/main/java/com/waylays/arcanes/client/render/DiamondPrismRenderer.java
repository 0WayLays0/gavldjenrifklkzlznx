package com.waylays.arcanes.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.waylays.arcanes.entity.gem.DiamondPrism;
import com.waylays.arcanes.spells.gem.Gem;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/**
 * The Diamond Prism: a great cut diamond turning slowly in the air, sparkling in every colour, a ring of small shards
 * circling it and a soft column of light under it; when it fires, rays split into red, green and blue reach out to
 * its targets; while it holds a Ruby Ray it flares.
 */
public class DiamondPrismRenderer extends EntityRenderer<DiamondPrism> {
    private static final int[] SPECTRUM = {0xFF4050, 0x50FF70, 0x5070FF};

    public DiamondPrismRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(DiamondPrism prism, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        float age = prism.tickCount + partialTicks;
        float appear = Mth.clamp(age / 12, 0, 1);
        float fade = Mth.clamp((prism.getLife() - age) / 12, 0, 1);
        float scale = appear * (1 + .12f * Mth.sin(appear * Mth.PI)) * (.6f + .4f * fade);
        boolean caught = age - prism.getCaught() < 4;
        float bob = .12f * Mth.sin(age * .07f);
        // The column of light down to the ground, and the sigil there
        poseStack.pushPose();
        poseStack.translate(0, -DiamondPrism.HOVER, 0);
        GemRender.sigil(poseStack, buffer, Gem.DIAMOND.color, 1.8f, .5f * appear * fade, age * .8f);
        poseStack.popPose();
        GemRender.ray(poseStack, buffer, new Vec3(0, bob - .5, 0), new Vec3(0, -DiamondPrism.HOVER, 0), Gem.DIAMOND.color, .35f, .25f * appear * fade, age);
        // The diamond
        poseStack.pushPose();
        poseStack.translate(0, bob, 0);
        poseStack.mulPose(Axis.YP.rotationDegrees(prism.getYRot() + age * 1.5f));
        poseStack.mulPose(Axis.XP.rotationDegrees(8 * Mth.sin(age * .05f)));
        GemRender.jewel(poseStack, buffer, GemRender.Cut.BRILLIANT, Gem.DIAMOND.color, 1.3f * scale, .55f, age, caught ? 1 : .55f, true, prism.getId());
        poseStack.popPose();
        // The ring of shards
        for (int i = 0; i < 6; i++) {
            float a = i * Mth.TWO_PI / 6 + age * .05f;
            poseStack.pushPose();
            poseStack.translate(Mth.cos(a) * 1.2f * scale, bob + .25f * Mth.sin(age * .1f + i), Mth.sin(a) * 1.2f * scale);
            poseStack.mulPose(Axis.YP.rotation(-a));
            poseStack.mulPose(Axis.ZP.rotationDegrees(30));
            GemRender.gem(poseStack, buffer, GemRender.Cut.CRYSTAL, Gem.DIAMOND.color, .3f * scale, .7f, age + i * 20);
            poseStack.popPose();
        }
        if (caught) {
            GemRender.glow(poseStack, buffer, new Vec3(0, bob, 0), 0xFF4060, 2.4f, .7f);
        }
        // Its rays
        float since = age - prism.getShotAt();
        if (since >= 0 && since < 8) {
            float strength = 1 - since / 8;
            for (int i = 0; i < DiamondPrism.MAX_SHOTS; i++) {
                Entity target = prism.level().getEntity(prism.getShot(i));
                if (target == null) {
                    continue;
                }
                Vec3 to = target.getPosition(partialTicks).add(0, target.getBbHeight() * .5, 0).subtract(prism.getPosition(partialTicks));
                Vec3 from = new Vec3(0, bob, 0);
                Vec3 side = to.subtract(from).cross(new Vec3(0, 1, 0));
                side = side.lengthSqr() < 1.0E-4 ? new Vec3(.1, 0, 0) : side.normalize().scale(.09);
                for (int c = 0; c < 3; c++) {
                    GemRender.ray(poseStack, buffer, from, to.add(side.scale(c - 1)), SPECTRUM[c], .06f, strength, age + c);
                }
                GemRender.glow(poseStack, buffer, to, 0xFFFFFF, .7f, strength);
            }
        }
        super.render(prism, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    @Override
    public boolean shouldRender(DiamondPrism prism, Frustum frustum, double x, double y, double z) {
        return frustum.isVisible(prism.getBoundingBox().inflate(DiamondPrism.RANGE));
    }

    @Override
    public ResourceLocation getTextureLocation(DiamondPrism prism) {
        return GemRender.FACET;
    }
}
