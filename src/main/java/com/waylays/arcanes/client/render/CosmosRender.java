package com.waylays.arcanes.client.render;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.waylays.arcanes.ArcanesOublies;
import com.waylays.arcanes.spells.cosmos.Cosmos;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Vector3f;

/**
 * Drawing shared by the Cosmos renderers and the sky of the Pocket Universe.
 * <ul>
 * <li>Planets and moons: textured spheres lit by a sun of their own (a day side and a night side with a soft
 * terminator), wrapped in a glow of atmosphere; rings of dusty ice; a black hole's accretion disk.</li>
 * <li>Starlight: twinkling four-pointed stars, starfields and nebulae on the inside of a sphere.</li>
 * <li>Time: the TVA's clock (a face, glowing hands), and its time door, a glowing orange frame on a haze of rising
 * light.</li>
 * <li>Echoes: any creature drawn again as a glitching silhouette of scanlines in time's orange, its exact shape,
 * whatever it is (a past self, a frozen moment, a variant).</li>
 * </ul>
 */
public final class CosmosRender {
    private static final String DIR = "textures/entity/cosmos/";
    public static final ResourceLocation STARS = ArcanesOublies.id(DIR + "stars.png");
    public static final ResourceLocation NEBULA = ArcanesOublies.id(DIR + "nebula.png");
    public static final ResourceLocation GALAXY = ArcanesOublies.id(DIR + "galaxy.png");
    public static final ResourceLocation PLANET_GAS = ArcanesOublies.id(DIR + "planet_gas.png");
    public static final ResourceLocation PLANET_OCEAN = ArcanesOublies.id(DIR + "planet_ocean.png");
    public static final ResourceLocation PLANET_ICE = ArcanesOublies.id(DIR + "planet_ice.png");
    public static final ResourceLocation PLANET_EMBER = ArcanesOublies.id(DIR + "planet_ember.png");
    public static final ResourceLocation MOON = ArcanesOublies.id(DIR + "moon.png");
    public static final ResourceLocation RING = ArcanesOublies.id(DIR + "ring.png");
    public static final ResourceLocation ACCRETION = ArcanesOublies.id(DIR + "accretion.png");
    public static final ResourceLocation RIM = ArcanesOublies.id(DIR + "rim.png");
    public static final ResourceLocation VORTEX = ArcanesOublies.id(DIR + "vortex.png");
    public static final ResourceLocation HAZE = ArcanesOublies.id(DIR + "haze.png");
    public static final ResourceLocation SCANLINES = ArcanesOublies.id(DIR + "scanlines.png");
    public static final ResourceLocation CLOUD = ArcanesOublies.id(DIR + "cloud.png");
    public static final ResourceLocation CLOCK = ArcanesOublies.id(DIR + "clock.png");
    public static final ResourceLocation[] PLANETS = {PLANET_GAS, PLANET_OCEAN, PLANET_ICE, PLANET_EMBER};

    /** The unit sphere: {@link #LAT} bands of {@link #LON} quads, each corner (x, y, z, u, v). */
    private static final int LAT = 16;
    private static final int LON = 32;
    private static final float[][][] SPHERE = new float[LAT + 1][LON + 1][];
    private static final Vector3f UP = new Vector3f(0, 1, 0);
    /** Nesting depth of echoes: an echo never draws another echo. */
    private static int echoDepth;

    static {
        for (int i = 0; i <= LAT; i++) {
            float v = i / (float) LAT;
            float lat = Mth.HALF_PI - v * Mth.PI;
            for (int j = 0; j <= LON; j++) {
                float u = j / (float) LON;
                float lon = u * Mth.TWO_PI;
                SPHERE[i][j] = new float[]{Mth.cos(lat) * Mth.cos(lon), Mth.sin(lat), Mth.cos(lat) * Mth.sin(lon), u, v};
            }
        }
    }

    private CosmosRender() {}

    // ------------------------------------------------------------------ planets

