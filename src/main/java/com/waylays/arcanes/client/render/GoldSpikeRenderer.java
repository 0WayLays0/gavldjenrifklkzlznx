package com.waylays.arcanes.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.waylays.arcanes.entity.gold.GoldSpikeEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * A cluster of Golden Spikes: the sun glowing on the ground where it is about to come out, then a great crystal of gold
 * and smaller ones leaning out round it, shooting up white-hot with a little overshoot, cooling to polished gold, and
 * sinking back. The crown that ends a wave is a ring of seven leaning crystals round a towering one.
 */
public class GoldSpikeRenderer extends EntityRenderer<GoldSpikeEntity> {
    public GoldSpikeRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(GoldSpikeEntity spike, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        float age = spike.tickCount + partialTicks;
        int warmup = spike.getWarmup();
        float since = age - warmup;
        boolean crown = spike.isCrown();
        if (since < 0) {
            // The ground glows where it is about to burst out
            float warn = warmup <= 0 ? 1 : Mth.clamp(1 + since / warmup, 0, 1);
            GoldRender.sigil(poseStack, buffer, (crown ? 1.6f : .7f) * (.5f + .5f * warn), warn * .8f, age * 3);
            return;
        }
        float rise = since < GoldSpikeEntity.RISE_TICKS ? backOut(since / GoldSpikeEntity.RISE_TICKS) : 1;
        float sinkFrom = GoldSpikeEntity.OUT_TICKS - GoldSpikeEntity.SINK_TICKS;
        float sink = Mth.clamp((since - sinkFrom) / GoldSpikeEntity.SINK_TICKS, 0, 1);
        float out = rise * (1 - sink * sink);
        float heat = Mth.clamp(1 - since / 12f, 0, 1) * .85f;
        int rgb = GoldRender.heat(heat);
        int seed = spike.getId();

        VertexConsumer metal = GoldRender.metal(buffer);
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(-spike.getYRot()));
        // No two clusters alike: the great crystal's shape, size and lean, and how many grow round it, all vary
        float tall = (crown ? 2.6f : 1.4f) * (.75f + .6f * RuneRender.hash(seed * 7));
        float wide = (crown ? 1.6f : 1.15f) * (.8f + .45f * RuneRender.hash(seed * 11));
        float[] main = crown ? GoldRender.CRYSTAL : pick(seed * 37);
        if (main == GoldRender.NEEDLE) {
            tall *= 1.25f;
        } else if (main == GoldRender.NUGGET) {
            tall *= .6f;
        }
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(RuneRender.hash(seed * 5) * 360));
        crystal(metal, poseStack, main, RuneRender.hash(seed * 3) * 12, wide, tall, out, rgb, age, 0);
        poseStack.popPose();
        int around = crown ? 6 + (int) (RuneRender.hash(seed * 41) * 4) : 1 + (int) (RuneRender.hash(seed * 43) * 4);
        for (int k = 0; k < around; k++) {
            float angle = k * 360f / around + RuneRender.hash(seed * 31 + k) * (240f / around);
            float lean = 14 + RuneRender.hash(seed * 17 + k) * 34;
            float size = (crown ? .7f : .35f) + RuneRender.hash(seed * 13 + k) * (crown ? .55f : .5f);
            float[] shape = pick(seed * 53 + k);
            float stretch = shape == GoldRender.NEEDLE ? 2.1f : shape == GoldRender.NUGGET ? .9f : 1.3f + RuneRender.hash(seed * 19 + k) * .6f;
            float reach = (crown ? .75f : .22f) + RuneRender.hash(seed * 23 + k) * (crown ? .35f : .25f);
            poseStack.pushPose();
            poseStack.mulPose(Axis.YP.rotationDegrees(angle));
            poseStack.translate(reach, 0, 0);
            // The small ones come out a beat after the great one
            float late = Mth.clamp(out * 1.15f - RuneRender.hash(seed * 29 + k) * .15f, 0, 1);
            crystal(metal, poseStack, shape, lean, size, size * stretch, late, rgb, age, k + 1);
            poseStack.popPose();
        }
        poseStack.popPose();

        if (heat > .05f) {
            GemRender.glow(poseStack, buffer, new Vec3(0, .25, 0), GoldRender.MOLTEN, crown ? 2.6f : 1.4f, heat * out);
        }
        super.render(spike, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    /** One crystal of {@code shape}, leaning {@code lean} degrees outward (+X), {@code width} across and {@code height} high, {@code out} of the ground. */
    private static void crystal(VertexConsumer metal, PoseStack poseStack, float[] shape, float lean, float width, float height, float out,
                                int rgb, float time, int index) {
        if (out <= .01f) {
            return;
        }
        poseStack.pushPose();
        poseStack.mulPose(Axis.ZP.rotationDegrees(-lean));
        // It rises out of the ground: what is not out yet is below it
        poseStack.translate(0, -height * (1 - out), 0);
        poseStack.mulPose(Axis.YP.rotationDegrees(index * 47));
        poseStack.scale(width, height, width);
        Solid.draw(metal, poseStack.last(), shape, rgb, 255, time + index * 11, .9f, GlyphRender.FULL);
        poseStack.popPose();
    }

    private static float[] pick(int seed) {
        return GoldRender.CRYSTALS[(int) (RuneRender.hash(seed) * GoldRender.CRYSTALS.length) % GoldRender.CRYSTALS.length];
    }

    /** Ease out with a little overshoot. */
    private static float backOut(float x) {
        float c1 = 1.70158f, c3 = c1 + 1, t = x - 1;
        return 1 + c3 * t * t * t + c1 * t * t;
    }

    @Override
    public ResourceLocation getTextureLocation(GoldSpikeEntity spike) {
        return GoldRender.METAL;
    }
}
