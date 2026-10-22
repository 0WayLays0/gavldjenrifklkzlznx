package com.waylays.arcanes.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.waylays.arcanes.entity.gold.GildedChainsEntity;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/**
 * Gilded Chains: the sun stone burning under the prisoner, a sun medallion at the foot of each chain with an eye bolt
 * standing on it, real links of gold shooting out of the bolts into two loops locked round the prisoner, and a gold
 * padlock hanging from them on the caster's side. The links flash each time the chains lay a coat of gold.
 */
public class GildedChainsRenderer extends EntityRenderer<GildedChainsEntity> {
    private static final int KEYHOLE = 0x3A2408;

    public GildedChainsRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(GildedChainsEntity entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        float age = entity.tickCount + partialTicks;
        float t = Mth.clamp(age / GildedChainsEntity.EMERGE_TICKS, 0f, 1f);
        float emerge = 1 - (1 - t) * (1 - t) * (1 - t);
        Entity bound = entity.getFirstPassenger();
        float width = bound == null ? .6f : bound.getBbWidth();
        float height = bound == null ? 1.8f : bound.getBbHeight();
        float fade = Mth.clamp((entity.getDuration() - age) / 20f, .3f, 1f);

        GoldRender.sigil(poseStack, buffer, (width * .5f + 1.6f) * emerge, fade, age * 1.5f);
        for (int i = 0; i < GildedChainsEntity.CHAIN_COUNT; i++) {
            Vec3 anchor = GildedChainsEntity.anchorOffset(i, width);
            poseStack.pushPose();
            poseStack.translate(anchor.x, .035, anchor.z);
            poseStack.mulPose(Axis.YP.rotationDegrees(i * 90 + 45));
            GoldRender.coin(poseStack, buffer, .75f * (.4f + .6f * t), age, i);
            poseStack.popPose();
        }

        // Gold glints: the chains are lit as if by a torch even in the dark, and flash as they gild
        int light = LightTexture.pack(Math.max(10, LightTexture.block(packedLight)), LightTexture.sky(packedLight));
        float since = age % GildedChainsEntity.GILD_EVERY;
        float pulse = entity.tickCount >= GildedChainsEntity.GILD_EVERY ? Math.max(0, 1 - since / 8f) : 0;
        int rgb = GlyphRender.whiten(GoldRender.GOLD, pulse * .6f);
        VertexConsumer metal = GoldRender.metal(buffer);
        float waist = width * .5f + .08f;
        for (int i = 0; i < GildedChainsEntity.CHAIN_COUNT; i++) {
            Vec3 anchor = GildedChainsEntity.anchorOffset(i, width);
            Vec3 out = new Vec3(anchor.x, 0, anchor.z).normalize();
            float facing = (float) Math.toDegrees(Math.atan2(out.z, -out.x));
            GoldRender.link(metal, poseStack, anchor.add(0, .17, 0), new Vec3(0, 1, 0), facing, .8f, rgb, age, light);
            // A little tension tremor once the chains are taut
            float tremor = t >= 1 ? Mth.sin(age * 1.7f + i * 2.1f) * .02f : 0f;
            Vec3 attach = out.scale(waist).add(0, height * (i % 2 == 0 ? .42f : .58f) + tremor, 0);
            Vec3 from = anchor.add(0, .3, 0);
            GoldRender.chain(metal, poseStack, from, attach, (float) from.distanceTo(attach) * emerge, 1f, rgb, age, light);
        }
        if (t >= 1) {
            GoldRender.loop(metal, poseStack, waist, height * .42f, .9f, rgb, age, light);
            GoldRender.loop(metal, poseStack, waist, height * .58f, .9f, rgb, age, light);
            padlock(metal, poseStack, entity.getYRot(), waist, height * .42f, age, light);
        }
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    /** The padlock hangs from the lower loop on the side {@code yaw} faces, swaying a little. */
    private static void padlock(VertexConsumer metal, PoseStack poseStack, float yaw, float waist, float y, float age, int light) {
        float rad = yaw * Mth.DEG_TO_RAD;
        poseStack.pushPose();
        poseStack.translate(-Mth.sin(rad) * (waist + .05f), y - .1f, Mth.cos(rad) * (waist + .05f));
        poseStack.mulPose(Axis.YP.rotationDegrees(-yaw));
        poseStack.mulPose(Axis.XP.rotationDegrees(Mth.sin(age * .15f) * 6));
        poseStack.scale(.8f, .8f, .8f);
        PoseStack.Pose pose = poseStack.last();
        Solid.draw(metal, pose, GoldRender.LINK, GoldRender.PALE, 255, age, .5f, light);
        Solid.draw(metal, pose, GoldRender.PADLOCK, GoldRender.GOLD, 255, age, .8f, light);
        Solid.draw(metal, pose, GoldRender.KEYHOLE, KEYHOLE, 255, 0, 0, light);
        poseStack.popPose();
    }

    @Override
    public ResourceLocation getTextureLocation(GildedChainsEntity entity) {
        return GoldRender.METAL;
    }
}