    /**
     * A planet of {@code texture}, {@code radius} across, centred on the origin, turned {@code spin} (0 to 1, a whole
     * turn) round its axis. It is lit from {@code sun} (a direction in the pose's frame, null for even light): the
     * side away from it falls to {@code night} (0 black, 1 as bright as day), with a soft terminator.
     */
    public static void planet(PoseStack poseStack, MultiBufferSource buffer, ResourceLocation texture, float radius, float spin,
                              Vector3f sun, float night, int tint) {
        VertexConsumer consumer = buffer.getBuffer(RenderType.entityCutoutNoCull(texture));
        PoseStack.Pose pose = poseStack.last();
        int r0 = (tint >> 16) & 0xFF, g0 = (tint >> 8) & 0xFF, b0 = tint & 0xFF;
        Vector3f s = sun == null ? null : new Vector3f(sun).normalize();
        for (int i = 0; i < LAT; i++) {
            for (int j = 0; j < LON; j++) {
                sphereVertex(consumer, pose, SPHERE[i][j], radius, spin, s, night, r0, g0, b0);
                sphereVertex(consumer, pose, SPHERE[i + 1][j], radius, spin, s, night, r0, g0, b0);
                sphereVertex(consumer, pose, SPHERE[i + 1][j + 1], radius, spin, s, night, r0, g0, b0);
                sphereVertex(consumer, pose, SPHERE[i][j + 1], radius, spin, s, night, r0, g0, b0);
            }
        }
    }

    private static void sphereVertex(VertexConsumer consumer, PoseStack.Pose pose, float[] p, float radius, float spin, Vector3f sun,
                                     float night, int r0, int g0, int b0) {
        float light = 1;
        if (sun != null) {
            float d = p[0] * sun.x + p[1] * sun.y + p[2] * sun.z;
            light = night + (1 - night) * Mth.clamp(d * 2.2f + .35f, 0, 1);
        }
        int argb = 0xFF000000 | (int) (r0 * light) << 16 | (int) (g0 * light) << 8 | (int) (b0 * light);
        RenderHelper.vertex(consumer, pose, p[0] * radius, p[1] * radius, p[2] * radius, p[3] + spin, p[4], argb, GlyphRender.FULL, UP.x, UP.y, UP.z);
    }

    /** A sphere of pure black (a black hole's horizon): it hides what is behind it and gives back no light. */
    public static void blackSphere(PoseStack poseStack, MultiBufferSource buffer, float radius) {
        planet(poseStack, buffer, MOON, radius, 0, null, 0, 0x000000);
    }

    /** The glow of an atmosphere round a planet {@code radius} across, seen edge-on from wherever the camera is. */
    public static void atmosphere(PoseStack poseStack, MultiBufferSource buffer, float radius, int rgb, float strength) {
        if (strength <= .01f) {
            return;
        }
        poseStack.pushPose();
        poseStack.mulPose(Minecraft.getInstance().getEntityRenderDispatcher().cameraOrientation());
        GlyphRender.halo(buffer, poseStack.last(), RIM, radius * 1.36f, RuneRender.additive(rgb, strength));
        poseStack.popPose();
    }

    /**
     * A flat ring round the origin in the XZ plane, from {@code inner} to {@code outer}: the dusty ice of a planet's
     * rings, seen from both sides.
     */
    public static void ring(PoseStack poseStack, MultiBufferSource buffer, float inner, float outer, int tint, float alpha) {
        VertexConsumer consumer = buffer.getBuffer(RenderType.entityTranslucent(RING));
        PoseStack.Pose pose = poseStack.last();
        int argb = RuneRender.alpha(tint, alpha);
        int n = 48;
        for (int k = 0; k < n; k++) {
            float a0 = k * Mth.TWO_PI / n, a1 = (k + 1) * Mth.TWO_PI / n;
            float c0 = Mth.cos(a0), s0 = Mth.sin(a0), c1 = Mth.cos(a1), s1 = Mth.sin(a1);
            float v0 = k / (float) n, v1 = (k + 1) / (float) n;
            annulusQuad(consumer, pose, inner, outer, c0, s0, c1, s1, 0, 1, v0, v1, argb);
        }
    }

