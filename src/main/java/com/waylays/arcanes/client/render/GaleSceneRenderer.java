package com.waylays.arcanes.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.waylays.arcanes.ArcanesOublies;
import com.waylays.arcanes.client.render.boss.BossMesh;
import com.waylays.arcanes.client.render.ritual.RitualMesh;
import com.waylays.arcanes.client.render.water.WaterMesh;
import com.waylays.arcanes.client.render.wind.WindMesh;
import com.waylays.arcanes.entity.gale.GaleScene;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import static com.waylays.arcanes.client.render.ElementFx.FULL;

/**
 * The Gale's spells, drawn as air so dense it bends the light: rippling lines, light against dark, through which the
 * world seems to waver, faintly violet, a white edge where the air cuts. Blades of air flying with their after-images;
 * a ball of crushed air, shells of bent air turning round a bright heart, that bursts in rings; a low wave of wind
 * running along the ground, dust kicked up before it; the dash's trail cut across with blades; the weight of the air
 * pressing down in a column of falling lines on a cracking ring; and Cor Leonis, blades flashing all round the caster
 * in every direction inside a whirling wall of bent air.
 */
public class GaleSceneRenderer extends EntityRenderer<GaleScene> {
    public static final ResourceLocation WARP = ArcanesOublies.id("textures/entity/gale/warp.png");
    public static final ResourceLocation BLADE = ArcanesOublies.id("textures/entity/gale/blade.png");
    private static final int WHITE = 0xF8F4FF, LILAC = 0xC4A8FF, VIOLET = 0x9A66FF, DEEP = 0x4A2A9A;

    public GaleSceneRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(GaleScene e, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource out, int packedLight) {
        SceneBuffers buffer = SceneBuffers.begin();
        // On top of snow layers or a carpet, if the scene stands in them
        poseStack.pushPose();
        poseStack.translate(0, ElementFx.floorLift(e), 0);
        float age = e.tickCount + partialTicks;
        WaterMesh.View view = WaterViews.of(this.entityRenderDispatcher, e, partialTicks);
        int seed = e.getId() * 41;
        int light = ElementFx.light(e, 15);
        switch (e.getKind()) {
            case SLASH -> slash(e, poseStack, buffer, view, age, partialTicks, seed);
            case SPHERE -> sphere(e, poseStack, buffer, view, age, partialTicks, seed);
            case SWEEP -> sweep(e, poseStack, buffer, view, age, seed, light);
            case DASH -> dash(e, poseStack, buffer, view, age, partialTicks, seed);
            case GRAVITY -> gravity(e, poseStack, buffer, view, age, seed, light);
            case STORM -> storm(e, poseStack, buffer, view, age, seed);
        }
        buffer.flush(out);
        poseStack.popPose();
        super.render(e, entityYaw, partialTicks, poseStack, out, packedLight);
    }

    // ---------------------------------------------------------------- the pieces

    /** Any unit vector square to {@code d}. */
    private static Vector3f square(Vector3f d) {
        Vector3f s = Math.abs(d.y) < .95f ? new Vector3f(d).cross(0, 1, 0) : new Vector3f(1, 0, 0);
        return s.normalize();
    }

