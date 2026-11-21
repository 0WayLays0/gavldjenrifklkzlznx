package com.waylays.arcanes.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.waylays.arcanes.ArcanesOublies;
import com.waylays.arcanes.client.render.boss.BossMesh;
import com.waylays.arcanes.client.render.ritual.RitualMesh;
import com.waylays.arcanes.client.render.water.WaterMesh;
import com.waylays.arcanes.entity.plant.PlantScene;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import static com.waylays.arcanes.client.render.ElementFx.FULL;

/**
 * Plant Magic's spells, drawn: a stem rising, leaves unfurling on it, its bud swelling and opening into a great flower
 * of curved petals with a golden heart, thorns flung out of it; a haze of golden pollen swirling and glittering, little
 * flowers opening on the ground under it; seeds flying in a fan, sprouting into puffballs that swell, glow and burst
 * into green spores; a shaft of sunlight, rays round it, leaves whirling up in it, grass and flowers springing up at its
 * foot; a flytrap rising from a bed of great leaves, its two lobes fringed with long teeth, red inside, snapping shut
 * and chewing.
 */
public class PlantSceneRenderer extends EntityRenderer<PlantScene> {
    public static final ResourceLocation LEAF = ArcanesOublies.id("textures/entity/plant/leaf.png");
    public static final ResourceLocation PETAL = ArcanesOublies.id("textures/entity/plant/petal.png");
    public static final ResourceLocation STEM = ArcanesOublies.id("textures/entity/plant/stem.png");
    public static final ResourceLocation FLESH = ArcanesOublies.id("textures/entity/plant/flesh.png");
    private static final int GREEN = 0x5ACF48, LEAFY = 0x4FB43E, PINK = 0xFF7AC0, GOLD = 0xFFD84A, SPORE = 0x9AE060, IVORY = 0xF4ECD8;

    public PlantSceneRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(PlantScene e, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource out, int packedLight) {
        // Recorded per render type, then written out one type after another (see SceneBuffers)
        SceneBuffers buffer = SceneBuffers.begin();
        float age = e.tickCount + partialTicks;
        WaterMesh.View view = WaterViews.of(this.entityRenderDispatcher, e, partialTicks);
        int seed = e.getId() * 31;
        int light = ElementFx.light(e, 11);
        switch (e.getKind()) {
            case BLOOM -> bloom(e, poseStack, buffer, view, age, seed, light);
            case POLLEN -> pollen(e, poseStack, buffer, view, age, seed, light);
            case SEEDS -> seeds(e, poseStack, buffer, view, age, seed, light);
            case SUN -> sun(e, poseStack, buffer, view, age, seed, light);
            case MAW -> maw(e, poseStack, buffer, view, age, seed, light);
        }
        buffer.flush(out);
        super.render(e, entityYaw, partialTicks, poseStack, out, packedLight);
    }

    /**
     * A leaf or a petal (its shape is in its texture) from {@code base} out along {@code dir}, {@code width} across
     * {@code side}, curving toward {@code bend} (its tip pushed {@code bend} times its length that way): a strip of
     * {@code segs}, its colour running from {@code root} to {@code tip}, lit as from above.
     */
    private static void blade(VertexConsumer c, PoseStack.Pose p, Vector3f base, Vector3f dir, Vector3f side, Vector3f bend, float length, float width,
                              int segs, int root, int tip, int light) {
        if (length <= .01f) {
            return;
        }
        Vector3f s = new Vector3f(side).normalize().mul(width * .5f);
        Vector3f prev = null;
        for (int i = 0; i <= segs; i++) {
            float t = (float) i / segs;
            Vector3f at = new Vector3f(dir).mul(length * t).add(new Vector3f(bend).mul(length * t * t)).add(base);
            if (prev != null) {
                float t0 = (float) (i - 1) / segs;
                Vector3f a = new Vector3f(prev).sub(s), b = new Vector3f(prev).add(s), d = new Vector3f(at).add(s), f = new Vector3f(at).sub(s);
                Vector3f n = new Vector3f(b).sub(a).cross(new Vector3f(f).sub(a));
                n = n.lengthSquared() < 1.0E-12f ? new Vector3f(BossMesh.UP) : n.normalize();
                if (n.y < 0) {
                    n.negate();
                }
                n.add(0, .6f, 0).normalize();
                BossMesh.quad(c, p, a, b, d, f, 0, 1 - t0, 1, 1 - t0, 1, 1 - t, 0, 1 - t, BossMesh.blend(root, tip, t0), BossMesh.blend(root, tip, t), light, n);
            }
            prev = at;
        }
    }

