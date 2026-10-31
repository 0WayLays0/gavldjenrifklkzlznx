package com.waylays.arcanes.client.render.ritual;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.waylays.arcanes.client.render.ritual.RitualFigures.Shape;
import com.waylays.arcanes.client.render.ritual.RitualFigures.Sigil;
import com.waylays.arcanes.client.render.ritual.RitualFigures.Stroke;
import com.waylays.arcanes.client.render.ritual.RitualMesh.Plane;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

import static com.waylays.arcanes.client.render.water.WaterMesh.*;

/**
 * A ritual's figure, as it is drawn and as the ritual builds:
 * <ol>
 * <li>Drawn: the chalk lines trace themselves one after another, a spark of chalk dust at their tip, then the sigils
 * appear one by one.</li>
 * <li>Waiting: the chalk breathes a faint light; each station's ring blinks until someone kneels on it, then shines.</li>
 * <li>Channelling: the light runs along the lines like fire along a fuse, lighting each sigil it passes; motes of light
 * rise off the lit chalk; threads of light reach from the kneeling to the figure's heart, where an orb of light swells
 * with sigils turning round it; at last a shaft of light climbs from it into the sky (on a wall, the doorway fills with
 * light). The menhirs' stones rise out of the ground; crystals sprout along the path.</li>
 * <li>Cast: a flash, a ring of light racing out over the ground, the lines burning out. Broken: the lines flicker red,
 * break apart and die.</li>
 * </ol>
 */
public final class RitualFigureModel {
    public static final int DRAWING = 0, WAITING = 1, CHANNELING = 2, DONE = 3, FAILED = 4;
    /** Ticks the figure lingers after the ritual is cast or broken (as {@code RitualCircle.AFTERGLOW}). */
    public static final float AFTERGLOW = 80;

    private RitualFigureModel() {}

