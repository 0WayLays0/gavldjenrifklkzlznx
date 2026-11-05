package com.waylays.arcanes.client.render.melody;

import com.mojang.blaze3d.vertex.PoseStack;
import com.waylays.arcanes.client.render.boss.BossMesh;
import com.waylays.arcanes.client.render.clockwork.ClockMesh;
import com.waylays.arcanes.client.render.water.WaterMesh;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.function.IntFunction;

import static com.waylays.arcanes.client.render.melody.MelodyMesh.GOLD;
import static com.waylays.arcanes.client.render.melody.MelodyMesh.GOLD_LIGHT;
import static com.waylays.arcanes.client.render.melody.MelodyMesh.ROSE;
import static com.waylays.arcanes.client.render.melody.MelodyMesh.VIOLET;
import static com.waylays.arcanes.client.render.melody.MelodyMesh.WHITE;

/**
 * Music Magic's phantom instruments.
 * <ul>
 * <li>War drum: a red drum bound in gold and laced with gold cord, beaten by two mallets of gold in turn; each beat
 * shakes its skin and sends a ring of sound out over the ground.</li>
 * <li>Harp of light: a harp of gold and varnished wood floating over the caster's shoulder, its strings of light plucked
 * one by one, each note a ray at an enemy.</li>
 * <li>Phantom trumpets: five trumpets of gold in an arc behind the caster, sounding three blasts, rings of sound
 * flying from their bells.</li>
 * <li>Storm organ: pipes of gold rising from a case of dark wood under an arch, a clef at its crown; each chord lights a
 * pipe and calls a pillar of light down on an enemy; then its last great chord.</li>
 * <li>Phantom orchestra: violins in a ring round the caster, their bows drawn, staves of light wheeling round the field
 * in all its colours, a great clef of gold turning overhead, notes falling on the enemies; then the finale.</li>
 * </ul>
 */
public final class MelodyInstrumentModels {
    private MelodyInstrumentModels() {}

    private static float yawOf(Vector3f d) {
        return (float) Math.atan2(d.x, d.z);
    }

    // ---------------------------------------------------------------- war drum

