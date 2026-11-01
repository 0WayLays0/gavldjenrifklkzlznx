package com.waylays.arcanes.client.render.boss;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.waylays.arcanes.client.render.fire.FireMesh;
import com.waylays.arcanes.client.render.ritual.RitualMesh;
import com.waylays.arcanes.client.render.water.WaterMesh;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

/**
 * The Fallen Moon: a moon, grey and cratered, shows high in the sky in a pale halo and comes down on the arena, slowly
 * then faster and faster; as it falls its underside catches fire, flames stream up behind it and cracks open across it
 * glowing like embers, while its shadow spreads on the ground and dust and stones rise to meet it. It strikes: a flash,
 * a dome of fire swelling over the arena, a ring racing out, a wall of dust rolling away, and the moon bursting into
 * burning pieces.
 */
public final class MoonModel {
    /** How long the aftermath lasts (ticks). */
    public static final int AFTER = 40;
    private static final int FRAGMENTS = 16;

    private MoonModel() {}

    /** How far down it has come at {@code fall} (0 to 1 of its fall): slowly at first, then faster and faster. */
    public static float eased(float fall) {
        return (float) Math.pow(fall, 2.4f);
    }

    /**
     * Drawn from the middle of the arena (the pose's origin, on the ground); {@code from} where the moon shows (in the
     * same frame), {@code radius} the moon's, {@code zone} the arena's; {@code age} ticks since it showed, {@code fall}
     * 0 to 1 of its fall, {@code since} ticks since it struck (negative before).
     */
    public static void draw(PoseStack poseStack, MultiBufferSource buffer, WaterMesh.View view, Vector3f from, float radius, float zone, float age, float fall,
                            float since, int seed, int light) {
        if (since < 0) {
            falling(poseStack, buffer, view, from, radius, zone, age, fall, seed, light);
        } else {
            impact(poseStack, buffer, view, radius, zone, age, since, seed, light);
        }
    }

