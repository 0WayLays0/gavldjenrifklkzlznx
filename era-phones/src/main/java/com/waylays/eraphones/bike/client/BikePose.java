package com.waylays.eraphones.bike.client;

import com.waylays.eraphones.bike.BikeEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;

/**
 * A rider on the bicycle: sitting up on the long seat, hands on the grips of the high bars, the legs going round
 * with the pedals (a leg has no knee: it swings to follow its pedal, furthest forward as the pedal comes round in front).
 */
public final class BikePose {
    private BikePose() {}

    public static void apply(HumanoidModel<?> model, LivingEntity rider, float ageInTicks) {
        if (!(rider.getVehicle() instanceof BikeEntity bike)) {
            return;
        }
        float pt = Mth.clamp(ageInTicks - rider.tickCount, 0, 1);
        float crank = Mth.lerp(pt, bike.crankO, bike.crank);
        // Sitting on the seat, leaning a little toward the bars
        model.body.xRot = .22f;
        // Hands forward on the grips, a little wide
        model.rightArm.xRot = -1.02f;
        model.leftArm.xRot = -1.02f;
        model.rightArm.yRot = 0;
        model.leftArm.yRot = 0;
        model.rightArm.zRot = .1f;
        model.leftArm.zRot = -.1f;
        // The right foot on the drive side's pedal (up at rest), the left on the other
        model.rightLeg.xRot = -(.4f + .24f * Mth.sin(crank));
        model.leftLeg.xRot = -(.4f + .24f * Mth.sin(crank + Mth.PI));
        model.rightLeg.yRot = .04f;
        model.leftLeg.yRot = -.04f;
        model.rightLeg.zRot = .03f;
        model.leftLeg.zRot = -.03f;
        model.hat.copyFrom(model.head);
    }
}