    /**
     * A black hole's accretion disk round the origin in the XZ plane, from {@code inner} to {@code outer}, light added
     * to the scene: white-hot inside, red outside, its streaks turning by {@code spin} (in turns).
     */
    public static void disk(PoseStack poseStack, MultiBufferSource buffer, float inner, float outer, float spin, int rgb, float strength) {
        VertexConsumer consumer = buffer.getBuffer(RenderType.eyes(ACCRETION));
        PoseStack.Pose pose = poseStack.last();
        int argb = RuneRender.additive(rgb, strength);
        int n = 64;
        for (int k = 0; k < n; k++) {
            float a0 = k * Mth.TWO_PI / n, a1 = (k + 1) * Mth.TWO_PI / n;
            float u0 = k * 2f / n + spin, u1 = (k + 1) * 2f / n + spin;
            annulusQuadUv(consumer, pose, inner, outer, Mth.cos(a0), Mth.sin(a0), Mth.cos(a1), Mth.sin(a1), u0, u1, argb);
        }
    }

    private static void annulusQuad(VertexConsumer c, PoseStack.Pose pose, float inner, float outer, float c0, float s0, float c1, float s1,
                                    float u0, float u1, float v0, float v1, int argb) {
        RenderHelper.vertex(c, pose, c0 * inner, 0, s0 * inner, u0, v0, argb, GlyphRender.FULL, 0, 1, 0);
        RenderHelper.vertex(c, pose, c0 * outer, 0, s0 * outer, u1, v0, argb, GlyphRender.FULL, 0, 1, 0);
        RenderHelper.vertex(c, pose, c1 * outer, 0, s1 * outer, u1, v1, argb, GlyphRender.FULL, 0, 1, 0);
        RenderHelper.vertex(c, pose, c1 * inner, 0, s1 * inner, u0, v1, argb, GlyphRender.FULL, 0, 1, 0);
    }

    /** A strip of an annulus with u round it and v across it (the accretion disk's layout), seen from both sides. */
    private static void annulusQuadUv(VertexConsumer c, PoseStack.Pose pose, float inner, float outer, float c0, float s0, float c1, float s1,
                                      float u0, float u1, int argb) {
        RenderHelper.vertex(c, pose, c0 * inner, 0, s0 * inner, u0, 0, argb, GlyphRender.FULL, 0, 1, 0);
        RenderHelper.vertex(c, pose, c1 * inner, 0, s1 * inner, u1, 0, argb, GlyphRender.FULL, 0, 1, 0);
        RenderHelper.vertex(c, pose, c1 * outer, 0, s1 * outer, u1, 1, argb, GlyphRender.FULL, 0, 1, 0);
        RenderHelper.vertex(c, pose, c0 * outer, 0, s0 * outer, u0, 1, argb, GlyphRender.FULL, 0, 1, 0);
        RenderHelper.vertex(c, pose, c0 * outer, 0, s0 * outer, u0, 1, argb, GlyphRender.FULL, 0, -1, 0);
        RenderHelper.vertex(c, pose, c1 * outer, 0, s1 * outer, u1, 1, argb, GlyphRender.FULL, 0, -1, 0);
        RenderHelper.vertex(c, pose, c1 * inner, 0, s1 * inner, u1, 0, argb, GlyphRender.FULL, 0, -1, 0);
        RenderHelper.vertex(c, pose, c0 * inner, 0, s0 * inner, u0, 0, argb, GlyphRender.FULL, 0, -1, 0);
    }

    /**
     * A ring of light facing the camera round the origin, from {@code inner} to {@code outer}, of the accretion
     * texture: the far side of a black hole's disk, bent over the top and under the bottom of the hole by its pull.
     */
    public static void lensedDisk(PoseStack poseStack, MultiBufferSource buffer, float inner, float outer, float spin, int rgb, float strength) {
        poseStack.pushPose();
        poseStack.mulPose(Minecraft.getInstance().getEntityRenderDispatcher().cameraOrientation());
        poseStack.mulPose(Axis.XP.rotationDegrees(90));
        disk(poseStack, buffer, inner, outer, spin, rgb, strength);
        poseStack.popPose();
    }

    // ------------------------------------------------------------------ starlight

