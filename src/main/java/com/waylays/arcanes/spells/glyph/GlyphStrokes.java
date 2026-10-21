package com.waylays.arcanes.spells.glyph;

import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.List;

/**
 * The glyphs' strokes, in the order they are written: polylines in a square from -1 to 1 (x right, y up). The six
 * element glyphs are drawn for the addon (tools/glyph_assets.py draws the icons from the same strokes).
 */
public final class GlyphStrokes {
    private static final List<List<float[][]>> ELEMENTS = new ArrayList<>();
    /** The ring around the Primordial Glyph and the Seal. */
    public static final List<float[][]> RING = List.of(arc(0, 0, 1, 0, Mth.TWO_PI, 40));
    /** The star of the Primordial Glyph, through its six element glyphs: two triangles and a small ring in the middle. */
    public static final List<float[][]> STAR = List.of(triangle(0), triangle(Mth.PI / 3), arc(0, 0, .3f, 0, Mth.TWO_PI, 20));

    static {
        for (GlyphElement element : GlyphElement.values()) {
            ELEMENTS.add(build(element));
        }
    }

    private GlyphStrokes() {}

    public static List<float[][]> of(GlyphElement element) {
        return ELEMENTS.get(element.ordinal());
    }

    /** The total length of a glyph's strokes. */
    public static float length(List<float[][]> strokes) {
        float total = 0;
        for (float[][] stroke : strokes) {
            for (int i = 1; i < stroke.length; i++) {
                total += Mth.sqrt(sq(stroke[i][0] - stroke[i - 1][0]) + sq(stroke[i][1] - stroke[i - 1][1]));
            }
        }
        return total;
    }

    private static float sq(float v) {
        return v * v;
    }

    private static List<float[][]> build(GlyphElement element) {
        List<float[][]> s = new ArrayList<>();
        switch (element) {
            case FIRE -> {
                // A flame: its heart rising in an S, a tongue on each side, the hearth under it
                s.add(smooth(new float[][]{{0, -.9f}, {.28f, -.5f}, {-.18f, -.1f}, {.22f, .3f}, {-.06f, .66f}, {.02f, .96f}}));
                s.add(smooth(new float[][]{{-.18f, -.78f}, {-.56f, -.3f}, {-.44f, .18f}, {-.62f, .46f}}));
                s.add(smooth(new float[][]{{.18f, -.78f}, {.56f, -.26f}, {.5f, .14f}, {.64f, .4f}}));
                s.add(smooth(new float[][]{{-.68f, -.84f}, {0, -.98f}, {.68f, -.84f}}));
            }
            case FROST -> {
                // Six arms, each with a pair of barbs
                for (int k = 0; k < 3; k++) {
                    float a = k * Mth.PI / 3 + Mth.HALF_PI;
                    float c = Mth.cos(a), d = Mth.sin(a);
                    s.add(new float[][]{{-c * .92f, -d * .92f}, {c * .92f, d * .92f}});
                }
                for (int k = 0; k < 6; k++) {
                    float a = k * Mth.PI / 3 + Mth.HALF_PI;
                    float c = Mth.cos(a), d = Mth.sin(a);
                    float bx = c * .55f, by = d * .55f;
                    for (int side = -1; side <= 1; side += 2) {
                        float b = a + side * .7f;
                        s.add(new float[][]{{bx, by}, {bx + Mth.cos(b) * .28f, by + Mth.sin(b) * .28f}});
                    }
                }
            }
            case STORM -> {
                // A ring, struck through by a bolt
                s.add(arc(0, 0, .82f, Mth.HALF_PI, Mth.HALF_PI + Mth.TWO_PI * .92f, 34));
                s.add(new float[][]{{-.18f, .98f}, {.28f, .12f}, {-.12f, .08f}, {.22f, -.98f}});
            }
            case TIDE -> {
                // A falling drop over three waves
                s.add(smooth(new float[][]{{0, .98f}, {-.2f, .68f}, {0, .52f}, {.2f, .68f}, {0, .98f}}));
                for (int k = 0; k < 3; k++) {
                    float y = .22f - k * .42f;
                    float[][] wave = new float[17][];
                    for (int i = 0; i <= 16; i++) {
                        float x = -.9f + i * 1.8f / 16;
                        wave[i] = new float[]{x, y + .14f * Mth.sin((x + k * .3f) * Mth.PI * 1.6f)};
                    }
                    s.add(wave);
                }
            }
            case GALE -> {
                // A spiral, swept along by two gusts
                float[][] spiral = new float[30][];
                for (int i = 0; i < 30; i++) {
                    float t = i / 29f;
                    float a = t * Mth.TWO_PI * 1.5f;
                    float r = .08f + t * .62f;
                    spiral[i] = new float[]{Mth.cos(a) * r, .15f + Mth.sin(a) * r};
                }
                s.add(spiral);
                s.add(smooth(new float[][]{{-.92f, -.52f}, {-.2f, -.66f}, {.5f, -.6f}, {.92f, -.4f}}));
                s.add(smooth(new float[][]{{-.7f, -.86f}, {0, -.92f}, {.7f, -.8f}}));
            }
            case STONE -> {
                // A mountain on its base, a bar across it and a dot over the bar
                s.add(new float[][]{{-.86f, -.72f}, {0, .86f}, {.86f, -.72f}, {-.86f, -.72f}});
                s.add(new float[][]{{-.36f, -.22f}, {.36f, -.22f}});
                s.add(arc(0, .22f, .1f, 0, Mth.TWO_PI, 10));
                s.add(new float[][]{{-.96f, -.92f}, {.96f, -.92f}});
            }
        }
        return s;
    }

    private static float[][] triangle(float start) {
        float[][] out = new float[4][];
        for (int i = 0; i < 4; i++) {
            float a = start + i * Mth.TWO_PI / 3;
            out[i] = new float[]{Mth.cos(a), Mth.sin(a)};
        }
        return out;
    }

    /** An arc of a circle centred on ({@code cx}, {@code cy}). */
    public static float[][] arc(float cx, float cy, float r, float a0, float a1, int segments) {
        float[][] out = new float[segments + 1][];
        for (int i = 0; i <= segments; i++) {
            float a = a0 + (a1 - a0) * i / segments;
            out[i] = new float[]{cx + Mth.cos(a) * r, cy + Mth.sin(a) * r};
        }
        return out;
    }

    /** A smooth curve through the points (Catmull-Rom, 6 steps between two points). */
    public static float[][] smooth(float[][] p) {
        int steps = 6;
        float[][] out = new float[(p.length - 1) * steps + 1][];
        int n = 0;
        for (int i = 0; i < p.length - 1; i++) {
            float[] p0 = p[Math.max(0, i - 1)], p1 = p[i], p2 = p[i + 1], p3 = p[Math.min(p.length - 1, i + 2)];
            for (int k = 0; k < steps; k++) {
                float t = k / (float) steps;
                out[n++] = new float[]{cr(p0[0], p1[0], p2[0], p3[0], t), cr(p0[1], p1[1], p2[1], p3[1], t)};
            }
        }
        out[n] = p[p.length - 1];
        return out;
    }

    private static float cr(float a, float b, float c, float d, float t) {
        return .5f * (2 * b + (-a + c) * t + (2 * a - 5 * b + 4 * c - d) * t * t + (-a + 3 * b - 3 * c + d) * t * t * t);
    }
}
