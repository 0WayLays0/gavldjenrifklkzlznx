package com.waylays.arcanes.client.render.boss;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.waylays.arcanes.ArcanesOublies;
import com.waylays.arcanes.client.render.ritual.RitualMesh;
import com.waylays.arcanes.client.render.sand.SandMesh;
import com.waylays.arcanes.client.render.water.WaterMesh;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Reaper's Scythe: the Reaper itself rises over the caster, eight blocks of tattered black robe and hood, a skull in the
 * hood's shadow, its eyes burning. Its bony hands grip a great scythe: an ebony haft bound in gold, a skull at its collar,
 * a long curved blade of folded steel with runes down its flat and light along its edge. It reaps round the caster
 * twice, half a turn in front, then half a turn behind, its jaw falling open, a sheet of light trailing the blade; then
 * it sinks into smoke.
 */
public final class ScytheModel {
    private static final ResourceLocation CLOTH = ArcanesOublies.id("textures/entity/boss/m_cloth.png");
    private static final ResourceLocation BONE = ArcanesOublies.id("textures/entity/boss/m_bone.png");
    private static final ResourceLocation STEEL = ArcanesOublies.id("textures/entity/boss/m_steel.png");
    /** Ticks each half-turn takes. */
    private static final float REAP = 6;
    // The scythe in the Reaper's own space (it faces +Z): the haft's top and its collar, the blade's tip
    private static final Vector3f TOP = new Vector3f(-1.3f, 7.6f, -.9f), COLLAR = new Vector3f(.7f, 2.0f, 4.5f), TIP = new Vector3f(-6.2f, 1.5f, 5.4f);
    private static final Vector3f HEAD = new Vector3f(0, 8.55f, .1f);
    private static final int BLADE = 16;

    private ScytheModel() {}

    /** The Reaper's turn (radians, from the caster's facing round to its left) at {@code hit} ticks after the first reap. */
    private static float angle(float hit, int sweep) {
        if (hit < 0) {
            return Mth.PI * .5f + .35f * BossMesh.ramp(hit, -20, 0);
        }
        if (hit < REAP) {
            return Mth.PI * .5f + .35f - (Mth.PI + .35f) * ease(hit / REAP);
        }
        if (hit < sweep) {
            return -Mth.PI * .5f;
        }
        return -Mth.PI * .5f - Mth.PI * ease(Math.min(1, (hit - sweep) / REAP));
    }

    private static float ease(float t) {
        return t * t * (3 - 2 * t);
    }

