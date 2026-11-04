package com.waylays.arcanes.client.render.aurora;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.waylays.arcanes.client.render.boss.BossMesh;
import com.waylays.arcanes.client.render.boss.Sculpt;
import com.waylays.arcanes.client.render.gem.GemMesh;
import com.waylays.arcanes.client.render.lotus.LotusMesh;
import com.waylays.arcanes.client.render.ritual.RitualMesh;
import com.waylays.arcanes.client.render.water.WaterMesh;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

/**
 * Aurora Magic's great spells.
 * <ul>
 * <li>Celestial Spiral: a funnel of aurora ribbons whirling up out of a ring of light, a curtain coiling round its foot,
 * stars swept round in it.</li>
 * <li>Aurora Serpent: a serpent woven of the northern lights: a long body of shimmering bands, two strands twisting
 * round it, fins of ice down its back, a head of ice with horns swept back, eyes of white fire and long whiskers of
 * light. It circles the sky and dives on its prey.</li>
 * <li>Polar Halo: a great ring of light high over the field, points of ice round it, curtains of the aurora hanging from
 * it; beams of every colour fall from it.</li>
 * <li>Boreal Night: a dome of night sky strewn with stars falls over the field, the aurora rippling across it; great
 * curtains sweep the ground; at last all the lights gather into a pillar.</li>
 * </ul>
 */
public final class AuroraSkyModels {
    private AuroraSkyModels() {}

    // ---------------------------------------------------------------- spiral

    public static void spiral(PoseStack poseStack, MultiBufferSource buffer, WaterMesh.View view, float age, int life, float radius, int seed, int light) {
        float k = BossMesh.ramp(age, 0, 15) * (1 - BossMesh.ramp(age, life - 15, life));
        if (k <= .01f) {
            return;
        }
        float h = 8 * k;
        PoseStack.Pose p = poseStack.last();
        VertexConsumer band = BossMesh.light(buffer, AuroraMesh.BAND);
        int strands = 9, n = 30;
        for (int s = 0; s < strands; s++) {
            Vector3f[] pts = new Vector3f[n];
            for (int i = 0; i < n; i++) {
                float t = (float) i / (n - 1);
                float y = t * h;
                float r = radius * (.35f + .75f * t) * k;
                float a = s * Mth.TWO_PI / strands + t * 4.5f + age * (.14f - .04f * t);
                pts[i] = new Vector3f(Mth.cos(a) * r, y, Mth.sin(a) * r);
            }
            AuroraMesh.ribbon(band, p, view.eye(), pts, .55f, .85f, age * 2 + s * 20, light);
        }
        // A curtain coiling round its foot
        VertexConsumer rays = BossMesh.light(buffer, AuroraMesh.RAYS);
        int m = 40;
        Vector3f[] coil = new Vector3f[m];
        for (int i = 0; i < m; i++) {
            float t = (float) i / (m - 1);
            float a = t * Mth.TWO_PI * 1.6f + age * .1f;
            float r = radius * (1.15f - .45f * t) * k;
            coil[i] = new Vector3f(Mth.cos(a) * r, t * 1.5f, Mth.sin(a) * r);
        }
        AuroraMesh.curtain(rays, p, coil, 2.5f, age, .8f * k, age * .02f, light);
        VertexConsumer ring = BossMesh.light(buffer, BossMesh.RING);
        AuroraMesh.ring(ring, p, .08f, radius * .9f * k, radius * 1.25f * k, AuroraMesh.TEAL, .7f * k, light);
        AuroraMesh.glow(poseStack, buffer, view, new Vector3f(0, h * .4f, 0), radius * 2.2f, AuroraMesh.TEAL, .25f * k, light);
        // Stars swept round in it
        VertexConsumer star = BossMesh.light(buffer, GemMesh.SPARKLE);
        for (int i = 0; i < 40; i++) {
            float hh = BossMesh.hash(seed + i * 7);
            float t = (hh + age * .01f) % 1;
            float a = BossMesh.hash(seed + i * 3) * Mth.TWO_PI + age * .2f + t * 4;
            float r = radius * (.35f + .75f * t) * k;
            RitualMesh.billboard(star, poseStack, view, new Vector3f(Mth.cos(a) * r, t * h, Mth.sin(a) * r), .35f, age * .1f, -1,
                    BossMesh.glow(i % 3 == 0 ? AuroraMesh.hue(t) : AuroraMesh.ICE, .9f * Mth.sin(t * Mth.PI)), light);
        }
    }

