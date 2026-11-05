package com.waylays.arcanes.client.render.melody;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.waylays.arcanes.client.render.boss.BossMesh;
import com.waylays.arcanes.client.render.ritual.RitualMesh;
import com.waylays.arcanes.client.render.water.WaterMesh;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import static com.waylays.arcanes.client.render.melody.MelodyMesh.GOLD;
import static com.waylays.arcanes.client.render.melody.MelodyMesh.GOLD_LIGHT;
import static com.waylays.arcanes.client.render.melody.MelodyMesh.MINT;
import static com.waylays.arcanes.client.render.melody.MelodyMesh.ROSE;
import static com.waylays.arcanes.client.render.melody.MelodyMesh.VIOLET;

/**
 * Music Magic's lighter pieces.
 * <ul>
 * <li>Note: a note of gold flying, rocking as it goes, a short staff of light trailing behind it; it bursts into little
 * notes where it stops.</li>
 * <li>Staff slash: five lines of light swept round in front of the caster like a blade, notes riding on them.</li>
 * <li>Soothing melody: a staff turning on the ground round the caster, notes rising round them, a soft green glow at
 * each healing note.</li>
 * <li>Crescendo: rings of sound rolling out, growing as they go, notes running round the leading ring.</li>
 * <li>Lullaby: a treble clef of gold turning over the ground, a staff circling under it, notes drifting down like snow.</li>
 * </ul>
 */
public final class MelodyModels {
    private MelodyModels() {}

    static Vector3f unit(Vector3f d) {
        Vector3f v = new Vector3f(d);
        if (v.lengthSquared() < 1.0E-6f) {
            v.set(0, 0, 1);
        }
        return v.normalize();
    }

    /** Square to {@code d}, flat on the ground. */
    static Vector3f side(Vector3f d) {
        Vector3f s = new Vector3f(d.z, 0, -d.x);
        return s.lengthSquared() < 1.0E-6f ? new Vector3f(1, 0, 0) : s.normalize();
    }

    /** Little notes flung out from the origin, {@code t} (0 to 1) of the way, and a flash. */
    public static void burst(PoseStack poseStack, MultiBufferSource buffer, WaterMesh.View view, float t, float size, int seed, int light) {
        if (t >= 1) {
            return;
        }
        float out = 1 - (1 - t) * (1 - t), fade = 1 - t;
        for (int k = 0; k < 6; k++) {
            float a = k * Mth.TWO_PI / 6 + seed;
            float up = BossMesh.hash(seed + k) * .8f + .3f;
            Vector3f at = new Vector3f(Mth.cos(a) * size * 1.6f * out, (up * out - out * out * .5f) * size * 1.6f, Mth.sin(a) * size * 1.6f * out);
            MelodyMesh.noteFacing(poseStack, buffer, view, at, MelodyMesh.kind(k + seed), .4f * Mth.sin(t * 6 + k), size * .4f * (1 - t * .5f),
                    MelodyMesh.hue(k * .17f), fade, light);
        }
        MelodyMesh.glow(poseStack, buffer, view, new Vector3f(), size * (1.2f + 2.5f * t), GOLD_LIGHT, fade * fade, light);
        MelodyMesh.glow(poseStack, buffer, view, new Vector3f(), size * (2.5f + 2.5f * t), ROSE, fade * fade * .5f, light);
        MelodyMesh.glints(poseStack, buffer, view, new Vector3f(0, -size, 0), size * 2.2f, size * 2.5f, 10, t * 40, fade, seed, light);
    }

    // ---------------------------------------------------------------- note