    /** Round the caster (the pose's origin), facing {@code yaw} (degrees); {@code hit} ticks since the first reap; reaching {@code radius}. */
    public static void draw(PoseStack poseStack, MultiBufferSource buffer, WaterMesh.View view, float age, float hit, float yaw, float radius, int sweep, int zone,
                            int seed, int light) {
        float appear = BossMesh.ramp(age, 0, 16);
        float gone = BossMesh.ramp(hit, sweep + REAP + 6, sweep + REAP + 18);
        float size = (1 - (1 - appear) * (1 - appear)) * (1 - gone * gone);
        if (size < .02f) {
            return;
        }
        float face = -yaw * Mth.DEG_TO_RAD;
        float a = angle(hit, sweep);
        float reaping = Math.max(hit >= 0 && hit < REAP ? Mth.sin(hit / REAP * Mth.PI) : 0, hit >= sweep && hit < sweep + REAP ? Mth.sin((hit - sweep) / REAP * Mth.PI) : 0);
        float bob = Mth.sin(age * .12f) * .2f;
        float wind = .4f + reaping * 1.6f;
        float jaw = .1f + .4f * reaping + .05f * Mth.sin(age * .5f);
        Quaternionf turn = new Quaternionf().rotationY(face + a);
        float k = radius / 9f * size;

        // ---- cloth: the robe, its mantle, the hood, the sleeves
        VertexConsumer cloth = BossMesh.solid(buffer, CLOTH);
        place(poseStack, turn, k, bob);
        PoseStack.Pose p = poseStack.last();
        Figures.robe(cloth, p, 7.7f, 3.0f, .85f, 2.1f, 9, 1.6f, wind, seed, 0xFFFFFFFF, 0xFF0A0610, light);
        Figures.robe(cloth, p, 8.0f, 6.5f, .55f, 1.55f, 7, .4f, wind * .4f, seed + 5, 0xFFE8E0F0, 0xFF0A0610, light);
        poseStack.pushPose();
        poseStack.translate(HEAD.x, HEAD.y, HEAD.z);
        poseStack.mulPose(new Quaternionf().rotationX(.12f));
        Figures.hood(cloth, poseStack, .62f, 1.9f, 0xFFF0E8F8, light);
        poseStack.popPose();
        Vector3f gripR = grip(.3f), gripL = grip(.52f);
        sleeve(cloth, p, new Vector3f(-1.0f, 7.3f, .1f), new Vector3f(-1.5f, 6.3f, .4f), gripR, light);
        sleeve(cloth, p, new Vector3f(1.0f, 7.3f, .1f), new Vector3f(1.45f, 5.6f, 1.0f), gripL, light);
        poseStack.popPose();

        // ---- bone: the skull, the hands, the skull at the collar
        VertexConsumer bone = BossMesh.solid(buffer, BONE);
        place(poseStack, turn, k, bob);
        poseStack.pushPose();
        poseStack.translate(HEAD.x, HEAD.y - .05f, HEAD.z + .05f);
        poseStack.mulPose(new Quaternionf().rotationX(.15f));
        poseStack.scale(.95f, .95f, .95f);
        Figures.skull(bone, poseStack, jaw, light);
        poseStack.popPose();
        Vector3f along = new Vector3f(COLLAR).sub(TOP).normalize();
        for (Vector3f g : new Vector3f[] {gripR, gripL}) {
            poseStack.pushPose();
            poseStack.translate(g.x, g.y, g.z);
            poseStack.mulPose(new Quaternionf().rotationTo(new Vector3f(1, 0, 0), along).rotateX(g == gripR ? -.6f : 2.4f));
            poseStack.scale(1.5f, 1.5f, 1.5f);
            Figures.claw(bone, poseStack, .085f, light);
            poseStack.popPose();
        }
        poseStack.pushPose();
        poseStack.translate(COLLAR.x - .1f, COLLAR.y + .25f, COLLAR.z + .1f);
        poseStack.mulPose(new Quaternionf().rotationY(-.6f));
        poseStack.scale(.42f, .42f, .42f);
        Figures.skull(bone, poseStack, .2f, light);
        poseStack.popPose();
        poseStack.popPose();

        // ---- the haft (ebony, bound in gold) and the blade (steel)
        VertexConsumer iron = BossMesh.solid(buffer, BellModel.IRON);
        place(poseStack, turn, k, bob);
        p = poseStack.last();
        Vector3f[] haft = haft();
        Sculpt.sweep(iron, p, haft, Sculpt.fill(.12f, haft.length), Sculpt.fill(.12f, haft.length), Sculpt.circle(10), false, null, .6f,
                Sculpt.fill(0xFF6A5A78, haft.length), true, light);
        poseStack.popPose();
        VertexConsumer gold = BossMesh.solid(buffer, BellModel.GOLD);
        place(poseStack, turn, k, bob);
        p = poseStack.last();
        for (float t : new float[] {.04f, .16f, .42f, .66f, .9f, .97f}) {
            Vector3f at = point(haft, t);
            poseStack.pushPose();
            poseStack.translate(at.x, at.y, at.z);
            poseStack.mulPose(new Quaternionf().rotationTo(new Vector3f(0, 1, 0), along));
            Sculpt.lathe(gold, poseStack.last(), new float[] {.14f, -.09f, .17f, -.06f, .17f, .06f, .14f, .09f}, 12, 2, 1, 0xFFFFFFFF, 0xFFFFFFFF, light);
            poseStack.popPose();
        }
        // The pommel's spike, the collar's socket
        BossMesh.horn(gold, p, new Vector3f(TOP), new Vector3f(along).negate(), new Vector3f(0, .1f, 0), .9f, .13f, 6, 4, 0xFFFFFFFF, 0xFFFFF0C0, light);
        poseStack.pushPose();
        poseStack.translate(COLLAR.x, COLLAR.y, COLLAR.z);
        poseStack.mulPose(new Quaternionf().rotationTo(new Vector3f(0, 1, 0), along));
        Sculpt.lathe(gold, poseStack.last(), new float[] {.13f, -.4f, .22f, -.3f, .26f, 0, .2f, .25f, 0, .3f}, 12, 2, 1, 0xFFFFFFFF, 0xFFFFFFFF, light);
        poseStack.popPose();
        poseStack.popPose();
        VertexConsumer steel = BossMesh.solid(buffer, STEEL);
        place(poseStack, turn, k, bob);
        p = poseStack.last();
        Vector3f[] blade = blade();
        float[] width = new float[BLADE], thick = new float[BLADE];
        for (int i = 0; i < BLADE; i++) {
            float t = (float) i / (BLADE - 1);
            width[i] = .62f * (float) Math.pow(1 - t, .7f) * (.75f + .5f * Mth.sin(Mth.PI * Math.min(1, t * 1.6f))) + .02f;
            thick[i] = .09f * (1 - t) + .015f;
        }
        Sculpt.sweep(steel, p, blade, width, thick, Sculpt.WEDGE, true, new Vector3f(0, 0, -1), .35f, Sculpt.fill(0xFFFFFFFF, BLADE), true, light);
        poseStack.popPose();

        // ---- light: the eyes, the runes and the edge of the blade, the sheet it trails, the haze round it
        int g = BossMesh.glow(zone, .7f + .3f * reaping);
        VertexConsumer runes = BossMesh.light(buffer, BellModel.RUNE_BAND);
        place(poseStack, turn, k, bob);
        p = poseStack.last();
        for (int side = -1; side <= 1; side += 2) {
            for (int i = 0; i + 1 < BLADE - 3; i++) {
                Vector3f[] q0 = flat(blade, width, thick, i, side), q1 = flat(blade, width, thick, i + 1, side);
                float u0 = i * .35f, u1 = (i + 1) * .35f;
                if (side > 0) {
                    Sculpt.put(runes, p, q0[0], u0, 0, g, light, new Vector3f(0, 1, 0));
                    Sculpt.put(runes, p, q0[1], u0, 1, g, light, new Vector3f(0, 1, 0));
                    Sculpt.put(runes, p, q1[1], u1, 1, g, light, new Vector3f(0, 1, 0));
                    Sculpt.put(runes, p, q1[0], u1, 0, g, light, new Vector3f(0, 1, 0));
                } else {
                    Sculpt.put(runes, p, q1[0], u1, 0, g, light, new Vector3f(0, -1, 0));
                    Sculpt.put(runes, p, q1[1], u1, 1, g, light, new Vector3f(0, -1, 0));
                    Sculpt.put(runes, p, q0[1], u0, 1, g, light, new Vector3f(0, -1, 0));
                    Sculpt.put(runes, p, q0[0], u0, 0, g, light, new Vector3f(0, -1, 0));
                }
            }
        }
        poseStack.popPose();
        VertexConsumer beam = BossMesh.light(buffer, BossMesh.BEAM);
        place(poseStack, turn, k, bob);
        p = poseStack.last();
        Vector3f[] edge = new Vector3f[BLADE];
        Vector3f side0 = new Vector3f(0, 0, -1);
        for (int i = 0; i < BLADE; i++) {
            Vector3f tan = new Vector3f(blade[Math.min(BLADE - 1, i + 1)]).sub(blade[Math.max(0, i - 1)]).normalize();
            Vector3f s = new Vector3f(side0).sub(new Vector3f(tan).mul(side0.dot(tan))).normalize();
            edge[i] = new Vector3f(blade[i]).add(s.mul(width[i]));
        }
        Vector3f eyeLocal = local(view.eye(), turn, k, bob);
        SandMesh.ribbon(beam, p, eyeLocal, edge, Sculpt.taper(.35f + .3f * reaping, .08f, BLADE, 1), 1, 0, Sculpt.fill(g, BLADE), light);
        poseStack.popPose();
        VertexConsumer glow = BossMesh.light(buffer, BossMesh.GLOW);
        Vector3f head = world(new Vector3f(HEAD).add(0, -.05f, .5f), turn, k, bob);
        Vector3f right = turn.transform(new Vector3f(1, 0, 0)).mul(.19f * k);
        int eyes = BossMesh.glow(zone, .9f + .1f * Mth.sin(age * .9f));
        RitualMesh.billboard(glow, poseStack, view, new Vector3f(head).add(right), .5f * k, 0, -1, eyes, light);
        RitualMesh.billboard(glow, poseStack, view, new Vector3f(head).sub(right), .5f * k, 0, -1, eyes, light);
        RitualMesh.billboard(glow, poseStack, view, new Vector3f(head).add(right), 1.4f * k, 0, -1, BossMesh.glow(zone, .35f), light);
        RitualMesh.billboard(glow, poseStack, view, new Vector3f(head).sub(right), 1.4f * k, 0, -1, BossMesh.glow(zone, .35f), light);
        RitualMesh.billboard(glow, poseStack, view, world(new Vector3f(0, 5.5f, 0), turn, k, bob), 12 * k, 0, -1, BossMesh.glow(zone, .12f), light);
        // The sheet of light the blade leaves as it reaps: the ground it has just swept
        if (reaping > .01f) {
            float now = face + a;
            float back = (hit < sweep ? 1 : 1) * .9f;
            int n = 20;
            for (int i = 0; i < n; i++) {
                float t0 = (float) i / n, t1 = (float) (i + 1) / n;
                float w0 = now + back * t0 - .45f, w1 = now + back * t1 - .45f;
                int c0 = BossMesh.glow(zone, reaping * (1 - t0) * .7f), c1 = BossMesh.glow(zone, reaping * (1 - t1) * .7f);
                arc(glow, poseStack.last(), w0, w1, radius * .25f, radius * .95f, 1.6f, c0, c1, light);
            }
        }
        // Smoke where it rose and where it sinks, wisps under its hem
        VertexConsumer smoke = BossMesh.see(buffer, BossMesh.SMOKE);
        float puff = Math.max(1 - appear, gone);
        for (int i = 0; i < 10; i++) {
            float an = i * Mth.TWO_PI / 10 + age * .02f;
            float r = 1.6f + BossMesh.hash(seed + i) * 1.2f;
            float y = 2.4f + (age * .04f + BossMesh.hash(seed + i * 5) * 3) % 3;
            RitualMesh.billboard(smoke, poseStack, view, new Vector3f(Mth.cos(an) * r, y, Mth.sin(an) * r), 2.2f + puff * 4, an, -1,
                    BossMesh.argb(0x1A1024, (.35f + puff * .5f) * Math.max(size, puff)), light);
        }
    }

