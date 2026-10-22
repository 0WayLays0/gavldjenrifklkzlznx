package com.waylays.arcanes.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.waylays.arcanes.ArcanesOublies;
import com.waylays.arcanes.entity.gold.GoldenSentinel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * Golden Sentinel: a humanoid in full golden armor with a golden sword and glowing eyes, the sun of Eldorado standing
 * behind its head with its rays turning slowly. It rises out of molten gold when summoned, glowing as it comes up.
 */
public class GoldenSentinelRenderer extends HumanoidMobRenderer<GoldenSentinel, HumanoidModel<GoldenSentinel>> {
    private static final ResourceLocation TEXTURE = ArcanesOublies.id("textures/entity/golden_sentinel.png");
    private static final RenderType EYES = RenderType.eyes(ArcanesOublies.id("textures/entity/golden_sentinel_eyes.png"));

    public GoldenSentinelRenderer(EntityRendererProvider.Context context) {
        super(context, new HumanoidModel<>(context.bakeLayer(ModelLayers.ZOMBIE)), .5f);
        this.addLayer(new HumanoidArmorLayer<>(this,
                new HumanoidModel<>(context.bakeLayer(ModelLayers.ZOMBIE_INNER_ARMOR)),
                new HumanoidModel<>(context.bakeLayer(ModelLayers.ZOMBIE_OUTER_ARMOR)),
                context.getModelManager()));
        this.addLayer(new EyesLayer<>(this) {
            @Override
            public RenderType renderType() {
                return EYES;
            }
        });
        this.addLayer(new SunCrest(this));
    }

    @Override
    public void render(GoldenSentinel sentinel, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        float age = sentinel.tickCount + partialTicks;
        float rise = Mth.clamp(age / GoldenSentinel.EMERGE_TICKS, 0, 1);
        if (rise < 1) {
            // Rising out of the molten gold: what is not up yet is under the ground
            float eased = 1 - (1 - rise) * (1 - rise);
            GemRender.glow(poseStack, buffer, new Vec3(0, .2, 0), GoldRender.MOLTEN, 2.4f, 1 - rise * .6f);
            poseStack.pushPose();
            poseStack.translate(0, -sentinel.getBbHeight() * (1 - eased), 0);
            super.render(sentinel, entityYaw, partialTicks, poseStack, buffer, packedLight);
            poseStack.popPose();
            return;
        }
        super.render(sentinel, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(GoldenSentinel entity) {
        return TEXTURE;
    }

    /** The sun of Eldorado behind the sentinel's head: a stamped disc of gold and twelve rays round it, turning. */
    private static final class SunCrest extends RenderLayer<GoldenSentinel, HumanoidModel<GoldenSentinel>> {
        private static final int RAYS = 12;

        SunCrest(RenderLayerParent<GoldenSentinel, HumanoidModel<GoldenSentinel>> parent) {
            super(parent);
        }

        @Override
        public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, GoldenSentinel sentinel, float limbSwing, float limbSwingAmount,
                           float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
            if (sentinel.isInvisible()) {
                return;
            }
            poseStack.pushPose();
            getParentModel().head.translateAndRotate(poseStack);
            // Behind the head (the model's +Z), level with its middle; the disc faces forward
            poseStack.translate(0, -.32f, .3f);
            poseStack.mulPose(Axis.ZP.rotationDegrees(ageInTicks * .8f));
            VertexConsumer metal = GoldRender.metal(buffer);
            for (int k = 0; k < RAYS; k++) {
                poseStack.pushPose();
                poseStack.mulPose(Axis.ZP.rotationDegrees(k * 360f / RAYS));
                poseStack.translate(0, .28f, 0);
                float length = k % 2 == 0 ? .34f : .22f;
                poseStack.scale(.18f, length, .1f);
                Solid.draw(metal, poseStack.last(), GoldRender.NEEDLE, k % 2 == 0 ? GoldRender.GOLD : GoldRender.PALE, 255, ageInTicks + k * 5, .8f,
                        GlyphRender.FULL);
                poseStack.popPose();
            }
            poseStack.mulPose(Axis.XP.rotationDegrees(90));
            GoldRender.coin(poseStack, buffer, .58f, ageInTicks, sentinel.getId());
            poseStack.popPose();
        }
    }
}
