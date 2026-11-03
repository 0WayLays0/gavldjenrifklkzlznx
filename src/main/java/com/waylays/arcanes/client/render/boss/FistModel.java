package com.waylays.arcanes.client.render.boss;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.waylays.arcanes.ArcanesOublies;
import com.waylays.arcanes.client.render.ritual.RitualMesh;
import com.waylays.arcanes.client.render.water.WaterMesh;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Titan's Fist: a colossal gauntlet of blackened plate, edged and riveted with gold, plunges out of a rune circle in the
 * sky, knuckles first, its arm a pillar of light reaching back up into the circle. Its fingers are plates on plates
 * curled into a fist, four spikes on its knuckles, runes engraved on the back of its hand burning brighter as it falls.
 * It strikes: the ground cracks open round it in burning fissures, rocks fly, dust rolls out; then it is drawn back up.
 */
public final class FistModel {
    private static final ResourceLocation IRON = BellModel.IRON;
    private static final ResourceLocation GOLD = BellModel.GOLD;
    private static final ResourceLocation ENGRAVING = ArcanesOublies.id("textures/entity/boss/m_engraving.png");
    private static final ResourceLocation CRACKS = ArcanesOublies.id("textures/entity/boss/m_cracks.png");
    private static final ResourceLocation ROCK = ArcanesOublies.id("textures/entity/boss/m_obsidian.png");
    private static final ResourceLocation RUNE_BAND = BellModel.RUNE_BAND;
    private static final int ROCKS = 22;
    /** How far it leans in as it strikes (radians). */
    private static final float TILT = .62f;

    private FistModel() {}

