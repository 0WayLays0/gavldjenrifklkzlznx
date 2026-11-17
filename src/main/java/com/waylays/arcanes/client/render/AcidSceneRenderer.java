package com.waylays.arcanes.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.waylays.arcanes.ArcanesOublies;
import com.waylays.arcanes.client.render.boss.BossMesh;
import com.waylays.arcanes.client.render.clockwork.ClockMesh;
import com.waylays.arcanes.client.render.ritual.RitualMesh;
import com.waylays.arcanes.client.render.water.WaterMesh;
import com.waylays.arcanes.entity.acid.AcidScene;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/**
 * Acid Magic's spells, drawn: a fan of glowing green drops sprayed ahead, streaking and fuming where they fall; a pool
 * of acid that slowly swirls, its bubbles swelling and bursting, green fumes curling up from it; a sickly cloud
 * raining streaks of acid that splash in small rings on a puddle below; a wobbling bubble of acid round an enemy, its
 * own bubbles rising inside, that bursts in a splash; a ring-shaped wave of acid rolling out round the caster, foam on
 * its crest, the ground behind it wet and fuming.
 */
public class AcidSceneRenderer extends EntityRenderer<AcidScene> {
    public static final ResourceLocation ACID = ArcanesOublies.id("textures/entity/acid/acid.png");
    public static final ResourceLocation FROTH = ArcanesOublies.id("textures/entity/acid/froth.png");
    private static final int BRIGHT = 0xD8FF4A, GREEN = 0x8CF02A, DEEP = 0x3C9A14, FUME = 0x9ACC3A, CLOUD = 0x4C5A2E;
    private static final int FULL = LightTexture.FULL_BRIGHT;
    private static final float GRAVITY = .04f;

    public AcidSceneRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(AcidScene e, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource out, int packedLight) {
        // Recorded per render type, then written out one type after another (see SceneBuffers)
        SceneBuffers buffer = SceneBuffers.begin();
        float age = e.tickCount + partialTicks;
        WaterMesh.View view = WaterViews.of(this.entityRenderDispatcher, e, partialTicks);
        int seed = e.getId() * 37;
        switch (e.getKind()) {
            case SPRAY -> spray(e, poseStack, buffer, view, age, seed);
            case POOL -> pool(e, poseStack, buffer, view, age, seed, packedLight);
            case RAIN -> rain(e, poseStack, buffer, view, age, seed, packedLight);
            case DISSOLVE -> dissolve(e, poseStack, buffer, view, age, partialTicks, seed, packedLight);
            case TIDE -> tide(e, poseStack, buffer, view, age, seed, packedLight);
        }
        buffer.flush(out);
        super.render(e, entityYaw, partialTicks, poseStack, out, packedLight);
    }

    private static Vector3f v(Vec3 d) {
        return new Vector3f((float) d.x, (float) d.y, (float) d.z);
    }

    /** A glowing drop of acid at {@code at}, flying from {@code from}: its streak, its halo. */
    private static void drop(VertexConsumer streaks, PoseStack poseStack, WaterMesh.View view, Vector3f at, Vector3f from, float width, float alpha) {
        WaterMesh.streak(streaks, poseStack.last(), view.eye(), at, from, width * 2.2f, BossMesh.glow(GREEN, .45f * alpha), FULL);
        WaterMesh.streak(streaks, poseStack.last(), view.eye(), at, from, width, BossMesh.glow(BRIGHT, alpha), FULL);
    }

