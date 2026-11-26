package com.waylays.arcanes.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.waylays.arcanes.ArcanesOublies;
import com.waylays.arcanes.client.render.boss.BossMesh;
import com.waylays.arcanes.client.render.clockwork.ClockMesh;
import com.waylays.arcanes.client.render.ritual.RitualMesh;
import com.waylays.arcanes.client.render.water.WaterMesh;
import com.waylays.arcanes.entity.snow.SnowScene;
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

import static com.waylays.arcanes.client.render.ElementFx.FULL;

/**
 * Snow Magic's spells, drawn: a great snowball rolling and growing, ploughing a furrow, powder flying off it, bursting
 * into chunks and flakes; a snow globe (a brass base, a little snowy wood of pines under a dome of glass, frost on the
 * glass, snow falling) that shatters; a ribbon of ice laid on the ground, clusters of crystals along it, frost
 * glinting; a crown of ice crystals turning over the caster's head, snowflakes circling it, its icicles streaking
 * to their mark; a blizzard whirling round in a wall of driven snow under heavy clouds, the frozen bursting into spikes
 * of ice. No mist or fog sheets: they hid the spells.
 */
public class SnowSceneRenderer extends EntityRenderer<SnowScene> {
    public static final ResourceLocation SNOW = ArcanesOublies.id("textures/entity/snow/snow.png");
    public static final ResourceLocation ICE = ArcanesOublies.id("textures/entity/snow/ice.png");
    public static final ResourceLocation FLAKE = ArcanesOublies.id("textures/entity/snow/flake.png");
    private static final int WHITE = 0xF4FAFF, FROST = 0xCFEFFF, BLUE = 0x8CC8FF, DEEP = 0x4A90D8, PINE = 0x2E6A44;

    public SnowSceneRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(SnowScene e, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource out, int packedLight) {
        // Recorded per render type, then written out one type after another (see SceneBuffers)
        SceneBuffers buffer = SceneBuffers.begin();
        // On top of snow layers or a carpet, if the scene stands in them
        poseStack.pushPose();
        poseStack.translate(0, ElementFx.floorLift(e), 0);
        float age = e.tickCount + partialTicks;
        WaterMesh.View view = WaterViews.of(this.entityRenderDispatcher, e, partialTicks);
        int seed = e.getId() * 31;
        // Snow is white in any weather: lit as by full daylight (a dimmer light turned the powder and the blizzard grey)
        int light = ElementFx.light(e, 15);
        switch (e.getKind()) {
            case ROLL -> roll(e, poseStack, buffer, view, age, partialTicks, seed, light);
            case GLOBE -> globe(e, poseStack, buffer, view, age, partialTicks, seed, light);
            case TRAIL -> trail(e, poseStack, buffer, view, age, seed, light);
            case CROWN -> crown(e, poseStack, buffer, view, age, partialTicks, seed, light);
            case WHITEOUT -> whiteout(e, poseStack, buffer, view, age, partialTicks, seed, light);
        }
        buffer.flush(out);
        poseStack.popPose();
        super.render(e, entityYaw, partialTicks, poseStack, out, packedLight);
    }

    /** A burst of powder: chunks of snow thrown and falling, flakes glittering. */
    private static void powder(PoseStack poseStack, MultiBufferSource buffer, WaterMesh.View view, Vector3f at, float size, float t, int seed, int light) {
        if (t >= 1) {
            return;
        }
        float out = 1 - (1 - t) * (1 - t);
        // Chunks of packed snow thrown out and falling
        VertexConsumer snow = BossMesh.see(buffer, SNOW);
        for (int k = 0; k < 14; k++) {
            float a = BossMesh.hash(seed + k * 7) * Mth.TWO_PI, s = .5f + .5f * BossMesh.hash(seed + k * 11);
            Vector3f p = new Vector3f(Mth.cos(a) * size * 2.4f * s * out, size * (2.6f * t - 3.2f * t * t) * (1 + s), Mth.sin(a) * size * 2.4f * s * out).add(at);
            ElementFx.lumpy(snow, poseStack.last(), p, size * (.14f + .1f * s) * (1 - t * .5f), 5, 8, .3f, 2.5f, 0, seed + k, .8f, BossMesh.argb(0xFFFFFF, 1),
                    light);
        }
        ElementFx.burst(poseStack, buffer, view, FLAKE, at, size * 3, size * 2, .3f, 16, t, FROST, 1, seed + 5, true, FULL);
    }

    // ---------------------------------------------------------------- Avalanche Ball