    /** A flower of {@code petals} petals at {@code at}, opening upward ({@code open} 0: a bud, 1: wide open), a gold heart. */
    private static void flower(MultiBufferSource buffer, PoseStack.Pose p, Vector3f at, float size, float open, float spin, int petals, int rgb, float alpha,
                               int light) {
        if (alpha <= .01f || size <= .01f) {
            return;
        }
        VertexConsumer c = BossMesh.see(buffer, PETAL);
        float tilt = Mth.lerp(open, .25f, 1.35f);
        for (int k = 0; k < petals; k++) {
            float a = spin + k * Mth.TWO_PI / petals;
            Vector3f out = new Vector3f(Mth.cos(a), 0, Mth.sin(a));
            Vector3f dir = new Vector3f(out).mul(Mth.sin(tilt)).add(0, Mth.cos(tilt), 0);
            blade(c, p, at, dir, new Vector3f(-Mth.sin(a), 0, Mth.cos(a)), new Vector3f(0, .25f * open, 0), size, size * .75f, 3,
                    BossMesh.argb(BossMesh.mix(rgb, 0xFFFFFF, .5f), alpha), BossMesh.argb(rgb, alpha), light);
        }
        BossMesh.sphere(BossMesh.see(buffer, SnowSceneRenderer.SNOW), p, new Vector3f(at).add(0, size * .05f, 0), size * .2f, 4, 8, 0, 0, .5f, BossMesh.argb(GOLD, alpha), FULL);
    }

    // ---------------------------------------------------------------- Thorn Bloom