    /** A line through {@code pts}, turned to the eye: a violet haze under a white core, brightest midway. */
    private static void line(MultiBufferSource buffer, PoseStack poseStack, WaterMesh.View view, Vector3f[] pts, float width, float alpha) {
        if (alpha <= .01f || pts.length < 2) {
            return;
        }
        VertexConsumer c = BossMesh.light(buffer, WindMesh.STREAK);
        int n = pts.length;
        int[] halo = new int[n], core = new int[n];
        for (int i = 0; i < n; i++) {
            float k = Mth.sin(Mth.PI * i / (n - 1));
            halo[i] = BossMesh.glow(VIOLET, .45f * alpha * k);
            core[i] = BossMesh.glow(WHITE, alpha * k);
        }
        // A violet line drawn see-through under the glow: the glow alone is lost against a bright sky or snow
        int[] under = new int[n];
        for (int i = 0; i < n; i++) {
            under[i] = BossMesh.argb(VIOLET, .75f * alpha * Mth.sin(Mth.PI * i / (n - 1)));
        }
        RitualMesh.thread(BossMesh.see(buffer, WindMesh.STREAK), poseStack.last(), view.eye(), pts, width * 2.2f, under, FULL);
        RitualMesh.thread(c, poseStack.last(), view.eye(), pts, width * 3.5f, halo, FULL);
        RitualMesh.thread(c, poseStack.last(), view.eye(), pts, width, core, FULL);
    }

    /**
     * A blade of air at {@code at}, flying along {@code f}: an arc {@code r} across ({@code sweep} radians), bowed
     * forward, slanted by {@code tilt} round its flight; its white edge in front, the bent air trailing {@code width}
     * behind it, thickest in its middle.
     */
    private static void crescent(MultiBufferSource buffer, PoseStack poseStack, WaterMesh.View view, Vector3f at, Vector3f f, float tilt, float r,
                                 float sweep, float width, float age, float alpha) {
        if (alpha <= .01f) {
            return;
        }
        Vector3f fw = new Vector3f(f).normalize();
        Vector3f s0 = square(fw), up = new Vector3f(s0).cross(fw).normalize();
        Vector3f side = new Vector3f(s0).mul(Mth.cos(tilt)).add(new Vector3f(up).mul(Mth.sin(tilt)));
        int n = 16;
        Vector3f[] front = new Vector3f[n], back = new Vector3f[n], wide = new Vector3f[n];
        for (int i = 0; i < n; i++) {
            float t = (float) i / (n - 1), a = (t - .5f) * sweep;
            float w = width * Mth.sin(Mth.PI * t) + .05f;
            front[i] = new Vector3f(side).mul(Mth.sin(a) * r).add(new Vector3f(fw).mul((Mth.cos(a) - 1) * r)).add(at);
            back[i] = new Vector3f(front[i]).sub(new Vector3f(fw).mul(w));
            wide[i] = new Vector3f(front[i]).sub(new Vector3f(fw).mul(w * 2.2f));
        }
        VertexConsumer bent = BossMesh.see(buffer, WARP);
        VertexConsumer edge = BossMesh.see(buffer, BLADE);
        VertexConsumer glow = BossMesh.light(buffer, BLADE);
        for (int i = 0; i + 1 < n; i++) {
            float u0 = (float) i / (n - 1) * 2 + age * .35f, u1 = (float) (i + 1) / (n - 1) * 2 + age * .35f;
            // The air bent behind it: rippling light and dark, widest of all
            BossMesh.quad2(bent, poseStack.last(), front[i], front[i + 1], wide[i + 1], wide[i], u0, 0, u1, 0, u1, 1, u0, 1, BossMesh.argb(LILAC, .9f * alpha),
                    BossMesh.argb(LILAC, .9f * alpha), FULL);
            // Its body, lilac, and its edge, glowing
            BossMesh.quad2(edge, poseStack.last(), front[i], front[i + 1], back[i + 1], back[i], u0, 0, u1, 0, u1, 1, u0, 1, BossMesh.argb(VIOLET, alpha),
                    BossMesh.argb(VIOLET, alpha), FULL);
            BossMesh.quad2(glow, poseStack.last(), front[i], front[i + 1], back[i + 1], back[i], u0, 0, u1, 0, u1, 1, u0, 1, BossMesh.glow(VIOLET, .7f * alpha),
                    BossMesh.glow(VIOLET, .7f * alpha), FULL);
        }
        line(buffer, poseStack, view, front, .08f, alpha);
    }

