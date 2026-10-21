package com.waylays.arcanes.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.waylays.arcanes.ArcanesOublies;
import com.waylays.arcanes.entity.book.ChapterRune;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * The rune marked on an enemy by The Final Chapter: a glyph hovering over its head and a ring at its feet, violet
 * turning white-gold, spinning faster and faster until it goes off.
 */
public class ChapterRuneRenderer extends EntityRenderer<ChapterRune> {
    private static final ResourceLocation GLYPH = ArcanesOublies.id("textures/entity/chapter_rune.png");
    private static final ResourceLocation RING = ArcanesOublies.id("textures/entity/ruin_sigil.png");

    public ChapterRuneRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(ChapterRune entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        LivingEntity target = entity.getTarget();
        if (target == null) {
            return;
        }
        float age = entity.tickCount + partialTicks;
        float appear = Mth.clamp(age / 6f, 0f, 1f);
        float charge = Mth.clamp(age / Math.max(1, entity.getFuse()), 0f, 1f);
        float last = Mth.clamp((age - entity.getFuse() + 12) / 12f, 0f, 1f);
        float pulse = 1 + Mth.sin(age * (.4f + last * 2f)) * (.06f + last * .15f);
        float r = Mth.lerp(charge, .6f, 1f);
        float g = Mth.lerp(charge * charge, .36f, .9f);
        float b = Mth.lerp(charge * charge, 1f, .7f);

        Vec3 targetPos = target.getPosition(partialTicks);
        Vec3 selfPos = entity.getPosition(partialTicks);
        poseStack.pushPose();
        poseStack.translate(targetPos.x - selfPos.x, targetPos.y - selfPos.y, targetPos.z - selfPos.z);

        // Ring at the feet
        poseStack.pushPose();
        poseStack.translate(0, .04, 0);
        poseStack.mulPose(Axis.YP.rotationDegrees(entity.getYRot() + age * (2 + charge * 10)));
        float ring = (target.getBbWidth() * .5f + .6f) * appear;
        RenderHelper.groundQuad(buffer.getBuffer(RenderType.eyes(RING)), poseStack.last(), ring, BlastEffectRenderer.rgb(r * .8f, g * .8f, b * .8f), LightTexture.FULL_BRIGHT);
        poseStack.popPose();

        // Glyph over the head
        poseStack.translate(0, target.getBbHeight() + .7f + Mth.sin(age * .15f) * .08f, 0);
        poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
        float size = 1.1f * appear * pulse;
        BlastEffectRenderer.layer(buffer, BlastEffectRenderer.CORE, poseStack, size * 1.6f, 0, BlastEffectRenderer.rgb(r * .5f * charge, g * .35f * charge, b * .5f * charge));
        BlastEffectRenderer.layer(buffer, GLYPH, poseStack, size, age * (1.5f + charge * 12f), BlastEffectRenderer.rgb(r, g, b));
        poseStack.popPose();
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(ChapterRune entity) {
        return GLYPH;
    }
}
