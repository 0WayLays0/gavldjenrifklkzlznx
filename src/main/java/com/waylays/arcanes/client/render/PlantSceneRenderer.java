package com.waylays.arcanes.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.waylays.arcanes.ArcanesOublies;
import com.waylays.arcanes.client.render.boss.BossMesh;
import com.waylays.arcanes.client.render.clockwork.ClockMesh;
import com.waylays.arcanes.client.render.ritual.RitualMesh;
import com.waylays.arcanes.client.render.water.WaterMesh;
import com.waylays.arcanes.entity.plant.PlantScene;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/**
 * Plant Magic's spells, drawn: a stem rising, its bud swelling and opening into a great pink flower, thorns flung out
 * of it; golden pollen drifting and glittering, little flowers opening on the ground under it; seeds flying in a fan,
 * sprouting into puffballs that swell and burst into green spores; a shaft of sunlight with leaves whirling up in it;
 * a carnivorous flower rising from a bed of leaves, its green jaws lined with teeth, red inside, snapping shut and
 * chewing.
 */
public class PlantSceneRenderer extends EntityRenderer<PlantScene> {
    public static final ResourceLocation LEAF = ArcanesOublies.id("textures/entity/plant/leaf.png");
    public static final ResourceLocation PETAL = ArcanesOublies.id("textures/entity/plant/petal.png");
    public static final ResourceLocation STEM = ArcanesOublies.id("textures/entity/plant/stem.png");
    public static final ResourceLocation FLESH = ArcanesOublies.id("textures/entity/plant/flesh.png");
    private static final int GREEN = 0x5ACF48, PINK = 0xFF8AC8, GOLD = 0xFFD84A, SPORE = 0x9AD860, IVORY = 0xF4ECD8;
    private static final int FULL = LightTexture.FULL_BRIGHT;

