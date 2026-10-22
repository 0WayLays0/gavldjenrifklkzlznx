package com.waylays.arcanes.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.waylays.arcanes.entity.gem.GemShard;
import com.waylays.arcanes.spells.gem.Gem;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/**
 * A flung gem shard: a star of lapis (a little block of lapis flecked with gold) or a crystal of its gem, spinning,
 * a streak of its light behind it.
 */
public class GemShardRenderer extends EntityRenderer<GemShard> {
    public GemShardRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(GemShard shard, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        float age = shard.tickCount + partialTicks;
        Gem gem = shard.getGem();
        Vec3 mid = new Vec3(0, shard.getBbHeight() * .5, 0);
        Vec3 v = shard.getDeltaMovement();
        if (v.lengthSqr() > 1.0E-4) {
            GemRender.ray(poseStack, buffer, mid, mid.subtract(v.normalize().scale(1.6)), gem == Gem.LAPIS ? gem.light : gem.color, .1f, .8f, age);
        }
        poseStack.pushPose();
        poseStack.translate(mid.x, mid.y, mid.z);
        poseStack.mulPose(Axis.YP.rotationDegrees(age * 23));
        poseStack.mulPose(Axis.XP.rotationDegrees(age * 31));
        if (gem == Gem.LAPIS) {
            RuneRender.block(Blocks.LAPIS_BLOCK.defaultBlockState(), poseStack, buffer, GlyphRender.FULL, 0, -.15f, 0, .3f, .3f, .3f, 0, 0);
            GemRender.sparkles(poseStack, buffer, GemRender.Cut.CRYSTAL, gem.light, .5f, age * 3, false, shard.getId());
        } else {
            GemRender.jewel(poseStack, buffer, GemRender.Cut.CRYSTAL, gem.color, .45f, .85f, age * 3, .7f, gem == Gem.DIAMOND, shard.getId());
        }
        poseStack.popPose();
        GemRender.glow(poseStack, buffer, mid, gem == Gem.LAPIS ? gem.light : gem.color, .5f, .5f);
        super.render(shard, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(GemShard shard) {
        return GemRender.FACET;
    }
}