    /** A four-pointed star facing the camera at the origin, {@code size} across, turned {@code spin} degrees. */
    public static void star(PoseStack poseStack, MultiBufferSource buffer, float size, int rgb, float strength, float spin) {
        if (strength <= .01f) {
            return;
        }
        poseStack.pushPose();
        poseStack.mulPose(Minecraft.getInstance().getEntityRenderDispatcher().cameraOrientation());
        poseStack.mulPose(Axis.ZP.rotationDegrees(spin));
        RenderHelper.billboard(buffer.getBuffer(RenderType.eyes(GemRender.SPARKLE)), poseStack.last(), size * .5f, RuneRender.additive(rgb, strength), GlyphRender.FULL);
        GlyphRender.halo(buffer, poseStack.last(), GlyphRender.GLOW, size * .32f, RuneRender.additive(rgb, strength * .6f));
        poseStack.popPose();
    }

    /** A star that twinkles on its own (by {@code seed}) as {@code time} goes. */
    public static void twinkle(PoseStack poseStack, MultiBufferSource buffer, float size, int rgb, float strength, float time, int seed) {
        float h = RuneRender.hash(seed);
        float tw = .65f + .35f * Mth.sin(time * (.15f + .2f * h) + h * 40);
        star(poseStack, buffer, size * (.8f + .3f * tw), rgb, strength * tw, time * (1 + h) + h * 90);
    }

    /**
     * The inside of a sphere {@code radius} across round the origin painted with {@code texture} (light added, tiled
     * {@code tiles} times round), for skies and the depths seen through doors: a starfield, a nebula.
     */
    public static void skySphere(PoseStack poseStack, MultiBufferSource buffer, ResourceLocation texture, float radius, int tiles, int rgb, float strength) {
        VertexConsumer consumer = buffer.getBuffer(RenderType.eyes(texture));
        PoseStack.Pose pose = poseStack.last();
        int argb = RuneRender.additive(rgb, strength);
        for (int i = 0; i < LAT; i++) {
            for (int j = 0; j < LON; j++) {
                float[] a = SPHERE[i][j], b = SPHERE[i + 1][j], c = SPHERE[i + 1][j + 1], d = SPHERE[i][j + 1];
                // Wound to face the inside, where the camera is
                for (float[] p : new float[][]{a, b, c, d}) {
                    RenderHelper.vertex(consumer, pose, p[0] * radius, p[1] * radius, p[2] * radius, p[3] * tiles, p[4] * tiles * .5f, argb, GlyphRender.FULL, 0, 1, 0);
                }
            }
        }
    }

    /**
     * A dome over the origin {@code radius} across painted with {@code texture} (light added, tiled {@code uTiles} times
     * round and {@code vTiles} up, sliding by {@code drift}), seen from outside and from within: a field's bubble.
     */
    public static void dome(PoseStack poseStack, MultiBufferSource buffer, ResourceLocation texture, float radius, float uTiles, float vTiles,
                            float drift, int argb) {
        VertexConsumer consumer = buffer.getBuffer(RenderType.eyes(texture));
        PoseStack.Pose pose = poseStack.last();
        int lat = 10, lon = 32;
        for (int i = 0; i < lat; i++) {
            float v0 = i / (float) lat, v1 = (i + 1) / (float) lat;
            for (int j = 0; j < lon; j++) {
                float u0 = j / (float) lon, u1 = (j + 1) / (float) lon;
                float[] a = domePoint(v0, u0, radius), b = domePoint(v1, u0, radius), c = domePoint(v1, u1, radius), d = domePoint(v0, u1, radius);
                float tu0 = u0 * uTiles + drift, tu1 = u1 * uTiles + drift, tv0 = v0 * vTiles - drift, tv1 = v1 * vTiles - drift;
                // Outside, then inside
                domeVertex(consumer, pose, a, tu0, tv0, argb);
                domeVertex(consumer, pose, d, tu1, tv0, argb);
                domeVertex(consumer, pose, c, tu1, tv1, argb);
                domeVertex(consumer, pose, b, tu0, tv1, argb);
                domeVertex(consumer, pose, a, tu0, tv0, argb);
                domeVertex(consumer, pose, b, tu0, tv1, argb);
                domeVertex(consumer, pose, c, tu1, tv1, argb);
                domeVertex(consumer, pose, d, tu1, tv0, argb);
            }
        }
    }