    /** A lens of bent air facing the eye: rippling rings round {@code at}, turning. */
    private static void lens(MultiBufferSource buffer, PoseStack poseStack, WaterMesh.View view, Vector3f at, float size, float age, float alpha) {
        if (alpha <= .01f) {
            return;
        }
        VertexConsumer c = BossMesh.see(buffer, WARP);
        for (int k = 0; k < 3; k++) {
            RitualMesh.billboard(c, poseStack, view, at, size * (1 - k * .22f), age * (.05f + k * .03f) * (k % 2 == 0 ? 1 : -1) + k, -1,
                    BossMesh.argb(LILAC, alpha * (.8f - k * .15f)), FULL);
        }
    }

    // ---------------------------------------------------------------- Air Slash

    private void slash(GaleScene e, PoseStack poseStack, MultiBufferSource buffer, WaterMesh.View view, float age, float pt, int seed) {
        Vector3f f = ElementFx.v(e.aim());
        float appear = Math.min(1, age / 2f);
        float fade = e.burst() ? Math.max(0, 1 - e.sinceBurst(pt) / 8f) : 1;
        float grow = e.burst() ? 1 + e.sinceBurst(pt) * .06f : 1;
        float r = GaleScene.SLASH_WIDTH * .62f * grow;
        crescent(buffer, poseStack, view, new Vector3f(), f, e.tilt(), r, 2f, .9f, age, appear * fade);
        // Its after-images, fainter and fainter behind it
        for (int k = 1; k <= 3; k++) {
            Vector3f at = new Vector3f(f).mul(-k * GaleScene.SLASH_SPEED * .45f);
            crescent(buffer, poseStack, view, at, f, e.tilt(), r * (1 - k * .06f), 2f - k * .15f, .6f, age - k, appear * fade * (.45f - k * .12f));
        }
        // Lines of wind streaming back off it
        Vector3f side = square(f);
        for (int k = 0; k < 6; k++) {
            float h = BossMesh.hash(seed + k);
            Vector3f a = new Vector3f(side).mul((h - .5f) * r * 1.6f).add(new Vector3f(f).mul(-.4f));
            Vector3f b = new Vector3f(a).add(new Vector3f(f).mul(-1.6f - h * 1.4f));
            line(buffer, poseStack, view, new Vector3f[]{b, new Vector3f(a).lerp(b, .5f), a}, .03f, .6f * appear * fade);
        }
        ElementFx.bloom(poseStack, buffer, view, new Vector3f(), r * .5f, VIOLET, .25f * appear * fade);
    }

    // ---------------------------------------------------------------- Air Sphere