    private void roll(SnowScene e, PoseStack poseStack, MultiBufferSource buffer, WaterMesh.View view, float age, float pt, int seed, int light) {
        float r = e.getSize() * 1.15f;
        if (e.hasBurst()) {
            float t = e.sinceBurst(pt) / 24f;
            powder(poseStack, buffer, view, new Vector3f(0, r, 0), r * 1.3f, t, seed, light);
            ElementFx.groundRing(poseStack, buffer, .07f, r * 3.5f * t, r * 3.5f * t + .7f, FROST, (1 - t) * .9f);
            ElementFx.bloom(poseStack, buffer, view, new Vector3f(0, r, 0), r * 1.5f, BLUE, .6f * (1 - t));
            return;
        }
        Vec3 fw = e.forward();
        Vector3f f = ElementFx.v(fw), side = new Vector3f(f.z, 0, -f.x);
        // The furrow it ploughs behind it: packed snow, darker at its edges
        float behind = Math.min(age * SnowScene.ROLL_SPEED, 7);
        VertexConsumer furrow = BossMesh.ground(buffer, SNOW);
        int segs = 14;
        for (int i = 0; i < segs; i++) {
            float t0 = (float) i / segs, t1 = (float) (i + 1) / segs;
            float w0 = r * (.85f - .35f * t0), w1 = r * (.85f - .35f * t1);
            Vector3f c0 = new Vector3f(f).mul(-behind * t0).add(0, .06f, 0), c1 = new Vector3f(f).mul(-behind * t1).add(0, .06f, 0);
            float a0 = .85f * (1 - t0), a1 = .85f * (1 - t1);
            ElementFx.flat(furrow, poseStack.last(), new Vector3f(c0).sub(new Vector3f(side).mul(w0)), new Vector3f(c0).add(new Vector3f(side).mul(w0)),
                    new Vector3f(c1).add(new Vector3f(side).mul(w1)), new Vector3f(c1).sub(new Vector3f(side).mul(w1)), 0, t0 * 3, 1, t0 * 3, 1, t1 * 3, 0, t1 * 3,
                    BossMesh.argb(0xB8D0EA, a0), BossMesh.argb(0xB8D0EA, a1), light);
        }
        // Banks of snow heaped up along both sides of the furrow, and snow sprayed off its sides
        VertexConsumer banks = BossMesh.solid(buffer, SNOW);
        for (int i = 0; i < 8; i++) {
            float t = (i + .5f) / 8;
            if (t * behind < .3f) {
                continue;
            }
            for (int s = -1; s <= 1; s += 2) {
                float w = r * (.95f - .35f * t);
                Vector3f at = new Vector3f(f).mul(-behind * t).add(new Vector3f(side).mul(s * w)).add(0, .02f, 0);
                ElementFx.lumpy(banks, poseStack.last(), at, r * (.28f - .12f * t), 5, 9, .3f, 2.2f, 0, seed + i * 7 + s, .45f, BossMesh.argb(0xF4FAFF, 1),
                        light);
            }
        }
        for (int s = -1; s <= 1; s += 2) {
            for (int k = 0; k < 6; k++) {
                float t = ((age * .12f + BossMesh.hash(seed + k * 3 + s)) % 1);
                Vector3f at = new Vector3f(side).mul(s * (r * .9f + t * r * 1.6f)).add(new Vector3f(f).mul(r * .2f - t * r * .8f))
                        .add(0, r * .3f + 3.2f * r * t * (1 - t), 0);
                RitualMesh.billboard(BossMesh.see(buffer, SNOW), poseStack, view, at, r * .18f * (1 - t * .5f), age * .2f + k, -1,
                        BossMesh.argb(0xFFFFFF, 1 - t), light);
            }
        }
        // The ball, turned over as far as it has rolled, lumps of snow packed onto it
        float turn = age * SnowScene.ROLL_SPEED / Math.max(.2f, r);
        poseStack.pushPose();
        poseStack.translate(0, r, 0);
        poseStack.mulPose(new Quaternionf().rotateAxis(turn, (float) fw.z, 0, (float) -fw.x));
        // A lumpy ball of packed snow, clods stuck on it, bits of ice glinting in it
        VertexConsumer snow = BossMesh.solid(buffer, SNOW);
        ElementFx.lumpy(snow, poseStack.last(), new Vector3f(), r, 18, 30, .1f, 2.2f, 0, seed, 1, BossMesh.argb(0xFFFFFF, 1), light);
        for (int k = 0; k < 9; k++) {
            float a = BossMesh.hash(seed + k) * Mth.TWO_PI, b = BossMesh.hash(seed + k * 5) * Mth.PI;
            Vector3f at = new Vector3f(Mth.sin(b) * Mth.cos(a), Mth.cos(b), Mth.sin(b) * Mth.sin(a)).mul(r * .92f);
            ElementFx.lumpy(snow, poseStack.last(), at, r * (.18f + .12f * BossMesh.hash(seed + k * 7)), 6, 10, .3f, 2.5f, 0, seed + k * 3, .8f,
                    BossMesh.argb(0xEAF4FF, 1), light);
        }
        VertexConsumer glints = BossMesh.see(buffer, ICE);
        for (int k = 0; k < 7; k++) {
            float a = BossMesh.hash(seed + k * 13) * Mth.TWO_PI, b = BossMesh.hash(seed + k * 17) * Mth.PI;
            Vector3f d = new Vector3f(Mth.sin(b) * Mth.cos(a), Mth.cos(b), Mth.sin(b) * Mth.sin(a));
            RitualMesh.crystal(glints, poseStack.last(), new Vector3f(d).mul(r * .85f), d, r * .35f, r * .07f, k, BossMesh.argb(0xBFE6FF, .9f), light);
        }
        BossMesh.sphere(BossMesh.light(buffer, BossMesh.SHEEN), poseStack.last(), new Vector3f(), r * 1.03f, 10, 18, age * .01f, 0, 1,
                BossMesh.glow(FROST, .22f), FULL);
        poseStack.popPose();
        // Flakes glittering round it; a cold glow under it
        ElementFx.motes(poseStack, buffer, view, FLAKE, new Vector3f(0, .2f, 0), r * 1.8f, r * 2.4f, .22f, 16, age, FROST, .9f, seed + 7);
        ElementFx.bloom(poseStack, buffer, view, new Vector3f(0, .3f, 0), r * 1.2f, BLUE, .3f);
        // Clods shaken off it, bouncing and rolling behind
        VertexConsumer clods = BossMesh.solid(buffer, SNOW);
        for (int k = 0; k < 6; k++) {
            float cycle = 14 + 6 * BossMesh.hash(seed + k);
            float t = ((age + BossMesh.hash(seed + k * 3) * cycle) % cycle) / cycle;
            float across = (BossMesh.hash(seed + k * 5) - .5f) * r * 1.6f;
            Vector3f p = new Vector3f(f).mul(-t * r * 3).add(new Vector3f(side).mul(across)).add(0, r * .25f + Math.abs(Mth.sin(t * Mth.PI * 2)) * r * .4f * (1 - t),
                    0);
            ElementFx.lumpy(clods, poseStack.last(), p, r * .14f * (1 - t * .6f), 5, 8, .35f, 2.5f, 0, seed + k * 7, .8f, BossMesh.argb(0xF4FAFF, 1), light);
        }
    }

