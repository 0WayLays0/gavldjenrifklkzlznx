package com.waylays.arcanes.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.waylays.arcanes.ArcanesOublies;
import com.waylays.arcanes.client.render.boss.BossMesh;
import com.waylays.arcanes.client.render.clockwork.ClockMesh;
import com.waylays.arcanes.client.render.ritual.RitualMesh;
import com.waylays.arcanes.client.render.water.WaterMesh;
import com.waylays.arcanes.entity.snow.SnowScene;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Snow Magic's spells, drawn: a packed snowball rolling and growing, puffs of powder in its wake, bursting into a cloud;
 * a snow globe (a brass base, a snowy floor, a dome of glass with snow whirling and falling inside) that shatters into
 * shards; a ribbon of ice laid on the ground, crystals growing along its edges and frost twinkling on it; a crown of ice
 * crystals turning over the caster's head, its icicles streaking to their mark; a wall of blizzard whirling round,
 * flurries racing inside it over a frosted ground, and the frozen bursting into ice.
 */
public class SnowSceneRenderer extends EntityRenderer<SnowScene> {
    public static final ResourceLocation SNOW = ArcanesOublies.id("textures/entity/snow/snow.png");
    public static final ResourceLocation ICE = ArcanesOublies.id("textures/entity/snow/ice.png");
    public static final ResourceLocation FLAKE = ArcanesOublies.id("textures/entity/snow/flake.png");
    private static final int WHITE = 0xF4FAFF, FROST = 0xCFEFFF, BLUE = 0x8CC8FF, DEEP = 0x4A90D8;

    public SnowSceneRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(SnowScene e, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource out, int packedLight) {
        // Recorded per render type, then written out one type after another (see SceneBuffers)
        SceneBuffers buffer = SceneBuffers.begin();
        float age = e.tickCount + partialTicks;
        WaterMesh.View view = WaterViews.of(this.entityRenderDispatcher, e, partialTicks);
        int seed = e.getId() * 31;
        switch (e.getKind()) {
            case ROLL -> roll(e, poseStack, buffer, view, age, partialTicks, seed, packedLight);
            case GLOBE -> globe(e, poseStack, buffer, view, age, partialTicks, seed, packedLight);
            case TRAIL -> trail(e, poseStack, buffer, view, age, seed, packedLight);
            case CROWN -> crown(e, poseStack, buffer, view, age, partialTicks, seed);
            case WHITEOUT -> whiteout(e, poseStack, buffer, view, age, partialTicks, seed, packedLight);
        }
        buffer.flush(out);
        super.render(e, entityYaw, partialTicks, poseStack, out, packedLight);
    }

    private static Vector3f v(Vec3 d) {
        return new Vector3f((float) d.x, (float) d.y, (float) d.z);
    }

    /** Puffs of powder bursting out from {@code at}, {@code t} (0 to 1) of the way, {@code size} big. */
    private static void powder(PoseStack poseStack, MultiBufferSource buffer, WaterMesh.View view, Vector3f at, float size, float t, int count, int seed, int light) {
        if (t >= 1) {
            return;
        }
        VertexConsumer cloud = BossMesh.see(buffer, BossMesh.CLOUD);
        VertexConsumer flakes = BossMesh.light(buffer, FLAKE);
        float out = 1 - (1 - t) * (1 - t);
        for (int k = 0; k < count; k++) {
            float a = k * Mth.TWO_PI / count + BossMesh.hash(seed + k) * .6f, up = BossMesh.hash(seed + k * 3);
            Vector3f p = new Vector3f(Mth.cos(a) * size * 2.2f * out, (up * 1.4f - out * .4f) * size * out, Mth.sin(a) * size * 2.2f * out).add(at);
            RitualMesh.billboard(cloud, poseStack, view, p, size * (1.1f + 1.4f * t), a + t, -1, BossMesh.argb(WHITE, .75f * (1 - t)), light);
            RitualMesh.billboard(flakes, poseStack, view, new Vector3f(p).mul(1.2f), .25f, t * 4 + k, -1, BossMesh.glow(FROST, 1 - t), LightTexture.FULL_BRIGHT);
        }
    }

