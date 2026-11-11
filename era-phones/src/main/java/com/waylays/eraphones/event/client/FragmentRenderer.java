package com.waylays.eraphones.event.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.waylays.eraphones.EraPhones;
import com.waylays.eraphones.event.StarFragment;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * The star fragment: a cluster of pale crystal shards bursting from one point like a star, white-gold at their hearts,
 * a cold blue-lilac at their tips, each facet catching the light, twinkling one after another; turning slowly as it
 * floats; wrapped in a soft golden halo that breathes, a four-pointed glint across it.
 */
public class FragmentRenderer extends EntityRenderer<StarFragment> {
    private static final ResourceLocation CRYSTAL = EraPhones.id("textures/entity/fragment_etoile.png");
    private static final ResourceLocation HALO = EraPhones.id("textures/entity/fragment_halo.png");
    private static final ResourceLocation GLINT = EraPhones.id("textures/entity/fragment_eclat.png");
    /** The shards: direction (yaw, pitch from straight up, degrees), length, thickness. The first is the heart. */
    private static final float[][] SHARDS = {
            {0, 0, .30f, .105f}, {0, 180, .2f, .085f},
            {20, 62, .26f, .07f}, {110, 70, .22f, .06f}, {200, 58, .27f, .068f}, {290, 66, .21f, .058f},
            {65, 112, .17f, .05f}, {160, 118, .16f, .048f}, {250, 108, .18f, .05f}, {340, 115, .15f, .045f},
            {45, 30, .19f, .05f}, {225, 34, .18f, .048f}};
    private static final Vector3f LIGHT = new Vector3f(.4f, .8f, .45f).normalize();