    private static void falling(PoseStack poseStack, MultiBufferSource buffer, WaterMesh.View view, Vector3f from, float radius, float zone, float age, float fall,
                                int seed, int light) {
        float e = eased(fall);
        Vector3f centre = new Vector3f(from).lerp(new Vector3f(0, radius * .55f, 0), e);
        float show = BossMesh.ramp(age, 0, 20);
        float r = radius * (.6f + .4f * show);
        float heat = BossMesh.ramp(fall, .25f, 1);
        float speed = BossMesh.ramp(fall, .4f, 1);
        float turn = age * .0015f + BossMesh.hash(seed) ;
        // Its shadow and the arena darkening under it
        VertexConsumer shade = BossMesh.see(buffer, BossMesh.DISC);
        BossMesh.pool(shade, poseStack.last(), centre.x, .04f, centre.z, zone * 2 * (.35f + .65f * e), 0, BossMesh.argb(0x080406, .55f * e * show), light);
        // The moon
        VertexConsumer rock = BossMesh.solid(buffer, BossMesh.MOON);
        BossMesh.sphere(rock, poseStack.last(), centre, r, 16, 32, turn, 0, 1, 0xFFFFFFFF, light);
        // Its cracks, glowing as it heats
        VertexConsumer cracks = BossMesh.light(buffer, BossMesh.MOON_CRACKS);
        float glowK = heat * (.75f + .25f * Mth.sin(age * .35f));
        BossMesh.sphere(cracks, poseStack.last(), centre, r * 1.006f, 16, 32, turn, 0, 1, BossMesh.glow(BossMesh.mix(0xFF5A1A, 0xFFD080, heat), glowK), light);
        // Its underside burning
        VertexConsumer burn = BossMesh.light(buffer, BossMesh.NOISE);
        BossMesh.sphere(burn, poseStack.last(), centre, r * 1.04f, 6, 32, turn * 3 - age * .01f, .55f, 1, BossMesh.glow(0xFF8A3A, .8f * heat), light);
        BossMesh.sphere(burn, poseStack.last(), centre, r * 1.13f, 6, 32, -age * .02f, .6f, 1, BossMesh.glow(0xFF5020, .5f * heat), light);
        // Its pale halo, giving way to the glow of its fire
        VertexConsumer glow = BossMesh.light(buffer, BossMesh.GLOW);
        RitualMesh.billboard(glow, poseStack, view, centre, r * 4.5f, 0, -1, BossMesh.glow(0xC8DCFF, .5f * show * (1 - .7f * heat)), light);
        RitualMesh.billboard(glow, poseStack, view, new Vector3f(centre).add(0, -r * .6f, 0), r * 4 * (.5f + heat), 0, -1, BossMesh.glow(0xFF7030, .6f * heat),
                light);
        // Flames streaming up behind it
        FireMesh.Batch fire = new FireMesh.Batch(view, 1, age, light);
        if (heat > 0) {
            for (int k = 0; k < 18; k++) {
                float a = k * Mth.TWO_PI / 18 + BossMesh.hash(seed + k) * .3f;
                float lat = .55f + .25f * BossMesh.hash(seed + k * 3);
                float th = lat * Mth.PI;
                Vector3f at = new Vector3f(Mth.sin(th) * Mth.cos(a), Mth.cos(th), Mth.sin(th) * Mth.sin(a)).mul(r * 1.02f).add(centre);
                Vector3f up = new Vector3f(Mth.cos(a) * .25f, 1, Mth.sin(a) * .25f).normalize();
                fire.lickAlong(at, up, r * .75f, r * (.6f + 2.6f * speed) * (.8f + .4f * BossMesh.hash(seed + k * 5)), seed + k * 11, .9f * heat);
            }
        }
        // Dust whirling up off the ground, and stones rising to meet it
        float near = BossMesh.ramp(fall, .55f, 1);
        if (near > 0) {
            VertexConsumer dust = BossMesh.see(buffer, BossMesh.DUST);
            for (int k = 0; k < 22; k++) {
                float a = k * Mth.TWO_PI / 22 + age * .03f * (1 + BossMesh.hash(seed + k));
                float rr = zone * (.45f + .5f * BossMesh.hash(seed + k * 7)) * (1.1f - .2f * near);
                WaterMesh.puff(dust, poseStack, view, Mth.cos(a) * rr, .6f + 1.5f * near * BossMesh.hash(seed + k * 9), Mth.sin(a) * rr, 3 + 3 * near, a, -1,
                        BossMesh.argb(0x9A8C7C, .55f * near), light);
            }
            VertexConsumer stones = BossMesh.solid(buffer, BossMesh.MOON);
            for (int k = 0; k < 12; k++) {
                float a = BossMesh.hash(seed + k * 13) * Mth.TWO_PI;
                float rr = zone * .7f * BossMesh.hash(seed + k * 17);
                float lift = near * near * (centre.y - r) * (.3f + .5f * BossMesh.hash(seed + k * 19));
                BossMesh.sphere(stones, poseStack.last(), new Vector3f(Mth.cos(a) * rr, .3f + lift, Mth.sin(a) * rr), .3f + .3f * BossMesh.hash(seed + k), 4, 6,
                        age * .01f + k, 0, 1, 0xFFB0A8A0, light);
            }
        }
        fire.flush(buffer, poseStack);
    }

