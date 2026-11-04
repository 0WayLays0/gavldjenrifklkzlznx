package com.waylays.arcanes.client.render.clockwork;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.waylays.arcanes.client.render.boss.BossMesh;
import com.waylays.arcanes.client.render.boss.Sculpt;
import com.waylays.arcanes.client.render.ritual.RitualMesh;
import com.waylays.arcanes.client.render.water.WaterMesh;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.function.IntFunction;
import java.util.function.IntUnaryOperator;

import static com.waylays.arcanes.client.render.clockwork.ClockMesh.BRASS;
import static com.waylays.arcanes.client.render.clockwork.ClockMesh.COPPER;
import static com.waylays.arcanes.client.render.clockwork.ClockMesh.ENAMEL;
import static com.waylays.arcanes.client.render.clockwork.ClockMesh.PLATE;
import static com.waylays.arcanes.client.render.clockwork.ClockMesh.STEEL;
import static com.waylays.arcanes.client.render.clockwork.ClockMesh.WHITE;

/**
 * Clockwork Magic's great machines.
 * <ul>
 * <li>Brass Automaton: a walking machine near three blocks tall: a barrel chest of brass with a porthole on its blue
 * core, a copper helmet with a glowing slit, pauldrons with turning gears, piston arms ending in steel fists, a winding
 * key turning in its back, steam puffing from two stacks; it strides, winds back an arm and punches.</li>
 * <li>Brass Locomotive: rails and sleepers laid down ahead in a line of blue light, then a red and brass steam engine
 * charging down them, its drivers turning with their rods, smoke from its stack, a blue headlight, a cowcatcher.</li>
 * <li>Airship: a canvas envelope ribbed in brass, fins and a turning propeller, a brass gondola with glowing windows and
 * two engines, flying over and dropping bombs that burst in fire and smoke.</li>
 * <li>Great Mechanism: a great gear turning under the caster, four meshing round it, a ring of toothed brass round the
 * field, eight great gears standing round it with pistons pumping steam between them, and high above an astrolabe of
 * tumbling rings round a core of blue energy that hurls arcs at the enemies, until it overloads.</li>
 * </ul>
 */
public final class ClockMachineModels {
    private ClockMachineModels() {}

    private static float yaw(Vector3f dir) {
        return (float) Math.atan2(dir.x, dir.z);
    }

    // ---------------------------------------------------------------- automaton

    /** A limb from the pose's origin straight down {@code len}: a steel piston in a sleeve of {@code sleeve}. */
    private static void limb(PoseStack poseStack, MultiBufferSource buffer, ResourceLocation sleeve, float len, float r, int light) {
        ClockMesh.cylinder(poseStack.last(), buffer, STEEL, r * .55f, -len, 0, 8, WHITE, light);
        ClockMesh.turned(poseStack.last(), buffer, sleeve, new float[] {0, -len * .7f, r * .85f, -len * .7f, r, -len * .6f, r, -len * .05f, r * .8f, 0, 0, 0}, 12, WHITE,
                light);
    }

    private static void ball(PoseStack poseStack, MultiBufferSource buffer, ResourceLocation metal, float r, int light) {
        BossMesh.sphere(BossMesh.solid(buffer, metal), poseStack.last(), new Vector3f(), r, 6, 10, 0, 0, 1, WHITE, light);
    }

    /** An arm hanging from the shoulder (the pose's origin): {@code swing} forward at the shoulder, {@code bend} at the elbow, the forearm's piston out by {@code reach}. */
    private static void arm(PoseStack poseStack, MultiBufferSource buffer, float swing, float bend, float reach, float spread, float gear, int light) {
        poseStack.pushPose();
        poseStack.mulPose(new Quaternionf().rotationX(-swing).rotateZ(spread));
        ball(poseStack, buffer, COPPER, .17f, light);
        limb(poseStack, buffer, BRASS, .48f, .1f, light);
        poseStack.translate(0, -.5f, 0);
        ball(poseStack, buffer, COPPER, .12f, light);
        ClockMesh.gearAt(poseStack, buffer, BRASS, new Vector3f(Math.signum(spread) * .1f, 0, 0), new Vector3f(1, 0, 0), gear, .14f, 8, .05f, 0, WHITE, light);
        poseStack.mulPose(new Quaternionf().rotationX(-bend));
        // The forearm: a heavy brass housing, the piston running out of it to the fist
        ClockMesh.turned(poseStack.last(), buffer, BRASS, new float[] {0, -.45f, .13f, -.45f, .16f, -.4f, .16f, -.08f, .12f, 0, 0, 0}, 14, WHITE, light);
        ClockMesh.cylinder(poseStack.last(), buffer, STEEL, .07f, -.5f - reach, -.4f, 8, WHITE, light);
        poseStack.translate(0, -.62f - reach, 0);
        Sculpt.torus(BossMesh.solid(buffer, COPPER), poseStack.last(), .12f, .035f, 14, 6, 0, Mth.TWO_PI, 2, WHITE, light);
        ClockMesh.box(poseStack.last(), buffer, STEEL, .28f, .26f, .3f, WHITE, light);
        for (int k = 0; k < 4; k++) {
            ClockMesh.boxAt(poseStack, buffer, STEEL, -.1f + k * .066f, -.15f, .08f, .055f, .06f, .12f, WHITE, light);
        }
        poseStack.popPose();
    }

    /** A leg from the hip (the pose's origin): {@code swing} forward, {@code knee} bent. */
    private static void leg(PoseStack poseStack, MultiBufferSource buffer, float swing, float knee, float gear, int light) {
        poseStack.pushPose();
        poseStack.mulPose(new Quaternionf().rotationX(-swing));
        ClockMesh.gearAt(poseStack, buffer, COPPER, new Vector3f(), new Vector3f(1, 0, 0), gear, .16f, 10, .08f, 0, WHITE, light);
        limb(poseStack, buffer, BRASS, .52f, .12f, light);
        poseStack.translate(0, -.54f, 0);
        ball(poseStack, buffer, COPPER, .12f, light);
        poseStack.mulPose(new Quaternionf().rotationX(knee));
        ClockMesh.turned(poseStack.last(), buffer, BRASS, new float[] {0, -.48f, .1f, -.48f, .12f, -.4f, .15f, -.15f, .12f, 0, 0, 0}, 12, WHITE, light);
        poseStack.translate(0, -.5f, 0);
        poseStack.mulPose(new Quaternionf().rotationX(-knee + swing));
        ClockMesh.boxAt(poseStack, buffer, STEEL, 0, -.03f, .06f, .26f, .12f, .44f, WHITE, light);
        poseStack.popPose();
    }

