package com.waylays.arcanes.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.waylays.arcanes.entity.gold.AurumBladeEntity;
import com.waylays.arcanes.spells.gem.Gem;
import com.waylays.arcanes.spells.gold.AurumBladeSpell;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * Aurum Blade: a giant sword of gold, forged in the sky. Molten gold pours down from the sun vault; the guard, the
 * grip and the ruby pommel take shape first, then the blade runs down from the guard to its point, white-hot. It
 * plunges point first, shudders in the ground as the shock runs out in a ring of light, cools to polished gold, rings
 * again, and thins away to nothing as it melts into coins. The sun of Eldorado is stamped on both faces of the guard.
 */
public class AurumBladeRenderer extends EntityRenderer<AurumBladeEntity> {
    /** The blade, point up from the guard: a diamond section narrowing to a long point. */
    private static final float[] BLADE = new Solid.Builder()
            .band(section(0, .27f, .075f), section(3.3f, .2f, .06f))
            .fan(section(3.3f, .2f, .06f), new float[]{0, 4.2f, 0})
            .build();
    private static final float[] GUARD = new Solid.Builder()
            .box(-.95f, -.12f, -.1f, .95f, .12f, .1f)
            .build();
    private static final float[] GRIP = new Solid.Builder()
            .band(Solid.ring(6, -1.02f, .085f, .085f, 0), Solid.ring(6, -.12f, .085f, .085f, 0))
            .band(Solid.ring(6, -.86f, .108f, .108f, 0), Solid.ring(6, -.79f, .108f, .108f, 0))
            .band(Solid.ring(6, -.61f, .108f, .108f, 0), Solid.ring(6, -.54f, .108f, .108f, 0))
            .band(Solid.ring(6, -.36f, .108f, .108f, 0), Solid.ring(6, -.29f, .108f, .108f, 0))
            .band(Solid.ring(8, -1.14f, .16f, .16f, 0), Solid.ring(8, -1.02f, .11f, .11f, 0))
            .build();
    /** Length of the blade, guard to point, and the sword's scale. */
    private static final float BLADE_LENGTH = 4.2f;
    private static final float SCALE = 1.1f;
    /** How deep the point drives into the ground. */
    private static final float SINK = 1f;
    private static final int FORGE_TICKS = 12;

    public AurumBladeRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    private static float[][] section(float y, float half, float thick) {
        return new float[][]{{half, y, 0}, {0, y, thick}, {-half, y, 0}, {0, y, -thick}};
    }