    private static float[] domePoint(float v, float u, float r) {
        float lat = v * Mth.HALF_PI, lon = u * Mth.TWO_PI;
        return new float[]{Mth.cos(lat) * Mth.cos(lon) * r, Mth.sin(lat) * r, Mth.cos(lat) * Mth.sin(lon) * r};
    }

    private static void domeVertex(VertexConsumer consumer, PoseStack.Pose pose, float[] p, float u, float v, int argb) {
        RenderHelper.vertex(consumer, pose, p[0], p[1], p[2], u, v, argb, GlyphRender.FULL, 0, 1, 0);
    }

    // ------------------------------------------------------------------ time

    /**
     * The TVA's clock facing +Z at the origin (and -Z), {@code radius} across: its face, then its hands glowing in
     * front of it. Angles are in turns, 0 at twelve, going clockwise.
     */
    public static void clock(PoseStack poseStack, MultiBufferSource buffer, float radius, float alpha, float hour, float minute, float second) {
        PoseStack.Pose pose = poseStack.last();
        RenderHelper.rect(buffer.getBuffer(RenderType.entityTranslucent(CLOCK)), pose, radius * 2, radius * 2, RuneRender.alpha(0xFFFFFF, alpha), GlyphRender.FULL, true);
        VertexConsumer stroke = buffer.getBuffer(RenderType.eyes(GlyphRender.STROKE));
        for (int side = -1; side <= 1; side += 2) {
            poseStack.pushPose();
            poseStack.translate(0, 0, side * radius * .02f);
            if (side < 0) {
                poseStack.mulPose(Axis.YP.rotationDegrees(180));
            }
            PoseStack.Pose p = poseStack.last();
            float mirror = side < 0 ? -1 : 1;
            hand(stroke, p, hour * mirror, radius * .45f, radius * .07f, RuneRender.additive(Cosmos.AMBER, alpha));
            hand(stroke, p, minute * mirror, radius * .68f, radius * .05f, RuneRender.additive(Cosmos.CREAM, alpha));
            if (second >= 0) {
                hand(stroke, p, second * mirror, radius * .78f, radius * .02f, RuneRender.additive(Cosmos.ORANGE, alpha));
            }
            GlyphRender.dot(stroke, p, 0, 0, radius * .07f, RuneRender.additive(Cosmos.CREAM, alpha));
            poseStack.popPose();
        }
    }

    private static void hand(VertexConsumer stroke, PoseStack.Pose pose, float turns, float length, float width, int argb) {
        float a = turns * Mth.TWO_PI;
        float x = Mth.sin(a) * length, y = Mth.cos(a) * length;
        GlyphRender.ribbon(stroke, pose, -x * .12f, -y * .12f, x, y, width, argb);
    }

