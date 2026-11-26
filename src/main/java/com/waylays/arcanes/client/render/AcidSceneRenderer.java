package com.waylays.arcanes.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.waylays.arcanes.ArcanesOublies;
import com.waylays.arcanes.client.render.boss.BossMesh;
import com.waylays.arcanes.client.render.ritual.RitualMesh;
import com.waylays.arcanes.client.render.water.WaterMesh;
import com.waylays.arcanes.entity.acid.AcidScene;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import org.joml.Vector3f;

import static com.waylays.arcanes.client.render.ElementFx.FULL;

/**
 * Acid Magic's spells, drawn: a jet of glowing acid sprayed ahead, a fan of fat drops streaking out of it, hissing
 * splats where they fall; a glowing pool of acid that slowly swirls, its bubbles swelling and bursting, froth round
 * its rim; a sickly green cloud in body, flickering within, raining glowing streaks that splash
 * in small rings on a puddle below; a clear bubble of acid round an enemy, glassy at its rim, froth at its foot,
 * bubbles rising inside, that bursts in a splash; a low ring-shaped wave of acid rolling out round the caster, its
 * crest glowing and foaming, the ground behind it wet. No fumes, mist or haze: their see-through sheets hid the spells.
 */
public class AcidSceneRenderer extends EntityRenderer<AcidScene> {
    public static final ResourceLocation ACID = ArcanesOublies.id("textures/entity/acid/acid.png");
    public static final ResourceLocation FROTH = ArcanesOublies.id("textures/entity/acid/froth.png");
    private static final int BRIGHT = 0xD8FF4A, GREEN = 0x8CF02A, DEEP = 0x3C9A14, FUME = 0xB8E870;
    private static final float GRAVITY = .04f;

    public AcidSceneRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(AcidScene e, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource out, int packedLight) {
        // Recorded per render type, then written out one type after another (see SceneBuffers)
        SceneBuffers buffer = SceneBuffers.begin();
        // On top of snow layers or a carpet, if the scene stands in them
        poseStack.pushPose();
        poseStack.translate(0, ElementFx.floorLift(e), 0);
        float age = e.tickCount + partialTicks;
        WaterMesh.View view = WaterViews.of(this.entityRenderDispatcher, e, partialTicks);
        int seed = e.getId() * 37;
        int light = ElementFx.light(e, 13);
        switch (e.getKind()) {
            case SPRAY -> spray(e, poseStack, buffer, view, age, seed, light);
            case POOL -> pool(e, poseStack, buffer, view, age, seed, light);
            case RAIN -> rain(e, poseStack, buffer, view, age, seed, light);
            case DISSOLVE -> dissolve(e, poseStack, buffer, view, age, partialTicks, seed, light);
            case TIDE -> tide(e, poseStack, buffer, view, age, seed, light);
        }
        buffer.flush(out);
        poseStack.popPose();
        super.render(e, entityYaw, partialTicks, poseStack, out, packedLight);
    }

    /** A glowing drop of acid at {@code at}, flying from {@code from}: its green halo, its bright core. */
    private static void drop(VertexConsumer streaks, PoseStack poseStack, WaterMesh.View view, Vector3f at, Vector3f from, float width, float alpha) {
        drop(streaks, poseStack, view, at, from, width, alpha, 0xF4FFC0);
    }

    /** As {@link #drop}, its core {@code core} (the rain's is green, not white). */
    private static void drop(VertexConsumer streaks, PoseStack poseStack, WaterMesh.View view, Vector3f at, Vector3f from, float width, float alpha, int core) {
        if (alpha <= .01f) {
            return;
        }
        WaterMesh.streak(streaks, poseStack.last(), view.eye(), at, from, width * 2.6f, BossMesh.glow(GREEN, .5f * alpha), FULL);
        WaterMesh.streak(streaks, poseStack.last(), view.eye(), at, from, width, BossMesh.glow(core, alpha), FULL);
    }

