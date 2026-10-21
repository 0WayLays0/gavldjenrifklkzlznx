package com.waylays.arcanes.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.waylays.arcanes.entity.glyph.GlyphSeal;
import com.waylays.arcanes.registry.ModSchools;
import com.waylays.arcanes.spells.glyph.GlyphElement;
import com.waylays.arcanes.spells.glyph.GlyphStrokes;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/**
 * Elemental Seal: the six element glyphs turning around the caster at chest height, a thread of gold light linking
 * them and a ring on the ground. The glyph about to fire burns brighter; the one that just fired flares and dims.
 */
public class GlyphSealRenderer extends EntityRenderer<GlyphSeal> {
    public GlyphSealRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(GlyphSeal seal, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        Entity bearer = seal.getBearer();
        float strength = seal.strength(partialTicks);
        if (bearer == null || strength <= 0) {
            return;
        }
        Vec3 feet = bearer.getPosition(partialTicks).subtract(seal.getPosition(partialTicks));
        float age = seal.tickCount + partialTicks;
        int next = seal.getNext();
        int last = Math.floorMod(next - 1, 6);
        float sinceShot = seal.sinceShot(partialTicks);

        poseStack.pushPose();
        poseStack.translate(feet.x, feet.y, feet.z);
        // The thread linking the glyphs
        VertexConsumer thread = buffer.getBuffer(RenderType.eyes(GlyphRender.STROKE));
        for (int i = 0; i < 6; i++) {
            float w = Math.min(seal.written(i, partialTicks), seal.written((i + 1) % 6, partialTicks));
            if (w > 0) {
                GlyphRender.beam(thread, poseStack.last(), seal.offset(i, partialTicks), seal.offset((i + 1) % 6, partialTicks), .08f,
                        RuneRender.additive(ModSchools.GLYPH_COLOR, .45f * w * strength));
            }
        }
        // The ring on the ground
        poseStack.pushPose();
        poseStack.translate(0, .05, 0);
        poseStack.mulPose(Axis.XP.rotationDegrees(90));
        poseStack.mulPose(Axis.ZP.rotationDegrees(age * GlyphSeal.SPIN));
        GlyphRender.strokes(buffer, poseStack.last(), GlyphStrokes.RING, GlyphSeal.ORBIT + .2f, seal.written(5, partialTicks), ModSchools.GLYPH_COLOR,
                0xFFF4DC, .5f * strength, .8f);
        poseStack.popPose();
        for (int i = 0; i < 6; i++) {
            float written = seal.written(i, partialTicks);
            if (written <= 0) {
                continue;
            }
            GlyphElement element = GlyphElement.byId(i);
            float glow = .65f;
            float size = .26f;
            if (i == next) {
                glow = .85f + .15f * Mth.sin(age * .5f);
            }
            if (i == last && sinceShot >= 0 && sinceShot < 10) {
                float k = 1 - sinceShot / 10;
                glow = .45f + .55f * k;
                size *= 1 + .5f * k;
            }
            Vec3 at = seal.offset(i, partialTicks);
            poseStack.pushPose();
            poseStack.translate(at.x, at.y, at.z);
            poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
            GlyphRender.halo(buffer, poseStack.last(), GlyphRender.GLOW, size * 1.8f, RuneRender.additive(element.color, .35f * glow * strength));
            GlyphRender.strokes(buffer, poseStack.last(), GlyphStrokes.of(element), size, written, element.color, element.core, glow * strength, 1.2f);
            poseStack.popPose();
        }
        poseStack.popPose();
        super.render(seal, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    @Override
    public boolean shouldRender(GlyphSeal entity, Frustum frustum, double x, double y, double z) {
        return true;
    }

    @Override
    public ResourceLocation getTextureLocation(GlyphSeal entity) {
        return GlyphRender.STROKE;
    }
}
