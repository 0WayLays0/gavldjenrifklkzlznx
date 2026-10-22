package com.waylays.arcanes.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.waylays.arcanes.entity.token.TokenHand;
import com.waylays.arcanes.spells.token.TokenFx;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * The Hand of Souls. Hand space: the palm lies flat facing down, the fingers point ahead (+Z) and curl down as the
 * hand closes, the thumb on the left curls in under the palm. Each of its tokens flies in from far around to its place
 * in turn; threads of light run along the fingers once it has formed; at the end the tokens burst outward and fall.
 */
public class TokenHandRenderer extends EntityRenderer<TokenHand> {
    private static final float STEP = .4f;
    private static final int FLY_TICKS = 16;

    public TokenHandRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    /** Where every token of the hand sits for a closing of {@code curl} (0 open, 1 a fist), in hand space. */
    static List<Vec3> slots(float curl) {
        List<Vec3> out = new ArrayList<>();
        for (int ix = 0; ix < 4; ix++) {
            for (int iz = 0; iz < 4; iz++) {
                out.add(new Vec3((ix - 1.5) * STEP, 0, (iz - 1.5) * STEP));
            }
        }
        for (int ix = 0; ix < 3; ix++) {
            for (int iz = 0; iz < 2; iz++) {
                out.add(new Vec3((ix - 1) * STEP, .05, -.85 - iz * STEP));
            }
        }
        for (int finger = 0; finger < 4; finger++) {
            out.addAll(finger(finger, curl));
        }
        out.addAll(thumb(curl));
        return out;
    }

    /** The three joints of a finger (index 0 is by the thumb), bending down one after another as the hand closes. */
    static List<Vec3> finger(int finger, float curl) {
        List<Vec3> out = new ArrayList<>();
        float length = finger == 3 ? .34f : STEP;
        Vec3 p = new Vec3((finger - 1.5) * STEP, 0, .8);
        float bend = curl * 1.15f;
        for (int k = 1; k <= 3; k++) {
            float a = Math.min(bend * k, 2.8f);
            p = p.add(0, -Mth.sin(a) * length, Mth.cos(a) * length);
            out.add(p);
        }
        return out;
    }

    /** The two joints of the thumb, swinging in under the palm as the hand closes. */
    static List<Vec3> thumb(float curl) {
        Vec3 open = new Vec3(-.6, 0, .8).normalize();
        Vec3 closed = new Vec3(.5, -.7, .5).normalize();
        Vec3 base = new Vec3(-.75, 0, -.1);
        Vec3 q1 = base.add(open.lerp(closed, curl * .5).normalize().scale(STEP));
        Vec3 q2 = q1.add(open.lerp(closed, curl).normalize().scale(STEP));
        return List.of(q1, q2);
    }

    /** A steady random number in [-1, 1] for a token and a channel, so each flies its own way. */
    private static float noise(int seed) {
        return RuneRender.hash(seed) * 2 - 1;
    }

    @Override
    public void render(TokenHand hand, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        float age = hand.tickCount + partialTicks;
        float curl = hand.curl(partialTicks);
        List<Vec3> slots = slots(curl);
        float scatter = Mth.clamp((age - TokenHand.SLAM_END) / TokenHand.DISPERSE, 0, 1);
        float fade = 1 - scatter * scatter;
        int seed = hand.getId() * 977;

        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(-hand.getYRot()));
        poseStack.scale(TokenHand.SCALE, TokenHand.SCALE, TokenHand.SCALE);
        float formed = 1;
        for (int i = 0; i < slots.size(); i++) {
            Vec3 slot = slots.get(i);
            // Flies in from far around, each token a little after the one before
            float t = Mth.clamp((age - i * .6f) / FLY_TICKS, 0, 1);
            float e = 1 - (1 - t) * (1 - t) * (1 - t);
            formed = Math.min(formed, t);
            Vec3 from = new Vec3(noise(seed + i * 3), noise(seed + i * 3 + 1) * .6 + .4, noise(seed + i * 3 + 2)).normalize().scale(5 + 2 * RuneRender.hash(seed + i * 7));
            Vec3 p = from.lerp(slot, e);
            if (scatter > 0) {
                Vec3 out = slot.normalize().add(noise(seed + i * 5) * .4, .3 + .3 * noise(seed + i * 5 + 1), noise(seed + i * 5 + 2) * .4);
                float s = scatter * TokenHand.DISPERSE;
                p = slot.add(out.scale(s * .18)).add(0, -.012 * s * s, 0);
            }
            if (t <= 0) {
                continue;
            }
            poseStack.pushPose();
            poseStack.translate(p.x, p.y, p.z);
            // Spinning as it flies, settling flat in the hand
            float spin = (1 - e) * 720 + i * 37 + (scatter > 0 ? scatter * 900 : 0);
            poseStack.mulPose(Axis.YP.rotationDegrees(spin));
            poseStack.mulPose(Axis.XP.rotationDegrees((1 - e) * 540 + 6 * Mth.sin(age * .2f + i)));
            TokenRender.token(poseStack, buffer, .42f * fade, GlyphRender.FULL);
            poseStack.popPose();
        }
        // Threads of light along the fingers and round the palm, once it has formed
        float threads = formed * fade;
        if (threads > 0) {
            VertexConsumer consumer = buffer.getBuffer(RenderType.eyes(GlyphRender.STROKE));
            int c = RuneRender.additive(TokenFx.CYAN, .55f * threads);
            for (int finger = 0; finger < 4; finger++) {
                Vec3 prev = new Vec3((finger - 1.5) * STEP, 0, .6);
                for (Vec3 joint : finger(finger, curl)) {
                    GlyphRender.beam(consumer, poseStack.last(), prev, joint, .1f, c);
                    prev = joint;
                }
            }
            Vec3 prev = new Vec3(-.6, 0, -.1);
            for (Vec3 joint : thumb(curl)) {
                GlyphRender.beam(consumer, poseStack.last(), prev, joint, .1f, c);
                prev = joint;
            }
            Vec3[] rim = {new Vec3(-.75, 0, -.75), new Vec3(.75, 0, -.75), new Vec3(.75, 0, .75), new Vec3(-.75, 0, .75)};
            for (int k = 0; k < 4; k++) {
                GlyphRender.beam(consumer, poseStack.last(), rim[k], rim[(k + 1) % 4], .08f, RuneRender.additive(TokenFx.CYAN, .35f * threads));
            }
            poseStack.pushPose();
            poseStack.translate(0, -.3, .2);
            TokenRender.glow(poseStack, buffer, this.entityRenderDispatcher, 1.6f, .35f * threads);
            poseStack.popPose();
        }
        poseStack.popPose();
        super.render(hand, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    @Override
    public boolean shouldRender(TokenHand entity, Frustum frustum, double x, double y, double z) {
        return true;
    }

    @Override
    public ResourceLocation getTextureLocation(TokenHand entity) {
        return GlyphRender.GLOW;
    }
}
