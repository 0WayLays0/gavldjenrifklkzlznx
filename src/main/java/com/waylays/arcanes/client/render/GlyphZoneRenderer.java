package com.waylays.arcanes.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.waylays.arcanes.entity.glyph.GlyphZone;
import com.waylays.arcanes.spells.glyph.GlyphReaction;
import com.waylays.arcanes.spells.glyph.GlyphStrokes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * Where a lasting reaction went off. A pool: molten rock (lava's own moving texture) or mud spread on the ground, its
 * edge soft, a rim of light round it. A vortex: a whirling funnel of streaks in the reaction's colours (steam, fire,
 * snow, storm clouds lit by lightning, water, sand). Under both, the two elements' glyphs turning on the ground.
 */
public class GlyphZoneRenderer extends EntityRenderer<GlyphZone> {
    private static final ResourceLocation LAVA = ResourceLocation.withDefaultNamespace("block/lava_still");
    private static final ResourceLocation MUD = ResourceLocation.withDefaultNamespace("block/mud");
    private static final float HEIGHT = 4.6f;
    private static final int SIDES = 18;

    public GlyphZoneRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(GlyphZone zone, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        float strength = zone.strength(partialTicks);
        if (strength <= 0) {
            return;
        }
        GlyphReaction reaction = zone.getReaction();
        float r = zone.getRadius();
        float age = zone.tickCount + partialTicks;
        if (zone.isPool()) {
            pool(poseStack, buffer, reaction == GlyphReaction.MAGMA, r * Mth.sqrt(strength), strength, packedLight);
        } else {
            vortex(poseStack, buffer, reaction, r, age, strength);
        }
        // The two elements' glyphs, turning on the ground at its heart
        poseStack.pushPose();
        poseStack.translate(0, .06, 0);
        poseStack.mulPose(Axis.XP.rotationDegrees(90));
        GlyphRender.halo(buffer, poseStack.last(), GlyphRender.RIPPLE, r + .2f, RuneRender.additive(reaction.color(), .7f * strength));
        for (int k = 0; k < 2; k++) {
            var element = k == 0 ? reaction.a : reaction.b;
            poseStack.pushPose();
            poseStack.mulPose(Axis.ZP.rotationDegrees(age * (k == 0 ? 2 : -2) + k * 90));
            poseStack.translate(k == 0 ? -.55f * r : .55f * r, 0, 0);
            GlyphRender.strokes(buffer, poseStack.last(), GlyphStrokes.of(element), .45f, 1, element.color, element.core, .6f * strength, 1);
            poseStack.popPose();
        }
        poseStack.popPose();
        super.render(zone, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    /** Tiles of lava or mud on the ground, fading out at the edge of a circle. */
    private static void pool(PoseStack poseStack, MultiBufferSource buffer, boolean magma, float r, float strength, int light) {
        TextureAtlasSprite sprite = Minecraft.getInstance().getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(magma ? LAVA : MUD);
        VertexConsumer consumer = buffer.getBuffer(RenderType.entityTranslucent(TextureAtlas.LOCATION_BLOCKS));
        PoseStack.Pose pose = poseStack.last();
        int lit = magma ? GlyphRender.FULL : light;
        int n = Mth.ceil(r);
        float y = .03f;
        for (int i = -n; i < n; i++) {
            for (int j = -n; j < n; j++) {
                if (Math.min(i * i, (i + 1) * (i + 1)) + Math.min(j * j, (j + 1) * (j + 1)) > r * r) {
                    continue;
                }
                corner(consumer, pose, i, j, r, strength, sprite.getU0(), sprite.getV0(), y, lit);
                corner(consumer, pose, i, j + 1, r, strength, sprite.getU0(), sprite.getV1(), y, lit);
                corner(consumer, pose, i + 1, j + 1, r, strength, sprite.getU1(), sprite.getV1(), y, lit);
                corner(consumer, pose, i + 1, j, r, strength, sprite.getU1(), sprite.getV0(), y, lit);
            }
        }
    }

    private static void corner(VertexConsumer consumer, PoseStack.Pose pose, float x, float z, float r, float strength, float u, float v, float y, int light) {
        float d = Mth.sqrt(x * x + z * z);
        float alpha = Mth.clamp((r - d) / .9f, 0, 1) * strength * .92f;
        RenderHelper.vertex(consumer, pose, x, y, z, u, v, RuneRender.alpha(0xFFFFFF, alpha), light, 0, 1, 0);
    }

    /** A whirling funnel: three walls of streaks turning at their own speeds, fading at the foot and the top. */
    private static void vortex(PoseStack poseStack, MultiBufferSource buffer, GlyphReaction reaction, float r, float age, float strength) {
        int[] colors = switch (reaction) {
            case STEAM -> new int[]{0xE8F2F6, 0xB8D4E0, 0xFFFFFF};
            case FIRESTORM -> new int[]{0xFF6A1A, 0xFFB040, 0xFFE0A0};
            case BLIZZARD -> new int[]{0xBFEFFF, 0x8FE6FF, 0xFFFFFF};
            case THUNDERSTORM -> new int[]{0x5A5F8A, 0x8A90C0, 0xFFE45C};
            case TYPHOON -> new int[]{0x3F9BFF, 0x7FC4FF, 0xD2EBFF};
            default -> new int[]{0xD9B26A, 0xB08A48, 0xF0DCA8};
        };
        // Lightning lights up the storm clouds now and then
        float flash = reaction == GlyphReaction.THUNDERSTORM && Math.floorMod((int) age, 10) < 2 ? 1 : 0;
        float grow = Mth.clamp(age / GlyphZone.GROW_TICKS, 0, 1);
        VertexConsumer consumer = buffer.getBuffer(RenderType.eyes(GlyphRender.SWIRL));
        PoseStack.Pose pose = poseStack.last();
        for (int layer = 0; layer < 3; layer++) {
            float bottom = r * (.25f + .12f * layer);
            float top = r * (1.05f + .15f * layer);
            float spin = age * (.035f + .02f * layer) * (layer == 1 ? -1 : 1);
            int color = layer == 2 && flash > 0 ? 0xFFFDE8 : colors[layer];
            float s = strength * (layer == 2 ? .45f + .5f * flash : .6f);
            wall(consumer, pose, bottom, top, HEIGHT * grow * (1 - .08f * layer), spin, color, s);
        }
    }

    private static void wall(VertexConsumer consumer, PoseStack.Pose pose, float bottom, float top, float height, float spin, int color, float strength) {
        int rings = 6;
        for (int ring = 0; ring < rings; ring++) {
            float t0 = ring / (float) rings, t1 = (ring + 1) / (float) rings;
            float r0 = Mth.lerp(t0 * t0, bottom, top), r1 = Mth.lerp(t1 * t1, bottom, top);
            float y0 = t0 * height, y1 = t1 * height;
            // Faint at the foot and fading out at the top
            int c0 = RuneRender.additive(color, strength * Math.min(1, t0 * 4) * (1 - t0));
            int c1 = RuneRender.additive(color, strength * Math.min(1, t1 * 4) * (1 - t1));
            for (int k = 0; k < SIDES; k++) {
                float a0 = k * Mth.TWO_PI / SIDES, a1 = (k + 1) * Mth.TWO_PI / SIDES;
                float u0 = k * 2f / SIDES + spin, u1 = (k + 1) * 2f / SIDES + spin;
                float x00 = Mth.cos(a0) * r0, z00 = Mth.sin(a0) * r0, x10 = Mth.cos(a1) * r0, z10 = Mth.sin(a1) * r0;
                float x01 = Mth.cos(a0) * r1, z01 = Mth.sin(a0) * r1, x11 = Mth.cos(a1) * r1, z11 = Mth.sin(a1) * r1;
                float v0 = t0 + spin * .5f, v1 = t1 + spin * .5f;
                RenderHelper.vertex(consumer, pose, x00, y0, z00, u0, v0, c0, GlyphRender.FULL, 0, 1, 0);
                RenderHelper.vertex(consumer, pose, x10, y0, z10, u1, v0, c0, GlyphRender.FULL, 0, 1, 0);
                RenderHelper.vertex(consumer, pose, x11, y1, z11, u1, v1, c1, GlyphRender.FULL, 0, 1, 0);
                RenderHelper.vertex(consumer, pose, x01, y1, z01, u0, v1, c1, GlyphRender.FULL, 0, 1, 0);
                RenderHelper.vertex(consumer, pose, x01, y1, z01, u0, v1, c1, GlyphRender.FULL, 0, 1, 0);
                RenderHelper.vertex(consumer, pose, x11, y1, z11, u1, v1, c1, GlyphRender.FULL, 0, 1, 0);
                RenderHelper.vertex(consumer, pose, x10, y0, z10, u1, v0, c0, GlyphRender.FULL, 0, 1, 0);
                RenderHelper.vertex(consumer, pose, x00, y0, z00, u0, v0, c0, GlyphRender.FULL, 0, 1, 0);
            }
        }
    }

    @Override
    public boolean shouldRender(GlyphZone entity, Frustum frustum, double x, double y, double z) {
        return true;
    }

    @Override
    public ResourceLocation getTextureLocation(GlyphZone entity) {
        return GlyphRender.SWIRL;
    }
}
