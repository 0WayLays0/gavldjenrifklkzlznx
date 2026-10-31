package com.waylays.arcanes.client.render.ritual;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

import static com.waylays.arcanes.client.render.water.WaterMesh.*;

/**
 * The Stone Colossus, built of stones (real blocks in game: cobblestone, mossy cobblestone, stone, andesite, cobbled
 * deepslate, tuff): broad and stooped, short thick legs, long arms hanging to its knees ending in great fists, a small
 * head sunk between boulder shoulders, a heart of amethyst burning in a gap of its chest, eyes of the same light, a few
 * pebbles circling it held by the same magic.
 * <p>
 * Raised, its stones tear out of the ground all round one by one, feet first, and fly up into their places, turning
 * as they come; then its heart and eyes light. Walking it rolls its shoulders, arms swinging. Striking, it heaves both
 * fists over its head and brings them down before it, its body bending into the blow. Falling apart, the light goes
 * out and its stones drop, the head first, tumbling, sinking into the ground.
 */
public final class ColossusModel {
    /** Where its stones go: one block-sized cube (0 to 1 on each axis) drawn in the pose, of a kind (0 to 5). */
    public interface Stones {
        void stone(PoseStack poseStack, int kind);
    }

    public static final int COBBLE = 0, MOSSY = 1, STONE = 2, ANDESITE = 3, DEEPSLATE = 4, TUFF = 5;
    public static final float ASSEMBLE = 60, CRUMBLE = 36, SLAM = 16;
    private static final int AMETHYST = 0xB27AFF;

    private ColossusModel() {}

    /** One stone of its body: where its middle is, how it is turned, its size, its kind. */
    private record Piece(Vector3f centre, Quaternionf turn, Vector3f size, int kind) {}

    /**
     * In the pose's frame, standing on its origin facing {@code +z}. {@code age} ticks since raised; {@code walk} and
     * {@code stride} its walk cycle and how hard it walks (0 to 1); {@code sinceSlam} ticks since its fists last came
     * down (large when they have not lately); {@code sinceEnd} ticks since it began to fall apart, or -1.
     */
    public static void draw(PoseStack poseStack, MultiBufferSource buffer, Stones stones, View view, float age, float walk, float stride, float sinceSlam,
                            float sinceEnd, int seed, int light) {
        List<Piece> body = new ArrayList<>();
        Vector3f heart = pose(body, age, walk, stride, sinceSlam, seed);
        int n = body.size();
        Vector3f eyeL = new Vector3f(), eyeR = new Vector3f();
        Piece head = body.get(n - 2);
        head.turn().transform(eyeL.set(-.22f, .05f, .46f)).add(head.centre());
        head.turn().transform(eyeR.set(.22f, .05f, .46f)).add(head.centre());

        for (int k = 0; k < n; k++) {
            Piece pc = body.get(k);
            Vector3f at = new Vector3f(pc.centre());
            Quaternionf turn = new Quaternionf(pc.turn());
            // Rising out of the ground into place
            float start = k / (float) n * ASSEMBLE * .62f, flight = ASSEMBLE * .3f;
            float t = Mth.clamp((age - start) / flight, 0, 1);
            if (t < 1) {
                float a = hash(seed + k * 7) * Mth.TWO_PI, r = 3 + 3 * hash(seed + k * 11);
                Vector3f from = new Vector3f(Mth.cos(a) * r, -1.4f, Mth.sin(a) * r);
                float e = t * t * (3 - 2 * t);
                Vector3f ctrl = new Vector3f(from).lerp(at, .5f).add(0, 3 + 2 * hash(seed + k), 0);
                at = bezier(from, ctrl, pc.centre(), e);
                turn = new Quaternionf().rotationXYZ((1 - e) * 6 * hash(seed + k * 3), (1 - e) * 5 * hash(seed + k * 5), (1 - e) * 4).mul(pc.turn());
            }
            // Falling apart: the highest first
            if (sinceEnd >= 0) {
                float drop = sinceEnd - (n - 1 - k) / (float) n * CRUMBLE * .35f;
                if (drop > 0) {
                    float out = .06f * drop;
                    float a = hash(seed + k * 13) * Mth.TWO_PI;
                    at.add(Mth.cos(a) * out, -.02f * drop * drop, Mth.sin(a) * out);
                    turn = new Quaternionf().rotationXYZ(drop * .09f * (hash(seed + k) - .5f) * 4, 0, drop * .06f).mul(turn);
                }
            }
            poseStack.pushPose();
            poseStack.translate(at.x, at.y, at.z);
            poseStack.mulPose(turn);
            poseStack.scale(pc.size().x, pc.size().y, pc.size().z);
            poseStack.translate(-.5f, -.5f, -.5f);
            stones.stone(poseStack, pc.kind());
            poseStack.popPose();
        }

        // Its heart and eyes: lit once it stands, gone as it falls
        float lit = Mth.clamp((age - ASSEMBLE * .85f) / 10, 0, 1) * (sinceEnd < 0 ? 1 : Math.max(0, 1 - sinceEnd / 8));
        if (lit <= 0) {
            return;
        }
        float pulse = .8f + .2f * Mth.sin(age * .15f);
        float strike = sinceSlam < SLAM ? Math.max(0, 1 - Math.abs(sinceSlam - SLAM * .5f) / 3) : 0;
        VertexConsumer crystal = buffer.getBuffer(RenderType.entityTranslucent(RitualMesh.CRYSTAL));
        PoseStack.Pose p = poseStack.last();
        for (int k = 0; k < 5; k++) {
            float a = k * 1.3f;
            Vector3f dir = new Vector3f(.5f * Mth.cos(a), .6f + .3f * hash(seed + k), .5f * Mth.sin(a) + .4f).normalize();
            RitualMesh.crystal(crystal, p, new Vector3f(heart).add(0, -.25f, -.15f), dir, (.45f + .2f * hash(seed + k * 3)) * lit, .11f * lit, a, argb(0xFFFFFF, .95f), 0xF000F0);
        }
        VertexConsumer glow = RitualMesh.light(buffer, RitualMesh.GLOW);
        RitualMesh.billboard(glow, poseStack, view, heart, (1.6f + 1.4f * strike) * pulse * lit, 0, -1, glow(AMETHYST, .9f * lit), light);
        RitualMesh.billboard(glow, poseStack, view, heart, (4 + 3 * strike) * lit, 0, -1, glow(AMETHYST, .35f * lit), light);
        RitualMesh.billboard(glow, poseStack, view, heart, .6f * lit, 0, -1, glow(0xFFFFFF, lit), light);
        for (Vector3f e : new Vector3f[] {eyeL, eyeR}) {
            RitualMesh.billboard(glow, poseStack, view, e, .55f * lit, 0, -1, glow(AMETHYST, lit), light);
            RitualMesh.billboard(glow, poseStack, view, e, .2f * lit, 0, -1, glow(0xFFFFFF, lit), light);
        }
        // Motes rising off its heart
        VertexConsumer motes = RitualMesh.light(buffer, RitualMesh.MOTE);
        for (int k = 0; k < 8; k++) {
            float t = (age * .03f + hash(seed + k * 17)) % 1;
            Vector3f m = new Vector3f(heart).add((hash(seed + k * 19) - .5f) * 1.2f, 1.2f * t, .3f + .3f * t);
            RitualMesh.billboard(motes, poseStack, view, m, .25f * (1 - t), 0, -1, glow(AMETHYST, Mth.sin(t * Mth.PI) * lit), light);
        }
    }

