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
    /** By bicycle (chopper, racer, town, BMX): how far forward the arms reach (radians from hanging down), how far in. */
    private static final float[][] ARMS = {{.80f, .09f}, {.92f, .2f}, {.78f, .17f}, {.82f, .13f}};

    private BikePose() {}

    public static void apply(HumanoidModel<?> model, LivingEntity rider, float ageInTicks) {
        if (!(rider.getVehicle() instanceof BikeEntity bike)) {
            return;
        }
        float pt = Mth.clamp(ageInTicks - rider.tickCount, 0, 1);
        float crank = Mth.lerp(pt, bike.crankO, bike.crank);
        // Sitting on the seat, leaning a little toward the bars
        model.body.xRot = .22f;
        // Hands on the grips: each bicycle's bars are at their own height, reach and width (worked out from the shoulder
        // to the grip, an arm being 0.7 block long): forward and down to them, a little inward to the narrower ones
        float[] arm = ARMS[bike.kind()];
        model.rightArm.xRot = -arm[0];
        model.leftArm.xRot = -arm[0];
        model.rightArm.yRot = 0;
        model.leftArm.yRot = 0;
        model.rightArm.zRot = -arm[1];
        model.leftArm.zRot = arm[1];
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