    /**
     * Where it lands (the pose's origin), coming from {@code from}; it lands {@code hit} ticks ago (negative before) after
     * falling {@code fall} ticks; facing {@code yaw} (degrees); {@code size} the blow's radius.
     */
    public static void draw(PoseStack poseStack, MultiBufferSource buffer, WaterMesh.View view, Vector3f from, float age, float hit, float fall, float yaw, float size,
                            int zone, int seed, int light) {
        // Hangs in the circle a moment, then falls ever faster; after the blow it rests, then is drawn back up
        float hang = Math.max(0, fall - 14);
        float down = hit < 0 ? Mth.clamp((age - hang) / Math.max(1, fall - hang), 0, 1) : 1;
        down = down * down * down;
        float rise = BossMesh.ramp(hit, 18, 40);
        if (rise >= 1) {
            return;
        }
        float s = size * .5f;
        // It strikes leaning toward the caster, its arm reaching back up beyond the target; it falls along its arm
        Quaternionf orient = new Quaternionf().rotationY(-yaw * Mth.DEG_TO_RAD).rotateY(Mth.PI).rotateX(-TILT);
        Vector3f arm = orient.transform(new Vector3f(0, 1, 0));
        Vector3f strike = orient.transform(new Vector3f(0, -1.3f, .1f)).mul(s);
        Vector3f land = new Vector3f(strike).negate().add(0, .05f, 0);
        float dist = (from.y + 4) * (1 - down) + rise * rise * 34;
        Vector3f at = new Vector3f(land).add(new Vector3f(arm).mul(dist));
        float heat = Mth.clamp(age / Math.max(1, fall), 0, 1);
        float glow = .35f + .65f * heat * heat * (1 - rise);
        // A shudder as it bites in
        float shudder = hit >= 0 && hit < 10 ? Mth.sin(hit * 2.6f) * .06f * (1 - hit / 10) : 0;
        Quaternionf facing = new Quaternionf(orient).rotateZ(shudder);

        // ---- the gauntlet's plates
        VertexConsumer iron = BossMesh.solid(buffer, IRON);
        place(poseStack, at, facing, s, shudder);
        plates(iron, poseStack, light);
        poseStack.popPose();
        VertexConsumer gold = BossMesh.solid(buffer, GOLD);
        place(poseStack, at, facing, s, shudder);
        trim(gold, poseStack, light);
        poseStack.popPose();
        // Rocks thrown up by the blow
        if (hit >= 0 && hit < 30) {
            VertexConsumer rock = BossMesh.solid(buffer, ROCK);
            for (int k = 0; k < ROCKS; k++) {
                float a = BossMesh.hash(seed + k * 7) * Mth.TWO_PI;
                float speed = .25f + BossMesh.hash(seed + k * 13) * .35f;
                float lift = .45f + BossMesh.hash(seed + k * 17) * .5f;
                float t = hit;
                float y = lift * t - .045f * t * t;
                if (y < -1) {
                    continue;
                }
                float r = s * .8f + speed * t;
                poseStack.pushPose();
                poseStack.translate(Mth.cos(a) * r, Math.max(.2f, y + .3f), Mth.sin(a) * r);
                poseStack.mulPose(new Quaternionf().rotationXYZ(t * .3f + k, t * .2f, k * 1.7f));
                float big = .14f + BossMesh.hash(seed + k * 19) * .3f;
                poseStack.scale(big, big * (.7f + BossMesh.hash(seed + k) * .6f), big);
                Sculpt.gem(rock, poseStack.last(), 1, .7f, .9f, 5, 0xFFB0A090, light);
                poseStack.popPose();
            }
        }

        // ---- light: the engraving, the runes round the cuff, the arm of light, the circle, the cracks
        VertexConsumer engraving = BossMesh.light(buffer, ENGRAVING);
        place(poseStack, at, facing, s, shudder);
        PoseStack.Pose p = poseStack.last();
        int g = BossMesh.glow(zone, glow);
        Sculpt.quad(engraving, p, new Vector3f(-.75f, -.15f, .615f), new Vector3f(.75f, -.15f, .615f), new Vector3f(.75f, .95f, .615f), new Vector3f(-.75f, .95f, .615f), g,
                light);
        poseStack.popPose();
        VertexConsumer runes = BossMesh.light(buffer, RUNE_BAND);
        place(poseStack, at, facing, s, shudder);
        Sculpt.lathe(runes, poseStack.last(), new float[] {1.01f, 2.3f, 1.06f, 2.62f}, 32, 3, 1 / .32f, g, g, light);
        poseStack.popPose();
        VertexConsumer beam = BossMesh.light(buffer, BossMesh.BEAM);
        float armFade = (1 - rise) * Mth.clamp(age / 6, 0, 1);
        Vector3f cuff = new Vector3f(at).add(new Vector3f(arm).mul(s * 3.1f));
        Vector3f sky = new Vector3f(land).add(new Vector3f(arm).mul(from.y + 10));
        RitualMesh.shaft(beam, poseStack.last(), view.eye(), cuff, sky, s * 1.7f, BossMesh.glow(zone, .5f * armFade), light);
        RitualMesh.shaft(beam, poseStack.last(), view.eye(), cuff, sky, s * .7f, BossMesh.glow(0xFFFFFF, .45f * armFade), light);
        // Streaks of its fall
        if (hit < 0 && down > .05f) {
            for (int k = 0; k < 8; k++) {
                float a = k * Mth.TWO_PI / 8 + seed;
                Vector3f a0 = new Vector3f(at).add(Mth.cos(a) * s * 1.1f, s * .6f, Mth.sin(a) * s * 1.1f);
                RitualMesh.shaft(beam, poseStack.last(), view.eye(), a0, new Vector3f(a0).add(new Vector3f(arm).mul(4 + down * 10)), .35f, BossMesh.glow(zone, .5f * down),
                        light);
            }
        }
        VertexConsumer circle = BossMesh.light(buffer, BossMesh.RUNES);
        Vector3f circleAt = new Vector3f(land).add(new Vector3f(arm).mul(from.y + 8));
        Vector3f cu = BossMesh.across(arm), cv = new Vector3f(arm).cross(cu).normalize();
        BossMesh.annulus(circle, poseStack.last(), circleAt, cu, cv, s * 1.2f, s * 3.4f, 48, 3, age * .03f,
                0, BossMesh.glow(zone, .9f * (1 - rise)), BossMesh.glow(zone, .3f * (1 - rise)), light, true);
        if (hit >= 0) {
            float spread = BossMesh.ramp(hit, 0, 8);
            float fade = 1 - BossMesh.ramp(hit, 26, 40);
            VertexConsumer cracks = BossMesh.light(buffer, CRACKS);
            BossMesh.pool(cracks, poseStack.last(), 0, .06f, 0, size * 2.6f * (.4f + .6f * spread), seed, BossMesh.glow(zone, fade), light);
            VertexConsumer ring = BossMesh.light(buffer, BossMesh.RING);
            float w = BossMesh.ramp(hit, 0, 12);
            BossMesh.pool(ring, poseStack.last(), 0, .1f, 0, size * (1 + w * 4), 0, BossMesh.glow(0xFFFFFF, (1 - w) * .9f), light);
            BossMesh.pool(ring, poseStack.last(), 0, .12f, 0, size * (1 + w * 2.6f), 1, BossMesh.glow(zone, (1 - w)), light);
            VertexConsumer dust = BossMesh.see(buffer, BossMesh.CLOUD);
            for (int k = 0; k < 12; k++) {
                float a = k * Mth.TWO_PI / 12 + BossMesh.hash(seed + k) * .4f;
                float r = s * (1 + 2.6f * BossMesh.ramp(hit, 0, 22));
                RitualMesh.billboard(dust, poseStack, view, new Vector3f(Mth.cos(a) * r, .8f + k % 3 * .4f, Mth.sin(a) * r), 3.5f + hit * .12f, k + hit * .02f, -1,
                        BossMesh.argb(0x6A5C58, .55f * fade), light);
            }
        }
        VertexConsumer halo = BossMesh.light(buffer, BossMesh.GLOW);
        RitualMesh.billboard(halo, poseStack, view, new Vector3f(at), s * 5, 0, -1, BossMesh.glow(zone, .25f * glow), light);
    }