    public static void drum(PoseStack poseStack, MultiBufferSource buffer, WaterMesh.View view, Vector3f dir, float age, int life, int rise, int first, int every,
                            float reach, int seed, int light) {
        float up = BossMesh.ramp(age, 0, rise);
        float gone = BossMesh.ramp(age, life - 12, life);
        if (gone >= 1) {
            return;
        }
        // Which beat it is at, and how long since it
        float fromFirst = age - first;
        int beat = fromFirst < 0 ? -1 : (int) (fromFirst / every);
        float since = beat < 0 ? 1000 : fromFirst - beat * every;
        float shake = beat < 0 ? 0 : (float) Math.exp(-since / 3) * Mth.sin(since * 2.4f);
        float r = .95f, h = 1.15f;
        float yaw = yawOf(dir);
        Quaternionf turn = new Quaternionf().rotationY(yaw);
        poseStack.pushPose();
        poseStack.translate(0, -(1 - up) * 1.6f - gone * 1.6f, 0);
        poseStack.mulPose(turn);
        float squash = 1 + shake * .04f;
        poseStack.pushPose();
        poseStack.translate(0, h * .5f + .12f, 0);
        poseStack.scale(squash, 1 / squash, squash);
        MelodyMesh.drum(poseStack, buffer, r, h, shake, light);
        poseStack.popPose();
        // Three legs of gold under it
        for (int k = 0; k < 3; k++) {
            float a = k * Mth.TWO_PI / 3 + .5f;
            ClockMesh.rod(poseStack, buffer, MelodyMesh.GOLD_METAL, new Vector3f(Mth.cos(a) * r * .7f, .15f, Mth.sin(a) * r * .7f),
                    new Vector3f(Mth.cos(a) * r * 1.05f, 0, Mth.sin(a) * r * 1.05f), .05f, WHITE, light);
        }
        // Two mallets beating it in turn
        for (int m = 0; m < 2; m++) {
            int mine = beat < 0 ? -1 : (beat % 2 == m ? beat : beat - 1);
            float last = mine < 0 ? 1000 : fromFirst - mine * every;
            int next = mine < 0 ? (m == 0 ? 0 : 1) : mine + 2;
            float until = first + next * every - age;
            // Raised as its beat comes, brought down on it, bouncing back up after
            float lift;
            if (until < 7) {
                lift = 1.1f * BossMesh.ramp(7 - until, 0, 5) * (until < 1.5f ? until / 1.5f : 1);
            } else {
                lift = .35f + .65f * (float) Math.exp(-last / 4) * Mth.clamp(last / 2, 0, 1);
            }
            float side = m == 0 ? 1 : -1;
            Vector3f hand = new Vector3f(side * (r + .7f), h + .9f, -.2f);
            float ang = -.3f + lift * 1.1f;
            Vector3f head = new Vector3f(side * (r * .45f), h + .2f + Mth.sin(ang) * .9f, -.1f + Mth.cos(ang) * .1f);
            ClockMesh.rod(poseStack, buffer, MelodyMesh.VARNISH, hand, head, .045f, WHITE, light);
            BossMesh.sphere(BossMesh.solid(buffer, MelodyMesh.GOLD_METAL), poseStack.last(), head, .14f, 6, 10, 0, 0, 1, 0xFFFFF0D0, light);
        }
        Vector3f skin = new Vector3f(0, h + .14f, 0);
        poseStack.popPose();
        // A flash on the skin, a ring of sound along the ground and notes springing up at each beat
        if (beat >= 0 && since < 14) {
            float t = since / 14f;
            PoseStack.Pose p = poseStack.last();
            MelodyModels.shock(p, buffer, t, r, reach, GOLD, 1 - gone, light);
            MelodyModels.shock(p, buffer, Mth.clamp(t * 1.3f - .15f, 0, .999f), r, reach * .8f, ROSE, .6f * (1 - gone), light);
            MelodyMesh.glow(poseStack, buffer, view, skin, 2.5f, GOLD_LIGHT, (1 - t) * (1 - t) * (1 - gone), light);
            for (int k = 0; k < 4; k++) {
                float a = k * Mth.HALF_PI + beat;
                Vector3f at = new Vector3f(Mth.cos(a) * (.4f + t * 1.4f), h + .3f + t * 2.2f, Mth.sin(a) * (.4f + t * 1.4f));
                MelodyMesh.noteFacing(poseStack, buffer, view, at, MelodyMesh.kind(k + beat), .3f * Mth.sin(t * 8 + k), .5f, MelodyMesh.hue(k * .2f + beat * .1f),
                        Mth.sin(t * Mth.PI) * (1 - gone), light);
            }
        }
        MelodyMesh.glow(poseStack, buffer, view, skin, 1.6f, ROSE, .25f * up * (1 - gone), light);
    }

    // ---------------------------------------------------------------- harp of light

