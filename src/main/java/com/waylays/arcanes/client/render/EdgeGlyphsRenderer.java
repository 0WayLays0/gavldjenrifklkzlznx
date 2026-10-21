package com.waylays.arcanes.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.waylays.arcanes.entity.rune.EdgeGlyphs;
import com.waylays.arcanes.spells.rune.RuneFx;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/** The Edge Rune on its bearer: three runes circling the weapon hand, trailing a faint glow. */
public class EdgeGlyphsRenderer extends EntityRenderer<EdgeGlyphs> {
    private static final int FULL = LightTexture.FULL_BRIGHT;

    public EdgeGlyphsRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(EdgeGlyphs glyphs, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        LivingEntity bearer = glyphs.getTarget();
        if (bearer == null || bearer.isInvisible()) {
            return;
        }
        float age = glyphs.tickCount + partialTicks;
        Vec3 own = new Vec3(Mth.lerp(partialTicks, glyphs.xOld, glyphs.getX()), Mth.lerp(partialTicks, glyphs.yOld, glyphs.getY()), Mth.lerp(partialTicks, glyphs.zOld, glyphs.getZ()));
        // The right hand: beside the body, half way up, a little ahead
        float body = Mth.rotLerp(partialTicks, bearer.yBodyRotO, bearer.yBodyRot) * Mth.DEG_TO_RAD;
        Vec3 forward = new Vec3(-Mth.sin(body), 0, Mth.cos(body));
        Vec3 right = new Vec3(-forward.z, 0, forward.x);
        Vec3 hand = bearer.getPosition(partialTicks).subtract(own)
                .add(right.scale(bearer.getBbWidth() * .6 + .1)).add(forward.scale(.15)).add(0, bearer.getBbHeight() * .42, 0);
        float appear = Mth.clamp(age / 8, 0, 1);
        for (int i = 0; i < 3; i++) {
            float a = age * .18f + i * Mth.TWO_PI / 3;
            Vec3 p = hand.add(right.scale(Mth.cos(a) * .32)).add(0, Mth.sin(a) * .32, 0).add(forward.scale(Mth.sin(a) * .12));
            poseStack.pushPose();
            poseStack.translate(p.x, p.y, p.z);
            poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
            RenderHelper.billboard(buffer.getBuffer(RenderType.eyes(RuneRender.GLOW)), poseStack.last(), .16f, RuneRender.additive(RuneFx.DEEP, .7f * appear), FULL);
            RuneRender.glyph(buffer.getBuffer(RenderType.eyes(RuneRender.GLYPHS)), poseStack.last(), glyphs.getId() + i * 4, .2f,
                    RuneRender.additive(RuneFx.SILVER, (.75f + .25f * Mth.sin(age * .3f + i)) * appear), FULL);
            poseStack.popPose();
        }
        super.render(glyphs, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    @Override
    public boolean shouldRender(EdgeGlyphs entity, Frustum frustum, double x, double y, double z) {
        return true;
    }

    @Override
    public ResourceLocation getTextureLocation(EdgeGlyphs entity) {
        return RuneRender.GLYPHS;
    }
}