    /** A puddle of acid lying flat, {@code r} across, slowly swirling, a rim of froth; bubbles swell and burst on it. */
    private static void puddle(PoseStack poseStack, MultiBufferSource buffer, WaterMesh.View view, float r, float age, float alpha, int bubbles, int seed,
                               int light) {
        if (alpha <= .01f || r <= .02f) {
            return;
        }
        BossMesh.annulus(BossMesh.ground(buffer, ACID), poseStack.last(), new Vector3f(0, .04f, 0), new Vector3f(1, 0, 0), new Vector3f(0, 0, -1), .01f, r, 40,
                Math.max(1, r * .6f), age * .003f, age * .005f, BossMesh.argb(0xFFFFFF, .9f * alpha), BossMesh.argb(0xE8FFD0, .75f * alpha), FULL, false);
        BossMesh.annulus(BossMesh.light(buffer, BossMesh.NOISE), poseStack.last(), new Vector3f(0, .05f, 0), new Vector3f(1, 0, 0), new Vector3f(0, 0, -1), .01f,
                r, 40, 2, -age * .004f, age * .008f, BossMesh.glow(GREEN, .25f * alpha), BossMesh.glow(BRIGHT, .1f * alpha), FULL, false);
        BossMesh.annulus(BossMesh.ground(buffer, FROTH), poseStack.last(), new Vector3f(0, .055f, 0), new Vector3f(1, 0, 0), new Vector3f(0, 0, -1), r * .86f,
                r * 1.04f, 40, Math.max(2, r * 2), age * .002f, 0, BossMesh.argb(0xFFFFFF, 0), BossMesh.argb(0xFFFFFF, .85f * alpha), light, false);
        VertexConsumer skin = BossMesh.see(buffer, WaterMesh.BUBBLE);
        for (int k = 0; k < bubbles; k++) {
            float cycle = 18 + 14 * BossMesh.hash(seed + k);
            float t = ((age + BossMesh.hash(seed + k * 3) * cycle) % cycle) / cycle;
            float a = BossMesh.hash(seed + k * 5) * Mth.TWO_PI, rr = r * .8f * Mth.sqrt(BossMesh.hash(seed + k * 7));
            Vector3f at = new Vector3f(Mth.cos(a) * rr, .04f, Mth.sin(a) * rr);
            if (t < .85f) {
                float size = (.06f + .16f * BossMesh.hash(seed + k * 11)) * (.3f + t);
                BossMesh.sphere(skin, poseStack.last(), at, size, 5, 8, age * .02f, 0, .5f, BossMesh.argb(0xC8FF90, .8f * alpha), FULL);
            } else {
                float pop = (t - .85f) / .15f;
                ClockMesh.ring(poseStack.last(), buffer, .07f, .1f + pop * .35f, .15f + pop * .4f, BRIGHT, (1 - pop) * alpha, FULL);
                ClockMesh.glow(poseStack, buffer, view, new Vector3f(at).add(0, .1f, 0), .6f, BRIGHT, (1 - pop) * .7f * alpha, FULL);
            }
        }
        ClockMesh.glow(poseStack, buffer, view, new Vector3f(0, .25f, 0), r * 2.6f, GREEN, .3f * alpha, FULL);
    }

    /** Green fumes curling up from a spot. */
    private static void fumes(PoseStack poseStack, MultiBufferSource buffer, WaterMesh.View view, Vector3f at, float reach, float size, int count, float age,
                              float alpha, int seed, int light) {
        ClockMesh.smoke(poseStack, buffer, view, at, new Vector3f(0, 1, 0), reach, size, count, age, .018f, alpha * .55f, FUME, seed, light);
    }

    // ---------------------------------------------------------------- Acid Spray

    private void spray(AcidScene e, PoseStack poseStack, MultiBufferSource buffer, WaterMesh.View view, float age, int seed) {
        Vector3f aim = v(e.aim());
        Vector3f side = new Vector3f(aim).cross(WaterMesh.UP);
        if (side.lengthSquared() < 1.0E-4f) {
            side.set(1, 0, 0);
        }
        side.normalize();
        Vector3f up = new Vector3f(side).cross(aim).normalize();
        float reach = e.getSize();
        float speed = reach / 9f;
        boolean spraying = age < AcidScene.SPRAY_TIME;
        float nozzle = spraying ? 1 : Math.max(0, 1 - (age - AcidScene.SPRAY_TIME) / 6f);
        VertexConsumer streaks = BossMesh.light(buffer, WaterMesh.STREAK);
        // Drops flying out in a fan, each launched at its own tick while the spray lasts
        for (int k = 0; k < 64; k++) {
            float born = BossMesh.hash(seed + k) * AcidScene.SPRAY_TIME;
            float t = age - born;
            if (t < 0 || t > 12) {
                continue;
            }
            float ax = (BossMesh.hash(seed + k * 3) * 2 - 1) * AcidScene.SPRAY_HALF, ay = (BossMesh.hash(seed + k * 5) * 2 - 1) * AcidScene.SPRAY_HALF * .55f;
            Vector3f dir = new Vector3f(aim).add(new Vector3f(side).mul(ax)).add(new Vector3f(up).mul(ay)).normalize();
            Vector3f vel = dir.mul(speed * (.8f + .4f * BossMesh.hash(seed + k * 7)));
            Vector3f at = WaterMesh.thrown(new Vector3f(), vel, t, GRAVITY);
            Vector3f from = WaterMesh.thrown(new Vector3f(), vel, Math.max(0, t - 1.6f), GRAVITY);
            float alpha = 1 - BossMesh.ramp(t, 8, 12);
            drop(streaks, poseStack, view, at, from, .07f + .05f * BossMesh.hash(seed + k * 11), alpha);
            if (t > 9 && k % 4 == 0) {
                fumes(poseStack, buffer, view, at, .8f, .35f, 2, age, alpha, seed + k, FULL);
            }
        }
        // A misty cone of fine droplets over the drops
        VertexConsumer mist = BossMesh.see(buffer, WaterMesh.MIST);
        for (int k = 0; k < 12; k++) {
            float t = ((age * .09f + BossMesh.hash(seed + k * 13)) % 1);
            float ax = (BossMesh.hash(seed + k * 17) * 2 - 1) * AcidScene.SPRAY_HALF * .8f;
            Vector3f at = new Vector3f(aim).add(new Vector3f(side).mul(ax)).normalize().mul(reach * t);
            RitualMesh.billboard(mist, poseStack, view, at, .3f + 1.4f * t, k + age * .03f, -1, BossMesh.argb(0xC8FF80, .4f * nozzle * (1 - t)), FULL);
        }
        ClockMesh.glow(poseStack, buffer, view, new Vector3f(aim).mul(.4f), 1.6f, BRIGHT, .7f * nozzle, FULL);
        ClockMesh.glow(poseStack, buffer, view, new Vector3f(aim).mul(reach * .5f), reach * 1.2f, GREEN, .18f * nozzle, FULL);
    }

