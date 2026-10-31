package com.mojang.math;

import org.joml.Quaternionf;
import org.joml.Vector3f;

/** Render lab stand-in for Minecraft's Axis. */
@FunctionalInterface
public interface Axis {
    Axis XN = a -> new Quaternionf().rotationX(-a);
    Axis XP = a -> new Quaternionf().rotationX(a);
    Axis YN = a -> new Quaternionf().rotationY(-a);
    Axis YP = a -> new Quaternionf().rotationY(a);
    Axis ZN = a -> new Quaternionf().rotationZ(-a);
    Axis ZP = a -> new Quaternionf().rotationZ(a);

    static Axis of(Vector3f axis) {
        return a -> new Quaternionf().rotationAxis(a, axis);
    }

    Quaternionf rotation(float radians);

    default Quaternionf rotationDegrees(float degrees) {
        return rotation(degrees * (float) (Math.PI / 180));
    }
}
