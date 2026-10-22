package com.waylays.arcanes.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.waylays.arcanes.ArcanesOublies;
import com.waylays.arcanes.entity.gold.GoldenDragon;
import com.waylays.arcanes.spells.gem.Gem;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/**
 * The Golden Dragon of Eldorado, built every frame: a long round body covered in scales of gold, its belly in pale
 * gold plates, that follows the path the dragon flew; a crest of crimson fins down its back and a crimson tuft at its
 * tail; four legs with ivory claws, paddling the air; and its head: a long snout with a jaw that opens, ivory teeth,
 * antlered horns swept back, a crimson mane and beard, ruby eyes, and two long whiskers of light flowing back. A
 * flaming pearl of gold floats before its jaws. A sheen of light runs down its body from head to tail.
 * <p>
 * Coming out of its sun, it is drawn only as far as it has come out; melting at the end, it goes from the tail up.
 */
public class GoldenDragonRenderer extends EntityRenderer<GoldenDragon> {
    private static final ResourceLocation SCALES = ArcanesOublies.id("textures/entity/gold/dragon_scales.png");
    private static final Vec3 UP = new Vec3(0, 1, 0);
    private static final Vector3f LIGHT = new Vector3f(.4f, 1, .6f).normalize();
    private static final int BODY_POINTS = 30;
    private static final double SPACING = .5;
    private static final int RING = 10;
    /** Points of the spine before the flown path: the neck, ahead of the chest. */
    private static final int NECK = 3;

    private static final int GOLD = GoldRender.GOLD;
    private static final int BELLY = 0xFFE9A8;
    private static final int CRIMSON = 0xC8283A;
    private static final int VERMILION = 0xE0522E;
    private static final int IVORY = 0xFFF2D6;
    private static final int PEARL = 0xFFF3C8;

    // The head, along +Y (its length) with +Z up and +X to its right, from the back of the skull at the origin
    private static final float[] SKULL = new Solid.Builder()
            .band(ring(10, 0, .42f, .36f, 0), ring(10, .45f, .38f, .34f, -.02f))
            .band(ring(10, .45f, .38f, .34f, -.02f), ring(10, .95f, .24f, .2f, -.1f))
            .fan(ring(10, .95f, .24f, .2f, -.1f), new float[]{0, 1.12f, -.1f})
            .fan(reversed(ring(10, 0, .42f, .36f, 0)), new float[]{0, -.14f, 0})
            .build();
    private static final float[] JAW = new Solid.Builder()
            .band(ring(6, 0, .3f, .08f, 0), ring(6, .8f, .17f, .05f, 0))
            .fan(ring(6, .8f, .17f, .05f, 0), new float[]{0, .92f, 0})
            .fan(reversed(ring(6, 0, .3f, .08f, 0)), new float[]{0, -.05f, 0})
            .build();

    /** The spine of this frame: positions (relative to the chest), frames and radii. */
    private final List<Vec3> spine = new ArrayList<>();
    private int realPoints;

    public GoldenDragonRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    /** {@code n} points round the Y axis at {@code y}, {@code rx} by {@code rz} across, centred {@code cz} up. */
    private static float[][] ring(int n, float y, float rx, float rz, float cz) {
        float[][] out = Solid.ring(n, y, rx, rz, 0);
        for (float[] p : out) {
            p[2] += cz;
        }
        return out;
    }

    private static float[][] reversed(float[][] ring) {
        float[][] out = new float[ring.length][];
        for (int i = 0; i < ring.length; i++) {
            out[i] = ring[ring.length - 1 - i];
        }
        return out;
    }