    /** To the gauntlet's own space: its middle at {@code at}, turned by {@code facing}, {@code s} blocks a unit. */
    private static void place(PoseStack poseStack, Vector3f at, Quaternionf facing, float s, float shudder) {
        poseStack.pushPose();
        poseStack.translate(at.x, at.y, at.z);
        poseStack.mulPose(facing);
        poseStack.scale(s, s, s);
    }

    /** A plate {@code w} by {@code len} by {@code d} along the line from {@code a} to {@code b}, its face to +Z. */
    private static void plate(VertexConsumer c, PoseStack poseStack, Vector3f a, Vector3f b, float w, float d, float bevel, int argb, int light) {
        Vector3f dir = new Vector3f(b).sub(a);
        float len = dir.length();
        poseStack.pushPose();
        poseStack.translate((a.x + b.x) * .5f, (a.y + b.y) * .5f, (a.z + b.z) * .5f);
        poseStack.mulPose(new Quaternionf().rotationTo(new Vector3f(0, 1, 0), dir.normalize()));
        Sculpt.block(c, poseStack.last(), w, len, d, bevel, .6f, argb, light);
        poseStack.popPose();
    }

    /** The finger joints of the fist, knuckle to tip, for the finger at {@code x}: closed, the tips tucked into the palm. */
    private static Vector3f[] finger(float x, float len) {
        return new Vector3f[] {new Vector3f(x, -.35f, .5f), new Vector3f(x, -1.05f * len, .28f), new Vector3f(x, -1.0f * len, -.42f), new Vector3f(x, -.5f, -.62f)};
    }

    private static final float[] FINGERS = {-.78f, -.26f, .26f, .78f};
    private static final float[] LENGTHS = {.92f, 1f, 1f, .96f};

