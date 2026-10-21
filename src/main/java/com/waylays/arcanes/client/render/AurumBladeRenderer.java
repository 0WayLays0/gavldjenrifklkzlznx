package com.waylays.arcanes.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.waylays.arcanes.ArcanesOublies;
import com.waylays.arcanes.entity.gold.AurumBladeEntity;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Aurum Blade: a giant full-bright golden sword that forms in the sky, turns to face the caster, plunges point first
 * into the ground and dissolves. A rune circle marks the strike point while it hovers.
 */
public class AurumBladeRenderer extends EntityRenderer<AurumBladeEntity> {
    private static final ResourceLocation CIRCLE = ArcanesOublies.id("textures/entity/gold_circle.png");
    private static final ResourceLocation GLOW = GoldItemProjectileRenderer.GLOW;
    private static final float SWORD_SCALE = 4.5f;
    /** Distance from the item's centre to its tip, in blocks, once scaled. */
    private static final float TIP = .70f * SWORD_SCALE;
    private static final float SINK = .9f;

    private final ItemRenderer itemRenderer;
    private final ItemStack sword = new ItemStack(Items.GOLDEN_SWORD);

    public AurumBladeRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.itemRenderer = context.getItemRenderer();
    }

    @Override
    public void render(AurumBladeEntity entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        float age = entity.tickCount + partialTicks;
        float formed = Mth.clamp(age / 8f, 0f, 1f);
        float plunge = Mth.clamp((age - AurumBladeEntity.PLUNGE_START) / (AurumBladeEntity.STRIKE_TICK - AurumBladeEntity.PLUNGE_START), 0f, 1f);
        float dissolve = Mth.clamp((age - (AurumBladeEntity.END_TICK - 8)) / 8f, 0f, 1f);
        float height = AurumBladeEntity.HOVER_HEIGHT * (1 - plunge * plunge);
        float bob = plunge > 0 ? 0 : Mth.sin(age * .25f) * .15f;
        float scale = formed * (1 - dissolve);

        // Strike marker: a rune circle that shrinks onto the point of impact
        if (age < AurumBladeEntity.STRIKE_TICK + 6) {
            float fade = age < AurumBladeEntity.STRIKE_TICK ? 1f : 1 - (age - AurumBladeEntity.STRIKE_TICK) / 6f;
            int a = (int) (255 * fade * formed);
            poseStack.pushPose();
            poseStack.translate(0, .03, 0);
            poseStack.mulPose(Axis.YP.rotationDegrees(-age * 4f));
            RenderHelper.groundQuad(buffer.getBuffer(RenderType.eyes(CIRCLE)), poseStack.last(), 2.6f - plunge * .8f, (a << 24) | (a << 16) | (a << 8) | a, LightTexture.FULL_BRIGHT);
            poseStack.popPose();
        }

        if (scale > 0) {
            poseStack.pushPose();
            poseStack.translate(0, height + bob + TIP - SINK, 0);

            // Halo around the blade
            poseStack.pushPose();
            poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
            float halo = 5.5f * scale * (1 + Mth.sin(age * .6f) * .08f);
            poseStack.scale(halo, halo, halo);
            RenderHelper.billboard(buffer.getBuffer(RenderType.eyes(GLOW)), poseStack.last(), .5f, 0xB0B0B0B0, LightTexture.FULL_BRIGHT);
            poseStack.popPose();

            // Spins while forming, settles facing the caster, then points straight down
            float spin = (1 - Mth.clamp(age / AurumBladeEntity.PLUNGE_START, 0f, 1f)) * 540f;
            poseStack.mulPose(Axis.YP.rotationDegrees(-entity.getYRot() + spin));
            poseStack.mulPose(Axis.ZP.rotationDegrees(135f));
            poseStack.scale(SWORD_SCALE * scale, SWORD_SCALE * scale, SWORD_SCALE * scale);
            itemRenderer.renderStatic(sword, ItemDisplayContext.FIXED, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, poseStack, buffer, entity.level(), entity.getId());
            poseStack.popPose();
        }

        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(AurumBladeEntity entity) {
        return GLOW;
    }
}