    private void bloom(PlantScene e, PoseStack poseStack, MultiBufferSource buffer, WaterMesh.View view, float age, int seed, int light) {
        float grow = BossMesh.ramp(age, 0, PlantScene.BLOOM_OPEN);
        float open = BossMesh.ramp(age, PlantScene.BLOOM_OPEN - 3, PlantScene.BLOOM_OPEN + 5);
        float wither = BossMesh.ramp(age, e.getLife() - 18, e.getLife());
        float height = 2.3f * grow * (1 - wither * .5f);
        Vector3f sway = new Vector3f(.12f * Mth.sin(age * .08f), 0, .08f * Mth.cos(age * .07f));
        // The stem, thorns on it, leaves unfurling from it and lying round its foot
        VertexConsumer stem = BossMesh.solid(buffer, STEM);
        BossMesh.horn(stem, poseStack.last(), new Vector3f(), new Vector3f(0, 1, 0), sway, height + .05f, .14f, 8, 8, BossMesh.argb(0x3A7A30, 1),
                BossMesh.argb(0x7ADA60, 1), light);
        for (int k = 0; k < 6; k++) {
            float t = .2f + .12f * k, a = k * 2.4f;
            Vector3f at = new Vector3f(sway).mul(t * t).add(0, height * t, 0);
            BossMesh.horn(stem, poseStack.last(), at, new Vector3f(Mth.cos(a), .4f, Mth.sin(a)).normalize(), new Vector3f(), .16f * grow, .035f, 4, 1,
                    BossMesh.argb(0x3A7A30, 1), BossMesh.argb(0xE8F0C0, 1), light);
        }
        VertexConsumer leaves = BossMesh.see(buffer, LEAF);
        int leaf = BossMesh.argb(BossMesh.mix(LEAFY, 0x8A7A40, wither), 1);
        for (int k = 0; k < 2; k++) {
            float t = .35f + .25f * k, a = k * Mth.PI + .6f;
            Vector3f at = new Vector3f(sway).mul(t * t).add(0, height * t, 0);
            Vector3f out = new Vector3f(Mth.cos(a), .5f, Mth.sin(a)).normalize();
            blade(leaves, poseStack.last(), at, out, new Vector3f(-Mth.sin(a), 0, Mth.cos(a)), new Vector3f(0, -.5f, 0), 1.1f * grow, .6f * grow, 4, leaf, leaf,
                    light);
        }
        for (int k = 0; k < 5; k++) {
            float a = k * Mth.TWO_PI / 5 + .4f;
            blade(leaves, poseStack.last(), new Vector3f(0, .05f, 0), new Vector3f(Mth.cos(a), .45f, Mth.sin(a)).normalize(), new Vector3f(-Mth.sin(a), 0, Mth.cos(a)),
                    new Vector3f(0, -.35f, 0), 1.4f * grow, .8f * grow, 4, leaf, leaf, light);
        }
        // The head: a bud swelling, opening into a great flower, three rings of petals curving out round a golden heart
        Vector3f head = new Vector3f(sway).add(0, height, 0);
        VertexConsumer petals = BossMesh.see(buffer, PETAL);
        float size = (.55f + .45f * grow) * (1 + .45f * open);
        int[] counts = {8, 7, 6};
        for (int ring = 0; ring < 3; ring++) {
            float tilt = Mth.lerp(open, .18f, 1.45f - ring * .32f) + wither * .5f;
            float len = size * (1.3f - ring * .28f);
            int col = BossMesh.mix(ring == 0 ? PINK : ring == 1 ? 0xFF9AD0 : 0xFFC8E4, 0x8A5A60, wither);
            for (int k = 0; k < counts[ring]; k++) {
                float a = k * Mth.TWO_PI / counts[ring] + ring * .5f + age * .004f;
                Vector3f out = new Vector3f(Mth.cos(a), 0, Mth.sin(a));
                Vector3f dir = new Vector3f(out).mul(Mth.sin(tilt)).add(0, Mth.cos(tilt), 0);
                blade(petals, poseStack.last(), head, dir, new Vector3f(-Mth.sin(a), 0, Mth.cos(a)), new Vector3f(0, .3f * open - .15f * wither, 0), len,
                        len * .72f, 4, BossMesh.argb(BossMesh.mix(col, 0xFFFFFF, .45f), 1 - wither * .4f), BossMesh.argb(col, 1 - wither * .4f), light);
            }
        }
        // The heart, stamens round it with glowing tips
        BossMesh.sphere(BossMesh.see(buffer, SnowSceneRenderer.SNOW), poseStack.last(), new Vector3f(head).add(0, .05f, 0), .2f + .1f * open, 6, 10, 0, 0, .5f,
                BossMesh.argb(GOLD, 1), FULL);
        VertexConsumer glow = BossMesh.light(buffer, BossMesh.GLOW);
        for (int k = 0; k < 10; k++) {
            float a = k * Mth.TWO_PI / 10;
            Vector3f d = new Vector3f(Mth.cos(a) * .5f, 1, Mth.sin(a) * .5f).normalize();
            Vector3f tip = new Vector3f(d).mul(.45f * open).add(head);
            BossMesh.horn(stem, poseStack.last(), head, d, new Vector3f(), .45f * open, .02f, 4, 1, BossMesh.argb(0xE8D890, 1), BossMesh.argb(GOLD, 1), FULL);
            BossMesh.sphere(glow, poseStack.last(), tip, .06f, 4, 6, 0, 0, 1, BossMesh.glow(GOLD, open * (1 - wither)), FULL);
        }
        ElementFx.bloom(poseStack, buffer, view, head, .9f + .6f * open, PINK, .35f * grow * (1 - wither));
        ElementFx.motes(poseStack, buffer, view, BossMesh.MOTE, new Vector3f(head).add(0, -.6f, 0), 1.4f, 2, .14f, 14, age, GOLD, open * (1 - wither), seed);
        // Its thorns flung out all round when it opens, petals torn off with them
        float since = age - PlantScene.BLOOM_OPEN;
        if (since >= 0 && since < 14) {
            float t = since / 14f;
            VertexConsumer streaks = BossMesh.light(buffer, WaterMesh.STREAK);
            for (int k = 0; k < 22; k++) {
                float a = k * Mth.TWO_PI / 22 + BossMesh.hash(seed + k) * .3f;
                Vector3f dir = new Vector3f(Mth.cos(a), -.25f + .35f * BossMesh.hash(seed + k * 3), Mth.sin(a)).normalize();
                Vector3f at = new Vector3f(dir).mul(e.getSize() * t).add(head);
                BossMesh.horn(stem, poseStack.last(), at, dir, new Vector3f(), .7f * (1 - t * .4f), .07f, 5, 2, BossMesh.argb(0x2E5A24, 1), BossMesh.argb(0xE8F4C0, 1),
                        light);
                WaterMesh.streak(streaks, poseStack.last(), view.eye(), at, new Vector3f(dir).mul(-1.4f).add(at), .12f, BossMesh.glow(0xC8F0A0, .6f * (1 - t)), FULL);
            }
            for (int k = 0; k < 10; k++) {
                float a = BossMesh.hash(seed + k * 7) * Mth.TWO_PI;
                Vector3f p = new Vector3f(Mth.cos(a) * 3 * t, (1.2f * t - 2 * t * t) * 2, Mth.sin(a) * 3 * t).add(head);
                RitualMesh.billboard(petals, poseStack, view, p, .45f, t * 8 + k, -1, BossMesh.argb(PINK, 1 - t), light);
            }
            ElementFx.groundRing(poseStack, buffer, .07f, e.getSize() * t, e.getSize() * t + .6f, PINK, 1 - t);
            ElementFx.bloom(poseStack, buffer, view, head, 2, PINK, .8f * (1 - t));
        }
    }

    // ---------------------------------------------------------------- Pollen Cloud