    // ---------------------------------------------------------------- Avalanche Ball

    private void roll(SnowScene e, PoseStack poseStack, MultiBufferSource buffer, WaterMesh.View view, float age, float pt, int seed, int light) {
        float r = e.getSize();
        if (e.hasBurst()) {
            float t = e.sinceBurst(pt) / 18f;
            powder(poseStack, buffer, view, new Vector3f(0, r, 0), r * 1.2f, t, 14, seed, light);
            ClockMesh.ring(poseStack.last(), buffer, .06f, r * 3 * t, r * 3 * t + .5f, FROST, (1 - t) * .8f, LightTexture.FULL_BRIGHT);
            return;
        }
        Vec3 f = e.forward();
        // Rolling over: turned about the axis square to its way, as far as it has gone
        float turn = age * SnowScene.ROLL_SPEED / Math.max(.2f, r);
        poseStack.pushPose();
        poseStack.translate(0, r, 0);
        poseStack.mulPose(new Quaternionf().rotateAxis(turn, (float) f.z, 0, (float) -f.x));
        VertexConsumer snow = BossMesh.see(buffer, SNOW);
        BossMesh.sphere(snow, poseStack.last(), new Vector3f(), r, 10, 16, 0, 0, 1, BossMesh.argb(WHITE, 1), light);
        // Clumps of snow packed on as it rolled
        for (int k = 0; k < 7; k++) {
            float a = BossMesh.hash(seed + k) * Mth.TWO_PI, b = BossMesh.hash(seed + k * 5) * Mth.PI;
            Vector3f at = new Vector3f(Mth.sin(b) * Mth.cos(a), Mth.cos(b), Mth.sin(b) * Mth.sin(a)).mul(r * .92f);
            BossMesh.sphere(snow, poseStack.last(), at, r * (.22f + .1f * BossMesh.hash(seed + k * 7)), 5, 8, 0, 0, 1, BossMesh.argb(0xE8F2FF, 1), light);
        }
        poseStack.popPose();
        // A cold glow under it, powder thrown up in its wake, frost glittering round it
        ClockMesh.glow(poseStack, buffer, view, new Vector3f(0, .1f, 0), r * 3, BLUE, .3f, LightTexture.FULL_BRIGHT);
        VertexConsumer cloud = BossMesh.see(buffer, BossMesh.CLOUD);
        for (int k = 0; k < 8; k++) {
            float back = k * .55f + (age * .2f) % .55f;
            Vector3f p = v(f.scale(-back - r * .8f)).add((BossMesh.hash(seed + k) - .5f) * r, r * (.3f + .15f * k / 8f), (BossMesh.hash(seed + k * 3) - .5f) * r);
            RitualMesh.billboard(cloud, poseStack, view, p, r * (1.3f - k * .08f) + .3f, k + age * .02f, -1, BossMesh.argb(WHITE, .45f * (1 - k / 8f)), light);
        }
        VertexConsumer flakes = BossMesh.light(buffer, FLAKE);
        for (int k = 0; k < 10; k++) {
            float a = BossMesh.hash(seed + k * 11) * Mth.TWO_PI + age * .1f, tw = .5f + .5f * Mth.sin(age * .4f + k * 2);
            Vector3f p = new Vector3f(Mth.cos(a) * r * 1.3f, r + Mth.sin(age * .1f + k) * r * .8f, Mth.sin(a) * r * 1.3f);
            RitualMesh.billboard(flakes, poseStack, view, p, .18f + .1f * tw, age * .05f + k, -1, BossMesh.glow(FROST, .8f * tw), LightTexture.FULL_BRIGHT);
        }
    }

    // ---------------------------------------------------------------- Snow Globe