    public static void note(PoseStack poseStack, MultiBufferSource buffer, WaterMesh.View view, Vector3f dir, float age, boolean done, float since, int seed,
                            int light) {
        if (done) {
            burst(poseStack, buffer, view, since / 10f, .7f, seed, light);
            return;
        }
        Vector3f d = unit(dir);
        float grow = Math.min(1, age / 3f);
        int glow = MelodyMesh.hue(BossMesh.hash(seed) * .9f);
        MelodyMesh.noteFacing(poseStack, buffer, view, new Vector3f(), MelodyMesh.kind(seed), .35f * Mth.sin(age * .45f + seed), .75f * grow, glow, 1, light);
        // A short staff of light trailing behind it, waving
        int n = 10;
        Vector3f s = side(d);
        Vector3f[] pts = new Vector3f[n];
        float len = Math.min(3.2f, age * .9f);
        for (int i = 0; i < n; i++) {
            float t = (float) i / (n - 1);
            pts[i] = new Vector3f(d).mul(-t * len).add(new Vector3f(s).mul(.18f * Mth.sin(age * .5f - t * 5))).add(0, .12f * Mth.cos(age * .4f - t * 4), 0);
        }
        MelodyMesh.staff(BossMesh.light(buffer, MelodyMesh.STAFF), poseStack.last(), view.eye(), pts, null, .5f, .85f * grow, age * .1f, BossMesh.hash(seed), light);
        MelodyMesh.glints(poseStack, buffer, view, new Vector3f(d).mul(-.8f).add(0, -.3f, 0), .6f, .6f, 5, age, .8f, seed, light);
    }

    // ---------------------------------------------------------------- staff slash

    public static void staffSlash(PoseStack poseStack, MultiBufferSource buffer, WaterMesh.View view, Vector3f dir, float age, int life, float reach, float arc,
                                  int sweep, int seed, int light) {
        Vector3f d = unit(new Vector3f(dir.x, 0, dir.z));
        float facing = (float) Math.atan2(d.z, d.x);
        float t = Mth.clamp(age / sweep, 0, 1);
        float ease = 1 - (1 - t) * (1 - t);
        // Swept from the caster's right to their left
        float start = facing - arc * .5f, lead = start + arc * ease;
        float tail = Math.max(start, lead - 1.6f - arc * .4f * (1 - t));
        float fade = 1 - BossMesh.ramp(age, sweep, life);
        if (fade <= .01f) {
            return;
        }
        float y = 1.1f;
        PoseStack.Pose p = poseStack.last();
        float r = reach * .62f, w = reach * .55f;
        MelodyMesh.staffRing(buffer, p, y, r, w, tail, lead - tail, 28, fade, age * .15f, BossMesh.hash(seed), light);
        // Its leading edge brighter: a second, narrower staff on top
        MelodyMesh.staffRing(buffer, p, y + .01f, r, w * .9f, Math.max(tail, lead - .5f), Math.min(.5f, lead - tail), 8, fade, age * .15f, .6f, light);
        // Notes riding on the lines
        for (int k = 0; k < 5; k++) {
            float a = tail + (lead - tail) * (.15f + .7f * BossMesh.hash(seed + k * 3));
            float rr = r + (k - 2) * w * .17f;
            Vector3f at = new Vector3f(Mth.cos(a) * rr, y + .15f, Mth.sin(a) * rr);
            MelodyMesh.noteFacing(poseStack, buffer, view, at, MelodyMesh.kind(k + seed), .3f * Mth.sin(age * .6f + k), .5f, MelodyMesh.hue(k * .2f), fade, light);
        }
        Vector3f edge = new Vector3f(Mth.cos(lead) * r, y, Mth.sin(lead) * r);
        MelodyMesh.glow(poseStack, buffer, view, edge, 2.4f, GOLD_LIGHT, fade * .7f, light);
        MelodyMesh.glints(poseStack, buffer, view, new Vector3f(edge).add(0, -.6f, 0), 1.2f, 1.2f, 8, age * 2, fade, seed, light);
    }

    // ---------------------------------------------------------------- soothing melody

