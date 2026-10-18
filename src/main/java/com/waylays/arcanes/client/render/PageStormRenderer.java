package com.waylays.arcanes.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.waylays.arcanes.ArcanesOublies;
import com.waylays.arcanes.entity.book.PageStorm;
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
 * Page Storm: runic pages whirling around the target at different heights and speeds, tumbling as they go, their
 * runes glowing violet. They rush in from afar at the start and fly off at the end.
 */
public class PageStormRenderer extends EntityRenderer<PageStorm> {
    static final ResourceLocation PAGE = ArcanesOublies.id("textures/entity/page.png");
    static final ResourceLocation PAGE_RUNES = ArcanesOublies.id("textures/entity/page_runes.png");

    public PageStormRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(PageStorm entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        LivingEntity target = entity.getTarget();
        if (target == null) {
            return;
        }
        float age = entity.tickCount + partialTicks;
        float gather = Mth.clamp(age / PageStorm.GATHER_TICKS, 0f, 1f);
        float scatter = Mth.clamp((age - entity.getDuration()) / PageStorm.SCATTER_TICKS, 0f, 1f);
        if (scatter >= 1) {
            return;
        }
        float baseRadius = target.getBbWidth() * .8f + .7f;
        float height = target.getBbHeight() * 1.15f;
        float glow = (1 - scatter) * (.7f + .3f * Mth.sin(age * .5f));

        // Follow the target's interpolated position
        Vec3 targetPos = target.getPosition(partialTicks);
        Vec3 selfPos = entity.getPosition(partialTicks);
        poseStack.pushPose();
        poseStack.translate(targetPos.x - selfPos.x, targetPos.y - selfPos.y, targetPos.z - selfPos.z);
        int pages = entity.getPages();
        for (int i = 0; i < pages; i++) {
            float phase = i / (float) pages;
            float speed = .2f + .06f * (i % 3);
            float angle = age * speed + phase * Mth.TWO_PI;
            // Pages rush in from 3 blocks out, and fly off at the end
            float radius = Mth.lerp(gather, baseRadius + 2.5f, baseRadius) * (1 + scatter * 3) + Mth.sin(age * .15f + i) * .15f;
            float y = height * (.1f + .8f * ((phase * 1.618f + age * .004f * (1 + i % 2)) % 1f)) + Mth.sin(age * .25f + i * 2) * .12f + scatter * 1.5f;
            poseStack.pushPose();
            poseStack.translate(Mth.cos(angle) * radius, y, Mth.sin(angle) * radius);
            // Facing along the wind, tumbling
            poseStack.mulPose(Axis.YP.rotation(-angle));
            poseStack.mulPose(Axis.XP.rotationDegrees(Mth.sin(age * .3f + i) * 45f));
            poseStack.mulPose(Axis.ZP.rotationDegrees(age * (6 + i % 4 * 3) + i * 40f));
            drawPage(buffer, poseStack, .42f, .56f, packedLight, glow);
            poseStack.popPose();
        }
        poseStack.popPose();
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    /** One page: parchment (lit by the world) with its runes glowing on both sides. */
    static void drawPage(MultiBufferSource buffer, PoseStack poseStack, float width, float height, int light, float glow) {
        RenderHelper.rect(buffer.getBuffer(RenderType.entityCutoutNoCull(PAGE)), poseStack.last(), width, height, 0xFFFFFFFF, light, false);
        if (glow > 0) {
            int v = (int) (Mth.clamp(glow, 0f, 1f) * 255);
            RenderHelper.rect(buffer.getBuffer(RenderType.eyes(PAGE_RUNES)), poseStack.last(), width, height, 0xFF000000 | (v << 16) | (v << 8) | v, LightTexture.FULL_BRIGHT, true);
        }
    }

    @Override
    public ResourceLocation getTextureLocation(PageStorm entity) {
        return PAGE;
    }
}