    private void globe(SnowScene e, PoseStack poseStack, MultiBufferSource buffer, WaterMesh.View view, float age, float pt, int seed, int light) {
        float r = e.getSize();
        float appear = BossMesh.ramp(age, 0, 8);
        float fade = 1 - BossMesh.ramp(age, e.getLife() - 6, e.getLife());
        boolean shattered = e.hasBurst();
        float st = shattered ? e.sinceBurst(pt) / 14f : 0;
        float floor = .36f;
        // The brass base, a band of darker brass round it
        ClockMesh.cylinder(poseStack.last(), buffer, ClockMesh.BRASS, r * 1.02f * appear, -.02f, floor, 40, BossMesh.argb(0xFFFFFF, 1), light);
        ClockMesh.cylinder(poseStack.last(), buffer, ClockMesh.BRASS, r * 1.07f * appear, -.02f, .1f, 40, BossMesh.argb(0x9A7A50, 1), light);
        // The snowy floor inside
        BossMesh.annulus(BossMesh.ground(buffer, SNOW), poseStack.last(), new Vector3f(0, floor + .01f, 0), new Vector3f(1, 0, 0), new Vector3f(0, 0, -1), .01f,
                r * appear, 32, 2, 0, 0, BossMesh.argb(WHITE, fade), BossMesh.argb(0xE0ECF8, fade), light, false);
        Vector3f centre = new Vector3f(0, floor, 0);
        if (!shattered) {
            // The glass dome: faint, its sheen turning, a bright rim where it meets the base
            BossMesh.sphere(BossMesh.see(buffer, BossMesh.SHEEN), poseStack.last(), centre, r * appear, 12, 28, age * .002f, 0, .5f,
                    BossMesh.argb(0xD8F0FF, .28f), LightTexture.FULL_BRIGHT);
            BossMesh.sphere(BossMesh.light(buffer, BossMesh.GLOW), poseStack.last(), centre, r * appear * 1.01f, 8, 24, 0, 0, .5f, BossMesh.glow(BLUE, .08f),
                    LightTexture.FULL_BRIGHT);
            ClockMesh.ring(poseStack.last(), buffer, floor + .02f, r * .9f * appear, r * appear, FROST, .6f, LightTexture.FULL_BRIGHT);
            // Snow whirling round and falling inside it
            VertexConsumer flakes = BossMesh.light(buffer, FLAKE);
            for (int k = 0; k < 46; k++) {
                float h1 = BossMesh.hash(seed + k), h2 = BossMesh.hash(seed + k * 3), h3 = BossMesh.hash(seed + k * 7);
                float a = h1 * Mth.TWO_PI + age * (.04f + .05f * h2);
                float rr = r * .82f * Mth.sqrt(h2);
                float fall = 1 - ((h3 + age * (.008f + .006f * h1)) % 1);
                float top = Mth.sqrt(Math.max(0, r * r * .7f - rr * rr));
                Vector3f p = new Vector3f(Mth.cos(a) * rr, floor + .1f + fall * top, Mth.sin(a) * rr);
                RitualMesh.billboard(flakes, poseStack, view, p, .12f + .1f * h3, age * .06f + k, -1, BossMesh.glow(WHITE, .85f * appear * fade),
                        LightTexture.FULL_BRIGHT);
            }
            ClockMesh.glow(poseStack, buffer, view, new Vector3f(0, floor + r * .4f, 0), r * 2.2f, FROST, .18f * appear, LightTexture.FULL_BRIGHT);
        } else if (st < 1) {
            // Shattered: shards of glass flung out, powder bursting
            VertexConsumer shards = BossMesh.light(buffer, BossMesh.SHEEN);
            float out = 1 - (1 - st) * (1 - st);
            for (int k = 0; k < 26; k++) {
                float a = BossMesh.hash(seed + k) * Mth.TWO_PI, b = BossMesh.hash(seed + k * 5) * Mth.HALF_PI;
                Vector3f dir = new Vector3f(Mth.cos(b) * Mth.cos(a), Mth.sin(b), Mth.cos(b) * Mth.sin(a));
                Vector3f p = new Vector3f(dir).mul(r * (1 + 1.6f * out)).add(centre).add(0, -2.5f * st * st, 0);
                RitualMesh.crystal(shards, poseStack.last(), p, new Vector3f(dir.z, dir.y + .3f, -dir.x).normalize(), .45f, .1f, st * 6 + k,
                        BossMesh.glow(FROST, 1 - st), LightTexture.FULL_BRIGHT);
            }
            powder(poseStack, buffer, view, new Vector3f(0, floor + r * .5f, 0), r * .6f, st, 12, seed, light);
        }
    }

