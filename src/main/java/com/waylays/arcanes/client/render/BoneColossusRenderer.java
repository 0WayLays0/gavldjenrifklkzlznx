package com.waylays.arcanes.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.waylays.arcanes.entity.bone.BoneColossus;
import com.waylays.arcanes.spells.bone.BoneFx;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/**
 * The Gashadokuro: a giant skeleton waist-deep in the ground: a curved spine, a great ribcage and its breastbone,
 * collarbones and shoulder blades, a huge skull with burning eyes, and two long arms (each worked out from the shoulder
 * to where the hand must be, the elbow bending outwards) ending in clawed hands that rest on the ground, or rise high
 * and smash down as a fist.
 */
public class BoneColossusRenderer extends EntityRenderer<BoneColossus> {
    public BoneColossusRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(BoneColossus colossus, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        float age = colossus.tickCount + partialTicks;
        float risen = colossus.risen(age);
        if (risen <= 0) {
            return;
        }
        poseStack.pushPose();
        poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(-colossus.getYRot()));
        poseStack.pushPose();
        poseStack.translate(0, -14 * (1 - risen), 0);
        VertexConsumer bone = BoneRender.buffer(buffer);
        PoseStack.Pose p = poseStack.last();
        int light = packedLight;
        // The spine, curving forward at the top
        Vector3f[] spine = new Vector3f[11];
        for (int k = 0; k < spine.length; k++) {
            float u = k / (float) (spine.length - 1);
            spine[k] = new Vector3f(0, -1.5f + 8.6f * u, -.6f + .7f * Mth.sin(u * Mth.PI * .8f));
        }
        for (int k = 0; k < spine.length; k++) {
            Vector3f along = new Vector3f(spine[Math.min(k + 1, spine.length - 1)]).sub(spine[Math.max(k - 1, 0)]).normalize();
            BoneRender.vertebra(bone, p, spine[k], along, new Vector3f(0, 0, -1), 1.15f - .25f * k / spine.length, light, BoneRender.WHITE);
        }
        // The ribcage: ribs from the spine round to the breastbone
        for (int k = 0; k < 6; k++) {
            float y = 3.2f + k * .62f;
            float r = 2.1f + .35f * Mth.sin((k + .5f) / 6 * Mth.PI);
            for (int side = -1; side <= 1; side += 2) {
                Vector3f[] rib = new Vector3f[9];
                for (int i = 0; i < 9; i++) {
                    float u = i / 8f;
                    float a = u * Mth.PI * .92f;
                    rib[i] = new Vector3f(side * (.45f + r * Mth.sin(a)), y - .9f * u * u, -.35f + r * (1 - Mth.cos(a)) * .75f);
                }
                BoneRender.curve(bone, p, rib, .2f, .13f, 7, light, BoneRender.WHITE);
            }
        }
        BoneRender.bone(bone, p, new Vector3f(0, 6.5f, 2.95f), new Vector3f(0, 3.2f, 2.55f), .32f, light, BoneRender.WHITE);
        // Collarbones and shoulder blades
        for (int side = -1; side <= 1; side += 2) {
            BoneRender.bone(bone, p, new Vector3f(side * .5f, 7.1f, 1.2f), new Vector3f(side * (BoneColossus.SHOULDER_X - .3f), BoneColossus.SHOULDER_Y + .3f, .2f),
                    .3f, light, BoneRender.WHITE);
            BoneRender.blob(bone, p, new Vector3f(side * 2.1f, 6.1f, -1.1f), new Vector3f(1.1f, 0, 0), new Vector3f(0, 1.4f, 0), new Vector3f(0, 0, .2f), 10, 6, light, BoneRender.WHITE);
        }
        // The arms
        Vec3 target = colossus.slamTarget();
        float since = age - colossus.slamStart();
        for (int side = -1; side <= 1; side += 2) {
            Vector3f shoulder = new Vector3f(side * BoneColossus.SHOULDER_X, BoneColossus.SHOULDER_Y, 0);
            Vector3f rest = new Vector3f(side * 5.2f, 1.3f + .25f * Mth.sin(age * .05f + side), 9.5f);
            Vector3f hand = rest;
            float fist = .35f;
            if (side == colossus.slamSide() && since >= 0 && since <= BoneColossus.RETURN) {
                Vector3f high = new Vector3f(side * 3f, 15.5f, 4f);
                Vector3f hit = new Vector3f((float) target.x, .9f, (float) target.z);
                if (since < BoneColossus.RAISE) {
                    float k = since / BoneColossus.RAISE;
                    hand = lerp(rest, high, k * k * (3 - 2 * k));
                    fist = .35f + .55f * k;
                } else if (since < BoneColossus.IMPACT) {
                    float k = (since - BoneColossus.RAISE) / (BoneColossus.IMPACT - BoneColossus.RAISE);
                    hand = lerp(high, hit, k * k);
                    fist = .9f;
                } else if (since < BoneColossus.IMPACT + 4) {
                    hand = hit;
                    fist = .9f;
                } else {
                    float k = (since - BoneColossus.IMPACT - 4) / (BoneColossus.RETURN - BoneColossus.IMPACT - 4);
                    hand = lerp(hit, rest, k * k * (3 - 2 * k));
                    fist = .9f - .55f * k;
                }
            }
            arm(bone, poseStack, shoulder, hand, side, fist, light);
            p = poseStack.last();
        }
        poseStack.popPose();
        // The skull last (it asks for its own glow buffer)
        poseStack.pushPose();
        poseStack.translate(0, -14 * (1 - risen) + 7.6f, .7f);
        float roar = Mth.clamp(1 - Math.abs(age - (BoneColossus.RISE - 4)) / 12, 0, 1);
        float strike = since >= BoneColossus.RAISE - 4 && since <= BoneColossus.IMPACT + 6 ? 1 : 0;
        poseStack.mulPose(com.mojang.math.Axis.XP.rotation(.15f + .15f * strike));
        BoneRender.skull(poseStack, buffer, 3.1f, .15f + .55f * Math.max(roar, strike * .6f), light, risen);
        poseStack.popPose();
        // The ground glowing where a blow fell, and round its waist as it rises
        VertexConsumer glow = buffer.getBuffer(RenderType.eyes(GlyphRender.GLOW));
        float struck = since >= BoneColossus.IMPACT ? Mth.clamp(1 - (since - BoneColossus.IMPACT) / 16, 0, 1) : 0;
        if (struck > 0) {
            poseStack.pushPose();
            poseStack.translate(target.x, .06, target.z);
            RenderHelper.groundQuad(glow, poseStack.last(), BoneColossus.SLAM_RADIUS * (1.5f - .5f * struck), RuneRender.additive(BoneFx.MARROW, struck), GlyphRender.FULL);
            poseStack.popPose();
        }
        float rising = Mth.clamp(1 - Math.abs(risen - .5f) * 2, 0, 1) * (age < BoneColossus.RISE + 10 || age > colossus.getLife() ? 1 : 0);
        if (rising > 0) {
            poseStack.pushPose();
            poseStack.translate(0, .05, .5);
            RenderHelper.groundQuad(glow, poseStack.last(), 6, RuneRender.additive(BoneFx.MARROW, rising * .8f), GlyphRender.FULL);
            poseStack.popPose();
        }
        poseStack.popPose();
        super.render(colossus, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    private static Vector3f lerp(Vector3f a, Vector3f b, float t) {
        return new Vector3f(a).lerp(b, t);
    }

    /** One arm, from the shoulder to the hand, its elbow bending outwards and back; the hand at the end. */
    private static void arm(VertexConsumer bone, PoseStack poseStack, Vector3f shoulder, Vector3f hand, int side, float fist, int light) {
        float l = BoneColossus.ARM;
        Vector3f d = new Vector3f(hand).sub(shoulder);
        float dist = Mth.clamp(d.length(), .5f, 2 * l - .02f);
        d.normalize();
        Vector3f wrist = new Vector3f(shoulder).add(new Vector3f(d).mul(dist));
        Vector3f pole = new Vector3f(side, .2f, -1f);
        pole.sub(new Vector3f(d).mul(pole.dot(d))).normalize();
        float half = dist * .5f;
        float h = Mth.sqrt(Math.max(0, l * l - half * half));
        Vector3f elbow = new Vector3f(shoulder).add(new Vector3f(d).mul(half)).add(pole.mul(h));
        PoseStack.Pose p = poseStack.last();
        BoneRender.ball(bone, p, shoulder, .75f, light, BoneRender.WHITE);
        BoneRender.bone(bone, p, shoulder, elbow, .6f, light, BoneRender.WHITE);
        Vector3f fore = new Vector3f(wrist).sub(elbow);
        Vector3f across = new Vector3f(fore).cross(new Vector3f(0, 1, 0));
        if (across.lengthSquared() < 1.0E-4f) {
            across.set(1, 0, 0);
        }
        across.normalize().mul(.22f);
        BoneRender.bone(bone, p, new Vector3f(elbow).add(across), new Vector3f(wrist).add(across), .38f, light, BoneRender.WHITE);
        BoneRender.bone(bone, p, new Vector3f(elbow).sub(across), new Vector3f(wrist).sub(across), .32f, light, BoneRender.WHITE);
        poseStack.pushPose();
        poseStack.translate(wrist.x, wrist.y, wrist.z);
        BoneRender.alignUp(poseStack, fore);
        BoneRender.hand(bone, poseStack.last(), 2.3f, fist, light, BoneRender.WHITE);
        poseStack.popPose();
    }

    @Override
    public boolean shouldRender(BoneColossus colossus, Frustum frustum, double x, double y, double z) {
        return frustum.isVisible(colossus.getBoundingBox().inflate(16, 16, 16));
    }

    @Override
    public ResourceLocation getTextureLocation(BoneColossus colossus) {
        return BoneRender.BONE;
    }
}
