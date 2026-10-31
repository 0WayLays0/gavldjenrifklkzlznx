package net.minecraft.world.phys;

import org.joml.Vector3f;

/** Render lab stand-in for Minecraft's Vec3. */
public class Vec3 {
    public static final Vec3 ZERO = new Vec3(0, 0, 0);
    public final double x;
    public final double y;
    public final double z;

    public Vec3(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public Vec3(Vector3f v) {
        this(v.x, v.y, v.z);
    }

    public Vec3 add(Vec3 o) {
        return new Vec3(x + o.x, y + o.y, z + o.z);
    }

    public Vec3 add(double a, double b, double c) {
        return new Vec3(x + a, y + b, z + c);
    }

    public Vec3 subtract(Vec3 o) {
        return new Vec3(x - o.x, y - o.y, z - o.z);
    }

    public Vec3 subtract(double a, double b, double c) {
        return new Vec3(x - a, y - b, z - c);
    }

    public Vec3 scale(double k) {
        return new Vec3(x * k, y * k, z * k);
    }

    public Vec3 multiply(double a, double b, double c) {
        return new Vec3(x * a, y * b, z * c);
    }

    public Vec3 multiply(Vec3 o) {
        return new Vec3(x * o.x, y * o.y, z * o.z);
    }

    public Vec3 normalize() {
        double l = length();
        return l < 1.0E-4 ? ZERO : new Vec3(x / l, y / l, z / l);
    }

    public Vec3 cross(Vec3 o) {
        return new Vec3(y * o.z - z * o.y, z * o.x - x * o.z, x * o.y - y * o.x);
    }

    public double dot(Vec3 o) {
        return x * o.x + y * o.y + z * o.z;
    }

    public double length() {
        return Math.sqrt(x * x + y * y + z * z);
    }

    public double lengthSqr() {
        return x * x + y * y + z * z;
    }

    public double horizontalDistance() {
        return Math.sqrt(x * x + z * z);
    }

    public double distanceTo(Vec3 o) {
        return subtract(o).length();
    }

    public double distanceToSqr(Vec3 o) {
        return subtract(o).lengthSqr();
    }

    public Vec3 lerp(Vec3 o, double t) {
        return new Vec3(x + (o.x - x) * t, y + (o.y - y) * t, z + (o.z - z) * t);
    }

    public Vec3 reverse() {
        return scale(-1);
    }

    public Vec3 yRot(float a) {
        double c = Math.cos(a), s = Math.sin(a);
        return new Vec3(x * c + z * s, y, z * c - x * s);
    }

    public Vec3 xRot(float a) {
        double c = Math.cos(a), s = Math.sin(a);
        return new Vec3(x, y * c + z * s, z * c - y * s);
    }

    public Vec3 zRot(float a) {
        double c = Math.cos(a), s = Math.sin(a);
        return new Vec3(x * c + y * s, y * c - x * s, z);
    }

    public Vector3f toVector3f() {
        return new Vector3f((float) x, (float) y, (float) z);
    }

    public double x() {
        return x;
    }

    public double y() {
        return y;
    }

    public double z() {
        return z;
    }
}