    /**
     * Draws figure {@code figure} on {@code plane} (in the pose's frame, the entity's position its origin);
     * {@code stations} where the participants kneel (on the ground), {@code occupied} which have someone (a bit each);
     * {@code state} as above, {@code since} ticks in it, {@code drawn} and {@code progress} (0 to 1) how far it is drawn
     * and channelled; {@code color} the ritual's.
     */
    public static void draw(PoseStack poseStack, MultiBufferSource buffer, View view, int figure, Plane plane, Vector3f[] stations, int occupied, int state,
                            float since, float drawn, float progress, float age, int color, int seed, int light) {
        Shape shape = RitualFigures.of(figure);
        Vector3f eye = view.eye();
        boolean wall = plane.n().y < .5f;
        float life = switch (state) {
            case DONE -> 1 - Mth.clamp((since - 30) / (AFTERGLOW - 30), 0, 1);
            case FAILED -> 1 - Mth.clamp(since / AFTERGLOW, 0, 1);
            default -> 1;
        };
        if (life <= 0) {
            return;
        }
        float p = state == DONE ? 1 : state == CHANNELING ? progress : 0;
        float strokesDrawn = Mth.clamp(drawn / .8f, 0, 1) * shape.total();
        float lit = state == CHANNELING ? Mth.clamp(p / .4f, 0, 1) * shape.total() : state == DONE ? shape.total() : 0;
        int hot = mix(color, 0xFFFFFF, .45f);
        // How bright the lit chalk is: rising as it builds, a flash once cast, dying after
        float bright = switch (state) {
            case CHANNELING -> .55f + .45f * p;
            case DONE -> Math.max(0, 1.4f - since / 30f);
            default -> 0;
        };

        // The chalk itself
        VertexConsumer chalk = RitualMesh.chalk(buffer);
        PoseStack.Pose pose = poseStack.last();
        int chalkColour = state == FAILED ? mix(RitualMesh.CHALK_WHITE, 0x3A3230, Mth.clamp(since / 20, 0, 1))
                : state == DONE ? mix(RitualMesh.CHALK_WHITE, mix(color, 0xFFFFFF, .7f), Math.max(0, 1 - since / 60)) : RitualMesh.CHALK_WHITE;
        for (Stroke st : shape.strokes()) {
            if (st.start() >= strokesDrawn) {
                break;
            }
            if (state == FAILED && broken(seed, st, since)) {
                continue;
            }
            RitualMesh.stroke(chalk, pose, plane, st.xs(), st.ys(), 0, strokesDrawn - st.start(), st.width(), .015f, .9f, argb(chalkColour, .92f * life), light);
        }
        // The stations' rings, on the ground
        Plane ground = Plane.ground(0);
        for (int i = 0; i < stations.length; i++) {
            float shown = Mth.clamp((drawn - .7f) / .1f, 0, 1);
            if (shown > 0) {
                ring(chalk, pose, Plane.of(stations[i], ground.u(), ground.v(), ground.n()), .6f, .07f, shown, .016f, argb(chalkColour, .9f * life), light);
            }
        }
        VertexConsumer chalkSigils = buffer.getBuffer(RenderType.entityTranslucent(RitualMesh.SIGILS));
        pose = poseStack.last();
        for (Sigil sg : shape.sigils()) {
            float shown = Mth.clamp((drawn - .8f - .2f * sg.at() + .05f) / .05f, 0, 1);
            if (shown > 0) {
                RitualMesh.sigil(chalkSigils, pose, plane, sg.x(), sg.y(), sg.size(), sg.angle(), sg.index(), .018f, argb(chalkColour, .92f * life * shown), light);
            }
        }
        for (int i = 0; i < stations.length; i++) {
            float shown = Mth.clamp((drawn - .7f) / .1f, 0, 1);
            if (shown > 0) {
                RitualMesh.sigil(chalkSigils, pose, Plane.of(stations[i], ground.u(), ground.v(), ground.n()), 0, 0, .55f, 0, RitualFigures.BOND, .017f,
                        argb(chalkColour, .9f * life * shown), light);
            }
        }

        // Its light
        VertexConsumer glow = RitualMesh.light(buffer, RitualMesh.CHALK_GLOW);
        pose = poseStack.last();
        for (Stroke st : shape.strokes()) {
            if (st.start() >= strokesDrawn) {
                break;
            }
            float w = st.width() * 4;
            switch (state) {
                case WAITING, DRAWING -> {
                    float k = state == WAITING ? .14f + .06f * Mth.sin(age * .12f) : .1f;
                    RitualMesh.stroke(glow, pose, plane, st.xs(), st.ys(), 0, strokesDrawn - st.start(), w, .03f, 1.4f, glow(color, k), light);
                }
                case CHANNELING, DONE -> {
                    float on = Math.min(lit, st.start() + st.length()) - st.start();
                    if (on > 0) {
                        RitualMesh.stroke(glow, pose, plane, st.xs(), st.ys(), 0, on, w, .03f, 1.4f, glow(state == DONE ? hot : color, bright * life), light);
                    }
                    if (on < st.length()) {
                        RitualMesh.stroke(glow, pose, plane, st.xs(), st.ys(), Math.max(0, on), st.length(), w, .03f, 1.4f, glow(color, .12f), light);
                    }
                }
                case FAILED -> {
                    if (!broken(seed, st, since)) {
                        float flicker = .5f + .5f * Mth.sin(age * 2.3f + st.start());
                        RitualMesh.stroke(glow, pose, plane, st.xs(), st.ys(), 0, st.length(), w, .03f, 1.4f, glow(RitualMesh.BROKEN, flicker * life), light);
                    }
                }
                default -> {}
            }
        }
        VertexConsumer sigilLight = RitualMesh.light(buffer, RitualMesh.SIGILS_GLOW);
        pose = poseStack.last();
        for (Sigil sg : shape.sigils()) {
            float on = switch (state) {
                case CHANNELING -> Mth.clamp((p - .2f - .25f * sg.at()) / .05f, 0, 1) * (.75f + .25f * Mth.sin(age * .2f + sg.at() * 9));
                case DONE -> bright;
                case WAITING -> .15f;
                default -> 0;
            };
            if (on > 0) {
                RitualMesh.sigil(sigilLight, pose, plane, sg.x(), sg.y(), sg.size() * 1.08f, sg.angle(), sg.index(), .032f, glow(state == DONE ? hot : color, on * life),
                        light);
            }
        }
        for (int i = 0; i < stations.length; i++) {
            boolean taken = (occupied & (1 << i)) != 0;
            if (drawn < .8f || state == FAILED) {
                continue;
            }
            float k = taken || state == DONE ? .9f * life : .2f + .2f * Mth.sin(age * .25f + i * 2);
            Plane at = Plane.of(stations[i], ground.u(), ground.v(), ground.n());
            RitualMesh.sigil(sigilLight, pose, at, 0, 0, .6f, 0, RitualFigures.BOND, .033f, glow(taken ? color : RitualMesh.CHALK_WHITE, k), light);
        }
        VertexConsumer ringLight = RitualMesh.light(buffer, RitualMesh.CHALK_GLOW);
        pose = poseStack.last();
        for (int i = 0; i < stations.length; i++) {
            boolean taken = (occupied & (1 << i)) != 0;
            if (drawn < .8f || state == FAILED) {
                continue;
            }
            float k = taken || state == DONE ? .9f * life : .15f + .2f * Mth.sin(age * .25f + i * 2);
            Plane at = Plane.of(stations[i], ground.u(), ground.v(), ground.n());
            ring(ringLight, pose, at, .6f, .3f, 1, .031f, glow(taken ? color : RitualMesh.CHALK_WHITE, k), light);
        }

        VertexConsumer motes = RitualMesh.light(buffer, RitualMesh.MOTE);
        float[] pt = new float[2];
        // The tip of the chalk as it draws
        if (state == DRAWING && drawn < .8f) {
            RitualFigures.pointAt(shape, strokesDrawn, pt);
            Vector3f tip = plane.at(pt[0], pt[1], .1f);
            RitualMesh.billboard(motes, poseStack, view, tip, .5f, age * .3f, -1, glow(0xFFFFFF, .9f), light);
            for (int k = 0; k < 4; k++) {
                float t = (age * .15f + hash(seed + k * 7)) % 1;
                Vector3f d = new Vector3f(tip).add((hash(seed + k) - .5f) * t, .5f * t, (hash(seed + k * 3) - .5f) * t);
                RitualMesh.billboard(motes, poseStack, view, d, .18f * (1 - t), 0, -1, glow(RitualMesh.CHALK_WHITE, .6f * (1 - t)), light);
            }
        }
        // The fuse's spark, motes rising off the lit chalk
        if (state == CHANNELING && lit < shape.total()) {
            RitualFigures.pointAt(shape, lit, pt);
            RitualMesh.billboard(motes, poseStack, view, plane.at(pt[0], pt[1], .15f), .8f, age * .4f, -1, glow(hot, 1), light);
        }
        if ((state == CHANNELING && p > .1f) || (state == DONE && since < 40)) {
            int count = (int) (14 + 30 * p);
            float fade = state == DONE ? 1 - since / 40 : 1;
            for (int k = 0; k < count; k++) {
                float t = (age * (.012f + .01f * hash(seed + k * 13)) + hash(seed + k * 5)) % 1;
                RitualFigures.pointAt(shape, hash(seed + k * 3) * lit, pt);
                Vector3f m = plane.at(pt[0], pt[1], .1f);
                m.add(.25f * Mth.sin(age * .1f + k), 2.6f * t, .25f * Mth.cos(age * .1f + k));
                RitualMesh.billboard(motes, poseStack, view, m, .28f * (1 - t * .6f), 0, -1, glow(k % 3 == 0 ? hot : color, Mth.sin(t * Mth.PI) * fade * life), light);
            }
        }

        // The heart: where the light gathers
        Vector3f heart = wall ? plane.at(shape.heartX(), shape.heartY(), .35f) : plane.at(shape.heartX(), shape.heartY(), shape.altar() ? 2.1f : 1.6f);
        float gather = state == CHANNELING ? Mth.clamp((p - .45f) / .2f, 0, 1) : state == DONE ? 1 : 0;
        if (gather > 0) {
            // Threads from those kneeling to the heart, pulses running along them
            VertexConsumer beam = RitualMesh.light(buffer, RitualMesh.BEAM);
            PoseStack.Pose bp = poseStack.last();
            float threadFade = state == DONE ? Math.max(0, 1 - since / 20) : 1;
            for (int i = 0; i < stations.length; i++) {
                Vector3f a = new Vector3f(stations[i]).add(0, 1.1f, 0);
                Vector3f[] curve = new Vector3f[14];
                int[] cols = new int[14];
                Vector3f ctrl = new Vector3f(a).add(heart).mul(.5f).add(0, 1.8f, 0);
                for (int j = 0; j < 14; j++) {
                    float t = j / 13f * gather;
                    curve[j] = bezier(a, ctrl, heart, t);
                    cols[j] = glow(color, (.8f + .2f * Mth.sin(age * .5f - j * .6f + i)) * threadFade * life);
                }
                RitualMesh.thread(beam, bp, eye, curve, .4f, cols, light);
                for (int j = 0; j < 14; j++) {
                    cols[j] = glow(hot, threadFade * life);
                }
                RitualMesh.thread(beam, bp, eye, curve, .14f, cols, light);
            }
            VertexConsumer pulses = RitualMesh.light(buffer, RitualMesh.MOTE);
            for (int i = 0; i < stations.length && gather >= 1 && state == CHANNELING; i++) {
                Vector3f a = new Vector3f(stations[i]).add(0, 1.1f, 0);
                Vector3f ctrl = new Vector3f(a).add(heart).mul(.5f).add(0, 1.8f, 0);
                for (int k = 0; k < 3; k++) {
                    float t = (age * .03f + k / 3f + i * .17f) % 1;
                    RitualMesh.billboard(pulses, poseStack, view, bezier(a, ctrl, heart, t), .55f, 0, -1, glow(hot, 1), light);
                }
            }
            // The orb, sigils turning round it
            VertexConsumer orb = RitualMesh.light(buffer, RitualMesh.GLOW);
            float burst = state == DONE ? Math.max(0, 1 - since / 25) : 0;
            float size = (.6f + 1.8f * gather * (.6f + .4f * p)) * (1 + .08f * Mth.sin(age * .3f)) + burst * 9;
            float orbFade = state == DONE ? Math.max(0, 1 - since / 30) : 1;
            RitualMesh.billboard(orb, poseStack, view, heart, size * 2.4f, 0, -1, glow(color, .55f * orbFade * life), light);
            RitualMesh.billboard(orb, poseStack, view, heart, size, 0, -1, glow(hot, orbFade * life), light);
            RitualMesh.billboard(orb, poseStack, view, heart, size * .45f, 0, -1, glow(0xFFFFFF, orbFade * life), light);
            VertexConsumer turning = RitualMesh.light(buffer, RitualMesh.SIGILS_GLOW);
            int[] marks = {RitualFigures.EYE, RitualFigures.KEY, RitualFigures.STAR, RitualFigures.SUN};
            for (int k = 0; k < 4 && state == CHANNELING; k++) {
                float a = age * .05f + k * Mth.HALF_PI;
                float r = 1 + .9f * gather;
                Vector3f s = wall ? new Vector3f(heart).add(new Vector3f(plane.u()).mul(Mth.cos(a) * r)).add(new Vector3f(plane.v()).mul(Mth.sin(a) * r))
                        : new Vector3f(heart).add(Mth.cos(a) * r, .3f * Mth.sin(age * .1f + k), Mth.sin(a) * r);
                RitualMesh.billboard(turning, poseStack, view, s, .7f * gather, 0, marks[k], glow(hot, gather * life), light);
            }
        }

        // The shaft into the sky, or the doorway filling with light
        float rise = state == CHANNELING ? Mth.clamp((p - .8f) / .2f, 0, 1) : state == DONE ? Math.max(0, 1 - since / 40) : 0;
        if (rise > 0) {
            VertexConsumer beam = RitualMesh.light(buffer, RitualMesh.BEAM);
            PoseStack.Pose bp = poseStack.last();
            if (wall) {
                fillArch(RitualMesh.light(buffer, RitualMesh.GLOW), poseStack.last(), plane, shape, glow(color, .9f * rise * life), light);
            } else {
                float top = state == DONE ? 120 : 6 + 90 * rise * rise;
                float width = .35f + .6f * rise + (state == DONE ? 2.5f * rise : 0);
                Vector3f head = new Vector3f(heart).add(0, top, 0);
                RitualMesh.shaft(beam, bp, eye, heart, head, width * 2.4f, glow(color, .5f * life), light);
                RitualMesh.shaft(beam, bp, eye, heart, head, width, glow(hot, life), light);
                RitualMesh.shaft(beam, bp, eye, heart, head, width * .35f, glow(0xFFFFFF, life), light);
            }
        }

        // Cast: a ring of light racing out over the ground
        if (state == DONE && since < 40) {
            VertexConsumer shock = RitualMesh.light(buffer, RitualMesh.SHOCK);
            float r = 2 + since * .9f;
            float k = 1 - since / 40;
            Vector3f foot = wall ? plane.at(0, 0, 1.5f) : new Vector3f();
            RitualMesh.flat(shock, poseStack.last(), Plane.of(new Vector3f(foot).add(0, .06f, 0), ground.u(), ground.v(), ground.n()), 0, 0, r * 2, 0, 0,
                    glow(hot, k), light);
        }
        // Broken: red sparks off the dying lines
        if (state == FAILED && since < 40) {
            VertexConsumer sparks = RitualMesh.light(buffer, RitualMesh.MOTE);
            for (int k = 0; k < 24; k++) {
                float t = (since / 40 + hash(seed + k * 3) * .5f) % 1;
                RitualFigures.pointAt(shape, hash(seed + k * 7) * shape.total(), pt);
                Vector3f m = plane.at(pt[0], pt[1], .1f + 1.5f * t);
                RitualMesh.billboard(sparks, poseStack, view, m, .25f * (1 - t), 0, -1, glow(RitualMesh.BROKEN, (1 - t) * life), light);
            }
        }

        // The menhirs rising, the crystals sprouting
        if (figure == RitualFigures.MENHIRS) {
            menhirs(poseStack, buffer, view, plane, shape, state, since, p, age, color, seed, life, light);
        } else if (figure == RitualFigures.PATH) {
            crystals(poseStack, buffer, plane, shape, state, since, p, age, color, seed, life, light);
        }
    }

