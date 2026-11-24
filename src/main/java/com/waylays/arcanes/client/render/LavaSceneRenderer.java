package com.waylays.arcanes.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.waylays.arcanes.ArcanesOublies;
import com.waylays.arcanes.client.render.boss.BossMesh;
import com.waylays.arcanes.client.render.clockwork.ClockMesh;
import com.waylays.arcanes.client.render.ritual.RitualMesh;
import com.waylays.arcanes.client.render.water.WaterMesh;
import com.waylays.arcanes.entity.lava.LavaScene;
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
 * Lava Magic's spells, drawn: a gob of magma in flight, a tail of fire and smoke behind it, splashing into a pool that
 * bubbles, glows and smokes; the ground cracking and glowing, then a churning jet of lava bursting up, throwing gobs of
 * fire that rain down; shards of obsidian veined with fire turning round the caster, flaring when struck; a river of
 * lava flowing out between banks of crust, its head a rolling tongue of fire, plates of crust drifting on it, cooling
 * dark at its end; the ground cracking round the caster in rings and spokes, then caving into a lake of magma, jets of
 * lava bursting from it.
 */
public class LavaSceneRenderer extends EntityRenderer<LavaScene> {
    public static final ResourceLocation LAVA = ArcanesOublies.id("textures/entity/lava/lava.png");
    public static final ResourceLocation CRUST = ArcanesOublies.id("textures/entity/lava/crust.png");
    public static final ResourceLocation OBSIDIAN = ArcanesOublies.id("textures/entity/lava/obsidian.png");
    private static final int HOT = 0xFFE070, ORANGE = 0xFF7A1E, RED = 0xD8301A, SMOKE = 0xA89488;

    public LavaSceneRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(LavaScene e, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource out, int packedLight) {
        // Recorded per render type, then written out one type after another (see SceneBuffers)
        SceneBuffers buffer = SceneBuffers.begin();
        float age = e.tickCount + partialTicks;
        WaterMesh.View view = WaterViews.of(this.entityRenderDispatcher, e, partialTicks);
        int seed = e.getId() * 31;
        int light = ElementFx.light(e, 12);
        switch (e.getKind()) {
            case SPIT -> spit(e, poseStack, buffer, view, age, partialTicks, seed, light);
            case GEYSER -> geyser(e, poseStack, buffer, view, age, seed, light);
            case CARAPACE -> carapace(e, poseStack, buffer, view, age, partialTicks, seed, light);
            case RIVER -> river(e, poseStack, buffer, view, age, seed, light);
            case CALDERA -> caldera(e, poseStack, buffer, view, age, partialTicks, seed, light);
        }
        buffer.flush(out);
        super.render(e, entityYaw, partialTicks, poseStack, out, packedLight);
    }

    /** A gob of molten rock: its skin churning, glowing from within, a halo of heat round it. */
    private static void glob(PoseStack poseStack, MultiBufferSource buffer, WaterMesh.View view, Vector3f at, float r, float age, float alpha) {
        if (alpha <= .01f || r <= .01f) {
            return;
        }
        int seed = Float.floatToIntBits(at.x * 7 + at.z * 13) & 0xFFFF;
        ElementFx.lumpy(BossMesh.see(buffer, LAVA), poseStack.last(), at, r, 14, 22, .16f, 2.2f, age * .06f, seed, 1, BossMesh.argb(0xFFFFFF, alpha), FULL);
        ElementFx.lumpy(BossMesh.light(buffer, BossMesh.NOISE), poseStack.last(), at, r * 1.05f, 12, 20, .18f, 2.2f, age * .06f, seed, 1,
                BossMesh.glow(ORANGE, .45f * alpha), FULL);
        ElementFx.bloom(poseStack, buffer, view, at, r * 1.6f, ORANGE, .55f * alpha);
    }