    public static void soothe(PoseStack poseStack, MultiBufferSource buffer, WaterMesh.View view, float age, int life, float radius, int every, int seed, int light) {
        float alpha = BossMesh.ramp(age, 0, 8) * (1 - BossMesh.ramp(age, life - 12, life));
        if (alpha <= .01f) {
            return;
        }
        PoseStack.Pose p = poseStack.last();
        // A staff turning on the ground round the caster, a soft green glow under it
        MelodyMesh.staffRing(buffer, p, .06f, 1.5f, .9f, age * .03f, Mth.TWO_PI, 48, alpha * .9f, age * .05f, .1f, light);
        MelodyMesh.ring(p, buffer, .04f, .2f, 1.9f, MINT, alpha * .35f, light);
        // At each healing note, a ring of soft light running out to the melody's reach
        float pulse = (age % every) / 10f;
        if (pulse < 1) {
            MelodyMesh.ring(p, buffer, .05f, radius * pulse * .9f, radius * pulse, MINT, alpha * (1 - pulse) * .6f, light);
        }
        // Notes rising round the caster
        for (int k = 0; k < 7; k++) {
            float u = ((age + k * 40f / 7) % 40) / 40f;
            float a = k * Mth.TWO_PI / 7 + u * 2.4f;
            float r = 1.1f + .2f * Mth.sin(u * 6 + k);
            Vector3f at = new Vector3f(Mth.cos(a) * r, .2f + u * 2.6f, Mth.sin(a) * r);
            float f = alpha * Mth.sin(u * Mth.PI);
            MelodyMesh.noteFacing(poseStack, buffer, view, at, MelodyMesh.kind(k + seed), .3f * Mth.sin(age * .2f + k), .5f, k % 2 == 0 ? MINT : ROSE, f, light);
        }
        MelodyMesh.glints(poseStack, buffer, view, new Vector3f(), 1.8f, 2.6f, 10, age, alpha, seed, light);
    }

    // ---------------------------------------------------------------- crescendo

    public static void crescendo(PoseStack poseStack, MultiBufferSource buffer, WaterMesh.View view, Vector3f dir, float age, boolean done, float since, int flight,
                                 float from, float to, int seed, int light) {
        Vector3f d = unit(dir);
        PoseStack.Pose p = poseStack.last();
        if (done) {
            float t = since / 10f;
            if (t < 1) {
                for (int k = 0; k < 3; k++) {
                    float r = to * (1 + t * (.6f + k * .3f));
                    MelodyMesh.soundRing(p, buffer, new Vector3f(d).mul(-k * .5f), d, r, .35f, MelodyMesh.hue(.6f + k * .1f), (1 - t) * (1 - t) * .9f, light);
                }
                burst(poseStack, buffer, view, t, 1.2f, seed, light);
            }
            return;
        }
        float t = Mth.clamp(age / flight, 0, 1);
        float lead = Mth.lerp(t, from, to);
        // Three rings, the leading one widest
        for (int k = 0; k < 3; k++) {
            float r = lead * (1 - k * .22f);
            float a = (.95f - k * .25f) * Math.min(1, age / 2f);
            MelodyMesh.soundRing(p, buffer, new Vector3f(d).mul(-k * .7f), d, r, .25f + .15f * t, MelodyMesh.hue(.05f + t * .55f - k * .08f), a, light);
            MelodyMesh.soundRing(p, buffer, new Vector3f(d).mul(-k * .7f), d, r * .9f, .5f, ROSE, a * .25f, light);
        }
        // Notes running round the leading ring
        Vector3f u = BossMesh.across(d), v = new Vector3f(d).cross(u).normalize();
        int notes = 6;
        for (int k = 0; k < notes; k++) {
            float a = k * Mth.TWO_PI / notes + age * .25f;
            Vector3f at = new Vector3f(u).mul(Mth.cos(a) * lead).add(new Vector3f(v).mul(Mth.sin(a) * lead));
            MelodyMesh.noteFacing(poseStack, buffer, view, at, MelodyMesh.kind(k + seed), .3f * Mth.sin(age * .5f + k), .45f + .35f * t, MelodyMesh.hue(k * .16f),
                    1, light);
        }
        MelodyMesh.glow(poseStack, buffer, view, new Vector3f(), 1.2f + lead, GOLD_LIGHT, .35f, light);
    }

    // ---------------------------------------------------------------- lullaby

