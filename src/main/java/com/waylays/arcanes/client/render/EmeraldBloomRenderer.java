package com.waylays.arcanes.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.waylays.arcanes.entity.gem.EmeraldBloom;
import com.waylays.arcanes.spells.gem.Gem;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * The Emerald Bloom: six long emerald-cut emeralds for petals, rising out of the ground and opening outward like a
 * flower, a great emerald turning in the air at its heart; motes of green light rising, and a soft ring of light
 * rolling out each time it heals.
 */
public class EmeraldBloomRenderer extends EntityRenderer<EmeraldBloom> {
    public EmeraldBloomRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(EmeraldBloom bloom, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        float age = bloom.tickCount + partialTicks;
        float open = bloom.open(partialTicks);
        // The petals
        for (int i = 0; i < EmeraldBloom.PETALS; i++) {
            float a = bloom.getYRot() * Mth.DEG_TO_RAD + i * Mth.TWO_PI / EmeraldBloom.PETALS;
            float sway = .04f * Mth.sin(age * .08f + i);
            poseStack.pushPose();
            poseStack.translate(Mth.cos(a) * EmeraldBloom.PETAL_RING * (.4f + .6f * open), 0, Mth.sin(a) * EmeraldBloom.PETAL_RING * (.4f + .6f * open));
            poseStack.mulPose(Axis.YP.rotation(-a + Mth.HALF_PI));
            // Leaning outward as the flower opens
            poseStack.mulPose(Axis.XP.rotation(-(.15f + .55f * open + sway)));
            poseStack.translate(0, .75f * open, 0);
            poseStack.scale(.55f, 1.6f, .32f);
            GemRender.gem(poseStack, buffer, GemRender.Cut.STEP, Gem.EMERALD.color, 1.05f * open, .72f, age + i * 17);
            poseStack.popPose();
        }
        // The heart
        Vec3 heart = new Vec3(0, .6f + 1.1f * open + .1f * Mth.sin(age * .1f), 0);
        poseStack.pushPose();
        poseStack.translate(heart.x, heart.y, heart.z);
        poseStack.mulPose(Axis.YP.rotationDegrees(age * 2));
        poseStack.mulPose(Axis.XP.rotationDegrees(90));
        GemRender.jewel(poseStack, buffer, GemRender.Cut.STEP, Gem.EMERALD.color, 1f * open, .75f, age, .8f, false, bloom.getId());
        poseStack.popPose();
        // Motes rising
        for (int k = 0; k < 10; k++) {
            float h = RuneRender.hash(bloom.getId() * 13 + k);
            float t = ((age * (.012f + .01f * h) + h) % 1);
            float a = h * Mth.TWO_PI + t * 2;
            float r = .5f + 2.5f * RuneRender.hash(k * 7 + 3);
            GemRender.glow(poseStack, buffer, new Vec3(Mth.cos(a) * r, t * 3.5f, Mth.sin(a) * r), Gem.EMERALD.light, .18f, open * (1 - t) * .9f);
        }
        // A ring of light at each healing
        if (age >= EmeraldBloom.OPEN) {
            float since = (age - EmeraldBloom.OPEN) % EmeraldBloom.HEAL_TICKS;
            float t = since / 14f;
            if (t < 1) {
                poseStack.pushPose();
                poseStack.translate(0, .1, 0);
                RenderHelper.groundQuad(buffer.getBuffer(RenderType.eyes(RuneRender.RING_GLOW)), poseStack.last(), EmeraldBloom.RANGE * t,
                        RuneRender.additive(Gem.EMERALD.light, (1 - t) * .7f * open), GlyphRender.FULL);
                poseStack.popPose();
            }
        }
        GemRender.glow(poseStack, buffer, heart, Gem.EMERALD.color, 2.2f, .45f * open);
        GemRender.sigil(poseStack, buffer, Gem.EMERALD.color, EmeraldBloom.RANGE * .45f, .5f * open, age * .5f);
        super.render(bloom, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    @Override
    public boolean shouldRender(EmeraldBloom bloom, Frustum frustum, double x, double y, double z) {
        return frustum.isVisible(bloom.getBoundingBox().inflate(EmeraldBloom.RANGE, 4, EmeraldBloom.RANGE));
    }

    @Override
    public ResourceLocation getTextureLocation(EmeraldBloom bloom) {
        return GemRender.FACET;
    }
}
