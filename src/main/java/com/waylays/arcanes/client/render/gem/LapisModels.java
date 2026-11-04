package com.waylays.arcanes.client.render.gem;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.waylays.arcanes.ArcanesOublies;
import com.waylays.arcanes.client.render.boss.BellModel;
import com.waylays.arcanes.client.render.boss.BossMesh;
import com.waylays.arcanes.client.render.boss.Sculpt;
import com.waylays.arcanes.client.render.lotus.LotusMesh;
import com.waylays.arcanes.client.render.ritual.RitualMesh;
import com.waylays.arcanes.client.render.water.WaterMesh;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * The lapis's newer spells. The lapis seals: the night sky and its golden stars.
 * <ul>
 * <li>Shooting Star: a five-pointed star of lapis flecked with gold, rimmed in gold, flying face first and turning,
 * a streak of gold behind it; it bursts in golden stars.</li>
 * <li>Celestial Vault: a disc of the night sky opens over the field, deep lapis set with a constellation of golden
 * stars, a ring of gold and runes round it; golden stars fall out of it onto the foes beneath.</li>
 * <li>Lapis Mirror: a tablet of lapis in a frame of gold, arched at its top, golden runes on it, circling its master;
 * it binds a foe with a chain of golden light.</li>
 * </ul>
 */
public final class LapisModels {
    public static final ResourceLocation LAPIS = ArcanesOublies.id("textures/entity/gem/lapis.png");
    private static final Vector3f U = new Vector3f(1, 0, 0), V = new Vector3f(0, 0, -1);
    private static final int BLUE = GemMesh.LAPIS, GOLD = GemMesh.LAPIS_GOLD;

    private LapisModels() {}

    /** A star of lapis, {@code r} to its points, facing along {@code normal}, turned {@code spin} about it, at {@code at}. */
    static void starSolid(PoseStack poseStack, MultiBufferSource buffer, Vector3f at, Vector3f normal, float spin, float r, int light) {
        poseStack.pushPose();
        poseStack.translate(at.x, at.y, at.z);
        poseStack.mulPose(LotusMesh.frame(BossMesh.across(normal).rotateAxis(spin, normal.x, normal.y, normal.z), normal));
        Sculpt.slab(BossMesh.solid(buffer, LAPIS), poseStack.last(), GemMesh.star(r, .44f), r * .35f, r * .12f, 1 / r * .5f, 0xFFFFFFFF, 0xFFF2C33C, light);
        poseStack.popPose();
    }

    // ---------------------------------------------------------------- shooting star

    public static void star(PoseStack poseStack, MultiBufferSource buffer, WaterMesh.View view, Vector3f dir, float age, boolean done, float since, int seed,
                            int light) {
        Vector3f d = new Vector3f(dir);
        if (d.lengthSquared() < 1.0E-6f) {
            d.set(0, 0, 1);
        }
        d.normalize();
        PoseStack.Pose p = poseStack.last();
        if (done) {
            float t = Mth.clamp(since / 12, 0, 1);
            if (t >= 1) {
                return;
            }
            float out = 1 - (1 - t) * (1 - t);
            for (int k = 0; k < 7; k++) {
                float a = k * Mth.TWO_PI / 7 + seed;
                Vector3f o = new Vector3f(Mth.cos(a), .4f + .6f * BossMesh.hash(seed + k), Mth.sin(a)).normalize();
                starSolid(poseStack, buffer, new Vector3f(o).mul(.3f + 2.2f * out).add(0, -t * t, 0), o, since * .5f + k, .2f * (1 - t * .6f), light);
            }
            VertexConsumer glow = BossMesh.light(buffer, BossMesh.GLOW);
            float flash = (1 - t) * (1 - t);
            RitualMesh.billboard(glow, poseStack, view, new Vector3f(), 1 + 3 * t, 0, -1, BossMesh.glow(0xFFF6D8, flash), light);
            RitualMesh.billboard(glow, poseStack, view, new Vector3f(), 3 + 3 * t, 0, -1, BossMesh.glow(BLUE, .8f * flash), light);
            VertexConsumer ring = BossMesh.light(buffer, BossMesh.RING);
            float rr = .4f + 2.6f * out;
            BossMesh.annulus(ring, p, new Vector3f(), new Vector3f(BossMesh.across(d)), new Vector3f(BossMesh.across(d)).cross(d).normalize(), rr * .8f, rr, 32, 1,
                    0, 0, BossMesh.glow(GOLD, 0), BossMesh.glow(GOLD, .8f * (1 - t)), light, true);
            return;
        }
        float grow = BossMesh.ramp(age, 0, 3);
        starSolid(poseStack, buffer, new Vector3f(), d, age * .35f, .55f * grow, light);
        VertexConsumer beam = BossMesh.light(buffer, BossMesh.BEAM);
        GemMesh.ray(beam, p, view.eye(), new Vector3f(d).mul(-.3f), new Vector3f(d).mul(-4), GOLD, .4f, .7f * grow, age);
        VertexConsumer sparkle = BossMesh.light(buffer, GemMesh.SPARKLE);
        for (int k = 0; k < 9; k++) {
            float t = (age * .25f + k / 9f) % 1;
            Vector3f side = BossMesh.across(d).rotateAxis(k * 2.3f + age * .2f, d.x, d.y, d.z);
            Vector3f at = new Vector3f(d).mul(-.6f - t * 3.5f).add(side.mul(.2f + .5f * t));
            RitualMesh.billboard(sparkle, poseStack, view, at, .35f * (1 - t), age * .1f, -1, BossMesh.glow(k % 3 == 0 ? 0xFFFFFF : GOLD, 1 - t), light);
        }
        VertexConsumer glow = BossMesh.light(buffer, BossMesh.GLOW);
        RitualMesh.billboard(glow, poseStack, view, new Vector3f(), 2.2f * grow, 0, -1, BossMesh.glow(BLUE, .45f), light);
        RitualMesh.billboard(glow, poseStack, view, new Vector3f(), .9f * grow, 0, -1, BossMesh.glow(GOLD, .5f), light);
    }