    /**
     * The automaton, facing {@code dir}: {@code stride} its walk's phase ({@code walking} 0 to 1 how much it walks),
     * {@code since} ticks since it began a punch that lands {@code wind} ticks in.
     */
    public static void automaton(PoseStack poseStack, MultiBufferSource buffer, WaterMesh.View view, Vector3f dir, float age, int life, float stride,
                                 float walking, float since, int rise, int wind, int seed, int light) {
        float k = BossMesh.ramp(age, 0, rise), end = BossMesh.ramp(age, life - 20, life);
        float up = 1 - (1 - k) * (1 - k);
        if (age < rise + 6) {
            float t = age / (rise + 6);
            ClockMesh.ring(poseStack.last(), buffer, .04f, 1.4f * t, .3f + 1.8f * t, ClockMesh.BLUE, (1 - t) * .8f, light);
            ClockMesh.steam(poseStack, buffer, view, new Vector3f(0, .2f, 0), new Vector3f(0, 1, 0), 2.5f, 1.1f, 7, age, .07f, (1 - t) * .9f, seed, light);
        }
        if (end >= 1) {
            return;
        }
        poseStack.pushPose();
        poseStack.translate(0, (up - 1) * 2.9f - end * 2.9f, 0);
        poseStack.mulPose(new Quaternionf().rotationY(yaw(dir)));
        // The punch: wound back, then thrown, then drawn in again
        float punch = 0, windup = 0;
        if (since >= 0 && since < wind + 12) {
            if (since < wind) {
                windup = Mth.sin(since / wind * Mth.HALF_PI);
            } else {
                float t = (since - wind) / 12;
                punch = t < .25f ? 1 : 1 - (t - .25f) / .75f;
            }
        }
        float s = Mth.sin(stride) * walking, c = Mth.cos(stride) * walking;
        float bob = Math.abs(Mth.sin(stride)) * .06f * walking + .02f * Mth.sin(age * .1f);
        // Legs
        poseStack.pushPose();
        poseStack.translate(0, 1.12f + bob, 0);
        for (int side = -1; side <= 1; side += 2) {
            poseStack.pushPose();
            poseStack.translate(side * .24f, 0, 0);
            float sw = s * .5f * side;
            leg(poseStack, buffer, sw, Math.max(0, -c * side) * .8f + .05f, stride * side, light);
            poseStack.popPose();
        }
        poseStack.popPose();
        // The body turns into the punch
        poseStack.pushPose();
        poseStack.translate(0, bob, 0);
        poseStack.mulPose(new Quaternionf().rotationY(-.35f * punch + .2f * windup));
        ClockMesh.boxAt(poseStack, buffer, PLATE, 0, 1.2f, 0, .62f, .26f, .42f, WHITE, light);
        ClockMesh.turned(poseStack.last(), buffer, BRASS, new float[] {0, 1.28f, .3f, 1.28f, .42f, 1.42f, .5f, 1.68f, .52f, 1.95f, .46f, 2.2f, .3f, 2.33f, 0, 2.36f}, 24,
                WHITE, light);
        for (float[] band : new float[][] {{1.45f, .44f}, {2.15f, .48f}}) {
            poseStack.pushPose();
            poseStack.translate(0, band[0], 0);
            Sculpt.torus(BossMesh.solid(buffer, COPPER), poseStack.last(), band[1], .045f, 32, 6, 0, Mth.TWO_PI, 4, WHITE, light);
            poseStack.popPose();
        }
        // The porthole on its core
        poseStack.pushPose();
        poseStack.translate(0, 1.82f, .48f);
        poseStack.mulPose(new Quaternionf().rotationX(Mth.HALF_PI));
        Sculpt.torus(BossMesh.solid(buffer, COPPER), poseStack.last(), .2f, .05f, 20, 6, 0, Mth.TWO_PI, 2, WHITE, light);
        for (int b = 0; b < 6; b++) {
            float a = b * Mth.TWO_PI / 6;
            ClockMesh.boxAt(poseStack, buffer, BRASS, Mth.cos(a) * .2f, .05f, Mth.sin(a) * .2f, .05f, .04f, .05f, WHITE, light);
        }
        poseStack.popPose();
        ClockMesh.core(poseStack, buffer, view, new Vector3f(0, 1.82f, .42f), .16f, age, .95f, light);
        // The winding key in its back, turning
        ClockMesh.rod(poseStack, buffer, STEEL, new Vector3f(0, 1.85f, -.45f), new Vector3f(0, 1.85f, -.82f), .05f, WHITE, light);
        poseStack.pushPose();
        poseStack.translate(0, 1.85f, -.84f);
        poseStack.mulPose(new Quaternionf().rotationZ(age * .06f));
        float[] bow = new float[48];
        for (int i = 0; i < 24; i++) {
            float a = i * Mth.TWO_PI / 24;
            float r = .08f + .2f * Math.abs(Mth.cos(a)) - .04f * Mth.cos(a * 2);
            bow[2 * i] = Mth.cos(a) * r * 1.3f;
            bow[2 * i + 1] = Mth.sin(a) * r * .7f;
        }
        Sculpt.slab(BossMesh.solid(buffer, BRASS), poseStack.last(), bow, .06f, .015f, .5f, WHITE, 0xFFB09060, light);
        poseStack.popPose();
        // The stacks on its back, puffing
        for (int side = -1; side <= 1; side += 2) {
            ClockMesh.rod(poseStack, buffer, STEEL, new Vector3f(side * .24f, 2.05f, -.3f), new Vector3f(side * .26f, 2.6f, -.34f), .065f, WHITE, light);
        }
        // Shoulders, pauldrons with their gears, the arms
        float[] swings = {.15f * c - .1f, .9f * windup * -1 + 1.45f * punch};
        float[] bends = {.35f, 1.6f * windup + .2f * (1 - punch) * (1 - windup)};
        for (int side = -1; side <= 1; side += 2) {
            poseStack.pushPose();
            poseStack.translate(side * .62f, 2.12f, 0);
            ClockMesh.turned(poseStack.last(), buffer, BRASS, new float[] {0, .25f, .24f, .18f, .3f, .02f, .28f, -.06f, 0, -.06f}, 16, WHITE, light);
            ClockMesh.gearAt(poseStack, buffer, COPPER, new Vector3f(side * .27f, .07f, 0), new Vector3f(1, 0, 0), age * .08f * side, .17f, 9, .05f, 0, WHITE, light);
            boolean right = side > 0;
            float sw = right ? swings[1] : -swings[0] * .6f + .1f * windup;
            float bd = right ? bends[1] : bends[0];
            arm(poseStack, buffer, sw, bd, right ? .28f * punch : 0, side * .08f, age * .1f, light);
            poseStack.popPose();
        }
        // The neck and the helmet, its slit glowing
        ClockMesh.cylinder(poseStack.last(), buffer, STEEL, .13f, 2.3f, 2.46f, 10, WHITE, light);
        ClockMesh.turned(poseStack.last(), buffer, COPPER, new float[] {0, 2.42f, .23f, 2.42f, .28f, 2.55f, .28f, 2.72f, .21f, 2.86f, .09f, 2.93f, 0, 2.94f}, 20, WHITE,
                light);
        ClockMesh.rod(poseStack, buffer, BRASS, new Vector3f(0, 2.92f, 0), new Vector3f(0, 3.12f, 0), .03f, WHITE, light);
        ClockMesh.gearAt(poseStack, buffer, BRASS, new Vector3f(0, 3.12f, 0), new Vector3f(0, 0, 1), age * .15f, .1f, 8, .03f, 0, WHITE, light);
        poseStack.pushPose();
        poseStack.translate(0, 2.64f, .26f);
        Sculpt.block(BossMesh.light(buffer, ClockMesh.CORE), poseStack.last(), .34f, .07f, .06f, .01f, .5f, BossMesh.glow(0xFFFFFF, .95f), light);
        poseStack.popPose();
        ClockMesh.glow(poseStack, buffer, view, new Vector3f(0, 2.64f, .3f), .8f, ClockMesh.BLUE, .6f + .3f * punch, light);
        poseStack.popPose();
        poseStack.popPose();
        // Steam from the stacks, and from the elbow as the punch lands (in the world's frame)
        Quaternionf turn = new Quaternionf().rotationY(yaw(dir));
        float lift = (up - 1) * 2.9f - end * 2.9f + bob;
        for (int side = -1; side <= 1; side += 2) {
            Vector3f top = turn.transform(new Vector3f(side * .26f, 2.62f, -.34f)).add(0, lift, 0);
            ClockMesh.steam(poseStack, buffer, view, top, new Vector3f(0, 1, -.3f), 1.6f, .35f, 4, age, .05f + .03f * walking, .5f * (1 - end), seed + side, light);
        }
        if (punch > 0) {
            Vector3f fist = turn.transform(new Vector3f(.75f, 2.0f, 1.6f)).add(0, lift, 0);
            ClockMesh.glow(poseStack, buffer, view, fist, 1.4f * punch, ClockMesh.BLUE_WHITE, punch * .6f, light);
            ClockMesh.steam(poseStack, buffer, view, turn.transform(new Vector3f(.7f, 1.9f, .4f)).add(0, lift, 0), turn.transform(new Vector3f(1, .3f, -.6f)), 1.5f, .5f,
                    4, since, .1f, punch * .8f, seed + 7, light);
            ClockMesh.sparks(poseStack, buffer, view, fist, 1, 8, since * 2, punch, seed, light);
        }
    }

