package com.waylays.arcanes.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.waylays.arcanes.ArcanesOublies;
import com.waylays.arcanes.entity.book.RewriteAura;
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
 * Rewrite: three bands of glowing handwriting wind around the caster in turn, rising from the feet to above the
 * head, turning opposite ways, then fading out.
 */
public class RewriteAuraRenderer extends EntityRenderer<RewriteAura> {
    private static final ResourceLocation SCRIPT = ArcanesOublies.id("textures/entity/script_band.png");
    private static final int SIDES = 24;
    private static final int BANDS = 3;

    public RewriteAuraRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(RewriteAura entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        LivingEntity target = entity.getTarget();
        if (target == null) {
            return;
        }
        float age = entity.tickCount + partialTicks;
        Vec3 targetPos = target.getPosition(partialTicks);
        Vec3 selfPos = entity.getPosition(partialTicks);
        poseStack.pushPose();
        poseStack.translate(targetPos.x - selfPos.x, targetPos.y - selfPos.y, targetPos.z - selfPos.z);
        VertexConsumer consumer = buffer.getBuffer(RenderType.eyes(SCRIPT));
        for (int i = 0; i < BANDS; i++) {
            float t = Mth.clamp((age - i * 5) / (RewriteAura.LIFETIME - 10), 0f, 1f);
            if (t <= 0 || t >= 1) {
                continue;
            }
            float alpha = Mth.sin(t * Mth.PI);
            float radius = target.getBbWidth() * .6f + .35f + i * .1f + t * .25f;
            float y = target.getBbHeight() * (t * 1.15f - .05f);
            poseStack.pushPose();
            poseStack.translate(0, y, 0);
            poseStack.mulPose(Axis.YP.rotationDegrees((i % 2 == 0 ? 1 : -1) * age * 7f + i * 50f));
            int color = BlastEffectRenderer.rgb(alpha, alpha * .85f, alpha * .55f + .2f * alpha);
            band(consumer, poseStack.last(), radius, .32f, color);
            poseStack.popPose();
        }
        poseStack.popPose();
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    /** A ring of script, seen from outside and from inside; the text runs around three times. */
    private static void band(VertexConsumer c, PoseStack.Pose pose, float radius, float height, int argb) {
        float h = height * .5f;
        for (int k = 0; k < SIDES; k++) {
            float a0 = k * Mth.TWO_PI / SIDES;
            float a1 = (k + 1) * Mth.TWO_PI / SIDES;
            float x0 = Mth.cos(a0) * radius;
            float z0 = Mth.sin(a0) * radius;
            float x1 = Mth.cos(a1) * radius;
            float z1 = Mth.sin(a1) * radius;
            float u0 = k * 3f / SIDES;
            float u1 = (k + 1) * 3f / SIDES;
            float nx = Mth.cos((a0 + a1) * .5f);
            float nz = Mth.sin((a0 + a1) * .5f);
            int light = LightTexture.FULL_BRIGHT;
            RenderHelper.vertex(c, pose, x0, -h, z0, u0, 1, argb, light, nx, 0, nz);
            RenderHelper.vertex(c, pose, x0, h, z0, u0, 0, argb, light, nx, 0, nz);
            RenderHelper.vertex(c, pose, x1, h, z1, u1, 0, argb, light, nx, 0, nz);
            RenderHelper.vertex(c, pose, x1, -h, z1, u1, 1, argb, light, nx, 0, nz);
            RenderHelper.vertex(c, pose, x1, -h, z1, u1, 1, argb, light, -nx, 0, -nz);
            RenderHelper.vertex(c, pose, x1, h, z1, u1, 0, argb, light, -nx, 0, -nz);
            RenderHelper.vertex(c, pose, x0, h, z0, u0, 0, argb, light, -nx, 0, -nz);
            RenderHelper.vertex(c, pose, x0, -h, z0, u0, 1, argb, light, -nx, 0, -nz);
        }
    }

    @Override
    public ResourceLocation getTextureLocation(RewriteAura entity) {
        return SCRIPT;
    }
}