    // ---------------------------------------------------------------- Snow Globe

    private void globe(SnowScene e, PoseStack poseStack, MultiBufferSource buffer, WaterMesh.View view, float age, float pt, int seed, int light) {
        float r = e.getSize();
        float appear = BossMesh.ramp(age, 0, 8);
        float fade = 1 - BossMesh.ramp(age, e.getLife() - 6, e.getLife());
        boolean shattered = e.hasBurst();
        float st = shattered ? e.sinceBurst(pt) / 16f : 0;
        float floor = .42f;
        float R = r * appear;
        // The brass base: a plinth, a band, a lip, rivets glinting
        ClockMesh.cylinder(poseStack.last(), buffer, ClockMesh.BRASS, R * 1.1f, -.02f, .14f, 48, BossMesh.argb(0x9A7A50, 1), light);
        ClockMesh.cylinder(poseStack.last(), buffer, ClockMesh.BRASS, R * 1.03f, .14f, floor - .06f, 48, BossMesh.argb(0xFFFFFF, 1), light);
        ClockMesh.cylinder(poseStack.last(), buffer, ClockMesh.BRASS, R * 1.07f, floor - .06f, floor, 48, BossMesh.argb(0xD8B070, 1), light);
        for (int k = 0; k < 12; k++) {
            float a = k * Mth.TWO_PI / 12;
            ClockMesh.glow(poseStack, buffer, view, new Vector3f(Mth.cos(a) * R * 1.05f, .28f, Mth.sin(a) * R * 1.05f), .25f, 0xFFE0A0, .5f * fade, FULL);
        }
        // The snowy floor inside, a little wood of pines on it
        BossMesh.annulus(BossMesh.ground(buffer, SNOW), poseStack.last(), new Vector3f(0, floor + .01f, 0), new Vector3f(1, 0, 0), new Vector3f(0, 0, -1), .01f,
                R, 40, 2, 0, 0, BossMesh.argb(0xFFFFFF, fade), BossMesh.argb(0xE0ECF8, fade), light, false);
        VertexConsumer needles = BossMesh.solid(buffer, SNOW);
        for (int k = 0; k < 5; k++) {
            float a = BossMesh.hash(seed + k * 3) * Mth.TWO_PI, rr = R * (.25f + .45f * BossMesh.hash(seed + k * 5));
            float h = (.7f + .6f * BossMesh.hash(seed + k * 7)) * appear;
            Vector3f base = new Vector3f(Mth.cos(a) * rr, floor, Mth.sin(a) * rr);
            for (int tier = 0; tier < 3; tier++) {
                Vector3f at = new Vector3f(base).add(0, h * (.15f + tier * .26f), 0);
                BossMesh.horn(needles, poseStack.last(), at, new Vector3f(0, 1, 0), new Vector3f(), h * (.5f - tier * .1f), h * (.34f - tier * .08f), 7, 2,
                        BossMesh.argb(PINE, 1), BossMesh.argb(tier == 2 ? 0xF4FAFF : 0x4A8A5E, 1), light);
            }
        }
        // A little cabin among the pines, snow on its roof, its window lit warm
        if (appear > .3f) {
            float c = R * .22f * appear;
            float ca = BossMesh.hash(seed + 41) * Mth.TWO_PI, cr = R * .35f;
            Vector3f at = new Vector3f(Mth.cos(ca) * cr, floor + c * .5f, Mth.sin(ca) * cr);
            Vector3f fx = new Vector3f(Mth.cos(ca + Mth.HALF_PI), 0, Mth.sin(ca + Mth.HALF_PI)), fz = new Vector3f(Mth.cos(ca), 0, Mth.sin(ca));
            VertexConsumer wood = BossMesh.solid(buffer, ClockMesh.CANVAS);
            BossMesh.box(wood, poseStack.last(), at, new Vector3f(fx).mul(c * .6f), new Vector3f(0, c * .5f, 0), new Vector3f(fz).mul(c * .5f),
                    BossMesh.argb(0x8A5A36, 1), light);
            for (int side = -1; side <= 1; side += 2) {
                float slope = .6f;
                Vector3f up = new Vector3f(0, Mth.cos(slope), 0).add(new Vector3f(fz).mul(side * Mth.sin(slope)));
                Vector3f down = new Vector3f(fz).mul(side * Mth.cos(slope)).add(0, -Mth.sin(slope), 0);
                Vector3f roof = new Vector3f(at).add(0, c * .5f + c * .2f, 0).add(new Vector3f(down).mul(c * .32f));
                BossMesh.box(needles, poseStack.last(), roof, new Vector3f(fx).mul(c * .7f), new Vector3f(up).mul(c * .06f), new Vector3f(down).mul(c * .36f),
                        BossMesh.argb(0xFFFFFF, 1), light);
            }
            Vector3f window = new Vector3f(at).add(new Vector3f(fz).mul(c * .51f)).add(0, c * .05f, 0);
            ClockMesh.glow(poseStack, buffer, view, window, c * 1.4f, 0xFFC060, .8f * fade, FULL);
            BossMesh.box(BossMesh.light(buffer, BossMesh.GLOW), poseStack.last(), window, new Vector3f(fx).mul(c * .16f), new Vector3f(0, c * .14f, 0),
                    new Vector3f(fz).mul(.005f), BossMesh.glow(0xFFD080, fade), FULL);
            // Drifts of snow heaped round it and the pines
            for (int k = 0; k < 7; k++) {
                float a = BossMesh.hash(seed + k * 43) * Mth.TWO_PI, rr = R * (.2f + .6f * BossMesh.hash(seed + k * 47));
                ElementFx.lumpy(needles, poseStack.last(), new Vector3f(Mth.cos(a) * rr, floor, Mth.sin(a) * rr), R * (.1f + .08f * BossMesh.hash(seed + k)) * appear, 5,
                        10, .25f, 2, 0, seed + k * 5, .35f, BossMesh.argb(0xFFFFFF, 1), light);
            }
        }
        Vector3f centre = new Vector3f(0, floor, 0);
        if (!shattered) {
            // The glass: a clear dome, frost on its lower part, bright arcs of reflection, a bright rim at the base
            BossMesh.sphere(BossMesh.see(buffer, BossMesh.SHEEN), poseStack.last(), centre, R, 14, 32, age * .002f, 0, .5f, BossMesh.argb(0xE0F4FF, .38f),
                    FULL);
            BossMesh.sphere(BossMesh.see(buffer, ICE), poseStack.last(), centre, R * 1.005f, 4, 32, 0, .36f, .5f, BossMesh.argb(0xFFFFFF, .55f), FULL);
            VertexConsumer gleam = BossMesh.light(buffer, BossMesh.GLOW);
            for (float a0 : new float[]{-.9f, -.55f, 2.1f}) {
                Vector3f[] arc = new Vector3f[10];
                int[] cols = new int[10];
                for (int i = 0; i < 10; i++) {
                    float th = .25f + i * .11f;
                    arc[i] = new Vector3f(Mth.sin(th) * Mth.cos(a0) * R * 1.01f, Mth.cos(th) * R * 1.01f, Mth.sin(th) * Mth.sin(a0) * R * 1.01f).add(centre);
                    cols[i] = BossMesh.glow(0xFFFFFF, .55f * Mth.sin(Mth.PI * i / 9f) * fade);
                }
                RitualMesh.thread(gleam, poseStack.last(), view.eye(), arc, .12f, cols, FULL);
            }
            ElementFx.groundRing(poseStack, buffer, floor + .02f, R * .93f, R * 1.02f, FROST, .7f * fade);
            // Snow falling and whirling inside
            VertexConsumer flakes = BossMesh.light(buffer, FLAKE);
            for (int k = 0; k < 70; k++) {
                float h1 = BossMesh.hash(seed + k), h2 = BossMesh.hash(seed + k * 3), h3 = BossMesh.hash(seed + k * 7);
                float a = h1 * Mth.TWO_PI + age * (.04f + .05f * h2);
                float rr = R * .85f * Mth.sqrt(h2);
                float top = Mth.sqrt(Math.max(0, R * R * .8f - rr * rr));
                float fall = 1 - ((h3 + age * (.008f + .006f * h1)) % 1);
                Vector3f p = new Vector3f(Mth.cos(a) * rr, floor + .1f + fall * top, Mth.sin(a) * rr);
                RitualMesh.billboard(flakes, poseStack, view, p, .14f + .12f * h3, age * .06f + k, -1, BossMesh.glow(WHITE, .9f * appear * fade), FULL);
            }
            ElementFx.bloom(poseStack, buffer, view, new Vector3f(0, floor + R * .45f, 0), R * .9f, FROST, .25f * appear);
        } else if (st < 1) {
            // Shattered: shards of glass flung out, flashing, and the snow inside bursting
            VertexConsumer shards = BossMesh.see(buffer, BossMesh.SHEEN);
            VertexConsumer flash = BossMesh.light(buffer, BossMesh.SHEEN);
            float out = 1 - (1 - st) * (1 - st);
            for (int k = 0; k < 34; k++) {
                float a = BossMesh.hash(seed + k) * Mth.TWO_PI, b = BossMesh.hash(seed + k * 5) * Mth.HALF_PI;
                Vector3f dir = new Vector3f(Mth.cos(b) * Mth.cos(a), Mth.sin(b), Mth.cos(b) * Mth.sin(a));
                Vector3f p = new Vector3f(dir).mul(R * (1 + 1.8f * out)).add(centre).add(0, -3f * st * st, 0);
                Vector3f d = new Vector3f(dir.z, dir.y + .3f, -dir.x).normalize();
                RitualMesh.crystal(shards, poseStack.last(), p, d, .6f, .14f, st * 6 + k, BossMesh.argb(0xE8F6FF, .8f * (1 - st)), FULL);
                RitualMesh.crystal(flash, poseStack.last(), p, d, .62f, .15f, st * 6 + k, BossMesh.glow(FROST, .5f * (1 - st)), FULL);
            }
            powder(poseStack, buffer, view, new Vector3f(0, floor + R * .5f, 0), R * .6f, st, seed, light);
            ElementFx.bloom(poseStack, buffer, view, new Vector3f(0, floor + R * .5f, 0), R * 1.2f, BLUE, .8f * (1 - st));
        }
    }