    public PlantSceneRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(PlantScene e, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        float age = e.tickCount + partialTicks;
        WaterMesh.View view = WaterViews.of(this.entityRenderDispatcher, e, partialTicks);
        int seed = e.getId() * 31;
        switch (e.getKind()) {
            case BLOOM -> bloom(e, poseStack, buffer, view, age, seed, packedLight);
            case POLLEN -> pollen(e, poseStack, buffer, view, age, seed, packedLight);
            case SEEDS -> seeds(e, poseStack, buffer, view, age, seed, packedLight);
            case SUN -> sun(e, poseStack, buffer, view, age, seed, packedLight);
            case MAW -> maw(e, poseStack, buffer, view, age, seed, packedLight);
        }
        super.render(e, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    /** A petal (or leaf) from {@code base} out along {@code dir} (unit), {@code length} long and {@code width} wide, seen from both sides. */
    private static void petal(VertexConsumer c, PoseStack.Pose p, Vector3f base, Vector3f dir, Vector3f side, float length, float width, int argb, int light) {
        Vector3f s = new Vector3f(side).mul(width * .5f);
        Vector3f tip = new Vector3f(dir).mul(length).add(base);
        BossMesh.quad2(c, p, new Vector3f(base).sub(s), new Vector3f(base).add(s), new Vector3f(tip).add(s), new Vector3f(tip).sub(s), 0, 1, 1, 1, 1, 0, 0, 0,
                argb, argb, light);
    }

    /** A small flower lying open on the ground at {@code at}. */
    private static void flower(VertexConsumer c, PoseStack.Pose p, Vector3f at, float size, float spin, int rgb, float alpha, int light) {
        for (int k = 0; k < 5; k++) {
            float a = spin + k * Mth.TWO_PI / 5;
            Vector3f dir = new Vector3f(Mth.cos(a), .25f, Mth.sin(a)).normalize();
            petal(c, p, at, dir, new Vector3f(-Mth.sin(a), 0, Mth.cos(a)), size, size * .6f, BossMesh.argb(rgb, alpha), light);
        }
    }

    // ---------------------------------------------------------------- Thorn Bloom

    private void bloom(PlantScene e, PoseStack poseStack, MultiBufferSource buffer, WaterMesh.View view, float age, int seed, int light) {
        float grow = BossMesh.ramp(age, 0, PlantScene.BLOOM_OPEN);
        float open = BossMesh.ramp(age, PlantScene.BLOOM_OPEN - 2, PlantScene.BLOOM_OPEN + 5);
        float wither = BossMesh.ramp(age, e.getLife() - 18, e.getLife());
        float height = 1.4f * grow * (1 - wither * .6f);
        float sway = .08f * Mth.sin(age * .1f);
        // The stem and the leaves at its foot
        BossMesh.horn(BossMesh.solid(buffer, STEM), poseStack.last(), new Vector3f(), new Vector3f(0, 1, 0), new Vector3f(sway, 0, 0), height + .05f, .1f, 6, 5,
                BossMesh.argb(0x3A7A30, 1), BossMesh.argb(0x6ACF58, 1), light);
        VertexConsumer leaves = BossMesh.see(buffer, LEAF);
        for (int k = 0; k < 4; k++) {
            float a = k * Mth.HALF_PI + .4f;
            petal(leaves, poseStack.last(), new Vector3f(0, .05f, 0), new Vector3f(Mth.cos(a), .35f, Mth.sin(a)).normalize(), new Vector3f(-Mth.sin(a), 0, Mth.cos(a)),
                    .9f * grow, .5f * grow, BossMesh.argb(0x6ACF58, 1 - wither), light);
        }
        // The head: a bud, opening into a flower
        Vector3f head = new Vector3f(sway * height, height, 0);
        VertexConsumer petals = BossMesh.see(buffer, PETAL);
        float tilt = Mth.lerp(open, .12f, 1.25f) + wither * .5f;
        float length = (.4f + .45f * grow) * (1 + .5f * open);
        for (int ring = 0; ring < 2; ring++) {
            for (int k = 0; k < 7; k++) {
                float a = k * Mth.TWO_PI / 7 + ring * .45f + age * .004f;
                float th = tilt * (ring == 0 ? 1 : .7f);
                Vector3f dir = new Vector3f(Mth.cos(a) * Mth.sin(th), Mth.cos(th), Mth.sin(a) * Mth.sin(th));
                int col = BossMesh.mix(ring == 0 ? PINK : 0xFFC4E4, 0x8A5A60, wither);
                petal(petals, poseStack.last(), head, dir, new Vector3f(-Mth.sin(a), 0, Mth.cos(a)), length * (ring == 0 ? 1 : .75f), .42f * (.5f + .5f * grow),
                        BossMesh.argb(col, 1 - wither * .5f), light);
            }
        }
        BossMesh.sphere(BossMesh.light(buffer, BossMesh.GLOW), poseStack.last(), head, .14f + .08f * open, 5, 8, 0, 0, 1, BossMesh.glow(GOLD, .9f * (1 - wither)), FULL);
        ClockMesh.glow(poseStack, buffer, view, head, 1.4f, PINK, .25f * grow * (1 - wither), FULL);
        // Its thorns flung out all round when it opens
        float since = age - PlantScene.BLOOM_OPEN;
        if (since >= 0 && since < 12) {
            float t = since / 12f;
            VertexConsumer thorns = BossMesh.solid(buffer, STEM);
            for (int k = 0; k < 18; k++) {
                float a = k * Mth.TWO_PI / 18 + BossMesh.hash(seed + k) * .3f;
                Vector3f dir = new Vector3f(Mth.cos(a), -.15f + .3f * BossMesh.hash(seed + k * 3), Mth.sin(a)).normalize();
                Vector3f at = new Vector3f(dir).mul(e.getSize() * t).add(head);
                BossMesh.horn(thorns, poseStack.last(), at, dir, new Vector3f(), .4f * (1 - t * .5f), .05f, 4, 2, BossMesh.argb(0x2E5A24, 1), BossMesh.argb(0xD8F0B0, 1),
                        light);
            }
            ClockMesh.ring(poseStack.last(), buffer, .06f, e.getSize() * t, e.getSize() * t + .4f, PINK, 1 - t, FULL);
        }
    }

    // ---------------------------------------------------------------- Pollen Cloud

    private void pollen(PlantScene e, PoseStack poseStack, MultiBufferSource buffer, WaterMesh.View view, float age, int seed, int light) {
        float r = e.getSize();
        float shown = BossMesh.ramp(age, 0, 20) * (1 - BossMesh.ramp(age, e.getLife() - 20, e.getLife()));
        if (shown <= .01f) {
            return;
        }
        VertexConsumer cloud = BossMesh.see(buffer, BossMesh.CLOUD);
        for (int k = 0; k < 14; k++) {
            float a = k * Mth.TWO_PI / 14 + age * .01f;
            float rr = r * (.4f + .35f * BossMesh.hash(seed + k));
            RitualMesh.billboard(cloud, poseStack, view, new Vector3f(Mth.cos(a) * rr, 1 + .6f * BossMesh.hash(seed + k * 3) + .2f * Mth.sin(age * .04f + k),
                    Mth.sin(a) * rr), 3, a, -1, BossMesh.argb(0xFFF0A0, .22f * shown), light);
        }
        VertexConsumer motes = BossMesh.light(buffer, BossMesh.MOTE);
        for (int k = 0; k < 80; k++) {
            float h1 = BossMesh.hash(seed + k), h2 = BossMesh.hash(seed + k * 3), h3 = BossMesh.hash(seed + k * 7);
            float a = h1 * Mth.TWO_PI + age * .008f * (h2 - .5f) * 4;
            float rr = r * Mth.sqrt(h2);
            float y = .3f + h3 * 2.6f + .3f * Mth.sin(age * .05f + k);
            float tw = .5f + .5f * Mth.sin(age * .3f + k * 1.3f);
            RitualMesh.billboard(motes, poseStack, view, new Vector3f(Mth.cos(a) * rr, y, Mth.sin(a) * rr), .12f + .1f * tw, 0, -1, BossMesh.glow(GOLD, .9f * shown * tw), FULL);
        }
        VertexConsumer petals = BossMesh.see(buffer, PETAL);
        int[] colours = {0xFFF07A, 0xFFFFFF, PINK, 0xC8A0FF};
        for (int k = 0; k < 14; k++) {
            float a = BossMesh.hash(seed + k * 11) * Mth.TWO_PI, rr = r * .9f * Mth.sqrt(BossMesh.hash(seed + k * 13));
            float bloom = BossMesh.ramp(age, k * 2, k * 2 + 15);
            flower(petals, poseStack.last(), new Vector3f(Mth.cos(a) * rr, .03f, Mth.sin(a) * rr), .22f * bloom, a, colours[k % 4], shown, light);
        }
    }

    // ---------------------------------------------------------------- Seed Volley

    private void seeds(PlantScene e, PoseStack poseStack, MultiBufferSource buffer, WaterMesh.View view, float age, int seed, int light) {
        VertexConsumer husk = BossMesh.solid(buffer, STEM);
        VertexConsumer puff = BossMesh.see(buffer, SnowSceneRenderer.SNOW);
        float fade = 1 - BossMesh.ramp(age, e.getLife() - 6, e.getLife());
        for (int i = 0; i < PlantScene.SEEDS; i++) {
            Vec3 spot3 = e.seedSpot(i);
            Vector3f spot = new Vector3f((float) spot3.x, 0, (float) spot3.z);
            if (age < PlantScene.SEED_FLIGHT) {
                float t = age / PlantScene.SEED_FLIGHT;
                Vector3f at = new Vector3f(spot).mul(t).add(0, 1.2f + 2.5f * 4 * t * (1 - t) - 1.2f * t, 0);
                BossMesh.sphere(husk, poseStack.last(), at, .1f, 4, 6, 0, 0, 1, BossMesh.argb(0x8A6A3A, 1), light);
                ClockMesh.glow(poseStack, buffer, view, at, .5f, SPORE, .5f, FULL);
                continue;
            }
            if (age < PlantScene.SEED_BURST) {
                float grow = BossMesh.ramp(age, PlantScene.SEED_FLIGHT, PlantScene.SEED_BURST - 3);
                float swell = 1 + .12f * Mth.sin(age * (1.2f + grow)) * grow;
                BossMesh.horn(husk, poseStack.last(), spot, new Vector3f(0, 1, 0), new Vector3f(), .35f * grow + .02f, .05f, 5, 2, BossMesh.argb(0x3A7A30, 1),
                        BossMesh.argb(0x8ACF68, 1), light);
                BossMesh.sphere(puff, poseStack.last(), new Vector3f(spot).add(0, .3f + .35f * grow, 0), (.08f + .32f * grow) * swell, 6, 10, 0, 0, 1,
                        BossMesh.argb(0xE8DCB0, 1), light);
                continue;
            }
            // Burst: a cloud of spores
            float t = (age - PlantScene.SEED_BURST) / 14f;
            if (t < 1) {
                VertexConsumer cloud = BossMesh.see(buffer, BossMesh.CLOUD);
                for (int k = 0; k < 6; k++) {
                    float a = k * Mth.TWO_PI / 6 + i;
                    Vector3f p = new Vector3f(Mth.cos(a) * 1.2f * t, .4f + .6f * t, Mth.sin(a) * 1.2f * t).add(spot);
                    RitualMesh.billboard(cloud, poseStack, view, p, .8f + 1.4f * t, a + t, -1, BossMesh.argb(SPORE, .6f * (1 - t) * fade), light);
                }
                ClockMesh.glow(poseStack, buffer, view, new Vector3f(spot).add(0, .5f, 0), 2, SPORE, .5f * (1 - t), FULL);
            }
        }
    }

    // ---------------------------------------------------------------- Photosynthesis

    private void sun(PlantScene e, PoseStack poseStack, MultiBufferSource buffer, WaterMesh.View view, float age, int seed, int light) {
        float shown = BossMesh.ramp(age, 0, 14) * (1 - BossMesh.ramp(age, e.getLife() - 20, e.getLife()));
        if (shown <= .01f) {
            return;
        }
        VertexConsumer beam = BossMesh.light(buffer, BossMesh.BEAM);
        float pulse = .85f + .15f * Mth.sin(age * .15f);
        RitualMesh.shaft(beam, poseStack.last(), view.eye(), new Vector3f(0, 16, 0), new Vector3f(0, 0, 0), 2.8f * pulse, BossMesh.glow(0xFFE89A, .35f * shown), FULL);
        RitualMesh.shaft(beam, poseStack.last(), view.eye(), new Vector3f(0, 16, 0), new Vector3f(0, 0, 0), 1.1f, BossMesh.glow(0xFFFBE0, .55f * shown), FULL);
        ClockMesh.ring(poseStack.last(), buffer, .05f, e.getSize() * .7f, e.getSize(), GOLD, .5f * shown, FULL);
        ClockMesh.ring(poseStack.last(), buffer, .06f, .2f, e.getSize() * .5f, GREEN, .25f * shown, FULL);
        // Leaves whirling up in the light
        VertexConsumer leaves = BossMesh.see(buffer, LEAF);
        for (int k = 0; k < 18; k++) {
            float h = BossMesh.hash(seed + k);
            float rise = (h + age * .008f) % 1;
            float a = k * Mth.TWO_PI / 18 + age * .07f;
            float rr = .7f + .4f * Mth.sin(age * .05f + k) + rise * .4f;
            Vector3f at = new Vector3f(Mth.cos(a) * rr, rise * 3.4f, Mth.sin(a) * rr);
            RitualMesh.billboard(leaves, poseStack, view, at, .32f, age * .1f + k, -1, BossMesh.argb(k % 3 == 0 ? 0xB8E070 : 0x6ACF58, shown * Mth.sin(Mth.PI * rise)),
                    light);
        }
        VertexConsumer motes = BossMesh.light(buffer, BossMesh.MOTE);
        for (int k = 0; k < 24; k++) {
            float rise = (BossMesh.hash(seed + k * 5) + age * .012f) % 1;
            float a = BossMesh.hash(seed + k * 7) * Mth.TWO_PI;
            RitualMesh.billboard(motes, poseStack, view, new Vector3f(Mth.cos(a) * .9f, rise * 4, Mth.sin(a) * .9f), .14f, 0, -1,
                    BossMesh.glow(GOLD, shown * Mth.sin(Mth.PI * rise)), FULL);
        }
    }

    // ---------------------------------------------------------------- Verdant Maw

    private void maw(PlantScene e, PoseStack poseStack, MultiBufferSource buffer, WaterMesh.View view, float age, int seed, int light) {
        float r = e.getSize();
        float rise = BossMesh.ramp(age, 0, 10);
        float sink = BossMesh.ramp(age, e.getLife() - 15, e.getLife());
        // The jaws: wide open while it rises, snapping shut, then chewing
        float angle;
        if (age < PlantScene.MAW_SNAP) {
            angle = 1.3f;
        } else if (age < PlantScene.MAW_SNAP + 3) {
            angle = Mth.lerp((age - PlantScene.MAW_SNAP) / 3f, 1.3f, 0);
        } else if (sink > 0) {
            angle = .9f * sink;
        } else {
            angle = .1f + .14f * Math.abs(Mth.sin(age * .45f));
        }
        // The bed of great leaves round it, its vines lashing
        VertexConsumer leaves = BossMesh.see(buffer, LEAF);
        for (int k = 0; k < 7; k++) {
            float a = k * Mth.TWO_PI / 7 + .2f;
            petal(leaves, poseStack.last(), new Vector3f(0, .04f, 0), new Vector3f(Mth.cos(a), .12f, Mth.sin(a)).normalize(), new Vector3f(-Mth.sin(a), 0, Mth.cos(a)),
                    r * 1.6f * rise * (1 - sink * .5f), r * .8f, BossMesh.argb(0x4AA03C, 1), light);
        }
        VertexConsumer stem = BossMesh.solid(buffer, STEM);
        for (int k = 0; k < 4; k++) {
            float a = k * Mth.HALF_PI + .8f;
            Vector3f bend = new Vector3f(Mth.cos(a) * .8f, -.3f, Mth.sin(a) * .8f).mul(1 + .3f * Mth.sin(age * .2f + k));
            BossMesh.horn(stem, poseStack.last(), new Vector3f(Mth.cos(a) * r * .5f, 0, Mth.sin(a) * r * .5f), new Vector3f(Mth.cos(a) * .3f, 1, Mth.sin(a) * .3f).normalize(),
                    bend, 2.2f * rise * (1 - sink), .12f, 6, 6, BossMesh.argb(0x2E6A24, 1), BossMesh.argb(0x7ACF58, 1), light);
        }
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(-e.getDir()));
        poseStack.translate(0, (rise - 1 - sink) * r * 1.3f, 0);
        float h = r * .75f;
        VertexConsumer skin = BossMesh.see(buffer, STEM);
        VertexConsumer flesh = BossMesh.see(buffer, FLESH);
        VertexConsumer teeth = BossMesh.solid(buffer, SnowSceneRenderer.SNOW);
        // The lower jaw: a bowl, green without and red within, teeth along its rim
        Vector3f low = new Vector3f(0, h, 0);
        BossMesh.sphere(skin, poseStack.last(), low, r, 8, 18, 0, .5f, 1, BossMesh.argb(0x5AA848, 1), light);
        BossMesh.sphere(flesh, poseStack.last(), low, r * .94f, 8, 18, 0, .5f, 1, BossMesh.argb(0xFFFFFF, 1), light);
        rimTeeth(teeth, poseStack.last(), low, r, 1, seed, light);
        // The upper jaw, hinged at the back
        poseStack.pushPose();
        poseStack.translate(0, h, -r);
        poseStack.mulPose(Axis.XP.rotation(-angle));
        poseStack.translate(0, 0, r);
        BossMesh.sphere(skin, poseStack.last(), new Vector3f(), r, 8, 18, 0, 0, .5f, BossMesh.argb(0x5AA848, 1), light);
        BossMesh.sphere(flesh, poseStack.last(), new Vector3f(), r * .94f, 8, 18, 0, 0, .5f, BossMesh.argb(0xFFFFFF, 1), light);
        rimTeeth(teeth, poseStack.last(), new Vector3f(), r, -1, seed + 50, light);
        // Spots of venom on its lid
        VertexConsumer glow = BossMesh.light(buffer, BossMesh.GLOW);
        for (int k = 0; k < 6; k++) {
            float a = BossMesh.hash(seed + k) * Mth.TWO_PI, b = .25f + .5f * BossMesh.hash(seed + k * 3);
            Vector3f at = new Vector3f(Mth.sin(b) * Mth.cos(a), Mth.cos(b), Mth.sin(b) * Mth.sin(a)).mul(r * 1.01f);
            BossMesh.sphere(glow, poseStack.last(), at, .1f, 4, 6, 0, 0, 1, BossMesh.glow(0xE0FF60, .6f), FULL);
        }
        poseStack.popPose();
        poseStack.popPose();
        float since = age - PlantScene.MAW_SNAP;
        if (since >= 0 && since < 12) {
            float t = since / 12f;
            ClockMesh.ring(poseStack.last(), buffer, .08f, r * (1 + 2 * t), r * (1 + 2 * t) + .6f, GREEN, 1 - t, FULL);
            ClockMesh.glow(poseStack, buffer, view, new Vector3f(0, r, 0), r * 4, 0xFF4060, .4f * (1 - t), FULL);
        }
    }

    /** Teeth round a jaw's rim, pointing up (the lower jaw: {@code way} 1) or down (the upper: -1), a little inward. */
    private static void rimTeeth(VertexConsumer c, PoseStack.Pose p, Vector3f centre, float r, int way, int seed, int light) {
        int n = 14;
        for (int k = 0; k < n; k++) {
            float a = k * Mth.TWO_PI / n + (way < 0 ? Mth.PI / n : 0);
            Vector3f rim = new Vector3f(Mth.cos(a) * r * .97f, 0, Mth.sin(a) * r * .97f).add(centre);
            Vector3f dir = new Vector3f(-Mth.cos(a) * .35f, way, -Mth.sin(a) * .35f).normalize();
            BossMesh.horn(c, p, rim, dir, new Vector3f(), (.28f + .14f * BossMesh.hash(seed + k)) * r / 2.4f * 1.4f, .06f * r / 2.4f * 1.6f, 5, 2,
                    BossMesh.argb(IVORY, 1), BossMesh.argb(0xFFFFFF, 1), light);
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