    // ---------------------------------------------------------------- serpent

    /** Where the serpent's head is at {@code t} (ticks since it was called), given its last dive (begun at {@code t0} on {@code target}, or none). */
    public static Vector3f head(float t, float t0, Vector3f target, float radius, float height, float spin, int dive) {
        float rise = BossMesh.ramp(t, 0, 18);
        if (target != null && t >= t0 && t <= t0 + dive) {
            float s = (t - t0) / dive;
            Vector3f from = circle(t0, radius, height, spin, 1), back = circle(t0 + dive, radius, height, spin, 1);
            Vector3f low = new Vector3f(target).add(0, 1.2f, 0);
            if (s < .5f) {
                float q = s * 2;
                Vector3f mid = new Vector3f(from).lerp(low, .5f).add(0, 3, 0);
                return bezier(from, mid, low, q * q);
            }
            float q = (s - .5f) * 2;
            Vector3f mid = new Vector3f(low).lerp(back, .5f).add(0, 1, 0);
            return bezier(low, mid, back, 1 - (1 - q) * (1 - q));
        }
        return circle(t, radius, height, spin, rise);
    }

    private static Vector3f circle(float t, float radius, float height, float spin, float rise) {
        float a = t * spin;
        return new Vector3f(Mth.cos(a) * radius * rise, 1.5f + (height - 1.5f) * rise + .8f * Mth.sin(t * spin * 2), Mth.sin(a) * radius * rise);
    }

    private static Vector3f bezier(Vector3f a, Vector3f m, Vector3f b, float t) {
        float s = 1 - t;
        return new Vector3f(a).mul(s * s).add(new Vector3f(m).mul(2 * s * t)).add(new Vector3f(b).mul(t * t));
    }