    /**
     * A jet of lava from {@code base} up {@code height}, {@code r} thick at its foot: its skin churning and flowing up, a
     * glow inside it, a swelling head, gobs of fire thrown from its top falling all round, smoke rolling off it.
     */
    private static void jet(PoseStack poseStack, MultiBufferSource buffer, WaterMesh.View view, Vector3f base, float height, float r, float age, float alpha,
                            int seed, int light) {
        if (height < .1f || alpha <= .01f) {
            return;
        }
        int n = 12;
        Vector3f[] axis = new Vector3f[n];
        float[] radius = new float[n], glowR = new float[n];
        int[] cols = new int[n], glows = new int[n];
        for (int i = 0; i < n; i++) {
            float t = (float) i / (n - 1);
            axis[i] = new Vector3f(r * .15f * Mth.sin(age * .3f + i * .8f), t * height, r * .15f * Mth.cos(age * .27f + i * .9f)).add(base);
            float swell = 1 + .14f * Mth.sin(age * .55f + i * 1.3f);
            radius[i] = r * (1.1f - .55f * t) * swell * (i == n - 1 ? .55f : 1);
            glowR[i] = radius[i] * 1.08f;
            cols[i] = BossMesh.argb(BossMesh.mix(0xFFFFFF, 0xFFD8A0, t), alpha);
            glows[i] = BossMesh.glow(t > .6f ? HOT : ORANGE, .45f * alpha);
        }
        WaterMesh.tube(BossMesh.see(buffer, LAVA), poseStack.last(), axis, radius, cols, 16, .22f, 2.5f, age * .08f, .35f, .3f, seed, FULL);
        WaterMesh.tube(BossMesh.light(buffer, BossMesh.NOISE), poseStack.last(), axis, glowR, glows, 14, .25f, 2.5f, age * .12f, .5f, -.4f, seed + 3, FULL);
        Vector3f top = new Vector3f(axis[n - 1]);
        glob(poseStack, buffer, view, top, r * .65f, age, alpha);
        // Gobs thrown from the top, falling in arcs, a streak behind each
        VertexConsumer gobs = BossMesh.see(buffer, LAVA);
        VertexConsumer streaks = BossMesh.light(buffer, WaterMesh.STREAK);
        for (int k = 0; k < 16; k++) {
            float h1 = BossMesh.hash(seed + k), h2 = BossMesh.hash(seed + k * 3), h3 = BossMesh.hash(seed + k * 5);
            float t = (age * (.035f + .02f * h2) + h1) % 1;
            float a = h3 * Mth.TWO_PI, reach = r * (1.6f + 2.2f * h2), up = height * (.25f + .3f * h1);
            Vector3f p = new Vector3f(Mth.cos(a) * reach * t, up * (2.4f * t) - (height + up) * t * t, Mth.sin(a) * reach * t).add(top);
            if (p.y < base.y) {
                continue;
            }
            float s = (.1f + .1f * h1) * (1 - .3f * t) * Math.min(1, r);
            BossMesh.sphere(gobs, poseStack.last(), p, s, 5, 8, age * .02f, 0, 1, BossMesh.argb(0xFFE0B0, alpha), FULL);
            Vector3f back = new Vector3f(Mth.cos(a) * reach, up * 2.4f - (height + up) * 2 * t, Mth.sin(a) * reach).mul(-.06f).add(p);
            WaterMesh.streak(streaks, poseStack.last(), view.eye(), p, back, s * 2.2f, BossMesh.glow(ORANGE, .8f * alpha), FULL);
        }
        // Heat shimmering all up the jet
        ElementFx.haze(poseStack, buffer, view, new Vector3f(base).add(0, -.1f, 0), r * 3.4f, height + 1.8f, 3, age, .045f, 0xFFB070, .55f * alpha);
        for (int i = 2; i < n; i += 3) {
            ElementFx.bloom(poseStack, buffer, view, axis[i], r * 1.3f, ORANGE, .35f * alpha);
        }
        ElementFx.motes(poseStack, buffer, view, BossMesh.EMBER, base, r * 2.2f, height * 1.2f, .22f, 18, age * 3, HOT, alpha, seed + 11);
        ElementFx.puffs(poseStack, buffer, view, BossMesh.SMOKE, top, r * 2.5f, height * .6f, r * 1.6f + .6f, 7, age, .025f, SMOKE, .45f * alpha, seed + 13,
                light);
    }

    /** A pool of lava lying flat, {@code r} across: scorched ground round it, its rim of crust; bubbles swell and burst on it. */
    private static void pool(PoseStack poseStack, MultiBufferSource buffer, WaterMesh.View view, float r, float age, float alpha, int seed, int light) {
        if (alpha <= .01f || r <= .02f) {
            return;
        }
        Vector3f u = new Vector3f(1, 0, 0), v = new Vector3f(0, 0, -1);
        BossMesh.annulus(BossMesh.ground(buffer, CRUST), poseStack.last(), new Vector3f(0, .03f, 0), u, v, r * .9f, r * 1.45f, 40, 8, 0, 0,
                BossMesh.argb(0x2A1C18, .85f * alpha), BossMesh.argb(0x2A1C18, 0), light, false);
        BossMesh.annulus(BossMesh.ground(buffer, LAVA), poseStack.last(), new Vector3f(0, .05f, 0), u, v, .01f, r, 40, 2, age * .002f, age * .004f,
                BossMesh.argb(0xFFF4D8, alpha), BossMesh.argb(0xFF9A60, alpha), FULL, false);
        BossMesh.annulus(BossMesh.light(buffer, BossMesh.NOISE), poseStack.last(), new Vector3f(0, .06f, 0), u, v, .01f, r * .95f, 40, 2, -age * .003f, age * .006f,
                BossMesh.glow(HOT, .45f * alpha), BossMesh.glow(ORANGE, .2f * alpha), FULL, false);
        BossMesh.annulus(BossMesh.ground(buffer, CRUST), poseStack.last(), new Vector3f(0, .07f, 0), u, v, r * .8f, r * 1.06f, 40, 6, 0, 0,
                BossMesh.argb(0xFFFFFF, 0), BossMesh.argb(0xFFFFFF, alpha), light, false);
        ElementFx.groundRing(poseStack, buffer, .075f, r * .78f, r * .86f, ORANGE, .8f * alpha);
        VertexConsumer skin = BossMesh.see(buffer, LAVA);
        for (int k = 0; k < 7; k++) {
            float cycle = 22 + 10 * BossMesh.hash(seed + k);
            float t = ((age + BossMesh.hash(seed + k * 3) * cycle) % cycle) / cycle;
            float a = BossMesh.hash(seed + k * 5) * Mth.TWO_PI, rr = r * .65f * Mth.sqrt(BossMesh.hash(seed + k * 7));
            float size = (.1f + .22f * t) * Math.min(1, r);
            Vector3f at = new Vector3f(Mth.cos(a) * rr, .05f, Mth.sin(a) * rr);
            if (t < .88f) {
                BossMesh.sphere(skin, poseStack.last(), at, size, 6, 10, age * .02f, 0, .5f, BossMesh.argb(0xFFE6B8, alpha), FULL);
            } else {
                float b = (t - .88f) / .12f;
                ElementFx.groundRing(poseStack, buffer, .08f, size * (1 + 2 * b), size * (1.3f + 2 * b), HOT, (1 - b) * alpha);
                ElementFx.bloom(poseStack, buffer, view, new Vector3f(at).add(0, .2f, 0), .35f, HOT, (1 - b) * alpha);
            }
        }
        ElementFx.bloom(poseStack, buffer, view, new Vector3f(0, .25f, 0), r * 1.1f, ORANGE, .45f * alpha);
        // Heat rising off it, the air over it wavering
        ElementFx.haze(poseStack, buffer, view, new Vector3f(0, .1f, 0), r * 1.9f, 1.8f + r * .5f, 3, age, .025f, 0xFFB070, .6f * alpha);
        ElementFx.motes(poseStack, buffer, view, BossMesh.EMBER, new Vector3f(0, .1f, 0), r, 2.2f, .18f, 10, age * 2, HOT, alpha, seed + 7);
        ElementFx.puffs(poseStack, buffer, view, BossMesh.SMOKE, new Vector3f(0, .3f, 0), r * 1.2f, 3, r * .8f + .4f, 5, age, .018f, SMOKE, .32f * alpha,
                seed + 9, light);
    }

