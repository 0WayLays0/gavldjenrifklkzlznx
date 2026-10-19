package com.waylays.arcanes.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.waylays.arcanes.ArcanesOublies;
import com.waylays.arcanes.entity.physical.Missile;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.ArrowRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

/**
 * Fighters' missiles: arrows and crossbow bolts as vanilla arrows; a throwing knife (spinning end over end in the air)
 * and a javelin as two crossed pictures along the flight; a thrown axe as the axe itself, tumbling; a harpoon as an
 * arrow trailing its rope back to the hand of whoever shot it.
 */
public class MissileRenderer extends ArrowRenderer<Missile> {
    private static final ResourceLocation ARROW = new ResourceLocation("textures/entity/projectiles/arrow.png");
    private static final ResourceLocation KNIFE = ArcanesOublies.id("textures/entity/throwing_knife.png");
    private static final ResourceLocation JAVELIN = ArcanesOublies.id("textures/entity/javelin.png");
    private static final int ROPE = 0xFF8A6A3E;
    private static final int ROPE_SEGMENTS = 16;

    private final ItemRenderer itemRenderer;

    public MissileRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.itemRenderer = context.getItemRenderer();
    }

    @Override
    public ResourceLocation getTextureLocation(Missile entity) {
        return ARROW;
    }

    @Override
    public boolean shouldRender(Missile entity, Frustum frustum, double x, double y, double z) {
        // The rope reaches past the harpoon's own box
        return entity.getKind() == Missile.Kind.HARPOON || super.shouldRender(entity, frustum, x, y, z);
    }

    @Override
    public void render(Missile entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        switch (entity.getKind()) {
            case ARROW, BOLT -> super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
            case HARPOON -> {
                super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
                rope(entity, partialTicks, poseStack, buffer);
            }
            case KNIFE -> picture(entity, partialTicks, poseStack, buffer, packedLight, KNIFE, .55f, .14f, 0, .375f, 1, .625f, 50);
            case JAVELIN -> picture(entity, partialTicks, poseStack, buffer, packedLight, JAVELIN, 1.7f, .21f, 0, 0, 1, 1, 0);
            case AXE -> axe(entity, partialTicks, poseStack, buffer, packedLight);
        }
    }

    /** Turns the pose to the flight: +X ahead, +Y up (as vanilla arrows). */
    private static void alongFlight(Missile entity, float partialTicks, PoseStack poseStack) {
        poseStack.mulPose(Axis.YP.rotationDegrees(Mth.lerp(partialTicks, entity.yRotO, entity.getYRot()) - 90));
        poseStack.mulPose(Axis.ZP.rotationDegrees(Mth.lerp(partialTicks, entity.xRotO, entity.getXRot())));
    }

    /** Two crossed pictures along the flight, spinning {@code spin} degrees a tick about the side axis while in the air. */
    private static void picture(Missile entity, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int light, ResourceLocation texture,
                                float length, float height, float u0, float v0, float u1, float v1, float spin) {
        poseStack.pushPose();
        alongFlight(entity, partialTicks, poseStack);
        if (spin != 0 && !entity.isStuck()) {
            poseStack.mulPose(Axis.ZP.rotationDegrees(-(entity.tickCount + partialTicks) * spin));
        }
        VertexConsumer consumer = buffer.getBuffer(RenderType.entityCutoutNoCull(texture));
        for (int i = 0; i < 2; i++) {
            RenderHelper.rectUv(consumer, poseStack.last(), length, height, u0, v0, u1, v1, 0xFFFFFFFF, light, false);
            poseStack.mulPose(Axis.XP.rotationDegrees(90));
        }
        poseStack.popPose();
    }

    private void axe(Missile entity, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int light) {
        ItemStack stack = entity.getItem().isEmpty() ? new ItemStack(Items.IRON_AXE) : entity.getItem();
        poseStack.pushPose();
        alongFlight(entity, partialTicks, poseStack);
        if (!entity.isStuck()) {
            poseStack.mulPose(Axis.ZP.rotationDegrees(-(entity.tickCount + partialTicks) * 40));
        }
        // The item picture's handle down and to the left: turn it so the blade leads
        poseStack.mulPose(Axis.ZP.rotationDegrees(-45));
        poseStack.scale(1.1f, 1.1f, 1.1f);
        itemRenderer.renderStatic(stack, ItemDisplayContext.FIXED, light, OverlayTexture.NO_OVERLAY, poseStack, buffer, entity.level(), entity.getId());
        poseStack.popPose();
    }

    /** The harpoon's rope, sagging a little, from the harpoon back to the shooter's hand. */
    private static void rope(Missile entity, float partialTicks, PoseStack poseStack, MultiBufferSource buffer) {
        Entity owner = entity.getOwner();
        if (!(owner instanceof LivingEntity shooter)) {
            return;
        }
        float yaw = Mth.lerp(partialTicks, shooter.yBodyRotO, shooter.yBodyRot) * Mth.DEG_TO_RAD;
        Vec3 right = new Vec3(-Mth.cos(yaw), 0, -Mth.sin(yaw));
        Vec3 hand = shooter.getPosition(partialTicks).add(0, shooter.getBbHeight() * .62, 0).add(right.scale(.3));
        Vec3 d = hand.subtract(entity.getPosition(partialTicks));
        double sag = Math.min(1.2, d.length() * .06);
        VertexConsumer consumer = buffer.getBuffer(RenderType.lineStrip());
        PoseStack.Pose pose = poseStack.last();
        Vec3 prev = null;
        for (int i = 0; i <= ROPE_SEGMENTS; i++) {
            float t = i / (float) ROPE_SEGMENTS;
            Vec3 p = d.scale(t).subtract(0, Math.sin(Math.PI * t) * sag, 0);
            Vec3 n = prev == null ? d.normalize() : p.subtract(prev).normalize();
            consumer.vertex(pose.pose(), (float) p.x, (float) p.y, (float) p.z)
                    .color((ROPE >> 16) & 0xFF, (ROPE >> 8) & 0xFF, ROPE & 0xFF, 255)
                    .normal(pose.normal(), (float) n.x, (float) n.y, (float) n.z)
                    .endVertex();
            prev = p;
        }
    }
}
