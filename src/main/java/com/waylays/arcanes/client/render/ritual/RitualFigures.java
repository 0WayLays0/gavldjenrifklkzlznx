package com.waylays.arcanes.client.render.ritual;

import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.List;

/**
 * The figures the rituals are drawn in, as strokes of chalk and sigils on a plane (blocks; {@code +y} the way the
 * figure looks, or up for a wall). Numbered as {@code Ritual.Figure}: the triangle, the six-pointed star, the spiral,
 * the arch (on a wall), the rose, the menhirs and the path (round an altar).
 */
public final class RitualFigures {
    public static final int TRIANGLE = 0, HEXAGRAM = 1, SPIRAL = 2, ARCH = 3, ROSE = 4, MENHIRS = 5, PATH = 6;
    /** The sigils of the atlas (see ritual_assets.py). */
    public static final int EYE = 0, STAR = 1, MOON = 2, FALLING_STAR = 3, WHIRL = 4, SPEAR = 5, BOND = 6, TRIDENT = 7, HOURGLASS = 8, STONE = 9, TIDE = 10,
            KEY = 11, SUN = 12, GATE = 13, CRYSTAL = 14, BLOOM = 15;

    private static final float R = 3.5f;
    private static final Shape[] CACHE = new Shape[7];

    private RitualFigures() {}

    /** One stroke: a line through its points, {@code width} wide, drawn from length {@code start} of the whole figure. */
    public record Stroke(float[] xs, float[] ys, float width, float length, float start) {}

    /** One sigil: at {@code (x, y)}, {@code size} across, turned by {@code angle}; drawn at {@code at} (0 to 1) of the drawing. */
    public record Sigil(float x, float y, float size, float angle, int index, float at) {}

    /**
     * A whole figure: its strokes in the order they are drawn, their total length, its sigils, where its heart is
     * (where the light gathers) and where standing stones rise (for the menhirs) or crystals sprout (for the path).
     */
    public record Shape(List<Stroke> strokes, float total, List<Sigil> sigils, float heartX, float heartY, float[][] stones, boolean altar) {}

    public static Shape of(int figure) {
        int f = Mth.clamp(figure, 0, CACHE.length - 1);
        if (CACHE[f] == null) {
            CACHE[f] = build(f);
        }
        return CACHE[f];
    }

    /** Where along a figure's strokes length {@code s} lies: {@code out[0], out[1]}. */
    public static void pointAt(Shape shape, float s, float[] out) {
        for (Stroke st : shape.strokes()) {
            if (s > st.start() + st.length()) {
                continue;
            }
            float along = st.start();
            for (int i = 0; i + 1 < st.xs().length; i++) {
                float dx = st.xs()[i + 1] - st.xs()[i], dy = st.ys()[i + 1] - st.ys()[i];
                float len = Mth.sqrt(dx * dx + dy * dy);
                if (along + len >= s || i + 2 == st.xs().length) {
                    float t = len < 1.0E-5f ? 0 : Mth.clamp((s - along) / len, 0, 1);
                    out[0] = st.xs()[i] + dx * t;
                    out[1] = st.ys()[i] + dy * t;
                    return;
                }
                along += len;
            }
        }
        Stroke last = shape.strokes().get(shape.strokes().size() - 1);
        out[0] = last.xs()[last.xs().length - 1];
        out[1] = last.ys()[last.ys().length - 1];
    }

    // ------------------------------------------------------------------------------------------------------- building

    private static final class Builder {
        final List<float[][]> lines = new ArrayList<>();
        final List<Float> widths = new ArrayList<>();
        final List<Sigil> sigils = new ArrayList<>();
        float heartX, heartY;
        float[][] stones = new float[0][];
        boolean altar;

        void line(float w, float... xy) {
            float[] xs = new float[xy.length / 2], ys = new float[xy.length / 2];
            for (int i = 0; i < xs.length; i++) {
                xs[i] = xy[i * 2];
                ys[i] = xy[i * 2 + 1];
            }
            lines.add(new float[][] {xs, ys});
            widths.add(w);
        }

        void arc(float w, float cx, float cy, float r, float a0, float a1, int segs) {
            float[] xs = new float[segs + 1], ys = new float[segs + 1];
            for (int i = 0; i <= segs; i++) {
                float a = a0 + (a1 - a0) * i / segs;
                xs[i] = cx + Mth.cos(a) * r;
                ys[i] = cy + Mth.sin(a) * r;
            }
            lines.add(new float[][] {xs, ys});
            widths.add(w);
        }