    private static void plates(VertexConsumer c, PoseStack poseStack, int light) {
        PoseStack.Pose p = poseStack.last();
        // The back of the hand and the palm
        poseStack.pushPose();
        poseStack.translate(0, .4f, 0);
        Sculpt.block(c, poseStack.last(), 2.0f, 1.3f, 1.2f, .14f, .5f, 0xFFFFFFFF, light);
        poseStack.popPose();
        // The fingers, joint by joint, and their knuckle caps
        for (int f = 0; f < 4; f++) {
            Vector3f[] j = finger(FINGERS[f], LENGTHS[f]);
            for (int k = 0; k < 3; k++) {
                plate(c, poseStack, j[k], j[k + 1], .48f, .56f, .08f, 0xFFFFFFFF, light);
            }
            for (int k = 1; k < 3; k++) {
                poseStack.pushPose();
                poseStack.translate(j[k].x, j[k].y, j[k].z);
                poseStack.mulPose(new Quaternionf().rotationZ(Mth.HALF_PI));
                Sculpt.lathe(c, poseStack.last(), new float[] {.2f, -.25f, .27f, -.18f, .29f, 0, .27f, .18f, .2f, .25f}, 12, 1, 1, 0xFFE0D8E8, 0xFFE0D8E8, light);
                poseStack.popPose();
            }
        }
        // The thumb, folded across the front of the fingers
        Vector3f t0 = new Vector3f(1.05f, .1f, .35f), t1 = new Vector3f(.95f, -.55f, .78f), t2 = new Vector3f(.05f, -.72f, .86f);
        plate(c, poseStack, t0, t1, .55f, .5f, .07f, 0xFFFFFFFF, light);
        plate(c, poseStack, t1, t2, .5f, .46f, .07f, 0xFFFFFFFF, light);
        // The wrist: lames one over the next, and the vambrace flaring up to its cuff
        Sculpt.lathe(c, p, new float[] {.8f, .95f, .84f, 1.1f, .78f, 1.3f, .86f, 1.35f, .9f, 1.55f, .84f, 1.7f, .93f, 1.75f, .97f, 2.0f, .95f, 2.1f}, 28, 3, .6f,
                0xFFFFFFFF, 0xFFFFFFFF, light);
        Sculpt.lathe(c, p, new float[] {.95f, 2.1f, .97f, 2.2f, 1.0f, 2.55f, 1.15f, 2.78f, 1.32f, 2.9f, 1.28f, 3.0f, 1.05f, 2.98f, .95f, 3.1f, .7f, 3.18f, 0, 3.2f}, 28, 3, .6f,
                0xFFFFFFFF, 0xFF8A8296, light);
    }

    private static void trim(VertexConsumer c, PoseStack poseStack, int light) {
        PoseStack.Pose p = poseStack.last();
        // The knuckle guard: a gilded bar over the knuckles, a spike on each
        plate(c, poseStack, new Vector3f(-1.08f, -.42f, .62f), new Vector3f(1.08f, -.42f, .62f), .34f, .22f, .06f, 0xFFFFFFFF, light);
        for (int f = 0; f < 4; f++) {
            Vector3f base = new Vector3f(FINGERS[f], -.62f, .55f);
            BossMesh.horn(c, p, base, new Vector3f(0, -1, .35f).normalize(), new Vector3f(0, 0, -.2f), .62f, .14f, 6, 4, 0xFFFFFFFF, 0xFFFFF0C0, light);
        }
        // Gilded edges round the cuff and the wrist lames, rivets on the back of the hand
        for (float[] ring : new float[][] {{.84f, 1.1f, .05f}, {.9f, 1.55f, .05f}, {.97f, 2.0f, .05f}, {1.32f, 2.91f, .07f}, {.99f, 2.25f, .04f}}) {
            poseStack.pushPose();
            poseStack.translate(0, ring[1], 0);
            Sculpt.torus(c, poseStack.last(), ring[0], ring[2], 32, 6, 0, Mth.TWO_PI, 8, 0xFFFFFFFF, light);
            poseStack.popPose();
        }
        for (int k = 0; k < 4; k++) {
            for (int side = -1; side <= 1; side += 2) {
                poseStack.pushPose();
                poseStack.translate(side * .86f, -.1f + k * .33f, .61f);
                poseStack.mulPose(new Quaternionf().rotationX(Mth.HALF_PI));
                Sculpt.lathe(c, poseStack.last(), new float[] {.08f, -.01f, .07f, .04f, 0, .055f}, 8, 1, 1, 0xFFFFFFFF, 0xFFFFFFFF, light);
                poseStack.popPose();
            }
        }
        // A gem set in the back of the hand, and the cuff's crest
        poseStack.pushPose();
        poseStack.translate(0, .4f, .62f);
        poseStack.mulPose(new Quaternionf().rotationX(Mth.HALF_PI));
        Sculpt.gem(c, poseStack.last(), .2f, .1f, .06f, 8, 0xFFFF6A50, light);
        poseStack.popPose();
        for (int k = 0; k < 6; k++) {
            float a = k * Mth.TWO_PI / 6 + Mth.HALF_PI;
            Vector3f base = new Vector3f(Mth.cos(a) * 1.28f, 2.92f, Mth.sin(a) * 1.28f);
            BossMesh.horn(c, p, base, new Vector3f(Mth.cos(a), 1.2f, Mth.sin(a)).normalize(), new Vector3f(0, .2f, 0), .5f, .1f, 5, 3, 0xFFFFFFFF, 0xFFFFF0C0, light);
        }
    }
}
