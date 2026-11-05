package com.waylays.arcanes.client.render.melody;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.waylays.arcanes.ArcanesOublies;
import com.waylays.arcanes.client.render.boss.BossMesh;
import com.waylays.arcanes.client.render.boss.Sculpt;
import com.waylays.arcanes.client.render.clockwork.ClockMesh;
import com.waylays.arcanes.client.render.ritual.RitualMesh;
import com.waylays.arcanes.client.render.water.WaterMesh;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * What Music Magic is made of: notes of gold that glow, staves of light they ride on, a treble clef, and instruments
 * (a war drum, a harp, trumpets, an organ's pipes, violins) of gold, varnished wood and drum skin.
 */
public final class MelodyMesh {
    /** The five lines of a staff, white (tinted), along U. */
    public static final ResourceLocation STAFF = ArcanesOublies.id("textures/entity/melody/staff.png");
    public static final ResourceLocation VARNISH = ArcanesOublies.id("textures/entity/melody/varnish.png");
    public static final ResourceLocation DRUMHEAD = ArcanesOublies.id("textures/entity/melody/drumhead.png");
    /** Gold: the clockwork's brushed brass, lit brighter. */
    public static final ResourceLocation GOLD_METAL = ClockMesh.BRASS;

    public static final int GOLD = 0xFFD36A;
    public static final int GOLD_LIGHT = 0xFFF0BE;
    public static final int ROSE = 0xFF6FD8;
    public static final int VIOLET = 0xA77BFF;
    public static final int MINT = 0x8CFFC0;
    public static final int WHITE = 0xFFFFFFFF;
    public static final int BRIGHT = 0xFFFFF4DC;

    private MelodyMesh() {}

    /** A colour along the school's run of hues, {@code t} 0 to 1: rose, violet, gold, rose again. */
    public static int hue(float t) {
        t = t - Mth.floor(t);
        if (t < 1 / 3f) {
            return BossMesh.mix(ROSE, VIOLET, t * 3);
        }
        if (t < 2 / 3f) {
            return BossMesh.mix(VIOLET, GOLD, t * 3 - 1);
        }
        return BossMesh.mix(GOLD, ROSE, t * 3 - 2);
    }

    // ---------------------------------------------------------------- notes

    /** The shapes a note can take. */
    public enum Note { QUARTER, EIGHTH, PAIR }

    private static float[] ellipse(float cx, float cy, float rx, float ry, float tilt, int n) {
        float[] pts = new float[n * 2];
        float c = Mth.cos(tilt), s = Mth.sin(tilt);
        for (int i = 0; i < n; i++) {
            float a = i * Mth.TWO_PI / n;
            float x = Mth.cos(a) * rx, y = Mth.sin(a) * ry;
            pts[2 * i] = cx + x * c - y * s;
            pts[2 * i + 1] = cy + x * s + y * c;
        }
        return pts;
    }

    /**
     * A note of gold, {@code size} tall, in the pose's XY plane (its head at the origin, its stem up), {@code depth}
     * thick; with a glow of {@code glow} behind it.
     */
    public static void note(PoseStack poseStack, MultiBufferSource buffer, WaterMesh.View view, Note kind, float size, int glow, float alpha, int light) {
        VertexConsumer c = BossMesh.solid(buffer, GOLD_METAL);
        PoseStack.Pose p = poseStack.last();
        float s = size / 3f;
        float depth = s * .3f;
        int rim = 0xFFB08040;
        int body = 0xFFFFF4DC;
        Sculpt.slab(c, p, ellipse(0, 0, s * .62f, s * .42f, .45f, 18), depth, depth * .3f, .5f, body, rim, light);
        poseStack.pushPose();
        poseStack.translate(s * .52f, s * 1.3f, 0);
        Sculpt.block(c, poseStack.last(), s * .14f, s * 2.6f, depth * .8f, .01f, .5f, body, light);
        poseStack.popPose();
        if (kind == Note.EIGHTH) {
            float[] flag = {s * .45f, s * 2.6f, s * .6f, s * 2.6f, s * 1.25f, s * 1.75f, s * 1.1f, s * 1.2f, s * .9f, s * 1.75f, s * .45f, s * 2.15f};
            Sculpt.slab(c, p, ClockMesh.ccw(flag), depth * .7f, .01f, .5f, body, rim, light);
        } else if (kind == Note.PAIR) {
            poseStack.pushPose();
            poseStack.translate(s * 1.6f, s * .2f, 0);
            Sculpt.slab(c, poseStack.last(), ellipse(0, 0, s * .62f, s * .42f, .45f, 18), depth, depth * .3f, .5f, body, rim, light);
            poseStack.translate(s * .52f, s * 1.3f, 0);
            Sculpt.block(c, poseStack.last(), s * .14f, s * 2.6f, depth * .8f, .01f, .5f, body, light);
            poseStack.popPose();
            float[] beam = {s * .45f, s * 2.35f, s * 2.19f, s * 2.55f, s * 2.19f, s * 2.85f, s * .45f, s * 2.65f};
            Sculpt.slab(c, p, ClockMesh.ccw(beam), depth * .8f, .01f, .5f, body, rim, light);
        }
        if (alpha > .01f) {
            ClockMesh.glow(poseStack, buffer, view, new Vector3f(s * .4f, s, 0), size * 1.8f, glow, alpha * .6f, light);
            ClockMesh.glow(poseStack, buffer, view, new Vector3f(0, 0, 0), size * .8f, BRIGHT & 0xFFFFFF, alpha * .5f, light);
        }
    }

    /** A note at {@code at}, turned {@code yaw} round Y and tipped {@code roll}, the whole note {@code size} tall. */
    public static void noteAt(PoseStack poseStack, MultiBufferSource buffer, WaterMesh.View view, Vector3f at, Note kind, float yaw, float roll, float size, int glow,
                              float alpha, int light) {
        Quaternionf turn = new Quaternionf().rotationY(yaw).rotateZ(roll);
        poseStack.pushPose();
        poseStack.translate(at.x, at.y, at.z);
        poseStack.mulPose(turn);
        poseStack.translate(-size * .15f, -size * .35f, 0);
        note(poseStack, buffer, local(view, at, turn), kind, size, glow, alpha, light);
        poseStack.popPose();
    }

    /** A note at {@code at} turned to face the eye (round the upright), rocking a little. */
    public static void noteFacing(PoseStack poseStack, MultiBufferSource buffer, WaterMesh.View view, Vector3f at, Note kind, float roll, float size, int glow,
                                  float alpha, int light) {
        Vector3f e = new Vector3f(view.eye()).sub(at);
        noteAt(poseStack, buffer, view, at, kind, (float) Math.atan2(e.x, e.z), roll, size, glow, alpha, light);
    }

    /** The view from a frame moved to {@code offset} and turned by {@code turn} (null: not turned). */
    public static WaterMesh.View local(WaterMesh.View view, Vector3f offset, Quaternionf turn) {
        Quaternionf undo = turn == null ? new Quaternionf() : new Quaternionf(turn).conjugate();
        Vector3f eye = undo.transform(new Vector3f(view.eye()).sub(offset));
        return new WaterMesh.View(new Quaternionf(undo).mul(view.camera()), eye);
    }

    /** Which kind of note the {@code k}th of a group is. */
    public static Note kind(int k) {
        return switch (Math.floorMod(k, 3)) {
            case 0 -> Note.EIGHTH;
            case 1 -> Note.QUARTER;
            default -> Note.PAIR;
        };
    }

    // ---------------------------------------------------------------- staves, the clef

    /**
     * A staff of light (its five lines running along it) through {@code pts}: square to {@code across[i]} at each point
     * if given, else turned to face {@code eye}; {@code width} wide, narrowing at its ends, its colour running along it
     * from {@code hueShift}; both faces.
     */
    public static void staff(VertexConsumer staff, PoseStack.Pose p, Vector3f eye, Vector3f[] pts, Vector3f[] across, float width, float alpha, float flow,
                             float hueShift, int light) {
        int n = pts.length;
        if (n < 2 || alpha <= .01f) {
            return;
        }
        Vector3f[] lo = new Vector3f[n], hi = new Vector3f[n];
        float[] u = new float[n];
        int[] col = new int[n];
        for (int i = 0; i < n; i++) {
            float t = (float) i / (n - 1);
            float w = width * Math.min(1, Math.min(t, 1 - t) * 8 + .2f) * .5f;
            Vector3f side;
            if (across != null) {
                side = new Vector3f(across[i]);
            } else {
                Vector3f tan = new Vector3f(pts[Math.min(n - 1, i + 1)]).sub(pts[Math.max(0, i - 1)]);
                side = new Vector3f(tan).cross(new Vector3f(eye).sub(pts[i]));
                if (side.lengthSquared() < 1.0E-10f) {
                    side = BossMesh.across(tan.lengthSquared() < 1.0E-10f ? new Vector3f(0, 1, 0) : tan.normalize());
                }
            }
            side.normalize().mul(w);
            lo[i] = new Vector3f(pts[i]).sub(side);
            hi[i] = new Vector3f(pts[i]).add(side);
            u[i] = i == 0 ? 0 : u[i - 1] + pts[i].distance(pts[i - 1]) / Math.max(.2f, width * 4);
            float fade = Math.min(1, Math.min(t, 1 - t) * 6 + .1f);
            col[i] = BossMesh.glow(hue(hueShift + t * .5f), alpha * fade);
        }
        for (int i = 0; i + 1 < n; i++) {
            BossMesh.quad2(staff, p, lo[i], lo[i + 1], hi[i + 1], hi[i], u[i] - flow, 1, u[i + 1] - flow, 1, u[i + 1] - flow, 0, u[i] - flow, 0, col[i], col[i + 1],
                    light);
        }
    }

    /** A ring of staff lying flat at height {@code y}, {@code r} round (its middle line), {@code width} wide, from {@code a0} over {@code arc}. */
    public static void staffRing(MultiBufferSource buffer, PoseStack.Pose p, float y, float r, float width, float a0, float arc, int segs, float alpha, float flow,
                                 float hueShift, int light) {
        Vector3f[] pts = new Vector3f[segs + 1], across = new Vector3f[segs + 1];
        for (int i = 0; i <= segs; i++) {
            float a = a0 + arc * i / segs;
            pts[i] = new Vector3f(Mth.cos(a) * r, y, Mth.sin(a) * r);
            across[i] = new Vector3f(Mth.cos(a), 0, Mth.sin(a));
        }
        staff(BossMesh.light(buffer, STAFF), p, null, pts, across, width, alpha, flow, hueShift, light);
    }

    /** A ring of staff standing up round the upright at height {@code y}, {@code r} round, waving by {@code wave}, from {@code a0} over {@code arc}. */
    public static void staffBand(MultiBufferSource buffer, PoseStack.Pose p, float y, float r, float width, float a0, float arc, int segs, float wave, float time,
                                 float alpha, float flow, float hueShift, int light) {
        Vector3f[] pts = arc(r, y, segs + 1, a0, arc, wave, time), across = new Vector3f[segs + 1];
        for (int i = 0; i <= segs; i++) {
            across[i] = new Vector3f(0, 1, 0);
        }
        staff(BossMesh.light(buffer, STAFF), p, null, pts, across, width, alpha, flow, hueShift, light);
    }

    /** Points round a circle of {@code r} at height {@code y}, from {@code a0} over {@code arc} radians, waving by {@code wave}. */
    public static Vector3f[] arc(float r, float y, int n, float a0, float arc, float wave, float time) {
        Vector3f[] pts = new Vector3f[n];
        for (int i = 0; i < n; i++) {
            float a = a0 + arc * i / (n - 1);
            pts[i] = new Vector3f(Mth.cos(a) * r, y + wave * Mth.sin(a * 3 + time * .1f), Mth.sin(a) * r);
        }
        return pts;
    }

    /** The treble clef's stroke, as a pen draws it: the spiral round the G line, up to the loop at the top, down the stem to the hook. */
    private static final float[] CLEF = {
            .02f, .33f, .08f, .38f, .04f, .45f, -.07f, .43f, -.12f, .33f, -.08f, .21f, .04f, .17f, .15f, .24f, .18f, .37f, .13f, .5f, .02f, .58f, -.08f, .68f,
            -.1f, .8f, -.04f, .93f, .04f, .98f, .07f, .9f, .03f, .78f, 0, .6f, .01f, .4f, .03f, .15f, .04f, 0, .02f, -.08f, -.05f, -.12f, -.11f, -.08f, -.11f, -.02f};

    /** A treble clef of gold, {@code size} tall, in the pose's XY plane (both faces), its foot at the origin. */
    public static void clef(PoseStack poseStack, MultiBufferSource buffer, float size, int light) {
        int k = CLEF.length / 2, per = 5, n = (k - 1) * per + 1;
        float s = size / 1.1f;
        Vector3f[] path = new Vector3f[n];
        float[] w = new float[n];
        for (int i = 0; i < n; i++) {
            int seg = Math.min(k - 2, i / per);
            float t = (i - seg * per) / (float) per;
            if (i == n - 1) {
                seg = k - 2;
                t = 1;
            }
            // Catmull-Rom through the pen's points
            int i0 = Math.max(0, seg - 1), i1 = seg, i2 = seg + 1, i3 = Math.min(k - 1, seg + 2);
            float x = cr(CLEF[2 * i0], CLEF[2 * i1], CLEF[2 * i2], CLEF[2 * i3], t);
            float y = cr(CLEF[2 * i0 + 1], CLEF[2 * i1 + 1], CLEF[2 * i2 + 1], CLEF[2 * i3 + 1], t);
            path[i] = new Vector3f(x * s, (y + .12f) * s, 0);
            float at = seg + t;
            // Thick round the spiral's outer sweep, fine at the top loop, the stem in between
            float thick = .022f + .022f * bump(at, 7.5f, 2.2f) + .006f * bump(at, 19, 3);
            w[i] = thick * s;
        }
        int[] col = Sculpt.fill(0xFFFFF0D0, n);
        Sculpt.sweep(BossMesh.solid(buffer, GOLD_METAL), poseStack.last(), path, w, w, Sculpt.circle(8), false, new Vector3f(0, 0, 1), .5f, col, true, light);
        // The dot at the end of its hook
        Vector3f end = path[n - 1];
        BossMesh.sphere(BossMesh.solid(buffer, GOLD_METAL), poseStack.last(), new Vector3f(end).add(.01f * s, -.01f * s, 0), s * .055f, 6, 10, 0, 0, 1, 0xFFFFF0D0,
                light);
    }

    private static float cr(float p0, float p1, float p2, float p3, float t) {
        float t2 = t * t, t3 = t2 * t;
        return .5f * (2 * p1 + (-p0 + p2) * t + (2 * p0 - 5 * p1 + 4 * p2 - p3) * t2 + (-p0 + 3 * p1 - 3 * p2 + p3) * t3);
    }

    private static float bump(float x, float at, float width) {
        float d = (x - at) / width;
        return (float) Math.exp(-d * d);
    }

    // ---------------------------------------------------------------- light

    public static void glow(PoseStack poseStack, MultiBufferSource buffer, WaterMesh.View view, Vector3f at, float size, int rgb, float alpha, int light) {
        ClockMesh.glow(poseStack, buffer, view, at, size, rgb, alpha, light);
    }

    /** Glints round {@code at}, twinkling. */
    public static void glints(PoseStack poseStack, MultiBufferSource buffer, WaterMesh.View view, Vector3f at, float r, float h, int count, float time, float alpha,
                              int seed, int light) {
        if (alpha <= .01f) {
            return;
        }
        VertexConsumer c = BossMesh.light(buffer, BossMesh.GLOW);
        for (int k = 0; k < count; k++) {
            float tw = Mth.sin(time * (.15f + .1f * BossMesh.hash(seed + k)) + k * 2.3f);
            if (tw < 0) {
                continue;
            }
            float a = BossMesh.hash(seed + k * 7) * Mth.TWO_PI, d = r * BossMesh.hash(seed + k * 3);
            Vector3f s = new Vector3f(Mth.cos(a) * d, h * BossMesh.hash(seed + k * 5), Mth.sin(a) * d).add(at);
            RitualMesh.billboard(c, poseStack, view, s, .25f + .3f * tw, 0, -1, BossMesh.glow(k % 3 == 0 ? GOLD : hue(k * .13f), alpha * tw), light);
        }
    }

    /** A ring of light lying flat at height {@code y} round the pose's origin, from {@code r0} to {@code r1}, bright at {@code r1}. */
    public static void ring(PoseStack.Pose p, MultiBufferSource buffer, float y, float r0, float r1, int rgb, float alpha, int light) {
        ClockMesh.ring(p, buffer, y, r0, r1, rgb, alpha, light);
    }

    /** A ring of sound standing up, facing along {@code dir} at {@code at}, {@code r} round. */
    public static void soundRing(PoseStack.Pose p, MultiBufferSource buffer, Vector3f at, Vector3f dir, float r, float width, int rgb, float alpha, int light) {
        if (alpha <= .01f) {
            return;
        }
        Vector3f u = BossMesh.across(dir), v = new Vector3f(dir).cross(u).normalize();
        BossMesh.annulus(BossMesh.light(buffer, BossMesh.GLOW), p, at, u, v, Math.max(.01f, r - width), r, 40, 1, 0, 0, BossMesh.glow(rgb, 0), BossMesh.glow(rgb, alpha),
                light, true);
    }

    // ---------------------------------------------------------------- instruments

    /** A war drum: red shell bound in gold, ropes laced round it, a skin on top; its middle at the origin, {@code r} round, {@code h} tall. */
    public static void drum(PoseStack poseStack, MultiBufferSource buffer, float r, float h, float shake, int light) {
        PoseStack.Pose p = poseStack.last();
        float hh = h * .5f;
        ClockMesh.turned(p, buffer, ClockMesh.ENAMEL, new float[] {0, -hh, r * .96f, -hh, r, -hh * .6f, r * 1.02f, 0, r, hh * .6f, r * .96f, hh, 0, hh}, 28, WHITE,
                light);
        for (float y : new float[] {-hh, hh}) {
            poseStack.pushPose();
            poseStack.translate(0, y, 0);
            Sculpt.torus(BossMesh.solid(buffer, GOLD_METAL), poseStack.last(), r * 1.0f, r * .07f, 32, 6, 0, Mth.TWO_PI, 6, WHITE, light);
            poseStack.popPose();
        }
        // The ropes laced up and down round it
        int laces = 10;
        for (int k = 0; k < laces; k++) {
            float a0 = k * Mth.TWO_PI / laces, a1 = (k + .5f) * Mth.TWO_PI / laces;
            Vector3f top = new Vector3f(Mth.cos(a0) * r * 1.04f, hh * .85f, Mth.sin(a0) * r * 1.04f);
            Vector3f bottom = new Vector3f(Mth.cos(a1) * r * 1.04f, -hh * .85f, Mth.sin(a1) * r * 1.04f);
            ClockMesh.rod(poseStack, buffer, GOLD_METAL, top, bottom, r * .025f, 0xFFE8D0A0, light);
        }
        // The skin, quivering when struck
        poseStack.pushPose();
        poseStack.translate(0, hh + .01f + shake * .03f, 0);
        Sculpt.disc(BossMesh.solid(buffer, DRUMHEAD), poseStack.last(), 0, r * .97f, 32, 0, WHITE, light, false);
        poseStack.popPose();
    }

    /** A harp of gold and varnished wood, its foot at the origin, {@code h} tall, its strings along +X; {@code pluck} shakes string {@code which}. */
    public static void harp(PoseStack poseStack, MultiBufferSource buffer, WaterMesh.View view, float h, int which, float pluck, float time, int light) {
        PoseStack.Pose p = poseStack.last();
        // The pillar: a column of gold
        poseStack.pushPose();
        poseStack.translate(0, 0, 0);
        ClockMesh.turned(p, buffer, GOLD_METAL, new float[] {0, 0, .16f * h / 3, 0, .2f * h / 3, .05f * h, .09f * h / 3, .15f * h, .08f * h / 3, .85f * h, .14f * h / 3,
                .9f * h, .1f * h / 3, h * .97f, 0, h}, 16, WHITE, light);
        poseStack.popPose();
        // The neck, curving over from the pillar's top to the soundboard's
        Vector3f n0 = new Vector3f(0, h * .95f, 0), n3 = new Vector3f(h * .62f, h * .62f, 0);
        Vector3f[] neck = Sculpt.bezier(n0, new Vector3f(h * .22f, h * 1.12f, 0), new Vector3f(h * .42f, h * .55f, 0), n3, 18);
        ClockMesh.pipe(p, buffer, GOLD_METAL, neck, h * .035f, WHITE, light);
        // The soundboard, from the foot up to the neck's end
        Vector3f[] board = Sculpt.curve(new Vector3f(h * .06f, h * .04f, 0), new Vector3f(h * .4f, h * .26f, 0), n3, 16);
        float[] bw = Sculpt.taper(h * .09f, h * .035f, 16, 1);
        Sculpt.sweep(BossMesh.solid(buffer, VARNISH), p, board, bw, Sculpt.fill(h * .05f, 16), Sculpt.circle(8), false, new Vector3f(0, 0, 1), .5f, Sculpt.fill(WHITE, 16),
                true, light);
        // Its strings of light, from the neck down to the soundboard
        VertexConsumer beam = BossMesh.light(buffer, BossMesh.BEAM);
        int strings = 10;
        for (int k = 0; k < strings; k++) {
            float t = (k + .5f) / strings;
            Vector3f top = neck[Math.min(neck.length - 1, (int) (t * (neck.length - 1)))];
            Vector3f bottom = board[Math.min(board.length - 1, (int) (t * (board.length - 1)))];
            Vector3f a = new Vector3f(top.x, top.y - h * .02f, 0), b = new Vector3f(bottom.x, bottom.y + h * .02f, 0);
            float shake = k == which ? pluck : 0;
            if (shake > 0) {
                Vector3f mid = new Vector3f(a).lerp(b, .5f).add(0, 0, h * .03f * shake * Mth.sin(time * 3.1f));
                RitualMesh.shaft(beam, p, view.eye(), a, mid, .05f + .06f * shake, BossMesh.glow(hue(t), .7f + .3f * shake), light);
                RitualMesh.shaft(beam, p, view.eye(), mid, b, .05f + .06f * shake, BossMesh.glow(hue(t), .7f + .3f * shake), light);
            } else {
                RitualMesh.shaft(beam, p, view.eye(), a, b, .045f, BossMesh.glow(hue(t), .55f), light);
            }
        }
    }

    /** A trumpet of gold along +Z, its mouthpiece at the origin, {@code len} long. */
    public static void trumpet(PoseStack poseStack, MultiBufferSource buffer, float len, int light) {
        poseStack.pushPose();
        poseStack.mulPose(new Quaternionf().rotationX(Mth.HALF_PI));
        int n = 14;
        float[] prof = new float[n * 2 + 4];
        prof[0] = 0;
        prof[1] = 0;
        for (int i = 0; i < n; i++) {
            float t = (float) i / (n - 1);
            float r = len * (.025f + .012f * (t < .08f ? 1 : 0) + .3f * (float) Math.pow(t, 7));
            prof[2 + 2 * i] = r;
            prof[3 + 2 * i] = t * len;
        }
        prof[2 + 2 * n] = len * .3f;
        prof[3 + 2 * n] = len * 1.01f;
        ClockMesh.turned(poseStack.last(), buffer, GOLD_METAL, prof, 20, WHITE, light);
        poseStack.popPose();
        // The valves and the loop of tube under them
        for (int k = 0; k < 3; k++) {
            ClockMesh.cylinder(lifted(poseStack, 0, len * .03f, len * (.3f + k * .07f)), buffer, GOLD_METAL, len * .025f, 0, len * .13f, 8, WHITE, light);
            poseStack.popPose();
        }
        Vector3f[] loop = Sculpt.bezier(new Vector3f(0, 0, len * .26f), new Vector3f(0, -len * .14f, len * .3f), new Vector3f(0, -len * .14f, len * .58f),
                new Vector3f(0, 0, len * .6f), 12);
        ClockMesh.pipe(poseStack.last(), buffer, GOLD_METAL, loop, len * .022f, WHITE, light);
    }

    private static PoseStack.Pose lifted(PoseStack poseStack, float x, float y, float z) {
        poseStack.pushPose();
        poseStack.translate(x, y, z);
        return poseStack.last();
    }

    /** An organ: pipes of gold rising from a case of varnished wood, {@code w} wide, its middle at the origin; {@code blow} lights pipe {@code which}. */
    public static void organ(PoseStack poseStack, MultiBufferSource buffer, WaterMesh.View view, float w, float rise, int which, float blow, float time, int light) {
        int pipes = 11;
        float h = w * .9f;
        // The case
        ClockMesh.boxAt(poseStack, buffer, VARNISH, 0, h * .1f, 0, w * 1.05f, h * .2f, w * .3f, WHITE, light);
        ClockMesh.boxAt(poseStack, buffer, GOLD_METAL, 0, h * .21f, 0, w * 1.08f, h * .02f, w * .32f, WHITE, light);
        for (int k = 0; k < pipes; k++) {
            float t = (float) k / (pipes - 1);
            float x = (t - .5f) * w;
            float ph = h * (.45f + .55f * Mth.sin(Mth.PI * (.15f + .7f * t))) * rise;
            float r = w * .035f * (1.2f - .4f * Math.abs(t - .5f) * 2);
            poseStack.pushPose();
            poseStack.translate(x, h * .22f, 0);
            ClockMesh.turned(poseStack.last(), buffer, GOLD_METAL, new float[] {0, 0, r * .5f, 0, r, r * 1.5f, r, ph, r * 1.05f, ph + r * .2f, r * .8f, ph + r * .2f}, 12,
                    WHITE, light);
            // Its mouth: a dark slit low on it
            ClockMesh.boxAt(poseStack, buffer, ClockMesh.STEEL, 0, ph * .18f + r * 2, r * .92f, r * 1.1f, r * .5f, r * .2f, 0xFF302030, light);
            poseStack.popPose();
            if (k == which && blow > 0) {
                Vector3f top = new Vector3f(x, h * .22f + ph + .2f, 0);
                glow(poseStack, buffer, view, top, 1.4f + blow, hue(t), blow * .8f, light);
            } else {
                glow(poseStack, buffer, view, new Vector3f(x, h * .22f + ph * .18f + r * 2, r), .5f, ROSE, .3f + .1f * Mth.sin(time * .2f + k), light);
            }
        }
        // An arch of gold over them, a clef at its crown
        Vector3f[] arch = new Vector3f[20];
        for (int i = 0; i < 20; i++) {
            float t = (float) i / 19;
            arch[i] = new Vector3f((t - .5f) * w * 1.1f, h * .22f + h * (.95f + .25f * Mth.sin(Mth.PI * t)) * rise, -w * .05f);
        }
        ClockMesh.pipe(poseStack.last(), buffer, GOLD_METAL, arch, w * .02f, WHITE, light);
        poseStack.pushPose();
        poseStack.translate(0, h * .22f + h * 1.2f * rise, -w * .05f);
        clef(poseStack, buffer, w * .25f * rise, light);
        poseStack.popPose();
    }

    /** A violin, its body's middle at the origin in the XY plane (its neck up +Y), {@code size} long, and its bow across it. */
    public static void violin(PoseStack poseStack, MultiBufferSource buffer, WaterMesh.View view, float size, float bow, int light) {
        float s = size / 2.4f;
        // Its outline: the wide lower bout, the waist, the narrower upper bout
        int half = 28;
        float[] body = new float[half * 4];
        for (int i = 0; i < half; i++) {
            float y = -.88f + 1.7f * i / (half - 1);
            // Two rounded bouts, blended where they meet, a little waist cut in between
            float lower = (float) Math.sqrt(Math.max(0, .25f - (y + .38f) * (y + .38f)));
            float upper = (float) Math.sqrt(Math.max(0, .16f - (y - .42f) * (y - .42f)));
            float w = (float) Math.pow(Math.pow(lower, 4) + Math.pow(upper, 4), .25f) - .07f * (float) Math.exp(-Math.pow((y - .03f) / .13f, 2));
            w = Math.max(.02f, w);
            body[2 * i] = w * s;
            body[2 * i + 1] = y * .8f * s;
            body[4 * half - 2 - 2 * i] = -w * s;
            body[4 * half - 1 - 2 * i] = y * .8f * s;
        }
        Sculpt.slab(BossMesh.solid(buffer, VARNISH), poseStack.last(), ClockMesh.ccw(body), .18f * s, .05f * s, .5f, WHITE, 0xFF7A3A18, light);
        // Its fingerboard of ebony, down over the body, and its bridge
        ClockMesh.boxAt(poseStack, buffer, ClockMesh.STEEL, 0, .62f * s, .12f * s, .11f * s, 1.6f * s, .04f * s, 0xFF201418, light);
        ClockMesh.boxAt(poseStack, buffer, VARNISH, 0, -.3f * s, .12f * s, .26f * s, .05f * s, .1f * s, 0xFFF0D8B0, light);
        // The neck and its scroll
        poseStack.pushPose();
        poseStack.translate(0, 1.5f * s, .02f * s);
        poseStack.mulPose(new Quaternionf().rotationY(Mth.HALF_PI));
        Sculpt.torus(BossMesh.solid(buffer, VARNISH), poseStack.last(), .07f * s, .035f * s, 12, 6, 0, Mth.TWO_PI * 1.3f, 2, WHITE, light);
        poseStack.popPose();
        // Strings of light
        VertexConsumer beam = BossMesh.light(buffer, BossMesh.BEAM);
        for (int k = 0; k < 4; k++) {
            float x = (k - 1.5f) * .03f * s;
            RitualMesh.shaft(beam, poseStack.last(), view.eye(), new Vector3f(x, -.5f * s, .1f * s), new Vector3f(x, 1.4f * s, .09f * s), .015f * s,
                    BossMesh.glow(GOLD_LIGHT, .8f), light);
        }
        // The bow, drawn back and forth across the strings
        float bx = (bow - .5f) * .8f * s;
        ClockMesh.rod(poseStack, buffer, VARNISH, new Vector3f(-1.1f * s + bx, -.1f * s, .14f * s), new Vector3f(1.1f * s + bx, .2f * s, .14f * s), .015f * s, WHITE,
                light);
        RitualMesh.shaft(BossMesh.light(buffer, BossMesh.BEAM), poseStack.last(), view.eye(), new Vector3f(-1.05f * s + bx, -.14f * s, .13f * s),
                new Vector3f(1.05f * s + bx, .16f * s, .13f * s), .03f * s, BossMesh.glow(0xFFFFFF, .6f), light);
    }
}