    private void sphere(GaleScene e, PoseStack poseStack, MultiBufferSource buffer, WaterMesh.View view, float age, float pt, int seed) {
        if (!e.burst()) {
            float appear = Math.min(1, age / 3f);
            float r = .7f * appear;
            VertexConsumer bent = BossMesh.see(buffer, WARP);
            // Shells of bent air, each turning its own way, round a bright heart
            for (int k = 0; k < 3; k++) {
                poseStack.pushPose();
                poseStack.mulPose(new org.joml.Quaternionf().rotateAxis(age * (.12f + k * .07f) * (k % 2 == 0 ? 1 : -1), k == 1 ? 1 : 0, k == 1 ? 0 : 1,
                        k == 2 ? 1 : 0));
                ElementFx.lumpy(bent, poseStack.last(), new Vector3f(), r * (1 - k * .18f), 10, 16, .05f, 2, age * .1f, seed + k, 1,
                        BossMesh.argb(LILAC, .85f), FULL);
                poseStack.popPose();
            }
            // Rings of wind wound round it
            for (int k = 0; k < 3; k++) {
                float a = age * .3f + k * 2.1f;
                Vector3f u = new Vector3f(Mth.cos(a), Mth.sin(a * .7f), Mth.sin(a)).normalize(), v = square(u);
                line(buffer, poseStack, view, WindMesh.arc(new Vector3f(), u, v, r * 1.25f, age * .5f + k, 4.2f, 14), .05f, .8f);
            }
            ElementFx.bloom(poseStack, buffer, view, new Vector3f(), r * .7f, VIOLET, .7f);
            lens(buffer, poseStack, view, new Vector3f(), r * 3.2f, age, .55f);
            ElementFx.motes(poseStack, buffer, view, BossMesh.MOTE, new Vector3f(0, -r, 0), r * 1.6f, r * 2, .1f, 10, age * 3, LILAC, .8f, seed);
            return;
        }
        // The burst: shells of bent air swelling out, a ring running over the ground, lines flung outward
        float t = Math.min(1, e.sinceBurst(pt) / GaleScene.SPHERE_AFTER);
        float out = 1 - (1 - t) * (1 - t);
        VertexConsumer bent = BossMesh.see(buffer, WARP);
        for (int k = 0; k < 2; k++) {
            float rr = GaleScene.SPHERE_BURST_R * out * (1 - k * .3f);
            ElementFx.lumpy(bent, poseStack.last(), new Vector3f(), rr, 12, 20, .04f, 2, age * .2f, seed + k, 1, BossMesh.argb(LILAC, (1 - t) * (.8f - k * .3f)),
                    FULL);
        }
        for (int k = 0; k < 18; k++) {
            float a = k * Mth.TWO_PI / 18 + BossMesh.hash(seed + k), b = (BossMesh.hash(seed + k * 3) - .5f) * 1.4f;
            Vector3f d = new Vector3f(Mth.cos(a) * Mth.cos(b), Mth.sin(b), Mth.sin(a) * Mth.cos(b));
            Vector3f head = new Vector3f(d).mul(GaleScene.SPHERE_BURST_R * out * 1.1f), tail = new Vector3f(d).mul(GaleScene.SPHERE_BURST_R * out * .5f);
            line(buffer, poseStack, view, new Vector3f[]{tail, new Vector3f(tail).lerp(head, .5f), head}, .05f, 1 - t);
        }
        ElementFx.bloom(poseStack, buffer, view, new Vector3f(), 1.5f * (1 - t * .5f), VIOLET, 1 - t);
        lens(buffer, poseStack, view, new Vector3f(), GaleScene.SPHERE_BURST_R * 2.4f * out, age, .7f * (1 - t));
    }

    // ---------------------------------------------------------------- Sweeping Gale

