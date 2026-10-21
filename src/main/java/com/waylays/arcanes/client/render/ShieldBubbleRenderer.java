package com.waylays.arcanes.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.waylays.arcanes.ArcanesOublies;
import com.waylays.arcanes.entity.explosion.ShieldBubble;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * Reactive Shield: a crackling hexagonal shell around the bearer. Violet at full charge, orange then red as it is
 * spent, with a white flash each time it explodes.
 */
public class ShieldBubbleRenderer extends EntityRenderer<ShieldBubble> {
    private static final ResourceLocation SHELL = ArcanesOublies.id("textures/entity/shield_bubble.png");

    public ShieldBubbleRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(ShieldBubble entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        LivingEntity owner = entity.getOwner();
        Minecraft minecraft = Minecraft.getInstance();
        // From inside, in first person, the shell would only hide the view
        if (owner == null || (owner == minecraft.getCameraEntity() && minecraft.options.getCameraType().isFirstPerson())) {
            return;
        }
        float age = entity.tickCount + partialTicks;
        float appear = Mth.clamp(age / 6f, 0f, 1f);
        float flash = Mth.clamp(1 - (age - entity.getFlashTick()) / 8f, 0f, 1f);
        float flicker = .8f + Mth.sin(age * 1.9f) * .1f + Mth.sin(age * 4.7f) * .07f;
        float size = owner.getBbHeight() * 1.45f * appear * (1 + flash * .25f);
        float r, g, b;
        switch (entity.getCharges()) {
            case 1 -> { r = 1f; g = .25f; b = .15f; }
            case 2 -> { r = 1f; g = .55f; b = .2f; }
            default -> { r = .7f; g = .35f; b = 1f; }
        }
        r = Mth.lerp(flash, r * flicker, 1f);
        g = Mth.lerp(flash, g * flicker, 1f);
        b = Mth.lerp(flash, b * flicker, 1f);

        // Stick to the bearer's interpolated position, not the bubble's own (one tick behind)
        Vec3 ownerPos = owner.getPosition(partialTicks);
        Vec3 selfPos = entity.getPosition(partialTicks);
        poseStack.pushPose();
        poseStack.translate(ownerPos.x - selfPos.x, ownerPos.y - selfPos.y + owner.getBbHeight() * .5f, ownerPos.z - selfPos.z);
        poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
        BlastEffectRenderer.layer(buffer, SHELL, poseStack, size, age * 3f, BlastEffectRenderer.rgb(r, g, b));
        BlastEffectRenderer.layer(buffer, SHELL, poseStack, size * .96f, -age * 5f + 30f, BlastEffectRenderer.rgb(r * .5f, g * .5f, b * .5f));
        if (flash > 0) {
            BlastEffectRenderer.layer(buffer, BlastEffectRenderer.RING, poseStack, size * (1 + (1 - flash) * 1.5f), 0, BlastEffectRenderer.rgb(flash, flash, flash));
        }
        poseStack.popPose();
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(ShieldBubble entity) {
        return SHELL;
    }
}