    /** Cracks of fire running out over the ground from the middle, {@code count} of them, out to {@code reach}. */
    private static void cracks(PoseStack poseStack, MultiBufferSource buffer, int count, float reach, float width, float heat, int seed) {
        if (heat <= .01f) {
            return;
        }
        VertexConsumer glow = BossMesh.light(buffer, BossMesh.GLOW);
        for (int k = 0; k < count; k++) {
            float a = k * Mth.TWO_PI / count + BossMesh.hash(seed + k) * .4f;
            Vector3f[] crack = BossMesh.jagged(new Vector3f(0, .06f, 0), new Vector3f(Mth.cos(a) * reach, .06f, Mth.sin(a) * reach), 7, .22f, seed + k);
            for (int i = 0; i + 1 < crack.length; i++) {
                float w = width * (1 - .5f * i / crack.length);
                RitualMesh.shaft(glow, poseStack.last(), new Vector3f(0, 60, 0), crack[i], crack[i + 1], w * 2.2f, BossMesh.glow(RED, .5f * heat), FULL);
                RitualMesh.shaft(glow, poseStack.last(), new Vector3f(0, 60, 0), crack[i], crack[i + 1], w, BossMesh.glow(HOT, heat), FULL);
            }
        }
    }

    /** Bits of rock jumping on shaking ground, {@code count} of them within {@code reach}. */
    private static void rubble(PoseStack poseStack, MultiBufferSource buffer, float reach, int count, float age, float strength, int seed, int light) {
        if (strength <= .01f) {
            return;
        }
        VertexConsumer rock = BossMesh.solid(buffer, CRUST);
        for (int k = 0; k < count; k++) {
            float a = BossMesh.hash(seed + k * 5) * Mth.TWO_PI, rr = reach * Mth.sqrt(BossMesh.hash(seed + k * 7));
            float cycle = 9 + 5 * BossMesh.hash(seed + k);
            float t = ((age + BossMesh.hash(seed + k * 3) * cycle) % cycle) / cycle;
            float hop = .5f * strength * 4 * t * (1 - t);
            float s = .08f + .08f * BossMesh.hash(seed + k * 11);
            Vector3f at = new Vector3f(Mth.cos(a) * rr, .06f + s * .7f + hop, Mth.sin(a) * rr);
            ElementFx.lumpy(rock, poseStack.last(), at, s * 1.3f, 5, 8, .35f, 1.8f, 0, seed + k * 13, .7f, BossMesh.argb(0xFFFFFF, 1), light);
            ElementFx.lumpy(BossMesh.light(buffer, BossMesh.NOISE), poseStack.last(), at, s * 1.36f, 5, 8, .35f, 1.8f, 0, seed + k * 13, .7f,
                    BossMesh.glow(ORANGE, .35f * strength), FULL);
        }
    }

    // ---------------------------------------------------------------- Magma Spit