    private void sweep(GaleScene e, PoseStack poseStack, MultiBufferSource buffer, WaterMesh.View view, float age, int seed, int light) {
        float t = Math.min(1, age / GaleScene.SWEEP_TIME);
        float fade = 1 - BossMesh.ramp(age, GaleScene.SWEEP_TIME, GaleScene.SWEEP_TIME + 12);
        if (fade <= .01f) {
            return;
        }
        Vector3f f = ElementFx.v(new Vec3(e.aim().x, 0, e.aim().z).normalize());
        Vector3f side = new Vector3f(f.z, 0, -f.x);
        float reach = GaleScene.SWEEP_REACH * t, half = (1.5f + (GaleScene.SWEEP_WIDTH - 1.5f) * t) / 2;
        int n = 14;
        Vector3f[] base = new Vector3f[n], top = new Vector3f[n], crest = new Vector3f[n];
        for (int i = 0; i < n; i++) {
            float s = (float) i / (n - 1) * 2 - 1;
            float bow = (1 - s * s) * .9f;
            float h = (.95f - .4f * s * s);
            base[i] = new Vector3f(f).mul(reach + bow).add(new Vector3f(side).mul(s * half)).add(0, .05f, 0);
            top[i] = new Vector3f(base[i]).add(new Vector3f(f).mul(-.35f)).add(0, h, 0);
            crest[i] = new Vector3f(base[i]).add(new Vector3f(f).mul(-.2f)).add(0, h * .9f, 0);
        }
        VertexConsumer bent = BossMesh.see(buffer, WARP);
        VertexConsumer edge = BossMesh.light(buffer, BLADE);
        for (int i = 0; i + 1 < n; i++) {
            float u0 = (float) i / (n - 1) * 3 + age * .4f, u1 = (float) (i + 1) / (n - 1) * 3 + age * .4f;
            // The wave: a wall of bent air, low (at the legs), its crest bright
            BossMesh.quad2(bent, poseStack.last(), top[i], top[i + 1], base[i + 1], base[i], u0, 0, u1, 0, u1, 1, u0, 1, BossMesh.argb(LILAC, fade),
                    BossMesh.argb(LILAC, .6f * fade), FULL);
            BossMesh.quad2(edge, poseStack.last(), top[i], top[i + 1], base[i + 1], base[i], u0, .1f, u1, .1f, u1, 1, u0, 1, BossMesh.glow(VIOLET, .6f * fade),
                    BossMesh.glow(VIOLET, .1f * fade), FULL);
        }
        line(buffer, poseStack, view, crest, .06f, fade);
        // Lines racing along the ground behind it, dust thrown up before it
        for (int k = 0; k < 8; k++) {
            float h = BossMesh.hash(seed + k), across = (h * 2 - 1) * half * .9f;
            Vector3f a = new Vector3f(f).mul(reach * .95f).add(new Vector3f(side).mul(across)).add(0, .1f + .3f * BossMesh.hash(seed + k * 3), 0);
            Vector3f b = new Vector3f(a).sub(new Vector3f(f).mul(1.5f + 2 * h));
            line(buffer, poseStack, view, new Vector3f[]{b, new Vector3f(a).lerp(b, .5f), a}, .03f, .7f * fade);
        }
        ElementFx.puffs(poseStack, buffer, view, WindMesh.DUST, new Vector3f(f).mul(reach + .5f).add(0, .2f, 0), half, .8f, .8f, 6, age, .08f, 0xC8BCA8,
                .6f * fade, seed + 5, light);
    }

    // ---------------------------------------------------------------- Cutting Dash

    private void dash(GaleScene e, PoseStack poseStack, MultiBufferSource buffer, WaterMesh.View view, float age, float pt, int seed) {
        Vector3f to;
        float fade;
        if (!e.burst()) {
            Entity caster = e.caster();
            if (caster == null) {
                return;
            }
            to = ElementFx.v(caster.getPosition(pt).subtract(e.getPosition(pt)));
            fade = 1;
        } else {
            to = ElementFx.v(e.end());
            fade = Math.max(0, 1 - e.sinceBurst(pt) / GaleScene.DASH_AFTER);
        }
        if (to.lengthSquared() < .01f || fade <= .01f) {
            return;
        }
        Vector3f f = new Vector3f(to).normalize(), side = square(f), up = new Vector3f(side).cross(f).normalize();
        if (up.y < 0) {
            up.negate();
        }
        Vector3f lift = new Vector3f(0, 1, 0);
        // The trail: a ribbon of bent air at waist height, from where the dash began to where it is
        VertexConsumer bent = BossMesh.see(buffer, WARP);
        int n = 10;
        for (int i = 0; i < n; i++) {
            float t0 = (float) i / n, t1 = (float) (i + 1) / n;
            Vector3f a0 = new Vector3f(to).mul(t0).add(lift), a1 = new Vector3f(to).mul(t1).add(lift);
            float w0 = .7f * t0 + .2f, w1 = .7f * t1 + .2f;
            BossMesh.quad2(bent, poseStack.last(), new Vector3f(a0).add(new Vector3f(up).mul(w0)), new Vector3f(a1).add(new Vector3f(up).mul(w1)),
                    new Vector3f(a1).sub(new Vector3f(up).mul(w1)), new Vector3f(a0).sub(new Vector3f(up).mul(w0)), t0 * 3 - age * .2f, 0, t1 * 3 - age * .2f, 0,
                    t1 * 3 - age * .2f, 1, t0 * 3 - age * .2f, 1, BossMesh.argb(LILAC, fade * t0), BossMesh.argb(LILAC, fade * t1), FULL);
        }
        // Speed lines along it, and blades crossing it where it cut
        for (int k = 0; k < 7; k++) {
            float h = BossMesh.hash(seed + k);
            Vector3f off = new Vector3f(side).mul((h - .5f) * 1.4f).add(new Vector3f(up).mul((BossMesh.hash(seed + k * 5) - .5f) * 1.2f)).add(lift);
            line(buffer, poseStack, view, new Vector3f[]{new Vector3f(off), new Vector3f(to).mul(.5f).add(off), new Vector3f(to).add(off)}, .025f,
                    .7f * fade);
        }
        float len = to.length();
        for (int k = 0; k < 3; k++) {
            Vector3f at = new Vector3f(to).mul((k + 1) / 4f).add(lift);
            float tilt = (k % 2 == 0 ? 1 : -1) * .8f;
            crescent(buffer, poseStack, view, at, side, tilt, .9f, 2.4f, .25f, age, fade * Math.min(1, len / 3));
        }
        ElementFx.groundRing(poseStack, buffer, .05f, .4f + (1 - fade) * 1.5f, .7f + (1 - fade) * 1.6f, LILAC, .7f * fade);
    }

