package com.waylays.arcanes.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.waylays.arcanes.ArcanesOublies;
import com.waylays.arcanes.entity.book.WordOfPower;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * Word of Power: giant runic letters of fire hanging in the air, popping into being one by one, flickering, then
 * swelling and burning out when the word explodes; a ring of fire rushes ahead of it.
 */
public class WordOfPowerRenderer extends EntityRenderer<WordOfPower> {
    private static final ResourceLocation GLYPHS = ArcanesOublies.id("textures/entity/power_glyphs.png");
    private static final float LETTER_SIZE = 1.25f;

    public WordOfPowerRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(WordOfPower entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        float age = entity.tickCount + partialTicks;
        float blast = Mth.clamp((age - WordOfPower.DETONATE_TICK) / (WordOfPower.END_TICK - WordOfPower.DETONATE_TICK), 0f, 1f);
        float charge = Mth.clamp((age - (WordOfPower.DETONATE_TICK - 8)) / 8f, 0f, 1f) * (blast > 0 ? 0 : 1);
        poseStack.pushPose();
        // Local +Z: where the word explodes; +X: the caster's left
        poseStack.mulPose(Axis.YP.rotationDegrees(-entity.getYRot()));
        poseStack.mulPose(Axis.XP.rotationDegrees(entity.getXRot()));
        for (int i = 0; i < WordOfPower.LETTERS; i++) {
            float since = age - (1 + i * WordOfPower.LETTER_INTERVAL);
            if (since < 0) {
                continue;
            }
            float t = Mth.clamp(since / 4f, 0f, 1f);
            // Pops in a little too big, settles, then swells as it explodes
            float pop = t < 1 ? Mth.sin(t * Mth.HALF_PI) * 1.2f : 1.2f - .2f * Mth.clamp((since - 4) / 3f, 0f, 1f);
            float size = LETTER_SIZE * pop * (1 + blast * 1.4f) * (1 + charge * .12f * Mth.sin(age * 2.1f + i));
            float fade = 1 - blast * blast;
            float flicker = .82f + .18f * Mth.sin(age * 1.7f + i * 2.3f) * Mth.sin(age * .9f + i);
            float bright = Mth.clamp(flicker * fade + charge * .3f, 0f, 1f);
            poseStack.pushPose();
            poseStack.translate(((WordOfPower.LETTERS - 1) * .5f - i) * WordOfPower.SPACING, Mth.sin(age * .2f + i * 1.3f) * .05f, 0);
            // Halo on both faces (additive layers are one-sided), then the glyph read left to right from the caster's side
            int halo = BlastEffectRenderer.rgb(bright * .9f, bright * .38f, bright * .12f);
            BlastEffectRenderer.layer(buffer, BlastEffectRenderer.CORE, poseStack, size * 1.7f, 0, halo);
            poseStack.mulPose(Axis.YP.rotationDegrees(180f));
            BlastEffectRenderer.layer(buffer, BlastEffectRenderer.CORE, poseStack, size * 1.7f, 0, halo);
            int glyph = entity.glyph(i);
            float u0 = (glyph % 4) * .25f;
            float v0 = (glyph / 4) * .5f;
            RenderHelper.rectUv(buffer.getBuffer(RenderType.eyes(GLYPHS)), poseStack.last(), size, size, u0, v0, u0 + .25f, v0 + .5f,
                    BlastEffectRenderer.rgb(bright, bright * .92f, bright * .8f), LightTexture.FULL_BRIGHT, true);
            poseStack.popPose();
        }
        // The shock ring rolling ahead of the blast
        if (blast > 0) {
            float ring = 1 - (1 - blast) * (1 - blast);
            float f = 1 - blast;
            poseStack.pushPose();
            poseStack.translate(0, 0, ring * 5f);
            BlastEffectRenderer.layer(buffer, BlastEffectRenderer.RING, poseStack, 2f + ring * 9f, 0, BlastEffectRenderer.rgb(f, f * .55f, f * .2f));
            poseStack.mulPose(Axis.YP.rotationDegrees(180f));
            BlastEffectRenderer.layer(buffer, BlastEffectRenderer.RING, poseStack, 2f + ring * 9f, 0, BlastEffectRenderer.rgb(f, f * .55f, f * .2f));
            poseStack.popPose();
        }
        poseStack.popPose();
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(WordOfPower entity) {
        return GLYPHS;
    }
}