    private void spit(LavaScene e, PoseStack poseStack, MultiBufferSource buffer, WaterMesh.View view, float age, float pt, int seed, int light) {
        if (!e.landed()) {
            // Which way it flies, from how far it moved since the last tick (no tail before it has moved)
            Vec3 vel = new Vec3(e.getX() - e.xo, e.getY() - e.yo, e.getZ() - e.zo);
            glob(poseStack, buffer, view, new Vector3f(), .45f, age, 1);
            if (vel.lengthSqr() < 1.0E-4) {
                ElementFx.motes(poseStack, buffer, view, BossMesh.EMBER, new Vector3f(0, -.5f, 0), .6f, 1, .18f, 8, age * 3, HOT, 1, seed);
                return;
            }
            Vector3f back = ElementFx.v(vel).normalize().negate();
            // A tail of fire: gobs shrinking behind it, a streak, smoke rolling off
            for (int i = 1; i <= 5; i++) {
                Vector3f p = new Vector3f(back).mul(i * .32f).add(.05f * Mth.sin(age + i), .05f * Mth.cos(age * 1.3f + i), 0);
                ElementFx.lumpy(BossMesh.see(buffer, LAVA), poseStack.last(), p, .3f * (1 - i / 6f), 7, 12, .25f, 2.5f, age * .1f + i, seed + i, 1,
                        BossMesh.argb(0xFFD8A0, 1 - i / 7f), FULL);
            }
            WaterMesh.streak(BossMesh.light(buffer, WaterMesh.STREAK), poseStack.last(), view.eye(), new Vector3f(), new Vector3f(back).mul(2.4f), .7f,
                    BossMesh.glow(ORANGE, .8f), FULL);
            ClockMesh.sparks(poseStack, buffer, view, new Vector3f(back).mul(.6f), .9f, 14, age, 1, seed, FULL);
            VertexConsumer smoke = BossMesh.see(buffer, BossMesh.SMOKE);
            for (int i = 0; i < 6; i++) {
                float tt = (i + (age * .4f) % 1) / 6f;
                Vector3f p = new Vector3f(back).mul(.6f + tt * 2.6f).add(0, tt * .6f, 0);
                RitualMesh.billboard(smoke, poseStack, view, p, .5f + tt * 1.1f, i + age * .02f, -1, BossMesh.argb(SMOKE, .4f * (1 - tt)), light);
            }
            return;
        }
        float since = e.sinceLanded(pt);
        float spread = BossMesh.ramp(since, 0, 6);
        float fade = 1 - BossMesh.ramp(age, e.getLife() - 15, e.getLife());
        pool(poseStack, buffer, view, e.getSize() * spread, age, fade, seed, light);
        if (since < 14) {
            // The splash: a crown of molten drops thrown out and falling, a ring of heat running out
            float t = since / 14f;
            VertexConsumer gobs = BossMesh.see(buffer, LAVA);
            for (int k = 0; k < 18; k++) {
                float a = k * Mth.TWO_PI / 18 + BossMesh.hash(seed + k);
                float s = .6f + .6f * BossMesh.hash(seed + k * 3);
                Vector3f p = new Vector3f(Mth.cos(a) * 2.4f * s * t, (2.4f * t - 2.6f * t * t) * s * 1.4f, Mth.sin(a) * 2.4f * s * t);
                if (p.y >= 0) {
                    BossMesh.sphere(gobs, poseStack.last(), p, .14f * (1 - t * .5f), 5, 8, k, 0, 1, BossMesh.argb(0xFFE0B0, 1), FULL);
                    ClockMesh.glow(poseStack, buffer, view, p, .5f, HOT, .6f * (1 - t), FULL);
                }
            }
            ElementFx.groundRing(poseStack, buffer, .09f, 3 * t, 3 * t + .5f, HOT, 1 - t);
            ElementFx.bloom(poseStack, buffer, view, new Vector3f(0, .5f, 0), 1.6f, ORANGE, 1 - t);
            ElementFx.burst(poseStack, buffer, view, BossMesh.EMBER, new Vector3f(0, .2f, 0), 3, 2, .3f, 20, t, HOT, 1, seed + 3, true, FULL);
        }
    }

    // ---------------------------------------------------------------- Lava Geyser

    private void geyser(LavaScene e, PoseStack poseStack, MultiBufferSource buffer, WaterMesh.View view, float age, int seed, int light) {
        float r = e.getSize();
        float warn = BossMesh.ramp(age, 0, LavaScene.GEYSER_WARN);
        float erupt = age - LavaScene.GEYSER_WARN;
        float heat = erupt < 0 ? warn : Math.max(0, 1 - erupt / (LavaScene.GEYSER_BURST + 15f));
        // The warning: the ground cracking and glowing, shaking, smoke seeping out
        cracks(poseStack, buffer, 10, r * 1.7f * warn, .12f, heat, seed);
        ElementFx.groundRing(poseStack, buffer, .06f, 0, r * .6f * warn, HOT, .6f * heat);
        ElementFx.bloom(poseStack, buffer, view, new Vector3f(0, .2f, 0), r * (.5f + warn), RED, .6f * heat);
        if (erupt < 0) {
            rubble(poseStack, buffer, r * 1.4f, 10, age, warn, seed, light);
            ElementFx.puffs(poseStack, buffer, view, BossMesh.SMOKE, new Vector3f(0, .1f, 0), r * 1.2f, 2, .8f, 6, age, .04f, SMOKE, .4f * warn, seed, light);
            return;
        }
        // The vent: a cone of crust heaved up round the hole, its lip glowing
        VertexConsumer crust = BossMesh.solid(buffer, CRUST);
        ElementFx.lumpy(crust, poseStack.last(), new Vector3f(), r * 1.05f, 8, 18, .25f, 1.8f, 0, seed + 99, .38f, BossMesh.argb(0xFFFFFF, 1), light);
        BossMesh.annulus(BossMesh.ground(buffer, LAVA), poseStack.last(), new Vector3f(0, r * .41f, 0), new Vector3f(1, 0, 0), new Vector3f(0, 0, -1), .01f,
                r * .45f, 24, 1, age * .003f, age * .006f, BossMesh.argb(0xFFF0D0, 1), BossMesh.argb(0xFF9A60, 1), FULL, false);
        ElementFx.groundRing(poseStack, buffer, r * .42f, r * .4f, r * .5f, HOT, .9f);
        // The jet: bursting up fast, churning, then sinking back into its pool
        float up = BossMesh.ramp(erupt, 0, 4) * (1 - BossMesh.ramp(erupt, LavaScene.GEYSER_BURST - 2, LavaScene.GEYSER_BURST + 8));
        jet(poseStack, buffer, view, new Vector3f(), LavaScene.GEYSER_HEIGHT * up, r * .55f, age, 1, seed, light);
        if (erupt < 10) {
            float t = erupt / 10f;
            ElementFx.groundRing(poseStack, buffer, .1f, r * 2.5f * t, r * 2.5f * t + .6f, HOT, 1 - t);
            ElementFx.burst(poseStack, buffer, view, BossMesh.EMBER, new Vector3f(0, .5f, 0), r * 2.5f, 3, .35f, 24, t, HOT, 1, seed + 5, true, FULL);
        }
        pool(poseStack, buffer, view, r * Math.max(.4f, up), age, Math.max(0, 1 - BossMesh.ramp(erupt, LavaScene.GEYSER_BURST, LavaScene.GEYSER_BURST + 18)),
                seed, light);
    }

