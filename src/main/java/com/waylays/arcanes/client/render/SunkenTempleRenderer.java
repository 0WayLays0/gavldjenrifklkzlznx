package com.waylays.arcanes.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.waylays.arcanes.entity.verdigris.SunkenTemple;
import com.waylays.arcanes.spells.verdigris.VerdigrisFx;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * The Sunken Temple: eight tall columns of verdigrised copper drums on mossy plinths, capitals hung with vines, rising
 * one after another in a ring; stone lintels drop onto them; a ring of old glyphs glows green on the ground inside.
 * Then every column topples inward at once.
 */
public class SunkenTempleRenderer extends EntityRenderer<SunkenTemple> {
    private static final float H = SunkenTemple.COLUMN_HEIGHT;

    public SunkenTempleRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(SunkenTemple temple, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        float fallen = temple.fallen(partialTicks);
        int seed = temple.getId();
        for (int i = 0; i < SunkenTemple.COLUMNS; i++) {
            float risen = temple.risen(i, partialTicks);
            if (risen <= .001f) {
                continue;
            }
            Vec3 at = temple.column(i);
            poseStack.pushPose();
            poseStack.translate(at.x, 0, at.z);
            // Standing, it rises out of the ground; lying, it sinks into it
            poseStack.translate(0, -(1 - risen) * (fallen > 0 ? 1.4f : H), 0);
            // Turned so that its +Z faces the middle: it topples that way
            poseStack.mulPose(Axis.YP.rotation((float) Mth.atan2(-at.x, -at.z)));
            if (fallen > 0) {
                poseStack.mulPose(Axis.XP.rotationDegrees(fallen * (.92f + .08f * RuneRender.hash(seed + i))));
            }
            column(poseStack, buffer, packedLight, seed * 8 + i);
            poseStack.popPose();
        }
        float lintels = temple.lintels(partialTicks);
        if (lintels > 0) {
            for (int i = 0; i < SunkenTemple.COLUMNS; i++) {
                Vec3 a = temple.column(i);
                Vec3 b = temple.column((i + 1) % SunkenTemple.COLUMNS);
                Vec3 mid = a.add(b).scale(.5);
                Vec3 d = b.subtract(a);
                poseStack.pushPose();
                // They drop into place from above, one after the other
                float l = Mth.clamp(lintels * 1.6f - i * .08f, 0, 1);
                poseStack.translate(mid.x, H + (1 - l * l) * 3, mid.z);
                poseStack.mulPose(Axis.YP.rotation((float) Mth.atan2(d.x, d.z)));
                VerdigrisRender.block(i % 3 == 0 ? VerdigrisRender.CRACKED_BRICKS : VerdigrisRender.MOSSY_BRICKS, poseStack, buffer, packedLight,
                        0, 0, 0, 1, .55f, (float) d.length() + 1.2f);
                VerdigrisRender.block(VerdigrisRender.MOSS_CARPET, poseStack, buffer, packedLight, 0, .55f, 0, .9f, 1, (float) d.length() + .8f);
                poseStack.popPose();
            }
        }
        ground(temple, partialTicks, poseStack, buffer);
        super.render(temple, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    /** One column, six blocks tall: plinth, three copper drums with carved bands, capital, mossy abacus, vines. */
    private static void column(PoseStack poseStack, MultiBufferSource buffer, int light, int seed) {
        VerdigrisRender.block(VerdigrisRender.MOSSY_BRICKS, poseStack, buffer, light, 0, 0, 0, 1.5f, .6f, 1.5f);
        VerdigrisRender.block(VerdigrisRender.OXIDIZED_CUT, poseStack, buffer, light, 0, .6f, 0, 1.2f, .3f, 1.2f);
        for (int k = 0; k < 3; k++) {
            float y = .9f + k * 1.4f;
            VerdigrisRender.turned(k == 1 ? VerdigrisRender.WEATHERED : VerdigrisRender.OXIDIZED, poseStack, buffer, light, 0, y, 0, .95f, 1.4f, .95f, 0);
            VerdigrisRender.turned(k == 1 ? VerdigrisRender.WEATHERED : VerdigrisRender.OXIDIZED, poseStack, buffer, light, 0, y, 0, .9f, 1.4f, .9f, 45);
            if (k > 0) {
                VerdigrisRender.block(VerdigrisRender.OXIDIZED_CHISELED, poseStack, buffer, light, 0, y - .08f, 0, 1.04f, .16f, 1.04f);
            }
        }
        VerdigrisRender.block(VerdigrisRender.OXIDIZED_CHISELED, poseStack, buffer, light, 0, 5.1f, 0, 1.35f, .45f, 1.35f);
        VerdigrisRender.block(VerdigrisRender.MOSSY_BRICKS, poseStack, buffer, light, 0, 5.55f, 0, 1.5f, .45f, 1.5f);
        VerdigrisRender.vines(poseStack, buffer, light, 6, 1.5f, 1.5f, VerdigrisRender.hanging(seed, 3.2f));
        VerdigrisRender.vines(poseStack, buffer, light, .9f, 1.5f, 1.5f, VerdigrisRender.hanging(seed + 3, .5f));
    }

    /** The ring of glyphs inside the temple, brightening until the columns fall, then a flash and gone. */
    private static void ground(SunkenTemple temple, float partialTicks, PoseStack poseStack, MultiBufferSource buffer) {
        float age = temple.tickCount + partialTicks;
        float strength;
        if (age < SunkenTemple.IMPACT) {
            strength = Mth.clamp(age / 20, 0, 1) * (.3f + .35f * age / SunkenTemple.IMPACT + .08f * Mth.sin(age * .4f));
        } else {
            strength = Mth.clamp(1 - (age - SunkenTemple.IMPACT) / 10, 0, 1);
        }
        if (strength <= .01f) {
            return;
        }
        poseStack.pushPose();
        poseStack.translate(0, .04, 0);
        poseStack.mulPose(Axis.YP.rotationDegrees(age * .7f));
        RenderHelper.groundQuad(buffer.getBuffer(RenderType.eyes(RuneRender.RING_GLOW)), poseStack.last(), SunkenTemple.RADIUS - .4f,
                RuneRender.additive(VerdigrisFx.VERDIGRIS, strength), LightTexture.FULL_BRIGHT);
        poseStack.mulPose(Axis.YP.rotationDegrees(-age * 1.6f));
        poseStack.translate(0, .01, 0);
        RenderHelper.groundQuad(buffer.getBuffer(RenderType.eyes(RuneRender.RING_GLOW)), poseStack.last(), SunkenTemple.RADIUS * .55f,
                RuneRender.additive(VerdigrisFx.MOSS, strength * .8f), LightTexture.FULL_BRIGHT);
        poseStack.popPose();
    }

    @Override
    public boolean shouldRender(SunkenTemple temple, Frustum frustum, double x, double y, double z) {
        return frustum.isVisible(temple.getBoundingBox().inflate(SunkenTemple.RADIUS + 2, 1, SunkenTemple.RADIUS + 2).expandTowards(0, H + 3, 0));
    }

    @Override
    public ResourceLocation getTextureLocation(SunkenTemple temple) {
        return RuneRender.GLOW;
    }
}