    // ---------------------------------------------------------------- Corrosive Pool

    private void pool(AcidScene e, PoseStack poseStack, MultiBufferSource buffer, WaterMesh.View view, float age, int seed, int light) {
        float spread = BossMesh.ramp(age, 0, 8);
        float fade = 1 - BossMesh.ramp(age, e.getLife() - 20, e.getLife());
        float r = e.getSize() * (.4f + .6f * spread);
        puddle(poseStack, buffer, view, r, age, fade, 10, seed, light);
        // The splash it landed with
        if (age < 12) {
            float t = age / 12f;
            VertexConsumer streaks = BossMesh.light(buffer, WaterMesh.STREAK);
            for (int k = 0; k < 18; k++) {
                float a = k * Mth.TWO_PI / 18 + BossMesh.hash(seed + k);
                Vector3f vel = new Vector3f(Mth.cos(a) * .18f, .3f + .15f * BossMesh.hash(seed + k * 3), Mth.sin(a) * .18f);
                drop(streaks, poseStack, view, WaterMesh.thrown(new Vector3f(), vel, age, GRAVITY * 1.5f),
                        WaterMesh.thrown(new Vector3f(), vel, Math.max(0, age - 1.5f), GRAVITY * 1.5f), .08f, 1 - t);
            }
        }
        for (int k = 0; k < 4; k++) {
            float a = k * Mth.TWO_PI / 4 + age * .01f;
            fumes(poseStack, buffer, view, new Vector3f(Mth.cos(a) * r * .5f, .1f, Mth.sin(a) * r * .5f), 2.2f, .6f, 3, age, fade, seed + k * 7, light);
        }
        float bite = Math.max(0, 1 - e.sinceStruck(age - e.tickCount) / 8f);
        ClockMesh.glow(poseStack, buffer, view, new Vector3f(0, .4f, 0), r * 2, BRIGHT, .35f * bite * fade, FULL);
    }

    // ---------------------------------------------------------------- Acid Rain