    // ---------------------------------------------------------------- Obsidian Carapace

    private void carapace(LavaScene e, PoseStack poseStack, MultiBufferSource buffer, WaterMesh.View view, float age, float pt, int seed, int light) {
        float appear = BossMesh.ramp(age, 0, 12);
        float fade = 1 - BossMesh.ramp(age, e.getLife() - 15, e.getLife());
        float shown = appear * fade;
        if (shown <= .01f) {
            return;
        }
        float flare = Math.max(0, 1 - e.sinceStruck(pt) / 10f);
        VertexConsumer stone = BossMesh.solid(buffer, OBSIDIAN);
        VertexConsumer veins = BossMesh.light(buffer, BossMesh.NOISE);
        // Great shards turning round, small ones turning the other way between them
        int shards = 8;
        for (int k = 0; k < shards; k++) {
            float a = age * .035f + k * Mth.TWO_PI / shards;
            float y = .2f + .9f * ((k * .37f) % 1) + .08f * Mth.sin(age * .1f + k);
            float rr = (.85f + .08f * Mth.sin(age * .05f + k)) * (2.2f - 1.2f * appear);
            Vector3f out = new Vector3f(Mth.cos(a), 0, Mth.sin(a));
            Vector3f dir = new Vector3f(out).mul(-.18f).add(new Vector3f(-Mth.sin(a), 0, Mth.cos(a)).mul(.2f * Mth.sin(k))).add(0, 1, 0).normalize();
            Vector3f base = new Vector3f(out).mul(rr).add(0, y, 0);
            float len = (.75f + .35f * BossMesh.hash(seed + k)) * shown, w = (.17f + .06f * BossMesh.hash(seed + k * 3)) * shown;
            RitualMesh.crystal(stone, poseStack.last(), base, dir, len, w, a * 2, BossMesh.argb(0xFFFFFF, 1), light);
            float pulse = .35f + .2f * Mth.sin(age * .2f + k) + .7f * flare;
            RitualMesh.crystal(veins, poseStack.last(), base, dir, len * 1.02f, w * 1.06f, a * 2, BossMesh.glow(ORANGE, pulse * shown), FULL);
            RitualMesh.crystal(BossMesh.light(buffer, BossMesh.SHEEN), poseStack.last(), base, dir, len * 1.04f, w * 1.1f, a * 2 + age * .02f,
                    BossMesh.glow(0xB890FF, .22f * shown), FULL);
            float b = -age * .05f + (k + .5f) * Mth.TWO_PI / shards;
            Vector3f small = new Vector3f(Mth.cos(b) * rr * 1.15f, .5f + .7f * ((k * .61f) % 1), Mth.sin(b) * rr * 1.15f);
            Vector3f sd = new Vector3f(Mth.cos(age * .07f + k), 1, Mth.sin(age * .09f + k)).normalize();
            RitualMesh.crystal(stone, poseStack.last(), small, sd, .32f * shown, .08f * shown, b, BossMesh.argb(0xFFFFFF, 1), light);
            RitualMesh.crystal(veins, poseStack.last(), small, sd, .33f * shown, .085f * shown, b, BossMesh.glow(HOT, pulse * .8f * shown), FULL);
        }
        // Molten rock dripping from the shards, glowing as it falls
        VertexConsumer drips = BossMesh.light(buffer, BossMesh.GLOW);
        for (int k = 0; k < 8; k++) {
            float cycle = 18 + 8 * BossMesh.hash(seed + k);
            float t = ((age + BossMesh.hash(seed + k * 5) * cycle) % cycle) / cycle;
            float a = age * .035f + k * Mth.TWO_PI / 8;
            float rr = (.85f + .08f * Mth.sin(age * .05f + k)) * (2.2f - 1.2f * appear);
            float y0 = .2f + .9f * ((k * .37f) % 1);
            Vector3f p = new Vector3f(Mth.cos(a) * rr, y0 - t * t * (y0 + .1f), Mth.sin(a) * rr);
            RitualMesh.billboard(drips, poseStack, view, p, .12f, 0, -1, BossMesh.glow(HOT, (1 - t) * shown), FULL);
            WaterMesh.streak(BossMesh.light(buffer, WaterMesh.STREAK), poseStack.last(), view.eye(), p, new Vector3f(p).add(0, .25f + t * .3f, 0), .05f,
                    BossMesh.glow(ORANGE, .6f * (1 - t) * shown), FULL);
        }
        // Heat round the caster's feet, embers rising, a glow that flares when the carapace is struck
        ElementFx.groundRing(poseStack, buffer, .05f, .7f, 1.25f, ORANGE, (.25f + .5f * flare) * shown);
        ElementFx.motes(poseStack, buffer, view, BossMesh.EMBER, new Vector3f(), 1.3f, 2.4f, .16f, 14, age * 2, HOT, .8f * shown, seed + 3);
        ElementFx.bloom(poseStack, buffer, view, new Vector3f(0, 1, 0), 1.4f, RED, (.2f + .8f * flare) * shown);
        ElementFx.haze(poseStack, buffer, view, new Vector3f(0, .2f, 0), 2.6f, 2.6f, 2, age, .03f, 0xFFB070, (.3f + .4f * flare) * shown);
        if (flare > 0) {
            ClockMesh.sparks(poseStack, buffer, view, new Vector3f(0, 1, 0), 1.8f, 20, age, flare, seed, FULL);
        }
    }

