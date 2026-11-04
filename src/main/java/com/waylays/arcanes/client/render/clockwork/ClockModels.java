package com.waylays.arcanes.client.render.clockwork;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.waylays.arcanes.client.render.boss.BossMesh;
import com.waylays.arcanes.client.render.boss.Sculpt;
import com.waylays.arcanes.client.render.ritual.RitualMesh;
import com.waylays.arcanes.client.render.water.WaterMesh;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import org.joml.Quaternionf;
import org.joml.Vector3f;


import static com.waylays.arcanes.client.render.clockwork.ClockMesh.BRASS;
import static com.waylays.arcanes.client.render.clockwork.ClockMesh.COPPER;
import static com.waylays.arcanes.client.render.clockwork.ClockMesh.STEEL;
import static com.waylays.arcanes.client.render.clockwork.ClockMesh.WHITE;

/**
 * Clockwork Magic's smaller machines.
 * <ul>
 * <li>Saw-gear: a ring of steel teeth spinning on its edge round two brass gears and a blue core, its rim glowing hot,
 * sparks flying from it; it breaks into cogs where it stops.</li>
 * <li>Rivet: a red-hot copper rivet, a trail of steam behind it.</li>
 * <li>Mainspring: a coiled steel spring between two brass plates, crushed down, then let go, steam bursting from its
 * sides; a ring of gears and sparks where its rider lands.</li>
 * <li>Gear trap: a brass gear lying on the ground, a ticking cog and a blue light at its middle, two jaws of steel teeth
 * lying open on it that snap shut on what steps in.</li>
 * <li>Turret: a brass drum on a steel tripod, a core of blue light on it, six barrels spinning as it fires.</li>
 * <li>Gyroscopic cage: three toothed rings tumbling round each other on their own axles, blue arcs leaping inward.</li>
 * </ul>
 */
public final class ClockModels {
    private ClockModels() {}

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

    // ---------------------------------------------------------------- saw-gear

    public static void saw(PoseStack poseStack, MultiBufferSource buffer, WaterMesh.View view, Vector3f dir, float age, boolean done, float since, int seed,
                           int light) {
        if (done) {
            ClockMesh.burst(poseStack, buffer, view, since / 10f, .8f, seed, light);
            return;
        }
        Vector3f d = unit(dir), axle = side(d);
        float grow = Math.min(1, age / 3f);
        float spin = -age * .9f;
        poseStack.pushPose();
        poseStack.scale(grow, grow, grow);
        ClockMesh.gearAt(poseStack, buffer, STEEL, new Vector3f(), axle, spin, .8f, 18, .1f, 0, WHITE, light);
        ClockMesh.gearAt(poseStack, buffer, BRASS, new Vector3f(axle).mul(.08f), axle, spin * 1.3f, .5f, 12, .08f, 5, WHITE, light);
        ClockMesh.gearAt(poseStack, buffer, BRASS, new Vector3f(axle).mul(-.08f), axle, spin * 1.3f, .5f, 12, .08f, 5, WHITE, light);
        // Its rim glowing hot as it spins
        Vector3f up = new Vector3f(d).cross(axle).normalize();
        BossMesh.annulus(BossMesh.light(buffer, BossMesh.GLOW), poseStack.last(), new Vector3f(), d, up, .55f, .9f, 40, 1, 0, 0, BossMesh.glow(ClockMesh.EMBER, 0),
                BossMesh.glow(ClockMesh.EMBER, .55f), light, true);
        ClockMesh.core(poseStack, buffer, view, new Vector3f(), .1f, age, .9f, light);
        poseStack.popPose();
        ClockMesh.sparks(poseStack, buffer, view, new Vector3f(d).mul(-.4f).add(0, -.6f, 0), 1.3f, 12, age, .9f, seed, light);
    }

    // ---------------------------------------------------------------- rivet