    /** A splat of acid where a drop fell, {@code size} across, hissing: its stain, a ring of froth. */
    private static void splat(PoseStack poseStack, MultiBufferSource buffer, WaterMesh.View view, Vector3f at, float size, float age, float alpha, int seed) {
        if (alpha <= .01f) {
            return;
        }
        Vector3f u = new Vector3f(1, 0, 0), v = new Vector3f(0, 0, -1);
        BossMesh.annulus(BossMesh.ground(buffer, ACID), poseStack.last(), new Vector3f(at).add(0, .03f, 0), u, v, .01f, size, 14, 1, seed * .1f, age * .01f,
                BossMesh.argb(0xE8FFB0, .9f * alpha), BossMesh.argb(0xB8F070, .6f * alpha), FULL, false);
        BossMesh.annulus(BossMesh.ground(buffer, FROTH), poseStack.last(), new Vector3f(at).add(0, .04f, 0), u, v, size * .7f, size * 1.1f, 14, 1, seed * .3f, 0,
                BossMesh.argb(0xFFFFFF, 0), BossMesh.argb(0xF4FFE0, .8f * alpha), FULL, false);
        ElementFx.bloom(poseStack, buffer, view, new Vector3f(at).add(0, .1f, 0), size * .6f, GREEN, .5f * alpha);
    }

    /** A puddle of acid lying flat, {@code r} across, glowing and slowly swirling, a rim of froth; bubbles swell and burst on it. */
    private static void puddle(PoseStack poseStack, MultiBufferSource buffer, WaterMesh.View view, float r, float age, float alpha, int bubbles, int seed,
                               int light) {
        if (alpha <= .01f || r <= .02f) {
            return;
        }
        Vector3f u = new Vector3f(1, 0, 0), v = new Vector3f(0, 0, -1);
        // The ground round it eaten dark, the acid, a glow swirling on it, froth at its rim
        BossMesh.annulus(BossMesh.ground(buffer, ACID), poseStack.last(), new Vector3f(0, .03f, 0), u, v, r * .9f, r * 1.3f, 40, 2, 0, 0,
                BossMesh.argb(0x2A4010, .7f * alpha), BossMesh.argb(0x2A4010, 0), light, false);
        BossMesh.annulus(BossMesh.ground(buffer, ACID), poseStack.last(), new Vector3f(0, .045f, 0), u, v, .01f, r, 48, Math.max(1, r * .6f), age * .003f,
                age * .005f, BossMesh.argb(0xF0FFD0, .95f * alpha), BossMesh.argb(0xC8F090, .85f * alpha), FULL, false);
        BossMesh.annulus(BossMesh.light(buffer, BossMesh.NOISE), poseStack.last(), new Vector3f(0, .05f, 0), u, v, .01f, r, 48, 2, -age * .004f, age * .008f,
                BossMesh.glow(BRIGHT, .4f * alpha), BossMesh.glow(GREEN, .25f * alpha), FULL, false);
        BossMesh.annulus(BossMesh.ground(buffer, FROTH), poseStack.last(), new Vector3f(0, .06f, 0), u, v, r * .82f, r * 1.04f, 48, Math.max(2, r * 2), age * .002f,
                0, BossMesh.argb(0xFFFFFF, 0), BossMesh.argb(0xF4FFE0, .95f * alpha), light, false);
        ElementFx.groundRing(poseStack, buffer, .065f, r * .95f, r * 1.02f, BRIGHT, .6f * alpha);
        VertexConsumer skin = BossMesh.see(buffer, WaterMesh.BUBBLE);
        for (int k = 0; k < bubbles; k++) {
            float cycle = 18 + 14 * BossMesh.hash(seed + k);
            float t = ((age + BossMesh.hash(seed + k * 3) * cycle) % cycle) / cycle;
            float a = BossMesh.hash(seed + k * 5) * Mth.TWO_PI, rr = r * .8f * Mth.sqrt(BossMesh.hash(seed + k * 7));
            Vector3f at = new Vector3f(Mth.cos(a) * rr, .05f, Mth.sin(a) * rr);
            if (t < .85f) {
                float size = (.1f + .2f * BossMesh.hash(seed + k * 11)) * (.3f + t) * Math.min(1, r);
                BossMesh.sphere(skin, poseStack.last(), at, size, 6, 10, age * .02f, 0, .5f, BossMesh.argb(0xE0FFA8, .9f * alpha), FULL);
            } else {
                float pop = (t - .85f) / .15f;
                ElementFx.groundRing(poseStack, buffer, .07f, .12f + pop * .4f, .18f + pop * .45f, BRIGHT, (1 - pop) * alpha);
                ElementFx.bloom(poseStack, buffer, view, new Vector3f(at).add(0, .12f, 0), .3f, BRIGHT, (1 - pop) * alpha);
            }
        }
        ElementFx.bloom(poseStack, buffer, view, new Vector3f(0, .2f, 0), r * 1.1f, GREEN, .4f * alpha);
    }