    // ---------------------------------------------------------------- locomotive

    private static void driverPair(PoseStack poseStack, MultiBufferSource buffer, float z, float r, float y, float turn, int light) {
        for (int side = -1; side <= 1; side += 2) {
            poseStack.pushPose();
            poseStack.translate(side * .7f, y, z);
            poseStack.mulPose(new Quaternionf().rotationY(Mth.HALF_PI));
            poseStack.mulPose(new Quaternionf().rotationZ(turn));
            ClockMesh.wheel(poseStack, buffer, ENAMEL, r, .14f, r > .5f ? 12 : 8, WHITE, light);
            poseStack.popPose();
        }
    }

    /** The engine, its front at the pose's origin, facing +Z, its wheels turned {@code roll} along. */
    private static void engine(PoseStack poseStack, MultiBufferSource buffer, WaterMesh.View view, float roll, float age, int light) {
        // Frame and buffer beam
        ClockMesh.boxAt(poseStack, buffer, STEEL, 0, .78f, -2.7f, 1.2f, .24f, 5.3f, WHITE, light);
        ClockMesh.boxAt(poseStack, buffer, ENAMEL, 0, .95f, -.02f, 1.6f, .3f, .16f, WHITE, light);
        // Wheels: a pony truck, two great drivers, a trailing wheel
        driverPair(poseStack, buffer, -.7f, .34f, .34f, roll / .34f, light);
        driverPair(poseStack, buffer, -1.95f, .64f, .64f, roll / .64f, light);
        driverPair(poseStack, buffer, -3.3f, .64f, .64f, roll / .64f, light);
        driverPair(poseStack, buffer, -4.7f, .4f, .4f, roll / .4f, light);
        // Cylinders, the rods: coupling rod between the drivers' pins, the main rod from the crosshead
        float phi = roll / .64f;
        for (int side = -1; side <= 1; side += 2) {
            float ph = phi + (side > 0 ? 0 : Mth.HALF_PI);
            Vector3f pin1 = new Vector3f(side * .86f, .64f + .34f * Mth.cos(ph), -1.95f + .34f * Mth.sin(ph));
            Vector3f pin2 = new Vector3f(side * .86f, .64f + .34f * Mth.cos(ph), -3.3f + .34f * Mth.sin(ph));
            ClockMesh.rod(poseStack, buffer, STEEL, pin1, pin2, .05f, WHITE, light);
            poseStack.pushPose();
            poseStack.translate(side * .82f, .9f, -.95f);
            poseStack.mulPose(new Quaternionf().rotationX(Mth.HALF_PI));
            ClockMesh.turned(poseStack.last(), buffer, BRASS, new float[] {0, -.4f, .2f, -.4f, .24f, -.35f, .24f, .35f, .2f, .4f, 0, .4f}, 14, WHITE, light);
            poseStack.popPose();
            Vector3f cross = new Vector3f(side * .86f, .9f, -1.35f + .25f * Mth.sin(ph) * .5f);
            ClockMesh.rod(poseStack, buffer, STEEL, cross, pin1, .045f, WHITE, light);
        }
        // The boiler: red enamel bound in brass, the smokebox and its door, the headlight
        poseStack.pushPose();
        poseStack.translate(0, 1.5f, 0);
        poseStack.mulPose(new Quaternionf().rotationX(Mth.HALF_PI));
        ClockMesh.turned(poseStack.last(), buffer, ENAMEL, new float[] {0, -4.05f, .62f, -4.05f, .62f, -.55f, 0, -.55f}, 24, WHITE, light);
        ClockMesh.turned(poseStack.last(), buffer, STEEL, new float[] {0, -.6f, .67f, -.6f, .67f, .02f, 0, .02f}, 24, WHITE, light);
        ClockMesh.turned(poseStack.last(), buffer, BRASS, new float[] {.55f, .0f, .55f, .06f, .4f, .12f, 0, .14f}, 24, WHITE, light);
        for (float z : new float[] {-.6f, -1.5f, -2.5f, -3.5f}) {
            poseStack.pushPose();
            poseStack.translate(0, z, 0);
            Sculpt.torus(BossMesh.solid(buffer, BRASS), poseStack.last(), .63f, .04f, 32, 6, 0, Mth.TWO_PI, 4, WHITE, light);
            poseStack.popPose();
        }
        poseStack.popPose();
        // The smokestack, the steam dome, the bell
        poseStack.pushPose();
        poseStack.translate(0, 2.0f, -.4f);
        ClockMesh.turned(poseStack.last(), buffer, STEEL, new float[] {0, 0, .2f, 0, .18f, .4f, .32f, .82f, .35f, .95f, .27f, .97f, .2f, .5f}, 16, WHITE, light);
        poseStack.popPose();
        poseStack.pushPose();
        poseStack.translate(0, 2.05f, -2.1f);
        ClockMesh.turned(poseStack.last(), buffer, BRASS, new float[] {0, -.1f, .32f, -.1f, .3f, .16f, .2f, .34f, 0, .4f}, 16, WHITE, light);
        poseStack.translate(0, 0, -1.1f);
        ClockMesh.turned(poseStack.last(), buffer, BRASS, new float[] {0, .2f, .16f, .14f, .2f, -.05f, .23f, -.1f, 0, -.1f}, 12, WHITE, light);
        poseStack.popPose();
        // The headlight: a brass lamp with a lens of blue light
        poseStack.pushPose();
        poseStack.translate(0, 2.25f, -.25f);
        poseStack.mulPose(new Quaternionf().rotationX(Mth.HALF_PI));
        ClockMesh.turned(poseStack.last(), buffer, BRASS, new float[] {0, -.15f, .18f, -.15f, .2f, .2f, .16f, .22f, 0, .22f}, 14, WHITE, light);
        poseStack.popPose();
        ClockMesh.core(poseStack, buffer, view, new Vector3f(0, 2.25f, .02f), .12f, age, 1, light);
        // The cab: red enamel, a steel roof, windows of blue light
        ClockMesh.boxAt(poseStack, buffer, ENAMEL, 0, 1.6f, -4.75f, 1.5f, 1.3f, 1.4f, WHITE, light);
        ClockMesh.boxAt(poseStack, buffer, STEEL, 0, 2.3f, -4.75f, 1.75f, .1f, 1.75f, WHITE, light);
        for (int side = -1; side <= 1; side += 2) {
            ClockMesh.boxAt(poseStack, buffer, BRASS, side * .76f, 1.92f, -4.75f, .04f, .5f, .62f, WHITE, light);
            poseStack.pushPose();
            poseStack.translate(side * .78f, 1.92f, -4.75f);
            Sculpt.block(BossMesh.light(buffer, ClockMesh.CORE), poseStack.last(), .03f, .4f, .52f, .005f, .5f, BossMesh.glow(0xFFFFFF, .8f), light);
            poseStack.popPose();
            poseStack.pushPose();
            poseStack.translate(side * .38f, 1.95f, -4.04f);
            Sculpt.block(BossMesh.light(buffer, ClockMesh.CORE), poseStack.last(), .32f, .36f, .03f, .005f, .5f, BossMesh.glow(0xFFFFFF, .8f), light);
            poseStack.popPose();
        }
        // The cowcatcher
        for (int b = 0; b < 6; b++) {
            float x = -.7f + b * .28f;
            ClockMesh.rod(poseStack, buffer, STEEL, new Vector3f(x, .82f, .05f), new Vector3f(x * .18f, .26f, .62f), .04f, WHITE, light);
        }
        ClockMesh.rod(poseStack, buffer, STEEL, new Vector3f(-.75f, .26f, .1f), new Vector3f(0, .26f, .62f), .04f, WHITE, light);
        ClockMesh.rod(poseStack, buffer, STEEL, new Vector3f(.75f, .26f, .1f), new Vector3f(0, .26f, .62f), .04f, WHITE, light);
        // The firebox's blue glow under the cab
        ClockMesh.glow(poseStack, buffer, view, new Vector3f(0, .9f, -4.1f), 1.6f, ClockMesh.BLUE, .7f, light);
    }

