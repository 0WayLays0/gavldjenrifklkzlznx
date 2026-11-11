package com.waylays.eraphones.carry.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.waylays.eraphones.EraPhones;
import com.waylays.eraphones.carry.Carry;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;

/**
 * Carried in someone's arms: the one carried lies across them on their back, head on one side, legs over the other,
 * limbs slack; the carrier's arms reach forward under them.
 */
@EventBusSubscriber(modid = EraPhones.MODID, value = Dist.CLIENT)
public final class CarryPose {
    private CarryPose() {}

    /** The one carried, laid down: turned about the carrier's forward line, then onto their back. */
    @SubscribeEvent
    public static void onRender(RenderPlayerEvent.Pre event) {
        Player p = event.getEntity();
        if (!(p.getVehicle() instanceof Player carrier) || !Carry.carrying(carrier, p)) {
            return;
        }
        PoseStack pose = event.getPoseStack();
        float yaw = carrier.yBodyRot;
        float k = p.getScale();
        pose.translate(0, .9 * k, 0);
        // In the carrier's frame (forward +z, across +x): first onto the side about the forward line (the head across the
        // arms), then onto the back about the line across (the face up)
        pose.mulPose(Axis.YP.rotationDegrees(-yaw));
        pose.mulPose(Axis.XP.rotationDegrees(-90));
        pose.mulPose(Axis.ZP.rotationDegrees(90));
        pose.mulPose(Axis.YP.rotationDegrees(yaw));
        pose.translate(0, -.9 * k, 0);
    }

    public static void apply(HumanoidModel<?> model, LivingEntity e) {
        if (e instanceof Player p && p.getFirstPassenger() instanceof Player carried && Carry.carrying(p, carried)) {
            // Both arms forward, a little bent under the weight
            model.rightArm.xRot = -1.05f;
            model.leftArm.xRot = -1.05f;
            model.rightArm.yRot = -.12f;
            model.leftArm.yRot = .12f;
            model.rightArm.zRot = 0;
            model.leftArm.zRot = 0;
        } else if (e instanceof Player p && p.getVehicle() instanceof Player carrier && Carry.carrying(carrier, p)) {
            // Slack: arms hanging, legs together, the head resting
            model.rightArm.xRot = -.2f;
            model.leftArm.xRot = -.2f;
            model.rightArm.zRot = .15f;
            model.leftArm.zRot = -.15f;
            model.rightLeg.xRot = 0;
            model.leftLeg.xRot = 0;
            model.rightLeg.yRot = 0;
            model.leftLeg.yRot = 0;
            model.head.yRot = Mth.clamp(model.head.yRot, -.6f, .6f);
            model.head.xRot = Mth.clamp(model.head.xRot, -.4f, .4f);
            model.hat.copyFrom(model.head);
        }
    }
}