    /** Whether a stroke has broken off yet, the ritual having failed. */
    private static boolean broken(int seed, Stroke st, float since) {
        return hash(seed + (int) (st.start() * 13)) < since / 50;
    }

    private static Vector3f bezier(Vector3f a, Vector3f c, Vector3f b, float t) {
        float u = 1 - t;
        return new Vector3f(a).mul(u * u).add(new Vector3f(c).mul(2 * u * t)).add(new Vector3f(b).mul(t * t));
    }

    /** A ring on the plane round its origin. */
    private static void ring(VertexConsumer c, PoseStack.Pose p, Plane plane, float r, float width, float traced, float lift, int argb, int light) {
        int n = 20;
        float[] xs = new float[n + 1], ys = new float[n + 1];
        for (int i = 0; i <= n; i++) {
            float a = i * Mth.TWO_PI / n;
            xs[i] = Mth.cos(a) * r;
            ys[i] = Mth.sin(a) * r;
        }
        RitualMesh.stroke(c, p, plane, xs, ys, 0, traced * Mth.TWO_PI * r, width, lift, width < .2f ? .9f : 1.4f, argb, light);
    }

    /** The doorway drawn on the wall filling with light: a fan from its heart over the inner arch. */
    private static void fillArch(VertexConsumer c, PoseStack.Pose p, Plane plane, Shape shape, int argb, int light) {
        Stroke arch = shape.strokes().get(0);
        float hx = shape.heartX(), hy = shape.heartY();
        Vector3f centre = plane.at(hx, hy, .04f);
        Vector3f n = plane.n();
        for (int i = 0; i + 1 < arch.xs().length; i++) {
            float x0 = arch.xs()[i], y0 = arch.ys()[i], x1 = arch.xs()[i + 1], y1 = arch.ys()[i + 1];
            put(c, p, centre, .5f, .5f, argb, light, n);
            put(c, p, plane.at(x0, y0, .04f), .5f + (x0 - hx) * .1f, .5f - (y0 - hy) * .1f, argb, light, n);
            put(c, p, plane.at(x1, y1, .04f), .5f + (x1 - hx) * .1f, .5f - (y1 - hy) * .1f, argb, light, n);
            put(c, p, centre, .5f, .5f, argb, light, n);
        }
    }