    @Override
    public void render(AurumBladeEntity blade, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        float age = blade.tickCount + partialTicks;
        float hover = blade.getHover();
        float forge = Mth.clamp(age / FORGE_TICKS, 0, 1);
        float since = age - AurumBladeEntity.STRIKE_TICK;
        float tip;
        if (age < AurumBladeEntity.PLUNGE_START) {
            tip = hover + Mth.sin(age * .25f) * .12f;
        } else if (since < 0) {
            float p = (age - AurumBladeEntity.PLUNGE_START) / (AurumBladeEntity.STRIKE_TICK - AurumBladeEntity.PLUNGE_START);
            tip = Mth.lerp(p * p, hover, -SINK);
        } else {
            tip = -SINK;
        }
        float tilt = since > 0 ? (3 + RuneRender.hash(blade.getId()) * 6) * Mth.clamp(since / 3, 0, 1)
                + Mth.sin(since * 2.2f) * 5 * Math.max(0, 1 - since / 14) : 0;
        float heat = since < 0 ? (forge < 1 ? 1 : .85f) : Mth.clamp(.85f - since / 20, 0, .85f);
        float melt = Mth.clamp((AurumBladeEntity.END_TICK - age) / 8, .01f, 1);
        float hilt = Mth.clamp(forge * 3, .01f, 1) * melt;
        float fill = Mth.clamp((forge - .2f) / .8f, .01f, 1);
        float spin = (1 - forge) * 360;
        float guardY = tip + BLADE_LENGTH * SCALE;

        poseStack.pushPose();
        poseStack.translate(0, tip, 0);
        poseStack.mulPose(Axis.YP.rotationDegrees(-blade.getYRot() + spin));
        poseStack.mulPose(Axis.ZP.rotationDegrees(tilt));
        // Point down: the sword is built point up from its guard
        poseStack.mulPose(Axis.ZP.rotationDegrees(180));
        poseStack.translate(0, -BLADE_LENGTH * SCALE, 0);
        poseStack.scale(SCALE, SCALE, SCALE);
        drawSword(poseStack, buffer, heat, fill, hilt, melt, age);
        poseStack.popPose();

        if (since < 0) {
            // The pour from the vault onto the blade's running front, and the light it gives
            float front = guardY - BLADE_LENGTH * SCALE * fill;
            if (forge < 1) {
                GemRender.ray(poseStack, buffer, new Vec3(0, guardY + 6.2, 0), new Vec3(0, front, 0), GoldRender.MOLTEN, .32f, .9f * (1 - forge * .5f), age);
                GemRender.glow(poseStack, buffer, new Vec3(0, front, 0), GoldRender.MOLTEN, 1.6f, 1);
            }
            GemRender.glow(poseStack, buffer, new Vec3(0, guardY - 1.5, 0), GoldRender.GOLD, 5.5f * hilt, .35f);
        } else {
            GemRender.glow(poseStack, buffer, new Vec3(0, .3, 0), GoldRender.MOLTEN, 3.2f, Mth.clamp(1 - since / 10, 0, 1));
            shock(poseStack, buffer, since, AurumBladeSpell.RADIUS + 1);
            shock(poseStack, buffer, age - AurumBladeEntity.RING_TICK, AurumBladeSpell.RADIUS + 2.2f);
        }
        super.render(blade, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    /** The sword in its own space: guard at the origin, point up. */
    private static void drawSword(PoseStack poseStack, MultiBufferSource buffer, float heat, float fill, float hilt, float melt, float age) {
        VertexConsumer metal = GoldRender.metal(buffer);
        int bladeColour = GoldRender.heat(heat);
        int hiltColour = GoldRender.heat(heat * .5f);
        poseStack.pushPose();
        poseStack.scale(melt, fill, melt);
        Solid.draw(metal, poseStack.last(), BLADE, bladeColour, 255, age, .8f, GlyphRender.FULL);
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.scale(hilt, hilt, hilt);
        Solid.draw(metal, poseStack.last(), GUARD, hiltColour, 255, age + 5, .7f, GlyphRender.FULL);
        Solid.draw(metal, poseStack.last(), GRIP, GoldRender.lerp(GoldRender.DEEP, bladeColour, heat * .5f), 255, age + 9, .5f, GlyphRender.FULL);
        // Quillons: a needle of gold leaning out from each end of the guard toward the point
        for (int side = -1; side <= 1; side += 2) {
            poseStack.pushPose();
            poseStack.translate(side * .9f, .05f, 0);
            poseStack.mulPose(Axis.ZP.rotationDegrees(-side * 38));
            poseStack.scale(.32f, .75f, .32f);
            Solid.draw(metal, poseStack.last(), GoldRender.NEEDLE, hiltColour, 255, age + side * 3, .9f, GlyphRender.FULL);
            poseStack.popPose();
        }
        // The sun stamped on both faces of the guard
        for (int side = -1; side <= 1; side += 2) {
            poseStack.pushPose();
            poseStack.translate(0, 0, side * .1f);
            poseStack.mulPose(Axis.XP.rotationDegrees(90));
            GoldRender.coin(poseStack, buffer, .6f, age, side);
            poseStack.popPose();
        }
        // The ruby pommel
        poseStack.pushPose();
        poseStack.translate(0, -1.3f, 0);
        poseStack.mulPose(Axis.YP.rotationDegrees(age * 2));
        GemRender.gem(poseStack, buffer, GemRender.Cut.BRILLIANT, Gem.RUBY.color, .36f, .9f, age);
        poseStack.popPose();
        poseStack.popPose();
    }

    /** A ring of light running out along the ground {@code since} ticks after a shock, out to {@code reach}. */
    private static void shock(PoseStack poseStack, MultiBufferSource buffer, float since, float reach) {
        if (since < 0 || since > 8) {
            return;
        }
        float t = since / 8;
        poseStack.pushPose();
        poseStack.translate(0, .06, 0);
        RenderHelper.groundQuad(buffer.getBuffer(RenderType.eyes(RuneRender.RING_GLOW)), poseStack.last(), reach * (.2f + .8f * t),
                RuneRender.additive(GoldRender.PALE, (1 - t) * .9f), GlyphRender.FULL);
        poseStack.popPose();
    }

    @Override
    public ResourceLocation getTextureLocation(AurumBladeEntity blade) {
        return GoldRender.METAL;
    }
}