    private void pollen(PlantScene e, PoseStack poseStack, MultiBufferSource buffer, WaterMesh.View view, float age, int seed, int light) {
        float r = e.getSize();
        float shown = BossMesh.ramp(age, 0, 20) * (1 - BossMesh.ramp(age, e.getLife() - 20, e.getLife()));
        if (shown <= .01f) {
            return;
        }
        // A golden haze turning slowly, lit by its own glitter
        VertexConsumer cloud = BossMesh.see(buffer, BossMesh.CLOUD);
        for (int k = 0; k < 20; k++) {
            float a = k * Mth.TWO_PI / 20 + age * .012f;
            float rr = r * (.3f + .55f * BossMesh.hash(seed + k));
            float y = .6f + 1.6f * BossMesh.hash(seed + k * 3) + .25f * Mth.sin(age * .04f + k);
            RitualMesh.billboard(cloud, poseStack, view, new Vector3f(Mth.cos(a) * rr, y, Mth.sin(a) * rr), 2.6f + BossMesh.hash(seed + k * 5), a, -1,
                    BossMesh.argb(0xFFE890, .3f * shown), light);
        }
        VertexConsumer haze = BossMesh.light(buffer, BossMesh.CLOUD);
        for (int k = 0; k < 8; k++) {
            float a = k * Mth.TWO_PI / 8 - age * .008f;
            RitualMesh.billboard(haze, poseStack, view, new Vector3f(Mth.cos(a) * r * .5f, 1.3f, Mth.sin(a) * r * .5f), 3.5f, a, -1,
                    BossMesh.glow(GOLD, .12f * shown), FULL);
        }
        // Grains of pollen swirling up in a slow whirl, glittering
        VertexConsumer motes = BossMesh.light(buffer, BossMesh.MOTE);
        for (int k = 0; k < 140; k++) {
            float h1 = BossMesh.hash(seed + k), h2 = BossMesh.hash(seed + k * 3), h3 = BossMesh.hash(seed + k * 7);
            float a = h1 * Mth.TWO_PI + age * (.006f + .012f * h2);
            float rr = r * Mth.sqrt(h2);
            float y = (.2f + h3 * 3 + age * .01f * (.5f + h1)) % 3.2f;
            float tw = .5f + .5f * Mth.sin(age * .3f + k * 1.3f);
            float edge = Mth.sin(Mth.PI * y / 3.2f);
            RitualMesh.billboard(motes, poseStack, view, new Vector3f(Mth.cos(a) * rr, y, Mth.sin(a) * rr), .1f + .12f * tw, 0, -1,
                    BossMesh.glow(k % 5 == 0 ? 0xFFFFFF : GOLD, shown * tw * edge), FULL);
        }
        // Flowers opening on the ground under it, petals drifting down
        int[] colours = {0xFFF07A, 0xFFFFFF, PINK, 0xC8A0FF, 0xFFB060};
        for (int k = 0; k < 18; k++) {
            float a = BossMesh.hash(seed + k * 11) * Mth.TWO_PI, rr = r * .9f * Mth.sqrt(BossMesh.hash(seed + k * 13));
            float bloom = BossMesh.ramp(age, k * 2, k * 2 + 15);
            flower(buffer, poseStack.last(), new Vector3f(Mth.cos(a) * rr, .04f, Mth.sin(a) * rr), .3f * bloom, bloom * .9f, a, 5, colours[k % 5], shown, light);
        }
        VertexConsumer petals = BossMesh.see(buffer, PETAL);
        for (int k = 0; k < 12; k++) {
            float fall = 1 - (BossMesh.hash(seed + k * 17) + age * .006f) % 1;
            float a = BossMesh.hash(seed + k * 19) * Mth.TWO_PI + age * .02f, rr = r * .8f * BossMesh.hash(seed + k * 23);
            Vector3f p = new Vector3f(Mth.cos(a) * rr, fall * 3, Mth.sin(a) * rr);
            RitualMesh.billboard(petals, poseStack, view, p, .28f, age * .05f + k, -1, BossMesh.argb(colours[k % 5], shown * Mth.sin(Mth.PI * fall)), light);
        }
        ElementFx.groundRing(poseStack, buffer, .05f, r * .9f, r, GOLD, .35f * shown);
    }

    // ---------------------------------------------------------------- Seed Volley