    public static void harp(PoseStack poseStack, MultiBufferSource buffer, WaterMesh.View view, Vector3f dir, Vector3f place, float age, int life, int rise,
                            int every, int ray, float since, Vector3f target, int seed, int light) {
        float up = BossMesh.ramp(age, 0, rise);
        float alpha = up * (1 - BossMesh.ramp(age, life - 12, life));
        if (alpha <= .01f) {
            return;
        }
        float h = 2.2f;
        Vector3f at = new Vector3f(place).add(0, .25f * Mth.sin(age * .08f), 0);
        // Its strings' plane along the way the caster faces, so they are seen from the side
        Quaternionf turn = new Quaternionf().rotationY(yawOf(dir) - Mth.HALF_PI);
        int plucked = age < rise ? -1 : (int) ((age - rise) / every) % 10;
        float pluck = age < rise ? 0 : (float) Math.exp(-((age - rise) % every) / 3);
        poseStack.pushPose();
        poseStack.translate(at.x, at.y, at.z);
        poseStack.mulPose(turn);
        poseStack.scale(up, up, up);
        poseStack.translate(-h * .3f, 0, 0);
        MelodyMesh.harp(poseStack, buffer, MelodyMesh.local(view, at, turn), h, plucked, pluck, age, light);
        poseStack.popPose();
        Vector3f middle = new Vector3f(at).add(0, h * .55f, 0);
        MelodyMesh.glow(poseStack, buffer, view, middle, 3.2f, GOLD_LIGHT, alpha * .3f, light);
        MelodyMesh.glints(poseStack, buffer, view, new Vector3f(at), 1.2f, h, 8, age, alpha, seed, light);
        // The note it plays flies as a ray of light to its enemy
        if (target != null && since < ray) {
            float t = since / ray;
            float a = (1 - t) * alpha;
            int c = MelodyMesh.hue(plucked * .1f);
            MelodyModels.ray(poseStack, buffer, view, middle, target, .35f * (1 - t * .5f), c, a, light);
            MelodyMesh.glow(poseStack, buffer, view, target, 1.8f * (1 + t), c, a, light);
            MelodyMesh.noteFacing(poseStack, buffer, view, new Vector3f(target).add(0, .6f + t, 0), MelodyMesh.kind(plucked), .3f, .6f, c, a, light);
        }
    }

    // ---------------------------------------------------------------- phantom trumpets

    public static void trumpets(PoseStack poseStack, MultiBufferSource buffer, WaterMesh.View view, Vector3f dir, float age, int life, int count, int rise,
                                int first, int every, int blasts, IntFunction<Vector3f> place, float reach, int seed, int light) {
        float fade = 1 - BossMesh.ramp(age, life - 8, life);
        Vector3f d = MelodyModels.unit(new Vector3f(dir.x, 0, dir.z));
        float yaw = yawOf(d);
        // Which blast, and how long since it
        float fromFirst = age - first;
        int blast = fromFirst < 0 ? -1 : Math.min(blasts - 1, (int) (fromFirst / every));
        float since = blast < 0 ? 1000 : fromFirst - blast * every;
        for (int k = 0; k < count; k++) {
            float up = BossMesh.ramp(age, k * 2, rise + k * 2);
            if (up <= .01f || fade <= .01f) {
                continue;
            }
            Vector3f at = place.apply(k).add(0, .1f * Mth.sin(age * .1f + k), 0);
            float recoil = since < 8 ? (float) Math.exp(-since / 2.5f) * .35f : 0;
            Vector3f pos = new Vector3f(at).sub(new Vector3f(d).mul(recoil));
            // Pointing ahead, tipped up a little, the outer ones turned out
            float spread = (k - (count - 1) * .5f) * .12f;
            Quaternionf turn = new Quaternionf().rotationY(yaw - spread).rotateX(-.18f);
            poseStack.pushPose();
            poseStack.translate(pos.x, pos.y, pos.z);
            poseStack.mulPose(turn);
            poseStack.scale(up, up, up);
            poseStack.translate(0, 0, -.8f);
            MelodyMesh.trumpet(poseStack, buffer, 1.6f, light);
            poseStack.popPose();
            Vector3f out = turn.transform(new Vector3f(0, 0, 1));
            Vector3f bell = new Vector3f(pos).add(new Vector3f(out).mul(.8f * up));
            MelodyMesh.glow(poseStack, buffer, view, bell, 1.2f + (since < 8 ? 2.5f * (1 - since / 8) : 0), GOLD_LIGHT, fade * (since < 8 ? .9f : .3f), light);
            // Rings of sound flying from the bell after each blast
            if (since < 12) {
                for (int j = 0; j < 3; j++) {
                    float s = since - j * 2;
                    if (s < 0) {
                        continue;
                    }
                    float t = s / 10f;
                    if (t >= 1) {
                        continue;
                    }
                    Vector3f c = new Vector3f(bell).add(new Vector3f(out).mul(t * reach * .7f));
                    MelodyMesh.soundRing(poseStack.last(), buffer, c, out, .5f + t * 2.2f, .2f + t * .2f, MelodyMesh.hue(k * .15f + j * .1f), (1 - t) * fade, light);
                }
            }
        }
        // Notes flung ahead with each blast
        if (blast >= 0 && since < 14) {
            float t = since / 14f;
            for (int k = 0; k < 8; k++) {
                float a = (k - 3.5f) * .14f;
                Vector3f dd = new Quaternionf().rotationY(a).transform(new Vector3f(d));
                Vector3f at = new Vector3f(dd).mul(1.5f + t * reach * .8f).add(0, 2.4f + .5f * Mth.sin(k * 1.7f) + t * .5f, 0);
                MelodyMesh.noteFacing(poseStack, buffer, view, at, MelodyMesh.kind(k + blast), .4f * Mth.sin(t * 9 + k), .6f, MelodyMesh.hue(k * .12f), (1 - t) * fade,
                        light);
            }
        }
    }