    // ---------------------------------------------------------------- celestial vault

    /** Over the ground at the pose's origin, {@code radius} round, {@code height} up; its last star fell {@code since} ticks ago on {@code target}. */
    public static void vault(PoseStack poseStack, MultiBufferSource buffer, WaterMesh.View view, float age, int life, float radius, float height, float since,
                             Vector3f target, int seed, int light) {
        float open = BossMesh.ramp(age, 0, 12) * (1 - BossMesh.ramp(age, life - 12, life));
        if (open <= .01f) {
            return;
        }
        float r = radius * open;
        float spin = age * .004f;
        PoseStack.Pose p = poseStack.last();
        Vector3f top = new Vector3f(0, height, 0);
        // ---- the night sky: a disc of deep lapis, seen from below
        poseStack.pushPose();
        poseStack.translate(0, height, 0);
        poseStack.mulPose(new Quaternionf().rotationY(spin));
        Sculpt.disc(BossMesh.see(buffer, LAPIS), poseStack.last(), 0, r, 48, 0, BossMesh.argb(0x8AA0E0, .92f), light, true);
        // Its rim of gold
        Sculpt.torus(BossMesh.solid(buffer, BellModel.GOLD), poseStack.last(), r, .14f * open, 64, 6, 0, Mth.TWO_PI, 12, 0xFFFFFFFF, light);
        poseStack.popPose();
        // A ring of golden runes turning round it, and its constellation
        VertexConsumer runes = BossMesh.light(buffer, BossMesh.RUNES);
        BossMesh.annulus(runes, p, new Vector3f(top).add(0, -.05f, 0), U, new Vector3f(0, 0, 1), r * .82f, r * .98f, 64, 6, -age * .01f, 0, BossMesh.glow(GOLD, .8f),
                BossMesh.glow(GOLD, .8f), light, true);
        Vector3f[] stars = new Vector3f[9];
        for (int k = 0; k < stars.length; k++) {
            float a = BossMesh.hash(seed + k * 7) * Mth.TWO_PI + spin;
            float d = r * (.15f + .6f * BossMesh.hash(seed + k * 11));
            stars[k] = new Vector3f(Mth.cos(a) * d, height - .08f, Mth.sin(a) * d);
        }
        VertexConsumer line = BossMesh.light(buffer, BossMesh.BEAM);
        for (int k = 0; k + 1 < stars.length; k++) {
            RitualMesh.shaft(line, p, view.eye(), stars[k], stars[k + 1], .06f, BossMesh.glow(GOLD, .55f), light);
        }
        VertexConsumer sparkle = BossMesh.light(buffer, GemMesh.SPARKLE);
        for (int k = 0; k < stars.length; k++) {
            float tw = .7f + .3f * Mth.sin(age * .2f + k * 1.7f);
            RitualMesh.billboard(sparkle, poseStack, view, stars[k], .7f * tw, age * .02f + k, -1, BossMesh.glow(k % 3 == 0 ? 0xFFFFFF : GOLD, tw), light);
        }
        for (int k = 0; k < 30; k++) {
            float a = BossMesh.hash(seed + k * 13) * Mth.TWO_PI + spin;
            float d = r * (float) Math.sqrt(BossMesh.hash(seed + k * 17)) * .95f;
            float tw = Mth.sin(age * (.1f + .1f * BossMesh.hash(k)) + k * 3);
            if (tw > .2f) {
                RitualMesh.billboard(sparkle, poseStack, view, new Vector3f(Mth.cos(a) * d, height - .06f, Mth.sin(a) * d), .3f, k, -1, BossMesh.glow(0xFFFFFF, tw * .8f),
                        light);
            }
        }
        // ---- its shadow on the ground: a ring of blue
        VertexConsumer ring = BossMesh.light(buffer, BossMesh.RING);
        BossMesh.annulus(ring, p, new Vector3f(0, .1f, 0), U, V, r * .9f, r, 56, 1, 0, 0, BossMesh.glow(BLUE, 0), BossMesh.glow(BLUE, .6f), light, false);
        VertexConsumer glow = BossMesh.light(buffer, BossMesh.GLOW);
        RitualMesh.billboard(glow, poseStack, view, new Vector3f(top).add(0, -.5f, 0), r * 2.2f, 0, -1, BossMesh.glow(BLUE, .25f * open), light);
        // ---- a star falling on its foe
        if (target != null && since >= 0 && since < 14) {
            float fall = 6;
            Vector3f from = new Vector3f(target.x * .6f, height, target.z * .6f);
            if (since < fall) {
                float t = since / fall;
                Vector3f at = new Vector3f(from).lerp(target, t * t);
                Vector3f d = new Vector3f(target).sub(from).normalize();
                starSolid(poseStack, buffer, at, new Vector3f(d).negate(), since, .35f, light);
                VertexConsumer beam = BossMesh.light(buffer, BossMesh.BEAM);
                GemMesh.ray(beam, p, view.eye(), at, new Vector3f(at).sub(new Vector3f(d).mul(3)), GOLD, .3f, .9f, age);
            } else {
                float b = (since - fall) / 8;
                VertexConsumer flash = BossMesh.light(buffer, BossMesh.GLOW);
                RitualMesh.billboard(flash, poseStack, view, target, .8f + 2 * b, 0, -1, BossMesh.glow(GOLD, (1 - b)), light);
                RitualMesh.billboard(flash, poseStack, view, target, 2 + 2 * b, 0, -1, BossMesh.glow(BLUE, (1 - b) * .6f), light);
            }
        }
    }