    // ---------------------------------------------------------------- Acid Spray

    private void spray(AcidScene e, PoseStack poseStack, MultiBufferSource buffer, WaterMesh.View view, float age, int seed, int light) {
        Vector3f aim = ElementFx.v(e.aim());
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
        // The jet: a glowing cone of acid widening out of the hands, churning as it goes
        if (nozzle > .01f) {
            int n = 8;
            Vector3f[] axis = new Vector3f[n];
            float[] radius = new float[n], glowR = new float[n];
            int[] cols = new int[n], glows = new int[n];
            float len = reach * .55f * Math.min(1, age / 3f);
            for (int i = 0; i < n; i++) {
                float t = (float) i / (n - 1);
                axis[i] = WaterMesh.thrown(new Vector3f(), new Vector3f(aim).mul(speed), t * len / speed, GRAVITY * .5f);
                radius[i] = .08f + t * len * AcidScene.SPRAY_HALF * .45f;
                glowR[i] = radius[i] * 1.15f;
                cols[i] = BossMesh.argb(0xE8FFC0, .6f * nozzle * (1 - t * .85f));
                glows[i] = BossMesh.glow(GREEN, .45f * nozzle * (1 - t));
            }
            WaterMesh.tube(BossMesh.see(buffer, ACID), poseStack.last(), axis, radius, cols, 14, .3f, 3, age * .5f, .8f, .5f, seed, FULL, 1);
            WaterMesh.tube(BossMesh.light(buffer, BossMesh.NOISE), poseStack.last(), axis, glowR, glows, 12, .35f, 3, age * .6f, .6f, -.5f, seed + 3, FULL);
        }
        // Fat drops flying out in a fan, each launched at its own tick while the spray lasts
        VertexConsumer streaks = BossMesh.light(buffer, WaterMesh.STREAK);
        VertexConsumer blobs = BossMesh.see(buffer, ACID);
        for (int k = 0; k < 90; k++) {
            float born = BossMesh.hash(seed + k) * AcidScene.SPRAY_TIME;
            float t = age - born;
            if (t < 0 || t > 14) {
                continue;
            }
            float ax = (BossMesh.hash(seed + k * 3) * 2 - 1) * AcidScene.SPRAY_HALF, ay = (BossMesh.hash(seed + k * 5) * 2 - 1) * AcidScene.SPRAY_HALF * .55f;
            Vector3f dir = new Vector3f(aim).add(new Vector3f(side).mul(ax)).add(new Vector3f(up).mul(ay)).normalize();
            Vector3f vel = dir.mul(speed * (.8f + .4f * BossMesh.hash(seed + k * 7)));
            Vector3f at = WaterMesh.thrown(new Vector3f(), vel, t, GRAVITY);
            Vector3f from = WaterMesh.thrown(new Vector3f(), vel, Math.max(0, t - 2f), GRAVITY);
            if (at.y < -1.3f) {
                continue;
            }
            float alpha = 1 - BossMesh.ramp(t, 9, 14);
            float w = .1f + .08f * BossMesh.hash(seed + k * 11);
            drop(streaks, poseStack, view, at, from, w, alpha);
            if (k % 3 == 0) {
                BossMesh.sphere(blobs, poseStack.last(), at, w * .9f, 4, 6, 0, 0, 1, BossMesh.argb(0xE8FFB0, alpha), FULL);
            }
        }
        // Where the drops land, splats of acid hissing on the ground (the spell rises from the caster's eyes: the ground is
        // taken 1.3 blocks under it, flat)
        float ground = -1.3f;
        for (int k = 0; k < 90; k += 3) {
            float born = BossMesh.hash(seed + k) * AcidScene.SPRAY_TIME;
            float ax = (BossMesh.hash(seed + k * 3) * 2 - 1) * AcidScene.SPRAY_HALF, ay = (BossMesh.hash(seed + k * 5) * 2 - 1) * AcidScene.SPRAY_HALF * .55f;
            Vector3f dir = new Vector3f(aim).add(new Vector3f(side).mul(ax)).add(new Vector3f(up).mul(ay)).normalize();
            Vector3f vel = dir.mul(speed * (.8f + .4f * BossMesh.hash(seed + k * 7)));
            float land = (vel.y + Mth.sqrt(vel.y * vel.y + 2 * GRAVITY * -ground)) / GRAVITY;
            float since = age - born - land;
            if (since < 0) {
                continue;
            }
            Vector3f at = WaterMesh.thrown(new Vector3f(), vel, land, GRAVITY);
            at.y = ground;
            float stay = 1 - BossMesh.ramp(age, e.getLife() - 12, e.getLife());
            splat(poseStack, buffer, view, at, (.25f + .2f * BossMesh.hash(seed + k * 9)) * Math.min(1, since / 3f + .4f), age, stay, seed + k);
        }
        ElementFx.bloom(poseStack, buffer, view, new Vector3f(aim).mul(.3f), .7f, BRIGHT, nozzle);
        ElementFx.bloom(poseStack, buffer, view, new Vector3f(aim).mul(reach * .45f), reach * .4f, GREEN, .3f * nozzle);
    }

