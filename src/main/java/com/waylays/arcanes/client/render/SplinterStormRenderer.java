package com.waylays.arcanes.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.waylays.arcanes.entity.wood.SplinterStorm;
import com.waylays.arcanes.spells.wood.WoodFx;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Splinter Cyclone: a funnel of wood, narrow at its foot and wide at its top, everything in it climbing as it whirls:
 * curled shavings of every wood (pale birch, oak, dark oak, spruce, cherry) turning over, chips tumbling, splinters
 * flying point first, flakes of bark, split planks and broken sticks heavier and slower near the top, a haze of
 * sawdust; streaks of dusty wind spiralling with them; chips skidding round its foot over a ring of stirred dust, a
 * faint green thread of life up its middle. When it bursts everything flies outwards and falls.
 */
public class SplinterStormRenderer extends EntityRenderer<SplinterStorm> {
    private static final int SHAVINGS = 80;
    private static final int CHIPS = 90;
    private static final int SPLINTERS = 50;
    private static final int FLAKES = 30;
    private static final int PLANKS = 9;
    private static final int STICKS = 7;
    private static final int SKIDDERS = 30;
    private static final int SAWDUST = 160;
    private static final int STREAKS = 7;
    /** Woods: birch, oak, dark oak, spruce, cherry, jungle. */
    private static final int[] WOODS = {0xFFF4EAD2, 0xFFE8C48C, 0xFF9A7048, 0xFFC08A58, 0xFFF2BCB4, 0xFFD8A070};

    public SplinterStormRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    private static float funnel(float t) {
        return .5f + (SplinterStorm.RADIUS + .7f) * (float) Math.pow(t, 1.25);
    }

    private static int wood(int seed) {
        return WOODS[(int) (WoodFx.hash(seed) * WOODS.length) % WOODS.length];
    }

    /**
     * Where one piece of the storm is now (x, y, z, its angle round the axis): climbing the funnel at its own pace,
     * whirling round, flung outwards and falling once the storm bursts.
     */
    private static float[] orbit(int seed, float age, float climbSpeed, float tMin, float tMax, float spread, float scatter) {
        float t = tMin + (tMax - tMin) * ((WoodFx.hash(seed) + age * climbSpeed) % 1);
        float y = t * SplinterStorm.HEIGHT;
        float r = funnel(t) * (1 - spread * .5f + spread * WoodFx.hash(seed + 1));
        float a = WoodFx.hash(seed + 2) * Mth.TWO_PI + age * (.36f - .18f * t);
        if (scatter > 0) {
            r += scatter * (5 + 7 * WoodFx.hash(seed + 3));
            y = Math.max(.05f, y + scatter * 3 - scatter * scatter * (y + 5));
        }
        return new float[]{Mth.cos(a) * r, y, Mth.sin(a) * r, a};
    }

