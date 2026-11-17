package com.waylays.arcanes.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.waylays.arcanes.ArcanesOublies;
import com.waylays.arcanes.client.render.boss.BossMesh;
import com.waylays.arcanes.client.render.clockwork.ClockMesh;
import com.waylays.arcanes.client.render.ritual.RitualMesh;
import com.waylays.arcanes.client.render.water.WaterMesh;
import com.waylays.arcanes.entity.lava.LavaScene;
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
 * Lava Magic's spells, drawn: a gob of glowing magma in flight, trailing embers, splashing into a pool that bubbles
 * and smokes; cracks glowing on the ground, then a churning column of lava raining drops of fire; plates of obsidian
 * veined with lava floating round the caster, flaring when struck; a river of lava flowing out between banks of crust,
 * plates of crust drifting on it, cooling dark at its end; the ground cracking round the caster in rings and spokes,
 * then a lake of magma, its rim of crust, fountains of lava bursting from it.
 */
public class LavaSceneRenderer extends EntityRenderer<LavaScene> {
    public static final ResourceLocation LAVA = ArcanesOublies.id("textures/entity/lava/lava.png");
    public static final ResourceLocation CRUST = ArcanesOublies.id("textures/entity/lava/crust.png");
    public static final ResourceLocation OBSIDIAN = ArcanesOublies.id("textures/entity/lava/obsidian.png");
    private static final int HOT = 0xFFE070, ORANGE = 0xFF7A1E, RED = 0xD8301A, SMOKE = 0x3A3030;
    private static final int FULL = LightTexture.FULL_BRIGHT;

