package com.waylays.arcanes.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.waylays.arcanes.entity.verdigris.SporePatch;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/** Moss Spores: a round carpet of moss spreading over the ground, with tufts of moss and flowering azalea rising out of it. */
public class SporePatchRenderer extends EntityRenderer<SporePatch> {
    private static final BlockState AZALEA = Blocks.AZALEA.defaultBlockState();
    private static final BlockState FLOWERING = Blocks.FLOWERING_AZALEA.defaultBlockState();
    private static final int TUFTS = 7;

    public SporePatchRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(SporePatch patch, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        float spread = patch.spread(partialTicks);
        float radius = SporePatch.RADIUS * spread;
        if (radius < .05f) {
            return;
        }
        int reach = (int) Math.ceil(SporePatch.RADIUS);
        for (int ix = -reach; ix <= reach; ix++) {
            for (int iz = -reach; iz <= reach; iz++) {
                // A tile shrinks toward the rim, so the patch reads round
                float size = Mth.clamp(radius + .5f - Mth.sqrt(ix * ix + iz * iz), 0, 1);
                if (size > .05f) {
                    VerdigrisRender.block(VerdigrisRender.MOSS_CARPET, poseStack, buffer, packedLight, ix, .01f + ((ix + iz) & 1) * .002f, iz, size, 1, size);
                }
            }
        }
        int seed = patch.getId();
        for (int i = 0; i < TUFTS; i++) {
            float a = (i + RuneRender.hash(seed + i)) * Mth.TWO_PI / TUFTS;
            float d = radius * (.25f + .55f * RuneRender.hash(seed * 3 + i));
            float x = Mth.cos(a) * d;
            float z = Mth.sin(a) * d;
            float s = (.4f + .25f * RuneRender.hash(seed * 7 + i)) * spread;
            switch (i % 3) {
                case 0 -> VerdigrisRender.turned(VerdigrisRender.MOSS, poseStack, buffer, packedLight, x, 0, z, s * .8f, s * .5f, s * .8f, a * 40);
                case 1 -> VerdigrisRender.turned(AZALEA, poseStack, buffer, packedLight, x, 0, z, s, s, s, a * 40);
                default -> VerdigrisRender.turned(FLOWERING, poseStack, buffer, packedLight, x, 0, z, s, s, s, a * 40);
            }
        }
        super.render(patch, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    @Override
    public boolean shouldRender(SporePatch patch, Frustum frustum, double x, double y, double z) {
        return frustum.isVisible(patch.getBoundingBox().inflate(SporePatch.RADIUS + 1, 1.5, SporePatch.RADIUS + 1));
    }

    @Override
    public ResourceLocation getTextureLocation(SporePatch patch) {
        return RuneRender.GLOW;
    }
}