        void circle(float w, float cx, float cy, float r, float phase) {
            arc(w, cx, cy, r, phase, phase + Mth.TWO_PI, Math.max(16, (int) (r * 12)));
        }

        /** A closed polygon of {@code n} corners on a circle of radius {@code r}, the first at {@code phase}. */
        void polygon(float w, float r, int n, float phase, int step) {
            float[] xy = new float[(n + 1) * 2];
            for (int i = 0; i <= n; i++) {
                float a = phase + (i * step % n) * Mth.TWO_PI / n;
                xy[i * 2] = Mth.cos(a) * r;
                xy[i * 2 + 1] = Mth.sin(a) * r;
            }
            line(w, xy);
        }

        void sigil(float x, float y, float size, float angle, int index) {
            sigils.add(new Sigil(x, y, size, angle, index, 0));
        }

        Shape build() {
            List<Stroke> strokes = new ArrayList<>();
            float total = 0;
            for (int k = 0; k < lines.size(); k++) {
                float[] xs = lines.get(k)[0], ys = lines.get(k)[1];
                float len = 0;
                for (int i = 0; i + 1 < xs.length; i++) {
                    len += Mth.sqrt((xs[i + 1] - xs[i]) * (xs[i + 1] - xs[i]) + (ys[i + 1] - ys[i]) * (ys[i + 1] - ys[i]));
                }
                strokes.add(new Stroke(xs, ys, widths.get(k), len, total));
                total += len;
            }
            // Sigils are drawn one after another once the strokes are done
            List<Sigil> timed = new ArrayList<>();
            for (int k = 0; k < sigils.size(); k++) {
                Sigil s = sigils.get(k);
                timed.add(new Sigil(s.x(), s.y(), s.size(), s.angle(), s.index(), (k + 1f) / (sigils.size() + 1f)));
            }
            return new Shape(strokes, total, timed, heartX, heartY, stones, altar);
        }
    }