    /** Circling over the pose's origin; its last dive began {@code since} ticks ago on {@code target} (or null). */
    public static void serpent(PoseStack poseStack, MultiBufferSource buffer, WaterMesh.View view, float age, int life, float since, Vector3f target, float radius,
                               float height, float spin, int dive, int seed, int light) {
        float fade = 1 - BossMesh.ramp(age, life - 20, life);
        if (fade <= .01f) {
            return;
        }
        float t0 = target != null ? age - since : -1000;
        int n = 30;
        Vector3f[] body = new Vector3f[n];
        for (int i = 0; i < n; i++) {
            body[i] = head(Math.max(0, age - i * .85f), t0, target, radius, height, spin, dive);
        }
        PoseStack.Pose p = poseStack.last();
        Vector3f dirHead = new Vector3f(body[0]).sub(body[1]);
        if (dirHead.lengthSquared() < 1.0E-6f) {
            dirHead.set(1, 0, 0);
        }
        dirHead.normalize();
        // The body: a broad band of shimmering light, tapering to the tail; two strands twisting round it
        VertexConsumer band = BossMesh.light(buffer, AuroraMesh.BAND);
        float[] w = new float[n];
        int[] col = new int[n];
        for (int i = 0; i < n; i++) {
            float t = (float) i / (n - 1);
            w[i] = 2.2f * (1 - t * .8f) * (i == 0 ? .6f : 1);
            col[i] = BossMesh.glow(AuroraMesh.hue((t * 1.4f + age * .01f) % 1), .95f * fade);
        }
        com.waylays.arcanes.client.render.sand.SandMesh.ribbon(band, p, view.eye(), body, w, .5f, age * .03f, col, light);
        com.waylays.arcanes.client.render.sand.SandMesh.ribbon(band, p, view.eye(), body, scaled(w, .45f), .5f, age * .05f, white(n, .8f * fade), light);
        for (int s = 0; s < 2; s++) {
            Vector3f[] strand = new Vector3f[n];
            for (int i = 0; i < n; i++) {
                Vector3f tan = new Vector3f(body[Math.max(0, i - 1)]).sub(body[Math.min(n - 1, i + 1)]);
                if (tan.lengthSquared() < 1.0E-6f) {
                    tan.set(1, 0, 0);
                }
                tan.normalize();
                Vector3f u = BossMesh.across(tan), v = new Vector3f(u).cross(tan).normalize();
                float a = i * .7f - age * .3f + s * Mth.PI;
                float r = .9f * (1 - (float) i / n);
                strand[i] = new Vector3f(body[i]).add(new Vector3f(u).mul(Mth.cos(a) * r)).add(new Vector3f(v).mul(Mth.sin(a) * r));
            }
            AuroraMesh.ribbon(band, p, view.eye(), strand, .5f, .85f * fade, age * 3 + s * 50, light);
        }
        // Fins of ice down its back
        for (int i = 3; i < n - 6; i += 3) {
            Vector3f tan = new Vector3f(body[i - 1]).sub(body[i + 1]);
            if (tan.lengthSquared() < 1.0E-6f) {
                continue;
            }
            tan.normalize();
            Vector3f up = new Vector3f(0, 1, 0).sub(new Vector3f(tan).mul(tan.y)).normalize().add(new Vector3f(tan).mul(-.6f)).normalize();
            float s = .32f * (1 - (float) i / n) * fade;
            AuroraMesh.ice(buffer, p, new Vector3f(body[i]).add(new Vector3f(up).mul(s * 1.6f)), up, s, 2.4f, age * 2 + i, light);
        }
        // The head: a snout of ice, horns swept back, eyes of white fire, whiskers of light
        Vector3f h = body[0];
        Vector3f side = BossMesh.across(dirHead), top = new Vector3f(side).cross(dirHead).normalize();
        if (top.y < 0) {
            top.negate();
            side.negate();
        }
        AuroraMesh.ice(buffer, p, new Vector3f(h).add(new Vector3f(dirHead).mul(.45f)), dirHead, .5f * fade, 2.2f, age * 3, light);
        for (int s = -1; s <= 1; s += 2) {
            Vector3f horn = new Vector3f(dirHead).mul(-1).add(new Vector3f(top).mul(.9f)).add(new Vector3f(side).mul(s * .45f)).normalize();
            AuroraMesh.ice(buffer, p, new Vector3f(h).add(new Vector3f(top).mul(.3f)).add(new Vector3f(side).mul(s * .25f)).add(new Vector3f(horn).mul(.45f)), horn,
                    .2f * fade, 3.2f, age * 2 + s, light);
            Vector3f eye = new Vector3f(h).add(new Vector3f(dirHead).mul(.3f)).add(new Vector3f(side).mul(s * .3f)).add(new Vector3f(top).mul(.15f));
            AuroraMesh.glow(poseStack, buffer, view, eye, .45f, 0xFFFFFF, fade, light);
            Vector3f[] whisker = new Vector3f[8];
            for (int i = 0; i < 8; i++) {
                float t = i / 7f;
                whisker[i] = new Vector3f(h).add(new Vector3f(dirHead).mul(.6f - t * 2.5f)).add(new Vector3f(side).mul(s * (.3f + t * .9f)))
                        .add(new Vector3f(top).mul(-.1f + .4f * Mth.sin(t * 4 + age * .3f)));
            }
            AuroraMesh.ribbon(BossMesh.light(buffer, AuroraMesh.BAND), p, view.eye(), whisker, .14f, .9f * fade, age * 4, light);
        }
        AuroraMesh.glow(poseStack, buffer, view, h, 2.6f, AuroraMesh.TEAL, .55f * fade, light);
        // Where it strikes: a flash, a ring of light on the ground
        if (target != null && since > dive * .5f && since < dive * .5f + 14) {
            float t = (since - dive * .5f) / 14;
            VertexConsumer ring = BossMesh.light(buffer, BossMesh.RING);
            float rr = .6f + 4 * (1 - (1 - t) * (1 - t));
            BossMesh.annulus(ring, poseStack.last(), new Vector3f(target).add(0, .1f, 0), new Vector3f(1, 0, 0), new Vector3f(0, 0, -1), rr * .8f, rr, 40, 1, 0, 0,
                    BossMesh.glow(AuroraMesh.TEAL, 0), BossMesh.glow(AuroraMesh.GREEN, (1 - t)), light, true);
            AuroraMesh.glow(poseStack, buffer, view, new Vector3f(target).add(0, 1, 0), 3 + 3 * t, AuroraMesh.TEAL, (1 - t) * .8f, light);
        }
        AuroraMesh.stars(poseStack, buffer, view, new Vector3f(0, height - 2, 0), radius + 1, 4, age, 24, .7f * fade, seed, light);
    }