    // ---------------------------------------------------------------- mirror

    /** The tablet's outline (counter-clockwise): straight sides and an arched top, about 1 by 1.4. */
    private static float[] tablet() {
        int arc = 12;
        float[] s = new float[(arc + 1 + 2) * 2];
        int k = 0;
        s[k++] = -.5f;
        s[k++] = -.7f;
        s[k++] = .5f;
        s[k++] = -.7f;
        for (int i = 0; i <= arc; i++) {
            float a = Mth.lerp((float) i / arc, 0, Mth.PI);
            s[k++] = Mth.cos(a) * .5f;
            s[k++] = .2f + Mth.sin(a) * .5f;
        }
        return s;
    }

    private static final float[] TABLET = tablet();

    /** Round its master (the pose's origin), who faces {@code dir}; its last seal {@code since} ticks ago on {@code target}. */
    public static void mirror(PoseStack poseStack, MultiBufferSource buffer, WaterMesh.View view, Vector3f dir, float age, int life, float since, Vector3f target,
                              int seed, int light) {
        float k = BossMesh.ramp(age, 0, 10) * (1 - BossMesh.ramp(age, life - 12, life));
        if (k <= .01f) {
            return;
        }
        float a = age * .06f;
        Vector3f out = new Vector3f(Mth.cos(a), 0, Mth.sin(a));
        Vector3f at = new Vector3f(out).mul(1.5f).add(0, 1.3f + .1f * Mth.sin(age * .1f), 0);
        poseStack.pushPose();
        poseStack.translate(at.x, at.y, at.z);
        poseStack.mulPose(LotusMesh.frame(new Vector3f(0, 1, 0), out));
        poseStack.scale(k, k, k);
        Sculpt.slab(BossMesh.solid(buffer, LAPIS), poseStack.last(), TABLET, .12f, .03f, .5f, 0xFFFFFFFF, 0xFFF2C33C, light);
        // Its golden frame, standing proud of both faces
        Vector3f[] edge = new Vector3f[TABLET.length / 2 + 1];
        for (int i = 0; i < TABLET.length / 2; i++) {
            edge[i] = new Vector3f(TABLET[2 * i], TABLET[2 * i + 1], 0);
        }
        edge[edge.length - 1] = new Vector3f(edge[0]);
        Sculpt.sweep(BossMesh.solid(buffer, BellModel.GOLD), poseStack.last(), edge, Sculpt.fill(.06f, edge.length), Sculpt.fill(.1f, edge.length), Sculpt.circle(6),
                false, new Vector3f(0, 0, 1), 2, Sculpt.fill(0xFFFFFFFF, edge.length), false, light);
        // Golden runes burning on both its faces, a sheen sliding over them
        VertexConsumer sig = BossMesh.light(buffer, RitualMesh.SIGILS_GLOW);
        PoseStack.Pose p = poseStack.last();
        float glowing = .65f + .35f * Mth.sin(age * .15f);
        for (int side = -1; side <= 1; side += 2) {
            Vector3f n = new Vector3f(0, 0, side);
            for (int i = 0; i < 3; i++) {
                float y = .45f - i * .42f, s = .32f;
                Vector3f c = new Vector3f(0, y, side * .065f);
                Vector3f ux = new Vector3f(side * s * .5f, 0, 0), uy = new Vector3f(0, s * .5f, 0);
                int index = (seed + i * 5) & 15;
                float u0 = (index % 4) * .25f, v0 = (index / 4) * .25f;
                BossMesh.quad(sig, p, new Vector3f(c).sub(ux).sub(uy), new Vector3f(c).add(ux).sub(uy), new Vector3f(c).add(ux).add(uy), new Vector3f(c).sub(ux).add(uy),
                        u0, v0 + .25f, u0 + .25f, v0 + .25f, u0 + .25f, v0, u0, v0, BossMesh.glow(GOLD, glowing), BossMesh.glow(GOLD, glowing), light, n);
            }
        }
        poseStack.popPose();
        VertexConsumer glow = BossMesh.light(buffer, BossMesh.GLOW);
        RitualMesh.billboard(glow, poseStack, view, at, 2 * k, 0, -1, BossMesh.glow(BLUE, .3f), light);
        // A dome of faint blue light round its master, where shots are turned
        VertexConsumer ring = BossMesh.light(buffer, BossMesh.RING);
        BossMesh.annulus(ring, poseStack.last(), new Vector3f(0, .08f, 0), U, V, 2.6f * k, 3 * k, 48, 1, age * .02f, 0, BossMesh.glow(BLUE, 0), BossMesh.glow(BLUE,
                .45f), light, false);
        // ---- the seal: a chain of golden light to the foe, a ring of runes round it
        if (target != null && since >= 0 && since < 16) {
            float t = since / 16;
            float s = (1 - t) * (1 - t);
            VertexConsumer beam = BossMesh.light(buffer, BossMesh.BEAM);
            Vector3f[] chain = Sculpt.curve(at, new Vector3f(at).lerp(target, .5f).add(0, .8f, 0), target, 12);
            for (int i = 0; i + 1 < chain.length; i++) {
                RitualMesh.shaft(beam, poseStack.last(), view.eye(), chain[i], chain[i + 1], i % 2 == 0 ? .28f : .14f, BossMesh.glow(GOLD, s), light);
            }
            VertexConsumer runes = BossMesh.light(buffer, BossMesh.RUNES);
            BossMesh.annulus(runes, poseStack.last(), new Vector3f(target).add(0, -.6f, 0), U, V, .6f, 1.1f, 32, 3, age * .05f, 0, BossMesh.glow(GOLD, s),
                    BossMesh.glow(GOLD, s), light, true);
        }
    }
}