    private void rain(AcidScene e, PoseStack poseStack, MultiBufferSource buffer, WaterMesh.View view, float age, int seed, int light) {
        float r = e.getSize();
        float h = AcidScene.RAIN_HEIGHT;
        float gather = BossMesh.ramp(age, 0, 15);
        float fade = 1 - BossMesh.ramp(age, e.getLife() - 20, e.getLife());
        float shown = gather * fade;
        // The cloud: dark sickly puffs churning, lit green from within now and then
        VertexConsumer puffs = BossMesh.see(buffer, BossMesh.CLOUD);
        for (int k = 0; k < 22; k++) {
            float a = BossMesh.hash(seed + k) * Mth.TWO_PI + age * .004f * (k % 2 == 0 ? 1 : -1);
            float rr = r * Mth.sqrt(BossMesh.hash(seed + k * 3)) * (.6f + .4f * gather);
            float y = h + (BossMesh.hash(seed + k * 5) - .3f) * .9f + .15f * Mth.sin(age * .05f + k);
            int col = BossMesh.mix(CLOUD, 0x2A3418, BossMesh.hash(seed + k * 7));
            RitualMesh.billboard(puffs, poseStack, view, new Vector3f(Mth.cos(a) * rr, y, Mth.sin(a) * rr), 1.6f + 1.2f * BossMesh.hash(seed + k * 11),
                    k + age * .005f, -1, BossMesh.argb(col, .85f * shown), light);
        }
        int flash = Mth.floor(age / 9);
        float flicker = BossMesh.hash(seed + flash * 13) > .55f ? 1 - ((age % 9) / 9f) : 0;
        float fa = BossMesh.hash(seed + flash * 17) * Mth.TWO_PI, fr = r * .6f * BossMesh.hash(seed + flash * 19);
        ClockMesh.glow(poseStack, buffer, view, new Vector3f(Mth.cos(fa) * fr, h, Mth.sin(fa) * fr), r * 2.2f, GREEN, (.15f + .5f * flicker) * shown, FULL);
        // The rain: streaks falling, each splashing in a small ring where it lands
        VertexConsumer streaks = BossMesh.light(buffer, WaterMesh.STREAK);
        float fall = h / 8f;
        for (int k = 0; k < 48; k++) {
            float cycle = 10;
            float t = (age + BossMesh.hash(seed + k * 23) * cycle) % cycle;
            int round = Mth.floor((age + BossMesh.hash(seed + k * 23) * cycle) / cycle);
            float a = BossMesh.hash(seed + k * 29 + round * 7) * Mth.TWO_PI, rr = r * Mth.sqrt(BossMesh.hash(seed + k * 31 + round * 11));
            float x = Mth.cos(a) * rr, z = Mth.sin(a) * rr;
            if (t < 8) {
                float y = h - .4f - fall * t;
                drop(streaks, poseStack, view, new Vector3f(x, y, z), new Vector3f(x, y + .9f, z), .05f, .9f * shown);
            } else {
                float s = (t - 8) / 2f;
                poseStack.pushPose();
                poseStack.translate(x, 0, z);
                ClockMesh.ring(poseStack.last(), buffer, .07f, .05f + s * .3f, .1f + s * .35f, BRIGHT, (1 - s) * shown, FULL);
                poseStack.popPose();
            }
        }
        puddle(poseStack, buffer, view, r * .9f * gather, age, .7f * shown, 6, seed + 3, light);
        fumes(poseStack, buffer, view, new Vector3f(0, .1f, 0), 2, .8f, 5, age, shown, seed, light);
    }

    // ---------------------------------------------------------------- Dissolve