    public static void rivet(PoseStack poseStack, MultiBufferSource buffer, WaterMesh.View view, Vector3f dir, float age, boolean done, float since, int seed,
                             int light) {
        if (done) {
            float t = since / 10f;
            if (t < 1) {
                ClockMesh.glow(poseStack, buffer, view, new Vector3f(), .6f + t, ClockMesh.EMBER, (1 - t) * (1 - t), light);
                ClockMesh.sparks(poseStack, buffer, view, new Vector3f(), .8f, 8, since * 1.5f, 1 - t, seed, light);
                ClockMesh.steam(poseStack, buffer, view, new Vector3f(), new Vector3f(0, 1, 0), .8f, .5f, 3, since, .1f, (1 - t) * .6f, seed, light);
            }
            return;
        }
        Vector3f d = unit(dir);
        poseStack.pushPose();
        poseStack.mulPose(new Quaternionf().rotationTo(new Vector3f(0, 1, 0), d));
        poseStack.mulPose(new Quaternionf().rotationY(age * .5f));
        // The rivet's shank, then its domed head leading, glowing red-hot
        ClockMesh.turned(poseStack.last(), buffer, COPPER, new float[] {0, -.42f, .07f, -.42f, .07f, -.06f, .16f, -.06f, .17f, -.02f, .14f, .05f, .08f, .1f, 0, .11f},
                10, WHITE, light);
        BossMesh.sphere(BossMesh.light(buffer, BossMesh.GLOW), poseStack.last(), new Vector3f(0, .02f, 0), .17f, 4, 8, 0, 0, .5f, BossMesh.glow(ClockMesh.EMBER, .6f),
                light);
        poseStack.popPose();
        ClockMesh.glow(poseStack, buffer, view, new Vector3f(d).mul(.05f), .7f, ClockMesh.EMBER, .7f, light);
        ClockMesh.steam(poseStack, buffer, view, new Vector3f(d).mul(-.4f), new Vector3f(d).negate(), 2.4f, .45f, 6, age, .18f, .55f, seed, light);
    }

    // ---------------------------------------------------------------- mainspring

    public static void spring(PoseStack poseStack, MultiBufferSource buffer, WaterMesh.View view, Vector3f dir, float age, int life, float since,
                              Vector3f landed, int launch, int seed, int light) {
        float grow = Math.min(1, age / 3f);
        float t = age - launch;
        float h;
        if (t < 0) {
            // Crushed down, trembling
            h = Mth.lerp(Math.min(1, age / launch), 1f, .42f) + .02f * Mth.sin(age * 3);
        } else {
            h = 1.35f + 1.05f * (float) Math.exp(-t / 7) * Mth.cos(t * .8f + Mth.PI * .35f);
        }
        float sink = BossMesh.ramp(age, launch + 22, launch + 34);
        if (sink < 1) {
            poseStack.pushPose();
            poseStack.translate(0, -sink * (h + .5f), 0);
            poseStack.scale(grow, grow, grow);
            poseStack.mulPose(new Quaternionf().rotationY((float) Math.atan2(dir.x, dir.z)));
            PoseStack.Pose p = poseStack.last();
            // The base plate, its rim and four little gears round it
            ClockMesh.turned(p, buffer, ClockMesh.PLATE, new float[] {0, 0, .9f, 0, .95f, .06f, .9f, .14f, 0, .14f}, 24, WHITE, light);
            Sculpt.torus(BossMesh.solid(buffer, COPPER), p, .9f, .05f, 32, 6, 0, Mth.TWO_PI, 4, WHITE, light);
            for (int k = 0; k < 4; k++) {
                float a = k * Mth.HALF_PI + Mth.PI / 4;
                ClockMesh.gearAt(poseStack, buffer, BRASS, new Vector3f(Mth.cos(a) * .66f, .19f, Mth.sin(a) * .66f), new Vector3f(0, 1, 0),
                        (k % 2 == 0 ? 1 : -1) * age * .2f, .2f, 8, .06f, 0, WHITE, light);
            }
            // The spring and its top plate
            poseStack.pushPose();
            poseStack.translate(0, .14f, 0);
            ClockMesh.spring(poseStack.last(), buffer, STEEL, .5f, h, 5, .075f, age * .05f, WHITE, light);
            poseStack.translate(0, h, 0);
            ClockMesh.turned(poseStack.last(), buffer, BRASS, new float[] {0, 0, .72f, 0, .75f, .06f, .62f, .14f, .25f, .16f, .2f, .22f, 0, .22f}, 24, WHITE, light);
            ClockMesh.core(poseStack, buffer, view, new Vector3f(0, -.08f, 0), .14f, age, .8f, light);
            poseStack.popPose();
            poseStack.popPose();
        }
        if (t >= 0 && t < 14) {
            float k = t / 14;
            // Steam bursting out to the sides as it lets go, and a ring of light
            for (int s = 0; s < 4; s++) {
                float a = s * Mth.HALF_PI + .3f;
                ClockMesh.steam(poseStack, buffer, view, new Vector3f(Mth.cos(a) * .7f, .4f, Mth.sin(a) * .7f), new Vector3f(Mth.cos(a), .2f, Mth.sin(a)), 2.2f, .7f, 4,
                        t, .09f, (1 - k) * .9f, seed + s * 9, light);
            }
            ClockMesh.ring(poseStack.last(), buffer, .05f, .5f + 3 * k, .9f + 3.4f * k, ClockMesh.BLUE, (1 - k) * .7f, light);
            ClockMesh.sparks(poseStack, buffer, view, new Vector3f(0, .3f, 0), 1.6f, 14, t * 2, 1 - k, seed, light);
        }
        if (landed != null && since < 24) {
            stomp(poseStack, buffer, view, landed, since / 24f, seed, light);
        }
    }