    private void seeds(PlantScene e, PoseStack poseStack, MultiBufferSource buffer, WaterMesh.View view, float age, int seed, int light) {
        VertexConsumer husk = BossMesh.solid(buffer, STEM);
        VertexConsumer puff = BossMesh.see(buffer, SnowSceneRenderer.SNOW);
        VertexConsumer leaves = BossMesh.see(buffer, LEAF);
        VertexConsumer glow = BossMesh.light(buffer, BossMesh.GLOW);
        float fade = 1 - BossMesh.ramp(age, e.getLife() - 6, e.getLife());
        for (int i = 0; i < PlantScene.SEEDS; i++) {
            Vec3 spot3 = e.seedSpot(i);
            Vector3f spot = new Vector3f((float) spot3.x, 0, (float) spot3.z);
            if (age < PlantScene.SEED_FLIGHT) {
                // In flight: a seed spinning, a green streak behind it
                float t = age / PlantScene.SEED_FLIGHT, t0 = Math.max(0, t - .25f);
                Vector3f at = new Vector3f(spot).mul(t).add(0, 1.2f + 2.5f * 4 * t * (1 - t) - 1.2f * t, 0);
                Vector3f was = new Vector3f(spot).mul(t0).add(0, 1.2f + 2.5f * 4 * t0 * (1 - t0) - 1.2f * t0, 0);
                BossMesh.sphere(husk, poseStack.last(), at, .16f, 5, 8, age * .2f, 0, 1, BossMesh.argb(0xA07A44, 1), light);
                WaterMesh.streak(BossMesh.light(buffer, WaterMesh.STREAK), poseStack.last(), view.eye(), at, was, .25f, BossMesh.glow(SPORE, .8f), FULL);
                ElementFx.bloom(poseStack, buffer, view, at, .35f, SPORE, .6f);
                continue;
            }
            if (age < PlantScene.SEED_BURST) {
                // Sprouted: a stalk, two leaves, a puffball swelling and throbbing, glowing from within
                float grow = BossMesh.ramp(age, PlantScene.SEED_FLIGHT, PlantScene.SEED_BURST - 3);
                float swell = 1 + .14f * Mth.sin(age * (1.2f + grow)) * grow;
                float stalk = .45f * grow + .03f;
                BossMesh.horn(husk, poseStack.last(), spot, new Vector3f(0, 1, 0), new Vector3f(), stalk, .07f, 6, 2, BossMesh.argb(0x3A7A30, 1),
                        BossMesh.argb(0x8ACF68, 1), light);
                for (int s = -1; s <= 1; s += 2) {
                    float a = i * 1.7f + (s > 0 ? 0 : Mth.PI);
                    blade(leaves, poseStack.last(), new Vector3f(spot).add(0, .05f, 0), new Vector3f(Mth.cos(a), .5f, Mth.sin(a)).normalize(),
                            new Vector3f(-Mth.sin(a), 0, Mth.cos(a)), new Vector3f(0, -.3f, 0), .55f * grow, .35f * grow, 3, BossMesh.argb(LEAFY, 1),
                            BossMesh.argb(LEAFY, 1), light);
                }
                float pr = (.12f + .45f * grow) * swell;
                Vector3f ball = new Vector3f(spot).add(0, stalk + pr * .8f, 0);
                BossMesh.sphere(puff, poseStack.last(), ball, pr, 10, 16, i * .1f, 0, 1, BossMesh.argb(0xF0E6C0, 1), light);
                for (int k = 0; k < 7; k++) {
                    float a = BossMesh.hash(seed + i * 9 + k) * Mth.TWO_PI, b = BossMesh.hash(seed + i * 9 + k * 3) * 2.2f;
                    Vector3f at = new Vector3f(Mth.sin(b) * Mth.cos(a), Mth.cos(b), Mth.sin(b) * Mth.sin(a)).mul(pr).add(ball);
                    BossMesh.sphere(husk, poseStack.last(), at, pr * .12f, 3, 5, 0, 0, 1, BossMesh.argb(0xB89A60, 1), light);
                }
                BossMesh.sphere(glow, poseStack.last(), ball, pr * 1.06f, 8, 12, 0, 0, 1, BossMesh.glow(SPORE, .25f * grow * (1 + Mth.sin(age * .8f))), FULL);
                continue;
            }
            // Burst: a cloud of green spores rolling out, spores glittering in it
            float t = (age - PlantScene.SEED_BURST) / 16f;
            if (t < 1) {
                Vector3f at = new Vector3f(spot).add(0, .6f, 0);
                ElementFx.puffs(poseStack, buffer, view, BossMesh.CLOUD, at, 2.4f * t + .2f, .6f, 1.2f + 1.2f * t, 7, t * 40, .02f, SPORE, .75f * (1 - t) * fade,
                        seed + i * 13, light);
                ElementFx.burst(poseStack, buffer, view, BossMesh.MOTE, at, 2, 1.2f, .22f, 14, t, SPORE, 1, seed + i * 7, true, FULL);
                ElementFx.groundRing(poseStack, buffer, .06f, 2 * t, 2 * t + .35f, SPORE, (1 - t) * .8f);
                ElementFx.bloom(poseStack, buffer, view, at, 1.2f, SPORE, .7f * (1 - t));
            }
        }
    }

    // ---------------------------------------------------------------- Photosynthesis