    // ---------------------------------------------------------------- Lava River

    private void river(LavaScene e, PoseStack poseStack, MultiBufferSource buffer, WaterMesh.View view, float age, int seed, int light) {
        float flow = BossMesh.ramp(age, 0, LavaScene.RIVER_FLOW);
        float length = e.getSize() * flow;
        if (length < .05f) {
            return;
        }
        float cool = BossMesh.ramp(age, e.getLife() - 30, e.getLife() - 5);
        float gone = 1 - BossMesh.ramp(age, e.getLife() - 8, e.getLife());
        Vector3f f = ElementFx.v(e.forward()), side = new Vector3f(f.z, 0, -f.x);
        float half = LavaScene.RIVER_HALF * 1.1f;
        int segs = 32;
        VertexConsumer lava = BossMesh.ground(buffer, LAVA);
        VertexConsumer glowing = BossMesh.light(buffer, BossMesh.NOISE);
        VertexConsumer crust = BossMesh.ground(buffer, CRUST);
        VertexConsumer edge = BossMesh.light(buffer, BossMesh.GLOW);
        Vector3f[] inL = new Vector3f[segs + 1], inR = new Vector3f[segs + 1];
        for (int i = 0; i < segs; i++) {
            float t0 = (float) i / segs, t1 = (float) (i + 1) / segs;
            float w0 = half * (.85f + .22f * Mth.sin(t0 * 9 + seed)), w1 = half * (.85f + .22f * Mth.sin(t1 * 9 + seed));
            Vector3f c0 = new Vector3f(f).mul(t0 * length).add(0, .08f, 0), c1 = new Vector3f(f).mul(t1 * length).add(0, .08f, 0);
            // The front of the flow is already cooling as it creeps
            float front = BossMesh.ramp(t1, .85f, 1) * (1 - flow) * .5f;
            float dark = Math.min(1, cool + front);
            int col = BossMesh.mix(0xFFFFFF, 0x4A2A26, dark);
            float v0 = t0 * length * .35f - age * .03f, v1 = t1 * length * .35f - age * .03f;
            Vector3f l0 = new Vector3f(c0).sub(new Vector3f(side).mul(w0)), r0 = new Vector3f(c0).add(new Vector3f(side).mul(w0));
            Vector3f l1 = new Vector3f(c1).sub(new Vector3f(side).mul(w1)), r1 = new Vector3f(c1).add(new Vector3f(side).mul(w1));
            ElementFx.flat(lava, poseStack.last(), l0, r0, r1, l1, 0, v0, 1, v0, 1, v1, 0, v1, BossMesh.argb(col, gone), BossMesh.argb(col, gone), FULL);
            Vector3f[] lift = {new Vector3f(l0).add(0, .01f, 0), new Vector3f(r0).add(0, .01f, 0), new Vector3f(r1).add(0, .01f, 0), new Vector3f(l1).add(0, .01f, 0)};
            ElementFx.flat(glowing, poseStack.last(), lift[0], lift[1], lift[2], lift[3], 0, v0 * 1.4f, 1, v0 * 1.4f, 1, v1 * 1.4f, 0, v1 * 1.4f,
                    BossMesh.glow(HOT, .4f * (1 - dark) * gone), BossMesh.glow(HOT, .4f * (1 - dark) * gone), FULL);
            // Its banks of crust, raised
            for (int s = -1; s <= 1; s += 2) {
                Vector3f in0 = new Vector3f(c0).add(new Vector3f(side).mul(s * w0 * .9f)), out0 = new Vector3f(c0).add(new Vector3f(side).mul(s * (w0 + .45f)));
                Vector3f in1 = new Vector3f(c1).add(new Vector3f(side).mul(s * w1 * .9f)), out1 = new Vector3f(c1).add(new Vector3f(side).mul(s * (w1 + .45f)));
                in0.y = in1.y = .12f;
                out0.y = out1.y = .07f;
                ElementFx.flat(crust, poseStack.last(), in0, out0, out1, in1, 0, t0 * 4, 1, t0 * 4, 1, t1 * 4, 0, t1 * 4, BossMesh.argb(0xFFFFFF, gone),
                        BossMesh.argb(0xFFFFFF, gone), light);
            }
            inL[i] = new Vector3f(c0).sub(new Vector3f(side).mul(w0 * .9f)).add(0, .06f, 0);
            inR[i] = new Vector3f(c0).add(new Vector3f(side).mul(w0 * .9f)).add(0, .06f, 0);
            if (i == segs - 1) {
                inL[segs] = new Vector3f(c1).sub(new Vector3f(side).mul(w1 * .9f)).add(0, .06f, 0);
                inR[segs] = new Vector3f(c1).add(new Vector3f(side).mul(w1 * .9f)).add(0, .06f, 0);
            }
        }
        // The banks' inner edges glowing where the lava licks them
        for (Vector3f[] line : new Vector3f[][]{inL, inR}) {
            int[] cols = new int[line.length];
            for (int i = 0; i < line.length; i++) {
                cols[i] = BossMesh.glow(ORANGE, .7f * (1 - cool) * gone);
            }
            RitualMesh.thread(edge, poseStack.last(), view.eye(), line, .22f, cols, FULL);
        }
        // Bright threads of molten rock running down its surface
        VertexConsumer runs = BossMesh.light(buffer, BossMesh.GLOW);
        for (int k = 0; k < 16; k++) {
            float t = (BossMesh.hash(seed + k * 23) + age * (.012f + .008f * BossMesh.hash(seed + k))) % 1;
            if (t * e.getSize() > length) {
                continue;
            }
            float across = (BossMesh.hash(seed + k * 29) * 2 - 1) * half * .7f;
            Vector3f a0 = new Vector3f(f).mul(t * e.getSize()).add(new Vector3f(side).mul(across)).add(0, .1f, 0);
            Vector3f a1 = new Vector3f(a0).add(new Vector3f(f).mul(-.9f));
            RitualMesh.shaft(runs, poseStack.last(), new Vector3f(0, 60, 0), a0, a1, .1f, BossMesh.glow(HOT, .8f * (1 - cool) * gone * Mth.sin(Mth.PI * t)), FULL);
        }
        // Its head, a rolling tongue of fire while it still flows
        if (flow < 1) {
            Vector3f head = new Vector3f(f).mul(length).add(0, .05f, 0);
            ElementFx.lumpy(BossMesh.see(buffer, LAVA), poseStack.last(), head, half * .95f, 10, 18, .2f, 2, age * .08f, seed, .45f, BossMesh.argb(0xFFE6C0, 1),
                    FULL);
            ElementFx.bloom(poseStack, buffer, view, new Vector3f(head).add(0, .4f, 0), 1.2f, ORANGE, .8f);
        }
        // Plates of crust drifting on it
        VertexConsumer stone = BossMesh.solid(buffer, CRUST);
        for (int k = 0; k < 12; k++) {
            float t = (BossMesh.hash(seed + k) + age * .006f) % 1;
            if (t * e.getSize() > length - .3f) {
                continue;
            }
            float across = (BossMesh.hash(seed + k * 3) * 2 - 1) * half * .5f;
            Vector3f at = new Vector3f(f).mul(t * e.getSize()).add(new Vector3f(side).mul(across)).add(0, .11f, 0);
            float s = .15f + .18f * BossMesh.hash(seed + k * 5);
            float turn = age * .01f + k;
            ElementFx.lumpy(stone, poseStack.last(), at, s * 1.2f, 4, 9, .3f, 1.6f, turn * .1f, seed + k * 17, .22f, BossMesh.argb(0xFFFFFF, 1), light);
        }
        for (int k = 0; k < 4; k++) {
            ElementFx.haze(poseStack, buffer, view, new Vector3f(f).mul(length * (k + .5f) / 4).add(0, .1f, 0), half * 2.4f, 2f, 2, age + k * 9, .03f,
                    0xFFB070, .5f * (1 - cool) * gone);
        }
        for (int k = 0; k <= 5; k++) {
            Vector3f p = new Vector3f(f).mul(length * k / 5f).add(0, .4f, 0);
            ElementFx.bloom(poseStack, buffer, view, p, 1.3f, ORANGE, .35f * (1 - cool) * gone);
        }
        ElementFx.motes(poseStack, buffer, view, BossMesh.EMBER, new Vector3f(f).mul(length * .5f), Math.max(half, length * .45f), 2, .18f, 20, age * 2, HOT,
                (1 - cool) * gone, seed + 5);
        for (int k = 0; k < 3; k++) {
            ElementFx.puffs(poseStack, buffer, view, BossMesh.SMOKE, new Vector3f(f).mul(length * (k + .5f) / 3).add(0, .2f, 0), half, 2.5f, .9f, 3, age, .02f,
                    SMOKE, (.25f + .3f * cool) * gone, seed + k * 7, light);
        }
    }

