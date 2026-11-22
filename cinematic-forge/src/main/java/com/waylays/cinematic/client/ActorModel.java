package com.waylays.cinematic.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.waylays.cinematic.studio.ActorEntity;
import com.waylays.cinematic.studio.BodyPart;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * The player model with elbows and knees, plus the take's gestures and the pose editor's joint turns. Turning the
 * torso turns the head and the arms with it, around the waist.
 */
public class ActorModel extends PlayerModel<ActorEntity> {
    final ModelPart rightForearm, leftForearm, rightShin, leftShin;
    private final ModelPart rightSleeveLower, leftSleeveLower, rightPantsLower, leftPantsLower;
    private final ModelPart[] all;
    private final float[] offset = new float[3];

    public ActorModel(ModelPart root, boolean slim) {
        super(root, slim);
        rightForearm = rightArm.getChild("right_forearm");
        leftForearm = leftArm.getChild("left_forearm");
        rightShin = rightLeg.getChild("right_shin");
        leftShin = leftLeg.getChild("left_shin");
        rightSleeveLower = rightSleeve.getChild("right_sleeve_lower");
        leftSleeveLower = leftSleeve.getChild("left_sleeve_lower");
        rightPantsLower = rightPants.getChild("right_pants_lower");
        leftPantsLower = leftPants.getChild("left_pants_lower");
        all = new ModelPart[] {head, hat, body, jacket, rightArm, leftArm, rightLeg, leftLeg, rightSleeve, leftSleeve, rightPants, leftPants,
                rightForearm, leftForearm, rightShin, leftShin, rightSleeveLower, leftSleeveLower, rightPantsLower, leftPantsLower};
    }

    @Override
    public void setupAnim(ActorEntity e, float limbSwing, float limbAmount, float age, float headYaw, float headPitch) {
        // The vanilla animation does not reset every angle each frame: start from a straight pose so nothing adds up
        for (ModelPart p : all) {
            p.xRot = 0f;
            p.yRot = 0f;
            p.zRot = 0f;
        }
        super.setupAnim(e, limbSwing, limbAmount, age, headYaw, headPitch);
        float partial = age - e.tickCount;
        float in = Math.min(1f, (e.emoteTicks() + partial) / 5f);
        if (e.prevEmote() != 0 && in < 1f) EmotePoses.apply(this, e.prevEmote(), 1f - in, age);
        if (e.emote() != 0) EmotePoses.apply(this, e.emote(), in, age);

        float time = ClientStudio.actorTime(e.actorId(), partial);
        turn(e, BodyPart.HEAD, head, time);
        turn(e, BodyPart.RIGHT_ARM, rightArm, time);
        turn(e, BodyPart.LEFT_ARM, leftArm, time);
        turn(e, BodyPart.RIGHT_LEG, rightLeg, time);
        turn(e, BodyPart.LEFT_LEG, leftLeg, time);
        turn(e, BodyPart.RIGHT_FOREARM, rightForearm, time);
        turn(e, BodyPart.LEFT_FOREARM, leftForearm, time);
        turn(e, BodyPart.RIGHT_SHIN, rightShin, time);
        turn(e, BodyPart.LEFT_SHIN, leftShin, time);
        if (ClientStudio.poseOffset(e.actorId(), BodyPart.BODY, time, offset)) {
            bendTorso(offset[0] * Mth.DEG_TO_RAD, offset[1] * Mth.DEG_TO_RAD, offset[2] * Mth.DEG_TO_RAD);
        }

        hat.copyFrom(head);
        jacket.copyFrom(body);
        leftSleeve.copyFrom(leftArm);
        rightSleeve.copyFrom(rightArm);
        leftPants.copyFrom(leftLeg);
        rightPants.copyFrom(rightLeg);
        rightSleeveLower.copyFrom(rightForearm);
        leftSleeveLower.copyFrom(leftForearm);
        rightPantsLower.copyFrom(rightShin);
        leftPantsLower.copyFrom(leftShin);
    }

    private void turn(ActorEntity e, BodyPart part, ModelPart p, float time) {
        if (ClientStudio.poseOffset(e.actorId(), part, time, offset)) {
            p.xRot += offset[0] * Mth.DEG_TO_RAD;
            p.yRot += offset[1] * Mth.DEG_TO_RAD;
            p.zRot += offset[2] * Mth.DEG_TO_RAD;
        }
    }

    /** Turns the torso, the head and the arms together around the waist (the legs stay). */
    private void bendTorso(float x, float y, float z) {
        Quaternionf q = new Quaternionf().rotationZYX(z, y, x);
        Vector3f waist = new Vector3f(body.x, body.y + 12f, body.z);
        for (ModelPart p : new ModelPart[] {head, body, rightArm, leftArm}) {
            Vector3f pivot = new Vector3f(p.x, p.y, p.z).sub(waist);
            q.transform(pivot);
            pivot.add(waist);
            p.x = pivot.x;
            p.y = pivot.y;
            p.z = pivot.z;
            Quaternionf r = new Quaternionf(q).mul(new Quaternionf().rotationZYX(p.zRot, p.yRot, p.xRot));
            setEuler(p, r);
        }
    }

    /** Back from a rotation to the part's angles (applied by Minecraft as Z, then Y, then X). */
    private static void setEuler(ModelPart p, Quaternionf q) {
        float qx = q.x, qy = q.y, qz = q.z, qw = q.w;
        float r00 = 1 - 2 * (qy * qy + qz * qz);
        float r10 = 2 * (qx * qy + qw * qz);
        float r20 = 2 * (qx * qz - qw * qy);
        float r21 = 2 * (qy * qz + qw * qx);
        float r22 = 1 - 2 * (qx * qx + qy * qy);
        p.yRot = (float) Math.asin(Mth.clamp(-r20, -1f, 1f));
        p.xRot = (float) Math.atan2(r21, r22);
        p.zRot = (float) Math.atan2(r10, r00);
    }

    /** Items are held at the end of the forearm. */
    @Override
    public void translateToHand(HumanoidArm arm, PoseStack pose) {
        super.translateToHand(arm, pose);
        (arm == HumanoidArm.RIGHT ? rightForearm : leftForearm).translateAndRotate(pose);
        pose.translate(0f, -4f / 16f, 0f);
    }
}