    // ---------------------------------------------------------------- Corrosive Pool

    private void pool(AcidScene e, PoseStack poseStack, MultiBufferSource buffer, WaterMesh.View view, float age, int seed, int light) {
        float spread = BossMesh.ramp(age, 0, 8);
        float fade = 1 - BossMesh.ramp(age, e.getLife() - 20, e.getLife());
        float r = e.getSize() * (.4f + .6f * spread);
        puddle(poseStack, buffer, view, r, age, fade, 14, seed, light);
        // The splash it landed with
        if (age < 14) {
            float t = age / 14f;
            VertexConsumer streaks = BossMesh.light(buffer, WaterMesh.STREAK);
            for (int k = 0; k < 24; k++) {
                float a = k * Mth.TWO_PI / 24 + BossMesh.hash(seed + k);
                Vector3f vel = new Vector3f(Mth.cos(a) * .2f, .3f + .18f * BossMesh.hash(seed + k * 3), Mth.sin(a) * .2f);
                drop(streaks, poseStack, view, WaterMesh.thrown(new Vector3f(), vel, age, GRAVITY * 1.5f),
                        WaterMesh.thrown(new Vector3f(), vel, Math.max(0, age - 1.5f), GRAVITY * 1.5f), .1f, 1 - t);
            }
            ElementFx.groundRing(poseStack, buffer, .08f, e.getSize() * 1.4f * t, e.getSize() * 1.4f * t + .4f, BRIGHT, 1 - t);
        }
        ElementFx.motes(poseStack, buffer, view, BossMesh.MOTE, new Vector3f(), r * .9f, 1.8f, .14f, 14, age, BRIGHT, fade, seed + 5);
        float bite = Math.max(0, 1 - e.sinceStruck(age - e.tickCount) / 8f);
        ElementFx.bloom(poseStack, buffer, view, new Vector3f(0, .4f, 0), r, BRIGHT, .5f * bite * fade);
    }

    // ---------------------------------------------------------------- Acid Rain

