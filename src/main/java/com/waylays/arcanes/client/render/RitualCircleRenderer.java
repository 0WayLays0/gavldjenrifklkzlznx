package com.waylays.arcanes.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.waylays.arcanes.ritual.RitualCircle;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * A ritual circle, as it is drawn and as the ritual builds:
 * <ol>
 * <li>Drawn: the chalk traces itself, the outer circles first, then the star joining the stations, then the stations'
 * rings and the twelve runes around.</li>
 * <li>Waiting: the chalk glows faintly; each station blinks until someone kneels on it, then shines in the ritual's
 * colour.</li>
 * <li>Channelling: the runes light one by one; then beams reach from every station to the middle and climb; a pillar of
 * light rises from the middle into the sky; at last a ring of runes opens high above, turning faster and faster.</li>
 * <li>Cast: a flash, the pillar widens and fades. Broken: the circle flickers red and dies.</li>
 * </ol>
 */
public class RitualCircleRenderer extends EntityRenderer<RitualCircle> {
    private static final int CHALK = 0xF4F0E4;
    private static final int BROKEN = 0xFF3030;
    private static final int RUNES = 12;
    private static final float SKY = 24;

    public RitualCircleRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(RitualCircle circle, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        RitualCircle.State state = circle.getState();
        float age = circle.tickCount + partialTicks;
        float since = circle.sinceState(partialTicks);
        float drawn = circle.drawn(partialTicks);
        float progress = circle.progress(partialTicks);
        int color = state == RitualCircle.State.FAILED ? BROKEN : circle.getRitual().color;
        // How bright it all is: dying after the end, flickering when broken
        float life = switch (state) {
            case DONE -> 1 - Mth.clamp((since - 20) / (RitualCircle.AFTERGLOW - 20), 0, 1);
            case FAILED -> (1 - since / RitualCircle.AFTERGLOW) * (.6f + .4f * Mth.sin(age * 2.7f));
            default -> 1;
        };
        if (life <= 0) {
            return;
        }
        float glow = state == RitualCircle.State.WAITING ? .55f + .15f * Mth.sin(age * .12f)
                : state == RitualCircle.State.CHANNELING ? .6f + .4f * progress : .7f;
        int n = circle.getRitual().participants;
        VertexConsumer stroke = buffer.getBuffer(RenderType.eyes(GlyphRender.STROKE));
        poseStack.pushPose();
        poseStack.translate(0, .03, 0);
        PoseStack.Pose pose = poseStack.last();
        int chalk = RuneRender.additive(state == RitualCircle.State.CHANNELING || state == RitualCircle.State.DONE
                ? GlyphRender.whiten(color, .5f) : state == RitualCircle.State.FAILED ? BROKEN : CHALK, glow * life);

        // The two outer circles, traced around
        float circles = Mth.clamp(drawn / .5f, 0, 1);
        ring(stroke, pose, RitualCircle.RADIUS + .45f, circles, .1f, chalk);
        ring(stroke, pose, RitualCircle.RADIUS - .65f, circles, .07f, chalk);
        // The star joining the stations (a line through the middle for two)
        float star = Mth.clamp((drawn - .5f) / .3f, 0, 1);
        int step = n == 5 ? 2 : 1;
        for (int i = 0; i < n; i++) {
            float t = Mth.clamp(star * n - i, 0, 1);
            if (t <= 0) {
                continue;
            }
            Vec3 a = circle.station(i);
            Vec3 b = n <= 2 ? Vec3.ZERO : circle.station((i + step) % n);
            GlyphRender.beam(stroke, pose, a, a.lerp(b, t), .08f, chalk);
            if (n == 4) {
                GlyphRender.beam(stroke, pose, a, a.lerp(circle.station((i + 2) % n), t * .5f), .06f, chalk);
            }
        }
        poseStack.popPose();

        float rest = Mth.clamp((drawn - .8f) / .2f, 0, 1);
        stations(circle, poseStack, buffer, age, rest, color, life);
        runes(circle, poseStack, buffer, age, rest, progress, color, life, state);
        if (state == RitualCircle.State.CHANNELING || state == RitualCircle.State.DONE) {
            channel(circle, poseStack, buffer, age, since, progress, color, life, state == RitualCircle.State.DONE);
        }
        super.render(circle, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    /** A circle of chalk of radius {@code r}, traced as far as {@code traced} (0 to 1). */
    private static void ring(VertexConsumer stroke, PoseStack.Pose pose, float r, float traced, float width, int argb) {
        int segments = 48;
        int shown = Mth.ceil(segments * traced);
        for (int i = 0; i < shown; i++) {
            float a0 = i * Mth.TWO_PI / segments;
            float a1 = Math.min(i + 1, segments * traced) * Mth.TWO_PI / segments;
            GlyphRender.beam(stroke, pose, new Vec3(Mth.cos(a0) * r, 0, Mth.sin(a0) * r), new Vec3(Mth.cos(a1) * r, 0, Mth.sin(a1) * r), width, argb);
        }
    }

    /** Each station: a small ring, blinking while empty, shining in the ritual's colour when someone kneels on it. */
    private static void stations(RitualCircle circle, PoseStack poseStack, MultiBufferSource buffer, float age, float shown, int color, float life) {
        if (shown <= 0) {
            return;
        }
        int occupied = circle.getOccupied();
        for (int i = 0; i < circle.getRitual().participants; i++) {
            boolean taken = (occupied & (1 << i)) != 0;
            float strength = taken ? .9f : .35f + .3f * Mth.sin(age * .25f + i);
            Vec3 s = circle.station(i);
            poseStack.pushPose();
            poseStack.translate(s.x, .05, s.z);
            poseStack.mulPose(Axis.YP.rotationDegrees(age * (taken ? 3 : 1)));
            RenderHelper.groundQuad(buffer.getBuffer(RenderType.eyes(RuneRender.RING_GLOW)), poseStack.last(), .8f * shown,
                    RuneRender.additive(taken ? color : CHALK, strength * life), GlyphRender.FULL);
            if (taken) {
                RenderHelper.groundQuad(buffer.getBuffer(RenderType.eyes(GlyphRender.GLOW)), poseStack.last(), 1.1f,
                        RuneRender.additive(color, .4f * life), GlyphRender.FULL);
            }
            poseStack.popPose();
        }
    }

    /** The twelve runes around the circle: chalk at first, lit one by one in the ritual's colour as it builds. */
    private static void runes(RitualCircle circle, PoseStack poseStack, MultiBufferSource buffer, float age, float shown, float progress,
                              int color, float life, RitualCircle.State state) {
        if (shown <= 0) {
            return;
        }
        float lit = state == RitualCircle.State.DONE ? RUNES : progress / .35f * RUNES;
        VertexConsumer glyphs = buffer.getBuffer(RenderType.eyes(RuneRender.GLYPHS));
        for (int k = 0; k < RUNES; k++) {
            float a = circle.getYRot() * Mth.DEG_TO_RAD + (k + .5f) * Mth.TWO_PI / RUNES;
            float on = Mth.clamp(lit - k, 0, 1);
            int argb = on > 0 ? RuneRender.additive(GlyphRender.whiten(color, .2f * on), (.5f + .5f * on) * life)
                    : RuneRender.additive(CHALK, (.25f + .1f * Mth.sin(age * .2f + k)) * life * shown);
            poseStack.pushPose();
            float r = RitualCircle.RADIUS - .1f;
            poseStack.translate(Mth.cos(a) * r, .04 + on * .02, Mth.sin(a) * r);
            poseStack.mulPose(Axis.YP.rotation(-a - Mth.HALF_PI));
            poseStack.mulPose(Axis.XP.rotationDegrees(-90));
            RuneRender.glyph(glyphs, poseStack.last(), circle.getId() + k, .55f * shown, argb, GlyphRender.FULL);
            poseStack.popPose();
        }
    }

    /** Beams from the stations to the middle, the pillar of light, the ring in the sky; a flash once cast. */
    private void channel(RitualCircle circle, PoseStack poseStack, MultiBufferSource buffer, float age, float since, float p,
                         int color, float life, boolean done) {
        VertexConsumer stroke = buffer.getBuffer(RenderType.eyes(GlyphRender.STROKE));
        PoseStack.Pose pose = poseStack.last();
        int n = circle.getRitual().participants;
        float beams = Mth.clamp((p - .35f) / .15f, 0, 1);
        if (beams > 0) {
            float apex = 2 + 6 * Mth.clamp((p - .35f) / .45f, 0, 1);
            for (int i = 0; i < n; i++) {
                Vec3 s = circle.station(i).add(0, 1.2, 0);
                Vec3 top = new Vec3(0, apex, 0);
                float pulse = .6f + .4f * Mth.sin(age * .4f + i * 1.3f);
                GlyphRender.beam(stroke, pose, s, s.lerp(top, beams), .22f, RuneRender.additive(color, pulse * life));
                GlyphRender.beam(stroke, pose, s, s.lerp(top, beams), .08f, RuneRender.additive(0xFFFFFF, pulse * .8f * life));
            }
        }
        float pillar = Mth.clamp((p - .5f) / .3f, 0, 1);
        if (pillar > 0 || done) {
            float height = done ? 60 : 2 + 58 * pillar * pillar;
            float flash = done ? Math.max(0, 1 - since / 20f) : 0;
            float width = (.3f + .9f * pillar) + flash * 3;
            Vec3 foot = new Vec3(0, .1, 0);
            Vec3 head = new Vec3(0, height, 0);
            GlyphRender.beam(stroke, pose, foot, head, width * 1.8f, RuneRender.additive(color, (.35f + .3f * flash) * life));
            GlyphRender.beam(stroke, pose, foot, head, width, RuneRender.additive(GlyphRender.whiten(color, .5f), (.7f + .3f * flash) * life));
            GlyphRender.beam(stroke, pose, foot, head, width * .35f, RuneRender.additive(0xFFFFFF, life));
            poseStack.pushPose();
            poseStack.translate(0, .06, 0);
            poseStack.mulPose(Axis.YP.rotationDegrees(-age * 2));
            RenderHelper.groundQuad(buffer.getBuffer(RenderType.eyes(RuneRender.RING_GLOW)), poseStack.last(), RitualCircle.RADIUS * (.6f + .4f * pillar),
                    RuneRender.additive(color, .5f * life), GlyphRender.FULL);
            poseStack.popPose();
        }
        float sky = Mth.clamp((p - .75f) / .25f, 0, 1);
        if (sky > 0 || done) {
            float open = done ? 1 + since / 30f : sky;
            float spin = age * (2 + 10 * sky * sky);
            for (int layer = 0; layer < 2; layer++) {
                poseStack.pushPose();
                poseStack.translate(0, SKY + layer * .2f, 0);
                poseStack.mulPose(Axis.YP.rotationDegrees(layer == 0 ? spin : -spin * .7f));
                float size = (layer == 0 ? 9 : 6) * open;
                // Seen from below: the quad faces down too
                RenderHelper.groundQuad(buffer.getBuffer(RenderType.eyes(RuneRender.RING_GLOW)), poseStack.last(), size,
                        RuneRender.additive(color, .8f * life), GlyphRender.FULL);
                poseStack.mulPose(Axis.XP.rotationDegrees(180));
                RenderHelper.groundQuad(buffer.getBuffer(RenderType.eyes(RuneRender.RING_GLOW)), poseStack.last(), size,
                        RuneRender.additive(color, .8f * life), GlyphRender.FULL);
                poseStack.popPose();
            }
        }
    }

    @Override
    public boolean shouldRender(RitualCircle circle, Frustum frustum, double x, double y, double z) {
        return true;
    }

    @Override
    public ResourceLocation getTextureLocation(RitualCircle circle) {
        return GlyphRender.GLOW;
    }
}