    // ---------------------------------------------------------------- Frost Glide

    private void trail(SnowScene e, PoseStack poseStack, MultiBufferSource buffer, WaterMesh.View view, float age, int seed, int light) {
        float length = e.getSize() * BossMesh.ramp(age, 0, SnowScene.GLIDE_TIME + 2);
        float fade = 1 - BossMesh.ramp(age, e.getLife() - 30, e.getLife());
        if (length < .05f || fade <= 0) {
            return;
        }
        Vec3 fw = e.forward();
        Vector3f f = v(fw), side = new Vector3f(f.z, 0, -f.x);
        float w = .8f;
        int segs = 24;
        VertexConsumer ice = BossMesh.ground(buffer, ICE);
        for (int i = 0; i < segs; i++) {
            float t0 = (float) i / segs, t1 = (float) (i + 1) / segs;
            // Its edges waver a little, like ice grown, not cut
            float w0 = w * (.85f + .25f * BossMesh.hash(seed + i)), w1 = w * (.85f + .25f * BossMesh.hash(seed + i + 1));
            Vector3f c0 = new Vector3f(f).mul(t0 * length).add(0, .04f, 0), c1 = new Vector3f(f).mul(t1 * length).add(0, .04f, 0);
            float a0 = fade * (i == 0 ? .5f : .82f), a1 = fade * .82f;
            BossMesh.quad(ice, poseStack.last(), new Vector3f(c0).sub(new Vector3f(side).mul(w0)), new Vector3f(c0).add(new Vector3f(side).mul(w0)),
                    new Vector3f(c1).add(new Vector3f(side).mul(w1)), new Vector3f(c1).sub(new Vector3f(side).mul(w1)), 0, t0 * length * .5f, 1, t0 * length * .5f,
                    1, t1 * length * .5f, 0, t1 * length * .5f, BossMesh.argb(0xE0F4FF, a0), BossMesh.argb(0xE0F4FF, a1), light, BossMesh.UP);
        }
        // Crystals grown along both edges, tilted out
        VertexConsumer crystals = BossMesh.see(buffer, ICE);
        int n = Math.max(1, (int) (length / .7f));
        for (int k = 0; k < n; k++) {
            for (int s = -1; s <= 1; s += 2) {
                float t = (k + .5f) / n;
                float h = BossMesh.hash(seed + k * 13 + s);
                Vector3f base = new Vector3f(f).mul(t * length).add(new Vector3f(side).mul(s * w * (.9f + .2f * h))).add(0, .02f, 0);
                Vector3f dir = new Vector3f(side).mul(s * .45f).add(new Vector3f(f).mul((h - .5f) * .4f)).add(0, 1, 0).normalize();
                float grow = BossMesh.ramp(age, t * SnowScene.GLIDE_TIME, t * SnowScene.GLIDE_TIME + 6);
                RitualMesh.crystal(crystals, poseStack.last(), base, dir, (.22f + .3f * h) * grow, .06f + .04f * h, h * 3, BossMesh.argb(0xD8F0FF, .9f * fade), light);
            }
        }
        // Frost twinkling on it, a cold glow along it
        VertexConsumer flakes = BossMesh.light(buffer, FLAKE);
        for (int k = 0; k < 18; k++) {
            float t = BossMesh.hash(seed + k * 17), across = BossMesh.hash(seed + k * 19) * 2 - 1;
            float tw = Math.max(0, Mth.sin(age * .25f + k * 1.7f));
            Vector3f p = new Vector3f(f).mul(t * length).add(new Vector3f(side).mul(across * w * .8f)).add(0, .15f, 0);
            RitualMesh.billboard(flakes, poseStack, view, p, .22f * tw + .05f, age * .03f + k, -1, BossMesh.glow(WHITE, tw * fade), LightTexture.FULL_BRIGHT);
        }
        for (int k = 0; k <= 4; k++) {
            ClockMesh.glow(poseStack, buffer, view, new Vector3f(f).mul(length * k / 4f).add(0, .1f, 0), 2.2f, BLUE, .12f * fade, LightTexture.FULL_BRIGHT);
        }
    }

