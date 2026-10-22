package com.waylays.arcanes.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.waylays.arcanes.entity.gem.LapisSeal;
import com.waylays.arcanes.spells.gem.Gem;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * The Lapis Seal: four tablets of lapis rising out of the ground round the prisoner and turning slowly about it, a
 * golden rune burning on each; chains of golden light from each tablet to the prisoner, beads of light running along
 * them; a golden ring turning over its head. When the seal breaks, the tablets fall apart.
 */
public class LapisSealRenderer extends EntityRenderer<LapisSeal> {
    private static final BlockState LAPIS = Blocks.LAPIS_BLOCK.defaultBlockState();

    public LapisSealRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(LapisSeal seal, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        float age = seal.tickCount + partialTicks;
        int life = seal.getLife();
        float risen = seal.risen(partialTicks);
        boolean broken = age >= life;
        float since = age - life;
        LivingEntity target = seal.getTarget();
        Vec3 base = seal.getPosition(partialTicks);
        Vec3 chest = target != null ? target.getPosition(partialTicks).add(0, target.getBbHeight() * .55, 0).subtract(base) : new Vec3(0, 1, 0);
        float headY = target != null ? target.getBbHeight() + .35f : 2.2f;
        // The tablets (or their pieces)
        for (int i = 0; i < LapisSeal.TABLETS; i++) {
            Vec3 t = seal.tablet(i, age);
            float face = (float) Mth.atan2(-t.x, -t.z);
            if (broken) {
                for (int k = 0; k < 4; k++) {
                    int s = seal.getId() * 31 + i * 7 + k;
                    float vx = (float) t.x * .06f + (RuneRender.hash(s) - .5f) * .1f;
                    float vz = (float) t.z * .06f + (RuneRender.hash(s + 1) - .5f) * .1f;
                    WallRender.fragment(LAPIS, poseStack, buffer, packedLight, (float) t.x, .3f + k * .35f, (float) t.z, vx, .15f + .1f * RuneRender.hash(s + 2),
                            vz, since, LapisSeal.BREAK, .4f, s);
                }
                continue;
            }
            poseStack.pushPose();
            poseStack.translate(t.x, -1.4f * (1 - risen) + .08f * Mth.sin(age * .1f + i), t.z);
            poseStack.mulPose(Axis.YP.rotation(face));
            RuneRender.block(LAPIS, poseStack, buffer, packedLight, 0, 0, 0, .9f, 1.4f, .2f, 0, 0);
            poseStack.popPose();
        }
        if (broken) {
            super.render(seal, entityYaw, partialTicks, poseStack, buffer, packedLight);
            return;
        }
        // Their runes
        VertexConsumer glyphs = buffer.getBuffer(RenderType.eyes(RuneRender.GLYPHS));
        float glow = risen * (.7f + .3f * Mth.sin(age * .2f));
        for (int i = 0; i < LapisSeal.TABLETS; i++) {
            Vec3 t = seal.tablet(i, age);
            float out = (float) Mth.atan2(t.x, t.z);
            poseStack.pushPose();
            poseStack.translate(t.x * 1.07, -1.4f * (1 - risen) + .75f, t.z * 1.07);
            poseStack.mulPose(Axis.YP.rotation(out));
            RuneRender.glyph(glyphs, poseStack.last(), seal.getId() + i * 5, .6f, RuneRender.additive(Gem.LAPIS.light, glow), GlyphRender.FULL);
            poseStack.popPose();
        }
        // The chains, once they have risen
        if (risen >= 1) {
            VertexConsumer stroke = buffer.getBuffer(RenderType.eyes(GlyphRender.STROKE));
            for (int i = 0; i < LapisSeal.TABLETS; i++) {
                Vec3 t = seal.tablet(i, age).add(0, .75, 0);
                // A chain sags a little
                Vec3 prev = t;
                for (int k = 1; k <= 8; k++) {
                    float u = k / 8f;
                    Vec3 p = t.lerp(chest, u).add(0, -.25 * Mth.sin(u * Mth.PI), 0);
                    GlyphRender.beam(stroke, poseStack.last(), prev, p, k % 2 == 0 ? .09f : .05f, RuneRender.additive(Gem.LAPIS.light, .75f));
                    prev = p;
                }
            }
            for (int i = 0; i < LapisSeal.TABLETS; i++) {
                Vec3 t = seal.tablet(i, age).add(0, .75, 0);
                float u = ((age * .05f + i * .25f) % 1);
                GemRender.glow(poseStack, buffer, t.lerp(chest, u).add(0, -.25 * Mth.sin(u * Mth.PI), 0), Gem.LAPIS.light, .22f, .9f);
            }
            // The golden ring over its head
            poseStack.pushPose();
            poseStack.translate(chest.x, headY, chest.z);
            poseStack.mulPose(Axis.YP.rotationDegrees(age * 3));
            RenderHelper.groundQuad(buffer.getBuffer(RenderType.eyes(RuneRender.RING_GLOW)), poseStack.last(), .8f, RuneRender.additive(Gem.LAPIS.light, .8f), GlyphRender.FULL);
            poseStack.popPose();
        }
        // The sigil under it
        GemRender.sigil(poseStack, buffer, Gem.LAPIS.color, LapisSeal.RADIUS + .9f, .6f * risen, age * .7f);
        super.render(seal, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    @Override
    public boolean shouldRender(LapisSeal seal, Frustum frustum, double x, double y, double z) {
        return frustum.isVisible(seal.getBoundingBox().inflate(4));
    }

    @Override
    public ResourceLocation getTextureLocation(LapisSeal seal) {
        return GemRender.SIGIL;
    }
}