    private void dissolve(AcidScene e, PoseStack poseStack, MultiBufferSource buffer, WaterMesh.View view, float age, float pt, int seed, int light) {
        Entity victim = e.victim();
        float height = victim != null ? victim.getBbHeight() : 1.6f;
        float r = e.getSize();
        int melt = e.getLife() - 12;
        float centreY = height * .5f;
        if (age < melt) {
            // The bubble: closing round the enemy, wobbling, its surface slowly flowing, bubbles rising within
            float close = BossMesh.ramp(age, 0, 10);
            float wob = .06f * Mth.sin(age * .35f);
            float rr = r * (1.6f - .6f * close);
            poseStack.pushPose();
            poseStack.translate(0, centreY, 0);
            poseStack.scale(1 + wob, (height * .5f + .3f) / r * (1 - wob), 1 + wob);
            BossMesh.sphere(BossMesh.see(buffer, ACID), poseStack.last(), new Vector3f(), rr, 12, 20, age * .004f, 0, 1, BossMesh.argb(0xFFFFFF, .55f * close), FULL);
            BossMesh.sphere(BossMesh.light(buffer, BossMesh.NOISE), poseStack.last(), new Vector3f(), rr * 1.03f, 10, 18, -age * .008f, 0, 1,
                    BossMesh.glow(GREEN, .3f * close), FULL);
            poseStack.popPose();
            VertexConsumer skin = BossMesh.see(buffer, WaterMesh.BUBBLE);
            for (int k = 0; k < 12; k++) {
                float t = ((age * .03f + BossMesh.hash(seed + k)) % 1);
                float a = BossMesh.hash(seed + k * 3) * Mth.TWO_PI + age * .05f;
                float rad = r * .7f * BossMesh.hash(seed + k * 5);
                Vector3f at = new Vector3f(Mth.cos(a) * rad, height * t * 1.1f, Mth.sin(a) * rad);
                BossMesh.sphere(skin, poseStack.last(), at, .05f + .07f * BossMesh.hash(seed + k * 7), 4, 8, 0, 0, 1, BossMesh.argb(0xD8FFA0, .8f * close * (1 - t)),
                        FULL);
            }
            float bite = Math.max(0, 1 - ((age - 1) % 20) / 8f);
            ClockMesh.glow(poseStack, buffer, view, new Vector3f(0, centreY, 0), r * 3.4f, GREEN, (.2f + .3f * bite) * close, FULL);
            fumes(poseStack, buffer, view, new Vector3f(0, height + .2f, 0), 1.6f, .5f, 4, age, close, seed, light);
            puddle(poseStack, buffer, view, r * .9f * close, age, .6f * close, 3, seed, light);
            return;
        }
        // The burst: the bubble splashes out, drops flung in every way, a puddle left that fades
        float since = age - melt;
        float t = since / 12f;
        VertexConsumer streaks = BossMesh.light(buffer, WaterMesh.STREAK);
        for (int k = 0; k < 40; k++) {
            float a = BossMesh.hash(seed + k) * Mth.TWO_PI;
            float up = BossMesh.hash(seed + k * 3) * 2 - .5f;
            Vector3f vel = new Vector3f(Mth.cos(a), up * .6f, Mth.sin(a)).normalize().mul(.22f + .12f * BossMesh.hash(seed + k * 5));
            Vector3f origin = new Vector3f(0, centreY, 0);
            drop(streaks, poseStack, view, WaterMesh.thrown(origin, vel, since, GRAVITY), WaterMesh.thrown(origin, vel, Math.max(0, since - 1.5f), GRAVITY),
                    .09f, 1 - t);
        }
        if (since < 4) {
            BossMesh.sphere(BossMesh.see(buffer, ACID), poseStack.last(), new Vector3f(0, centreY, 0), r * (1 + since * .25f), 10, 16, 0, 0, 1,
                    BossMesh.argb(0xFFFFFF, .5f * (1 - since / 4)), FULL);
        }
        ClockMesh.glow(poseStack, buffer, view, new Vector3f(0, centreY, 0), r * 5, BRIGHT, .9f * (1 - t), FULL);
        ClockMesh.ring(poseStack.last(), buffer, .08f, r * 2.4f * t, r * 2.4f * t + .5f, BRIGHT, 1 - t, FULL);
        puddle(poseStack, buffer, view, r * 1.4f, age, 1 - t, 4, seed, light);
        fumes(poseStack, buffer, view, new Vector3f(0, .2f, 0), 2.5f, .9f, 6, age, 1 - t, seed, light);
    }

    // ---------------------------------------------------------------- Corroding Tide