    /** Where the rider lands: a great brass gear printed on the ground and sinking, a ring of light, sparks and steam. */
    static void stomp(PoseStack poseStack, MultiBufferSource buffer, WaterMesh.View view, Vector3f at, float t, int seed, int light) {
        poseStack.pushPose();
        poseStack.translate(at.x, at.y, at.z);
        float out = 1 - (1 - t) * (1 - t);
        if (t < .6f) {
            float k = t / .6f;
            ClockMesh.gearAt(poseStack, buffer, BRASS, new Vector3f(0, .06f - k * .4f, 0), new Vector3f(0, 1, 0), t * 3, 1.6f, 20, .14f, 6, WHITE, light);
        }
        ClockMesh.ring(poseStack.last(), buffer, .08f, .4f + 3 * out, 1 + 3.2f * out, ClockMesh.EMBER, (1 - t) * .8f, light);
        ClockMesh.ring(poseStack.last(), buffer, .1f, 2.4f * out, .4f + 2.8f * out, ClockMesh.BLUE, (1 - t) * .6f, light);
        ClockMesh.sparks(poseStack, buffer, view, new Vector3f(0, .3f, 0), 2.2f, 16, t * 12, 1 - t, seed, light);
        for (int s = 0; s < 6; s++) {
            float a = s * Mth.TWO_PI / 6;
            ClockMesh.steam(poseStack, buffer, view, new Vector3f(Mth.cos(a), .3f, Mth.sin(a)), new Vector3f(Mth.cos(a), .15f, Mth.sin(a)), 2.5f, .9f, 2, t * 24, .06f,
                    (1 - t) * .7f, seed + s, light);
        }
        poseStack.popPose();
    }

    // ---------------------------------------------------------------- gear trap

    /** A jaw: a half ring of steel lying in the XY plane (y up from its hinge along X), its teeth pointing in. */
    private static float[] jaw(float phase) {
        int teeth = 7;
        float outer = 1.05f, rim = .8f, tip = .5f;
        int arc = 16;
        float[] pts = new float[(arc + 1) * 2 + (teeth * 2 + 1) * 2];
        int n = 0;
        for (int i = 0; i <= arc; i++) {
            float a = Mth.PI * i / arc;
            pts[n++] = Mth.cos(a) * outer;
            pts[n++] = Mth.sin(a) * outer;
        }
        // Back along the inside, a tooth between each pair of notches
        for (int i = teeth * 2; i >= 0; i--) {
            float a = Mth.PI * Mth.clamp((i + phase) / (teeth * 2), 0, 1);
            float r = i % 2 == 1 ? tip : rim;
            pts[n++] = Mth.cos(a) * r;
            pts[n++] = Mth.sin(a) * r;
        }
        return pts;
    }