    public static void lullaby(PoseStack poseStack, MultiBufferSource buffer, WaterMesh.View view, float age, int life, float radius, int rise, int seed, int light) {
        float up = BossMesh.ramp(age, 0, rise);
        float alpha = up * (1 - BossMesh.ramp(age, life - 20, life));
        if (alpha <= .01f) {
            return;
        }
        PoseStack.Pose p = poseStack.last();
        // On the ground: a staff all round the song's reach, a soft violet glow inside it
        MelodyMesh.staffRing(buffer, p, .06f, radius, 1.1f, -age * .01f, Mth.TWO_PI, 64, alpha * .85f, age * .03f, .3f, light);
        MelodyMesh.ring(p, buffer, .04f, 0, radius, VIOLET, alpha * .25f, light);
        // A staff circling under the clef, waving gently
        MelodyMesh.staffBand(buffer, p, 1.6f + .2f * Mth.sin(age * .05f), radius * .7f, .8f, age * .02f, Mth.TWO_PI * .8f, 56, .25f, age, alpha * .8f, age * .04f,
                .45f, light);
        // The clef, turning slowly over the middle
        float h = 2.8f + .25f * Mth.sin(age * .06f);
        poseStack.pushPose();
        poseStack.translate(0, h, 0);
        // Turned to the eye, swaying a little, so its curls are always seen
        poseStack.mulPose(new Quaternionf().rotationY((float) Math.atan2(view.eye().x, view.eye().z) + .3f * Mth.sin(age * .03f)));
        float s = 1.9f * up;
        poseStack.translate(0, -s * .45f, 0);
        MelodyMesh.clef(poseStack, buffer, s, light);
        poseStack.popPose();
        MelodyMesh.glow(poseStack, buffer, view, new Vector3f(0, h, 0), 4.5f, VIOLET, alpha * .55f, light);
        MelodyMesh.glow(poseStack, buffer, view, new Vector3f(0, h, 0), 2.2f, ROSE, alpha * .4f, light);
        // Notes drifting down like snow over the sleepers
        for (int k = 0; k < 14; k++) {
            float u = ((age * .6f + k * 60f / 14) % 60) / 60f;
            float a = BossMesh.hash(seed + k) * Mth.TWO_PI, r = radius * Mth.sqrt(BossMesh.hash(seed + k * 7)) * .9f;
            Vector3f at = new Vector3f(Mth.cos(a) * r + .4f * Mth.sin(u * 9 + k), 4.2f - u * 4.2f, Mth.sin(a) * r + .4f * Mth.cos(u * 7 + k));
            MelodyMesh.noteFacing(poseStack, buffer, view, at, MelodyMesh.kind(k), .5f * Mth.sin(u * 8 + k), .45f, k % 2 == 0 ? VIOLET : ROSE,
                    alpha * Mth.sin(u * Mth.PI), light);
        }
        MelodyMesh.glints(poseStack, buffer, view, new Vector3f(), radius, 3.5f, 16, age * .6f, alpha * .8f, seed, light);
    }

    /** A sound ring running out along the ground from {@code r0} to {@code r1}, {@code t} (0 to 1) of the way. */
    public static void shock(PoseStack.Pose p, MultiBufferSource buffer, float t, float r0, float r1, int rgb, float alpha, int light) {
        if (t < 0 || t >= 1) {
            return;
        }
        float r = Mth.lerp(1 - (1 - t) * (1 - t), r0, r1);
        MelodyMesh.ring(p, buffer, .06f, r * .82f, r, rgb, alpha * (1 - t), light);
        MelodyMesh.staffRing(buffer, p, .08f, r * .9f, Math.max(.4f, r * .12f), 0, Mth.TWO_PI, 48, alpha * (1 - t), 0, t, light);
    }

    /** A beam of light from {@code a} to {@code b}, glowing round. */
    public static void ray(PoseStack poseStack, MultiBufferSource buffer, WaterMesh.View view, Vector3f a, Vector3f b, float width, int rgb, float alpha, int light) {
        if (alpha <= .01f) {
            return;
        }
        VertexConsumer beam = BossMesh.light(buffer, BossMesh.BEAM);
        RitualMesh.shaft(beam, poseStack.last(), view.eye(), a, b, width, BossMesh.glow(rgb, alpha), light);
        RitualMesh.shaft(beam, poseStack.last(), view.eye(), a, b, width * .35f, BossMesh.glow(0xFFFFFF, alpha), light);
    }
}
