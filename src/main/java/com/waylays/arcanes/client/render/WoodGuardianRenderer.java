package com.waylays.arcanes.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.waylays.arcanes.entity.wood.WoodGuardian;
import com.waylays.arcanes.spells.wood.WoodFx;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/**
 * The Sylvan Guardian, every part of it braided living wood: legs of three twisting strands bending at the knee, each
 * foot a splay of roots gripping the ground; a body braided of five strands, a green heart glowing between them; long
 * arms of three strands with knotted elbows and hands of long twig fingers; a head of gnarled wood under a heavy
 * brow, two green eyes deep under it, a crack of a mouth, a beard of hanging moss, and two branches growing from it in
 * leaf and flower, more leaves on its shoulders. It walks with heavy, bent-kneed steps, turns its head to look,
 * brings both arms down on its foe, plunges its hands into the ground, and pulls itself out of the earth as it comes.
 */
public class WoodGuardianRenderer extends EntityRenderer<WoodGuardian> {
    private static final float HIP = 1.45f;
    private static final float THIGH = .82f;
    private static final float SHIN = .82f;
    private static final float SHOULDER_Y = 2.72f;
    private static final float SHOULDER_X = .74f;
    private static final float UPPER_ARM = .95f;
    private static final float FOREARM = .95f;
    private static final Vector3f NECK = new Vector3f(0, 2.85f, .05f);

    private Quaternionf unturn = new Quaternionf();