    @Override
    public void render(SplinterStorm storm, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int light) {
        float age = storm.tickCount + partialTicks;
        float strength = storm.strength(age);
        float scatter = Mth.clamp((age - storm.getLife()) / SplinterStorm.SCATTER, 0, 1);
        if (strength <= .01f && scatter >= 1) {
            return;
        }
        float shown = Mth.clamp(age / SplinterStorm.GATHER, .12f, 1);
        float fall = 1 - scatter;
        int seed = storm.getId() * 4241;
        Quaternionf camera = this.entityRenderDispatcher.cameraOrientation();

        // Split wood: chips, splinters, planks, the chips skidding round its foot, the sawdust
        VertexConsumer grain = WoodRender.grain(buffer);
        for (int i = 0; i < CHIPS * shown; i++) {
            int s = seed + i * 13;
            float[] o = orbit(s, age, .005f + .004f * WoodFx.hash(s + 5), 0, 1, .5f, scatter);
            poseStack.pushPose();
            poseStack.translate(o[0], o[1], o[2]);
            poseStack.mulPose(Axis.YP.rotation(-o[3]));
            poseStack.mulPose(Axis.XP.rotation(age * (.3f + .4f * WoodFx.hash(s + 6)) + i));
            poseStack.mulPose(Axis.ZP.rotation(age * .25f + i * 2));
            float k = .5f + WoodFx.hash(s + 7);
            WoodRender.box(grain, poseStack.last(), .08f * k, .025f * k, .055f * k, light, wood(s));
            poseStack.popPose();
        }
        PoseStack.Pose p = poseStack.last();
        for (int i = 0; i < SPLINTERS * shown; i++) {
            int s = seed + i * 17 + 5000;
            float[] o = orbit(s, age, .006f + .004f * WoodFx.hash(s + 5), .05f, 1, .4f, scatter);
            // Point first along the whirl
            float a = o[3];
            Vector3f along = new Vector3f(-Mth.sin(a), .25f, Mth.cos(a)).normalize();
            float len = .3f + .35f * WoodFx.hash(s + 6);
            Vector3f mid = new Vector3f(o[0], o[1], o[2]);
            Vector3f tail = new Vector3f(mid).sub(new Vector3f(along).mul(len * .5f));
            Vector3f tip = new Vector3f(mid).add(new Vector3f(along).mul(len * .5f));
            BoneRender.tube(grain, p, tail, tip, .03f, .002f, 4, light, wood(s));
        }
        for (int i = 0; i < PLANKS * shown; i++) {
            int s = seed + i * 23 + 9000;
            float[] o = orbit(s, age, .002f + .001f * WoodFx.hash(s + 5), .55f, 1, .3f, scatter);
            poseStack.pushPose();
            poseStack.translate(o[0], o[1], o[2]);
            poseStack.mulPose(Axis.YP.rotation(-o[3] + age * .05f));
            poseStack.mulPose(Axis.XP.rotation(age * .07f + i));
            poseStack.mulPose(Axis.ZP.rotation(age * .05f));
            WoodRender.box(grain, poseStack.last(), .12f, .03f, .38f + .1f * WoodFx.hash(s + 8), light, wood(s));
            poseStack.popPose();
        }
        for (int i = 0; i < SKIDDERS * shown * fall; i++) {
            int s = seed + i * 29 + 12000;
            float a = WoodFx.hash(s) * Mth.TWO_PI + age * (.2f + .1f * WoodFx.hash(s + 1));
            float r = 1.2f + 2.6f * WoodFx.hash(s + 2);
            poseStack.pushPose();
            poseStack.translate(Mth.cos(a) * r, .04f + .18f * Math.abs(Mth.sin(age * .3f + i)), Mth.sin(a) * r);
            poseStack.mulPose(Axis.YP.rotation(-a + age * .4f));
            WoodRender.box(grain, poseStack.last(), .07f, .02f, .05f, light, wood(s));
            poseStack.popPose();
        }
        for (int i = 0; i < SAWDUST * shown; i++) {
            int s = seed + i * 31 + 15000;
            float[] o = orbit(s, age, .008f + .006f * WoodFx.hash(s + 5), 0, 1, .9f, scatter);
            poseStack.pushPose();
            poseStack.translate(o[0], o[1], o[2]);
            poseStack.mulPose(camera);
            RenderHelper.billboard(grain, poseStack.last(), .03f + .025f * WoodFx.hash(s + 6), wood(s), light);
            poseStack.popPose();
        }

        // Curled shavings
        VertexConsumer shaving = buffer.getBuffer(RenderType.entityCutoutNoCull(WoodRender.SHAVING));
        for (int i = 0; i < SHAVINGS * shown; i++) {
            int s = seed + i * 37 + 20000;
            float[] o = orbit(s, age, .004f + .004f * WoodFx.hash(s + 5), 0, 1, .55f, scatter);
            poseStack.pushPose();
            poseStack.translate(o[0], o[1], o[2]);
            poseStack.mulPose(Axis.YP.rotation(-o[3]));
            poseStack.mulPose(Axis.XP.rotation(age * (.15f + .25f * WoodFx.hash(s + 6)) + i));
            poseStack.mulPose(Axis.ZP.rotation(age * .2f + i * 3));
            float k = .7f + .6f * WoodFx.hash(s + 7);
            curl(shaving, poseStack.last(), .05f * k, .022f * k, 1.6f + 1.4f * WoodFx.hash(s + 8), .07f * k, light, wood(s));
            poseStack.popPose();
        }

        // Flakes of bark and broken sticks
        VertexConsumer bark = WoodRender.bark(buffer);
        for (int i = 0; i < FLAKES * shown; i++) {
            int s = seed + i * 41 + 25000;
            float[] o = orbit(s, age, .004f + .003f * WoodFx.hash(s + 5), .1f, 1, .5f, scatter);
            poseStack.pushPose();
            poseStack.translate(o[0], o[1], o[2]);
            poseStack.mulPose(Axis.YP.rotation(-o[3]));
            poseStack.mulPose(Axis.XP.rotation(age * .35f + i));
            poseStack.mulPose(Axis.ZP.rotation(age * .15f));
            WoodRender.box(bark, poseStack.last(), .1f, .02f, .07f, light, BoneRender.WHITE);
            poseStack.popPose();
        }
        p = poseStack.last();
        for (int i = 0; i < STICKS * shown; i++) {
            int s = seed + i * 43 + 30000;
            float[] o = orbit(s, age, .0015f + .001f * WoodFx.hash(s + 5), .5f, 1, .3f, scatter);
            float spin = age * .12f + i;
            Vector3f d = new Vector3f(Mth.cos(spin) * Mth.cos(o[3]), Mth.sin(spin) * .6f, Mth.cos(spin) * Mth.sin(o[3])).normalize();
            Vector3f c = new Vector3f(o[0], o[1], o[2]);
            float len = .5f + .4f * WoodFx.hash(s + 6);
            Vector3f a = new Vector3f(c).sub(new Vector3f(d).mul(len));
            Vector3f b = new Vector3f(c).add(new Vector3f(d).mul(len));
            Vector3f bend = new Vector3f(c).add(.1f, .12f, -.06f);
            BoneRender.curve(bark, p, new Vector3f[]{a, bend, b}, .06f, .035f, 5, light, BoneRender.WHITE);
            Vector3f twig = new Vector3f(bend).add(new Vector3f(d).mul(.25f)).add(0, .25f, 0);
            BoneRender.tube(bark, p, bend, twig, .03f, .01f, 4, light, BoneRender.WHITE);
        }

        // Dusty wind spiralling with it, and the stirred dust round its foot
        VertexConsumer wind = buffer.getBuffer(RenderType.entityTranslucent(WoodRender.WIND));
        p = poseStack.last();
        for (int w = 0; w < STREAKS; w++) {
            float phase = w * Mth.TWO_PI / STREAKS;
            float t0 = (WoodFx.hash(seed + w * 7) + age * .01f) % 1;
            net.minecraft.world.phys.Vec3 prev = null;
            for (int k = 0; k <= 14; k++) {
                float u = k / 14f;
                float t = Mth.clamp(t0 * .5f + u * .55f, 0, 1);
                float r = funnel(t) * (.9f + .15f * Mth.sin(w + u * 4)) + scatter * 6;
                float a = phase + age * (.34f - .16f * t) + u * 2.6f;
                net.minecraft.world.phys.Vec3 at = new net.minecraft.world.phys.Vec3(Mth.cos(a) * r, t * SplinterStorm.HEIGHT, Mth.sin(a) * r);
                if (prev != null) {
                    float alpha = .45f * strength * Mth.sin(u * Mth.PI);
                    GlyphRender.beam(wind, p, prev, at, .35f + .3f * t, RuneRender.alpha(0xD8C4A0, alpha));
                }
                prev = at;
            }
        }
        VertexConsumer dust = buffer.getBuffer(RenderType.entityTranslucent(WoodRender.DUST));
        for (int k = 0; k < 3; k++) {
            poseStack.pushPose();
            poseStack.translate(0, .04f + k * .02f, 0);
            poseStack.mulPose(Axis.YP.rotation(age * (.15f + .05f * k) + k));
            RenderHelper.groundQuad(dust, poseStack.last(), 2.2f + 1.4f * k, RuneRender.alpha(0xC8B08A, (.55f - .12f * k) * strength), light);
            poseStack.popPose();
        }

        // A faint green thread of life up its middle
        VertexConsumer glow = buffer.getBuffer(RenderType.eyes(GlyphRender.STROKE));
        float pulse = .25f + .1f * Mth.sin(age * .3f);
        GlyphRender.beam(glow, poseStack.last(), new net.minecraft.world.phys.Vec3(0, .2, 0), new net.minecraft.world.phys.Vec3(0, SplinterStorm.HEIGHT * .9, 0), .5f,
                RuneRender.additive(WoodFx.LIFE, pulse * strength));
        super.render(storm, entityYaw, partialTicks, poseStack, buffer, light);
    }