    // ---------------------------------------------------------------- Hoarfrost Crown

    private void crown(SnowScene e, PoseStack poseStack, MultiBufferSource buffer, WaterMesh.View view, float age, float pt, int seed) {
        int full = LightTexture.FULL_BRIGHT;
        float appear = BossMesh.ramp(age, 0, 10);
        float fade = 1 - BossMesh.ramp(age, e.getLife() - 15, e.getLife());
        float shown = appear * fade;
        if (shown <= .01f) {
            return;
        }
        VertexConsumer ice = BossMesh.see(buffer, ICE);
        VertexConsumer gleam = BossMesh.light(buffer, BossMesh.SHEEN);
        int points = 7;
        float spin = age * .045f;
        for (int k = 0; k < points; k++) {
            float a = spin + k * Mth.TWO_PI / points;
            float bob = .05f * Mth.sin(age * .12f + k);
            Vector3f base = new Vector3f(Mth.cos(a) * .42f, bob, Mth.sin(a) * .42f);
            Vector3f dir = new Vector3f(Mth.cos(a) * .3f, 1, Mth.sin(a) * .3f).normalize();
            float len = (k % 2 == 0 ? .5f : .34f) * shown;
            RitualMesh.crystal(ice, poseStack.last(), base, dir, len, .075f, a, BossMesh.argb(0xE6F6FF, .95f), full);
            RitualMesh.crystal(gleam, poseStack.last(), base, dir, len * 1.05f, .08f, a, BossMesh.glow(BLUE, .35f), full);
        }
        ClockMesh.ring(poseStack.last(), buffer, 0, .3f, .5f, FROST, .45f * shown, full);
        ClockMesh.glow(poseStack, buffer, view, new Vector3f(0, .2f, 0), 1.4f, BLUE, .3f * shown, full);
        // An icicle loosed: it streaks to its mark, then bursts there
        Entity victim = e.victim();
        float since = e.sinceStruck(pt);
        if (victim != null && since >= 0 && since < 12) {
            Vec3 to3 = victim.getPosition(pt).add(0, victim.getBbHeight() * .6, 0).subtract(e.getPosition(pt));
            Vector3f to = v(to3);
            float fly = Math.min(1, since / 4f);
            if (fly < 1) {
                Vector3f head = new Vector3f(to).mul(fly), tail = new Vector3f(to).mul(Math.max(0, fly - .35f));
                WaterMesh.streak(BossMesh.light(buffer, WaterMesh.STREAK), poseStack.last(), view.eye(), head, tail, .18f, BossMesh.glow(FROST, .9f), full);
                Vector3f d = new Vector3f(to).normalize();
                RitualMesh.crystal(ice, poseStack.last(), new Vector3f(head).sub(new Vector3f(d).mul(.5f)), d, .55f, .07f, age, BossMesh.argb(0xF0FAFF, 1), full);
            } else {
                float t = (since - 4) / 8f;
                ClockMesh.glow(poseStack, buffer, view, to, 1.6f * (1 + t), FROST, .8f * (1 - t), full);
                VertexConsumer flakes = BossMesh.light(buffer, FLAKE);
                for (int k = 0; k < 8; k++) {
                    float a = k * Mth.TWO_PI / 8;
                    Vector3f p = new Vector3f(Mth.cos(a), Mth.sin(a * 2) * .5f, Mth.sin(a)).mul(.8f * t).add(to);
                    RitualMesh.billboard(flakes, poseStack, view, p, .22f, t * 3 + k, -1, BossMesh.glow(WHITE, 1 - t), full);
                }
            }
        }
    }

    // ---------------------------------------------------------------- Whiteout