    private void tide(AcidScene e, PoseStack poseStack, MultiBufferSource buffer, WaterMesh.View view, float age, int seed, int light) {
        float reach = e.getSize();
        float roll = BossMesh.ramp(age, 0, AcidScene.TIDE_ROLL);
        float front = reach * roll;
        float wave = 1 - BossMesh.ramp(age, AcidScene.TIDE_ROLL - 6, AcidScene.TIDE_ROLL + 4);
        float fade = 1 - BossMesh.ramp(age, e.getLife() - 18, e.getLife());
        // The ground behind the wave: wet with acid, fuming
        if (front > .3f) {
            BossMesh.annulus(BossMesh.ground(buffer, ACID), poseStack.last(), new Vector3f(0, .04f, 0), new Vector3f(1, 0, 0), new Vector3f(0, 0, -1), .01f,
                    front, 64, Math.max(1, front * .5f), age * .002f, age * .004f, BossMesh.argb(0xFFFFFF, .25f * fade), BossMesh.argb(0xE8FFD0, .6f * fade), FULL,
                    false);
        }
        // The wave: a ring of acid, its back sloping up to a crest that curls over its front, foam riding on top
        if (wave > .01f && front > .2f) {
            float h = (.6f + 1.1f * Mth.sin(Mth.PI * Math.min(1, roll * 1.3f))) * wave;
            int segs = 72;
            VertexConsumer body = BossMesh.see(buffer, ACID);
            VertexConsumer foam = BossMesh.see(buffer, FROTH);
            float[] rs = {front - 2.2f, front - .9f, front - .2f, front + .15f, front + .35f};
            float[] ys = {.05f, h * .65f, h, h * .85f, .05f};
            for (int i = 0; i < segs; i++) {
                float a0 = i * Mth.TWO_PI / segs, a1 = (i + 1) * Mth.TWO_PI / segs;
                float sw0 = 1 + .12f * Mth.sin(a0 * 7 + age * .3f), sw1 = 1 + .12f * Mth.sin(a1 * 7 + age * .3f);
                float c0 = Mth.cos(a0), s0 = Mth.sin(a0), c1 = Mth.cos(a1), s1 = Mth.sin(a1);
                float u0 = a0 * front * .25f, u1 = a1 * front * .25f;
                for (int j = 0; j + 1 < rs.length; j++) {
                    float ra = Math.max(0, rs[j]), rb = Math.max(0, rs[j + 1]);
                    float ya0 = ys[j] * sw0, yb0 = ys[j + 1] * sw0, ya1 = ys[j] * sw1, yb1 = ys[j + 1] * sw1;
                    float va = j * .3f - age * .04f, vb = (j + 1) * .3f - age * .04f;
                    int ca = BossMesh.argb(BossMesh.mix(0x9AD060, 0xFFFFFF, ys[j] / Math.max(h, .01f)), .8f * wave);
                    int cb = BossMesh.argb(BossMesh.mix(0x9AD060, 0xFFFFFF, ys[j + 1] / Math.max(h, .01f)), .8f * wave);
                    BossMesh.quad2(body, poseStack.last(), new Vector3f(c0 * ra, ya0, s0 * ra), new Vector3f(c1 * ra, ya1, s1 * ra),
                            new Vector3f(c1 * rb, yb1, s1 * rb), new Vector3f(c0 * rb, yb0, s0 * rb), u0, va, u1, va, u1, vb, u0, vb, ca, cb, FULL);
                }
                // The foam on its crest
                float fr0 = front - .45f, fr1 = front + .25f;
                BossMesh.quad2(foam, poseStack.last(), new Vector3f(c0 * fr0, h * .92f * sw0 + .02f, s0 * fr0), new Vector3f(c1 * fr0, h * .92f * sw1 + .02f, s1 * fr0),
                        new Vector3f(c1 * fr1, h * .95f * sw1 + .02f, s1 * fr1), new Vector3f(c0 * fr1, h * .95f * sw0 + .02f, s0 * fr1), u0, 0, u1, 0, u1, 1, u0, 1,
                        BossMesh.argb(0xFFFFFF, .9f * wave), BossMesh.argb(0xFFFFFF, .9f * wave), FULL);
            }
            ClockMesh.ring(poseStack.last(), buffer, .1f, Math.max(0, front - 1.2f), front + .4f, BRIGHT, .5f * wave, FULL);
            // Spray flung off its crest
            VertexConsumer streaks = BossMesh.light(buffer, WaterMesh.STREAK);
            for (int k = 0; k < 36; k++) {
                float a = k * Mth.TWO_PI / 36 + BossMesh.hash(seed + k) * .2f;
                float t = ((age * .15f + BossMesh.hash(seed + k * 3)) % 1);
                Vector3f origin = new Vector3f(Mth.cos(a) * front, h, Mth.sin(a) * front);
                Vector3f vel = new Vector3f(Mth.cos(a) * .12f, .12f, Mth.sin(a) * .12f);
                drop(streaks, poseStack, view, WaterMesh.thrown(origin, vel, t * 8, GRAVITY), WaterMesh.thrown(origin, vel, Math.max(0, t * 8 - 1.5f), GRAVITY), .07f,
                        wave * (1 - t));
            }
            for (int k = 0; k < 8; k++) {
                float a = k * Mth.TWO_PI / 8 + .3f;
                ClockMesh.glow(poseStack, buffer, view, new Vector3f(Mth.cos(a) * front, h * .6f, Mth.sin(a) * front), 3, GREEN, .35f * wave, FULL);
            }
        }
        for (int k = 0; k < 6; k++) {
            float a = k * Mth.TWO_PI / 6 + seed;
            float rr = front * (.3f + .5f * BossMesh.hash(seed + k));
            fumes(poseStack, buffer, view, new Vector3f(Mth.cos(a) * rr, .1f, Mth.sin(a) * rr), 2.4f, .8f, 3, age, fade * Math.min(1, front / 3), seed + k * 5, light);
        }
        ClockMesh.glow(poseStack, buffer, view, new Vector3f(0, .5f, 0), 3 + front * 1.5f, GREEN, .3f * fade, FULL);
    }

    @Override
    public boolean shouldRender(AcidScene e, Frustum frustum, double x, double y, double z) {
        return true;
    }

    @Override
    public ResourceLocation getTextureLocation(AcidScene e) {
        return ACID;
    }
}