    /**
     * The TVA's time door standing at the origin, facing +Z: a frame of orange light {@code width} by {@code height}
     * (its foot on the ground), glowing hazy light rising inside it, a swirl in its heart. {@code open} 0 to 1: it
     * opens from a line of light in its middle.
     */
    public static void door(PoseStack poseStack, MultiBufferSource buffer, float width, float height, float open, float time) {
        if (open <= .01f) {
            return;
        }
        float w = width * open;
        float flicker = 1 + .06f * Mth.sin(time * 1.3f) + .04f * Mth.sin(time * 3.1f);
        poseStack.pushPose();
        poseStack.translate(0, height * .5f, 0);
        // The haze, rising
        RenderHelper.rect(buffer.getBuffer(RenderType.energySwirl(HAZE, 0, -time * .012f)), poseStack.last(), w, height,
                RuneRender.additive(Cosmos.ORANGE, .75f * open), GlyphRender.FULL, true);
        // The swirl in its heart
        poseStack.pushPose();
        poseStack.translate(0, 0, .01f);
        poseStack.mulPose(Axis.ZP.rotationDegrees(-time * 4));
        float swirl = Math.min(w, height) * .9f;
        RenderHelper.rect(buffer.getBuffer(RenderType.eyes(VORTEX)), poseStack.last(), swirl, swirl, RuneRender.additive(Cosmos.AMBER, .7f * open), GlyphRender.FULL, true);
        poseStack.popPose();
        // The frame: a wide glow and a bright core round the opening
        VertexConsumer stroke = buffer.getBuffer(RenderType.eyes(GlyphRender.STROKE));
        float hw = w * .5f, hh = height * .5f;
        Vec3[] corners = {new Vec3(-hw, -hh, 0), new Vec3(hw, -hh, 0), new Vec3(hw, hh, 0), new Vec3(-hw, hh, 0)};
        for (int k = 0; k < 4; k++) {
            Vec3 a = corners[k], b = corners[(k + 1) % 4];
            GlyphRender.beam(stroke, poseStack.last(), a, b, .5f * flicker, RuneRender.additive(Cosmos.ORANGE, .55f * open));
            GlyphRender.beam(stroke, poseStack.last(), a, b, .2f * flicker, RuneRender.additive(Cosmos.AMBER, .95f * open));
            GlyphRender.beam(stroke, poseStack.last(), a, b, .07f, RuneRender.additive(Cosmos.CREAM, open));
        }
        GlyphRender.halo(buffer, poseStack.last(), GlyphRender.GLOW, Math.max(w, height) * .9f, RuneRender.additive(Cosmos.ORANGE, .25f * open * flicker));
        poseStack.popPose();
    }

    // ------------------------------------------------------------------ echoes

    /**
     * {@code entity} drawn again where the pose is, as an echo of itself: its exact shape filled with scanlines of
     * {@code rgb} light scrolling by {@code scroll}, {@code strength} 0 to 1. Whatever the creature, its own renderer
     * draws it; only the light changes. An echo is never drawn inside another.
     */
    public static void echo(Entity entity, PoseStack poseStack, MultiBufferSource buffer, float partialTick, int rgb, float strength, float scroll) {
        echo(entity, poseStack, buffer, partialTick, rgb, strength, scroll, 0, 1);
    }

    /**
     * An echo shown only between the heights {@code from} and {@code to} (shares of its height from its feet, 0 to 1),
     * with a bright seam of cream light along each edge: a creature crumbling into light from its feet up (from
     * rising), or being scanned in (to rising). An invisible creature (one being pruned) is drawn all the same.
     */
    public static void echo(Entity entity, PoseStack poseStack, MultiBufferSource buffer, float partialTick, int rgb, float strength, float scroll,
                            float from, float to) {
        if (entity == null || echoDepth > 0 || strength <= .01f || from >= to) {
            return;
        }
        @SuppressWarnings("unchecked")
        EntityRenderer<Entity> renderer = (EntityRenderer<Entity>) Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(entity);
        RenderType swirl = RenderType.energySwirl(SCANLINES, scroll * .3f, scroll);
        int argb = RuneRender.additive(rgb, strength);
        float originY = poseStack.last().pose().m31();
        float height = Math.max(.2f, entity.getBbHeight());
        MultiBufferSource tinted = type -> type.format() == DefaultVertexFormat.NEW_ENTITY
                ? new Masked(buffer.getBuffer(swirl), argb, originY, height, from, to, RuneRender.additive(Cosmos.CREAM, strength), true)
                : Discard.INSTANCE;
        boolean wasInvisible = entity.isInvisible();
        echoDepth++;
        try {
            if (wasInvisible) {
                entity.setInvisible(false);
            }
            float yaw = Mth.lerp(partialTick, entity.yRotO, entity.getYRot());
            renderer.render(entity, yaw, partialTick, poseStack, tinted, GlyphRender.FULL);
        } catch (RuntimeException e) {
            // A creature that cannot be drawn this way is just not echoed
        } finally {
            if (wasInvisible) {
                entity.setInvisible(true);
            }
            echoDepth--;
        }
    }

    /** True while an echo is being drawn (renderers that echo others skip their own echoes then). */
    public static boolean inEcho() {
        return echoDepth > 0;
    }

