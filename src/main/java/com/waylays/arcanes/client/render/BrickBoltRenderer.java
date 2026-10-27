package com.waylays.arcanes.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.waylays.arcanes.entity.wall.BrickBolt;
import com.waylays.arcanes.spells.wall.WallFx;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * A flying brick of Wall Magic, its mortar glowing, a streak of embers behind it: rising out of the ground and hovering
 * before it is hurled (a bolt), a lump of four lobbed high with a sigil burning where it will land (a mortar), or a
 * brick dropping out of the vortex (the rain), or a great blazing block of eight hurled from the sky (the siege), a
 * sigil burning where it will land.
 */
public class BrickBoltRenderer extends EntityRenderer<BrickBolt> {
    public BrickBoltRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(BrickBolt bolt, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        float age = bolt.tickCount + partialTicks;
        int seed = bolt.getId() * 41;
        boolean hovering = bolt.getKind() == BrickBolt.Kind.BOLT && age < bolt.getHover();
        Vec3 motion = bolt.getDeltaMovement();
        if (!hovering && motion.lengthSqr() > 1.0E-4) {
            boolean boulder = bolt.getKind() == BrickBolt.Kind.BOULDER;
            Vec3 back = motion.normalize().scale(-(boulder ? 7 : bolt.getKind() == BrickBolt.Kind.MORTAR ? 3 : 2.4));
            Vec3 mid = new Vec3(0, bolt.getBbHeight() * .5, 0);
            VertexConsumer stroke = buffer.getBuffer(RenderType.eyes(GlyphRender.STROKE));
            GlyphRender.beam(stroke, poseStack.last(), mid, mid.add(back), boulder ? 1.5f : .45f, WallRender.ember(.4f, .7f));
            GlyphRender.beam(stroke, poseStack.last(), mid, mid.add(back.scale(.6)), boulder ? .7f : .18f, WallRender.ember(1, .9f));
        }
        if (bolt.getKind() == BrickBolt.Kind.MORTAR || bolt.getKind() == BrickBolt.Kind.BOULDER) {
            // Where it will land
            Vec3 at = bolt.getTarget().subtract(bolt.getPosition(partialTicks));
            float r = bolt.getKind() == BrickBolt.Kind.BOULDER ? BrickBolt.BOULDER_RADIUS : BrickBolt.MORTAR_RADIUS;
            poseStack.pushPose();
            poseStack.translate(at.x, at.y, at.z);
            WallRender.sigil(poseStack, buffer, r, .55f + .25f * Mth.sin(age * .5f), -age * 3);
            poseStack.popPose();
        }
        poseStack.pushPose();
        poseStack.translate(0, bolt.getBbHeight() * .5f, 0);
        if (hovering) {
            float rise = Mth.clamp(age / 6, 0, 1);
            poseStack.translate(0, -(1 - rise) * (1 - rise) * 1.6f + .08f * Mth.sin(age * .4f), 0);
            poseStack.mulPose(Axis.YP.rotationDegrees(age * 4 + seed));
        } else if (bolt.getKind() == BrickBolt.Kind.BOULDER) {
            // A great block tumbles slowly
            poseStack.mulPose(Axis.YP.rotationDegrees(age * 5 + seed));
            poseStack.mulPose(Axis.XP.rotationDegrees(age * 7));
        } else {
            poseStack.mulPose(Axis.YP.rotationDegrees(age * 19 + seed));
            poseStack.mulPose(Axis.XP.rotationDegrees(age * 27));
        }
        switch (bolt.getKind()) {
            case BOLT, FALLING -> {
                float s = bolt.getKind() == BrickBolt.Kind.BOLT ? .62f : .6f;
                RuneRender.block(WallFx.brick(seed), poseStack, buffer, packedLight, 0, -s * .5f, 0, s, s, s, 0, 0);
                // The mortar after the brick: a buffer must not be written to again once another one has been asked for
                outline(buffer.getBuffer(RenderType.eyes(GlyphRender.STROKE)), poseStack, s, hovering ? .8f : 1);
            }
            case MORTAR -> {
                for (int i = 0; i < 4; i++) {
                    RuneRender.block(WallFx.brick(seed + i), poseStack, buffer, packedLight, lumpX(i), lumpY(i), lumpZ(i), .55f, .55f, .55f, 0, 0);
                }
                VertexConsumer stroke = buffer.getBuffer(RenderType.eyes(GlyphRender.STROKE));
                for (int i = 0; i < 4; i++) {
                    poseStack.pushPose();
                    poseStack.translate(lumpX(i), lumpY(i) + .275f, lumpZ(i));
                    outline(stroke, poseStack, .55f, 1);
                    poseStack.popPose();
                }
            }
            case BOULDER -> {
                // Eight blocks, two by two by two, each a little askew
                for (int i = 0; i < 8; i++) {
                    float x = (i & 1) * .9f - .45f, y = ((i >> 1) & 1) * .9f - .9f, z = (i >> 2) * .9f - .45f;
                    RuneRender.block(WallFx.brick(seed + i), poseStack, buffer, packedLight, x, y, z, .92f, .92f, .92f,
                            (RuneRender.hash(seed + i) - .5f) * 8, (RuneRender.hash(seed + i + 9) - .5f) * 8);
                }
                VertexConsumer stroke = buffer.getBuffer(RenderType.eyes(GlyphRender.STROKE));
                for (int i = 0; i < 8; i++) {
                    poseStack.pushPose();
                    poseStack.translate((i & 1) * .9f - .45f, ((i >> 1) & 1) * .9f - .45f, (i >> 2) * .9f - .45f);
                    outline(stroke, poseStack, .92f, 1);
                    poseStack.popPose();
                }
            }
        }
        poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
        float halo = switch (bolt.getKind()) {
            case MORTAR -> 1.1f;
            case BOULDER -> 2.6f;
            default -> .7f;
        };
        GlyphRender.halo(buffer, poseStack.last(), GlyphRender.GLOW, halo, WallRender.ember(.6f, bolt.getKind() == BrickBolt.Kind.BOULDER ? .8f : .55f));
        poseStack.popPose();
        super.render(bolt, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    /** Where brick {@code i} of a mortar's lump of four sits (the bottom middle). */
    private static float lumpX(int i) {
        return (i & 1) * .5f - .25f;
    }

    private static float lumpY(int i) {
        return i == 3 ? -.1f : -.5f;
    }

    private static float lumpZ(int i) {
        return (i >> 1) * .5f - .25f;
    }

    /** The glowing mortar round a cube {@code s} wide centred on the origin. */
    private static void outline(VertexConsumer stroke, PoseStack poseStack, float s, float strength) {
        float h = s * .5f + .005f;
        int argb = WallRender.ember(.7f, strength);
        float w = .05f;
        Vec3[] p = {new Vec3(-h, -h, -h), new Vec3(h, -h, -h), new Vec3(h, -h, h), new Vec3(-h, -h, h),
                new Vec3(-h, h, -h), new Vec3(h, h, -h), new Vec3(h, h, h), new Vec3(-h, h, h)};
        int[][] edges = {{0, 1}, {1, 2}, {2, 3}, {3, 0}, {4, 5}, {5, 6}, {6, 7}, {7, 4}, {0, 4}, {1, 5}, {2, 6}, {3, 7}};
        for (int[] e : edges) {
            GlyphRender.beam(stroke, poseStack.last(), p[e[0]], p[e[1]], w, argb);
        }
    }

    @Override
    public boolean shouldRender(BrickBolt bolt, Frustum frustum, double x, double y, double z) {
        return bolt.getKind() == BrickBolt.Kind.MORTAR || bolt.getKind() == BrickBolt.Kind.BOULDER || super.shouldRender(bolt, frustum, x, y, z);
    }

    @Override
    public ResourceLocation getTextureLocation(BrickBolt bolt) {
        return WallRender.SIGIL;
    }
}