    private void sun(PlantScene e, PoseStack poseStack, MultiBufferSource buffer, WaterMesh.View view, float age, int seed, int light) {
        float shown = BossMesh.ramp(age, 0, 14) * (1 - BossMesh.ramp(age, e.getLife() - 20, e.getLife()));
        if (shown <= .01f) {
            return;
        }
        float r = e.getSize();
        // The shaft of sunlight: a soft wide glow, a bright core, rays slanting round it
        VertexConsumer beam = BossMesh.light(buffer, BossMesh.BEAM);
        float pulse = .85f + .15f * Mth.sin(age * .15f);
        RitualMesh.shaft(beam, poseStack.last(), view.eye(), new Vector3f(0, 18, 0), new Vector3f(), 4.2f * pulse, BossMesh.glow(0xFFE89A, .22f * shown), FULL);
        RitualMesh.shaft(beam, poseStack.last(), view.eye(), new Vector3f(0, 18, 0), new Vector3f(), 2.2f * pulse, BossMesh.glow(0xFFF0B0, .35f * shown), FULL);
        RitualMesh.shaft(beam, poseStack.last(), view.eye(), new Vector3f(0, 18, 0), new Vector3f(), .9f, BossMesh.glow(0xFFFDF0, .6f * shown), FULL);
        for (int k = 0; k < 6; k++) {
            float a = k * Mth.TWO_PI / 6 + age * .01f;
            float tw = .5f + .5f * Mth.sin(age * .09f + k * 2);
            Vector3f top = new Vector3f(Mth.cos(a) * 3.5f, 16, Mth.sin(a) * 3.5f), foot = new Vector3f(Mth.cos(a) * r * .8f, 0, Mth.sin(a) * r * .8f);
            RitualMesh.shaft(beam, poseStack.last(), view.eye(), top, foot, .7f, BossMesh.glow(0xFFE89A, .2f * tw * shown), FULL);
        }
        // Sunlit ground: warm rings, grass and flowers springing up
        ElementFx.groundRing(poseStack, buffer, .05f, 0, r * .6f, 0xFFF0B0, .35f * shown);
        ElementFx.groundRing(poseStack, buffer, .055f, r * .75f, r, GOLD, .5f * shown);
        VertexConsumer grass = BossMesh.see(buffer, LEAF);
        for (int k = 0; k < 26; k++) {
            float a = BossMesh.hash(seed + k * 3) * Mth.TWO_PI, rr = r * (.3f + .65f * BossMesh.hash(seed + k * 5));
            float g = BossMesh.ramp(age, k * .8f, k * .8f + 12) * shown;
            Vector3f at = new Vector3f(Mth.cos(a) * rr, .03f, Mth.sin(a) * rr);
            blade(grass, poseStack.last(), at, new Vector3f(Mth.cos(a) * .3f, 1, Mth.sin(a) * .3f).normalize(), new Vector3f(-Mth.sin(a), 0, Mth.cos(a)),
                    new Vector3f(Mth.cos(a) * .3f, 0, Mth.sin(a) * .3f), .5f * g, .22f * g, 3, BossMesh.argb(0x3A8A30, 1), BossMesh.argb(0x9AE070, 1), light);
        }
        for (int k = 0; k < 8; k++) {
            float a = BossMesh.hash(seed + k * 29) * Mth.TWO_PI, rr = r * (.35f + .55f * BossMesh.hash(seed + k * 31));
            float g = BossMesh.ramp(age, 8 + k * 2, 20 + k * 2);
            flower(buffer, poseStack.last(), new Vector3f(Mth.cos(a) * rr, .05f, Mth.sin(a) * rr), .28f * g, g, a, 5, k % 2 == 0 ? 0xFFF07A : 0xFFFFFF, shown,
                    light);
        }
        // Leaves whirling up in the light, tumbling as they go
        VertexConsumer leaves = BossMesh.see(buffer, LEAF);
        for (int k = 0; k < 22; k++) {
            float h = BossMesh.hash(seed + k);
            float rise = (h + age * .008f) % 1;
            float a = k * Mth.TWO_PI / 22 + age * .07f;
            float rr = .8f + .4f * Mth.sin(age * .05f + k) + rise * .6f;
            Vector3f at = new Vector3f(Mth.cos(a) * rr, .2f + rise * 4, Mth.sin(a) * rr);
            float tumble = age * (.08f + .05f * h) + k;
            Vector3f dir = new Vector3f(Mth.cos(tumble), Mth.sin(tumble * .7f), Mth.sin(tumble)).normalize();
            Vector3f side = new Vector3f(-Mth.sin(a), 0, Mth.cos(a));
            float fadeIn = Mth.sin(Mth.PI * rise) * shown;
            int col = BossMesh.argb(k % 3 == 0 ? 0xB8E070 : LEAFY, fadeIn);
            blade(leaves, poseStack.last(), new Vector3f(dir).mul(-.2f).add(at), dir, side, new Vector3f(0, -.1f, 0), .42f, .3f, 2, col, col, light);
        }
        ElementFx.motes(poseStack, buffer, view, BossMesh.MOTE, new Vector3f(), 1.2f, 4.5f, .16f, 30, age, GOLD, shown, seed + 3);
        ElementFx.bloom(poseStack, buffer, view, new Vector3f(0, 1, 0), 1.3f, 0xFFF0B0, .35f * shown);
        // Healing: green sparks rising round the caster
        ElementFx.motes(poseStack, buffer, view, BossMesh.MOTE, new Vector3f(), .7f, 2.4f, .2f, 10, age * 2, GREEN, .9f * shown, seed + 9);
    }