    /**
     * A vertex consumer showing a model only between two heights (shares of {@code height} above {@code originY}, the
     * pose's height of its feet), with a seam of {@code edge} light at each limit. {@code additive}: every vertex gets
     * {@code fixed} light at full brightness (an echo); otherwise the model keeps its colours and fades by alpha.
     */
    public static final class Masked implements VertexConsumer {
        private static final float SEAM = .08f;
        private final VertexConsumer out;
        private final int fixed;
        private final float originY, height, from, to;
        private final int edge;
        private final boolean additive;
        private float factor = 1;
        private float seam;

        public Masked(VertexConsumer out, int fixed, float originY, float height, float from, float to, int edge, boolean additive) {
            this.out = out;
            this.fixed = fixed;
            this.originY = originY;
            this.height = height;
            this.from = from;
            this.to = to;
            this.edge = edge;
            this.additive = additive;
        }

        @Override
        public VertexConsumer addVertex(float x, float y, float z) {
            float h = (y - originY) / height;
            float below = from <= 0 ? 1 : Mth.clamp((h - from) / SEAM, 0, 1);
            float above = to >= 1 ? 1 : Mth.clamp((to - h) / SEAM, 0, 1);
            factor = below * above;
            seam = factor > 0 ? Math.max(from <= 0 ? 0 : 1 - Mth.clamp((h - from) / (SEAM * 2), 0, 1),
                    to >= 1 ? 0 : 1 - Mth.clamp((to - h) / (SEAM * 2), 0, 1)) : 0;
            out.addVertex(x, y, z);
            return this;
        }

        @Override
        public VertexConsumer setColor(int red, int green, int blue, int alpha) {
            int r = additive ? (fixed >> 16) & 0xFF : red;
            int g = additive ? (fixed >> 8) & 0xFF : green;
            int b = additive ? fixed & 0xFF : blue;
            if (seam > 0) {
                r = (int) Mth.lerp(seam * .8f, r, (edge >> 16) & 0xFF);
                g = (int) Mth.lerp(seam * .8f, g, (edge >> 8) & 0xFF);
                b = (int) Mth.lerp(seam * .8f, b, edge & 0xFF);
            }
            if (additive) {
                out.setColor((int) (r * factor), (int) (g * factor), (int) (b * factor), 255);
            } else {
                out.setColor(r, g, b, (int) (alpha * factor));
            }
            return this;
        }

        @Override
        public VertexConsumer setUv(float u, float v) {
            out.setUv(u, v);
            return this;
        }

        @Override
        public VertexConsumer setUv1(int u, int v) {
            out.setUv1(u, v);
            return this;
        }

        @Override
        public VertexConsumer setUv2(int u, int v) {
            if (additive) {
                out.setUv2(GlyphRender.FULL & 0xFFFF, GlyphRender.FULL >> 16);
            } else {
                out.setUv2(u, v);
            }
            return this;
        }

        @Override
        public VertexConsumer setNormal(float x, float y, float z) {
            out.setNormal(x, y, z);
            return this;
        }
    }

    /** What an echo does not draw (text, lines): nothing. */
    private static final class Discard implements VertexConsumer {
        static final Discard INSTANCE = new Discard();

        @Override
        public VertexConsumer addVertex(float x, float y, float z) {
            return this;
        }

        @Override
        public VertexConsumer setColor(int red, int green, int blue, int alpha) {
            return this;
        }

        @Override
        public VertexConsumer setUv(float u, float v) {
            return this;
        }

        @Override
        public VertexConsumer setUv1(int u, int v) {
            return this;
        }

        @Override
        public VertexConsumer setUv2(int u, int v) {
            return this;
        }

        @Override
        public VertexConsumer setNormal(float x, float y, float z) {
            return this;
        }
    }

    // ------------------------------------------------------------------ helpers

    /** A pose-frame direction for a sun shining from {@code worldDir} on something drawn in the world. */
    public static Vector3f sunFrom(Vec3 worldDir) {
        return new Vector3f((float) worldDir.x, (float) worldDir.y, (float) worldDir.z).normalize();
    }

    /** The direction {@code dir} brought into a pose turned by {@code rotation} (its inverse applied). */
    public static Vector3f intoPose(Vector3f dir, Matrix3f rotation) {
        return new Matrix3f(rotation).transpose().transform(new Vector3f(dir));
    }
}