    /**
     * The track laid {@code laid} along from the scene, the engine's front at {@code front} along it ({@code visible} 0 to 1 as
     * it appears), {@code gone} ticks after it burst in steam at the end of the track (negative before).
     */
    public static void locomotive(PoseStack poseStack, MultiBufferSource buffer, WaterMesh.View view, Vector3f dir, float age, int life, float laid, float front,
                                  float visible, float gone, float speed, int seed, int light) {
        float sink = BossMesh.ramp(age, life - 14, life);
        poseStack.pushPose();
        poseStack.mulPose(new Quaternionf().rotationY(yaw(dir)));
        poseStack.translate(0, -sink * .5f, 0);
        // The track: sleepers, two rails, a line of blue light where it is being laid
        for (float z = -1.2f; z < laid; z += .8f) {
            ClockMesh.boxAt(poseStack, buffer, ClockMesh.SLEEPER, 0, .07f, z, 2.1f, .14f, .32f, WHITE, light);
        }
        float start = -1.4f;
        if (laid > start) {
            for (int side = -1; side <= 1; side += 2) {
                ClockMesh.boxAt(poseStack, buffer, STEEL, side * .72f, .22f, (start + laid) * .5f, .12f, .16f, laid - start, WHITE, light);
            }
        }
        if (age < 18 || front < laid - .5f && visible <= 0) {
            float glowK = Math.max(0, 1 - Math.max(0, age - 14) / 4f);
            ClockMesh.glow(poseStack, buffer, view, new Vector3f(0, .3f, laid), 2.2f, ClockMesh.BLUE, glowK, light);
            ClockMesh.sparks(poseStack, buffer, view, new Vector3f(0, .2f, laid), .8f, 10, age * 2, glowK, seed, light);
        }
        poseStack.pushPose();
        poseStack.translate(0, 0, front);
        if (visible > 0 && gone < 0) {
            poseStack.pushPose();
            poseStack.translate(0, (visible - 1) * 3, 0);
            engine(poseStack, buffer, view, front, age, light);
            poseStack.popPose();
            // Smoke from the stack, swept back; steam chuffing from the cylinders; sparks under the drivers
            ClockMesh.smoke(poseStack, buffer, view, new Vector3f(0, 3.0f, -.4f), new Vector3f(0, .6f, -1), 4.5f + speed * 4, 1.1f, 9, age, .09f, .8f, 0x5A5A60, seed,
                    light);
            ClockMesh.steam(poseStack, buffer, view, new Vector3f(0, 3.1f, -.4f), new Vector3f(0, 1, -.6f), 3, .8f, 5, age, .12f, .6f, seed + 3, light);
            for (int side = -1; side <= 1; side += 2) {
                ClockMesh.steam(poseStack, buffer, view, new Vector3f(side * 1.0f, .6f, -.9f), new Vector3f(side, .1f, -.4f), 1.6f, .55f, 4, age * 1.5f, .1f, .7f,
                        seed + side * 5, light);
            }
            ClockMesh.sparks(poseStack, buffer, view, new Vector3f(0, .2f, -2.6f), 1.4f, 10, age * 2, Math.min(1, speed * 1.2f), seed, light);
            ClockMesh.glow(poseStack, buffer, view, new Vector3f(0, 2.25f, 1.2f), 3.2f, ClockMesh.BLUE, .35f, light);
        }
        if (visible > 0 && visible < 1) {
            ClockMesh.steam(poseStack, buffer, view, new Vector3f(0, .5f, -2.5f), new Vector3f(0, 1, 0), 3, 1.6f, 8, age, .08f, (1 - visible), seed + 11, light);
        }
        if (gone >= 0 && gone < 20) {
            float t = gone / 20;
            poseStack.pushPose();
            poseStack.translate(0, 1.5f, 0);
            ClockMesh.burst(poseStack, buffer, view, t, 1.6f, seed, light);
            ClockMesh.steam(poseStack, buffer, view, new Vector3f(0, 0, -2), new Vector3f(0, 1, .3f), 5, 2.2f, 10, gone, .05f, (1 - t) * .9f, seed + 1, light);
            poseStack.popPose();
        }
        poseStack.popPose();
        poseStack.popPose();
    }

