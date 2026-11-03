package com.waylays.arcanes.client.render.boss;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.waylays.arcanes.ArcanesOublies;
import com.waylays.arcanes.client.render.ritual.RitualMesh;
import com.waylays.arcanes.client.render.water.WaterMesh;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Bell of Doom: a great bronze bell, green with age, lowered out of a rune circle in the sky on two chains from a beam of
 * black iron. A crown of gold spikes rings its shoulder, a band of runes burns round its waist, its lip is gilded; six
 * loops crown it, a clapper of iron hangs in its dark mouth. It swings and tolls three times: at each toll the runes
 * flare, a ring of sound bursts from its lip and races down and out over the ground. Then it is drawn back up.
 */
public final class BellModel {
    public static final ResourceLocation BRONZE = ArcanesOublies.id("textures/entity/boss/m_bronze.png");
    public static final ResourceLocation IRON = ArcanesOublies.id("textures/entity/boss/m_iron_plate.png");
    public static final ResourceLocation GOLD = ArcanesOublies.id("textures/entity/boss/m_gold_chased.png");
    public static final ResourceLocation RUNE_BAND = ArcanesOublies.id("textures/entity/boss/m_rune_band.png");

    /** The bell's outside, lip to crown, then its inside, crown to lip ({r, y}); one unit is SCALE blocks. */
    private static final float[] OUTSIDE = {2.02f, -3.0f, 2.16f, -2.96f, 2.2f, -2.86f, 2.14f, -2.72f, 2.0f, -2.52f, 1.78f, -2.3f, 1.55f, -2.0f, 1.38f, -1.62f,
            1.27f, -1.2f, 1.2f, -.8f, 1.17f, -.5f, 1.12f, -.28f, 1.0f, -.12f, .78f, -.03f, .45f, .01f, 0, .02f};
    private static final float[] INSIDE = {0, -.24f, .5f, -.28f, .86f, -.42f, 1.02f, -.7f, 1.08f, -1.1f, 1.18f, -1.6f, 1.38f, -2.05f, 1.62f, -2.42f, 1.84f, -2.74f,
            1.96f, -2.94f, 2.02f, -3.0f};
    private static final float SCALE = 1.6f;
    /** The waist's band of runes: from and to (heights) and how far round its texture wraps. */
    private static final float BAND_LOW = -1.75f, BAND_HIGH = -1.05f;
    private static final int CROWN_SPIKES = 16;

    private BellModel() {}

    /** The bell's radius at height {@code y} on its outside (between profile points). */
    private static float radiusAt(float y) {
        for (int i = 0; i + 3 < OUTSIDE.length; i += 2) {
            float y0 = OUTSIDE[i + 1], y1 = OUTSIDE[i + 3];
            if (y >= y0 && y <= y1) {
                return Mth.lerp((y - y0) / Math.max(y1 - y0, 1.0E-4f), OUTSIDE[i], OUTSIDE[i + 2]);
            }
        }
        return 1;
    }

