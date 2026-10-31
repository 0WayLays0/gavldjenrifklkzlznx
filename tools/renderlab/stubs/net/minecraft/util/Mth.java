package net.minecraft.util;

/** Render lab stand-in for Minecraft's Mth (the functions the renderers use). */
public final class Mth {
    public static final float PI = (float) Math.PI;
    public static final float HALF_PI = (float) (Math.PI / 2);
    public static final float TWO_PI = (float) (Math.PI * 2);
    public static final float DEG_TO_RAD = (float) (Math.PI / 180);
    public static final float RAD_TO_DEG = (float) (180 / Math.PI);
    public static final float EPSILON = 1.0E-5f;

    private Mth() {}

    public static float sin(float a) {
        return (float) Math.sin(a);
    }

    public static float cos(float a) {
        return (float) Math.cos(a);
    }

    public static float sqrt(float a) {
        return (float) Math.sqrt(a);
    }

    public static int floor(float a) {
        return (int) Math.floor(a);
    }

    public static int floor(double a) {
        return (int) Math.floor(a);
    }

    public static int ceil(float a) {
        return (int) Math.ceil(a);
    }

    public static float abs(float a) {
        return Math.abs(a);
    }

    public static int abs(int a) {
        return Math.abs(a);
    }

    public static float clamp(float v, float lo, float hi) {
        return v < lo ? lo : Math.min(v, hi);
    }

    public static double clamp(double v, double lo, double hi) {
        return v < lo ? lo : Math.min(v, hi);
    }

    public static int clamp(int v, int lo, int hi) {
        return Math.min(Math.max(v, lo), hi);
    }

    public static long clamp(long v, long lo, long hi) {
        return Math.min(Math.max(v, lo), hi);
    }

    public static float lerp(float t, float a, float b) {
        return a + t * (b - a);
    }

    public static double lerp(double t, double a, double b) {
        return a + t * (b - a);
    }

    public static float frac(float a) {
        return a - (float) Math.floor(a);
    }

    public static double frac(double a) {
        return a - Math.floor(a);
    }

    public static float wrapDegrees(float a) {
        float f = a % 360;
        if (f >= 180) {
            f -= 360;
        }
        if (f < -180) {
            f += 360;
        }
        return f;
    }

    public static double atan2(double y, double x) {
        return Math.atan2(y, x);
    }

    public static float rotLerp(float t, float a, float b) {
        return a + t * wrapDegrees(b - a);
    }

    public static float square(float a) {
        return a * a;
    }

    public static int color(float r, float g, float b) {
        return ((int) (r * 255) << 16) | ((int) (g * 255) << 8) | (int) (b * 255);
    }

    public static float inverseLerp(float v, float a, float b) {
        return (v - a) / (b - a);
    }

    public static int hsvToRgb(float h, float s, float v) {
        int i = (int) (h * 6) % 6;
        float f = h * 6 - (float) Math.floor(h * 6);
        float p = v * (1 - s), q = v * (1 - f * s), t = v * (1 - (1 - f) * s);
        float r, g, b;
        switch (i) {
            case 0 -> { r = v; g = t; b = p; }
            case 1 -> { r = q; g = v; b = p; }
            case 2 -> { r = p; g = v; b = t; }
            case 3 -> { r = p; g = q; b = v; }
            case 4 -> { r = t; g = p; b = v; }
            default -> { r = v; g = p; b = q; }
        }
        return color(r, g, b);
    }
}