    // ---------------------------------------------------------------- airship

    private static void propeller(PoseStack poseStack, MultiBufferSource buffer, WaterMesh.View view, float r, float spin, int blades, int light) {
        poseStack.pushPose();
        poseStack.mulPose(new Quaternionf().rotationZ(spin));
        for (int b = 0; b < blades; b++) {
            poseStack.pushPose();
            poseStack.mulPose(new Quaternionf().rotationZ(b * Mth.TWO_PI / blades));
            poseStack.translate(r * .5f, 0, 0);
            poseStack.mulPose(new Quaternionf().rotationX(.35f));
            ClockMesh.box(poseStack.last(), buffer, BRASS, r, r * .2f, .04f, WHITE, light);
            poseStack.popPose();
        }
        poseStack.popPose();
        BossMesh.annulus(BossMesh.light(buffer, BossMesh.GLOW), poseStack.last(), new Vector3f(), new Vector3f(1, 0, 0), new Vector3f(0, 1, 0), r * .2f, r * 1.05f, 24, 1,
                0, 0, BossMesh.glow(0xFFE0B0, .05f), BossMesh.glow(0xFFE0B0, .18f), light, true);
    }

    /** The ship itself, its middle at the pose's origin, flying along +Z. */
    private static void ship(PoseStack poseStack, MultiBufferSource buffer, WaterMesh.View view, float age, int light) {
        // The envelope, its ribs, its nose and tail
        int n = 16;
        float[] prof = new float[(n + 1) * 2];
        float[] radius = new float[n + 1];
        for (int i = 0; i <= n; i++) {
            float z = Mth.lerp((float) i / n, -4.2f, 3.8f);
            float u = (z + .2f) / 4.0f;
            float r = 1.6f * (float) Math.sqrt(Math.max(0, 1 - u * u)) * (z < -2 ? .85f + .15f * (z + 4.2f) / 2.2f : 1);
            radius[i] = r;
            prof[2 * i] = r;
            prof[2 * i + 1] = z;
        }
        poseStack.pushPose();
        poseStack.mulPose(new Quaternionf().rotationX(Mth.HALF_PI));
        ClockMesh.turned(poseStack.last(), buffer, ClockMesh.CANVAS, prof, 32, WHITE, light);
        for (int i = 2; i <= n - 2; i += 3) {
            poseStack.pushPose();
            poseStack.translate(0, prof[2 * i + 1], 0);
            Sculpt.torus(BossMesh.solid(buffer, BRASS), poseStack.last(), radius[i] + .02f, .05f, 40, 6, 0, Mth.TWO_PI, 4, WHITE, light);
            poseStack.popPose();
        }
        ClockMesh.turned(poseStack.last(), buffer, BRASS, new float[] {0, 3.55f, .3f, 3.55f, .2f, 3.85f, 0, 3.92f}, 12, WHITE, light);
        ClockMesh.turned(poseStack.last(), buffer, BRASS, new float[] {0, -4.45f, .12f, -4.45f, .4f, -4.0f, 0, -3.9f}, 12, WHITE, light);
        poseStack.popPose();
        // The fins
        for (int f = 0; f < 4; f++) {
            poseStack.pushPose();
            poseStack.mulPose(new Quaternionf().rotationZ(f * Mth.HALF_PI + Mth.PI / 4));
            poseStack.mulPose(new Quaternionf().rotationX(Mth.HALF_PI));
            Sculpt.slab(BossMesh.solid(buffer, COPPER), poseStack.last(), ClockMesh.ccw(new float[] {.3f, -2.3f, 1.55f, -3.5f, 1.75f, -4.5f, .3f, -4.25f}), .07f, .02f,
                    .5f, WHITE, 0xFFC08060, light);
            poseStack.popPose();
        }
        // The propeller at its tail
        poseStack.pushPose();
        poseStack.translate(0, 0, -4.55f);
        propeller(poseStack, buffer, view, 1.1f, age * .9f, 4, light);
        poseStack.popPose();
        // The gondola hanging under it, its struts, its windows, its two engines
        poseStack.pushPose();
        poseStack.translate(0, -2.1f, 0);
        poseStack.pushPose();
        poseStack.mulPose(new Quaternionf().rotationX(Mth.HALF_PI));
        ClockMesh.turned(poseStack.last(), buffer, BRASS, new float[] {0, -1.7f, .25f, -1.6f, .45f, -1.1f, .5f, .6f, .42f, 1.3f, .2f, 1.7f, 0, 1.78f}, 20, WHITE, light);
        poseStack.popPose();
        ClockMesh.boxAt(poseStack, buffer, ENAMEL, 0, .42f, 0, .7f, .1f, 2.4f, WHITE, light);
        for (int side = -1; side <= 1; side += 2) {
            for (int w = 0; w < 3; w++) {
                poseStack.pushPose();
                poseStack.translate(side * .47f, .08f, -.8f + w * .8f);
                Sculpt.block(BossMesh.light(buffer, ClockMesh.CORE), poseStack.last(), .05f, .2f, .3f, .005f, .5f, BossMesh.glow(0xFFFFFF, .85f), light);
                poseStack.popPose();
            }
            for (float z : new float[] {-1.0f, 1.0f}) {
                ClockMesh.rod(poseStack, buffer, STEEL, new Vector3f(side * .3f, .45f, z), new Vector3f(side * .7f, .95f, z * 1.3f), .035f, WHITE, light);
            }
            ClockMesh.rod(poseStack, buffer, STEEL, new Vector3f(side * .45f, 0, -.5f), new Vector3f(side * 1.35f, .1f, -.5f), .045f, WHITE, light);
            poseStack.pushPose();
            poseStack.translate(side * 1.35f, .1f, -.5f);
            poseStack.pushPose();
            poseStack.mulPose(new Quaternionf().rotationX(Mth.HALF_PI));
            ClockMesh.turned(poseStack.last(), buffer, COPPER, new float[] {0, -.45f, .12f, -.42f, .17f, -.2f, .17f, .25f, .1f, .4f, 0, .42f}, 12, WHITE, light);
            poseStack.popPose();
            poseStack.translate(0, 0, -.5f);
            propeller(poseStack, buffer, view, .45f, -age * 1.2f * side, 3, light);
            poseStack.popPose();
        }
        ClockMesh.core(poseStack, buffer, view, new Vector3f(0, -.45f, 1.0f), .12f, age, .9f, light);
        poseStack.popPose();
    }