    private static void impact(PoseStack poseStack, MultiBufferSource buffer, WaterMesh.View view, float radius, float zone, float age, float since, int seed,
                               int light) {
        float t = Mth.clamp(since / AFTER, 0, 1);
        Vector3f heart = new Vector3f(0, radius * .55f, 0);
        // The moon in burning pieces, flung out and falling
        VertexConsumer rock = BossMesh.solid(buffer, BossMesh.MOON);
        Vector3f[] at = new Vector3f[FRAGMENTS];
        float[] size = new float[FRAGMENTS];
        for (int k = 0; k < FRAGMENTS; k++) {
            float a = k * Mth.TWO_PI / FRAGMENTS + BossMesh.hash(seed + k) * .4f;
            float out = .45f + .5f * BossMesh.hash(seed + k * 3);
            Vector3f v = new Vector3f(Mth.cos(a) * out, .5f + .7f * BossMesh.hash(seed + k * 5), Mth.sin(a) * out);
            Vector3f p = WaterMesh.thrown(new Vector3f(Mth.cos(a), .3f, Mth.sin(a)).mul(radius * .6f).add(heart), v, since, .045f);
            p.y = Math.max(p.y, .3f);
            at[k] = p;
            size[k] = radius * (.12f + .14f * BossMesh.hash(seed + k * 7)) * (1 - BossMesh.ramp(since, AFTER * .6f, AFTER));
            if (size[k] > .02f) {
                BossMesh.sphere(rock, poseStack.last(), p, size[k], 5, 8, since * .02f + k, 0, 1, 0xFFFFFFFF, light);
            }
        }
        VertexConsumer cracks = BossMesh.light(buffer, BossMesh.MOON_CRACKS);
        for (int k = 0; k < FRAGMENTS; k++) {
            if (size[k] > .02f) {
                BossMesh.sphere(cracks, poseStack.last(), at[k], size[k] * 1.02f, 5, 8, since * .02f + k, 0, 1, BossMesh.glow(0xFF7A30, 1 - t), light);
            }
        }
        // The dome of fire swelling over the arena
        float swell = BossMesh.ramp(since, 0, 14);
        VertexConsumer dome = BossMesh.light(buffer, BossMesh.NOISE);
        BossMesh.sphere(dome, poseStack.last(), new Vector3f(), zone * (.25f + .8f * swell), 8, 32, -since * .01f, 0, .5f,
                BossMesh.glow(0xFFA050, .7f * (1 - BossMesh.ramp(since, 4, 30))), light);
        // The ring racing out
        VertexConsumer ring = BossMesh.light(buffer, BossMesh.RING);
        BossMesh.pool(ring, poseStack.last(), 0, .1f, 0, zone * 2 * (.2f + 1.6f * BossMesh.ramp(since, 0, 18)), 0, BossMesh.glow(0xFFD0A0, 1 - BossMesh.ramp(since, 0, 18)),
                light);
        // The flash
        VertexConsumer glow = BossMesh.light(buffer, BossMesh.GLOW);
        float flash = 1 - BossMesh.ramp(since, 0, 10);
        RitualMesh.billboard(glow, poseStack, view, heart, zone * 2.5f, 0, -1, BossMesh.glow(0xFFFFFF, flash), light);
        RitualMesh.billboard(glow, poseStack, view, heart, zone * 1.4f, 0, -1, BossMesh.glow(0xFF8040, .8f * (1 - t)), light);
        // The wall of dust rolling away
        VertexConsumer dust = BossMesh.see(buffer, BossMesh.DUST);
        for (int k = 0; k < 30; k++) {
            float a = k * Mth.TWO_PI / 30 + BossMesh.hash(seed + k * 9) * .2f;
            float rr = 2 + zone * 1.25f * (float) Math.sqrt(BossMesh.ramp(since, 0, AFTER));
            WaterMesh.puff(dust, poseStack, view, Mth.cos(a) * rr, 1 + 1.5f * BossMesh.hash(seed + k), Mth.sin(a) * rr, 4 + 4 * t, a + since * .02f, -1,
                    BossMesh.argb(0x8E8274, .65f * (1 - t)), light);
        }
        // Fire where it struck
        FireMesh.Batch fire = new FireMesh.Batch(view, 1, age, light);
        float burn = 1 - BossMesh.ramp(since, 10, AFTER);
        for (int j = 0; j < 2; j++) {
            float r = zone * (.18f + .2f * j);
            int n = (int) (Mth.TWO_PI * r / 1.5f);
            Vector3f[] base = new Vector3f[n + 1];
            float[] h = new float[n + 1], al = new float[n + 1];
            for (int i = 0; i <= n; i++) {
                float a = i * Mth.TWO_PI / n;
                base[i] = new Vector3f(Mth.cos(a) * r, .05f, Mth.sin(a) * r);
                h[i] = (3 - j) * burn;
                al[i] = burn;
            }
            fire.curtain(base, h, al, .9f, seed + j * 17);
        }
        fire.flush(buffer, poseStack);
    }
}
