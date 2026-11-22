package com.waylays.cinematic.client;

import com.waylays.cinematic.studio.ActorEntity;
import com.waylays.cinematic.studio.BodyPart;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;

/** The player model, plus the take's gestures and the pose editor's limb turns. */
public class ActorModel extends PlayerModel<ActorEntity> {
    private final float[] offset = new float[3];

    public ActorModel(ModelPart root, boolean slim) {
        super(root, slim);
    }

    @Override
    public void setupAnim(ActorEntity e, float limbSwing, float limbAmount, float age, float headYaw, float headPitch) {
        super.setupAnim(e, limbSwing, limbAmount, age, headYaw, headPitch);
        float partial = age - e.tickCount;
        float in = Math.min(1f, (e.emoteTicks() + partial) / 5f);
        if (e.prevEmote() != 0 && in < 1f) EmotePoses.apply(this, e.prevEmote(), 1f - in, age);
        if (e.emote() != 0) EmotePoses.apply(this, e.emote(), in, age);

        float time = ClientStudio.actorTime(e.actorId(), partial);
        turn(e, BodyPart.HEAD, head, time);
        turn(e, BodyPart.BODY, body, time);
        turn(e, BodyPart.RIGHT_ARM, rightArm, time);
        turn(e, BodyPart.LEFT_ARM, leftArm, time);
        turn(e, BodyPart.RIGHT_LEG, rightLeg, time);
        turn(e, BodyPart.LEFT_LEG, leftLeg, time);

        hat.copyFrom(head);
        jacket.copyFrom(body);
        leftSleeve.copyFrom(leftArm);
        rightSleeve.copyFrom(rightArm);
        leftPants.copyFrom(leftLeg);
        rightPants.copyFrom(rightLeg);
    }

    private void turn(ActorEntity e, BodyPart part, ModelPart p, float time) {
        if (ClientStudio.poseOffset(e.actorId(), part, time, offset)) {
            p.xRot += offset[0] * Mth.DEG_TO_RAD;
            p.yRot += offset[1] * Mth.DEG_TO_RAD;
            p.zRot += offset[2] * Mth.DEG_TO_RAD;
        }
    }
}