    // ---------------------------------------------------------------- Crushing Gravity

    private void gravity(GaleScene e, PoseStack poseStack, MultiBufferSource buffer, WaterMesh.View view, float age, int seed, int light) {
        float r = GaleScene.GRAVITY_R;
        float appear = BossMesh.ramp(age, 0, 8);
        float fade = 1 - BossMesh.ramp(age, e.getLife() - 15, e.getLife());
        float shown = appear * fade;
        if (shown <= .01f) {
            return;
        }
        Vector3f u = new Vector3f(1, 0, 0), v = new Vector3f(0, 0, -1);
        // The ground under the weight: a shadow, bent air turning on it, a ring cracking
        ElementFx.shadow(poseStack, buffer, new Vector3f(), r * 1.1f, .45f * shown);
        BossMesh.annulus(BossMesh.ground(buffer, WARP), poseStack.last(), new Vector3f(0, .06f, 0), u, v, .01f, r, 48, 3, age * .01f, age * .02f,
                BossMesh.argb(LILAC, .8f * shown), BossMesh.argb(LILAC, .5f * shown), FULL, false);
        ElementFx.groundRing(poseStack, buffer, .07f, r - .25f, r, VIOLET, .8f * shown);
        ElementFx.groundRing(poseStack, buffer, .07f, r * .55f, r * .6f, DEEP, .5f * shown);
        // The weight overhead: a lens of bent air pressing down
        BossMesh.annulus(BossMesh.see(buffer, WARP), poseStack.last(), new Vector3f(0, 4.2f - .4f * Mth.sin(age * .15f), 0), u, v, .01f, r, 48, 2,
                -age * .015f, -age * .03f, BossMesh.argb(LILAC, .7f * shown), BossMesh.argb(LILAC, 0), FULL, true);
        // Lines of falling air, raining straight down hard
        for (int k = 0; k < 18; k++) {
            float h1 = BossMesh.hash(seed + k), h2 = BossMesh.hash(seed + k * 3);
            float a = h1 * Mth.TWO_PI, rr = r * Mth.sqrt(h2);
            float y = 4.4f - ((age * .45f + BossMesh.hash(seed + k * 7) * 4.4f) % 4.4f);
            Vector3f top = new Vector3f(Mth.cos(a) * rr, y + 1.2f, Mth.sin(a) * rr), bottom = new Vector3f(Mth.cos(a) * rr, Math.max(.05f, y), Mth.sin(a) * rr);
            line(buffer, poseStack, view, new Vector3f[]{top, new Vector3f(top).lerp(bottom, .5f), bottom}, .06f, .9f * shown);
        }
        ElementFx.puffs(poseStack, buffer, view, WindMesh.DUST, new Vector3f(0, .1f, 0), r, .3f, .9f, 10, age, .03f, 0xC8BCA8, .45f * shown, seed + 3, light);
        ElementFx.bloom(poseStack, buffer, view, new Vector3f(0, 2.5f, 0), r * .5f, DEEP, .25f * shown);
    }

