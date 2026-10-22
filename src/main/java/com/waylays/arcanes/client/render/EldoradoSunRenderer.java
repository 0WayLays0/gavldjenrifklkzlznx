package com.waylays.arcanes.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.waylays.arcanes.entity.gold.EldoradoSun;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/**
 * Sun of Eldorado: the sun stone of Eldorado, a great disc of gold stamped with the sun's face and set with rubies and
 * emeralds, sixteen rays of gold round it turning slowly, always facing the onlooker, in a corona of light. Golden
 * beams run to the targets of the last volley. Before it explodes it shrinks, spins faster and burns white.
 */
public class EldoradoSunRenderer extends EntityRenderer<EldoradoSun> {
    /** Radius of the disc at full size. */
    private static final float RADIUS = 1.3f;

    public EldoradoSunRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(EldoradoSun sun, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        float age = sun.tickCount + partialTicks;
        float grow = Mth.clamp(age / EldoradoSun.GROW_TICKS, 0f, 1f);
        grow = 1 - (1 - grow) * (1 - grow);
        float size = Math.max(.02f, grow * (1 + Mth.sin(age * .3f) * .04f));
        float collapse = Mth.clamp((age - EldoradoSun.COLLAPSE_TICK) / (EldoradoSun.EXPLODE_TICK - EldoradoSun.COLLAPSE_TICK), 0f, 1f);
        if (collapse > 0) {
            size *= (1 - .7f * collapse) * (1 + Mth.sin(age * 2.5f) * .15f * collapse);
        }
        float spin = age * (1.2f + collapse * 14);
        int rayColour = GoldRender.lerp(GoldRender.GOLD, 0xFFF4D8, collapse);
        float half = sun.getBbHeight() * .5f;
        Vec3 centre = new Vec3(0, half, 0);

        GemRender.glow(poseStack, buffer, centre, GoldRender.GOLD, 9 * size, .45f + .3f * collapse);
        GemRender.glow(poseStack, buffer, centre, GoldRender.MOLTEN, 4.5f * size, .7f);

        poseStack.pushPose();
        poseStack.translate(0, half, 0);
        // Its face turned toward whoever looks at it
        GoldRender.faceCamera(poseStack, sun.getPosition(partialTicks).add(centre));
        poseStack.mulPose(Axis.ZP.rotationDegrees(spin));
        GoldRender.sunStone(poseStack, buffer, RADIUS * size, rayColour, GoldRender.lerp(GoldRender.DEEP, rayColour, collapse), age, sun.getId());
        poseStack.popPose();

        // Beams of the last volley
        float beamAge = age - sun.getVolleyTick();
        if (beamAge >= 0 && beamAge < EldoradoSun.BEAM_TICKS) {
            float fade = 1 - beamAge / EldoradoSun.BEAM_TICKS;
            Vec3 from = sun.getPosition(partialTicks).add(centre);
            for (int i = 0; i < EldoradoSun.MAX_BEAMS; i++) {
                Entity target = sun.level().getEntity(sun.getBeamTarget(i));
                if (target == null) {
                    continue;
                }
                Vec3 to = target.getPosition(partialTicks).add(0, target.getBbHeight() * .5, 0).subtract(from).add(centre);
                GemRender.ray(poseStack, buffer, centre, to, GoldRender.GOLD, .45f * fade + .1f, fade, age + i * 3);
                GemRender.glow(poseStack, buffer, to, GoldRender.PALE, 1.4f, fade);
            }
        }
        super.render(sun, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(EldoradoSun sun) {
        return GoldRender.SIGIL;
    }
}