    private static Shape build(int figure) {
        Builder b = new Builder();
        float up = Mth.HALF_PI;
        switch (figure) {
            case TRIANGLE -> {
                // A star falling: a great triangle pointing ahead in a circle, a small one turned the other way at its
                // heart, lines down from its corners, the falling star at its middle
                b.circle(.11f, 0, 0, R + .45f, up);
                b.polygon(.1f, R + .2f, 3, up, 1);
                b.circle(.07f, 0, 0, R - .55f, up + Mth.PI);
                b.polygon(.08f, 1.5f, 3, -up, 1);
                for (int k = 0; k < 3; k++) {
                    float a = up + k * Mth.TWO_PI / 3;
                    b.line(.06f, Mth.cos(a) * (R + .2f), Mth.sin(a) * (R + .2f), Mth.cos(a) * 1.55f, Mth.sin(a) * 1.55f);
                }
                b.circle(.06f, 0, 0, .9f, 0);
                b.sigil(0, 0, 1.3f, 0, FALLING_STAR);
                int[] marks = {SUN, STAR, MOON};
                for (int k = 0; k < 3; k++) {
                    float a = -up + k * Mth.TWO_PI / 3;
                    b.sigil(Mth.cos(a) * 2.35f, Mth.sin(a) * 2.35f, .7f, a + up, marks[k]);
                }
            }
            case HEXAGRAM -> {
                // A six-pointed star in a double ring, a hexagon at its heart, the watching eye
                b.circle(.11f, 0, 0, R + .45f, 0);
                b.circle(.06f, 0, 0, R + .2f, 0);
                b.polygon(.1f, R - .05f, 3, up, 1);
                b.polygon(.1f, R - .05f, 3, -up, 1);
                b.polygon(.07f, (R - .05f) / Mth.sqrt(3), 6, 0, 1);
                b.circle(.06f, 0, 0, 1.05f, 0);
                b.sigil(0, 0, 1.4f, 0, EYE);
                int[] marks = {SPEAR, KEY, SPEAR, KEY, SPEAR, KEY};
                for (int k = 0; k < 6; k++) {
                    float a = up + k * Mth.TWO_PI / 6;
                    b.sigil(Mth.cos(a) * 2.6f, Mth.sin(a) * 2.6f, .55f, a - up, marks[k]);
                }
            }
            case SPIRAL -> {
                // Three arms wound out from the heart, a ring of waves round them, bolts striking out between the arms
                for (int arm = 0; arm < 3; arm++) {
                    int n = 40;
                    float[] xy = new float[n * 2];
                    for (int i = 0; i < n; i++) {
                        float t = (float) i / (n - 1);
                        float a = arm * Mth.TWO_PI / 3 + t * 1.7f * Mth.PI;
                        float r = .35f + (R - .1f) * t;
                        xy[i * 2] = Mth.cos(a) * r;
                        xy[i * 2 + 1] = Mth.sin(a) * r;
                    }
                    b.line(.1f, xy);
                }
                int n = 97;
                float[] wave = new float[n * 2];
                for (int i = 0; i < n; i++) {
                    float a = i * Mth.TWO_PI / (n - 1);
                    float r = R + .45f + .13f * Mth.sin(a * 16);
                    wave[i * 2] = Mth.cos(a) * r;
                    wave[i * 2 + 1] = Mth.sin(a) * r;
                }
                b.line(.09f, wave);
                for (int k = 0; k < 6; k++) {
                    float a = k * Mth.TWO_PI / 6 + .5f;
                    float ca = Mth.cos(a), sa = Mth.sin(a), cb = Mth.cos(a + .14f), sb = Mth.sin(a + .14f), cc = Mth.cos(a - .1f), sc = Mth.sin(a - .1f);
                    b.line(.07f, ca * 2.1f, sa * 2.1f, cb * 2.8f, sb * 2.8f, cc * 3.2f, sc * 3.2f, Mth.cos(a + .05f) * 4.6f, Mth.sin(a + .05f) * 4.6f);
                }
                b.circle(.07f, 0, 0, .6f, 0);
                b.sigil(0, 0, 1f, 0, WHIRL);
                for (int k = 0; k < 3; k++) {
                    float a = k * Mth.TWO_PI / 3 + 2.4f;
                    b.sigil(Mth.cos(a) * 2.2f, Mth.sin(a) * 2.2f, .6f, a - up, TRIDENT);
                }
            }
            case ARCH -> {
                // A pointed doorway drawn on the wall, double-lined, sigils down its jambs, its key at the top, a
                // threshold at its foot and rays above
                pointedArch(b, .1f, 1.3f, 2.4f, 1.8f);
                pointedArch(b, .08f, 1.7f, 2.4f, 2.2f);
                b.line(.1f, -2.4f, .06f, 2.4f, .06f);
                b.line(.06f, -1.3f, .5f, 1.3f, .5f);
                for (int k = -2; k <= 2; k++) {
                    float a = up + k * .32f;
                    b.line(.06f, Mth.cos(a) * 2.75f, 2.4f + Mth.sin(a) * 2.75f, Mth.cos(a) * 3.35f, 2.4f + Mth.sin(a) * 3.35f);
                }
                b.circle(.07f, 0, 2.05f, .65f, up);
                b.sigil(0, 2.05f, 1f, 0, KEY);
                b.sigil(0, 4.85f, .55f, 0, GATE);
                int[] marks = {MOON, STAR, EYE};
                for (int k = 0; k < 3; k++) {
                    b.sigil(-1.5f, .95f + k * .6f, .32f, 0, marks[k]);
                    b.sigil(1.5f, .95f + k * .6f, .32f, 0, marks[2 - k]);
                }
                b.heartX = 0;
                b.heartY = 2.05f;
            }
            case ROSE -> {
                // A rose: six petals round a seventh, a ring scalloped like petals, the bloom at its heart
                b.circle(.1f, 0, 0, R + .45f, up);
                float rho = 1.15f;
                b.circle(.08f, 0, 0, rho, up);
                for (int k = 0; k < 6; k++) {
                    float a = up + k * Mth.TWO_PI / 6;
                    b.circle(.07f, Mth.cos(a) * rho, Mth.sin(a) * rho, rho, a + Mth.PI);
                }
                b.circle(.07f, 0, 0, rho * 2, up);
                for (int k = 0; k < 16; k++) {
                    float a = k * Mth.TWO_PI / 16;
                    b.arc(.07f, Mth.cos(a) * (R - .05f), Mth.sin(a) * (R - .05f), .62f, a - Mth.HALF_PI, a + Mth.HALF_PI, 10);
                }
                b.sigil(0, 0, .8f, 0, BLOOM);
                for (int k = 0; k < 6; k++) {
                    float a = up + Mth.PI / 6 + k * Mth.TWO_PI / 6;
                    b.sigil(Mth.cos(a) * 2.75f, Mth.sin(a) * 2.75f, .5f, a - up, k % 2 == 0 ? SUN : BOND);
                }
            }
            case MENHIRS -> {
                // Round the altar: a square of stones, a diamond within it, lines out to the corners where the
                // menhirs rise
                float h = R;
                b.line(.11f, -h, -h, h, -h, h, h, -h, h, -h, -h);
                b.polygon(.09f, h, 4, 0, 1);
                b.circle(.07f, 0, 0, 1.4f, up);
                b.circle(.06f, 0, 0, 2.3f, up);
                float[][] corners = {{h, h}, {-h, h}, {-h, -h}, {h, -h}};
                for (float[] c : corners) {
                    b.line(.07f, c[0] * .4f, c[1] * .4f, c[0], c[1]);
                    b.circle(.07f, c[0], c[1], .7f, 0);
                }
                for (int k = 0; k < 4; k++) {
                    float a = k * Mth.HALF_PI;
                    b.sigil(Mth.cos(a) * 1.85f, Mth.sin(a) * 1.85f, .55f, a - up, k % 2 == 0 ? STONE : SPEAR);
                }
                b.stones = new float[][] {{h, h}, {-h, h}, {-h, -h}, {h, -h}};
                b.altar = true;
            }
            case PATH -> {
                // Round the altar a ring of crystals, and a path of light leading out ahead, chevrons along it
                b.circle(.1f, 0, 0, R + .45f, up);
                b.circle(.07f, 0, 0, 1.5f, up);
                for (int k = 0; k < 6; k++) {
                    float a = k * Mth.TWO_PI / 6;
                    float cx = Mth.cos(a) * 2.55f, cy = Mth.sin(a) * 2.55f;
                    float ux = Mth.cos(a) * .55f, uy = Mth.sin(a) * .55f, vx = -uy * .5f, vy = ux * .5f;
                    b.line(.07f, cx + ux, cy + uy, cx + vx, cy + vy, cx - ux, cy - uy, cx - vx, cy - vy, cx + ux, cy + uy);
                }
                float y0 = R + .45f, y1 = R + 7.5f;
                b.line(.1f, -1, y0, -1, y1);
                b.line(.1f, 1, y0, 1, y1);
                for (float y = y0 + .8f; y < y1 - .4f; y += 1.3f) {
                    b.line(.08f, -.7f, y, 0, y + .6f, .7f, y);
                }
                b.line(.09f, -1.4f, y1, 0, y1 + 1.2f, 1.4f, y1);
                b.sigil(0, y1 - .2f, .8f, 0, CRYSTAL);
                for (int k = 0; k < 6; k++) {
                    float a = k * Mth.TWO_PI / 6 + Mth.PI / 6;
                    b.sigil(Mth.cos(a) * 2.6f, Mth.sin(a) * 2.6f, .45f, a - up, CRYSTAL);
                }
                float[][] sprouts = new float[10][];
                for (int k = 0; k < 10; k++) {
                    sprouts[k] = new float[] {k % 2 == 0 ? -1.2f : 1.2f, y0 + .5f + k * .68f};
                }
                b.stones = sprouts;
                b.altar = true;
            }
            default -> b.circle(.1f, 0, 0, R, 0);
        }
        return b.build();
    }

    /** A pointed arch standing on the plane's foot: jambs {@code half} out from the middle up to {@code spring}, arcs of {@code r} meeting above. */
    private static void pointedArch(Builder b, float w, float half, float spring, float r) {
        float off = r - half;
        float top = (float) Math.acos(off / r);
        int n = 14;
        float[] xy = new float[(n * 2 + 4) * 2];
        int k = 0;
        xy[k++] = -half;
        xy[k++] = 0;
        for (int i = 0; i <= n; i++) {
            float a = Mth.PI - top * i / n;
            xy[k++] = off + Mth.cos(a) * r;
            xy[k++] = spring + Mth.sin(a) * r;
        }
        for (int i = n; i >= 0; i--) {
            float a = top * i / n;
            xy[k++] = -off + Mth.cos(a) * r;
            xy[k++] = spring + Mth.sin(a) * r;
        }
        xy[k++] = half;
        xy[k++] = 0;
        float[] trimmed = new float[k];
        System.arraycopy(xy, 0, trimmed, 0, k);
        b.line(w, trimmed);
    }
}