    /** The standing stones of the menhirs: they rise out of the ground as the ritual builds, runes lit on their faces. */
    private static void menhirs(PoseStack poseStack, MultiBufferSource buffer, View view, Plane plane, Shape shape, int state, float since, float p, float age,
                                int color, int seed, float life, int light) {
        float rise = switch (state) {
            case CHANNELING -> Mth.clamp((p - .1f) / .5f, 0, 1);
            case DONE -> 1 - Mth.clamp((since - 30) / 40, 0, 1);
            default -> 0;
        };
        if (rise <= 0) {
            return;
        }
        VertexConsumer stone = buffer.getBuffer(RenderType.entityCutoutNoCull(RitualMesh.MENHIR));
        PoseStack.Pose pose = poseStack.last();
        float ease = rise * rise * (3 - 2 * rise);
        for (int k = 0; k < shape.stones().length; k++) {
            float[] s = shape.stones()[k];
            Vector3f at = plane.at(s[0], s[1], 0);
            float h = 2.4f + .5f * hash(seed + k);
            float a = (float) Mth.atan2(s[1], s[0]) + .3f * (hash(seed + k * 3) - .5f);
            Vector3f ax = new Vector3f(Mth.cos(a), 0, Mth.sin(a)), az = new Vector3f(-Mth.sin(a), 0, Mth.cos(a));
            Vector3f ay = new Vector3f(.06f * (hash(seed + k * 5) - .5f), 1, .06f * (hash(seed + k * 7) - .5f)).normalize();
            float top = h * ease;
            RitualMesh.block(stone, pose, at, ax, ay, az, -.32f, top - h, -.45f, .32f, top, .45f, 1, 0xFFFFFFFF, light);
        }
        VertexConsumer runes = RitualMesh.light(buffer, RitualMesh.SIGILS_GLOW);
        pose = poseStack.last();
        float on = state == CHANNELING ? Mth.clamp((p - .5f) / .2f, 0, 1) : Math.max(0, 1 - since / 40);
        for (int k = 0; k < shape.stones().length && on > 0; k++) {
            float[] s = shape.stones()[k];
            float h = 2.4f + .5f * hash(seed + k);
            float a = (float) Mth.atan2(s[1], s[0]) + .3f * (hash(seed + k * 3) - .5f);
            Vector3f in = new Vector3f(-Mth.cos(a), 0, -Mth.sin(a));
            Vector3f side = new Vector3f(-Mth.sin(a), 0, Mth.cos(a));
            Vector3f face = plane.at(s[0], s[1], 0).add(new Vector3f(in).mul(.335f));
            Plane rune = Plane.of(face, side, new Vector3f(0, 1, 0), in);
            float y = h * ease - h * .45f;
            RitualMesh.sigil(runes, pose, rune, 0, y, .5f, 0, k % 2 == 0 ? RitualFigures.STONE : RitualFigures.SPEAR, 0, glow(color, on * life), light);
        }
        // Dust round their feet as they rise
        if (state == CHANNELING && rise < 1) {
            VertexConsumer dust = translucent(buffer, MIST);
            for (int k = 0; k < shape.stones().length; k++) {
                float[] s = shape.stones()[k];
                Vector3f at = plane.at(s[0], s[1], .3f);
                puff(dust, poseStack, view, at.x, at.y, at.z, 1.4f, age * .02f + k, -1, argb(0x9A948A, .35f * (1 - rise)), light);
            }
        }
    }