    // ---------------------------------------------------------------- Frost Glide

    private void trail(SnowScene e, PoseStack poseStack, MultiBufferSource buffer, WaterMesh.View view, float age, int seed, int light) {
        float length = e.getSize() * BossMesh.ramp(age, 0, SnowScene.GLIDE_TIME + 2);
        float fade = 1 - BossMesh.ramp(age, e.getLife() - 30, e.getLife());
        if (length < .05f || fade <= 0) {
            return;
        }
        Vector3f f = ElementFx.v(e.forward()), side = new Vector3f(f.z, 0, -f.x);
        float w = .95f;
        int segs = 28;
        VertexConsumer under = BossMesh.ground(buffer, SNOW);
        VertexConsumer ice = BossMesh.ground(buffer, ICE);
        VertexConsumer edge = BossMesh.light(buffer, BossMesh.GLOW);
        Vector3f[] left = new Vector3f[segs + 1], right = new Vector3f[segs + 1];
        for (int i = 0; i <= segs; i++) {
            float t = (float) i / segs;
            float wi = w * (.8f + .3f * BossMesh.hash(seed + i));
            Vector3f c = new Vector3f(f).mul(t * length).add(0, .05f, 0);
            left[i] = new Vector3f(c).sub(new Vector3f(side).mul(wi));
            right[i] = new Vector3f(c).add(new Vector3f(side).mul(wi));
        }
        for (int i = 0; i < segs; i++) {
            float t0 = (float) i / segs, t1 = (float) (i + 1) / segs;
            // A blue shadow under the ice (it shows on snow too), the ice over it
            Vector3f l0 = new Vector3f(left[i]).sub(new Vector3f(side).mul(.15f)), r0 = new Vector3f(right[i]).add(new Vector3f(side).mul(.15f));
            Vector3f l1 = new Vector3f(left[i + 1]).sub(new Vector3f(side).mul(.15f)), r1 = new Vector3f(right[i + 1]).add(new Vector3f(side).mul(.15f));
            ElementFx.flat(under, poseStack.last(), l0, r0, r1, l1, 0, 0, 1, 0, 1, 1, 0, 1, BossMesh.argb(0x4A86C8, .55f * fade), BossMesh.argb(0x4A86C8, .55f * fade),
                    light);
            Vector3f a0 = new Vector3f(left[i]).add(0, .02f, 0), b0 = new Vector3f(right[i]).add(0, .02f, 0);
            Vector3f a1 = new Vector3f(left[i + 1]).add(0, .02f, 0), b1 = new Vector3f(right[i + 1]).add(0, .02f, 0);
            ElementFx.flat(ice, poseStack.last(), a0, b0, b1, a1, 0, t0 * length * .4f, 1, t0 * length * .4f, 1, t1 * length * .4f, 0, t1 * length * .4f,
                    BossMesh.argb(0xB4E0FF, .9f * fade), BossMesh.argb(0xB4E0FF, .9f * fade), light);
        }
        // Its edges glowing cold
        for (Vector3f[] line : new Vector3f[][]{left, right}) {
            int[] cols = new int[line.length];
            Vector3f[] raised = new Vector3f[line.length];
            for (int i = 0; i < line.length; i++) {
                raised[i] = new Vector3f(line[i]).add(0, .09f, 0);
                cols[i] = BossMesh.glow(BLUE, .7f * fade);
            }
            RitualMesh.thread(edge, poseStack.last(), view.eye(), raised, .16f, cols, FULL);
        }
        // Clusters of crystals grown along both edges, tilted out
        VertexConsumer crystals = BossMesh.see(buffer, ICE);
        VertexConsumer gleam = BossMesh.light(buffer, BossMesh.SHEEN);
        int n = Math.max(1, (int) (length / .8f));
        for (int k = 0; k < n; k++) {
            for (int s = -1; s <= 1; s += 2) {
                float t = (k + .5f) / n;
                float grow = BossMesh.ramp(age, t * SnowScene.GLIDE_TIME, t * SnowScene.GLIDE_TIME + 6) * fade;
                for (int c = 0; c < 3; c++) {
                    float h = BossMesh.hash(seed + k * 13 + s * 3 + c * 7);
                    Vector3f base = new Vector3f(f).mul(t * length + (h - .5f) * .4f).add(new Vector3f(side).mul(s * w * (.85f + .3f * h))).add(0, .03f, 0);
                    Vector3f dir = new Vector3f(side).mul(s * (.35f + .3f * h)).add(new Vector3f(f).mul((h - .5f) * .5f)).add(0, 1, 0).normalize();
                    float len = (.35f + .65f * h) * grow;
                    RitualMesh.crystal(crystals, poseStack.last(), base, dir, len, .07f + .06f * h, h * 3, BossMesh.argb(0xE6F6FF, .92f), light);
                    RitualMesh.crystal(gleam, poseStack.last(), base, dir, len * 1.04f, .08f + .06f * h, h * 3, BossMesh.glow(BLUE, .3f), FULL);
                }
            }
        }
        // Frost glinting
        VertexConsumer flakes = BossMesh.light(buffer, FLAKE);
        for (int k = 0; k < 24; k++) {
            float t = BossMesh.hash(seed + k * 17), across = BossMesh.hash(seed + k * 19) * 2 - 1;
            float tw = Math.max(0, Mth.sin(age * .25f + k * 1.7f));
            Vector3f p = new Vector3f(f).mul(t * length).add(new Vector3f(side).mul(across * w * .8f)).add(0, .2f, 0);
            RitualMesh.billboard(flakes, poseStack, view, p, .26f * tw + .05f, age * .03f + k, -1, BossMesh.glow(WHITE, tw * fade), FULL);
        }
    }