    private static void place(PoseStack poseStack, Quaternionf turn, float k, float bob) {
        poseStack.pushPose();
        poseStack.mulPose(turn);
        poseStack.translate(0, bob, 0);
        poseStack.scale(k, k, k);
    }

    private static Vector3f world(Vector3f local, Quaternionf turn, float k, float bob) {
        return turn.transform(new Vector3f(local).mul(k).add(0, bob, 0));
    }

    private static Vector3f local(Vector3f world, Quaternionf turn, float k, float bob) {
        return new Quaternionf(turn).conjugate().transform(new Vector3f(world)).sub(0, bob, 0).div(Math.max(k, 1.0E-3f));
    }

    private static Vector3f[] haft() {
        Vector3f mid = new Vector3f(TOP).lerp(COLLAR, .5f).add(.35f, .3f, 0);
        return Sculpt.curve(TOP, mid, COLLAR, 12);
    }

    private static Vector3f point(Vector3f[] pts, float t) {
        float f = t * (pts.length - 1);
        int i = Math.min(pts.length - 2, (int) f);
        return new Vector3f(pts[i]).lerp(pts[i + 1], f - i);
    }

    private static Vector3f grip(float t) {
        return point(haft(), t);
    }

    private static Vector3f[] blade() {
        Vector3f c1 = new Vector3f(COLLAR).add(-2.4f, .25f, .9f), c2 = new Vector3f(TIP).add(2.2f, .05f, 1.3f);
        return Sculpt.bezier(new Vector3f(COLLAR).add(-.1f, 0, 0), c1, c2, TIP, BLADE);
    }