    private void rain(AcidScene e, PoseStack poseStack, MultiBufferSource buffer, WaterMesh.View view, float age, int seed, int light) {
        float r = e.getSize();
        float h = AcidScene.RAIN_HEIGHT;
        float gather = BossMesh.ramp(age, 0, 15);
        float fade = 1 - BossMesh.ramp(age, e.getLife() - 20, e.getLife());
        float shown = gather * fade;
        // The cloud: sickly green-yellow puffs churning, a deeper green underneath, lit from within now and then
        int flash = Mth.floor(age / 9);
        float flicker = BossMesh.hash(seed + flash * 13) > .55f ? 1 - ((age % 9) / 9f) : 0;
        // In body, not a fog (a fog hid the rain and everything behind it): a low, flat bank of soft lumps, yellow-green
        // and pale on top, sicklier underneath, brightening when it flashes
        VertexConsumer puffs = BossMesh.solid(buffer, SnowSceneRenderer.SNOW);
        for (int k = 0; k < 26; k++) {
            float a = BossMesh.hash(seed + k) * Mth.TWO_PI + age * .004f * (k % 2 == 0 ? 1 : -1);
            float rr = r * 1.05f * Mth.sqrt(BossMesh.hash(seed + k * 3)) * (.6f + .4f * gather);
            float top = BossMesh.hash(seed + k * 5);
            float y = h + (top - .4f) * .5f + .1f * Mth.sin(age * .05f + k);
            // Pale: its underside, turned from the light, is drawn darker, and a deep green read as a tree from below
            int col = BossMesh.mix(0xC4DC7C, 0xF2FAD0, top);
            col = BossMesh.mix(col, 0xF4FFB8, .45f * flicker);
            float size = (.55f + .35f * BossMesh.hash(seed + k * 11)) * (.7f + .3f * gather) * fade;
            ElementFx.lumpy(puffs, poseStack.last(), new Vector3f(Mth.cos(a) * rr, y, Mth.sin(a) * rr), size, 8, 12, .12f, 1.6f, age * .01f, seed + k * 7,
                    .55f, BossMesh.argb(col, 1), FULL);
        }
        float fa = BossMesh.hash(seed + flash * 17) * Mth.TWO_PI, fr = r * .6f * BossMesh.hash(seed + flash * 19);
        ElementFx.bloom(poseStack, buffer, view, new Vector3f(Mth.cos(fa) * fr, h - .2f, Mth.sin(fa) * fr), r * .8f, GREEN, (.2f + .8f * flicker) * shown);
        ElementFx.bloom(poseStack, buffer, view, new Vector3f(0, h - .6f, 0), r * .9f, DEEP, .3f * shown);
        // The rain: glowing streaks falling, each splashing in a small ring where it lands, droplets bouncing up
        VertexConsumer streaks = BossMesh.light(buffer, WaterMesh.STREAK);
        VertexConsumer body = BossMesh.see(buffer, WaterMesh.STREAK);
        float fall = h / 8f;
        for (int k = 0; k < 64; k++) {
            float cycle = 10;
            float t = (age + BossMesh.hash(seed + k * 23) * cycle) % cycle;
            int round = Mth.floor((age + BossMesh.hash(seed + k * 23) * cycle) / cycle);
            float a = BossMesh.hash(seed + k * 29 + round * 7) * Mth.TWO_PI, rr = r * Mth.sqrt(BossMesh.hash(seed + k * 31 + round * 11));
            float x = Mth.cos(a) * rr, z = Mth.sin(a) * rr;
            if (t < 8) {
                float y = h - .6f - fall * t;
                // A green body drawn see-through under the glow (glow alone turns white against a bright sky)
                WaterMesh.streak(body, poseStack.last(), view.eye(), new Vector3f(x, y, z), new Vector3f(x, y + 1.1f, z), .1f, BossMesh.argb(0x7ACC2A, .85f * shown),
                        FULL);
                drop(streaks, poseStack, view, new Vector3f(x, y, z), new Vector3f(x, y + 1.1f, z), .05f, .6f * shown, BRIGHT);
            } else {
                float s = (t - 8) / 2f;
                poseStack.pushPose();
                poseStack.translate(x, 0, z);
                ElementFx.groundRing(poseStack, buffer, .08f, .05f + s * .35f, .12f + s * .4f, BRIGHT, (1 - s) * shown);
                for (int d = 0; d < 3; d++) {
                    float da = d * Mth.TWO_PI / 3 + k;
                    Vector3f vel = new Vector3f(Mth.cos(da) * .06f, .14f, Mth.sin(da) * .06f);
                    drop(streaks, poseStack, view, WaterMesh.thrown(new Vector3f(0, .05f, 0), vel, s * 4, GRAVITY),
                            WaterMesh.thrown(new Vector3f(0, .05f, 0), vel, Math.max(0, s * 4 - 1), GRAVITY), .04f, (1 - s) * shown);
                }
                poseStack.popPose();
            }
        }
        puddle(poseStack, buffer, view, r * .9f * gather, age, .8f * shown, 8, seed + 3, light);
    }

    // ---------------------------------------------------------------- Dissolve