    /**
     * Under it (the pose's origin), the bell hanging at {@code bell}; {@code hit} ticks since its first toll (negative
     * before), tolling every {@code every} ticks, three times; its rings reach {@code radius}.
     */
    public static void draw(PoseStack poseStack, MultiBufferSource buffer, WaterMesh.View view, Vector3f bell, float age, float hit, int every, float radius,
                            int zone, int seed, int light) {
        float end = every * 2 + 12;
        // Lowered out of the sky, then drawn back up
        float down = BossMesh.ramp(age, 0, 18);
        float up = BossMesh.ramp(hit, end, end + 16);
        float lift = 14 * (1 - down) * (1 - down) + 16 * up * up;
        if (up >= 1) {
            return;
        }
        // Its swing: a kick at each toll, rocking on dying away; the clapper lags behind
        float swing = 0, clap = 0, flare = 0;
        for (int k = 0; k < 3; k++) {
            float s = hit - k * every + 4;
            if (s >= 0) {
                swing += .2f * (float) Math.exp(-s / 9) * Mth.sin(s * .42f);
                clap += .34f * (float) Math.exp(-s / 7) * Mth.sin(s * .42f - 1.1f);
            }
            float t = hit - k * every;
            if (t >= 0) {
                flare = Math.max(flare, (float) Math.exp(-t / 5));
            }
        }
        // Before the first toll it trembles, as if something stirred in it
        if (hit < 0) {
            float shake = BossMesh.ramp(hit, -30, 0);
            swing += Mth.sin(age * 2.3f) * .02f * shake;
        }
        float glow = (.45f + .2f * Mth.sin(age * .25f)) * down * (1 - up) + flare * .8f;
        Vector3f top = new Vector3f(bell).add(0, lift, 0);
        float yokeY = .9f * SCALE;

        // ---- bronze: the bell, its loops, its dark mouth
        VertexConsumer bronze = BossMesh.solid(buffer, BRONZE);
        poseStack.pushPose();
        hang(poseStack, top, yokeY, swing);
        PoseStack.Pose p = poseStack.last();
        Sculpt.lathe(bronze, p, OUTSIDE, 40, 4, .5f, 0xFFFFFFFF, 0xFFE8E0D8, light);
        Sculpt.lathe(bronze, p, INSIDE, 40, 4, .5f, 0xFF2A1E16, 0xFF5A4232, light);
        // The loops that crown it: six round a seventh
        for (int k = 0; k < 7; k++) {
            poseStack.pushPose();
            if (k < 6) {
                float a = k * Mth.TWO_PI / 6;
                poseStack.translate(Mth.cos(a) * .32f, .2f, Mth.sin(a) * .32f);
                poseStack.mulPose(new Quaternionf().rotationY(-a).rotateX(Mth.HALF_PI));
            } else {
                poseStack.translate(0, .32f, 0);
                poseStack.mulPose(new Quaternionf().rotationZ(Mth.HALF_PI));
            }
            poseStack.scale(1, 1, k < 6 ? .8f : 1.1f);
            Sculpt.torus(bronze, poseStack.last(), k < 6 ? .2f : .26f, .065f, 14, 6, 0, Mth.TWO_PI, 1, 0xFFD8CCC0, light);
            poseStack.popPose();
        }
        poseStack.popPose();

        // ---- gold: the crown of spikes, the beads, the lip, the beam's caps
        VertexConsumer gold = BossMesh.solid(buffer, GOLD);
        poseStack.pushPose();
        hang(poseStack, top, yokeY, swing);
        p = poseStack.last();
        bead(gold, poseStack, -.4f, .07f);
        bead(gold, poseStack, BAND_HIGH + .08f, .045f);
        bead(gold, poseStack, BAND_LOW - .08f, .045f);
        bead(gold, poseStack, -2.58f, .06f);
        // The gilded lip
        Sculpt.lathe(gold, p, new float[] {2.0f, -3.03f, 2.18f, -2.99f, 2.23f, -2.88f}, 40, 6, 1, 0xFFFFFFFF, 0xFFFFFFFF, light);
        // The crown: spikes standing round the shoulder, leaning out
        for (int k = 0; k < CROWN_SPIKES; k++) {
            float a = (k + .5f) * Mth.TWO_PI / CROWN_SPIKES;
            float r = radiusAt(-.36f);
            boolean tall = k % 2 == 0;
            Vector3f base = new Vector3f(Mth.cos(a) * r, -.36f, Mth.sin(a) * r);
            Vector3f out = new Vector3f(Mth.cos(a), 0, Mth.sin(a));
            Vector3f dir = new Vector3f(out).mul(.35f).add(0, 1, 0).normalize();
            BossMesh.horn(gold, p, base, dir, new Vector3f(out).mul(.25f), tall ? .62f : .36f, tall ? .1f : .07f, 5, 3, 0xFFFFE6A0, 0xFFFFFFFF, light);
        }
        // Little gilded bosses round the sound bow
        for (int k = 0; k < 12; k++) {
            float a = k * Mth.TWO_PI / 12;
            float r = radiusAt(-2.4f) + .04f;
            poseStack.pushPose();
            poseStack.translate(Mth.cos(a) * r, -2.4f, Mth.sin(a) * r);
            poseStack.mulPose(new Quaternionf().rotationY(-a).rotateZ(-Mth.HALF_PI + .5f));
            Sculpt.gem(gold, poseStack.last(), .09f, .05f, .02f, 6, 0xFFFFFFFF, light);
            poseStack.popPose();
        }
        poseStack.popPose();
        // The beam's gilded caps (on the beam, which does not swing)
        poseStack.pushPose();
        poseStack.translate(top.x, top.y + yokeY, top.z);
        for (int side = -1; side <= 1; side += 2) {
            poseStack.pushPose();
            poseStack.translate(side * 3.05f, 0, 0);
            poseStack.mulPose(new Quaternionf().rotationZ(-side * Mth.HALF_PI));
            Sculpt.lathe(gold, poseStack.last(), new float[] {.38f, -.02f, .42f, .08f, .3f, .22f, .16f, .34f, .1f, .52f, 0, .56f}, 16, 2, 1, 0xFFFFFFFF, 0xFFFFFFFF,
                    light);
            poseStack.popPose();
        }
        poseStack.popPose();

        // ---- iron: the beam, its straps, the chains up to the sky, the clapper
        VertexConsumer iron = BossMesh.solid(buffer, IRON);
        poseStack.pushPose();
        poseStack.translate(top.x, top.y + yokeY, top.z);
        Sculpt.block(iron, poseStack.last(), 6.1f, .62f, .7f, .07f, .5f, 0xFFFFFFFF, light);
        // Rivets along it
        for (int k = -5; k <= 5; k++) {
            for (int s = -1; s <= 1; s += 2) {
                poseStack.pushPose();
                poseStack.translate(k * .52f, 0, s * .36f);
                poseStack.mulPose(new Quaternionf().rotationX(s * Mth.HALF_PI));
                Sculpt.lathe(iron, poseStack.last(), new float[] {.07f, -.01f, .06f, .03f, 0, .045f}, 8, 1, 1, 0xFFC0B8C8, 0xFFC0B8C8, light);
                poseStack.popPose();
            }
        }
        // The chains, from each end of the beam up into the circle
        for (int side = -1; side <= 1; side += 2) {
            chain(iron, poseStack, new Vector3f(side * 2.7f, .3f, 0), new Vector3f(side * 3.6f, 15, 0), .26f, seed + side);
        }
        poseStack.popPose();
        poseStack.pushPose();
        hang(poseStack, top, yokeY, swing);
        // The straps over the crown, up to the beam
        for (int side = -1; side <= 1; side += 2) {
            Vector3f[] strap = Sculpt.curve(new Vector3f(side * .55f, .05f, 0), new Vector3f(side * .7f, .5f, 0), new Vector3f(side * .5f, yokeY / SCALE - .15f, 0), 6);
            Sculpt.sweep(iron, poseStack.last(), strap, Sculpt.fill(.2f, 6), Sculpt.fill(.06f, 6), Sculpt.RECT, true, new Vector3f(0, 0, 1), .5f,
                    Sculpt.fill(0xFFFFFFFF, 6), true, light);
        }
        // The clapper: a rod and a heavy ball, swinging behind the bell
        poseStack.translate(0, -.3f, 0);
        poseStack.mulPose(new Quaternionf().rotationX(clap));
        Vector3f[] rod = {new Vector3f(0, 0, 0), new Vector3f(0, -1.1f, 0), new Vector3f(0, -2.0f, 0)};
        Sculpt.sweep(iron, poseStack.last(), rod, Sculpt.fill(.07f, 3), Sculpt.fill(.07f, 3), Sculpt.circle(8), false, null, 1, Sculpt.fill(0xFFFFFFFF, 3), true,
                light);
        poseStack.translate(0, -2.3f, 0);
        Sculpt.lathe(iron, poseStack.last(), sphere(.32f, 10), 12, 1, 1, 0xFFFFFFFF, 0xFFFFFFFF, light);
        poseStack.popPose();

        // ---- light: the band of runes, the toll's flash, the circle overhead, the rings of sound
        VertexConsumer runes = BossMesh.light(buffer, RUNE_BAND);
        poseStack.pushPose();
        hang(poseStack, top, yokeY, swing);
        float[] band = new float[] {radiusAt(BAND_LOW) * 1.012f, BAND_LOW, radiusAt(BAND_HIGH) * 1.012f, BAND_HIGH};
        Sculpt.lathe(runes, poseStack.last(), band, 40, 4, 1 / bandLength(band), BossMesh.glow(zone, glow), BossMesh.glow(zone, glow), light);
        poseStack.popPose();
        VertexConsumer circle = BossMesh.light(buffer, BossMesh.RUNES);
        float sky = top.y + yokeY + 14;
        BossMesh.annulus(circle, poseStack.last(), new Vector3f(top.x, sky, top.z), new Vector3f(1, 0, 0), new Vector3f(0, 0, -1), 2.5f, 7, 48, 3, age * .015f, 0,
                BossMesh.glow(zone, .8f * (1 - up)), BossMesh.glow(zone, .35f * (1 - up)), light, true);
        // Rings of sound: each toll a ring bursting from the lip, falling to the ground and racing out over it
        VertexConsumer ring = BossMesh.light(buffer, BossMesh.RING);
        float lipY = top.y - 3.0f * SCALE;
        for (int k = 0; k < 3; k++) {
            float s = hit - k * every;
            if (s < 0 || s > 18) {
                continue;
            }
            float t = s / 18, f = 1 - t;
            for (int w = 0; w < 3; w++) {
                float tw = Math.max(0, t - w * .08f);
                float y = Mth.lerp(Math.min(1, tw * 3), lipY, .15f + w * .05f);
                float r = Mth.lerp(Math.min(1, tw * 3), 2.2f * SCALE, 3.6f * SCALE) + radius * Math.max(0, tw * 1.25f - .25f);
                BossMesh.pool(ring, poseStack.last(), top.x, y, top.z, r * 2.15f, k + w, BossMesh.glow(w == 0 ? 0xFFFFFF : zone, f * (1 - w * .3f)), light);
            }
        }
        VertexConsumer halo = BossMesh.light(buffer, BossMesh.GLOW);
        RitualMesh.billboard(halo, poseStack, view, new Vector3f(top).add(0, -1.6f * SCALE, 0), 8 + 10 * flare, 0, -1, BossMesh.glow(zone, (.18f + .55f * flare) * (1 - up)),
                light);
    }

