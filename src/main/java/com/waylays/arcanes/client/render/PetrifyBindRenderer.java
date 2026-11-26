package com.waylays.arcanes.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.waylays.arcanes.entity.rune.PetrifyBind;
import com.waylays.arcanes.spells.rune.RuneFx;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * The Petrifying Rune on its victim: a rune circle glowing under its feet and a crust of stone slabs climbing its legs,
 * cracks of light running up them; when the hold ends the slabs fly apart.
 */
public class PetrifyBindRenderer extends EntityRenderer<PetrifyBind> {
    private static final int FULL = LightTexture.FULL_BRIGHT;
    private static final BlockState[] CRUST = {Blocks.COBBLED_DEEPSLATE.defaultBlockState(), Blocks.DEEPSLATE.defaultBlockState(),
            Blocks.TUFF.defaultBlockState()};
    private static final int PIECES = 7;

    public PetrifyBindRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(PetrifyBind bind, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        LivingEntity target = bind.getTarget();
        float age = bind.tickCount + partialTicks;
        float shatter = bind.shatterAge(partialTicks);
        float width = target != null ? target.getBbWidth() : .6f;
        float height = (target != null ? target.getBbHeight() : 1.8f) * .55f;
        poseStack.pushPose();
        if (target != null && shatter < 0) {
            // Stick to the victim's drawn position
            Vec3 at = target.getPosition(partialTicks);
            Vec3 own = new Vec3(Mth.lerp(partialTicks, bind.xOld, bind.getX()), Mth.lerp(partialTicks, bind.yOld, bind.getY()), Mth.lerp(partialTicks, bind.zOld, bind.getZ()));
            poseStack.translate(at.x - own.x, at.y - own.y, at.z - own.z);
        }
        float lit = shatter < 0 ? Mth.clamp(age / 4, 0, 1) : Mth.clamp(1 - shatter / 6, 0, 1);
        // The rune under its feet
        poseStack.pushPose();
        poseStack.translate(0, .03, 0);
        poseStack.mulPose(Axis.YP.rotationDegrees(age * 2));
        float r = width * .5f + .9f;
        RenderHelper.groundQuad(buffer.getBuffer(com.waylays.arcanes.client.render.SpellRenderTypes.veil(RuneRender.RING_CARVED)), poseStack.last(), r, RuneRender.alpha(0xFFFFFF, lit), packedLight);
        poseStack.translate(0, .01, 0);
        RenderHelper.groundQuad(buffer.getBuffer(RenderType.eyes(RuneRender.RING_GLOW)), poseStack.last(), r, RuneRender.additive(RuneFx.GLOW, lit), FULL);
        poseStack.popPose();
        // The crust of stone
        float grow = Mth.clamp(age / PetrifyBind.GROW_TICKS, 0, 1);
        grow = 1 - (1 - grow) * (1 - grow);
        float scatter = shatter < 0 ? 0 : shatter / PetrifyBind.SHATTER_TICKS;
        if (scatter < 1) {
            for (int i = 0; i < PIECES; i++) {
                float a = i * Mth.TWO_PI / PIECES + RuneRender.hash(bind.getId() + i) * .5f;
                float h = height * (.75f + RuneRender.hash(bind.getId() * 3 + i) * .4f) * grow;
                float d = width * .5f + .05f + scatter * 1.6f;
                float lift = scatter * (1.2f - scatter * .9f);
                float size = 1 - scatter;
                poseStack.pushPose();
                poseStack.translate(Mth.cos(a) * d, lift, Mth.sin(a) * d);
                poseStack.mulPose(Axis.YP.rotationDegrees(90 - a * Mth.RAD_TO_DEG));
                poseStack.mulPose(Axis.XP.rotationDegrees(-10 + scatter * 80));
                RuneRender.block(CRUST[i % CRUST.length], poseStack, buffer, packedLight, 0, 0, 0, .42f * size, h * size, .26f * size, 0, 0);
                if (i % 2 == 0 && shatter < 0) {
                    // A crack of light running up the slab
                    poseStack.translate(0, h * .5f, .14f);
                    RuneRender.glyph(buffer.getBuffer(RenderType.eyes(RuneRender.GLYPHS)), poseStack.last(), bind.getId() + i, Math.min(.36f, h * .7f),
                            RuneRender.additive(RuneFx.GLOW, .85f * grow), FULL);
                }
                poseStack.popPose();
            }
        }
        poseStack.popPose();
        super.render(bind, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    @Override
    public boolean shouldRender(PetrifyBind entity, Frustum frustum, double x, double y, double z) {
        return true;
    }

    @Override
    public ResourceLocation getTextureLocation(PetrifyBind entity) {
        return RuneRender.GLYPHS;
    }
}