    private void dissolve(AcidScene e, PoseStack poseStack, MultiBufferSource buffer, WaterMesh.View view, float age, float pt, int seed, int light) {
        Entity victim = e.victim();
        float height = victim != null ? victim.getBbHeight() : 1.6f;
        float r = e.getSize();
        int melt = e.getLife() - 12;
        float centreY = height * .5f;
        if (age < melt) {
            // The bubble: closing round the enemy, wobbling, clear, glassy at its rim, its surface slowly flowing
            float close = BossMesh.ramp(age, 0, 10);
            float wob = .06f * Mth.sin(age * .35f);
            float rr = r * (1.6f - .6f * close);
            poseStack.pushPose();
            poseStack.translate(0, centreY, 0);
            poseStack.scale(1 + wob, (height * .5f + .3f) / r * (1 - wob), 1 + wob);
            ElementFx.lumpy(BossMesh.see(buffer, ACID), poseStack.last(), new Vector3f(), rr, 16, 26, .06f, 2, age * .05f, seed, 1,
                    BossMesh.argb(0xE0FFB0, .3f * close), FULL);
            BossMesh.sphere(BossMesh.light(buffer, BossMesh.SHEEN), poseStack.last(), new Vector3f(), rr * 1.02f, 14, 24, age * .003f, 0, 1,
                    BossMesh.glow(BRIGHT, .3f * close), FULL);
            BossMesh.sphere(BossMesh.light(buffer, BossMesh.NOISE), poseStack.last(), new Vector3f(), rr * 1.04f, 12, 20, -age * .008f, 0, 1,
                    BossMesh.glow(GREEN, .2f * close), FULL);
            poseStack.popPose();
            // Froth round its foot, drips running down it, bubbles rising inside
            BossMesh.annulus(BossMesh.see(buffer, FROTH), poseStack.last(), new Vector3f(0, .08f, 0), new Vector3f(1, 0, 0), new Vector3f(0, 0, -1), rr * .7f,
                    rr * 1.15f, 32, 3, age * .004f, 0, BossMesh.argb(0xF4FFE0, .9f * close), BossMesh.argb(0xF4FFE0, 0), light, true);
            VertexConsumer streaks = BossMesh.light(buffer, WaterMesh.STREAK);
            for (int k = 0; k < 10; k++) {
                float t = ((age * .04f + BossMesh.hash(seed + k * 9)) % 1);
                float a = BossMesh.hash(seed + k * 13) * Mth.TWO_PI;
                float y = height * (1.05f - t * 1.05f);
                float span = Mth.sqrt(Math.max(0, 1 - (y - centreY) * (y - centreY) / Math.max(.01f, (height * .5f + .3f) * (height * .5f + .3f))));
                Vector3f at = new Vector3f(Mth.cos(a) * rr * span, y, Mth.sin(a) * rr * span);
                drop(streaks, poseStack, view, at, new Vector3f(at).add(0, .35f, 0), .06f, .8f * close);
            }
            VertexConsumer skin = BossMesh.see(buffer, WaterMesh.BUBBLE);
            for (int k = 0; k < 18; k++) {
                float t = ((age * .03f + BossMesh.hash(seed + k)) % 1);
                float a = BossMesh.hash(seed + k * 3) * Mth.TWO_PI + age * .05f;
                float rad = r * .75f * BossMesh.hash(seed + k * 5);
                Vector3f at = new Vector3f(Mth.cos(a) * rad, height * t * 1.1f, Mth.sin(a) * rad);
                BossMesh.sphere(skin, poseStack.last(), at, .06f + .09f * BossMesh.hash(seed + k * 7), 5, 8, 0, 0, 1,
                        BossMesh.argb(0xE8FFB0, .9f * close * (1 - t)), FULL);
            }
            float bite = Math.max(0, 1 - ((age - 1) % 20) / 8f);
            ElementFx.bloom(poseStack, buffer, view, new Vector3f(0, centreY, 0), r * 1.4f, GREEN, (.25f + .4f * bite) * close);
            puddle(poseStack, buffer, view, r * 1.1f * close, age, .7f * close, 4, seed, light);
            return;
        }
        // The burst: the bubble splashes out, drops flung in every way, a puddle left that fades
        float since = age - melt;
        float t = since / 12f;
        VertexConsumer streaks = BossMesh.light(buffer, WaterMesh.STREAK);
        for (int k = 0; k < 56; k++) {
            float a = BossMesh.hash(seed + k) * Mth.TWO_PI;
            float up = BossMesh.hash(seed + k * 3) * 2 - .5f;
            Vector3f vel = new Vector3f(Mth.cos(a), up * .6f, Mth.sin(a)).normalize().mul(.24f + .14f * BossMesh.hash(seed + k * 5));
            Vector3f origin = new Vector3f(0, centreY, 0);
            drop(streaks, poseStack, view, WaterMesh.thrown(origin, vel, since, GRAVITY), WaterMesh.thrown(origin, vel, Math.max(0, since - 1.8f), GRAVITY),
                    .12f, 1 - t);
        }
        if (since < 4) {
            BossMesh.sphere(BossMesh.see(buffer, ACID), poseStack.last(), new Vector3f(0, centreY, 0), r * (1 + since * .3f), 12, 18, 0, 0, 1,
                    BossMesh.argb(0xE0FFB0, .35f * (1 - since / 4)), FULL);
        }
        ElementFx.bloom(poseStack, buffer, view, new Vector3f(0, centreY, 0), r * 2, BRIGHT, 1 - t);
        ElementFx.groundRing(poseStack, buffer, .08f, r * 2.6f * t, r * 2.6f * t + .6f, BRIGHT, 1 - t);
        puddle(poseStack, buffer, view, r * 1.5f, age, 1 - t, 5, seed, light);
    }

