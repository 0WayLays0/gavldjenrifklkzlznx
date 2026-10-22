package com.waylays.arcanes.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.waylays.arcanes.entity.verdigris.AncientPillar;
import com.waylays.arcanes.spells.verdigris.VerdigrisFx;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The columns of old copper bursting out of the ground: the mossy Ancient Pillar, the plainer columns of the
 * Colonnade and the tall Lightning Rod, a grate of green copper crowned with a copper bulb and a rod that flares
 * when it draws lightning. Vines hang from their capitals; rubble lies around their feet.
 */
public class AncientPillarRenderer extends EntityRenderer<AncientPillar> {
    private static final BlockState BULB = Blocks.OXIDIZED_COPPER_BULB.defaultBlockState();
    private static final BlockState BULB_LIT = BULB.setValue(BlockStateProperties.LIT, true);
    private static final BlockState ROD = Blocks.LIGHTNING_ROD.defaultBlockState();

    public AncientPillarRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(AncientPillar pillar, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        float risen = pillar.risen(partialTicks);
        if (risen <= .001f) {
            return;
        }
        float s = pillar.getScale();
        float height = pillar.height();
        int seed = pillar.getId();
        float sinking = pillar.sinking(partialTicks);
        float shake = sinking > 0 ? .05f * Mth.sin((pillar.tickCount + partialTicks) * 3.3f) : 0;
        rubble(poseStack, buffer, packedLight, seed, s, Mth.clamp(risen, 0, 1));

        poseStack.pushPose();
        poseStack.translate(shake, -(1 - risen) * height, shake * .7f);
        poseStack.mulPose(Axis.YP.rotationDegrees(-pillar.getYRot() + RuneRender.hash(seed) * 90));
        if (pillar.getKind() != AncientPillar.Kind.ROD) {
            // A slight lean, as old stones have
            poseStack.mulPose(Axis.XP.rotationDegrees((RuneRender.hash(seed * 3) - .5f) * 7));
            poseStack.mulPose(Axis.ZP.rotationDegrees((RuneRender.hash(seed * 5) - .5f) * 7));
        }
        switch (pillar.getKind()) {
            case PILLAR -> pillar(poseStack, buffer, packedLight, seed, s);
            case COLUMN -> column(poseStack, buffer, packedLight, seed, s);
            case ROD -> rod(pillar, poseStack, buffer, packedLight, partialTicks);
        }
        poseStack.popPose();
        super.render(pillar, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    /** The Ancient Pillar (3.6 blocks): mossy base, a shaft of weathered then green copper, a carved capital under moss. */
    private static void pillar(PoseStack poseStack, MultiBufferSource buffer, int light, int seed, float s) {
        VerdigrisRender.block(VerdigrisRender.MOSSY_BRICKS, poseStack, buffer, light, 0, 0, 0, 1.3f * s, .45f * s, 1.3f * s);
        VerdigrisRender.block(VerdigrisRender.WEATHERED_CUT, poseStack, buffer, light, 0, .45f * s, 0, .95f * s, 1.05f * s, .95f * s);
        VerdigrisRender.block(VerdigrisRender.EXPOSED_CHISELED, poseStack, buffer, light, 0, 1.5f * s, 0, 1.02f * s, .35f * s, 1.02f * s);
        VerdigrisRender.block(VerdigrisRender.OXIDIZED_CUT, poseStack, buffer, light, 0, 1.85f * s, 0, .95f * s, 1.2f * s, .95f * s);
        VerdigrisRender.block(VerdigrisRender.OXIDIZED_CHISELED, poseStack, buffer, light, 0, 3.05f * s, 0, 1.25f * s, .4f * s, 1.25f * s);
        VerdigrisRender.block(VerdigrisRender.MOSS, poseStack, buffer, light, 0, 3.45f * s, 0, 1.15f * s, .15f * s, 1.15f * s);
        VerdigrisRender.vines(poseStack, buffer, light, 3.45f * s, 1.25f * s, 1.25f * s, VerdigrisRender.hanging(seed, 2.4f * s));
    }

    /** A Colonnade column (2.8 blocks): plainer, mossy cobbles at its foot. */
    private static void column(PoseStack poseStack, MultiBufferSource buffer, int light, int seed, float s) {
        VerdigrisRender.block(VerdigrisRender.MOSSY_COBBLE, poseStack, buffer, light, 0, 0, 0, 1.1f * s, .35f * s, 1.1f * s);
        VerdigrisRender.block(VerdigrisRender.WEATHERED, poseStack, buffer, light, 0, .35f * s, 0, .8f * s, 1.05f * s, .8f * s);
        VerdigrisRender.block(VerdigrisRender.OXIDIZED, poseStack, buffer, light, 0, 1.4f * s, 0, .8f * s, 1.05f * s, .8f * s);
        VerdigrisRender.block(VerdigrisRender.OXIDIZED_CUT, poseStack, buffer, light, 0, 2.45f * s, 0, 1.05f * s, .35f * s, 1.05f * s);
        VerdigrisRender.block(VerdigrisRender.MOSS_CARPET, poseStack, buffer, light, 0, 2.8f * s, 0, 1f * s, 1, 1f * s);
        VerdigrisRender.vines(poseStack, buffer, light, 2.8f * s, 1.05f * s, 1.05f * s, VerdigrisRender.hanging(seed, 1.6f * s));
    }

    /** The Lightning Rod (4.6 blocks): a carved foot, a shaft of green copper grate, a bulb and the rod, flaring as it strikes. */
    private void rod(AncientPillar pillar, PoseStack poseStack, MultiBufferSource buffer, int light, float partialTicks) {
        float since = pillar.sinceStruck(partialTicks);
        boolean flash = since >= 0 && since < 8;
        int lit = flash ? LightTexture.FULL_BRIGHT : light;
        VerdigrisRender.block(VerdigrisRender.CHISELED_BRICKS, poseStack, buffer, light, 0, 0, 0, 1.2f, .5f, 1.2f);
        VerdigrisRender.block(VerdigrisRender.MOSSY_BRICKS, poseStack, buffer, light, 0, .5f, 0, .95f, .4f, .95f);
        VerdigrisRender.block(VerdigrisRender.OXIDIZED_GRATE, poseStack, buffer, light, 0, .9f, 0, .7f, 2.2f, .7f);
        VerdigrisRender.block(VerdigrisRender.OXIDIZED_CUT, poseStack, buffer, light, 0, 3.1f, 0, .95f, .3f, .95f);
        VerdigrisRender.block(flash ? BULB_LIT : BULB, poseStack, buffer, lit, 0, 3.4f, 0, .8f, .8f, .8f);
        VerdigrisRender.block(ROD, poseStack, buffer, lit, 0, 4.2f, 0, 1, .55f, 1);
        VerdigrisRender.vines(poseStack, buffer, light, 3.1f, .95f, .95f, VerdigrisRender.hanging(pillar.getId(), 2.2f));
        // A glow on the rod: faint while it waits, bright as it strikes
        float glow = flash ? 1 - since / 8 : .25f + .1f * Mth.sin((pillar.tickCount + partialTicks) * .3f);
        poseStack.pushPose();
        poseStack.translate(0, 4.55f, 0);
        poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
        RenderHelper.billboard(buffer.getBuffer(RenderType.eyes(RuneRender.GLOW)), poseStack.last(), flash ? 1.4f : .6f,
                RuneRender.additive(flash ? 0xDDFFF4 : VerdigrisFx.VERDIGRIS, glow), LightTexture.FULL_BRIGHT);
        poseStack.popPose();
    }

    /** Broken earth and mossy stones thrown up around its foot. */
    private static void rubble(PoseStack poseStack, MultiBufferSource buffer, int light, int seed, float s, float risen) {
        VerdigrisRender.block(VerdigrisRender.COARSE_DIRT, poseStack, buffer, light, 0, -.04f, 0, 1.9f * s * risen, .08f, 1.9f * s * risen);
        for (int i = 0; i < 4; i++) {
            float a = (i + RuneRender.hash(seed + i)) * Mth.HALF_PI;
            float d = (.95f + .3f * RuneRender.hash(seed * 7 + i)) * s;
            float size = (.25f + .15f * RuneRender.hash(seed * 11 + i)) * s * risen;
            RuneRender.block(i % 2 == 0 ? VerdigrisRender.MOSSY_COBBLE : VerdigrisRender.CRACKED_BRICKS, poseStack, buffer, light,
                    Mth.cos(a) * d, -.08f, Mth.sin(a) * d, size, size, size, 20 * Mth.sin(a * 3), 20 * Mth.cos(a * 2));
        }
    }

    @Override
    public boolean shouldRender(AncientPillar pillar, Frustum frustum, double x, double y, double z) {
        return frustum.isVisible(pillar.getBoundingBox().inflate(1.5, 2, 1.5).expandTowards(0, pillar.height(), 0));
    }

    @Override
    public ResourceLocation getTextureLocation(AncientPillar pillar) {
        return RuneRender.GLOW;
    }
}