    public LavaSceneRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(LavaScene e, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        float age = e.tickCount + partialTicks;
        WaterMesh.View view = WaterViews.of(this.entityRenderDispatcher, e, partialTicks);
        int seed = e.getId() * 31;
        switch (e.getKind()) {
            case SPIT -> spit(e, poseStack, buffer, view, age, partialTicks, seed);
            case GEYSER -> geyser(e, poseStack, buffer, view, age, seed);
            case CARAPACE -> carapace(e, poseStack, buffer, view, age, partialTicks, seed, packedLight);
            case RIVER -> river(e, poseStack, buffer, view, age, seed, packedLight);
            case CALDERA -> caldera(e, poseStack, buffer, view, age, partialTicks, seed, packedLight);
        }
        super.render(e, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    private static Vector3f v(Vec3 d) {
        return new Vector3f((float) d.x, (float) d.y, (float) d.z);
    }

    /** Molten rock: its surface churning, glowing from within. */
    private static void magma(PoseStack poseStack, MultiBufferSource buffer, WaterMesh.View view, Vector3f at, float r, float age, float alpha) {
        BossMesh.sphere(BossMesh.see(buffer, LAVA), poseStack.last(), at, r, 8, 14, age * .006f, 0, 1, BossMesh.argb(0xFFFFFF, alpha), FULL);
        BossMesh.sphere(BossMesh.light(buffer, BossMesh.NOISE), poseStack.last(), at, r * 1.08f, 6, 12, -age * .01f, 0, 1, BossMesh.glow(ORANGE, .35f * alpha), FULL);
        ClockMesh.glow(poseStack, buffer, view, at, r * 5, ORANGE, .45f * alpha, FULL);
    }

    /** A pool of lava lying flat, {@code r} across, its rim of crust; bubbles swell and burst on it. */
    private static void pool(PoseStack poseStack, MultiBufferSource buffer, WaterMesh.View view, float r, float age, float alpha, int seed, int light) {
        BossMesh.annulus(BossMesh.ground(buffer, LAVA), poseStack.last(), new Vector3f(0, .04f, 0), new Vector3f(1, 0, 0), new Vector3f(0, 0, -1), .01f, r, 40, 2,
                age * .002f, age * .004f, BossMesh.argb(0xFFFFFF, alpha), BossMesh.argb(0xFFD0A0, alpha), FULL, false);
        BossMesh.annulus(BossMesh.ground(buffer, CRUST), poseStack.last(), new Vector3f(0, .05f, 0), new Vector3f(1, 0, 0), new Vector3f(0, 0, -1), r * .82f,
                r * 1.08f, 40, 6, 0, 0, BossMesh.argb(0xFFFFFF, 0), BossMesh.argb(0xFFFFFF, alpha), light, false);
        for (int k = 0; k < 6; k++) {
            float cycle = 22 + 10 * BossMesh.hash(seed + k);
            float t = ((age + BossMesh.hash(seed + k * 3) * cycle) % cycle) / cycle;
            float a = BossMesh.hash(seed + k * 5) * Mth.TWO_PI, rr = r * .7f * Mth.sqrt(BossMesh.hash(seed + k * 7));
            float size = .08f + .18f * t;
            Vector3f at = new Vector3f(Mth.cos(a) * rr, .04f + size * .5f, Mth.sin(a) * rr);
            if (t < .9f) {
                BossMesh.sphere(BossMesh.see(buffer, LAVA), poseStack.last(), at, size, 5, 8, age * .02f, 0, .5f, BossMesh.argb(0xFFE0B0, alpha), FULL);
            } else {
                ClockMesh.glow(poseStack, buffer, view, at, .8f, HOT, alpha * (1 - t) * 8, FULL);
            }
        }
        ClockMesh.glow(poseStack, buffer, view, new Vector3f(0, .3f, 0), r * 3, ORANGE, .35f * alpha, FULL);
        ClockMesh.smoke(poseStack, buffer, view, new Vector3f(0, .2f, 0), new Vector3f(0, 1, 0), 2.5f, .8f, 5, age, .015f, .3f * alpha, SMOKE, seed, light);
    }

    // ---------------------------------------------------------------- Magma Spit

    private void spit(LavaScene e, PoseStack poseStack, MultiBufferSource buffer, WaterMesh.View view, float age, float pt, int seed) {
        if (!e.landed()) {
            magma(poseStack, buffer, view, new Vector3f(), .32f, age, 1);
            ClockMesh.sparks(poseStack, buffer, view, new Vector3f(), .8f, 10, age, 1, seed, FULL);
            return;
        }
        float since = e.sinceLanded(pt);
        float spread = BossMesh.ramp(since, 0, 6);
        float fade = 1 - BossMesh.ramp(age, e.getLife() - 15, e.getLife());
        pool(poseStack, buffer, view, e.getSize() * spread, age, fade, seed, 0xF000F0);
        if (since < 12) {
            float t = since / 12f;
            VertexConsumer embers = BossMesh.light(buffer, BossMesh.EMBER);
            for (int k = 0; k < 16; k++) {
                float a = k * Mth.TWO_PI / 16 + BossMesh.hash(seed + k);
                Vector3f p = new Vector3f(Mth.cos(a) * 2 * t, (1.4f * t - 1.8f * t * t) * (1 + BossMesh.hash(seed + k * 3)), Mth.sin(a) * 2 * t);
                RitualMesh.billboard(embers, poseStack, view, p, .3f, k, -1, BossMesh.glow(HOT, 1 - t), FULL);
            }
        }
    }

    // ---------------------------------------------------------------- Lava Geyser

    private void geyser(LavaScene e, PoseStack poseStack, MultiBufferSource buffer, WaterMesh.View view, float age, int seed) {
        float r = e.getSize();
        float warn = BossMesh.ramp(age, 0, LavaScene.GEYSER_WARN);
        float erupt = age - LavaScene.GEYSER_WARN;
        // The cracks: spokes of fire spreading over the ground, brighter and brighter
        VertexConsumer glow = BossMesh.light(buffer, BossMesh.GLOW);
        float heat = erupt < 0 ? warn : Math.max(0, 1 - erupt / (LavaScene.GEYSER_BURST + 15f));
        for (int k = 0; k < 9; k++) {
            float a = k * Mth.TWO_PI / 9 + BossMesh.hash(seed + k) * .4f;
            Vector3f[] crack = BossMesh.jagged(new Vector3f(0, .05f, 0), new Vector3f(Mth.cos(a) * r * 1.6f * warn, .05f, Mth.sin(a) * r * 1.6f * warn), 5, .2f,
                    seed + k);
            for (int i = 0; i + 1 < crack.length; i++) {
                RitualMesh.shaft(glow, poseStack.last(), new Vector3f(0, 60, 0), crack[i], crack[i + 1], .16f, BossMesh.glow(ORANGE, heat), FULL);
            }
        }
        ClockMesh.glow(poseStack, buffer, view, new Vector3f(0, .1f, 0), r * 3 * (.5f + warn), RED, .5f * heat, FULL);
        if (erupt < 0) {
            ClockMesh.smoke(poseStack, buffer, view, new Vector3f(0, .1f, 0), new Vector3f(0, 1, 0), 1.5f, .6f, 4, age, .03f, .4f * warn, SMOKE, seed, 0xF000F0);
            return;
        }
        // The column: rising fast, churning, then sinking back
        float up = BossMesh.ramp(erupt, 0, 4) * (1 - BossMesh.ramp(erupt, LavaScene.GEYSER_BURST - 2, LavaScene.GEYSER_BURST + 8));
        float h = LavaScene.GEYSER_HEIGHT * up;
        if (h > .1f) {
            int n = 10;
            for (int i = 0; i < n; i++) {
                float t = (float) i / n;
                float y = t * h;
                float rr = r * (.75f - .35f * t) * (1 + .12f * Mth.sin(age * .8f + i * 1.7f));
                magma(poseStack, buffer, view, new Vector3f(.08f * Mth.sin(age * .3f + i), y, .08f * Mth.cos(age * .37f + i)), rr, age + i * 7, 1);
            }
            // The crown of spray at its top
            VertexConsumer embers = BossMesh.light(buffer, BossMesh.EMBER);
            for (int k = 0; k < 20; k++) {
                float t = ((age * .06f + BossMesh.hash(seed + k)) % 1);
                float a = BossMesh.hash(seed + k * 3) * Mth.TWO_PI;
                Vector3f p = new Vector3f(Mth.cos(a) * 2.2f * t, h + (1.6f * t - 3 * t * t) * 2, Mth.sin(a) * 2.2f * t);
                RitualMesh.billboard(embers, poseStack, view, p, .35f * (1 - t * .5f), k, -1, BossMesh.glow(k % 3 == 0 ? HOT : ORANGE, 1 - t), FULL);
            }
            ClockMesh.smoke(poseStack, buffer, view, new Vector3f(0, h, 0), new Vector3f(0, 1, 0), 3, 1.4f, 6, age, .02f, .5f * up, SMOKE, seed, 0xF000F0);
        }
        pool(poseStack, buffer, view, r * Math.max(.3f, up), age, Math.max(0, 1 - BossMesh.ramp(erupt, LavaScene.GEYSER_BURST, LavaScene.GEYSER_BURST + 18)), seed,
                0xF000F0);
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
        int plates = 7;
        for (int k = 0; k < plates; k++) {
            float a = age * .03f + k * Mth.TWO_PI / plates;
            float y = .35f + 1.25f * ((k * .37f) % 1) + .06f * Mth.sin(age * .1f + k);
            float rr = (.78f + .1f * Mth.sin(age * .05f + k)) * (2 - appear);
            Vector3f out = new Vector3f(Mth.cos(a), 0, Mth.sin(a));
            Vector3f tangent = new Vector3f(-Mth.sin(a), 0, Mth.cos(a));
            Vector3f centre = new Vector3f(out).mul(rr).add(0, y, 0);
            float w = (.22f + .08f * BossMesh.hash(seed + k)) * shown, hgt = (.26f + .1f * BossMesh.hash(seed + k * 3)) * shown;
            BossMesh.box(stone, poseStack.last(), centre, new Vector3f(tangent).mul(w), new Vector3f(0, hgt, 0).add(new Vector3f(out).mul(-.05f)),
                    new Vector3f(out).mul(.04f), BossMesh.argb(0xFFFFFF, 1), light);
            BossMesh.box(veins, poseStack.last(), new Vector3f(out).mul(rr + .045f).add(0, y, 0), new Vector3f(tangent).mul(w * .92f), new Vector3f(0, hgt * .92f, 0),
                    new Vector3f(out).mul(.002f), BossMesh.glow(ORANGE, (.35f + .2f * Mth.sin(age * .2f + k) + .6f * flare) * shown), FULL);
        }
        ClockMesh.glow(poseStack, buffer, view, new Vector3f(0, 1, 0), 2.6f, RED, (.15f + .6f * flare) * shown, FULL);
        if (flare > 0) {
            ClockMesh.sparks(poseStack, buffer, view, new Vector3f(0, 1, 0), 1.6f, 14, age, flare, seed, FULL);
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
        Vector3f f = v(e.forward()), side = new Vector3f(f.z, 0, -f.x);
        float half = LavaScene.RIVER_HALF;
        int segs = 28;
        VertexConsumer lava = BossMesh.ground(buffer, LAVA);
        VertexConsumer crust = BossMesh.ground(buffer, CRUST);
        for (int i = 0; i < segs; i++) {
            float t0 = (float) i / segs, t1 = (float) (i + 1) / segs;
            float w0 = half * (.85f + .25f * Mth.sin(t0 * 9 + seed)), w1 = half * (.85f + .25f * Mth.sin(t1 * 9 + seed));
            Vector3f c0 = new Vector3f(f).mul(t0 * length).add(0, .05f, 0), c1 = new Vector3f(f).mul(t1 * length).add(0, .05f, 0);
            // The front of the flow is already cooling as it creeps
            float front = BossMesh.ramp(t1, .8f, 1) * (1 - flow) * .6f;
            int col = BossMesh.mix(0xFFFFFF, 0x553030, Math.min(1, cool + front));
            float v0 = t0 * length * .4f - age * .03f, v1 = t1 * length * .4f - age * .03f;
            BossMesh.quad(lava, poseStack.last(), new Vector3f(c0).sub(new Vector3f(side).mul(w0)), new Vector3f(c0).add(new Vector3f(side).mul(w0)),
                    new Vector3f(c1).add(new Vector3f(side).mul(w1)), new Vector3f(c1).sub(new Vector3f(side).mul(w1)), 0, v0, 1, v0, 1, v1, 0, v1,
                    BossMesh.argb(col, gone), BossMesh.argb(col, gone), FULL, BossMesh.UP);
            // Its banks of crust
            for (int s = -1; s <= 1; s += 2) {
                Vector3f in0 = new Vector3f(c0).add(new Vector3f(side).mul(s * w0 * .92f)), out0 = new Vector3f(c0).add(new Vector3f(side).mul(s * (w0 + .3f)));
                Vector3f in1 = new Vector3f(c1).add(new Vector3f(side).mul(s * w1 * .92f)), out1 = new Vector3f(c1).add(new Vector3f(side).mul(s * (w1 + .3f)));
                in0.y = in1.y = out0.y = out1.y = .07f;
                if (s > 0) {
                    BossMesh.quad(crust, poseStack.last(), in0, out0, out1, in1, 0, t0 * 3, 1, t0 * 3, 1, t1 * 3, 0, t1 * 3, BossMesh.argb(0xFFFFFF, gone),
                            BossMesh.argb(0xFFFFFF, gone), light, BossMesh.UP);
                } else {
                    BossMesh.quad(crust, poseStack.last(), out0, in0, in1, out1, 0, t0 * 3, 1, t0 * 3, 1, t1 * 3, 0, t1 * 3, BossMesh.argb(0xFFFFFF, gone),
                            BossMesh.argb(0xFFFFFF, gone), light, BossMesh.UP);
                }
            }
        }
        // Plates of crust drifting on it
        VertexConsumer stone = BossMesh.solid(buffer, CRUST);
        for (int k = 0; k < 9; k++) {
            float t = (BossMesh.hash(seed + k) + age * .006f) % 1;
            if (t * e.getSize() > length) {
                continue;
            }
            float across = (BossMesh.hash(seed + k * 3) * 2 - 1) * half * .5f;
            Vector3f at = new Vector3f(f).mul(t * e.getSize()).add(new Vector3f(side).mul(across)).add(0, .08f, 0);
            float s = .15f + .15f * BossMesh.hash(seed + k * 5);
            float turn = age * .01f + k;
            BossMesh.box(stone, poseStack.last(), at, new Vector3f(Mth.cos(turn), 0, Mth.sin(turn)).mul(s), new Vector3f(0, .025f, 0),
                    new Vector3f(-Mth.sin(turn), 0, Mth.cos(turn)).mul(s * .8f), BossMesh.argb(0xFFFFFF, gone), light);
        }
        for (int k = 0; k <= 4; k++) {
            Vector3f p = new Vector3f(f).mul(length * k / 4f).add(0, .3f, 0);
            ClockMesh.glow(poseStack, buffer, view, p, 3, ORANGE, .3f * (1 - cool) * gone, FULL);
        }
        ClockMesh.sparks(poseStack, buffer, view, new Vector3f(f).mul(length * .5f), length * .3f, 12, age, (1 - cool) * gone, seed, FULL);
    }

    // ---------------------------------------------------------------- Caldera

    private void caldera(LavaScene e, PoseStack poseStack, MultiBufferSource buffer, WaterMesh.View view, float age, float pt, int seed, int light) {
        float r = e.getSize();
        VertexConsumer glow = BossMesh.light(buffer, BossMesh.GLOW);
        if (!e.collapsed()) {
            // The ground cracking: spokes out from the caster, rings joining them, brighter as it nears the end
            float t = BossMesh.ramp(age, 0, LavaScene.CALDERA_COLLAPSE);
            for (int k = 0; k < 14; k++) {
                float a = k * Mth.TWO_PI / 14 + BossMesh.hash(seed + k) * .3f;
                Vector3f[] crack = BossMesh.jagged(new Vector3f(0, .05f, 0), new Vector3f(Mth.cos(a) * r * t, .05f, Mth.sin(a) * r * t), 7, .25f, seed + k);
                for (int i = 0; i + 1 < crack.length; i++) {
                    RitualMesh.shaft(glow, poseStack.last(), new Vector3f(0, 60, 0), crack[i], crack[i + 1], .2f, BossMesh.glow(ORANGE, .4f + .6f * t), FULL);
                }
            }
            for (int ring = 1; ring <= 2; ring++) {
                float rr = r * ring / 2.5f;
                if (t * r > rr) {
                    ClockMesh.ring(poseStack.last(), buffer, .05f, rr - .1f, rr + .1f, ORANGE, .7f * t, FULL);
                }
            }
            ClockMesh.glow(poseStack, buffer, view, new Vector3f(0, .2f, 0), r * 2 * t, RED, .4f * t, FULL);
            ClockMesh.smoke(poseStack, buffer, view, new Vector3f(0, .1f, 0), new Vector3f(0, 1, 0), 3, 1.6f, 8, age, .02f, .45f * t, SMOKE, seed, light);
            return;
        }
        float since = e.sinceLanded(pt);
        float spread = BossMesh.ramp(since, 0, 8);
        float fade = 1 - BossMesh.ramp(age, e.getLife() - 20, e.getLife());
        // The lake, slowly turning, its rim of crust
        BossMesh.annulus(BossMesh.ground(buffer, LAVA), poseStack.last(), new Vector3f(0, .05f, 0), new Vector3f(1, 0, 0), new Vector3f(0, 0, -1), .01f, r * spread,
                64, 6, age * .0015f, age * .003f, BossMesh.argb(0xFFFFFF, fade), BossMesh.argb(0xFFC090, fade), FULL, false);
        BossMesh.annulus(BossMesh.ground(buffer, CRUST), poseStack.last(), new Vector3f(0, .06f, 0), new Vector3f(1, 0, 0), new Vector3f(0, 0, -1), r * spread * .9f,
                r * spread * 1.08f, 64, 16, 0, 0, BossMesh.argb(0xFFFFFF, 0), BossMesh.argb(0xFFFFFF, fade), light, false);
        // Fountains bursting from it, each in its own time
        for (int k = 0; k < 6; k++) {
            float cycle = 30 + 14 * BossMesh.hash(seed + k);
            float t = ((age + BossMesh.hash(seed + k * 3) * cycle) % cycle) / cycle;
            float a = BossMesh.hash(seed + k * 5) * Mth.TWO_PI, rr = r * .65f * Mth.sqrt(BossMesh.hash(seed + k * 7));
            float hgt = (2 + 2 * BossMesh.hash(seed + k * 11)) * Mth.sin(Mth.PI * t);
            Vector3f base = new Vector3f(Mth.cos(a) * rr, 0, Mth.sin(a) * rr);
            for (int i = 0; i < 4; i++) {
                float y = hgt * (1 - i * .22f);
                magma(poseStack, buffer, view, new Vector3f(base).add(0, y, 0), (.35f - i * .06f) * fade, age + k * 13 + i * 5, fade);
            }
        }
        if (since < 20) {
            float t = since / 20f;
            ClockMesh.ring(poseStack.last(), buffer, .1f, r * t, r * t + 1, HOT, 1 - t, FULL);
            ClockMesh.sparks(poseStack, buffer, view, new Vector3f(0, .5f, 0), r * .8f, 30, age, 1 - t, seed, FULL);
        }
        ClockMesh.glow(poseStack, buffer, view, new Vector3f(0, .6f, 0), r * 2.6f, ORANGE, .35f * fade, FULL);
        ClockMesh.smoke(poseStack, buffer, view, new Vector3f(0, .3f, 0), new Vector3f(0, 1, 0), 4, 2, 10, age, .015f, .4f * fade, SMOKE, seed, light);
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
