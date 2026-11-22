package com.waylays.cinematic.client;

import com.waylays.cinematic.studio.Emote;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;

/** The gestures, as limb angles (radians) blended over the recorded movement. */
final class EmotePoses {
    private EmotePoses() {}

    /** How far down the whole body goes for this gesture (blocks), for sitting and kneeling. */
    static float drop(int emote) {
        return switch (Emote.byId(emote)) {
            case SIT -> 0.36f;
            case KNEEL -> 0.36f;
            default -> 0f;
        };
    }

    static void apply(ActorModel m, int emote, float w, float t) {
        if (w <= 0f) return;
        switch (Emote.byId(emote)) {
            case WAVE -> {
                set(m.rightArm, w, -2.5f, 0f, 0.35f);
                set(m.rightForearm, w, -0.35f - 0.35f * Mth.sin(t * 0.55f), 0f, 0f);
                set(m.head, w, -0.1f, m.head.yRot, 0.08f);
            }
            case POINT -> {
                set(m.rightArm, w, -1.55f + m.head.xRot, m.head.yRot * 0.8f, 0f);
                set(m.leftArm, w, 0.1f, 0f, -0.05f);
            }
            case ARMS_CROSSED -> {
                set(m.rightArm, w, -0.55f, -0.35f, 0.15f);
                set(m.rightForearm, w, -1.55f, 0f, 0f);
                set(m.leftArm, w, -0.45f, 0.35f, -0.15f);
                set(m.leftForearm, w, -1.65f, 0f, 0f);
            }
            case HANDS_UP -> {
                set(m.rightArm, w, -3.0f, 0f, 0.35f);
                set(m.leftArm, w, -3.0f, 0f, -0.35f);
            }
            case FACEPALM -> {
                set(m.rightArm, w, -1.3f, -0.45f, 0f);
                set(m.rightForearm, w, -1.75f, 0f, 0f);
                set(m.head, w, 0.45f, 0f, 0f);
            }
            case SALUTE -> {
                set(m.rightArm, w, -1.6f, -0.9f, 0.4f);
                set(m.rightForearm, w, -1.9f, 0f, 0f);
            }
            case THINK -> {
                set(m.rightArm, w, -0.5f, -0.35f, 0f);
                set(m.rightForearm, w, -2.2f, 0f, 0f);
                set(m.leftArm, w, -0.6f, 0.45f, 0f);
                set(m.leftForearm, w, -1.4f, 0f, 0f);
                set(m.head, w, 0.2f, m.head.yRot, 0.1f);
            }
            case CLAP -> {
                float s = Math.max(0f, Mth.sin(t * 1.4f));
                set(m.rightArm, w, -0.7f, -0.3f - 0.25f * s, 0f);
                set(m.rightForearm, w, -1.0f, 0f, 0f);
                set(m.leftArm, w, -0.7f, 0.3f + 0.25f * s, 0f);
                set(m.leftForearm, w, -1.0f, 0f, 0f);
            }
            case SIT -> {
                set(m.rightLeg, w, -1.5f, 0.1f, 0f);
                set(m.rightShin, w, 1.5f, 0f, 0f);
                set(m.leftLeg, w, -1.5f, -0.1f, 0f);
                set(m.leftShin, w, 1.5f, 0f, 0f);
                set(m.rightArm, w, -0.35f, 0f, 0.05f);
                set(m.leftArm, w, -0.35f, 0f, -0.05f);
            }
            case DANCE -> {
                float s = Mth.sin(t * 0.45f);
                set(m.body, w, 0f, 0f, 0.15f * s);
                set(m.rightArm, w, -0.6f + 0.7f * s, 0f, 0.5f);
                set(m.leftArm, w, -0.6f - 0.7f * s, 0f, -0.5f);
                set(m.rightLeg, w, 0.35f * s, 0f, 0.05f);
                set(m.leftLeg, w, -0.35f * s, 0f, -0.05f);
                set(m.head, w, 0.1f * Mth.sin(t * 0.9f), 0f, -0.12f * s);
            }
            case KNEEL -> {
                set(m.rightLeg, w, -1.5f, 0f, 0f);
                set(m.rightShin, w, 1.5f, 0f, 0f);
                set(m.leftLeg, w, 0f, 0f, 0f);
                set(m.leftShin, w, 1.5f, 0f, 0f);
                set(m.rightArm, w, -0.7f, 0f, 0f);
                set(m.rightForearm, w, -0.6f, 0f, 0f);
            }
            case SHRUG -> {
                set(m.rightArm, w, -0.2f, 0f, 0.35f);
                set(m.rightForearm, w, -1.5f, 0f, 0f);
                set(m.leftArm, w, -0.2f, 0f, -0.35f);
                set(m.leftForearm, w, -1.5f, 0f, 0f);
                set(m.head, w, -0.05f, m.head.yRot, 0.18f);
            }
            default -> {
            }
        }
    }

    private static void set(ModelPart p, float w, float x, float y, float z) {
        p.xRot = Mth.lerp(w, p.xRot, x);
        p.yRot = Mth.lerp(w, p.yRot, y);
        p.zRot = Mth.lerp(w, p.zRot, z);
    }
}