    /** Amethyst crystals sprouting along the path as the ritual builds. */
    private static void crystals(PoseStack poseStack, MultiBufferSource buffer, Plane plane, Shape shape, int state, float since, float p, float age, int color,
                                 int seed, float life, int light) {
        float grow = switch (state) {
            case CHANNELING -> Mth.clamp((p - .3f) / .5f, 0, 1);
            case DONE -> 1 - Mth.clamp((since - 20) / 40, 0, 1);
            default -> 0;
        };
        if (grow <= 0) {
            return;
        }
        VertexConsumer crystal = buffer.getBuffer(RenderType.entityTranslucent(RitualMesh.CRYSTAL));
        PoseStack.Pose pose = poseStack.last();
        int n = shape.stones().length;
        for (int k = 0; k < n; k++) {
            float g = Mth.clamp(grow * n - k * .6f, 0, 1);
            if (g <= 0) {
                continue;
            }
            float[] s = shape.stones()[k];
            Vector3f at = plane.at(s[0], s[1], -.05f);
            for (int j = 0; j < 3; j++) {
                float a = hash(seed + k * 7 + j) * Mth.TWO_PI;
                Vector3f dir = new Vector3f(.35f * Mth.cos(a), 1, .35f * Mth.sin(a)).normalize();
                float len = (.6f + .5f * hash(seed + k * 3 + j)) * g * (j == 0 ? 1.3f : .8f);
                RitualMesh.crystal(crystal, pose, new Vector3f(at).add(.12f * Mth.cos(a), 0, .12f * Mth.sin(a)), dir, len, .14f * g, a, argb(0xFFFFFF, .9f * life),
                        light);
            }
        }
        VertexConsumer shine = RitualMesh.light(buffer, RitualMesh.GLOW);
        pose = poseStack.last();
        for (int k = 0; k < n; k++) {
            float g = Mth.clamp(grow * n - k * .6f, 0, 1);
            if (g > 0) {
                float[] s = shape.stones()[k];
                Vector3f at = plane.at(s[0], s[1], .5f * g);
                RitualMesh.flat(shine, pose, Plane.of(at, new Vector3f(1, 0, 0), new Vector3f(0, 0, 1), new Vector3f(0, 1, 0)), 0, 0, 1.6f * g, 0, 0,
                        glow(color, .5f * g * life * (.8f + .2f * Mth.sin(age * .2f + k))), light);
            }
        }
    }
}
