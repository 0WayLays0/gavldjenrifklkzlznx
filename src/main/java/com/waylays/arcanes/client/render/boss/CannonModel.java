package com.waylays.arcanes.client.render.boss;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.waylays.arcanes.client.render.ritual.RitualMesh;
import com.waylays.arcanes.client.render.water.WaterMesh;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Annihilation Beam: a rune engine forms at the head of the strip, facing down it: three rings of black iron and gold
 * turning on each other like a gyroscope, faster and faster as it charges, a great crimson gem at their heart drawing
 * the light in, a rune circle spinning before it. Then it fires: a thick beam of crimson energy with a white-hot core
 * rushes down the strip, rings of light racing along it, the engine shuddering; and it winds down and folds away.
 */
public final class CannonModel {
    private static final int CRIMSON = 0xFF3048;

    private CannonModel() {}

    /**
     * At the head of the strip (the pose's origin), its engine {@code height} up, facing down the strip along {@code forward};
     * {@code charge} 0 to 1 while it charges, {@code firing} ticks since it fired (negative before), lasting
     * {@code duration}; the beam {@code length} long and {@code width} wide.
     */
    public static void draw(PoseStack poseStack, MultiBufferSource buffer, WaterMesh.View view, Vector3f forward, float height, float charge, float firing, int duration,
                            float age, float length, float width, int light) {
        boolean fired = firing >= 0;
        float end = 1 - Mth.clamp((firing - duration) / 8f, 0, 1);
        float form = Math.min(1, age / 10f) * end;
        if (form <= 0) {
            return;
        }
        Vector3f f = new Vector3f(forward).normalize();
        // Over the caster's head and behind it, so that it stands clear of them and of the ground
        Vector3f centre = new Vector3f(0, height + 1.5f, 0).sub(new Vector3f(f).mul(1.2f));
        // The engine's own space: its axis (local Y) down the strip
        Quaternionf facing = new Quaternionf().rotationTo(new Vector3f(0, 1, 0), f);
        float spin = fired ? age * .5f : age * (.05f + .3f * charge * charge);
        float shake = fired && firing < duration ? .05f * Mth.sin(age * 3.1f) : 0;
        float s = form * (.6f + .4f * Math.min(1, charge + (fired ? 1 : 0)));

        // ---- the rings: black iron, gold edges, studs
        VertexConsumer iron = BossMesh.solid(buffer, BellModel.IRON);
        for (int r = 0; r < 3; r++) {
            place(poseStack, centre, facing, s, shake);
            ring(poseStack, r, spin);
            Sculpt.torus(iron, poseStack.last(), 1.9f - r * .45f, .13f - r * .02f, 48, 8, 0, Mth.TWO_PI, 10, 0xFFFFFFFF, light);
            poseStack.popPose();
        }
        VertexConsumer gold = BossMesh.solid(buffer, BellModel.GOLD);
        for (int r = 0; r < 3; r++) {
            place(poseStack, centre, facing, s, shake);
            ring(poseStack, r, spin);
            float big = 1.9f - r * .45f;
            for (int k = 0; k < 8; k++) {
                float a = k * Mth.TWO_PI / 8;
                poseStack.pushPose();
                poseStack.translate(Mth.cos(a) * big, 0, Mth.sin(a) * big);
                poseStack.mulPose(new Quaternionf().rotationY(-a).rotateZ(-Mth.HALF_PI));
                BossMesh.horn(gold, poseStack.last(), new Vector3f(), new Vector3f(0, 1, 0), new Vector3f(), .35f - r * .06f, .08f, 5, 2, 0xFFFFFFFF, 0xFFFFF0C0, light);
                poseStack.popPose();
            }
            Sculpt.torus(gold, poseStack.last(), big, .05f, 48, 5, 0, Mth.TWO_PI, 10, 0xFFE0C080, light);
            poseStack.popPose();
        }
        // ---- the gem at its heart (crimson glass), its gold setting
        place(poseStack, centre, facing, s, shake);
        poseStack.mulPose(new Quaternionf().rotationY(spin * .7f));
        Sculpt.torus(gold, poseStack.last(), .62f, .07f, 24, 6, 0, Mth.TWO_PI, 4, 0xFFFFFFFF, light);
        poseStack.popPose();
        VertexConsumer glass = BossMesh.solid(buffer, LotusGlass.TEXTURE);
        place(poseStack, centre, facing, s, shake);
        poseStack.mulPose(new Quaternionf().rotationY(spin * .7f));
        Sculpt.gem(glass, poseStack.last(), .58f, .5f, .9f, 10, 0xFFFFFFFF, light);
        poseStack.mulPose(new Quaternionf().rotationX(Mth.PI));
        Sculpt.gem(glass, poseStack.last(), .58f, .3f, .5f, 10, 0xFFFFFFFF, light);
        poseStack.popPose();

        // ---- light: the rune circle before it, motes drawn in, the beam, its rings
        int heat = BossMesh.glow(CRIMSON, fired ? 1 : .35f + .65f * charge);
        VertexConsumer runes = BossMesh.light(buffer, BossMesh.RUNES);
        Vector3f muzzle = new Vector3f(centre).add(new Vector3f(f).mul(1.2f));
        Vector3f u = BossMesh.across(f), v = new Vector3f(f).cross(u).normalize();
        float cs = Mth.cos(spin * 1.5f), sn = Mth.sin(spin * 1.5f);
        Vector3f ru = new Vector3f(u).mul(cs).add(new Vector3f(v).mul(sn)), rv = new Vector3f(u).mul(-sn).add(new Vector3f(v).mul(cs));
        BossMesh.plate(runes, poseStack.last(), muzzle, ru, rv, (fired ? 5.4f : 2 + 3 * charge) * end, BossMesh.glow(CRIMSON, (fired ? 1 : .5f + .5f * charge) * end),
                light);
        VertexConsumer glow = BossMesh.light(buffer, BossMesh.GLOW);
        RitualMesh.billboard(glow, poseStack, view, centre, (1.5f + 3 * charge + (fired ? 3 : 0)) * end, 0, -1, heat, light);
        if (!fired) {
            VertexConsumer motes = BossMesh.light(buffer, BossMesh.MOTE);
            for (int k = 0; k < 20; k++) {
                float t = (age * .05f + BossMesh.hash(k * 7)) % 1;
                float a = BossMesh.hash(k * 13) * Mth.TWO_PI + t * 3;
                float rr = 5 * (1 - t);
                Vector3f at = new Vector3f(centre).add(new Vector3f(u).mul(Mth.cos(a) * rr)).add(new Vector3f(v).mul(Mth.sin(a) * rr));
                RitualMesh.billboard(motes, poseStack, view, at, .35f, 0, -1, BossMesh.glow(k % 3 == 0 ? 0xFFFFFF : CRIMSON, Mth.sin(t * Mth.PI) * charge), light);
            }
        } else if (end > 0) {
            Vector3f to = new Vector3f(muzzle).add(new Vector3f(f).mul(length));
            // Sloping down to the strip's far end, at the height of a body
            to.y = height;
            float flicker = .88f + .12f * Mth.sin(age * 2.3f) + .06f * Mth.sin(age * 5.7f);
            float burst = firing < 6 ? 1 + (1 - firing / 6) * .8f : 1;
            float w = width * end * flicker * burst;
            VertexConsumer beam = BossMesh.light(buffer, BossMesh.BEAM);
            // A wide haze, the crimson body, the white-hot core
            RitualMesh.shaft(beam, poseStack.last(), view.eye(), muzzle, to, w * 2.6f, BossMesh.glow(CRIMSON, .35f * end), light);
            RitualMesh.shaft(beam, poseStack.last(), view.eye(), muzzle, to, w * 1.5f, BossMesh.glow(CRIMSON, end), light);
            RitualMesh.shaft(beam, poseStack.last(), view.eye(), muzzle, to, w * .9f, BossMesh.glow(0xFF8090, .8f * end), light);
            RitualMesh.shaft(beam, poseStack.last(), view.eye(), muzzle, to, w * .42f, BossMesh.glow(0xFFF4F0, end), light);
            // Rings of light racing down it
            VertexConsumer ring = BossMesh.light(buffer, BossMesh.RING);
            for (int k = 0; k < 12; k++) {
                float t = (age * .1f + k / 12f) % 1;
                Vector3f at = new Vector3f(muzzle).lerp(to, t);
                BossMesh.plate(ring, poseStack.last(), at, u, v, w * (1.9f + .9f * Mth.sin(t * Mth.PI)), BossMesh.glow(k % 2 == 0 ? 0xFFFFFF : CRIMSON, (1 - t) * end),
                        light);
            }
            // The ground it burns along, and where it strikes the far end: a blaze, a shock rolling out
            VertexConsumer scorch = BossMesh.light(buffer, BossMesh.NOISE);
            Vector3f side = new Vector3f(-f.z, 0, f.x).mul(w * .9f);
            int segs = Math.max(2, (int) (length / 2));
            for (int i = 0; i < segs; i++) {
                Vector3f a0 = new Vector3f(f).mul(1.5f + length * i / segs).add(0, .06f, 0), a1 = new Vector3f(f).mul(1.5f + length * (i + 1) / segs).add(0, .06f, 0);
                float v0 = i * .5f - age * .2f, v1 = (i + 1) * .5f - age * .2f;
                BossMesh.faced(scorch, poseStack.last(), new Vector3f(a0).add(side), new Vector3f(a0).sub(side), new Vector3f(a1).sub(side), new Vector3f(a1).add(side),
                        new float[] {1, v0, 0, v0, 0, v1, 1, v1}, BossMesh.glow(CRIMSON, .8f * end), BossMesh.glow(CRIMSON, .8f * end), BossMesh.glow(0xFF6040, .8f * end),
                        BossMesh.glow(0xFF6040, .8f * end), light, BossMesh.UP);
            }
            VertexConsumer impact = BossMesh.light(buffer, BossMesh.GLOW);
            float throb = .8f + .2f * Mth.sin(age * 1.9f);
            RitualMesh.billboard(impact, poseStack, view, to, w * 5 * throb, 0, -1, BossMesh.glow(CRIMSON, end), light);
            RitualMesh.billboard(impact, poseStack, view, to, w * 2.2f * throb, 0, -1, BossMesh.glow(0xFFFFFF, end), light);
            RitualMesh.billboard(impact, poseStack, view, muzzle, w * 3.2f * burst, 0, -1, BossMesh.glow(0xFFE0E0, .8f * end), light);
            VertexConsumer shock = BossMesh.light(buffer, BossMesh.RING);
            float wave = (age * .12f) % 1;
            BossMesh.pool(shock, poseStack.last(), to.x, .1f, to.z, w * (2 + 6 * wave), 0, BossMesh.glow(CRIMSON, (1 - wave) * end), light);
            VertexConsumer sparks = BossMesh.light(buffer, BossMesh.MOTE);
            for (int k = 0; k < 24; k++) {
                float life = (age * .12f + BossMesh.hash(k * 5)) % 1;
                float a = BossMesh.hash(k * 7) * Mth.TWO_PI;
                Vector3f vel = new Vector3f(Mth.cos(a) * .35f, .4f + .4f * BossMesh.hash(k * 11), Mth.sin(a) * .35f);
                Vector3f at = WaterMesh.thrown(to, vel, life * 14, .06f);
                RitualMesh.billboard(sparks, poseStack, view, at, .45f * (1 - life), 0, -1, BossMesh.glow(life < .4f ? 0xFFFFFF : CRIMSON, (1 - life) * end), light);
            }
        }
    }

    private static void place(PoseStack poseStack, Vector3f centre, Quaternionf facing, float s, float shake) {
        poseStack.pushPose();
        poseStack.translate(centre.x + shake, centre.y + shake * .5f, centre.z);
        poseStack.mulPose(facing);
        poseStack.scale(s, s, s);
    }

    /** Turns ring {@code r} of the gyroscope: the outer about the engine's axis, the next two about crossing axes. */
    private static void ring(PoseStack poseStack, int r, float spin) {
        if (r == 0) {
            poseStack.mulPose(new Quaternionf().rotationY(spin));
        } else if (r == 1) {
            poseStack.mulPose(new Quaternionf().rotationX(spin * 1.3f + .6f));
        } else {
            poseStack.mulPose(new Quaternionf().rotationZ(-spin * 1.7f + 1.2f));
        }
    }

    /** The crimson glass of the lotus, for the gem at the engine's heart. */
    private static final class LotusGlass {
        static final net.minecraft.resources.ResourceLocation TEXTURE = com.waylays.arcanes.ArcanesOublies.id("textures/entity/boss/m_crimson_glass.png");
    }
}