    /** The middle of the blade's flat on one side (+1 top, -1 underneath) at point {@code i}: two points across it. */
    private static Vector3f[] flat(Vector3f[] blade, float[] width, float[] thick, int i, int side) {
        Vector3f tan = new Vector3f(blade[Math.min(blade.length - 1, i + 1)]).sub(blade[Math.max(0, i - 1)]).normalize();
        Vector3f s = new Vector3f(0, 0, -1);
        s.sub(new Vector3f(tan).mul(s.dot(tan))).normalize();
        Vector3f up = new Vector3f(tan).cross(s).normalize().mul(side * thick[i] * .62f);
        Vector3f a = new Vector3f(blade[i]).add(new Vector3f(s).mul(-width[i] * .45f)).add(up);
        Vector3f b = new Vector3f(blade[i]).add(new Vector3f(s).mul(width[i] * .25f)).add(up);
        return new Vector3f[] {a, b};
    }

    /** A sleeve from the shoulder by the elbow to the wrist (just short of the grip), flaring at its cuff. */
    private static void sleeve(VertexConsumer c, PoseStack.Pose p, Vector3f shoulder, Vector3f elbow, Vector3f grip, int light) {
        Vector3f wrist = new Vector3f(grip).lerp(elbow, .22f);
        Vector3f[] pts = Sculpt.curve(shoulder, elbow, wrist, 8);
        float[] r = {.42f, .38f, .34f, .32f, .32f, .36f, .46f, .56f};
        Sculpt.sweep(c, p, pts, r, r, Sculpt.circle(10), false, null, .5f, Sculpt.fade(0xFFFFFFFF, 0xFF6A5A78, 8), false, light);
    }

    /** A flat band of light lying {@code y} over the ground between the angles {@code w0} and {@code w1} (as the Reaper turns), from {@code r0} to {@code r1}. */
    private static void arc(VertexConsumer c, PoseStack.Pose p, float w0, float w1, float r0, float r1, float y, int c0, int c1, int light) {
        Vector3f i0 = new Vector3f(Mth.sin(w0) * r0, y, Mth.cos(w0) * r0), o0 = new Vector3f(Mth.sin(w0) * r1, y, Mth.cos(w0) * r1);
        Vector3f i1 = new Vector3f(Mth.sin(w1) * r0, y, Mth.cos(w1) * r0), o1 = new Vector3f(Mth.sin(w1) * r1, y, Mth.cos(w1) * r1);
        BossMesh.quad2(c, p, i0, o0, o1, i1, 0, .5f, 1, .5f, 1, .5f, 0, .5f, c0, c1, light);
    }
}