    // ---------------------------------------------------------------- Cor Leonis

    private void storm(GaleScene e, PoseStack poseStack, MultiBufferSource buffer, WaterMesh.View view, float age, int seed) {
        float appear = BossMesh.ramp(age, 0, 6);
        float fade = 1 - BossMesh.ramp(age, e.getLife() - 12, e.getLife());
        float shown = appear * fade;
        if (shown <= .01f) {
            return;
        }
        float R = GaleScene.STORM_R;
        // Blades flashing all round in every direction, each a moment, then the next
        int blades = 12, cycle = 7;
        for (int k = 0; k < blades; k++) {
            float local = age + k * cycle / (float) blades;
            int round = Mth.floor(local / cycle);
            float t = (local % cycle) / cycle;
            int h = seed + k * 31 + round * 977;
            float a = BossMesh.hash(h) * Mth.TWO_PI, rr = 1.1f + (R - 1.4f) * BossMesh.hash(h + 1);
            float y = .3f + 2.3f * BossMesh.hash(h + 2);
            Vector3f at = new Vector3f(Mth.cos(a) * rr, y, Mth.sin(a) * rr);
            Vector3f dir = new Vector3f(BossMesh.hash(h + 3) - .5f, (BossMesh.hash(h + 4) - .5f) * 1.2f, BossMesh.hash(h + 5) - .5f);
            if (dir.lengthSquared() < .01f) {
                dir.set(1, 0, 0);
            }
            at.add(new Vector3f(dir).normalize().mul((t - .5f) * 1.6f));
            float alpha = Mth.sin(Mth.PI * t) * shown;
            crescent(buffer, poseStack, view, at, dir, BossMesh.hash(h + 6) * Mth.TWO_PI, .8f + .6f * BossMesh.hash(h + 7), 2.2f, .45f, age, alpha);
        }
        // The typhoon: walls of bent air whirling round, lines spiralling up, a violet glow on its bearer
        ElementFx.wall(poseStack, buffer, WARP, R * .92f, 0, 3.2f, 3, age, .09f, VIOLET, .4f * shown, FULL);
        ElementFx.wall(poseStack, buffer, WARP, R * .6f, .2f, 2.6f, 2, age + 30, -.13f, VIOLET, .25f * shown, FULL);
        for (int k = 0; k < 5; k++) {
            float a0 = age * .35f + k * Mth.TWO_PI / 5;
            line(buffer, poseStack, view, WindMesh.helix(0, 0, R * .5f, R * .95f, a0, 2.6f, .1f, 3.2f, 14), .04f, .7f * shown);
        }
        ElementFx.groundRing(poseStack, buffer, .06f, R * .85f, R, VIOLET, .6f * shown);
        ElementFx.bloom(poseStack, buffer, view, new Vector3f(0, 1, 0), 1.2f, VIOLET, .35f * shown);
        ElementFx.motes(poseStack, buffer, view, BossMesh.MOTE, new Vector3f(), R, 3, .12f, 24, age * 4, LILAC, .8f * shown, seed + 9);
    }

    @Override
    public boolean shouldRender(GaleScene e, Frustum frustum, double x, double y, double z) {
        return true;
    }

    @Override
    public ResourceLocation getTextureLocation(GaleScene e) {
        return WARP;
    }
}
