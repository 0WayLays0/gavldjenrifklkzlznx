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
import org.joml.Vector3f;

/**
 * The Sylvan Guardian: two legs of dark oak, each splitting into roots at its foot; a body of oak logs leaning a little
 * forwards, a smaller log for its head with two green eyes glowing in it and a beard of vines; long arms of twisted bark
 * ending in twiggy fingers; a crown of azalea, flowering here and there, on its head and shoulders. It walks heavily,
 * brings both arms down on its foe, and plunges its hands into the ground; it pulls itself out of the earth as it comes.
 */
public class WoodGuardianRenderer extends EntityRenderer<WoodGuardian> {
    private static final float HIP = 1.45f;
    private static final float SHOULDER_Y = 2.75f;
    private static final float SHOULDER_X = .78f;

    public WoodGuardianRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 1;
    }

    @Override
    public void render(WoodGuardian guardian, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int light) {
        float age = guardian.tickCount + partialTicks;
        float formed = Mth.clamp(age / WoodGuardian.FORM_TICKS, 0, 1);
        formed = 1 - (1 - formed) * (1 - formed);
        float limb = guardian.walkAnimation.position(partialTicks);
        float walk = Math.min(1, guardian.walkAnimation.speed(partialTicks));
        float swing = Mth.sin(limb * .55f) * .45f * walk;
        float breathe = .03f * Mth.sin(age * .08f);
        int seed = guardian.getId() * 53;
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(-Mth.rotLerp(partialTicks, guardian.yBodyRotO, guardian.yBodyRot)));
        // Pulling itself out of the ground
        poseStack.translate(0, -3.6f * (1 - formed), 0);
        float hurt = guardian.hurtTime > 0 ? guardian.hurtTime / 10f : 0;
        poseStack.mulPose(Axis.XP.rotation(-.12f * hurt));
        // The arms: hanging and swinging, both coming down on a foe, or plunged into the ground
        Vector3f[] hands = new Vector3f[2];
        float slam = age - guardian.getSlamTick();
        float roots = age - guardian.getRootsTick();
        for (int i = 0; i < 2; i++) {
            int side = i == 0 ? -1 : 1;
            Vector3f rest = new Vector3f(side * 1.0f, 1.05f + breathe, .15f + side * swing * .9f);
            Vector3f hand = rest;
            if (slam >= 0 && slam < WoodGuardian.SLAM_TICKS + 6) {
                Vector3f up = new Vector3f(side * .55f, 4.4f, .5f);
                Vector3f down = new Vector3f(side * .45f, .5f, 1.9f);
                if (slam < 4) {
                    hand = lerp(rest, up, smooth(slam / 4));
                } else if (slam < 7) {
                    hand = lerp(up, down, (slam - 4) / 3 * ((slam - 4) / 3));
                } else if (slam < WoodGuardian.SLAM_TICKS) {
                    hand = down;
                } else {
                    hand = lerp(down, rest, smooth((slam - WoodGuardian.SLAM_TICKS) / 6));
                }
            } else if (roots >= 0 && roots < 20) {
                Vector3f up = new Vector3f(side * .7f, 3.6f, .9f);
                Vector3f in = new Vector3f(side * .5f, -.3f, 1.5f);
                if (roots < 6) {
                    hand = lerp(rest, up, smooth(roots / 6));
                } else if (roots < 9) {
                    hand = lerp(up, in, (roots - 6) / 3);
                } else if (roots < 14) {
                    hand = in;
                } else {
                    hand = lerp(in, rest, smooth((roots - 14) / 6));
                }
            }
            hands[i] = hand;
        }
        float lean = .1f + (slam >= 4 && slam < WoodGuardian.SLAM_TICKS ? .25f : 0) + (roots >= 6 && roots < 14 ? .35f : 0);
        // The legs (logs), each swinging from its hip
        for (int i = 0; i < 2; i++) {
            int side = i == 0 ? -1 : 1;
            poseStack.pushPose();
            poseStack.translate(side * .42f, HIP, 0);
            poseStack.mulPose(Axis.XP.rotation(side * swing));
            WoodRender.log(WoodRender.DARK_LOG, poseStack, buffer, light, 0, -HIP, 0, .56f, HIP + .1f);
            poseStack.popPose();
        }
        // The body and the head, leaning forwards from the hips
        poseStack.pushPose();
        poseStack.translate(0, HIP, 0);
        poseStack.mulPose(Axis.XP.rotation(lean));
        poseStack.translate(0, breathe, 0);
        WoodRender.log(WoodRender.LOG, poseStack, buffer, light, 0, -.1f, 0, 1.15f, .9f);
        WoodRender.log(WoodRender.LOG, poseStack, buffer, light, 0, .78f, .03f, 1.3f, .62f);
        WoodRender.log(WoodRender.LOG, poseStack, buffer, light, 0, 1.38f, .12f, .8f, .78f);
        // The crown: azalea heaped on the head and the shoulders
        RuneRender.block(WoodRender.leaves(seed), poseStack, buffer, light, 0, 2.05f, .05f, 1.25f, .75f, 1.15f, -8, 0);
        RuneRender.block(WoodRender.leaves(seed + 1), poseStack, buffer, light, -.75f, 1.32f, 0, .78f, .7f, .8f, 0, 12);
        RuneRender.block(WoodRender.leaves(seed + 2), poseStack, buffer, light, .75f, 1.32f, 0, .78f, .7f, .8f, 0, -12);
        RuneRender.block(WoodRender.leaves(seed + 3), poseStack, buffer, light, -.25f, 2.5f, -.1f, .65f, .55f, .65f, 10, 6);
        RuneRender.block(WoodRender.leaves(seed + 4), poseStack, buffer, light, .3f, 2.45f, .15f, .55f, .5f, .55f, -6, -10);
        poseStack.popPose();
        // Bark: feet, arms and fingers. The arms are worked out in the body's frame, from the leaning shoulders
        VertexConsumer bark = WoodRender.bark(buffer);
        PoseStack.Pose p = poseStack.last();
        for (int i = 0; i < 2; i++) {
            int side = i == 0 ? -1 : 1;
            float legSwing = side * swing;
            // Roots splaying from each foot (the foot swings with its leg)
            Vector3f foot = new Vector3f(side * .42f, HIP - HIP * Mth.cos(legSwing), -HIP * Mth.sin(legSwing));
            for (int k = 0; k < 4; k++) {
                float a = (k - 1.5f) * .6f + (side < 0 ? .3f : -.3f);
                Vector3f[] toe = {
                        new Vector3f(foot).add(0, .3f, 0),
                        new Vector3f(foot).add(Mth.sin(a) * .35f, .08f, Mth.cos(a) * .35f),
                        new Vector3f(foot).add(Mth.sin(a) * .7f, -.05f, Mth.cos(a) * .7f)};
                BoneRender.curve(bark, p, toe, .13f, .04f, 5, light, BoneRender.WHITE);
            }
            Vector3f shoulder = new Vector3f(side * SHOULDER_X, SHOULDER_Y - HIP, 0).rotateX(lean).add(0, HIP + breathe, 0);
            Vector3f hand = hands[i];
            Vector3f elbow = new Vector3f(shoulder).lerp(hand, .5f).add(side * .35f, .1f, -.2f);
            Vector3f[] arm = new Vector3f[7];
            for (int k = 0; k < arm.length; k++) {
                float u = k / (float) (arm.length - 1);
                Vector3f a = new Vector3f(shoulder).lerp(elbow, Math.min(1, u * 2));
                Vector3f b = new Vector3f(elbow).lerp(hand, Math.max(0, u * 2 - 1));
                arm[k] = u < .5f ? a : b;
                // Twisted and knotted
                arm[k].add(.05f * Mth.sin(u * 9 + side), .04f * Mth.cos(u * 7), 0);
            }
            BoneRender.ball(bark, p, shoulder, .3f, light, BoneRender.WHITE);
            BoneRender.curve(bark, p, arm, .26f, .15f, 7, light, BoneRender.WHITE);
            BoneRender.ball(bark, p, elbow, .19f, light, BoneRender.WHITE);
            // Twiggy fingers
            Vector3f along = new Vector3f(hand).sub(elbow).normalize();
            Vector3f across = new Vector3f(along).cross(new Vector3f(0, 1, 0));
            if (across.lengthSquared() < 1.0E-4f) {
                across.set(1, 0, 0);
            }
            across.normalize();
            for (int k = 0; k < 4; k++) {
                float spread = (k - 1.5f) * .17f;
                Vector3f base = new Vector3f(hand).add(new Vector3f(across).mul(spread * .6f));
                Vector3f mid = new Vector3f(base).add(new Vector3f(along).mul(.3f)).add(new Vector3f(across).mul(spread));
                Vector3f tip = new Vector3f(mid).add(new Vector3f(along).mul(.28f)).add(new Vector3f(across).mul(spread * .8f)).add(0, -.08f, 0);
                BoneRender.curve(bark, p, new Vector3f[]{base, mid, tip}, .07f, .025f, 4, light, BoneRender.WHITE);
            }
        }
        // A beard of vines hanging from its chin
        VertexConsumer vine = WoodRender.vine(buffer);
        Vector3f chin = new Vector3f(0, 1.4f, .52f).rotateX(lean).add(0, HIP + breathe, 0);
        for (int k = 0; k < 5; k++) {
            float x = (k - 2) * .12f;
            float len = .5f + .35f * WoodFx.hash(seed + k);
            Vector3f[] strand = new Vector3f[4];
            for (int i = 0; i < 4; i++) {
                float u = i / 3f;
                strand[i] = new Vector3f(chin).add(x * (1 - .3f * u), -len * u, .08f * u + .03f * Mth.sin(age * .1f + k + u * 3));
            }
            BoneRender.curve(vine, p, strand, .045f, .02f, 4, light, BoneRender.WHITE);
        }
        // Its eyes, green and glowing, in the head's front face
        VertexConsumer glow = buffer.getBuffer(RenderType.eyes(GlyphRender.GLOW));
        float blink = (age % 90) < 3 ? .2f : 1;
        for (int side = -1; side <= 1; side += 2) {
            Vector3f eye = new Vector3f(side * .2f, 1.85f, .53f).rotateX(lean).add(0, HIP + breathe, 0);
            poseStack.pushPose();
            poseStack.translate(eye.x, eye.y, eye.z);
            RenderHelper.billboard(glow, poseStack.last(), .17f, RuneRender.additive(WoodFx.LIFE, formed * blink), GlyphRender.FULL);
            RenderHelper.billboard(glow, poseStack.last(), .07f, RuneRender.additive(WoodFx.LIFE_CORE, formed * blink), GlyphRender.FULL);
            poseStack.popPose();
        }
        poseStack.popPose();
        super.render(guardian, entityYaw, partialTicks, poseStack, buffer, light);
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