    // ---------------------------------------------------------------- Corroding Tide

    private void tide(AcidScene e, PoseStack poseStack, MultiBufferSource buffer, WaterMesh.View view, float age, int seed, int light) {
        float reach = e.getSize();
        float roll = BossMesh.ramp(age, 0, AcidScene.TIDE_ROLL);
        float front = reach * roll;
        float wave = 1 - BossMesh.ramp(age, AcidScene.TIDE_ROLL - 6, AcidScene.TIDE_ROLL + 4);
        float fade = 1 - BossMesh.ramp(age, e.getLife() - 18, e.getLife());
        // The ground behind the wave: wet with acid, glowing, fuming
        if (front > .3f) {
            BossMesh.annulus(BossMesh.ground(buffer, ACID), poseStack.last(), new Vector3f(0, .04f, 0), new Vector3f(1, 0, 0), new Vector3f(0, 0, -1), .01f,
                    front, 72, Math.max(1, front * .5f), age * .002f, age * .004f, BossMesh.argb(0xE8FFC0, .2f * fade), BossMesh.argb(0xD8FFA0, .55f * fade), FULL,
                    false);
            BossMesh.annulus(BossMesh.light(buffer, BossMesh.NOISE), poseStack.last(), new Vector3f(0, .05f, 0), new Vector3f(1, 0, 0), new Vector3f(0, 0, -1),
                    Math.max(.01f, front - 3), front, 72, Math.max(1, front * .4f), -age * .003f, age * .006f, BossMesh.glow(GREEN, 0),
                    BossMesh.glow(BRIGHT, .3f * fade), FULL, false);
        }
        // The wave: a low ring of clear acid, its back sloping up to a crest that curls over its front, glowing, foam on top
        if (wave > .01f && front > .2f) {
            float h = (.45f + .75f * Mth.sin(Mth.PI * Math.min(1, roll * 1.3f))) * wave;
            int segs = 80;
            VertexConsumer body = BossMesh.see(buffer, ACID);
            VertexConsumer foam = BossMesh.see(buffer, FROTH);
            VertexConsumer crest = BossMesh.light(buffer, BossMesh.NOISE);
            float[] rs = {front - 2f, front - .8f, front - .2f, front + .12f, front + .3f};
            float[] ys = {.05f, h * .6f, h, h * .85f, .05f};
            for (int i = 0; i < segs; i++) {
                float a0 = i * Mth.TWO_PI / segs, a1 = (i + 1) * Mth.TWO_PI / segs;
                float sw0 = 1 + .14f * Mth.sin(a0 * 9 + age * .3f), sw1 = 1 + .14f * Mth.sin(a1 * 9 + age * .3f);
                float c0 = Mth.cos(a0), s0 = Mth.sin(a0), c1 = Mth.cos(a1), s1 = Mth.sin(a1);
                float u0 = a0 * front * .25f, u1 = a1 * front * .25f;
                for (int j = 0; j + 1 < rs.length; j++) {
                    float ra = Math.max(0, rs[j]), rb = Math.max(0, rs[j + 1]);
                    float ya0 = ys[j] * sw0, yb0 = ys[j + 1] * sw0, ya1 = ys[j] * sw1, yb1 = ys[j + 1] * sw1;
                    float va = j * .3f - age * .04f, vb = (j + 1) * .3f - age * .04f;
                    float la = ys[j] / Math.max(h, .01f), lb = ys[j + 1] / Math.max(h, .01f);
                    int ca = BossMesh.argb(BossMesh.mix(0xB8E880, 0xF4FFD8, la), (.35f + .2f * la) * wave);
                    int cb = BossMesh.argb(BossMesh.mix(0xB8E880, 0xF4FFD8, lb), (.35f + .2f * lb) * wave);
                    Vector3f p0 = new Vector3f(c0 * ra, ya0, s0 * ra), p1 = new Vector3f(c1 * ra, ya1, s1 * ra);
                    Vector3f p2 = new Vector3f(c1 * rb, yb1, s1 * rb), p3 = new Vector3f(c0 * rb, yb0, s0 * rb);
                    ElementFx.twoSided(body, poseStack.last(), p0, p1, p2, p3, u0, va, u1, va, u1, vb, u0, vb, ca, cb, FULL);
                    if (j == 1 || j == 2) {
                        BossMesh.quad2(crest, poseStack.last(), p0, p1, p2, p3, u0, va, u1, va, u1, vb, u0, vb, BossMesh.glow(GREEN, .25f * wave * la),
                                BossMesh.glow(BRIGHT, .35f * wave * lb), FULL);
                    }
                }
                // The foam on its crest
                float fr0 = front - .4f, fr1 = front + .22f;
                ElementFx.twoSided(foam, poseStack.last(), new Vector3f(c0 * fr0, h * .93f * sw0 + .03f, s0 * fr0), new Vector3f(c1 * fr0, h * .93f * sw1 + .03f, s1 * fr0),
                        new Vector3f(c1 * fr1, h * .96f * sw1 + .03f, s1 * fr1), new Vector3f(c0 * fr1, h * .96f * sw0 + .03f, s0 * fr1), u0, 0, u1, 0, u1, 1, u0, 1,
                        BossMesh.argb(0xFFFFFF, .9f * wave), BossMesh.argb(0xFFFFFF, .9f * wave), light);
            }
            ElementFx.groundRing(poseStack, buffer, .1f, Math.max(0, front - 1.2f), front + .4f, BRIGHT, .5f * wave);
            VertexConsumer froth = BossMesh.see(buffer, FROTH);
            for (int k = 0; k < 40; k++) {
                float a = k * Mth.TWO_PI / 40 + BossMesh.hash(seed + k * 31) * .15f;
                float sw = 1 + .14f * Mth.sin(a * 9 + age * .3f);
                Vector3f at = new Vector3f(Mth.cos(a) * (front - .1f), h * .95f * sw, Mth.sin(a) * (front - .1f));
                ElementFx.lumpy(froth, poseStack.last(), at, .22f + .12f * BossMesh.hash(seed + k), 5, 8, .35f, 2.5f, age * .1f, seed + k, .6f,
                        BossMesh.argb(0xF8FFE8, .9f * wave), light);
            }
            // Spray flung off its crest
            VertexConsumer streaks = BossMesh.light(buffer, WaterMesh.STREAK);
            for (int k = 0; k < 48; k++) {
                float a = k * Mth.TWO_PI / 48 + BossMesh.hash(seed + k) * .2f;
                float t = ((age * .15f + BossMesh.hash(seed + k * 3)) % 1);
                Vector3f origin = new Vector3f(Mth.cos(a) * front, h, Mth.sin(a) * front);
                Vector3f vel = new Vector3f(Mth.cos(a) * .12f, .12f, Mth.sin(a) * .12f);
                drop(streaks, poseStack, view, WaterMesh.thrown(origin, vel, t * 8, GRAVITY), WaterMesh.thrown(origin, vel, Math.max(0, t * 8 - 1.5f), GRAVITY), .08f,
                        wave * (1 - t));
            }
            for (int k = 0; k < 10; k++) {
                float a = k * Mth.TWO_PI / 10 + .3f;
                ElementFx.bloom(poseStack, buffer, view, new Vector3f(Mth.cos(a) * front, h * .6f, Mth.sin(a) * front), 1.1f, GREEN, .35f * wave);
            }
        }
        ElementFx.motes(poseStack, buffer, view, BossMesh.MOTE, new Vector3f(), Math.max(1, front), 1.5f, .14f, 30, age, BRIGHT, fade, seed + 7);
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