    public WoodGuardianRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 1;
    }

    /** Points of the upper body lean forwards about the hips; the head turns about the neck. */
    private record Frame(float lean, float headYaw, float breathe) {
        Vector3f upper(Vector3f p) {
            return new Vector3f(p).sub(0, HIP, 0).rotateX(lean).add(0, HIP + breathe, 0);
        }

        Vector3f head(Vector3f p) {
            return upper(new Vector3f(p).sub(NECK).rotateY(headYaw).add(NECK));
        }
    }

    @Override
    public void render(WoodGuardian guardian, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int light) {
        float age = guardian.tickCount + partialTicks;
        float formed = Mth.clamp(age / WoodGuardian.FORM_TICKS, 0, 1);
        formed = 1 - (1 - formed) * (1 - formed);
        float limb = guardian.walkAnimation.position(partialTicks);
        float walk = Math.min(1, guardian.walkAnimation.speed(partialTicks) * 1.4f);
        int seed = guardian.getId() * 53;
        float bodyYaw = Mth.rotLerp(partialTicks, guardian.yBodyRotO, guardian.yBodyRot);
        float headYaw = Mth.clamp(Mth.wrapDegrees(Mth.rotLerp(partialTicks, guardian.yHeadRotO, guardian.yHeadRot) - bodyYaw), -50, 50);
        float slam = age - guardian.getSlamTick();
        float roots = age - guardian.getRootsTick();
        float lean = .12f + .05f * walk + (slam >= 4 && slam < WoodGuardian.SLAM_TICKS ? .3f : 0) + (roots >= 6 && roots < 14 ? .4f : 0);
        float bob = walk * .07f * Math.abs(Mth.sin(limb * .55f));
        Frame f = new Frame(lean, -headYaw * Mth.DEG_TO_RAD, .03f * Mth.sin(age * .08f) - bob);

        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(-bodyYaw));
        // Pulling itself out of the ground
        poseStack.translate(0, -3.6f * (1 - formed), 0);
        float hurt = guardian.hurtTime > 0 ? guardian.hurtTime / 10f : 0;
        poseStack.mulPose(Axis.XP.rotation(-.1f * hurt));
        // Undoes the body's turn and stagger, so glows face the camera
        unturn = new Quaternionf(Axis.XP.rotation(.1f * hurt)).mul(Axis.YP.rotationDegrees(bodyYaw));
        VertexConsumer bark = WoodRender.bark(buffer);
        PoseStack.Pose p = poseStack.last();
        List<Vector3f[]> leafSpots = new ArrayList<>();

        // Legs: hip, knee (bending forwards), ankle; feet of roots
        for (int i = 0; i < 2; i++) {
            int side = i == 0 ? -1 : 1;
            float phase = limb * .55f + (i == 0 ? 0 : Mth.PI);
            float stride = .5f * walk;
            Vector3f hip = new Vector3f(side * .4f, HIP - bob, 0);
            Vector3f foot = new Vector3f(side * .46f, .16f + Math.max(0, Mth.cos(phase)) * .38f * walk, stride * Mth.sin(phase) + .05f);
            Vector3f knee = joint(hip, foot, THIGH, SHIN, new Vector3f(side * .15f, 0, 1));
            braid(bark, p, hip, knee, 3, .2f, .16f, .7f, light, seed + i * 100);
            BoneRender.ball(bark, p, knee, .19f, light, BoneRender.WHITE);
            braid(bark, p, knee, foot, 3, .16f, .12f, -.6f, light, seed + i * 100 + 30);
            BoneRender.ball(bark, p, foot, .16f, light, BoneRender.WHITE);
            for (int k = 0; k < 5; k++) {
                // Four roots splaying forwards and out, one behind for a heel
                float a = k < 4 ? (k - 1.5f) * .45f + side * .15f : Mth.PI + side * .2f;
                float len = k < 4 ? .62f + .12f * WoodFx.hash(seed + i * 9 + k) : .4f;
                Vector3f dir = new Vector3f(Mth.sin(a), 0, Mth.cos(a));
                float grip = foot.y - .16f;
                Vector3f[] toe = {
                        new Vector3f(foot),
                        new Vector3f(foot).add(new Vector3f(dir).mul(len * .45f)).add(0, -foot.y * .7f + .04f, 0),
                        new Vector3f(foot).add(new Vector3f(dir).mul(len)).add(0, -foot.y + .02f - grip * .3f, 0),
                        new Vector3f(foot).add(new Vector3f(dir).mul(len * 1.2f)).add(0, -foot.y - .05f, 0)};
                BoneRender.curve(bark, p, toe, .1f, .025f, 5, light, BoneRender.WHITE);
            }
        }
        // The pelvis: a knot of roots
        BoneRender.blob(bark, p, f.upper(new Vector3f(0, HIP + .05f, 0)), new Vector3f(.48f, 0, 0), new Vector3f(0, .26f, 0), new Vector3f(0, 0, .34f), 9, 5, light, BoneRender.WHITE);

        // The body: five strands braided up from the hips, broad at the chest
        int strands = 5;
        for (int s = 0; s < strands; s++) {
            int n = 9;
            Vector3f[] pts = new Vector3f[n];
            for (int k = 0; k < n; k++) {
                float u = (float) k / (n - 1);
                float y = HIP + u * (SHOULDER_Y + .08f - HIP);
                float r = .3f + .26f * Mth.sin(u * Mth.PI * .8f) + .04f * Mth.sin(u * 9 + s);
                float a = s * Mth.TWO_PI / strands + u * 2.2f;
                pts[k] = f.upper(new Vector3f(Mth.cos(a) * r * 1.15f, y, Mth.sin(a) * r * .85f));
            }
            BoneRender.curve(bark, p, pts, .2f, .17f, 7, light, BoneRender.WHITE);
        }
        // Shoulders, and a collar of wood the head grows out of
        Vector3f[] shoulders = new Vector3f[2];
        for (int i = 0; i < 2; i++) {
            int side = i == 0 ? -1 : 1;
            shoulders[i] = f.upper(new Vector3f(side * SHOULDER_X, SHOULDER_Y, 0));
            BoneRender.blob(bark, p, shoulders[i], new Vector3f(.3f, 0, 0), new Vector3f(0, .26f, 0), new Vector3f(0, 0, .28f), 8, 5, light, BoneRender.WHITE);
            leafSpots.add(new Vector3f[]{f.upper(new Vector3f(side * (SHOULDER_X + .05f), SHOULDER_Y + .28f, -.05f)), new Vector3f(.32f, 10, 0)});
        }
        BoneRender.blob(bark, p, f.upper(new Vector3f(0, SHOULDER_Y + .05f, 0)), new Vector3f(.55f, 0, 0), new Vector3f(0, .2f, 0), new Vector3f(0, 0, .36f), 9, 5, light, BoneRender.WHITE);

        // Arms: hanging and swinging, both coming down on a foe, or plunged into the ground
        for (int i = 0; i < 2; i++) {
            int side = i == 0 ? -1 : 1;
            float swing = Mth.sin(limb * .55f + (i == 0 ? Mth.PI : 0)) * .45f * walk;
            Vector3f rest = new Vector3f(side * 1.02f, 1.0f + f.breathe(), .3f + swing);
            Vector3f hand = rest;
            float curl = .35f;
            if (slam >= 0 && slam < WoodGuardian.SLAM_TICKS + 6) {
                Vector3f up = new Vector3f(side * .5f, 4.5f, .4f);
                Vector3f down = new Vector3f(side * .42f, .45f, 2.0f);
                if (slam < 4) {
                    hand = lerp(rest, up, smooth(slam / 4));
                } else if (slam < 7) {
                    float k = (slam - 4) / 3;
                    hand = lerp(up, down, k * k);
                } else if (slam < WoodGuardian.SLAM_TICKS) {
                    hand = down;
                } else {
                    hand = lerp(down, rest, smooth((slam - WoodGuardian.SLAM_TICKS) / 6));
                }
                curl = .9f;
            } else if (roots >= 0 && roots < 20) {
                Vector3f up = new Vector3f(side * .7f, 3.8f, .9f);
                Vector3f in = new Vector3f(side * .5f, -.25f, 1.6f);
                if (roots < 6) {
                    hand = lerp(rest, up, smooth(roots / 6));
                } else if (roots < 9) {
                    hand = lerp(up, in, (roots - 6) / 3);
                } else if (roots < 14) {
                    hand = in;
                } else {
                    hand = lerp(in, rest, smooth((roots - 14) / 6));
                }
                curl = .1f;
            }
            Vector3f shoulder = shoulders[i];
            Vector3f elbow = joint(shoulder, hand, UPPER_ARM, FOREARM, new Vector3f(side * .7f, -.1f, -1));
            Vector3f wrist = new Vector3f(hand);
            braid(bark, p, shoulder, elbow, 3, .19f, .15f, .8f, light, seed + i * 200);
            BoneRender.ball(bark, p, elbow, .17f, light, BoneRender.WHITE);
            braid(bark, p, elbow, wrist, 3, .15f, .11f, -.7f, light, seed + i * 200 + 40);
            hand(bark, p, elbow, wrist, side, curl, light, seed + i * 7);
        }

        // The head: gnarled wood, a heavy brow, the nose a knot, the jaw
        Vector3f skull = f.head(new Vector3f(0, 3.12f, .18f));
        BoneRender.blob(bark, p, skull, new Vector3f(.42f, 0, 0), new Vector3f(0, .44f, 0), new Vector3f(0, 0, .4f), 10, 7, light, BoneRender.WHITE);
        BoneRender.blob(bark, p, f.head(new Vector3f(0, 2.86f, .3f)), new Vector3f(.32f, 0, 0), new Vector3f(0, .2f, 0), new Vector3f(0, 0, .3f), 8, 5, light, BoneRender.WHITE);
        Vector3f[] brow = new Vector3f[7];
        for (int k = 0; k < brow.length; k++) {
            float u = (float) k / (brow.length - 1) * 2 - 1;
            brow[k] = f.head(new Vector3f(u * .38f, 3.27f - .05f * u * u, .5f - .12f * u * u));
        }
        BoneRender.curve(bark, p, brow, .1f, .1f, 6, light, BoneRender.WHITE);
        BoneRender.spike(bark, p, f.head(new Vector3f(0, 3.12f, .52f)), f.head(new Vector3f(.02f, 2.98f, .72f)), .08f, new Vector3f(), light, BoneRender.WHITE);
        // Deep sockets and the crack of the mouth (darkened wood)
        for (int side = -1; side <= 1; side += 2) {
            BoneRender.blob(bark, p, f.head(new Vector3f(side * .16f, 3.15f, .5f)), new Vector3f(.1f, 0, 0), new Vector3f(0, .065f, 0), new Vector3f(0, 0, .05f), 6, 4, light, 0xFF1E140C);
        }
        BoneRender.blob(bark, p, f.head(new Vector3f(0, 2.92f, .55f)), new Vector3f(.17f, 0, 0), new Vector3f(0, .035f, 0), new Vector3f(0, 0, .04f), 6, 3, light, 0xFF1E140C);
        // Two branches growing from the head, in leaf and flower
        for (int side = -1; side <= 1; side += 2) {
            Vector3f[] branch = {
                    f.head(new Vector3f(side * .22f, 3.42f, .05f)),
                    f.head(new Vector3f(side * .42f, 3.85f, -.08f)),
                    f.head(new Vector3f(side * .5f, 4.25f, -.25f)),
                    f.head(new Vector3f(side * .72f, 4.55f, -.3f))};
            BoneRender.curve(bark, p, branch, .1f, .03f, 6, light, BoneRender.WHITE);
            BoneRender.tube(bark, p, branch[1], f.head(new Vector3f(side * .8f, 4.05f, .05f)), .05f, .015f, 5, light, BoneRender.WHITE);
            leafSpots.add(new Vector3f[]{branch[3], new Vector3f(.42f, 16, 1)});
            leafSpots.add(new Vector3f[]{f.head(new Vector3f(side * .8f, 4.05f, .05f)), new Vector3f(.26f, 8, 1)});
        }
        leafSpots.add(new Vector3f[]{f.head(new Vector3f(0, 3.55f, -.1f)), new Vector3f(.38f, 14, 1)});
        leafSpots.add(new Vector3f[]{f.upper(new Vector3f(.3f, 1.9f, .45f)), new Vector3f(.18f, 5, 0)});
        leafSpots.add(new Vector3f[]{f.upper(new Vector3f(-.25f, 2.35f, -.45f)), new Vector3f(.2f, 6, 0)});

        // A beard of hanging moss
        VertexConsumer vine = WoodRender.vine(buffer);
        for (int k = 0; k < 7; k++) {
            float x = (k - 3) * .08f;
            float len = .45f + .35f * WoodFx.hash(seed + k);
            Vector3f[] strand = new Vector3f[4];
            for (int i = 0; i < 4; i++) {
                float u = i / 3f;
                Vector3f top = f.head(new Vector3f(x, 2.8f, .48f - Math.abs(x) * .4f));
                strand[i] = new Vector3f(top).add(x * .3f * u, -len * u, .06f * u + .03f * Mth.sin(age * .1f + k + u * 3));
            }
            BoneRender.curve(vine, p, strand, .035f, .015f, 4, light, BoneRender.WHITE);
        }

        // Leaves
        VertexConsumer leaf = WoodRender.leafBuffer(buffer);
        for (Vector3f[] spot : leafSpots) {
            cluster(poseStack, leaf, spot[0], spot[1].x, (int) spot[1].y, spot[1].z > .5f, seed + (int) (spot[0].x * 97 + spot[0].y * 31), age, light);
        }

        // The glow: its eyes, its mouth, its heart between the strands
        VertexConsumer glow = buffer.getBuffer(RenderType.eyes(GlyphRender.GLOW));
        float blink = (age % 90) < 3 ? .15f : 1;
        float beat = .75f + .25f * Mth.sin(age * .15f);
        for (int side = -1; side <= 1; side += 2) {
            billboard(poseStack, glow, f.head(new Vector3f(side * .16f, 3.15f, .55f)), .14f, RuneRender.additive(WoodFx.LIFE, formed * blink));
            billboard(poseStack, glow, f.head(new Vector3f(side * .16f, 3.15f, .56f)), .06f, RuneRender.additive(WoodFx.LIFE_CORE, formed * blink));
        }
        billboard(poseStack, glow, f.head(new Vector3f(0, 2.92f, .58f)), .12f, RuneRender.additive(WoodFx.LIFE, .45f * formed));
        billboard(poseStack, glow, f.upper(new Vector3f(0, 2.15f, .05f)), .55f, RuneRender.additive(WoodFx.LIFE, .7f * beat * formed));
        billboard(poseStack, glow, f.upper(new Vector3f(0, 2.15f, .05f)), .22f, RuneRender.additive(WoodFx.LIFE_CORE, .9f * beat * formed));
        if (formed < 1) {
            poseStack.pushPose();
            poseStack.translate(0, .05f + 3.6f * (1 - formed), 0);
            RenderHelper.groundQuad(glow, poseStack.last(), 2.2f, RuneRender.additive(WoodFx.LIFE, (1 - formed) * .8f), GlyphRender.FULL);
            poseStack.popPose();
        }
        poseStack.popPose();
        super.render(guardian, entityYaw, partialTicks, poseStack, buffer, light);
    }

    /** The middle joint of a limb from {@code a} to {@code b} with bones {@code l1} and {@code l2}, bending towards {@code pole}. */
    private static Vector3f joint(Vector3f a, Vector3f b, float l1, float l2, Vector3f pole) {
        Vector3f d = new Vector3f(b).sub(a);
        float dist = Mth.clamp(d.length(), .2f, l1 + l2 - .02f);
        d.normalize();
        float x = (l1 * l1 - l2 * l2 + dist * dist) / (2 * dist);
        float h = Mth.sqrt(Math.max(0, l1 * l1 - x * x));
        Vector3f bend = new Vector3f(pole).sub(new Vector3f(d).mul(pole.dot(d)));
        if (bend.lengthSquared() < 1.0E-4f) {
            bend.set(0, 0, 1);
        }
        bend.normalize();
        return new Vector3f(a).add(new Vector3f(d).mul(x)).add(bend.mul(h));
    }

    /** {@code strands} strands of wood twisting round each other from {@code a} to {@code b}, bulging in the middle. */
    private static void braid(VertexConsumer c, PoseStack.Pose p, Vector3f a, Vector3f b, int strands, float r0, float r1, float twist, int light, int seed) {
        Vector3f d = new Vector3f(b).sub(a);
        float len = d.length();
        if (len < 1.0E-3f) {
            return;
        }
        d.div(len);
        Vector3f e1 = new Vector3f(d).cross(Math.abs(d.y) > .9f ? new Vector3f(1, 0, 0) : new Vector3f(0, 1, 0)).normalize();
        Vector3f e2 = new Vector3f(d).cross(e1).normalize();
        int n = 6;
        for (int s = 0; s < strands; s++) {
            Vector3f[] pts = new Vector3f[n];
            for (int k = 0; k < n; k++) {
                float u = (float) k / (n - 1);
                float r = Mth.lerp(u, r0, r1) * (.55f + .2f * Mth.sin(u * Mth.PI));
                float ang = s * Mth.TWO_PI / strands + u * twist * Mth.TWO_PI + seed;
                pts[k] = new Vector3f(a).add(new Vector3f(d).mul(u * len))
                        .add(new Vector3f(e1).mul(Mth.cos(ang) * r)).add(new Vector3f(e2).mul(Mth.sin(ang) * r));
            }
            BoneRender.curve(c, p, pts, r0 * .62f, r1 * .62f, 6, light, BoneRender.WHITE);
        }
    }

    /** A hand: a knotted palm, four long twig fingers and a thumb, curled by {@code curl}. */
    private static void hand(VertexConsumer c, PoseStack.Pose p, Vector3f elbow, Vector3f wrist, int side, float curl, int light, int seed) {
        Vector3f along = new Vector3f(wrist).sub(elbow).normalize();
        Vector3f across = new Vector3f(along).cross(new Vector3f(0, 1, 0));
        if (across.lengthSquared() < 1.0E-4f) {
            across.set(1, 0, 0);
        }
        across.normalize();
        Vector3f palmUp = new Vector3f(across).cross(along).normalize();
        Vector3f palm = new Vector3f(wrist).add(new Vector3f(along).mul(.14f));
        BoneRender.blob(c, p, palm, new Vector3f(across).mul(.17f), new Vector3f(along).mul(.16f), new Vector3f(palmUp).mul(.09f), 7, 4, light, BoneRender.WHITE);
        for (int k = 0; k < 5; k++) {
            boolean thumb = k == 4;
            float spread = thumb ? side * .2f : (k - 1.5f) * .09f;
            Vector3f base = new Vector3f(palm).add(new Vector3f(across).mul(thumb ? side * .16f : spread)).add(new Vector3f(along).mul(thumb ? .02f : .12f));
            Vector3f dir = new Vector3f(along).add(new Vector3f(across).mul(thumb ? side * .8f : spread * 2.5f)).normalize();
            float l1 = thumb ? .22f : .3f + .06f * WoodFx.hash(seed + k);
            float l2 = thumb ? .16f : .26f + .05f * WoodFx.hash(seed + k + 5);
            Vector3f mid = new Vector3f(base).add(new Vector3f(dir).mul(l1)).add(new Vector3f(palmUp).mul(-curl * .08f));
            Vector3f bent = new Vector3f(dir).mul(1 - curl).add(new Vector3f(palmUp).mul(-curl * 1.1f)).normalize();
            Vector3f tip = new Vector3f(mid).add(new Vector3f(bent).mul(l2));
            BoneRender.curve(c, p, new Vector3f[]{base, mid, tip}, .055f, .015f, 5, light, BoneRender.WHITE);
        }
    }

    /** A clump of leaves round {@code centre}, swaying, flowering if {@code flowers}. */
    private static void cluster(PoseStack poseStack, VertexConsumer leaf, Vector3f centre, float radius, int count, boolean flowers, int seed, float age, int light) {
        for (int k = 0; k < count; k++) {
            float a = WoodFx.hash(seed + k * 3) * Mth.TWO_PI;
            float up = WoodFx.hash(seed + k * 3 + 1) * 2 - 1;
            float rr = Mth.sqrt(1 - up * up) * radius * (.4f + .6f * WoodFx.hash(seed + k * 3 + 2));
            poseStack.pushPose();
            poseStack.translate(centre.x + Mth.cos(a) * rr, centre.y + up * radius * .7f, centre.z + Mth.sin(a) * rr);
            poseStack.mulPose(Axis.YP.rotation(a + .15f * Mth.sin(age * .07f + k)));
            poseStack.mulPose(Axis.XP.rotation(-.9f + .5f * WoodFx.hash(seed + k * 5) + .08f * Mth.sin(age * .1f + k)));
            int colour = flowers && WoodFx.hash(seed + k * 7) < .22f ? 0xFFF2A6D8 : WoodRender.leafColour(seed + k);
            WoodRender.leaf(leaf, poseStack.last(), .22f + .1f * WoodFx.hash(seed + k * 11), colour, light);
            poseStack.popPose();
        }
    }

    private void billboard(PoseStack poseStack, VertexConsumer glow, Vector3f at, float half, int argb) {
        poseStack.pushPose();
        poseStack.translate(at.x, at.y, at.z);
        poseStack.mulPose(unturn);
        poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
        RenderHelper.billboard(glow, poseStack.last(), half, argb, GlyphRender.FULL);
        poseStack.popPose();
    }

    private static float smooth(float t) {
        t = Mth.clamp(t, 0, 1);
        return t * t * (3 - 2 * t);
    }

    private static Vector3f lerp(Vector3f a, Vector3f b, float t) {
        return new Vector3f(a).lerp(b, t);
    }

    @Override
    public ResourceLocation getTextureLocation(WoodGuardian guardian) {
        return WoodRender.BARK;
    }
}