    /** A bomb falling, its nose turned along {@code fall}, a fuse sputtering on its tail. */
    private static void bomb(PoseStack poseStack, MultiBufferSource buffer, WaterMesh.View view, Vector3f at, Vector3f fall, float age, int seed, int light) {
        poseStack.pushPose();
        poseStack.translate(at.x, at.y, at.z);
        poseStack.mulPose(new Quaternionf().rotationTo(new Vector3f(0, -1, 0), ClockModels.unit(fall)));
        BossMesh.sphere(BossMesh.solid(buffer, STEEL), poseStack.last(), new Vector3f(), .24f, 6, 10, 0, 0, 1, WHITE, light);
        Sculpt.torus(BossMesh.solid(buffer, BRASS), poseStack.last(), .24f, .035f, 16, 6, 0, Mth.TWO_PI, 2, WHITE, light);
        for (int f = 0; f < 4; f++) {
            poseStack.pushPose();
            poseStack.mulPose(new Quaternionf().rotationY(f * Mth.HALF_PI));
            ClockMesh.boxAt(poseStack, buffer, BRASS, .1f, .3f, 0, .2f, .2f, .03f, WHITE, light);
            poseStack.popPose();
        }
        poseStack.popPose();
        Vector3f tail = new Vector3f(ClockModels.unit(fall)).mul(-.4f).add(at);
        ClockMesh.glow(poseStack, buffer, view, tail, .5f + .2f * Mth.sin(age * 2 + seed), ClockMesh.EMBER, .9f, light);
    }

    /** A bomb going off {@code t} (0 to 1) of the way through, at {@code at}. */
    private static void blast(PoseStack poseStack, MultiBufferSource buffer, WaterMesh.View view, Vector3f at, float t, int seed, int light) {
        float out = 1 - (1 - t) * (1 - t), fade = 1 - t;
        poseStack.pushPose();
        poseStack.translate(at.x, at.y, at.z);
        ClockMesh.glow(poseStack, buffer, view, new Vector3f(0, .8f, 0), 2 + 4 * out, ClockMesh.EMBER, fade * fade, light);
        ClockMesh.glow(poseStack, buffer, view, new Vector3f(0, .6f, 0), 1.2f + 2 * out, 0xFFF0C0, fade * fade * fade, light);
        ClockMesh.ring(poseStack.last(), buffer, .06f, 2.6f * out, .4f + 2.8f * out, ClockMesh.EMBER, fade * .8f, light);
        ClockMesh.sparks(poseStack, buffer, view, new Vector3f(0, .3f, 0), 2.6f, 14, t * 12, fade, seed, light);
        ClockMesh.smoke(poseStack, buffer, view, new Vector3f(0, .5f, 0), new Vector3f(0, 1, 0), 3.2f, 1.3f, 7, t * 9, .11f, fade * .85f, 0x4A4646, seed, light);
        poseStack.popPose();
    }

