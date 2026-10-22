package com.waylays.arcanes.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.waylays.arcanes.entity.gem.RubyHeart;
import com.waylays.arcanes.spells.gem.Gem;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/**
 * The Burning Heart: a cut ruby spinning through the air with a streak of fire behind it; set in, it beats (swelling
 * and flaring on each beat, a ring of heat rolling out), hotter and whiter each time, shaking before it bursts.
 */
public class RubyHeartRenderer extends EntityRenderer<RubyHeart> {
    public RubyHeartRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(RubyHeart heart, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        float age = heart.tickCount + partialTicks;
        float heat = heart.heat(partialTicks);
        int set = heart.getSetAt();
        Vec3 at = new Vec3(0, heart.getBbHeight() * .5, 0);
        // Set in an enemy: drawn on it, where it is now
        Entity host = set >= 0 ? heart.level().getEntity(heart.getHost()) : null;
        if (host != null) {
            Vec3 on = host.getPosition(partialTicks).add(0, host.getBbHeight() * .6, 0);
            at = on.subtract(heart.getPosition(partialTicks));
        }
        float since = set < 0 ? 0 : age - set;
        float beat = set < 0 ? 0 : Math.max(0, 1 - (since % RubyHeart.BEAT_TICKS) / 6f);
        int color = GlyphRender.whiten(Gem.RUBY.color, heat * heat * .6f);
        if (set < 0) {
            Vec3 v = heart.getDeltaMovement();
            if (v.lengthSqr() > 1.0E-4) {
                GemRender.ray(poseStack, buffer, at, at.subtract(v.normalize().scale(2.2)), 0xFF5020, .18f, .7f, age);
            }
        } else if (beat > 0) {
            // A ring of heat rolling out of each beat
            float r = (1 - beat) * RubyHeart.BEAT_RADIUS * 1.4f;
            GemRender.glow(poseStack, buffer, at, Gem.RUBY.color, r + .4f, beat * .5f);
        }
        float shake = heat > .8f ? (heat - .8f) * .3f * Mth.sin(age * 5.1f) : 0;
        poseStack.pushPose();
        poseStack.translate(at.x + shake, at.y, at.z + shake * .7f);
        poseStack.mulPose(Axis.YP.rotationDegrees(age * (set < 0 ? 24 : 6)));
        poseStack.mulPose(Axis.XP.rotationDegrees(set < 0 ? age * 17 : 25));
        float size = .5f * (1 + .25f * beat + .2f * heat);
        GemRender.jewel(poseStack, buffer, GemRender.Cut.BRILLIANT, color, size, .85f, age * 3, .6f + .4f * heat + .4f * beat, false, heart.getId());
        poseStack.popPose();
        GemRender.glow(poseStack, buffer, at, 0xFF6020, .9f + 1.2f * heat + beat, .35f + .4f * heat);
        super.render(heart, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    @Override
    public boolean shouldRender(RubyHeart heart, Frustum frustum, double x, double y, double z) {
        return frustum.isVisible(heart.getBoundingBox().inflate(4));
    }

    @Override
    public ResourceLocation getTextureLocation(RubyHeart heart) {
        return GemRender.FACET;
    }
}