    private static float[] scaled(float[] w, float k) {
        float[] o = new float[w.length];
        for (int i = 0; i < w.length; i++) {
            o[i] = w[i] * k;
        }
        return o;
    }

    private static int[] white(int n, float a) {
        int[] c = new int[n];
        for (int i = 0; i < n; i++) {
            c[i] = BossMesh.glow(0xF4FFFF, a * (1 - (float) i / n));
        }
        return c;
    }

    // ---------------------------------------------------------------- halo

    /** Over the spot at the pose's origin, {@code radius} round, {@code height} up; its last beam fell {@code since} ticks ago at {@code target}. */
    public static void halo(PoseStack poseStack, MultiBufferSource buffer, WaterMesh.View view, float age, int life, float radius, float height, int open, float since,
                            Vector3f target, int seed, int light) {
        float k = BossMesh.ramp(age, 0, open) * (1 - BossMesh.ramp(age, life - 15, life));
        if (k <= .01f) {
            return;
        }
        float r = radius * .85f * k;
        float spin = age * .01f;
        PoseStack.Pose p = poseStack.last();
        // The ring of light itself, a second fainter one inside
        VertexConsumer band = BossMesh.light(buffer, AuroraMesh.BAND);
        Vector3f[] ring = AuroraMesh.arc(r, height, 65, spin, Mth.TWO_PI);
        int[] col = new int[ring.length];
        for (int i = 0; i < ring.length; i++) {
            col[i] = BossMesh.glow(AuroraMesh.band(i / 64f * 2, age * .5f), .95f);
        }
        com.waylays.arcanes.client.render.sand.SandMesh.ribbon(band, p, view.eye(), ring, AuroraMesh.fill(.7f, ring.length), .5f, age * .02f, col, light);
        Vector3f[] inner = AuroraMesh.arc(r * .82f, height + .3f, 49, -spin * 1.5f, Mth.TWO_PI);
        com.waylays.arcanes.client.render.sand.SandMesh.ribbon(band, p, view.eye(), inner, AuroraMesh.fill(.3f, inner.length), .5f, age * .03f,
                Sculpt.fill(BossMesh.glow(AuroraMesh.ICE, .7f), inner.length), light);
        // Points of ice round it, pointing out
        for (int i = 0; i < 16; i++) {
            float a = i * Mth.TWO_PI / 16 + spin;
            Vector3f out = new Vector3f(Mth.cos(a), .15f, Mth.sin(a)).normalize();
            AuroraMesh.ice(buffer, p, new Vector3f(Mth.cos(a) * (r + .35f), height, Mth.sin(a) * (r + .35f)), out, (i % 2 == 0 ? .3f : .2f) * k, 2.6f, age * 2 + i, light);
        }
        // Curtains hanging down from it
        VertexConsumer rays = BossMesh.light(buffer, AuroraMesh.RAYS);
        Vector3f[] hems = AuroraMesh.arc(r * .97f, height - 4.5f, 48, spin, Mth.TWO_PI);
        AuroraMesh.curtain(rays, p, hems, 4.3f, age, .75f * k, age * .01f, light);
        // Its shadow of light on the ground
        VertexConsumer ground = BossMesh.light(buffer, BossMesh.RING);
        AuroraMesh.ring(ground, p, .08f, r * .9f, r * 1.05f, AuroraMesh.VIOLET, .6f * k, light);
        AuroraMesh.glow(poseStack, buffer, view, new Vector3f(0, height, 0), r * 2.5f, AuroraMesh.TEAL, .15f * k, light);
        // A beam falling on its foe
        if (target != null && since >= 0 && since < 9) {
            float s = 1 - since / 9;
            int hue = AuroraMesh.hue(BossMesh.hash((int) (age - since)));
            Vector3f from = new Vector3f(target.x * .8f, height, target.z * .8f);
            VertexConsumer beam = BossMesh.light(buffer, BossMesh.BEAM);
            GemMesh.ray(beam, p, view.eye(), from, new Vector3f(target).add(0, .1f, 0), hue, .55f, s, age);
            VertexConsumer flash = BossMesh.light(buffer, BossMesh.RING);
            BossMesh.annulus(flash, p, new Vector3f(target).add(0, .12f, 0), new Vector3f(1, 0, 0), new Vector3f(0, 0, -1), .3f, .6f + 1.4f * (1 - s), 24, 1, 0, 0,
                    BossMesh.glow(hue, 0), BossMesh.glow(hue, s), light, true);
            AuroraMesh.glow(poseStack, buffer, view, new Vector3f(target).add(0, .8f, 0), 2.5f, hue, s * .8f, light);
        }
        AuroraMesh.stars(poseStack, buffer, view, new Vector3f(0, 1, 0), r, height, age, 30, .7f * k, seed, light);
    }

