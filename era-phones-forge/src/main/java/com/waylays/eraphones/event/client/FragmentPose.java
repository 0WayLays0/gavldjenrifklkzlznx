package com.waylays.eraphones.event.client;

import com.waylays.eraphones.event.StarFragment;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;

/**
 * A bearer of the star fragment: standing facing it, both arms out to it, the hands on its sides at its height (how
 * far out depends on how many stand round it, so how far they are from it); the arms straining a little, trembling
 * when there are only two; short heavy steps when it moves; the head free to look about, but not right round.
 */
public final class FragmentPose {
    /** The shoulder's height above the feet, and an arm's reach to the palm (blocks, as the player is drawn). */
    private static final float SHOULDER = 1.29f, ARM = .56f;

    private FragmentPose() {}

    public static void apply(HumanoidModel<?> model, LivingEntity bearer, float ageInTicks) {
        if (!(bearer.getVehicle() instanceof StarFragment f)) {
            return;
        }
        float pt = Mth.clamp(ageInTicks - bearer.tickCount, 0, 1);
        int n = f.getPassengers().size();
        float reach = StarFragment.reach(n);
        // From the shoulder down and out to the fragment's side: forward by its distance, down to its middle
        float down = SHOULDER - StarFragment.HELD_AT, forward = reach - .06f;
        float lift = (float) Math.atan2(forward, down);
        float walk = Mth.lerp(pt, f.walkO, f.walk), stride = Mth.lerp(pt, f.strideO, f.stride);
        float strain = (n <= 2 ? .02f : .008f) * Mth.sin(ageInTicks * 1.7f) + .04f * stride * Mth.sin(walk * 2);
        model.rightArm.xRot = -lift + strain;
        model.leftArm.xRot = -lift - strain * .7f;
        model.rightArm.yRot = -.06f;
        model.leftArm.yRot = .06f;
        // The palms on its sides, a little in
        model.rightArm.zRot = -.06f;
        model.leftArm.zRot = .06f;
        // Leaning back a little under the weight
        model.body.xRot = -.05f;
        // Short heavy steps
        float leg = Mth.cos(walk) * .42f * stride;
        model.rightLeg.xRot = leg;
        model.leftLeg.xRot = -leg;
        model.rightLeg.yRot = 0;
        model.leftLeg.yRot = 0;
        model.rightLeg.zRot = .02f;
        model.leftLeg.zRot = -.02f;
        // The head turns, but not round to the back
        model.head.yRot = Mth.clamp(model.head.yRot, -1.1f, 1.1f);
        model.hat.copyFrom(model.head);
    }
}