    // ---------------------------------------------------------------- storm organ

    public static void organ(PoseStack poseStack, MultiBufferSource buffer, WaterMesh.View view, Vector3f dir, float age, int life, float radius, int rise, int every,
                             int finale, int fall, float since, Vector3f target, int seed, int light) {
        float up = BossMesh.ramp(age, 0, rise);
        float gone = BossMesh.ramp(age, life - 15, life);
        if (gone >= 1) {
            return;
        }
        float w = 5;
        Vector3f d = MelodyModels.unit(new Vector3f(dir.x, 0, dir.z));
        Quaternionf turn = new Quaternionf().rotationY(yawOf(d));
        int k = age < rise ? -1 : (int) ((age - rise) / every);
        boolean playing = age >= rise && age < finale;
        int which = playing ? Math.floorMod(k * 7, 11) : -1;
        float blow = playing ? (float) Math.exp(-((age - rise) % every) / 3) : 0;
        float sink = (1 - up) * 3 + gone * 3;
        poseStack.pushPose();
        poseStack.translate(0, -sink, 0);
        poseStack.mulPose(turn);
        MelodyMesh.organ(poseStack, buffer, MelodyMesh.local(view, new Vector3f(0, -sink, 0), turn), w, Math.max(.05f, up), which, blow, age, light);
        poseStack.popPose();
        PoseStack.Pose p = poseStack.last();
        float alpha = up * (1 - gone);
        // The field it plays over
        MelodyMesh.staffRing(buffer, p, .06f, radius, 1.4f, age * .005f, Mth.TWO_PI, 96, alpha * .7f, age * .02f, .5f, light);
        MelodyMesh.ring(p, buffer, .04f, radius - .6f, radius, VIOLET, alpha * .35f, light);
        Vector3f crown = new Vector3f(0, w * 1.35f - sink, 0);
        MelodyMesh.glow(poseStack, buffer, view, crown, 4 + blow * 2, ROSE, alpha * (.35f + .3f * blow), light);
        // A pillar of light called down on an enemy: a staff arcing from the crown over it, then the pillar falling
        if (playing && target != null && since < fall + 10) {
            float t = since / fall;
            float a = alpha * (since < fall ? 1 : 1 - (since - fall) / 10);
            Vector3f top = new Vector3f(target).add(0, 10, 0);
            Vector3f mid = new Vector3f(crown).lerp(top, .5f).add(0, 3, 0);
            int n = 16;
            Vector3f[] arc = new Vector3f[n];
            for (int i = 0; i < n; i++) {
                float s = (float) i / (n - 1);
                arc[i] = new Vector3f(crown).mul((1 - s) * (1 - s)).add(new Vector3f(mid).mul(2 * s * (1 - s))).add(new Vector3f(top).mul(s * s));
            }
            MelodyMesh.staff(BossMesh.light(buffer, MelodyMesh.STAFF), p, view.eye(), arc, null, 1.1f, a * .8f, age * .2f, k * .13f, light);
            float bottom = since < fall ? 10 * (1 - t * t) : 0;
            MelodyModels.ray(poseStack, buffer, view, top, new Vector3f(target).add(0, bottom, 0), 1.1f, MelodyMesh.hue(k * .13f), a, light);
            poseStack.pushPose();
            poseStack.translate(target.x, 0, target.z);
            MelodyMesh.ring(poseStack.last(), buffer, target.y + .07f, 1.4f, 2.1f, VIOLET, a * .8f, light);
            if (since >= fall) {
                MelodyModels.shock(poseStack.last(), buffer, (since - fall) / 10f, .5f, 3.5f, GOLD, a + .2f, light);
            }
            poseStack.popPose();
            if (since >= fall) {
                MelodyMesh.glow(poseStack, buffer, view, new Vector3f(target).add(0, .8f, 0), 3.5f, GOLD_LIGHT, a, light);
            }
        }
        // The last great chord
        float f = age - finale;
        if (f >= 0 && f < 24) {
            float t = f / 24f;
            MelodyModels.shock(p, buffer, t, 2, radius * 1.1f, GOLD, 1, light);
            MelodyModels.shock(p, buffer, Mth.clamp(t * 1.4f, 0, .999f), 2, radius * .8f, ROSE, .8f, light);
            MelodyMesh.glow(poseStack, buffer, view, crown, 12 * (1 - t * .5f), GOLD_LIGHT, (1 - t) * (1 - t), light);
            MelodyModels.burst(poseStack, buffer, view, t, 4, seed, light);
        }
    }