    private static Vector3f bezier(Vector3f a, Vector3f c, Vector3f b, float t) {
        float u = 1 - t;
        return new Vector3f(a).mul(u * u).add(new Vector3f(c).mul(2 * u * t)).add(new Vector3f(b).mul(t * t));
    }

    /** Lays out its stones for this moment (feet first, head and its brow last) and gives where its heart is. */
    private static Vector3f pose(List<Piece> body, float age, float walk, float stride, float sinceSlam, int seed) {
        float swing = Mth.sin(walk * .55f) * .55f * stride;
        float bob = Math.abs(Mth.cos(walk * .55f)) * .12f * stride;
        float breath = Mth.sin(age * .06f) * .03f;
        // The blow: heave both fists overhead, bring them down before it, rise again
        float raise = 0, lean = .12f;
        if (sinceSlam < SLAM) {
            float t = sinceSlam / SLAM;
            if (t < .45f) {
                float e = t / .45f;
                raise = -2.7f * e * e * (3 - 2 * e);
                lean = .12f - .2f * e;
            } else if (t < .55f) {
                float e = (t - .45f) / .1f;
                raise = Mth.lerp(e * e, -2.7f, -.85f);
                lean = Mth.lerp(e, -.08f, .55f);
            } else {
                float e = (t - .55f) / .45f;
                e = e * e * (3 - 2 * e);
                raise = Mth.lerp(e, -.85f, 0);
                lean = Mth.lerp(e, .55f, .12f);
            }
        }
        // Legs
        for (int side = -1; side <= 1; side += 2) {
            float a = swing * side;
            Vector3f hip = new Vector3f(side * .78f, 2.45f + bob, 0);
            Quaternionf leg = new Quaternionf().rotationX(a);
            Vector3f knee = leg.transform(new Vector3f(0, -1.15f, 0)).add(hip);
            Quaternionf shinTurn = new Quaternionf().rotationX(a * .5f + Math.max(0, -a) * .6f);
            Vector3f ankle = shinTurn.transform(new Vector3f(0, -1.0f, 0)).add(knee);
            body.add(new Piece(new Vector3f(ankle.x, Math.max(.2f, ankle.y - .2f), ankle.z + .15f), new Quaternionf().rotationY(.06f * side),
                    new Vector3f(1.05f, .42f, 1.35f), DEEPSLATE));
            body.add(new Piece(leg(knee, ankle), shinTurn, new Vector3f(.82f, 1.1f, .82f), COBBLE));
            body.add(new Piece(leg(hip, knee), leg, new Vector3f(.95f, 1.25f, .95f), side < 0 ? MOSSY : ANDESITE));
        }
        // Body: everything above the hips bends about them
        Vector3f waist = new Vector3f(0, 2.6f + bob, 0);
        Quaternionf trunk = new Quaternionf().rotationX(lean).rotateZ(swing * .08f);
        body.add(new Piece(new Vector3f(waist).add(0, -.1f, 0), new Quaternionf().rotationZ(swing * .05f), new Vector3f(2.1f, .75f, 1.15f), DEEPSLATE));
        body.add(new Piece(at(waist, trunk, 0, .85f, -.05f), new Quaternionf(trunk), new Vector3f(1.6f, .95f, 1.2f), STONE));
        body.add(new Piece(at(waist, trunk, -.62f, 1.95f + breath, -.1f), new Quaternionf(trunk).rotateZ(.08f), new Vector3f(1.25f, 1.35f, 1.4f), COBBLE));
        body.add(new Piece(at(waist, trunk, .62f, 1.95f + breath, -.1f), new Quaternionf(trunk).rotateZ(-.08f), new Vector3f(1.25f, 1.35f, 1.4f), MOSSY));
        body.add(new Piece(at(waist, trunk, 0, 2.15f, -.55f), new Quaternionf(trunk), new Vector3f(1.1f, 1.1f, .7f), TUFF));
        Vector3f heart = at(waist, trunk, 0, 1.85f + breath, .55f);
        // Arms: shoulders, upper arms, forearms and fists, swinging against the legs (or heaving for the blow)
        for (int side = -1; side <= 1; side += 2) {
            Vector3f shoulder = at(waist, trunk, side * 1.62f, 2.45f + breath, 0);
            body.add(new Piece(new Vector3f(shoulder).add(0, .15f, 0), new Quaternionf(trunk).rotateY(.3f * side).rotateZ(-.15f * side),
                    new Vector3f(1.15f, 1.1f, 1.15f), side < 0 ? MOSSY : COBBLE));
            float a = raise != 0 ? raise : -swing * side * .7f + Mth.sin(age * .05f + side) * .04f;
            Quaternionf arm = new Quaternionf(trunk).rotateX(a).rotateZ(-.12f * side);
            Vector3f elbow = arm.transform(new Vector3f(0, -1.55f, 0)).add(shoulder);
            Quaternionf fore = new Quaternionf(arm).rotateX(raise != 0 ? -.15f : -.25f);
            Vector3f wrist = fore.transform(new Vector3f(0, -1.35f, 0)).add(elbow);
            body.add(new Piece(leg(shoulder, elbow), arm, new Vector3f(.75f, 1.45f, .75f), STONE));
            body.add(new Piece(leg(elbow, wrist), fore, new Vector3f(.85f, 1.3f, .85f), ANDESITE));
            body.add(new Piece(fore.transform(new Vector3f(0, -.5f, .05f)).add(wrist), fore, new Vector3f(1.15f, 1.0f, 1.15f), side < 0 ? COBBLE : DEEPSLATE));
        }
        // Pebbles held round it
        for (int k = 0; k < 4; k++) {
            float a = age * .04f + k * Mth.HALF_PI;
            Vector3f c = new Vector3f(heart).add(Mth.cos(a) * 1.9f, .5f * Mth.sin(age * .05f + k), Mth.sin(a) * 1.4f);
            body.add(new Piece(c, new Quaternionf().rotationXYZ(age * .05f + k, age * .03f, k), new Vector3f(.3f), k % 2 == 0 ? STONE : TUFF));
        }
        // Head, sunk between the shoulders, and its brow (the last two pieces)
        Quaternionf look = new Quaternionf(trunk).rotateX(-lean * .6f);
        Vector3f neck = at(waist, trunk, 0, 2.75f + breath, .25f);
        body.add(new Piece(look.transform(new Vector3f(0, .3f, .1f)).add(neck), look, new Vector3f(.95f, .85f, .95f), COBBLE));
        body.add(new Piece(look.transform(new Vector3f(0, .58f, .48f)).add(neck), look, new Vector3f(1.05f, .28f, .35f), MOSSY));
        return heart;
    }

    /** Halfway between two joints. */
    private static Vector3f leg(Vector3f a, Vector3f b) {
        return new Vector3f(a).add(b).mul(.5f);
    }

    /** {@code (x, y, z)} from {@code origin}, turned by {@code turn}. */
    private static Vector3f at(Vector3f origin, Quaternionf turn, float x, float y, float z) {
        return turn.transform(new Vector3f(x, y, z)).add(origin);
    }
}