    private void whiteout(SnowScene e, PoseStack poseStack, MultiBufferSource buffer, WaterMesh.View view, float age, float pt, int seed, int light) {
        int full = LightTexture.FULL_BRIGHT;
        float r = e.getSize();
        float appear = BossMesh.ramp(age, 0, 25);
        float fade = 1 - BossMesh.ramp(age, e.getLife() - 20, e.getLife());
        float shown = appear * fade;
        // Frost spreading over the ground
        BossMesh.annulus(BossMesh.ground(buffer, ICE), poseStack.last(), new Vector3f(0, .05f, 0), new Vector3f(1, 0, 0), new Vector3f(0, 0, -1), .01f,
                r * appear, 56, 6, 0, 0, BossMesh.argb(0xEAF6FF, .55f * fade), BossMesh.argb(0xEAF6FF, .2f * fade), light, false);
        // The wall of blizzard, whirling round in three tiers, each its own way
        VertexConsumer cloud = BossMesh.see(buffer, BossMesh.CLOUD);
        for (int tier = 0; tier < 3; tier++) {
            int count = 30;
            float way = tier % 2 == 0 ? 1 : -1;
            for (int k = 0; k < count; k++) {
                float a = k * Mth.TWO_PI / count + age * .035f * way * (1 + tier * .25f) + tier;
                float rr = r * (.9f + .07f * tier) + .4f * Mth.sin(age * .07f + k);
                float y = .4f + tier * 1.35f + .35f * Mth.sin(age * .05f + k * 1.3f);
                RitualMesh.billboard(cloud, poseStack, view, new Vector3f(Mth.cos(a) * rr, y, Mth.sin(a) * rr), 3.2f - tier * .3f, a + age * .01f, -1,
                        BossMesh.argb(WHITE, .5f * shown), light);
            }
        }
        // Flurries racing round inside it
        VertexConsumer streaks = BossMesh.light(buffer, WaterMesh.STREAK);
        for (int k = 0; k < 70; k++) {
            float h1 = BossMesh.hash(seed + k), h2 = BossMesh.hash(seed + k * 3), h3 = BossMesh.hash(seed + k * 7);
            float a = h1 * Mth.TWO_PI + age * (.05f + .05f * h2);
            float rr = r * (.15f + .8f * h2), y = .2f + h3 * 3.6f;
            Vector3f head = new Vector3f(Mth.cos(a) * rr, y, Mth.sin(a) * rr);
            Vector3f tangent = new Vector3f(-Mth.sin(a), -.08f, Mth.cos(a)).mul(.7f + .6f * h3);
            WaterMesh.streak(streaks, poseStack.last(), view.eye(), head, new Vector3f(head).sub(tangent), .05f, BossMesh.glow(WHITE, .65f * shown), full);
        }
        ClockMesh.glow(poseStack, buffer, view, new Vector3f(0, 1.5f, 0), r * 1.6f, FROST, .12f * shown, full);
        // The frozen shatter: a ring of cold running out, spikes of ice bursting from the ground
        if (e.hasBurst()) {
            float t = e.sinceBurst(pt) / 20f;
            if (t < 1) {
                ClockMesh.ring(poseStack.last(), buffer, .08f, r * t, r * t + .8f, FROST, 1 - t, full);
                ClockMesh.glow(poseStack, buffer, view, new Vector3f(0, 1, 0), r * 2 * (1 - t * .5f), BLUE, .6f * (1 - t), full);
                VertexConsumer ice = BossMesh.light(buffer, BossMesh.SHEEN);
                float rise = Mth.sin(Mth.PI * Math.min(1, t * 1.5f));
                for (int k = 0; k < 24; k++) {
                    float a = BossMesh.hash(seed + k * 23) * Mth.TWO_PI, rr = r * (.3f + .65f * BossMesh.hash(seed + k * 29));
                    Vector3f base = new Vector3f(Mth.cos(a) * rr, 0, Mth.sin(a) * rr);
                    Vector3f dir = new Vector3f(Mth.cos(a) * .3f, 1, Mth.sin(a) * .3f).normalize();
                    RitualMesh.crystal(ice, poseStack.last(), base, dir, (1 + BossMesh.hash(seed + k) * 1.4f) * rise, .18f, a, BossMesh.glow(FROST, .9f * (1 - t)),
                            full);
                }
            }
        }
    }

    @Override
    public boolean shouldRender(SnowScene e, Frustum frustum, double x, double y, double z) {
        return true;
    }

    @Override
    public ResourceLocation getTextureLocation(SnowScene e) {
        return SNOW;
    }
}