    /**
     * The airship flying over along {@code dir}, {@code along} from the middle of the bombed ground (the pose's origin),
     * {@code height} up; its bombs, each dropped at {@code dropAt(i)} and landing {@code fall} ticks later at {@code landsAt(i)}.
     */
    public static void airship(PoseStack poseStack, MultiBufferSource buffer, WaterMesh.View view, Vector3f dir, float age, int life, float along, float height,
                               int bombs, IntUnaryOperator dropAt, IntFunction<Vector3f> landsAt, int fall, float speed, int seed, int light) {
        Vector3f d = new Vector3f(dir.x, 0, dir.z);
        d = d.lengthSquared() < 1.0E-6f ? new Vector3f(0, 0, 1) : d.normalize();
        float in = Math.min(1, age / 10f), out = 1 - BossMesh.ramp(age, life - 10, life);
        float scale = Math.min(in, out);
        Vector3f at = new Vector3f(d).mul(along).add(0, height, 0);
        if (scale > 0) {
            poseStack.pushPose();
            poseStack.translate(at.x, at.y + .15f * Mth.sin(age * .08f), at.z);
            poseStack.mulPose(new Quaternionf().rotationY(yaw(d)).rotateZ(.04f * Mth.sin(age * .05f)));
            poseStack.scale(scale, scale, scale);
            ship(poseStack, buffer, view, age, light);
            poseStack.popPose();
            // Its engines' steam, and its searchlight on the ground below
            Vector3f back = new Vector3f(d).negate();
            ClockMesh.steam(poseStack, buffer, view, new Vector3f(d).mul(-1.1f).add(at).add(0, -2.0f, 0), back, 3, .6f, 5, age, .1f, .45f * scale, seed, light);
            VertexConsumer beam = BossMesh.light(buffer, BossMesh.BEAM);
            Vector3f lamp = new Vector3f(d).mul(1).add(at).add(0, -2.6f, 0);
            RitualMesh.shaft(beam, poseStack.last(), view.eye(), lamp, new Vector3f(d).mul(along + 2.5f).add(0, .1f, 0), 1.6f, BossMesh.glow(ClockMesh.BLUE, .12f * scale),
                    light);
        }
        for (int i = 0; i < bombs; i++) {
            int drop = dropAt.applyAsInt(i);
            float t = age - drop;
            if (t < 0 || t > fall + 16) {
                continue;
            }
            Vector3f lands = landsAt.apply(i);
            if (t <= fall) {
                float u = t / fall;
                float shipThen = along - speed * (age - drop);
                Vector3f from = new Vector3f(d).mul(shipThen).add(0, height - 2.6f, 0);
                Vector3f pos = new Vector3f(from).lerp(new Vector3f(lands.x, 0, lands.z), u);
                pos.y = (height - 2.6f) * (1 - u * u) + .25f;
                Vector3f fallDir = new Vector3f(lands.x - from.x, -(height - 2.6f) * 2 * Math.max(u, .15f), lands.z - from.z);
                bomb(poseStack, buffer, view, pos, fallDir, age, seed + i, light);
            } else {
                blast(poseStack, buffer, view, lands, (t - fall) / 16, seed + i * 13, light);
            }
        }
    }

    // ---------------------------------------------------------------- great mechanism

