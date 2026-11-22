package com.waylays.cinematic.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.waylays.cinematic.studio.ActorEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.CustomHeadLayer;
import net.minecraft.client.renderer.entity.layers.ElytraLayer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/** Draws actors like players, with their own skin (any resolution) and the wide or slim arms. */
public class ActorRenderer extends EntityRenderer<ActorEntity> {
    private final Body wide, slim;

    public ActorRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        wide = new Body(ctx, false);
        slim = new Body(ctx, true);
        shadowRadius = 0.5f;
    }

    static ResourceLocation skin(ActorEntity e) {
        ResourceLocation rl = SkinCache.get(e.skin());
        return rl != null ? rl : DefaultPlayerSkin.getDefaultSkin();
    }

    @Override
    public ResourceLocation getTextureLocation(ActorEntity e) {
        return skin(e);
    }

    @Override
    public Vec3 getRenderOffset(ActorEntity e, float partialTick) {
        return e.isCrouching() ? new Vec3(0, -0.125, 0) : Vec3.ZERO;
    }

    @Override
    public void render(ActorEntity e, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
        if (ClientStudio.hidden(e)) return;
        (e.slim() ? slim : wide).render(e, yaw, partialTick, pose, buffers, light);
    }

    static final class Body extends LivingEntityRenderer<ActorEntity, ActorModel> {
        Body(EntityRendererProvider.Context ctx, boolean slim) {
            super(ctx, new ActorModel(ctx.bakeLayer(slim ? ModelLayers.PLAYER_SLIM : ModelLayers.PLAYER), slim), 0.5f);
            addLayer(new HumanoidArmorLayer<>(this,
                    new HumanoidModel<>(ctx.bakeLayer(slim ? ModelLayers.PLAYER_SLIM_INNER_ARMOR : ModelLayers.PLAYER_INNER_ARMOR)),
                    new HumanoidModel<>(ctx.bakeLayer(slim ? ModelLayers.PLAYER_SLIM_OUTER_ARMOR : ModelLayers.PLAYER_OUTER_ARMOR)),
                    ctx.getModelManager()));
            addLayer(new ItemInHandLayer<>(this, ctx.getItemInHandRenderer()));
            addLayer(new CustomHeadLayer<>(this, ctx.getModelSet(), ctx.getItemInHandRenderer()));
            addLayer(new ElytraLayer<>(this, ctx.getModelSet()));
        }

        @Override
        public ResourceLocation getTextureLocation(ActorEntity e) {
            return skin(e);
        }

        @Override
        public void render(ActorEntity e, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
            ActorModel m = getModel();
            m.crouching = e.isCrouching();
            HumanoidModel.ArmPose main = armPose(e, InteractionHand.MAIN_HAND);
            HumanoidModel.ArmPose off = armPose(e, InteractionHand.OFF_HAND);
            if (main.isTwoHanded()) off = e.getOffhandItem().isEmpty() ? HumanoidModel.ArmPose.EMPTY : HumanoidModel.ArmPose.ITEM;
            m.rightArmPose = main;
            m.leftArmPose = off;
            super.render(e, yaw, partialTick, pose, buffers, light);
        }

        private static HumanoidModel.ArmPose armPose(ActorEntity e, InteractionHand hand) {
            ItemStack st = e.getItemInHand(hand);
            if (st.isEmpty()) return HumanoidModel.ArmPose.EMPTY;
            if (e.isUsingItem() && e.getUsedItemHand() == hand) {
                switch (st.getUseAnimation()) {
                    case BLOCK:
                        return HumanoidModel.ArmPose.BLOCK;
                    case BOW:
                        return HumanoidModel.ArmPose.BOW_AND_ARROW;
                    case SPEAR:
                        return HumanoidModel.ArmPose.THROW_SPEAR;
                    case CROSSBOW:
                        return HumanoidModel.ArmPose.CROSSBOW_CHARGE;
                    case SPYGLASS:
                        return HumanoidModel.ArmPose.SPYGLASS;
                    case TOOT_HORN:
                        return HumanoidModel.ArmPose.TOOT_HORN;
                    default:
                        break;
                }
            } else if (!e.swinging && st.getItem() instanceof CrossbowItem && CrossbowItem.isCharged(st)) {
                return HumanoidModel.ArmPose.CROSSBOW_HOLD;
            }
            return HumanoidModel.ArmPose.ITEM;
        }

        @Override
        protected void scale(ActorEntity e, PoseStack pose, float partialTick) {
            pose.scale(0.9375f, 0.9375f, 0.9375f);
        }

        @Override
        protected void setupRotations(ActorEntity e, PoseStack pose, float age, float bodyYaw, float partialTick) {
            float swim = e.getSwimAmount(partialTick);
            super.setupRotations(e, pose, age, bodyYaw, partialTick);
            if (e.isFallFlying()) {
                float t = e.getFallFlyingTicks() + partialTick;
                float k = Mth.clamp(t * t / 100f, 0f, 1f);
                pose.mulPose(Axis.XP.rotationDegrees(k * (-90f - e.getXRot())));
            } else if (swim > 0f) {
                float target = e.isInWater() ? -90f - e.getXRot() : -90f;
                pose.mulPose(Axis.XP.rotationDegrees(Mth.lerp(swim, 0f, target)));
                if (e.isVisuallySwimming()) pose.translate(0f, -1f, 0.3f);
            }
            float in = Math.min(1f, (e.emoteTicks() + partialTick) / 5f);
            float drop = EmotePoses.drop(e.emote()) * in + EmotePoses.drop(e.prevEmote()) * (e.prevEmote() != 0 ? 1f - in : 0f);
            if (drop > 0f) pose.translate(0f, -drop, 0f);
        }

        @Override
        protected boolean shouldShowName(ActorEntity e) {
            return false;
        }
    }
}