    public FragmentRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = .3f;
        this.shadowStrength = .4f;
    }

    @Override
    public ResourceLocation getTextureLocation(StarFragment f) {
        return CRYSTAL;
    }

    @Override
    protected int getBlockLightLevel(StarFragment f, net.minecraft.core.BlockPos pos) {
        return 15;
    }

    @Override
    public void render(StarFragment f, float yaw, float pt, PoseStack pose, MultiBufferSource buffers, int light) {
        float t = f.tickCount + pt;
        boolean held = f.isVehicle();
        float bob = held ? Mth.sin(Mth.lerp(pt, f.walkO, f.walk) * 2) * .025f * Mth.lerp(pt, f.strideO, f.stride)
                : Mth.sin(t * .06f) * .07f;
        pose.pushPose();
        pose.translate(0, (held ? StarFragment.HELD_AT : StarFragment.RESTS_AT) + bob, 0);
        // The halo and the glint, facing the eye
        pose.pushPose();
        pose.mulPose(entityRenderDispatcher.cameraOrientation());
        float breathe = .5f + .5f * Mth.sin(t * .09f);
        quad(pose, buffers.getBuffer(RenderType.eyes(HALO)), 1.5f + .15f * breathe, 0xFF, 0xD8, 0x8C, (int) (110 + 50 * breathe));
        quad(pose, buffers.getBuffer(RenderType.eyes(HALO)), .7f, 0xFF, 0xF6, 0xE0, 200);
        pose.mulPose(Axis.ZP.rotationDegrees(t * .8f));
        quad(pose, buffers.getBuffer(RenderType.eyes(GLINT)), 1.1f + .2f * Mth.sin(t * .13f), 0xFF, 0xFF, 0xFF, (int) (150 + 80 * breathe));
        pose.popPose();
        // The shards, turning slowly
        pose.mulPose(Axis.YP.rotationDegrees(t * (held ? .6f : 1.2f)));
        pose.mulPose(Axis.XP.rotationDegrees(8 * Mth.sin(t * .03f)));
        VertexConsumer vc = buffers.getBuffer(RenderType.entityTranslucentEmissive(CRYSTAL));
        for (int i = 0; i < SHARDS.length; i++) {
            float[] s = SHARDS[i];
            float twinkle = .82f + .18f * Mth.sin(t * .17f + i * 1.9f);
            pose.pushPose();
            pose.mulPose(Axis.YP.rotationDegrees(s[0]));
            pose.mulPose(Axis.XP.rotationDegrees(s[1]));
            shard(pose, vc, s[2], s[3], twinkle, i);
            pose.popPose();
        }
        pose.popPose();
        super.render(f, yaw, pt, pose, buffers, light);
    }

    /** A square facing the eye, {@code size} across, tinted. */
    private static void quad(PoseStack pose, VertexConsumer vc, float size, int r, int g, int b, int a) {
        Matrix4f m = pose.last().pose();
        float h = size / 2;
        vc.addVertex(m, -h, -h, 0).setColor(r, g, b, a).setUv(0, 1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(0, 0, 1);
        vc.addVertex(m, h, -h, 0).setColor(r, g, b, a).setUv(1, 1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(0, 0, 1);
        vc.addVertex(m, h, h, 0).setColor(r, g, b, a).setUv(1, 0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(0, 0, 1);
        vc.addVertex(m, -h, h, 0).setColor(r, g, b, a).setUv(0, 0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(0, 0, 1);
    }

    /**
     * One shard, pointing up from the heart: six-sided, swelling a little from its root, then tapering to a point; its
     * facets lit by a fixed light, warm at the root, cold at the tip.
     */
    private static void shard(PoseStack pose, VertexConsumer vc, float length, float thick, float twinkle, int seed) {
        int sides = 6;
        float waist = length * .32f, root = -thick * .6f;
        Vector3f[] ring = new Vector3f[sides];
        for (int k = 0; k < sides; k++) {
            float a = (k + .5f * (seed % 2)) * Mth.TWO_PI / sides;
            float r = thick * (k % 2 == 0 ? 1 : .82f);
            ring[k] = new Vector3f(Mth.cos(a) * r, waist, Mth.sin(a) * r);
        }
        Vector3f tip = new Vector3f(0, length, 0), base = new Vector3f(0, root, 0);
        for (int k = 0; k < sides; k++) {
            Vector3f a = ring[k], b = ring[(k + 1) % sides];
            // Up to the tip, then down to the root
            face(pose.last(), vc, a, b, tip, twinkle, .55f, 1f, k);
            face(pose.last(), vc, b, a, base, twinkle, .55f, 0f, k);
        }
    }

    /** A three-cornered facet (drawn as a quad with its last corner twice), coloured by how it meets the light. */
    private static void face(PoseStack.Pose m, VertexConsumer vc, Vector3f a, Vector3f b, Vector3f c, float twinkle, float vMid, float vEnd, int k) {
        Vector3f n = new Vector3f(b).sub(a).cross(new Vector3f(c).sub(a)).normalize();
        float lit = .62f + .38f * Math.max(0, n.dot(LIGHT));
        float edge = (k % 3 == 0 ? 1.06f : 1f) * twinkle;
        // Warm white-gold at the heart, toward a cold lilac-blue at the points
        int[] warm = {255, 236, 190}, cold = {196, 210, 255};
        int[] ca = mix(warm, cold, .25f, lit * edge), cc = mix(warm, cold, vEnd, lit * edge);
        put(m, vc, a, ca, 0, vMid, n);
        put(m, vc, b, ca, 1, vMid, n);
        put(m, vc, c, cc, .5f, vEnd, n);
        put(m, vc, c, cc, .5f, vEnd, n);
    }

    private static int[] mix(int[] a, int[] b, float k, float light) {
        int[] o = new int[3];
        for (int i = 0; i < 3; i++) {
            o[i] = Mth.clamp((int) ((a[i] + (b[i] - a[i]) * k) * light), 0, 255);
        }
        return o;
    }

    private static void put(PoseStack.Pose m, VertexConsumer vc, Vector3f p, int[] c, float u, float v, Vector3f n) {
        vc.addVertex(m.pose(), p.x, p.y, p.z).setColor(c[0], c[1], c[2], 235).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightTexture.FULL_BRIGHT).setNormal(m, n.x, n.y, n.z);
    }
}