    /**
     * A curled shaving: a thin strip {@code width} wide rolled into a loosening spiral, starting {@code r0} out and
     * opening by {@code grow} a turn, {@code turns} turns long, the texture along it.
     */
    private static void curl(VertexConsumer c, PoseStack.Pose p, float r0, float grow, float turns, float width, int light, int argb) {
        int steps = 12;
        float total = turns * Mth.TWO_PI;
        float pitch = width * .6f / Mth.TWO_PI;
        for (int j = 0; j < steps; j++) {
            float a0 = total * j / steps, a1 = total * (j + 1) / steps;
            float ra = r0 + grow * a0 / Mth.TWO_PI, rb = r0 + grow * a1 / Mth.TWO_PI;
            float xa = Mth.cos(a0) * ra, za = Mth.sin(a0) * ra, xb = Mth.cos(a1) * rb, zb = Mth.sin(a1) * rb;
            float ya = a0 * pitch, yb = a1 * pitch;
            float ua = (float) j / steps, ub = (float) (j + 1) / steps;
            RenderHelper.vertex(c, p, xa, ya - width * .5f, za, ua, 1, argb, light, Mth.cos(a0), 0, Mth.sin(a0));
            RenderHelper.vertex(c, p, xa, ya + width * .5f, za, ua, 0, argb, light, Mth.cos(a0), 0, Mth.sin(a0));
            RenderHelper.vertex(c, p, xb, yb + width * .5f, zb, ub, 0, argb, light, Mth.cos(a1), 0, Mth.sin(a1));
            RenderHelper.vertex(c, p, xb, yb - width * .5f, zb, ub, 1, argb, light, Mth.cos(a1), 0, Mth.sin(a1));
        }
    }

    @Override
    public boolean shouldRender(SplinterStorm storm, Frustum frustum, double x, double y, double z) {
        return frustum.isVisible(storm.getBoundingBox().inflate(SplinterStorm.RADIUS + 10, SplinterStorm.HEIGHT + 2, SplinterStorm.RADIUS + 10));
    }

    @Override
    public ResourceLocation getTextureLocation(SplinterStorm storm) {
        return WoodRender.GRAIN;
    }
}