    // ---------------------------------------------------------------- Caldera

    private void caldera(LavaScene e, PoseStack poseStack, MultiBufferSource buffer, WaterMesh.View view, float age, float pt, int seed, int light) {
        float r = e.getSize();
        if (!e.collapsed()) {
            // The ground cracking: spokes out from the caster, rings joining them, shaking, brighter as it nears the end
            float t = BossMesh.ramp(age, 0, LavaScene.CALDERA_COLLAPSE);
            cracks(poseStack, buffer, 16, r * t, .16f, .4f + .6f * t, seed);
            for (int ring = 1; ring <= 3; ring++) {
                float rr = r * ring / 3.4f;
                if (t * r > rr) {
                    ElementFx.groundRing(poseStack, buffer, .06f, rr - .12f, rr + .12f, ORANGE, .8f * t);
                }
            }
            rubble(poseStack, buffer, r * t, 22, age, t, seed, light);
            ElementFx.bloom(poseStack, buffer, view, new Vector3f(0, .2f, 0), r * .8f * t, RED, .5f * t);
            ElementFx.puffs(poseStack, buffer, view, BossMesh.SMOKE, new Vector3f(0, .1f, 0), r * t, 2.5f, 1.2f, 10, age, .025f, SMOKE, .35f * t, seed, light);
            return;
        }
        float since = e.sinceLanded(pt);
        float spread = BossMesh.ramp(since, 0, 8);
        float fade = 1 - BossMesh.ramp(age, e.getLife() - 20, e.getLife());
        Vector3f u = new Vector3f(1, 0, 0), v = new Vector3f(0, 0, -1);
        float R = r * spread;
        // The lake, slowly turning, glowing veins running over it, its rim of crust, scorched ground round it
        BossMesh.annulus(BossMesh.ground(buffer, CRUST), poseStack.last(), new Vector3f(0, .03f, 0), u, v, R * .95f, R * 1.3f, 64, 20, 0, 0,
                BossMesh.argb(0x2A1C18, .85f * fade), BossMesh.argb(0x2A1C18, 0), light, false);
        BossMesh.annulus(BossMesh.ground(buffer, LAVA), poseStack.last(), new Vector3f(0, .05f, 0), u, v, .01f, R, 64, 6, age * .0015f, age * .003f,
                BossMesh.argb(0xFFF0D0, fade), BossMesh.argb(0xFFA070, fade), FULL, false);
        BossMesh.annulus(BossMesh.light(buffer, BossMesh.NOISE), poseStack.last(), new Vector3f(0, .06f, 0), u, v, .01f, R * .97f, 64, 5, -age * .002f, age * .005f,
                BossMesh.glow(HOT, .4f * fade), BossMesh.glow(ORANGE, .25f * fade), FULL, false);
        BossMesh.annulus(BossMesh.ground(buffer, CRUST), poseStack.last(), new Vector3f(0, .07f, 0), u, v, R * .88f, R * 1.06f, 64, 16, 0, 0,
                BossMesh.argb(0xFFFFFF, 0), BossMesh.argb(0xFFFFFF, fade), light, false);
        ElementFx.groundRing(poseStack, buffer, .075f, R * .86f, R * .92f, ORANGE, .8f * fade);
        // Islands of crust drifting round on it
        VertexConsumer stone = BossMesh.solid(buffer, CRUST);
        for (int k = 0; k < 10; k++) {
            float a = BossMesh.hash(seed + k * 3) * Mth.TWO_PI + age * .004f, rr = R * (.25f + .55f * BossMesh.hash(seed + k * 5));
            float s = .25f + .35f * BossMesh.hash(seed + k * 7), turn = age * .006f + k;
            ElementFx.lumpy(stone, poseStack.last(), new Vector3f(Mth.cos(a) * rr, .1f, Mth.sin(a) * rr), s * 1.3f, 5, 10, .3f, 1.6f, turn * .1f, seed + k * 19,
                    .25f, BossMesh.argb(0xFFFFFF, 1), light);
        }
        // Jets bursting from it, each in its own time
        for (int k = 0; k < 5; k++) {
            float cycle = 34 + 14 * BossMesh.hash(seed + k);
            float t = ((age + BossMesh.hash(seed + k * 3) * cycle) % cycle) / cycle;
            float a = BossMesh.hash(seed + k * 5) * Mth.TWO_PI + k * 1.3f, rr = R * (.3f + .45f * BossMesh.hash(seed + k * 7));
            float hgt = (2.5f + 2.5f * BossMesh.hash(seed + k * 11)) * Mth.sin(Mth.PI * Math.min(1, t * 1.6f));
            Vector3f base = new Vector3f(Mth.cos(a) * rr, 0, Mth.sin(a) * rr);
            jet(poseStack, buffer, view, base, hgt * fade, .5f, age + k * 13, fade, seed + k * 41, light);
        }
        // The collapse: a ring of fire running out, embers flung, the ground shaking
        if (since < 20) {
            float t = since / 20f;
            ElementFx.groundRing(poseStack, buffer, .1f, r * t, r * t + 1.2f, HOT, 1 - t);
            ElementFx.burst(poseStack, buffer, view, BossMesh.EMBER, new Vector3f(0, .5f, 0), r, 4, .4f, 36, t, HOT, 1, seed + 3, true, FULL);
            ElementFx.bloom(poseStack, buffer, view, new Vector3f(0, 1, 0), r * (1 - t * .5f), ORANGE, 1 - t);
        }
        ElementFx.bloom(poseStack, buffer, view, new Vector3f(0, .6f, 0), R * .9f, ORANGE, .35f * fade);
        for (int k = 0; k < 5; k++) {
            float a = k * Mth.TWO_PI / 5 + .4f, rr = k == 0 ? 0 : R * .55f;
            ElementFx.haze(poseStack, buffer, view, new Vector3f(Mth.cos(a) * rr, .1f, Mth.sin(a) * rr), R * .7f, 3.2f, 2, age + k * 13, .03f, 0xFFB070,
                    .5f * fade);
        }
        ElementFx.motes(poseStack, buffer, view, BossMesh.EMBER, new Vector3f(), R, 4, .22f, 30, age * 2, HOT, fade, seed + 9);
        ElementFx.puffs(poseStack, buffer, view, BossMesh.SMOKE, new Vector3f(0, .3f, 0), R, 5, 1.8f, 10, age, .012f, SMOKE, .35f * fade, seed + 7, light);
    }

    @Override
    public boolean shouldRender(LavaScene e, Frustum frustum, double x, double y, double z) {
        return true;
    }

    @Override
    public ResourceLocation getTextureLocation(LavaScene e) {
        return LAVA;
    }
}