    public static void mechanism(PoseStack poseStack, MultiBufferSource buffer, WaterMesh.View view, float age, int life, float radius, float since,
                                 Vector3f target, int rise, int overload, int seed, int light) {
        float k = BossMesh.ramp(age, 0, rise);
        float up = 1 - (1 - k) * (1 - k) * (1 - k);
        float blow = BossMesh.ramp(age, overload, overload + 30);
        float spin = age * .012f;
        float sinkAll = blow * 4;
        // The great gear underfoot and the four meshing round it
        float big = 4.2f;
        ClockMesh.gearAt(poseStack, buffer, BRASS, new Vector3f(0, .06f - (1 - up) * .6f - sinkAll, 0), new Vector3f(0, 1, 0), spin, big, 36, .2f, 8, WHITE, light);
        for (int g = 0; g < 4; g++) {
            float a = g * Mth.HALF_PI + Mth.PI / 4;
            float r = 1.6f, dist = big + r - .18f;
            float turn = ClockMesh.mesh(spin, 36, 14) + Mth.PI / 14 + a * (1 + 36f / 14);
            ClockMesh.gearAt(poseStack, buffer, COPPER, new Vector3f(Mth.cos(a) * dist, .08f - (1 - up) * .6f - sinkAll, Mth.sin(a) * dist), new Vector3f(0, 1, 0), turn,
                    r, 14, .16f, 4, WHITE, light);
        }
        // The great toothed ring round the field
        float ringR = radius * .92f;
        poseStack.pushPose();
        poseStack.translate(0, .15f - (1 - up) * .8f - sinkAll, 0);
        poseStack.mulPose(new Quaternionf().rotationY(-spin * .4f));
        Sculpt.torus(BossMesh.solid(buffer, BRASS), poseStack.last(), ringR, .16f, 96, 8, 0, Mth.TWO_PI, 24, WHITE, light);
        VertexConsumer steel = BossMesh.solid(buffer, STEEL);
        for (int t = 0; t < 64; t++) {
            poseStack.pushPose();
            poseStack.mulPose(new Quaternionf().rotationY(t * Mth.TWO_PI / 64));
            poseStack.translate(ringR - .24f, 0, 0);
            Sculpt.block(steel, poseStack.last(), .24f, .22f, .3f, .03f, .5f, WHITE, light);
            poseStack.popPose();
        }
        poseStack.popPose();
        // Eight great gears standing round, rising out of the ground; pistons pumping between them
        float standR = radius * .75f;
        int stands = 8;
        Vector3f[] hubs = new Vector3f[stands];
        for (int g = 0; g < stands; g++) {
            float a = g * Mth.TWO_PI / stands;
            float r = 2.3f;
            Vector3f radial = new Vector3f(Mth.cos(a), 0, Mth.sin(a));
            float y = Mth.lerp(up, -r - .4f, r + .25f) - sinkAll * 1.5f;
            hubs[g] = new Vector3f(radial).mul(standR).add(0, y, 0);
            ClockMesh.gearAt(poseStack, buffer, g % 2 == 0 ? BRASS : COPPER, hubs[g], radial, (g % 2 == 0 ? 1 : -1) * spin * 2.2f, r, 22, .3f, 6, WHITE, light);
            ClockMesh.core(poseStack, buffer, view, new Vector3f(radial).mul(standR - .25f).add(0, y, 0), .16f, age + g * 5, .9f * up * (1 - blow), light);
            // The piston between this gear and the next
            float b = a + Mth.PI / stands;
            Vector3f at = new Vector3f(Mth.cos(b), 0, Mth.sin(b)).mul(standR * .98f);
            float pump = .5f + .5f * Mth.sin(age * .3f + g * 1.3f);
            float base = (up - 1) * 3 - sinkAll * 1.5f;
            poseStack.pushPose();
            poseStack.translate(at.x, base, at.z);
            ClockMesh.turned(poseStack.last(), buffer, BRASS, new float[] {0, 0, .42f, 0, .42f, .2f, .32f, .3f, .32f, 1.5f, .38f, 1.6f, 0, 1.62f}, 16, WHITE, light);
            ClockMesh.cylinder(poseStack.last(), buffer, STEEL, .14f, 1.5f, 2.2f + pump * 1.2f, 10, WHITE, light);
            poseStack.translate(0, 2.2f + pump * 1.2f, 0);
            ClockMesh.turned(poseStack.last(), buffer, COPPER, new float[] {0, 0, .3f, 0, .3f, .2f, 0, .22f}, 14, WHITE, light);
            poseStack.popPose();
            if (up >= 1 && blow < 1) {
                float vent = Math.max(0, Mth.sin(age * .3f + g * 1.3f - .6f));
                ClockMesh.steam(poseStack, buffer, view, new Vector3f(at).add(0, base + 1.65f, 0), new Vector3f(0, 1, 0), 2.6f, .6f, 4, age + g * 7, .09f,
                        .35f + .45f * vent, seed + g, light);
            }
        }
        // The astrolabe high above: tumbling rings round the great core
        Vector3f core = new Vector3f(0, Mth.lerp(up, 1.5f, 7.5f) + .25f * Mth.sin(age * .05f), 0);
        float coreK = up * (1 - BossMesh.ramp(age, overload + 4, overload + 12));
        if (coreK > 0) {
            float[] rs = {3.1f, 2.6f, 2.1f};
            ResourceLocation[] metals = {BRASS, COPPER, BRASS};
            Quaternionf[] turns = {
                    new Quaternionf().rotationY(age * .02f).rotateX(.5f + age * .025f),
                    new Quaternionf().rotationZ(age * .03f + 1).rotateY(-age * .02f),
                    new Quaternionf().rotationX(1.2f).rotateY(age * .045f)};
            for (int i = 0; i < 3; i++) {
                poseStack.pushPose();
                poseStack.translate(core.x, core.y, core.z);
                poseStack.mulPose(turns[i]);
                poseStack.scale(coreK, coreK, coreK);
                Sculpt.torus(BossMesh.solid(buffer, metals[i]), poseStack.last(), rs[i], .1f, 64, 8, 0, Mth.TWO_PI, 8, WHITE, light);
                VertexConsumer teeth = BossMesh.solid(buffer, STEEL);
                for (int t = 0; t < 24; t++) {
                    poseStack.pushPose();
                    poseStack.mulPose(new Quaternionf().rotationY(t * Mth.TWO_PI / 24));
                    poseStack.translate(rs[i] + .12f, 0, 0);
                    Sculpt.block(teeth, poseStack.last(), .14f, .14f, .16f, .02f, .5f, WHITE, light);
                    poseStack.popPose();
                }
                poseStack.popPose();
            }
            ClockMesh.core(poseStack, buffer, view, core, .95f * coreK * (1 + blow * 1.5f), age, 1, light);
            ClockMesh.glow(poseStack, buffer, view, core, 9 * coreK, ClockMesh.BLUE, .35f, light);
            // Arcs from the core to the great gears, leaping afresh every few ticks
            for (int g = 0; g < stands; g++) {
                if (BossMesh.hash(seed + g * 31 + Mth.floor(age / 4) * 7) > .55f) {
                    ClockMesh.arc(poseStack, buffer, view.eye(), core, hubs[g], age, .09f, .8f * coreK, seed + g * 13, light);
                }
            }
            // The core's strike on an enemy
            if (target != null && since < 8 && blow <= 0) {
                float f = 1 - since / 8;
                ClockMesh.arc(poseStack, buffer, view.eye(), core, target, age, .2f, f, seed + 99, light);
                ClockMesh.arc(poseStack, buffer, view.eye(), core, target, age + 1, .12f, f, seed + 77, light);
                ClockMesh.glow(poseStack, buffer, view, target, 2.5f, ClockMesh.BLUE_WHITE, f, light);
                ClockMesh.sparks(poseStack, buffer, view, target, 1, 8, since * 2, f, seed, light);
            }
        }
        ClockMesh.ring(poseStack.last(), buffer, .05f, radius * .95f, radius, ClockMesh.BLUE, .4f * up * (1 - blow), light);
        // The overload: the core flares white, a wave of blue light runs out over the field, the machine blows apart in steam
        if (blow > 0 && blow < 1) {
            float out = 1 - (1 - blow) * (1 - blow), fade = 1 - blow;
            ClockMesh.glow(poseStack, buffer, view, core, 6 + 26 * out, ClockMesh.BLUE_WHITE, fade, light);
            ClockMesh.glow(poseStack, buffer, view, core, 3 + 8 * out, 0xFFFFFF, fade * fade, light);
            ClockMesh.ring(poseStack.last(), buffer, .1f, radius * 1.15f * out - 2, radius * 1.15f * out, ClockMesh.BLUE_WHITE, fade, light);
            ClockMesh.ring(poseStack.last(), buffer, .12f, Math.max(0, radius * .8f * out - 4), radius * .8f * out, ClockMesh.BLUE, fade * .7f, light);
            poseStack.pushPose();
            poseStack.translate(core.x, core.y, core.z);
            ClockMesh.burst(poseStack, buffer, view, blow, 3, seed, light);
            poseStack.popPose();
            for (int g = 0; g < stands; g++) {
                float a = g * Mth.TWO_PI / stands;
                ClockMesh.steam(poseStack, buffer, view, new Vector3f(Mth.cos(a) * standR, 1, Mth.sin(a) * standR), new Vector3f(Mth.cos(a), 1.2f, Mth.sin(a)), 6, 2.2f,
                        3, blow * 20, .05f, fade * .9f, seed + g, light);
            }
        }
    }
}