    // ---------------------------------------------------------------- Hoarfrost Crown

    private void crown(SnowScene e, PoseStack poseStack, MultiBufferSource buffer, WaterMesh.View view, float age, float pt, int seed, int light) {
        float appear = BossMesh.ramp(age, 0, 10);
        float fade = 1 - BossMesh.ramp(age, e.getLife() - 15, e.getLife());
        float shown = appear * fade;
        if (shown <= .01f) {
            return;
        }
        VertexConsumer ice = BossMesh.see(buffer, ICE);
        VertexConsumer gleam = BossMesh.light(buffer, BossMesh.SHEEN);
        float spin = age * .045f;
        // A circlet of ice, then the great crystals standing on it, small ones leaning out between them
        ClockMesh.ring(poseStack.last(), buffer, .02f, .44f, .6f, FROST, .7f * shown, FULL);
        int points = 9;
        for (int k = 0; k < points; k++) {
            float a = spin + k * Mth.TWO_PI / points;
            float bob = .05f * Mth.sin(age * .12f + k);
            Vector3f base = new Vector3f(Mth.cos(a) * .52f, bob, Mth.sin(a) * .52f);
            Vector3f dir = new Vector3f(Mth.cos(a) * .25f, 1, Mth.sin(a) * .25f).normalize();
            float len = (k % 3 == 0 ? .95f : k % 3 == 1 ? .55f : .7f) * shown;
            RitualMesh.crystal(ice, poseStack.last(), base, dir, len, .1f, a, BossMesh.argb(0xEAF8FF, .95f), FULL);
            RitualMesh.crystal(gleam, poseStack.last(), base, dir, len * 1.05f, .11f, a, BossMesh.glow(BLUE, .4f), FULL);
            // A cold light at its heart, pulsing
            RitualMesh.crystal(BossMesh.light(buffer, BossMesh.GLOW), poseStack.last(), new Vector3f(dir).mul(len * .1f).add(base), dir, len * .7f, .04f, a,
                    BossMesh.glow(0x9AD8FF, (.5f + .3f * Mth.sin(age * .15f + k)) * shown), FULL);
            float b = a + Mth.PI / points;
            Vector3f lean = new Vector3f(Mth.cos(b) * .7f, .6f, Mth.sin(b) * .7f).normalize();
            RitualMesh.crystal(ice, poseStack.last(), new Vector3f(Mth.cos(b) * .5f, bob, Mth.sin(b) * .5f), lean, .32f * shown, .06f, b,
                    BossMesh.argb(0xEAF8FF, .9f), FULL);
        }
        // Snowflakes circling it, a cold halo
        VertexConsumer flakes = BossMesh.light(buffer, FLAKE);
        for (int k = 0; k < 8; k++) {
            float a = -age * .06f + k * Mth.TWO_PI / 8;
            Vector3f p = new Vector3f(Mth.cos(a) * .95f, .35f + .15f * Mth.sin(age * .1f + k), Mth.sin(a) * .95f);
            RitualMesh.billboard(flakes, poseStack, view, p, .28f, age * .05f + k, -1, BossMesh.glow(WHITE, .9f * shown), FULL);
        }
        ElementFx.bloom(poseStack, buffer, view, new Vector3f(0, .35f, 0), .9f, BLUE, .35f * shown);
        ClockMesh.ring(poseStack.last(), buffer, 1.05f + .04f * Mth.sin(age * .1f), .5f, .58f, FROST, .45f * shown, FULL);
        ClockMesh.ring(poseStack.last(), buffer, 1.3f + .04f * Mth.sin(age * .1f + 1), .3f, .35f, WHITE, .35f * shown, FULL);
        // An icicle loosed: it streaks to its mark, frost trailing it, and bursts there
        Entity victim = e.victim();
        float since = e.sinceStruck(pt);
        if (victim != null && since >= 0 && since < 14) {
            Vec3 to3 = victim.getPosition(pt).add(0, victim.getBbHeight() * .6, 0).subtract(e.getPosition(pt));
            Vector3f to = ElementFx.v(to3);
            float fly = Math.min(1, since / 4f);
            if (fly < 1) {
                Vector3f head = new Vector3f(to).mul(fly), tail = new Vector3f(to).mul(Math.max(0, fly - .45f));
                WaterMesh.streak(BossMesh.light(buffer, WaterMesh.STREAK), poseStack.last(), view.eye(), head, tail, .34f, BossMesh.glow(FROST, .9f), FULL);
                Vector3f d = new Vector3f(to).normalize();
                RitualMesh.crystal(ice, poseStack.last(), new Vector3f(head).sub(new Vector3f(d).mul(.8f)), d, .9f, .12f, age, BossMesh.argb(0xF0FAFF, 1), FULL);
                ElementFx.bloom(poseStack, buffer, view, head, .5f, BLUE, .7f);
            } else {
                float t = (since - 4) / 10f;
                ElementFx.bloom(poseStack, buffer, view, to, 1.2f * (1 + t), FROST, .9f * (1 - t));
                ElementFx.burst(poseStack, buffer, view, FLAKE, to, 1.6f, .8f, .3f, 12, t, WHITE, 1, seed + (int) since, true, FULL);
                for (int k = 0; k < 6; k++) {
                    float a = k * Mth.TWO_PI / 6;
                    Vector3f d = new Vector3f(Mth.cos(a), .4f, Mth.sin(a)).normalize();
                    RitualMesh.crystal(ice, poseStack.last(), new Vector3f(to).add(new Vector3f(d).mul(.9f * t)), d, .35f * (1 - t), .06f, a,
                            BossMesh.argb(0xF0FAFF, 1 - t), FULL);
                }
            }
        }
    }