    // ---------------------------------------------------------------- night

    /** Over the caster's spot (the pose's origin), {@code radius} round. */
    public static void night(PoseStack poseStack, MultiBufferSource buffer, WaterMesh.View view, float age, int life, float radius, int fall, int wave, int sweep,
                             int gather, int seed, int light) {
        float dusk = BossMesh.ramp(age, 0, fall) * (1 - BossMesh.ramp(age, life - 25, life));
        if (dusk <= .01f) {
            return;
        }
        PoseStack.Pose p = poseStack.last();
        // The dome of night, strewn with stars
        VertexConsumer sky = BossMesh.see(buffer, AuroraMesh.NIGHT);
        BossMesh.sphere(sky, p, new Vector3f(), radius, 12, 36, age * .0006f, 0, .5f, BossMesh.argb(0xFFFFFF, .62f * dusk), light);
        VertexConsumer star = BossMesh.light(buffer, GemMesh.SPARKLE);
        for (int i = 0; i < 90; i++) {
            float az = BossMesh.hash(seed + i * 7) * Mth.TWO_PI + age * .0006f * Mth.TWO_PI;
            float el = .15f + 1.3f * BossMesh.hash(seed + i * 11);
            float tw = .6f + .4f * Mth.sin(age * (.08f + .1f * BossMesh.hash(i)) + i);
            Vector3f s = new Vector3f(Mth.cos(az) * Mth.cos(el), Mth.sin(el), Mth.sin(az) * Mth.cos(el)).mul(radius * .97f);
            RitualMesh.billboard(star, poseStack, view, s, .45f + .4f * BossMesh.hash(i * 5), i, -1, BossMesh.glow(i % 6 == 0 ? AuroraMesh.hue(BossMesh.hash(i)) : 0xFFFFFF,
                    tw * dusk), light);
        }
        // The aurora rippling across the dome
        VertexConsumer rays = BossMesh.light(buffer, AuroraMesh.RAYS);
        for (int c = 0; c < 3; c++) {
            int n = 40;
            Vector3f[] hems = new Vector3f[n];
            float tilt = c * 1.1f + age * .002f;
            for (int i = 0; i < n; i++) {
                float t = (float) i / (n - 1) * 2 - 1;
                float x = t * radius * .8f;
                float z = (c - 1) * radius * .3f + radius * .15f * Mth.sin(t * 3 + age * .02f + c);
                float y = radius * .45f + radius * .1f * Mth.cos(t * 2 + c) - Math.abs(t) * radius * .15f;
                hems[i] = new Vector3f(x * Mth.cos(tilt) - z * Mth.sin(tilt), y, x * Mth.sin(tilt) + z * Mth.cos(tilt));
            }
            AuroraMesh.curtain(rays, p, hems, radius * .35f, age + c * 40, .8f * dusk, age * .006f + c, light);
        }
        // The waves: great curtains sweeping the ground
        for (int w = 0; w < 4; w++) {
            float local = age - (fall + w * wave);
            if (local < 0 || local > sweep + 10) {
                continue;
            }
            float t = Math.min(1, local / sweep);
            float fade = 1 - BossMesh.ramp(local, sweep, sweep + 10);
            float a = w * Mth.HALF_PI + .4f;
            Vector3f across = new Vector3f(Mth.cos(a), 0, Mth.sin(a)), line = new Vector3f(-across.z, 0, across.x);
            float x = -radius + 2 * radius * t;
            float half = Mth.sqrt(Math.max(.1f, radius * radius - x * x));
            int n = 30;
            Vector3f[] hems = new Vector3f[n];
            for (int i = 0; i < n; i++) {
                float s = (float) i / (n - 1) * 2 - 1;
                hems[i] = new Vector3f(across).mul(x + .8f * Mth.sin(s * 5 + age * .1f)).add(new Vector3f(line).mul(s * half));
            }
            AuroraMesh.curtain(rays, p, hems, 9, age, fade, age * .02f, light);
            VertexConsumer pool = BossMesh.light(buffer, BossMesh.GLOW);
            for (int i = 0; i < 6; i++) {
                float s = (i / 5f * 2 - 1) * half;
                Vector3f g = new Vector3f(across).mul(x).add(new Vector3f(line).mul(s));
                BossMesh.pool(pool, p, g.x, .06f, g.z, 4, i, BossMesh.glow(AuroraMesh.GREEN, .3f * fade), light);
            }
        }
        // At last all the lights gather into a pillar
        float g = age - gather;
        if (g > -24) {
            float pull = BossMesh.ramp(g, -24, 0);
            VertexConsumer band = BossMesh.light(buffer, AuroraMesh.BAND);
            for (int s = 0; s < 10; s++) {
                float a0 = s * Mth.TWO_PI / 10 + age * .05f;
                int n = 16;
                Vector3f[] pts = new Vector3f[n];
                for (int i = 0; i < n; i++) {
                    float t = (float) i / (n - 1);
                    float r = radius * (1 - pull) * (1 - t) + .3f;
                    float a = a0 + t * 2;
                    pts[i] = new Vector3f(Mth.cos(a) * r, radius * .5f * (1 - t) * (1 - pull * .5f) + 1 + t * 2, Mth.sin(a) * r);
                }
                AuroraMesh.ribbon(band, p, view.eye(), pts, .5f, .8f * (g < 0 ? pull : Math.max(0, 1 - g / 10)), age * 3 + s * 20, light);
            }
            if (g >= 0 && g < 40) {
                float burst = (float) Math.exp(-g / 10);
                VertexConsumer beam = BossMesh.light(buffer, BossMesh.BEAM);
                float w = 2.6f * (1 - g / 50);
                int body = BossMesh.glow(AuroraMesh.TEAL, burst), core = BossMesh.glow(0xFFFFFF, burst);
                Sculpt.lathe(beam, p, new float[] {w, 0, w, 40}, 32, 4, .05f, body, body, light);
                Sculpt.lathe(beam, p, new float[] {w, 40, w, 0}, 32, 4, .05f, body, body, light);
                Sculpt.lathe(beam, p, new float[] {w * .4f, 0, w * .4f, 40}, 24, 4, .05f, core, core, light);
                Sculpt.lathe(beam, p, new float[] {w * .4f, 40, w * .4f, 0}, 24, 4, .05f, core, core, light);
                VertexConsumer ring = BossMesh.light(buffer, BossMesh.RING);
                float rt = BossMesh.ramp(g, 0, 20);
                float rr = radius * (.1f + 1.1f * rt);
                AuroraMesh.ring(ring, p, .15f, rr * .8f, rr, AuroraMesh.GREEN, (1 - rt), light);
                AuroraMesh.glow(poseStack, buffer, view, new Vector3f(0, 2, 0), 10, 0xFFFFFF, burst * .7f, light);
            }
        }
        LotusMesh.motes(poseStack, buffer, view, new Vector3f(0, .2f, 0), radius * .9f, 5, age, 40, AuroraMesh.GREEN, .8f * dusk, seed, light);
    }
}
