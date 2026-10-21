package com.waylays.arcanes.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.waylays.arcanes.ArcanesOublies;
import com.waylays.arcanes.entity.gold.EldoradoSun;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/**
 * Sun of Eldorado: layered additive billboards (wide glow, two counter-rotating ray crowns, white-hot core), a turning
 * rune ring around it, and golden beams to the targets of the last volley. Shrinks and flickers before exploding.
 */
public class EldoradoSunRenderer extends EntityRenderer<EldoradoSun> {
    private static final ResourceLocation GLOW = GoldItemProjectileRenderer.GLOW;
    private static final ResourceLocation SUN = ArcanesOublies.id("textures/entity/eldorado_sun.png");
    private static final ResourceLocation CIRCLE = ArcanesOublies.id("textures/entity/gold_circle.png");
    private static final ResourceLocation BEAM = ArcanesOublies.id("textures/entity/gold_beam.png");

    public EldoradoSunRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(EldoradoSun entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        float age = entity.tickCount + partialTicks;
        float grow = Mth.clamp(age / EldoradoSun.GROW_TICKS, 0f, 1f);
        grow = 1 - (1 - grow) * (1 - grow);
        float size = grow * (1 + Mth.sin(age * .3f) * .05f);
        if (age > EldoradoSun.COLLAPSE_TICK) {
            float c = Mth.clamp((age - EldoradoSun.COLLAPSE_TICK) / (EldoradoSun.EXPLODE_TICK - EldoradoSun.COLLAPSE_TICK), 0f, 1f);
            size *= (1 - .75f * c) * (1 + Mth.sin(age * 2.5f) * .15f * c);
        }
        float half = entity.getBbHeight() * .5f;

        poseStack.pushPose();
        poseStack.translate(0, half, 0);

        // Rune ring turning flat around the sun
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(age * 1.5f));
        RenderHelper.groundQuad(buffer.getBuffer(RenderType.eyes(CIRCLE)), poseStack.last(), 3.6f * size, 0xFFFFFFFF, LightTexture.FULL_BRIGHT);
        poseStack.popPose();

        // Camera-facing layers
        poseStack.pushPose();
        poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
        layer(buffer.getBuffer(RenderType.eyes(GLOW)), poseStack, 8f * size, 0, 0xFF909090);
        layer(buffer.getBuffer(RenderType.eyes(SUN)), poseStack, 4.6f * size, age * 1.2f, 0xFFFFFFFF);
        layer(buffer.getBuffer(RenderType.eyes(SUN)), poseStack, 3.6f * size, -age * 2f + 22.5f, 0xFFC0C0C0);
        layer(buffer.getBuffer(RenderType.eyes(GLOW)), poseStack, 2.2f * size, 0, 0xFFFFFFFF);
        poseStack.popPose();

        // Beams of the last volley
        float beamAge = age - entity.getVolleyTick();
        if (beamAge >= 0 && beamAge < EldoradoSun.BEAM_TICKS) {
            float fade = 1 - beamAge / EldoradoSun.BEAM_TICKS;
            Vec3 sun = entity.getPosition(partialTicks).add(0, half, 0);
            VertexConsumer beams = buffer.getBuffer(RenderType.eyes(BEAM));
            int v = (int) (255 * fade);
            int color = (255 << 24) | (v << 16) | (v << 8) | v;
            for (int i = 0; i < EldoradoSun.MAX_BEAMS; i++) {
                Entity target = entity.level().getEntity(entity.getBeamTarget(i));
                if (target == null) {
                    continue;
                }
                Vec3 to = target.getPosition(partialTicks).add(0, target.getBbHeight() * .5, 0).subtract(sun);
                float length = (float) to.length();
                RenderHelper.strand(beams, poseStack.last(), Vec3.ZERO, to, .5f * fade + .1f, Math.max(length, .01f), color, LightTexture.FULL_BRIGHT);
            }
        }
        poseStack.popPose();

        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    private static void layer(VertexConsumer consumer, PoseStack poseStack, float size, float rollDegrees, int argb) {
        poseStack.pushPose();
        poseStack.mulPose(Axis.ZP.rotationDegrees(rollDegrees));
        poseStack.scale(size, size, size);
        RenderHelper.billboard(consumer, poseStack.last(), .5f, argb, LightTexture.FULL_BRIGHT);
        poseStack.popPose();
    }

    @Override
    public ResourceLocation getTextureLocation(EldoradoSun entity) {
        return SUN;
    }
}