    // ---------------------------------------------------------------- phantom orchestra

    public static void orchestra(PoseStack poseStack, MultiBufferSource buffer, WaterMesh.View view, float age, int life, float radius, int rise, int violins,
                                 IntFunction<Vector3f> violinPlace, int finale, int fall, float since, Vector3f target, int seed, int light) {
        float up = BossMesh.ramp(age, 0, rise);
        float gone = BossMesh.ramp(age, finale + 8, life);
        float alpha = up * (1 - gone);
        if (alpha <= .01f) {
            return;
        }
        PoseStack.Pose p = poseStack.last();
        float f = age - finale;
        float swell = f >= 0 ? 1 + 1.5f * (float) Math.exp(-f / 6) : 1;
        // The field: a great staff round its edge, another nearer in turning the other way, a glow over the ground
        MelodyMesh.staffRing(buffer, p, .06f, radius, 1.8f, age * .004f, Mth.TWO_PI, 128, alpha * .8f, age * .02f, 0, light);
        MelodyMesh.staffRing(buffer, p, .07f, radius * .45f, 1.1f, -age * .01f, Mth.TWO_PI, 64, alpha * .6f, age * .03f, .4f, light);
        MelodyMesh.ring(p, buffer, .04f, radius - 1, radius, VIOLET, alpha * .3f, light);
        // Staves of light wheeling round the field at three heights, each its own way
        for (int s = 0; s < 3; s++) {
            float r = radius * (.38f + s * .22f);
            float y = 2.4f + s * 1.6f + .3f * Mth.sin(age * .04f + s);
            float turn = age * (s % 2 == 0 ? .02f : -.015f) * (1 + s * .3f) + s * 2;
            MelodyMesh.staffBand(buffer, p, y, r, (1 + s * .25f) * swell, turn, Mth.TWO_PI * .72f, 72, .5f + s * .2f, age, alpha * .85f, age * .05f, s * .33f, light);
        }
        // The violins in a ring, turned in, their bows drawn
        for (int k = 0; k < violins; k++) {
            float vin = BossMesh.ramp(age, k * 3, k * 3 + 20) * (1 - gone);
            if (vin <= .01f) {
                continue;
            }
            Vector3f at = violinPlace.apply(k);
            Quaternionf turn = new Quaternionf().rotationY((float) Math.atan2(-at.x, -at.z)).rotateZ(.5f).rotateX(-.25f);
            poseStack.pushPose();
            poseStack.translate(at.x, at.y, at.z);
            poseStack.mulPose(turn);
            poseStack.scale(vin, vin, vin);
            MelodyMesh.violin(poseStack, buffer, MelodyMesh.local(view, at, turn), 2.1f, .5f + .5f * Mth.sin(age * .25f + k * .8f), light);
            poseStack.popPose();
            MelodyMesh.glow(poseStack, buffer, view, at, 2.4f, MelodyMesh.hue(k / (float) violins), vin * .35f, light);
            // A note springing from each violin now and then
            float u = ((age + k * 7) % 28) / 28f;
            MelodyMesh.noteFacing(poseStack, buffer, view, new Vector3f(at).mul(1 - u * .3f).add(0, .8f + u * 2.4f, 0), MelodyMesh.kind(k), .3f * Mth.sin(u * 7),
                    .5f, MelodyMesh.hue(k / (float) violins + .3f), vin * Mth.sin(u * Mth.PI), light);
        }
        // The clef turning over the middle, glowing
        float h = 7 + .4f * Mth.sin(age * .05f);
        poseStack.pushPose();
        poseStack.translate(0, h, 0);
        poseStack.mulPose(new Quaternionf().rotationY((float) Math.atan2(view.eye().x, view.eye().z) + .3f * Mth.sin(age * .02f)));
        float cs = 3.6f * up * (1 - gone * .5f) * (f >= 0 ? 1 + .25f * (float) Math.exp(-f / 5) : 1);
        poseStack.translate(0, -cs * .45f, 0);
        MelodyMesh.clef(poseStack, buffer, cs, light);
        poseStack.popPose();
        MelodyMesh.glow(poseStack, buffer, view, new Vector3f(0, h, 0), 9 * swell, VIOLET, alpha * .45f, light);
        MelodyMesh.glow(poseStack, buffer, view, new Vector3f(0, h, 0), 5 * swell, GOLD_LIGHT, alpha * .35f, light);
        MelodyMesh.glints(poseStack, buffer, view, new Vector3f(), radius * .9f, 9, 30, age * .5f, alpha, seed, light);
        // A note falling on an enemy
        if (target != null && since < fall + 10 && f < 0) {
            float t = Math.min(1, since / fall);
            Vector3f at = new Vector3f(target).add(0, 9 * (1 - t * t) + .4f, 0);
            float a = since < fall ? 1 : 1 - (since - fall) / 10;
            MelodyMesh.noteFacing(poseStack, buffer, view, at, MelodyMesh.kind((int) since + seed), .3f * Mth.sin(since), 1.4f, MelodyMesh.hue(BossMesh.hash(seed) * .9f),
                    a * alpha, light);
            if (since >= fall) {
                poseStack.pushPose();
                poseStack.translate(target.x, target.y, target.z);
                MelodyModels.shock(poseStack.last(), buffer, (since - fall) / 10f, .4f, 2.4f, ROSE, alpha, light);
                poseStack.popPose();
            }
        }
        // The finale: rings of every colour over the whole field, a flash, notes flung everywhere
        if (f >= 0 && f < 30) {
            float t = f / 30f;
            MelodyModels.shock(p, buffer, t, 2, radius * 1.15f, GOLD, 1, light);
            MelodyModels.shock(p, buffer, Mth.clamp(t * 1.3f, 0, .999f), 2, radius, ROSE, .9f, light);
            MelodyModels.shock(p, buffer, Mth.clamp(t * 1.7f, 0, .999f), 2, radius * .8f, VIOLET, .8f, light);
            MelodyMesh.glow(poseStack, buffer, view, new Vector3f(0, h, 0), 20 * (1 - t * .4f), GOLD_LIGHT, (1 - t) * (1 - t), light);
            poseStack.pushPose();
            poseStack.translate(0, 3, 0);
            MelodyModels.burst(poseStack, buffer, MelodyMesh.local(view, new Vector3f(0, 3, 0), null), t, 5, seed, light);
            poseStack.popPose();
        }
    }
}