    public static void trap(PoseStack poseStack, MultiBufferSource buffer, WaterMesh.View view, float age, int life, boolean sprung, boolean done, float since,
                            int arm, int snap, int hold, int seed, int light) {
        float rise = BossMesh.ramp(age, 0, arm);
        float sink = done ? BossMesh.ramp(since, snap + hold + 6, snap + hold + 18) : 0;
        poseStack.pushPose();
        poseStack.translate(0, (rise - 1) * .35f - sink * .5f, 0);
        // The ground plate: a great brass gear lying flat, ticking round a notch each second while it waits
        float tick = sprung ? 0 : Mth.floor(age / 20) * (Mth.TWO_PI / 28) + Mth.clamp(age % 20, 0, 2) * .03f;
        ClockMesh.gearAt(poseStack, buffer, BRASS, new Vector3f(0, .05f, 0), new Vector3f(0, 1, 0), tick, 1.25f, 28, .1f, 0, WHITE, light);
        ClockMesh.gearAt(poseStack, buffer, COPPER, new Vector3f(0, .13f, 0), new Vector3f(0, 1, 0), -tick * 3.5f, .34f, 8, .08f, 0, WHITE, light);
        ClockMesh.rod(poseStack, buffer, STEEL, new Vector3f(-1.12f, .14f, 0), new Vector3f(1.12f, .14f, 0), .05f, WHITE, light);
        // The jaws: lying open, snapping shut on their prey, opening again when they let go
        float close;
        if (!sprung) {
            close = 0;
        } else if (since < snap + hold) {
            float k = Math.min(1, since / snap);
            close = 1 - (1 - k) * (1 - k) * (1 - k);
        } else {
            close = 1 - BossMesh.ramp(since, snap + hold, snap + hold + 8);
        }
        for (int s = 0; s < 2; s++) {
            poseStack.pushPose();
            poseStack.mulPose(new Quaternionf().rotationY(s * Mth.PI));
            poseStack.translate(0, .15f, -.05f);
            poseStack.mulPose(new Quaternionf().rotationX(-Mth.HALF_PI + close * Mth.HALF_PI * .96f));
            Sculpt.slab(BossMesh.solid(buffer, STEEL), poseStack.last(), jaw(s * .5f), .09f, .02f, .5f, WHITE, 0xFF8890A0, light);
            poseStack.popPose();
        }
        // The blue light at its middle: steady while it waits, flaring as it bites
        float bite = sprung ? Math.max(0, 1 - since / 8) : 0;
        ClockMesh.core(poseStack, buffer, view, new Vector3f(0, .24f, 0), .1f, age, rise * (.7f + .3f * Mth.sin(age * .25f)) * (1 - sink), light);
        if (bite > 0) {
            ClockMesh.glow(poseStack, buffer, view, new Vector3f(0, .6f, 0), 2.5f, ClockMesh.BLUE_WHITE, bite, light);
            ClockMesh.sparks(poseStack, buffer, view, new Vector3f(0, .8f, 0), 1.2f, 14, since * 2, bite, seed, light);
        }
        if (sprung && since < snap + hold && (int) (age / 3) % 3 == 0) {
            ClockMesh.arc(poseStack, buffer, view.eye(), new Vector3f(-.9f, .5f, 0), new Vector3f(.9f, .6f, 0), age, .06f, .8f, seed, light);
        }
        poseStack.popPose();
        if (!sprung && rise >= 1) {
            ClockMesh.ring(poseStack.last(), buffer, .03f, 1.2f, 1.45f, ClockMesh.BLUE, .25f + .15f * Mth.sin(age * .2f), light);
        }
    }

    // ---------------------------------------------------------------- turret