    // ---------------------------------------------------------------- Whiteout

    private void whiteout(SnowScene e, PoseStack poseStack, MultiBufferSource buffer, WaterMesh.View view, float age, float pt, int seed, int light) {
        float r = e.getSize();
        float appear = BossMesh.ramp(age, 0, 25);
        float fade = 1 - BossMesh.ramp(age, e.getLife() - 20, e.getLife());
        float shown = appear * fade;
        // Frost spreading over the ground, deeper blue at the wall's foot
        BossMesh.annulus(BossMesh.ground(buffer, ICE), poseStack.last(), new Vector3f(0, .05f, 0), new Vector3f(1, 0, 0), new Vector3f(0, 0, -1), .01f,
                r * appear, 64, 6, 0, 0, BossMesh.argb(0xEAF6FF, .7f * fade), BossMesh.argb(0xB8DCF8, .55f * fade), light, false);
        // The wall of blizzard: snow driven round hard in streaks, each band its own way, clods of snow tumbling in it.
        // No fog or cloud sheets: they hid everything behind them
        VertexConsumer driven = BossMesh.see(buffer, WaterMesh.STREAK);
        for (int k = 0; k < 120; k++) {
            float h1 = BossMesh.hash(seed + k * 13), h2 = BossMesh.hash(seed + k * 17), h3 = BossMesh.hash(seed + k * 19);
            float turn = k % 3 == 1 ? -1 : 1;
            float a = h1 * Mth.TWO_PI + age * (.04f + .03f * h2) * turn;
            float rr = r * (.88f + .12f * h2), y = .15f + h3 * 5.3f;
            Vector3f head = new Vector3f(Mth.cos(a) * rr, y, Mth.sin(a) * rr);
            Vector3f tangent = new Vector3f(-Mth.sin(a) * turn, -.1f, Mth.cos(a) * turn).mul(.9f + .9f * h3);
            WaterMesh.streak(driven, poseStack.last(), view.eye(), head, new Vector3f(head).sub(tangent), .13f, BossMesh.argb(WHITE, .9f * shown), light);
        }
        VertexConsumer clumps = BossMesh.solid(buffer, SNOW);
        for (int k = 0; k < 18; k++) {
            float a = BossMesh.hash(seed + k * 29) * Mth.TWO_PI + age * (.05f + .02f * BossMesh.hash(seed + k)) * (k % 2 == 0 ? 1 : -1);
            float rr = r * (.9f + .08f * BossMesh.hash(seed + k * 31));
            float y = .4f + 4.4f * BossMesh.hash(seed + k * 37) + .4f * Mth.sin(age * .1f + k);
            ElementFx.lumpy(clumps, poseStack.last(), new Vector3f(Mth.cos(a) * rr, y, Mth.sin(a) * rr), (.18f + .14f * BossMesh.hash(seed + k * 41)) * shown,
                    5, 8, .3f, 2.5f, age * .05f, seed + k, .85f, BossMesh.argb(0xFFFFFF, 1), light);
        }
        // The storm's ceiling: snow clouds in body, each a bank of soft white lumps, turning slowly over it
        for (int k = 0; k < 9; k++) {
            float a = k * Mth.TWO_PI / 9 + age * .006f;
            float rr = r * (.15f + .75f * BossMesh.hash(seed + k * 7));
            Vector3f centre = new Vector3f(Mth.cos(a) * rr, 6 + .4f * BossMesh.hash(seed + k * 13), Mth.sin(a) * rr);
            for (int j = 0; j < 5; j++) {
                float ja = BossMesh.hash(seed + k * 17 + j) * Mth.TWO_PI, jr = j == 0 ? 0 : .7f + .5f * BossMesh.hash(seed + k * 19 + j);
                float size = (j == 0 ? 1 : .55f + .3f * BossMesh.hash(seed + k * 23 + j)) * shown;
                Vector3f p = new Vector3f(centre).add(Mth.cos(ja) * jr, (j == 0 ? .25f : 0) - .1f * jr, Mth.sin(ja) * jr);
                int col = BossMesh.mix(0xE4ECF6, 0xFFFFFF, BossMesh.hash(seed + k * 29 + j));
                ElementFx.lumpy(clumps, poseStack.last(), p, size, 8, 12, .12f, 1.6f, age * .01f, seed + k * 31 + j, .62f, BossMesh.argb(col, 1), FULL);
            }
        }
        // Snow piling up inside as it blows
        float pile = BossMesh.ramp(age, 10, 90) * fade;
        BossMesh.annulus(BossMesh.ground(buffer, SNOW), poseStack.last(), new Vector3f(0, .07f, 0), new Vector3f(1, 0, 0), new Vector3f(0, 0, -1), .01f, r * .97f, 64,
                6, 0, 0, BossMesh.argb(0xFFFFFF, .55f * pile), BossMesh.argb(0xFFFFFF, .85f * pile), light, false);
        // Snow driving round inside, soft streaks and flakes
        VertexConsumer streaks = BossMesh.light(buffer, WaterMesh.STREAK);
        for (int k = 0; k < 110; k++) {
            float h1 = BossMesh.hash(seed + k), h2 = BossMesh.hash(seed + k * 3), h3 = BossMesh.hash(seed + k * 7);
            float a = h1 * Mth.TWO_PI + age * (.05f + .06f * h2);
            float rr = r * (.2f + .78f * h2), y = .2f + h3 * 4.2f;
            Vector3f head = new Vector3f(Mth.cos(a) * rr, y, Mth.sin(a) * rr);
            Vector3f tangent = new Vector3f(-Mth.sin(a), -.12f, Mth.cos(a)).mul(.4f + .5f * h3);
            WaterMesh.streak(streaks, poseStack.last(), view.eye(), head, new Vector3f(head).sub(tangent), .07f, BossMesh.glow(WHITE, .32f * shown), FULL);
        }
        ElementFx.motes(poseStack, buffer, view, FLAKE, new Vector3f(), r * .95f, 4.5f, .3f, 50, age * 2, WHITE, .9f * shown, seed + 9);
        ElementFx.bloom(poseStack, buffer, view, new Vector3f(0, 1.8f, 0), r * .9f, FROST, .12f * shown);
        // The frozen shatter: a ring of cold running out, spikes of ice bursting from the ground, shards and flakes
        if (e.hasBurst()) {
            float t = e.sinceBurst(pt) / 22f;
            if (t < 1) {
                ElementFx.groundRing(poseStack, buffer, .08f, r * t, r * t + 1, FROST, 1 - t);
                ElementFx.bloom(poseStack, buffer, view, new Vector3f(0, 1, 0), r * 1.2f * (1 - t * .5f), BLUE, .7f * (1 - t));
                VertexConsumer ice = BossMesh.see(buffer, ICE);
                VertexConsumer gleam = BossMesh.light(buffer, BossMesh.SHEEN);
                float rise = Mth.sin(Mth.PI * Math.min(1, t * 1.4f));
                for (int k = 0; k < 30; k++) {
                    float a = BossMesh.hash(seed + k * 23) * Mth.TWO_PI, rr = r * (.25f + .7f * BossMesh.hash(seed + k * 29));
                    Vector3f base = new Vector3f(Mth.cos(a) * rr, 0, Mth.sin(a) * rr);
                    Vector3f dir = new Vector3f(Mth.cos(a) * .35f, 1, Mth.sin(a) * .35f).normalize();
                    float len = (1.2f + BossMesh.hash(seed + k) * 1.8f) * rise;
                    RitualMesh.crystal(ice, poseStack.last(), base, dir, len, .24f, a, BossMesh.argb(0xE6F6FF, .92f), light);
                    RitualMesh.crystal(gleam, poseStack.last(), base, dir, len * 1.03f, .26f, a, BossMesh.glow(FROST, .45f * (1 - t)), FULL);
                }
                ElementFx.burst(poseStack, buffer, view, FLAKE, new Vector3f(0, .5f, 0), r, 2.5f, .4f, 30, t, WHITE, 1, seed + 41, true, FULL);
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