    // ---------------------------------------------------------------- Verdant Maw

    private void maw(PlantScene e, PoseStack poseStack, MultiBufferSource buffer, WaterMesh.View view, float age, int seed, int light) {
        float r = e.getSize();
        float rise = BossMesh.ramp(age, 0, 10);
        float sink = BossMesh.ramp(age, e.getLife() - 15, e.getLife());
        // The lobes: wide open while it rises, snapping shut, then chewing
        float angle;
        if (age < PlantScene.MAW_SNAP) {
            angle = 1.25f;
        } else if (age < PlantScene.MAW_SNAP + 3) {
            angle = Mth.lerp((age - PlantScene.MAW_SNAP) / 3f, 1.25f, 0);
        } else if (sink > 0) {
            angle = .9f * sink;
        } else {
            angle = .08f + .16f * Math.abs(Mth.sin(age * .45f));
        }
        // The bed of great leaves round it, curling up at their tips, its vines lashing
        VertexConsumer leaves = BossMesh.see(buffer, LEAF);
        int leaf = BossMesh.argb(LEAFY, 1);
        for (int k = 0; k < 8; k++) {
            float a = k * Mth.TWO_PI / 8 + .2f;
            float len = r * 1.5f * rise * (1 - sink * .5f);
            blade(leaves, poseStack.last(), new Vector3f(0, .05f, 0), new Vector3f(Mth.cos(a), .08f, Mth.sin(a)).normalize(), new Vector3f(-Mth.sin(a), 0, Mth.cos(a)),
                    new Vector3f(0, .45f, 0), len, r * .75f, 5, leaf, BossMesh.argb(0x8AD860, 1), light);
        }
        VertexConsumer stem = BossMesh.solid(buffer, STEM);
        for (int k = 0; k < 5; k++) {
            float a = k * Mth.TWO_PI / 5 + .8f;
            Vector3f bend = new Vector3f(Mth.cos(a) * .9f, -.3f, Mth.sin(a) * .9f).mul(1 + .35f * Mth.sin(age * .2f + k));
            BossMesh.horn(stem, poseStack.last(), new Vector3f(Mth.cos(a) * r * .55f, 0, Mth.sin(a) * r * .55f),
                    new Vector3f(Mth.cos(a) * .3f, 1, Mth.sin(a) * .3f).normalize(), bend, 2.4f * rise * (1 - sink), .13f, 6, 8, BossMesh.argb(0x2E6A24, 1),
                    BossMesh.argb(0x7ACF58, 1), light);
        }
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(-e.getDir()));
        poseStack.translate(0, (rise - 1 - sink) * r * 1.4f, 0);
        float h = r * .95f;
        // Its thick stalk
        BossMesh.horn(stem, poseStack.last(), new Vector3f(), new Vector3f(0, 1, 0), new Vector3f(), h, r * .22f, 10, 3, BossMesh.argb(0x3A7A30, 1),
                BossMesh.argb(0x5AA848, 1), light);
        VertexConsumer skin = BossMesh.see(buffer, STEM);
        VertexConsumer flesh = BossMesh.see(buffer, FLESH);
        VertexConsumer teeth = BossMesh.solid(buffer, SnowSceneRenderer.SNOW);
        VertexConsumer glow = BossMesh.light(buffer, BossMesh.GLOW);
        // The lower lobe: a shallow bowl, green without, red within, long teeth fringing it
        poseStack.pushPose();
        poseStack.translate(0, h, 0);
        lobe(poseStack, skin, flesh, glow, r, .5f, 1, angle, light);
        poseStack.popPose();
        rimTeeth(teeth, poseStack.last(), new Vector3f(0, h, 0), r, 1, seed, light);
        // The upper lobe, hinged at the back
        poseStack.pushPose();
        poseStack.translate(0, h, -r);
        poseStack.mulPose(Axis.XP.rotation(-angle));
        poseStack.translate(0, 0, r);
        lobe(poseStack, skin, flesh, glow, r, 0, .5f, angle, light);
        rimTeeth(teeth, poseStack.last(), new Vector3f(), r, -1, seed + 50, light);
        // Spots of venom on its lid
        for (int k = 0; k < 7; k++) {
            float a = BossMesh.hash(seed + k) * Mth.TWO_PI, b = .25f + .5f * BossMesh.hash(seed + k * 3);
            Vector3f at = new Vector3f(Mth.sin(b) * Mth.cos(a) * r * 1.01f, Mth.cos(b) * r * .5f * 1.02f, Mth.sin(b) * Mth.sin(a) * r * 1.01f);
            BossMesh.sphere(glow, poseStack.last(), at, .12f, 4, 6, 0, 0, 1, BossMesh.glow(0xE0FF60, .7f), FULL);
        }
        poseStack.popPose();
        poseStack.popPose();
        // A glow of its red maw while open; drool dripping as it chews
        if (angle > .3f && sink <= 0) {
            ElementFx.bloom(poseStack, buffer, view, new Vector3f(0, h + (rise - 1) * r * 1.4f + .3f, 0), r * .7f, 0xFF4060, .4f * angle);
        }
        float since = age - PlantScene.MAW_SNAP;
        if (since >= 0 && since < 14) {
            float t = since / 14f;
            ElementFx.groundRing(poseStack, buffer, .08f, r * (1 + 2 * t), r * (1 + 2 * t) + .7f, GREEN, 1 - t);
            ElementFx.bloom(poseStack, buffer, view, new Vector3f(0, h, 0), r * 1.4f, 0xFF4060, .6f * (1 - t));
            ElementFx.burst(poseStack, buffer, view, LEAF, new Vector3f(0, h, 0), r * 2, 2, .4f, 14, t, LEAFY, 1, seed + 3, false, light);
        } else if (since >= 14 && sink <= 0) {
            ElementFx.motes(poseStack, buffer, view, BossMesh.MOTE, new Vector3f(0, .3f, 0), r * .9f, h, .18f, 10, age, 0xC8FF60, .8f, seed + 5);
        }
    }

    /**
     * One lobe of the trap ({@code lat0} to {@code lat1} of a sphere: the top half or the bottom), flattened into a
     * shallow shell, {@code r} across: green skin outside, red flesh inside, a glow of red inside while open.
     */
    private static void lobe(PoseStack poseStack, VertexConsumer skin, VertexConsumer flesh, VertexConsumer glow, float r, float lat0, float lat1, float open,
                             int light) {
        poseStack.pushPose();
        poseStack.scale(1, .5f, 1);
        BossMesh.sphere(skin, poseStack.last(), new Vector3f(), r, 10, 24, 0, lat0, lat1, BossMesh.argb(0x6ABE4A, 1), light);
        BossMesh.sphere(flesh, poseStack.last(), new Vector3f(), r * .95f, 10, 24, 0, lat0, lat1, BossMesh.argb(0xFF8A90, 1), FULL);
        if (open > .2f) {
            BossMesh.sphere(glow, poseStack.last(), new Vector3f(), r * .93f, 8, 18, 0, lat0, lat1, BossMesh.glow(0xFF3050, .25f * Math.min(1, open)), FULL);
        }
        poseStack.popPose();
    }

    /** Long teeth round a lobe's rim, reaching out and up (the lower lobe: {@code way} 1) or out and down (the upper: -1). */
    private static void rimTeeth(VertexConsumer c, PoseStack.Pose p, Vector3f centre, float r, int way, int seed, int light) {
        int n = 18;
        for (int k = 0; k < n; k++) {
            float a = k * Mth.TWO_PI / n + (way < 0 ? Mth.PI / n : 0);
            Vector3f rim = new Vector3f(Mth.cos(a) * r * .98f, 0, Mth.sin(a) * r * .98f).add(centre);
            Vector3f dir = new Vector3f(Mth.cos(a) * .5f, way, Mth.sin(a) * .5f).normalize();
            BossMesh.horn(c, p, rim, dir, new Vector3f(Mth.cos(a) * .25f, 0, Mth.sin(a) * .25f), (.45f + .25f * BossMesh.hash(seed + k)) * r / 2.4f * 1.5f,
                    .07f * r / 2.4f * 1.5f, 5, 3, BossMesh.argb(0x9ACF60, 1), BossMesh.argb(IVORY, 1), light);
        }
    }

    @Override
    public boolean shouldRender(PlantScene e, Frustum frustum, double x, double y, double z) {
        return true;
    }

    @Override
    public ResourceLocation getTextureLocation(PlantScene e) {
        return LEAF;
    }
}