    public static void turret(PoseStack poseStack, MultiBufferSource buffer, WaterMesh.View view, Vector3f dir, float age, int life, float since,
                              Vector3f target, int rise, int seed, int light) {
        float k = BossMesh.ramp(age, 0, rise);
        float sink = BossMesh.ramp(age, life - 15, life);
        float hub = Mth.lerp(k, .3f, 1.05f) - sink * 1.2f;
        poseStack.pushPose();
        // The tripod
        for (int i = 0; i < 3; i++) {
            float a = i * Mth.TWO_PI / 3 + .5f;
            float spread = Mth.lerp(k, .2f, 1f);
            Vector3f foot = new Vector3f(Mth.cos(a) * spread, Math.min(0, -sink * 1.2f), Mth.sin(a) * spread);
            Vector3f top = new Vector3f(Mth.cos(a) * .18f, hub, Mth.sin(a) * .18f);
            ClockMesh.rod(poseStack, buffer, STEEL, foot, top, .05f, WHITE, light);
            poseStack.pushPose();
            poseStack.translate(foot.x, foot.y, foot.z);
            ClockMesh.cylinder(poseStack.last(), buffer, BRASS, .1f, 0, .06f, 10, WHITE, light);
            poseStack.popPose();
        }
        // The drum, and its turning head
        Vector3f aim = target != null && since < 50 ? new Vector3f(target).sub(0, hub + .25f, 0) : unit(dir);
        float yaw = (float) Math.atan2(aim.x, aim.z);
        float pitch = (float) Math.atan2(aim.y, Math.sqrt(aim.x * aim.x + aim.z * aim.z));
        poseStack.translate(0, hub, 0);
        ClockMesh.turned(poseStack.last(), buffer, BRASS, new float[] {0, -.22f, .3f, -.22f, .38f, -.1f, .38f, .08f, .26f, .2f, 0, .2f}, 20, WHITE, light);
        Sculpt.torus(BossMesh.solid(buffer, COPPER), poseStack.last(), .38f, .045f, 28, 6, 0, Mth.TWO_PI, 4, WHITE, light);
        poseStack.translate(0, .3f, 0);
        Quaternionf turn = new Quaternionf().rotationY(yaw).rotateX(-pitch);
        poseStack.mulPose(turn);
        // The housing behind the barrels, the core on it, a vent at its back
        poseStack.pushPose();
        poseStack.mulPose(new Quaternionf().rotationX(Mth.HALF_PI));
        ClockMesh.turned(poseStack.last(), buffer, BRASS, new float[] {0, -.15f, .2f, -.15f, .22f, -.1f, .22f, .35f, .18f, .4f, 0, .4f}, 16, WHITE, light);
        poseStack.popPose();
        ClockMesh.boxAt(poseStack, buffer, COPPER, .3f, -.02f, -.05f, .16f, .22f, .3f, WHITE, light);
        ClockMesh.core(poseStack, buffer, view, new Vector3f(0, .22f, -.12f), .09f, age, .9f, light);
        ClockMesh.rod(poseStack, buffer, STEEL, new Vector3f(0, .1f, -.38f), new Vector3f(0, .45f, -.42f), .05f, WHITE, light);
        float firing = since < 20 ? 1 - since / 20 : 0;
        float spin = age * (.12f + .9f * firing);
        for (int b = 0; b < 6; b++) {
            float a = spin + b * Mth.TWO_PI / 6;
            Vector3f off = new Vector3f(Mth.cos(a) * .11f, Mth.sin(a) * .11f, 0);
            ClockMesh.rod(poseStack, buffer, STEEL, new Vector3f(off).add(0, 0, .3f), new Vector3f(off).add(0, 0, 1.05f), .035f, WHITE, light);
        }
        for (float z : new float[] {.45f, .95f}) {
            poseStack.pushPose();
            poseStack.translate(0, 0, z);
            poseStack.mulPose(new Quaternionf().rotationX(Mth.HALF_PI));
            Sculpt.torus(BossMesh.solid(buffer, BRASS), poseStack.last(), .16f, .035f, 16, 6, 0, Mth.TWO_PI, 2, WHITE, light);
            poseStack.popPose();
        }
        poseStack.popPose();
        Vector3f muzzle = turn.transform(new Vector3f(0, 0, 1.1f)).add(0, hub + .3f, 0);
        Vector3f vent = turn.transform(new Vector3f(0, .45f, -.42f)).add(0, hub + .3f, 0);
        ClockMesh.steam(poseStack, buffer, view, vent, new Vector3f(0, 1, 0), 1.4f, .35f, 4, age, .06f, .35f + .4f * firing, seed, light);
        if (target != null && since < 4) {
            float f = 1 - since / 4;
            ClockMesh.glow(poseStack, buffer, view, muzzle, 1.1f, ClockMesh.EMBER, f, light);
            ClockMesh.glow(poseStack, buffer, view, muzzle, .5f, 0xFFF4D0, f, light);
            VertexConsumer beam = BossMesh.light(buffer, BossMesh.BEAM);
            RitualMesh.shaft(beam, poseStack.last(), view.eye(), muzzle, target, .14f * f + .04f, BossMesh.glow(0xFFD080, f), light);
            ClockMesh.sparks(poseStack, buffer, view, target, .6f, 6, since * 3, f, seed, light);
        }
    }

