package com.waylays.cinematic.client;

import com.waylays.cinematic.studio.BodyPart;

/**
 * What each joint can do, in words: a slider per movement, with natural limits (an elbow only bends one way, a head
 * does not turn all the way round). A movement turns one of the joint's angles, the sign saying which way is "more".
 */
final class Joints {
    /** {@code comp}: 0 = x, 1 = y, 2 = z angle; {@code sign} turns the shown value into the stored angle. */
    record Move(String key, int comp, float sign, float min, float max) {}

    private Joints() {}

    static Move[] moves(BodyPart p) {
        return switch (p) {
            case HEAD -> new Move[] {
                    new Move("cinematic.move.head_nod", 0, 1, -70, 70),
                    new Move("cinematic.move.head_turn", 1, 1, -80, 80),
                    new Move("cinematic.move.head_tilt", 2, 1, -40, 40)};
            case BODY -> new Move[] {
                    new Move("cinematic.move.body_lean", 0, 1, -40, 90),
                    new Move("cinematic.move.body_twist", 1, 1, -80, 80),
                    new Move("cinematic.move.body_side", 2, 1, -40, 40)};
            case RIGHT_ARM, LEFT_ARM -> {
                float side = p == BodyPart.RIGHT_ARM ? 1 : -1;
                yield new Move[] {
                        new Move("cinematic.move.arm_raise", 0, -1, -60, 180),
                        new Move("cinematic.move.arm_spread", 2, side, -20, 180),
                        new Move("cinematic.move.arm_twist", 1, side, -90, 90)};
            }
            case RIGHT_FOREARM, LEFT_FOREARM -> new Move[] {new Move("cinematic.move.elbow", 0, -1, 0, 150)};
            case RIGHT_LEG, LEFT_LEG -> {
                float side = p == BodyPart.RIGHT_LEG ? 1 : -1;
                yield new Move[] {
                        new Move("cinematic.move.leg_raise", 0, -1, -60, 120),
                        new Move("cinematic.move.leg_spread", 2, side, -15, 80),
                        new Move("cinematic.move.leg_twist", 1, side, -60, 60)};
            }
            case RIGHT_SHIN, LEFT_SHIN -> new Move[] {new Move("cinematic.move.knee", 0, 1, 0, 150)};
        };
    }
}