    @Override
    public void render(GoldenDragon dragon, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        float age = dragon.tickCount + partialTicks;
        int duration = dragon.getDuration();
        float melt = Mth.clamp((duration - age) / GoldenDragon.MELT_TICKS, 0, 1);
        Vec3 origin = dragon.getPosition(partialTicks);
        Vec3 heading = dragon.getHeading(partialTicks);
        int light = LightTexture.pack(Math.max(12, LightTexture.block(packedLight)), LightTexture.sky(packedLight));

        buildSpine(dragon, origin, heading, age);
        int n = spine.size();
        // Only as far as it has come out of its sun; melting, from the tail up
        int shown = Math.min(n, NECK + realPoints);
        shown = Math.max(NECK + 1, Math.round(shown * melt));
        Vec3[] tangent = new Vec3[n];
        Vec3[] right = new Vec3[n];
        Vec3[] up = new Vec3[n];
        float[] radius = new float[n];
        for (int i = 0; i < n; i++) {
            Vec3 ahead = spine.get(Math.max(0, i - 1));
            Vec3 behind = spine.get(Math.min(n - 1, i + 1));
            Vec3 t = ahead.subtract(behind);
            tangent[i] = t.lengthSqr() < 1.0E-6 ? heading : t.normalize();
            right[i] = safeRight(tangent[i], i > 0 ? right[i - 1] : null);
            up[i] = right[i].cross(tangent[i]).normalize();
            float f = i / (float) (n - 1);
            radius[i] = f < .12f ? .42f + f / .12f * .12f : .54f - .44f * (float) Math.pow((f - .12f) / .88f, 1.2);
        }

        body(buffer.getBuffer(RenderType.entityCutoutNoCull(SCALES)), poseStack.last(), tangent, right, up, radius, shown, shown < n, age, melt, light);

        VertexConsumer metal = GoldRender.metal(buffer);
        crest(metal, poseStack, tangent, right, up, radius, shown, age, light);
        legs(metal, poseStack, tangent, right, up, radius, shown, age, light);
        if (shown == n) {
            tuft(metal, poseStack, spine.get(n - 1), tangent[n - 1], right[n - 1], up[n - 1], age, light);
        }
        Vec3 headRight = safeRight(heading, null);
        Vec3 headUp = headRight.cross(heading).normalize();
        Vec3 skull = heading.scale(1.05);
        float open = dragon.isBreathing() ? 24 + Mth.sin(age * .9f) * 4 : 4 + Mth.sin(age * .15f) * 3;
        poseStack.pushPose();
        basis(poseStack, skull, headRight, heading);
        head(metal, poseStack, buffer, open, age, light);
        poseStack.popPose();

        // Light: its eyes, its whiskers, its pearl and its breath
        for (int side = -1; side <= 1; side += 2) {
            GemRender.glow(poseStack, buffer, local(skull, headRight, heading, headUp, side * .34f, .5f, .1f), Gem.RUBY.color, .5f, .8f);
            whisker(poseStack, buffer, skull, headRight, heading, headUp, side, age);
        }
        if (melt > .5f) {
            Vec3 pearl = skull.add(heading.scale(2.2)).add(headUp.scale(.3 + Mth.sin(age * .1f) * .25)).add(headRight.scale(Mth.cos(age * .07f) * .6));
            GemRender.glow(poseStack, buffer, pearl, GoldRender.MOLTEN, 1.3f + Mth.sin(age * .5f) * .15f, .8f);
            GemRender.glow(poseStack, buffer, pearl, GoldRender.GOLD, 2.4f, .5f);
            poseStack.pushPose();
            poseStack.translate(pearl.x, pearl.y, pearl.z);
            poseStack.mulPose(Axis.YP.rotationDegrees(age * 3));
            GemRender.gem(poseStack, buffer, GemRender.Cut.BRILLIANT, PEARL, .42f, .9f, age);
            poseStack.popPose();
        }
        if (dragon.isBreathing()) {
            Vec3 jaws = local(skull, headRight, heading, headUp, 0, 1, -.22f);
            Vec3 aim = heading;
            GemRender.ray(poseStack, buffer, jaws, jaws.add(aim.scale(9)), GoldRender.MOLTEN, .8f, .9f, age);
            GemRender.ray(poseStack, buffer, jaws, jaws.add(aim.scale(7)), GoldRender.GOLD, .35f, 1, age + 3);
            GemRender.glow(poseStack, buffer, jaws, GoldRender.MOLTEN, 1.6f, 1);
        }
        super.render(dragon, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    // ---------------------------------------------------------------- the body

    private void buildSpine(GoldenDragon dragon, Vec3 origin, Vec3 heading, float age) {
        spine.clear();
        spine.add(heading.scale(1.0));
        spine.add(heading.scale(.5));
        spine.add(Vec3.ZERO);
        List<Vec3> path = followPath(dragon, origin, heading);
        Vec3 side = safeRight(heading, null);
        for (int i = 0; i < path.size(); i++) {
            // A slow serpentine sway, growing toward the tail
            double sway = Mth.sin(age * .18f - i * .45f) * .3 * (i + 1) / path.size();
            double lift = Mth.cos(age * .14f - i * .38f) * .15 * (i + 1) / path.size();
            spine.add(path.get(i).subtract(origin).add(side.scale(sway)).add(0, lift, 0));
        }
    }

    /** Points every {@link #SPACING} back along the flown path; past its start they go straight on (not drawn). */
    private List<Vec3> followPath(GoldenDragon dragon, Vec3 origin, Vec3 heading) {
        List<Vec3> out = new ArrayList<>();
        Vec3 cursor = origin;
        double need = SPACING;
        int k = 0;
        realPoints = 0;
        boolean real = true;
        while (out.size() < BODY_POINTS) {
            Vec3 next = real ? dragon.getTrailPoint(k) : null;
            if (next == null) {
                real = false;
                Vec3 dir = heading.scale(-1);
                if (out.size() >= 2) {
                    Vec3 d = out.get(out.size() - 1).subtract(out.get(out.size() - 2));
                    if (d.lengthSqr() > 1.0E-4) {
                        dir = d.normalize();
                    }
                }
                cursor = cursor.add(dir.scale(need));
                out.add(cursor);
                need = SPACING;
                continue;
            }
            double d = next.distanceTo(cursor);
            if (d >= need) {
                cursor = cursor.add(next.subtract(cursor).scale(need / d));
                out.add(cursor);
                realPoints++;
                need = SPACING;
            } else {
                need -= d;
                cursor = next;
                k++;
            }
        }
        return out;
    }

    /**
     * The scaled tube of the body: rings of points round the spine, smooth-shaded, gold above and pale gold below, a
     * sheen running down it; its end capped (a point at the tail, or flat where it is cut).
     */
    private void body(VertexConsumer out, PoseStack.Pose pose, Vec3[] tangent, Vec3[] right, Vec3[] up, float[] radius, int shown, boolean cut,
                      float age, float melt, int light) {
        Vec3[][] point = new Vec3[shown][RING + 1];
        Vec3[][] normal = new Vec3[shown][RING + 1];
        int[][] colour = new int[shown][RING + 1];
        for (int i = 0; i < shown; i++) {
            Vec3 centre = spine.get(i);
            float sheen = Mth.sin(i * .35f - age * .3f);
            sheen = sheen > 0 ? (float) Math.pow(sheen, 10) * .55f : 0;
            // Melting, the cut end glows like molten gold
            float heat = melt < 1 ? Mth.clamp(1 - (shown - 1 - i) / 4f, 0, 1) * (1 - melt) * 1.5f : 0;
            for (int k = 0; k <= RING; k++) {
                float a = k * Mth.TWO_PI / RING;
                float c = Mth.cos(a), s = Mth.sin(a);
                Vec3 n = right[i].scale(c).add(up[i].scale(s));
                point[i][k] = centre.add(right[i].scale(c * radius[i])).add(up[i].scale(s * radius[i] * .92f));
                normal[i][k] = n;
                float belly = Mth.clamp((-s - .3f) / .4f, 0, 1);
                int base = GoldRender.lerp(GOLD, BELLY, belly);
                base = GoldRender.lerp(base, GoldRender.MOLTEN, Math.min(1, heat));
                float lit = (float) (n.x * LIGHT.x() + n.y * LIGHT.y() + n.z * LIGHT.z());
                float shade = .6f + .4f * Math.max(0, lit);
                int r = (int) Mth.lerp(sheen, ((base >> 16) & 0xFF) * shade, 255);
                int g = (int) Mth.lerp(sheen, ((base >> 8) & 0xFF) * shade, 255);
                int b = (int) Mth.lerp(sheen, (base & 0xFF) * shade, 255);
                colour[i][k] = 0xFF000000 | Mth.clamp(r, 0, 255) << 16 | Mth.clamp(g, 0, 255) << 8 | Mth.clamp(b, 0, 255);
            }
        }
        for (int i = 0; i + 1 < shown; i++) {
            float v0 = i * .5f, v1 = (i + 1) * .5f;
            for (int k = 0; k < RING; k++) {
                float u0 = k * 2f / RING, u1 = (k + 1) * 2f / RING;
                vertex(out, pose, point[i][k], u0, v0, colour[i][k], light, normal[i][k]);
                vertex(out, pose, point[i + 1][k], u0, v1, colour[i + 1][k], light, normal[i + 1][k]);
                vertex(out, pose, point[i + 1][k + 1], u1, v1, colour[i + 1][k + 1], light, normal[i + 1][k + 1]);
                vertex(out, pose, point[i][k + 1], u1, v0, colour[i][k + 1], light, normal[i][k + 1]);
            }
        }
        // The end: a point at the tail, or flat where it is still in its sun or melting away
        int last = shown - 1;
        Vec3 tip = spine.get(last).subtract(tangent[last].scale(cut ? 0 : radius[last] * 2.5));
        Vec3 back = tangent[last].scale(-1);
        for (int k = 0; k < RING; k++) {
            vertex(out, pose, point[last][k], 0, 0, colour[last][k], light, back);
            vertex(out, pose, tip, .5f, 1, colour[last][k], light, back);
            vertex(out, pose, tip, .5f, 1, colour[last][k], light, back);
            vertex(out, pose, point[last][k + 1], 1, 0, colour[last][k + 1], light, back);
        }
    }

    private static void vertex(VertexConsumer out, PoseStack.Pose pose, Vec3 p, float u, float v, int argb, int light, Vec3 n) {
        RenderHelper.vertex(out, pose, (float) p.x, (float) p.y, (float) p.z, u, v, argb, light, (float) n.x, (float) n.y, (float) n.z);
    }

    /** Crimson fins down its back, leaning back, rippling, smaller toward the tail. */
    private void crest(VertexConsumer metal, PoseStack poseStack, Vec3[] tangent, Vec3[] right, Vec3[] up, float[] radius, int shown, float age, int light) {
        for (int i = NECK; i < shown - 2; i += 2) {
            float lean = .55f + .15f * Mth.sin(age * .3f - i * .5f);
            Vec3 dir = up[i].scale(Mth.cos(lean)).subtract(tangent[i].scale(Mth.sin(lean)));
            poseStack.pushPose();
            basis(poseStack, spine.get(i).add(up[i].scale(radius[i] * .8f)), tangent[i], dir);
            poseStack.scale(radius[i] * 1.6f, radius[i] * 1.5f, .3f);
            Solid.draw(metal, poseStack.last(), GoldRender.NEEDLE, i % 4 == 1 ? VERMILION : CRIMSON, 255, age + i * 3, .6f, light);
            poseStack.popPose();
        }
    }

    /** Four legs, a pair behind the neck and a pair at mid-body, each with three ivory claws, paddling the air. */
    private void legs(VertexConsumer metal, PoseStack poseStack, Vec3[] tangent, Vec3[] right, Vec3[] up, float[] radius, int shown, float age, int light) {
        for (int i : new int[]{NECK + 3, NECK + 12}) {
            if (i >= shown) {
                continue;
            }
            for (int side = -1; side <= 1; side += 2) {
                float swing = Mth.sin(age * .25f + i + (side > 0 ? 0 : Mth.PI));
                Vec3 shoulder = spine.get(i).add(right[i].scale(side * radius[i] * .8f)).subtract(up[i].scale(radius[i] * .3f));
                Vec3 dir = up[i].scale(-.8).add(right[i].scale(side * .5)).add(tangent[i].scale(swing * .45)).normalize();
                poseStack.pushPose();
                basis(poseStack, shoulder, tangent[i], dir);
                poseStack.scale(.26f, .85f, .26f);
                Solid.draw(metal, poseStack.last(), GoldRender.CRYSTAL, GOLD, 255, age + i, .7f, light);
                poseStack.popPose();
                Vec3 foot = shoulder.add(dir.scale(.75));
                for (int c = -1; c <= 1; c++) {
                    Vec3 claw = tangent[i].scale(.8).subtract(up[i].scale(.5)).add(right[i].scale(c * .45 + side * .2)).normalize();
                    poseStack.pushPose();
                    basis(poseStack, foot, right[i], claw);
                    poseStack.scale(.12f, .32f, .12f);
                    Solid.draw(metal, poseStack.last(), GoldRender.NEEDLE, IVORY, 255, age + c * 5, .5f, light);
                    poseStack.popPose();
                }
            }
        }
    }

    /** A tuft of crimson fins fanned out at the tip of its tail. */
    private static void tuft(VertexConsumer metal, PoseStack poseStack, Vec3 at, Vec3 tangent, Vec3 right, Vec3 up, float age, int light) {
        for (int k = -3; k <= 3; k++) {
            float spread = k * .32f + Mth.sin(age * .3f + k) * .08f;
            Vec3 dir = tangent.scale(-Mth.cos(spread)).add(up.scale(Mth.sin(spread))).normalize();
            poseStack.pushPose();
            basis(poseStack, at, right, dir);
            poseStack.scale(.9f, 1.1f - Math.abs(k) * .12f, .25f);
            Solid.draw(metal, poseStack.last(), GoldRender.NEEDLE, k % 2 == 0 ? CRIMSON : VERMILION, 255, age + k * 4, .5f, light);
            poseStack.popPose();
        }
    }

    // ---------------------------------------------------------------- the head

    /** The head in its own space (see {@link #SKULL}): skull and snout, jaw, teeth, horns, brows, mane, beard, eyes. */
    private static void head(VertexConsumer metal, PoseStack poseStack, MultiBufferSource buffer, float open, float age, int light) {
        PoseStack.Pose pose = poseStack.last();
        Solid.draw(metal, pose, SKULL, GOLD, 255, age, .8f, light);
        // The jaw hinges under the back of the skull
        poseStack.pushPose();
        poseStack.translate(0, .12f, -.34f);
        poseStack.mulPose(Axis.XP.rotationDegrees(-open));
        Solid.draw(metal, poseStack.last(), JAW, BELLY, 255, age + 7, .6f, light);
        for (int side = -1; side <= 1; side += 2) {
            for (int t = 0; t < 3; t++) {
                poseStack.pushPose();
                poseStack.translate(side * (.24f - t * .05f), .35f + t * .17f, .04f);
                poseStack.mulPose(Axis.XP.rotationDegrees(90));
                poseStack.scale(.05f, .1f, .05f);
                Solid.draw(metal, poseStack.last(), GoldRender.NEEDLE, IVORY, 255, 0, 0, light);
                poseStack.popPose();
            }
        }
        // A crimson beard under the chin
        for (int b = -2; b <= 2; b++) {
            poseStack.pushPose();
            poseStack.translate(b * .06f, .3f, -.06f);
            poseStack.mulPose(Axis.XP.rotationDegrees(-150 + b * 6 + Mth.sin(age * .3f + b) * 8));
            poseStack.scale(.12f, .45f - Math.abs(b) * .06f, .1f);
            Solid.draw(metal, poseStack.last(), GoldRender.NEEDLE, CRIMSON, 255, age + b, .4f, light);
            poseStack.popPose();
        }
        poseStack.popPose();
        // Upper teeth, down from the upper jaw
        for (int side = -1; side <= 1; side += 2) {
            for (int t = 0; t < 3; t++) {
                poseStack.pushPose();
                poseStack.translate(side * (.22f - t * .05f), .55f + t * .17f, -.32f + t * .02f);
                poseStack.mulPose(Axis.XP.rotationDegrees(-90));
                poseStack.scale(.05f, .13f, .05f);
                Solid.draw(metal, poseStack.last(), GoldRender.NEEDLE, IVORY, 255, 0, 0, light);
                poseStack.popPose();
            }
        }
        for (int side = -1; side <= 1; side += 2) {
            // Antlered horns swept back, each with two tines
            Vec3 base = new Vec3(side * .2, .1, .3);
            Vec3 dir = new Vec3(side * .35, -.85, .55).normalize();
            horn(metal, poseStack, base, dir, 1.35f, .14f, age, light);
            horn(metal, poseStack, base.add(dir.scale(.5)), dir.add(0, 0, .9).add(side * .2, 0, 0).normalize(), .4f, .08f, age + 3, light);
            horn(metal, poseStack, base.add(dir.scale(.9)), dir.add(0, .2, 1).normalize(), .32f, .07f, age + 6, light);
            // A heavy brow spiking back over each eye
            horn(metal, poseStack, new Vec3(side * .28, .52, .2), new Vec3(side * .3, -.7, .6).normalize(), .38f, .1f, age, light, GoldRender.DEEP);
        }
        // The mane: crimson locks round the back of the head, flowing back and rippling
        for (int k = 0; k < 14; k++) {
            float a = k * Mth.TWO_PI / 14;
            float c = Mth.cos(a), s = Mth.sin(a);
            if (s < -.55f) {
                continue;
            }
            Vec3 root = new Vec3(c * .38, .02, s * .32);
            Vec3 dir = new Vec3(c * .75, -1, s * .75 + Mth.sin(age * .3f + k) * .2).normalize();
            horn(metal, poseStack, root, dir, .75f + RuneRender.hash(k * 7) * .3f, .2f, age + k, light, k % 2 == 0 ? CRIMSON : VERMILION);
        }
        for (int side = -1; side <= 1; side += 2) {
            poseStack.pushPose();
            poseStack.translate(side * .34f, .5f, .1f);
            GemRender.gem(poseStack, buffer, GemRender.Cut.BRILLIANT, Gem.RUBY.color, .15f, .95f, age + side * 10);
            poseStack.popPose();
        }
    }

    private static void horn(VertexConsumer metal, PoseStack poseStack, Vec3 base, Vec3 dir, float length, float width, float age, int light) {
        horn(metal, poseStack, base, dir, length, width, age, light, IVORY);
    }

    private static void horn(VertexConsumer metal, PoseStack poseStack, Vec3 base, Vec3 dir, float length, float width, float age, int light, int rgb) {
        poseStack.pushPose();
        basis(poseStack, base, new Vec3(1, 0, 0), dir);
        poseStack.scale(width * 2.5f, length, width * 2.5f);
        Solid.draw(metal, poseStack.last(), GoldRender.NEEDLE, rgb, 255, age, .6f, light);
        poseStack.popPose();
    }

    /** A long whisker of light from one side of the snout, flowing back and waving. */
    private static void whisker(PoseStack poseStack, MultiBufferSource buffer, Vec3 skull, Vec3 right, Vec3 forward, Vec3 up, int side, float age) {
        Vec3 previous = local(skull, right, forward, up, side * .2f, 1.0f, -.12f);
        for (int j = 1; j <= 8; j++) {
            float x = side * (.2f + .17f * j);
            float y = 1.0f - .3f * j;
            float z = -.12f + Mth.sin(age * .22f + j * .7f) * .07f * j - .03f * j;
            Vec3 next = local(skull, right, forward, up, x, y, z);
            GemRender.ray(poseStack, buffer, previous, next, GoldRender.PALE, .06f * (1.2f - j / 10f), .9f, age + j);
            previous = next;
        }
    }

    /** A point of the head's space, relative to the chest. */
    private static Vec3 local(Vec3 origin, Vec3 right, Vec3 forward, Vec3 up, float x, float y, float z) {
        return origin.add(right.scale(x)).add(forward.scale(y)).add(up.scale(z));
    }

    /**
     * Moves the pose to {@code at} with its Y axis along {@code yAxis} and its X axis as near {@code xAxis} as can be
     * (Z completing them), so the shapes drawn after are laid along those directions.
     */
    private static void basis(PoseStack poseStack, Vec3 at, Vec3 xAxis, Vec3 yAxis) {
        Vec3 y = yAxis.normalize();
        Vec3 z = xAxis.cross(y);
        z = z.lengthSqr() < 1.0E-6 ? safeRight(y, null).cross(y) : z.normalize();
        Vec3 x = y.cross(z);
        Matrix4f m = new Matrix4f(
                (float) x.x, (float) x.y, (float) x.z, 0,
                (float) y.x, (float) y.y, (float) y.z, 0,
                (float) z.x, (float) z.y, (float) z.z, 0,
                (float) at.x, (float) at.y, (float) at.z, 1);
        PoseStack.Pose pose = poseStack.last();
        pose.pose().mul(m);
        pose.normal().mul(new Matrix3f(m));
    }

    private static Vec3 safeRight(Vec3 dir, Vec3 previous) {
        Vec3 r = dir.cross(UP);
        if (r.lengthSqr() < 1.0E-4) {
            return previous != null ? previous : new Vec3(1, 0, 0);
        }
        return r.normalize();
    }

    @Override
    public ResourceLocation getTextureLocation(GoldenDragon dragon) {
        return SCALES;
    }
}