    // ---------------------------------------------------------------- gyroscopic cage

    /** A ring of the cage, round Y in its own frame, {@code r} across, toothed on its outside. */
    private static void cageRing(PoseStack poseStack, MultiBufferSource buffer, net.minecraft.resources.ResourceLocation metal, float r, int light) {
        Sculpt.torus(BossMesh.solid(buffer, metal), poseStack.last(), r, .07f, 48, 8, 0, Mth.TWO_PI, 6, WHITE, light);
        VertexConsumer steel = BossMesh.solid(buffer, STEEL);
        for (int k = 0; k < 18; k++) {
            poseStack.pushPose();
            poseStack.mulPose(new Quaternionf().rotationY(k * Mth.TWO_PI / 18));
            poseStack.translate(r + .08f, 0, 0);
            Sculpt.block(steel, poseStack.last(), .1f, .09f, .1f, .015f, .5f, WHITE, light);
            poseStack.popPose();
        }
    }

    public static void gyro(PoseStack poseStack, MultiBufferSource buffer, WaterMesh.View view, float age, int life, float radius, float since, boolean done,
                            int close, int burst, int seed, int light) {
        float k = BossMesh.ramp(age, 0, close);
        float end = BossMesh.ramp(age, life - burst, life);
        Vector3f c = new Vector3f(0, 1, 0);
        if (end > 0) {
            poseStack.pushPose();
            poseStack.translate(c.x, c.y, c.z);
            ClockMesh.burst(poseStack, buffer, view, end, radius * .6f, seed, light);
            poseStack.popPose();
        }
        if (end < .35f) {
            float scale = (1 + (1 - k) * (1 - k) * 1.4f) * (1 + end * 2);
            float[] rs = {radius, radius * .8f, radius * .62f};
            net.minecraft.resources.ResourceLocation[] metals = {BRASS, COPPER, BRASS};
            Quaternionf[] turns = {
                    new Quaternionf().rotationY(age * .05f).rotateX(age * .07f),
                    new Quaternionf().rotationZ(age * .09f + 1).rotateY(-age * .06f),
                    new Quaternionf().rotationX(1.1f).rotateY(age * .13f).rotateZ(age * .04f)};
            for (int i = 0; i < 3; i++) {
                poseStack.pushPose();
                poseStack.translate(c.x, c.y, c.z);
                poseStack.mulPose(turns[i]);
                poseStack.scale(scale, scale, scale);
                cageRing(poseStack, buffer, metals[i], rs[i], light);
                poseStack.popPose();
            }
            // The pivots of the outer ring: two blue lights
            for (int s = -1; s <= 1; s += 2) {
                Vector3f pivot = turns[0].transform(new Vector3f(rs[0] * scale * s, 0, 0)).add(c);
                ClockMesh.core(poseStack, buffer, view, pivot, .1f, age, .9f * k, light);
            }
            // Arcs leaping in from the rings as they strike
            if (since < 6) {
                float f = 1 - since / 6;
                for (int a = 0; a < 5; a++) {
                    float ang = a * Mth.TWO_PI / 5 + seed + (int) (age / 2);
                    Vector3f from = turns[a % 3].transform(new Vector3f(Mth.cos(ang), 0, Mth.sin(ang)).mul(rs[a % 3] * scale)).add(c);
                    ClockMesh.arc(poseStack, buffer, view.eye(), from, c, age, .07f, f, seed + a * 17, light);
                }
                ClockMesh.glow(poseStack, buffer, view, c, 2.2f, ClockMesh.BLUE, f * .8f, light);
            }
            ClockMesh.glow(poseStack, buffer, view, c, radius * 2.2f, ClockMesh.BLUE, .18f * k, light);
            ClockMesh.ring(poseStack.last(), buffer, .03f, radius * .9f, radius * 1.15f, ClockMesh.BLUE, .35f * k * (1 - end), light);
        }
    }
}
