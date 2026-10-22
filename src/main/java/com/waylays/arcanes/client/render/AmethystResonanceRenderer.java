package com.waylays.arcanes.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.waylays.arcanes.entity.gem.AmethystResonance;
import com.waylays.arcanes.spells.gem.Gem;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/**
 * Amethyst Resonance: eight clusters of amethyst crystals bursting out of the ground in a ring (a budding block with
 * small clusters at its foot, three to five great crystal points leaning out), glowing from within; each time they
 * ring they quiver and flare, and a ring of light rolls out across the ground.
 */
public class AmethystResonanceRenderer extends EntityRenderer<AmethystResonance> {
    public AmethystResonanceRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(AmethystResonance ring, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        float age = ring.tickCount + partialTicks;
        float rise = Mth.clamp(age / AmethystResonance.RISE, 0, 1);
        rise = rise * (1 + .15f * Mth.sin(rise * Mth.PI));
        float end = Mth.clamp((AmethystResonance.end() - age) / 6, 0, 1);
        // How hard they ring now: a flare at each pulse, dying away
        float ringing = 0;
        for (int k = 1; k <= AmethystResonance.PULSES; k++) {
            float since = age - AmethystResonance.pulseAt(k);
            if (since >= 0) {
                ringing = Math.max(ringing, Math.max(0, 1 - since / 14));
            }
        }
        int seed = ring.getId() * 41;
        for (int i = 0; i < AmethystResonance.CLUSTERS; i++) {
            Vec3 c = ring.cluster(i);
            float out = (float) Mth.atan2(c.x, c.z);
            float quiver = ringing * .06f * Mth.sin(age * 3.1f + i);
            poseStack.pushPose();
            poseStack.translate(c.x + quiver, -1.6f * (1 - rise), c.z - quiver);
            poseStack.mulPose(Axis.YP.rotation(out));
            // The budding block, sunk in the ground, and small clusters at its foot
            RuneRender.block(Blocks.BUDDING_AMETHYST.defaultBlockState(), poseStack, buffer, packedLight, 0, -.7f, 0, .9f, .9f, .9f, 0, 0);
            RuneRender.block(Blocks.AMETHYST_CLUSTER.defaultBlockState(), poseStack, buffer, packedLight, -.45f, 0, .2f, .7f, .7f, .7f, 0, 0);
            RuneRender.block(Blocks.LARGE_AMETHYST_BUD.defaultBlockState(), poseStack, buffer, packedLight, .45f, 0, -.1f, .6f, .6f, .6f, 0, 0);
            // The great points, leaning out
            int points = 3 + (int) (RuneRender.hash(seed + i) * 3);
            for (int k = 0; k < points; k++) {
                float h = RuneRender.hash(seed + i * 13 + k);
                float size = (.9f + 1.1f * h) * end;
                poseStack.pushPose();
                poseStack.translate((k - (points - 1) * .5f) * .28f, size * .5f, 0);
                poseStack.mulPose(Axis.XP.rotationDegrees(18 + 22 * h));
                poseStack.mulPose(Axis.ZP.rotationDegrees((k - (points - 1) * .5f) * 14));
                poseStack.scale(.8f, 1.6f, .8f);
                GemRender.gem(poseStack, buffer, GemRender.Cut.CRYSTAL, GlyphRender.whiten(Gem.AMETHYST.color, ringing * .4f), size * .6f, .75f, age + k * 9 + i * 5);
                poseStack.popPose();
            }
            poseStack.popPose();
            GemRender.glow(poseStack, buffer, c.add(0, 1 * rise, 0), Gem.AMETHYST.color, 1.2f + ringing, (.3f + .6f * ringing) * end);
        }
        // The waves of sound rolling out
        for (int k = 1; k <= AmethystResonance.PULSES; k++) {
            float r = ring.wave(k, age);
            if (r <= 0) {
                continue;
            }
            float t = r / ring.getRange();
            poseStack.pushPose();
            poseStack.translate(0, .08 + .02 * k, 0);
            poseStack.mulPose(Axis.YP.rotationDegrees(age * 2));
            RenderHelper.groundQuad(buffer.getBuffer(RenderType.eyes(RuneRender.RING_GLOW)), poseStack.last(), r,
                    RuneRender.additive(Gem.AMETHYST.light, (1 - t) * .9f), GlyphRender.FULL);
            poseStack.translate(0, .9 * (1 - t), 0);
            RenderHelper.groundQuad(buffer.getBuffer(RenderType.eyes(RuneRender.RING_GLOW)), poseStack.last(), r * .97f,
                    RuneRender.additive(Gem.AMETHYST.color, (1 - t) * .5f), GlyphRender.FULL);
            poseStack.popPose();
        }
        GemRender.sigil(poseStack, buffer, Gem.AMETHYST.color, AmethystResonance.RING + 1.2f, .5f * rise * end + .4f * ringing, age * .6f);
        super.render(ring, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    @Override
    public boolean shouldRender(AmethystResonance ring, Frustum frustum, double x, double y, double z) {
        return frustum.isVisible(ring.getBoundingBox().inflate(ring.getRange() + 2, 4, ring.getRange() + 2));
    }

    @Override
    public ResourceLocation getTextureLocation(AmethystResonance ring) {
        return GemRender.FACET;
    }
}