    /** To the bell's own space: hung from the beam {@code yokeY} over {@code top}, swung {@code swing} about it. */
    private static void hang(PoseStack poseStack, Vector3f top, float yokeY, float swing) {
        poseStack.translate(top.x, top.y + yokeY, top.z);
        poseStack.mulPose(new Quaternionf().rotationX(swing));
        poseStack.translate(0, -yokeY, 0);
        poseStack.scale(SCALE, SCALE, SCALE);
    }

    /** A gilded bead round the bell at height {@code y}. */
    private static void bead(VertexConsumer c, PoseStack poseStack, float y, float r) {
        poseStack.pushPose();
        poseStack.translate(0, y, 0);
        Sculpt.torus(c, poseStack.last(), radiusAt(y) + r * .3f, r, 48, 6, 0, Mth.TWO_PI, 10, 0xFFFFFFFF, 0xF000F0);
        poseStack.popPose();
    }

    private static float bandLength(float[] band) {
        return Mth.sqrt((band[2] - band[0]) * (band[2] - band[0]) + (band[3] - band[1]) * (band[3] - band[1]));
    }

    /** A ball's profile for the lathe, bottom to top. */
    static float[] sphere(float r, int n) {
        float[] s = new float[2 * (n + 1)];
        for (int i = 0; i <= n; i++) {
            float a = -Mth.HALF_PI + i * Mth.PI / n;
            s[2 * i] = Math.max(0, Mth.cos(a) * r);
            s[2 * i + 1] = Mth.sin(a) * r;
        }
        return s;
    }

    /** A chain of oval links from {@code a} to {@code b}, each {@code size} long, every other one turned a quarter. */
    static void chain(VertexConsumer c, PoseStack poseStack, Vector3f a, Vector3f b, float size, int seed) {
        Vector3f d = new Vector3f(b).sub(a);
        float len = d.length();
        int n = Math.max(1, (int) (len / (size * 1.55f)));
        d.normalize();
        Quaternionf along = new Quaternionf().rotationTo(new Vector3f(0, 1, 0), d);
        for (int i = 0; i < n; i++) {
            Vector3f at = new Vector3f(a).add(new Vector3f(d).mul((i + .5f) * len / n));
            poseStack.pushPose();
            poseStack.translate(at.x, at.y, at.z);
            poseStack.mulPose(along);
            poseStack.mulPose(new Quaternionf().rotationY(i % 2 == 0 ? 0 : Mth.HALF_PI).rotateX(Mth.HALF_PI));
            poseStack.scale(1, 1, 1.7f);
            Sculpt.torus(c, poseStack.last(), size * .5f, size * .14f, 12, 6, 0, Mth.TWO_PI, 1, 0xFFFFFFFF, 0xF000F0);
            poseStack.popPose();
        }
    }
}
